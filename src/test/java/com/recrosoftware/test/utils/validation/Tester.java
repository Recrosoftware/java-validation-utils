package com.recrosoftware.test.utils.validation;

import com.recrosoftware.utils.validation.BeanValidator;
import com.recrosoftware.utils.validation.Validable;
import com.recrosoftware.utils.validation.ValidationError;
import com.recrosoftware.utils.validation.annotation.Range;
import com.recrosoftware.utils.validation.annotation.Required;
import com.recrosoftware.utils.validation.annotation.Validate;
import com.recrosoftware.utils.validation.exception.ValidationProcessingException;

import java.util.ArrayList;
import java.util.List;

public class Tester {
    private static class ToValidate implements Validable {
        @Required(or = "property2")
        private Integer property1;

        @Required(or = "property3")
        private Boolean property2;

        @Range(from = 2)
        private List<SubValidate> property3 = new ArrayList<SubValidate>() {{
            add(new SubValidate());
        }};
    }

    private static class SubValidate implements Validable {
        @Required(with = "sub_property_2")
        String subProperty1 = "Hello";

        @Validate(as = "sub_property_2")
        @Required(with = "subProperty3")
        String subProperty2 = "world";

        @Range(to = 5)
        @Required
        List<Boolean> subProperty3;
    }

    public static void main(String[] args) {
        try {
            BeanValidator.validate(new ToValidate());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ValidationProcessingException ex) {
            for(ValidationError error: ex.getValidationErrors()) {
                System.out.println(String.format("%s\t -> %s", error.getField(), error.getReason()));
            }
        }
    }
}
