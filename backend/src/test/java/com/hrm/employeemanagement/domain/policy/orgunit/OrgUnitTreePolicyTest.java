package com.hrm.employeemanagement.domain.policy.orgunit;

import com.hrm.employeemanagement.domain.exception.orgunit.CyclicDependencyException;
import com.hrm.employeemanagement.domain.orgunit.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrgUnitTreePolicyTest {

    private OrgUnitTreePolicy policy;

    @BeforeEach
    void setUp() {
        policy = new OrgUnitTreePolicy();
    }

    @Test
    @DisplayName("Should throw CyclicDependencyException when moving unit to itself")
    void shouldThrowExceptionWhenMovingUnitToItself() {
        OrgUnit unit = createUnit(1L, "DEV-CENTER", "/1/", 1, null);

        assertThrows(CyclicDependencyException.class, () -> policy.validateNoCycle(unit, unit));
    }

    @Test
    @DisplayName("Should throw CyclicDependencyException when moving parent unit to descendant child")
    void shouldThrowExceptionWhenMovingParentToChild() {
        OrgUnit parent = createUnit(1L, "DEV-CENTER", "/1/", 1, null);
        OrgUnit descendantChild = createUnit(3L, "BE-TEAM", "/1/2/3/", 3, new OrgUnitId(2L));

        assertThrows(CyclicDependencyException.class, () -> policy.validateNoCycle(parent, descendantChild));
    }

    @Test
    @DisplayName("Should pass validation when moving to a valid parent in another branch")
    void shouldPassWhenMovingToValidNewParent() {
        OrgUnit unitToMove = createUnit(2L, "WEB-DEPT", "/1/2/", 2, new OrgUnitId(1L));
        OrgUnit validNewParent = createUnit(4L, "HR-CENTER", "/4/", 1, null);

        assertDoesNotThrow(() -> policy.validateNoCycle(unitToMove, validNewParent));
    }

    private OrgUnit createUnit(Long id, String code, String path, int level, OrgUnitId parentId) {
        return new OrgUnit(
                new OrgUnitId(id), code, "Unit " + code, OrgUnitType.DEPARTMENT,
                parentId, path, level, OrgUnitStatus.ACTIVE, null, null,
                LocalDateTime.now(), null
        );
    }
}
