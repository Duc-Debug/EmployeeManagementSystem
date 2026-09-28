package com.hrm.employeemanagement.application.dto.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
/*
*Cập nhật 1 dòng giờ công đang ở trạng thái DRAFT
 */
public record UpdateWorkLogCommand(
        Long entryId,
        Long projectId,
        Long taskId,
        LocalDate workDate,
        BigDecimal hours,
        Boolean isBillable,
        String description
) {
}
