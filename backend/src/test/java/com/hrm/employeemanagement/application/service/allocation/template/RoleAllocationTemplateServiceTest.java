package com.hrm.employeemanagement.application.service.allocation.template;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.allocation.template.ApplyRoleAllocationTemplateCommand;
import com.hrm.employeemanagement.application.dto.allocation.template.ApplyRoleAllocationTemplateResult;
import com.hrm.employeemanagement.application.dto.allocation.template.CreateRoleAllocationTemplateCommand;
import com.hrm.employeemanagement.application.dto.allocation.template.PreviewRoleAllocationResult;
import com.hrm.employeemanagement.application.dto.allocation.template.RoleAllocationTemplateDetailResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.template.LoadProjectRoleAllocationStructurePort;
import com.hrm.employeemanagement.application.port.outbound.allocation.template.LoadRoleAllocationTemplatePort;
import com.hrm.employeemanagement.application.port.outbound.allocation.template.SaveRoleAllocationTemplatePort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.template.ProjectRoleAllocationTemplate;
import com.hrm.employeemanagement.domain.allocation.template.ProjectRoleAllocationTemplateItem;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.user.UserId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleAllocationTemplateService Application Unit Tests (All 4 Scenarios)")
class RoleAllocationTemplateServiceTest {

    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private SaveRoleAllocationTemplatePort saveTemplatePort;
    @Mock
    private LoadRoleAllocationTemplatePort loadTemplatePort;
    @Mock
    private LoadProjectRoleAllocationStructurePort loadStructurePort;
    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private LoadProjectRolePort loadRolePort;
    @Mock
    private LoadEmployeePort loadEmployeePort;
    @Mock
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    @Mock
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    @Mock
    private SaveWeeklyProjectAllocationPort saveAllocationPort;
    @Mock
    private LoadProjectResourceDemandPort loadDemandPort;
    @Mock
    private SaveProjectResourceDemandPort saveDemandPort;
    @Mock
    private SaveAuditLogInNewTransactionPort saveAuditLogPort;

    private RoleAllocationTemplateService service;

    private ProjectRole devRole;
    private ProjectRole testRole;
    private ProjectRole baRole;
    private Project targetProject;

    @BeforeEach
    void setUp() {
        service = new RoleAllocationTemplateService(
                authorizationService,
                loadUserPort,
                saveTemplatePort,
                loadTemplatePort,
                loadStructurePort,
                loadProjectPort,
                loadRolePort,
                loadEmployeePort,
                loadWeeklyAvailabilityPort,
                loadAllocationPort,
                saveAllocationPort,
                loadDemandPort,
                saveDemandPort,
                saveAuditLogPort
        );

        devRole = new ProjectRole(new ProjectRoleId(1L), "DEV", "Developer", "Dev role");
        testRole = new ProjectRole(new ProjectRoleId(2L), "TEST", "Tester", "Test role");
        baRole = new ProjectRole(new ProjectRoleId(3L), "BA", "Business Analyst", "BA role");

        targetProject = new Project(
                new ProjectId(200L),
                "PRJ-NEW",
                "Dự án mới",
                1L,
                new EmployeeId(1L),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 19),
                BigDecimal.valueOf(160.0),
                "Dự án mới",
                ProjectStatus.ACTIVE,
                new UserId(1L),
                null,
                null,
                0L
        );
    }

    @Test
    @DisplayName("Kịch bản 1: Luồng thành công - Lưu mẫu 3 vai trò và áp cho dự án mới, gợi ý đúng nhân sự")
    void scenario1_Success_SaveTemplateAndPreviewApply() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(99L);
        when(loadTemplatePort.existsByCode("TPL_WEB_STANDARD")).thenReturn(false);

        List<ProjectRoleAllocationTemplateItem> templateItems = List.of(
                new ProjectRoleAllocationTemplateItem(10L, 1L, 1L, BigDecimal.valueOf(40.0)),
                new ProjectRoleAllocationTemplateItem(11L, 1L, 2L, BigDecimal.valueOf(20.0)),
                new ProjectRoleAllocationTemplateItem(12L, 1L, 3L, BigDecimal.valueOf(20.0))
        );

        ProjectRoleAllocationTemplate savedTemplate = new ProjectRoleAllocationTemplate(
                1L,
                "TPL_WEB_STANDARD",
                "Mẫu phát triển Web chuẩn",
                "Gồm DEV, TEST, BA",
                100L,
                99L,
                null,
                null,
                0L,
                templateItems
        );

        when(saveTemplatePort.save(any())).thenReturn(savedTemplate);
        when(loadRolePort.findAll()).thenReturn(List.of(devRole, testRole, baRole));

        // 1. Tạo mẫu thành công
        CreateRoleAllocationTemplateCommand createCmd = new CreateRoleAllocationTemplateCommand(
                "TPL_WEB_STANDARD",
                "Mẫu phát triển Web chuẩn",
                "Gồm DEV, TEST, BA",
                100L,
                List.of(
                        new CreateRoleAllocationTemplateCommand.ItemCommand(1L, BigDecimal.valueOf(40.0)),
                        new CreateRoleAllocationTemplateCommand.ItemCommand(2L, BigDecimal.valueOf(20.0)),
                        new CreateRoleAllocationTemplateCommand.ItemCommand(3L, BigDecimal.valueOf(20.0))
                )
        );

        RoleAllocationTemplateDetailResult created = service.createTemplate(createCmd);
        assertThat(created.id()).isEqualTo(1L);
        assertThat(created.items()).hasSize(3);

        // 2. Gợi ý nhân sự cho dự án mới
        when(loadTemplatePort.findById(1L)).thenReturn(Optional.of(savedTemplate));
        when(loadProjectPort.findById(new ProjectId(200L))).thenReturn(Optional.of(targetProject));

        Employee empDev = new Employee(new EmployeeId(101L), new UserId(1L), 1L, "EMP01", "Dev Nguyễn", "DEV", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        Employee empTest = new Employee(new EmployeeId(102L), new UserId(2L), 1L, "EMP02", "Tester Trần", "TEST", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        Employee empBa = new Employee(new EmployeeId(103L), new UserId(3L), 1L, "EMP03", "BA Lê", "BA", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(empDev, empTest, empBa));

        // Cả 3 đều rảnh 40h/tuần
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of(
                new WeeklyAvailability(null, 101L, YearWeek.from(LocalDate.of(2026, 10, 5)), 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40.0)),
                new WeeklyAvailability(null, 102L, YearWeek.from(LocalDate.of(2026, 10, 5)), 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40.0)),
                new WeeklyAvailability(null, 103L, YearWeek.from(LocalDate.of(2026, 10, 5)), 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40.0))
        ));
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());

        PreviewRoleAllocationResult preview = service.previewSuggestion(1L, 200L);
        assertThat(preview.hasUnassignedRoles()).isFalse();
        assertThat(preview.suggestions()).hasSize(3);
        assertThat(preview.suggestions().get(0).suggestedEmployeeId()).isEqualTo(101L);
        assertThat(preview.suggestions().get(1).suggestedEmployeeId()).isEqualTo(102L);
        assertThat(preview.suggestions().get(2).suggestedEmployeeId()).isEqualTo(103L);

        // 3. Áp dụng phân bổ
        ApplyRoleAllocationTemplateCommand applyCmd = new ApplyRoleAllocationTemplateCommand(
                1L,
                200L,
                List.of(
                        new ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand(1L, 101L, BigDecimal.valueOf(40.0)),
                        new ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand(2L, 102L, BigDecimal.valueOf(20.0)),
                        new ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand(3L, 103L, BigDecimal.valueOf(20.0))
                )
        );

        ApplyRoleAllocationTemplateResult applyResult = service.applyTemplate(applyCmd);
        assertThat(applyResult.appliedRolesCount()).isEqualTo(3);
        assertThat(applyResult.allocatedEmployeesCount()).isEqualTo(3);
        assertThat(applyResult.unassignedRolesCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Kịch bản 2: Ngoại lệ - Không tìm được người rảnh cho vai trò -> Để trống và cảnh báo cần bổ sung nhân sự")
    void scenario2_Exception_NoFreeCandidate_ShouldLeaveUnassignedWithWarning() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(99L);

        List<ProjectRoleAllocationTemplateItem> templateItems = List.of(
                new ProjectRoleAllocationTemplateItem(10L, 1L, 1L, BigDecimal.valueOf(40.0))
        );
        ProjectRoleAllocationTemplate template = new ProjectRoleAllocationTemplate(
                1L, "TPL_DEV_ONLY", "Dev Only", "Desc", 100L, 99L, null, null, 0L, templateItems
        );

        when(loadTemplatePort.findById(1L)).thenReturn(Optional.of(template));
        when(loadProjectPort.findById(new ProjectId(200L))).thenReturn(Optional.of(targetProject));
        when(loadRolePort.findAll()).thenReturn(List.of(devRole));

        // Dev Nguyễn chỉ còn rảnh 10h/tuần trong khi vai trò cần 40h/tuần
        Employee empDev = new Employee(new EmployeeId(101L), new UserId(1L), 1L, "EMP01", "Dev Nguyễn", "DEV", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(empDev));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of(
                new WeeklyAvailability(null, 101L, YearWeek.from(LocalDate.of(2026, 10, 5)), 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(10.0))
        ));
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());

        PreviewRoleAllocationResult preview = service.previewSuggestion(1L, 200L);

        assertThat(preview.hasUnassignedRoles()).isTrue();
        assertThat(preview.suggestions().get(0).assigned()).isFalse();
        assertThat(preview.suggestions().get(0).suggestedEmployeeId()).isNull();
        assertThat(preview.suggestions().get(0).warningMessage())
                .isEqualTo("Cần bổ sung nhân sự cho vai trò Developer");
        assertThat(preview.warnings()).contains("Cần bổ sung nhân sự cho vai trò Developer");
    }

    @Test
    @DisplayName("Kịch bản 3: Không có quyền - Người dùng không phải VT-03 -> Chặn và ghi audit log ACCESS_DENIED")
    void scenario3_Unauthorized_NonVT03User_ShouldThrowAndLogAccessDenied() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE));

        assertThatThrownBy(() -> service.getAllTemplates())
                .isInstanceOf(PermissionDeniedException.class);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog logged = auditCaptor.getValue();
        assertThat(logged.getAction()).isEqualTo("ACCESS_DENIED");
        assertThat(logged.getTableName()).isEqualTo("ROLE_ALLOCATION_TEMPLATE");

        verify(loadTemplatePort, never()).findAll();
    }

    @Test
    @DisplayName("Kịch bản 4: Lưu lịch sử - Xác nhận áp mẫu vào dự án -> Ghi nhật ký vào audit_logs")
    void scenario4_AuditHistory_ApplyTemplate_ShouldRecordAuditLog() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(99L);

        List<ProjectRoleAllocationTemplateItem> templateItems = List.of(
                new ProjectRoleAllocationTemplateItem(10L, 1L, 1L, BigDecimal.valueOf(40.0))
        );
        ProjectRoleAllocationTemplate template = new ProjectRoleAllocationTemplate(
                1L, "TPL_DEV_ONLY", "Dev Only", "Desc", 100L, 99L, null, null, 0L, templateItems
        );

        when(loadTemplatePort.findById(1L)).thenReturn(Optional.of(template));
        when(loadProjectPort.findById(new ProjectId(200L))).thenReturn(Optional.of(targetProject));
        when(loadRolePort.findAll()).thenReturn(List.of(devRole));

        ApplyRoleAllocationTemplateCommand command = new ApplyRoleAllocationTemplateCommand(
                1L,
                200L,
                List.of(new ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand(1L, 101L, BigDecimal.valueOf(40.0)))
        );

        service.applyTemplate(command);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog logged = auditCaptor.getValue();
        assertThat(logged.getUserId()).isEqualTo(99L);
        assertThat(logged.getAction()).isEqualTo("APPLY_ROLE_ALLOCATION_TEMPLATE");
        assertThat(logged.getTableName()).isEqualTo("ROLE_ALLOCATION_TEMPLATE");
        assertThat(logged.getRecordId()).isEqualTo(1L);
    }
}
