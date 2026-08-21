package com.hrm.employeemanagement.domain.policy.orgunit;

import java.util.Objects;

import com.hrm.employeemanagement.domain.exception.orgunit.CyclicDependencyException;
import com.hrm.employeemanagement.domain.exception.orgunit.InactiveParentException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;

public class OrgUnitTreePolicy {
    /**
     * Quy tắc BR-ORG-02: Kiểm tra không tạo vòng lặp khi di chuyển nút cây.
     * Nút cha mới không được là chính nút đang di chuyển hoặc nằm trong nhánh con
     * của nó.
     */
    public void validateNoCycle(OrgUnit unitToMove, OrgUnit newParent) {
        if (unitToMove == null || newParent == null) {
            throw new IllegalArgumentException("Unit to move and new parent cannot be null");
        }
        if (unitToMove.getId().equals(newParent.getId())) {
            throw new CyclicDependencyException("A unit cannot be its own parent node..");
        }
        String unitPathSegment = "/" + unitToMove.getId().getValue() + "/";
        if (newParent.getTreePath().contains(unitPathSegment)) {
            throw new CyclicDependencyException("It is not possible to move the parent unit inside its child branch.");
        }
    }

    /**
     * Business Rule BR-ORG-04: Nút cha phải ở trạng thái ACTIVE mới được nhận nút
     * con.
     */
    public void validateActiveParent(OrgUnit parentUnit) {
        Objects.requireNonNull(parentUnit, "Parent unit cannot be null");
        if (parentUnit.getStatus() != OrgUnitStatus.ACTIVE) {
            throw new InactiveParentException(
                    "Cannot assign or move unit under an inactive parent unit ID: " + parentUnit.getId().getValue());
        }
    }
}
