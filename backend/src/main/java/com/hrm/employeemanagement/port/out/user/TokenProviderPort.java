package com.hrm.employeemanagement.port.out.user;

import com.hrm.employeemanagement.domain.model.user.User;

public interface TokenProviderPort {
    String generateToken(User user);
    String getUsernameFromToken(String token);
    boolean validateToken(String token);
}
