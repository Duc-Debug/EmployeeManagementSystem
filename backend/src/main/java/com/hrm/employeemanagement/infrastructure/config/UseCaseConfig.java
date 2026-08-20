package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.service.user.AuthService;
import com.hrm.employeemanagement.application.service.user.UserService;
import com.hrm.employeemanagement.domain.repository.user.AuditLogRepository;
import com.hrm.employeemanagement.domain.repository.user.EmployeeRepository;
import com.hrm.employeemanagement.domain.repository.user.RoleRepository;
import com.hrm.employeemanagement.domain.repository.user.UserRepository;
import com.hrm.employeemanagement.infrastructure.security.UserStatusCache;
import com.hrm.employeemanagement.port.in.user.AuthenticateUserUseCase;
import com.hrm.employeemanagement.port.out.user.PasswordEncoderPort;
import com.hrm.employeemanagement.port.out.user.TokenProviderPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public TransactionalUserServiceDecorator userService(UserRepository userRepository,
                                                         RoleRepository roleRepository,
                                                         EmployeeRepository employeeRepository,
                                                         AuditLogRepository auditLogRepository,
                                                         PasswordEncoderPort passwordEncoder,
                                                         UserStatusCache userStatusCache) {
        UserService pureJavaUserService = new UserService(userRepository, roleRepository, employeeRepository, auditLogRepository, passwordEncoder);
        return new TransactionalUserServiceDecorator(pureJavaUserService, userStatusCache);
    }

    @Bean
    public AuthenticateUserUseCase authService(UserRepository userRepository,
                                              PasswordEncoderPort passwordEncoder,
                                              TokenProviderPort tokenProvider) {
        return new AuthService(userRepository, passwordEncoder, tokenProvider);
    }
}
