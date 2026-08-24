package com.hrm.employeemanagement.domain.policy.orgunit;

import com.hrm.employeemanagement.domain.exception.orgunit.CyclicDependencyException;
import com.hrm.employeemanagement.domain.exception.orgunit.NullOrgUnitIdException;
import com.hrm.employeemanagement.domain.orgunit.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrgUnitTreePolicyTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 1, 1, 0, 0);
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
    @DisplayName("Should throw CyclicDependencyException when moving parent unit to immediate child")
    void shouldThrowExceptionWhenMovingParentToImmediateChild() {
        OrgUnit parent = createUnit(1L, "DEV-CENTER", "/1/", 1, null);
        OrgUnit child = createUnit(2L, "WEB-DEPT", "/1/2/", 2, new OrgUnitId(1L));

        assertThrows(CyclicDependencyException.class, () -> policy.validateNoCycle(parent, child));
    }

    @Test
    @DisplayName("Should throw CyclicDependencyException when moving parent unit to deep descendant (4 levels)")
    void shouldThrowExceptionWhenMovingParentToDeepDescendant() {
        OrgUnit rootA = createUnit(1L, "ROOT-A", "/1/", 1, null);
        OrgUnit deepD = createUnit(4L, "TEAM-D", "/1/2/3/4/", 4, new OrgUnitId(3L));

        assertThrows(CyclicDependencyException.class, () -> policy.validateNoCycle(rootA, deepD));
    }

    @Test
    @DisplayName("Should pass validation when moving unit to a sibling node in another branch")
    void shouldPassWhenMovingToSiblingNode() {
        // Company (1) -> A (2) -> B (3)
        // Company (1) -> C (4)
        OrgUnit unitB = createUnit(3L, "UNIT-B", "/1/2/3/", 3, new OrgUnitId(2L));
        OrgUnit unitC = createUnit(4L, "UNIT-C", "/1/4/", 2, new OrgUnitId(1L));

        assertDoesNotThrow(() -> policy.validateNoCycle(unitB, unitC));
    }

    @Test
    @DisplayName("Should pass boundary test when target node has similar prefix string (e.g. /1/2/ vs /1/20/)")
    void shouldPassBoundaryTestWithSimilarPrefixCode() {
        OrgUnit unit2 = createUnit(2L, "UNIT-2", "/1/2/", 2, new OrgUnitId(1L));
        OrgUnit unit20 = createUnit(20L, "UNIT-20", "/1/20/", 2, new OrgUnitId(1L));

        assertDoesNotThrow(() -> policy.validateNoCycle(unit2, unit20));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when parameters are null")
    void shouldThrowExceptionWhenParametersAreNull() {
        OrgUnit validUnit = createUnit(1L, "DEV-CENTER", "/1/", 1, null);

        assertThrows(IllegalArgumentException.class, () -> policy.validateNoCycle(null, validUnit));
        assertThrows(IllegalArgumentException.class, () -> policy.validateNoCycle(validUnit, null));
    }

    @Test
    @DisplayName("Should throw NullOrgUnitIdException when unitToMove or newParent has null ID")
    void shouldRejectUnitWithoutId() {
        OrgUnit unitWithNullId = createUnit(null, "DEV", "/1/", 1, null);
        OrgUnit parent = createUnit(2L, "HR", "/2/", 1, null);

        assertThrows(NullOrgUnitIdException.class, () -> policy.validateNoCycle(unitWithNullId, parent));
    }

    private OrgUnit createUnit(Long id, String code, String path, int level, OrgUnitId parentId) {
        return new OrgUnit(
                id != null ? new OrgUnitId(id) : null, code, "Unit " + code, OrgUnitType.DEPARTMENT,
                parentId, path, level, OrgUnitStatus.ACTIVE, null, 1L,
                FIXED_TIME, null
        );
    }
}