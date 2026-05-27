package com.example.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PreCheckRequest {
    @NotNull(message = "会议室ID不能为空")
    private Long roomId;

    @NotBlank(message = "预订日期不能为空")
    private String bookingDate;

    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;
}
