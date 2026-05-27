package com.example.booking.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.booking.entity.Booking;
import com.example.booking.enums.BookingStatus;
import com.example.booking.repository.mapper.BookingMapper;
import com.example.booking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Sends reminders for upcoming bookings.
 * Runs every 15 minutes to catch bookings starting within the next hour.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingReminderSender {

    private final BookingMapper bookingMapper;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 */15 * * * *")  // Every 15 minutes
    public void sendUpcomingReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime oneHourLater = now.plusHours(1);

        List<Booking> upcomingBookings = bookingMapper.selectList(
                new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getBookingDate, today)
                        .eq(Booking::getStatus, BookingStatus.CONFIRMED.name())
                        .ge(Booking::getStartTime, now)
                        .le(Booking::getStartTime, oneHourLater));

        log.info("Found {} upcoming bookings to remind", upcomingBookings.size());
        for (Booking booking : upcomingBookings) {
            try {
                notificationService.sendReminderNotice(booking);
            } catch (Exception e) {
                log.error("Failed to send reminder for booking {}: {}", booking.getId(), e.getMessage());
            }
        }
    }
}
