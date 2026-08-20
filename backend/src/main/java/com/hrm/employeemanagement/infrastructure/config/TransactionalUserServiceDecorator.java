package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.dto.user.CreateUserCommand;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.dto.user.UpdateUserRoleCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.application.service.user.UserService;
import com.hrm.employeemanagement.infrastructure.security.UserStatusCache;
import com.hrm.employeemanagement.port.in.user.CreateUserUseCase;
import com.hrm.employeemanagement.port.in.user.GetUserListUseCase;
import com.hrm.employeemanagement.port.in.user.ToggleUserStatusUseCase;
import com.hrm.employeemanagement.port.in.user.UpdateUserRoleUseCase;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transaction-Aware Decorator at Infrastructure Layer.
 * Manages database transaction boundaries and cache eviction for Use Cases
 * while keeping the underlying Application Service (UserService) 100% Pure Java.
 */
public class TransactionalUserServiceDecorator implements CreateUserUseCase, ToggleUserStatusUseCase, UpdateUserRoleUseCase, GetUserListUseCase {

    private final UserService delegate;
    private final UserStatusCache userStatusCache;

    public TransactionalUserServiceDecorator(UserService delegate, UserStatusCache userStatusCache) {
        this.delegate = delegate;
        this.userStatusCache = userStatusCache;
    }

    @Override
    @Transactional
    public UserResult createUser(CreateUserCommand command, Long currentAdminId) {
        return delegate.createUser(command, currentAdminId);
    }

    @Override
    @Transactional
    public UserResult toggleUserStatus(Long userId, boolean lock, Long currentAdminId) {
        UserResult result = delegate.toggleUserStatus(userId, lock, currentAdminId);
        // Immediate Cache Invalidation upon Lock/Unlock
        userStatusCache.evict(result.getUsername());
        return result;
    }

    @Override
    @Transactional
    public UserResult updateUserRole(UpdateUserRoleCommand command, Long currentAdminId) {
        UserResult result = delegate.updateUserRole(command, currentAdminId);
        // Immediate Cache Invalidation upon Role change
        userStatusCache.evict(result.getUsername());
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
}
