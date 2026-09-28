package com.hrm.employeemanagement.infrastructure.transaction.allocation;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.allocation.ProvideScheduleFeedbackResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.ProvideScheduleFeedbackUseCase;

@Transactional
public class TransactionalProvideScheduleFeedbackUseCase implements ProvideScheduleFeedbackUseCase {

    private final ProvideScheduleFeedbackUseCase delegate;

    public TransactionalProvideScheduleFeedbackUseCase(ProvideScheduleFeedbackUseCase delegate) {
        this.delegate = Objects.requireNonNull(delegate, "ProvideScheduleFeedbackUseCase delegate must not be null");
    }

    @Override
    public ProvideScheduleFeedbackResult provideFeedback(LocalDate weekStart, String reason, String ipAddress) {
        return delegate.provideFeedback(weekStart, reason, ipAddress);
    }
}
