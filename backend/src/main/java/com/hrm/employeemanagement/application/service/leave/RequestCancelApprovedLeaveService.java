package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.port.inbound.leave.RequestCancelApprovedLeaveUseCase;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.leave.LeaveRequestNotFoundException;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.user.UserId;

import java.time.LocalDate;
import java.util.Objects;

/**
 * NCL-05-CN-007: Nhân viên chuyên môn gửi yêu cầu hủy đơn nghỉ phép đã duyệt.
 */
public class RequestCancelApprovedLeaveService implements RequestCancelApprovedLeaveUseCase {

    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final SaveLeaveRequestPort saveLeaveRequestPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveLeaveAuditLogPort saveLeaveAuditLogPort;
    private final AuthorizationService authorizationService;

    public RequestCancelApprovedLeaveService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            LoadEmployeePort loadEmployeePort,
            SaveLeaveAuditLogPort saveLeaveAuditLogPort,
            AuthorizationService authorizationService
    ) {
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.saveLeaveRequestPort = Objects.requireNonNull(saveLeaveRequestPort, "saveLeaveRequestPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.saveLeaveAuditLogPort = Objects.requireNonNull(saveLeaveAuditLogPort, "saveLeaveAuditLogPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
    }

    @Override
    public LeaveRequestResult requestCancelApprovedLeave(Long leaveRequestId, String reason) {
        Long currentUserId = authorizationService.require(PermissionCode.LEAVE_REQUEST_CREATE);

        Employee currentEmployee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Tài khoản chưa được liên kết với hồ sơ nhân sự"));

        LeaveRequest leaveRequest = loadLeaveRequestPort.findByIdForUpdate(leaveRequestId)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Không tìm thấy đơn xin nghỉ phép với mã: " + leaveRequestId));

        // Chỉ được gửi yêu cầu hủy đơn của chính mình
        if (!currentEmployee.getIdValue().equals(leaveRequest.getEmployeeId())) {
            throw new PermissionDeniedException(PermissionCode.LEAVE_REQUEST_CREATE);
        }

        // Domain method: kiểm tra status == APPROVED và startDate > today
        leaveRequest.requestCancellation(reason, LocalDate.now());
        LeaveRequest savedRequest = saveLeaveRequestPort.save(leaveRequest);

        // Ghi Audit Log
        String auditDesc = String.format("Gửi yêu cầu hủy đơn nghỉ phép #%d (%s đến %s). Lý do: %s",
                savedRequest.getId(),
                savedRequest.getStartDate(),
                savedRequest.getEndDate(),
                reason);
        saveLeaveAuditLogPort.recordAudit(currentUserId, "REQUEST_CANCEL_LEAVE_REQUEST", auditDesc);

        return LeaveRequestResult.fromDomain(savedRequest);
    }
}
