package com.example.booking.controller;

import com.example.booking.common.Result;
import com.example.booking.entity.Room;
import com.example.booking.service.admin.AdminRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/rooms")
@RequiredArgsConstructor
public class AdminRoomController {

    private final AdminRoomService adminRoomService;

    @PostMapping
    public Result<Room> create(@RequestBody Map<String, Object> body) {
        Room room = new Room();
        room.setName(getString(body, "name"));
        room.setVenueId(getLong(body, "venueId"));
        room.setCapacity(body.containsKey("capacity") ? ((Number) body.get("capacity")).intValue() : null);
        room.setFloor(body.containsKey("floor") ? ((Number) body.get("floor")).intValue() : null);
        room.setDescription(getString(body, "description"));
        room.setPricePerHour(body.containsKey("pricePerHour") ? new java.math.BigDecimal(body.get("pricePerHour").toString()) : null);
        @SuppressWarnings("unchecked")
        List<Long> amenityIds = (List<Long>) body.get("amenityIds");
        return Result.success(adminRoomService.createRoom(room, amenityIds));
    }

    @PutMapping("/{id}")
    public Result<Room> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Room room = new Room();
        room.setName(getString(body, "name"));
        room.setVenueId(getLong(body, "venueId"));
        room.setCapacity(body.containsKey("capacity") ? ((Number) body.get("capacity")).intValue() : null);
        room.setFloor(body.containsKey("floor") ? ((Number) body.get("floor")).intValue() : null);
        room.setDescription(getString(body, "description"));
        room.setPricePerHour(body.containsKey("pricePerHour") ? new java.math.BigDecimal(body.get("pricePerHour").toString()) : null);
        @SuppressWarnings("unchecked")
        List<Long> amenityIds = (List<Long>) body.get("amenityIds");
        return Result.success(adminRoomService.updateRoom(id, room, amenityIds));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        adminRoomService.deleteRoom(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminRoomService.updateStatus(id, body.get("status"));
        return Result.success();
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.longValue();
        return val != null ? Long.parseLong(val.toString()) : null;
    }
}
