package com.example.booking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("room_amenity")
public class RoomAmenity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long amenityId;
}
