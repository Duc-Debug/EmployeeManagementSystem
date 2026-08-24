package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.port.inbound.user.AuthenticateUserUseCase;
import com.hrm.employeemanagement.application.port.outbound.email.SimulatedEmailPort;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.security.TokenProviderPort;
import com.hrm.employeemanagement.application.port.outbound.user.*;
import com.hrm.employeemanagement.application.service.user.AuthService;
import com.hrm.employeemanagement.application.service.user.PasswordService;
import com.hrm.employeemanagement.application.service.user.UserService;
import com.hrm.employeemanagement.infrastructure.security.UserStatusCache;
import com.hrm.employeemanagement.infrastructure.transaction.user.TransactionalPasswordServiceDecorator;
import com.hrm.employeemanagement.infrastructure.transaction.user.TransactionalUserServiceDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public TransactionalUserServiceDecorator userService(LoadUserPort loadUserPort,
                                                         SaveUserPort saveUserPort,
                                                         LoadRolePort loadRolePort,
                                                         LoadEmployeePort loadEmployeePort,
                                                         SaveEmployeePort saveEmployeePort,
                                                         SaveAuditLogPort saveAuditLogPort,
                                                         LoadDepartmentPort loadDepartmentPort,
                                                         PasswordEncoderPort passwordEncoder,
                                                         UserStatusCache userStatusCache) {
        UserService pureJavaUserService = new UserService(
                loadUserPort,
                saveUserPort,
                loadRolePort,
                loadEmployeePort,
                saveEmployeePort,
                saveAuditLogPort,
                loadDepartmentPort,
                passwordEncoder
        );
        return new TransactionalUserServiceDecorator(pureJavaUserService, userStatusCache);
    }

    @Bean
    public AuthenticateUserUseCase authService(LoadUserPort loadUserPort,
                                              PasswordEncoderPort passwordEncoder,
                                              TokenProviderPort tokenProvider) {
        return new AuthService(loadUserPort, passwordEncoder, tokenProvider);
    }

    @Bean
    public TransactionalPasswordServiceDecorator passwordService(LoadUserPort loadUserPort,
                                                                 SaveUserPort saveUserPort,
                                                                 PasswordEncoderPort passwordEncoder,
                                                                 SavePasswordResetTokenPort savePasswordResetTokenPort,
                                                                 LoadPasswordResetTokenPort loadPasswordResetTokenPort,
                                                                 SimulatedEmailPort simulatedEmailPort,
                                                                 SaveAuditLogPort saveAuditLogPort,
                                                                 UserStatusCache userStatusCache) {
        PasswordService pureJavaPasswordService = new PasswordService(
                loadUserPort,
                saveUserPort,
                passwordEncoder,
                savePasswordResetTokenPort,
                loadPasswordResetTokenPort,
                simulatedEmailPort,
                saveAuditLogPort
        );
        return new TransactionalPasswordServiceDecorator(pureJavaPasswordService, loadUserPort, userStatusCache);
    }
}
