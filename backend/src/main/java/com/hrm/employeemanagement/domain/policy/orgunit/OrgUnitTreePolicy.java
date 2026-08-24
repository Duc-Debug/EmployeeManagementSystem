package com.hrm.employeemanagement.domain.policy.orgunit;

import java.util.Objects;

import com.hrm.employeemanagement.domain.exception.orgunit.CyclicDependencyException;
import com.hrm.employeemanagement.domain.exception.orgunit.InactiveParentException;
import com.hrm.employeemanagement.domain.exception.orgunit.InvalidTreePathException;
import com.hrm.employeemanagement.domain.exception.orgunit.NullOrgUnitIdException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;

public class OrgUnitTreePolicy {

    /**
     * Quy tắc BR-ORG-02: Kiểm tra không tạo vòng lặp khi di chuyển nút cây.
     * Nút cha mới không được là chính nút đang di chuyển hoặc là nút con/cháu thuộc
     * nhánh subtree của nó.
     */
    public void validateNoCycle(OrgUnit unitToMove, OrgUnit newParent) {
        if (unitToMove == null || newParent == null) {
            throw new IllegalArgumentException("Đơn vị cần di chuyển và đơn vị cha mới không được để trống.");
        }

        if (unitToMove.getId() == null || unitToMove.getId().getValue() == null) {
            throw new NullOrgUnitIdException("Mã định danh đơn vị cần di chuyển không được để trống.");
        }

        if (newParent.getId() == null || newParent.getId().getValue() == null) {
            throw new NullOrgUnitIdException("Mã định danh đơn vị cha mới không được để trống.");
        }

        if (unitToMove.getTreePath() == null || unitToMove.getTreePath().isBlank()) {
            throw new InvalidTreePathException("Đường dẫn cây của đơn vị cần di chuyển không được để trống.");
        }

        if (newParent.getTreePath() == null || newParent.getTreePath().isBlank()) {
            throw new InvalidTreePathException("Đường dẫn cây của đơn vị cha mới không được để trống.");
        }

        if (!unitToMove.getTreePath().endsWith("/") || !newParent.getTreePath().endsWith("/")) {
            throw new InvalidTreePathException("Đường dẫn cây phải bắt đầu và kết thúc bằng dấu gạch chéo '/'.");
        }

        // 1. Không được di chuyển nút vào chính nó
        if (Objects.equals(unitToMove.getId(), newParent.getId())) {
            throw new CyclicDependencyException("Một đơn vị không thể là nút cha của chính nó.");
        }

        // 2. Không được di chuyển nút cha vào làm con/cháu thuộc subtree của chính nó
        // Thuật toán Materialized Path: Mọi nút con/cháu của unitToMove đều có treePath
        // bắt đầu bằng treePath của unitToMove
        if (newParent.getTreePath().startsWith(unitToMove.getTreePath())) {
            throw new CyclicDependencyException(
                    "Không thể di chuyển một đơn vị cha vào bên trong một trong các nút con/cháu của chính nó.");
        }
    }

    /**
     * Business Rule BR-ORG-04: Nút cha phải ở trạng thái ACTIVE mới được nhận nút
     * con.
     */
    public void validateActiveParent(OrgUnit parentUnit) {
        Objects.requireNonNull(parentUnit, "Đơn vị cha không được phép là null");
        if (parentUnit.getStatus() != OrgUnitStatus.ACTIVE) {
            throw new InactiveParentException(
                    "Không thể gán hoặc di chuyển đơn vị dưới một đơn vị cha đang không hoạt động (ID: "
                            + parentUnit.getId().getValue() + ").");
        }
    }
}
