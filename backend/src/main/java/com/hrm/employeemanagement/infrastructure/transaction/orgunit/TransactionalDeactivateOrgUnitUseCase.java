package com.hrm.employeemanagement.infrastructure.transaction.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.DeactivateOrgUnitCommand;
import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitResult;
import com.hrm.employeemanagement.application.port.inbound.orgunit.DeactivateOrgUnitUseCase;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

public class TransactionalDeactivateOrgUnitUseCase implements DeactivateOrgUnitUseCase {
    private final DeactivateOrgUnitUseCase delegate;

    public TransactionalDeactivateOrgUnitUseCase(DeactivateOrgUnitUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "DeactivateOrgUnitUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public OrgUnitResult execute(DeactivateOrgUnitCommand command) {
        return delegate.execute(command);
    }
}