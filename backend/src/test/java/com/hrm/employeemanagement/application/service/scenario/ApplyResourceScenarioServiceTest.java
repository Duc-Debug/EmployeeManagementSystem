package com.hrm.employeemanagement.application.service.scenario;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioCommand;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioPreviewResult;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.DeleteScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioAlreadyAppliedException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioBaselineStaleException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;
import com.hrm.employeemanagement.domain.scenario.ScenarioAllocationSnapshotItem;
import com.hrm.employeemanagement.domain.scenario.ScenarioDemand;
import com.hrm.employeemanagement.domain.scenario.ScenarioStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplyResourceScenarioService Unit Tests (NCL-08-CN-003)")
class ApplyResourceScenarioServiceTest {

    @Mock private AuthorizationService authorizationService;
    @Mock private LoadUserPort loadUserPort;
    @Mock private LoadEmployeePort loadEmployeePort;
    @Mock private LoadOrgUnitPort loadOrgUnitPort;
    @Mock private LoadProjectPort loadProjectPort;
    @Mock private LoadResourceScenarioPort loadScenarioPort;
    @Mock private SaveResourceScenarioPort saveScenarioPort;
    @Mock private LoadScenarioDemandPort loadDemandPort;
    @Mock private LoadScenarioSnapshotPort loadSnapshotPort;
    @Mock private SaveScenarioSnapshotPort saveSnapshotPort;
    @Mock private DeleteScenarioSnapshotPort deleteSnapshotPort;
    @Mock private LoadWeeklyProjectAllocationPort loadAllocationPort;
    @Mock private SaveWeeklyProjectAllocationPort saveAllocationPort;
    @Mock private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    @Mock private LoadHolidaysPort loadHolidaysPort;
    @Mock private LoadApprovedLeavesPort loadApprovedLeavesPort;
    @Mock private LoadWorkingCalendarPort loadWorkingCalendarPort;
    @Mock private SaveAuditLogPort saveAuditLogPort;

    private ApplyResourceScenarioService service;

    private final Long rmUserId = 100L;
    private final Long orgUnitId = 10L;
    private final Long projectId = 200L;
    private final Long scenarioId = 1L;
    private final Long empId1 = 1001L;

    private User rmUser;
    private ResourceScenario draftScenario;
    private Project project;
    private Employee emp1;
    private ScenarioDemand demand;
    private ScenarioAllocationSnapshotItem snapshotItem;

    @BeforeEach
    void setUp() {
        service = new ApplyResourceScenarioService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadProjectPort,
                loadScenarioPort,
                saveScenarioPort,
                loadDemandPort,
                loadSnapshotPort,
                saveSnapshotPort,
                deleteSnapshotPort,
                loadAllocationPort,
                saveAllocationPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort,
                saveAuditLogPort
        );

        rmUser = new User(
                new UserId(rmUserId),
                "resourcemanager",
                "rm@example.com",
                new com.hrm.employeemanagement.domain.role.Role(new com.hrm.employeemanagement.domain.role.RoleId(3L), com.hrm.employeemanagement.domain.role.RoleCode.VT_03, "Quản lý nguồn lực"),
                com.hrm.employeemanagement.domain.user.UserStatus.ACTIVE,
                null,
                DataScope.ORGANIZATION_BRANCH,
                orgUnitId,
                1L
        );

        draftScenario = new ResourceScenario(
                scenarioId,
                "SCN-2026-001",
                "Kịch bản thử nghiệm dự án Alpha",
                "Mô tả kịch bản",
                orgUnitId,
                ScenarioStatus.DRAFT,
                2026,
                38,
                2,
                LocalDateTime.now().minusHours(2),
                rmUserId,
                LocalDateTime.now().minusHours(2),
                null,
                0L
        );

        project = new Project(
                new ProjectId(projectId),
                "PRJ-ALPHA",
                "Dự án Alpha",
                orgUnitId,
                null,
                null,
                null,
                BigDecimal.ZERO,
                "Mô tả",
                com.hrm.employeemanagement.domain.project.ProjectStatus.ACTIVE,
                new UserId(1L),
                LocalDateTime.now(),
                null,
                0L
        );

        emp1 = new Employee(
                new EmployeeId(empId1),
                new UserId(1L),
                orgUnitId,
                "EMP-1001",
                "Nguyễn Văn A",
                "Java Developer",
                null,
                null,
                false,
                40,
                com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE
        );

        demand = ScenarioDemand.create(
                scenarioId,
                "Nhu cầu Java Backend",
                1,
                2026,
                38,
                2026,
                39,
                BigDecimal.valueOf(20),
                "Java Developer"
        );

        snapshotItem = ScenarioAllocationSnapshotItem.create(
                scenarioId,
                empId1,
                2026,
                38,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(40)
        );
    }

    @Test
    @DisplayName("TC-01: Luồng thành công - Áp dụng kịch bản cập nhật weekly_project_allocations, đổi status sang applied và lưu audit log")
    void testApplyScenario_Success_TC01() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadUserPort.findById(new UserId(rmUserId))).thenReturn(Optional.of(rmUser));
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(draftScenario));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(project));
        when(loadSnapshotPort.findByScenarioId(scenarioId)).thenReturn(List.of(snapshotItem));
        when(loadDemandPort.findByScenarioId(scenarioId)).thenReturn(List.of(demand));
        when(loadEmployeePort.findAllByIdIn(any())).thenReturn(List.of(emp1));

        // Phân bổ thật hiện tại khớp với snapshot (TC-02 pass: 10h)
        WeeklyProjectAllocation existingAlloc = WeeklyProjectAllocation.createNew(empId1, 999L, YearWeek.of(2026, 38), BigDecimal.valueOf(10));
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of(existingAlloc));

        // Chưa có phân bổ trên dự án mục tiêu projectId
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(eq(projectId), any(), any(), any())).thenReturn(List.of());
        when(saveScenarioPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApplyScenarioCommand command = new ApplyScenarioCommand(scenarioId, projectId, "Áp dụng sau khi ký hợp đồng");
        ApplyScenarioResult result = service.applyScenario(command);

        assertThat(result.scenarioId()).isEqualTo(scenarioId);
        assertThat(result.targetProjectId()).isEqualTo(projectId);
        assertThat(result.status()).isEqualTo("applied");
        assertThat(result.appliedAllocationsCount()).isGreaterThan(0);
        assertThat(draftScenario.getStatus()).isEqualTo(ScenarioStatus.APPLIED);
        assertThat(draftScenario.getTargetProjectId()).isEqualTo(projectId);

        // Verify save weekly allocation called
        verify(saveAllocationPort, atLeastOnce()).save(any(WeeklyProjectAllocation.class));

        // Verify audit log saved (TC-04)
        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(logCaptor.capture());
        AuditLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getAction()).isEqualTo("APPLY_SCENARIO_TO_REAL_ALLOCATION");
        assertThat(savedLog.getRecordId()).isEqualTo(scenarioId);
    }

    @Test
    @DisplayName("TC-02: Ngoại lệ - Dữ liệu gốc đã đổi sau khi tạo kịch bản -> Ném ScenarioBaselineStaleException")
    void testApplyScenario_BaselineStale_ThrowsException_TC02() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadUserPort.findById(new UserId(rmUserId))).thenReturn(Optional.of(rmUser));
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(draftScenario));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(project));
        when(loadSnapshotPort.findByScenarioId(scenarioId)).thenReturn(List.of(snapshotItem));
        when(loadDemandPort.findByScenarioId(scenarioId)).thenReturn(List.of(demand));
        when(loadEmployeePort.findAllByIdIn(any())).thenReturn(List.of(emp1));

        // Dữ liệu thật hiện tại là 25h (khác với snapshotItem là 10h)
        WeeklyProjectAllocation changedAlloc = WeeklyProjectAllocation.createNew(empId1, 999L, YearWeek.of(2026, 38), BigDecimal.valueOf(25));
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of(changedAlloc));

        ApplyScenarioCommand command = new ApplyScenarioCommand(scenarioId, projectId, "Áp dụng thử");

        assertThatThrownBy(() -> service.applyScenario(command))
                .isInstanceOf(ScenarioBaselineStaleException.class)
                .hasMessageContaining("Dữ liệu phân bổ thật đã thay đổi sau khi kịch bản được tạo");

        verify(saveAllocationPort, never()).save(any());
        assertThat(draftScenario.getStatus()).isEqualTo(ScenarioStatus.DRAFT);
    }

    @Test
    @DisplayName("TC-02: Preview phát hiện dữ liệu gốc đã đổi -> cờ isBaselineStale=true và trả về staleReasons")
    void testPreviewApplyScenario_BaselineStale_TC02() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadUserPort.findById(new UserId(rmUserId))).thenReturn(Optional.of(rmUser));
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(draftScenario));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(project));
        when(loadSnapshotPort.findByScenarioId(scenarioId)).thenReturn(List.of(snapshotItem));
        when(loadDemandPort.findByScenarioId(scenarioId)).thenReturn(List.of(demand));
        when(loadEmployeePort.findAllByIdIn(any())).thenReturn(List.of(emp1));

        // Phân bổ thật đã đổi thành 20h
        WeeklyProjectAllocation changedAlloc = WeeklyProjectAllocation.createNew(empId1, 999L, YearWeek.of(2026, 38), BigDecimal.valueOf(20));
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of(changedAlloc));
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(eq(projectId), any(), any(), any())).thenReturn(List.of());

        ApplyScenarioPreviewResult preview = service.previewApplyScenario(scenarioId, projectId);

        assertThat(preview.isBaselineStale()).isTrue();
        assertThat(preview.staleReasons()).isNotEmpty();
        assertThat(preview.staleReasons().get(0)).contains("10");
        assertThat(preview.staleReasons().get(0)).contains("20");
    }

    @Test
    @DisplayName("TC-03: Không có quyền - Người dùng không phải Quản lý nguồn lực -> Ném PermissionDeniedException")
    void testApplyScenario_UnauthorizedUser_ThrowsAndLogsAudit_TC03() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_MANAGE));

        ApplyScenarioCommand command = new ApplyScenarioCommand(scenarioId, projectId, "Không có quyền");

        assertThatThrownBy(() -> service.applyScenario(command))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("Kịch bản đã ở trạng thái APPLIED -> Ném ScenarioAlreadyAppliedException")
    void testApplyScenario_AlreadyApplied_ThrowsException() {
        draftScenario.setStatus(ScenarioStatus.APPLIED);
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadUserPort.findById(new UserId(rmUserId))).thenReturn(Optional.of(rmUser));
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(draftScenario));

        ApplyScenarioCommand command = new ApplyScenarioCommand(scenarioId, projectId, "Thử áp dụng lại");

        assertThatThrownBy(() -> service.applyScenario(command))
                .isInstanceOf(ScenarioAlreadyAppliedException.class)
                .hasMessageContaining("đã được áp dụng");
    }

    @Test
    @DisplayName("Làm mới snapshot kịch bản (Refresh baseline) -> Xóa snapshot cũ, lưu snapshot mới và cập nhật baseSnapshotAt")
    void testRefreshScenarioBaseline_Success() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadUserPort.findById(new UserId(rmUserId))).thenReturn(Optional.of(rmUser));
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(draftScenario));
        OrgUnit mockOrg = mock(OrgUnit.class);
        lenient().when(mockOrg.getId()).thenReturn(new OrgUnitId(orgUnitId));
        lenient().when(mockOrg.getUnitName()).thenReturn("Phòng Kỹ Thuật");
        lenient().when(mockOrg.getTreePath()).thenReturn("/10/");
        when(loadOrgUnitPort.findById(new OrgUnitId(orgUnitId))).thenReturn(Optional.of(mockOrg));
        when(loadOrgUnitPort.findSubTree("/10/")).thenReturn(List.of(mockOrg));
        when(loadEmployeePort.findActiveByOrgUnitIds(any())).thenReturn(List.of(emp1));
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any())).thenReturn(java.util.Map.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(saveScenarioPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ScenarioResult result = service.refreshScenarioBaseline(scenarioId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(scenarioId);
        verify(deleteSnapshotPort).deleteByScenarioId(scenarioId);
        verify(saveSnapshotPort).saveAll(any());
        verify(saveScenarioPort).save(draftScenario);

        // Verify audit log for refresh
        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo("REFRESH_SCENARIO_BASELINE");
    }
}
