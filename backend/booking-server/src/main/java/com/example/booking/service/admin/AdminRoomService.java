package com.example.booking.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.booking.common.PageResult;
import com.example.booking.entity.*;
import com.example.booking.repository.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRoomService {

    private final RoomMapper roomMapper;
    private final RoomImageMapper roomImageMapper;
    private final RoomAmenityMapper roomAmenityMapper;
    private final AmenityMapper amenityMapper;

    public Room createRoom(Room room, List<Long> amenityIds) {
        room.setStatus("AVAILABLE");
        roomMapper.insert(room);
        if (amenityIds != null) {
            for (Long aid : amenityIds) {
                RoomAmenity ra = new RoomAmenity();
                ra.setRoomId(room.getId());
                ra.setAmenityId(aid);
                roomAmenityMapper.insert(ra);
            }
        }
        return room;
    }

    @Transactional
    public Room updateRoom(Long id, Room room, List<Long> amenityIds) {
        room.setId(id);
        roomMapper.updateById(room);
        if (amenityIds != null) {
            roomAmenityMapper.delete(new LambdaQueryWrapper<RoomAmenity>().eq(RoomAmenity::getRoomId, id));
            for (Long aid : amenityIds) {
                RoomAmenity ra = new RoomAmenity();
                ra.setRoomId(id);
                ra.setAmenityId(aid);
                roomAmenityMapper.insert(ra);
            }
        }
        return roomMapper.selectById(id);
    }

    public void deleteRoom(Long id) {
        roomMapper.deleteById(id);
    }

    public void updateStatus(Long id, String status) {
        Room room = roomMapper.selectById(id);
        if (room != null) {
            room.setStatus(status);
            roomMapper.updateById(room);
        }
    }
}
