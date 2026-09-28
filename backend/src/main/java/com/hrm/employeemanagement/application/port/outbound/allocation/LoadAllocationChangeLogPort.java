package com.hrm.employeemanagement.application.port.outbound.allocation;

import java.util.List;
import com.hrm.employeemanagement.domain.allocation.AllocationChangeLog;

public interface LoadAllocationChangeLogPort {

    List<AllocationChangeLog> findByAllocationId(Long allocationId);
}
