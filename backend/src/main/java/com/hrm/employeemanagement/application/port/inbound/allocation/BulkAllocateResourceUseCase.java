package com.hrm.employeemanagement.application.port.inbound.allocation;

import com.hrm.employeemanagement.application.dto.allocation.BulkAllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.BulkAllocationResult;

/**
 * NCL-06-CN-006: Phân bổ hàng loạt cho nhiều tuần trong một thao tác.
 */
public interface BulkAllocateResourceUseCase {
    BulkAllocationResult bulkAllocateResource(BulkAllocateResourceCommand command);
}
