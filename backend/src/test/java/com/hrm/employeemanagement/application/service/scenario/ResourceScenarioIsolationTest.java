package com.hrm.employeemanagement.application.service.scenario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.application.dto.scenario.AddScenarioDemandCommand;
import com.hrm.employeemanagement.application.dto.scenario.CreateScenarioCommand;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioSimulationResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.DeleteWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.DeleteScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.dto.scenario.PatchScenarioCommand;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioSharePort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;
import com.hrm.employeemanagement.domain.scenario.ScenarioAllocationSnapshotItem;
import com.hrm.employeemanagement.domain.scenario.ScenarioDemand;
import java.time.LocalDate;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResourceScenarioIsolationTest {

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
    private SaveScenarioDemandPort saveDemandPort;
    private LoadScenarioDemandPort loadDemandPort;
    private DeleteScenarioDemandPort deleteDemandPort;
    private LoadCapacityThresholdPort loadCapacityThresholdPort;
    private SaveAuditLogPort saveAuditLogPort;
    private LoadScenarioSharePort loadScenarioSharePort;
    private LoadProjectPort loadProjectPort;
    private SaveAuditLogInNewTransactionPort deniedAuditLogPort;

    // Mock ports for real allocation write methods to prove they are NEVER called
    private SaveWeeklyProjectAllocationPort realSaveAllocationPort;
    private DeleteWeeklyProjectAllocationPort realDeleteAllocationPort;

    private ResourceScenarioService scenarioService;
    private ScenarioDemandService demandService;
    private ScenarioSimulationCalculationService simulationService;

    private User vt03User;
    private OrgUnit branchUnit;
    private Employee emp1;

    // Simulated real allocations state
    private List<WeeklyProjectAllocation> realAllocations;
    // Simulated scenario snapshot state
    private List<ScenarioAllocationSnapshotItem> storedSnapshots;
    // Simulated scenario demands state
    private List<ScenarioDemand> storedDemands;
    // Stored scenario state
    private ResourceScenario storedScenario;

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
        saveDemandPort = mock(SaveScenarioDemandPort.class);
        loadDemandPort = mock(LoadScenarioDemandPort.class);
        deleteDemandPort = mock(DeleteScenarioDemandPort.class);
        loadCapacityThresholdPort = mock(LoadCapacityThresholdPort.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);
        loadScenarioSharePort = mock(LoadScenarioSharePort.class);
        loadProjectPort = mock(LoadProjectPort.class);
        deniedAuditLogPort = mock(SaveAuditLogInNewTransactionPort.class);

        realSaveAllocationPort = mock(SaveWeeklyProjectAllocationPort.class);
        realDeleteAllocationPort = mock(DeleteWeeklyProjectAllocationPort.class);

        demandService = new ScenarioDemandService(
                authorizationService,
                loadUserPort,
                loadOrgUnitPort,
                loadScenarioPort,
                saveDemandPort,
                loadDemandPort,
                deleteDemandPort,
                saveAuditLogPort
        );

        simulationService = new ScenarioSimulationCalculationService(
                authorizationService,
                loadUserPort,
                loadOrgUnitPort,
                loadEmployeePort,
                loadScenarioPort,
                loadDemandPort,
                loadSnapshotPort,
                loadCapacityThresholdPort,
                loadScenarioSharePort,
                loadProjectPort,
                deniedAuditLogPort,
                saveAuditLogPort
        );

        scenarioService = new ResourceScenarioService(
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
                saveAuditLogPort,
                loadScenarioSharePort,
                loadProjectPort,
                simulationService,
                deniedAuditLogPort
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

        branchUnit = mock(OrgUnit.class);
        when(branchUnit.getId()).thenReturn(new OrgUnitId(10L));
        when(branchUnit.getUnitName()).thenReturn("Phòng Kỹ Thuật");
        when(branchUnit.getTreePath()).thenReturn("/10/");

        emp1 = new Employee(
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
                EmployeeStatus.ACTIVE
        );

        when(authorizationService.require(any())).thenReturn(103L);
        when(loadUserPort.findById(new UserId(103L))).thenReturn(Optional.of(vt03User));
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);
        when(loadOrgUnitPort.findById(new OrgUnitId(10L))).thenReturn(Optional.of(branchUnit));
        when(loadOrgUnitPort.findSubTree(any())).thenReturn(List.of(branchUnit));
        when(loadEmployeePort.findActiveByOrgUnitIds(anyList())).thenReturn(List.of(emp1));
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(List.of(emp1));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadCapacityThresholdPort.findByScope(any(), any())).thenReturn(Optional.empty());

        realAllocations = new ArrayList<>();
        realAllocations.add(new WeeklyProjectAllocation(1001L, 1L, 501L, YearWeek.of(2026, 38), BigDecimal.valueOf(32)));

        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList()))
                .thenAnswer(inv -> new ArrayList<>(realAllocations));

        storedSnapshots = new ArrayList<>();
        doAnswer(inv -> {
            List<ScenarioAllocationSnapshotItem> items = inv.getArgument(0);
            storedSnapshots.addAll(items);
            return null;
        }).when(saveSnapshotPort).saveAll(anyList());
        when(loadSnapshotPort.findByScenarioId(anyLong())).thenAnswer(inv -> storedSnapshots);

        storedDemands = new ArrayList<>();
        when(saveDemandPort.save(any(ScenarioDemand.class))).thenAnswer(inv -> {
            ScenarioDemand d = inv.getArgument(0);
            if (d.getId() == null) d.setId((long) (storedDemands.size() + 1));
            storedDemands.add(d);
            return d;
        });
        when(loadDemandPort.findByScenarioId(anyLong())).thenAnswer(inv -> storedDemands);
        when(loadDemandPort.findById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return storedDemands.stream().filter(d -> d.getId().equals(id)).findFirst();
        });

        storedScenario = ResourceScenario.createNew("SCN-01", "Kịch bản", "", 10L, 2026, 38, 8, 103L);
        storedScenario.setId(1L);

        when(saveScenarioPort.save(any(ResourceScenario.class))).thenAnswer(inv -> {
            ResourceScenario s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(1L);
            }
            storedScenario = s;
            return s;
        });
        when(loadScenarioPort.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(storedScenario));
    }

    @Test
    @DisplayName("QTN-14 & AC-02: Thao tác scenario tuyệt đối KHÔNG đụng dữ liệu phân bổ thật")
    void testScenarioModification_DoesNotAffectRealAllocation() {
        // 1. VT-03 tạo scenario
        CreateScenarioCommand createCmd = new CreateScenarioCommand(
                "SCN-01", "Kịch bản mô phỏng", "Mô tả", 10L, 2026, 38, 8
        );
        ScenarioResult scenario = scenarioService.createScenario(createCmd);
        assertNotNull(scenario);

        // 2. VT-03 thêm demand lớn (ví dụ 10 người x 40h)
        AddScenarioDemandCommand demandCmd = new AddScenarioDemandCommand(
                scenario.id(), "Nhu cầu lớn", 10, 2026, 38, 2026, 42, BigDecimal.valueOf(40), "Fullstack"
        );
        demandService.addDemand(demandCmd);

        // 3. VT-03 tính toán kết quả mô phỏng
        ScenarioSimulationResult simResult = simulationService.getSimulationResult(scenario.id());
        assertNotNull(simResult);
        // Tuần 38: Workload = 32h snapshot + 400h demand = 432h
        assertEquals(BigDecimal.valueOf(432), simResult.weeklyMetrics().get(0).scenarioWorkloadHours());

        // 4. KIỂM TRA INVARIANT QUAN TRỌNG NHẤT: Phân bổ thật hoàn toàn không bị chạm vào
        assertEquals(1, realAllocations.size(), "Dữ liệu allocation thật không bị thêm bớt bản ghi nào");
        assertEquals(BigDecimal.valueOf(32), realAllocations.get(0).getAllocatedHours(), "Số giờ phân bổ thật không bị thay đổi");
        verifyNoInteractions(realSaveAllocationPort);
        verifyNoInteractions(realDeleteAllocationPort);
    }

    @Test
    @DisplayName("QTN-14 & AC-02: Khi phân bổ thật thay đổi sau đó, snapshot và simulation kịch bản KHÔNG bị đổi")
    void testRealAllocationChange_DoesNotAffectExistingScenarioSnapshot() {
        // 1. Tạo scenario tại thời điểm T0 (lúc phân bổ thật = 32h)
        CreateScenarioCommand createCmd = new CreateScenarioCommand(
                "SCN-01", "Kịch bản mô phỏng", "Mô tả", 10L, 2026, 38, 8
        );
        ScenarioResult scenario = scenarioService.createScenario(createCmd);
        assertNotNull(scenario);

        // 2. Giả sử tại thời điểm T1 (sau khi tạo scenario), phân bổ thật bị thay đổi từ 32h lên 60h
        realAllocations.get(0).updateAllocatedHours(BigDecimal.valueOf(60));
        assertEquals(BigDecimal.valueOf(60), realAllocations.get(0).getAllocatedHours());

        // 3. Xem lại kết quả mô phỏng kịch bản: Phải vẫn giữ 32h từ snapshot, KHÔNG bị ảnh hưởng bởi 60h thật
        ScenarioSimulationResult simResult = simulationService.getSimulationResult(scenario.id());
        assertNotNull(simResult);
        assertEquals(
                BigDecimal.valueOf(32),
                simResult.weeklyMetrics().get(0).snapshotAllocatedHours(),
                "Kịch bản sandbox phải đọc từ snapshot tại thời điểm tạo, không bị ảnh hưởng khi allocation thật thay đổi"
        );
    }

    @Test
    @DisplayName("QTN-14 & BR-06 & BR-07: Lưu kịch bản (SAVED) đóng băng snapshot_data; thay đổi phân bổ thật không làm đổi kết quả xem; sửa kịch bản chuyển về DRAFT")
    void testSavedScenarioSnapshotIsolation_AndTransitionToDraftOnEdit() {
        // 1. Tạo scenario DRAFT
        CreateScenarioCommand createCmd = new CreateScenarioCommand(
                "SCN-01", "Kịch bản mô phỏng", "Mô tả", 10L, 2026, 38, 8
        );
        ScenarioResult scenario = scenarioService.createScenario(createCmd);
        assertEquals("draft", scenario.status());

        // Thêm demand 40h
        demandService.addDemand(new AddScenarioDemandCommand(
                scenario.id(), "Nhu cầu A", 1, 2026, 38, 2026, 38, BigDecimal.valueOf(40), "Backend"
        ));

        // 2. Lưu kịch bản (Save snapshot)
        ScenarioResult savedResult = scenarioService.saveScenario(scenario.id());
        assertEquals("saved", savedResult.status());
        assertNotNull(storedScenario.getSnapshotData(), "snapshot_data phải được lưu");

        // Đọc simulation tại thời điểm SAVED: 32h allocation thật + 40h demand = 72h
        ScenarioSimulationResult savedSim = simulationService.getSimulationResult(scenario.id());
        assertEquals(BigDecimal.valueOf(72), savedSim.weeklyMetrics().get(0).scenarioWorkloadHours());

        // 3. Giả lập biến động bên ngoài: Thay đổi dữ liệu phân bổ thật lên 100h
        realAllocations.get(0).updateAllocatedHours(BigDecimal.valueOf(100));

        // Khi gọi getSimulationResult trên kịch bản SAVED, BR-06 đảm bảo đọc trực tiếp từ snapshot_data
        ScenarioSimulationResult simAfterLiveChange = simulationService.getSimulationResult(scenario.id());
        assertEquals(BigDecimal.valueOf(72), simAfterLiveChange.weeklyMetrics().get(0).scenarioWorkloadHours(),
                "Kịch bản SAVED phải đọc trực tiếp từ snapshot_data, bảo đảm tuyệt đối không bị ảnh hưởng bởi biến động dữ liệu");

        // 4. BR-07: Khi chủ sở hữu chỉnh sửa kịch bản (patch name/note), kịch bản tự động chuyển về DRAFT
        PatchScenarioCommand patchCmd = new PatchScenarioCommand(
                scenario.id(), "Kịch bản đã sửa", "Ghi chú mới"
        );
        ScenarioResult patchedResult = scenarioService.patchScenario(patchCmd);
        assertEquals("draft", patchedResult.status(), "Sau khi sửa thông tin, trạng thái phải chuyển về DRAFT");
        assertEquals("Kịch bản đã sửa", patchedResult.name());
        assertEquals("Ghi chú mới", patchedResult.note());
    }
}
