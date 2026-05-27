package com.example.booking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalTime;

@Data
@TableName("booking")
public class Booking {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bookingNo;
    private Long userId;
    private Long roomId;
    @TableField(typeHandler = com.example.booking.config.TsRangeTypeHandler.class)
    private Object timeSlot;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String title;
    private String purpose;
    private Integer attendeeCount;
    private String status;
    private String remark;
    @Version
    private Integer version;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
