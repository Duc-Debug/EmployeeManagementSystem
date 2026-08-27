package com.hrm.employeemanagement.application.dto.orgunit;

import com.hrm.employeemanagement.domain.exception.orgunit.InvalidOrgUnitManagerException;
import com.hrm.employeemanagement.domain.exception.orgunit.RequiredFieldMissingException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateOrgUnitCommandTest {

    @Test
    @DisplayName("Should create valid CreateOrgUnitCommand and trim inputs")
    void shouldCreateValidCommand() {
        CreateOrgUnitCommand command = new CreateOrgUnitCommand(
                " dev-center ", " Khối Phát Triển ", OrgUnitType.CENTER, 1L, 10L, " Mô tả "
        );

        assertEquals("DEV-CENTER", command.unitCode());
        assertEquals("Khối Phát Triển", command.unitName());
        assertEquals("Mô tả", command.description());
    }

    @Test
    @DisplayName("Should throw RequiredFieldMissingException when unitCode, unitName, or unitType is missing")
    void shouldThrowExceptionWhenRequiredFieldIsMissing() {
        assertThrows(RequiredFieldMissingException.class, () -> new CreateOrgUnitCommand(
                "", "Khối Phát Triển", OrgUnitType.CENTER, 1L, 10L, null
        ));
        assertThrows(RequiredFieldMissingException.class, () -> new CreateOrgUnitCommand(
                "DEV-CENTER", "   ", OrgUnitType.CENTER, 1L, 10L, null
        ));
        assertThrows(RequiredFieldMissingException.class, () -> new CreateOrgUnitCommand(
                "DEV-CENTER", "Khối Phát Triển", null, 1L, 10L, null
        ));
    }

    @Test
    @DisplayName("Should create valid CreateOrgUnitCommand with null managerId")
    void shouldCreateValidCommandWithNullManagerId() {
        CreateOrgUnitCommand command = new CreateOrgUnitCommand(
                "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER, 1L, null, null
        );
        assertNull(command.managerId());
    }

    @Test
    @DisplayName("Should throw InvalidOrgUnitManagerException when managerId is invalid (<= 0)")
    void shouldThrowExceptionWhenManagerIdIsInvalid() {
        assertThrows(InvalidOrgUnitManagerException.class, () -> new CreateOrgUnitCommand(
                "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER, 1L, -1L, null
        ));
    }
}
