package com.hrm.employeemanagement.application.port.outbound.authorization;
import com.hrm.employeemanagement.domain.user.User;

public interface GetAuthenticatedUserPort {

    User getAuthenticatedUser();
}