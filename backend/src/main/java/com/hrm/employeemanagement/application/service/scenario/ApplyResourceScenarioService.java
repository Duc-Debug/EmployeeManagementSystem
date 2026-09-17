package com.hrm.employeemanagement.application.service.scenario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioCommand;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioPreviewResult;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioPreviewResult.EmployeeComparisonRowResult;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioPreviewResult.WeeklyComparisonCellResult;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioPreviewResult.WeeklyHeaderResult;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.ApplyScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.PreviewApplyScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.RefreshScenarioBaselineUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.DeleteScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyCapacityMatrixPolicy;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.Holiday;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailabilityPolicy;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.scenario.InvalidTargetProjectException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioAlreadyAppliedException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioBaselineStaleException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotFoundException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotModifiableException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;
import com.hrm.employeemanagement.domain.scenario.ScenarioAllocationSnapshotItem;
import com.hrm.employeemanagement.domain.scenario.ScenarioDemand;
import com.hrm.employeemanagement.domain.scenario.ScenarioStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class ApplyResourceScenarioService implements
        PreviewApplyScenarioUseCase,
        ApplyScenarioUseCase,
        RefreshScenarioBaselineUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApplyResourceScenarioService.class);

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadResourceScenarioPort loadScenarioPort;
    private final SaveResourceScenarioPort saveScenarioPort;
    private final LoadScenarioDemandPort loadDemandPort;
    private final LoadScenarioSnapshotPort loadSnapshotPort;
    private final SaveScenarioSnapshotPort saveSnapshotPort;
    private final DeleteScenarioSnapshotPort deleteSnapshotPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final SaveWeeklyProjectAllocationPort saveAllocationPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final SaveAuditLogPort saveAuditLogPort;

    public ApplyResourceScenarioService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadProjectPort loadProjectPort,
            LoadResourceScenarioPort loadScenarioPort,
            SaveResourceScenarioPort saveScenarioPort,
            LoadScenarioDemandPort loadDemandPort,
            LoadScenarioSnapshotPort loadSnapshotPort,
            SaveScenarioSnapshotPort saveSnapshotPort,
            DeleteScenarioSnapshotPort deleteSnapshotPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            SaveAuditLogPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadScenarioPort = Objects.requireNonNull(loadScenarioPort, "LoadResourceScenarioPort must not be null");
        this.saveScenarioPort = Objects.requireNonNull(saveScenarioPort, "SaveResourceScenarioPort must not be null");
        this.loadDemandPort = Objects.requireNonNull(loadDemandPort, "LoadScenarioDemandPort must not be null");
        this.loadSnapshotPort = Objects.requireNonNull(loadSnapshotPort, "LoadScenarioSnapshotPort must not be null");
        this.saveSnapshotPort = Objects.requireNonNull(saveSnapshotPort, "SaveScenarioSnapshotPort must not be null");
        this.deleteSnapshotPort = Objects.requireNonNull(deleteSnapshotPort, "DeleteScenarioSnapshotPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.saveAllocationPort = Objects.requireNonNull(saveAllocationPort, "SaveWeeklyProjectAllocationPort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "LoadWeeklyAvailabilityPort must not be null");
        this.loadHolidaysPort = Objects.requireNonNull(loadHolidaysPort, "LoadHolidaysPort must not be null");
        this.loadApprovedLeavesPort = Objects.requireNonNull(loadApprovedLeavesPort, "LoadApprovedLeavesPort must not be null");
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
    }

    @Override
    public ApplyScenarioPreviewResult previewApplyScenario(Long scenarioId, Long targetProjectId) {
        User currentUser = requireResourceManagerUser();

        ResourceScenario scenario = loadScenarioPort.findById(scenarioId)
                .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));

        validateScenarioScope(currentUser, scenario.getOrgUnitId());

        Project targetProject = loadProjectPort.findById(new ProjectId(targetProjectId))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + targetProjectId));

        validateProjectScope(currentUser, targetProject);

        List<YearWeek> targetWeeks = buildTargetWeeks(scenario.getFromYear(), scenario.getFromWeek(), scenario.getDurationWeeks());
        List<ScenarioAllocationSnapshotItem> snapshotItems = loadSnapshotPort.findByScenarioId(scenarioId);
        List<ScenarioDemand> demands = loadDemandPort.findByScenarioId(scenarioId);

        List<Long> snapshotEmpIds = snapshotItems.stream()
                .map(ScenarioAllocationSnapshotItem::getEmployeeId)
                .distinct()
                .toList();

        Map<Long, Employee> employeeMap = loadEmployeeMap(snapshotEmpIds);

        // 1. Kiểm tra tính toàn vẹn của baseline snapshot (TC-02)
        List<String> staleReasons = checkBaselineStale(snapshotItems, targetWeeks, employeeMap);
        boolean isBaselineStale = !staleReasons.isEmpty();

        // 2. Tính toán phân bổ nhu cầu kịch bản xuống nhân sự
        Map<Long, Map<String, BigDecimal>> empDemandHoursMap = calculateDemandDistribution(demands, snapshotEmpIds, employeeMap, targetWeeks);

        // 3. Nạp phân bổ hiện tại trên dự án mục tiêu
        List<WeeklyProjectAllocation> targetProjectAllocations = loadAllocationPort.loadAllocationsForProjectInWeekRange(
                targetProjectId,
                scenario.getFromYear(),
                targetWeeks.get(0).weekNumber(),
                targetWeeks.get(targetWeeks.size() - 1).weekNumber()
        );
        Map<String, BigDecimal> currentProjectAllocMap = targetProjectAllocations.stream()
                .collect(Collectors.toMap(
                        a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                        WeeklyProjectAllocation::getAllocatedHours,
                        BigDecimal::add
                ));

        // Nạp tổng phân bổ hiện tại của các nhân sự trên toàn bộ công ty
        List<WeeklyProjectAllocation> allCurrentAllocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(snapshotEmpIds, targetWeeks);
        Map<String, BigDecimal> currentTotalAllocMap = allCurrentAllocations.stream()
                .collect(Collectors.toMap(
                        a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                        WeeklyProjectAllocation::getAllocatedHours,
                        BigDecimal::add
                ));

        Map<String, BigDecimal> snapshotAvailMap = snapshotItems.stream()
                .collect(Collectors.toMap(
                        i -> makeKey(i.getEmployeeId(), i.getYearNumber(), i.getWeekNumber()),
                        ScenarioAllocationSnapshotItem::getAvailableHours,
                        (a, b) -> a
                ));

        // 4. Xây dựng Header các tuần
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM");
        List<WeeklyHeaderResult> weekHeaders = targetWeeks.stream()
                .map(yw -> new WeeklyHeaderResult(
                        yw.year(),
                        yw.weekNumber(),
                        "T" + yw.weekNumber() + " (" + yw.getStartDate().format(dtf) + " - " + yw.getEndDate().format(dtf) + ")"
                ))
                .toList();

        // 5. Xây dựng các hàng so sánh cho nhân sự
        List<EmployeeComparisonRowResult> rows = new ArrayList<>();
        BigDecimal totalAdditionalHoursAll = BigDecimal.ZERO;
        int affectedCount = 0;

        for (Long empId : snapshotEmpIds) {
            Employee emp = employeeMap.get(empId);
            String empCode = emp != null ? emp.getEmployeeCode() : "EMP-" + empId;
            String fullName = emp != null ? emp.getFullName() : "Nhân viên " + empId;
            String profRole = emp != null ? emp.getProfessionalRole() : "";

            List<WeeklyComparisonCellResult> cells = new ArrayList<>();
            BigDecimal empTotalScenarioHours = BigDecimal.ZERO;

            for (YearWeek yw : targetWeeks) {
                String key = makeKey(empId, yw.year(), yw.weekNumber());
                BigDecimal curProjHours = currentProjectAllocMap.getOrDefault(key, BigDecimal.ZERO);
                BigDecimal curTotalHours = currentTotalAllocMap.getOrDefault(key, BigDecimal.ZERO);
                BigDecimal scenHours = empDemandHoursMap.getOrDefault(empId, Map.of()).getOrDefault(key, BigDecimal.ZERO);

                BigDecimal newProjHours = curProjHours.add(scenHours);
                BigDecimal newTotalHours = curTotalHours.add(scenHours);
                BigDecimal availHours = snapshotAvailMap.getOrDefault(key, emp != null && emp.getStandardHoursPerWeek() != null
                        ? BigDecimal.valueOf(emp.getStandardHoursPerWeek()) : BigDecimal.valueOf(40));

                boolean isOverloaded = newTotalHours.compareTo(availHours) > 0;

                cells.add(new WeeklyComparisonCellResult(
                        yw.year(),
                        yw.weekNumber(),
                        curProjHours,
                        curTotalHours,
                        scenHours,
                        newProjHours,
                        newTotalHours,
                        availHours,
                        isOverloaded
                ));

                empTotalScenarioHours = empTotalScenarioHours.add(scenHours);
            }

            if (empTotalScenarioHours.compareTo(BigDecimal.ZERO) > 0) {
                affectedCount++;
            }
            totalAdditionalHoursAll = totalAdditionalHoursAll.add(empTotalScenarioHours);

            rows.add(new EmployeeComparisonRowResult(
                    empId,
                    empCode,
                    fullName,
                    profRole,
                    cells,
                    empTotalScenarioHours
            ));
        }

        // Sắp xếp ưu tiên hiển thị nhân sự có giờ kịch bản > 0 lên trước
        rows.sort((r1, r2) -> r2.totalScenarioHours().compareTo(r1.totalScenarioHours()));

        return new ApplyScenarioPreviewResult(
                scenario.getId(),
                scenario.getCode(),
                scenario.getName(),
                targetProject.getId().value(),
                targetProject.getProjectName(),
                isBaselineStale,
                staleReasons,
                weekHeaders,
                rows,
                affectedCount,
                totalAdditionalHoursAll
        );
    }

    @Override
    public ApplyScenarioResult applyScenario(ApplyScenarioCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Dữ liệu áp dụng kịch bản không được để trống");
        }

        User currentUser = requireResourceManagerUser();
        Long currentUserId = currentUser.getIdValue();

        ResourceScenario scenario = loadScenarioPort.findById(command.scenarioId())
                .orElseThrow(() -> new ScenarioNotFoundException(command.scenarioId()));

        if (scenario.getStatus() == ScenarioStatus.APPLIED) {
            throw new ScenarioAlreadyAppliedException("Kịch bản " + scenario.getCode() + " đã được áp dụng vào dữ liệu thật trước đó");
        }
        if (scenario.getStatus() != ScenarioStatus.DRAFT) {
            throw new ScenarioNotModifiableException("Chỉ được áp dụng kịch bản ở trạng thái draft. Trạng thái hiện tại: " + scenario.getStatus().getValue());
        }

        validateScenarioScope(currentUser, scenario.getOrgUnitId());

        Project targetProject = loadProjectPort.findById(new ProjectId(command.targetProjectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.targetProjectId()));

        validateProjectScope(currentUser, targetProject);

        List<YearWeek> targetWeeks = buildTargetWeeks(scenario.getFromYear(), scenario.getFromWeek(), scenario.getDurationWeeks());
        List<ScenarioAllocationSnapshotItem> snapshotItems = loadSnapshotPort.findByScenarioId(scenario.getId());
        List<ScenarioDemand> demands = loadDemandPort.findByScenarioId(scenario.getId());

        List<Long> snapshotEmpIds = snapshotItems.stream()
                .map(ScenarioAllocationSnapshotItem::getEmployeeId)
                .distinct()
                .toList();

        Map<Long, Employee> employeeMap = loadEmployeeMap(snapshotEmpIds);

        // 1. Kiểm tra tính tươi mới của baseline snapshot (TC-02)
        List<String> staleReasons = checkBaselineStale(snapshotItems, targetWeeks, employeeMap);
        if (!staleReasons.isEmpty()) {
            throw new ScenarioBaselineStaleException(
                    "Dữ liệu phân bổ thật đã thay đổi sau khi kịch bản được tạo. Vui lòng làm mới kịch bản trước khi áp dụng."
            );
        }

        // 2. Tính toán phân bổ số giờ kịch bản cho từng nhân sự
        Map<Long, Map<String, BigDecimal>> empDemandHoursMap = calculateDemandDistribution(demands, snapshotEmpIds, employeeMap, targetWeeks);

        // 3. Nạp phân bổ hiện tại trên dự án mục tiêu
        List<WeeklyProjectAllocation> existingProjectAllocations = loadAllocationPort.loadAllocationsForProjectInWeekRange(
                command.targetProjectId(),
                scenario.getFromYear(),
                targetWeeks.get(0).weekNumber(),
                targetWeeks.get(targetWeeks.size() - 1).weekNumber()
        );
        Map<String, WeeklyProjectAllocation> existingAllocMap = existingProjectAllocations.stream()
                .collect(Collectors.toMap(
                        a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                        a -> a,
                        (a, b) -> a
                ));

        // 4. Cập nhật hoặc tạo mới weekly_project_allocations cho dự án mục tiêu (TC-01 & QTN-14)
        int appliedCount = 0;
        Set<Long> affectedEmployees = new HashSet<>();

        for (Map.Entry<Long, Map<String, BigDecimal>> empEntry : empDemandHoursMap.entrySet()) {
            Long empId = empEntry.getKey();
            Map<String, BigDecimal> weekHoursMap = empEntry.getValue();

            for (YearWeek yw : targetWeeks) {
                String key = makeKey(empId, yw.year(), yw.weekNumber());
                BigDecimal additionalHours = weekHoursMap.getOrDefault(key, BigDecimal.ZERO);

                if (additionalHours.compareTo(BigDecimal.ZERO) > 0) {
                    WeeklyProjectAllocation existingAlloc = existingAllocMap.get(key);
                    if (existingAlloc != null) {
                        BigDecimal newHours = existingAlloc.getAllocatedHours().add(additionalHours);
                        existingAlloc.updateAllocatedHours(newHours);
                        saveAllocationPort.save(existingAlloc);
                    } else {
                        WeeklyProjectAllocation newAlloc = WeeklyProjectAllocation.createNew(
                                empId,
                                command.targetProjectId(),
                                yw,
                                additionalHours
                        );
                        saveAllocationPort.save(newAlloc);
                    }
                    appliedCount++;
                    affectedEmployees.add(empId);
                }
            }
        }

        // 5. Cập nhật trạng thái kịch bản thành APPLIED
        scenario.markAsApplied(command.targetProjectId(), currentUserId);
        saveScenarioPort.save(scenario);

        // 6. Ghi nhật ký kiểm toán nguyên tử (TC-04)
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "APPLY_SCENARIO_TO_REAL_ALLOCATION",
                "resource_scenarios",
                scenario.getId(),
                null,
                String.format(
                        "code=%s;targetProjectId=%d;appliedAllocations=%d;affectedEmployees=%d;note=%s",
                        scenario.getCode(),
                        command.targetProjectId(),
                        appliedCount,
                        affectedEmployees.size(),
                        command.note() != null ? command.note() : ""
                )
        ));

        return new ApplyScenarioResult(
                scenario.getId(),
                scenario.getCode(),
                targetProject.getId().value(),
                targetProject.getProjectName(),
                scenario.getStatus().getValue(),
                appliedCount,
                affectedEmployees.size(),
                scenario.getAppliedAt(),
                "Áp dụng kịch bản " + scenario.getCode() + " vào dự án " + targetProject.getProjectName() + " thành công!"
        );
    }

    @Override
    public ScenarioResult refreshScenarioBaseline(Long scenarioId) {
        User currentUser = requireResourceManagerUser();

        ResourceScenario scenario = loadScenarioPort.findById(scenarioId)
                .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));

        validateScenarioScope(currentUser, scenario.getOrgUnitId());

        if (scenario.getStatus() != ScenarioStatus.DRAFT) {
            throw new ScenarioNotModifiableException(
                    "Không thể làm mới kịch bản ở trạng thái: " + scenario.getStatus().getValue() + ". Chỉ được làm mới kịch bản ở trạng thái draft."
            );
        }

        // 1. Xóa toàn bộ snapshot cũ
        deleteSnapshotPort.deleteByScenarioId(scenarioId);

        // 2. Chụp lại snapshot mới từ dữ liệu thật hiện tại
        List<YearWeek> targetWeeks = buildTargetWeeks(scenario.getFromYear(), scenario.getFromWeek(), scenario.getDurationWeeks());
        List<Long> branchOrgUnitIds = resolveScopeBranchOrgUnitIds(scenario.getOrgUnitId());
        List<Employee> branchEmployees = loadEmployeePort.findActiveByOrgUnitIds(branchOrgUnitIds);

        List<ScenarioAllocationSnapshotItem> newSnapshotItems = new ArrayList<>();
        if (!branchEmployees.isEmpty()) {
            List<Long> empIds = branchEmployees.stream().map(Employee::getIdValue).toList();

            List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(empIds, targetWeeks);
            Map<String, BigDecimal> allocationMap = allocations.stream()
                    .collect(Collectors.groupingBy(
                            a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                            Collectors.reducing(BigDecimal.ZERO, WeeklyProjectAllocation::getAllocatedHours, BigDecimal::add)
                    ));

            List<WeeklyAvailability> availabilities = loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(empIds, targetWeeks);
            Map<String, WeeklyAvailability> availabilityMap = availabilities.stream()
                    .collect(Collectors.toMap(
                            a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                            a -> a,
                            (e1, e2) -> e1
                    ));

            Map<Long, Map<YearWeek, BigDecimal>> leaveHoursMap = loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(empIds, targetWeeks);

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

                    newSnapshotItems.add(ScenarioAllocationSnapshotItem.create(
                            scenario.getId(),
                            emp.getIdValue(),
                            yw.year(),
                            yw.weekNumber(),
                            allocatedHours,
                            availableHours
                    ));
                }
            }

            saveSnapshotPort.saveAll(newSnapshotItems);
        }

        // 3. Cập nhật baseSnapshotAt trên scenario
        scenario.updateBaseSnapshotAt(LocalDateTime.now());
        ResourceScenario savedScenario = saveScenarioPort.save(scenario);

        // 4. Ghi Audit log
        saveAuditLogPort.save(AuditLog.createChange(
                currentUser.getIdValue(),
                "REFRESH_SCENARIO_BASELINE",
                "resource_scenarios",
                scenario.getId(),
                null,
                "Refreshed baseline snapshot for scenario " + scenario.getCode()
        ));

        String orgUnitName = loadOrgUnitPort.findById(new OrgUnitId(savedScenario.getOrgUnitId()))
                .map(OrgUnit::getUnitName)
                .orElse("Không xác định");

        return new ScenarioResult(
                savedScenario.getId(),
                savedScenario.getCode(),
                savedScenario.getName(),
                savedScenario.getDescription(),
                savedScenario.getOrgUnitId(),
                orgUnitName,
                savedScenario.getStatus().getValue(),
                savedScenario.getFromYear(),
                savedScenario.getFromWeek(),
                savedScenario.getDurationWeeks(),
                savedScenario.getBaseSnapshotAt(),
                savedScenario.getCreatedBy(),
                currentUser.getUsername(),
                savedScenario.getCreatedAt(),
                savedScenario.getUpdatedAt(),
                loadDemandPort.findByScenarioId(savedScenario.getId()).size(),
                branchEmployees.size(),
                savedScenario.getTargetProjectId(),
                savedScenario.getAppliedAt(),
                savedScenario.getAppliedBy()
        );
    }

    private User requireResourceManagerUser() {
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        return loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));
    }

    private List<String> checkBaselineStale(
            List<ScenarioAllocationSnapshotItem> snapshotItems,
            List<YearWeek> targetWeeks,
            Map<Long, Employee> employeeMap
    ) {
        List<Long> snapshotEmpIds = snapshotItems.stream()
                .map(ScenarioAllocationSnapshotItem::getEmployeeId)
                .distinct()
                .toList();

        List<WeeklyProjectAllocation> currentAllocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(snapshotEmpIds, targetWeeks);
        Map<String, BigDecimal> currentAllocMap = currentAllocations.stream()
                .collect(Collectors.toMap(
                        a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                        WeeklyProjectAllocation::getAllocatedHours,
                        BigDecimal::add
                ));

        List<String> staleReasons = new ArrayList<>();
        for (ScenarioAllocationSnapshotItem item : snapshotItems) {
            String key = makeKey(item.getEmployeeId(), item.getYearNumber(), item.getWeekNumber());
            BigDecimal currentAllocated = currentAllocMap.getOrDefault(key, BigDecimal.ZERO);

            if (currentAllocated.compareTo(item.getAllocatedHours()) != 0) {
                Employee emp = employeeMap.get(item.getEmployeeId());
                String empName = emp != null ? emp.getFullName() : "ID " + item.getEmployeeId();
                staleReasons.add(String.format(
                        "Nhân sự %s: phân bổ tuần %d/%d gốc đã đổi từ %s giờ sang %s giờ",
                        empName,
                        item.getWeekNumber(),
                        item.getYearNumber(),
                        item.getAllocatedHours().stripTrailingZeros().toPlainString(),
                        currentAllocated.stripTrailingZeros().toPlainString()
                ));
            }
        }
        return staleReasons;
    }

    private Map<Long, Map<String, BigDecimal>> calculateDemandDistribution(
            List<ScenarioDemand> demands,
            List<Long> empIds,
            Map<Long, Employee> employeeMap,
            List<YearWeek> targetWeeks
    ) {
        Map<Long, Map<String, BigDecimal>> empDemandHoursMap = new HashMap<>();

        for (YearWeek yw : targetWeeks) {
            String weekKey = makeKey(yw.year(), yw.weekNumber());
            List<ScenarioDemand> activeDemands = demands.stream()
                    .filter(d -> d.isActiveInWeek(yw))
                    .toList();

            for (ScenarioDemand d : activeDemands) {
                BigDecimal totalDemandHours = d.getTotalHoursPerWeek();
                if (totalDemandHours.compareTo(BigDecimal.ZERO) <= 0) continue;

                String req = d.getSkillRequirement();
                List<Long> matchingEmpIds = empIds.stream()
                        .filter(id -> {
                            Employee emp = employeeMap.get(id);
                            return emp != null && isRoleMatching(emp.getProfessionalRole(), req);
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
                        String mapKey = makeKey(empId, yw.year(), yw.weekNumber());
                        empDemandHoursMap
                                .computeIfAbsent(empId, k -> new HashMap<>())
                                .merge(mapKey, empHours, BigDecimal::add);
                    }
                }
            }
        }

        return empDemandHoursMap;
    }

    private Map<Long, Employee> loadEmployeeMap(List<Long> empIds) {
        List<EmployeeId> employeeIds = empIds.stream().map(EmployeeId::new).toList();
        List<Employee> employees = employeeIds.isEmpty() ? List.of() : loadEmployeePort.findAllByIdIn(employeeIds);
        return employees.stream()
                .filter(Objects::nonNull)
                .filter(e -> e.getIdValue() != null)
                .collect(Collectors.toMap(Employee::getIdValue, e -> e, (e1, e2) -> e1));
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

    private String makeKey(int year, int weekNumber) {
        return year + "_" + weekNumber;
    }

    private void validateScenarioScope(User currentUser, Long targetOrgUnitId) {
        if (!isOrgUnitInUserScope(currentUser, targetOrgUnitId)) {
            throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        }
    }

    private void validateProjectScope(User currentUser, Project project) {
        if (project.getOrgUnitId() == null) {
            return;
        }
        if (!isOrgUnitInUserScope(currentUser, project.getOrgUnitId())) {
            throw new InvalidTargetProjectException("Dự án mục tiêu không thuộc phạm vi quản lý của bạn");
        }
    }

    private boolean isOrgUnitInUserScope(User currentUser, Long targetOrgUnitId) {
        return AuthorizationService.isOrgUnitInUserScope(currentUser, targetOrgUnitId, loadOrgUnitPort);
    }
}
