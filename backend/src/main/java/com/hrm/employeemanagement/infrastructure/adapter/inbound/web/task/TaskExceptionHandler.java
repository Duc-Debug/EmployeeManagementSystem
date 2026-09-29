package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

/**
 * @deprecated Toàn bộ xử lý ngoại lệ được tập trung vào {@link GlobalExceptionHandler}.
 * Lớp này được giữ lại kế thừa GlobalExceptionHandler để đảm bảo tương thích ngược với các standalone test.
 */
@Deprecated
public class TaskExceptionHandler extends GlobalExceptionHandler {
}
