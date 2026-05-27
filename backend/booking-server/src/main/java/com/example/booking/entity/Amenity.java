package com.example.booking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("amenity")
public class Amenity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String icon;
    private String category;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
