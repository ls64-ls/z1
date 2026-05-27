package com.example.booking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.booking.entity.Booking;
import com.example.booking.repository.mapper.BookingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final BookingMapper bookingMapper;

    public Map<String, Object> getUsageReport(String startDate, String endDate, Long venueId) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        List<Booking> bookings = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .between(Booking::getBookingDate, start, end)
                .notIn(Booking::getStatus, "CANCELLED"));

        Map<String, Object> report = new HashMap<>();
        report.put("totalBookings", bookings.size());
        report.put("startDate", start.toString());
        report.put("endDate", end.toString());

        // Daily breakdown
        Map<LocalDate, Long> daily = bookings.stream()
                .collect(Collectors.groupingBy(Booking::getBookingDate, Collectors.counting()));
        report.put("dailyBreakdown", daily);

        return report;
    }

    public Map<String, Object> getRevenueReport(String startDate, String endDate, Long venueId) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        List<Booking> bookings = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .between(Booking::getBookingDate, start, end)
                .notIn(Booking::getStatus, "CANCELLED"));

        // Simplified: assume 1 hour per booking for revenue calculation
        long totalBookings = bookings.size();
        long totalHours = 0;
        for (Booking b : bookings) {
            if (b.getStartTime() != null && b.getEndTime() != null) {
                totalHours += java.time.Duration.between(b.getStartTime(), b.getEndTime()).toHours();
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("totalBookings", totalBookings);
        report.put("totalHours", totalHours);
        report.put("startDate", start.toString());
        report.put("endDate", end.toString());
        return report;
    }
}
