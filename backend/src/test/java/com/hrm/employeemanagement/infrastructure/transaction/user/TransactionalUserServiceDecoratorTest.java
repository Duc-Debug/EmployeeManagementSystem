package com.hrm.employeemanagement.infrastructure.transaction.user;

import com.hrm.employeemanagement.application.dto.user.UpdateUserRoleCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.application.service.user.UserService;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.security.UserStatusCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionalUserServiceDecoratorTest {

    @Mock
    private UserService delegate;

    @Mock
    private UserStatusCache userStatusCache;

    private TransactionalUserServiceDecorator decorator;

    @BeforeEach
    void setUp() {
        decorator = new TransactionalUserServiceDecorator(delegate, userStatusCache);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("Xóa cache sau khi Transaction Commit thành công khi toggleUserStatus")
    void testToggleUserStatus_EvictsCacheAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        UserResult userResult = new UserResult(10L, "john_doe", "VT-04", "Nhân viên", UserStatus.LOCKED, null, "John Doe", null, null);
        when(delegate.toggleUserStatus(10L, true, 1L)).thenReturn(userResult);

        UserResult result = decorator.toggleUserStatus(10L, true, 1L);

        assertEquals(UserStatus.LOCKED, result.getStatus());
        // Cache should NOT be evicted yet before commit
        verify(userStatusCache, never()).evict("john_doe");

        // Simulate Spring Transaction Commit
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        // Cache must be evicted now
        verify(userStatusCache, times(1)).evict("john_doe");
    }

    @Test
    @DisplayName("Xóa cache sau khi Transaction Commit thành công khi updateUserRole")
    void testUpdateUserRole_EvictsCacheAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        UpdateUserRoleCommand command = new UpdateUserRoleCommand(10L, "VT-02", 5L);
        UserResult userResult = new UserResult(10L, "john_doe", "VT-02", "Quản lý dự án", UserStatus.ACTIVE, null, "John Doe", 5L, null);
        when(delegate.updateUserRole(command, 1L)).thenReturn(userResult);

        UserResult result = decorator.updateUserRole(command, 1L);

        assertEquals("VT-02", result.getRoleCode());
        // Cache should NOT be evicted yet before commit
        verify(userStatusCache, never()).evict("john_doe");

        // Simulate Spring Transaction Commit
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        // Cache must be evicted now
        verify(userStatusCache, times(1)).evict("john_doe");
    }
}
