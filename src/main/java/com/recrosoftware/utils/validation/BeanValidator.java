package com.recrosoftware.utils.validation;

import com.recrosoftware.utils.validation.annotation.Range;
import com.recrosoftware.utils.validation.annotation.Required;
import com.recrosoftware.utils.validation.annotation.Validate;
import com.recrosoftware.utils.validation.exception.ValidationInvalidFieldException;
import com.recrosoftware.utils.validation.exception.ValidationProcessingException;
import com.recrosoftware.utils.validation.exception.ValidationUnsupportedFieldException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

public class BeanValidator {
    public static void validate(Validable bean) throws IllegalAccessException, ValidationProcessingException {
        final int MAX_VALIDATION_DEPTH = 256;

        List<ValidationError> validationErrors = new BeanValidator(bean, MAX_VALIDATION_DEPTH)
                .compute("");

        if (validationErrors != null && !validationErrors.isEmpty()) {
            throw new ValidationProcessingException(validationErrors);
        }
    }

    private static final List<Class<? extends Annotation>> VALIDATION_TOOLKIT_ANNOTATIONS;

    static {
        VALIDATION_TOOLKIT_ANNOTATIONS = Collections.unmodifiableList(new ArrayList<Class<? extends Annotation>>() {{
            add(Range.class);
            add(Required.class);
            add(Validate.class);
        }});
    }

    private Validable bean;
    private Map<String, Field> beanFields;

    private List<ValidationError> validationErrors;
    private Map<String, Map<Class<?>, Boolean>> validationSummary;

    private String fieldPrefix;

    private final int currentDepth;

    private BeanValidator(Validable bean, int currentDepth) {
        this.currentDepth = currentDepth;

        this.bean = bean;
        this.validationErrors = new ArrayList<>();
        this.validationSummary = new HashMap<>();
    }

    private List<ValidationError> compute(String fieldPrefix) throws IllegalAccessException {
        if (currentDepth <= 0) {
            validationErrors.add(new ValidationError(fieldPrefix, "Reached max validation depth."));
            return validationErrors;
        }

        if (this.bean == null) {
            return null;
        }

        List<ValidationError> beanValidationErrors = this.bean.validate(asField(fieldPrefix, ""));
        if (beanValidationErrors != null && !beanValidationErrors.isEmpty()) {
            this.validationErrors.addAll(beanValidationErrors);
        }

        this.fieldPrefix = fieldPrefix;

        Class<?> beanClass = this.bean.getClass();
        this.beanFields = new HashMap<>();

        for (Field field : this.getFields(beanClass)) {
            String fieldName = field.getName();
            Validate validateAnn = field.getAnnotation(Validate.class);
            if (validateAnn != null && !validateAnn.as().trim().isEmpty()) {
                fieldName = validateAnn.as();
            }

            this.beanFields.put(fieldName, field);
        }

        for (String fieldName : this.beanFields.keySet()) {
            Field field = this.beanFields.get(fieldName);

            field.setAccessible(true);
            Object fieldValue = field.get(bean);

            validateField(field, fieldName, fieldValue);

            // region Validate inner bean
            if (fieldValue instanceof Validable) {
                List<ValidationError> errors = new BeanValidator((Validable) fieldValue, currentDepth - 1)
                        .compute(asField(fieldPrefix, fieldName));

                if (errors != null) {
                    validationErrors.addAll(errors);
                }
            }
            if (fieldValue instanceof Iterable) {
                Iterable iterable = (Iterable) fieldValue;
                int counter = 0;
                for (Object subBean : iterable) {
                    if (subBean instanceof Validable) {
                        List<ValidationError> errors = new BeanValidator((Validable) subBean, currentDepth - 1)
                                .compute(String.format("%s[%s]", asField(fieldPrefix, fieldName), counter));

                        if (errors != null) {
                            validationErrors.addAll(errors);
                        }
                    }
                    counter++;
                }
            }
            // endregion
        }

        return validationErrors;
    }

    private void validateField(String fieldName) throws IllegalAccessException {
        Field field = this.beanFields.get(fieldName);
        if (field == null) {
            throw new ValidationInvalidFieldException(asField(this.fieldPrefix, fieldName));
        }

        field.setAccessible(true);
        this.validateField(field, fieldName, field.get(this.bean));
    }

    private void validateField(Field field, String fieldName, Object fieldValue) throws IllegalAccessException {
        if (this.validationSummary.get(fieldName) != null) {
            return;
        }
        Map<Class<?>, Boolean> fieldSummary = new HashMap<>();
        this.validationSummary.put(fieldName, fieldSummary);

        // region Validator: Range
        Range vRange = field.getAnnotation(Range.class);
        if (vRange != null && fieldValue != null) {
            Double from = vRange.from();
            Double to = vRange.to();

            if (fieldValue instanceof Number) {
                double castValue = ((Number) fieldValue).doubleValue();
                if (castValue < from || castValue > to) {
                    fieldSummary.put(Range.class, false);

                    if (Double.isInfinite(from)) {
                        this.validationErrors.add(new ValidationError(asField(fieldPrefix, fieldName), String.format("Value must be lesser than %s.", to)));
                    } else if (Double.isInfinite(to)) {
                        this.validationErrors.add(new ValidationError(asField(fieldPrefix, fieldName), String.format("Value must be greater than %s.", from)));
                    } else {
                        this.validationErrors.add(new ValidationError(asField(fieldPrefix, fieldName), String.format("Value must range between %s and %s.", from, to)));
                    }
                }
            } else if (fieldValue instanceof String) {
                int castValue = ((String) fieldValue).length();
                from = Math.max(0, from);
                if (castValue < from || castValue > to) {
                    fieldSummary.put(Range.class, false);
                    if (Double.isInfinite(to)) {
                        this.validationErrors.add(new ValidationError(asField(fieldPrefix, fieldName), String.format("String length must be at least %d characters.", from.intValue())));
                    } else {
                        this.validationErrors.add(new ValidationError(asField(fieldPrefix, fieldName), String.format("String length must ranges between %d and %d characters.", from.intValue(), to.intValue())));
                    }
                }
            } else if (fieldValue instanceof Collection) {
                int castValue = ((Collection) fieldValue).size();
                from = Math.max(0, from);
                if (castValue < from || castValue > to) {
                    fieldSummary.put(Range.class, false);
                    if (Double.isInfinite(to)) {
                        this.validationErrors.add(new ValidationError(asField(fieldPrefix, fieldName), String.format("Collection size must be at least %d elements", from.intValue())));
                    } else {
                        this.validationErrors.add(new ValidationError(asField(fieldPrefix, fieldName), String.format("Collection size must ranges between %d and %d elements", from.intValue(), to.intValue())));
                    }
                }
            } else {
                throw new ValidationUnsupportedFieldException(this.asField(fieldPrefix, fieldName), Range.class);
            }
        }
        // endregion

        // region Validator: Required
        Required vRequired = field.getAnnotation(Required.class);
        if (vRequired != null) {
            fieldSummary.put(Required.class, false);

            if (fieldValue == null) {
                boolean anyValid = false;
                for (String otherField : vRequired.or()) {
                    boolean valid = true;
                    this.validateField(otherField);
                    Map<Class<?>, Boolean> otherFieldSummary = this.validationSummary.get(otherField);
                    for (Class<?> validatorClass : otherFieldSummary.keySet()) {
                        if (!otherFieldSummary.get(validatorClass)) {
                            valid = false;
                            break;
                        }
                    }
                    if (valid) {
                        fieldSummary.put(Required.class, true);
                        anyValid = true;
                        break;
                    }
                }
                if (!anyValid) {
                    this.validationErrors.add(new ValidationError(asField(fieldPrefix, fieldName), "Required field."));
                }
            } else {
                boolean allValid = true;
                for (String otherField : vRequired.with()) {
                    boolean otherValid = true;
                    this.validateField(otherField);
                    Map<Class<?>, Boolean> otherFieldSummary = this.validationSummary.get(otherField);
                    for (Class<?> validatorClass : otherFieldSummary.keySet()) {
                        if (!otherFieldSummary.get(validatorClass)) {
                            otherValid = false;
                            break;
                        }
                    }
                    if (!otherValid) {
                        allValid = false;
                        break;
                    }
                }
                if (allValid) {
                    fieldSummary.put(Required.class, true);
                } else {
                    this.validationErrors.add(new ValidationError(asField(fieldPrefix, fieldName), "Validation failed due to dependents."));
                }
            }
        }
        // endregion
    }

    private String asField(String prefix, String field) {
        StringBuilder sb = new StringBuilder();

        if (prefix != null && !prefix.isEmpty()) {
            sb.append(prefix).append(".");
        }

        return sb.append(field).toString();
    }

    private Set<Field> getFields(Class<?> beanClass) {
        Set<Field> beanFields = new HashSet<>();
        for (; beanClass != null; beanClass = beanClass.getSuperclass()) {
            for (Field field : beanClass.getDeclaredFields()) {
                for (Class<? extends Annotation> constrainClass : VALIDATION_TOOLKIT_ANNOTATIONS) {
                    if (field.getAnnotation(constrainClass) != null) {
                        beanFields.add(field);
                        break;
                    }
                }
            }
        }
        return beanFields;
    }
}
