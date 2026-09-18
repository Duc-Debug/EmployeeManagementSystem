package com.hrm.employeemanagement.domain.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.user.UserId;

@DisplayName("ScenarioDemandDistributionPolicy Unit Tests")
class ScenarioDemandDistributionPolicyTest {

    @Test
    @DisplayName("Phân bổ đều và xử lý số dư xu (remainder cents) chính xác")
    void testCalculateDistribution_WithRemainderCents() {
        Long empId1 = 1L;
        Long empId2 = 2L;
        Long empId3 = 3L;

        Employee e1 = createEmployee(empId1, "Frontend Developer");
        Employee e2 = createEmployee(empId2, "Frontend Developer");
        Employee e3 = createEmployee(empId3, "Frontend Developer");

        Map<Long, Employee> employeeMap = Map.of(empId1, e1, empId2, e2, empId3, e3);
        List<Long> snapshotEmpIds = List.of(empId1, empId2, empId3);

        YearWeek yw1 = YearWeek.of(2026, 40);
        List<YearWeek> targetWeeks = List.of(yw1);

        // Nhu cầu 10 giờ cho 3 người trong tuần 40. 10 / 3 = 3.33, dư 0.01 cho người đầu tiên
        ScenarioDemand demand = ScenarioDemand.create(
                100L,
                "Frontend demand",
                1,
                2026, 40,
                2026, 40,
                BigDecimal.valueOf(10),
                "Frontend Developer"
        );

        Map<Long, Map<String, BigDecimal>> result = ScenarioDemandDistributionPolicy.calculateDistribution(
                List.of(demand),
                snapshotEmpIds,
                employeeMap,
                targetWeeks
        );

        String k1 = ScenarioDemandDistributionPolicy.makeKey(empId1, yw1.year(), yw1.weekNumber());
        String k2 = ScenarioDemandDistributionPolicy.makeKey(empId2, yw1.year(), yw1.weekNumber());
        String k3 = ScenarioDemandDistributionPolicy.makeKey(empId3, yw1.year(), yw1.weekNumber());

        BigDecimal h1 = result.get(empId1).get(k1);
        BigDecimal h2 = result.get(empId2).get(k2);
        BigDecimal h3 = result.get(empId3).get(k3);

        assertThat(h1).isEqualByComparingTo(new BigDecimal("3.34"));
        assertThat(h2).isEqualByComparingTo(new BigDecimal("3.33"));
        assertThat(h3).isEqualByComparingTo(new BigDecimal("3.33"));
        assertThat(h1.add(h2).add(h3)).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("Khớp vai trò (role matching) chính xác theo từ khóa phân tách")
    void testRoleMatching() {
        assertThat(ScenarioDemandDistributionPolicy.isRoleMatching("Java Developer", "Java")).isTrue();
        assertThat(ScenarioDemandDistributionPolicy.isRoleMatching("Java Developer", "Java Developer")).isTrue();
        assertThat(ScenarioDemandDistributionPolicy.isRoleMatching("Senior Backend Java", "java")).isTrue();
        assertThat(ScenarioDemandDistributionPolicy.isRoleMatching("JavaScript Developer", "Java")).isFalse();
        assertThat(ScenarioDemandDistributionPolicy.isRoleMatching(null, "Java")).isFalse();
        assertThat(ScenarioDemandDistributionPolicy.isRoleMatching("Java Developer", null)).isTrue();
        assertThat(ScenarioDemandDistributionPolicy.isRoleMatching("Java Developer", "")).isTrue();
    }

    @Test
    @DisplayName("HIGH-03: Aggregate capacity invariant across multiple demands in the same week (không vượt quá capacity)")
    void testCalculateDistribution_EnforcesCapacityAcrossMultipleDemands() {
        Long empIdA = 10L;
        Employee empA = createEmployee(empIdA, "Backend Developer");
        Map<Long, Employee> employeeMap = Map.of(empIdA, empA);
        List<Long> snapshotEmpIds = List.of(empIdA);

        YearWeek yw = YearWeek.of(2026, 40);
        List<YearWeek> targetWeeks = List.of(yw);

        // Demand 1: 30h Backend
        ScenarioDemand demand1 = ScenarioDemand.create(
                101L, "Backend Demand 1", 1,
                2026, 40, 2026, 40,
                BigDecimal.valueOf(30), "Backend Developer"
        );
        // Demand 2: 30h Backend
        ScenarioDemand demand2 = ScenarioDemand.create(
                102L, "Backend Demand 2", 2,
                2026, 40, 2026, 40,
                BigDecimal.valueOf(30), "Backend Developer"
        );

        String key = ScenarioDemandDistributionPolicy.makeKey(empIdA, yw.year(), yw.weekNumber());
        Map<String, BigDecimal> capacityMap = Map.of(key, BigDecimal.valueOf(40));

        Map<Long, Map<String, BigDecimal>> result = ScenarioDemandDistributionPolicy.calculateDistribution(
                List.of(demand1, demand2),
                snapshotEmpIds,
                employeeMap,
                targetWeeks,
                capacityMap
        );

        BigDecimal totalAllocated = result.get(empIdA).get(key);

        // Demand 1 takes 30h, Demand 2 capped at remaining 10h -> Total 40h <= Capacity 40h
        assertThat(totalAllocated).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    @DisplayName("HIGH-02: Không phân bổ giờ nhu cầu cho nhân sự có trạng thái không ACTIVE (TERMINATED)")
    void testCalculateDistribution_IgnoresInactiveEmployee() {
        Long activeEmpId = 1L;
        Long inactiveEmpId = 2L;

        Employee activeEmp = createEmployee(activeEmpId, "Backend Developer");
        Employee inactiveEmp = new Employee(
                new EmployeeId(inactiveEmpId),
                new UserId(inactiveEmpId),
                1L,
                "EMP-2",
                "Inactive Employee",
                "Backend Developer",
                null,
                null,
                false,
                40,
                EmployeeStatus.TERMINATED
        );

        Map<Long, Employee> employeeMap = Map.of(activeEmpId, activeEmp, inactiveEmpId, inactiveEmp);
        List<Long> snapshotEmpIds = List.of(activeEmpId, inactiveEmpId);

        YearWeek yw = YearWeek.of(2026, 40);
        List<YearWeek> targetWeeks = List.of(yw);

        ScenarioDemand demand = ScenarioDemand.create(
                101L, "Demand", 1,
                2026, 40, 2026, 40,
                BigDecimal.valueOf(20), "Backend Developer"
        );

        Map<Long, Map<String, BigDecimal>> result = ScenarioDemandDistributionPolicy.calculateDistribution(
                List.of(demand),
                snapshotEmpIds,
                employeeMap,
                targetWeeks
        );

        String keyActive = ScenarioDemandDistributionPolicy.makeKey(activeEmpId, yw.year(), yw.weekNumber());
        assertThat(result.get(activeEmpId).get(keyActive)).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(result.get(inactiveEmpId)).isNull();
    }

    @Test
    @DisplayName("Comment 1: calculateDistributionWithMetrics ghi nhận đầy đủ requested, applied, unfulfilled hours khi bị chạm trần capacity")
    void testCalculateDistributionWithMetrics_PartialAllocationReporting() {
        Long empId = 1L;
        Employee emp = createEmployee(empId, "Java Developer");
        Map<Long, Employee> employeeMap = Map.of(empId, emp);
        List<Long> snapshotEmpIds = List.of(empId);

        YearWeek yw = YearWeek.of(2026, 40);
        List<YearWeek> targetWeeks = List.of(yw);

        // Nhu cầu yêu cầu 40 giờ
        ScenarioDemand demand = ScenarioDemand.create(
                101L, "Demand 40h", 1,
                2026, 40, 2026, 40,
                BigDecimal.valueOf(40), "Java Developer"
        );

        // Nhưng nhân viên chỉ còn 25 giờ capacity
        String key = ScenarioDemandDistributionPolicy.makeKey(empId, yw.year(), yw.weekNumber());
        Map<String, BigDecimal> capacityMap = Map.of(key, BigDecimal.valueOf(25));

        ScenarioDistributionResult result = ScenarioDemandDistributionPolicy.calculateDistributionWithMetrics(
                List.of(demand),
                snapshotEmpIds,
                employeeMap,
                targetWeeks,
                capacityMap
        );

        assertThat(result.totalRequestedHours()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(result.totalAppliedHours()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(result.totalUnfulfilledHours()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(result.isPartiallyFulfilled()).isTrue();
        assertThat(result.empDemandHoursMap().get(empId).get(key)).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(result.unfulfilledDetails()).hasSize(1);
        assertThat(result.unfulfilledDetails().get(0).unfulfilledHours()).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    @Test
    @DisplayName("Reviewer Point 1 Regression: Available=40h, Existing=32h -> Remaining=8h; Demand=16h -> Applied=8h, Unfulfilled=8h")
    void testCalculateDistribution_RemainingCapacityInvariant_Regression() {
        Long empId = 1L;
        Employee emp = createEmployee(empId, "Java Developer");
        Map<Long, Employee> employeeMap = Map.of(empId, emp);
        List<Long> snapshotEmpIds = List.of(empId);

        YearWeek yw = YearWeek.of(2026, 40);
        List<YearWeek> targetWeeks = List.of(yw);

        ScenarioDemand demand = ScenarioDemand.create(
                101L, "Demand 16h", 1,
                2026, 40, 2026, 40,
                BigDecimal.valueOf(16), "Java Developer"
        );

        // Available = 40h, Existing = 32h -> Remaining = 40 - 32 = 8h
        BigDecimal availableHours = new BigDecimal("40.00");
        BigDecimal existingAllocatedHours = new BigDecimal("32.00");
        BigDecimal remainingCapacity = availableHours.subtract(existingAllocatedHours).max(BigDecimal.ZERO);

        String key = ScenarioDemandDistributionPolicy.makeKey(empId, yw.year(), yw.weekNumber());
        Map<String, BigDecimal> capacityMap = Map.of(key, remainingCapacity);

        ScenarioDistributionResult result = ScenarioDemandDistributionPolicy.calculateDistributionWithMetrics(
                List.of(demand),
                snapshotEmpIds,
                employeeMap,
                targetWeeks,
                capacityMap
        );

        assertThat(result.totalRequestedHours()).isEqualByComparingTo(new BigDecimal("16.00"));
        assertThat(result.totalAppliedHours()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(result.totalUnfulfilledHours()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(result.isPartiallyFulfilled()).isTrue();

        // Invariant: Existing (32h) + Applied (8h) = 40h <= Available (40h) -> No overload
        BigDecimal totalPostAllocation = existingAllocatedHours.add(result.totalAppliedHours());
        assertThat(totalPostAllocation).isLessThanOrEqualTo(availableHours);

        // Breakdown verification
        assertThat(result.unfulfilledDetails()).hasSize(1);
        UnfulfilledDemandDetail detail = result.unfulfilledDetails().get(0);
        assertThat(detail.demandName()).isEqualTo("Demand 16h");
        assertThat(detail.requestedHours()).isEqualByComparingTo(new BigDecimal("16.00"));
        assertThat(detail.appliedHours()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(detail.unfulfilledHours()).isEqualByComparingTo(new BigDecimal("8.00"));
    }

    @Test
    @DisplayName("Reviewer Point 2: Ngữ nghĩa totalRequestedHours trên dải tuần - 1 demand, 4 tuần, 40h/tuần -> requested=160h, applied + unfulfilled = requested")
    void testCalculateDistribution_MultiWeekDemandSemantics() {
        Long empId = 1L;
        Employee emp = createEmployee(empId, "Java Developer");
        Map<Long, Employee> employeeMap = Map.of(empId, emp);
        List<Long> snapshotEmpIds = List.of(empId);

        List<YearWeek> targetWeeks = List.of(
                YearWeek.of(2026, 40),
                YearWeek.of(2026, 41),
                YearWeek.of(2026, 42),
                YearWeek.of(2026, 43)
        );

        // 1 demand kéo dài 4 tuần, mỗi tuần 40h
        ScenarioDemand demand = ScenarioDemand.create(
                101L, "Backend Dev Demand", 1,
                2026, 40, 2026, 43,
                BigDecimal.valueOf(40), "Java Developer"
        );

        // Giả sử tuần 40 và 41 nhân sự còn đủ 40h, nhưng tuần 42 chỉ còn 20h, tuần 43 chỉ còn 10h
        Map<String, BigDecimal> capacityMap = Map.of(
                ScenarioDemandDistributionPolicy.makeKey(empId, 2026, 40), new BigDecimal("40.00"),
                ScenarioDemandDistributionPolicy.makeKey(empId, 2026, 41), new BigDecimal("40.00"),
                ScenarioDemandDistributionPolicy.makeKey(empId, 2026, 42), new BigDecimal("20.00"),
                ScenarioDemandDistributionPolicy.makeKey(empId, 2026, 43), new BigDecimal("10.00")
        );

        ScenarioDistributionResult result = ScenarioDemandDistributionPolicy.calculateDistributionWithMetrics(
                List.of(demand),
                snapshotEmpIds,
                employeeMap,
                targetWeeks,
                capacityMap
        );

        // 4 tuần * 40h = 160h
        assertThat(result.totalRequestedHours()).isEqualByComparingTo(new BigDecimal("160.00"));
        // Đã phân bổ: 40 + 40 + 20 + 10 = 110h
        assertThat(result.totalAppliedHours()).isEqualByComparingTo(new BigDecimal("110.00"));
        // Chưa phân bổ: 160 - 110 = 50h
        assertThat(result.totalUnfulfilledHours()).isEqualByComparingTo(new BigDecimal("50.00"));
        // Bất biến: applied + unfulfilled == requested
        assertThat(result.totalAppliedHours().add(result.totalUnfulfilledHours()))
                .isEqualByComparingTo(result.totalRequestedHours());

        // Breakdown chi tiết: hụt ở tuần 42 (20h) và tuần 43 (30h)
        assertThat(result.unfulfilledDetails()).hasSize(2);
        assertThat(result.unfulfilledDetails().get(0).weekNumber()).isEqualTo(42);
        assertThat(result.unfulfilledDetails().get(0).unfulfilledHours()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(result.unfulfilledDetails().get(1).weekNumber()).isEqualTo(43);
        assertThat(result.unfulfilledDetails().get(1).unfulfilledHours()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    private Employee createEmployee(Long id, String role) {
        return new Employee(
                new EmployeeId(id),
                new UserId(id),
                1L,
                "EMP-" + id,
                "Employee " + id,
                role,
                null,
                null,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }
}
