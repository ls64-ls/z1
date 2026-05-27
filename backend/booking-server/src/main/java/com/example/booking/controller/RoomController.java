package com.example.booking.controller;

import com.example.booking.common.PageResult;
import com.example.booking.common.Result;
import com.example.booking.dto.response.RoomVO;
import com.example.booking.dto.response.AvailabilityVO;
import com.example.booking.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /**
     * Default list/search endpoint — used by miniapp home page.
     * Maps to GET /api/v1/rooms?venueId=&capacity=&amenityIds=&page=&size=
     */
    @GetMapping
    public Result<PageResult<RoomVO>> list(
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) List<Long> amenityIds,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(roomService.searchRooms(venueId, capacity, amenityIds, page, size));
    }

    @GetMapping("/search")
    public Result<PageResult<RoomVO>> search(
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) List<Long> amenityIds,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(roomService.searchRooms(venueId, capacity, amenityIds, page, size));
    }

    @GetMapping("/{id}")
    public Result<RoomVO> detail(@PathVariable Long id) {
        RoomVO vo = roomService.getDetail(id);
        if (vo == null) {
            return Result.error(3004, "会议室不存在");
        }
        return Result.success(vo);
    }

    @GetMapping("/{id}/availability")
    public Result<AvailabilityVO> availability(@PathVariable Long id, @RequestParam String date) {
        return Result.success(roomService.getAvailability(id, date));
    }
}
