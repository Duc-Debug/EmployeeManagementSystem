package com.hrm.employeemanagement.application.service.scenario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.hrm.employeemanagement.application.dto.scenario.CreateScenarioCommand;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
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
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;
import com.hrm.employeemanagement.domain.scenario.ScenarioAllocationSnapshotItem;
import com.hrm.employeemanagement.domain.scenario.ScenarioStatus;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.domain.role.RoleId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResourceScenarioServiceTest {

    private AuthorizationService authorizationService;
    private LoadUserPort loadUserPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private LoadHolidaysPort loadHolidaysPort;
    private LoadApprovedLeavesPort loadApprovedLeavesPort;
    private LoadWorkingCalendarPort loadWorkingCalendarPort;
    private SaveResourceScenarioPort saveScenarioPort;
    private LoadResourceScenarioPort loadScenarioPort;
    private SaveScenarioSnapshotPort saveSnapshotPort;
    private LoadScenarioSnapshotPort loadSnapshotPort;
    private LoadScenarioDemandPort loadDemandPort;
    private SaveAuditLogPort saveAuditLogPort;

    private ResourceScenarioService service;

    private User vt03User;
    private User vt01User;
    private User vt02User;
    private OrgUnit branchUnit;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadUserPort = mock(LoadUserPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        loadAllocationPort = mock(LoadWeeklyProjectAllocationPort.class);
        loadWeeklyAvailabilityPort = mock(LoadWeeklyAvailabilityPort.class);
        loadHolidaysPort = mock(LoadHolidaysPort.class);
        loadApprovedLeavesPort = mock(LoadApprovedLeavesPort.class);
        loadWorkingCalendarPort = mock(LoadWorkingCalendarPort.class);
        saveScenarioPort = mock(SaveResourceScenarioPort.class);
        loadScenarioPort = mock(LoadResourceScenarioPort.class);
        saveSnapshotPort = mock(SaveScenarioSnapshotPort.class);
        loadSnapshotPort = mock(LoadScenarioSnapshotPort.class);
        loadDemandPort = mock(LoadScenarioDemandPort.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);

        service = new ResourceScenarioService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadAllocationPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort,
                saveScenarioPort,
                loadScenarioPort,
                saveSnapshotPort,
                loadSnapshotPort,
                loadDemandPort,
                saveAuditLogPort
        );

        vt03User = new User(
                new UserId(103L),
                "vt03_user",
                "hash",
                new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực"),
                UserStatus.ACTIVE,
                null,
                DataScope.ORGANIZATION_BRANCH,
                10L,
                1L
        );

        vt01User = new User(
                new UserId(101L),
                "vt01_user",
                "hash",
                new Role(new RoleId(1L), RoleCode.VT_01, "Ban giám đốc"),
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                1L
        );

        vt02User = new User(
                new UserId(102L),
                "vt02_user",
                "hash",
                new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án"),
                UserStatus.ACTIVE,
                null,
                DataScope.SELF,
                null,
                1L
        );

        branchUnit = mock(OrgUnit.class);
        lenient().when(branchUnit.getId()).thenReturn(new OrgUnitId(10L));
        lenient().when(branchUnit.getUnitName()).thenReturn("Phòng Kỹ Thuật");
        lenient().when(branchUnit.getTreePath()).thenReturn("/10/");
    }

    @Test
    @DisplayName("AC-01: VT-03 tạo scenario thành công, chụp snapshot phân bổ thật, status draft")
    void testCreateScenario_VT03_SuccessWithSnapshot() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(103L);
        when(loadUserPort.findById(new UserId(103L))).thenReturn(Optional.of(vt03User));
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);
        when(loadOrgUnitPort.findById(new OrgUnitId(10L))).thenReturn(Optional.of(branchUnit));
        when(loadOrgUnitPort.findSubTree(any())).thenReturn(List.of(branchUnit));

        Employee emp1 = new Employee(
                new EmployeeId(1L),
                new UserId(201L),
                10L,
                "EMP001",
                "Nguyễn Văn A",
                "Dev",
                LocalDate.of(2025, 1, 1),
                null,
                false,
                40,
                com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findActiveByOrgUnitIds(anyList())).thenReturn(List.of(emp1));

        WeeklyProjectAllocation alloc1 = new WeeklyProjectAllocation(
                1001L, 1L, 501L, YearWeek.of(2026, 38), BigDecimal.valueOf(30)
        );
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of(alloc1));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());

        when(saveScenarioPort.save(any(ResourceScenario.class))).thenAnswer(invocation -> {
            ResourceScenario s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        CreateScenarioCommand command = new CreateScenarioCommand(
                "SCN-2026-001",
                "Kịch bản dự án X",
                "Mô phỏng nhận dự án mới",
                10L,
                2026,
                38,
                8
        );

        ScenarioResult result = service.createScenario(command);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("draft", result.status());
        assertEquals(10L, result.orgUnitId());
        assertEquals(1, result.snapshotEmployeesCount());

        // Xác minh snapshot đã được chụp và lưu
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ScenarioAllocationSnapshotItem>> snapshotCaptor = ArgumentCaptor.forClass(List.class);
        verify(saveSnapshotPort, times(1)).saveAll(snapshotCaptor.capture());
        List<ScenarioAllocationSnapshotItem> savedSnapshots = snapshotCaptor.getValue();
        assertEquals(8, savedSnapshots.size()); // 1 nhân sự x 8 tuần

        // Xác minh audit log ghi lại thao tác tạo
        verify(saveAuditLogPort, times(1)).save(argThat(log ->
                "CREATE_SCENARIO".equals(log.getAction()) &&
                "resource_scenarios".equals(log.getTableName()) &&
                Long.valueOf(1L).equals(log.getRecordId())
        ));
    }

    @Test
    @DisplayName("AC-03: VT-01 không được phép tạo kịch bản -> Bị từ chối PermissionDeniedException")
    void testCreateScenario_VT01_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(101L);
        when(loadUserPort.findById(new UserId(101L))).thenReturn(Optional.of(vt01User));

        CreateScenarioCommand command = new CreateScenarioCommand(
                "SCN-01", "Kịch bản test", "Mô tả", 10L, 2026, 38, 8
        );

        assertThrows(PermissionDeniedException.class, () -> service.createScenario(command));

        verify(saveAuditLogPort, times(1)).save(argThat(log ->
                "ACCESS_DENIED_SCENARIO_CREATE".equals(log.getAction())
        ));
    }

    @Test
    @DisplayName("AC-05: VT-03 tạo kịch bản ngoài phạm vi branch -> Bị từ chối PermissionDeniedException")
    void testCreateScenario_VT03_OutOfBranchScope_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(103L);
        when(loadUserPort.findById(new UserId(103L))).thenReturn(Optional.of(vt03User));
        when(loadOrgUnitPort.existsInOrgUnitBranch(99L, 10L)).thenReturn(false);

        CreateScenarioCommand command = new CreateScenarioCommand(
                "SCN-01", "Kịch bản test", "Mô tả", 99L, 2026, 38, 8
        );

        assertThrows(PermissionDeniedException.class, () -> service.createScenario(command));

        verify(saveAuditLogPort, times(1)).save(argThat(log ->
                "ACCESS_DENIED_SCENARIO_CREATE".equals(log.getAction()) &&
                log.getNewValue().contains("ORG_UNIT_OUT_OF_SCOPE")
        ));
    }

    @Test
    @DisplayName("AC-05: VT-03 xem chi tiết kịch bản ngoài branch -> Bị từ chối PermissionDeniedException")
    void testGetScenarioById_VT03_OutOfBranchScope_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_READ)).thenReturn(103L);
        when(loadUserPort.findById(new UserId(103L))).thenReturn(Optional.of(vt03User));

        ResourceScenario outsideScenario = ResourceScenario.createNew(
                "SCN-99", "Kịch bản ngoài branch", "Mô tả", 99L, 2026, 38, 8, 999L
        );
        outsideScenario.setId(99L);
        when(loadScenarioPort.findById(99L)).thenReturn(Optional.of(outsideScenario));
        when(loadOrgUnitPort.existsInOrgUnitBranch(99L, 10L)).thenReturn(false);

        assertThrows(PermissionDeniedException.class, () -> service.getScenarioById(99L));

        verify(saveAuditLogPort, times(1)).save(argThat(log ->
                "ACCESS_DENIED_SCENARIO_VIEW".equals(log.getAction()) &&
                log.getNewValue().contains("SCENARIO_OUT_OF_SCOPE")
        ));
    }

    @Test
    @DisplayName("VT-01 xem kịch bản thành công (phạm vi COMPANY toàn công ty)")
    void testGetScenarioById_VT01_Success() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_READ)).thenReturn(101L);
        when(loadUserPort.findById(new UserId(101L))).thenReturn(Optional.of(vt01User));

        ResourceScenario scenario = ResourceScenario.createNew(
                "SCN-10", "Kịch bản bộ phận 10", "Mô tả", 10L, 2026, 38, 8, 103L
        );
        scenario.setId(10L);
        when(loadScenarioPort.findById(10L)).thenReturn(Optional.of(scenario));
        when(loadOrgUnitPort.findById(new OrgUnitId(10L))).thenReturn(Optional.of(branchUnit));
        when(loadUserPort.findById(new UserId(103L))).thenReturn(Optional.of(vt03User));
        when(loadDemandPort.findByScenarioId(10L)).thenReturn(List.of());
        when(loadSnapshotPort.findByScenarioId(10L)).thenReturn(List.of());

        var detail = service.getScenarioById(10L);
        assertNotNull(detail);
        assertEquals("SCN-10", detail.scenario().code());
    }
}
