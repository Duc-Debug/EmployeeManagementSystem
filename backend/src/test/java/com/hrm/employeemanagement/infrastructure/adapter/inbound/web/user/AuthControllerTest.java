package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.application.dto.user.AuthTokenResult;
import com.hrm.employeemanagement.application.dto.user.LoginCommand;
import com.hrm.employeemanagement.application.port.inbound.user.AuthenticateUserUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthenticateUserUseCase authenticateUserUseCase;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authenticateUserUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
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
    @DisplayName("POST /api/v1/auth/login trả về 401 Unauthorized khi sai thông tin đăng nhập")
    void testLogin_BadCredentials_Returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        when(authenticateUserUseCase.login(any(LoginCommand.class)))
                .thenThrow(new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("không chính xác")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login trả về 403 Forbidden khi tài khoản bị khóa")
    void testLogin_LockedUser_Returns403() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("locked_user");
        request.setPassword("pass");

        when(authenticateUserUseCase.login(any(LoginCommand.class)))
                .thenThrow(new DisabledException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("đã bị khóa")));
    }
}
