package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.validation;

import com.hrm.employeemanagement.domain.user.PasswordPolicyValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            // Let @NotBlank / @NotNull handle null checks if required
            return true;
        }
        return PasswordPolicyValidator.isValid(value);
    }
}
