package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.hrm.employeemanagement.application.dto.skill.*;
import com.hrm.employeemanagement.application.port.inbound.skill.*;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/v1/skills")
@Validated
public class SkillController {

    private final CreateSkillUseCase createSkillUseCase;
    private final UpdateSkillUseCase updateSkillUseCase;
    private final MergeSkillUseCase mergeSkillUseCase;
    private final DeactivateSkillUseCase deactivateSkillUseCase;
    private final GetSkillListUseCase getSkillListUseCase;
    private final GetSkillGroupListUseCase getSkillGroupListUseCase;
    private final CreateSkillGroupUseCase createSkillGroupUseCase;
    private final UpdateSkillGroupUseCase updateSkillGroupUseCase;
    private final DeactivateSkillGroupUseCase deactivateSkillGroupUseCase;

    public SkillController(
            @Qualifier("transactionalCreateSkillUseCase") CreateSkillUseCase createSkillUseCase,
            @Qualifier("transactionalUpdateSkillUseCase") UpdateSkillUseCase updateSkillUseCase,
            @Qualifier("transactionalMergeSkillUseCase") MergeSkillUseCase mergeSkillUseCase,
            @Qualifier("transactionalDeactivateSkillUseCase") DeactivateSkillUseCase deactivateSkillUseCase,
            @Qualifier("getSkillListUseCase") GetSkillListUseCase getSkillListUseCase,
            @Qualifier("getSkillGroupListUseCase") GetSkillGroupListUseCase getSkillGroupListUseCase,
            @Qualifier("transactionalCreateSkillGroupUseCase") CreateSkillGroupUseCase createSkillGroupUseCase,
            @Qualifier("transactionalUpdateSkillGroupUseCase") UpdateSkillGroupUseCase updateSkillGroupUseCase,
            @Qualifier("transactionalDeactivateSkillGroupUseCase") DeactivateSkillGroupUseCase deactivateSkillGroupUseCase) {
        this.createSkillUseCase = createSkillUseCase;
        this.updateSkillUseCase = updateSkillUseCase;
        this.mergeSkillUseCase = mergeSkillUseCase;
        this.deactivateSkillUseCase = deactivateSkillUseCase;
        this.getSkillListUseCase = getSkillListUseCase;
        this.getSkillGroupListUseCase = getSkillGroupListUseCase;
        this.createSkillGroupUseCase = createSkillGroupUseCase;
        this.updateSkillGroupUseCase = updateSkillGroupUseCase;
        this.deactivateSkillGroupUseCase = deactivateSkillGroupUseCase;
    }

    @GetMapping
    public ResponseEntity<List<SkillResponse>> getSkills(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) SkillStatus status,
            @RequestParam(required = false) String keyword) {
        List<SkillResult> results = getSkillListUseCase.execute(groupId, status, keyword);
        return ResponseEntity.ok(results.stream().map(SkillResponse::fromResult).toList());
    }

    @PostMapping
    public ResponseEntity<SkillResponse> createSkill(@Valid @RequestBody CreateSkillRequest request) {
        SkillResult result = createSkillUseCase.execute(request.toCommand());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.id())
                .toUri();
        return ResponseEntity.created(location).body(SkillResponse.fromResult(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SkillResponse> updateSkill(
            @PathVariable @Positive(message = "ID phải là số dương") Long id,
            @Valid @RequestBody UpdateSkillRequest request) {
        SkillResult result = updateSkillUseCase.execute(request.toCommand(id));
        return ResponseEntity.ok(SkillResponse.fromResult(result));
    }

    @PostMapping("/merge")
    public ResponseEntity<SkillResponse> mergeSkills(@Valid @RequestBody MergeSkillRequest request) {
        SkillResult result = mergeSkillUseCase.execute(request.toCommand());
        return ResponseEntity.ok(SkillResponse.fromResult(result));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<SkillResponse> deactivateSkill(
            @PathVariable @Positive(message = "ID phải là số dương") Long id) {
        SkillResult result = deactivateSkillUseCase.execute(new DeactivateSkillCommand(id));
        return ResponseEntity.ok(SkillResponse.fromResult(result));
    }

    // =========================================================================
    // SKILL GROUPS ENDPOINTS
    // =========================================================================

    @GetMapping("/groups")
    public ResponseEntity<List<SkillGroupResponse>> getSkillGroups() {
        List<SkillGroupResult> results = getSkillGroupListUseCase.execute();
        return ResponseEntity.ok(results.stream().map(SkillGroupResponse::fromResult).toList());
    }

    @PostMapping("/groups")
    public ResponseEntity<SkillGroupResponse> createSkillGroup(@Valid @RequestBody CreateSkillGroupRequest request) {
        SkillGroupResult result = createSkillGroupUseCase.execute(request.toCommand());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.id())
                .toUri();
        return ResponseEntity.created(location).body(SkillGroupResponse.fromResult(result));
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<SkillGroupResponse> updateSkillGroup(
            @PathVariable @Positive(message = "ID phải là số dương") Long id,
            @Valid @RequestBody UpdateSkillGroupRequest request) {
        SkillGroupResult result = updateSkillGroupUseCase.execute(request.toCommand(id));
        return ResponseEntity.ok(SkillGroupResponse.fromResult(result));
    }

    @PatchMapping("/groups/{id}/deactivate")
    public ResponseEntity<SkillGroupResponse> deactivateSkillGroup(
            @PathVariable @Positive(message = "ID phải là số dương") Long id) {
        SkillGroupResult result = deactivateSkillGroupUseCase.execute(new DeactivateSkillGroupCommand(id));
        return ResponseEntity.ok(SkillGroupResponse.fromResult(result));
    }
}