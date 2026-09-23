package com.hrm.employeemanagement.application.service.scenario;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hrm.employeemanagement.application.dto.scenario.*;
import com.hrm.employeemanagement.application.port.inbound.scenario.*;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.*;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyCapacityMatrixPolicy;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.Holiday;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailabilityPolicy;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.scenario.CorruptedScenarioSnapshotException;
import com.hrm.employeemanagement.domain.exception.scenario.DuplicateScenarioCodeException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;
import com.hrm.employeemanagement.domain.scenario.ScenarioAllocationSnapshotItem;
import com.hrm.employeemanagement.domain.scenario.ScenarioDemand;
import com.hrm.employeemanagement.domain.scenario.ScenarioShare;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class ResourceScenarioService implements
        CreateSimulationScenarioUseCase,
        GetSimulationScenarioUseCase,
        ListSimulationScenariosUseCase,
        SaveSimulationScenarioUseCase,
        PatchSimulationScenarioUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ResourceScenarioService.class);

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final SaveResourceScenarioPort saveScenarioPort;
    private final LoadResourceScenarioPort loadScenarioPort;
    private final SaveScenarioSnapshotPort saveSnapshotPort;
    private final LoadScenarioSnapshotPort loadSnapshotPort;
    private final LoadScenarioDemandPort loadDemandPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final LoadScenarioSharePort loadScenarioSharePort;
    private final LoadProjectPort loadProjectPort;
    private final GetScenarioSimulationResultUseCase simulationResultUseCase;
    private final SaveAuditLogInNewTransactionPort deniedAuditLogPort;
    private final ObjectMapper objectMapper;

    public ResourceScenarioService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            SaveResourceScenarioPort saveScenarioPort,
            LoadResourceScenarioPort loadScenarioPort,
            SaveScenarioSnapshotPort saveSnapshotPort,
            LoadScenarioSnapshotPort loadSnapshotPort,
            LoadScenarioDemandPort loadDemandPort,
            SaveAuditLogPort saveAuditLogPort
    ) {
        this(
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
                null,
                null,
                null,
                null
        );
    }

    public ResourceScenarioService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            SaveResourceScenarioPort saveScenarioPort,
            LoadResourceScenarioPort loadScenarioPort,
            SaveScenarioSnapshotPort saveSnapshotPort,
            LoadScenarioSnapshotPort loadSnapshotPort,
            LoadScenarioDemandPort loadDemandPort,
            SaveAuditLogPort saveAuditLogPort,
            LoadScenarioSharePort loadScenarioSharePort,
            LoadProjectPort loadProjectPort,
            GetScenarioSimulationResultUseCase simulationResultUseCase,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "LoadWeeklyAvailabilityPort must not be null");
        this.loadHolidaysPort = Objects.requireNonNull(loadHolidaysPort, "LoadHolidaysPort must not be null");
        this.loadApprovedLeavesPort = Objects.requireNonNull(loadApprovedLeavesPort, "LoadApprovedLeavesPort must not be null");
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
        this.saveScenarioPort = Objects.requireNonNull(saveScenarioPort, "SaveResourceScenarioPort must not be null");
        this.loadScenarioPort = Objects.requireNonNull(loadScenarioPort, "LoadResourceScenarioPort must not be null");
        this.saveSnapshotPort = Objects.requireNonNull(saveSnapshotPort, "SaveScenarioSnapshotPort must not be null");
        this.loadSnapshotPort = Objects.requireNonNull(loadSnapshotPort, "LoadScenarioSnapshotPort must not be null");
        this.loadDemandPort = Objects.requireNonNull(loadDemandPort, "LoadScenarioDemandPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.loadScenarioSharePort = loadScenarioSharePort;
        this.loadProjectPort = loadProjectPort;
        this.simulationResultUseCase = simulationResultUseCase;
        this.deniedAuditLogPort = deniedAuditLogPort;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public ScenarioResult createScenario(CreateScenarioCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Dữ liệu kịch bản không được để trống");
        }
        if (command.durationWeeks() == null || command.durationWeeks() < 1 || command.durationWeeks() > 16) {
            throw new IllegalArgumentException("Số tuần mô phỏng phải từ 1 đến 16 tuần");
        }
        if ((command.fromYear() == null && command.fromWeek() != null)
                || (command.fromYear() != null && command.fromWeek() == null)) {
            throw new IllegalArgumentException("Năm bắt đầu (fromYear) và tuần bắt đầu (fromWeek) phải cùng được cung cấp hoặc cùng để trống");
        }
        if (command.fromYear() != null && (command.fromYear() < 2000 || command.fromYear() > 2100)) {
            throw new IllegalArgumentException("Năm bắt đầu không hợp lệ (2000 - 2100)");
        }
        if (command.fromWeek() != null && (command.fromWeek() < 1 || command.fromWeek() > 53)) {
            throw new IllegalArgumentException("Tuần bắt đầu phải từ 1 đến 53");
        }

        // 1. Kiểm tra quyền hạn: Dùng AuthorizationService làm nguồn sự thật duy nhất (RESOURCE_SCENARIO_MANAGE)
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        // 2. Xác định đơn vị mục tiêu và kiểm tra DataScope
        Long targetOrgUnitId = command.orgUnitId();
        if (targetOrgUnitId == null) {
            targetOrgUnitId = currentUser.getScopeOrgUnitId();
        }
        if (targetOrgUnitId == null) {
            throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        }

        validateManageScope(currentUser, targetOrgUnitId);

        OrgUnit orgUnit = loadOrgUnitPort.findById(new OrgUnitId(targetOrgUnitId))
                .orElseThrow(() -> new OrgUnitNotFoundException("Không tìm thấy bộ phận với ID: " + command.orgUnitId()));

        // Xác định khoảng tuần của kịch bản
        int fromYear;
        int fromWeek;
        if (command.fromYear() != null && command.fromWeek() != null) {
            fromYear = command.fromYear();
            fromWeek = command.fromWeek();
        } else {
            LocalDate now = LocalDate.now();
            fromYear = now.get(IsoFields.WEEK_BASED_YEAR);
            fromWeek = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        }

        int durationWeeks = command.durationWeeks();

        String code = command.code();
        boolean isClientProvidedCode = code != null && !code.trim().isEmpty();

        if (isClientProvidedCode) {
            code = code.trim();
            if (loadScenarioPort.existsByCode(code)) {
                throw new DuplicateScenarioCodeException(code);
            }
        } else {
            code = generateUniqueScenarioCode(fromYear);
        }

        // 3. Tạo thực thể ResourceScenario
        ResourceScenario savedScenario;
        if (isClientProvidedCode) {
            ResourceScenario scenario = ResourceScenario.createNew(
                    code,
                    command.name().trim(),
                    command.description() != null ? command.description().trim() : null,
                    targetOrgUnitId,
                    fromYear,
                    fromWeek,
                    durationWeeks,
                    currentUserId
            );
            savedScenario = saveScenarioPort.save(scenario);
        } else {
            int maxAttempts = 5;
            DuplicateScenarioCodeException lastEx = null;
            ResourceScenario created = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                String candidateCode = (attempt == 1) ? code : generateUniqueScenarioCode(fromYear);
                ResourceScenario scenario = ResourceScenario.createNew(
                        candidateCode,
                        command.name().trim(),
                        command.description() != null ? command.description().trim() : null,
                        targetOrgUnitId,
                        fromYear,
                        fromWeek,
                        durationWeeks,
                        currentUserId
                );
                try {
                    created = saveScenarioPort.save(scenario);
                    break;
                } catch (DuplicateScenarioCodeException ex) {
                    lastEx = ex;
                }
            }
            if (created == null) {
                throw lastEx != null ? lastEx : new DuplicateScenarioCodeException("Không thể tạo mã kịch bản sau nhiều lần thử");
            }
            savedScenario = created;
        }

        // 4. Chụp Snapshot dữ liệu phân bổ thật và năng lực khả dụng tại thời điểm tạo (QTN-14)
        List<YearWeek> targetWeeks = buildTargetWeeks(fromYear, fromWeek, durationWeeks);
        List<Long> branchOrgUnitIds = resolveScopeBranchOrgUnitIds(targetOrgUnitId);
        List<Employee> branchEmployees = loadEmployeePort.findActiveByOrgUnitIds(branchOrgUnitIds);

        List<ScenarioAllocationSnapshotItem> snapshotItems = new ArrayList<>();
        if (!branchEmployees.isEmpty()) {
            List<Long> empIds = branchEmployees.stream().map(Employee::getIdValue).toList();

            // Load dữ liệu phân bổ thật (chỉ đọc)
            List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(empIds, targetWeeks);
            Map<String, BigDecimal> allocationMap = allocations.stream()
                    .collect(Collectors.groupingBy(
                            a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                            Collectors.reducing(BigDecimal.ZERO, WeeklyProjectAllocation::getAllocatedHours, BigDecimal::add)
                    ));

            // Load dữ liệu khả dụng cấu hình
            List<WeeklyAvailability> availabilities = loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(empIds, targetWeeks);
            Map<String, WeeklyAvailability> availabilityMap = availabilities.stream()
                    .collect(Collectors.toMap(
                            a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                            a -> a,
                            (e1, e2) -> e1
                    ));

            // Load ngày nghỉ phép đã duyệt
            Map<Long, Map<YearWeek, BigDecimal>> leaveHoursMap = loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(empIds, targetWeeks);

            // Load ngày lễ
            LocalDate minStart = targetWeeks.get(0).getStartDate();
            LocalDate maxEnd = targetWeeks.get(targetWeeks.size() - 1).getEndDate();
            List<Holiday> holidays = loadHolidaysPort.getHolidaysBetween(minStart, maxEnd);
            Set<DayOfWeek> workingDays = resolveWorkingDays();
            Map<YearWeek, Integer> holidayHoursByWeek = targetWeeks.stream()
                    .collect(Collectors.toMap(
                            yw -> yw,
                            yw -> WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yw, holidays, workingDays)
                    ));

            int weekWorkingDaysCount = workingDays.isEmpty() ? 5 : workingDays.size();

            for (Employee emp : branchEmployees) {
                for (YearWeek yw : targetWeeks) {
                    String key = makeKey(emp.getIdValue(), yw.year(), yw.weekNumber());

                    WeeklyAvailability savedAvail = availabilityMap.get(key);
                    BigDecimal baseAvailable;
                    if (savedAvail != null) {
                        baseAvailable = savedAvail.getNetAvailableHours();
                    } else {
                        int standardHours = emp.getStandardHoursPerWeek() != null ? emp.getStandardHoursPerWeek() : 40;
                        int holidayHours = holidayHoursByWeek.getOrDefault(yw, 0);
                        BigDecimal leaveHours = leaveHoursMap.getOrDefault(emp.getIdValue(), Map.of()).getOrDefault(yw, BigDecimal.ZERO);
                        baseAvailable = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, leaveHours);
                    }

                    BigDecimal availableHours = WeeklyCapacityMatrixPolicy.adjustAvailableHoursForContract(
                            baseAvailable,
                            emp.getContractEndDate(),
                            yw.getStartDate(),
                            yw.getEndDate(),
                            weekWorkingDaysCount
                    );

                    BigDecimal allocatedHours = allocationMap.getOrDefault(key, BigDecimal.ZERO);

                    snapshotItems.add(ScenarioAllocationSnapshotItem.create(
                            savedScenario.getId(),
                            emp.getIdValue(),
                            yw.year(),
                            yw.weekNumber(),
                            allocatedHours,
                            availableHours
                    ));
                }
            }

            saveSnapshotPort.saveAll(snapshotItems);
        }

        // 5. Ghi Audit log nguyên tử trong cùng transaction
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "CREATE_SCENARIO",
                "resource_scenarios",
                savedScenario.getId(),
                null,
                "code=" + savedScenario.getCode() + ";name=" + savedScenario.getName() + ";orgUnitId=" + targetOrgUnitId
        ));

        return toScenarioResult(savedScenario, orgUnit.getUnitName(), currentUser.getUsername(), 0, branchEmployees.size());
    }

    @Override
    public List<ScenarioResult> listScenarios(Long orgUnitId) {
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_READ);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        // 1. Kịch bản do người dùng này tạo (Owner)
        List<ResourceScenario> ownedScenarios;
        if (orgUnitId != null) {
            List<Long> branchIds = resolveScopeBranchOrgUnitIds(orgUnitId);
            ownedScenarios = loadScenarioPort.findAllByOrgUnitIds(branchIds).stream()
                    .filter(s -> s.getCreatedBy().equals(currentUserId))
                    .toList();
        } else {
            ownedScenarios = loadScenarioPort.findAll().stream()
                    .filter(s -> s.getCreatedBy().equals(currentUserId))
                    .toList();
        }

        Map<Long, ScenarioResult> resultMap = new LinkedHashMap<>();
        for (ResourceScenario s : ownedScenarios) {
            resultMap.put(s.getId(), enrichScenarioResult(s, "EDIT"));
        }

        // 2. Kịch bản được chia sẻ cho người dùng này (Active Shares)
        if (loadScenarioSharePort != null) {
            List<ScenarioShare> activeShares = loadScenarioSharePort.findActiveSharesByUserId(currentUserId);
            for (ScenarioShare share : activeShares) {
                if (resultMap.containsKey(share.getScenarioId())) {
                    continue; // Đã có trong owned
                }
                Optional<ResourceScenario> scenarioOpt = loadScenarioPort.findById(share.getScenarioId());
                if (scenarioOpt.isEmpty()) {
                    continue;
                }
                ResourceScenario sharedScenario = scenarioOpt.get();
                if (orgUnitId != null && !sharedScenario.getOrgUnitId().equals(orgUnitId)) {
                    continue;
                }

                // Invariant: Recipient chỉ được xem kịch bản khi ở trạng thái SAVED
                if (!sharedScenario.isSaved()) {
                    continue;
                }

                // BR-08: Re-check scope tại thời điểm list
                if (isRecipientScopeValid(currentUser, sharedScenario)) {
                    resultMap.put(sharedScenario.getId(), enrichScenarioResult(sharedScenario, "VIEW_ONLY"));
                }
            }
        }

        return new ArrayList<>(resultMap.values());
    }

    @Override
    public ScenarioDetailResult getScenarioById(Long scenarioId) {
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_READ);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        ResourceScenario scenario = loadScenarioPort.findById(scenarioId)
                .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));

        boolean isOwner = scenario.getCreatedBy().equals(currentUserId);
        String viewMode;

        if (isOwner) {
            validateReadScope(currentUser, scenario.getOrgUnitId());
            viewMode = "EDIT";
        } else if (currentUser.getRole().getCode() == RoleCode.VT_01) {
            // VT-01 (Giám đốc) có quyền xem toàn công ty
            viewMode = "VIEW_ONLY";
        } else {
            RoleCode roleCode = currentUser.getRole().getCode();
            if (roleCode != RoleCode.VT_02 && roleCode != RoleCode.VT_03) {
                logDenied(currentUserId, scenarioId, "INVALID_ROLE_" + roleCode.getCode());
                throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_READ);
            }

            // Invariant: Recipient chỉ được xem kịch bản khi ở trạng thái SAVED
            if (!scenario.isSaved()) {
                logDenied(currentUserId, scenarioId, "SCENARIO_NOT_SAVED");
                throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_READ);
            }

            if (loadScenarioSharePort == null || !loadScenarioSharePort.hasActiveShare(scenarioId, currentUserId)) {
                logDenied(currentUserId, scenarioId, "NO_ACTIVE_SHARE");
                throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_READ);
            }

            // BR-08: Re-check scope tại thời điểm mở
            if (!isRecipientScopeValid(currentUser, scenario)) {
                logDenied(currentUserId, scenarioId, "SCOPE_LOST");
                throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_READ);
            }

            viewMode = "VIEW_ONLY";
        }

        // BR-06: Snapshot isolation - Đọc từ snapshot_data nếu ở chế độ VIEW_ONLY hoặc SAVED có snapshot
        if ((viewMode.equals("VIEW_ONLY") || scenario.isSaved())
                && scenario.getSnapshotData() != null && !scenario.getSnapshotData().trim().isEmpty()) {
            try {
                ScenarioSnapshotData snapshotData = objectMapper.readValue(scenario.getSnapshotData(), ScenarioSnapshotData.class);
                if (snapshotData != null && snapshotData.scenario() != null) {
                    ScenarioResult res = snapshotData.scenario().withViewMode(viewMode);
                    return new ScenarioDetailResult(res, snapshotData.demands() != null ? snapshotData.demands() : List.of());
                }
            } catch (Exception e) {
                log.error("Failed to parse snapshotData in scenario {}: {}", scenarioId, e.getMessage(), e);
                throw new CorruptedScenarioSnapshotException("Dữ liệu ảnh chụp kịch bản không hợp lệ hoặc bị hỏng", e);
            }
        }

        List<ScenarioDemand> demands = loadDemandPort.findByScenarioId(scenarioId);
        List<ScenarioDemandResult> demandResults = demands.stream()
                .map(d -> new ScenarioDemandResult(
                        d.getId(),
                        d.getScenarioId(),
                        d.getDemandName(),
                        d.getHeadcount(),
                        d.getStartYear(),
                        d.getStartWeek(),
                        d.getEndYear(),
                        d.getEndWeek(),
                        d.getHoursPerWeekPerPerson(),
                        d.getTotalHoursPerWeek(),
                        d.getSkillRequirement(),
                        d.getCreatedAt(),
                        d.getUpdatedAt()
                ))
                .toList();

        ScenarioResult baseResult = enrichScenarioResult(scenario, viewMode);
        return new ScenarioDetailResult(baseResult, demandResults);
    }

    @Override
    public ScenarioResult saveScenario(Long scenarioId) {
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        ResourceScenario scenario = loadScenarioPort.findById(scenarioId)
                .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));

        if (!scenario.getCreatedBy().equals(currentUserId)) {
            logDenied(currentUserId, scenarioId, "SAVE_NOT_OWNER");
            throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        }

        // 1. Tính toán kết quả mô phỏng (Precondition: simulation completes successfully)
        ScenarioSimulationResult simulationResult = null;
        if (simulationResultUseCase != null) {
            simulationResult = simulationResultUseCase.getSimulationResult(scenarioId);
        }

        // 2. Thu thập danh sách projectIds từ allocations của các nhân sự trong kịch bản
        List<YearWeek> targetWeeks = buildTargetWeeks(scenario.getFromYear(), scenario.getFromWeek(), scenario.getDurationWeeks());
        List<ScenarioAllocationSnapshotItem> snapshotItems = loadSnapshotPort.findByScenarioId(scenarioId);
        List<Long> empIds = snapshotItems.stream().map(ScenarioAllocationSnapshotItem::getEmployeeId).distinct().toList();

        List<Long> projectIds = new ArrayList<>();
        List<String> projectNames = new ArrayList<>();
        if (!empIds.isEmpty()) {
            List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(empIds, targetWeeks);
            projectIds = allocations.stream()
                    .map(WeeklyProjectAllocation::getProjectId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (loadProjectPort != null && !projectIds.isEmpty()) {
                List<ProjectId> pIds = projectIds.stream().map(ProjectId::new).toList();
                projectNames = loadProjectPort.findAllById(pIds).stream()
                        .map(Project::getProjectName)
                        .toList();
            }
        }

        // 3. Nạp danh sách demands hiện tại
        List<ScenarioDemand> demands = loadDemandPort.findByScenarioId(scenarioId);
        List<ScenarioDemandResult> demandResults = demands.stream()
                .map(d -> new ScenarioDemandResult(
                        d.getId(),
                        d.getScenarioId(),
                        d.getDemandName(),
                        d.getHeadcount(),
                        d.getStartYear(),
                        d.getStartWeek(),
                        d.getEndYear(),
                        d.getEndWeek(),
                        d.getHoursPerWeekPerPerson(),
                        d.getTotalHoursPerWeek(),
                        d.getSkillRequirement(),
                        d.getCreatedAt(),
                        d.getUpdatedAt()
                ))
                .toList();

        ScenarioResult baseResult = enrichScenarioResult(scenario, "EDIT");

        // 4. Đóng gói snapshot_data
        ScenarioSnapshotData snapshotData = new ScenarioSnapshotData(
                1,
                LocalDateTime.now(),
                currentUserId,
                currentUser.getUsername(),
                projectIds,
                projectNames,
                baseResult,
                demandResults,
                simulationResult
        );

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshotData);
        } catch (Exception e) {
            log.error("Failed to serialize scenario snapshot: {}", e.getMessage(), e);
            throw new IllegalStateException("Không thể serialize snapshot kịch bản: " + e.getMessage(), e);
        }

        String oldStatus = scenario.getStatus().getValue();
        scenario.saveSnapshot(snapshotJson);
        ResourceScenario saved = saveScenarioPort.save(scenario);

        // BR-11: Ghi Audit log SCENARIO_SAVED (atomic trong transaction)
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "SCENARIO_SAVED",
                "resource_scenarios",
                saved.getId(),
                "status=" + oldStatus,
                "status=saved;snapshotVersion=1;projectIdsCount=" + projectIds.size()
        ));

        return enrichScenarioResult(saved, "EDIT");
    }

    @Override
    public ScenarioResult patchScenario(PatchScenarioCommand command) {
        if (command == null || command.scenarioId() == null) {
            throw new IllegalArgumentException("Dữ liệu cập nhật kịch bản không hợp lệ");
        }
        if (command.name() == null && command.note() == null) {
            throw new IllegalArgumentException("Cần cung cấp ít nhất tên hoặc ghi chú kịch bản cần cập nhật");
        }

        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        ResourceScenario scenario = loadScenarioPort.findById(command.scenarioId())
                .orElseThrow(() -> new ScenarioNotFoundException(command.scenarioId()));

        if (!scenario.getCreatedBy().equals(currentUserId)) {
            logDenied(currentUserId, command.scenarioId(), "PATCH_NOT_OWNER");
            throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        }

        scenario.updateBasicInfo(command.name(), command.note());
        ResourceScenario saved = saveScenarioPort.save(scenario);

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "PATCH_SCENARIO",
                "resource_scenarios",
                saved.getId(),
                null,
                "name=" + saved.getName() + ";note=" + saved.getNote() + ";status=" + saved.getStatus().getValue()
        ));

        return enrichScenarioResult(saved, "EDIT");
    }

    private boolean isRecipientScopeValid(User currentUser, ResourceScenario scenario) {
        RoleCode roleCode = currentUser.getRole().getCode();
        if (roleCode == RoleCode.VT_01) {
            return true;
        }
        if (roleCode == RoleCode.VT_02) {
            if (currentUser.getEmployeeId() == null || loadProjectPort == null) {
                return false;
            }
            List<Long> scenarioProjectIds = extractProjectIds(scenario.getSnapshotData());
            List<Long> managed = loadProjectPort.findAllManagedProjectIds(currentUser.getEmployeeId().value());
            return managed.stream().anyMatch(scenarioProjectIds::contains);
        }
        if (roleCode == RoleCode.VT_03) {
            Long userOrgUnitId = currentUser.getScopeOrgUnitId();
            if (userOrgUnitId == null && currentUser.getEmployeeId() != null) {
                Employee emp = loadEmployeePort.findById(currentUser.getEmployeeId()).orElse(null);
                if (emp != null) {
                    userOrgUnitId = emp.getOrgUnitId();
                }
            }
            return userOrgUnitId != null && userOrgUnitId.equals(scenario.getOrgUnitId());
        }
        return false;
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

    private void logDenied(Long userId, Long scenarioId, String reason) {
        if (deniedAuditLogPort != null) {
            deniedAuditLogPort.save(AuditLog.createChange(
                    userId,
                    "SCENARIO_ACCESS_DENIED",
                    "resource_scenarios",
                    scenarioId,
                    null,
                    "action=SCENARIO_ACCESS;reason=" + reason
            ));
        }
    }

    private ScenarioResult enrichScenarioResult(ResourceScenario scenario) {
        return enrichScenarioResult(scenario, "EDIT");
    }

    private ScenarioResult enrichScenarioResult(ResourceScenario scenario, String viewMode) {
        String orgUnitName = loadOrgUnitPort.findById(new OrgUnitId(scenario.getOrgUnitId()))
                .map(OrgUnit::getUnitName)
                .orElse("Không xác định");

        String creatorName = loadUserPort.findById(new UserId(scenario.getCreatedBy()))
                .map(User::getUsername)
                .orElse("Hệ thống");

        List<ScenarioDemand> demands = loadDemandPort.findByScenarioId(scenario.getId());
        List<ScenarioAllocationSnapshotItem> snapshots = loadSnapshotPort.findByScenarioId(scenario.getId());
        int uniqueEmpCount = (int) snapshots.stream().map(ScenarioAllocationSnapshotItem::getEmployeeId).distinct().count();

        return toScenarioResult(scenario, orgUnitName, creatorName, demands.size(), uniqueEmpCount, viewMode);
    }

    private ScenarioResult toScenarioResult(
            ResourceScenario scenario,
            String orgUnitName,
            String creatorName,
            int demandsCount,
            int snapshotEmployeesCount
    ) {
        return toScenarioResult(scenario, orgUnitName, creatorName, demandsCount, snapshotEmployeesCount, "EDIT");
    }

    private ScenarioResult toScenarioResult(
            ResourceScenario scenario,
            String orgUnitName,
            String creatorName,
            int demandsCount,
            int snapshotEmployeesCount,
            String viewMode
    ) {
        return new ScenarioResult(
                scenario.getId(),
                scenario.getCode(),
                scenario.getName(),
                scenario.getDescription(),
                scenario.getNote(),
                scenario.getOrgUnitId(),
                orgUnitName,
                scenario.getStatus().getValue(),
                scenario.getFromYear(),
                scenario.getFromWeek(),
                scenario.getDurationWeeks(),
                scenario.getBaseSnapshotAt(),
                scenario.getCreatedBy(),
                creatorName,
                scenario.getCreatedAt(),
                scenario.getUpdatedAt(),
                demandsCount,
                snapshotEmployeesCount,
                scenario.getTargetProjectId(),
                scenario.getAppliedAt(),
                scenario.getAppliedBy(),
                viewMode
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

    private List<Long> resolveScopeBranchOrgUnitIds(Long orgUnitId) {
        if (orgUnitId != null) {
            Optional<OrgUnit> unitOpt = loadOrgUnitPort.findById(new OrgUnitId(orgUnitId));
            if (unitOpt.isPresent()) {
                List<OrgUnit> subTree = loadOrgUnitPort.findSubTree(unitOpt.get().getTreePath());
                return subTree.stream().map(u -> u.getId().getValue()).toList();
            }
            return List.of(orgUnitId);
        }
        return List.of();
    }

    private Set<DayOfWeek> resolveWorkingDays() {
        if (loadWorkingCalendarPort != null) {
            com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar calendar = loadWorkingCalendarPort.loadCompanyCalendar();
            if (calendar != null && calendar.getWorkingDays() != null && !calendar.getWorkingDays().isEmpty()) {
                return calendar.getWorkingDays();
            }
        }
        return Set.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
        );
    }

    private String makeKey(Long employeeId, int year, int weekNumber) {
        return employeeId + "_" + year + "_" + weekNumber;
    }

    private String generateScenarioCode(int year) {
        String uuid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "SCN-" + year + "-" + uuid;
    }

    private String generateUniqueScenarioCode(int year) {
        for (int i = 0; i < 5; i++) {
            String candidate = generateScenarioCode(year);
            if (!loadScenarioPort.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new DuplicateScenarioCodeException("Không thể tạo mã kịch bản duy nhất sau 5 lần thử");
    }

    private void validateManageScope(User currentUser, Long targetOrgUnitId) {
        if (isOrgUnitInUserScope(currentUser, targetOrgUnitId)) {
            return;
        }
        throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_MANAGE);
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
}