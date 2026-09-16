package com.hrm.employeemanagement.application.service.dashboard.capacity;

import com.hrm.employeemanagement.application.dto.dashboard.capacity.CapacityDashboardQuery;
import com.hrm.employeemanagement.application.dto.dashboard.capacity.CapacityDashboardResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.LoadScheduleConflictPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflict;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCapacityDashboardServiceTest {

    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private LoadEmployeePort loadEmployeePort;
    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;
    @Mock
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    @Mock
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    @Mock
    private LoadHolidaysPort loadHolidaysPort;
    @Mock
    private LoadApprovedLeavesPort loadApprovedLeavesPort;
    @Mock
    private LoadWorkingCalendarPort loadWorkingCalendarPort;
    @Mock
    private LoadScheduleConflictPort loadScheduleConflictPort;
    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private LoadProjectMemberPort loadProjectMemberPort;
    @Mock
    private LoadCapacityThresholdPort loadCapacityThresholdPort;
    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    @Mock
    private User executiveUser;
    @Mock
    private OrgUnit itDept;

    private GetCapacityDashboardService service;

    @BeforeEach
    void setUp() {
        service = new GetCapacityDashboardService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadAllocationPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort,
                loadScheduleConflictPort,
                loadProjectPort,
                loadProjectMemberPort,
                loadCapacityThresholdPort,
                saveAuditLogPort
        );

        lenient().when(executiveUser.getId()).thenReturn(new UserId(1L));
        lenient().when(executiveUser.getDataScope()).thenReturn(DataScope.COMPANY);
        lenient().when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(executiveUser));

        lenient().when(itDept.getId()).thenReturn(new OrgUnitId(10L));
        lenient().when(itDept.getUnitName()).thenReturn("Phòng Phần mềm");
        lenient().when(itDept.getTreePath()).thenReturn("/1/10");
        lenient().when(loadOrgUnitPort.findById(new OrgUnitId(10L))).thenReturn(Optional.of(itDept));
        lenient().when(loadOrgUnitPort.findAllByIdIn(anyList())).thenReturn(List.of(itDept));
    }

    @Test
    @DisplayName("TC-01: Luồng thành công - Đã có dữ liệu phân bổ 8 tuần tới -> Hiện đủ 5 chỉ số chính và dữ liệu chi tiết")
    void shouldReturnCompleteMetricsForSelectedPeriod_Success() {
        // Given
        when(authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ)).thenReturn(1L);

        Employee emp1 = createEmployee(101L, "EMP001", "Nguyễn Văn A", 10L, 40);
        Employee emp2 = createEmployee(102L, "EMP002", "Trần Thị B", 10L, 40);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp1, emp2));

        // emp1 có phân bổ 50h/tuần (quá tải > 40h), emp2 có phân bổ 20h/tuần
        List<WeeklyProjectAllocation> allocations = new ArrayList<>();
        allocations.add(new WeeklyProjectAllocation(1L, 101L, 1L, YearWeek.of(2026, 38), BigDecimal.valueOf(50.0)));
        allocations.add(new WeeklyProjectAllocation(2L, 102L, 1L, YearWeek.of(2026, 38), BigDecimal.valueOf(20.0)));
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(allocations);
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());

        // 1 conflict unresolved
        ScheduleConflict conflict = ScheduleConflict.create(
                101L, 2026, 38, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(50.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(10.0), "Xung đột lịch"
        );
        conflict.setId(1L);
        when(loadScheduleConflictPort.findConflicts(anyInt(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(List.of(conflict));

        // 1 active project
        Project activeProject = new Project(
                new ProjectId(1L), "PRJ-01", "Dự án Alpha", 10L, new EmployeeId(101L),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), BigDecimal.valueOf(1000), "Mô tả",
                ProjectStatus.ACTIVE, new UserId(1L), LocalDateTime.now(), LocalDateTime.now(), 0L
        );
        when(loadProjectPort.findAll(anyInt(), anyInt())).thenReturn(List.of(activeProject));

        // When
        CapacityDashboardQuery query = new CapacityDashboardQuery(null, 2026, 38, 8);
        CapacityDashboardResult result = service.execute(query);

        // Then: Kiểm tra 5 chỉ số cốt lõi
        assertThat(result).isNotNull();
        assertThat(result.fromYear()).isEqualTo(2026);
        assertThat(result.fromWeek()).isEqualTo(38);
        assertThat(result.durationWeeks()).isEqualTo(8);
        assertThat(result.averageCapacityUtilization()).isNotNull();
        assertThat(result.averageCapacityUtilization().compareTo(BigDecimal.ZERO)).isGreaterThan(0);
        assertThat(result.overloadedEmployeesCount()).isEqualTo(1); // emp1 quá tải
        assertThat(result.departmentFreeHours().compareTo(BigDecimal.ZERO)).isGreaterThan(0);
        assertThat(result.unresolvedScheduleConflictsCount()).isEqualTo(1); // 1 conflict
        assertThat(result.activeProjectsCount()).isEqualTo(1); // 1 active project

        // Kiểm tra weekly metrics, department breakdown, overloaded employee list
        assertThat(result.weeklyMetrics()).hasSize(8);
        assertThat(result.overloadedEmployees()).hasSize(1);
        assertThat(result.overloadedEmployees().get(0).employeeCode()).isEqualTo("EMP001");
        assertThat(result.unresolvedConflicts()).hasSize(1);
        assertThat(result.activeProjects()).hasSize(1);

        // Verify audit log được ghi nhận (TC-04)
        verify(saveAuditLogPort, times(1)).save(argThat(log ->
                "CAPACITY_DASHBOARD_VIEWED".equals(log.getAction()) && "capacity_dashboard".equals(log.getTableName())
        ));
    }

    @Test
    @DisplayName("TC-02: Dữ liệu rỗng - Chưa có phân bổ nào trong kỳ -> Trả về các chỉ số bằng không thay vì báo lỗi")
    void shouldReturnZeroMetricsWhenNoAllocationsOrEmployees_TC02() {
        // Given
        when(authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ)).thenReturn(1L);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of());
        when(loadProjectPort.findAll(anyInt(), anyInt())).thenReturn(List.of());

        // When
        CapacityDashboardQuery query = new CapacityDashboardQuery(null, 2026, 38, 8);
        CapacityDashboardResult result = service.execute(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.averageCapacityUtilization()).isEqualTo(BigDecimal.ZERO.setScale(1));
        assertThat(result.overloadedEmployeesCount()).isEqualTo(0);
        assertThat(result.departmentFreeHours()).isEqualTo(BigDecimal.ZERO.setScale(1));
        assertThat(result.unresolvedScheduleConflictsCount()).isEqualTo(0);
        assertThat(result.activeProjectsCount()).isEqualTo(0);
        assertThat(result.weeklyMetrics()).hasSize(8);
        assertThat(result.overloadedEmployees()).isEmpty();
        assertThat(result.unresolvedConflicts()).isEmpty();
        assertThat(result.activeProjects()).isEmpty();

        // Verify audit log success
        verify(saveAuditLogPort, times(1)).save(argThat(log ->
                "CAPACITY_DASHBOARD_VIEWED".equals(log.getAction())
        ));
    }

    @Test
    @DisplayName("TC-03: Không có quyền - Người dùng không phải ban giám đốc hoặc quản lý -> Từ chối truy cập và ghi nhật ký")
    void shouldDenyAccessAndRecordAuditLog_WhenUserLacksPermission_TC03() {
        // Given
        doThrow(new PermissionDeniedException(PermissionCode.CAPACITY_DASHBOARD_READ))
                .when(authorizationService).require(PermissionCode.CAPACITY_DASHBOARD_READ);

        // When & Then
        CapacityDashboardQuery query = new CapacityDashboardQuery(null, 2026, 38, 8);
        assertThatThrownBy(() -> service.execute(query))
                .isInstanceOf(PermissionDeniedException.class);

        // Verify denied audit log
        verify(saveAuditLogPort, times(1)).save(argThat(log ->
                "ACCESS_DENIED_CAPACITY_DASHBOARD".equals(log.getAction())
        ));
    }

    @Test
    @DisplayName("TC-04: Lưu lịch sử - Ghi lại người thực hiện, nội dung và thời điểm khi xem bảng điều khiển")
    void shouldRecordAuditLogWithDetails_TC04() {
        // Given
        when(authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ)).thenReturn(1L);
        lenient().when(loadEmployeePort.findActiveByOrgUnitIds(anyList())).thenReturn(List.of());
        lenient().when(loadEmployeePort.findAllActive()).thenReturn(List.of());
        lenient().when(loadProjectPort.findByOrgUnitBranch(anyLong(), anyInt(), anyInt())).thenReturn(List.of());
        lenient().when(loadProjectPort.findAll(anyInt(), anyInt())).thenReturn(List.of());

        // When
        CapacityDashboardQuery query = new CapacityDashboardQuery(10L, 2026, 40, 4);
        service.execute(query);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(captor.capture());
        AuditLog savedLog = captor.getValue();
        assertThat(savedLog.getAction()).isEqualTo("CAPACITY_DASHBOARD_VIEWED");
        assertThat(savedLog.getUserId()).isEqualTo(1L);
        assertThat(savedLog.getNewValue()).contains("4 tuần");
        assertThat(savedLog.getNewValue()).contains("Phòng Phần mềm");
    }

    private Employee createEmployee(Long id, String code, String name, Long orgUnitId, int standardHours) {
        return new Employee(
                new EmployeeId(id), new UserId(id), orgUnitId, code, name, "Lập trình viên",
                LocalDate.of(2025, 1, 1), null, false, standardHours, EmployeeStatus.ACTIVE
        );
    }
}
