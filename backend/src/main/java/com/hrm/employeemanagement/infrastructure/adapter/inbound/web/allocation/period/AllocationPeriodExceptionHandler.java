package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.period;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.ErrorResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

/**
 * @deprecated Toàn bộ xử lý ngoại lệ được tập trung vào {@link GlobalExceptionHandler}.
 * Lớp này được giữ lại kế thừa GlobalExceptionHandler để đảm bảo tương thích ngược với các standalone test.
 */
@Deprecated
public class AllocationPeriodExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(PermissionDeniedException.class)
    @Override
    public ResponseEntity<ErrorResponse> handlePermissionDenied(PermissionDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.of("PERMISSION_DENIED", ex.getMessage(), HttpStatus.FORBIDDEN.value())
        );
    }
}
