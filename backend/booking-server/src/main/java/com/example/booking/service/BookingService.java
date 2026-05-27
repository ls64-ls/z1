package com.example.booking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.booking.cache.AvailabilityCacheManager;
import com.example.booking.common.PageResult;
import com.example.booking.dto.response.BookingVO;
import com.example.booking.dto.response.PreCheckResultVO;
import com.example.booking.entity.*;
import com.example.booking.enums.BookingStatus;
import com.example.booking.exception.BusinessException;
import com.example.booking.exception.ErrorCode;
import com.example.booking.repository.mapper.*;
import com.example.booking.util.BookingNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingMapper bookingMapper;
    private final RoomMapper roomMapper;
    private final VenueMapper venueMapper;
    private final BookingNoGenerator bookingNoGenerator;
    private final RedissonClient redissonClient;
    private final NotificationService notificationService;
    private final AvailabilityCacheManager availabilityCacheManager;

    /**
     * Pre-check — lightweight conflict detection without creating a booking.
     * Returns availability info and suggestions if the slot is taken.
     */
    public PreCheckResultVO preCheck(Long roomId, String bookingDate, String startTime, String endTime) {
        LocalDate date = LocalDate.parse(bookingDate);
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);

        if (!start.isBefore(end)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "开始时间必须在结束时间之前");
        }

        int conflicts = bookingMapper.countConflicts(roomId, date, start, end);
        if (conflicts > 0) {
            // Find conflicting booking details
            List<Booking> conflicting = findConflictingBookings(roomId, date, start, end);
            PreCheckResultVO.ConflictBookingVO conflictVO = null;
            if (!conflicting.isEmpty()) {
                Booking b = conflicting.get(0);
                conflictVO = PreCheckResultVO.ConflictBookingVO.builder()
                        .startTime(b.getStartTime() != null ? b.getStartTime().toString() : null)
                        .endTime(b.getEndTime() != null ? b.getEndTime().toString() : null)
                        .build();
            }

            // Find nearest available slot suggestion
            PreCheckResultVO.SuggestionVO suggestion = findNearestAvailable(roomId, date, end);

            return PreCheckResultVO.builder()
                    .available(false)
                    .conflictBooking(conflictVO)
                    .suggestion(suggestion)
                    .build();
        }

        return PreCheckResultVO.builder().available(true).build();
    }

    /**
     * Core booking creation with multi-layer conflict protection:
     * Layer 1: Redis distributed lock (room:date granularity)
     * Layer 2: Application-level conflict check via GiST index
     * Layer 3: PostgreSQL exclusion constraint (database-level guarantee)
     */
    @Transactional(timeout = 10)
    public Map<String, Object> create(Long userId, Long roomId, String bookingDate, String startTime, String endTime,
                                       String title, Integer attendeeCount, String remark) {
        LocalDate date = LocalDate.parse(bookingDate);
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);

        if (!start.isBefore(end)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "开始时间必须在结束时间之前");
        }

        // Validate room availability
        Room room = roomMapper.selectById(roomId);
        if (room == null || !"AVAILABLE".equals(room.getStatus())) {
            throw new BusinessException(ErrorCode.ROOM_NOT_AVAILABLE);
        }

        String lockKey = "booking:lock:room:" + roomId + ":date:" + bookingDate;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Layer 1: Redis distributed lock
            if (!lock.tryLock(3, 5, TimeUnit.SECONDS)) {
                throw new BusinessException(ErrorCode.BOOKING_BUSY, "系统繁忙，请稍后重试");
            }

            // Layer 2: Application-level conflict check
            int conflicts = bookingMapper.countConflicts(roomId, date, start, end);
            if (conflicts > 0) {
                throw new BusinessException(ErrorCode.BOOKING_CONFLICT);
            }

            // Build and insert booking
            String bookingNo = bookingNoGenerator.generate(date);
            String tsRange = "[\"" + date + " " + start + "\",\"" + date + " " + end + "\")";

            Booking booking = new Booking();
            booking.setBookingNo(bookingNo);
            booking.setUserId(userId);
            booking.setRoomId(roomId);
            booking.setBookingDate(date);
            booking.setStartTime(start);
            booking.setEndTime(end);
            booking.setTimeSlot(tsRange);
            booking.setTitle(title);
            booking.setAttendeeCount(attendeeCount);
            booking.setRemark(remark);
            booking.setStatus(BookingStatus.CONFIRMED.name());

            // Layer 3: PostgreSQL exclusion constraint auto-validates on insert
            bookingMapper.insert(booking);

            // Post-commit hooks
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            notificationService.sendBookingConfirmation(booking);
                            availabilityCacheManager.evictRoomAvailability(roomId, bookingDate);
                        }
                    });

            Map<String, Object> result = new HashMap<>();
            result.put("id", booking.getId());
            result.put("bookingNo", bookingNo);
            result.put("status", booking.getStatus());
            return result;

        } catch (DataIntegrityViolationException e) {
            if (e.getMostSpecificCause() != null
                    && e.getMostSpecificCause().getMessage().contains("excl_booking_no_overlap")) {
                throw new BusinessException(ErrorCode.SLOT_CONFLICT, "该时段已被他人抢先预订");
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.BOOKING_BUSY, "系统繁忙，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public PageResult<BookingVO> getMyBookings(Long userId, String status, int page, int size) {
        LambdaQueryWrapper<Booking> qw = new LambdaQueryWrapper<Booking>()
                .eq(Booking::getUserId, userId)
                .orderByDesc(Booking::getBookingDate, Booking::getStartTime);
        if (status != null && !status.isEmpty()) {
            qw.eq(Booking::getStatus, status);
        }
        Page<Booking> pg = bookingMapper.selectPage(new Page<>(page, size), qw);

        List<BookingVO> records = pg.getRecords().stream()
                .map(this::toBookingVO)
                .collect(Collectors.toList());

        return new PageResult<>(page, size, pg.getTotal(), records);
    }

    public BookingVO getDetail(Long bookingId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) return null;
        return toBookingVO(booking);
    }

    @Transactional
    public void cancel(Long bookingId, Long userId, String reason) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }
        BookingStatus status = BookingStatus.valueOf(booking.getStatus());
        if (!status.isCancellable() && status != BookingStatus.PENDING) {
            throw new BusinessException(ErrorCode.STATUS_INVALID, "当前状态不可取消");
        }

        booking.setStatus(BookingStatus.CANCELLED.name());
        bookingMapper.updateById(booking);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationService.sendCancellationNotice(booking, reason);
                        availabilityCacheManager.evictRoomAvailability(
                                booking.getRoomId(),
                                booking.getBookingDate() != null ? booking.getBookingDate().toString() : "");
                    }
                });
    }

    // --- Helper methods ---

    private BookingVO toBookingVO(Booking b) {
        BookingVO.BookingVOBuilder builder = BookingVO.builder()
                .id(b.getId())
                .bookingNo(b.getBookingNo())
                .roomId(b.getRoomId())
                .userId(b.getUserId())
                .bookingDate(b.getBookingDate() != null ? b.getBookingDate().toString() : null)
                .startTime(b.getStartTime() != null ? b.getStartTime().toString() : null)
                .endTime(b.getEndTime() != null ? b.getEndTime().toString() : null)
                .title(b.getTitle())
                .attendeeCount(b.getAttendeeCount())
                .status(b.getStatus())
                .remark(b.getRemark())
                .createdAt(b.getCreatedAt() != null
                        ? b.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : null);

        Room room = roomMapper.selectById(b.getRoomId());
        if (room != null) {
            builder.roomName(room.getName());

            long hours = Duration.between(b.getStartTime(), b.getEndTime()).toHours();
            if (hours <= 0) hours = 1;
            if (room.getPricePerHour() != null) {
                builder.totalAmount(room.getPricePerHour().multiply(BigDecimal.valueOf(hours)));
            }

            Venue venue = venueMapper.selectById(room.getVenueId());
            if (venue != null) {
                builder.venueName(venue.getName());
            }
        }

        return builder.build();
    }

    private List<Booking> findConflictingBookings(Long roomId, LocalDate date, LocalTime start, LocalTime end) {
        return bookingMapper.selectList(
                new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getRoomId, roomId)
                        .eq(Booking::getBookingDate, date)
                        .notIn(Booking::getStatus, "CANCELLED", "EXPIRED")
                        .lt(Booking::getStartTime, end)
                        .gt(Booking::getEndTime, start)   // Manual overlap check as fallback
                        .last("LIMIT 1"));
    }

    private PreCheckResultVO.SuggestionVO findNearestAvailable(Long roomId, LocalDate date, LocalTime after) {
        // Find the nearest gap after the requested time
        List<Booking> dayBookings = bookingMapper.selectList(
                new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getRoomId, roomId)
                        .eq(Booking::getBookingDate, date)
                        .notIn(Booking::getStatus, "CANCELLED", "EXPIRED")
                        .orderByAsc(Booking::getStartTime));

        LocalTime cursor = after;
        for (Booking b : dayBookings) {
            if (b.getStartTime() == null || b.getEndTime() == null) continue;
            if (!cursor.isBefore(b.getStartTime())) {
                cursor = cursor.isBefore(b.getEndTime()) ? b.getEndTime() : cursor;
            } else {
                break;  // found a gap
            }
        }

        // Check against venue close time (default 22:00)
        LocalTime closeTime = LocalTime.of(22, 0);
        if (!cursor.isBefore(closeTime) || Duration.between(cursor, closeTime).toHours() < 1) {
            return null;  // no available slot today
        }

        LocalTime suggestionEnd = cursor.plusHours(2).isAfter(closeTime) ? closeTime : cursor.plusHours(2);
        return PreCheckResultVO.SuggestionVO.builder()
                .availableStart(cursor.toString())
                .availableEnd(suggestionEnd.toString())
                .build();
    }
}
