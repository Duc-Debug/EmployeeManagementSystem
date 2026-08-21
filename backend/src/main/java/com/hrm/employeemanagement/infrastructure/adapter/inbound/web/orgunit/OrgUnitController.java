package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.*;
import com.hrm.employeemanagement.application.port.inbound.orgunit.*;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/org-units")
@Validated
public class OrgUnitController {

    private final CreateOrgUnitUseCase createOrgUnitUseCase;
    private final UpdateOrgUnitUseCase updateOrgUnitUseCase;
    private final MoveOrgUnitUseCase moveOrgUnitUseCase;
    private final DeactivateOrgUnitUseCase deactivateOrgUnitUseCase;
    private final GetOrgTreeUseCase getOrgTreeUseCase;

    public OrgUnitController(
            @Qualifier("transactionalCreateOrgUnitUseCase") CreateOrgUnitUseCase createOrgUnitUseCase,
            @Qualifier("transactionalUpdateOrgUnitUseCase") UpdateOrgUnitUseCase updateOrgUnitUseCase,
            @Qualifier("transactionalMoveOrgUnitUseCase") MoveOrgUnitUseCase moveOrgUnitUseCase,
            @Qualifier("transactionalDeactivateOrgUnitUseCase") DeactivateOrgUnitUseCase deactivateOrgUnitUseCase,
            @Qualifier("orgUnitService") GetOrgTreeUseCase getOrgTreeUseCase) {
        this.createOrgUnitUseCase = createOrgUnitUseCase;
        this.updateOrgUnitUseCase = updateOrgUnitUseCase;
        this.moveOrgUnitUseCase = moveOrgUnitUseCase;
        this.deactivateOrgUnitUseCase = deactivateOrgUnitUseCase;
        this.getOrgTreeUseCase = getOrgTreeUseCase;
    }

    @PostMapping
    public ResponseEntity<OrgUnitResponse> createUnit(@Valid @RequestBody CreateOrgUnitRequest request) {
        OrgUnitResult result = createOrgUnitUseCase.execute(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrgUnitResponse.fromResult(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrgUnitResponse> updateUnit(
            @PathVariable @Positive(message = "ID must be positive and greater than 0") Long id,
            @Valid @RequestBody UpdateOrgUnitRequest request) {
        OrgUnitResult result = updateOrgUnitUseCase.execute(request.toCommand(id));
        return ResponseEntity.ok(OrgUnitResponse.fromResult(result));
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<OrgUnitResponse> moveUnit(
            @PathVariable @Positive(message = "ID must be positive and greater than 0") Long id,
            @Valid @RequestBody MoveOrgUnitRequest request) {
        OrgUnitResult result = moveOrgUnitUseCase.execute(request.toCommand(id));
        return ResponseEntity.ok(OrgUnitResponse.fromResult(result));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<OrgUnitResponse> deactivateUnit(
            @PathVariable @Positive(message = "ID must be positive and greater than 0") Long id) {
        OrgUnitResult result = deactivateOrgUnitUseCase.execute(new DeactivateOrgUnitCommand(id));
        return ResponseEntity.ok(OrgUnitResponse.fromResult(result));
    }

    @GetMapping("/tree")
    public ResponseEntity<List<OrgUnitNodeResponse>> getOrgTree() {
        List<OrgUnitNodeResult> treeResult = getOrgTreeUseCase.execute();
        List<OrgUnitNodeResponse> response = treeResult.stream()
                .map(OrgUnitNodeResponse::fromResult)
                .toList();
        return ResponseEntity.ok(response);
    }
}