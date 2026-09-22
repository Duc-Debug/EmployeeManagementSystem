package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.hrm.employeemanagement.domain.exception.DomainException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeVersionConflictException;
import com.hrm.employeemanagement.domain.exception.leave.DuplicateLeaveRequestException;
import com.hrm.employeemanagement.domain.exception.leave.InvalidLeaveDateRangeException;
import com.hrm.employeemanagement.domain.exception.leave.LeaveBalanceExceededException;
import com.hrm.employeemanagement.domain.exception.orgunit.CyclicDependencyException;
import com.hrm.employeemanagement.domain.exception.orgunit.DuplicateUnitCodeException;
import com.hrm.employeemanagement.domain.exception.orgunit.InactiveParentException;
import com.hrm.employeemanagement.domain.exception.orgunit.InvalidOrgUnitManagerException;
import com.hrm.employeemanagement.domain.exception.orgunit.InvalidTreePathException;
import com.hrm.employeemanagement.domain.exception.orgunit.NullOrgUnitIdException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.orgunit.RequiredFieldMissingException;
import com.hrm.employeemanagement.domain.exception.skill.EmployeeSkillNotFoundException;
import com.hrm.employeemanagement.domain.exception.skill.SkillNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;

import jakarta.validation.ConstraintViolationException;

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

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.leave.LeaveRequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLeaveRequestNotFound(com.hrm.employeemanagement.domain.exception.leave.LeaveRequestNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "LEAVE_REQUEST_NOT_FOUND",
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

    @ExceptionHandler(EmployeeVersionConflictException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeVersionConflict(EmployeeVersionConflictException ex) {
        ErrorResponse response = ErrorResponse.of(
                "EMPLOYEE_VERSION_CONFLICT",
                ex.getMessage() != null ? ex.getMessage() : "Hồ sơ nhân sự đã được cập nhật bởi người dùng khác. Vui lòng tải lại dữ liệu và thử lại.",
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class
    })
    public ResponseEntity<ErrorResponse> handleGenericOptimisticLocking(Exception ex) {
        ErrorResponse response = ErrorResponse.of(
                "CONCURRENT_MODIFICATION_CONFLICT",
                "Dữ liệu đã được cập nhật bởi một thao tác khác cùng thời điểm. Vui lòng tải lại và thử lại.",
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.workweek.StandardWorkWeekVersionConflictException.class)
    public ResponseEntity<ErrorResponse> handleStandardWorkWeekVersionConflict(
            com.hrm.employeemanagement.domain.exception.workweek.StandardWorkWeekVersionConflictException ex) {
        ErrorResponse response = ErrorResponse.of(
                "STANDARD_WORK_WEEK_VERSION_CONFLICT",
                ex.getMessage(),
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

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.allocation.OutsourcedContractPeriodException.class)
    public ResponseEntity<ErrorResponse> handleOutsourcedContractPeriod(com.hrm.employeemanagement.domain.exception.allocation.OutsourcedContractPeriodException ex) {
        java.util.Map<String, Object> details = new java.util.HashMap<>();
        if (ex.getEmployeeId() != null) details.put("employeeId", ex.getEmployeeId());
        if (ex.getEmployeeCode() != null) details.put("employeeCode", ex.getEmployeeCode());
        if (ex.getProviderName() != null) details.put("providerName", ex.getProviderName());
        if (ex.getStartDate() != null) details.put("startDate", ex.getStartDate().toString());
        if (ex.getContractEndDate() != null) details.put("contractEndDate", ex.getContractEndDate().toString());
        if (ex.getYearWeek() != null) details.put("yearWeek", ex.getYearWeek().weekNumber() + "/" + ex.getYearWeek().year());

        ErrorResponse response = ErrorResponse.of(
                "OUTSOURCED_CONTRACT_PERIOD_VIOLATION",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.allocation.InvalidCapacityThresholdException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCapacityThreshold(com.hrm.employeemanagement.domain.exception.allocation.InvalidCapacityThresholdException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_CAPACITY_THRESHOLD",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.allocation.CapacityThresholdNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCapacityThresholdNotFound(com.hrm.employeemanagement.domain.exception.allocation.CapacityThresholdNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "CAPACITY_THRESHOLD_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.allocation.CapacityThresholdVersionConflictException.class)
    public ResponseEntity<ErrorResponse> handleCapacityThresholdVersionConflict(com.hrm.employeemanagement.domain.exception.allocation.CapacityThresholdVersionConflictException ex) {
        ErrorResponse response = ErrorResponse.of(
                "CONCURRENT_MODIFICATION_CONFLICT",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
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

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.InvalidCommentDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCommentData(
            com.hrm.employeemanagement.domain.exception.task.InvalidCommentDataException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_COMMENT_DATA", ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.TaskCommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskCommentNotFound(
            com.hrm.employeemanagement.domain.exception.task.TaskCommentNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "TASK_COMMENT_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
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

    // 14.7. Timesheet and Work Log Exceptions
    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.timesheet.DailyHoursLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleDailyHoursLimitExceeded(com.hrm.employeemanagement.domain.exception.timesheet.DailyHoursLimitExceededException ex) {
        java.util.Map<String, Object> details = java.util.Map.of(
                "workDate", ex.getWorkDate().toString(),
                "currentHours", ex.getCurrentHours(),
                "requestedHours", ex.getRequestedHours()
        );
        ErrorResponse response = ErrorResponse.of(
                "DAILY_HOURS_LIMIT_EXCEEDED",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.timesheet.WorkLogInvalidHoursException.class)
    public ResponseEntity<ErrorResponse> handleWorkLogInvalidHours(com.hrm.employeemanagement.domain.exception.timesheet.WorkLogInvalidHoursException ex) {
        ErrorResponse response = ErrorResponse.of(
                "WORK_LOG_INVALID_HOURS",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.timesheet.WorkLogDescriptionBlankException.class)
    public ResponseEntity<ErrorResponse> handleWorkLogDescriptionBlank(com.hrm.employeemanagement.domain.exception.timesheet.WorkLogDescriptionBlankException ex) {
        ErrorResponse response = ErrorResponse.of(
                "WORK_LOG_DESCRIPTION_BLANK",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.timesheet.WorkLogInClosedProjectException.class)
    public ResponseEntity<ErrorResponse> handleWorkLogInClosedProject(com.hrm.employeemanagement.domain.exception.timesheet.WorkLogInClosedProjectException ex) {
        ErrorResponse response = ErrorResponse.of(
                "WORK_LOG_IN_CLOSED_PROJECT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.timesheet.WorkLogTaskNotAssignedException.class)
    public ResponseEntity<ErrorResponse> handleWorkLogTaskNotAssigned(com.hrm.employeemanagement.domain.exception.timesheet.WorkLogTaskNotAssignedException ex) {
        ErrorResponse response = ErrorResponse.of(
                "WORK_LOG_TASK_NOT_ASSIGNED",
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.timesheet.TimesheetImmutableException.class)
    public ResponseEntity<ErrorResponse> handleTimesheetImmutable(com.hrm.employeemanagement.domain.exception.timesheet.TimesheetImmutableException ex) {
        ErrorResponse response = ErrorResponse.of(
                "TIMESHEET_IMMUTABLE",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.timesheet.EmptyTimesheetSubmissionException.class)
    public ResponseEntity<ErrorResponse> handleEmptyTimesheetSubmission(com.hrm.employeemanagement.domain.exception.timesheet.EmptyTimesheetSubmissionException ex) {
        ErrorResponse response = ErrorResponse.of(
                "EMPTY_TIMESHEET_SUBMISSION",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.employee.EmployeeInactiveException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeInactive(com.hrm.employeemanagement.domain.exception.employee.EmployeeInactiveException ex) {
        ErrorResponse response = ErrorResponse.of(
                "EMPLOYEE_INACTIVE",
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.timesheet.TimesheetNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTimesheetNotFound(com.hrm.employeemanagement.domain.exception.timesheet.TimesheetNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "TIMESHEET_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.timesheet.TimesheetEntryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTimesheetEntryNotFound(com.hrm.employeemanagement.domain.exception.timesheet.TimesheetEntryNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "TIMESHEET_ENTRY_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.timesheet.WorkLogAdjustmentReasonRequiredException.class)
    public ResponseEntity<ErrorResponse> handleWorkLogAdjustmentReasonRequired(com.hrm.employeemanagement.domain.exception.timesheet.WorkLogAdjustmentReasonRequiredException ex) {
        ErrorResponse response = ErrorResponse.of(
                "WORK_LOG_ADJUSTMENT_REASON_REQUIRED",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.timesheet.TimesheetNotApprovedException.class)
    public ResponseEntity<ErrorResponse> handleTimesheetNotApproved(com.hrm.employeemanagement.domain.exception.timesheet.TimesheetNotApprovedException ex) {
        ErrorResponse response = ErrorResponse.of(
                "TIMESHEET_NOT_APPROVED",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.timesheet.TimesheetEntryVersionConflictException.class)
    public ResponseEntity<ErrorResponse> handleTimesheetEntryVersionConflict(com.hrm.employeemanagement.domain.exception.timesheet.TimesheetEntryVersionConflictException ex) {
        ErrorResponse response = ErrorResponse.of(
                "TIMESHEET_ENTRY_VERSION_CONFLICT",
                ex.getMessage() != null ? ex.getMessage() : "Dữ liệu dòng giờ công đã bị thay đổi bởi người khác. Vui lòng tải lại trang và thử lại.",
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
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

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleScenarioNotFound(com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "SCENARIO_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.scenario.ScenarioDemandNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleScenarioDemandNotFound(com.hrm.employeemanagement.domain.exception.scenario.ScenarioDemandNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "SCENARIO_DEMAND_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.scenario.InvalidScenarioDemandException.class)
    public ResponseEntity<ErrorResponse> handleInvalidScenarioDemand(com.hrm.employeemanagement.domain.exception.scenario.InvalidScenarioDemandException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_SCENARIO_DEMAND",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotModifiableException.class)
    public ResponseEntity<ErrorResponse> handleScenarioNotModifiable(com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotModifiableException ex) {
        ErrorResponse response = ErrorResponse.of(
                "SCENARIO_NOT_MODIFIABLE",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.scenario.DuplicateScenarioCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateScenarioCode(com.hrm.employeemanagement.domain.exception.scenario.DuplicateScenarioCodeException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_SCENARIO_CODE",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.scenario.InsufficientScenariosForComparisonException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientScenariosForComparison(com.hrm.employeemanagement.domain.exception.scenario.InsufficientScenariosForComparisonException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INSUFFICIENT_SCENARIOS_FOR_COMPARISON",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotSavedException.class)
    public ResponseEntity<ErrorResponse> handleScenarioNotSaved(com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotSavedException ex) {
        ErrorResponse response = ErrorResponse.of(
                "SCENARIO_NOT_SAVED",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.scenario.InvalidShareRecipientException.class)
    public ResponseEntity<ErrorResponse> handleInvalidShareRecipient(com.hrm.employeemanagement.domain.exception.scenario.InvalidShareRecipientException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_SHARE_RECIPIENT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.scenario.DuplicateScenarioShareException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateScenarioShare(com.hrm.employeemanagement.domain.exception.scenario.DuplicateScenarioShareException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_SCENARIO_SHARE",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.scenario.CorruptedScenarioSnapshotException.class)
    public ResponseEntity<ErrorResponse> handleCorruptedScenarioSnapshot(com.hrm.employeemanagement.domain.exception.scenario.CorruptedScenarioSnapshotException ex) {
        ErrorResponse response = ErrorResponse.of(
                "CORRUPTED_SCENARIO_SNAPSHOT",
                ex.getMessage(),
                HttpStatus.UNPROCESSABLE_ENTITY.value());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.scenario.ScenarioBaselineStaleException.class)
    public ResponseEntity<ErrorResponse> handleScenarioBaselineStale(com.hrm.employeemanagement.domain.exception.scenario.ScenarioBaselineStaleException ex) {
        ErrorResponse response = ErrorResponse.of(
                "SCENARIO_BASELINE_STALE",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.scenario.ScenarioAlreadyAppliedException.class)
    public ResponseEntity<ErrorResponse> handleScenarioAlreadyApplied(com.hrm.employeemanagement.domain.exception.scenario.ScenarioAlreadyAppliedException ex) {
        ErrorResponse response = ErrorResponse.of(
                "SCENARIO_ALREADY_APPLIED",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.scenario.ScenarioDemandCapacityExceededException.class)
    public ResponseEntity<ErrorResponse> handleScenarioDemandCapacityExceeded(com.hrm.employeemanagement.domain.exception.scenario.ScenarioDemandCapacityExceededException ex) {
        ErrorResponse response = ErrorResponse.of(
                "SCENARIO_DEMAND_CAPACITY_EXCEEDED",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.scenario.InvalidTargetProjectException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTargetProject(com.hrm.employeemanagement.domain.exception.scenario.InvalidTargetProjectException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_TARGET_PROJECT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.notification.NotificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotificationNotFound(com.hrm.employeemanagement.domain.exception.notification.NotificationNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "NOTIFICATION_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.notification.NotificationAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleNotificationAccessDenied(com.hrm.employeemanagement.domain.exception.notification.NotificationAccessDeniedException ex) {
        ErrorResponse response = ErrorResponse.of(
                "NOTIFICATION_ACCESS_DENIED",
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.workweek.InvalidStandardWorkWeekException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStandardWorkWeek(com.hrm.employeemanagement.domain.exception.workweek.InvalidStandardWorkWeekException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_STANDARD_WORK_WEEK",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.workweek.StandardWorkWeekNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStandardWorkWeekNotFound(com.hrm.employeemanagement.domain.exception.workweek.StandardWorkWeekNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "STANDARD_WORK_WEEK_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 15. Catch-all Internal Server Error (500 INTERNAL SERVER ERROR)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unhandled internal server error occurred", ex);

        ErrorResponse response = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "Máy chủ gặp lỗi khi xử lý dữ liệu. Vui lòng thử lại hoặc liên hệ quản trị viên.",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
