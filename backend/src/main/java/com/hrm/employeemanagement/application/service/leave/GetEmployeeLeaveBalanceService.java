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

public class GetEmployeeLeaveBalanceService implements GetEmployeeLeaveBalanceUseCase {

    private final LoadEmployeePort loadEmployeePort;
    private final LoadLeaveBalancePort loadLeaveBalancePort;
    private final SaveLeaveBalancePort saveLeaveBalancePort;
    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final AuthorizationService authorizationService;
    private final SaveAuditLogInNewTransactionPort auditLogRepository;

    public GetEmployeeLeaveBalanceService(
            LoadEmployeePort loadEmployeePort,
            LoadLeaveBalancePort loadLeaveBalancePort,
            SaveLeaveBalancePort saveLeaveBalancePort,
            LoadLeaveRequestPort loadLeaveRequestPort,
            AuthorizationService authorizationService,
            SaveAuditLogInNewTransactionPort auditLogRepository
    ) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadLeaveBalancePort = Objects.requireNonNull(loadLeaveBalancePort, "loadLeaveBalancePort must not be null");
        this.saveLeaveBalancePort = Objects.requireNonNull(saveLeaveBalancePort, "saveLeaveBalancePort must not be null");
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository must not be null");
    }

    @Override
    public LeaveBalanceResult getEmployeeLeaveBalance(Long targetEmployeeId, Integer year) {
        Long currentUserId = authorizationService.require(PermissionCode.LEAVE_BALANCE_READ);

        Employee currentEmployee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Tài khoản chưa được liên kết với hồ sơ nhân sự"));

        // TC-03: Kiểm tra quyền truy cập. Nếu nhân viên xem trộm người khác => Từ chối & ghi Audit Log
        boolean isSelf = currentEmployee.getIdValue().equals(targetEmployeeId);
        if (!isSelf && !authorizationService.hasPermission(PermissionCode.LEAVE_BALANCE_MANAGE)) {
            // Ghi nhật ký lần từ chối truy cập (AC-03)
            auditLogRepository.save(AuditLog.createChange(
                    currentUserId,
                    "ACCESS_DENIED_LEAVE_BALANCE",
                    "employee_leave_balances",
                    targetEmployeeId,
                    null,
                    String.format("Người dùng ID %d cố ý truy cập quỹ ngày phép của nhân viên ID %d mà không có quyền",
                            currentUserId, targetEmployeeId)
            ));
            throw new PermissionDeniedException(PermissionCode.LEAVE_BALANCE_READ);
        }

        Employee targetEmployee = loadEmployeePort.findById(new EmployeeId(targetEmployeeId))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân sự"));

        int targetYear = (year != null && year > 2000) ? year : LocalDate.now().getYear();

        LeaveBalance balance = loadLeaveBalancePort.findByEmployeeIdAndYear(targetEmployeeId, targetYear)
                .orElseGet(() -> saveLeaveBalancePort.save(LeaveBalance.createDefault(targetEmployeeId, targetYear)));

        List<LeaveRequest> requests = loadLeaveRequestPort.findByEmployeeIdAndYear(targetEmployeeId, targetYear);

        BigDecimal usedDays = requests.stream()
                .filter(r -> r.getLeaveType() == LeaveType.ANNUAL && r.getStatus() == LeaveStatus.APPROVED)
                .map(r -> BigDecimal.valueOf(r.getDaysCount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingDays = requests.stream()
                .filter(r -> r.getLeaveType() == LeaveType.ANNUAL && r.getStatus() == LeaveStatus.PENDING)
                .map(r -> BigDecimal.valueOf(r.getDaysCount()))
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
}
