package com.example.booking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.booking.cache.AvailabilityCacheManager;
import com.example.booking.cache.RoomCacheManager;
import com.example.booking.common.PageResult;
import com.example.booking.dto.response.*;
import com.example.booking.entity.*;
import com.example.booking.repository.mapper.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomMapper roomMapper;
    private final RoomImageMapper roomImageMapper;
    private final RoomAmenityMapper roomAmenityMapper;
    private final AmenityMapper amenityMapper;
    private final BookingMapper bookingMapper;
    private final AvailabilityRuleMapper availabilityRuleMapper;
    private final BlockedTimeSlotMapper blockedTimeSlotMapper;
    private final VenueMapper venueMapper;
    private final RoomCacheManager roomCacheManager;
    private final AvailabilityCacheManager availabilityCacheManager;
    private final ObjectMapper objectMapper;

    public PageResult<RoomVO> searchRooms(Long venueId, Integer capacity, List<Long> amenityIds, int page, int size) {
        List<Room> rooms = roomMapper.searchRooms(venueId, capacity, amenityIds);

        List<RoomVO> enriched = rooms.stream()
                .map(this::toRoomVO)
                .collect(Collectors.toList());

        int start = (page - 1) * size;
        int end = Math.min(start + size, enriched.size());
        List<RoomVO> paged = start < enriched.size() ? enriched.subList(start, end) : Collections.emptyList();

        return new PageResult<>(page, size, (long) enriched.size(), paged);
    }

    public RoomVO getDetail(Long roomId) {
        // Try cache first
        String cached = roomCacheManager.getRoomDetailFromCache(roomId);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, RoomVO.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached room detail for roomId={}", roomId);
            }
        }

        Room room = roomMapper.selectById(roomId);
        if (room == null) return null;

        RoomVO vo = toRoomVO(room);

        // Cache the result
        try {
            roomCacheManager.setRoomDetail(roomId, objectMapper.writeValueAsString(vo));
        } catch (Exception e) {
            log.warn("Failed to cache room detail for roomId={}", roomId);
        }

        return vo;
    }

    public AvailabilityVO getAvailability(Long roomId, String date) {
        // Try cache first
        String cached = availabilityCacheManager.getAvailability(roomId, date);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, AvailabilityVO.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached availability for roomId={}, date={}", roomId, date);
            }
        }

        LocalDate bookingDate = LocalDate.parse(date);
        int dayOfWeek = bookingDate.getDayOfWeek().getValue();

        List<AvailabilityRule> rules = availabilityRuleMapper.selectList(
                new LambdaQueryWrapper<AvailabilityRule>()
                        .eq(AvailabilityRule::getRoomId, roomId)
                        .eq(AvailabilityRule::getDayOfWeek, dayOfWeek)
                        .eq(AvailabilityRule::getIsActive, true));

        List<AvailabilityVO.SlotVO> slots = new ArrayList<>();
        for (AvailabilityRule rule : rules) {
            LocalTime t = rule.getOpenTime();
            while (t.isBefore(rule.getCloseTime())) {
                LocalTime next = t.plusHours(1);
                if (!next.isAfter(rule.getCloseTime())) {
                    slots.add(AvailabilityVO.SlotVO.builder()
                            .start(t.toString())
                            .end(next.toString())
                            .available(true)
                            .build());
                }
                t = next;
            }
        }

        // Mark booked slots
        List<Booking> bookings = bookingMapper.selectList(
                new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getRoomId, roomId)
                        .eq(Booking::getBookingDate, bookingDate)
                        .notIn(Booking::getStatus, "CANCELLED", "EXPIRED"));

        Set<String> bookedSet = new HashSet<>();
        for (Booking b : bookings) {
            if (b.getStartTime() != null && b.getEndTime() != null) {
                bookedSet.add(b.getStartTime().toString() + "-" + b.getEndTime().toString());
            }
        }

        // Check blocked slots
        List<BlockedTimeSlot> blocked = blockedTimeSlotMapper.selectList(
                new LambdaQueryWrapper<BlockedTimeSlot>()
                        .eq(BlockedTimeSlot::getRoomId, roomId)
                        .eq(BlockedTimeSlot::getBlockedDate, bookingDate));
        for (BlockedTimeSlot b : blocked) {
            if (b.getStartTime() != null && b.getEndTime() != null) {
                bookedSet.add(b.getStartTime().toString() + "-" + b.getEndTime().toString());
            }
        }

        for (AvailabilityVO.SlotVO slot : slots) {
            String key = slot.getStart() + "-" + slot.getEnd();
            if (bookedSet.contains(key)) {
                slot.setAvailable(false);
            }
        }

        AvailabilityVO vo = AvailabilityVO.builder()
                .date(date)
                .dayOfWeek(dayOfWeek)
                .slots(slots)
                .build();

        // Cache the result
        try {
            availabilityCacheManager.setAvailability(roomId, date, objectMapper.writeValueAsString(vo));
        } catch (Exception e) {
            log.warn("Failed to cache availability for roomId={}, date={}", roomId, date);
        }

        return vo;
    }

    private RoomVO toRoomVO(Room room) {
        Venue venue = venueMapper.selectById(room.getVenueId());
        List<Amenity> amenities = getAmenitiesForRoom(room.getId());
        List<String> images = roomImageMapper.selectList(
                        new LambdaQueryWrapper<RoomImage>()
                                .eq(RoomImage::getRoomId, room.getId())
                                .orderByAsc(RoomImage::getSortOrder))
                .stream().map(RoomImage::getUrl)
                .collect(Collectors.toList());

        List<AmenityVO> amenityVOs = amenities.stream()
                .map(a -> AmenityVO.builder().id(a.getId()).name(a.getName()).icon(a.getIcon()).build())
                .collect(Collectors.toList());

        return RoomVO.builder()
                .id(room.getId())
                .name(room.getName())
                .venueId(room.getVenueId())
                .venueName(venue != null ? venue.getName() : "")
                .capacity(room.getCapacity())
                .areaSqm(room.getAreaSqm())
                .floor(room.getFloor())
                .description(room.getDescription())
                .pricePerHour(room.getPricePerHour())
                .pricePerHalfDay(room.getPricePerHalfDay())
                .pricePerDay(room.getPricePerDay())
                .status(room.getStatus())
                .available("AVAILABLE".equals(room.getStatus()))
                .remainingSlots(0)
                .amenities(amenityVOs)
                .images(images)
                .coverImage(images.isEmpty() ? null : images.get(0))
                .build();
    }

    private List<Amenity> getAmenitiesForRoom(Long roomId) {
        List<RoomAmenity> ras = roomAmenityMapper.selectList(
                new LambdaQueryWrapper<RoomAmenity>().eq(RoomAmenity::getRoomId, roomId));
        if (ras.isEmpty()) return Collections.emptyList();
        return amenityMapper.selectBatchIds(
                ras.stream().map(RoomAmenity::getAmenityId).collect(Collectors.toList()));
    }
}
