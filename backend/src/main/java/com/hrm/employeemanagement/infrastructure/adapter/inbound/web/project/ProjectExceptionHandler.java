package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.DuplicateProjectCodeException;
import com.hrm.employeemanagement.domain.exception.project.DuplicateResourceDemandException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDateRangeException;
import com.hrm.employeemanagement.domain.exception.project.InvalidResourceDemandException;
import com.hrm.employeemanagement.domain.exception.project.ProjectDateNotConfiguredException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.projecttemplate.ProjectTemplateNotFoundException;
import com.hrm.employeemanagement.domain.exception.role.RoleNotFoundException;
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

        @ExceptionHandler(DuplicateResourceDemandException.class)
        public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceDemand(DuplicateResourceDemandException ex) {
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

        @ExceptionHandler(ProjectTemplateNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleProjectTemplateNotFound(ProjectTemplateNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(InvalidResourceDemandException.class)
        public ResponseEntity<ApiResponse<Void>> handleInvalidResourceDemand(InvalidResourceDemandException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(ProjectDateNotConfiguredException.class)
        public ResponseEntity<ApiResponse<Void>> handleProjectDateNotConfigured(ProjectDateNotConfiguredException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(RoleNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleRoleNotFound(RoleNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.DuplicateProjectMemberException.class)
        public ResponseEntity<ApiResponse<Void>> handleDuplicateProjectMember(com.hrm.employeemanagement.domain.exception.project.DuplicateProjectMemberException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.ProjectMemberNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleProjectMemberNotFound(com.hrm.employeemanagement.domain.exception.project.ProjectMemberNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(com.hrm.employeemanagement.domain.exception.project.MemberHasActiveTasksException.class)
        public ResponseEntity<ApiResponse<Void>> handleMemberHasActiveTasks(com.hrm.employeemanagement.domain.exception.project.MemberHasActiveTasksException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.ProjectClosedException.class)
        public ResponseEntity<ApiResponse<Void>> handleProjectClosed(com.hrm.employeemanagement.domain.exception.task.ProjectClosedException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleEmployeeNotFound(com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        }
}
