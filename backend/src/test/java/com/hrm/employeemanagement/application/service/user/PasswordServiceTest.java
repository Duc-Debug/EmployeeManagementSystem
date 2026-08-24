package com.hrm.employeemanagement.application.service.user;

import com.hrm.employeemanagement.application.dto.user.ChangePasswordCommand;
import com.hrm.employeemanagement.application.dto.user.RequestPasswordResetCommand;
import com.hrm.employeemanagement.application.dto.user.ResetPasswordCommand;
import com.hrm.employeemanagement.application.port.outbound.email.SimulatedEmailPort;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.user.*;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.exception.user.InvalidPasswordException;
import com.hrm.employeemanagement.domain.exception.user.InvalidResetTokenException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.PasswordResetToken;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private SaveUserPort saveUserPort;
    @Mock
    private PasswordEncoderPort passwordEncoder;
    @Mock
    private SavePasswordResetTokenPort savePasswordResetTokenPort;
    @Mock
    private LoadPasswordResetTokenPort loadPasswordResetTokenPort;
    @Mock
    private SimulatedEmailPort simulatedEmailPort;
    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    private PasswordService passwordService;
    private User testUser;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService(
                loadUserPort,
                saveUserPort,
                passwordEncoder,
                savePasswordResetTokenPort,
                loadPasswordResetTokenPort,
                simulatedEmailPort,
                saveAuditLogPort
        );

        Role role = new Role(new RoleId(4L), RoleCode.VT_07, "Nhân viên công ty");
        testUser = new User(new UserId(10L), "employee1", "old_hash", role, UserStatus.ACTIVE, null, "employee1@company.com", null, 0L);
    }

    @Test
    void changePassword_success() {
        ChangePasswordCommand command = new ChangePasswordCommand(10L, "OldPass123", "NewPass123", "NewPass123");

        when(loadUserPort.findById(new UserId(10L))).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPass123", "old_hash")).thenReturn(true);
        when(passwordEncoder.matches("NewPass123", "old_hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPass123")).thenReturn("new_hash");

        passwordService.changePassword(command);

        verify(saveUserPort).save(testUser);
        verify(saveAuditLogPort).save(any(AuditLog.class));
        assertEquals("new_hash", testUser.getPasswordHash());
        assertNotNull(testUser.getPasswordChangedAt());
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsException() {
        ChangePasswordCommand command = new ChangePasswordCommand(10L, "WrongOldPass", "NewPass123", "NewPass123");

        when(loadUserPort.findById(new UserId(10L))).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongOldPass", "old_hash")).thenReturn(false);

        assertThrows(InvalidPasswordException.class, () -> passwordService.changePassword(command));
        verify(saveUserPort, never()).save(any());
    }

    @Test
    void changePassword_mismatchedConfirmPassword_throwsException() {
        ChangePasswordCommand command = new ChangePasswordCommand(10L, "OldPass123", "NewPass123", "MismatchPass");

        assertThrows(InvalidPasswordException.class, () -> passwordService.changePassword(command));
    }

    @Test
    void requestPasswordReset_existingUserWithEmail_invalidatesOldTokensAndSendsEmail() {
        RequestPasswordResetCommand command = new RequestPasswordResetCommand("employee1@company.com");

        when(loadUserPort.findByUsernameOrEmail("employee1@company.com")).thenReturn(Optional.of(testUser));

        passwordService.requestPasswordReset(command);

        verify(savePasswordResetTokenPort).invalidateActiveTokensByUserId(testUser.getId());
        verify(savePasswordResetTokenPort).save(any(PasswordResetToken.class));
        verify(simulatedEmailPort).sendPasswordResetEmail(eq("employee1@company.com"), eq("employee1"), anyString(), eq(15L));
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    void requestPasswordReset_userWithoutEmail_invalidatesTokensButDoesNotSendEmail() {
        Role role = new Role(new RoleId(4L), RoleCode.VT_07, "Nhân viên công ty");
        User userWithoutEmail = new User(new UserId(20L), "noemail_user", "hash", role, UserStatus.ACTIVE, null, null, null, 0L);

        RequestPasswordResetCommand command = new RequestPasswordResetCommand("noemail_user");

        when(loadUserPort.findByUsernameOrEmail("noemail_user")).thenReturn(Optional.of(userWithoutEmail));

        passwordService.requestPasswordReset(command);

        verify(savePasswordResetTokenPort).invalidateActiveTokensByUserId(userWithoutEmail.getId());
        verify(savePasswordResetTokenPort).save(any(PasswordResetToken.class));
        verify(simulatedEmailPort, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void resetPassword_validToken_success() {
        String rawToken = "valid_token";
        String hashedToken = PasswordService.hashToken(rawToken);
        PasswordResetToken token = new PasswordResetToken(1L, new UserId(10L), hashedToken, Instant.now().plusSeconds(900), false, Instant.now());
        ResetPasswordCommand command = new ResetPasswordCommand(rawToken, "NewPass123", "NewPass123");

        when(loadPasswordResetTokenPort.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));
        when(loadUserPort.findById(new UserId(10L))).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewPass123")).thenReturn("new_hash");

        passwordService.resetPassword(command);

        assertTrue(token.isUsed());
        verify(savePasswordResetTokenPort).save(token);
        verify(saveUserPort).save(testUser);
        verify(saveAuditLogPort).save(any(AuditLog.class));
        assertEquals("new_hash", testUser.getPasswordHash());
    }

    @Test
    void resetPassword_expiredToken_throwsException() {
        String rawToken = "expired_token";
        String hashedToken = PasswordService.hashToken(rawToken);
        PasswordResetToken expiredToken = new PasswordResetToken(1L, new UserId(10L), hashedToken, Instant.now().minusSeconds(10), false, Instant.now().minusSeconds(1000));
        ResetPasswordCommand command = new ResetPasswordCommand(rawToken, "NewPass123", "NewPass123");

        when(loadPasswordResetTokenPort.findByTokenHash(hashedToken)).thenReturn(Optional.of(expiredToken));

        assertThrows(InvalidResetTokenException.class, () -> passwordService.resetPassword(command));
    }
}
