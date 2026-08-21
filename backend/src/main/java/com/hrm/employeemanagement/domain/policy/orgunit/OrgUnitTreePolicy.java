package com.hrm.employeemanagement.domain.policy.orgunit;

import java.util.Objects;

import com.hrm.employeemanagement.domain.exception.orgunit.CyclicDependencyException;
import com.hrm.employeemanagement.domain.exception.orgunit.InactiveParentException;
import com.hrm.employeemanagement.domain.exception.orgunit.InvalidTreePathException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;

public class OrgUnitTreePolicy {

    /**
     * Quy tắc BR-ORG-02: Kiểm tra không tạo vòng lặp khi di chuyển nút cây.
     * Nút cha mới không được là chính nút đang di chuyển hoặc là nút con/cháu thuộc nhánh subtree của nó.
     */
    public void validateNoCycle(OrgUnit unitToMove, OrgUnit newParent) {
        if (unitToMove == null || newParent == null) {
            throw new IllegalArgumentException("Unit to move and new parent cannot be null");
        }

        if (unitToMove.getId() == null || unitToMove.getId().getValue() == null) {
            throw new IllegalArgumentException("Unit to move ID cannot be null");
        }

        if (newParent.getId() == null || newParent.getId().getValue() == null) {
            throw new IllegalArgumentException("New parent ID cannot be null");
        }

        if (unitToMove.getTreePath() == null || unitToMove.getTreePath().isBlank()) {
            throw new InvalidTreePathException("Unit to move treePath cannot be null or blank");
        }

        if (newParent.getTreePath() == null || newParent.getTreePath().isBlank()) {
            throw new InvalidTreePathException("New parent treePath cannot be null or blank");
        }

        if (!unitToMove.getTreePath().endsWith("/") || !newParent.getTreePath().endsWith("/")) {
            throw new InvalidTreePathException("Tree path must start and end with a trailing slash '/'");
        }

        // 1. Không được di chuyển nút vào chính nó
        if (Objects.equals(unitToMove.getId(), newParent.getId())) {
            throw new CyclicDependencyException("A unit cannot be its own parent node.");
        }

        // 2. Không được di chuyển nút cha vào làm con/cháu thuộc subtree của chính nó
        // Thuật toán Materialized Path: Mọi nút con/cháu của unitToMove đều có treePath bắt đầu bằng treePath của unitToMove
        if (newParent.getTreePath().startsWith(unitToMove.getTreePath())) {
            throw new CyclicDependencyException("Cannot move a parent unit inside one of its own descendant nodes.");
        }
    }

    /**
     * Business Rule BR-ORG-04: Nút cha phải ở trạng thái ACTIVE mới được nhận nút con.
     */
    public void validateActiveParent(OrgUnit parentUnit) {
        Objects.requireNonNull(parentUnit, "Parent unit cannot be null");
        if (parentUnit.getStatus() != OrgUnitStatus.ACTIVE) {
            throw new InactiveParentException(
                    "Cannot assign or move unit under an inactive parent unit ID: " + parentUnit.getId().getValue());
        }
    }
}
