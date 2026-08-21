package com.hrm.employeemanagement.domain.policy.orgunit;

import com.hrm.employeemanagement.domain.exception.orgunit.CyclicDependencyException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;

public class OrgUnitTreePolicy {
    /**
     * Quy tắc BR-ORG-02: Kiểm tra không tạo vòng lặp khi di chuyển nút cây.
     * Nút cha mới không được là chính nút đang di chuyển hoặc nằm trong nhánh con của nó.
     */
    public void validateNoCycle(OrgUnit unitToMove, OrgUnit newParent) {
        if (unitToMove.getId().equals(newParent.getId())) {
            throw new CyclicDependencyException("A unit cannot be its own parent node..");
        }
        String unitPathSegment = "/" + unitToMove.getId().getValue() + "/";
        if (newParent.getTreePath().contains(unitPathSegment)) {
            throw new CyclicDependencyException("It is not possible to move the parent unit inside its child branch.");
        }
    }
}
