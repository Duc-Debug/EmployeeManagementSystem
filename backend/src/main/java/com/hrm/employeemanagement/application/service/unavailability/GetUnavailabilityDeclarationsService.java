package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;
import com.hrm.employeemanagement.application.port.inbound.unavailability.GetDepartmentUnavailabilityDeclarationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.GetMyUnavailabilityDeclarationsUseCase;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.LoadUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GetUnavailabilityDeclarationsService implements
        GetMyUnavailabilityDeclarationsUseCase,
        GetDepartmentUnavailabilityDeclarationsUseCase {

    private final LoadUnavailabilityDeclarationPort loadUnavailabilityPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final AuthorizationService authorizationService;

    public GetUnavailabilityDeclarationsService(
            LoadUnavailabilityDeclarationPort loadUnavailabilityPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            AuthorizationService authorizationService
    ) {
        this.loadUnavailabilityPort = Objects.requireNonNull(loadUnavailabilityPort, "loadUnavailabilityPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.loadOrgUnitPort = loadOrgUnitPort;
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
    }

    @Override
    public List<UnavailabilityDeclarationResult> getMyDeclarations() {
        Long currentUserId = authorizationService.require(PermissionCode.UNAVAILABILITY_READ);

        Employee employee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElse(null);

        if (employee == null) {
            return List.of();
        }

        List<UnavailabilityDeclaration> declarations = loadUnavailabilityPort.findByEmployeeId(employee.getIdValue());
        return declarations.stream()
                .map(UnavailabilityDeclarationResult::fromDomain)
                .toList();
    }

    @Override
    public List<UnavailabilityDeclarationResult> getPendingDeclarations(Long requestedOrgUnitId) {
        Long currentUserId = authorizationService.require(PermissionCode.UNAVAILABILITY_APPROVE);

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        List<Long> targetBranchIds;
        switch (currentUser.getDataScope()) {
            case COMPANY -> {
                targetBranchIds = resolveBranchIds(requestedOrgUnitId);
            }
            case ORGANIZATION_BRANCH -> {
                if (currentUser.getScopeOrgUnitId() == null) {
                    throw new PermissionDeniedException(PermissionCode.UNAVAILABILITY_APPROVE);
                }
                if (requestedOrgUnitId != null) {
                    if (loadOrgUnitPort != null && !loadOrgUnitPort.existsInOrgUnitBranch(requestedOrgUnitId, currentUser.getScopeOrgUnitId())) {
                        throw new PermissionDeniedException(PermissionCode.UNAVAILABILITY_APPROVE);
                    }
                    targetBranchIds = resolveBranchIds(requestedOrgUnitId);
                } else {
                    targetBranchIds = resolveBranchIds(currentUser.getScopeOrgUnitId());
                }
            }
            case SELF -> {
                targetBranchIds = List.of();
            }
            default -> targetBranchIds = List.of();
        }

        List<UnavailabilityDeclaration> pending;
        if (targetBranchIds == null) {
            // DataScope là COMPANY và không lọc theo orgUnitId cụ thể -> Lấy toàn bộ đơn PENDING toàn công ty
            pending = loadUnavailabilityPort.findAllPending();
        } else if (targetBranchIds.isEmpty()) {
            pending = List.of();
        } else {
            pending = loadUnavailabilityPort.findPendingByOrgUnitIds(targetBranchIds);
        }

        return pending.stream()
                .map(UnavailabilityDeclarationResult::fromDomain)
                .toList();
    }

    private List<Long> resolveBranchIds(Long orgUnitId) {
        if (orgUnitId == null || loadOrgUnitPort == null) {
            return null;
        }
        Optional<OrgUnit> unitOpt = loadOrgUnitPort.findById(new OrgUnitId(orgUnitId));
        if (unitOpt.isPresent()) {
            return loadOrgUnitPort.findSubTree(unitOpt.get().getTreePath())
                    .stream()
                    .map(u -> u.getId().getValue())
                    .toList();
        }
        return List.of(orgUnitId);
    }
}
