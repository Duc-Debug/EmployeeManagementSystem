package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import com.hrm.employeemanagement.domain.exception.user.DuplicateUsernameException;
import com.hrm.employeemanagement.domain.exception.user.UserAlreadyLockedException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Ánh xạ UserNotFoundException thành HTTP 404 NOT FOUND")
    void testHandleUserNotFound() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleUserNotFound(
                new UserNotFoundException("Không tìm thấy người dùng")
        );
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Không tìm thấy người dùng", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Ánh xạ DuplicateUsernameException thành HTTP 409 CONFLICT")
    void testHandleDuplicateUsername() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleDuplicateUsername(
                new DuplicateUsernameException("Tên đăng nhập đã tồn tại")
        );
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Tên đăng nhập đã tồn tại", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Ánh xạ DataIntegrityViolationException vi phạm UNIQUE username thành HTTP 409 CONFLICT")
    void testHandleDataIntegrityViolation_UniqueUsername_Returns409() {
        SQLException sqlEx = new SQLException("Unique index or primary key violation: CONSTRAINT_INDEX_4 ON USERS(USERNAME)");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("could not execute statement", sqlEx);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleDataIntegrityViolation(ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Tên đăng nhập đã tồn tại trong hệ thống", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Ánh xạ DataIntegrityViolationException vi phạm UNIQUE employee_code thành HTTP 409 CONFLICT")
    void testHandleDataIntegrityViolation_UniqueEmployeeCode_Returns409() {
        SQLException sqlEx = new SQLException("Unique index or primary key violation: CONSTRAINT_INDEX_5 ON EMPLOYEES(EMPLOYEE_CODE)");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("could not execute statement", sqlEx);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleDataIntegrityViolation(ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Mã nhân viên đã tồn tại trong hệ thống", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Ánh xạ DataIntegrityViolationException vi phạm FOREIGN KEY department_id thành HTTP 400 BAD REQUEST")
    void testHandleDataIntegrityViolation_ForeignKeyDepartment_Returns400() {
        SQLException sqlEx = new SQLException("Referential integrity constraint violation: FK_EMPLOYEES_DEPARTMENT FOREIGN KEY(DEPARTMENT_ID) REFERENCES DEPARTMENTS(ID)");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("could not execute statement", sqlEx);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleDataIntegrityViolation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Phòng ban được chỉ định không tồn tại trong hệ thống", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Ánh xạ ObjectOptimisticLockingFailureException thành HTTP 409 CONFLICT")
    void testHandleOptimisticLocking_Returns409() {
        ObjectOptimisticLockingFailureException ex = new ObjectOptimisticLockingFailureException("User", 1L);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleOptimisticLocking(ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertNotNull(response.getBody().getMessage());
    }

    @Test
    @DisplayName("Ánh xạ BusinessRuleViolation (ví dụ UserAlreadyLockedException) thành HTTP 400 BAD REQUEST")
    void testHandleBusinessRuleViolation_Returns400() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessRuleViolation(
                new UserAlreadyLockedException("Tài khoản này hiện đã bị khóa")
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Tài khoản này hiện đã bị khóa", response.getBody().getMessage());
    }
}
