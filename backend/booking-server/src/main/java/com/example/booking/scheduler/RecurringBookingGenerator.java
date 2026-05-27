package com.example.booking.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.booking.entity.RecurringRule;
import com.example.booking.repository.mapper.RecurringRuleMapper;
import com.example.booking.service.RecurringBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled job: generates upcoming booking instances for active recurring rules.
 * Runs daily to maintain a ~60 day buffer of future bookings.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringBookingGenerator {

    private final RecurringRuleMapper recurringRuleMapper;
    private final RecurringBookingService recurringBookingService;

    @Scheduled(cron = "0 30 2 * * *")  // 2:30 AM daily
    public void generateUpcomingBookings() {
        log.info("Starting recurring booking generation...");

        List<RecurringRule> activeRules = recurringRuleMapper.selectList(
                new LambdaQueryWrapper<RecurringRule>()
                        .eq(RecurringRule::getStatus, "ACTIVE"));

        int totalGenerated = 0;
        for (RecurringRule rule : activeRules) {
            try {
                var created = recurringBookingService.generateInstances(
                        rule.getUserId(), rule, 60);
                totalGenerated += created.size();
            } catch (Exception e) {
                log.error("Failed to generate bookings for recurring rule {}: {}", rule.getId(), e.getMessage());
            }
        }

        log.info("Recurring booking generation complete. Generated {} bookings across {} active rules.",
                totalGenerated, activeRules.size());
    }
}
