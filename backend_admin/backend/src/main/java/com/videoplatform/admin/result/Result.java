package com.videoplatform.admin.result;

import lombok.Data;
import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public Result(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
    }
    
    public Result(Integer code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功");
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    
    public static <T> Result<T> fail() {
        return new Result<>(400, "操作失败");
    }
    
    public static <T> Result<T> fail(String message) {
        return new Result<>(400, message);
    }
    
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message);
    }
    
    public boolean isSuccess() {
        return Integer.valueOf(200).equals(this.code);
    }
}
