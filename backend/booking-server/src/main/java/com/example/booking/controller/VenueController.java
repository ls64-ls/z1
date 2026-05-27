package com.example.booking.controller;

import com.example.booking.common.Result;
import com.example.booking.dto.response.VenueVO;
import com.example.booking.service.VenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    public Result<List<VenueVO>> list() {
        return Result.success(venueService.listActive());
    }
}
