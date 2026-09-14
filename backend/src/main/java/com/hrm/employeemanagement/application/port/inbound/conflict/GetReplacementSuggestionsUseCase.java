package com.hrm.employeemanagement.application.port.inbound.conflict;

import com.hrm.employeemanagement.application.dto.conflict.ReplacementSuggestionResult;

public interface GetReplacementSuggestionsUseCase {
    ReplacementSuggestionResult getReplacementSuggestions(Long conflictId, Long skillId, Integer minProficiencyLevel);
}
