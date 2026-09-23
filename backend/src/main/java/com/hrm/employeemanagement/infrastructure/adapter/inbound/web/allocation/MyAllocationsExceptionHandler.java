package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = MyAllocationsController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MyAllocationsExceptionHandler {

    @ExceptionHandler(InvalidQueryParameterException.class)
    public ResponseEntity<MyAllocationsErrorResponse> handleInvalidQueryParameter(InvalidQueryParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MyAllocationsErrorResponse.of(400, "INVALID_QUERY_PARAMETER", ex.getMessage()));
    }

    @ExceptionHandler(InvalidWeekFormatException.class)
    public ResponseEntity<MyAllocationsErrorResponse> handleInvalidWeekFormat(InvalidWeekFormatException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MyAllocationsErrorResponse.of(400, "INVALID_WEEK_FORMAT", ex.getMessage()));
    }

    @ExceptionHandler(InvalidWeeksFormatException.class)
    public ResponseEntity<MyAllocationsErrorResponse> handleInvalidWeeksFormat(InvalidWeeksFormatException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MyAllocationsErrorResponse.of(400, "INVALID_WEEKS_FORMAT", ex.getMessage()));
    }

    @ExceptionHandler(WeekStartNotMondayException.class)
    public ResponseEntity<MyAllocationsErrorResponse> handleWeekStartNotMonday(WeekStartNotMondayException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MyAllocationsErrorResponse.of(400, "WEEK_START_NOT_MONDAY", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<MyAllocationsErrorResponse> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MyAllocationsErrorResponse.of(400, "INVALID_FEEDBACK_REASON", msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MyAllocationsErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MyAllocationsErrorResponse.of(400, "INVALID_FEEDBACK_REASON", ex.getMessage()));
    }
}