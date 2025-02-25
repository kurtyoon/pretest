package dev.kurtyoon.pretest.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;

public class ExceptionDto {

    @JsonProperty("code")
    private final Integer code;

    @JsonProperty("message")
    private final String message;

    public ExceptionDto(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public ExceptionDto(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ExceptionDto of(ErrorCode errorCode) {
        return new ExceptionDto(errorCode.getCode(), errorCode.getMessage());
    }
}
