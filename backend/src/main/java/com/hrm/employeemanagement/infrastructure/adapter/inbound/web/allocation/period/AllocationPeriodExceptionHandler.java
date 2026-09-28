package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.period;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hrm.employeemanagement.domain.exception.allocation.AllocationPeriodLockedException;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationPeriodNotFoundException;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationPeriodException;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationPeriodStateException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.ErrorResponse;

/**
 * Xử lý ngoại lệ chuyên biệt cho phân hệ Khóa kỳ kế hoạch phân bổ (NCL-06-CN-009 / QTN-18).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AllocationPeriodExceptionHandler {

    @ExceptionHandler(AllocationPeriodLockedException.class)
    public ResponseEntity<ErrorResponse> handleAllocationPeriodLocked(AllocationPeriodLockedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.of("ALLOCATION_PERIOD_LOCKED", ex.getMessage(), HttpStatus.CONFLICT.value())
        );
    }

    @ExceptionHandler(AllocationPeriodNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAllocationPeriodNotFound(AllocationPeriodNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.of("ALLOCATION_PERIOD_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND.value())
        );
    }

    @ExceptionHandler(InvalidAllocationPeriodStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPeriodState(InvalidAllocationPeriodStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.of("INVALID_PERIOD_STATE", ex.getMessage(), HttpStatus.BAD_REQUEST.value())
        );
    }

    @ExceptionHandler(InvalidAllocationPeriodException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPeriod(InvalidAllocationPeriodException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.of("INVALID_PERIOD_DATA", ex.getMessage(), HttpStatus.BAD_REQUEST.value())
        );
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePermissionDenied(com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.of("PERMISSION_DENIED", ex.getMessage(), HttpStatus.FORBIDDEN.value())
        );
    }
}
