package com.hrm.employeemanagement.infrastructure.transaction.user;

import com.hrm.employeemanagement.application.dto.user.ChangePasswordCommand;
import com.hrm.employeemanagement.application.dto.user.RequestPasswordResetCommand;
import com.hrm.employeemanagement.application.dto.user.ResetPasswordCommand;
import com.hrm.employeemanagement.application.port.inbound.user.ChangePasswordUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.RequestPasswordResetUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.ResetPasswordUseCase;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.user.PasswordService;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.security.UserStatusCache;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Transaction-Aware Decorator for Password Operations.
 * Manages database transaction boundaries and post-commit cache eviction for Password Use Cases.
 */
public class TransactionalPasswordServiceDecorator implements ChangePasswordUseCase, RequestPasswordResetUseCase, ResetPasswordUseCase {

    private final PasswordService delegate;
    private final LoadUserPort loadUserPort;
    private final UserStatusCache userStatusCache;

    public TransactionalPasswordServiceDecorator(PasswordService delegate, LoadUserPort loadUserPort, UserStatusCache userStatusCache) {
        this.delegate = delegate;
        this.loadUserPort = loadUserPort;
        this.userStatusCache = userStatusCache;
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        delegate.changePassword(command);
        if (command.userId() != null) {
            loadUserPort.findById(new UserId(command.userId()))
                    .ifPresent(user -> evictCacheAfterCommit(user.getUsername()));
        }
    }

    @Override
    @Transactional
    public void requestPasswordReset(RequestPasswordResetCommand command) {
        delegate.requestPasswordReset(command);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordCommand command) {
        delegate.resetPassword(command);
        // Note: Password reset token invalidates cached user session
        userStatusCache.clear();
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
