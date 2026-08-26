package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import com.hrm.employeemanagement.application.dto.user.AuthTokenResult;
import com.hrm.employeemanagement.application.dto.user.ChangePasswordCommand;
import com.hrm.employeemanagement.application.dto.user.LoginCommand;
import com.hrm.employeemanagement.application.dto.user.LogoutCommand;
import com.hrm.employeemanagement.application.dto.user.RequestPasswordResetCommand;
import com.hrm.employeemanagement.application.dto.user.ResetPasswordCommand;
import com.hrm.employeemanagement.application.port.inbound.user.AuthenticateUserUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.ChangePasswordUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.LogoutUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.RequestPasswordResetUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.ResetPasswordUseCase;
import com.hrm.employeemanagement.domain.exception.user.InvalidCredentialsException;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ChangePasswordRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ForgotPasswordRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.LoginRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ResetPasswordRequest;
import com.hrm.employeemanagement.infrastructure.security.ForgotPasswordRateLimiter;
import com.hrm.employeemanagement.infrastructure.security.LoginRateLimiter;
import com.hrm.employeemanagement.infrastructure.security.UserStatusCache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final LoginRateLimiter loginRateLimiter;
    private final ForgotPasswordRateLimiter forgotPasswordRateLimiter;
    private final UserStatusCache userStatusCache;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase,
                          LogoutUseCase logoutUseCase,
                          ChangePasswordUseCase changePasswordUseCase,
                          RequestPasswordResetUseCase requestPasswordResetUseCase,
                          ResetPasswordUseCase resetPasswordUseCase,
                          LoginRateLimiter loginRateLimiter,
                          ForgotPasswordRateLimiter forgotPasswordRateLimiter,
                          UserStatusCache userStatusCache) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.logoutUseCase = logoutUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.loginRateLimiter = loginRateLimiter;
        this.forgotPasswordRateLimiter = forgotPasswordRateLimiter;
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
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest,
                                                      @RequestParam(value = "allDevices", required = false, defaultValue = "false") boolean allDevices) {
        String authHeader = httpRequest != null ? httpRequest.getHeader("Authorization") : null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isBlank()) {
                if (logoutUseCase != null) {
                    logoutUseCase.logout(new LogoutCommand(token, allDevices));
                }

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof User user && userStatusCache != null) {
                    userStatusCache.evict(user.getUsername());
                }
            }
        }

        return ResponseEntity.ok(
                ApiResponse.success(allDevices ? "Đăng xuất khỏi tất cả thiết bị thành công" : "Đăng xuất thành công", null)
        );
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null || currentUser.getIdValue() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Bạn cần đăng nhập để đổi mật khẩu"));
        }

        ChangePasswordCommand command = new ChangePasswordCommand(
                currentUser.getIdValue(),
                request.currentPassword(),
                request.newPassword(),
                request.confirmPassword()
        );

        changePasswordUseCase.changePassword(command);

        return ResponseEntity.ok(
                ApiResponse.success("Đổi mật khẩu thành công. Các phiên đăng nhập cũ đã được chấm dứt.", null)
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                             HttpServletRequest httpRequest) {
        String clientIp = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
        String normalizedIdentity = request.identity() != null ? request.identity().trim().toLowerCase() : "";
        String rateLimitKey = clientIp + ":" + normalizedIdentity;

        if (forgotPasswordRateLimiter.isRateLimited(rateLimitKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Bạn đã gửi yêu cầu quá nhiều lần. Vui lòng đợi 1 phút trước khi thử lại."));
        }

        forgotPasswordRateLimiter.recordRequest(rateLimitKey);

        RequestPasswordResetCommand command = new RequestPasswordResetCommand(request.identity());
        requestPasswordResetUseCase.requestPasswordReset(command);

        return ResponseEntity.ok(
                ApiResponse.success("Nếu thông tin tài khoản hợp lệ, liên kết khôi phục mật khẩu đã được gửi đến email đăng ký.", null)
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordCommand command = new ResetPasswordCommand(
                request.token(),
                request.newPassword(),
                request.confirmPassword()
        );

        resetPasswordUseCase.resetPassword(command);

        return ResponseEntity.ok(
                ApiResponse.success("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới.", null)
        );
    }
}