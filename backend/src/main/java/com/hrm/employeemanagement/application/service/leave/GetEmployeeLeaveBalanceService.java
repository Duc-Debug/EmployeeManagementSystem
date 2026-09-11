package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveBalanceResult;
import com.hrm.employeemanagement.application.port.inbound.leave.GetEmployeeLeaveBalanceUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveBalancePort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveBalancePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.leave.LeaveBalance;
import com.hrm.employeemanagement.domain.leave.LeaveBalancePolicy;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.leave.LeaveStatus;
import com.hrm.employeemanagement.domain.leave.LeaveType;
import com.hrm.employeemanagement.domain.user.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.user.User;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

public class GetEmployeeLeaveBalanceService implements GetEmployeeLeaveBalanceUseCase {

    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadLeaveBalancePort loadLeaveBalancePort;
    private final SaveLeaveBalancePort saveLeaveBalancePort;
    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final AuthorizationService authorizationService;
    private final SaveAuditLogInNewTransactionPort auditLogRepository;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final LoadHolidaysPort loadHolidaysPort;

    public GetEmployeeLeaveBalanceService(
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadLeaveBalancePort loadLeaveBalancePort,
            SaveLeaveBalancePort saveLeaveBalancePort,
            LoadLeaveRequestPort loadLeaveRequestPort,
            AuthorizationService authorizationService,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            LoadHolidaysPort loadHolidaysPort
    ) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "loadOrgUnitPort must not be null");
        this.loadLeaveBalancePort = Objects.requireNonNull(loadLeaveBalancePort, "loadLeaveBalancePort must not be null");
        this.saveLeaveBalancePort = Objects.requireNonNull(saveLeaveBalancePort, "saveLeaveBalancePort must not be null");
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository must not be null");
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
        this.loadHolidaysPort = loadHolidaysPort;
    }

    @Override
    public LeaveBalanceResult getEmployeeLeaveBalance(Long targetEmployeeId, Integer year) {
        Long currentUserId = authorizationService.require(PermissionCode.LEAVE_BALANCE_READ);

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại với ID: " + currentUserId));

        Employee targetEmployee = loadEmployeePort.findById(new EmployeeId(targetEmployeeId))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân sự"));

        // Áp dụng mô hình DataScope (SELF / ORG_BRANCH / COMPANY)
        requireEmployeeInScope(currentUser, targetEmployee, currentUserId, targetEmployeeId);

        int targetYear = (year != null && year > 2000) ? year : LocalDate.now().getYear();

        CompanyWorkingCalendar calendar = loadWorkingCalendarPort != null
                ? loadWorkingCalendarPort.loadCompanyCalendar()
                : CompanyWorkingCalendar.createDefault();
        Set<LocalDate> holidayDates = (loadHolidaysPort != null)
                ? new java.util.HashSet<>(loadHolidaysPort.getHolidayDatesBetween(LocalDate.of(targetYear, 1, 1), LocalDate.of(targetYear, 12, 31)))
                : Collections.emptySet();

        // Tải thông tin định mức phép năm một cách atomic tránh race condition
        LeaveBalance balance = loadLeaveBalancePort.findOrCreateDefault(targetEmployeeId, targetYear);

        List<LeaveRequest> requests = loadLeaveRequestPort.findByEmployeeIdAndYear(targetEmployeeId, targetYear);

        BigDecimal usedDays = requests.stream()
                .filter(r -> r.getLeaveType() == LeaveType.ANNUAL && r.getStatus() == LeaveStatus.APPROVED)
                .map(r -> BigDecimal.valueOf(LeaveBalancePolicy.calculateWorkingDaysInYear(
                        r.getStartDate(), r.getEndDate(), targetYear, calendar, holidayDates)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingDays = requests.stream()
                .filter(r -> r.getLeaveType() == LeaveType.ANNUAL && r.getStatus() == LeaveStatus.PENDING)
                .map(r -> BigDecimal.valueOf(LeaveBalancePolicy.calculateWorkingDaysInYear(
                        r.getStartDate(), r.getEndDate(), targetYear, calendar, holidayDates)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingDays = LeaveBalancePolicy.calculateRemainingDays(
                balance.getEntitledDays(),
                balance.getCarriedOverDays(),
                usedDays,
                pendingDays
        );

        return new LeaveBalanceResult(
                targetEmployeeId,
                targetYear,
                balance.getEntitledDays(),
                balance.getCarriedOverDays(),
                balance.getTotalAllocatedDays(),
                usedDays,
                pendingDays,
                remainingDays
        );
    }

    private void requireEmployeeInScope(User currentUser, Employee targetEmployee, Long currentUserId, Long targetEmployeeId) {
        boolean allowed = switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> currentUser.getIdValue() != null && currentUser.getIdValue().equals(targetEmployee.getUserIdValue());
            case ORGANIZATION_BRANCH -> targetEmployee.getOrgUnitId() != null
                    && currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(targetEmployee.getOrgUnitId(), currentUser.getScopeOrgUnitId());
        };

        if (!allowed) {
            // Ghi nhật ký lần từ chối truy cập (AC-03)
            auditLogRepository.save(AuditLog.createChange(
                    currentUserId,
                    "ACCESS_DENIED_LEAVE_BALANCE",
                    "employee_leave_balances",
                    targetEmployeeId,
                    null,
                    String.format("Người dùng ID %d cố ý truy cập quỹ ngày phép của nhân viên ID %d nằm ngoài phạm vi dữ liệu (%s)",
                            currentUserId, targetEmployeeId, currentUser.getDataScope())
            ));
            throw new PermissionDeniedException(PermissionCode.LEAVE_BALANCE_READ);
        }
    }
}
