package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.DuplicateProjectCodeException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDateRangeException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

@RestControllerAdvice(basePackages = "com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProjectExceptionHandler {

        @ExceptionHandler(PermissionDeniedException.class)
        public ResponseEntity<ApiResponse<Void>> handlePermissionDenied(
                        PermissionDeniedException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(
                                                ApiResponse.error(
                                                                ex.getMessage()));
        }

        @ExceptionHandler(ProjectNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleProjectNotFound(
                        ProjectNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(
                                                ApiResponse.error(
                                                                ex.getMessage()));
        }

        @ExceptionHandler(DuplicateProjectCodeException.class)
        public ResponseEntity<ApiResponse<Void>> handleDuplicateProjectCode(DuplicateProjectCodeException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(InvalidProjectDataException.class)
        public ResponseEntity<ApiResponse<Void>> handleInvalidProjectData(InvalidProjectDataException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(InvalidProjectDateRangeException.class)
        public ResponseEntity<ApiResponse<Void>> handleInvalidProjectDateRange(InvalidProjectDateRangeException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
        }
}
