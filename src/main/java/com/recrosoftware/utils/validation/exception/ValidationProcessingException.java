package com.recrosoftware.utils.validation.exception;

import com.recrosoftware.utils.validation.ValidationError;

import java.util.Collections;
import java.util.List;

public class ValidationProcessingException extends Exception {
    private final List<ValidationError> validationErrors;

    public ValidationProcessingException(List<ValidationError> validationErrors) {
        this.validationErrors = Collections.unmodifiableList(validationErrors);
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }
}
