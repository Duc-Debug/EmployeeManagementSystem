package com.hrm.employeemanagement.application.port.outbound.security;

import java.util.Optional;

public interface CurrentUserPort {
    Optional<Long> getCurrentUserId();
}