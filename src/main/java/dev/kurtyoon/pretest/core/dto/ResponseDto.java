package dev.kurtyoon.pretest.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.kurtyoon.pretest.core.exception.CommonException;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;
import jakarta.annotation.Nullable;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

public class ResponseDto<T> {

    @JsonIgnore
    private final HttpStatus httpStatus;

    @NotNull
    private final Boolean success;

    @Nullable
    private final T data;

    @Nullable
    private final ExceptionDto error;

    public ResponseDto(
            HttpStatus httpStatus,
            Boolean success,
            @Nullable T data,
            @Nullable ExceptionDto error
    ) {
        this.httpStatus = httpStatus;
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ResponseDto<T> created(@Nullable final T data) {
        return new ResponseDto<>(HttpStatus.CREATED, true, data, null);
    }

    public static ResponseDto<Object> fail(final ConstraintViolationException e) {
        return new ResponseDto<>(
                HttpStatus.BAD_REQUEST,
                false,
                null,
                new ArgumentNotValidExceptionDto(e)
        );
    }

    public static ResponseDto<Object> fail(final MethodArgumentNotValidException e) {
        return new ResponseDto<>(
                HttpStatus.BAD_REQUEST,
                false,
                null,
                new ArgumentNotValidExceptionDto(e)
        );
    }

    public static ResponseDto<Object> fail(final MissingServletRequestParameterException e) {
        return new ResponseDto<>(
                HttpStatus.BAD_REQUEST,
                false,
                null,
                ExceptionDto.of(ErrorCode.MISSING_REQUEST_PARAMETER)
        );
    }

    public static ResponseDto<Object> fail(final MissingServletRequestPartException e) {
        return new ResponseDto<>(
                HttpStatus.BAD_REQUEST,
                false,
                null,
                ExceptionDto.of(ErrorCode.MISSING_REQUEST_PARAMETER)
        );
    }

    public static ResponseDto<Object> fail(final MethodArgumentTypeMismatchException e) {
        return new ResponseDto<>(
                HttpStatus.BAD_REQUEST,
                false,
                null,
                ExceptionDto.of(ErrorCode.INVALID_PARAMETER_FORMAT)
        );
    }

    public static ResponseDto<Object> fail(final CommonException e) {
        return new ResponseDto<>(
                e.getErrorCode().getHttpStatus(),
                false,
                null,
                ExceptionDto.of(e.getErrorCode())
        );
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
