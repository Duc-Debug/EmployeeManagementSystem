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
                "Backend",
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

        // Kiểm tra Audit Log được lưu (NCL-08-CN-002-TC-04)
        verify(saveAuditLogPort, times(1)).save(any(com.hrm.employeemanagement.domain.audit.AuditLog.class));
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
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 20L)).thenReturn(false);

        assertThrows(PermissionDeniedException.class, () -> service.getSimulationResult(1L));
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
}
