package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.conflict;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.conflict.ConfirmReplacementProposalCommand;
import com.hrm.employeemanagement.application.dto.conflict.ReplacementProposalResult;
import com.hrm.employeemanagement.application.dto.conflict.ReplacementSuggestionResult;
import com.hrm.employeemanagement.application.port.inbound.conflict.ConfirmReplacementProposalUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.GetReplacementSuggestionsUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/schedule-conflicts")
public class ScheduleConflictReplacementController {

    private final GetReplacementSuggestionsUseCase getReplacementSuggestionsUseCase;
    private final ConfirmReplacementProposalUseCase confirmReplacementProposalUseCase;

    public ScheduleConflictReplacementController(
            GetReplacementSuggestionsUseCase getReplacementSuggestionsUseCase,
            ConfirmReplacementProposalUseCase confirmReplacementProposalUseCase
    ) {
        this.getReplacementSuggestionsUseCase = getReplacementSuggestionsUseCase;
        this.confirmReplacementProposalUseCase = confirmReplacementProposalUseCase;
    }

    /**
     * API Gợi ý nhân sự thay thế khi có xung đột lịch (NCL-07-CN-002)
     */
    @GetMapping("/{id}/replacement-suggestions")
    @PreAuthorize("hasAuthority('RESOURCE_REPLACEMENT_SUGGEST')")
    public ResponseEntity<ApiResponse<ReplacementSuggestionResult>> getReplacementSuggestions(
            @PathVariable Long id,
            @RequestParam(required = false) Long skillId,
            @RequestParam(required = false) Integer minProficiencyLevel
    ) {
        ReplacementSuggestionResult result = getReplacementSuggestionsUseCase
                .getReplacementSuggestions(id, skillId, minProficiencyLevel);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách gợi ý nhân sự thay thế thành công", result));
    }

    /**
     * API Xác nhận đề xuất nhân sự thay thế (NCL-07-CN-002)
     */
    @PostMapping("/{id}/confirm-replacement")
    @PreAuthorize("hasAuthority('RESOURCE_REPLACEMENT_SUGGEST')")
    public ResponseEntity<ApiResponse<ReplacementProposalResult>> confirmReplacementProposal(
            @PathVariable Long id,
            @RequestBody ConfirmReplacementProposalCommand command
    ) {
        ConfirmReplacementProposalCommand targetCommand = new ConfirmReplacementProposalCommand(
                id,
                command.replacementEmployeeId(),
                command.skillId(),
                command.proficiencyLevel(),
                command.notes()
        );
        ReplacementProposalResult result = confirmReplacementProposalUseCase
                .confirmReplacementProposal(targetCommand);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận đề xuất nhân sự thay thế thành công", result));
    }
}
