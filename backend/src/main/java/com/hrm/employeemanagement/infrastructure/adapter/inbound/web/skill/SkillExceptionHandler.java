package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.hrm.employeemanagement.domain.exception.skill.EmployeeSkillNotFoundException;
import com.hrm.employeemanagement.domain.exception.skill.SkillGroupNotFoundException;
import com.hrm.employeemanagement.domain.exception.skill.SkillNotFoundException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.ErrorResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

/**
 * @deprecated Toàn bộ xử lý ngoại lệ được tập trung vào {@link GlobalExceptionHandler}.
 * Lớp này được giữ lại kế thừa GlobalExceptionHandler để đảm bảo tương thích ngược với các standalone test.
 */
@Deprecated
public class SkillExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(SkillNotFoundException.class)
    @Override
    public ResponseEntity<ErrorResponse> handleSkillNotFound(SkillNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(SkillGroupNotFoundException.class)
    @Override
    public ResponseEntity<ErrorResponse> handleSkillGroupNotFound(SkillGroupNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(EmployeeSkillNotFoundException.class)
    @Override
    public ResponseEntity<ErrorResponse> handleEmployeeSkillNotFound(EmployeeSkillNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler({org.springframework.orm.ObjectOptimisticLockingFailureException.class, jakarta.persistence.OptimisticLockException.class})
    @Override
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("EMPLOYEE_SKILL_VERSION_CONFLICT",
                        "Bản ghi kỹ năng đã được cập nhật bởi một yêu cầu khác. Vui lòng tải lại trang.",
                        HttpStatus.CONFLICT.value()));
    }
}
