package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveBalanceResult;
import com.hrm.employeemanagement.application.port.inbound.leave.GetMyLeaveBalanceUseCase;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveBalancePort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveBalancePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
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

public class GetMyLeaveBalanceService implements GetMyLeaveBalanceUseCase {

    private final LoadEmployeePort loadEmployeePort;
    private final LoadLeaveBalancePort loadLeaveBalancePort;
    private final SaveLeaveBalancePort saveLeaveBalancePort;
    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final AuthorizationService authorizationService;

    public GetMyLeaveBalanceService(
            LoadEmployeePort loadEmployeePort,
            LoadLeaveBalancePort loadLeaveBalancePort,
            SaveLeaveBalancePort saveLeaveBalancePort,
            LoadLeaveRequestPort loadLeaveRequestPort,
            AuthorizationService authorizationService
    ) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadLeaveBalancePort = Objects.requireNonNull(loadLeaveBalancePort, "loadLeaveBalancePort must not be null");
        this.saveLeaveBalancePort = Objects.requireNonNull(saveLeaveBalancePort, "saveLeaveBalancePort must not be null");
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
    }

    @Override
    public LeaveBalanceResult getMyLeaveBalance(Integer year) {
        Long currentUserId = authorizationService.require(PermissionCode.LEAVE_BALANCE_READ);

        Employee currentEmployee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Tài khoản chưa được liên kết với hồ sơ nhân sự"));

        int targetYear = (year != null && year > 2000) ? year : LocalDate.now().getYear();
        Long employeeId = currentEmployee.getIdValue();

        // 1. Tải thông tin định mức phép năm (nếu chưa có thì tự khởi tạo mặc định 12 ngày một cách atomic)
        LeaveBalance balance = loadLeaveBalancePort.findOrCreateDefault(employeeId, targetYear);

        // 2. Tải danh sách đơn nghỉ trong năm của nhân viên để tổng hợp ngày nghỉ phép năm (ANNUAL)
        List<LeaveRequest> requests = loadLeaveRequestPort.findByEmployeeIdAndYear(employeeId, targetYear);

        BigDecimal usedDays = requests.stream()
                .filter(r -> r.getLeaveType() == LeaveType.ANNUAL && r.getStatus() == LeaveStatus.APPROVED)
                .map(r -> BigDecimal.valueOf(LeaveBalancePolicy.calculateWorkingDaysInYear(r.getStartDate(), r.getEndDate(), targetYear)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingDays = requests.stream()
                .filter(r -> r.getLeaveType() == LeaveType.ANNUAL && r.getStatus() == LeaveStatus.PENDING)
                .map(r -> BigDecimal.valueOf(LeaveBalancePolicy.calculateWorkingDaysInYear(r.getStartDate(), r.getEndDate(), targetYear)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Tính số ngày phép còn lại theo Policy
        BigDecimal remainingDays = LeaveBalancePolicy.calculateRemainingDays(
                balance.getEntitledDays(),
                balance.getCarriedOverDays(),
                usedDays,
                pendingDays
        );

        return new LeaveBalanceResult(
                employeeId,
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
