package com.example.booking.service;

import com.example.booking.dto.request.CreateBookingRequest;
import com.example.booking.entity.Booking;
import com.example.booking.entity.RecurringRule;
import com.example.booking.enums.RecurrenceType;
import com.example.booking.exception.BusinessException;
import com.example.booking.exception.ErrorCode;
import com.example.booking.repository.mapper.RecurringRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringBookingService {

    private final RecurringRuleMapper recurringRuleMapper;
    private final BookingService bookingService;

    @Transactional
    public Map<String, Object> createRecurring(Long userId, CreateBookingRequest req) {
        CreateBookingRequest.RecurringInfo ri = req.getRecurring();
        LocalDate startDate = LocalDate.parse(req.getBookingDate());
        LocalDate endDate = ri.getEndDate() != null ? LocalDate.parse(ri.getEndDate()) : startDate.plusMonths(3);

        // Create the recurring rule
        RecurringRule rule = new RecurringRule();
        rule.setUserId(userId);
        rule.setRoomId(req.getRoomId());
        rule.setTitle(req.getTitle());
        rule.setAttendeeCount(req.getAttendeeCount());
        rule.setStartDate(startDate);
        rule.setEndDate(endDate);
        rule.setStartTime(LocalTime.parse(req.getStartTime()));
        rule.setEndTime(LocalTime.parse(req.getEndTime()));
        rule.setRepeatType(ri.getType());
        rule.setRepeatDays(ri.getDaysOfWeek());
        rule.setStatus("ACTIVE");
        rule.setRemark(req.getRemark());
        recurringRuleMapper.insert(rule);

        // Generate instances for the next 60 days
        List<Map<String, Object>> created = generateInstances(userId, rule, 60);

        Map<String, Object> result = new HashMap<>();
        result.put("recurringRuleId", rule.getId());
        result.put("rule", rule);
        result.put("generatedCount", created.size());
        result.put("bookings", created);
        return result;
    }

    @Transactional
    public void cancelRecurring(Long ruleId, Long userId) {
        RecurringRule rule = recurringRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BusinessException(ErrorCode.RECURRING_RULE_NOT_FOUND);
        }
        if (!rule.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }
        rule.setStatus("ENDED");
        recurringRuleMapper.updateById(rule);
    }

    /**
     * Generate future booking instances from a recurring rule.
     * Called both at creation time and periodically by the scheduler.
     */
    public List<Map<String, Object>> generateInstances(Long userId, RecurringRule rule, int maxDays) {
        RecurrenceType type = RecurrenceType.valueOf(rule.getRepeatType());
        int[] daysOfWeek = rule.getRepeatDays() instanceof int[] ? (int[]) rule.getRepeatDays()
                : convertToIntArray(rule.getRepeatDays());

        LocalDate cursor = rule.getLastGeneratedDate() != null
                ? rule.getLastGeneratedDate().plusDays(1)
                : rule.getStartDate();
        LocalDate limit = rule.getEndDate() != null && rule.getEndDate().isBefore(cursor.plusDays(maxDays))
                ? rule.getEndDate()
                : cursor.plusDays(maxDays);

        List<Map<String, Object>> created = new ArrayList<>();
        Set<LocalDate> validDates = computeValidDates(cursor, limit, type, daysOfWeek);

        for (LocalDate date : validDates) {
            try {
                Map<String, Object> result = bookingService.create(
                        userId, rule.getRoomId(),
                        date.toString(),
                        rule.getStartTime().toString(),
                        rule.getEndTime().toString(),
                        rule.getTitle(), rule.getAttendeeCount(), rule.getRemark());
                created.add(result);
            } catch (BusinessException e) {
                if (e.getCode() == ErrorCode.BOOKING_CONFLICT.getCode()
                        || e.getCode() == ErrorCode.SLOT_CONFLICT.getCode()) {
                    log.info("Skipping conflict date {} for recurring rule {}", date, rule.getId());
                } else {
                    throw e;
                }
            }
        }

        // Update the rule's last generated date
        if (!created.isEmpty() || !validDates.isEmpty()) {
            LocalDate lastDate = validDates.isEmpty() ? cursor : validDates.stream().max(LocalDate::compareTo).orElse(cursor);
            rule.setLastGeneratedDate(lastDate);
            recurringRuleMapper.updateById(rule);
        }

        return created;
    }

    private Set<LocalDate> computeValidDates(LocalDate from, LocalDate to, RecurrenceType type, int[] daysOfWeek) {
        Set<LocalDate> dates = new TreeSet<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            boolean match;
            switch (type) {
                case DAILY:
                    match = true;
                    break;
                case WEEKLY:
                case BIWEEKLY:
                    match = isDayInArray(cursor.getDayOfWeek().getValue(), daysOfWeek);
                    if (type == RecurrenceType.BIWEEKLY) {
                        long weekNum = cursor.toEpochDay() / 7;
                        match = match && (weekNum % 2 == 0);
                    }
                    break;
                case MONTHLY:
                    int dayOfMonth = from.getDayOfMonth();
                    match = cursor.getDayOfMonth() == dayOfMonth;
                    break;
                default:
                    match = false;
            }
            if (match) {
                dates.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    private boolean isDayInArray(int day, int[] array) {
        if (array == null) return false;
        for (int d : array) {
            if (d == day) return true;
        }
        return false;
    }

    private int[] convertToIntArray(Object obj) {
        if (obj instanceof List<?> list) {
            return list.stream().mapToInt(o -> ((Number) o).intValue()).toArray();
        }
        if (obj instanceof int[] arr) return arr;
        if (obj instanceof Object[] arr) {
            return Arrays.stream(arr).mapToInt(o -> ((Number) o).intValue()).toArray();
        }
        return new int[0];
    }
}
