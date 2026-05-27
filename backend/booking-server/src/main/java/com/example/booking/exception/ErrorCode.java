package com.example.booking.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 1xxx: Parameter errors
    PARAM_INVALID(1001, "参数无效"),
    PARAM_MISSING(1002, "缺少必要参数"),

    // 2xxx: Auth errors
    UNAUTHORIZED(2001, "未登录或登录已过期"),
    FORBIDDEN(2002, "无权限访问"),
    WECHAT_AUTH_FAILED(2003, "微信登录失败"),

    // 3xxx: Business errors
    ROOM_UNAVAILABLE(3001, "该会议室暂不可用"),
    SLOT_CONFLICT(3002, "该时段已被预订"),
    BOOKING_CONFLICT(3002, "该时段已被预订"),
    BOOKING_BUSY(3003, "系统繁忙，请稍后重试"),
    NOT_FOUND(3004, "资源不存在"),
    CANNOT_CANCEL(3005, "该预订无法取消"),
    CANNOT_MODIFY(3006, "该预订无法修改"),
    CHECK_IN_FAILED(3007, "签到失败"),
    RECURRING_RULE_NOT_FOUND(3008, "重复规则不存在"),
    NO_PERMISSION(3009, "无权限操作"),
    STATUS_INVALID(3010, "状态不允许此操作"),
    ALREADY_CHECKED_IN(3011, "已签到，不可重复签到"),
    ALREADY_CHECKED_OUT(3012, "已签退，不可重复签退"),
    CHECK_IN_TOO_EARLY(3013, "签到时间未到"),
    CHECK_IN_TOO_LATE(3014, "已超过签到时间"),
    ROOM_NOT_AVAILABLE(3001, "该会议室暂不可用"),

    // 5xxx: Server errors
    SYSTEM_ERROR(5001, "系统内部错误"),
    DB_ERROR(5002, "数据库异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
