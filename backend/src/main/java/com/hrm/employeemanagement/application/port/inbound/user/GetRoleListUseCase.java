package com.hrm.employeemanagement.application.port.inbound.user;

import java.util.List;
import com.hrm.employeemanagement.application.dto.user.RoleResult;

public interface GetRoleListUseCase {
    List<RoleResult> getRoles();
}
