package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common;

import com.hrm.employeemanagement.domain.exception.DomainException;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationOverloadWarningException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeVersionConflictException;
import com.hrm.employeemanagement.domain.exception.orgunit.CyclicDependencyException;
import com.hrm.employeemanagement.domain.exception.orgunit.DuplicateUnitCodeException;
import com.hrm.employeemanagement.domain.exception.orgunit.InactiveParentException;
import com.hrm.employeemanagement.domain.exception.orgunit.InvalidOrgUnitManagerException;
import com.hrm.employeemanagement.domain.exception.orgunit.InvalidTreePathException;
import com.hrm.employeemanagement.domain.exception.orgunit.NullOrgUnitIdException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.orgunit.RequiredFieldMissingException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.leave.DuplicateLeaveRequestException;
import com.hrm.employeemanagement.domain.exception.leave.InvalidLeaveDateRangeException;
import com.hrm.employeemanagement.domain.exception.leave.LeaveBalanceExceededException;
import com.hrm.employeemanagement.domain.exception.skill.EmployeeSkillNotFoundException;
import com.hrm.employeemanagement.domain.exception.skill.SkillNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Handle OrgUnitNotFoundException (404 NOT FOUND)
    @ExceptionHandler(OrgUnitNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OrgUnitNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "ORG_UNIT_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 1.1 Handle EmployeeNotFoundException (404 NOT FOUND)
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFound(EmployeeNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "EMPLOYEE_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "USER_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(SkillNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSkillNotFound(SkillNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "SKILL_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(EmployeeSkillNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeSkillNotFound(EmployeeSkillNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "EMPLOYEE_SKILL_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({EmployeeVersionConflictException.class,
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(Exception ex) {
        ErrorResponse response = ErrorResponse.of(
                "EMPLOYEE_VERSION_CONFLICT",
                "Hồ sơ nhân sự đã được cập nhật bởi người dùng khác. Vui lòng tải lại dữ liệu và thử lại.",
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePermissionDenied(PermissionDeniedException ex) {
        ErrorResponse response = ErrorResponse.of(
                "FORBIDDEN",
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 2. Handle DuplicateUnitCodeException (409 CONFLICT)
    @ExceptionHandler(DuplicateUnitCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCode(DuplicateUnitCodeException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_UNIT_CODE",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.user.DuplicateUsernameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUsername(com.hrm.employeemanagement.domain.exception.user.DuplicateUsernameException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_USERNAME",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.user.DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(com.hrm.employeemanagement.domain.exception.user.DuplicateEmailException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_EMAIL",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.calendar.DuplicateHolidayException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateHoliday(com.hrm.employeemanagement.domain.exception.calendar.DuplicateHolidayException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_HOLIDAY",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.calendar.HolidayNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleHolidayNotFound(com.hrm.employeemanagement.domain.exception.calendar.HolidayNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "HOLIDAY_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.calendar.InvalidWorkingCalendarException.class)
    public ResponseEntity<ErrorResponse> handleInvalidWorkingCalendar(com.hrm.employeemanagement.domain.exception.calendar.InvalidWorkingCalendarException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_WORKING_CALENDAR",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 3. Handle CyclicDependencyException (400 BAD REQUEST)
    @ExceptionHandler(CyclicDependencyException.class)
    public ResponseEntity<ErrorResponse> handleCyclicDependency(CyclicDependencyException ex) {
        ErrorResponse response = ErrorResponse.of(
                "CYCLIC_DEPENDENCY_ERROR",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 4. Handle InactiveParentException (400 BAD REQUEST)
    @ExceptionHandler(InactiveParentException.class)
    public ResponseEntity<ErrorResponse> handleInactiveParent(InactiveParentException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INACTIVE_PARENT_UNIT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 5. Handle InvalidTreePathException (400 BAD REQUEST)
    @ExceptionHandler(InvalidTreePathException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTreePath(InvalidTreePathException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_TREE_PATH",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 6. Handle NullOrgUnitIdException (400 BAD REQUEST)
    @ExceptionHandler(NullOrgUnitIdException.class)
    public ResponseEntity<ErrorResponse> handleNullOrgUnitId(NullOrgUnitIdException ex) {
        ErrorResponse response = ErrorResponse.of(
                "NULL_ORG_UNIT_ID",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 7. Handle InvalidOrgUnitManagerException (400 BAD REQUEST)
    @ExceptionHandler(InvalidOrgUnitManagerException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrgUnitManager(InvalidOrgUnitManagerException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_ORG_UNIT_MANAGER",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 8. Handle RequiredFieldMissingException (400 BAD REQUEST)
    @ExceptionHandler(RequiredFieldMissingException.class)
    public ResponseEntity<ErrorResponse> handleRequiredFieldMissing(RequiredFieldMissingException ex) {
        ErrorResponse response = ErrorResponse.of(
                "REQUIRED_FIELD_MISSING",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handle AllocationOverloadWarningException (400 BAD REQUEST kèm chi tiết số giờ vượt)
    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.allocation.AllocationOverloadWarningException.class)
    public ResponseEntity<ErrorResponse> handleAllocationOverloadWarning(com.hrm.employeemanagement.domain.exception.allocation.AllocationOverloadWarningException ex) {
        java.util.Map<String, Object> details = java.util.Map.of(
                "availableHours", ex.getAvailableHours(),
                "allocatedHours", ex.getAllocatedHours(),
                "overloadHours", ex.getOverloadHours()
        );
        ErrorResponse response = ErrorResponse.of(
                "ALLOCATION_OVERLOAD_WARNING",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 7. Handle Generic DomainException (400 BAD REQUEST)
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleGenericDomainException(DomainException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DOMAIN_RULE_VIOLATION",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 8. Handle DTO Validation Exceptions (@Valid Request Body)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String detailMessage = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    return fieldName + ": " + errorMessage;
                })
                .collect(Collectors.joining("; "));

        ErrorResponse response = ErrorResponse.of(
                "VALIDATION_ERROR",
                "Validation failed for fields: " + detailMessage,
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 9. Handle ConstraintViolationException (@PathVariable / @RequestParam
    // validation in @Validated Controllers)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_PARAMETER",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 10. Handle HandlerMethodValidationException (Spring Boot 3.2+ method
    // parameter validation)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_PARAMETER",
                "Validation failed for method parameters",
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 11. Handle MethodArgumentTypeMismatchException (e.g. GET /org-units/abc where
    // id expects Long)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_PARAMETER",
                "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 12. Handle HttpMessageNotReadableException (Malformed JSON or invalid Enum
    // value)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable mostSpecificCause = ex.getMostSpecificCause();
        if (mostSpecificCause instanceof IllegalArgumentException) {
            ErrorResponse response = ErrorResponse.of(
                    "INVALID_ARGUMENT",
                    mostSpecificCause.getMessage(),
                    HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        ErrorResponse response = ErrorResponse.of(
                "MALFORMED_JSON",
                "Malformed JSON request body or invalid property format",
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 13. Handle MissingServletRequestParameterException (Missing required query
    // parameter)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        ErrorResponse response = ErrorResponse.of(
                "MISSING_PARAMETER",
                "Required query parameter '" + ex.getParameterName() + "' is missing",
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 14. Handle IllegalArgumentException from Value Objects / Technical Argument
    // Validation
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 14.1. Handle DataIntegrityViolationException (Foreign key / Duplicate key constraints)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        log.error("Data integrity violation: ", ex);
        ErrorResponse response = ErrorResponse.of(
                "DATA_INTEGRITY_VIOLATION",
                rootMsg != null ? rootMsg : "Dữ liệu không hợp lệ hoặc tham chiếu tới đối tượng không tồn tại (User ID / Org Unit ID không hợp lệ)",
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 14.2. Handle AccessDeniedException (@PreAuthorize security check failures)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access denied exception in controller: ", ex);
        ErrorResponse response = ErrorResponse.of(
                "FORBIDDEN",
                "Bạn không có quyền truy cập chức năng này",
                HttpStatus.FORBIDDEN.value());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 14.3. Handle InvalidLeaveDateRangeException (NCL-05-CN-002 TC-03)
    @ExceptionHandler(InvalidLeaveDateRangeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLeaveDateRange(InvalidLeaveDateRangeException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_LEAVE_DATE_RANGE",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 14.4. Handle DuplicateLeaveRequestException (NCL-05-CN-002 TC-02)
    @ExceptionHandler(DuplicateLeaveRequestException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateLeaveRequest(DuplicateLeaveRequestException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_LEAVE_REQUEST",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 14.5. Handle LeaveBalanceExceededException (NCL-05-CN-005 TC-02)
    @ExceptionHandler(LeaveBalanceExceededException.class)
    public ResponseEntity<ErrorResponse> handleLeaveBalanceExceeded(LeaveBalanceExceededException ex) {
        ErrorResponse response = ErrorResponse.of(
                "LEAVE_BALANCE_EXCEEDED",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 14.6. Handle IllegalStateException (Invalid state transitions, e.g. approving already approved leave)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorResponse response = ErrorResponse.of(
                "ILLEGAL_STATE",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        log.warn("Resource or endpoint not found: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
                "NOT_FOUND",
                "Endpoint hoặc tài nguyên không tồn tại: " + ex.getResourcePath(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 15. Catch-all Internal Server Error (500 INTERNAL SERVER ERROR)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unhandled internal server error occurred", ex);

        ErrorResponse response = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred.",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
