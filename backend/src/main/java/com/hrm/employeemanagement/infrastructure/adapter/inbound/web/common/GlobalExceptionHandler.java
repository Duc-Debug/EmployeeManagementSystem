package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common;

import com.hrm.employeemanagement.domain.exception.DomainException;
import com.hrm.employeemanagement.domain.exception.orgunit.CyclicDependencyException;
import com.hrm.employeemanagement.domain.exception.orgunit.DuplicateUnitCodeException;
import com.hrm.employeemanagement.domain.exception.orgunit.InactiveParentException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Xử lý lỗi không tìm thấy (404 NOT FOUND)
    @ExceptionHandler(OrgUnitNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OrgUnitNotFoundException ex) {
        ErrorResponse response = ErrorResponse.of(
                "ORG_UNIT_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 2. Xử lý lỗi trùng mã đơn vị (409 CONFLICT)
    @ExceptionHandler(DuplicateUnitCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCode(DuplicateUnitCodeException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DUPLICATE_UNIT_CODE",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 3. Xử lý lỗi vòng lặp Cây tổ chức (400 BAD REQUEST)
    @ExceptionHandler(CyclicDependencyException.class)
    public ResponseEntity<ErrorResponse> handleCyclicDependency(CyclicDependencyException ex) {
        ErrorResponse response = ErrorResponse.of(
                "CYCLIC_DEPENDENCY_ERROR",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 4. Xử lý lỗi nút cha bị khóa/INACTIVE (400 BAD REQUEST)
    @ExceptionHandler(InactiveParentException.class)
    public ResponseEntity<ErrorResponse> handleInactiveParent(InactiveParentException ex) {
        ErrorResponse response = ErrorResponse.of(
                "INACTIVE_PARENT_UNIT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 5. Xử lý các Domain Exception khác chưa phân loại
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleGenericDomainException(DomainException ex) {
        ErrorResponse response = ErrorResponse.of(
                "DOMAIN_RULE_VIOLATION",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 6. Xử lý lỗi Validation DTO (@Valid Request Body)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // 7. Catch-all lỗi hệ thống chung (500 INTERNAL SERVER ERROR)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse response = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}