package com.recrosoftware.utils.validation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Range {
    double from() default Double.NEGATIVE_INFINITY;

    double to() default Double.POSITIVE_INFINITY;
}
