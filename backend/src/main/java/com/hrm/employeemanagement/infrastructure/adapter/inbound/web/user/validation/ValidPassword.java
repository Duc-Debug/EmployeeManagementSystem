package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Mật khẩu phải có tối thiểu 8 ký tự, bao gồm cả chữ cái và chữ số";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
