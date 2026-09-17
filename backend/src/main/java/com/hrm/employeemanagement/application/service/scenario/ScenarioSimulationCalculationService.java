package com.hrm.employeemanagement.application.service.scenario;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hrm.employeemanagement.application.dto.scenario.EmployeeSnapshotCellResult;
import com.hrm.employeemanagement.application.dto.scenario.EmployeeSnapshotRowResult;
import com.hrm.employeemanagement.application.dto.scenario.OverloadedEmployeeResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioSimulationResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioSnapshotData;
import com.hrm.employeemanagement.application.dto.scenario.WeeklySimulationMetricResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.GetScenarioSimulationResultUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioSharePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.CapacityStatus;
import com.hrm.employeemanagement.domain.allocation.WeeklyCapacityMatrixPolicy;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.scenario.CorruptedScenarioSnapshotException;
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
    private final LoadScenarioSharePort loadScenarioSharePort;
    private final LoadProjectPort loadProjectPort;
    private final SaveAuditLogInNewTransactionPort deniedAuditLogPort;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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
        this(authorizationService, loadUserPort, loadOrgUnitPort, loadEmployeePort, loadScenarioPort, loadDemandPort, loadSnapshotPort, loadCapacityThresholdPort, null, null, null, null);
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
        this(authorizationService, loadUserPort, loadOrgUnitPort, loadEmployeePort, loadScenarioPort, loadDemandPort, loadSnapshotPort, loadCapacityThresholdPort, null, null, null, saveAuditLogPort);
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
            LoadScenarioSharePort loadScenarioSharePort,
            LoadProjectPort loadProjectPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort,
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
        this.loadScenarioSharePort = loadScenarioSharePort;
        this.loadProjectPort = loadProjectPort;
        this.deniedAuditLogPort = deniedAuditLogPort;
        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public ScenarioSimulationResult getSimulationResult(Long scenarioId) {
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_READ);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        ResourceScenario scenario = loadScenarioPort.findById(scenarioId)
                .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));

        boolean isOwner = scenario.getCreatedBy().equals(currentUserId);
        com.hrm.employeemanagement.domain.role.RoleCode roleCode = currentUser.getRole().getCode();

        // BR-02: Quyền truy cập
        if (!isOwner) {
            if (roleCode == com.hrm.employeemanagement.domain.role.RoleCode.VT_01) {
                // VT-01 có toàn quyền đọc
            } else if (roleCode != com.hrm.employeemanagement.domain.role.RoleCode.VT_02 && roleCode != com.hrm.employeemanagement.domain.role.RoleCode.VT_03) {
                logDenied(currentUserId, scenarioId, "ROLE_NOT_AUTHORIZED");
                throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_READ);
            } else if (loadScenarioSharePort != null) {
                // Invariant: Recipient chỉ được xem kết quả mô phỏng khi kịch bản ở trạng thái SAVED
                if (!scenario.isSaved()) {
                    logDenied(currentUserId, scenarioId, "SCENARIO_NOT_SAVED");
                    throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_READ);
                }

                if (!loadScenarioSharePort.hasActiveShare(scenarioId, currentUserId)) {
                    logDenied(currentUserId, scenarioId, "NO_ACTIVE_SHARE");
                    throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_READ);
                }

                // BR-08: Re-check scope tại thời điểm mở
                List<Long> projectIds = extractProjectIds(scenario.getSnapshotData());
                if (roleCode == com.hrm.employeemanagement.domain.role.RoleCode.VT_02) {
                    if (currentUser.getEmployeeId() == null) {
                        logDenied(currentUserId, scenarioId, "VT02_NO_EMPLOYEE_PROFILE");
                        throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_READ);
                    }
                    if (loadProjectPort != null) {
                        List<Long> managed = loadProjectPort.findAllManagedProjectIds(currentUser.getEmployeeId().value());
                        boolean overlap = managed.stream().anyMatch(projectIds::contains);
                        if (!overlap) {
                            logDenied(currentUserId, scenarioId, "VT02_SCOPE_LOST_NO_PROJECT_MANAGED");
                            throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_READ);
                        }
                    }
                } else if (roleCode == com.hrm.employeemanagement.domain.role.RoleCode.VT_03) {
                    Long userOrgUnitId = currentUser.getScopeOrgUnitId();
                    if (userOrgUnitId == null || !userOrgUnitId.equals(scenario.getOrgUnitId())) {
                        logDenied(currentUserId, scenarioId, "VT03_SCOPE_LOST_WRONG_ORG_UNIT");
                        throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_READ);
                    }
                }
            } else {
                validateReadScope(currentUser, scenario.getOrgUnitId());
            }
        } else {
            validateReadScope(currentUser, scenario.getOrgUnitId());
        }

        // BR-06: Snapshot isolation - Nếu kịch bản đã lưu và có snapshot_data (hoặc người xem VIEW_ONLY), đọc trực tiếp từ snapshot
        if ((!isOwner || scenario.isSaved()) && scenario.getSnapshotData() != null && !scenario.getSnapshotData().trim().isEmpty()) {
            try {
                ScenarioSnapshotData snapshotData = objectMapper.readValue(scenario.getSnapshotData(), ScenarioSnapshotData.class);
                if (snapshotData != null && snapshotData.simulationResult() != null) {
                    return snapshotData.simulationResult();
                }
            } catch (Exception e) {
                log.error("Failed to parse snapshotData simulationResult in scenario {}: {}", scenarioId, e.getMessage(), e);
                throw new CorruptedScenarioSnapshotException("Dữ liệu ảnh chụp kịch bản không hợp lệ hoặc bị hỏng", e);
            }
        }

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

        // 4. Xây dựng danh sách nhân sự snapshot & danh sách nhân sự vượt năng lực (Personnel level overload)
        List<Long> empIds = new ArrayList<>(snapshotByEmpAndWeek.keySet());
        List<com.hrm.employeemanagement.domain.employee.EmployeeId> employeeIds = empIds.stream()
                .map(com.hrm.employeemanagement.domain.employee.EmployeeId::new)
                .toList();
        List<Employee> employees = employeeIds.isEmpty() ? List.of() : loadEmployeePort.findAllByIdIn(employeeIds);
        Map<Long, Employee> employeeMap = employees.stream()
                .filter(Objects::nonNull)
                .filter(e -> e.getIdValue() != null)
                .collect(Collectors.toMap(
                        Employee::getIdValue,
                        e -> e,
                        (e1, e2) -> {
                            log.warn("Duplicate employee ID detected in scenario snapshot resolution: id={}", e1.getIdValue());
                            return e1;
                        }
                ));

        List<Long> missingEmployeeIds = empIds.stream()
                .filter(id -> !employeeMap.containsKey(id))
                .toList();
        if (!missingEmployeeIds.isEmpty()) {
            log.warn("Scenario simulation contains snapshot references to missing employees: scenarioId={}, employeeIds={}",
                    scenarioId, missingEmployeeIds);
        }

        // 4.1. Phân bổ Nhu cầu kịch bản (ScenarioDemand) xuống từng nhân sự theo Chức danh / Kỹ năng
        Map<Long, Map<String, BigDecimal>> empDemandHoursMap = new HashMap<>();
        for (YearWeek yw : targetWeeks) {
            String weekKey = yw.year() + "_" + yw.weekNumber();
            List<ScenarioDemand> activeDemandsInWeek = demands.stream()
                    .filter(d -> d.isActiveInWeek(yw))
                    .toList();

            for (ScenarioDemand d : activeDemandsInWeek) {
                BigDecimal totalDemandHours = d.getTotalHoursPerWeek();
                if (totalDemandHours.compareTo(BigDecimal.ZERO) <= 0) continue;

                String req = d.getSkillRequirement();
                List<Long> matchingEmpIds = empIds.stream()
                        .filter(empId -> {
                            Employee emp = employeeMap.get(empId);
                            if (emp == null) return false;
                            return isRoleMatching(emp.getProfessionalRole(), req);
                        })
                        .toList();

                if (!matchingEmpIds.isEmpty()) {
                    int count = matchingEmpIds.size();
                    BigDecimal basePerEmp = totalDemandHours.divide(BigDecimal.valueOf(count), 2, RoundingMode.FLOOR);
                    BigDecimal allocatedSoFar = basePerEmp.multiply(BigDecimal.valueOf(count));
                    int remainderCents = totalDemandHours.subtract(allocatedSoFar).movePointRight(2).intValue();

                    for (int i = 0; i < count; i++) {
                        Long empId = matchingEmpIds.get(i);
                        BigDecimal empHours = (i < remainderCents)
                                ? basePerEmp.add(new BigDecimal("0.01"))
                                : basePerEmp;
                        empDemandHoursMap
                                .computeIfAbsent(empId, k -> new HashMap<>())
                                .merge(weekKey, empHours, BigDecimal::add);
                    }
                }
            }
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
                BigDecimal baseAlloc = item != null ? item.getAllocatedHours() : BigDecimal.ZERO;
                BigDecimal defaultAvail = (emp != null && emp.getStandardHoursPerWeek() != null)
                        ? BigDecimal.valueOf(emp.getStandardHoursPerWeek())
                        : BigDecimal.valueOf(40);
                BigDecimal avail = item != null ? item.getAvailableHours() : defaultAvail;

                BigDecimal empDemandHours = empDemandHoursMap
                        .getOrDefault(empId, Map.of())
                        .getOrDefault(weekKey, BigDecimal.ZERO);

                BigDecimal simulatedAlloc = baseAlloc.add(empDemandHours);

                CapacityStatus empStatus = WeeklyCapacityMatrixPolicy.determineStatus(simulatedAlloc, avail, overloadThreshold, idleThreshold);
                boolean isEmpOverloaded = (empStatus == CapacityStatus.OVERLOADED);
                BigDecimal excessHours = WeeklyCapacityMatrixPolicy.calculateExcessHours(simulatedAlloc, avail);
                BigDecimal utilizationPercentage = WeeklyCapacityMatrixPolicy.calculateUtilizationPercentage(simulatedAlloc, avail);

                cells.add(new EmployeeSnapshotCellResult(
                        yw.year(),
                        yw.weekNumber(),
                        simulatedAlloc,
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
                            simulatedAlloc,
                            avail,
                            excessHours,
                            utilizationPercentage,
                            empStatus
                    ));
                }
            }

            employeeSnapshots.add(new EmployeeSnapshotRowResult(empId, empCode, fullName, profRole, cells));
        }

        // Sắp xếp danh sách nhân sự theo tên và danh sách vượt năng lực theo năm + tuần + tên
        employeeSnapshots.sort(Comparator.comparing(EmployeeSnapshotRowResult::fullName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        overloadedEmployees.sort(Comparator.comparing(OverloadedEmployeeResult::year)
                .thenComparing(OverloadedEmployeeResult::weekNumber)
                .thenComparing(OverloadedEmployeeResult::fullName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

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
        return AuthorizationService.isOrgUnitInUserScope(currentUser, targetOrgUnitId, loadOrgUnitPort);
    }

    private boolean isRoleMatching(String employeeRole, String requirement) {
        if (requirement == null || requirement.trim().isEmpty()) {
            return true;
        }
        if (employeeRole == null || employeeRole.trim().isEmpty()) {
            return false;
        }
        String trimmedReq = requirement.trim().toLowerCase();
        String trimmedRole = employeeRole.trim().toLowerCase();
        if (trimmedRole.equals(trimmedReq)) {
            return true;
        }
        String regex = "(?i)(^|[^a-zA-Z0-9_#+])" + Pattern.quote(trimmedReq) + "([^a-zA-Z0-9_#+]|$)";
        return Pattern.compile(regex).matcher(trimmedRole).find();
    }

    private void logDenied(Long userId, Long scenarioId, String reason) {
        if (deniedAuditLogPort != null) {
            deniedAuditLogPort.save(com.hrm.employeemanagement.domain.audit.AuditLog.createChange(
                    userId,
                    "SCENARIO_ACCESS_DENIED",
                    "resource_scenarios",
                    scenarioId,
                    null,
                    "action=GET_SIMULATION;reason=" + reason
            ));
        }
    }

    private List<Long> extractProjectIds(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.trim().isEmpty()) {
            return List.of();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(snapshotJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            Object projectIdsObj = map.get("projectIds");
            if (projectIdsObj instanceof List<?> list) {
                return list.stream().map(o -> Long.valueOf(o.toString())).toList();
            }
        } catch (Exception e) {
            log.error("Failed to extract projectIds from scenario snapshot JSON: {}", e.getMessage(), e);
            throw new CorruptedScenarioSnapshotException("Không thể trích xuất danh sách dự án từ ảnh chụp kịch bản", e);
        }
        return List.of();
    }
}
