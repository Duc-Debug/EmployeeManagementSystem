package com.hrm.employeemanagement.application.service.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.allocation.AdjustAllocationCommand;
import com.hrm.employeemanagement.application.dto.allocation.AllocationChangeLogResult;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.AdjustResourceAllocationUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.AllocationNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.CheckActualHoursPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.DeleteWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadAllocationChangeLogPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveAllocationChangeLogPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.AdjustmentAction;
import com.hrm.employeemanagement.domain.allocation.AllocationAdjustmentPolicy;
import com.hrm.employeemanagement.domain.allocation.AllocationChangeLog;
import com.hrm.employeemanagement.domain.allocation.AllocationNotificationPolicy;
import com.hrm.employeemanagement.domain.allocation.WeeklyCapacityMatrixPolicy;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationNotFoundException;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationOverloadWarningException;
import com.hrm.employeemanagement.domain.exception.allocation.EmployeeInactiveException;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationAdjustmentException;
import com.hrm.employeemanagement.domain.exception.allocation.OutsourcedContractPeriodException;
import com.hrm.employeemanagement.domain.exception.allocation.ProjectInactiveException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Application service thực thi Use Case điều chỉnh phân bổ nguồn lực (NCL-06-CN-004 / QTN-15).
 * Pure Java, không phụ thuộc framework, giao dịch được bọc ngoài bằng Transaction Decorator.
 */
public class AdjustResourceAllocationService implements AdjustResourceAllocationUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final SaveWeeklyProjectAllocationPort saveAllocationPort;
    private final DeleteWeeklyProjectAllocationPort deleteAllocationPort;
    private final SaveAllocationChangeLogPort saveChangeLogPort;
    private final LoadAllocationChangeLogPort loadChangeLogPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort deniedAuditLogPort;
    private final CheckActualHoursPort checkActualHoursPort;
    private final AllocationNotificationPort notificationPort;
    private final LoadOrgUnitPort loadOrgUnitPort;

    public AdjustResourceAllocationService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            DeleteWeeklyProjectAllocationPort deleteAllocationPort,
            SaveAllocationChangeLogPort saveChangeLogPort,
            LoadAllocationChangeLogPort loadChangeLogPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort,
            CheckActualHoursPort checkActualHoursPort,
            AllocationNotificationPort notificationPort,
            LoadOrgUnitPort loadOrgUnitPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "LoadWeeklyAvailabilityPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.saveAllocationPort = Objects.requireNonNull(saveAllocationPort, "SaveWeeklyProjectAllocationPort must not be null");
        this.deleteAllocationPort = Objects.requireNonNull(deleteAllocationPort, "DeleteWeeklyProjectAllocationPort must not be null");
        this.saveChangeLogPort = Objects.requireNonNull(saveChangeLogPort, "SaveAllocationChangeLogPort must not be null");
        this.loadChangeLogPort = Objects.requireNonNull(loadChangeLogPort, "LoadAllocationChangeLogPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.deniedAuditLogPort = Objects.requireNonNull(deniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.checkActualHoursPort = Objects.requireNonNull(checkActualHoursPort, "CheckActualHoursPort must not be null");
        this.notificationPort = Objects.requireNonNull(notificationPort, "AllocationNotificationPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
    }

    @Override
    public WeeklyCapacityResult adjustAllocation(Long allocationId, AdjustAllocationCommand command) {
        if (allocationId == null) {
            throw new InvalidAllocationAdjustmentException("Mã dòng phân bổ (allocationId) không được để trống");
        }
        if (command == null || command.action() == null) {
            throw new InvalidAllocationAdjustmentException("Hành động điều chỉnh (action) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        WeeklyProjectAllocation allocation = loadAllocationPort.findById(allocationId)
                .orElseThrow(() -> new AllocationNotFoundException(allocationId));

        Employee employee = loadEmployeePort.findByIdForUpdate(new EmployeeId(allocation.getEmployeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân sự với ID: " + allocation.getEmployeeId()));

        requireOrgUnitInDataScope(currentUser, employee.getOrgUnitId(), currentUserId, allocationId);

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new EmployeeInactiveException("Không thể điều chỉnh phân bổ cho nhân sự không còn ở trạng thái hoạt động");
        }

        Project project = loadProjectPort.findById(new ProjectId(allocation.getProjectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + allocation.getProjectId()));

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new ProjectInactiveException("Không thể điều chỉnh phân bổ nhân sự vào dự án không ở trạng thái hoạt động");
        }

        return switch (command.action()) {
            case ADD -> throw new InvalidAllocationAdjustmentException("Hành động ADD không áp dụng cho điều chỉnh dòng phân bổ hiện có");
            case EDIT_HOURS -> executeEditHours(allocation, employee, project, command, currentUserId);
            case MOVE_WEEK -> executeMoveWeek(allocation, employee, project, command, currentUserId);
            case REMOVE -> {
                executeRemove(allocation, employee, project, currentUserId);
                yield calculateCapacity(employee, allocation.getYearWeek());
            }
            case NOTE_VARIANCE -> executeNoteVariance(allocation, employee, project, command.varianceReason(), currentUserId);
        };
    }

    @Override
    public void removeAllocation(Long allocationId) {
        if (allocationId == null) {
            throw new InvalidAllocationAdjustmentException("Mã dòng phân bổ (allocationId) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        WeeklyProjectAllocation allocation = loadAllocationPort.findById(allocationId)
                .orElseThrow(() -> new AllocationNotFoundException(allocationId));

        Employee employee = loadEmployeePort.findByIdForUpdate(new EmployeeId(allocation.getEmployeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân sự với ID: " + allocation.getEmployeeId()));

        requireOrgUnitInDataScope(currentUser, employee.getOrgUnitId(), currentUserId, allocationId);

        Project project = loadProjectPort.findById(new ProjectId(allocation.getProjectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + allocation.getProjectId()));

        executeRemove(allocation, employee, project, currentUserId);
    }

    @Override
    public WeeklyCapacityResult noteVariance(Long allocationId, String varianceReason) {
        if (allocationId == null) {
            throw new InvalidAllocationAdjustmentException("Mã dòng phân bổ (allocationId) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        WeeklyProjectAllocation allocation = loadAllocationPort.findById(allocationId)
                .orElseThrow(() -> new AllocationNotFoundException(allocationId));

        Employee employee = loadEmployeePort.findByIdForUpdate(new EmployeeId(allocation.getEmployeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân sự với ID: " + allocation.getEmployeeId()));

        requireOrgUnitInDataScope(currentUser, employee.getOrgUnitId(), currentUserId, allocationId);

        Project project = loadProjectPort.findById(new ProjectId(allocation.getProjectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + allocation.getProjectId()));

        return executeNoteVariance(allocation, employee, project, varianceReason, currentUserId);
    }

    @Override
    public List<AllocationChangeLogResult> getHistory(Long allocationId) {
        if (allocationId == null) {
            throw new InvalidAllocationAdjustmentException("Mã dòng phân bổ (allocationId) không được để trống");
        }

        Long currentUserId = authorizationService.requireAny(
                PermissionCode.RESOURCE_ALLOCATION_READ,
                PermissionCode.RESOURCE_ALLOCATION_MANAGE
        );
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        WeeklyProjectAllocation alloc = loadAllocationPort.findById(allocationId)
                .orElseThrow(() -> new AllocationNotFoundException(allocationId));
        Employee employee = loadEmployeePort.findById(new EmployeeId(alloc.getEmployeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân sự với ID: " + alloc.getEmployeeId()));
        requireOrgUnitInDataScope(currentUser, employee.getOrgUnitId(), currentUserId, allocationId);

        List<AllocationChangeLog> logs = loadChangeLogPort.findByAllocationId(allocationId);

        List<UserId> userIds = logs.stream()
                .map(AllocationChangeLog::getChangedBy)
                .filter(Objects::nonNull)
                .distinct()
                .map(UserId::new)
                .toList();

        Map<Long, String> userNames = loadUserPort.findAllByIdIn(userIds).stream()
                .collect(Collectors.toMap(
                        u -> u.getId().value(),
                        User::getUsername,
                        (existing, replacement) -> existing
                ));

        return logs.stream().map(log -> new AllocationChangeLogResult(
                log.getId(),
                log.getAllocationId(),
                log.getAction(),
                log.getOldValue(),
                log.getNewValue(),
                log.getChangedBy(),
                userNames.getOrDefault(log.getChangedBy(), "ID:" + log.getChangedBy()),
                log.getChangedAt(),
                log.getNotifiedPmIds()
        )).toList();
    }

    // =========================================================================
    // PRIVATE EXECUTION HELPERS
    // =========================================================================

    private WeeklyCapacityResult executeEditHours(
            WeeklyProjectAllocation allocation,
            Employee employee,
            Project project,
            AdjustAllocationCommand command,
            Long currentUserId
    ) {
        if (command.newHours() != null && command.allocationPercentage() != null) {
            throw new InvalidAllocationAdjustmentException("Không được cung cấp đồng thời số giờ phân bổ và tỷ lệ phần trăm phân bổ");
        }
        if (command.newHours() == null && command.allocationPercentage() == null) {
            throw new InvalidAllocationAdjustmentException("Vui lòng nhập số giờ phân bổ mới hoặc tỷ lệ phần trăm phân bổ mới");
        }

        YearWeek yearWeek = allocation.getYearWeek();
        Optional<WeeklyAvailability> availabilityOpt = loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employee.getIdValue(), yearWeek);
        int standardHours = employee.getStandardHoursPerWeek() != null ? employee.getStandardHoursPerWeek() : 40;
        BigDecimal netAvailableHours = availabilityOpt.map(WeeklyAvailability::getNetAvailableHours)
                .orElse(BigDecimal.valueOf(standardHours));

        BigDecimal effectiveHours;
        BigDecimal effectivePercentage;

        if (command.allocationPercentage() != null) {
            AllocationAdjustmentPolicy.validateAllocationPercentage(command.allocationPercentage());
            effectivePercentage = command.allocationPercentage();
            effectiveHours = netAvailableHours.multiply(effectivePercentage)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        } else {
            AllocationAdjustmentPolicy.validateNewHours(command.newHours());
            effectiveHours = command.newHours();
            if (netAvailableHours.compareTo(BigDecimal.ZERO) > 0) {
                effectivePercentage = effectiveHours.multiply(BigDecimal.valueOf(100))
                        .divide(netAvailableHours, 2, java.math.RoundingMode.HALF_UP);
            } else {
                effectivePercentage = BigDecimal.ZERO;
            }
        }

        List<WeeklyProjectAllocation> existingAllocations = loadAllocationPort.loadAllocationsForEmployee(employee.getIdValue(), yearWeek);
        BigDecimal otherProjectsAllocatedSum = existingAllocations.stream()
                .filter(a -> !a.getId().equals(allocation.getId()))
                .map(WeeklyProjectAllocation::getAllocatedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRequestedAllocated = otherProjectsAllocatedSum.add(effectiveHours);
        boolean isOverloaded = WeeklyCapacityMatrixPolicy.isOverloaded(totalRequestedAllocated, netAvailableHours);
        BigDecimal excessHours = WeeklyCapacityMatrixPolicy.calculateExcessHours(totalRequestedAllocated, netAvailableHours);

        if (isOverloaded) {
            String reason = command.overloadReason();
            if (reason == null || reason.trim().isEmpty()) {
                throw new AllocationOverloadWarningException(
                        "Không thể điều chỉnh phân bổ: Tổng số giờ (" + totalRequestedAllocated + "h) vượt quá số giờ khả dụng ("
                                + netAvailableHours + "h) của nhân sự trong tuần " + yearWeek.weekNumber() + "/" + yearWeek.year()
                                + ". Số giờ vượt: " + excessHours + "h. Yêu cầu Quản lý nguồn lực xác nhận có ghi rõ lý do.",
                        netAvailableHours,
                        totalRequestedAllocated,
                        excessHours
                );
            }

            if (!authorizationService.hasPermission(PermissionCode.RESOURCE_ALLOCATION_OVERLOAD_BYPASS)) {
                deniedAuditLogPort.save(AuditLog.createChange(
                        currentUserId,
                        "ACCESS_DENIED_OVERLOAD_CONFIRM",
                        "weekly_project_allocations",
                        allocation.getId(),
                        null,
                        "user_id=" + currentUserId + ";attempted_overload_hours=" + excessHours
                ));
                throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_OVERLOAD_BYPASS);
            }
        }

        String oldValue = allocation.getAllocatedHours() + "h"
                + (allocation.getAllocationPercentage() != null ? " (" + allocation.getAllocationPercentage() + "%)" : "");
        String newValue = effectiveHours + "h (" + effectivePercentage + "%)";

        allocation.updateAllocation(effectiveHours, effectivePercentage, currentUserId);

        java.time.LocalDateTime approvedAt = java.time.LocalDateTime.now();
        if (isOverloaded) {
            allocation.markOverloaded(command.overloadReason(), currentUserId, approvedAt);
        } else {
            allocation.clearOverload();
        }

        // Lưu phân bổ và ghi nhật ký kiểm toán trong CÙNG transaction (QTN-15)
        WeeklyProjectAllocation saved = saveAllocationPort.save(allocation);

        String notifiedPmIds = notifyStakeholders(
                project, employee, currentUserId, "EDIT",
                AllocationNotificationPolicy.YearWeekRange.ofSingle(yearWeek),
                oldValue, newValue
        );

        saveChangeLogPort.save(AllocationChangeLog.create(
                saved.getId(),
                AdjustmentAction.EDIT_HOURS,
                oldValue,
                newValue,
                currentUserId,
                notifiedPmIds
        ));

        String auditAction = employee.isOutsourced() ? "ADJUST_OUTSOURCED_ALLOCATION" : "ALLOCATION_HOURS_EDITED";
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                auditAction,
                "weekly_project_allocations",
                saved.getId(),
                "Phân bổ cũ: " + oldValue,
                "Phân bổ mới: " + newValue
        ));

        return calculateCapacity(employee, yearWeek);
    }

    private WeeklyCapacityResult executeMoveWeek(
            WeeklyProjectAllocation allocation,
            Employee employee,
            Project project,
            AdjustAllocationCommand command,
            Long currentUserId
    ) {
        if (command.targetYear() == null || command.targetWeek() == null) {
            throw new InvalidAllocationAdjustmentException("Tuần đích (targetYear và targetWeek) không được để trống");
        }

        YearWeek sourceWeek = allocation.getYearWeek();
        YearWeek targetWeek = YearWeek.of(command.targetYear(), command.targetWeek());

        AllocationAdjustmentPolicy.validateTargetWeek(sourceWeek, targetWeek, LocalDate.now());

        // [QTN-21 / NCL-14-CN-002] Ràng buộc hạn hợp đồng cho tuần đích
        if (employee.isOutsourced()) {
            if (!employee.isWithinContractPeriod(targetWeek)) {
                String providerInfo = (employee.getProviderName() != null && !employee.getProviderName().isBlank())
                        ? " (đơn vị cung cấp: " + employee.getProviderName() + ")"
                        : "";
                String periodInfo = (employee.getStartDate() != null && employee.getContractEndDate() != null)
                        ? " từ " + employee.getStartDate() + " đến " + employee.getContractEndDate()
                        : "";
                throw new OutsourcedContractPeriodException(
                        "Không thể chuyển phân bổ: Tuần đích " + targetWeek.weekNumber() + "/" + targetWeek.year()
                                + " nằm ngoài thời hạn hợp đồng của nhân sự thuê ngoài " + employee.getFullName() + providerInfo
                                + " (" + periodInfo + ")",
                        employee.getIdValue(),
                        employee.getEmployeeCode(),
                        employee.getProviderName(),
                        employee.getStartDate(),
                        employee.getContractEndDate(),
                        targetWeek
                );
            }
        } else {
            LocalDate weekStartDate = targetWeek.getStartDate();
            if (employee.getContractEndDate() != null && employee.getContractEndDate().isBefore(weekStartDate)) {
                throw new EmployeeInactiveException("Nhân sự đã kết thúc hợp đồng lao động trước tuần đích (" + targetWeek.weekNumber() + "/" + targetWeek.year() + ")");
            }
        }

        if (checkActualHoursPort.hasActualHours(employee.getIdValue(), project.getIdValue(), sourceWeek)
                && AllocationAdjustmentPolicy.isWeekEnded(sourceWeek, LocalDate.now())) {
            throw new InvalidAllocationAdjustmentException(
                    "Không thể chuyển phân bổ từ tuần " + sourceWeek.weekNumber() + "/" + sourceWeek.year()
                            + " vì tuần này đã kết thúc và đã ghi nhận giờ công thực tế"
            );
        }

        Optional<WeeklyProjectAllocation> existingInTarget = loadAllocationPort.loadAllocation(
                employee.getIdValue(), project.getIdValue(), targetWeek);
        if (existingInTarget.isPresent() && !existingInTarget.get().getId().equals(allocation.getId())) {
            throw new InvalidAllocationAdjustmentException(
                    "Nhân sự đã có dòng phân bổ cho dự án này tại tuần đích " + targetWeek.weekNumber() + "/" + targetWeek.year()
            );
        }

        Optional<WeeklyAvailability> targetAvailabilityOpt = loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employee.getIdValue(), targetWeek);
        int standardHours = employee.getStandardHoursPerWeek() != null ? employee.getStandardHoursPerWeek() : 40;
        BigDecimal targetNetAvailable = targetAvailabilityOpt.map(WeeklyAvailability::getNetAvailableHours)
                .orElse(BigDecimal.valueOf(standardHours));

        List<WeeklyProjectAllocation> targetWeekAllocations = loadAllocationPort.loadAllocationsForEmployee(employee.getIdValue(), targetWeek);
        BigDecimal targetOtherProjectsSum = targetWeekAllocations.stream()
                .filter(a -> !a.getId().equals(allocation.getId()))
                .map(WeeklyProjectAllocation::getAllocatedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal targetTotal = targetOtherProjectsSum.add(allocation.getAllocatedHours());
        boolean isOverloaded = WeeklyCapacityMatrixPolicy.isOverloaded(targetTotal, targetNetAvailable);
        BigDecimal excessHours = WeeklyCapacityMatrixPolicy.calculateExcessHours(targetTotal, targetNetAvailable);

        if (isOverloaded) {
            String reason = command.overloadReason();
            if (reason == null || reason.trim().isEmpty()) {
                throw new AllocationOverloadWarningException(
                        "Không thể chuyển phân bổ: Tại tuần đích (" + targetWeek.weekNumber() + "/" + targetWeek.year()
                                + "), tổng số giờ (" + targetTotal + "h) vượt quá giờ khả dụng (" + targetNetAvailable
                                + "h) của nhân sự. Số giờ vượt: " + excessHours + "h. Yêu cầu Quản lý nguồn lực xác nhận lý do.",
                        targetNetAvailable,
                        targetTotal,
                        excessHours
                );
            }

            if (!authorizationService.hasPermission(PermissionCode.RESOURCE_ALLOCATION_OVERLOAD_BYPASS)) {
                deniedAuditLogPort.save(AuditLog.createChange(
                        currentUserId,
                        "ACCESS_DENIED_OVERLOAD_CONFIRM",
                        "weekly_project_allocations",
                        allocation.getId(),
                        null,
                        "user_id=" + currentUserId + ";attempted_overload_hours=" + excessHours
                ));
                throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_OVERLOAD_BYPASS);
            }
        }

        String oldValue = "Tuần " + sourceWeek.weekNumber() + "/" + sourceWeek.year() + " (" + allocation.getAllocatedHours() + "h)";
        String newValue = "Tuần " + targetWeek.weekNumber() + "/" + targetWeek.year() + " (" + allocation.getAllocatedHours() + "h)";

        allocation.moveWeek(targetWeek, currentUserId);
        if (isOverloaded) {
            allocation.markOverloaded(command.overloadReason(), currentUserId, java.time.LocalDateTime.now());
        } else {
            allocation.clearOverload();
        }

        WeeklyProjectAllocation saved = saveAllocationPort.save(allocation);

        String notifiedPmIds = notifyStakeholders(
                project, employee, currentUserId, "MOVE_WEEK",
                AllocationNotificationPolicy.YearWeekRange.ofSingle(targetWeek),
                oldValue, newValue
        );

        saveChangeLogPort.save(AllocationChangeLog.create(
                saved.getId(),
                AdjustmentAction.MOVE_WEEK,
                oldValue,
                newValue,
                currentUserId,
                notifiedPmIds
        ));

        String auditAction = employee.isOutsourced() ? "ADJUST_OUTSOURCED_ALLOCATION" : "ALLOCATION_WEEK_MOVED";
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                auditAction,
                "weekly_project_allocations",
                saved.getId(),
                "Tuần cũ: " + oldValue,
                "Tuần mới: " + newValue
        ));

        return calculateCapacity(employee, targetWeek);
    }

    private void executeRemove(
            WeeklyProjectAllocation allocation,
            Employee employee,
            Project project,
            Long currentUserId
    ) {
        boolean hasActualHours = checkActualHoursPort.hasActualHours(
                employee.getIdValue(),
                project.getIdValue(),
                allocation.getYearWeek()
        );

        AllocationAdjustmentPolicy.validateCanRemove(
                allocation.getId(),
                allocation.getYearWeek(),
                hasActualHours,
                LocalDate.now()
        );

        String oldValue = allocation.getAllocatedHours() + "h tại tuần "
                + allocation.getYearWeek().weekNumber() + "/" + allocation.getYearWeek().year();
        String newValue = "Đã gỡ bỏ (REMOVED)";

        deleteAllocationPort.delete(allocation);

        String notifiedPmIds = notifyStakeholders(
                project, employee, currentUserId, "REMOVE",
                AllocationNotificationPolicy.YearWeekRange.ofSingle(allocation.getYearWeek()),
                oldValue, newValue
        );

        saveChangeLogPort.save(AllocationChangeLog.create(
                allocation.getId(),
                AdjustmentAction.REMOVE,
                oldValue,
                newValue,
                currentUserId,
                notifiedPmIds
        ));

        String auditAction = employee.isOutsourced() ? "ADJUST_OUTSOURCED_ALLOCATION" : "ALLOCATION_REMOVED";
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                auditAction,
                "weekly_project_allocations",
                allocation.getId(),
                "Phân bổ cũ: " + oldValue,
                "Trạng thái mới: " + newValue
        ));
    }

    private WeeklyCapacityResult executeNoteVariance(
            WeeklyProjectAllocation allocation,
            Employee employee,
            Project project,
            String varianceReason,
            Long currentUserId
    ) {
        AllocationAdjustmentPolicy.validateVarianceNote(varianceReason);

        String oldValue = allocation.getVarianceNote() != null ? allocation.getVarianceNote() : "(Trống)";
        String newValue = varianceReason.trim();

        allocation.noteVariance(newValue, currentUserId);
        WeeklyProjectAllocation saved = saveAllocationPort.save(allocation);

        String notifiedPmIds = notifyStakeholders(
                project, employee, currentUserId, "NOTE_VARIANCE",
                AllocationNotificationPolicy.YearWeekRange.ofSingle(allocation.getYearWeek()),
                oldValue, newValue
        );

        saveChangeLogPort.save(AllocationChangeLog.create(
                saved.getId(),
                AdjustmentAction.NOTE_VARIANCE,
                oldValue,
                newValue,
                currentUserId,
                notifiedPmIds
        ));

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "ALLOCATION_VARIANCE_NOTED",
                "weekly_project_allocations",
                saved.getId(),
                "Lý do cũ: " + oldValue,
                "Lý do mới: " + newValue
        ));

        return calculateCapacity(employee, allocation.getYearWeek());
    }

    private String notifyStakeholders(
            Project project,
            Employee employee,
            Long actorUserId,
            String actionType,
            AllocationNotificationPolicy.YearWeekRange weekRange,
            String oldValue,
            String newValue
    ) {
        Long pmId = project.getManagerId() != null ? project.getManagerId().value() : null;
        String actorName = "Người quản lý nguồn lực";
        if (actorUserId != null) {
            actorName = loadUserPort.findById(new UserId(actorUserId))
                    .map(User::getUsername)
                    .orElse("ID:" + actorUserId);
        }
        String title = AllocationNotificationPolicy.formatTitle(project.getProjectName(), actionType);
        String content = AllocationNotificationPolicy.formatContent(
                actorName,
                actionType,
                employee != null ? employee.getFullName() : "Nhân sự",
                project.getProjectName(),
                weekRange,
                oldValue,
                newValue
        );
        notificationPort.notifyAllocationChanged(
                project.getIdValue(),
                employee != null ? employee.getIdValue() : null,
                actorUserId,
                title,
                content
        );
        return pmId != null ? String.valueOf(pmId) : null;
    }

    private void requireOrgUnitInDataScope(User currentUser, Long orgUnitId, Long currentUserId, Long allocationId) {
        if (!isOrgUnitInDataScope(currentUser, orgUnitId)) {
            deniedAuditLogPort.save(AuditLog.createChange(
                    currentUserId,
                    "ACCESS_DENIED_DATA_SCOPE",
                    "weekly_project_allocations",
                    allocationId,
                    null,
                    "user_id=" + currentUserId + ";dataScope=" + currentUser.getDataScope()
                            + ";scopeOrgUnitId=" + currentUser.getScopeOrgUnitId() + ";targetOrgUnitId=" + orgUnitId
            ));
            throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        }
    }

    private boolean isOrgUnitInDataScope(User currentUser, Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> false;
            case ORGANIZATION_BRANCH -> currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, currentUser.getScopeOrgUnitId());
        };
    }

    private User loadCurrentUserOrThrow(Long currentUserId) {
        return loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + currentUserId));
    }

    private WeeklyCapacityResult calculateCapacity(Employee employee, YearWeek yearWeek) {
        Optional<WeeklyAvailability> availabilityOpt = loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employee.getIdValue(), yearWeek);
        List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployee(employee.getIdValue(), yearWeek);

        int standardHours = employee.getStandardHoursPerWeek() != null ? employee.getStandardHoursPerWeek() : 40;
        BigDecimal netAvailable = availabilityOpt.map(WeeklyAvailability::getNetAvailableHours)
                .orElse(BigDecimal.valueOf(standardHours));

        BigDecimal totalAllocated = allocations.stream()
                .map(WeeklyProjectAllocation::getAllocatedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingHours = netAvailable.subtract(totalAllocated);
        boolean isOverAllocated = remainingHours.compareTo(BigDecimal.ZERO) < 0;
        String warningMessage = isOverAllocated
                ? "Cảnh báo: Nhân sự bị phân bổ vượt quá " + remainingHours.abs() + " giờ khả dụng"
                : null;

        return new WeeklyCapacityResult(
                employee.getIdValue(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                yearWeek.year(),
                yearWeek.weekNumber(),
                standardHours,
                netAvailable,
                totalAllocated,
                remainingHours,
                isOverAllocated,
                warningMessage
        );
    }
}
