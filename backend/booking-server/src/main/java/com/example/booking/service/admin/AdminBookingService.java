package com.example.booking.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.booking.common.PageResult;
import com.example.booking.entity.Booking;
import com.example.booking.entity.Room;
import com.example.booking.entity.Venue;
import com.example.booking.repository.mapper.BookingMapper;
import com.example.booking.repository.mapper.RoomMapper;
import com.example.booking.repository.mapper.VenueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final BookingMapper bookingMapper;
    private final RoomMapper roomMapper;
    private final VenueMapper venueMapper;

    public PageResult<Map<String, Object>> list(String status, Long venueId, String keyword, int page, int size) {
        LambdaQueryWrapper<Booking> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq(Booking::getStatus, status);
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(Booking::getTitle, keyword).or().like(Booking::getBookingNo, keyword));
        }
        qw.orderByDesc(Booking::getBookingDate, Booking::getStartTime);

        Page<Booking> pg = bookingMapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> records = new ArrayList<>();
        for (Booking b : pg.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("bookingNo", b.getBookingNo());
            map.put("userId", b.getUserId());
            map.put("title", b.getTitle());
            map.put("bookingDate", b.getBookingDate() != null ? b.getBookingDate().toString() : null);
            map.put("startTime", b.getStartTime() != null ? b.getStartTime().toString() : null);
            map.put("endTime", b.getEndTime() != null ? b.getEndTime().toString() : null);
            map.put("status", b.getStatus());
            map.put("createTime", b.getCreatedAt() != null ? b.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);

            Room room = roomMapper.selectById(b.getRoomId());
            if (room != null) {
                map.put("roomName", room.getName());
                Venue venue = venueMapper.selectById(room.getVenueId());
                map.put("venueName", venue != null ? venue.getName() : "");
            }
            records.add(map);
        }
        return new PageResult<>(page, size, pg.getTotal(), records);
    }

    public void updateStatus(Long id, String status) {
        Booking booking = bookingMapper.selectById(id);
        if (booking != null) {
            booking.setStatus(status);
            bookingMapper.updateById(booking);
        }
    }
}
