package com.hrm.employeemanagement.infrastructure.adapter.outbound.security;

import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.infrastructure.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityContextCurrentUserAdapter implements CurrentUserPort {

    @Override
    public Optional<Long> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof User user) {
                return Optional.ofNullable(user.getIdValue());
            }
            if (principal instanceof UserPrincipal up) {
                return Optional.ofNullable(up.getId());
            }
        }
        if (auth != null && auth.getPrincipal() instanceof com.hrm.employeemanagement.domain.user.User user) {
            return Optional.ofNullable(user.getIdValue());
        }
        return Optional.empty();
    }
}