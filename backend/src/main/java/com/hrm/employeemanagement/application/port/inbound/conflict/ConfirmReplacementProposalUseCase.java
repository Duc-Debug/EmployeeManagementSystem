package com.hrm.employeemanagement.application.port.inbound.conflict;

import com.hrm.employeemanagement.application.dto.conflict.ConfirmReplacementProposalCommand;
import com.hrm.employeemanagement.application.dto.conflict.ReplacementProposalResult;

public interface ConfirmReplacementProposalUseCase {
    ReplacementProposalResult confirmReplacementProposal(ConfirmReplacementProposalCommand command);
}
