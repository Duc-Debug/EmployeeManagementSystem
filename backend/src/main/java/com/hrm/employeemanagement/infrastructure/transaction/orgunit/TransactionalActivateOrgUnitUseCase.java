package com.hrm.employeemanagement.infrastructure.transaction.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.ActivateOrgUnitCommand;
import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitResult;
import com.hrm.employeemanagement.application.port.inbound.orgunit.ActivateOrgUnitUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class TransactionalActivateOrgUnitUseCase implements ActivateOrgUnitUseCase {
    private final ActivateOrgUnitUseCase delegate;

    public TransactionalActivateOrgUnitUseCase(ActivateOrgUnitUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "ActivateOrgUnitUseCase delegate không được phép là null");
    }

    @Override
    @Transactional
    public OrgUnitResult execute(ActivateOrgUnitCommand command) {
        return delegate.execute(command);
    }
}
