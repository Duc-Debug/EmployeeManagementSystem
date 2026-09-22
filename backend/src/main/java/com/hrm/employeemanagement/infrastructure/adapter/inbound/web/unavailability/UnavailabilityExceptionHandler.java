package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.unavailability;

import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityPeriodException;
import com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityStatusException;
import com.hrm.employeemanagement.domain.exception.unavailability.UnavailabilityConflictException;
import com.hrm.employeemanagement.domain.exception.unavailability.UnavailabilityDeclarationNotFoundException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.hrm.employeemanagement.infrastructure.adapter.inbound.web.unavailability")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UnavailabilityExceptionHandler {

    @ExceptionHandler(UnavailabilityDeclarationNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(UnavailabilityDeclarationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("UNAVAILABILITY_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidUnavailabilityPeriodException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPeriod(InvalidUnavailabilityPeriodException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_PERIOD", ex.getMessage()));
    }

    @ExceptionHandler(InvalidUnavailabilityStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStatus(InvalidUnavailabilityStatusException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_STATUS", ex.getMessage()));
    }

    @ExceptionHandler(UnavailabilityConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(UnavailabilityConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("UNAVAILABILITY_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePermissionDenied(PermissionDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("PERMISSION_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(java.util.stream.Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_ERROR", errorMessage));
    }
}
