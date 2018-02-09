package com.recrosoftware.utils.validation;

import java.util.List;

public interface Validable {
    default List<ValidationError> validate(String prefix) {
        return null;
    }
}
