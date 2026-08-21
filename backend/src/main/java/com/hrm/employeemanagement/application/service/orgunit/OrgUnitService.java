package com.hrm.employeemanagement.application.service.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.*;
import com.hrm.employeemanagement.application.port.inbound.orgunit.*;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.SaveOrgUnitPort;
import com.hrm.employeemanagement.domain.exception.orgunit.DuplicateUnitCodeException;
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
    private final OrgUnitTreePolicy orgUnitTreePolicy;

    public OrgUnitService(LoadOrgUnitPort loadOrgUnitPort, SaveOrgUnitPort saveOrgUnitPort) {
        this.loadOrgUnitPort = loadOrgUnitPort;
        this.saveOrgUnitPort = saveOrgUnitPort;
        this.orgUnitTreePolicy = new OrgUnitTreePolicy();
    }

    @Override
    public OrgUnitResult execute(CreateOrgUnitCommand command) {
        // BR-ORG-01: Check unique unit code
        if (loadOrgUnitPort.existsByUnitCode(command.unitCode())) {
            throw new DuplicateUnitCodeException("Unit code '" + command.unitCode() + "' already exists");
        }
        OrgUnitId parentId = null;
        String parentTreePath = "/";
        int level = 1;
        if (command.parentId() != null) {
            OrgUnit parent = loadOrgUnitPort.findById(new OrgUnitId(command.parentId()))
                    .orElseThrow(
                            () -> new OrgUnitNotFoundException("Parent unit not found with ID: " + command.parentId()));
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
                null,
                LocalDateTime.now(),
                null);
        OrgUnit savedUnit = saveOrgUnitPort.save(newUnit);
        String finalTreePath = parentTreePath + savedUnit.getId().getValue() + "/";
        savedUnit.changeParent(parentId, finalTreePath, level);
        OrgUnit finalSavedUnit = saveOrgUnitPort.save(savedUnit);
        return toResult(finalSavedUnit);
    }

    @Override
    public OrgUnitResult execute(UpdateOrgUnitCommand command) {
        OrgUnit unit = loadOrgUnitPort.findById(new OrgUnitId(command.id()))
                .orElseThrow(
                        () -> new OrgUnitNotFoundException("Organizational unit not found with ID: " + command.id()));
        unit.updateInfo(command.unitName(), command.unitType(), command.description());
        OrgUnit savedUnit = saveOrgUnitPort.save(unit);
        return toResult(savedUnit);
    }

        @Override
    public OrgUnitResult execute(MoveOrgUnitCommand command) {
        OrgUnit unitToMove = loadOrgUnitPort.findById(new OrgUnitId(command.id()))
                .orElseThrow(() -> new OrgUnitNotFoundException("Organizational unit not found with ID: " + command.id()));

        OrgUnit newParent = loadOrgUnitPort.findById(new OrgUnitId(command.newParentId()))
                .orElseThrow(() -> new OrgUnitNotFoundException("New parent unit not found with ID: " + command.newParentId()));

        // BR-ORG-04: Active parent validation
        orgUnitTreePolicy.validateActiveParent(newParent);

        // BR-ORG-02: Non-cyclic graph check
        orgUnitTreePolicy.validateNoCycle(unitToMove, newParent);

        String oldTreePath = unitToMove.getTreePath();
        int oldLevel = unitToMove.getLevel() != null ? unitToMove.getLevel() : 1;

        String newTreePath = newParent.getTreePath() + unitToMove.getId().getValue() + "/";
        int newLevel = newParent.getLevel() + 1;
        int levelDelta = newLevel - oldLevel; // ✅ Lưu lại levelDelta chuẩn xác trước khi mutate

        // Mutate nút hiện tại
        unitToMove.changeParent(newParent.getId(), newTreePath, newLevel);
        OrgUnit savedUnit = saveOrgUnitPort.save(unitToMove);

        // Cập nhật đường dẫn và level chuẩn xác cho toàn bộ nút con/cháu
        List<OrgUnit> childUnits = loadOrgUnitPort.findSubTree(oldTreePath);
        for (OrgUnit child : childUnits) {
            if (!child.getId().equals(unitToMove.getId())) {
                String updatedChildPath = child.getTreePath().replace(oldTreePath, newTreePath);
                int updatedChildLevel = child.getLevel() + levelDelta; 
                child.changeParent(child.getParentId(), updatedChildPath, updatedChildLevel);
                saveOrgUnitPort.save(child);
            }
        }

        return toResult(savedUnit);
    }

    @Override
    public OrgUnitResult execute(DeactivateOrgUnitCommand command) {
        OrgUnit unit = loadOrgUnitPort.findById(new OrgUnitId(command.id()))
                .orElseThrow(
                        () -> new OrgUnitNotFoundException("Organizational unit not found with ID: " + command.id()));

        // 1. Deactivate nút cha được chọn
        unit.deactivate();
        OrgUnit savedUnit = saveOrgUnitPort.save(unit);

        // 2. Cascading Deactivation: Vô hiệu hóa dây chuyền toàn bộ các nút con/cháu
        // thuộc nhánh treePath này
        List<OrgUnit> subTreeUnits = loadOrgUnitPort.findSubTree(unit.getTreePath());
        for (OrgUnit child : subTreeUnits) {
            if (!child.getId().equals(unit.getId()) && child.getStatus() == OrgUnitStatus.ACTIVE) {
                child.deactivate();
                saveOrgUnitPort.save(child);
            }
        }

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