package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.application.dto.user.AuthTokenResult;
import com.hrm.employeemanagement.application.dto.user.LoginCommand;
import com.hrm.employeemanagement.application.port.inbound.user.AuthenticateUserUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.ChangePasswordUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.LogoutUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.RequestPasswordResetUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.ResetPasswordUseCase;
import com.hrm.employeemanagement.domain.exception.user.InvalidCredentialsException;
import com.hrm.employeemanagement.domain.exception.user.UserLockedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ForgotPasswordRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.LoginRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ResetPasswordRequest;
import com.hrm.employeemanagement.infrastructure.security.ForgotPasswordRateLimiter;
import com.hrm.employeemanagement.infrastructure.security.LoginRateLimiter;
import com.hrm.employeemanagement.infrastructure.security.UserStatusCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthenticateUserUseCase authenticateUserUseCase;
    @Mock
    private LogoutUseCase logoutUseCase;
    @Mock
    private ChangePasswordUseCase changePasswordUseCase;
    @Mock
    private RequestPasswordResetUseCase requestPasswordResetUseCase;
    @Mock
    private ResetPasswordUseCase resetPasswordUseCase;

    private UserStatusCache userStatusCache;

    @BeforeEach
    void setUp() {
        userStatusCache = new UserStatusCache();
        AuthController authController = new AuthController(
                authenticateUserUseCase,
                logoutUseCase,
                changePasswordUseCase,
                requestPasswordResetUseCase,
                resetPasswordUseCase,
                new LoginRateLimiter(),
                new ForgotPasswordRateLimiter(),
                userStatusCache
        );
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new UserExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login trả về 200 OK khi đăng nhập thành công")
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        AuthTokenResult tokenResult = new AuthTokenResult("sample.jwt.token", "Bearer", 1L, "admin", "VT-06");
        when(authenticateUserUseCase.login(any(LoginCommand.class))).thenReturn(tokenResult);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("sample.jwt.token"))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.roleCode").value("VT-06"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login trả về 401 Unauthorized khi sai thông tin đăng nhập (InvalidCredentialsException)")
    void testLogin_BadCredentials_Returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        when(authenticateUserUseCase.login(any(LoginCommand.class)))
                .thenThrow(new InvalidCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("không chính xác")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login trả về 403 Forbidden khi tài khoản bị khóa (UserLockedException)")
    void testLogin_LockedUser_Returns403() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("locked_user");
        request.setPassword("pass");

        when(authenticateUserUseCase.login(any(LoginCommand.class)))
                .thenThrow(new UserLockedException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("đã bị khóa")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login trả về 429 Too Many Requests khi thử sai liên tiếp quá 5 lần")
    void testLogin_RateLimiting_Returns429After5Failures() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("brute_user");
        request.setPassword("wrong");

        when(authenticateUserUseCase.login(any(LoginCommand.class)))
                .thenThrow(new InvalidCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        // Fail 5 times
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt should be blocked with 429 Too Many Requests
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("quá nhiều lần")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout trả về 200 OK và gọi LogoutUseCase khi có Bearer token")
    void testLogout_WithBearerToken_Returns200OK() throws Exception {
        String token = "sample.valid.jwt";

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));

        verify(logoutUseCase, times(1))
                .logout(org.mockito.ArgumentMatchers.argThat(cmd -> token.equals(cmd.token())));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout trả về 200 OK khi không có Authorization header (idempotent)")
    void testLogout_WithoutAuthorizationHeader_Returns200OK() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));

        verify(logoutUseCase, never()).logout(any());
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout?allDevices=true trả về 200 OK và gọi LogoutUseCase với cờ allDevices")
    void testLogout_AllDevices_Returns200OK() throws Exception {
        String token = "sample.valid.jwt";

        mockMvc.perform(post("/api/v1/auth/logout?allDevices=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng xuất khỏi tất cả thiết bị thành công"));

        verify(logoutUseCase, times(1))
                .logout(org.mockito.ArgumentMatchers.argThat(cmd -> cmd.allDevices() && token.equals(cmd.token())));
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password thành công")
    void testForgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("employee1@company.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(requestPasswordResetUseCase).requestPasswordReset(any());
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password thành công")
    void testResetPassword_Success() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("token123", "NewPass123", "NewPass123");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(resetPasswordUseCase).resetPassword(any());
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password trả về 429 Too Many Requests khi gửi quá 3 lần/phút")
    void testForgotPassword_RateLimiting_Returns429() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("spam_user@company.com");

        // 3 requests allowed
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // 4th request blocked with 429
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("quá nhiều lần")));
    }
}