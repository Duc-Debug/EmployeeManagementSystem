package com.hrm.employeemanagement.application.service.user;

import com.hrm.employeemanagement.application.dto.user.AuthTokenResult;
import com.hrm.employeemanagement.application.dto.user.LoginCommand;
import com.hrm.employeemanagement.application.port.inbound.user.AuthenticateUserUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.security.TokenProviderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.domain.exception.user.InvalidCredentialsException;
import com.hrm.employeemanagement.domain.exception.user.UserLockedException;
import com.hrm.employeemanagement.domain.user.User;

/**
 * Pure Java 100% Application Service (Zero Spring framework dependencies).
 * Implements AuthenticateUserUseCase.
 */
public class AuthService implements AuthenticateUserUseCase {

    private final LoadUserPort loadUserPort;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;

    public AuthService(LoadUserPort loadUserPort, PasswordEncoderPort passwordEncoder, TokenProviderPort tokenProvider) {
        this.loadUserPort = loadUserPort;
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
}
