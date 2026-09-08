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
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
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
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.project.ProjectId;

public class ResourceAllocationService implements AllocateResourceUseCase {

    private final AuthorizationService authorizationService;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final SaveWeeklyProjectAllocationPort saveAllocationPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final SaveAuditLogInNewTransactionPort saveAuditLogPort;

    public ResourceAllocationService(
            AuthorizationService authorizationService,
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "LoadWeeklyAvailabilityPort must not be null");
        this.saveAllocationPort = Objects.requireNonNull(saveAllocationPort, "SaveWeeklyProjectAllocationPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
    }

    @Override
    public WeeklyCapacityResult allocateResource(AllocateResourceCommand command) {
        // [TC-04] Kiểm tra quyền hạn của Quản lý nguồn lực (RESOURCE_ALLOCATION_MANAGE)
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE);

        YearWeek yearWeek = YearWeek.of(command.year(), command.weekNumber());

        // [TC-02] Load nhân sự thông qua EmployeeId
        Employee employee = loadEmployeePort.findById(new EmployeeId(command.employeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân sự với ID: " + command.employeeId()));

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new EmployeeInactiveException("Không thể phân bổ cho nhân sự không còn ở trạng thái hoạt động");
        }

        LocalDate weekStartDate = yearWeek.getStartDate();
        if (employee.getContractEndDate() != null && employee.getContractEndDate().isBefore(weekStartDate)) {
            throw new EmployeeInactiveException("Nhân sự đã kết thúc hợp đồng lao động trước tuần được chọn (" + yearWeek.weekNumber() + "/" + yearWeek.year() + ")");
        }

        // 🔴 FIX: Kiểm tra Dự án có tồn tại hay không
        loadProjectPort.findById(new ProjectId(command.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        // Tìm bản ghi phân bổ hiện tại hoặc tạo mới
        Optional<WeeklyProjectAllocation> existingOpt = loadAllocationPort.loadAllocation(
                command.employeeId(), command.projectId(), yearWeek);

        WeeklyProjectAllocation allocation;
        if (existingOpt.isPresent()) {
            allocation = existingOpt.get();
            // [TC-03] Kiểm tra số giờ âm tự động trong domain model
            allocation.updateAllocatedHours(command.allocatedHours());
        } else {
            // [TC-03] Kiểm tra số giờ âm tự động trong domain model
            allocation = WeeklyProjectAllocation.createNew(
                    command.employeeId(), command.projectId(), yearWeek, command.allocatedHours());
        }

        // 🟠 FIX Major: Xử lý Race Condition với Unique Constraint (employee_id, project_id, year, week)
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

        // [TC-01] Tính toán công suất và số giờ còn rảnh
        return calculateCapacity(employee, yearWeek);
    }

    @Override
    public List<WeeklyCapacityResult> getWeeklyCapacities(List<Long> employeeIds, Integer year, Integer weekNumber) {
        authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ);
        YearWeek yearWeek = YearWeek.of(year, weekNumber);

        return employeeIds.stream().map(empId -> {
            Employee employee = loadEmployeePort.findById(new EmployeeId(empId)).orElse(null);
            if (employee == null) {
                return null;
            }
            return calculateCapacity(employee, yearWeek);
        }).filter(Objects::nonNull).toList();
    }

    private WeeklyCapacityResult calculateCapacity(Employee employee, YearWeek yearWeek) {
        Optional<WeeklyAvailability> availabilityOpt = loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employee.getIdValue(), yearWeek);

        // 🟠 FIX Major: Fallback standardHours chuẩn hóa 40h và không bị null trong Response DTO
        int standardHours = employee.getStandardHoursPerWeek() != null ? employee.getStandardHoursPerWeek() : 40;

        BigDecimal netAvailable = availabilityOpt.map(WeeklyAvailability::getNetAvailableHours)
                .orElse(BigDecimal.valueOf(standardHours));

        // Tính tổng số giờ đã phân bổ cho tất cả dự án trong tuần đó
        List<WeeklyProjectAllocation> allAllocations = loadAllocationPort.loadAllocationsForEmployee(employee.getIdValue(), yearWeek);

        BigDecimal totalAllocated = allAllocations.stream()
                .map(WeeklyProjectAllocation::getAllocatedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // [TC-01] Số giờ rảnh còn lại = Net Available Hours - Total Allocated Hours
        BigDecimal remainingHours = netAvailable.subtract(totalAllocated);

        // 🟠 FIX Major: Đánh dấu Over-allocation và trả warning message rõ ràng
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
