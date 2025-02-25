package dev.kurtyoon.pretest.core.exception;

import dev.kurtyoon.pretest.core.exception.error.ErrorCode;

public class CommonException extends RuntimeException {

    private final ErrorCode errorCode;

    public CommonException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String getMessage() {
        return errorCode.getMessage();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
