package com.hrm.employeemanagement.infrastructure.transaction.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.MoveOrgUnitCommand;
import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitResult;
import com.hrm.employeemanagement.application.port.inbound.orgunit.MoveOrgUnitUseCase;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

public class TransactionalMoveOrgUnitUseCase implements MoveOrgUnitUseCase {
    private final MoveOrgUnitUseCase delegate;

    public TransactionalMoveOrgUnitUseCase(MoveOrgUnitUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "MoveOrgUnitUseCase delegate không được phép là null");
    }

    @Override
    @Transactional
    public OrgUnitResult execute(MoveOrgUnitCommand command) {
        return delegate.execute(command);
    }
}