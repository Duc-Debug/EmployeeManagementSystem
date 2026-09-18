package com.hrm.employeemanagement.application.service.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.scenario.ScenarioAllocationSnapshotItem;
import com.hrm.employeemanagement.domain.user.UserId;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScenarioBaselineValidator Unit Tests")
class ScenarioBaselineValidatorTest {

    @Mock private LoadWeeklyProjectAllocationPort loadAllocationPort;
    @Mock private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    @Mock private LoadHolidaysPort loadHolidaysPort;
    @Mock private LoadApprovedLeavesPort loadApprovedLeavesPort;
    @Mock private LoadWorkingCalendarPort loadWorkingCalendarPort;

    private ScenarioBaselineValidator validator;

    private final Long empId = 1001L;
    private final YearWeek targetWeek = YearWeek.of(2026, 40);
    private Employee emp;

    @BeforeEach
    void setUp() {
        validator = new ScenarioBaselineValidator(
                loadAllocationPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort
        );

        emp = new Employee(
                new EmployeeId(empId),
                new UserId(1L),
                10L,
                "EMP-1001",
                "Nguyễn Văn A",
                "Backend Developer",
                null,
                null,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("Phát hiện stale khi allocatedHours thực tế đã thay đổi")
    void testCheckBaselineStale_AllocatedHoursChanged() {
        ScenarioAllocationSnapshotItem snapshot = ScenarioAllocationSnapshotItem.create(
                1L, empId, 2026, 40, BigDecimal.valueOf(10), BigDecimal.valueOf(40)
        );

        // Hiện tại đổi thành 20h
        WeeklyProjectAllocation currentAlloc = WeeklyProjectAllocation.createNew(
                empId, 200L, targetWeek, BigDecimal.valueOf(20)
        );
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of(currentAlloc));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any())).thenReturn(Map.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());

        List<String> reasons = validator.checkBaselineStale(
                List.of(snapshot),
                List.of(targetWeek),
                Map.of(empId, emp)
        );

        assertThat(reasons).hasSize(1);
        assertThat(reasons.get(0)).contains("phân bổ");
        assertThat(reasons.get(0)).contains("10");
        assertThat(reasons.get(0)).contains("20");
    }

    @Test
    @DisplayName("Phát hiện stale khi availableHours thực tế đã thay đổi dù allocatedHours không đổi")
    void testCheckBaselineStale_AvailableHoursChanged_EvenIfAllocatedUnchanged() {
        ScenarioAllocationSnapshotItem snapshot = ScenarioAllocationSnapshotItem.create(
                1L, empId, 2026, 40, BigDecimal.valueOf(10), BigDecimal.valueOf(40)
        );

        // allocatedHours vẫn giữ nguyên 10h
        WeeklyProjectAllocation currentAlloc = WeeklyProjectAllocation.createNew(
                empId, 200L, targetWeek, BigDecimal.valueOf(10)
        );
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of(currentAlloc));

        // Nhưng nhân viên được duyệt nghỉ phép 16h trong tuần -> khả dụng thực tế còn 24h
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any()))
                .thenReturn(Map.of(empId, Map.of(targetWeek, BigDecimal.valueOf(16))));
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());

        List<String> reasons = validator.checkBaselineStale(
                List.of(snapshot),
                List.of(targetWeek),
                Map.of(empId, emp)
        );

        assertThat(reasons).hasSize(1);
        assertThat(reasons.get(0)).contains("khả dụng");
        assertThat(reasons.get(0)).contains("40");
        assertThat(reasons.get(0)).contains("24");
    }

    @Test
    @DisplayName("Không báo stale khi cả allocatedHours và availableHours đều khớp")
    void testCheckBaselineStale_NoChange_ReturnsEmpty() {
        ScenarioAllocationSnapshotItem snapshot = ScenarioAllocationSnapshotItem.create(
                1L, empId, 2026, 40, BigDecimal.valueOf(10), BigDecimal.valueOf(40)
        );

        WeeklyProjectAllocation currentAlloc = WeeklyProjectAllocation.createNew(
                empId, 200L, targetWeek, BigDecimal.valueOf(10)
        );
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of(currentAlloc));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any())).thenReturn(Map.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());

        List<String> reasons = validator.checkBaselineStale(
                List.of(snapshot),
                List.of(targetWeek),
                Map.of(empId, emp)
        );

        assertThat(reasons).isEmpty();
    }

    @Test
    @DisplayName("HIGH-02: Phát hiện stale khi nhân sự chuyển sang trạng thái TERMINATED (không còn ACTIVE)")
    void testCheckBaselineStale_EmployeeInactive() {
        ScenarioAllocationSnapshotItem snapshot = ScenarioAllocationSnapshotItem.create(
                1L, empId, 2026, 40, BigDecimal.valueOf(10), BigDecimal.valueOf(40)
        );
        Employee inactiveEmp = new Employee(
                new EmployeeId(empId), new UserId(1L), 10L, "EMP-1001", "Nguyễn Văn A",
                "Backend Developer", null, null, false, 40, EmployeeStatus.TERMINATED
        );

        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any())).thenReturn(Map.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());

        List<String> reasons = validator.checkBaselineStale(
                List.of(snapshot),
                List.of(targetWeek),
                Map.of(empId, inactiveEmp),
                List.of(10L)
        );

        assertThat(reasons).anyMatch(r -> r.contains("không còn ở trạng thái hoạt động"));
    }

    @Test
    @DisplayName("HIGH-02: Phát hiện stale khi nhân sự chuyển khỏi đơn vị thuộc phạm vi kịch bản")
    void testCheckBaselineStale_EmployeeTransferredOutOfOrgUnit() {
        ScenarioAllocationSnapshotItem snapshot = ScenarioAllocationSnapshotItem.create(
                1L, empId, 2026, 40, BigDecimal.valueOf(10), BigDecimal.valueOf(40)
        );
        // emp có orgUnitId = 10L, nhưng kịch bản chỉ cho phép [20L, 30L]
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any())).thenReturn(Map.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());

        List<String> reasons = validator.checkBaselineStale(
                List.of(snapshot),
                List.of(targetWeek),
                Map.of(empId, emp),
                List.of(20L, 30L)
        );

        assertThat(reasons).anyMatch(r -> r.contains("đã chuyển khỏi đơn vị thuộc phạm vi kịch bản"));
    }

    @Test
    @DisplayName("HIGH-02: Phát hiện stale khi nhân sự trong snapshot không còn tồn tại trong hệ thống")
    void testCheckBaselineStale_EmployeeNotFound() {
        ScenarioAllocationSnapshotItem snapshot = ScenarioAllocationSnapshotItem.create(
                1L, empId, 2026, 40, BigDecimal.valueOf(10), BigDecimal.valueOf(40)
        );

        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any())).thenReturn(Map.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());

        List<String> reasons = validator.checkBaselineStale(
                List.of(snapshot),
                List.of(targetWeek),
                Map.of() // Empty employee map
        );

        assertThat(reasons).anyMatch(r -> r.contains("không còn tồn tại trong hệ thống"));
    }
}
