package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common;

import com.hrm.employeemanagement.domain.exception.DomainException;
import com.hrm.employeemanagement.domain.exception.orgunit.CyclicDependencyException;
import com.hrm.employeemanagement.domain.exception.orgunit.DuplicateUnitCodeException;
import com.hrm.employeemanagement.domain.exception.orgunit.InactiveParentException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.stream.Collectors;

@RestControllerAdvice
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

    // 2. Handle DuplicateUnitCodeException (409 CONFLICT)
    @ExceptionHandler(DuplicateUnitCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCode(DuplicateUnitCodeException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_UNIT_CODE",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
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

    // 5. Handle Generic DomainException (400 BAD REQUEST)
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleGenericDomainException(DomainException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DOMAIN_RULE_VIOLATION",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 6. Handle DTO Validation Exceptions (@Valid Request Body)
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

    // 7. Handle ConstraintViolationException (@PathVariable / @RequestParam validation in @Validated Controllers)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_PARAMETER",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 8. Handle HandlerMethodValidationException (Spring Boot 3.2+ method parameter validation)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_PARAMETER",
                "Validation failed for method parameters",
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 9. Handle MethodArgumentTypeMismatchException (e.g. GET /org-units/abc where id expects Long)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_PARAMETER",
                "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 10. Handle HttpMessageNotReadableException (Malformed JSON or invalid Enum value)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        ErrorResponse response = ErrorResponse.of(
                "MALFORMED_JSON",
                "Malformed JSON request body or invalid property format",
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 11. Handle MissingServletRequestParameterException (Missing required query parameter)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        ErrorResponse response = ErrorResponse.of(
                "MISSING_PARAMETER",
                "Required query parameter '" + ex.getParameterName() + "' is missing",
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 12. Handle IllegalArgumentException from Value Objects / Technical Argument Validation
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 13. Catch-all Internal Server Error (500 INTERNAL SERVER ERROR)
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