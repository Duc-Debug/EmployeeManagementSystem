package com.hrm.employeemanagement.port.in.user;

import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.dto.user.UserResult;

public interface GetUserListUseCase {
    PageResult<UserResult> getUsers(int page, int size);
    UserResult getUserById(Long id);
}
