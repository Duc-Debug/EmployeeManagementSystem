package com.hrm.employeemanagement.infrastructure.transaction.allocation;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.allocation.idleness.AcknowledgeProlongedIdleStaffCommand;
import com.hrm.employeemanagement.application.dto.allocation.idleness.AcknowledgeProlongedIdleStaffResult;
import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdlenessQuery;
import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdlenessReportResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.idleness.AcknowledgeProlongedIdleStaffUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.idleness.GetProlongedIdleStaffUseCase;
import com.hrm.employeemanagement.application.service.allocation.idleness.ProlongedIdleStaffService;

/**
 * Transactional Decorator cho Use Case NCL-07-CN-006: Cảnh báo nhân sự nhàn rỗi kéo dài.
 */
public class TransactionalProlongedIdleStaffService implements GetProlongedIdleStaffUseCase, AcknowledgeProlongedIdleStaffUseCase {

    private final ProlongedIdleStaffService delegate;

    public TransactionalProlongedIdleStaffService(ProlongedIdleStaffService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional(readOnly = true)
    public ProlongedIdlenessReportResult getProlongedIdleStaff(ProlongedIdlenessQuery query) {
        return delegate.getProlongedIdleStaff(query);
    }

    @Override
    @Transactional
    public AcknowledgeProlongedIdleStaffResult acknowledgeProlongedIdleStaff(AcknowledgeProlongedIdleStaffCommand command) {
        return delegate.acknowledgeProlongedIdleStaff(command);
    }
}
