package com.recrosoftware.utils.validation.exception;

public class ValidationInvalidFieldException extends RuntimeException {
    private final String field;

    public ValidationInvalidFieldException(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
