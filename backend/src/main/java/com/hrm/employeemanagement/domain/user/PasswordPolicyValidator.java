package com.hrm.employeemanagement.domain.user;

/**
 * Shared domain password policy validator (Pure Java - zero framework dependencies).
 * Enforces:
 * - Minimum 8 characters
 * - Must contain at least one letter (a-zA-Z)
 * - Must contain at least one digit (0-9)
 */
public final class PasswordPolicyValidator {

    public static final int MIN_LENGTH = 8;
    public static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";
    public static final String POLICY_MESSAGE = "Mật khẩu phải có tối thiểu 8 ký tự, bao gồm cả chữ cái và chữ số";

    private PasswordPolicyValidator() {}

    public static boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        return password.matches(PASSWORD_PATTERN);
    }
}
