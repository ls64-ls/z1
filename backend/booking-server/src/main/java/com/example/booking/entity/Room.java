package com.example.booking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@TableName("room")
public class Room {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long venueId;
    private String name;
    private String description;
    private Integer capacity;
    private BigDecimal areaSqm;
    private Integer floor;
    private BigDecimal pricePerHour;
    @TableField("price_per_halfday")
    private BigDecimal pricePerHalfDay;
    private BigDecimal pricePerDay;
    private String status;
    private Integer sortOrder;
    @Version
    private Integer version;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
