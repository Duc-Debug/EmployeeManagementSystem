package com.hrm.employeemanagement.application.port.inbound.outsourcedcontract;

import com.hrm.employeemanagement.application.dto.outsourcedcontract.ExpiringOutsourcedContractListResult;

public interface GetExpiringOutsourcedContractsUseCase {
    ExpiringOutsourcedContractListResult execute(Integer thresholdDays);
}
