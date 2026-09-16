package com.hrm.employeemanagement.application.service.scenario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hrm.employeemanagement.application.dto.scenario.EmployeeSnapshotCellResult;
import com.hrm.employeemanagement.application.dto.scenario.EmployeeSnapshotRowResult;
import com.hrm.employeemanagement.application.dto.scenario.OverloadedEmployeeResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioSimulationResult;
import com.hrm.employeemanagement.application.dto.scenario.WeeklySimulationMetricResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.GetScenarioSimulationResultUseCase;
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
import com.hrm.employeemanagement.domain.allocation.WeeklyCapacityMatrixPolicy;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;
import com.hrm.employeemanagement.domain.scenario.ScenarioAllocationSnapshotItem;
import com.hrm.employeemanagement.domain.scenario.ScenarioDemand;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class ScenarioSimulationCalculationService implements GetScenarioSimulationResultUseCase {

    private static final Logger log = LoggerFactory.getLogger(ScenarioSimulationCalculationService.class);

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadResourceScenarioPort loadScenarioPort;
    private final LoadScenarioDemandPort loadDemandPort;
    private final LoadScenarioSnapshotPort loadSnapshotPort;
    private final LoadCapacityThresholdPort loadCapacityThresholdPort;
    private final SaveAuditLogPort saveAuditLogPort;

    public ScenarioSimulationCalculationService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadEmployeePort loadEmployeePort,
            LoadResourceScenarioPort loadScenarioPort,
            LoadScenarioDemandPort loadDemandPort,
            LoadScenarioSnapshotPort loadSnapshotPort,
            LoadCapacityThresholdPort loadCapacityThresholdPort
    ) {
        this(authorizationService, loadUserPort, loadOrgUnitPort, loadEmployeePort, loadScenarioPort, loadDemandPort, loadSnapshotPort, loadCapacityThresholdPort, null);
    }

    public ScenarioSimulationCalculationService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadEmployeePort loadEmployeePort,
            LoadResourceScenarioPort loadScenarioPort,
            LoadScenarioDemandPort loadDemandPort,
            LoadScenarioSnapshotPort loadSnapshotPort,
            LoadCapacityThresholdPort loadCapacityThresholdPort,
            SaveAuditLogPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadScenarioPort = Objects.requireNonNull(loadScenarioPort, "LoadResourceScenarioPort must not be null");
        this.loadDemandPort = Objects.requireNonNull(loadDemandPort, "LoadScenarioDemandPort must not be null");
        this.loadSnapshotPort = Objects.requireNonNull(loadSnapshotPort, "LoadScenarioSnapshotPort must not be null");
        this.loadCapacityThresholdPort = Objects.requireNonNull(loadCapacityThresholdPort, "LoadCapacityThresholdPort must not be null");
        this.saveAuditLogPort = saveAuditLogPort;
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
    }

    @Override
    public ScenarioSimulationResult getSimulationResult(Long scenarioId) {
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_READ);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        ResourceScenario scenario = loadScenarioPort.findById(scenarioId)
                .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));

        validateReadScope(currentUser, scenario.getOrgUnitId());

        OrgUnit orgUnit = loadOrgUnitPort.findById(new OrgUnitId(scenario.getOrgUnitId())).orElse(null);
        String orgUnitName = orgUnit != null ? orgUnit.getUnitName() : "Không xác định";

        // 1. Nạp ngưỡng quá tải và nhàn rỗi theo QTN-23
        BigDecimal overloadThreshold = WeeklyCapacityMatrixPolicy.DEFAULT_OVERLOAD_THRESHOLD;
        BigDecimal idleThreshold = WeeklyCapacityMatrixPolicy.UNDERUTILIZED_THRESHOLD;
        Optional<CapacityThresholdConfig> configOpt = loadCapacityThresholdPort.findByScope(CapacityThresholdScope.ORG_UNIT, scenario.getOrgUnitId());
        if (configOpt.isEmpty()) {
            configOpt = loadCapacityThresholdPort.findByScope(CapacityThresholdScope.COMPANY, null);
        }
        if (configOpt.isPresent()) {
            overloadThreshold = configOpt.get().getOverloadThreshold();
            idleThreshold = configOpt.get().getIdleThreshold();
        }

        // 2. Nạp snapshot items (chỉ đọc) và nhu cầu giả định của kịch bản
        List<ScenarioAllocationSnapshotItem> snapshotItems = loadSnapshotPort.findByScenarioId(scenarioId);
        List<ScenarioDemand> demands = loadDemandPort.findByScenarioId(scenarioId);

        // 3. Xây dựng khoảng tuần mục tiêu
        List<YearWeek> targetWeeks = buildTargetWeeks(scenario.getFromYear(), scenario.getFromWeek(), scenario.getDurationWeeks());

        // Nhóm snapshot theo (year, week)
        Map<String, BigDecimal> snapshotAllocatedByWeek = new HashMap<>();
        Map<String, BigDecimal> snapshotAvailableByWeek = new HashMap<>();
        Map<Long, Map<String, ScenarioAllocationSnapshotItem>> snapshotByEmpAndWeek = new HashMap<>();

        for (ScenarioAllocationSnapshotItem item : snapshotItems) {
            String weekKey = item.getYearNumber() + "_" + item.getWeekNumber();
            snapshotAllocatedByWeek.merge(weekKey, item.getAllocatedHours(), BigDecimal::add);
            snapshotAvailableByWeek.merge(weekKey, item.getAvailableHours(), BigDecimal::add);

            snapshotByEmpAndWeek.computeIfAbsent(item.getEmployeeId(), k -> new HashMap<>())
                    .put(weekKey, item);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM");
        List<WeeklySimulationMetricResult> weeklyMetrics = new ArrayList<>();

        for (YearWeek yw : targetWeeks) {
            String weekKey = yw.year() + "_" + yw.weekNumber();
            String weekLabel = "T" + yw.weekNumber() + " (" + yw.getStartDate().format(dtf) + " - " + yw.getEndDate().format(dtf) + ")";

            BigDecimal snapshotAllocated = snapshotAllocatedByWeek.getOrDefault(weekKey, BigDecimal.ZERO);
            BigDecimal availableHours = snapshotAvailableByWeek.getOrDefault(weekKey, BigDecimal.ZERO);

            // Tính tổng nhu cầu giả định có hiệu lực trong tuần này
            BigDecimal demandHours = BigDecimal.ZERO;
            for (ScenarioDemand d : demands) {
                if (d.isActiveInWeek(yw)) {
                    demandHours = demandHours.add(d.getTotalHoursPerWeek());
                }
            }

            // Scenario workload = Snapshot Allocated + Demand Hours (Mô hình Section VI)
            BigDecimal scenarioWorkload = snapshotAllocated.add(demandHours);

            // Tái sử dụng logic tính toán / cảnh báo từ NCL-06-CN-002 (WeeklyCapacityMatrixPolicy)
            CapacityStatus status = WeeklyCapacityMatrixPolicy.determineStatus(scenarioWorkload, availableHours, overloadThreshold, idleThreshold);
            boolean isOverloaded = (status == CapacityStatus.OVERLOADED);
            BigDecimal excessHours = WeeklyCapacityMatrixPolicy.calculateExcessHours(scenarioWorkload, availableHours);
            BigDecimal remainingHours = WeeklyCapacityMatrixPolicy.calculateRemainingHours(availableHours, scenarioWorkload);
            BigDecimal utilizationPercentage = WeeklyCapacityMatrixPolicy.calculateUtilizationPercentage(scenarioWorkload, availableHours);

            weeklyMetrics.add(new WeeklySimulationMetricResult(
                    yw.year(),
                    yw.weekNumber(),
                    weekLabel,
                    snapshotAllocated,
                    demandHours,
                    scenarioWorkload,
                    availableHours,
                    remainingHours,
                    excessHours,
                    utilizationPercentage,
                    status,
                    isOverloaded
            ));
        }

        // 4. Xây dựng danh sách nhân sự snapshot (baseline breakdown)
        // 4. Xây dựng danh sách nhân sự snapshot & danh sách nhân sự vượt năng lực (Personnel level overload)
        List<Long> empIds = new ArrayList<>(snapshotByEmpAndWeek.keySet());
        List<com.hrm.employeemanagement.domain.employee.EmployeeId> employeeIds = empIds.stream()
                .map(com.hrm.employeemanagement.domain.employee.EmployeeId::new)
                .toList();
        List<Employee> employees = employeeIds.isEmpty() ? List.of() : loadEmployeePort.findAllByIdIn(employeeIds);
        Map<Long, Employee> employeeMap = employees.stream()
                .filter(Objects::nonNull)
                .filter(e -> e.getIdValue() != null)
                .collect(Collectors.toMap(Employee::getIdValue, e -> e, (e1, e2) -> e1));

        List<Long> missingEmployeeIds = empIds.stream()
                .filter(id -> !employeeMap.containsKey(id))
                .toList();
        if (!missingEmployeeIds.isEmpty()) {
            log.warn("Scenario simulation contains snapshot references to missing employees: scenarioId={}, employeeIds={}",
                    scenarioId, missingEmployeeIds);
        }

        List<EmployeeSnapshotRowResult> employeeSnapshots = new ArrayList<>();
        List<OverloadedEmployeeResult> overloadedEmployees = new ArrayList<>();

        for (Long empId : empIds) {
            Employee emp = employeeMap.get(empId);
            String empCode = emp != null ? emp.getEmployeeCode() : "EMP-" + empId;
            String fullName = emp != null ? emp.getFullName() : "Nhân viên " + empId;
            String profRole = emp != null ? emp.getProfessionalRole() : "";

            Map<String, ScenarioAllocationSnapshotItem> empWeeks = snapshotByEmpAndWeek.getOrDefault(empId, Map.of());
            List<EmployeeSnapshotCellResult> cells = new ArrayList<>();

            for (YearWeek yw : targetWeeks) {
                String weekKey = yw.year() + "_" + yw.weekNumber();
                String weekLabel = "T" + yw.weekNumber() + " (" + yw.getStartDate().format(dtf) + " - " + yw.getEndDate().format(dtf) + ")";
                ScenarioAllocationSnapshotItem item = empWeeks.get(weekKey);
                BigDecimal alloc = item != null ? item.getAllocatedHours() : BigDecimal.ZERO;
                BigDecimal avail = item != null ? item.getAvailableHours() : BigDecimal.ZERO;
                cells.add(new EmployeeSnapshotCellResult(yw.year(), yw.weekNumber(), alloc, avail));

                CapacityStatus empStatus = WeeklyCapacityMatrixPolicy.determineStatus(alloc, avail, overloadThreshold, idleThreshold);
                boolean isEmpOverloaded = (empStatus == CapacityStatus.OVERLOADED);
                BigDecimal excessHours = WeeklyCapacityMatrixPolicy.calculateExcessHours(alloc, avail);
                BigDecimal utilizationPercentage = WeeklyCapacityMatrixPolicy.calculateUtilizationPercentage(alloc, avail);

                cells.add(new EmployeeSnapshotCellResult(
                        yw.year(),
                        yw.weekNumber(),
                        alloc,
                        avail,
                        excessHours,
                        utilizationPercentage,
                        empStatus,
                        isEmpOverloaded
                ));

                if (isEmpOverloaded) {
                    overloadedEmployees.add(new OverloadedEmployeeResult(
                            empId,
                            empCode,
                            fullName,
                            profRole,
                            yw.year(),
                            yw.weekNumber(),
                            weekLabel,
                            alloc,
                            avail,
                            excessHours,
                            utilizationPercentage,
                            empStatus
                    ));
                }
            }

            employeeSnapshots.add(new EmployeeSnapshotRowResult(empId, empCode, fullName, profRole, cells));
        }

        // Sắp xếp danh sách nhân sự theo tên
        // Sắp xếp danh sách nhân sự theo tên và danh sách vượt năng lực theo tuần + tên
        employeeSnapshots.sort(Comparator.comparing(EmployeeSnapshotRowResult::fullName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        overloadedEmployees.sort(Comparator.comparing(OverloadedEmployeeResult::weekNumber)
                .thenComparing(OverloadedEmployeeResult::fullName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        if (saveAuditLogPort != null) {
            saveAuditLogPort.save(com.hrm.employeemanagement.domain.audit.AuditLog.create(
                    currentUser.getIdValue(),
                    "SIMULATE_SCENARIO",
                    "resource_scenarios",
                    scenario.getId()
            ));
        }
        saveAuditLogPort.save(com.hrm.employeemanagement.domain.audit.AuditLog.create(
                currentUser.getIdValue(),
                "SIMULATE_SCENARIO",
                "resource_scenarios",
                scenario.getId()
        ));

        return new ScenarioSimulationResult(
                scenario.getId(),
                scenario.getCode(),
                scenario.getName(),
                scenario.getOrgUnitId(),
                orgUnitName,
                scenario.getStatus().getValue(),
                scenario.getBaseSnapshotAt(),
                weeklyMetrics,
                overloadedEmployees,
                employeeSnapshots,
                overloadThreshold,
                idleThreshold
        );
    }

    private List<YearWeek> buildTargetWeeks(int fromYear, int fromWeek, int durationWeeks) {
        List<YearWeek> list = new ArrayList<>(durationWeeks);
        YearWeek start = YearWeek.of(fromYear, fromWeek);
        LocalDate monday = start.getStartDate();
        for (int i = 0; i < durationWeeks; i++) {
            int y = monday.get(IsoFields.WEEK_BASED_YEAR);
            int w = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            list.add(YearWeek.of(y, w));
            monday = monday.plusWeeks(1);
        }
        return list;
    }

    private void validateReadScope(User currentUser, Long targetOrgUnitId) {
        if (isOrgUnitInUserScope(currentUser, targetOrgUnitId)) {
            return;
        }
        throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_READ);
    }

    private boolean isOrgUnitInUserScope(User currentUser, Long targetOrgUnitId) {
        if (currentUser.getDataScope() == DataScope.COMPANY) {
            return true;
        }
        Long userScopeOrgUnitId = currentUser.getScopeOrgUnitId();
        if (userScopeOrgUnitId == null) return false;
        if (userScopeOrgUnitId.equals(targetOrgUnitId)) return true;
        return loadOrgUnitPort.existsInOrgUnitBranch(targetOrgUnitId, userScopeOrgUnitId);
    }
}
