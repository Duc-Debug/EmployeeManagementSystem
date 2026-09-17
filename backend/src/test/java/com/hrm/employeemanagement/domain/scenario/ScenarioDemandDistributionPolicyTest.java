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
