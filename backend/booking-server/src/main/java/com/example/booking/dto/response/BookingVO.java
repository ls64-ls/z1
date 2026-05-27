package com.example.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingVO {
    private Long id;
    private String bookingNo;
    private Long roomId;
    private String roomName;
    private String venueName;
    private Long userId;
    private String bookingDate;
    private String startTime;
    private String endTime;
    private String title;
    private Integer attendeeCount;
    private String status;
    private String remark;
    private BigDecimal totalAmount;
    private Long recurringRuleId;
    private String createdAt;
}
