package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.port.inbound.leave.GetPendingLeaveRequestsUseCase;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.util.List;
import java.util.Objects;

/**
 * NCL-05-CN-003: Lấy danh sách đơn nghỉ phép chờ duyệt theo DataScope của người dùng (TC-01, TC-03).
 * Đưa toàn bộ việc lọc DataScope và phân trang xuống tầng Persistence Layer để tránh lỗi N+1 queries.
 */
public class GetPendingLeaveRequestsService implements GetPendingLeaveRequestsUseCase {

    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final LoadUserPort loadUserPort;
    private final AuthorizationService authorizationService;

    public GetPendingLeaveRequestsService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            LoadUserPort loadUserPort,
            AuthorizationService authorizationService
    ) {
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
    }

    public GetPendingLeaveRequestsService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            AuthorizationService authorizationService
    ) {
        this(loadLeaveRequestPort, loadUserPort, authorizationService);
    }

    @Override
    public List<LeaveRequestResult> getPendingLeaveRequests() {
        return getPendingLeaveRequests(0, 1000).getContent();
    }

    @Override
    public PageResult<LeaveRequestResult> getPendingLeaveRequests(int page, int size) {
        Long currentUserId = authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        PageResult<LeaveRequest> pagedRequests = loadLeaveRequestPort.findPendingRequests(
                currentUser.getDataScope(),
                currentUser.getScopeOrgUnitId(),
                currentUser.getIdValue(),
                page,
                size
        );

        List<LeaveRequestResult> content = pagedRequests.getContent().stream()
                .map(LeaveRequestResult::fromDomain)
                .toList();

        return new PageResult<>(
                content,
                pagedRequests.getPage(),
                pagedRequests.getSize(),
                pagedRequests.getTotalElements()
        );
    }
}
