package com.hrm.employeemanagement.infrastructure.adapter.outbound.security;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.security.JwtProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderAdapterTest {

    private static final String VALID_SECRET = "very-secure-secret-key-that-is-longer-than-32-characters-and-256-bits!";
    private static final long EXPIRATION_MS = 3600000; // 1 hour
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Cấu hình JWT không hợp lệ khi jwt.secret là null")
    void testJwtProperties_InvalidWhenSecretIsNull() {
        Set<ConstraintViolation<JwtProperties>> violations = VALIDATOR.validate(new JwtProperties(null, EXPIRATION_MS));

        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Cấu hình JWT không hợp lệ khi jwt.secret rỗng")
    void testJwtProperties_InvalidWhenSecretIsEmpty() {
        Set<ConstraintViolation<JwtProperties>> violations = VALIDATOR.validate(new JwtProperties("   ", EXPIRATION_MS));

        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Cấu hình JWT không hợp lệ khi jwt.secret ngắn hơn 256 bits (< 32 ký tự)")
    void testJwtProperties_InvalidWhenSecretIsTooShort() {
        Set<ConstraintViolation<JwtProperties>> violations = VALIDATOR.validate(new JwtProperties("too-short-secret-key", EXPIRATION_MS));

        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Sinh JWT token và xác thực thành công khi jwt.secret hợp lệ")
    void testGenerateAndValidateToken_Success() {
        JwtTokenProviderAdapter provider = new JwtTokenProviderAdapter(jwtProperties());
        Role role = new Role(new RoleId(6L), RoleCode.VT_06, "Quản trị viên");
        User user = new User(new UserId(1L), "admin", "encoded_password", role, UserStatus.ACTIVE, new EmployeeId(10L));

        String token = provider.generateToken(user);
        assertNotNull(token);
        assertTrue(provider.validateToken(token));
        assertEquals("admin", provider.getUsernameFromToken(token));
        assertEquals(1L, provider.getUserIdFromToken(token));
        assertNotNull(provider.getJtiFromToken(token));
        assertTrue(provider.getIssuedAtTimestampFromToken(token) > 0);
        assertTrue(provider.getRemainingExpirationMs(token) > 0);
        assertTrue(provider.getRemainingExpirationMs(token) <= EXPIRATION_MS);
    }

    @Test
    @DisplayName("Xác thực thất bại khi JWT token bị giả mạo hoặc sai định dạng")
    void testValidateToken_InvalidToken_ReturnsFalse() {
        JwtTokenProviderAdapter provider = new JwtTokenProviderAdapter(jwtProperties());
        assertFalse(provider.validateToken("invalid.jwt.token"));
        assertFalse(provider.validateToken(""));
        assertFalse(provider.validateToken(null));
        assertEquals(0L, provider.getRemainingExpirationMs("invalid.jwt.token"));
        org.junit.jupiter.api.Assertions.assertNull(provider.getUserIdFromToken("invalid.jwt.token"));
    }

    private static JwtProperties jwtProperties() {
        return new JwtProperties(VALID_SECRET, EXPIRATION_MS);
    }
}
