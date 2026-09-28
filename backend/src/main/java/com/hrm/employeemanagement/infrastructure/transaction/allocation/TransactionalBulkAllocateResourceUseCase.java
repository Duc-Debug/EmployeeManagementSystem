package com.hrm.employeemanagement.infrastructure.transaction.allocation;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.allocation.BulkAllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.BulkAllocationResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.BulkAllocateResourceUseCase;

/**
 * NCL-06-CN-006: Transactional Decorator cho BulkAllocateResourceUseCase, đảm bảo thao tác phân bổ hàng loạt được thực thi trong một Spring Transaction.
 */
public class TransactionalBulkAllocateResourceUseCase implements BulkAllocateResourceUseCase {

    private final BulkAllocateResourceUseCase pureDelegate;

    public TransactionalBulkAllocateResourceUseCase(BulkAllocateResourceUseCase pureDelegate) {
        this.pureDelegate = Objects.requireNonNull(pureDelegate, "BulkAllocateResourceUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public BulkAllocationResult bulkAllocateResource(BulkAllocateResourceCommand command) {
        return pureDelegate.bulkAllocateResource(command);
    }
}
