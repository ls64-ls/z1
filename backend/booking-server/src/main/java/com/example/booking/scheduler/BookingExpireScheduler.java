package com.example.booking.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.booking.entity.Booking;
import com.example.booking.enums.BookingStatus;
import com.example.booking.repository.mapper.BookingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpireScheduler {

    private final BookingMapper bookingMapper;

    @Scheduled(cron = "0 * * * * *")
    public void expirePastBookings() {
        LambdaUpdateWrapper<Booking> uw = new LambdaUpdateWrapper<>();
        uw.set(Booking::getStatus, BookingStatus.EXPIRED.name())
          .eq(Booking::getStatus, BookingStatus.CONFIRMED.name())
          .lt(Booking::getBookingDate, LocalDate.now());
        bookingMapper.update(null, uw);
    }
}
