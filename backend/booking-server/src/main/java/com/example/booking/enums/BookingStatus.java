package com.example.booking.enums;

import lombok.Getter;

@Getter
public enum BookingStatus {
    PENDING("待支付"),
    CONFIRMED("已确认"),
    CHECKED_IN("已签到"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    EXPIRED("已过期");

    private final String description;

    BookingStatus(String description) {
        this.description = description;
    }

    public boolean isCancellable() {
        return this == CONFIRMED;
    }

    public boolean isActive() {
        return this != CANCELLED && this != EXPIRED;
    }
}
