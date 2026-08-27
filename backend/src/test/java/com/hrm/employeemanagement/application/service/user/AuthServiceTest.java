package com.hrm.employeemanagement.application.service.user;

import com.hrm.employeemanagement.application.dto.user.AuthTokenResult;
import com.hrm.employeemanagement.application.dto.user.LoginCommand;
import com.hrm.employeemanagement.application.dto.user.LogoutCommand;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.security.TokenBlacklistPort;
import com.hrm.employeemanagement.application.port.outbound.security.TokenProviderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.user.InvalidCredentialsException;
import com.hrm.employeemanagement.domain.exception.user.UserLockedException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private SaveUserPort saveUserPort;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private TokenProviderPort tokenProvider;

    @Mock
    private TokenBlacklistPort tokenBlacklistPort;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    private AuthService authService;

    private Role userRole;

    @BeforeEach
    void setUp() {
        authService = new AuthService(loadUserPort, saveUserPort, passwordEncoder, tokenProvider, tokenBlacklistPort, saveAuditLogPort);
        userRole = new Role(new RoleId(1L), RoleCode.VT_06, "Quản trị viên");
    }

    @Test
    @DisplayName("Đăng nhập thành công với tài khoản active và mật khẩu chính xác")
    void testLogin_Success() {
        LoginCommand command = new LoginCommand("admin", "password123");
        User user = new User(new UserId(1L), "admin", "encoded_hash", userRole, UserStatus.ACTIVE, new EmployeeId(10L));

        when(loadUserPort.findByUsername("admin")).thenReturn(Optional.of(user));
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
    @DisplayName("Đăng nhập thất bại khi tên đăng nhập không tồn tại ném InvalidCredentialsException")
    void testLogin_UsernameNotFound_ThrowsInvalidCredentialsException() {
        LoginCommand command = new LoginCommand("nonexistent", "password123");

        when(loadUserPort.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> {
            authService.login(command);
        });

        verify(tokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai mật khẩu ném InvalidCredentialsException")
    void testLogin_WrongPassword_ThrowsInvalidCredentialsException() {
        LoginCommand command = new LoginCommand("admin", "wrongpassword");
        User user = new User(new UserId(1L), "admin", "encoded_hash", userRole, UserStatus.ACTIVE, new EmployeeId(10L));

        when(loadUserPort.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encoded_hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> {
            authService.login(command);
        });

        verify(tokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tài khoản bị khóa ném UserLockedException")
    void testLogin_LockedUser_ThrowsUserLockedException() {
        LoginCommand command = new LoginCommand("locked_user", "password123");
        User user = new User(new UserId(2L), "locked_user", "encoded_hash", userRole, UserStatus.LOCKED, new EmployeeId(20L));

        when(loadUserPort.findByUsername("locked_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_hash")).thenReturn(true);

        assertThrows(UserLockedException.class, () -> {
            authService.login(command);
        });

        verify(tokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Đăng xuất thành công với token hợp lệ: đưa token vào blacklist và ghi nhận audit log")
    void testLogout_Success() {
        String token = "valid.jwt.token";
        LogoutCommand command = new LogoutCommand(token);

        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getRemainingExpirationMs(token)).thenReturn(3600000L);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(1L);

        authService.logout(command);

        verify(tokenBlacklistPort, times(1)).blacklist(token, 3600000L);
        verify(saveAuditLogPort, times(1)).save(any());
    }

    @Test
    @DisplayName("Đăng xuất khi token không hợp lệ hoặc đã hết hạn: không blacklist và không ghi audit log")
    void testLogout_InvalidToken() {
        String token = "invalid.jwt.token";
        LogoutCommand command = new LogoutCommand(token);

        when(tokenProvider.validateToken(token)).thenReturn(false);

        authService.logout(command);

        verify(tokenBlacklistPort, never()).blacklist(anyString(), anyLong());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Đăng xuất với token hợp lệ: trích xuất userId từ TokenProviderPort")
    void testLogout_ExtractUserIdFromToken() {
        String token = "valid.jwt.token";
        LogoutCommand command = new LogoutCommand(token);

        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getRemainingExpirationMs(token)).thenReturn(1800000L);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(2L);

        authService.logout(command);

        verify(tokenBlacklistPort, times(1)).blacklist(token, 1800000L);
        verify(saveAuditLogPort, times(1)).save(any());
    }

    @Test
    @DisplayName("Đăng xuất mọi thiết bị (allDevices = true): thu hồi toàn bộ token của user và ghi nhận LOGOUT_ALL")
    void testLogout_AllDevices_Success() {
        String token = "valid.jwt.token";
        LogoutCommand command = LogoutCommand.of(token, true);
        User user = new User(new UserId(1L), "admin", "encoded_hash", userRole, UserStatus.ACTIVE, new EmployeeId(10L));

        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn("admin");
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(1L);
        when(tokenProvider.getRemainingExpirationMs(token)).thenReturn(3600000L);
        when(loadUserPort.findByUsername("admin")).thenReturn(Optional.of(user));

        authService.logout(command);

        verify(tokenBlacklistPort, times(1)).blacklist(token, 3600000L);
        verify(tokenBlacklistPort, never()).blacklistUser(anyString(), anyLong());
        verify(saveUserPort, times(1)).save(user);
        assertEquals(2, user.getTokenVersion());
        verify(saveAuditLogPort, times(1)).save(argThat(audit -> "LOGOUT_ALL".equals(audit.getAction())));
    }

    @Test
    @DisplayName("Đăng xuất với token đã bị blacklist hoặc revoked: không cập nhật trạng thái thu hồi hay ghi log lại")
    void testLogout_AlreadyBlacklistedOrRevoked_Ignored() {
        String token = "blacklisted.jwt.token";
        LogoutCommand command = LogoutCommand.of(token, true);

        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenBlacklistPort.isBlacklisted(token)).thenReturn(true);

        authService.logout(command);

        verify(tokenBlacklistPort, never()).blacklist(anyString(), anyLong());
        verify(tokenBlacklistPort, never()).blacklistUser(anyString(), anyLong());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Đăng xuất với command là null ném NullPointerException (Fail-Fast)")
    void testLogout_NullCommand_ThrowsNullPointerException() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            authService.logout(null);
        });

        assertEquals("LogoutCommand must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Đăng xuất mọi thiết bị thất bại ở bước DB save: không blacklist token để cho phép retry")
    void testLogout_AllDevices_DbFailure_DoesNotBlacklistToken() {
        String token = "valid.jwt.token";
        LogoutCommand command = LogoutCommand.of(token, true);
        User user = new User(new UserId(1L), "admin", "encoded_hash", userRole, UserStatus.ACTIVE, new EmployeeId(10L));

        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn("admin");
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(1L);
        when(loadUserPort.findByUsername("admin")).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("DB Connection Error")).when(saveUserPort).save(user);

        assertThrows(RuntimeException.class, () -> authService.logout(command));

        verify(tokenBlacklistPort, never()).blacklist(anyString(), anyLong());
    }
}