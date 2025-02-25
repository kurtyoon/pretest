package dev.kurtyoon.pretest.core.validator;

import dev.kurtyoon.pretest.core.annotation.DateTimeValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateTimeValueValidator implements ConstraintValidator<DateTimeValue, String> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void initialize(DateTimeValue constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        try {
            LocalDateTime.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return false;
        }

        return true;
    }
}
