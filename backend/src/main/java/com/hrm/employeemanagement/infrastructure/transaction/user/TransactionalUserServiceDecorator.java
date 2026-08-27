package com.hrm.employeemanagement.infrastructure.transaction.user;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.hrm.employeemanagement.application.dto.user.CreateUserCommand;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.dto.user.UpdateUserRoleCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.application.port.inbound.user.CreateUserUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.GetCurrentUserProfileUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.GetUserListUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.ToggleUserStatusUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.UpdateUserRoleUseCase;
import com.hrm.employeemanagement.application.service.user.UserService;
import com.hrm.employeemanagement.infrastructure.security.UserStatusCache;

/**
 * Transaction-Aware Decorator at Infrastructure Layer.
 * Manages database transaction boundaries and post-commit cache invalidation for Use Cases
 * while keeping the underlying Application Service (UserService) 100% Pure Java.
 */
public class TransactionalUserServiceDecorator implements CreateUserUseCase, ToggleUserStatusUseCase, UpdateUserRoleUseCase, GetUserListUseCase, GetCurrentUserProfileUseCase {

    private final UserService delegate;
    private final UserStatusCache userStatusCache;

    public TransactionalUserServiceDecorator(UserService delegate, UserStatusCache userStatusCache) {
        this.delegate = delegate;
        this.userStatusCache = userStatusCache;
    }

    @Override
    @Transactional
    public UserResult createUser(CreateUserCommand command) {
        return delegate.createUser(command);
    }

    @Override
    @Transactional
    public UserResult toggleUserStatus(Long userId, boolean lock) {
        UserResult result = delegate.toggleUserStatus(userId, lock);
        // Defer cache invalidation until after the database transaction successfully commits
        evictCacheAfterCommit(result.getUsername());
        return result;
    }

    @Override
    @Transactional
    public UserResult updateUserRole(UpdateUserRoleCommand command) {
        UserResult result = delegate.updateUserRole(command);
        // Defer cache invalidation until after the database transaction successfully commits
        evictCacheAfterCommit(result.getUsername());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserResult> getUsers(int page, int size) {
        return delegate.getUsers(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResult getUserById(Long id) {
        return delegate.getUserById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResult getCurrentUserProfile(Long id) {
        return delegate.getCurrentUserProfile(id);
    }

    /**
     * Invalidate user cache strictly after successful transaction commit.
     * Prevents race conditions where concurrent requests repopulate stale uncommitted data into the cache.
     */
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
