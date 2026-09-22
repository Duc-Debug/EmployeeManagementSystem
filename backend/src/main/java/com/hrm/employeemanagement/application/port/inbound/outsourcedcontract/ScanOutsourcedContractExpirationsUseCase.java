package com.hrm.employeemanagement.application.port.inbound.outsourcedcontract;

import com.hrm.employeemanagement.application.dto.outsourcedcontract.ScanOutsourcedContractsResult;

public interface ScanOutsourcedContractExpirationsUseCase {
    ScanOutsourcedContractsResult execute(boolean isManualTrigger);
}
