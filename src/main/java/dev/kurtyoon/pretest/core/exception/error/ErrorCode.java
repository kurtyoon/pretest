package dev.kurtyoon.pretest.core.exception.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {


    // Invalid Argument Error
    INVALID_ARGUMENT(40000, HttpStatus.BAD_REQUEST, "요청에 유효하지 않은 인자입니다."),
    MISSING_REQUEST_PARAMETER(40000, HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다."),
    INVALID_PARAMETER_FORMAT(40000, HttpStatus.BAD_REQUEST, "요청 파라미터의 형식이 올바르지 않습니다."),
    MISSING_REQUEST_PART(40000, HttpStatus.BAD_REQUEST, "필수 요청 파트가 누락되었습니다."),
    INVALID_PART_FORMAT(40000, HttpStatus.BAD_REQUEST, "요청 파트의 형식이 올바르지 않습니다."),

    // Not Found Error
    NOT_FOUND_END_POINT(40400, HttpStatus.NOT_FOUND, "요청 엔드포인트가 존재하지 않습니다."),

    // Internal Server Error
    INTERNAL_SERVER_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 에러입니다."),

    ;

    private final Integer code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(
            Integer code,
            HttpStatus httpStatus,
            String message
    ) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
