package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.backup;

import com.hrm.employeemanagement.domain.backup.exception.BackupAccessDeniedException;
import com.hrm.employeemanagement.domain.backup.exception.BackupNotFoundException;
import com.hrm.employeemanagement.domain.backup.exception.BackupRestoreFailedException;
import com.hrm.employeemanagement.domain.backup.exception.InvalidBackupStatusException;
import com.hrm.employeemanagement.domain.backup.exception.InvalidRestoreConfirmationException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.hrm.employeemanagement.infrastructure.adapter.inbound.web.backup")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BackupExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BackupExceptionHandler.class);

    @ExceptionHandler(BackupAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(BackupAccessDeniedException e) {
        log.warn("Từ chối truy cập sao lưu & phục hồi: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", e.getMessage()));
    }

    @ExceptionHandler(BackupNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(BackupNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(InvalidBackupStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStatus(InvalidBackupStatusException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_BACKUP_STATUS", e.getMessage()));
    }

    @ExceptionHandler(InvalidRestoreConfirmationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidConfirmation(InvalidRestoreConfirmationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_CONFIRMATION", e.getMessage()));
    }

    @ExceptionHandler(BackupRestoreFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleRestoreFailed(BackupRestoreFailedException e) {
        log.error("Phục hồi sao lưu thất bại: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("RESTORE_FAILED", e.getMessage()));
    }
}
