package com.hrm.employeemanagement.infrastructure.adapter.outbound.security;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderAdapterTest {

    private static final String VALID_SECRET = "very-secure-secret-key-that-is-longer-than-32-characters-and-256-bits!";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    @Test
    @DisplayName("Khởi tạo thất bại (Fail-Fast) khi jwt.secret là null")
    void testConstructor_FailsWhenSecretIsNull() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            new JwtTokenProviderAdapter(null, EXPIRATION_MS);
        });
        assertTrue(ex.getMessage().contains("jwt.secret"));
    }

    @Test
    @DisplayName("Khởi tạo thất bại (Fail-Fast) khi jwt.secret rỗng")
    void testConstructor_FailsWhenSecretIsEmpty() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            new JwtTokenProviderAdapter("   ", EXPIRATION_MS);
        });
        assertTrue(ex.getMessage().contains("jwt.secret"));
    }

    @Test
    @DisplayName("Khởi tạo thất bại (Fail-Fast) khi jwt.secret ngắn hơn 256 bits (< 32 ký tự)")
    void testConstructor_FailsWhenSecretIsTooShort() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            new JwtTokenProviderAdapter("too-short-secret-key", EXPIRATION_MS);
        });
        assertTrue(ex.getMessage().contains("at least 256 bits"));
    }

    @Test
    @DisplayName("Sinh JWT token và xác thực thành công khi jwt.secret hợp lệ")
    void testGenerateAndValidateToken_Success() {
        JwtTokenProviderAdapter provider = new JwtTokenProviderAdapter(VALID_SECRET, EXPIRATION_MS);
        Role role = new Role(new RoleId(6L), RoleCode.VT_06, "Quản trị viên");
        User user = new User(new UserId(1L), "admin", "encoded_password", role, UserStatus.ACTIVE, new EmployeeId(10L));

        String token = provider.generateToken(user);
        assertNotNull(token);
        assertTrue(provider.validateToken(token));
        assertEquals("admin", provider.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("Xác thực thất bại khi JWT token bị giả mạo hoặc sai định dạng")
    void testValidateToken_InvalidToken_ReturnsFalse() {
        JwtTokenProviderAdapter provider = new JwtTokenProviderAdapter(VALID_SECRET, EXPIRATION_MS);
        assertFalse(provider.validateToken("invalid.jwt.token"));
        assertFalse(provider.validateToken(""));
        assertFalse(provider.validateToken(null));
    }
}
