package com.hrm.employeemanagement.application.service.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;

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

        // [TC-02] Load nhân sự
        Employee employee = loadEmployeePort.findById(new EmployeeId(command.employeeId()))
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

        // Tìm bản ghi phân bổ hiện tại hoặc tạo mới
        Optional<WeeklyProjectAllocation> existingOpt = loadAllocationPort.loadAllocation(
                command.employeeId(), command.projectId(), yearWeek);

        WeeklyProjectAllocation allocation;
        if (existingOpt.isPresent()) {
            allocation = existingOpt.get();
            allocation.updateAllocatedHours(command.allocatedHours());
        } else {
            allocation = WeeklyProjectAllocation.createNew(
                    command.employeeId(), command.projectId(), yearWeek, command.allocatedHours());
        }

        // Lưu bản ghi với Race Condition handling
        WeeklyProjectAllocation saved;
        try {
            saved = saveAllocationPort.save(allocation);
        } catch (DataIntegrityViolationException ex) {
            WeeklyProjectAllocation retryAllocation = loadAllocationPort.loadAllocation(command.employeeId(), command.projectId(), yearWeek)
                    .orElseThrow(() -> ex);
            retryAllocation.updateAllocatedHours(command.allocatedHours());
            saved = saveAllocationPort.save(retryAllocation);
        }

        // [TC-05] Ghi nhật ký kiểm toán (Audit Log)
        String oldValue = existingOpt.map(a -> a.getAllocatedHours().toString()).orElse("0");
        String newValue = command.allocatedHours().toString();

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

        return employeeIds.stream().map(empId -> {
            Employee employee = loadEmployeePort.findById(new EmployeeId(empId))
                    .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân sự với ID: " + empId));
            requireOrgUnitInDataScope(currentUser, employee.getOrgUnitId(), PermissionCode.RESOURCE_ALLOCATION_READ);
            return calculateCapacity(employee, yearWeek);
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

        int standardHours = employee.getStandardHoursPerWeek() != null ? employee.getStandardHoursPerWeek() : 40;

        BigDecimal netAvailable = availabilityOpt.map(WeeklyAvailability::getNetAvailableHours)
                .orElse(BigDecimal.valueOf(standardHours));

        List<WeeklyProjectAllocation> allAllocations = loadAllocationPort.loadAllocationsForEmployee(employee.getIdValue(), yearWeek);

        BigDecimal totalAllocated = allAllocations.stream()
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
