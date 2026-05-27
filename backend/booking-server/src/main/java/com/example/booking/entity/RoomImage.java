package com.example.booking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("room_image")
public class RoomImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String url;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
