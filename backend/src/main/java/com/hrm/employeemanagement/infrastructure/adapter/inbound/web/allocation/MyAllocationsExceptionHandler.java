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
}