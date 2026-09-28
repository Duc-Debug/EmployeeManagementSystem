package com.hrm.employeemanagement.infrastructure.transaction.outsourcedcontract;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.outsourcedcontract.AcknowledgeOutsourcedContractCommand;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.AcknowledgeOutsourcedContractResult;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.AcknowledgeOutsourcedContractWarningUseCase;

public class TransactionalAcknowledgeOutsourcedContractWarningDecorator implements AcknowledgeOutsourcedContractWarningUseCase {

    private final AcknowledgeOutsourcedContractWarningUseCase delegate;

    public TransactionalAcknowledgeOutsourcedContractWarningDecorator(AcknowledgeOutsourcedContractWarningUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    @Transactional
    public AcknowledgeOutsourcedContractResult execute(AcknowledgeOutsourcedContractCommand command) {
        return delegate.execute(command);
    }
}
