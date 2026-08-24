package com.hrm.employeemanagement.application.dto.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateUserCommandTest {

    @Test
    @DisplayName("Khởi tạo CreateUserCommand thành công khi đầy đủ thông tin")
    void testValidCommand() {
        CreateUserCommand command = new CreateUserCommand(
                "john_doe", "password123", "VT-04", "EMP-001", "John Doe", 10L
        );

        assertEquals("john_doe", command.username());
        assertEquals("password123", command.password());
        assertEquals("VT-04", command.roleCode());
        assertEquals("EMP-001", command.employeeCode());
        assertEquals("John Doe", command.fullName());
        assertEquals(10L, command.orgUnitId());
    }

    @Test
    @DisplayName("Báo lỗi khi username bị null hoặc rỗng")
    void testBlankUsername_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CreateUserCommand("", "password123", "VT-04", "EMP-001", "John Doe", 10L);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new CreateUserCommand(null, "password123", "VT-04", "EMP-001", "John Doe", 10L);
        });
    }

    @Test
    @DisplayName("Báo lỗi khi password bị null hoặc rỗng")
    void testBlankPassword_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CreateUserCommand("john_doe", "", "VT-04", "EMP-001", "John Doe", 10L);
        });
    }

    @Test
    @DisplayName("Báo lỗi khi orgUnitId bị null")
    void testNullOrgUnitId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CreateUserCommand("john_doe", "password123", "VT-04", "EMP-001", "John Doe", null);
        });
    }
}
