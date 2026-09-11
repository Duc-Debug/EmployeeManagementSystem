package com.hrm.employeemanagement.application.service.allocation;

import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityMatrixResult;
import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityMatrixResult.CapacityMatrixCellResult;
import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityMatrixResult.EmployeeCapacityRowResult;
import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityQuery;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.CapacityStatus;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCompanyWeeklyCapacityServiceTest {

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
    private User rmUser;
    @Mock
    private User directorUser;
    @Mock
    private User employeeUser;
    @Mock
    private OrgUnit itDept;

    private GetCompanyWeeklyCapacityService service;

    @BeforeEach
    void setUp() {
        service = new GetCompanyWeeklyCapacityService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadAllocationPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort
        );

        lenient().when(itDept.getId()).thenReturn(new OrgUnitId(10L));
        lenient().when(itDept.getUnitName()).thenReturn("Phòng Công nghệ thông tin");
        lenient().when(itDept.getTreePath()).thenReturn("/1/10");

        lenient().when(rmUser.getDataScope()).thenReturn(DataScope.ORGANIZATION_BRANCH);
        lenient().when(rmUser.getScopeOrgUnitId()).thenReturn(10L);

        lenient().when(directorUser.getDataScope()).thenReturn(DataScope.COMPANY);

        lenient().when(employeeUser.getDataScope()).thenReturn(DataScope.SELF);
    }

    private Employee createMockEmployee(Long id, String code, String name, Long orgUnitId, Integer standardHours) {
        return new Employee(
                new EmployeeId(id), null, orgUnitId, code, name,
                "Developer", LocalDate.of(2025, 1, 1), null, false, standardHours, EmployeeStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("TC-01 — Luồng thành công: Bộ phận 5 nhân sự, 8 tuần -> hiển thị lưới 5 hàng × 8 cột với tỷ lệ sử dụng chuẩn")
    void testSuccessfulGrid5Rows8Columns() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(100L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(rmUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(10L))).thenReturn(Optional.of(itDept));
        when(loadOrgUnitPort.findSubTree("/1/10")).thenReturn(List.of(itDept));
        when(loadOrgUnitPort.findAllByIdIn(anyList())).thenReturn(List.of(itDept));

        List<Employee> fiveEmployees = List.of(
                createMockEmployee(1L, "EMP001", "Nguyễn Văn A", 10L, 40),
                createMockEmployee(2L, "EMP002", "Trần Thị B", 10L, 40),
                createMockEmployee(3L, "EMP003", "Lê Văn C", 10L, 40),
                createMockEmployee(4L, "EMP004", "Phạm Thị D", 10L, 40),
                createMockEmployee(5L, "EMP005", "Hoàng Văn E", 10L, 40)
        );
        when(loadEmployeePort.findActiveByOrgUnitIds(List.of(10L))).thenReturn(fiveEmployees);
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());

        CompanyWeeklyCapacityQuery query = new CompanyWeeklyCapacityQuery(10L, 2026, 37, 8);
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);

        CompanyWeeklyCapacityMatrixResult result = service.getWeeklyCapacityMatrix(query);

        assertThat(result).isNotNull();
        assertThat(result.weeks()).hasSize(8);
        assertThat(result.rows()).hasSize(5);

        for (EmployeeCapacityRowResult row : result.rows()) {
            assertThat(row.cells()).hasSize(8);
            for (CapacityMatrixCellResult cell : row.cells()) {
                assertThat(cell.availableHours()).isEqualByComparingTo(BigDecimal.valueOf(40.0));
                assertThat(cell.allocatedHours()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(cell.utilizationPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(cell.isOverloaded()).isFalse();
                assertThat(cell.status()).isEqualTo(CapacityStatus.UNDERUTILIZED);
            }
        }
    }

    @Test
    @DisplayName("TC-02 — Quá tải: Nhân sự có tổng giờ phân bổ vượt quá khả dụng trong tuần -> ô bị đánh dấu quá tải và hiển thị excessHours")
    void testOverloadedEmployeeFlaggedAndExcessHoursCalculated() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(100L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(rmUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(10L))).thenReturn(Optional.of(itDept));
        when(loadOrgUnitPort.findSubTree("/1/10")).thenReturn(List.of(itDept));
        when(loadOrgUnitPort.findAllByIdIn(anyList())).thenReturn(List.of(itDept));
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);

        Employee emp1 = createMockEmployee(1L, "EMP001", "Nguyễn Văn A", 10L, 40);
        when(loadEmployeePort.findActiveByOrgUnitIds(List.of(10L))).thenReturn(List.of(emp1));

        // Phân bổ 48 giờ cho tuần 37/2026
        YearWeek yw37 = YearWeek.of(2026, 37);
        WeeklyProjectAllocation allocation1 = new WeeklyProjectAllocation(101L, 1L, 999L, yw37, BigDecimal.valueOf(48.0));
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of(allocation1));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());

        CompanyWeeklyCapacityQuery query = new CompanyWeeklyCapacityQuery(10L, 2026, 37, 8);
        CompanyWeeklyCapacityMatrixResult result = service.getWeeklyCapacityMatrix(query);

        assertThat(result.rows()).hasSize(1);
        EmployeeCapacityRowResult row = result.rows().get(0);
        CapacityMatrixCellResult week37Cell = row.cells().get(0);

        assertThat(week37Cell.year()).isEqualTo(2026);
        assertThat(week37Cell.weekNumber()).isEqualTo(37);
        assertThat(week37Cell.availableHours()).isEqualByComparingTo(BigDecimal.valueOf(40.0));
        assertThat(week37Cell.allocatedHours()).isEqualByComparingTo(BigDecimal.valueOf(48.0));
        assertThat(week37Cell.isOverloaded()).isTrue();
        assertThat(week37Cell.excessHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(week37Cell.utilizationPercentage()).isEqualByComparingTo(BigDecimal.valueOf(120.0));
        assertThat(week37Cell.status()).isEqualTo(CapacityStatus.OVERLOADED);
        assertThat(row.overloadedWeeksCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-03 — Data Scope: Người dùng RM chỉ quản lý một bộ phận, khi mở bảng năng lực ngoài branch thì bị từ chối 403 Forbidden")
    void testDataScopeEnforcedForBranchManager() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(100L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(rmUser));

        // Cố tình truy vấn phòng ban 999 không thuộc branch của IT Dept (10L)
        when(loadOrgUnitPort.existsInOrgUnitBranch(999L, 10L)).thenReturn(false);

        CompanyWeeklyCapacityQuery queryOutsideScope = new CompanyWeeklyCapacityQuery(999L, 2026, 37, 8);

        assertThatThrownBy(() -> service.getWeeklyCapacityMatrix(queryOutsideScope))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("TC-03 — Data Scope: Người dùng Ban giám đốc (COMPANY) có quyền xem toàn công ty hoặc lọc bất kỳ bộ phận nào")
    void testCompanyScopeCanViewAnyDepartment() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(200L);
        when(loadUserPort.findById(new UserId(200L))).thenReturn(Optional.of(directorUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(10L))).thenReturn(Optional.of(itDept));
        when(loadOrgUnitPort.findSubTree("/1/10")).thenReturn(List.of(itDept));
        when(loadEmployeePort.findActiveByOrgUnitIds(List.of(10L))).thenReturn(List.of());

        CompanyWeeklyCapacityQuery query = new CompanyWeeklyCapacityQuery(10L, 2026, 37, 8);
        CompanyWeeklyCapacityMatrixResult result = service.getWeeklyCapacityMatrix(query);

        assertThat(result).isNotNull();
        assertThat(result.orgUnitName()).isEqualTo("Phòng Công nghệ thông tin");
    }

    @Test
    @DisplayName("Data Scope: Người dùng chỉ có quyền SELF (VT-04) bị từ chối truy cập bảng năng lực tổng thể")
    void testSelfScopeDeniedAccess() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(300L);
        when(loadUserPort.findById(new UserId(300L))).thenReturn(Optional.of(employeeUser));

        CompanyWeeklyCapacityQuery query = new CompanyWeeklyCapacityQuery(10L, 2026, 37, 8);

        assertThatThrownBy(() -> service.getWeeklyCapacityMatrix(query))
                .isInstanceOf(PermissionDeniedException.class);
    }
}