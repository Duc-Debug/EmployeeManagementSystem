package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.port.inbound.leave.GetDepartmentMonthlyLeaveCalendarUseCase;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadDepartmentMonthlyLeavePort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.leave.DepartmentMonthlyLeaveCalendarService;
import com.hrm.employeemanagement.infrastructure.transaction.leave.TransactionalDepartmentMonthlyLeaveCalendarService;
import com.hrm.employeemanagement.application.port.outbound.calendar.HolidayQueryPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Configuration đăng ký Bean cho chức năng Xem lịch nghỉ bộ phận (NCL-05-CN-006).
 */
@Configuration
public class DepartmentLeaveCalendarConfig {

    @Bean
    public GetDepartmentMonthlyLeaveCalendarUseCase getDepartmentMonthlyLeaveCalendarUseCase(
            LoadOrgUnitPort loadOrgUnitPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadDepartmentMonthlyLeavePort loadDepartmentMonthlyLeavePort,
            AuthorizationService authorizationService,
            SaveAuditLogPort saveAuditLogPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            HolidayQueryPort holidayQueryPort
    ) {
        DepartmentMonthlyLeaveCalendarService pureService = new DepartmentMonthlyLeaveCalendarService(
                loadOrgUnitPort,
                loadEmployeePort,
                loadUserPort,
                loadDepartmentMonthlyLeavePort,
                authorizationService,
                saveAuditLogPort,
                loadWorkingCalendarPort,
                holidayQueryPort
        );

        return new TransactionalDepartmentMonthlyLeaveCalendarService(pureService);
    }
}
