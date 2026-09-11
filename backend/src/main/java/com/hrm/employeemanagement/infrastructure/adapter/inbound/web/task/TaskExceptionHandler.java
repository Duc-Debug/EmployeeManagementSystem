package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.AssigneeNotInProjectException;
import com.hrm.employeemanagement.domain.exception.task.CyclicTaskHierarchyException;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
import com.hrm.employeemanagement.domain.exception.task.TaskHasChildrenException;
import com.hrm.employeemanagement.domain.exception.task.TaskHasTimesheetException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

@RestControllerAdvice(basePackages = "com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TaskExceptionHandler {

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePermissionDenied(PermissionDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProjectNotFound(ProjectNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTaskNotFound(TaskNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ProjectClosedException.class)
    public ResponseEntity<ApiResponse<Void>> handleProjectClosed(ProjectClosedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CyclicTaskHierarchyException.class)
    public ResponseEntity<ApiResponse<Void>> handleCyclicHierarchy(CyclicTaskHierarchyException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.CyclicTaskDependencyException.class)
    public ResponseEntity<ApiResponse<Void>> handleCyclicTaskDependency(com.hrm.employeemanagement.domain.exception.task.CyclicTaskDependencyException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AssigneeNotInProjectException.class)
    public ResponseEntity<ApiResponse<Void>> handleAssigneeNotInProject(AssigneeNotInProjectException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTaskDataException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTaskData(InvalidTaskDataException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(TaskHasChildrenException.class)
    public ResponseEntity<ApiResponse<Void>> handleTaskHasChildren(TaskHasChildrenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(TaskHasTimesheetException.class)
    public ResponseEntity<ApiResponse<Void>> handleTaskHasTimesheet(TaskHasTimesheetException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.task.AssigneeInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleAssigneeInactive(com.hrm.employeemanagement.domain.exception.task.AssigneeInactiveException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmployeeNotFound(com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Mã công việc đã tồn tại trong dự án hoặc vi phạm toàn vẹn dữ liệu"));
    }
}
