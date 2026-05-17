package com.supermarket.common.core.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class R<T> implements Serializable {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    private R() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = ResultCode.SUCCESS.getMessage();
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = ok();
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(String message) {
        return fail(ResultCode.SYSTEM_ERROR.getCode(), message);
    }

    public static <T> R<T> fail(IResultCode rc) {
        return fail(rc.getCode(), rc.getMessage());
    }

    public static <T> R<T> fail(IResultCode rc, String message) {
        return fail(rc.getCode(), message);
    }

    public boolean isSuccess() {
        return this.code == 0;
    }

    public boolean isFail() {
        return !isSuccess();
    }
}
