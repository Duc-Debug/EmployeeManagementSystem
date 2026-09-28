package com.hrm.employeemanagement.infrastructure.transaction.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.CreateOrgUnitCommand;
import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitResult;
import com.hrm.employeemanagement.application.port.inbound.orgunit.CreateOrgUnitUseCase;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

public class TransactionalCreateOrgUnitUseCase implements CreateOrgUnitUseCase {
    private final CreateOrgUnitUseCase delegate;

    public TransactionalCreateOrgUnitUseCase(CreateOrgUnitUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "CreateOrgUnitUseCase delegate không được phép là null.");
    }

    @Override
    @Transactional
    public OrgUnitResult execute(CreateOrgUnitCommand command) {
        return delegate.execute(command);
    }
}