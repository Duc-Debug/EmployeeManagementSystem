package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.DuplicateEmployeeCodeException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.DuplicateUsernameException;
import com.hrm.employeemanagement.domain.exception.user.InvalidCredentialsException;
import com.hrm.employeemanagement.domain.exception.user.UserAlreadyLockedException;
import com.hrm.employeemanagement.domain.exception.user.UserLockedException;
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

class UserExceptionHandlerTest {

    private UserExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new UserExceptionHandler();
    }

    @Test
    @DisplayName("Ánh xạ OrgUnitNotFoundException thành HTTP 404 NOT FOUND")
    void testHandleOrgUnitNotFound() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleOrgUnitNotFound(
                new OrgUnitNotFoundException("Organizational unit not found with ID: 10")
        );
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Organizational unit not found with ID: 10", response.getBody().getMessage());
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
    @DisplayName("Ánh xạ DuplicateEmployeeCodeException thành HTTP 409 CONFLICT")
    void testHandleDuplicateEmployeeCode() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleDuplicateEmployeeCode(
                new DuplicateEmployeeCodeException("EMP-999")
        );
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Mã nhân viên 'EMP-999' đã tồn tại trong hệ thống", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Ánh xạ InvalidCredentialsException thành HTTP 401 UNAUTHORIZED")
    void testHandleInvalidCredentials() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleInvalidCredentials(
                new InvalidCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác")
        );
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Tên đăng nhập hoặc mật khẩu không chính xác", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Ánh xạ UserLockedException thành HTTP 403 FORBIDDEN")
    void testHandleUserLocked() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleUserLocked(
                new UserLockedException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên.")
        );
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Ánh xạ PermissionDeniedException thành HTTP 403 FORBIDDEN")
    void testHandlePermissionDenied_Returns403() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handlePermissionDenied(
                new PermissionDeniedException(PermissionCode.USER_READ)
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Không có quyền thực hiện thao tác: USER_READ", response.getBody().getMessage());
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
    @DisplayName("Ánh xạ DataIntegrityViolationException vi phạm FOREIGN KEY org_unit_id thành HTTP 400 BAD REQUEST")
    void testHandleDataIntegrityViolation_ForeignKeyOrgUnit_Returns400() {
        SQLException sqlEx = new SQLException("Referential integrity constraint violation: FK_EMPLOYEES_ORG_UNIT FOREIGN KEY(ORG_UNIT_ID) REFERENCES ORG_UNITS(ID)");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("could not execute statement", sqlEx);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleDataIntegrityViolation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Đơn vị tổ chức được chỉ định không tồn tại trong hệ thống", response.getBody().getMessage());
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

    @Test
    @DisplayName("Ánh xạ Generic Exception thành HTTP 500 với thông điệp an toàn, không làm lộ chi tiết hệ thống / SQL")
    void testHandleGenericException_MasksSensitiveDetails_Returns500() {
        RuntimeException sensitiveEx = new RuntimeException("SELECT * FROM users WHERE password_hash = 'secret' failed: table corruption at /var/lib/mysql");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGenericException(sensitiveEx);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        // Không được làm lộ nội dung truy vấn SQL hoặc đường dẫn hệ thống nhạy cảm
        assertFalse(response.getBody().getMessage().contains("SELECT * FROM users"));
        assertFalse(response.getBody().getMessage().contains("/var/lib/mysql"));
        // Phải trả về thông điệp tổng quát an toàn
        assertEquals("Đã xảy ra lỗi hệ thống. Vui lòng liên hệ quản trị viên hoặc thử lại sau.", response.getBody().getMessage());
    }
}
