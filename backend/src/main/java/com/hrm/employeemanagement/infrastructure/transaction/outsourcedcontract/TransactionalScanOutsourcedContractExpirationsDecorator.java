package com.hrm.employeemanagement.infrastructure.transaction.outsourcedcontract;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.outsourcedcontract.ScanOutsourcedContractsResult;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.ScanOutsourcedContractExpirationsUseCase;

public class TransactionalScanOutsourcedContractExpirationsDecorator implements ScanOutsourcedContractExpirationsUseCase {

    private final ScanOutsourcedContractExpirationsUseCase delegate;

    public TransactionalScanOutsourcedContractExpirationsDecorator(ScanOutsourcedContractExpirationsUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    @Transactional
    public ScanOutsourcedContractsResult execute(boolean isManualTrigger) {
        return delegate.execute(isManualTrigger);
    }
}
