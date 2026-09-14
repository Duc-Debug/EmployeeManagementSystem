package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.port.inbound.leave.ApproveLeaveRequestUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.availability.SaveWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyCapacityMatrixPolicy;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.Holiday;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailabilityPolicy;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.leave.LeaveRequestNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * NCL-05-CN-003: Xử lý phê duyệt đơn xin nghỉ phép (TC-01, TC-03, TC-04, QTN-10).
 */
public class ApproveLeaveRequestService implements ApproveLeaveRequestUseCase {

    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final SaveLeaveRequestPort saveLeaveRequestPort;
    private final SaveLeaveAuditLogPort saveLeaveAuditLogPort;
    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final LoadWeeklyProjectAllocationPort loadWeeklyProjectAllocationPort;
    private final SaveWeeklyProjectAllocationPort saveWeeklyProjectAllocationPort;

    public ApproveLeaveRequestService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveLeaveAuditLogPort saveLeaveAuditLogPort,
            AuthorizationService authorizationService
    ) {
        this(loadLeaveRequestPort, saveLeaveRequestPort, saveLeaveAuditLogPort, authorizationService,
                null, null, null, null, null, null, null, null, null, null);
    }

    public ApproveLeaveRequestService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveLeaveAuditLogPort saveLeaveAuditLogPort,
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadEmployeePort loadEmployeePort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort
    ) {
        this(loadLeaveRequestPort, saveLeaveRequestPort, saveLeaveAuditLogPort, authorizationService,
                loadUserPort, loadOrgUnitPort, loadEmployeePort, loadWeeklyAvailabilityPort, saveWeeklyAvailabilityPort,
                loadHolidaysPort, loadApprovedLeavesPort, loadWorkingCalendarPort, null, null);
    }

    public ApproveLeaveRequestService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveLeaveAuditLogPort saveLeaveAuditLogPort,
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadEmployeePort loadEmployeePort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            LoadWeeklyProjectAllocationPort loadWeeklyProjectAllocationPort,
            SaveWeeklyProjectAllocationPort saveWeeklyProjectAllocationPort
    ) {
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.saveLeaveRequestPort = Objects.requireNonNull(saveLeaveRequestPort, "saveLeaveRequestPort must not be null");
        this.saveLeaveAuditLogPort = Objects.requireNonNull(saveLeaveAuditLogPort, "saveLeaveAuditLogPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
        this.loadUserPort = loadUserPort;
        this.loadOrgUnitPort = loadOrgUnitPort;
        this.loadEmployeePort = loadEmployeePort;
        this.loadWeeklyAvailabilityPort = loadWeeklyAvailabilityPort;
        this.saveWeeklyAvailabilityPort = saveWeeklyAvailabilityPort;
        this.loadHolidaysPort = loadHolidaysPort;
        this.loadApprovedLeavesPort = loadApprovedLeavesPort;
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
        this.loadWeeklyProjectAllocationPort = loadWeeklyProjectAllocationPort;
        this.saveWeeklyProjectAllocationPort = saveWeeklyProjectAllocationPort;
    }

    @Override
    public LeaveRequestResult approveLeaveRequest(Long leaveRequestId, String approverComment) {
        // 1. TC-03: Kiểm tra quyền phê duyệt đơn nghỉ phép
        Long currentUserId = authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE);

        // 2. Tìm đơn xin nghỉ kèm khóa pessimistic lock (FOR UPDATE) để đảm bảo atomic state transition
        LeaveRequest leaveRequest = loadLeaveRequestPort.findByIdForUpdate(leaveRequestId)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Không tìm thấy đơn xin nghỉ phép với mã: " + leaveRequestId));

        // 3. Kiểm tra Data Scope & chống IDOR
        Employee employee = null;
        if (loadEmployeePort != null) {
            Optional<Employee> employeeOpt = loadEmployeePort.findById(new EmployeeId(leaveRequest.getEmployeeId()));
            if (employeeOpt.isPresent()) {
                employee = employeeOpt.get();
                if (loadUserPort != null) {
                    User currentUser = loadUserPort.findById(new UserId(currentUserId))
                            .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));
                    requireEmployeeInScope(currentUser, employee, PermissionCode.LEAVE_REQUEST_APPROVE);
                }
            }
        }

        // 4. Thực thi nghiệp vụ domain: chuyển trạng thái sang APPROVED, lưu người duyệt
        leaveRequest.approve(currentUserId, approverComment);

        // 5. Lưu lại vào cơ sở dữ liệu
        LeaveRequest savedRequest = saveLeaveRequestPort.save(leaveRequest);

        // 6. TC-04: Ghi nhật ký kiểm toán (Audit Log)
        String auditDesc = String.format("Phê duyệt đơn nghỉ phép #%d của nhân viên #%d (%s đến %s). Ghi chú: %s",
                savedRequest.getId(),
                savedRequest.getEmployeeId(),
                savedRequest.getStartDate(),
                savedRequest.getEndDate(),
                approverComment != null ? approverComment : "Không có");
        saveLeaveAuditLogPort.recordAudit(currentUserId, "APPROVE_LEAVE_REQUEST", auditDesc);

        // 7. QTN-10: Cập nhật và trừ giờ khả dụng tuần (WeeklyAvailability) cho tất cả tuần bị ảnh hưởng,
        // đồng thời cập nhật trạng thái quá tải (isOverloaded) cho các phân bổ dự án trong tuần
        if (employee != null) {
            recalculateWeeklyAvailability(savedRequest, employee, currentUserId);
        }

        return LeaveRequestResult.fromDomain(savedRequest);
    }

    private void recalculateWeeklyAvailability(LeaveRequest leaveRequest, Employee employee, Long currentUserId) {
        if (loadWeeklyAvailabilityPort == null || saveWeeklyAvailabilityPort == null) {
            return;
        }

        Set<DayOfWeek> workingDays = (loadWorkingCalendarPort != null)
                ? loadWorkingCalendarPort.loadCompanyCalendar().getWorkingDays()
                : Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

        int standardHoursPerWeek = (employee.getStandardHoursPerWeek() != null)
                ? employee.getStandardHoursPerWeek()
                : 40;

        Set<YearWeek> affectedWeeks = new LinkedHashSet<>();
        LocalDate curr = leaveRequest.getStartDate();
        while (!curr.isAfter(leaveRequest.getEndDate())) {
            affectedWeeks.add(YearWeek.from(curr));
            curr = curr.plusDays(1);
        }

        for (YearWeek yw : affectedWeeks) {
            int holidayHours = 0;
            if (loadHolidaysPort != null) {
                List<Holiday> holidays = loadHolidaysPort.getHolidaysBetween(yw.getStartDate(), yw.getEndDate());
                holidayHours = WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yw, holidays, workingDays);
            }

            BigDecimal approvedLeaveHours = BigDecimal.ZERO;
            if (loadApprovedLeavesPort != null) {
                approvedLeaveHours = loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(
                        employee.getIdValue(), yw.getStartDate(), yw.getEndDate());
            }

            Optional<WeeklyAvailability> existing = loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(
                    employee.getIdValue(), yw);

            WeeklyAvailability availability;
            if (existing.isPresent()) {
                availability = existing.get();
                availability.update(availability.getStandardHours(), holidayHours, approvedLeaveHours);
            } else {
                availability = WeeklyAvailability.createCalculated(
                        employee.getIdValue(), yw, standardHoursPerWeek, holidayHours, approvedLeaveHours);
            }

            saveWeeklyAvailabilityPort.save(availability);

            recalculateAllocationOverloadForWeek(employee.getIdValue(), yw, availability.getNetAvailableHours(), currentUserId);
        }
    }

    private void recalculateAllocationOverloadForWeek(Long employeeId, YearWeek yearWeek, BigDecimal netAvailableHours, Long currentUserId) {
        if (loadWeeklyProjectAllocationPort == null || saveWeeklyProjectAllocationPort == null) {
            return;
        }

        List<WeeklyProjectAllocation> allocations = loadWeeklyProjectAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek);
        if (allocations == null || allocations.isEmpty()) {
            return;
        }

        BigDecimal totalAllocated = allocations.stream()
                .map(WeeklyProjectAllocation::getAllocatedHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean isOverloaded = WeeklyCapacityMatrixPolicy.isOverloaded(totalAllocated, netAvailableHours);

        for (WeeklyProjectAllocation alloc : allocations) {
            boolean statusChanged = false;
            if (isOverloaded && !alloc.isOverloaded()) {
                alloc.markOverloaded(
                        "Phát sinh quá tải do đơn nghỉ phép được phê duyệt trong tuần",
                        currentUserId,
                        LocalDateTime.now()
                );
                statusChanged = true;
            } else if (!isOverloaded && alloc.isOverloaded()) {
                alloc.clearOverload();
                statusChanged = true;
            }

            if (statusChanged) {
                saveWeeklyProjectAllocationPort.save(alloc);
            }
        }
    }

    private void requireEmployeeInScope(User currentUser, Employee employee, PermissionCode permission) {
        if (!isEmployeeInScope(currentUser, employee)) {
            throw new PermissionDeniedException(permission);
        }
    }

    private boolean isEmployeeInScope(User currentUser, Employee employee) {
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> currentUser.getIdValue() != null && currentUser.getIdValue().equals(employee.getUserIdValue());
            case ORGANIZATION_BRANCH -> employee.getOrgUnitId() != null
                    && currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(
                            employee.getOrgUnitId(), currentUser.getScopeOrgUnitId());
        };
    }
}
