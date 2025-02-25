package dev.kurtyoon.pretest.core.annotation;

import dev.kurtyoon.pretest.core.validator.DateTimeValueValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateTimeValueValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface DateTimeValue {

    String message() default "잘못된 데이터 형식입니다. (yyyy-MM-dd HH:mm:ss)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
