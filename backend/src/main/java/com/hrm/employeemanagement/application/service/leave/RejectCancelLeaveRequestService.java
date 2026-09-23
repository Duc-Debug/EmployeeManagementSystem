package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.port.inbound.leave.RejectCancelLeaveRequestUseCase;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.leave.LeaveRequestNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.util.Objects;

/**
 * NCL-05-CN-007: Từ chối yêu cầu hủy đơn nghỉ phép đã duyệt.
 * Đơn nghỉ phép quay trở lại trạng thái APPROVED.
 */
public class RejectCancelLeaveRequestService implements RejectCancelLeaveRequestUseCase {

    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final SaveLeaveRequestPort saveLeaveRequestPort;
    private final SaveLeaveAuditLogPort saveLeaveAuditLogPort;
    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadEmployeePort loadEmployeePort;

    public RejectCancelLeaveRequestService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveLeaveAuditLogPort saveLeaveAuditLogPort,
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadEmployeePort loadEmployeePort
    ) {
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.saveLeaveRequestPort = Objects.requireNonNull(saveLeaveRequestPort, "saveLeaveRequestPort must not be null");
        this.saveLeaveAuditLogPort = Objects.requireNonNull(saveLeaveAuditLogPort, "saveLeaveAuditLogPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "loadOrgUnitPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
    }

    @Override
    public LeaveRequestResult rejectCancelLeaveRequest(Long leaveRequestId, String rejectionReason) {
        Long currentUserId = authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE);

        LeaveRequest leaveRequest = loadLeaveRequestPort.findByIdForUpdate(leaveRequestId)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Không tìm thấy đơn xin nghỉ phép với mã: " + leaveRequestId));

        Employee employee = loadEmployeePort.findById(new EmployeeId(leaveRequest.getEmployeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân sự của đơn nghỉ phép"));
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));
        requireEmployeeInScope(currentUser, employee, PermissionCode.LEAVE_REQUEST_APPROVE);

        // Thực thi domain: chuyển lại về APPROVED kèm lý do từ chối hủy
        leaveRequest.rejectCancellation(currentUserId, rejectionReason);
        LeaveRequest savedRequest = saveLeaveRequestPort.save(leaveRequest);

        // Ghi Audit Log
        String auditDesc = String.format("Từ chối yêu cầu hủy đơn nghỉ phép #%d của nhân viên #%d. Lý do từ chối: %s",
                savedRequest.getId(),
                savedRequest.getEmployeeId(),
                rejectionReason);
        saveLeaveAuditLogPort.recordAudit(currentUserId, "REJECT_CANCEL_LEAVE_REQUEST", auditDesc);

        return LeaveRequestResult.fromDomain(savedRequest);
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
                    && loadOrgUnitPort.existsInOrgUnitBranch(
                            employee.getOrgUnitId(), currentUser.getScopeOrgUnitId());
        };
    }
}
