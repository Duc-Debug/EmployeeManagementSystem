package com.hrm.employeemanagement.application.service.scenario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

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
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.scenario.InvalidTargetProjectException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioAlreadyAppliedException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioBaselineStaleException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioDemandCapacityExceededException;
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
import com.hrm.employeemanagement.domain.scenario.ScenarioDemandDistributionPolicy;
import com.hrm.employeemanagement.domain.scenario.ScenarioDistributionResult;
import com.hrm.employeemanagement.domain.scenario.ScenarioStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class ApplyResourceScenarioService implements
        PreviewApplyScenarioUseCase,
        ApplyScenarioUseCase,
        RefreshScenarioBaselineUseCase {

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
    private final SaveAuditLogPort saveAuditLogPort;
    private final ScenarioBaselineValidator scenarioBaselineValidator;

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
            SaveAuditLogPort saveAuditLogPort,
            ScenarioBaselineValidator scenarioBaselineValidator
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
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.scenarioBaselineValidator = Objects.requireNonNull(scenarioBaselineValidator, "ScenarioBaselineValidator must not be null");
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

        List<Long> branchOrgUnitIds = resolveScopeBranchOrgUnitIds(scenario.getOrgUnitId());

        // 1. Kiểm tra tính toàn vẹn của baseline snapshot (TC-02, HIGH-02)
        List<String> staleReasons = scenarioBaselineValidator.checkBaselineStale(snapshotItems, targetWeeks, employeeMap, branchOrgUnitIds);
        boolean isBaselineStale = !staleReasons.isEmpty();

        Map<String, BigDecimal> snapshotAvailMap = snapshotItems.stream()
                .collect(Collectors.toMap(
                        i -> makeKey(i.getEmployeeId(), i.getYearNumber(), i.getWeekNumber()),
                        ScenarioAllocationSnapshotItem::getAvailableHours,
                        (a, b) -> a
                ));

        // Năng lực còn lại thực tế của nhân sự sau khi trừ phân bổ hiện có: Remaining = max(0, Available - Allocated)
        Map<String, BigDecimal> snapshotRemainingMap = snapshotItems.stream()
                .collect(Collectors.toMap(
                        i -> makeKey(i.getEmployeeId(), i.getYearNumber(), i.getWeekNumber()),
                        i -> i.getAvailableHours().subtract(i.getAllocatedHours()).max(BigDecimal.ZERO),
                        (a, b) -> a
                ));

        // 2. Tính toán phân bổ nhu cầu kịch bản xuống nhân sự dựa trên năng lực còn lại (HIGH-03, HIGH-04)
        ScenarioDistributionResult distResult = ScenarioDemandDistributionPolicy.calculateDistributionWithMetrics(
                demands,
                snapshotEmpIds,
                employeeMap,
                targetWeeks,
                snapshotRemainingMap
        );
        Map<Long, Map<String, BigDecimal>> empDemandHoursMap = distResult.empDemandHoursMap();

        // 3. Nạp phân bổ hiện tại trên dự án mục tiêu (hỗ trợ vắt năm)
        List<WeeklyProjectAllocation> targetProjectAllocations = loadAllocationPort.loadAllocationsForProjectInWeeks(
                targetProjectId,
                targetWeeks
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
                totalAdditionalHoursAll,
                distResult.totalRequestedHours(),
                distResult.totalAppliedHours(),
                distResult.totalUnfulfilledHours(),
                distResult.isPartiallyFulfilled(),
                distResult.unfulfilledDetails()
        );
    }

    @Override
    public ApplyScenarioResult applyScenario(ApplyScenarioCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Dữ liệu áp dụng kịch bản không được để trống");
        }

        User currentUser = requireResourceManagerUser();
        Long currentUserId = currentUser.getIdValue();

        ResourceScenario scenario = loadScenarioPort.findByIdForUpdate(command.scenarioId())
                .orElseThrow(() -> new ScenarioNotFoundException(command.scenarioId()));

        if (scenario.getStatus() == ScenarioStatus.APPLIED) {
            throw new ScenarioAlreadyAppliedException("Kịch bản " + scenario.getCode() + " đã được áp dụng vào dữ liệu thật trước đó");
        }
        if (scenario.getStatus() != ScenarioStatus.DRAFT) {
            throw new ScenarioNotModifiableException("Chỉ được áp dụng kịch bản ở trạng thái draft. Trạng thái hiện tại: " + scenario.getStatus().getValue());
        }

        validateScenarioScope(currentUser, scenario.getOrgUnitId());

        Project targetProject = loadProjectPort.findByIdForUpdate(new ProjectId(command.targetProjectId()))
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

        List<Long> branchOrgUnitIds = resolveScopeBranchOrgUnitIds(scenario.getOrgUnitId());

        // 1. Thiết lập Concurrency Boundary: Khóa bi quan các phân bổ của dự án và nhân sự trong các tuần (HIGH-01)
        // Khóa bi quan các hàng nhân sự (Employee) theo thứ tự ID tăng dần để ngăn ngừa deadlock và race condition
        // khi tạo mới phân bổ chưa từng tồn tại (Root Aggregate Lock tương thích với ResourceAllocationService)
        snapshotEmpIds.stream()
                .sorted()
                .forEach(empId -> loadEmployeePort.findByIdForUpdate(new EmployeeId(empId)));

        List<WeeklyProjectAllocation> existingProjectAllocations = loadAllocationPort.loadAllocationsForProjectInWeeksForUpdate(
                command.targetProjectId(),
                targetWeeks
        );
        loadAllocationPort.loadAllocationsForEmployeesAndWeeksForUpdate(snapshotEmpIds, targetWeeks);

        // 2. Kiểm tra tính tươi mới của baseline snapshot và tính hợp lệ nhân sự (TC-02, HIGH-02)
        List<String> staleReasons = scenarioBaselineValidator.checkBaselineStale(snapshotItems, targetWeeks, employeeMap, branchOrgUnitIds);
        if (!staleReasons.isEmpty()) {
            throw new ScenarioBaselineStaleException(
                    "Dữ liệu phân bổ thật đã thay đổi sau khi kịch bản được tạo. Vui lòng làm mới kịch bản trước khi áp dụng."
            );
        }

        // Năng lực còn lại thực tế của nhân sự sau khi trừ phân bổ hiện có: Remaining = max(0, Available - Allocated)
        Map<String, BigDecimal> snapshotRemainingMap = snapshotItems.stream()
                .collect(Collectors.toMap(
                        i -> makeKey(i.getEmployeeId(), i.getYearNumber(), i.getWeekNumber()),
                        i -> i.getAvailableHours().subtract(i.getAllocatedHours()).max(BigDecimal.ZERO),
                        (a, b) -> a
                ));

        // 3. Tính toán phân bổ số giờ kịch bản cho từng nhân sự dựa trên năng lực còn lại (HIGH-03, HIGH-04, Comment 1)
        ScenarioDistributionResult distResult = ScenarioDemandDistributionPolicy.calculateDistributionWithMetrics(
                demands,
                snapshotEmpIds,
                employeeMap,
                targetWeeks,
                snapshotRemainingMap
        );
        Map<Long, Map<String, BigDecimal>> empDemandHoursMap = distResult.empDemandHoursMap();

        // 3.1. Kiểm tra trần capacity và yêu cầu đáp ứng trọn vẹn (Comment 1)
        if (distResult.isPartiallyFulfilled() && !command.isAllowPartialFulfillment()) {
            throw new ScenarioDemandCapacityExceededException(String.format(
                    "Không thể áp dụng kịch bản %s: Nhu cầu yêu cầu tổng cộng %s giờ nhưng chỉ có thể phân bổ %s giờ (thiếu %s giờ do vượt trần năng lực tuần của nhân sự). Bật tùy chọn 'allowPartialFulfillment' nếu bạn muốn chấp nhận phân bổ một phần.",
                    scenario.getCode(),
                    distResult.totalRequestedHours().stripTrailingZeros().toPlainString(),
                    distResult.totalAppliedHours().stripTrailingZeros().toPlainString(),
                    distResult.totalUnfulfilledHours().stripTrailingZeros().toPlainString()
            ));
        }

        // 4. Ánh xạ các phân bổ hiện tại trên dự án mục tiêu (hỗ trợ vắt năm)
        Map<String, WeeklyProjectAllocation> existingAllocMap = existingProjectAllocations.stream()
                .collect(Collectors.toMap(
                        a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                        a -> a,
                        (a, b) -> {
                            throw new IllegalStateException(String.format(
                                    "Dữ liệu phân bổ trên dự án ID %d có bản ghi trùng lặp vi phạm ràng buộc cho nhân sự ID %d ở tuần %d/%d",
                                    command.targetProjectId(), a.getEmployeeId(), a.getWeekNumber(), a.getYear()
                            ));
                        }
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
                        "code=%s;targetProjectId=%d;appliedAllocations=%d;affectedEmployees=%d;allowPartialFulfillment=%b;unfulfilledHours=%s;note=%s",
                        scenario.getCode(),
                        command.targetProjectId(),
                        appliedCount,
                        affectedEmployees.size(),
                        command.isAllowPartialFulfillment(),
                        distResult.totalUnfulfilledHours().stripTrailingZeros().toPlainString(),
                        command.note() != null ? command.note() : ""
                )
        ));

        String message = distResult.isPartiallyFulfilled()
                ? String.format("Áp dụng một phần thành công kịch bản %s vào dự án %s: đã phân bổ %s/%s giờ (thiếu %s giờ do chạm trần năng lực)",
                        scenario.getCode(),
                        targetProject.getProjectName(),
                        distResult.totalAppliedHours().stripTrailingZeros().toPlainString(),
                        distResult.totalRequestedHours().stripTrailingZeros().toPlainString(),
                        distResult.totalUnfulfilledHours().stripTrailingZeros().toPlainString())
                : "Áp dụng kịch bản " + scenario.getCode() + " vào dự án " + targetProject.getProjectName() + " thành công!";

        return new ApplyScenarioResult(
                scenario.getId(),
                scenario.getCode(),
                targetProject.getId().value(),
                targetProject.getProjectName(),
                scenario.getStatus().getValue(),
                appliedCount,
                affectedEmployees.size(),
                scenario.getAppliedAt(),
                message,
                distResult.totalRequestedHours(),
                distResult.totalAppliedHours(),
                distResult.totalUnfulfilledHours(),
                distResult.isPartiallyFulfilled(),
                distResult.unfulfilledDetails()
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
            Map<Long, Employee> employeeMap = branchEmployees.stream().collect(Collectors.toMap(Employee::getIdValue, e -> e, (e1, e2) -> e1));

            List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(empIds, targetWeeks);
            Map<String, BigDecimal> allocationMap = allocations.stream()
                    .collect(Collectors.groupingBy(
                            a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                            Collectors.reducing(BigDecimal.ZERO, WeeklyProjectAllocation::getAllocatedHours, BigDecimal::add)
                    ));

            Map<String, BigDecimal> availableMap = scenarioBaselineValidator.calculateCurrentAvailableHours(empIds, targetWeeks, employeeMap);

            for (Employee emp : branchEmployees) {
                for (YearWeek yw : targetWeeks) {
                    String key = makeKey(emp.getIdValue(), yw.year(), yw.weekNumber());
                    BigDecimal availableHours = availableMap.getOrDefault(key, BigDecimal.valueOf(40));
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

    private Map<Long, Employee> loadEmployeeMap(List<Long> empIds) {
        List<EmployeeId> employeeIds = empIds.stream().map(EmployeeId::new).toList();
        List<Employee> employees = employeeIds.isEmpty() ? List.of() : loadEmployeePort.findAllByIdIn(employeeIds);
        return employees.stream()
                .filter(Objects::nonNull)
                .filter(e -> e.getIdValue() != null)
                .collect(Collectors.toMap(Employee::getIdValue, e -> e, (e1, e2) -> e1));
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

    private String makeKey(Long employeeId, int year, int weekNumber) {
        return employeeId + "_" + year + "_" + weekNumber;
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
