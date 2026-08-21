package com.hrm.employeemanagement.infrastructure.transaction.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitResult;
import com.hrm.employeemanagement.application.dto.orgunit.UpdateOrgUnitCommand;
import com.hrm.employeemanagement.application.port.inbound.orgunit.UpdateOrgUnitUseCase;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

public class TransactionalUpdateOrgUnitUseCase implements UpdateOrgUnitUseCase {
    private final UpdateOrgUnitUseCase delegate;

    public TransactionalUpdateOrgUnitUseCase(UpdateOrgUnitUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "UpdateOrgUnitUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public OrgUnitResult execute(UpdateOrgUnitCommand command) {
        return delegate.execute(command);
    }
}