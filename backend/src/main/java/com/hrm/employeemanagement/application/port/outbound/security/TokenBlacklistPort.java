package com.hrm.employeemanagement.application.port.outbound.security;

public interface TokenBlacklistPort {
    void blacklist(String token, long remainingTtlMs);
    boolean isBlacklisted(String token);
    void blacklistUser(String username, long issuedBeforeTimestamp);
    boolean isUserRevoked(String username, long issuedAtTimestamp);
}

