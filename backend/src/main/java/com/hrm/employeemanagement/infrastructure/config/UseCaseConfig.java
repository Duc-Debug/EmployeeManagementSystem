package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.user.AuthenticateUserUseCase;
import com.hrm.employeemanagement.application.port.outbound.email.SimulatedEmailPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.PermissionQueryPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.security.TokenProviderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadPasswordResetTokenPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadRolePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SavePasswordResetTokenPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.user.AuthService;
import com.hrm.employeemanagement.application.service.user.PasswordService;
import com.hrm.employeemanagement.application.service.user.UserService;
import com.hrm.employeemanagement.infrastructure.security.UserStatusCache;
import com.hrm.employeemanagement.infrastructure.transaction.user.TransactionalPasswordServiceDecorator;
import com.hrm.employeemanagement.infrastructure.transaction.user.TransactionalUserServiceDecorator;

@Configuration
public class UseCaseConfig {

    @Bean
    public TransactionalUserServiceDecorator userService(LoadUserPort loadUserPort,
                                                         SaveUserPort saveUserPort,
                                                         LoadRolePort loadRolePort,
                                                         LoadEmployeePort loadEmployeePort,
                                                         SaveEmployeePort saveEmployeePort,
                                                         SaveAuditLogPort saveAuditLogPort,
                                                         SaveAuditLogInNewTransactionPort deniedAuditLogPort,
                                                         LoadOrgUnitPort loadOrgUnitPort,
                                                         PasswordEncoderPort passwordEncoder,
                                                         UserStatusCache userStatusCache,
                                                          AuthorizationService authorizationService) {
      UserService pureJavaUserService = new UserService(
        loadUserPort,
        saveUserPort,
        loadRolePort,
        loadEmployeePort,
        saveEmployeePort,
        saveAuditLogPort,
        deniedAuditLogPort,
        loadOrgUnitPort,
        passwordEncoder,
        authorizationService
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

    @Bean
    public AuthorizationService authorizationService(GetAuthenticatedUserPort authenticatedUserPort,
                                                     PermissionQueryPort permissionQueryPort,
                                                     SaveAuditLogInNewTransactionPort deniedAuditLogPort) {
        return new AuthorizationService(authenticatedUserPort, permissionQueryPort, deniedAuditLogPort);
    }
}
