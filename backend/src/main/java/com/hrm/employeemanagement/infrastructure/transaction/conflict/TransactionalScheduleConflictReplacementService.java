package com.hrm.employeemanagement.infrastructure.transaction.conflict;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.conflict.ConfirmReplacementProposalCommand;
import com.hrm.employeemanagement.application.dto.conflict.ReplacementProposalResult;
import com.hrm.employeemanagement.application.dto.conflict.ReplacementSuggestionResult;
import com.hrm.employeemanagement.application.port.inbound.conflict.ConfirmReplacementProposalUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.GetReplacementSuggestionsUseCase;
import com.hrm.employeemanagement.application.service.conflict.ScheduleConflictReplacementService;

public class TransactionalScheduleConflictReplacementService implements GetReplacementSuggestionsUseCase, ConfirmReplacementProposalUseCase {

    private final ScheduleConflictReplacementService delegate;

    public TransactionalScheduleConflictReplacementService(ScheduleConflictReplacementService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional(readOnly = true)
    public ReplacementSuggestionResult getReplacementSuggestions(Long conflictId, Long targetSkillId, Integer minProficiencyLevel) {
        return delegate.getReplacementSuggestions(conflictId, targetSkillId, minProficiencyLevel);
    }

    @Override
    @Transactional
    public ReplacementProposalResult confirmReplacementProposal(ConfirmReplacementProposalCommand command) {
        return delegate.confirmReplacementProposal(command);
    }
}
