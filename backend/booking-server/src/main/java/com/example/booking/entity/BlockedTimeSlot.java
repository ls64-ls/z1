package com.example.booking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalTime;

@Data
@TableName("blocked_time_slot")
public class BlockedTimeSlot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long venueId;
    private String title;
    private String reason;
    @TableField(typeHandler = com.example.booking.config.TsRangeTypeHandler.class)
    private Object timeSlot;
    private LocalDate blockedDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
