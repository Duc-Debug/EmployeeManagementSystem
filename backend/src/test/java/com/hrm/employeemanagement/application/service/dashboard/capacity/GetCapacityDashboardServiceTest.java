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
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.Holiday;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflict;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
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
        when(loadScheduleConflictPort.findUnresolvedConflictsForEmployees(anyList(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(conflict));

        // 1 active project
        Project activeProject = new Project(
                new ProjectId(1L), "PRJ-01", "Dự án Alpha", 10L, new EmployeeId(101L),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), BigDecimal.valueOf(1000), "Mô tả",
                ProjectStatus.ACTIVE, new UserId(1L), LocalDateTime.now(), LocalDateTime.now(), 0L
        );
        when(loadProjectPort.countActiveProjects()).thenReturn(1L);
        when(loadProjectPort.findActiveProjects(anyInt(), anyInt())).thenReturn(List.of(activeProject));
        when(loadProjectMemberPort.countMembersByProjectIds(anyList())).thenReturn(Map.of(1L, 5));

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
        assertThat(result.activeProjects().get(0).memberCount()).isEqualTo(5);

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
        lenient().when(loadProjectPort.countActiveProjects()).thenReturn(0L);
        lenient().when(loadProjectPort.findActiveProjects(anyInt(), anyInt())).thenReturn(List.of());

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
        lenient().when(loadProjectPort.countActiveProjectsByOrgUnitBranch(anyLong())).thenReturn(0L);
        lenient().when(loadProjectPort.findActiveProjectsByOrgUnitBranch(anyLong(), anyInt(), anyInt())).thenReturn(List.of());
        lenient().when(loadProjectPort.findActiveProjects(anyInt(), anyInt())).thenReturn(List.of());

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

    @Test
    @DisplayName("TC-05: Sắp xếp nhân sự quá tải - Số tuần quá tải giảm dần, sau đó tỷ lệ sử dụng giảm dần (tie-breaker)")
    void shouldSortOverloadedEmployeesWithCorrectTieBreaker() {
        // Given
        when(authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ)).thenReturn(1L);

        Employee empA = createEmployee(101L, "EMP_A", "Nhân viên A", 10L, 40);
        Employee empB = createEmployee(102L, "EMP_B", "Nhân viên B", 10L, 40);
        Employee empC = createEmployee(103L, "EMP_C", "Nhân viên C", 10L, 40);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(empA, empB, empC));

        // Emp A: 2 tuần quá tải (60h, 60h) -> 2 tuần, avg utilization = 150%
        // Emp B: 2 tuần quá tải (50h, 50h) -> 2 tuần, avg utilization = 125%
        // Emp C: 3 tuần quá tải (45h, 45h, 45h) -> 3 tuần, avg utilization = 112.5%
        List<WeeklyProjectAllocation> allocations = new ArrayList<>();
        allocations.add(new WeeklyProjectAllocation(1L, 101L, 1L, YearWeek.of(2026, 38), BigDecimal.valueOf(60.0)));
        allocations.add(new WeeklyProjectAllocation(2L, 101L, 1L, YearWeek.of(2026, 39), BigDecimal.valueOf(60.0)));

        allocations.add(new WeeklyProjectAllocation(3L, 102L, 1L, YearWeek.of(2026, 38), BigDecimal.valueOf(50.0)));
        allocations.add(new WeeklyProjectAllocation(4L, 102L, 1L, YearWeek.of(2026, 39), BigDecimal.valueOf(50.0)));

        allocations.add(new WeeklyProjectAllocation(5L, 103L, 1L, YearWeek.of(2026, 38), BigDecimal.valueOf(45.0)));
        allocations.add(new WeeklyProjectAllocation(6L, 103L, 1L, YearWeek.of(2026, 39), BigDecimal.valueOf(45.0)));
        allocations.add(new WeeklyProjectAllocation(7L, 103L, 1L, YearWeek.of(2026, 40), BigDecimal.valueOf(45.0)));

        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(allocations);
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadScheduleConflictPort.findUnresolvedConflictsForEmployees(anyList(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(List.of());
        lenient().when(loadProjectPort.countActiveProjects()).thenReturn(0L);
        lenient().when(loadProjectPort.findActiveProjects(anyInt(), anyInt())).thenReturn(List.of());

        // When
        CapacityDashboardQuery query = new CapacityDashboardQuery(null, 2026, 38, 4);
        CapacityDashboardResult result = service.execute(query);

        // Then
        assertThat(result.overloadedEmployees()).hasSize(3);
        // Emp C (3 tuần) phải đứng đầu
        assertThat(result.overloadedEmployees().get(0).employeeCode()).isEqualTo("EMP_C");
        assertThat(result.overloadedEmployees().get(0).overloadedWeeksCount()).isEqualTo(3);

        // Emp A (2 tuần, 150%) phải đứng trước Emp B (2 tuần, 125%)
        assertThat(result.overloadedEmployees().get(1).employeeCode()).isEqualTo("EMP_A");
        assertThat(result.overloadedEmployees().get(1).overloadedWeeksCount()).isEqualTo(2);

        assertThat(result.overloadedEmployees().get(2).employeeCode()).isEqualTo("EMP_B");
        assertThat(result.overloadedEmployees().get(2).overloadedWeeksCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-06: Query Validation - fromYear và fromWeek phải cùng truyền hoặc cùng null")
    void shouldRejectPartialYearOrWeekParameters() {
        assertThatThrownBy(() -> new CapacityDashboardQuery(null, 2026, null, 8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromYear và fromWeek phải được cung cấp đồng thời");

        assertThatThrownBy(() -> new CapacityDashboardQuery(null, null, 38, 8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromYear và fromWeek phải được cung cấp đồng thời");
    }

    @Test
    @DisplayName("TC-07: Query Validation - fromWeek = 53 trong năm 2025 (chỉ có 52 tuần) phải bị từ chối")
    void shouldRejectWeek53InYearWith52Weeks() {
        assertThatThrownBy(() -> new CapacityDashboardQuery(null, 2025, 53, 8))
                .isInstanceOf(com.hrm.employeemanagement.domain.exception.availability.InvalidWeekNumberException.class)
                .hasMessageContaining("Năm 2025 chỉ có 52 tuần");

        assertThatThrownBy(() -> new CapacityDashboardQuery(null, 2026, 999, 8))
                .isInstanceOf(com.hrm.employeemanagement.domain.exception.availability.InvalidWeekNumberException.class)
                .hasMessageContaining("Số tuần không hợp lệ");
    }

    @Test
    @DisplayName("TC-08: Cross-year dashboard period - Hiển thị chuyển giao năm ISO 2026-W52 đến 2027-W02")
    void shouldHandleCrossYearDashboardPeriod() {
        when(authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ)).thenReturn(1L);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of());
        lenient().when(loadProjectPort.countActiveProjects()).thenReturn(0L);
        lenient().when(loadProjectPort.findActiveProjects(anyInt(), anyInt())).thenReturn(List.of());

        CapacityDashboardQuery query = new CapacityDashboardQuery(null, 2026, 52, 4);
        CapacityDashboardResult result = service.execute(query);

        assertThat(result.weeklyMetrics()).hasSize(4);
        assertThat(result.weeklyMetrics().get(0).year()).isEqualTo(2026);
        assertThat(result.weeklyMetrics().get(0).weekNumber()).isEqualTo(52);
        assertThat(result.weeklyMetrics().get(1).year()).isEqualTo(2026);
        assertThat(result.weeklyMetrics().get(1).weekNumber()).isEqualTo(53);
        assertThat(result.weeklyMetrics().get(2).year()).isEqualTo(2027);
        assertThat(result.weeklyMetrics().get(2).weekNumber()).isEqualTo(1);
        assertThat(result.weeklyMetrics().get(3).year()).isEqualTo(2027);
        assertThat(result.weeklyMetrics().get(3).weekNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-09: Available hours = 0 - Không xảy ra lỗi chia cho 0 và tính toán tỷ lệ an toàn")
    void shouldHandleZeroAvailableHours() {
        when(authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ)).thenReturn(1L);

        Employee emp = createEmployee(101L, "EMP001", "Nguyễn Văn A", 10L, 40);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of());

        // Nghỉ phép toàn bộ 40h trong tuần -> Giờ khả dụng ròng = 0h
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList()))
                .thenReturn(Map.of(101L, Map.of(YearWeek.of(2026, 38), BigDecimal.valueOf(40.0))));

        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of());
        when(loadScheduleConflictPort.findUnresolvedConflictsForEmployees(anyList(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(List.of());
        lenient().when(loadProjectPort.countActiveProjects()).thenReturn(0L);
        lenient().when(loadProjectPort.findActiveProjects(anyInt(), anyInt())).thenReturn(List.of());

        CapacityDashboardResult result = service.execute(new CapacityDashboardQuery(null, 2026, 38, 1));

        assertThat(result.averageCapacityUtilization()).isNotNull();
        assertThat(result.averageCapacityUtilization()).isEqualTo(BigDecimal.ZERO.setScale(1));
        assertThat(result.weeklyMetrics()).hasSize(1);
        assertThat(result.weeklyMetrics().get(0).availableHours()).isEqualTo(BigDecimal.ZERO.setScale(1));
    }

    @Test
    @DisplayName("TC-10: Cấu hình ngưỡng tùy chỉnh - Không đánh dấu quá tải nếu tỷ lệ dưới ngưỡng overload mới (120%)")
    void shouldApplyCustomCapacityThreshold() {
        when(authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ)).thenReturn(1L);

        CapacityThresholdConfig customThreshold = CapacityThresholdConfig.createNew(
                CapacityThresholdScope.COMPANY, null,
                BigDecimal.valueOf(120.0), BigDecimal.valueOf(50.0), 1L
        );
        when(loadCapacityThresholdPort.findByScope(CapacityThresholdScope.COMPANY, null))
                .thenReturn(Optional.of(customThreshold));

        Employee emp = createEmployee(101L, "EMP001", "Nguyễn Văn A", 10L, 40);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));

        // Phân bổ 45h / 40h khả dụng = 112.5% tải -> Lớn hơn 100% nhưng nhỏ hơn ngưỡng 120%
        List<WeeklyProjectAllocation> allocations = List.of(
                new WeeklyProjectAllocation(1L, 101L, 1L, YearWeek.of(2026, 38), BigDecimal.valueOf(45.0))
        );
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(allocations);
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadScheduleConflictPort.findUnresolvedConflictsForEmployees(anyList(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(List.of());
        lenient().when(loadProjectPort.countActiveProjects()).thenReturn(0L);
        lenient().when(loadProjectPort.findActiveProjects(anyInt(), anyInt())).thenReturn(List.of());

        CapacityDashboardResult result = service.execute(new CapacityDashboardQuery(null, 2026, 38, 1));

        assertThat(result.overloadedEmployeesCount()).isEqualTo(0);
        assertThat(result.overloadedEmployees()).isEmpty();
    }

    @Test
    @DisplayName("TC-11: Hết hạn hợp đồng giữa tuần - Tự động điều chỉnh giảm giờ khả dụng theo số ngày làm việc còn lại")
    void shouldAdjustAvailableHoursWhenContractEndsMidweek() {
        when(authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ)).thenReturn(1L);

        // Hợp đồng kết thúc vào Thứ Tư 23/09/2026 trong tuần 2026-W39 (21/09 - 27/09)
        Employee emp = new Employee(
                new EmployeeId(101L), new UserId(101L), 10L, "EMP001", "Nguyễn Văn A", "Lập trình viên",
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 9, 23), false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of());
        when(loadScheduleConflictPort.findUnresolvedConflictsForEmployees(anyList(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(List.of());
        lenient().when(loadProjectPort.countActiveProjects()).thenReturn(0L);
        lenient().when(loadProjectPort.findActiveProjects(anyInt(), anyInt())).thenReturn(List.of());

        CapacityDashboardResult result = service.execute(new CapacityDashboardQuery(null, 2026, 39, 1));

        // 3 ngày làm việc (Thứ 2, 3, 4) / 5 ngày tiêu chuẩn -> 40 * 3 / 5 = 24.0h
        assertThat(result.weeklyMetrics()).hasSize(1);
        assertThat(result.weeklyMetrics().get(0).availableHours())
                .isEqualByComparingTo(BigDecimal.valueOf(24.0));
    }

    @Test
    @DisplayName("TC-12: Nghỉ lễ kết hợp Nghỉ phép - Khấu trừ đồng thời cả ngày lễ và giờ nghỉ phép trong tuần")
    void shouldDeductBothHolidayAndApprovedLeaveHours() {
        when(authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ)).thenReturn(1L);

        Employee emp = createEmployee(101L, "EMP001", "Nguyễn Văn A", 10L, 40);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of());

        // 1 ngày lễ (8h)
        Holiday holiday = new Holiday(LocalDate.of(2026, 9, 21), "Ngày Lễ Test", 8);
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of(holiday));

        // 1 ngày nghỉ phép (8h)
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList()))
                .thenReturn(Map.of(101L, Map.of(YearWeek.of(2026, 39), BigDecimal.valueOf(8.0))));

        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of());
        when(loadScheduleConflictPort.findUnresolvedConflictsForEmployees(anyList(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(List.of());
        lenient().when(loadProjectPort.countActiveProjects()).thenReturn(0L);
        lenient().when(loadProjectPort.findActiveProjects(anyInt(), anyInt())).thenReturn(List.of());

        CapacityDashboardResult result = service.execute(new CapacityDashboardQuery(null, 2026, 39, 1));

        // 40h - 8h lễ - 8h phép = 24.0h
        assertThat(result.weeklyMetrics()).hasSize(1);
        assertThat(result.weeklyMetrics().get(0).availableHours())
                .isEqualByComparingTo(BigDecimal.valueOf(24.0));
    }

    @Test
    @DisplayName("TC-13: Thiếu dữ liệu phân bổ thành viên dự án - Gán mặc định 0 thành viên an toàn")
    void shouldHandleMissingMemberCountMapForActiveProjects() {
        when(authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ)).thenReturn(1L);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of());

        Project project1 = new Project(
                new ProjectId(1L), "PRJ-01", "Dự án Test", 10L, new EmployeeId(101L),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), BigDecimal.valueOf(500), "Mô tả",
                ProjectStatus.ACTIVE, new UserId(1L), LocalDateTime.now(), LocalDateTime.now(), 0L
        );

        when(loadProjectPort.countActiveProjects()).thenReturn(2L);
        when(loadProjectPort.findActiveProjects(anyInt(), anyInt())).thenReturn(List.of(project1));
        when(loadProjectMemberPort.countMembersByProjectIds(anyList())).thenReturn(Map.of());

        CapacityDashboardResult result = service.execute(new CapacityDashboardQuery(null, 2026, 38, 1));

        assertThat(result.activeProjectsCount()).isEqualTo(2L);
        assertThat(result.activeProjects()).hasSize(1);
        assertThat(result.activeProjects().get(0).memberCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("TC-14: Giới hạn danh sách preview dự án tối đa 50 phần tử")
    void shouldLimitActiveProjectPreviewTo50() {
        when(authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ)).thenReturn(1L);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of());

        List<Project> projectList = new ArrayList<>();
        for (long i = 1; i <= 50; i++) {
            projectList.add(new Project(
                    new ProjectId(i), "PRJ-" + i, "Dự án " + i, 10L, new EmployeeId(101L),
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), BigDecimal.valueOf(100), "Mô tả",
                    ProjectStatus.ACTIVE, new UserId(1L), LocalDateTime.now(), LocalDateTime.now(), 0L
            ));
        }

        when(loadProjectPort.countActiveProjects()).thenReturn(60L);
        when(loadProjectPort.findActiveProjects(eq(0), eq(50))).thenReturn(projectList);
        when(loadProjectMemberPort.countMembersByProjectIds(anyList())).thenReturn(Map.of());

        CapacityDashboardResult result = service.execute(new CapacityDashboardQuery(null, 2026, 38, 8));

        assertThat(result.activeProjectsCount()).isEqualTo(60L);
        assertThat(result.activeProjects()).hasSize(50);
    }

    @Test
    @DisplayName("TC-15: Data Scope ORGANIZATION_BRANCH - Chỉ truy cập trong nhánh được phân quyền")
    void shouldRestrictDataScopeForOrganizationBranch() {
        when(authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ)).thenReturn(1L);
        when(executiveUser.getDataScope()).thenReturn(DataScope.ORGANIZATION_BRANCH);
        when(executiveUser.getScopeOrgUnitId()).thenReturn(10L);

        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);
        when(loadOrgUnitPort.findSubTree(anyString())).thenReturn(List.of(itDept));
        when(loadEmployeePort.findActiveByOrgUnitIds(anyList())).thenReturn(List.of());
        lenient().when(loadProjectPort.countActiveProjectsByOrgUnitBranch(10L)).thenReturn(0L);
        lenient().when(loadProjectPort.findActiveProjectsByOrgUnitBranch(eq(10L), anyInt(), anyInt())).thenReturn(List.of());

        // In scope query -> Thành công
        CapacityDashboardResult result = service.execute(new CapacityDashboardQuery(10L, 2026, 38, 4));
        assertThat(result).isNotNull();

        // Out of scope query -> Bị từ chối
        when(loadOrgUnitPort.existsInOrgUnitBranch(999L, 10L)).thenReturn(false);
        assertThatThrownBy(() -> service.execute(new CapacityDashboardQuery(999L, 2026, 38, 4)))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("TC-16: Data Scope SELF - PM (VT_02) chỉ xem trong phạm vi phòng ban của mình, vai trò khác bị từ chối")
    void shouldRestrictDataScopeForSelfProjectManager() {
        when(authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ)).thenReturn(1L);
        when(executiveUser.getDataScope()).thenReturn(DataScope.SELF);
        when(executiveUser.getRole()).thenReturn(new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án"));

        Employee pmEmp = createEmployee(101L, "PM001", "PM Nguyễn Văn C", 10L, 40);
        when(loadEmployeePort.findByUserId(new UserId(1L))).thenReturn(Optional.of(pmEmp));
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);
        when(loadOrgUnitPort.findSubTree(anyString())).thenReturn(List.of(itDept));
        when(loadEmployeePort.findActiveByOrgUnitIds(anyList())).thenReturn(List.of());
        lenient().when(loadProjectPort.countActiveProjectsByOrgUnitBranch(10L)).thenReturn(0L);
        lenient().when(loadProjectPort.findActiveProjectsByOrgUnitBranch(eq(10L), anyInt(), anyInt())).thenReturn(List.of());

        // PM xem phòng ban của mình -> Thành công
        CapacityDashboardResult result = service.execute(new CapacityDashboardQuery(10L, 2026, 38, 4));
        assertThat(result).isNotNull();

        // User role khác (không phải VT_02) có scope SELF -> Bị từ chối
        when(executiveUser.getRole()).thenReturn(new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên dự án"));
        assertThatThrownBy(() -> service.execute(new CapacityDashboardQuery(10L, 2026, 38, 4)))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("TC-17: Phòng ban không tồn tại -> Ném OrgUnitNotFoundException")
    void shouldThrowOrgUnitNotFoundException_WhenOrgUnitDoesNotExist() {
        when(authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ)).thenReturn(1L);
        when(loadOrgUnitPort.findById(new OrgUnitId(999L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new CapacityDashboardQuery(999L, 2026, 38, 8)))
                .isInstanceOf(OrgUnitNotFoundException.class)
                .hasMessageContaining("Không tìm thấy bộ phận: 999");
    }

    private Employee createEmployee(Long id, String code, String name, Long orgUnitId, int standardHours) {
        return new Employee(
                new EmployeeId(id), new UserId(id), orgUnitId, code, name, "Lập trình viên",
                LocalDate.of(2025, 1, 1), null, false, standardHours, EmployeeStatus.ACTIVE
        );
    }
}
