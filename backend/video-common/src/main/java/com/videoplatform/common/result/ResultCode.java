package com.videoplatform.common.result;

import lombok.Getter;

/**
 * 统一状态码定义
 */
@Getter
public enum ResultCode {
    
    // 成功状态码
    SUCCESS(200, "操作成功"),
    
    // 客户端错误 4xx
    FAIL(400, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权,请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    
    // 业务错误码 1xxx
    USER_NOT_EXIST(1001, "用户不存在"),
    USER_EXIST(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    USER_DISABLED(1004, "用户已被禁用"),
    TOKEN_EXPIRED(1005, "登录已过期,请重新登录"),
    TOKEN_INVALID(1006, "无效的Token"),
    USERNAME_EXISTS(1007, "用户名已存在"),
    EMAIL_EXISTS(1008, "邮箱已存在"),
    USERNAME_OR_PASSWORD_ERROR(1009, "用户名或密码错误"),
    CANNOT_FOLLOW_SELF(1010, "不能关注自己"),
    ALREADY_FOLLOWED(1011, "已经关注该用户"),
    NOT_FOLLOWED(1012, "未关注该用户"),
    
    VIDEO_NOT_EXIST(1101, "视频不存在"),
    VIDEO_AUDITING(1102, "视频审核中"),
    VIDEO_AUDIT_FAILED(1103, "视频审核失败"),
    VIDEO_DELETED(1104, "视频已删除"),
    
    COMMENT_NOT_EXIST(1201, "评论不存在"),
    COMMENT_DELETED(1202, "评论已删除"),
    
    UPLOAD_FILE_ERROR(1301, "文件上传失败"),
    FILE_SIZE_EXCEED(1302, "文件大小超出限制"),
    FILE_TYPE_ERROR(1303, "文件类型不支持"),
    
    // 服务端错误 5xx
    SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),
    
    // 限流熔断
    RATE_LIMIT(429, "请求过于频繁,请稍后再试"),
    DEGRADED(503, "服务降级中");
    
    private final Integer code;
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}