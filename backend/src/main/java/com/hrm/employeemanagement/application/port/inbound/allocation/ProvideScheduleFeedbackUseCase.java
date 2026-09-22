package com.hrm.employeemanagement.application.port.inbound.allocation;

import java.time.LocalDate;
import com.hrm.employeemanagement.application.dto.allocation.ProvideScheduleFeedbackResult;

public interface ProvideScheduleFeedbackUseCase {

    ProvideScheduleFeedbackResult provideFeedback(LocalDate weekStart, String reason, String ipAddress);
}
