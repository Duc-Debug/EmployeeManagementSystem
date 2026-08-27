package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.skill.SkillResult;
import com.hrm.employeemanagement.application.port.inbound.skill.MergeSkillUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto.MergeSkillRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto.SkillResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

    private final MergeSkillUseCase mergeSkillUseCase;

    public SkillController(
            @Qualifier("transactionalMergeSkillUseCase") MergeSkillUseCase mergeSkillUseCase) {
        this.mergeSkillUseCase = mergeSkillUseCase;
    }

    @PostMapping("/merge")
    public ResponseEntity<SkillResponse> mergeSkills(@Valid @RequestBody MergeSkillRequest request) {
        // 1. Chuyển đổi Request DTO sang Application Command
        SkillResult result = mergeSkillUseCase.execute(request.toCommand());
        
        // 2. Chuyển Result sang Response DTO trả về cho client
        return ResponseEntity.ok(SkillResponse.fromResult(result));
    }
}