package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.port.inbound.leave.RejectLeaveRequestUseCase;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveRequestPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.leave.LeaveRequestNotFoundException;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;

import java.util.Objects;

/**
 * NCL-05-CN-003: Xử lý từ chối đơn xin nghỉ phép kèm lý do (TC-03, TC-04).
 */
public class RejectLeaveRequestService implements RejectLeaveRequestUseCase {

    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final SaveLeaveRequestPort saveLeaveRequestPort;
    private final SaveLeaveAuditLogPort saveLeaveAuditLogPort;
    private final AuthorizationService authorizationService;

    public RejectLeaveRequestService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveLeaveAuditLogPort saveLeaveAuditLogPort,
            AuthorizationService authorizationService
    ) {
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.saveLeaveRequestPort = Objects.requireNonNull(saveLeaveRequestPort, "saveLeaveRequestPort must not be null");
        this.saveLeaveAuditLogPort = Objects.requireNonNull(saveLeaveAuditLogPort, "saveLeaveAuditLogPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
    }

    @Override
    public LeaveRequestResult rejectLeaveRequest(Long leaveRequestId, String rejectionReason) {
        // 1. TC-03: Kiểm tra quyền phê duyệt/từ chối đơn nghỉ phép
        Long currentUserId = authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE);

        // 2. Tìm đơn xin nghỉ
        LeaveRequest leaveRequest = loadLeaveRequestPort.findById(leaveRequestId)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Không tìm thấy đơn xin nghỉ phép với mã: " + leaveRequestId));

        // 3. Thực thi nghiệp vụ domain: chuyển sang REJECTED kèm lý do bắt buộc
        leaveRequest.reject(currentUserId, rejectionReason);

        // 4. Lưu lại vào cơ sở dữ liệu
        LeaveRequest savedRequest = saveLeaveRequestPort.save(leaveRequest);

        // 5. TC-04: Ghi nhật ký kiểm toán (Audit Log)
        String auditDesc = String.format("Từ chối đơn nghỉ phép #%d của nhân viên #%d (%s đến %s). Lý do: %s",
                savedRequest.getId(),
                savedRequest.getEmployeeId(),
                savedRequest.getStartDate(),
                savedRequest.getEndDate(),
                rejectionReason);
        saveLeaveAuditLogPort.recordAudit(currentUserId, "REJECT_LEAVE_REQUEST", auditDesc);

        return LeaveRequestResult.fromDomain(savedRequest);
    }
}
