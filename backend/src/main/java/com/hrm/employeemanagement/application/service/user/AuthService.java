package com.hrm.employeemanagement.application.service.user;

import com.hrm.employeemanagement.application.dto.user.AuthTokenResult;
import com.hrm.employeemanagement.application.dto.user.LoginCommand;
import com.hrm.employeemanagement.application.port.inbound.user.AuthenticateUserUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.LogoutUserUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.security.TokenProviderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveUserPort;
import com.hrm.employeemanagement.domain.exception.user.InvalidCredentialsException;
import com.hrm.employeemanagement.domain.exception.user.UserLockedException;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Pure Java 100% Application Service (Zero Spring framework dependencies).
 * Implements AuthenticateUserUseCase and LogoutUserUseCase.
 */
public class AuthService implements AuthenticateUserUseCase, LogoutUserUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;

    public AuthService(LoadUserPort loadUserPort, SaveUserPort saveUserPort, PasswordEncoderPort passwordEncoder, TokenProviderPort tokenProvider) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
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
    public void logout(Long userId) {
        logoutAndReturnUsername(userId);
    }

    public String logoutAndReturnUsername(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = loadUserPort.findById(new UserId(userId)).orElse(null);
        if (user != null) {
            user.logout();
            saveUserPort.save(user);
            return user.getUsername();
        }
        return null;
    }
}
