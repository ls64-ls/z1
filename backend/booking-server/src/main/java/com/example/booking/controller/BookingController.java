package com.example.booking.controller;

import com.example.booking.common.PageResult;
import com.example.booking.common.Result;
import com.example.booking.dto.request.CancelBookingRequest;
import com.example.booking.dto.request.CreateBookingRequest;
import com.example.booking.dto.request.PreCheckRequest;
import com.example.booking.dto.response.BookingVO;
import com.example.booking.dto.response.PreCheckResultVO;
import com.example.booking.security.UserContext;
import com.example.booking.service.BookingService;
import com.example.booking.service.RecurringBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final RecurringBookingService recurringBookingService;

    /**
     * Pre-check — lightweight conflict detection without creating a booking.
     */
    @PostMapping("/pre-check")
    public Result<PreCheckResultVO> preCheck(@Valid @RequestBody PreCheckRequest req) {
        return Result.success(bookingService.preCheck(
                req.getRoomId(), req.getBookingDate(), req.getStartTime(), req.getEndTime()));
    }

    /**
     * Create a booking. If recurring info is provided, creates the recurring rule
     * and generates the first batch of instances.
     */
    @PostMapping
    public Result<Map<String, Object>> create(@Valid @RequestBody CreateBookingRequest req) {
        Long userId = UserContext.getUserId();

        if (req.getRecurring() != null) {
            Map<String, Object> result = recurringBookingService.createRecurring(userId, req);
            return Result.success(result);
        }

        Map<String, Object> result = bookingService.create(
                userId, req.getRoomId(), req.getBookingDate(),
                req.getStartTime(), req.getEndTime(),
                req.getTitle(), req.getAttendeeCount(), req.getRemark());
        return Result.success(result);
    }

    /**
     * List current user's bookings with optional status filter.
     */
    @GetMapping
    public Result<PageResult<BookingVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        return Result.success(bookingService.getMyBookings(userId, status, page, size));
    }

    /**
     * Get booking detail.
     */
    @GetMapping("/{id}")
    public Result<BookingVO> detail(@PathVariable Long id) {
        BookingVO vo = bookingService.getDetail(id);
        if (vo == null) {
            return Result.error(3004, "预订不存在");
        }
        return Result.success(vo);
    }

    /**
     * Cancel a booking.
     */
    @PostMapping("/{id}/cancel")
    public Result<String> cancel(@PathVariable Long id, @RequestBody(required = false) CancelBookingRequest body) {
        Long userId = UserContext.getUserId();
        String reason = body != null ? body.getReason() : null;
        bookingService.cancel(id, userId, reason);
        return Result.success("已取消");
    }

    /**
     * Cancel an entire recurring sequence.
     */
    @DeleteMapping("/recurring/{ruleId}")
    public Result<String> cancelRecurring(@PathVariable Long ruleId) {
        Long userId = UserContext.getUserId();
        recurringBookingService.cancelRecurring(ruleId, userId);
        return Result.success("已取消整个重复序列");
    }
}
