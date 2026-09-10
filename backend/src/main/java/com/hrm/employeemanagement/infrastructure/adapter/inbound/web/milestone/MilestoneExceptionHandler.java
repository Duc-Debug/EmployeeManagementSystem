package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.milestone;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.milestone.DuplicateMilestoneNameException;
import com.hrm.employeemanagement.domain.exception.milestone.InvalidMilestoneDataException;
import com.hrm.employeemanagement.domain.exception.milestone.MilestoneNotFoundException;
import com.hrm.employeemanagement.domain.exception.milestone.ProjectHasNoWbsException;
import com.hrm.employeemanagement.domain.exception.milestone.TaskNotInProjectException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

@RestControllerAdvice(basePackages = "com.hrm.employeemanagement.infrastructure.adapter.inbound.web.milestone")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MilestoneExceptionHandler {

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePermissionDenied(PermissionDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProjectNotFound(ProjectNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MilestoneNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleMilestoneNotFound(MilestoneNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ProjectHasNoWbsException.class)
    public ResponseEntity<ApiResponse<Void>> handleProjectHasNoWbs(ProjectHasNoWbsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateMilestoneNameException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateMilestoneName(DuplicateMilestoneNameException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(TaskNotInProjectException.class)
    public ResponseEntity<ApiResponse<Void>> handleTaskNotInProject(TaskNotInProjectException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ProjectClosedException.class)
    public ResponseEntity<ApiResponse<Void>> handleProjectClosed(ProjectClosedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidMilestoneDataException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidMilestoneData(InvalidMilestoneDataException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Tên mốc tiến độ đã tồn tại trong dự án hoặc vi phạm toàn vẹn dữ liệu"));
    }
}
