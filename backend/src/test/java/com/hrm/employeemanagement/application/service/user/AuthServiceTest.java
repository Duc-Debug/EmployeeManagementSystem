package com.hrm.employeemanagement.application.service.user;

import com.hrm.employeemanagement.application.dto.user.AuthTokenResult;
import com.hrm.employeemanagement.application.dto.user.LoginCommand;
import com.hrm.employeemanagement.domain.model.role.Role;
import com.hrm.employeemanagement.domain.model.role.RoleCode;
import com.hrm.employeemanagement.domain.model.user.User;
import com.hrm.employeemanagement.domain.model.user.UserStatus;
import com.hrm.employeemanagement.domain.repository.user.UserRepository;
import com.hrm.employeemanagement.port.out.user.PasswordEncoderPort;
import com.hrm.employeemanagement.port.out.user.TokenProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private TokenProviderPort tokenProvider;

    private AuthService authService;

    private Role userRole;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider);
        userRole = new Role(1L, RoleCode.VT_06, "Quản trị viên");
    }

    @Test
    @DisplayName("Đăng nhập thành công với tài khoản active và mật khẩu chính xác")
    void testLogin_Success() {
        LoginCommand command = new LoginCommand("admin", "password123");
        User user = new User(1L, "admin", "encoded_hash", userRole, UserStatus.ACTIVE, 10L);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_hash")).thenReturn(true);
        when(tokenProvider.generateToken(user)).thenReturn("mocked.jwt.token");

        AuthTokenResult result = authService.login(command);

        assertNotNull(result);
        assertEquals("mocked.jwt.token", result.getToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(1L, result.getUserId());
        assertEquals("admin", result.getUsername());
        assertEquals("VT-06", result.getRoleCode());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tên đăng nhập không tồn tại")
    void testLogin_UsernameNotFound_ThrowsBadCredentialsException() {
        LoginCommand command = new LoginCommand("nonexistent", "password123");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> {
            authService.login(command);
        });

        verify(tokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai mật khẩu")
    void testLogin_WrongPassword_ThrowsBadCredentialsException() {
        LoginCommand command = new LoginCommand("admin", "wrongpassword");
        User user = new User(1L, "admin", "encoded_hash", userRole, UserStatus.ACTIVE, 10L);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encoded_hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> {
            authService.login(command);
        });

        verify(tokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tài khoản bị khóa")
    void testLogin_LockedUser_ThrowsDisabledException() {
        LoginCommand command = new LoginCommand("locked_user", "password123");
        User user = new User(2L, "locked_user", "encoded_hash", userRole, UserStatus.LOCKED, 20L);

        when(userRepository.findByUsername("locked_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_hash")).thenReturn(true);

        assertThrows(DisabledException.class, () -> {
            authService.login(command);
        });

        verify(tokenProvider, never()).generateToken(any());
    }
}
