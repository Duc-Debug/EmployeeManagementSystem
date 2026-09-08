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
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationCapacityExceededException;
import com.hrm.employeemanagement.domain.exception.allocation.EmployeeInactiveException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
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

        // Enforce capacity limit: Không cho phép vượt netAvailableHours
        if (totalRequestedAllocated.compareTo(netAvailableHours) > 0) {
            throw new AllocationCapacityExceededException(
                    "Không thể phân bổ: Tổng số giờ phân bổ (" + totalRequestedAllocated + "h) vượt quá số giờ khả dụng (" + netAvailableHours + "h) của nhân sự trong tuần " + yearWeek.weekNumber() + "/" + yearWeek.year()
            );
        }

        // Capture oldValue từ bản ghi phân bổ hiện tại cho dự án này (nếu có)
        Optional<WeeklyProjectAllocation> existingOpt = existingAllocations.stream()
                .filter(a -> a.getProjectId().equals(command.projectId()))
                .findFirst();

        BigDecimal oldHours = existingOpt.map(WeeklyProjectAllocation::getAllocatedHours).orElse(BigDecimal.ZERO);
        String oldValue = oldHours.toString();
        String newValue = command.allocatedHours().toString();

        WeeklyProjectAllocation allocation;
        if (existingOpt.isPresent()) {
            allocation = existingOpt.get();
            allocation.updateAllocatedHours(command.allocatedHours());
        } else {
            allocation = WeeklyProjectAllocation.createNew(
                    command.employeeId(), command.projectId(), yearWeek, command.allocatedHours());
        }

        // Lưu bản ghi (Concurrency retry được xử lý tại RetryableAllocateResourceUseCaseDecorator)
        WeeklyProjectAllocation saved = saveAllocationPort.save(allocation);

        // [TC-05] Ghi nhật ký kiểm toán (Audit Log)
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "RESOURCE_ALLOCATED",
                "weekly_project_allocations",
                saved.getId(),
                "Số giờ phân bổ cũ: " + oldValue + "h",
                "Số giờ phân bổ mới: " + newValue + "h cho nhân sự ID: " + employee.getIdValue() + ", dự án ID: " + command.projectId()
        ));

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
