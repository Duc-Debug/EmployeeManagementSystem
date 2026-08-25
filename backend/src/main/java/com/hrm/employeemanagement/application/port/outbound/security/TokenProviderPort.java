package com.hrm.employeemanagement.application.port.outbound.security;

import com.hrm.employeemanagement.domain.user.User;

public interface TokenProviderPort {
    String generateToken(User user);
    String getUsernameFromToken(String token);
    boolean validateToken(String token);
    long getRemainingExpirationMs(String token);
    Long getUserIdFromToken(String token);
    String getJtiFromToken(String token);
    long getIssuedAtTimestampFromToken(String token);
}
