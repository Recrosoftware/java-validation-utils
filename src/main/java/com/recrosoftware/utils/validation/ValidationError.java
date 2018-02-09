package com.recrosoftware.utils.validation;

public class ValidationError {
    private final String field;
    private final String reason;

    public ValidationError(String field, String reason) {
        this.field = field;
        this.reason = reason;
    }

    public String getField() {
        return field;
    }

    public String getReason() {
        return reason;
    }
}
