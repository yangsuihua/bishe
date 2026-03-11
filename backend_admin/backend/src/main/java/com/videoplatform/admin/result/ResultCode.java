package com.videoplatform.admin.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAIL(400, "操作失败"),
    UNAUTHORIZED(401, "未授权,请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    
    USER_NOT_EXIST(1001, "用户不存在"),
    USER_DISABLED(1004, "用户已被禁用"),
    TOKEN_EXPIRED(1005, "登录已过期,请重新登录"),
    TOKEN_INVALID(1006, "无效的Token"),
    USERNAME_OR_PASSWORD_ERROR(1009, "用户名或密码错误"),
    
    VIDEO_NOT_EXIST(1101, "视频不存在"),
    
    SERVER_ERROR(500, "服务器内部错误");
    
    private final Integer code;
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
