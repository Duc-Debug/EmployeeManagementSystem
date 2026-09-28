package com.hrm.employeemanagement.application.port.outbound.allocation.idleness;

import com.hrm.employeemanagement.domain.allocation.idleness.ProlongedIdlenessAcknowledgement;

public interface SaveProlongedIdlenessAcknowledgementPort {
    ProlongedIdlenessAcknowledgement save(ProlongedIdlenessAcknowledgement acknowledgement);
}
