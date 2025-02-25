package dev.kurtyoon.pretest.core.exception.handler;

import dev.kurtyoon.pretest.common.logging.LoggerUtils;
import dev.kurtyoon.pretest.core.dto.ResponseDto;
import dev.kurtyoon.pretest.core.exception.CommonException;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.UnexpectedTypeException;
import org.slf4j.Logger;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 전역 예외 처리를 담당하는 클래스
 * - @RestControllerAdvice: 모든 컨트롤러에서 발생하는 예외를 처리할 수 있도록 설정
 * - @ExceptionHandler: 특정 예외 발생 시 실행할 핸들러 메소드 정의
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerUtils.getLogger(GlobalExceptionHandler.class);

    /**
     * Multipart File 이 누락되었을 때 발생하는 예외 처리
     * - 예) Multipart 요청에서 `@RequestPart`로 파일을 기대했지만 포함되지 않은 경우
     */
    @ExceptionHandler(value = {MissingServletRequestPartException.class})
    public ResponseDto<?> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        log.error("Exception Handler Catch MissingServletRequestPartException: {}", e.getMessage());
        return ResponseDto.fail(new CommonException(ErrorCode.MISSING_REQUEST_PART));
    }

    /**
     * Multipart 요청이 잘못되었을 때 발생하는 예외 처리
     * - 예) 파일 크기 제한 초과, 잘못된 형식의 파일 업로드 등
     */
    @ExceptionHandler(value = {MultipartException.class})
    public ResponseDto<?> handleMultipartException(MultipartException e) {
        log.error("Exception Handler Catch MultipartException : {}", e.getMessage());
        return ResponseDto.fail(new CommonException(ErrorCode.INVALID_PART_FORMAT));
    }

    /**
     * JSON 요청 바디를 파싱할 수 없을 때 발생하는 예외 처리
     * - 예) 요청 본문이 비어 있거나 JSON 형식이 잘못된 경우
     */
    @ExceptionHandler(value = {HttpMessageNotReadableException.class})
    public ResponseDto<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("Exception Handler Catch HttpMessageNotReadableException : {}", e.getMessage());
        return ResponseDto.fail(new CommonException(ErrorCode.INVALID_ARGUMENT));
    }

    /**
     * 지원되지 않는 HTTP 메서드로 요청이 들어왔을 때 발생하는 예외 처리
     * - 예) POST 만 허용하는 엔드포인트에 GET 요청을 보낸 경우
     */
    @ExceptionHandler(value = {HttpRequestMethodNotSupportedException.class})
    public ResponseDto<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("ExceptionHandler catch HttpRequestMethodNotSupportedException: {}", e.getMessage());
        return ResponseDto.fail(new CommonException(ErrorCode.NOT_FOUND_END_POINT));
    }

    /**
     * 요청 데이터의 검증 실패 예외 처리 (DTO 유효성 검사)
     * - 예) @Valid 또는 @Validated 를 사용한 필드 검증 실패 시 발생
     */
    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public ResponseDto<?> handleArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("ExceptionHandler catch MethodArgumentNotValidException: {}", e.getMessage());
        return ResponseDto.fail(e);
    }

    /**
     * Bean Validation 에서 검증 실패 시 발생하는 예외 처리
     * - 예) @NotNull, @Size 등의 제약 조건을 위반한 경우
     */
    @ExceptionHandler(value = {ConstraintViolationException.class})
    public ResponseDto<?> handleConstraintViolationException(ConstraintViolationException e) {
        log.error("ExceptionHandler catch ConstraintViolationException: {}", e.getMessage());
        return ResponseDto.fail(e);
    }

    /**
     * 요청 파라미터 타입 불일치 예외 처리
     * - 예) Long 타입을 기대하는데 문자열을 보낸 경우
     */
    @ExceptionHandler(value = {MethodArgumentTypeMismatchException.class})
    public ResponseDto<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("ExceptionHandler catch MethodArgumentTypeMismatchException: {}", e.getMessage());
        return ResponseDto.fail(e);
    }

    /**
     * 필수 요청 파라미터가 누락되었을 때 발생하는 예외 처리
     * - 예) @RequestParam 에서 필수 값이 전달되지 않은 경우
     */
    @ExceptionHandler(value = {MissingServletRequestParameterException.class})
    public ResponseDto<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("ExceptionHandler catch MissingServletRequestParameterException: {}", e.getMessage());
        return ResponseDto.fail(e);
    }

    /**
     * 정의되지 않은 예외 발생 시 처리 (서버 내부 오류)
     * - 예) NullPointerException, IllegalArgumentException 등
     */
    @ExceptionHandler(value = {Exception.class})
    public ResponseDto<?> handleException(Exception e) {
        log.error("Exception Handler Catch Exception : {}", e.getMessage());
        return ResponseDto.fail(new CommonException(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * 개발자가 정의한 Custom Exception (`CommonException`) 처리
     * - 예) 비즈니스 로직에서 특정 에러 발생 시
     */
    @ExceptionHandler(value = {CommonException.class})
    public ResponseDto<?> handleApiException(CommonException e) {
        log.error("Exception Handler Catch CommonException : {}", e.getMessage());
        return ResponseDto.fail(e);
    }
}
