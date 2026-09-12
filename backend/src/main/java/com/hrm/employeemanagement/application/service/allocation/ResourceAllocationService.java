package com.hrm.employeemanagement.application.service.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.allocation.AllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.AllocateResourceUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyCapacityMatrixPolicy;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationOverloadWarningException;
import com.hrm.employeemanagement.domain.exception.allocation.EmployeeInactiveException;
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

public class ResourceAllocationService implements AllocateResourceUseCase {

    private final AuthorizationService authorizationService;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final SaveWeeklyProjectAllocationPort saveAllocationPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final SaveAuditLogInNewTransactionPort saveAuditLogPort;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;

    public ResourceAllocationService(
            AuthorizationService authorizationService,
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "LoadWeeklyAvailabilityPort must not be null");
        this.saveAllocationPort = Objects.requireNonNull(saveAllocationPort, "SaveWeeklyProjectAllocationPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
    }

    @Override
    public WeeklyCapacityResult allocateResource(AllocateResourceCommand command) {
        // [TC-04] Kiểm tra quyền hạn cơ bản
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + currentUserId));

        YearWeek yearWeek = YearWeek.of(command.year(), command.weekNumber());

        // [TC-02] Load nhân sự (Pessimistic write lock trên employee row để serialize concurrent allocations)
        Employee employee = loadEmployeePort.findByIdForUpdate(new EmployeeId(command.employeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân sự với ID: " + command.employeeId()));

        // Kiểm tra Phạm vi dữ liệu (Data Scope) cho Nhân sự
        requireOrgUnitInDataScope(currentUser, employee.getOrgUnitId(), PermissionCode.RESOURCE_ALLOCATION_MANAGE);

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new EmployeeInactiveException("Không thể phân bổ cho nhân sự không còn ở trạng thái hoạt động");
        }

        LocalDate weekStartDate = yearWeek.getStartDate();
        if (employee.getContractEndDate() != null && employee.getContractEndDate().isBefore(weekStartDate)) {
            throw new EmployeeInactiveException("Nhân sự đã kết thúc hợp đồng lao động trước tuần được chọn (" + yearWeek.weekNumber() + "/" + yearWeek.year() + ")");
        }

        // Load Dự án
        Project project = loadProjectPort.findById(new ProjectId(command.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        // Kiểm tra Phạm vi dữ liệu (Data Scope) cho Dự án
        requireOrgUnitInDataScope(currentUser, project.getOrgUnitId(), PermissionCode.RESOURCE_ALLOCATION_MANAGE);

        // Kiểm tra trạng thái Dự án (chỉ cho phép phân bổ cho dự án ở trạng thái ACTIVE)
        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new ProjectInactiveException("Không thể phân bổ nhân sự vào dự án không ở trạng thái hoạt động");
        }

        // Load khả dụng của nhân sự trong tuần
        Optional<WeeklyAvailability> availabilityOpt = loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(command.employeeId(), yearWeek);
        int standardHours = employee.getStandardHoursPerWeek() != null ? employee.getStandardHoursPerWeek() : 40;
        BigDecimal netAvailableHours = availabilityOpt.map(WeeklyAvailability::getNetAvailableHours)
                .orElse(BigDecimal.valueOf(standardHours));

        // Load tất cả allocations hiện tại của nhân sự trong tuần
        List<WeeklyProjectAllocation> existingAllocations = loadAllocationPort.loadAllocationsForEmployee(command.employeeId(), yearWeek);

        // Tính tổng số giờ phân bổ cho các dự án KHÁC dự án hiện tại
        BigDecimal otherProjectsAllocatedSum = existingAllocations.stream()
                .filter(a -> !a.getProjectId().equals(command.projectId()))
                .map(WeeklyProjectAllocation::getAllocatedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRequestedAllocated = otherProjectsAllocatedSum.add(command.allocatedHours());

        // [QTN-11 / NCL-06-CN-003] Phát hiện quá tải khi phân bổ theo tuần
        boolean isOverloaded = WeeklyCapacityMatrixPolicy.isOverloaded(totalRequestedAllocated, netAvailableHours);
        BigDecimal excessHours = WeeklyCapacityMatrixPolicy.calculateExcessHours(totalRequestedAllocated, netAvailableHours);

        if (isOverloaded) {
            String reason = command.overloadReason();
            if (reason == null || reason.trim().isEmpty()) {
                // TC-01, TC-03: Cảnh báo quá tải và yêu cầu xác nhận kèm lý do
                throw new AllocationOverloadWarningException(
                        "Không thể phân bổ: Tổng số giờ phân bổ (" + totalRequestedAllocated + "h) vượt quá số giờ khả dụng (" + netAvailableHours + "h) của nhân sự trong tuần " + yearWeek.weekNumber() + "/" + yearWeek.year() + ". Số giờ vượt: " + excessHours + "h. Yêu cầu Quản lý nguồn lực xác nhận có ghi rõ lý do.",
                        netAvailableHours,
                        totalRequestedAllocated,
                        excessHours
                );
            }

            // TC-04: Kiểm tra thẩm quyền phê duyệt vượt tải thông qua AuthorizationService (QTN-11)
            if (!authorizationService.hasPermission(PermissionCode.RESOURCE_ALLOCATION_OVERLOAD_BYPASS)) {
                saveAuditLogPort.save(AuditLog.createChange(
                        currentUserId,
                        "ACCESS_DENIED_OVERLOAD_CONFIRM",
                        "weekly_project_allocations",
                        null,
                        null,
                        "user_id=" + currentUserId + ";role=" + (currentUser.getRole() != null ? currentUser.getRole().getCode().getCode() : "UNKNOWN") + ";attempted_overload_hours=" + excessHours
                ));
                throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_OVERLOAD_BYPASS);
            }
        }

        // Capture oldValue từ bản ghi phân bổ hiện tại cho dự án này (nếu có)
        Optional<WeeklyProjectAllocation> existingOpt = existingAllocations.stream()
                .filter(a -> a.getProjectId().equals(command.projectId()))
                .findFirst();

        BigDecimal currentTotalAllocated = existingAllocations.stream()
                .map(WeeklyProjectAllocation::getAllocatedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Capture snapshot trạng thái cũ TRƯỚC KHI thực hiện bất kỳ mutation nào trên entity
        boolean oldIsOverloaded = existingOpt.map(WeeklyProjectAllocation::isOverloaded).orElse(false);
        BigDecimal oldHours = existingOpt.map(WeeklyProjectAllocation::getAllocatedHours).orElse(BigDecimal.ZERO);
        String oldValue = oldHours.toString();
        String newValue = command.allocatedHours().toString();
        String oldOverloadState = "isOverloaded=" + oldIsOverloaded
                + ";projectAllocatedHours=" + oldHours
                + ";totalWeeklyAllocatedHours=" + currentTotalAllocated;

        WeeklyProjectAllocation allocation;
        if (existingOpt.isPresent()) {
            allocation = existingOpt.get();
            allocation.updateAllocatedHours(command.allocatedHours());
        } else {
            allocation = WeeklyProjectAllocation.createNew(
                    command.employeeId(), command.projectId(), yearWeek, command.allocatedHours());
        }

        java.time.LocalDateTime approvedAt = java.time.LocalDateTime.now();
        if (isOverloaded) {
            allocation.markOverloaded(command.overloadReason(), currentUserId, approvedAt);
            // Đồng bộ quyết định phê duyệt overload mới nhất cho tất cả các phân bổ khác của nhân sự trong tuần
            for (WeeklyProjectAllocation otherAlloc : existingAllocations) {
                if (!otherAlloc.getProjectId().equals(command.projectId())) {
                    String otherOldOverloadState = "isOverloaded=" + otherAlloc.isOverloaded()
                            + ";projectAllocatedHours=" + otherAlloc.getAllocatedHours()
                            + ";totalWeeklyAllocatedHours=" + currentTotalAllocated;

                    otherAlloc.markOverloaded(command.overloadReason(), currentUserId, approvedAt);
                    WeeklyProjectAllocation savedOther = saveAllocationPort.save(otherAlloc);

                    String otherNewOverloadState = "isOverloaded=true;overloadReason=" + command.overloadReason().trim()
                            + ";approvedBy=" + currentUserId
                            + ";approvedAt=" + approvedAt
                            + ";projectAllocatedHours=" + otherAlloc.getAllocatedHours()
                            + ";totalWeeklyAllocatedHours=" + totalRequestedAllocated
                            + ";netAvailableHours=" + netAvailableHours
                            + ";overloadHours=" + excessHours;

                    saveAuditLogPort.save(AuditLog.createChange(
                            currentUserId,
                            "ALLOCATION_OVERLOAD_BYPASS",
                            "weekly_project_allocations",
                            savedOther.getId(),
                            otherOldOverloadState,
                            otherNewOverloadState
                    ));
                }
            }
        } else {
            allocation.clearOverload();
            // Khi tổng giờ trong tuần không còn quá tải (totalRequestedAllocated <= netAvailableHours),
            // dọn dẹp cờ isOverloaded trên tất cả các phân bổ khác của nhân sự trong tuần này
            for (WeeklyProjectAllocation otherAlloc : existingAllocations) {
                if (!otherAlloc.getProjectId().equals(command.projectId()) && otherAlloc.isOverloaded()) {
                    String otherOldOverloadState = "isOverloaded=true;projectAllocatedHours=" + otherAlloc.getAllocatedHours()
                            + ";totalWeeklyAllocatedHours=" + currentTotalAllocated;

                    otherAlloc.clearOverload();
                    WeeklyProjectAllocation savedOther = saveAllocationPort.save(otherAlloc);

                    String otherNewOverloadState = "isOverloaded=false;projectAllocatedHours=" + otherAlloc.getAllocatedHours()
                            + ";totalWeeklyAllocatedHours=" + totalRequestedAllocated;

                    saveAuditLogPort.save(AuditLog.createChange(
                            currentUserId,
                            "ALLOCATION_OVERLOAD_CLEARED",
                            "weekly_project_allocations",
                            savedOther.getId(),
                            otherOldOverloadState,
                            otherNewOverloadState
                    ));
                }
            }
        }

        // Lưu bản ghi (Concurrency retry được xử lý tại RetryableAllocateResourceUseCaseDecorator)
        WeeklyProjectAllocation saved = saveAllocationPort.save(allocation);

        // Ghi nhật ký kiểm toán chuẩn (Audit Log)
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "RESOURCE_ALLOCATED",
                "weekly_project_allocations",
                saved.getId(),
                "Số giờ phân bổ cũ: " + oldValue + "h",
                "Số giờ phân bổ mới: " + newValue + "h cho nhân sự ID: " + employee.getIdValue() + ", dự án ID: " + command.projectId()
        ));

        // [TC-05] Ghi nhật ký kiểm toán nghiệp vụ khi có thẩm quyền xác nhận vượt tải hợp lệ (State transition)
        if (isOverloaded) {
            String newOverloadState = "isOverloaded=true;overloadReason=" + command.overloadReason().trim()
                    + ";approvedBy=" + currentUserId
                    + ";approvedAt=" + approvedAt
                    + ";projectAllocatedHours=" + command.allocatedHours()
                    + ";totalWeeklyAllocatedHours=" + totalRequestedAllocated
                    + ";netAvailableHours=" + netAvailableHours
                    + ";overloadHours=" + excessHours;

            saveAuditLogPort.save(AuditLog.createChange(
                    currentUserId,
                    "ALLOCATION_OVERLOAD_BYPASS",
                    "weekly_project_allocations",
                    saved.getId(),
                    oldOverloadState,
                    newOverloadState
            ));
        } else if (oldIsOverloaded) {
            // Khi phân bổ hiện tại chuyển từ quá tải sang hết quá tải
            String newClearState = "isOverloaded=false;projectAllocatedHours=" + command.allocatedHours()
                    + ";totalWeeklyAllocatedHours=" + totalRequestedAllocated;

            saveAuditLogPort.save(AuditLog.createChange(
                    currentUserId,
                    "ALLOCATION_OVERLOAD_CLEARED",
                    "weekly_project_allocations",
                    saved.getId(),
                    oldOverloadState,
                    newClearState
            ));
        }

        return calculateCapacity(employee, yearWeek);
    }

    @Override
    public List<WeeklyCapacityResult> getWeeklyCapacities(List<Long> employeeIds, Integer year, Integer weekNumber) {
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + currentUserId));

        YearWeek yearWeek = YearWeek.of(year, weekNumber);

        // 1. Batch load nhân sự
        List<EmployeeId> empIds = employeeIds.stream().map(EmployeeId::new).toList();
        List<Employee> loadedEmployees = loadEmployeePort.findAllByIdIn(empIds);

        Map<Long, Employee> employeeMap = loadedEmployees.stream()
                .collect(Collectors.toMap(Employee::getIdValue, e -> e));

        for (Long id : employeeIds) {
            if (!employeeMap.containsKey(id)) {
                throw new EmployeeNotFoundException("Không tìm thấy nhân sự với ID: " + id);
            }
        }

        // Validate Data Scope cho tất cả nhân sự được truy vấn
        for (Employee emp : loadedEmployees) {
            requireOrgUnitInDataScope(currentUser, emp.getOrgUnitId(), PermissionCode.RESOURCE_ALLOCATION_READ);
        }

        // 2. Batch load availability (1 SQL query)
        List<WeeklyAvailability> availabilities = loadWeeklyAvailabilityPort.findByEmployeeIdInAndYearWeek(employeeIds, yearWeek);
        Map<Long, WeeklyAvailability> availabilityMap = availabilities.stream()
                .collect(Collectors.toMap(WeeklyAvailability::getEmployeeId, a -> a));

        // 3. Batch load allocations (1 SQL query)
        List<WeeklyProjectAllocation> allAllocations = loadAllocationPort.loadAllocationsForEmployeesInWeekRange(
                employeeIds, yearWeek.year(), yearWeek.weekNumber(), yearWeek.weekNumber());
        Map<Long, List<WeeklyProjectAllocation>> allocationMap = allAllocations.stream()
                .collect(Collectors.groupingBy(WeeklyProjectAllocation::getEmployeeId));

        // 4. Tính toán capacity trên bộ nhớ theo thứ tự employeeIds đầu vào
        return employeeIds.stream().map(empId -> {
            Employee employee = employeeMap.get(empId);
            WeeklyAvailability availability = availabilityMap.get(empId);
            List<WeeklyProjectAllocation> allocations = allocationMap.getOrDefault(empId, List.of());
            return calculateCapacityFromPreloaded(employee, yearWeek, availability, allocations);
        }).toList();
    }

    private void requireOrgUnitInDataScope(User currentUser, Long orgUnitId, PermissionCode permission) {
        if (!isOrgUnitInDataScope(currentUser, orgUnitId)) {
            throw new PermissionDeniedException(permission);
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

    private WeeklyCapacityResult calculateCapacity(Employee employee, YearWeek yearWeek) {
        Optional<WeeklyAvailability> availabilityOpt = loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employee.getIdValue(), yearWeek);
        List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployee(employee.getIdValue(), yearWeek);
        return calculateCapacityFromPreloaded(employee, yearWeek, availabilityOpt.orElse(null), allocations);
    }

    private WeeklyCapacityResult calculateCapacityFromPreloaded(
            Employee employee,
            YearWeek yearWeek,
            WeeklyAvailability availability,
            List<WeeklyProjectAllocation> allocations
    ) {
        int standardHours = employee.getStandardHoursPerWeek() != null ? employee.getStandardHoursPerWeek() : 40;

        BigDecimal netAvailable = availability != null
                ? availability.getNetAvailableHours()
                : BigDecimal.valueOf(standardHours);

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
