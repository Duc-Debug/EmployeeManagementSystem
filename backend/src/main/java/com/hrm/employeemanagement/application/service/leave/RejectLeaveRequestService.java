package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.port.inbound.leave.RejectLeaveRequestUseCase;
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
import java.util.Optional;

/**
 * NCL-05-CN-003: Xử lý từ chối đơn xin nghỉ phép kèm lý do (TC-03, TC-04).
 */
public class RejectLeaveRequestService implements RejectLeaveRequestUseCase {

    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final SaveLeaveRequestPort saveLeaveRequestPort;
    private final SaveLeaveAuditLogPort saveLeaveAuditLogPort;
    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadEmployeePort loadEmployeePort;

    public RejectLeaveRequestService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveLeaveAuditLogPort saveLeaveAuditLogPort,
            AuthorizationService authorizationService
    ) {
        this(loadLeaveRequestPort, saveLeaveRequestPort, saveLeaveAuditLogPort, authorizationService, null, null, null);
    }

    public RejectLeaveRequestService(
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
        this.loadUserPort = loadUserPort;
        this.loadOrgUnitPort = loadOrgUnitPort;
        this.loadEmployeePort = loadEmployeePort;
    }

    @Override
    public LeaveRequestResult rejectLeaveRequest(Long leaveRequestId, String rejectionReason) {
        // 1. TC-03: Kiểm tra quyền phê duyệt/từ chối đơn nghỉ phép
        Long currentUserId = authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE);

        // 2. Tìm đơn xin nghỉ
        LeaveRequest leaveRequest = loadLeaveRequestPort.findById(leaveRequestId)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Không tìm thấy đơn xin nghỉ phép với mã: " + leaveRequestId));

        // 3. Kiểm tra Data Scope & chống IDOR
        if (loadEmployeePort != null) {
            Optional<Employee> employeeOpt = loadEmployeePort.findById(new EmployeeId(leaveRequest.getEmployeeId()));
            if (employeeOpt.isPresent()) {
                Employee employee = employeeOpt.get();
                if (loadUserPort != null) {
                    User currentUser = loadUserPort.findById(new UserId(currentUserId))
                            .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));
                    requireEmployeeInScope(currentUser, employee, PermissionCode.LEAVE_REQUEST_APPROVE);
                }
            }
        }

        // 4. Thực thi nghiệp vụ domain: chuyển sang REJECTED kèm lý do bắt buộc
        leaveRequest.reject(currentUserId, rejectionReason);

        // 5. Lưu lại vào cơ sở dữ liệu
        LeaveRequest savedRequest = saveLeaveRequestPort.save(leaveRequest);

        // 6. TC-04: Ghi nhật ký kiểm toán (Audit Log)
        String auditDesc = String.format("Từ chối đơn nghỉ phép #%d của nhân viên #%d (%s đến %s). Lý do: %s",
                savedRequest.getId(),
                savedRequest.getEmployeeId(),
                savedRequest.getStartDate(),
                savedRequest.getEndDate(),
                rejectionReason);
        saveLeaveAuditLogPort.recordAudit(currentUserId, "REJECT_LEAVE_REQUEST", auditDesc);

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
                    && loadOrgUnitPort != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(
                            employee.getOrgUnitId(), currentUser.getScopeOrgUnitId());
        };
    }
}
