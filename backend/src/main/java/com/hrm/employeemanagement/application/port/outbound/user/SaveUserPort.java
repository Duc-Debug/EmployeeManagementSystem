package com.hrm.employeemanagement.application.port.outbound.user;

import com.hrm.employeemanagement.domain.user.User;

public interface SaveUserPort {
    User save(User user);
}
