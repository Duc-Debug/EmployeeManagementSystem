package com.hrm.employeemanagement.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Pass1234",
            "Abcd12345",
            "StrongPassword123!",
            "1234567a",
            "a1b2c3d4",
            "P@ssw0rd2026"
    })
    @DisplayName("Hợp lệ khi mật khẩu có tối thiểu 8 ký tự và bao gồm cả chữ cái và chữ số")
    void testValidPasswords(String password) {
        assertTrue(PasswordPolicyValidator.isValid(password));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "1234567",         // 7 digits
            "abcdefg",         // 7 letters
            "12345678",        // 8 digits no letter
            "abcdefgh",        // 8 letters no digit
            "pass 12",         // 7 chars
            "Short1"           // 6 chars
    })
    @DisplayName("Không hợp lệ khi mật khẩu dưới 8 ký tự hoặc thiếu chữ cái hoặc thiếu chữ số")
    void testInvalidPasswords(String password) {
        assertFalse(PasswordPolicyValidator.isValid(password));
    }

    @Test
    @DisplayName("Không hợp lệ khi mật khẩu là null")
    void testNullPassword() {
        assertFalse(PasswordPolicyValidator.isValid(null));
    }
}
