package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.port.inbound.leave.CancelLeaveRequestUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.user.UserId;

import java.util.NoSuchElementException;
import java.util.Objects;

public class CancelLeaveRequestService implements CancelLeaveRequestUseCase {

    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final SaveLeaveRequestPort saveLeaveRequestPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveAuditLogInNewTransactionPort auditLogRepository;
    private final AuthorizationService authorizationService;

    public CancelLeaveRequestService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            LoadEmployeePort loadEmployeePort,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            AuthorizationService authorizationService
    ) {
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.saveLeaveRequestPort = Objects.requireNonNull(saveLeaveRequestPort, "saveLeaveRequestPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
    }

    @Override
    public void cancelLeaveRequest(Long leaveRequestId) {
        Long currentUserId = authorizationService.require(PermissionCode.LEAVE_REQUEST_CREATE);

        Employee currentEmployee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Tài khoản chưa được liên kết với hồ sơ nhân sự"));

        LeaveRequest leaveRequest = loadLeaveRequestPort.findByIdForUpdate(leaveRequestId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn nghỉ phép với mã ID: " + leaveRequestId));

        // Chỉ được hủy đơn của chính mình
        if (!currentEmployee.getIdValue().equals(leaveRequest.getEmployeeId())) {
            throw new PermissionDeniedException(PermissionCode.LEAVE_REQUEST_CREATE);
        }

        // Hủy đơn (Domain method sẽ throw IllegalStateException nếu status != PENDING)
        leaveRequest.cancel();
        saveLeaveRequestPort.save(leaveRequest);

        // Ghi Audit Log
        auditLogRepository.save(AuditLog.createChange(
                currentUserId,
                "CANCEL_LEAVE_REQUEST",
                "leave_requests",
                leaveRequest.getId(),
                "PENDING",
                "CANCELLED"
        ));
    }
}
