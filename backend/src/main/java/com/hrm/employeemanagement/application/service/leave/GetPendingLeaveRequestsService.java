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
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * NCL-05-CN-003: Lấy danh sách đơn nghỉ phép chờ duyệt theo DataScope của người dùng (TC-01, TC-03).
 * Đưa toàn bộ việc lọc DataScope và phân trang xuống tầng Persistence Layer để tránh lỗi N+1 queries.
 */
public class GetPendingLeaveRequestsService implements GetPendingLeaveRequestsUseCase {

    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final AuthorizationService authorizationService;

    public GetPendingLeaveRequestsService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            LoadUserPort loadUserPort,
            AuthorizationService authorizationService
    ) {
        this(loadLeaveRequestPort, null, loadUserPort, null, authorizationService);
    }

    public GetPendingLeaveRequestsService(
            LoadLeaveRequestPort loadLeaveRequestPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            AuthorizationService authorizationService
    ) {
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.loadEmployeePort = loadEmployeePort;
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.loadOrgUnitPort = loadOrgUnitPort;
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
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

        List<LeaveRequest> requests = pagedRequests.getContent();

        Map<Long, Employee> employeeMap = Collections.emptyMap();
        List<EmployeeId> employeeIds = requests.stream()
                .map(LeaveRequest::getEmployeeId)
                .filter(Objects::nonNull)
                .distinct()
                .map(EmployeeId::new)
                .toList();

        if (loadEmployeePort != null && !employeeIds.isEmpty()) {
            employeeMap = loadEmployeePort.findAllByIdIn(employeeIds).stream()
                    .filter(e -> e.getIdValue() != null)
                    .collect(Collectors.toMap(Employee::getIdValue, Function.identity(), (a, b) -> a));
        }

        Map<Long, OrgUnit> orgUnitMap = Collections.emptyMap();
        if (loadOrgUnitPort != null && !employeeMap.isEmpty()) {
            List<Long> orgUnitIds = employeeMap.values().stream()
                    .map(Employee::getOrgUnitId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            if (!orgUnitIds.isEmpty()) {
                orgUnitMap = loadOrgUnitPort.findAllByIdIn(orgUnitIds).stream()
                        .filter(u -> u.getId() != null && u.getId().getValue() != null)
                        .collect(Collectors.toMap(u -> u.getId().getValue(), Function.identity(), (a, b) -> a));
            }
        }

        Map<Long, Employee> finalEmployeeMap = employeeMap;
        Map<Long, OrgUnit> finalOrgUnitMap = orgUnitMap;

        List<LeaveRequestResult> content = requests.stream()
                .map(req -> {
                    Employee emp = finalEmployeeMap.get(req.getEmployeeId());
                    String employeeName = emp != null ? emp.getFullName() : null;
                    OrgUnit ou = (emp != null && emp.getOrgUnitId() != null) ? finalOrgUnitMap.get(emp.getOrgUnitId()) : null;
                    String orgUnitName = ou != null ? ou.getUnitName() : null;
                    return LeaveRequestResult.fromDomain(req, employeeName, orgUnitName);
                })
                .toList();

        return new PageResult<>(
                content,
                pagedRequests.getPage(),
                pagedRequests.getSize(),
                pagedRequests.getTotalElements()
        );
    }
}
