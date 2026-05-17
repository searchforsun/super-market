package com.supermarket.common.core.exception;

import com.supermarket.common.core.result.IResultCode;
import com.supermarket.common.core.result.ResultCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final IResultCode resultCode;

    public BizException(IResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BizException(IResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public BizException(IResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.resultCode = resultCode;
    }

    /** @deprecated use {@link #BizException(IResultCode)} */
    @Deprecated
    public BizException(int code, String message) {
        super(message);
        this.resultCode = new IResultCode() {
            @Override public int getCode() { return code; }
            @Override public String getMessage() { return message; }
            @Override public com.supermarket.common.core.result.ErrorType getErrorType() { return com.supermarket.common.core.result.ErrorType.BUSINESS_ERROR; }
        };
    }

    /** @deprecated use {@link #BizException(IResultCode)} */
    @Deprecated
    public BizException(String message) {
        this(ResultCode.SYSTEM_ERROR, message);
    }

    public int getCode() {
        return resultCode.getCode();
    }
}
