package com.hrm.employeemanagement.infrastructure.transaction.user;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.hrm.employeemanagement.application.dto.user.AuthTokenResult;
import com.hrm.employeemanagement.application.dto.user.LoginCommand;
import com.hrm.employeemanagement.application.port.inbound.user.AuthenticateUserUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.LogoutUserUseCase;
import com.hrm.employeemanagement.application.service.user.AuthService;
import com.hrm.employeemanagement.infrastructure.security.UserStatusCache;

/**
 * Transaction-Aware Decorator for Authentication & Logout Use Cases.
 * Manages database transaction boundaries and post-commit cache eviction for logout.
 */
public class TransactionalAuthServiceDecorator implements AuthenticateUserUseCase, LogoutUserUseCase {

    private final AuthService delegate;
    private final UserStatusCache userStatusCache;

    public TransactionalAuthServiceDecorator(AuthService delegate, UserStatusCache userStatusCache) {
        this.delegate = delegate;
        this.userStatusCache = userStatusCache;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthTokenResult login(LoginCommand command) {
        return delegate.login(command);
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        String username = delegate.logoutAndReturnUsername(userId);
        if (username != null) {
            evictCacheAfterCommit(username);
        }
    }

    private void evictCacheAfterCommit(String username) {
        if (username == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    userStatusCache.evict(username);
                }
            });
        } else {
            userStatusCache.evict(username);
        }
    }
}
