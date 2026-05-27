package com.example.booking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("booking_check_in")
public class BookingCheckIn {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bookingId;
    private OffsetDateTime checkInTime;
    private OffsetDateTime checkOutTime;
    private Long operatorId;
    private String remark;
}
