package com.hrm.employeemanagement.application.service.user;

import com.hrm.employeemanagement.application.dto.user.AuthTokenResult;
import com.hrm.employeemanagement.application.dto.user.LoginCommand;
import com.hrm.employeemanagement.application.dto.user.LogoutCommand;
import com.hrm.employeemanagement.application.port.inbound.user.AuthenticateUserUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.LogoutUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.security.TokenBlacklistPort;
import com.hrm.employeemanagement.application.port.outbound.security.TokenProviderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.exception.user.InvalidCredentialsException;
import com.hrm.employeemanagement.domain.exception.user.UserLockedException;
import com.hrm.employeemanagement.domain.user.User;

/**
 * Pure Java 100% Application Service (Zero Spring framework dependencies).
 * Implements AuthenticateUserUseCase and LogoutUseCase.
 */
public class AuthService implements AuthenticateUserUseCase, LogoutUseCase {

    private final LoadUserPort loadUserPort;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final TokenBlacklistPort tokenBlacklistPort;
    private final SaveAuditLogPort saveAuditLogPort;

    public AuthService(LoadUserPort loadUserPort,
                       PasswordEncoderPort passwordEncoder,
                       TokenProviderPort tokenProvider) {
        this(loadUserPort, passwordEncoder, tokenProvider, null, null);
    }

    public AuthService(LoadUserPort loadUserPort,
                       PasswordEncoderPort passwordEncoder,
                       TokenProviderPort tokenProvider,
                       TokenBlacklistPort tokenBlacklistPort,
                       SaveAuditLogPort saveAuditLogPort) {
        this.loadUserPort = loadUserPort;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.tokenBlacklistPort = tokenBlacklistPort;
        this.saveAuditLogPort = saveAuditLogPort;
    }

    @Override
    public AuthTokenResult login(LoginCommand command) {
        User user = loadUserPort.findByUsername(command.username())
                .orElseThrow(() -> new InvalidCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        if (!user.isActive()) {
            throw new UserLockedException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên.");
        }

        String token = tokenProvider.generateToken(user);
        return new AuthTokenResult(token, "Bearer", user.getIdValue(), user.getUsername(), user.getRole().getCode().getCode());
    }

    @Override
    public void logout(LogoutCommand command) {
        if (command == null || command.token() == null || command.token().isBlank()) {
            return;
        }

        String token = command.token();
        if (tokenProvider.validateToken(token)) {
            long remainingTtl = tokenProvider.getRemainingExpirationMs(token);
            if (remainingTtl > 0 && tokenBlacklistPort != null) {
                tokenBlacklistPort.blacklist(token, remainingTtl);
            }

            Long userId = command.userId();
            if (userId == null) {
                userId = tokenProvider.getUserIdFromToken(token);
            }

            if (userId != null && saveAuditLogPort != null) {
                saveAuditLogPort.save(AuditLog.create(userId, "LOGOUT", "users", userId));
            }
        }
    }
}

