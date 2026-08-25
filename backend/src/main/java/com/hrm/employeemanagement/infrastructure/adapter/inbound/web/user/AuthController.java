package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.user.AuthTokenResult;
import com.hrm.employeemanagement.application.dto.user.LoginCommand;
import com.hrm.employeemanagement.application.dto.user.LogoutCommand;
import com.hrm.employeemanagement.application.port.inbound.user.AuthenticateUserUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.LogoutUseCase;
import com.hrm.employeemanagement.domain.exception.user.InvalidCredentialsException;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.LoginRequest;
import com.hrm.employeemanagement.infrastructure.security.LoginRateLimiter;
import com.hrm.employeemanagement.infrastructure.security.UserStatusCache;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final LogoutUseCase logoutUseCase;
    private final LoginRateLimiter loginRateLimiter;
    private final UserStatusCache userStatusCache;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase,
                          LogoutUseCase logoutUseCase,
                          LoginRateLimiter loginRateLimiter,
                          UserStatusCache userStatusCache) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.logoutUseCase = logoutUseCase;
        this.loginRateLimiter = loginRateLimiter;
        this.userStatusCache = userStatusCache;
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
            LoginCommand command = new LoginCommand(
                    request.getUsername(),
                    request.getPassword()
            );

            AuthTokenResult result = authenticateUserUseCase.login(command);

            loginRateLimiter.recordSuccessfulLogin(rateLimitKey);

            return ResponseEntity.ok(
                    ApiResponse.success("Đăng nhập thành công", result)
            );

        } catch (InvalidCredentialsException ex) {
            loginRateLimiter.recordFailedAttempt(rateLimitKey);
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
        String authHeader = httpRequest != null ? httpRequest.getHeader("Authorization") : null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isBlank()) {
                String username = null;
                Long userId = null;

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof User user) {
                    username = user.getUsername();
                    userId = user.getIdValue();
                }

                if (logoutUseCase != null) {
                    logoutUseCase.logout(new LogoutCommand(token, userId, username));
                }

                if (username != null && userStatusCache != null) {
                    userStatusCache.evict(username);
                }
            }
        }

        return ResponseEntity.ok(
                ApiResponse.success("Đăng xuất thành công", null)
        );
    }
}
