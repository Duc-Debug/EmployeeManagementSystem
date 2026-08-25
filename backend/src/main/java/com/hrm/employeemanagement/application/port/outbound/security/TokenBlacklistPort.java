package com.hrm.employeemanagement.application.port.outbound.security;

public interface TokenBlacklistPort {
    void blacklist(String token, long remainingTtlMs);
    boolean isBlacklisted(String token);
}
