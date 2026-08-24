package com.hrm.employeemanagement.application.service.user;

import com.hrm.employeemanagement.application.dto.user.ChangePasswordCommand;
import com.hrm.employeemanagement.application.dto.user.RequestPasswordResetCommand;
import com.hrm.employeemanagement.application.dto.user.ResetPasswordCommand;
import com.hrm.employeemanagement.application.port.inbound.user.ChangePasswordUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.RequestPasswordResetUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.ResetPasswordUseCase;
import com.hrm.employeemanagement.application.port.outbound.email.SimulatedEmailPort;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.user.*;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.exception.user.InvalidPasswordException;
import com.hrm.employeemanagement.domain.exception.user.InvalidResetTokenException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.user.PasswordResetToken;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure Java 100% Application Service (Zero Spring framework dependencies).
 * Implements ChangePasswordUseCase, RequestPasswordResetUseCase, ResetPasswordUseCase.
 */
public class PasswordService implements ChangePasswordUseCase, RequestPasswordResetUseCase, ResetPasswordUseCase {

    private static final long RESET_TOKEN_VALIDITY_MINUTES = 15;

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordEncoderPort passwordEncoder;
    private final SavePasswordResetTokenPort savePasswordResetTokenPort;
    private final LoadPasswordResetTokenPort loadPasswordResetTokenPort;
    private final SimulatedEmailPort simulatedEmailPort;
    private final SaveAuditLogPort saveAuditLogPort;

    public PasswordService(LoadUserPort loadUserPort,
                           SaveUserPort saveUserPort,
                           PasswordEncoderPort passwordEncoder,
                           SavePasswordResetTokenPort savePasswordResetTokenPort,
                           LoadPasswordResetTokenPort loadPasswordResetTokenPort,
                           SimulatedEmailPort simulatedEmailPort,
                           SaveAuditLogPort saveAuditLogPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordEncoder = passwordEncoder;
        this.savePasswordResetTokenPort = savePasswordResetTokenPort;
        this.loadPasswordResetTokenPort = loadPasswordResetTokenPort;
        this.simulatedEmailPort = simulatedEmailPort;
        this.saveAuditLogPort = saveAuditLogPort;
    }

    @Override
    public void changePassword(ChangePasswordCommand command) {
        if (command.userId() == null) {
            throw new InvalidPasswordException("Mã người dùng không hợp lệ");
        }
        validatePasswordPair(command.newPassword(), command.confirmPassword());

        User user = loadUserPort.findById(new UserId(command.userId()))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + command.userId()));

        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Mật khẩu hiện tại không chính xác");
        }

        if (passwordEncoder.matches(command.newPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Mật khẩu mới không được giống mật khẩu hiện tại");
        }

        String newPasswordHash = passwordEncoder.encode(command.newPassword());
        user.updatePassword(newPasswordHash, Instant.now());
        saveUserPort.save(user);

        saveAuditLogPort.save(AuditLog.create(user.getIdValue(), "CHANGE_PASSWORD", "users", user.getIdValue()));
    }

    @Override
    public void requestPasswordReset(RequestPasswordResetCommand command) {
        if (command.identity() == null || command.identity().isBlank()) {
            return;
        }

        String identity = command.identity().trim();
        Optional<User> userOpt = loadUserPort.findByUsernameOrEmail(identity);

        if (userOpt.isPresent() && userOpt.get().isActive()) {
            User user = userOpt.get();

            // Invalidate all previous active password reset tokens for this user
            savePasswordResetTokenPort.invalidateActiveTokensByUserId(user.getId());

            String rawTokenString = UUID.randomUUID().toString().replace("-", "");
            String hashedToken = hashToken(rawTokenString);
            
            PasswordResetToken resetToken = PasswordResetToken.createNew(
                    user.getId(),
                    hashedToken,
                    RESET_TOKEN_VALIDITY_MINUTES
            );
            savePasswordResetTokenPort.save(resetToken);

            // Send simulated email only if explicit email address is present on the user profile
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                simulatedEmailPort.sendPasswordResetEmail(user.getEmail(), user.getUsername(), rawTokenString, RESET_TOKEN_VALIDITY_MINUTES);
            }

            saveAuditLogPort.save(AuditLog.create(user.getIdValue(), "REQUEST_PASSWORD_RESET", "password_reset_tokens", resetToken.getId()));
        }
    }

    @Override
    public void resetPassword(ResetPasswordCommand command) {
        if (command.token() == null || command.token().isBlank()) {
            throw new InvalidResetTokenException("Mã khôi phục mật khẩu không được để trống");
        }
        validatePasswordPair(command.newPassword(), command.confirmPassword());

        String hashedToken = hashToken(command.token().trim());
        PasswordResetToken resetToken = loadPasswordResetTokenPort.findByTokenHash(hashedToken)
                .orElseThrow(() -> new InvalidResetTokenException("Mã khôi phục mật khẩu không hợp lệ hoặc không tồn tại"));

        Instant now = Instant.now();
        resetToken.validateAndMarkUsed(now);

        User user = loadUserPort.findById(resetToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy tài khoản người dùng gắn với mã khôi phục"));

        String newPasswordHash = passwordEncoder.encode(command.newPassword());
        user.updatePassword(newPasswordHash, now);

        // Atomic writes: Token status + User password + Audit Log in single transaction
        savePasswordResetTokenPort.save(resetToken);
        saveUserPort.save(user);
        saveAuditLogPort.save(AuditLog.create(user.getIdValue(), "RESET_PASSWORD", "users", user.getIdValue()));
    }

    private void validatePasswordPair(String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new InvalidPasswordException("Mật khẩu mới không được để trống");
        }
        if (newPassword.length() < 8) {
            throw new InvalidPasswordException("Mật khẩu mới phải có độ dài tối thiểu 8 ký tự");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new InvalidPasswordException("Mật khẩu mới và mật khẩu xác nhận không trùng khớp");
        }
    }

    public static String hashToken(String rawToken) {
        if (rawToken == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
