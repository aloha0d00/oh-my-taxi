package com.carpool.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 通用错误
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或认证已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "不支持的请求方法"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户相关 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PHONE_EXISTS(1002, "该手机号已被注册"),
    USER_STUDENT_ID_EXISTS(1003, "该学号已被注册"),
    USER_PASSWORD_ERROR(1004, "密码错误"),
    USER_CANNOT_DELETE_SELF(1005, "不能删除自己的账户"),

    // 行程相关 2xxx
    TRIP_NOT_FOUND(2001, "行程不存在"),
    TRIP_CLOSED(2002, "该行程已关闭"),
    TRIP_SEATS_INSUFFICIENT(2003, "座位不足"),
    TRIP_ALREADY_JOINED(2004, "您已经申请过该行程"),
    TRIP_ONLY_DRIVER_CAN_MODIFY(2005, "只有行程司机可以执行此操作"),

    // 车辆相关 3xxx
    VEHICLE_NOT_FOUND(3001, "车辆不存在"),
    VEHICLE_PLATE_EXISTS(3002, "该车牌号已被使用"),
    VEHICLE_NOT_OWNER(3003, "只能操作自己的车辆"),

    // 拼车申请相关 4xxx
    APPLICATION_NOT_FOUND(4001, "申请不存在"),
    APPLICATION_ALREADY_HANDLED(4002, "该申请已处理"),
    APPLICATION_CANNOT_CANCEL(4003, "该申请已完成或已取消"),
    APPLICATION_STATUS_INVALID(4004, "当前状态不可操作"),

    // 评分相关 5xxx
    RATING_ALREADY_EXISTS(5001, "您已评价过该行程"),
    RATING_SCORE_INVALID(5002, "评分必须在1-5之间"),
    RATING_TRIP_NOT_COMPLETED(5003, "只能评价已完成的行程"),
    RATING_NOT_PARTICIPANT(5004, "您未参与该行程，无法评价");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
