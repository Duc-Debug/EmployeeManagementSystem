package com.hrm.employeemanagement.application.service.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.*;
import com.hrm.employeemanagement.application.port.inbound.orgunit.*;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.SaveOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.orgunit.DuplicateUnitCodeException;
import com.hrm.employeemanagement.domain.exception.orgunit.InvalidOrgUnitManagerException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.*;
import com.hrm.employeemanagement.domain.policy.orgunit.OrgUnitTreePolicy;

import java.time.LocalDateTime;
import java.util.*;

public class OrgUnitService implements
        CreateOrgUnitUseCase,
        UpdateOrgUnitUseCase,
        MoveOrgUnitUseCase,
        DeactivateOrgUnitUseCase,
        GetOrgTreeUseCase {
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final SaveOrgUnitPort saveOrgUnitPort;
    private final LoadEmployeePort loadEmployeePort;
    private final OrgUnitTreePolicy orgUnitTreePolicy;
    private final SaveAuditLogPort saveAuditLogPort;
    private final CurrentUserPort currentUserPort;

    public OrgUnitService(LoadOrgUnitPort loadOrgUnitPort, SaveOrgUnitPort saveOrgUnitPort,
            LoadEmployeePort loadEmployeePort, SaveAuditLogPort saveAuditLogPort,
            CurrentUserPort currentUserPort) {
        this.loadOrgUnitPort = loadOrgUnitPort;
        this.saveOrgUnitPort = saveOrgUnitPort;
        this.loadEmployeePort = loadEmployeePort;
        this.orgUnitTreePolicy = new OrgUnitTreePolicy();
        this.saveAuditLogPort = saveAuditLogPort;
        this.currentUserPort = currentUserPort;
    }

    private Long getCurrentUserId() {
        return currentUserPort != null ? currentUserPort.getCurrentUserId().orElse(null) : null;
    }

    private void validateActiveManager(Long managerId) {
        if (loadEmployeePort != null) {
            Employee manager = loadEmployeePort.findById(new EmployeeId(managerId))
                    .orElseThrow(() -> new EmployeeNotFoundException(
                            "Không tìm thấy nhân viên quản lý với ID: " + managerId));
            if (manager.getStatus() != EmployeeStatus.ACTIVE) {
                throw new InvalidOrgUnitManagerException(
                        "Nhân viên quản lý (ID: " + managerId + ") hiện không ở trạng thái hoạt động.");
            }
        }
    }

    @Override
    public OrgUnitResult execute(CreateOrgUnitCommand command) {
        // BR-ORG-01: Check unique unit code
        if (loadOrgUnitPort.existsByUnitCode(command.unitCode())) {
            throw new DuplicateUnitCodeException("Mã đơn vị '" + command.unitCode() + "' đã tồn tại trong hệ thống");
        }

        // Validate business reference: Manager must exist and be ACTIVE
        validateActiveManager(command.managerId());

        OrgUnitId parentId = null;
        String parentTreePath = "/";
        int level = 1;
        if (command.parentId() != null) {
            OrgUnit parent = loadOrgUnitPort.findById(new OrgUnitId(command.parentId()))
                    .orElseThrow(
                            () -> new OrgUnitNotFoundException("Không tìm thấy đơn vị cha với ID: " + command.parentId()));
            orgUnitTreePolicy.validateActiveParent(parent);
            parentId = parent.getId();
            parentTreePath = parent.getTreePath();
            level = parent.getLevel() + 1;
        }
        OrgUnit newUnit = new OrgUnit(
                null,
                command.unitCode(),
                command.unitName(),
                command.unitType(),
                parentId,
                parentTreePath,
                level,
                OrgUnitStatus.ACTIVE,
                command.description(),
                command.managerId(),
                LocalDateTime.now(),
                null);
        OrgUnit savedUnit = saveOrgUnitPort.save(newUnit);
        saveAuditLogPort.save(
                AuditLog.create(getCurrentUserId(), "CREATE_ORG_UNIT", "org_units", savedUnit.getId().getValue()));
        return toResult(savedUnit);
    }

    @Override
    public OrgUnitResult execute(UpdateOrgUnitCommand command) {
        OrgUnit unit = loadOrgUnitPort.findById(new OrgUnitId(command.id()))
                .orElseThrow(
                        () -> new OrgUnitNotFoundException("Không tìm thấy đơn vị tổ chức với ID: " + command.id()));

        // Validate business reference: Manager must exist and be ACTIVE
        validateActiveManager(command.managerId());

        unit.updateInfo(command.unitName(), command.unitType(), command.managerId(), command.description());
        OrgUnit savedUnit = saveOrgUnitPort.save(unit);
        saveAuditLogPort.save(
                AuditLog.create(getCurrentUserId(), "UPDATE_ORG_UNIT", "org_units", savedUnit.getId().getValue()));

        return toResult(savedUnit);
    }

    @Override
    public OrgUnitResult execute(MoveOrgUnitCommand command) {
        OrgUnit unitToMove = loadOrgUnitPort.findById(new OrgUnitId(command.id()))
                .orElseThrow(
                        () -> new OrgUnitNotFoundException("Không tìm thấy đơn vị tổ chức với ID: " + command.id()));

        OrgUnit newParent = loadOrgUnitPort.findById(new OrgUnitId(command.newParentId()))
                .orElseThrow(() -> new OrgUnitNotFoundException(
                        "Không tìm thấy đơn vị cha mới với ID: " + command.newParentId()));

        // BR-ORG-04: Active parent validation
        orgUnitTreePolicy.validateActiveParent(newParent);

        // BR-ORG-02: Non-cyclic graph check
        orgUnitTreePolicy.validateNoCycle(unitToMove, newParent);

        String oldTreePath = unitToMove.getTreePath();
        int oldLevel = unitToMove.getLevel() != null ? unitToMove.getLevel() : 1;

        String newTreePath = newParent.getTreePath() + unitToMove.getId().getValue() + "/";
        int newLevel = newParent.getLevel() + 1;
        int levelDelta = newLevel - oldLevel;

        // Cập nhật nút cha và đường dẫn của nút hiện tại
        unitToMove.changeParent(newParent.getId(), newTreePath, newLevel);
        OrgUnit savedUnit = saveOrgUnitPort.save(unitToMove);

        // Bulk UPDATE 1 câu SQL duy nhất cho toàn bộ các nút con/cháu thuộc subtree
        saveOrgUnitPort.updateSubTreePaths(oldTreePath, newTreePath, levelDelta);

        saveAuditLogPort
                .save(AuditLog.create(getCurrentUserId(), "MOVE_ORG_UNIT", "org_units", savedUnit.getId().getValue()));
        return toResult(savedUnit);
    }

    @Override
    public OrgUnitResult execute(DeactivateOrgUnitCommand command) {
        OrgUnit unit = loadOrgUnitPort.findById(new OrgUnitId(command.id()))
                .orElseThrow(
                        () -> new OrgUnitNotFoundException("Không tìm thấy đơn vị tổ chức với ID: " + command.id()));

        // 1. Deactivate nút cha được chọn
        unit.deactivate();
        OrgUnit savedUnit = saveOrgUnitPort.save(unit);

        // 2. Cascading Deactivation: Bulk UPDATE 1 câu SQL duy nhất vô hiệu hóa toàn bộ các nút con/cháu thuộc nhánh subtree này
        saveOrgUnitPort.deactivateSubTree(unit.getTreePath());

        saveAuditLogPort.save(
                AuditLog.create(getCurrentUserId(), "DEACTIVATE_ORG_UNIT", "org_units", savedUnit.getId().getValue()));
        return toResult(savedUnit);
    }

    @Override
    public List<OrgUnitNodeResult> execute() {
        List<OrgUnit> allUnits = loadOrgUnitPort.findAllActive();
        return buildTreeHierarchy(allUnits);
    }

    private OrgUnitResult toResult(OrgUnit unit) {
        return new OrgUnitResult(
                unit.getId() != null ? unit.getId().getValue() : null,
                unit.getUnitCode(),
                unit.getUnitName(),
                unit.getUnitType(),
                unit.getParentId() != null ? unit.getParentId().getValue() : null,
                unit.getTreePath(),
                unit.getLevel(),
                unit.getStatus(),
                unit.getDescription(),
                unit.getManagerId(),
                unit.getCreatedAt(),
                unit.getUpdatedAt());
    }

    private List<OrgUnitNodeResult> buildTreeHierarchy(List<OrgUnit> units) {
        Map<Long, OrgUnitNodeResult> nodeMap = new HashMap<>();
        List<OrgUnitNodeResult> rootNodes = new ArrayList<>();
        for (OrgUnit u : units) {
            Long id = u.getId() != null ? u.getId().getValue() : null;
            OrgUnitNodeResult node = new OrgUnitNodeResult(
                    id,
                    u.getUnitCode(),
                    u.getUnitName(),
                    u.getUnitType(),
                    u.getParentId() != null ? u.getParentId().getValue() : null,
                    u.getTreePath(),
                    u.getLevel(),
                    u.getStatus(),
                    u.getDescription(),
                    u.getManagerId(),
                    new ArrayList<>());
            if (id != null) {
                nodeMap.put(id, node);
            }
        }
        for (OrgUnit u : units) {
            Long id = u.getId() != null ? u.getId().getValue() : null;
            Long parentId = u.getParentId() != null ? u.getParentId().getValue() : null;
            OrgUnitNodeResult currentNode = nodeMap.get(id);
            if (parentId == null || !nodeMap.containsKey(parentId)) {
                rootNodes.add(currentNode);
            } else {
                OrgUnitNodeResult parentNode = nodeMap.get(parentId);
                parentNode.children().add(currentNode);
            }
        }
        return rootNodes;
    }
}