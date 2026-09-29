package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidPasswordValidatorTest {

    private ValidPasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ValidPasswordValidator();
    }

    @Test
    @DisplayName("Giá trị null được bỏ qua (để @NotBlank / @NotNull xử lý)")
    void testNullReturnsTrue() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    @DisplayName("Mật khẩu hợp lệ trả về true")
    void testValidPassword() {
        assertTrue(validator.isValid("AdminPass123", null));
    }

    @Test
    @DisplayName("Mật khẩu không hợp lệ trả về false")
    void testInvalidPassword() {
        assertFalse(validator.isValid("123456", null));
        assertFalse(validator.isValid("secret", null));
        assertFalse(validator.isValid("12345678", null));
        assertFalse(validator.isValid("passwordonly", null));
    }
}
