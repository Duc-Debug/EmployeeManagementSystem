package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.*;
import com.hrm.employeemanagement.application.port.inbound.orgunit.*;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import java.net.URI;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/org-units")
@Validated
public class OrgUnitController {

    private final CreateOrgUnitUseCase createOrgUnitUseCase;
    private final UpdateOrgUnitUseCase updateOrgUnitUseCase;
    private final MoveOrgUnitUseCase moveOrgUnitUseCase;
    private final DeactivateOrgUnitUseCase deactivateOrgUnitUseCase;
    private final ActivateOrgUnitUseCase activateOrgUnitUseCase;
    private final GetOrgTreeUseCase getOrgTreeUseCase;

    public OrgUnitController(
            @Qualifier("transactionalCreateOrgUnitUseCase") CreateOrgUnitUseCase createOrgUnitUseCase,
            @Qualifier("transactionalUpdateOrgUnitUseCase") UpdateOrgUnitUseCase updateOrgUnitUseCase,
            @Qualifier("transactionalMoveOrgUnitUseCase") MoveOrgUnitUseCase moveOrgUnitUseCase,
            @Qualifier("transactionalDeactivateOrgUnitUseCase") DeactivateOrgUnitUseCase deactivateOrgUnitUseCase,
            @Qualifier("transactionalActivateOrgUnitUseCase") ActivateOrgUnitUseCase activateOrgUnitUseCase,
            @Qualifier("orgUnitService") GetOrgTreeUseCase getOrgTreeUseCase) {
        this.createOrgUnitUseCase = createOrgUnitUseCase;
        this.updateOrgUnitUseCase = updateOrgUnitUseCase;
        this.moveOrgUnitUseCase = moveOrgUnitUseCase;
        this.deactivateOrgUnitUseCase = deactivateOrgUnitUseCase;
        this.activateOrgUnitUseCase = activateOrgUnitUseCase;
        this.getOrgTreeUseCase = getOrgTreeUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VT-06') or hasAuthority('ORG_UNIT_MANAGE')")
    public ResponseEntity<ApiResponse<OrgUnitResponse>> createUnit(@Valid @RequestBody CreateOrgUnitRequest request) {
        OrgUnitResult result = createOrgUnitUseCase.execute(request.toCommand());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.id())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.success("Tạo đơn vị thành công", OrgUnitResponse.fromResult(result)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('VT-06') or hasAuthority('ORG_UNIT_MANAGE')")
    public ResponseEntity<ApiResponse<OrgUnitResponse>> updateUnit(
            @PathVariable @Positive(message = "ID phải là số dương và lớn hơn 0.") Long id,
            @Valid @RequestBody UpdateOrgUnitRequest request) {
        OrgUnitResult result = updateOrgUnitUseCase.execute(request.toCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật đơn vị thành công", OrgUnitResponse.fromResult(result)));
    }

    @PatchMapping("/{id}/move")
    @PreAuthorize("hasAuthority('VT-06') or hasAuthority('ORG_UNIT_MANAGE')")
    public ResponseEntity<ApiResponse<OrgUnitResponse>> moveUnit(
            @PathVariable @Positive(message = "ID phải là số dương và lớn hơn 0.") Long id,
            @Valid @RequestBody MoveOrgUnitRequest request) {
        OrgUnitResult result = moveOrgUnitUseCase.execute(request.toCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Di chuyển đơn vị thành công", OrgUnitResponse.fromResult(result)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('VT-06') or hasAuthority('ORG_UNIT_MANAGE')")
    public ResponseEntity<ApiResponse<OrgUnitResponse>> deactivateUnit(
            @PathVariable @Positive(message = "ID phải là số dương và lớn hơn 0.") Long id) {
        OrgUnitResult result = deactivateOrgUnitUseCase.execute(new DeactivateOrgUnitCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Vô hiệu hóa đơn vị thành công", OrgUnitResponse.fromResult(result)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('VT-06') or hasAuthority('ORG_UNIT_MANAGE')")
    public ResponseEntity<ApiResponse<OrgUnitResponse>> activateUnit(
            @PathVariable @Positive(message = "ID phải là số dương và lớn hơn 0.") Long id) {
        OrgUnitResult result = activateOrgUnitUseCase.execute(new ActivateOrgUnitCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Kích hoạt đơn vị thành công", OrgUnitResponse.fromResult(result)));
    }

    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OrgUnitNodeResponse>>> getOrgTree() {
        List<OrgUnitNodeResult> treeResult = getOrgTreeUseCase.execute();
        List<OrgUnitNodeResponse> response = treeResult.stream()
                .map(OrgUnitNodeResponse::fromResult)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}