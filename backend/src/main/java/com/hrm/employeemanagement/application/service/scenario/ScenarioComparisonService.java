package com.hrm.employeemanagement.application.service.scenario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.scenario.CompareScenariosCommand;
import com.hrm.employeemanagement.application.dto.scenario.OverloadedEmployeeResult;
import com.hrm.employeemanagement.application.dto.scenario.OverloadedEmployeeSummaryResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioComparisonItemResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioComparisonResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioSimulationResult;
import com.hrm.employeemanagement.application.dto.scenario.WeeklySimulationMetricResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.CompareSimulationScenariosUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.GetScenarioSimulationResultUseCase;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.scenario.InsufficientScenariosForComparisonException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class ScenarioComparisonService implements CompareSimulationScenariosUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadResourceScenarioPort loadScenarioPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final GetScenarioSimulationResultUseCase simulationResultUseCase;
    private final SaveAuditLogInNewTransactionPort saveAuditLogPort;

    public ScenarioComparisonService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadResourceScenarioPort loadScenarioPort,
            LoadOrgUnitPort loadOrgUnitPort,
            GetScenarioSimulationResultUseCase simulationResultUseCase,
            SaveAuditLogInNewTransactionPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadScenarioPort = Objects.requireNonNull(loadScenarioPort, "LoadResourceScenarioPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.simulationResultUseCase = Objects.requireNonNull(simulationResultUseCase, "GetScenarioSimulationResultUseCase must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
    }

    @Override
    public ScenarioComparisonResult compareScenarios(CompareScenariosCommand command) {
        // 1. Kiểm tra quyền hạn: Ban giám đốc (VT-01) và Permission RESOURCE_SCENARIO_COMPARE
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_COMPARE);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        if (currentUser.getRole() == null || currentUser.getRole().getCode() != RoleCode.VT_01) {
            saveAuditLogPort.save(AuditLog.createChange(
                    currentUserId,
                    "PERMISSION_DENIED",
                    "resource_scenarios",
                    null,
                    null,
                    "permission=RESOURCE_SCENARIO_COMPARE;reason=ONLY_EXECUTIVE_ROLE_ALLOWED"
            ));
            throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_COMPARE);
        }

        // 2. Validate điều kiện số lượng kịch bản (NCL-08-CN-004-TC-02)
        if (command == null || command.scenarioIds() == null) {
            throw new InsufficientScenariosForComparisonException("Cần ít nhất hai kịch bản để so sánh");
        }

        List<Long> distinctScenarioIds = command.scenarioIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctScenarioIds.size() < 2) {
            throw new InsufficientScenariosForComparisonException("Cần ít nhất hai kịch bản để so sánh");
        }
        if (distinctScenarioIds.size() > 10) {
            throw new IllegalArgumentException("Chỉ được so sánh tối đa 10 kịch bản cùng lúc");
        }

        // 3. Kiểm tra sự tồn tại của tất cả kịch bản trước khi tính toán
        Map<Long, ResourceScenario> scenarioMap = new java.util.LinkedHashMap<>();
        for (Long scenarioId : distinctScenarioIds) {
            ResourceScenario scenario = loadScenarioPort.findById(scenarioId)
                    .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));
            scenarioMap.put(scenarioId, scenario);
        }

        // 4. So sánh từng kịch bản (QTN-14: Đọc snapshot và tính toán cách ly, không đụng phân bổ thật)
        List<ScenarioComparisonItemResult> comparisonItems = new ArrayList<>();
        for (Map.Entry<Long, ResourceScenario> scenarioEntry : scenarioMap.entrySet()) {
            Long scenarioId = scenarioEntry.getKey();
            ResourceScenario scenario = scenarioEntry.getValue();

            String orgUnitName = loadOrgUnitPort.findById(new OrgUnitId(scenario.getOrgUnitId()))
                    .map(OrgUnit::getUnitName)
                    .orElse("Không xác định");

            ScenarioSimulationResult simResult = simulationResultUseCase.getSimulationResult(scenarioId);
            if (simResult == null) {
                throw new ScenarioNotFoundException("Không thể tính toán kết quả mô phỏng cho kịch bản: " + scenarioId);
            }

            // Tính toán các chỉ số so sánh
            List<WeeklySimulationMetricResult> weeklyMetrics = simResult.weeklyMetrics();
            int overloadedEmployeesCount = (int) simResult.overloadedEmployees().stream()
                    .map(OverloadedEmployeeResult::employeeId)
                    .distinct()
                    .count();

            BigDecimal totalShortfallHours = weeklyMetrics.stream()
                    .map(WeeklySimulationMetricResult::excessHours)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalDemandHours = weeklyMetrics.stream()
                    .map(WeeklySimulationMetricResult::demandHours)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalWorkloadHours = weeklyMetrics.stream()
                    .map(WeeklySimulationMetricResult::scenarioWorkloadHours)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalAvailableHours = weeklyMetrics.stream()
                    .map(WeeklySimulationMetricResult::availableHours)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal averageUtilizationPercentage = totalAvailableHours.compareTo(BigDecimal.ZERO) > 0
                    ? totalWorkloadHours.multiply(BigDecimal.valueOf(100)).divide(totalAvailableHours, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal peakUtilizationPercentage = weeklyMetrics.stream()
                    .map(WeeklySimulationMetricResult::utilizationPercentage)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            // Tổng hợp danh sách nhân sự quá tải
            Map<Long, List<OverloadedEmployeeResult>> empOverloads = simResult.overloadedEmployees().stream()
                    .collect(Collectors.groupingBy(OverloadedEmployeeResult::employeeId));

            List<OverloadedEmployeeSummaryResult> overloadedSummaries = empOverloads.entrySet().stream()
                    .map(entry -> {
                        List<OverloadedEmployeeResult> list = entry.getValue();
                        OverloadedEmployeeResult first = list.get(0);
                        BigDecimal maxExcess = list.stream()
                                .map(OverloadedEmployeeResult::excessHours)
                                .filter(Objects::nonNull)
                                .max(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO);
                        BigDecimal peakUtilization = list.stream()
                                .map(OverloadedEmployeeResult::utilizationPercentage)
                                .filter(Objects::nonNull)
                                .max(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO);
                        return new OverloadedEmployeeSummaryResult(
                                first.employeeId(),
                                first.employeeCode(),
                                first.fullName(),
                                first.professionalRole(),
                                list.size(),
                                maxExcess,
                                peakUtilization
                        );
                    })
                    .sorted(Comparator.comparing(OverloadedEmployeeSummaryResult::overloadedWeeksCount).reversed()
                            .thenComparing(OverloadedEmployeeSummaryResult::fullName, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            comparisonItems.add(new ScenarioComparisonItemResult(
                    scenario.getId(),
                    scenario.getCode(),
                    scenario.getName(),
                    scenario.getDescription(),
                    scenario.getOrgUnitId(),
                    orgUnitName,
                    scenario.getStatus().getValue(),
                    scenario.getFromYear(),
                    scenario.getFromWeek(),
                    scenario.getDurationWeeks(),
                    overloadedEmployeesCount,
                    totalShortfallHours,
                    totalShortfallHours, // totalRequiredAdditionalHours: Tổng giờ cần bổ sung
                    totalDemandHours,
                    totalWorkloadHours,
                    totalAvailableHours,
                    averageUtilizationPercentage,
                    peakUtilizationPercentage,
                    weeklyMetrics,
                    overloadedSummaries
            ));
        }

        LocalDateTime comparedAt = LocalDateTime.now();

        // 5. Tính toán metadata căn chỉnh kịch bản
        boolean isTimeframeAligned = comparisonItems.stream()
                .map(item -> item.fromYear() + "_" + item.fromWeek() + "_" + item.durationWeeks())
                .distinct()
                .count() <= 1;

        boolean isOrgUnitAligned = comparisonItems.stream()
                .map(ScenarioComparisonItemResult::orgUnitId)
                .filter(Objects::nonNull)
                .distinct()
                .count() <= 1;

        // 6. Ghi Audit Log lưu vết lịch sử thao tác so sánh kịch bản (NCL-08-CN-004-TC-04)
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "COMPARE_SCENARIOS",
                "resource_scenarios",
                distinctScenarioIds.get(0),
                null,
                "scenarioIds=" + distinctScenarioIds + ";scenariosCount=" + distinctScenarioIds.size()
        ));

        return new ScenarioComparisonResult(comparisonItems, isTimeframeAligned, isOrgUnitAligned, comparedAt);
    }
}
