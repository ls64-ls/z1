package com.example.booking.service;

import com.example.booking.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final StringRedisTemplate redisTemplate;

    @Async
    public void sendBookingConfirmation(Booking booking) {
        log.info("Sending booking confirmation: bookingNo={}, userId={}", booking.getBookingNo(), booking.getUserId());

        Map<String, String> body = Map.of(
                "type", "booking_confirmed",
                "userId", String.valueOf(booking.getUserId()),
                "bookingNo", booking.getBookingNo(),
                "roomId", String.valueOf(booking.getRoomId()),
                "bookingDate", booking.getBookingDate() != null ? booking.getBookingDate().toString() : "",
                "startTime", booking.getStartTime() != null ? booking.getStartTime().toString() : "",
                "endTime", booking.getEndTime() != null ? booking.getEndTime().toString() : "",
                "title", booking.getTitle() != null ? booking.getTitle() : ""
        );

        try {
            MapRecord<String, String, String> record = StreamRecords.newRecord()
                    .in("stream:wechat:notifications")
                    .ofMap(body);
            redisTemplate.opsForStream().add(record);
        } catch (Exception e) {
            log.warn("Failed to send booking confirmation to stream: {}", e.getMessage());
        }
    }

    @Async
    public void sendCancellationNotice(Booking booking, String reason) {
        log.info("Sending cancellation notice: bookingNo={}, reason={}", booking.getBookingNo(), reason);

        Map<String, String> body = Map.of(
                "type", "booking_cancelled",
                "userId", String.valueOf(booking.getUserId()),
                "bookingNo", booking.getBookingNo(),
                "reason", reason != null ? reason : ""
        );

        try {
            MapRecord<String, String, String> record = StreamRecords.newRecord()
                    .in("stream:wechat:notifications")
                    .ofMap(body);
            redisTemplate.opsForStream().add(record);
        } catch (Exception e) {
            log.warn("Failed to send cancellation notice to stream: {}", e.getMessage());
        }
    }

    @Async
    public void sendReminderNotice(Booking booking) {
        log.info("Sending reminder: bookingNo={}, startTime={}", booking.getBookingNo(), booking.getStartTime());

        Map<String, String> body = Map.of(
                "type", "booking_reminder",
                "userId", String.valueOf(booking.getUserId()),
                "bookingNo", booking.getBookingNo(),
                "title", booking.getTitle() != null ? booking.getTitle() : "",
                "startTime", booking.getStartTime() != null ? booking.getStartTime().toString() : ""
        );

        try {
            MapRecord<String, String, String> record = StreamRecords.newRecord()
                    .in("stream:wechat:notifications")
                    .ofMap(body);
            redisTemplate.opsForStream().add(record);
        } catch (Exception e) {
            log.warn("Failed to send reminder notice to stream: {}", e.getMessage());
        }
    }
}
