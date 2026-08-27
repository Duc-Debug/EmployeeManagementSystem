package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.employee.EmployeeProfileService;
import com.hrm.employeemanagement.infrastructure.transaction.employee.TransactionalEmployeeProfileServiceDecorator;

@Configuration
public class EmployeeProfileUseCaseConfig {

    @Bean
    public TransactionalEmployeeProfileServiceDecorator employeeProfileService(LoadEmployeePort loadEmployeePort,
                                                         SaveEmployeePort saveEmployeePort,
                                                         LoadUserPort loadUserPort,
                                                         LoadOrgUnitPort loadOrgUnitPort,
                                                         AuthorizationService authorizationService) {
        EmployeeProfileService service = new EmployeeProfileService(loadEmployeePort, saveEmployeePort,
                loadUserPort, loadOrgUnitPort, authorizationService);
        return new TransactionalEmployeeProfileServiceDecorator(service);
    }
}
