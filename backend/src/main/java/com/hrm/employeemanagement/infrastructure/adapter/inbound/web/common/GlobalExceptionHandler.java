package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
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
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Handle OrgUnitNotFoundException (404 NOT FOUND)
    @ExceptionHandler(OrgUnitNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrgUnitNotFound(OrgUnitNotFoundException ex) {
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
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(Exception ex) {
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
        // Specific requirement for MyAllocations feedback reason
        if (ex.getBindingResult().getTarget() instanceof com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.MyAllocationsController.ProvideScheduleFeedbackRequest
                || "provideScheduleFeedbackRequest".equalsIgnoreCase(ex.getBindingResult().getObjectName())) {
            String msg = ex.getBindingResult().getAllErrors().stream()
                    .map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse("Lý do hoặc ý kiến phản hồi không được để trống");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of("INVALID_FEEDBACK_REASON", msg, HttpStatus.BAD_REQUEST.value()));
        }

        String detailMessage = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + (fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "không hợp lệ");
                    }
                    String objName = error.getObjectName() != null ? error.getObjectName() : "object";
                    String msg = error.getDefaultMessage() != null ? error.getDefaultMessage() : "không hợp lệ";
                    return objName + ": " + msg;
                })
                .collect(Collectors.joining("; "));

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            if (error instanceof FieldError fieldError) {
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                fieldErrors.put(error.getObjectName(), error.getDefaultMessage());
            }
        }

        ErrorResponse response = ErrorResponse.of(
                "VALIDATION_ERROR",
                "Validation failed for fields: " + detailMessage,
                HttpStatus.BAD_REQUEST.value(),
                fieldErrors);
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
        String errorCode = (ex.getMessage() != null && ex.getMessage().contains("QTN-24"))
                ? "INVALID_FEEDBACK_REASON"
                : "INVALID_ARGUMENT";
        ErrorResponse response = ErrorResponse.of(
                errorCode,
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 14.1. Handle DataIntegrityViolationException (Foreign key / Duplicate key constraints)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        String lowerMsg = rootMsg != null ? rootMsg.toLowerCase() : "";
        log.error("Data integrity violation: ", ex);

        // Check for Unique Constraint violations -> 409 CONFLICT
        if (lowerMsg.contains("unique") || lowerMsg.contains("duplicate") || lowerMsg.contains("uk_")) {
            if (lowerMsg.contains("email") || lowerMsg.contains("uk_users_email")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ErrorResponse.of("DUPLICATE_EMAIL", "Email đã tồn tại trong hệ thống", HttpStatus.CONFLICT.value()));
            }
            if (lowerMsg.contains("username") || lowerMsg.contains("uk_users_username") || lowerMsg.contains("users")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ErrorResponse.of("DUPLICATE_USERNAME", "Tên đăng nhập đã tồn tại trong hệ thống", HttpStatus.CONFLICT.value()));
            }
            if (lowerMsg.contains("employee_code") || lowerMsg.contains("employees")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ErrorResponse.of("DUPLICATE_EMPLOYEE_CODE", "Mã nhân viên đã tồn tại trong hệ thống", HttpStatus.CONFLICT.value()));
            }
            if (lowerMsg.contains("user_id")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ErrorResponse.of("DUPLICATE_USER_LINK", "Người dùng này đã được liên kết với một hồ sơ nhân viên khác", HttpStatus.CONFLICT.value()));
            }
            if (lowerMsg.contains("uk_project_roles_name") || lowerMsg.contains("project_roles.name")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ErrorResponse.of("DUPLICATE_PROJECT_ROLE", "Tên vai trò chuyên môn đã tồn tại trong hệ thống", HttpStatus.CONFLICT.value()));
            }
            if (lowerMsg.contains("uk_tasks_project_task_code") || lowerMsg.contains("task")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ErrorResponse.of("DUPLICATE_TASK_CODE", "Mã công việc đã tồn tại trong dự án hoặc vi phạm toàn vẹn dữ liệu", HttpStatus.CONFLICT.value()));
            }
            if (lowerMsg.contains("milestone")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ErrorResponse.of("DUPLICATE_MILESTONE", "Tên mốc tiến độ đã tồn tại trong dự án hoặc vi phạm toàn vẹn dữ liệu", HttpStatus.CONFLICT.value()));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.of("DATA_INTEGRITY_VIOLATION", "Dữ liệu vi phạm ràng buộc toàn vẹn hoặc đã tồn tại trong hệ thống", HttpStatus.CONFLICT.value()));
        }

        // Check for Foreign Key Constraint violations -> 400 BAD REQUEST
        if (lowerMsg.contains("foreign key") || lowerMsg.contains("fk_") || lowerMsg.contains("referential integrity")) {
            if (lowerMsg.contains("org_unit") || lowerMsg.contains("org unit")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ErrorResponse.of("DATA_INTEGRITY_VIOLATION", "Đơn vị tổ chức được chỉ định không tồn tại trong hệ thống", HttpStatus.BAD_REQUEST.value()));
            }
            if (lowerMsg.contains("department")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ErrorResponse.of("DATA_INTEGRITY_VIOLATION", "Phòng ban được chỉ định không tồn tại trong hệ thống", HttpStatus.BAD_REQUEST.value()));
            }
            if (lowerMsg.contains("role")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ErrorResponse.of("DATA_INTEGRITY_VIOLATION", "Vai trò được chỉ định không tồn tại trong hệ thống", HttpStatus.BAD_REQUEST.value()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of("DATA_INTEGRITY_VIOLATION", "Dữ liệu tham chiếu không hợp lệ hoặc không tồn tại trong hệ thống", HttpStatus.BAD_REQUEST.value()));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("DATA_INTEGRITY_VIOLATION", rootMsg != null ? rootMsg : "Dữ liệu không đáp ứng các ràng buộc toàn vẹn của hệ thống", HttpStatus.BAD_REQUEST.value()));
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

    // --- User Domain Handlers ---
    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.role.RoleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoleNotFound(com.hrm.employeemanagement.domain.exception.role.RoleNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "ROLE_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.employee.DuplicateEmployeeCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmployeeCode(com.hrm.employeemanagement.domain.exception.employee.DuplicateEmployeeCodeException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_EMPLOYEE_CODE",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({
            com.hrm.employeemanagement.domain.exception.user.InvalidCredentialsException.class,
            org.springframework.security.authentication.BadCredentialsException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(RuntimeException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_CREDENTIALS",
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED.value());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler({
            com.hrm.employeemanagement.domain.exception.user.UserLockedException.class,
            org.springframework.security.authentication.DisabledException.class
    })
    public ResponseEntity<ErrorResponse> handleUserLocked(RuntimeException ex) {
        ErrorResponse response = ErrorResponse.of(
                "USER_LOCKED",
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler({
            com.hrm.employeemanagement.domain.exception.user.SelfLockingException.class,
            com.hrm.employeemanagement.domain.exception.user.LastAdminProtectionException.class,
            com.hrm.employeemanagement.domain.exception.user.UserAlreadyLockedException.class,
            com.hrm.employeemanagement.domain.exception.user.UserAlreadyActiveException.class,
            com.hrm.employeemanagement.domain.exception.user.InvalidPasswordException.class,
            com.hrm.employeemanagement.domain.exception.user.InvalidResetTokenException.class
    })
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(RuntimeException ex) {
        ErrorResponse response = ErrorResponse.of(
                "BUSINESS_RULE_VIOLATION",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // --- Project Domain Handlers ---
    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectNotFound(com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "PROJECT_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.DuplicateProjectCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProjectCode(com.hrm.employeemanagement.domain.exception.project.DuplicateProjectCodeException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_PROJECT_CODE",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.DuplicateResourceDemandException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceDemand(com.hrm.employeemanagement.domain.exception.project.DuplicateResourceDemandException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_RESOURCE_DEMAND",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProjectData(com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_PROJECT_DATA",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.InvalidProjectDateRangeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProjectDateRange(com.hrm.employeemanagement.domain.exception.project.InvalidProjectDateRangeException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_PROJECT_DATE_RANGE",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.projecttemplate.ProjectTemplateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectTemplateNotFound(com.hrm.employeemanagement.domain.exception.projecttemplate.ProjectTemplateNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "PROJECT_TEMPLATE_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.InvalidResourceDemandException.class)
    public ResponseEntity<ErrorResponse> handleInvalidResourceDemand(com.hrm.employeemanagement.domain.exception.project.InvalidResourceDemandException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_RESOURCE_DEMAND",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.ProjectDateNotConfiguredException.class)
    public ResponseEntity<ErrorResponse> handleProjectDateNotConfigured(com.hrm.employeemanagement.domain.exception.project.ProjectDateNotConfiguredException ex) {
        ErrorResponse response = ErrorResponse.of(
                "PROJECT_DATE_NOT_CONFIGURED",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.ProjectAlreadyClosedException.class)
    public ResponseEntity<ErrorResponse> handleProjectAlreadyClosed(com.hrm.employeemanagement.domain.exception.project.ProjectAlreadyClosedException ex) {
        ErrorResponse response = ErrorResponse.of(
                "PROJECT_ALREADY_CLOSED",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.ProjectNotClosedException.class)
    public ResponseEntity<ErrorResponse> handleProjectNotClosed(com.hrm.employeemanagement.domain.exception.project.ProjectNotClosedException ex) {
        ErrorResponse response = ErrorResponse.of(
                "PROJECT_NOT_CLOSED",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.ProjectHasUnfinishedTasksException.class)
    public ResponseEntity<ErrorResponse> handleProjectHasUnfinishedTasks(com.hrm.employeemanagement.domain.exception.project.ProjectHasUnfinishedTasksException ex) {
        ErrorResponse response = ErrorResponse.of(
                "PROJECT_HAS_UNFINISHED_TASKS",
                ex.getMessage(),
                HttpStatus.UNPROCESSABLE_ENTITY.value());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.DuplicateProjectMemberException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProjectMember(com.hrm.employeemanagement.domain.exception.project.DuplicateProjectMemberException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_PROJECT_MEMBER",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.ProjectMemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectMemberNotFound(com.hrm.employeemanagement.domain.exception.project.ProjectMemberNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "PROJECT_MEMBER_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.MemberHasActiveTasksException.class)
    public ResponseEntity<ErrorResponse> handleMemberHasActiveTasks(com.hrm.employeemanagement.domain.exception.project.MemberHasActiveTasksException ex) {
        ErrorResponse response = ErrorResponse.of(
                "MEMBER_HAS_ACTIVE_TASKS",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.role.DuplicateProjectRoleCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProjectRoleCode(com.hrm.employeemanagement.domain.exception.role.DuplicateProjectRoleCodeException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_PROJECT_ROLE_CODE",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.role.DuplicateProjectRoleNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProjectRoleName(com.hrm.employeemanagement.domain.exception.role.DuplicateProjectRoleNameException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_PROJECT_ROLE_NAME",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.role.InvalidProjectRoleDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProjectRoleData(com.hrm.employeemanagement.domain.exception.role.InvalidProjectRoleDataException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_PROJECT_ROLE_DATA",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.role.InvalidProjectRoleStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProjectRoleState(com.hrm.employeemanagement.domain.exception.role.InvalidProjectRoleStateException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_PROJECT_ROLE_STATE",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // --- Task Domain Handlers ---
    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "TASK_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.ProjectClosedException.class)
    public ResponseEntity<ErrorResponse> handleProjectClosed(com.hrm.employeemanagement.domain.exception.task.ProjectClosedException ex) {
        ErrorResponse response = ErrorResponse.of(
                "PROJECT_CLOSED",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.CyclicTaskHierarchyException.class)
    public ResponseEntity<ErrorResponse> handleCyclicTaskHierarchy(com.hrm.employeemanagement.domain.exception.task.CyclicTaskHierarchyException ex) {
        ErrorResponse response = ErrorResponse.of(
                "CYCLIC_TASK_HIERARCHY",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.CyclicTaskDependencyException.class)
    public ResponseEntity<ErrorResponse> handleCyclicTaskDependency(com.hrm.employeemanagement.domain.exception.task.CyclicTaskDependencyException ex) {
        ErrorResponse response = ErrorResponse.of(
                "CYCLIC_TASK_DEPENDENCY",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.AssigneeNotInProjectException.class)
    public ResponseEntity<ErrorResponse> handleAssigneeNotInProject(com.hrm.employeemanagement.domain.exception.task.AssigneeNotInProjectException ex) {
        ErrorResponse response = ErrorResponse.of(
                "ASSIGNEE_NOT_IN_PROJECT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTaskData(com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_TASK_DATA",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.TaskHasChildrenException.class)
    public ResponseEntity<ErrorResponse> handleTaskHasChildren(com.hrm.employeemanagement.domain.exception.task.TaskHasChildrenException ex) {
        ErrorResponse response = ErrorResponse.of(
                "TASK_HAS_CHILDREN",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.TaskHasTimesheetException.class)
    public ResponseEntity<ErrorResponse> handleTaskHasTimesheet(com.hrm.employeemanagement.domain.exception.task.TaskHasTimesheetException ex) {
        ErrorResponse response = ErrorResponse.of(
                "TASK_HAS_TIMESHEET",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.AssigneeInactiveException.class)
    public ResponseEntity<ErrorResponse> handleAssigneeInactive(com.hrm.employeemanagement.domain.exception.task.AssigneeInactiveException ex) {
        ErrorResponse response = ErrorResponse.of(
                "ASSIGNEE_INACTIVE",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.TaskNotAssignedToUserException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotAssignedToUser(com.hrm.employeemanagement.domain.exception.task.TaskNotAssignedToUserException ex) {
        ErrorResponse response = ErrorResponse.of(
                "TASK_NOT_ASSIGNED_TO_USER",
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // --- Skill Domain Handlers ---
    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.skill.SkillGroupNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSkillGroupNotFound(com.hrm.employeemanagement.domain.exception.skill.SkillGroupNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "SKILL_GROUP_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.skill.DuplicateSkillNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSkillName(com.hrm.employeemanagement.domain.exception.skill.DuplicateSkillNameException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_SKILL_NAME",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.skill.InvalidSkillMergeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSkillMerge(com.hrm.employeemanagement.domain.exception.skill.InvalidSkillMergeException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_SKILL_MERGE",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // --- Milestone Domain Handlers ---
    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.milestone.MilestoneNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMilestoneNotFound(com.hrm.employeemanagement.domain.exception.milestone.MilestoneNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "MILESTONE_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.milestone.ProjectHasNoWbsException.class)
    public ResponseEntity<ErrorResponse> handleProjectHasNoWbs(com.hrm.employeemanagement.domain.exception.milestone.ProjectHasNoWbsException ex) {
        ErrorResponse response = ErrorResponse.of(
                "PROJECT_HAS_NO_WBS",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.milestone.DuplicateMilestoneNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateMilestoneName(com.hrm.employeemanagement.domain.exception.milestone.DuplicateMilestoneNameException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_MILESTONE_NAME",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.milestone.TaskNotInProjectException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotInProject(com.hrm.employeemanagement.domain.exception.milestone.TaskNotInProjectException ex) {
        ErrorResponse response = ErrorResponse.of(
                "TASK_NOT_IN_PROJECT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.milestone.InvalidMilestoneDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMilestoneData(com.hrm.employeemanagement.domain.exception.milestone.InvalidMilestoneDataException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_MILESTONE_DATA",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // --- Allocation & MyAllocations Domain Handlers ---
    @ExceptionHandler(com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.InvalidQueryParameterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQueryParameter(com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.InvalidQueryParameterException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_QUERY_PARAMETER",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.InvalidWeekFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidWeekFormat(com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.InvalidWeekFormatException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_WEEK_FORMAT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.InvalidWeeksFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidWeeksFormat(com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.InvalidWeeksFormatException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_WEEKS_FORMAT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.WeekStartNotMondayException.class)
    public ResponseEntity<ErrorResponse> handleWeekStartNotMonday(com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.WeekStartNotMondayException ex) {
        ErrorResponse response = ErrorResponse.of(
                "WEEK_START_NOT_MONDAY",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.allocation.CannotRemoveAllocationWithActualHoursException.class)
    public ResponseEntity<ErrorResponse> handleCannotRemoveAllocationWithActualHours(com.hrm.employeemanagement.domain.exception.allocation.CannotRemoveAllocationWithActualHoursException ex) {
        ErrorResponse response = ErrorResponse.of(
                "CANNOT_REMOVE_ALLOCATION_WITH_ACTUAL_HOURS",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.allocation.AllocationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAllocationNotFound(com.hrm.employeemanagement.domain.exception.allocation.AllocationNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "ALLOCATION_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationAdjustmentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAllocationAdjustment(com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationAdjustmentException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_ALLOCATION_ADJUSTMENT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.allocation.AllocationPeriodLockedException.class)
    public ResponseEntity<ErrorResponse> handleAllocationPeriodLocked(com.hrm.employeemanagement.domain.exception.allocation.AllocationPeriodLockedException ex) {
        ErrorResponse response = ErrorResponse.of(
                "ALLOCATION_PERIOD_LOCKED",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.allocation.AllocationPeriodNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAllocationPeriodNotFound(com.hrm.employeemanagement.domain.exception.allocation.AllocationPeriodNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "ALLOCATION_PERIOD_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationPeriodStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPeriodState(com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationPeriodStateException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_PERIOD_STATE",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationPeriodException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPeriod(com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationPeriodException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_PERIOD_DATA",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // --- Reservation Domain Handlers ---
    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.reservation.ReservationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReservationNotFound(com.hrm.employeemanagement.domain.exception.reservation.ReservationNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "RESERVATION_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.reservation.InvalidReservationDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidReservationData(com.hrm.employeemanagement.domain.exception.reservation.InvalidReservationDataException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_RESERVATION_DATA",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.reservation.InvalidReservationStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidReservationState(com.hrm.employeemanagement.domain.exception.reservation.InvalidReservationStateException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_RESERVATION_STATE",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.allocation.EmployeeInactiveException.class)
    public ResponseEntity<ErrorResponse> handleAllocationEmployeeInactive(com.hrm.employeemanagement.domain.exception.allocation.EmployeeInactiveException ex) {
        ErrorResponse response = ErrorResponse.of(
                "EMPLOYEE_INACTIVE",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // --- Unavailability Domain Handlers ---
    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.unavailability.UnavailabilityDeclarationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUnavailabilityDeclarationNotFound(com.hrm.employeemanagement.domain.exception.unavailability.UnavailabilityDeclarationNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "UNAVAILABILITY_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityPeriodException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUnavailabilityPeriod(com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityPeriodException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_PERIOD",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUnavailabilityStatus(com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityStatusException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_STATUS",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.unavailability.UnavailabilityConflictException.class)
    public ResponseEntity<ErrorResponse> handleUnavailabilityConflict(com.hrm.employeemanagement.domain.exception.unavailability.UnavailabilityConflictException ex) {
        ErrorResponse response = ErrorResponse.of(
                "UNAVAILABILITY_CONFLICT",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // --- Backup Domain Handlers ---
    @ExceptionHandler(com.hrm.employeemanagement.domain.backup.exception.BackupAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleBackupAccessDenied(com.hrm.employeemanagement.domain.backup.exception.BackupAccessDeniedException e) {
        log.warn("Từ chối truy cập sao lưu & phục hồi: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(
                "FORBIDDEN",
                e.getMessage(),
                HttpStatus.FORBIDDEN.value());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.backup.exception.BackupNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBackupNotFound(com.hrm.employeemanagement.domain.backup.exception.BackupNotFoundException e) {
        ErrorResponse response = ErrorResponse.of(
                "NOT_FOUND",
                e.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.backup.exception.InvalidBackupStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBackupStatus(com.hrm.employeemanagement.domain.backup.exception.InvalidBackupStatusException e) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_BACKUP_STATUS",
                e.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.backup.exception.InvalidRestoreConfirmationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRestoreConfirmation(com.hrm.employeemanagement.domain.backup.exception.InvalidRestoreConfirmationException e) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_CONFIRMATION",
                e.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.backup.exception.BackupRestoreFailedException.class)
    public ResponseEntity<ErrorResponse> handleBackupRestoreFailed(com.hrm.employeemanagement.domain.backup.exception.BackupRestoreFailedException e) {
        log.error("Phục hồi sao lưu thất bại: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(
                "RESTORE_FAILED",
                e.getMessage(),
                HttpStatus.UNPROCESSABLE_ENTITY.value());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    // 15. Catch-all Internal Server Error (500 INTERNAL SERVER ERROR)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled internal server error occurred", ex);

        ErrorResponse response = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "Đã xảy ra lỗi hệ thống. Vui lòng liên hệ quản trị viên hoặc thử lại sau.",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
