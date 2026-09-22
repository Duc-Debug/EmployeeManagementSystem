package com.hrm.employeemanagement.application.port.inbound.outsourcedcontract;

import com.hrm.employeemanagement.application.dto.outsourcedcontract.AcknowledgeOutsourcedContractCommand;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.AcknowledgeOutsourcedContractResult;

public interface AcknowledgeOutsourcedContractWarningUseCase {
    AcknowledgeOutsourcedContractResult execute(AcknowledgeOutsourcedContractCommand command);
}
