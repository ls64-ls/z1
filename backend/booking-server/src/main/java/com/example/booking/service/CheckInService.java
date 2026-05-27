package com.example.booking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingCheckIn;
import com.example.booking.enums.BookingStatus;
import com.example.booking.exception.BusinessException;
import com.example.booking.exception.ErrorCode;
import com.example.booking.repository.mapper.BookingCheckInMapper;
import com.example.booking.repository.mapper.BookingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInService {

    private final BookingCheckInMapper checkInMapper;
    private final BookingMapper bookingMapper;

    @Transactional
    public void checkIn(Long bookingId, Long userId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!BookingStatus.CONFIRMED.name().equals(booking.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_INVALID, "褰撳墠鐘舵€佷笉鍙鍒帮紝浠呭凡纭鐨勯璁㈠彲绛惧埌");
        }

        BookingCheckIn checkIn = new BookingCheckIn();
        checkIn.setBookingId(bookingId);
        checkIn.setCheckInTime(OffsetDateTime.now());
        checkIn.setOperatorId(userId);
        checkInMapper.insert(checkIn);

        booking.setStatus(BookingStatus.CHECKED_IN.name());
        bookingMapper.updateById(booking);
        log.info("Check-in success: bookingId={}, userId={}", bookingId, userId);
    }

    @Transactional
    public void checkOut(Long bookingId, Long userId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!BookingStatus.CHECKED_IN.name().equals(booking.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_INVALID, "褰撳墠鐘舵€佷笉鍙閫€锛屼粎宸茬鍒扮殑棰勮鍙閫€");
        }

        BookingCheckIn checkIn = checkInMapper.selectOne(
                new LambdaQueryWrapper<BookingCheckIn>().eq(BookingCheckIn::getBookingId, bookingId));
        if (checkIn == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到签到记录");
        }
        checkIn.setCheckOutTime(OffsetDateTime.now());
        checkIn.setOperatorId(userId);
        checkInMapper.updateById(checkIn);

        booking.setStatus(BookingStatus.COMPLETED.name());
        bookingMapper.updateById(booking);
        log.info("Check-out success: bookingId={}, userId={}", bookingId, userId);
    }
}
