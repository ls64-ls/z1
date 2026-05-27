package com.example.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateBookingRequest {
    @NotNull(message = "会议室ID不能为空")
    private Long roomId;

    @NotBlank(message = "预订日期不能为空")
    private String bookingDate;

    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    @NotBlank(message = "会议主题不能为空")
    private String title;

    private Integer attendeeCount;
    private String remark;

    private RecurringInfo recurring;

    @Data
    public static class RecurringInfo {
        @NotBlank(message = "重复类型不能为空")
        private String type;            // DAILY, WEEKLY, BIWEEKLY, MONTHLY

        private int[] daysOfWeek;       // [1,3,5] = 周一三五
        private String endDate;
    }
}
