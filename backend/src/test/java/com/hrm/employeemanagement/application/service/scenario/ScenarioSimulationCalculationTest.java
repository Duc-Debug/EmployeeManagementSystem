package com.hrm.employeemanagement.application.service.scenario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.scenario.ScenarioSimulationResult;
import com.hrm.employeemanagement.application.dto.scenario.WeeklySimulationMetricResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.CapacityStatus;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;
import com.hrm.employeemanagement.domain.scenario.ScenarioAllocationSnapshotItem;
import com.hrm.employeemanagement.domain.scenario.ScenarioDemand;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

class ScenarioSimulationCalculationTest {

    private AuthorizationService authorizationService;
    private LoadUserPort loadUserPort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadResourceScenarioPort loadScenarioPort;
    private LoadScenarioDemandPort loadDemandPort;
    private LoadScenarioSnapshotPort loadSnapshotPort;
    private LoadCapacityThresholdPort loadCapacityThresholdPort;
    private SaveAuditLogPort saveAuditLogPort;

    private ScenarioSimulationCalculationService service;

    private User vt03User;
    private ResourceScenario scenario;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadUserPort = mock(LoadUserPort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadScenarioPort = mock(LoadResourceScenarioPort.class);
        loadDemandPort = mock(LoadScenarioDemandPort.class);
        loadSnapshotPort = mock(LoadScenarioSnapshotPort.class);
        loadCapacityThresholdPort = mock(LoadCapacityThresholdPort.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);

        service = new ScenarioSimulationCalculationService(
                authorizationService,
                loadUserPort,
                loadOrgUnitPort,
                loadEmployeePort,
                loadScenarioPort,
                loadDemandPort,
                loadSnapshotPort,
                loadCapacityThresholdPort,
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

        scenario = ResourceScenario.createNew(
                "SCN-01", "Kịch bản test", "Mô tả", 10L, 2026, 38, 4, 103L
        );
        scenario.setId(1L);

        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_READ)).thenReturn(103L);
        when(loadUserPort.findById(new UserId(103L))).thenReturn(Optional.of(vt03User));
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);
        when(loadScenarioPort.findById(1L)).thenReturn(Optional.of(scenario));
        when(loadCapacityThresholdPort.findByScope(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("VI. Tính toán năng lực mô phỏng: Snapshot + Demand = Workload, so sánh Available Capacity")
    void testSimulationCalculation_WorkloadAndCapacityComparison() {
        // Tuần 38: 1 nhân sự có snapshot allocation = 30h, available = 40h
        // Tuần 39: 1 nhân sự có snapshot allocation = 40h, available = 40h
        List<ScenarioAllocationSnapshotItem> snapshots = List.of(
                new ScenarioAllocationSnapshotItem(1L, 1L, 100L, 2026, 38, BigDecimal.valueOf(30), BigDecimal.valueOf(40)),
                new ScenarioAllocationSnapshotItem(2L, 1L, 100L, 2026, 39, BigDecimal.valueOf(40), BigDecimal.valueOf(40)),
                new ScenarioAllocationSnapshotItem(3L, 1L, 100L, 2026, 40, BigDecimal.valueOf(20), BigDecimal.valueOf(40)),
                new ScenarioAllocationSnapshotItem(4L, 1L, 100L, 2026, 41, BigDecimal.valueOf(0), BigDecimal.valueOf(40))
        );
        when(loadSnapshotPort.findByScenarioId(1L)).thenReturn(snapshots);

        // Nhu cầu giả định: 1 người cần 20h từ tuần 38 đến 39
        ScenarioDemand demand = ScenarioDemand.create(1L, "Hỗ trợ dự án", 1, 2026, 38, 2026, 39, BigDecimal.valueOf(20), "Java");
        demand.setId(10L);
        when(loadDemandPort.findByScenarioId(1L)).thenReturn(List.of(demand));

        Employee emp = new Employee(
                new EmployeeId(100L),
                new UserId(200L),
                10L,
                "EMP100",
                "Lê Văn B",
                "Backend Java",
                LocalDate.of(2025, 1, 1),
                null,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(List.of(emp));

        ScenarioSimulationResult result = service.getSimulationResult(1L);

        assertNotNull(result);
        assertEquals(4, result.weeklyMetrics().size());

        // Tuần 38: Snapshot Allocated = 30h, Demand = 20h -> Workload = 50h, Available = 40h -> Quá tải (OVERLOADED)
        WeeklySimulationMetricResult w38 = result.weeklyMetrics().get(0);
        assertEquals(38, w38.weekNumber());
        assertEquals(BigDecimal.valueOf(30), w38.snapshotAllocatedHours());
        assertEquals(BigDecimal.valueOf(20), w38.demandHours());
        assertEquals(BigDecimal.valueOf(50), w38.scenarioWorkloadHours());
        assertEquals(BigDecimal.valueOf(40), w38.availableHours());
        assertTrue(w38.isOverloaded());
        assertEquals(CapacityStatus.OVERLOADED, w38.status());
        assertEquals(BigDecimal.valueOf(10.0).setScale(2), w38.excessHours());
        assertEquals(BigDecimal.valueOf(125.0).setScale(1), w38.utilizationPercentage());

        // Tuần 39: Snapshot Allocated = 40h, Demand = 20h -> Workload = 60h, Available = 40h -> Quá tải (OVERLOADED)
        WeeklySimulationMetricResult w39 = result.weeklyMetrics().get(1);
        assertEquals(39, w39.weekNumber());
        assertEquals(BigDecimal.valueOf(60), w39.scenarioWorkloadHours());
        assertTrue(w39.isOverloaded());

        // Tuần 40: Demand không còn hiệu lực -> Workload = 20h, Available = 40h -> Tối ưu / 50%
        WeeklySimulationMetricResult w40 = result.weeklyMetrics().get(2);
        assertEquals(40, w40.weekNumber());
        assertEquals(BigDecimal.valueOf(0), w40.demandHours());
        assertEquals(BigDecimal.valueOf(20), w40.scenarioWorkloadHours());
        assertEquals(CapacityStatus.OPTIMAL, w40.status());
        assertFalse(w40.isOverloaded());

        // Tuần 41: Workload = 0h -> Nhàn rỗi (UNDERUTILIZED)
        WeeklySimulationMetricResult w41 = result.weeklyMetrics().get(3);
        assertEquals(41, w41.weekNumber());
        assertEquals(BigDecimal.valueOf(0), w41.scenarioWorkloadHours());
        assertEquals(CapacityStatus.UNDERUTILIZED, w41.status());

        // Kiểm tra danh sách nhân viên snapshot baseline
        assertEquals(1, result.employeeSnapshots().size());
        assertEquals("EMP100", result.employeeSnapshots().get(0).employeeCode());
        assertEquals("Lê Văn B", result.employeeSnapshots().get(0).fullName());

        // Regression Check (#1): Mỗi nhân sự có đúng 1 cell per week, không duplicate
        var empSnapshot = result.employeeSnapshots().get(0);
        assertEquals(4, empSnapshot.cells().size(), "Mỗi nhân sự chỉ có đúng 4 cells tương ứng 4 tuần mục tiêu, không trùng lặp");
        var weekNumbers = empSnapshot.cells().stream().map(c -> c.weekNumber()).toList();
        assertEquals(List.of(38, 39, 40, 41), weekNumbers, "Danh sách tuần trong cells phải duy nhất và theo thứ tự");

        // Kiểm tra Audit Log KHÔNG được gọi khi GET simulation result
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("NCL-08-CN-002-TC-02: Năng lực còn lại đủ cho nhu cầu mới -> Không nhân sự nào vỡ kế hoạch, hiện số giờ dư")
    void testSimulationCalculation_SufficientCapacity_ReportsNoOverload() {
        List<ScenarioAllocationSnapshotItem> snapshots = List.of(
                new ScenarioAllocationSnapshotItem(1L, 1L, 100L, 2026, 38, BigDecimal.valueOf(10), BigDecimal.valueOf(40))
        );
        when(loadSnapshotPort.findByScenarioId(1L)).thenReturn(snapshots);

        // Nhu cầu giả định 15h -> Workload = 25h <= 40h Available -> Tối ưu / dư 15h
        ScenarioDemand demand = ScenarioDemand.create(1L, "Nhu cầu nhẹ", 1, 2026, 38, 2026, 38, BigDecimal.valueOf(15), "Tester");
        demand.setId(11L);
        when(loadDemandPort.findByScenarioId(1L)).thenReturn(List.of(demand));

        Employee emp = new Employee(
                new EmployeeId(100L), new UserId(200L), 10L, "EMP100", "Lê Văn B", "Backend",
                LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(List.of(emp));

        ScenarioSimulationResult result = service.getSimulationResult(1L);

        assertNotNull(result);
        assertEquals(4, result.weeklyMetrics().size());
        // Kiểm tra tuần 38 (có demand 15h) không bị quá tải
        WeeklySimulationMetricResult metric = result.weeklyMetrics().get(0);
        assertFalse(metric.isOverloaded());
        assertEquals(CapacityStatus.OPTIMAL, metric.status());
        assertEquals(BigDecimal.valueOf(15.0).setScale(2), metric.remainingHours());
        assertEquals(BigDecimal.ZERO.setScale(2), metric.excessHours());
        // Tất cả 4 tuần đều không bị vỡ kế hoạch / quá tải
        assertTrue(result.weeklyMetrics().stream().noneMatch(WeeklySimulationMetricResult::isOverloaded));
    }

    @Test
    @DisplayName("Regression: Kịch bản vắt qua năm (2026-W52 đến 2027-W02) tính demand chính xác theo YearWeek")
    void testSimulationCalculation_CrossYearScenarioDemand_HandledCorrectly() {
        // Scenario bắt đầu từ 2026-W52, kéo dài 4 tuần: [2026-W52, 2026-W53, 2027-W01, 2027-W02]
        ResourceScenario crossYearScenario = ResourceScenario.createNew(
                "SCN-CROSS", "Kịch bản vắt năm", "Mô tả", 10L, 2026, 52, 4, 100L
        );
        crossYearScenario.setId(99L);
        when(loadScenarioPort.findById(99L)).thenReturn(Optional.of(crossYearScenario));

        // Snapshot: 1 nhân sự có 40h available mỗi tuần, 0h allocated
        List<ScenarioAllocationSnapshotItem> snapshots = List.of(
                new ScenarioAllocationSnapshotItem(1L, 99L, 100L, 2026, 52, BigDecimal.ZERO, BigDecimal.valueOf(40)),
                new ScenarioAllocationSnapshotItem(2L, 99L, 100L, 2026, 53, BigDecimal.ZERO, BigDecimal.valueOf(40)),
                new ScenarioAllocationSnapshotItem(3L, 99L, 100L, 2027, 1, BigDecimal.ZERO, BigDecimal.valueOf(40)),
                new ScenarioAllocationSnapshotItem(4L, 99L, 100L, 2027, 2, BigDecimal.ZERO, BigDecimal.valueOf(40))
        );
        when(loadSnapshotPort.findByScenarioId(99L)).thenReturn(snapshots);

        // Demand: Chỉ phát sinh ở 2027-W01 đến 2027-W02 (40h/tuần)
        ScenarioDemand crossYearDemand = ScenarioDemand.create(
                99L, "Demand năm mới", 1, 2027, 1, 2027, 2, BigDecimal.valueOf(40), "Golang"
        );
        crossYearDemand.setId(20L);
        when(loadDemandPort.findByScenarioId(99L)).thenReturn(List.of(crossYearDemand));

        Employee emp = new Employee(
                new EmployeeId(100L),
                new UserId(200L),
                10L,
                "EMP100",
                "Lê Văn B",
                "Backend",
                LocalDate.of(2025, 1, 1),
                null,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(List.of(emp));

        ScenarioSimulationResult result = service.getSimulationResult(99L);

        assertNotNull(result);
        assertEquals(4, result.weeklyMetrics().size());

        // Tuần 1: 2026-W52 -> demand = 0
        WeeklySimulationMetricResult m1 = result.weeklyMetrics().get(0);
        assertEquals(2026, m1.year());
        assertEquals(52, m1.weekNumber());
        assertEquals(BigDecimal.valueOf(0), m1.demandHours(), "Tuần 2026-W52 không được có demand");
        assertEquals(BigDecimal.valueOf(0), m1.scenarioWorkloadHours());

        // Tuần 2: 2026-W53 -> demand = 0
        WeeklySimulationMetricResult m2 = result.weeklyMetrics().get(1);
        assertEquals(2026, m2.year());
        assertEquals(53, m2.weekNumber());
        assertEquals(BigDecimal.valueOf(0), m2.demandHours(), "Tuần 2026-W53 không được có demand");
        assertEquals(BigDecimal.valueOf(0), m2.scenarioWorkloadHours());

        // Tuần 3: 2027-W01 -> demand = 40h
        WeeklySimulationMetricResult m3 = result.weeklyMetrics().get(2);
        assertEquals(2027, m3.year());
        assertEquals(1, m3.weekNumber());
        assertEquals(BigDecimal.valueOf(40), m3.demandHours(), "Tuần 2027-W01 phải có demand = 40");
        assertEquals(BigDecimal.valueOf(40), m3.scenarioWorkloadHours());

        // Tuần 4: 2027-W02 -> demand = 40h
        WeeklySimulationMetricResult m4 = result.weeklyMetrics().get(3);
        assertEquals(2027, m4.year());
        assertEquals(2, m4.weekNumber());
        assertEquals(BigDecimal.valueOf(40), m4.demandHours(), "Tuần 2027-W02 phải có demand = 40");
        assertEquals(BigDecimal.valueOf(40), m4.scenarioWorkloadHours());
    }

    @Test
    @DisplayName("Không có quyền RESOURCE_SCENARIO_READ -> Ném PermissionDeniedException")
    void testSimulation_NoPermission_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_READ))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_READ));

        assertThrows(PermissionDeniedException.class, () -> service.getSimulationResult(1L));
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Kịch bản ngoài phạm vi branch -> Ném PermissionDeniedException")
    void testSimulation_OutOfScope_ThrowsPermissionDenied() {
        ResourceScenario outOfScopeScenario = ResourceScenario.createNew(
                "SCN-OUT", "Kịch bản ngoài phạm vi", "Mô tả", 20L, 2026, 38, 4, 103L
        );
        outOfScopeScenario.setId(1L);
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_READ)).thenReturn(103L);
        when(loadUserPort.findById(new UserId(103L))).thenReturn(Optional.of(vt03User));
        when(loadScenarioPort.findById(1L)).thenReturn(Optional.of(outOfScopeScenario));
        when(loadOrgUnitPort.existsInOrgUnitBranch(20L, 10L)).thenReturn(false);
        lenient().when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 20L)).thenReturn(true);

        assertThrows(PermissionDeniedException.class, () -> service.getSimulationResult(1L));
        verify(loadOrgUnitPort, never()).existsInOrgUnitBranch(10L, 20L);
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Snapshot tham chiếu nhân sự không còn tồn tại -> vẫn trả kết quả dự phòng")
    void testSimulation_MissingEmployee_UsesSnapshotFallback() {
        when(loadSnapshotPort.findByScenarioId(1L)).thenReturn(List.of(
                new ScenarioAllocationSnapshotItem(1L, 1L, 999L, 2026, 38,
                        BigDecimal.valueOf(8), BigDecimal.valueOf(40))
        ));
        when(loadDemandPort.findByScenarioId(1L)).thenReturn(List.of());
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(List.of());

        ScenarioSimulationResult result = service.getSimulationResult(1L);

        assertEquals(1, result.employeeSnapshots().size());
        assertEquals("EMP-999", result.employeeSnapshots().get(0).employeeCode());
        assertEquals("Nhân viên 999", result.employeeSnapshots().get(0).fullName());
    }

    @Test
    @DisplayName("Bổ sung NCL-08-CN-002: Kiểm tra trả về danh sách nhân sự vượt năng lực/vỡ kế hoạch (Overloaded Employees)")
    void testSimulationCalculation_IdentifiesOverloadedPersonnel_ReturnsOverloadedEmployeesList() {
        // Employee 101: Allocated 45h / Available 40h (Overloaded 5h at week 38)
        // Employee 102: Allocated 30h / Available 40h (Optimal at week 38)
        List<ScenarioAllocationSnapshotItem> snapshots = List.of(
                new ScenarioAllocationSnapshotItem(1L, 1L, 101L, 2026, 38, BigDecimal.valueOf(45), BigDecimal.valueOf(40)),
                new ScenarioAllocationSnapshotItem(2L, 1L, 102L, 2026, 38, BigDecimal.valueOf(30), BigDecimal.valueOf(40))
        );
        when(loadSnapshotPort.findByScenarioId(1L)).thenReturn(snapshots);
        when(loadDemandPort.findByScenarioId(1L)).thenReturn(List.of());

        Employee emp1 = new Employee(
                new EmployeeId(101L), new UserId(201L), 10L, "EMP101", "Nguyễn Văn Overload", "Senior Java",
                LocalDate.of(2024, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        Employee emp2 = new Employee(
                new EmployeeId(102L), new UserId(202L), 10L, "EMP102", "Trần Văn Normal", "Frontend",
                LocalDate.of(2024, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(List.of(emp1, emp2));

        ScenarioSimulationResult result = service.getSimulationResult(1L);

        assertNotNull(result);
        assertNotNull(result.overloadedEmployees());
        assertEquals(1, result.overloadedEmployees().size(), "Phải trả về đúng 1 nhân sự bị vượt năng lực");
        
        com.hrm.employeemanagement.application.dto.scenario.OverloadedEmployeeResult overloaded = result.overloadedEmployees().get(0);
        assertEquals(101L, overloaded.employeeId());
        assertEquals("EMP101", overloaded.employeeCode());
        assertEquals("Nguyễn Văn Overload", overloaded.fullName());
        assertEquals(38, overloaded.weekNumber());
        assertEquals(BigDecimal.valueOf(45), overloaded.allocatedHours());
        assertEquals(BigDecimal.valueOf(40), overloaded.availableHours());
        assertEquals(BigDecimal.valueOf(5.0).setScale(2), overloaded.excessHours());
        assertEquals(CapacityStatus.OVERLOADED, overloaded.status());
    }

    @Test
    @DisplayName("NCL-08-CN-002: Kiểm tra sắp xếp overloadedEmployees đúng theo Năm -> Tuần -> Tên khi vắt qua năm")
    void testSimulationCalculation_SortsOverloadedEmployeesByYearAndWeekChronologically() {
        ResourceScenario crossYearScenario = ResourceScenario.createNew(
                "SCN-SORT", "Kịch bản test sort", "Mô tả", 10L, 2026, 53, 2, 100L
        );
        crossYearScenario.setId(88L);
        when(loadScenarioPort.findById(88L)).thenReturn(Optional.of(crossYearScenario));

        // Employee 101: Overloaded ở 2027-W01
        // Employee 102: Overloaded ở 2026-W53
        List<ScenarioAllocationSnapshotItem> snapshots = List.of(
                new ScenarioAllocationSnapshotItem(1L, 88L, 101L, 2027, 1, BigDecimal.valueOf(50), BigDecimal.valueOf(40)),
                new ScenarioAllocationSnapshotItem(2L, 88L, 102L, 2026, 53, BigDecimal.valueOf(45), BigDecimal.valueOf(40))
        );
        when(loadSnapshotPort.findByScenarioId(88L)).thenReturn(snapshots);
        when(loadDemandPort.findByScenarioId(88L)).thenReturn(List.of());

        Employee emp1 = new Employee(
                new EmployeeId(101L), new UserId(201L), 10L, "EMP101", "An B", "Senior Java",
                LocalDate.of(2024, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        Employee emp2 = new Employee(
                new EmployeeId(102L), new UserId(202L), 10L, "EMP102", "Bình C", "Frontend",
                LocalDate.of(2024, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(List.of(emp1, emp2));

        ScenarioSimulationResult result = service.getSimulationResult(88L);

        assertNotNull(result);
        assertEquals(2, result.overloadedEmployees().size());

        // Bản ghi 1 phải là 2026-W53 (Bình C)
        var first = result.overloadedEmployees().get(0);
        assertEquals(2026, first.year());
        assertEquals(53, first.weekNumber());

        // Bản ghi 2 phải là 2027-W01 (An B)
        var second = result.overloadedEmployees().get(1);
        assertEquals(2027, second.year());
        assertEquals(1, second.weekNumber());
    }

    @Test
    @DisplayName("NCL-08-CN-002: Nhu cầu kịch bản (Scenario Demand) được phân bổ khiến nhân sự cụ thể bị overload (A: 35+20=55h/40h, B: 10h/40h)")
    void testSimulationCalculation_DemandCausesSpecificEmployeeOverload() {
        ResourceScenario demandScenario = ResourceScenario.createNew(
                "SCN-DEMAND", "Kịch bản test demand overload", "Mô tả", 10L, 2026, 38, 1, 100L
        );
        demandScenario.setId(77L);
        when(loadScenarioPort.findById(77L)).thenReturn(Optional.of(demandScenario));

        // Snapshot: Employee A (101L) baseline = 35h / 40h, Employee B (102L) baseline = 10h / 40h
        List<ScenarioAllocationSnapshotItem> snapshots = List.of(
                new ScenarioAllocationSnapshotItem(1L, 77L, 101L, 2026, 38, BigDecimal.valueOf(35), BigDecimal.valueOf(40)),
                new ScenarioAllocationSnapshotItem(2L, 77L, 102L, 2026, 38, BigDecimal.valueOf(10), BigDecimal.valueOf(40))
        );
        when(loadSnapshotPort.findByScenarioId(77L)).thenReturn(snapshots);

        // Demand: +20h/tuần cho vị trí "Senior Java" (khớp với Employee A)
        ScenarioDemand demand = ScenarioDemand.create(
                77L, "Nhu cầu Backend", 1, 2026, 38, 2026, 38, BigDecimal.valueOf(20), "Senior Java"
        );
        demand.setId(701L);
        when(loadDemandPort.findByScenarioId(77L)).thenReturn(List.of(demand));

        Employee empA = new Employee(
                new EmployeeId(101L), new UserId(201L), 10L, "EMP101", "Nguyễn Văn A", "Senior Java",
                LocalDate.of(2024, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        Employee empB = new Employee(
                new EmployeeId(102L), new UserId(202L), 10L, "EMP102", "Trần Văn B", "Frontend",
                LocalDate.of(2024, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(List.of(empA, empB));

        ScenarioSimulationResult result = service.getSimulationResult(77L);

        assertNotNull(result);
        assertEquals(1, result.overloadedEmployees().size(), "Chỉ duy nhất Employee A bị overload do được nhận 20h demand");

        com.hrm.employeemanagement.application.dto.scenario.OverloadedEmployeeResult overloaded = result.overloadedEmployees().get(0);
        assertEquals(101L, overloaded.employeeId());
        assertEquals("EMP101", overloaded.employeeCode());
        assertEquals("Nguyễn Văn A", overloaded.fullName());
        assertEquals(38, overloaded.weekNumber());
        assertEquals(BigDecimal.valueOf(55.0).setScale(2), overloaded.allocatedHours(), "Allocated của Employee A phải là 35h baseline + 20h demand = 55h");
        assertEquals(BigDecimal.valueOf(40), overloaded.availableHours());
        assertEquals(BigDecimal.valueOf(15.0).setScale(2), overloaded.excessHours(), "Vượt định mức 15h");
        assertEquals(CapacityStatus.OVERLOADED, overloaded.status());
    }

    @Test
    @DisplayName("HIGH 1 Fix: Nhu cầu kịch bản yêu cầu vai trò Java nhưng snapshot chỉ có Tester/HR -> Không phân bổ nhầm sang Tester/HR")
    void testSimulation_UnmatchedDemandRole_DoesNotFallbackToAllPersonnel() {
        ResourceScenario demandScenario = ResourceScenario.createNew(
                "SCN-UNMATCH", "Kịch bản test unmatched role", "Mô tả", 10L, 2026, 38, 1, 100L
        );
        demandScenario.setId(66L);
        when(loadScenarioPort.findById(66L)).thenReturn(Optional.of(demandScenario));

        // Snapshot có 2 nhân sự: Tester (101L) baseline = 30h/40h, HR (102L) baseline = 20h/40h
        List<ScenarioAllocationSnapshotItem> snapshots = List.of(
                new ScenarioAllocationSnapshotItem(1L, 66L, 101L, 2026, 38, BigDecimal.valueOf(30), BigDecimal.valueOf(40)),
                new ScenarioAllocationSnapshotItem(2L, 66L, 102L, 2026, 38, BigDecimal.valueOf(20), BigDecimal.valueOf(40))
        );
        when(loadSnapshotPort.findByScenarioId(66L)).thenReturn(snapshots);

        // Demand: +40h cho role "Java Developer" (Không có ai đáp ứng)
        ScenarioDemand demand = ScenarioDemand.create(
                66L, "Nhu cầu Java Dev", 1, 2026, 38, 2026, 38, BigDecimal.valueOf(40), "Java Developer"
        );
        demand.setId(601L);
        when(loadDemandPort.findByScenarioId(66L)).thenReturn(List.of(demand));

        Employee empTester = new Employee(
                new EmployeeId(101L), new UserId(201L), 10L, "EMP101", "Phạm Văn Tester", "Tester",
                LocalDate.of(2024, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        Employee empHR = new Employee(
                new EmployeeId(102L), new UserId(202L), 10L, "EMP102", "Đỗ Thị HR", "HR Specialist",
                LocalDate.of(2024, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(List.of(empTester, empHR));

        ScenarioSimulationResult result = service.getSimulationResult(66L);

        assertNotNull(result);
        assertTrue(result.overloadedEmployees().isEmpty(), "Không nhân sự nào bị quá tải vì nhu cầu Java không được gán nhầm cho Tester/HR");

        // Kiểm tra simulatedAlloc của Tester và HR giữ nguyên baseline (không bị cộng 20h mỗi người)
        var testerSnapshot = result.employeeSnapshots().stream().filter(e -> e.employeeId().equals(101L)).findFirst().orElseThrow();
        assertEquals(BigDecimal.valueOf(30), testerSnapshot.cells().get(0).allocatedHours());

        var hrSnapshot = result.employeeSnapshots().stream().filter(e -> e.employeeId().equals(102L)).findFirst().orElseThrow();
        assertEquals(BigDecimal.valueOf(20), hrSnapshot.cells().get(0).allocatedHours());
    }

    @Test
    @DisplayName("MEDIUM 3 Fix: Thiếu snapshot item ở một tuần -> Fallback năng lực về standard hours (40h) thay vì 0h")
    void testSimulation_MissingSnapshotItem_FallbacksToStandardHours() {
        // Scenario kéo dài 2 tuần: 2026-W38 và 2026-W39
        ResourceScenario scenarioMissing = ResourceScenario.createNew(
                "SCN-MISSING", "Kịch bản thiếu snapshot tuần", "Mô tả", 10L, 2026, 38, 2, 100L
        );
        scenarioMissing.setId(55L);
        when(loadScenarioPort.findById(55L)).thenReturn(Optional.of(scenarioMissing));

        // Employee 101 chỉ có snapshot item ở week 38 (30h allocated / 40h available), hoàn toàn THIẾU snapshot item ở week 39
        List<ScenarioAllocationSnapshotItem> snapshots = List.of(
                new ScenarioAllocationSnapshotItem(1L, 55L, 101L, 2026, 38, BigDecimal.valueOf(30), BigDecimal.valueOf(40))
        );
        when(loadSnapshotPort.findByScenarioId(55L)).thenReturn(snapshots);
        when(loadDemandPort.findByScenarioId(55L)).thenReturn(List.of());

        Employee emp1 = new Employee(
                new EmployeeId(101L), new UserId(201L), 10L, "EMP101", "Lê Văn A", "Dev",
                LocalDate.of(2024, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(List.of(emp1));

        ScenarioSimulationResult result = service.getSimulationResult(55L);

        assertNotNull(result);
        assertEquals(1, result.employeeSnapshots().size());

        var emp1Snapshot = result.employeeSnapshots().get(0);
        assertEquals(2, emp1Snapshot.cells().size(), "Có 2 cells tương ứng 2 tuần mục tiêu");

        // Cell 0 (Week 38 - có snapshot item): availableHours = 40h
        assertEquals(38, emp1Snapshot.cells().get(0).weekNumber());
        assertEquals(BigDecimal.valueOf(40), emp1Snapshot.cells().get(0).availableHours());

        // Cell 1 (Week 39 - khuyết snapshot item): availableHours phải fallback về standard capacity 40h thay vì 0h
        assertEquals(39, emp1Snapshot.cells().get(1).weekNumber());
        assertEquals(BigDecimal.valueOf(40), emp1Snapshot.cells().get(1).availableHours(), "Khi thiếu snapshot item ở W39, availableHours phải fallback về 40h");
        assertFalse(emp1Snapshot.cells().get(1).isOverloaded(), "Không bị đánh dấu overload do sẵn sàng năng lực fallback 40h");
    }
}
