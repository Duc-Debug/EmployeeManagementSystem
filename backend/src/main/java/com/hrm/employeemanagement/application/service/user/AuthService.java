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
import com.hrm.employeemanagement.application.port.outbound.user.SaveUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.exception.user.InvalidCredentialsException;
import com.hrm.employeemanagement.domain.exception.user.UserLockedException;
import com.hrm.employeemanagement.domain.user.User;

import java.util.Objects;

/**
 * Pure Java 100% Application Service (Zero Spring framework dependencies).
 * Implements AuthenticateUserUseCase and LogoutUseCase.
 */
public class AuthService implements AuthenticateUserUseCase, LogoutUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final TokenBlacklistPort tokenBlacklistPort;
    private final SaveAuditLogPort saveAuditLogPort;

    public AuthService(LoadUserPort loadUserPort,
                       SaveUserPort saveUserPort,
                       PasswordEncoderPort passwordEncoder,
                       TokenProviderPort tokenProvider,
                       TokenBlacklistPort tokenBlacklistPort,
                       SaveAuditLogPort saveAuditLogPort) {
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.saveUserPort = Objects.requireNonNull(saveUserPort, "saveUserPort must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider must not be null");
        this.tokenBlacklistPort = Objects.requireNonNull(tokenBlacklistPort, "tokenBlacklistPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "saveAuditLogPort must not be null");
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
        Objects.requireNonNull(command, "LogoutCommand must not be null");

        String token = command.token();
        if (tokenProvider.validateToken(token)) {
            boolean isBlacklisted = tokenBlacklistPort.isBlacklisted(token);
            String username = tokenProvider.getUsernameFromToken(token);
            long issuedAt = tokenProvider.getIssuedAtTimestampFromToken(token);
            boolean isUserRevoked = username != null && tokenBlacklistPort.isUserRevoked(username, issuedAt);

            // Stolen or already revoked/blacklisted tokens cannot perform logout actions or alter revocation state
            if (isBlacklisted || isUserRevoked) {
                return;
            }

            long remainingTtl = tokenProvider.getRemainingExpirationMs(token);
            if (remainingTtl > 0) {
                tokenBlacklistPort.blacklist(token, remainingTtl);
            }

            Long userId = tokenProvider.getUserIdFromToken(token);

            if (command.allDevices() && username != null) {
                loadUserPort.findByUsername(username).ifPresent(user -> {
                    user.revokeAllSessions();
                    saveUserPort.save(user);
                });
                if (userId != null) {
                    saveAuditLogPort.save(AuditLog.create(userId, "LOGOUT_ALL", "users", userId));
                }
            } else if (userId != null) {
                saveAuditLogPort.save(AuditLog.create(userId, "LOGOUT", "users", userId));
            }
        }
    }
}

