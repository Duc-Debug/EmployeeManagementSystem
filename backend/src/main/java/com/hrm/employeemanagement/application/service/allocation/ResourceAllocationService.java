package com.hrm.employeemanagement.application.service.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.hrm.employeemanagement.application.dto.allocation.AllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.AllocateResourceUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
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

public class ResourceAllocationService implements AllocateResourceUseCase {

    private final AuthorizationService authorizationService;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final SaveWeeklyProjectAllocationPort saveAllocationPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final SaveAuditLogInNewTransactionPort saveAuditLogPort;

    public ResourceAllocationService(
            AuthorizationService authorizationService,
            LoadEmployeePort loadEmployeePort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
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

        // Lưu phân bổ vào DB
        WeeklyProjectAllocation saved = saveAllocationPort.save(allocation);

        // [TC-05] Ghi nhật ký kiểm toán (Audit Log) - ĐÃ SỬA GỌI ĐÚNG HÀM .save() VÀ AuditLog.createChange()
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

        BigDecimal netAvailable = availabilityOpt.map(WeeklyAvailability::getNetAvailableHours)
                .orElse(BigDecimal.valueOf(employee.getStandardHoursPerWeek() != null ? employee.getStandardHoursPerWeek() : 40));

        // Tính tổng số giờ đã phân bổ cho tất cả dự án trong tuần đó
        List<WeeklyProjectAllocation> allAllocations = loadAllocationPort.loadAllocationsForEmployee(employee.getIdValue(), yearWeek);

        BigDecimal totalAllocated = allAllocations.stream()
                .map(WeeklyProjectAllocation::getAllocatedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // [TC-01] Số giờ rảnh còn lại = Net Available Hours - Total Allocated Hours
        BigDecimal remainingHours = netAvailable.subtract(totalAllocated);

        return new WeeklyCapacityResult(
                employee.getIdValue(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                yearWeek.year(),
                yearWeek.weekNumber(),
                employee.getStandardHoursPerWeek(),
                netAvailable,
                totalAllocated,
                remainingHours
        );
    }
}
