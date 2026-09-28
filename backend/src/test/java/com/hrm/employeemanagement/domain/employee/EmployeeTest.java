package com.hrm.employeemanagement.domain.employee;

import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmployeeTest {

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = Employee.createNew(
                new UserId(10L),
                5L,
                "EMP-001",
                "Nguyen Van A"
        );
    }

    @Test
    @DisplayName("updateUserAccountDetails khong thay doi orgUnitId khi truyen null")
    void testUpdateUserAccountDetails_NullOrgUnitId_PreservesExistingOrgUnitId() {
        // Act
        employee.updateUserAccountDetails("Nguyen Van B", "EMP-002", null);

        // Assert
        assertEquals(5L, employee.getOrgUnitId(), "orgUnitId cu phai duoc bao toan khi truyen null");
        assertEquals("Nguyen Van B", employee.getFullName());
        assertEquals("EMP-002", employee.getEmployeeCode());
    }

    @Test
    @DisplayName("updateUserAccountDetails cap nhat orgUnitId moi khi truyen gia tri hop le")
    void testUpdateUserAccountDetails_NonNullOrgUnitId_UpdatesOrgUnitId() {
        // Act
        employee.updateUserAccountDetails("Nguyen Van B", "EMP-002", 99L);

        // Assert
        assertEquals(99L, employee.getOrgUnitId(), "orgUnitId moi phai duoc cap nhat");
        assertEquals("Nguyen Van B", employee.getFullName());
        assertEquals("EMP-002", employee.getEmployeeCode());
    }

    @Test
    @DisplayName("updateUserAccountDetails giu nguyen fullName va employeeCode khi truyen null hoac blank")
    void testUpdateUserAccountDetails_NullOrBlankFields_PreservesExistingValues() {
        // Act
        employee.updateUserAccountDetails(null, "   ", 88L);

        // Assert
        assertEquals("Nguyen Van A", employee.getFullName(), "fullName cu phai duoc bao toan");
        assertEquals("EMP-001", employee.getEmployeeCode(), "employeeCode cu phai duoc bao toan");
        assertEquals(88L, employee.getOrgUnitId(), "orgUnitId moi phai duoc cap nhat");
    }
}