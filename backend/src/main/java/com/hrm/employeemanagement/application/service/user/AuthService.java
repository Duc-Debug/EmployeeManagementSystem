package com.hrm.employeemanagement.application.service.user;

import com.hrm.employeemanagement.application.dto.user.AuthTokenResult;
import com.hrm.employeemanagement.application.dto.user.LoginCommand;
import com.hrm.employeemanagement.domain.model.user.User;
import com.hrm.employeemanagement.domain.repository.user.UserRepository;
import com.hrm.employeemanagement.port.in.user.AuthenticateUserUseCase;
import com.hrm.employeemanagement.port.out.user.PasswordEncoderPort;
import com.hrm.employeemanagement.port.out.user.TokenProviderPort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

/**
 * Pure Java 100% Application Service (No Spring Annotations allowed).
 * Implements Use Case port. Bean registration is handled in Infrastructure Configuration.
 */
public class AuthService implements AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoderPort passwordEncoder, TokenProviderPort tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public AuthTokenResult login(LoginCommand command) {
        User user = userRepository.findByUsername(command.username())
                .orElseThrow(() -> new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        if (!user.isActive()) {
            throw new DisabledException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên.");
        }

        String token = tokenProvider.generateToken(user);
        return new AuthTokenResult(token, "Bearer", user.getId(), user.getUsername(), user.getRole().getCode().getCode());
    }
}
