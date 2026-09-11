package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.port.inbound.leave.GetPendingLeaveRequestsUseCase;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * NCL-05-CN-003: Lấy danh sách đơn nghỉ phép chờ duyệt theo DataScope của người dùng (TC-01, TC-03).
 */
public class GetPendingLeaveRequestsService implements GetPendingLeaveRequestsUseCase {

    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final AuthorizationService authorizationService;

    public GetPendingLeaveRequestsService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            AuthorizationService authorizationService
    ) {
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "loadOrgUnitPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
    }

    @Override
    public List<LeaveRequestResult> getPendingLeaveRequests() {
        Long currentUserId = authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        List<LeaveRequest> allPending = loadLeaveRequestPort.findPendingRequests();
        List<LeaveRequestResult> filtered = new ArrayList<>();

        for (LeaveRequest req : allPending) {
            Employee employee = loadEmployeePort.findById(new EmployeeId(req.getEmployeeId())).orElse(null);
            if (employee != null && isEmployeeInScope(currentUser, employee)) {
                filtered.add(LeaveRequestResult.fromDomain(req));
            }
        }

        return filtered;
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
