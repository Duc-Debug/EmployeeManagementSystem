package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.user.User;

/**
 * Service kiểm tra Data Scope dùng chung cho các Use Case quản lý thời gian không sẵn sàng (NCL-13-CN-003).
 */
public class UnavailabilityDataScopeValidator {

    private final LoadOrgUnitPort loadOrgUnitPort;

    public UnavailabilityDataScopeValidator(LoadOrgUnitPort loadOrgUnitPort) {
        this.loadOrgUnitPort = loadOrgUnitPort;
    }

    /**
     * Xác thực xem nhân viên có thuộc phạm vi dữ liệu (Data Scope) của người dùng hiện tại hay không.
     *
     * @param currentUser    Người dùng thực hiện thao tác
     * @param employee       Nhân viên sở hữu đơn khai báo
     * @param permissionCode Quyền đang yêu cầu kiểm tra (dùng để báo lỗi PermissionDeniedException)
     */
    public void requireEmployeeInScope(User currentUser, Employee employee, PermissionCode permissionCode) {
        if (currentUser == null || employee == null) {
            throw new PermissionDeniedException(permissionCode);
        }

        boolean allowed = switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> currentUser.getIdValue() != null && currentUser.getIdValue().equals(employee.getUserIdValue());
            case ORGANIZATION_BRANCH -> employee.getOrgUnitId() != null
                    && currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(
                            employee.getOrgUnitId(), currentUser.getScopeOrgUnitId());
        };

        if (!allowed) {
            throw new PermissionDeniedException(permissionCode);
        }
    }
}
