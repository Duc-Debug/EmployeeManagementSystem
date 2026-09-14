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
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.template.ProjectRoleAllocationTemplate;
import com.hrm.employeemanagement.domain.allocation.template.ProjectRoleAllocationTemplateItem;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidRoleAllocationTemplateException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.project.demand.ProjectResourceDemand;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.User;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
    private LoadOrgUnitPort loadOrgUnitPort;
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
    private Project sourceProject;

    @BeforeEach
    void setUp() {
        service = new RoleAllocationTemplateService(
                authorizationService,
                loadUserPort,
                saveTemplatePort,
                loadTemplatePort,
                loadStructurePort,
                loadProjectPort,
                loadOrgUnitPort,
                loadRolePort,
                loadEmployeePort,
                loadWeeklyAvailabilityPort,
                loadAllocationPort,
                saveAllocationPort,
                loadDemandPort,
                saveDemandPort,
                saveAuditLogPort
        );

        User currentUser = mock(User.class);
        lenient().when(loadUserPort.findById(new UserId(99L))).thenReturn(Optional.of(currentUser));
        lenient().when(currentUser.getDataScope()).thenReturn(DataScope.COMPANY);

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

        sourceProject = new Project(
                new ProjectId(100L),
                "PRJ-SRC",
                "Dự án nguồn",
                1L,
                new EmployeeId(1L),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(160.0),
                "Dự án nguồn",
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

        when(loadProjectPort.findById(new ProjectId(100L))).thenReturn(Optional.of(sourceProject));
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
        Employee empDev = new Employee(new EmployeeId(101L), new UserId(1L), 1L,
                "EMP01", "Dev Nguyen", "DEV", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(empDev));

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

    @Test
    @DisplayName("Đảm bảo tính idempotent khi áp dụng lại mẫu nhiều lần")
    void applyTemplate_RepeatedRequest_ShouldRemainIdempotent() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(99L);
        ProjectRoleAllocationTemplate template = new ProjectRoleAllocationTemplate(
                1L, "TPL_DEV", "Dev", null, null, 99L, null, null, 0L,
                List.of(new ProjectRoleAllocationTemplateItem(10L, 1L, 1L, BigDecimal.valueOf(40))));
        Employee employee = new Employee(new EmployeeId(101L), new UserId(1L), 1L,
                "EMP01", "Dev Nguyen", "DEV", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        YearWeek week = YearWeek.from(targetProject.getStartDate());
        ProjectResourceDemand existingDemand = ProjectResourceDemand.createNew(
                targetProject.getId(), devRole.getId(), week, BigDecimal.valueOf(10));
        WeeklyProjectAllocation existingAllocation = new WeeklyProjectAllocation(
                1L, 101L, targetProject.getId().value(), week, BigDecimal.valueOf(40));
        existingAllocation.noteVariance("[ROLE_TEMPLATE:1:40.00]", 99L);

        when(loadTemplatePort.findById(1L)).thenReturn(Optional.of(template));
        when(loadProjectPort.findById(targetProject.getId())).thenReturn(Optional.of(targetProject));
        when(loadRolePort.findAll()).thenReturn(List.of(devRole));
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(employee));
        when(loadDemandPort.findByProjectIdAndRoleIdAndYearWeek(any(), any(), any()))
                .thenReturn(Optional.of(existingDemand));
        when(loadAllocationPort.loadAllocation(any(), any(), any())).thenReturn(Optional.of(existingAllocation));

        ApplyRoleAllocationTemplateCommand command = new ApplyRoleAllocationTemplateCommand(1L, 200L,
                List.of(new ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand(
                        1L, 101L, BigDecimal.valueOf(40))));

        service.applyTemplate(command);
        service.applyTemplate(command);

        assertThat(existingDemand.getRequiredHours()).isEqualByComparingTo("40");
        assertThat(existingAllocation.getAllocatedHours()).isEqualByComparingTo("40");
    }

    @Test
    @DisplayName("Case A: Áp template bảo tồn allocation thủ công hiện hữu của nhân sự")
    void applyTemplate_ExistingManualAllocation_ShouldPreserveManualHoursAndAddTemplateHours() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(99L);
        ProjectRoleAllocationTemplate template = new ProjectRoleAllocationTemplate(
                1L, "TPL_DEV", "Dev", null, null, 99L, null, null, 0L,
                List.of(new ProjectRoleAllocationTemplateItem(10L, 1L, 1L, BigDecimal.valueOf(10))));
        Employee employee = new Employee(new EmployeeId(101L), new UserId(1L), 1L,
                "EMP01", "Dev Nguyen", "DEV", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        YearWeek week = YearWeek.from(targetProject.getStartDate());

        WeeklyProjectAllocation manualAllocation = new WeeklyProjectAllocation(
                1L, 101L, targetProject.getId().value(), week, BigDecimal.valueOf(30));

        when(loadTemplatePort.findById(1L)).thenReturn(Optional.of(template));
        when(loadProjectPort.findById(targetProject.getId())).thenReturn(Optional.of(targetProject));
        when(loadRolePort.findAll()).thenReturn(List.of(devRole));
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(employee));
        when(loadDemandPort.findByProjectIdAndRoleIdAndYearWeek(any(), any(), any())).thenReturn(Optional.empty());
        when(loadAllocationPort.loadAllocation(eq(101L), any(), any())).thenReturn(Optional.of(manualAllocation));

        ApplyRoleAllocationTemplateCommand command = new ApplyRoleAllocationTemplateCommand(1L, 200L,
                List.of(new ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand(
                        1L, 101L, BigDecimal.valueOf(10))));

        service.applyTemplate(command);

        assertThat(manualAllocation.getAllocatedHours()).isEqualByComparingTo("40");
        assertThat(manualAllocation.getVarianceNote()).contains("[ROLE_TEMPLATE:1:10.00]");
    }

    @Test
    @DisplayName("Case B: Đổi assignee khi apply lại template thì gỡ allocation mẫu của assignee cũ")
    void applyTemplate_ReassignAssignee_ShouldReconcilePreviousAssigneeAllocation() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(99L);
        ProjectRoleAllocationTemplate template = new ProjectRoleAllocationTemplate(
                1L, "TPL_DEV", "Dev", null, null, 99L, null, null, 0L,
                List.of(new ProjectRoleAllocationTemplateItem(10L, 1L, 1L, BigDecimal.valueOf(20))));
        Employee employeeA = new Employee(new EmployeeId(101L), new UserId(1L), 1L,
                "EMP01", "Dev A", "DEV", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        Employee employeeB = new Employee(new EmployeeId(102L), new UserId(2L), 1L,
                "EMP02", "Dev B", "DEV", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        YearWeek week = YearWeek.from(targetProject.getStartDate());

        WeeklyProjectAllocation allocA = new WeeklyProjectAllocation(
                1L, 101L, targetProject.getId().value(), week, BigDecimal.valueOf(20));
        allocA.noteVariance("[ROLE_TEMPLATE:1:20.00]", 99L);

        when(loadTemplatePort.findById(1L)).thenReturn(Optional.of(template));
        when(loadProjectPort.findById(targetProject.getId())).thenReturn(Optional.of(targetProject));
        when(loadRolePort.findAll()).thenReturn(List.of(devRole));
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(employeeA, employeeB));
        when(loadDemandPort.findByProjectIdAndRoleIdAndYearWeek(any(), any(), any())).thenReturn(Optional.empty());

        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(any(), any(), any(), any()))
                .thenReturn(List.of(allocA));
        lenient().when(loadAllocationPort.loadAllocation(eq(101L), any(), any())).thenReturn(Optional.of(allocA));
        lenient().when(loadAllocationPort.loadAllocation(eq(102L), any(), any())).thenReturn(Optional.empty());

        ApplyRoleAllocationTemplateCommand command = new ApplyRoleAllocationTemplateCommand(1L, 200L,
                List.of(new ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand(
                        1L, 102L, BigDecimal.valueOf(20))));

        service.applyTemplate(command);

        assertThat(allocA.getAllocatedHours()).isEqualByComparingTo("0");

        ArgumentCaptor<WeeklyProjectAllocation> captor = ArgumentCaptor.forClass(WeeklyProjectAllocation.class);
        verify(saveAllocationPort, atLeastOnce()).save(captor.capture());
        List<WeeklyProjectAllocation> savedAllocs = captor.getAllValues();
        WeeklyProjectAllocation allocB = savedAllocs.stream()
                .filter(a -> Long.valueOf(102L).equals(a.getEmployeeId()))
                .findFirst().orElseThrow();
        assertThat(allocB.getAllocatedHours()).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("MEDIUM-HIGH: Không throw NPE trong validateCapacity khi employee.standardHoursPerWeek = null và chưa có WeeklyAvailability")
    void validateCapacity_NullStandardHoursAndMissingAvailability_ShouldNotThrowNPE() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(99L);
        ProjectRoleAllocationTemplate template = new ProjectRoleAllocationTemplate(
                1L, "TPL_DEV", "Dev", null, null, 99L, null, null, 0L,
                List.of(new ProjectRoleAllocationTemplateItem(10L, 1L, 1L, BigDecimal.valueOf(20))));
        Employee employee = new Employee(new EmployeeId(101L), new UserId(1L), 1L,
                "EMP01", "Dev Nguyen", "DEV", LocalDate.now(), null, false, null, EmployeeStatus.ACTIVE);

        when(loadTemplatePort.findById(1L)).thenReturn(Optional.of(template));
        when(loadProjectPort.findById(targetProject.getId())).thenReturn(Optional.of(targetProject));
        when(loadRolePort.findAll()).thenReturn(List.of(devRole));
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(employee));
        when(loadDemandPort.findByProjectIdAndRoleIdAndYearWeek(any(), any(), any())).thenReturn(Optional.empty());
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(any(), any())).thenReturn(Optional.empty());
        when(loadAllocationPort.loadAllocation(any(), any(), any())).thenReturn(Optional.empty());

        ApplyRoleAllocationTemplateCommand command = new ApplyRoleAllocationTemplateCommand(1L, 200L,
                List.of(new ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand(
                        1L, 101L, BigDecimal.valueOf(20))));

        ApplyRoleAllocationTemplateResult result = service.applyTemplate(command);
        assertThat(result.allocatedEmployeesCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Từ chối số giờ do client sửa khác với template trước khi ghi dữ liệu")
    void applyTemplate_TamperedHours_ShouldBeRejectedBeforeWrite() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(99L);
        ProjectRoleAllocationTemplate template = new ProjectRoleAllocationTemplate(
                1L, "TPL_DEV", "Dev", null, null, 99L, null, null, 0L,
                List.of(new ProjectRoleAllocationTemplateItem(10L, 1L, 1L, BigDecimal.valueOf(40))));
        when(loadTemplatePort.findById(1L)).thenReturn(Optional.of(template));
        when(loadProjectPort.findById(targetProject.getId())).thenReturn(Optional.of(targetProject));
        when(loadRolePort.findAll()).thenReturn(List.of(devRole));

        ApplyRoleAllocationTemplateCommand command = new ApplyRoleAllocationTemplateCommand(1L, 200L,
                List.of(new ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand(
                        1L, 101L, BigDecimal.valueOf(80))));

        assertThatThrownBy(() -> service.applyTemplate(command))
                .isInstanceOf(InvalidRoleAllocationTemplateException.class);
        verify(saveDemandPort, never()).save(any());
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("Preview không gợi ý tổng số giờ vượt capacity khi nhiều vai trò dùng chung candidate pool")
    void previewSuggestion_SharedCandidates_ShouldNotExceedAggregateCapacity() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(99L);

        ProjectRole role1 = new ProjectRole(new ProjectRoleId(11L), "ENG_1", "ENGINEER", null);
        ProjectRole role2 = new ProjectRole(new ProjectRoleId(12L), "ENG_2", "ENGINEER", null);
        ProjectRole role3 = new ProjectRole(new ProjectRoleId(13L), "ENG_3", "ENGINEER", null);
        ProjectRoleAllocationTemplate template = new ProjectRoleAllocationTemplate(
                1L, "TPL_ENGINEERING", "Engineering", null, null, 99L, null, null, 0L,
                List.of(
                        new ProjectRoleAllocationTemplateItem(21L, 1L, 11L, BigDecimal.valueOf(30)),
                        new ProjectRoleAllocationTemplateItem(22L, 1L, 12L, BigDecimal.valueOf(30)),
                        new ProjectRoleAllocationTemplateItem(23L, 1L, 13L, BigDecimal.valueOf(30))));
        Employee employee1 = new Employee(new EmployeeId(101L), new UserId(1L), 1L,
                "EMP01", "Engineer One", "ENGINEER", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        Employee employee2 = new Employee(new EmployeeId(102L), new UserId(2L), 1L,
                "EMP02", "Engineer Two", "ENGINEER", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);

        when(loadTemplatePort.findById(1L)).thenReturn(Optional.of(template));
        when(loadProjectPort.findById(targetProject.getId())).thenReturn(Optional.of(targetProject));
        when(loadRolePort.findAll()).thenReturn(List.of(role1, role2, role3));
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(employee1, employee2));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());

        PreviewRoleAllocationResult preview = service.previewSuggestion(1L, targetProject.getId().value());

        assertThat(preview.suggestions()).hasSize(3);
        assertThat(preview.suggestions().stream().filter(suggestion -> suggestion.assigned()).count()).isEqualTo(2);
        assertThat(preview.suggestions().stream()
                .filter(suggestion -> suggestion.assigned())
                .map(suggestion -> suggestion.suggestedEmployeeId()))
                .containsExactlyInAnyOrder(101L, 102L);
        assertThat(preview.hasUnassignedRoles()).isTrue();
    }

    @Test
    @DisplayName("Chặn truy cập khi dự án nằm ngoài phạm vi dữ liệu (ORGANIZATION_BRANCH)")
    void whenProjectOutOfDataScope_ShouldThrowPermissionDeniedException() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(99L);
        User branchUser = mock(User.class);
        when(loadUserPort.findById(new UserId(99L))).thenReturn(Optional.of(branchUser));
        when(branchUser.getDataScope()).thenReturn(DataScope.ORGANIZATION_BRANCH);
        when(branchUser.getScopeOrgUnitId()).thenReturn(10L);
        when(loadProjectPort.findById(new ProjectId(200L))).thenReturn(Optional.of(targetProject));
        when(loadProjectPort.existsInOrgUnitBranch(200L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> service.getStructureFromProject(200L))
                .isInstanceOf(PermissionDeniedException.class);
    }
}
