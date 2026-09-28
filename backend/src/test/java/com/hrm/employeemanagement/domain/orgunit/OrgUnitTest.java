package com.hrm.employeemanagement.domain.orgunit;

import com.hrm.employeemanagement.domain.exception.orgunit.InvalidOrgUnitManagerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrgUnitTest {

    @Test
    @DisplayName("Should create OrgUnit successfully with null managerId")
    void shouldCreateSuccessfullyWithNullManagerId() {
        OrgUnit unit = new OrgUnit(
                new OrgUnitId(2L), "DEV", "Phòng Dev", OrgUnitType.DEPARTMENT,
                new OrgUnitId(1L), "/1/2/", 2, OrgUnitStatus.ACTIVE, "Mô tả", null, LocalDateTime.now(), null
        );
        assertNull(unit.getManagerId());
    }

    @Test
    @DisplayName("Should throw InvalidOrgUnitManagerException when managerId is invalid (<= 0)")
    void shouldThrowExceptionWhenAssignInvalidManagerId() {
        OrgUnit unit = new OrgUnit(
                new OrgUnitId(1L), "DEV", "Phòng Dev", OrgUnitType.DEPARTMENT,
                null, "/1/", 1, OrgUnitStatus.ACTIVE, "Mô tả", 10L, LocalDateTime.now(), null
        );

        assertThrows(InvalidOrgUnitManagerException.class, () -> unit.assignManager(-1L));
        assertThrows(InvalidOrgUnitManagerException.class, () -> unit.assignManager(0L));
    }

    @Test
    @DisplayName("Should update OrgUnit info successfully with null managerId")
    void shouldUpdateInfoSuccessfullyWithNullManagerId() {
        OrgUnit unit = new OrgUnit(
                new OrgUnitId(1L), "DEV", "Phòng Dev", OrgUnitType.DEPARTMENT,
                null, "/1/", 1, OrgUnitStatus.ACTIVE, "Mô tả", 10L, LocalDateTime.now(), null
        );

        assertDoesNotThrow(() -> unit.updateInfo("Phòng Mới", OrgUnitType.CENTER, null, "Mô tả mới"));
        assertNull(unit.getManagerId());
    }
}