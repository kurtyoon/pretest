package dev.kurtyoon.pretest.core.dto;

import dev.kurtyoon.pretest.core.exception.error.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashMap;
import java.util.Map;

public class ArgumentNotValidExceptionDto extends ExceptionDto {

    private final Map<String, String> errorFields;

    public ArgumentNotValidExceptionDto(MethodArgumentNotValidException exception) {
        super(ErrorCode.INVALID_ARGUMENT);

        this.errorFields = new HashMap<>();

        exception.getBindingResult()
                .getAllErrors().forEach(e -> {
                    this.errorFields.put(toSnakeCase(((FieldError) e).getField()), e.getDefaultMessage());
                });
    }

    public ArgumentNotValidExceptionDto(ConstraintViolationException exception) {
        super(ErrorCode.INVALID_ARGUMENT);

        this.errorFields = new HashMap<>();

        for (ConstraintViolation<?> constraintViolation : exception.getConstraintViolations()) {
            errorFields.put(toSnakeCase(constraintViolation.getPropertyPath().toString()), constraintViolation.getMessage());
        }
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }
}
