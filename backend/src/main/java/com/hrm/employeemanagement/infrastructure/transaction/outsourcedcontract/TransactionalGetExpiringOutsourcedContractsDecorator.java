package com.hrm.employeemanagement.infrastructure.transaction.outsourcedcontract;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.outsourcedcontract.ExpiringOutsourcedContractListResult;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.GetExpiringOutsourcedContractsUseCase;

public class TransactionalGetExpiringOutsourcedContractsDecorator implements GetExpiringOutsourcedContractsUseCase {

    private final GetExpiringOutsourcedContractsUseCase delegate;

    public TransactionalGetExpiringOutsourcedContractsDecorator(GetExpiringOutsourcedContractsUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public ExpiringOutsourcedContractListResult execute(Integer thresholdDays) {
        return delegate.execute(thresholdDays);
    }
}
