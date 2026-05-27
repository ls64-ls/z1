package com.example.booking.controller;

import com.example.booking.common.Result;
import com.example.booking.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/usage")
    public Result<Map<String, Object>> usageReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long venueId) {
        return Result.success(reportService.getUsageReport(startDate, endDate, venueId));
    }

    @GetMapping("/revenue")
    public Result<Map<String, Object>> revenueReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long venueId) {
        return Result.success(reportService.getRevenueReport(startDate, endDate, venueId));
    }
}
