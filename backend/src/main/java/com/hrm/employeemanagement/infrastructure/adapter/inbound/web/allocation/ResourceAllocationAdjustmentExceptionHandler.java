package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hrm.employeemanagement.domain.exception.allocation.AllocationNotFoundException;
import com.hrm.employeemanagement.domain.exception.allocation.CannotRemoveAllocationWithActualHoursException;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationAdjustmentException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

@RestControllerAdvice(basePackages = "com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ResourceAllocationAdjustmentExceptionHandler {

    @ExceptionHandler(CannotRemoveAllocationWithActualHoursException.class)
    public ResponseEntity<ApiResponse<Void>> handleCannotRemoveAllocationWithActualHours(CannotRemoveAllocationWithActualHoursException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AllocationNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAllocationNotFound(AllocationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidAllocationAdjustmentException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidAllocationAdjustment(InvalidAllocationAdjustmentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePermissionDenied(PermissionDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }
}
