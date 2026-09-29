package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.ErrorResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

/**
 * @deprecated Toàn bộ xử lý ngoại lệ được tập trung vào {@link GlobalExceptionHandler}.
 * Lớp này được giữ lại kế thừa GlobalExceptionHandler để đảm bảo tương thích ngược với các standalone test.
 */
@Deprecated
public class MyAllocationsExceptionHandler extends GlobalExceptionHandler {
}