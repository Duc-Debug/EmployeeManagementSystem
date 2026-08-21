package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import com.hrm.employeemanagement.application.dto.user.AuthTokenResult;
import com.hrm.employeemanagement.application.dto.user.LoginCommand;
import com.hrm.employeemanagement.application.port.inbound.user.AuthenticateUserUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.LoginRequest;
import com.hrm.employeemanagement.infrastructure.security.LoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase, LoginRateLimiter loginRateLimiter) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResult>> login(@Valid @RequestBody LoginRequest request,
                                                              HttpServletRequest httpRequest) {
        String clientIp = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
        String rateLimitKey = clientIp + ":" + (request.getUsername() != null ? request.getUsername().trim() : "");

        if (loginRateLimiter.isBlocked(rateLimitKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Bạn đã thử đăng nhập sai quá nhiều lần. Vui lòng đợi 1 phút trước khi thử lại."));
        }

        try {
            LoginCommand command = new LoginCommand(request.getUsername(), request.getPassword());
            AuthTokenResult result = authenticateUserUseCase.login(command);
            loginRateLimiter.recordSuccessfulLogin(rateLimitKey);
            return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", result));
        } catch (Exception ex) {
            loginRateLimiter.recordFailedAttempt(rateLimitKey);
            throw ex;
        }
    }
}

