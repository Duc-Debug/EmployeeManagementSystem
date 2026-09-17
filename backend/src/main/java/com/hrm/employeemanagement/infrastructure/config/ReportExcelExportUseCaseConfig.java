package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.report.excel.ExportProjectAllocationReportExcelUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.report.excel.GenerateExcelWorkbookPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.report.excel.ExportProjectAllocationReportExcelService;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.excel.PoiExcelGeneratorAdapter;

/**
 * Spring Configuration đăng ký các bean cho Use Case Xuất báo cáo file Excel (NCL-10-CN-003).
 * Đảm bảo tầng Application không bị dính annotation @Service hay @Component theo kiến trúc Hexagonal.
 */
@Configuration
public class ReportExcelExportUseCaseConfig {

    @Bean
    public GenerateExcelWorkbookPort generateExcelWorkbookPort() {
        return new PoiExcelGeneratorAdapter();
    }

    @Bean
    public ExportProjectAllocationReportExcelUseCase exportProjectAllocationReportExcelUseCase(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadProjectMemberPort loadProjectMemberPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            GenerateExcelWorkbookPort generateExcelWorkbookPort,
            SaveAuditLogPort saveAuditLogPort,
            LoadOrgUnitPort loadOrgUnitPort
    ) {
        return new ExportProjectAllocationReportExcelService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadProjectPort,
                loadProjectMemberPort,
                loadAllocationPort,
                generateExcelWorkbookPort,
                saveAuditLogPort,
                loadOrgUnitPort
        );
    }
}
