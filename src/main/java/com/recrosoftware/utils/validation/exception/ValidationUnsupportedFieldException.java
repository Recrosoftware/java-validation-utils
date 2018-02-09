package com.recrosoftware.utils.validation.exception;

import java.lang.annotation.Annotation;

public class ValidationUnsupportedFieldException extends RuntimeException {
    private final String field;
    private final Class<? extends Annotation> validatorType;

    public ValidationUnsupportedFieldException(String field, Class<? extends Annotation> validatorType) {
        this.field = field;
        this.validatorType = validatorType;
    }

    public String getField() {
        return field;
    }

    public Class<? extends Annotation> getValidatorType() {
        return validatorType;
    }
}
