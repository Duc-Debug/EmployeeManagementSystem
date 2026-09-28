package com.hrm.employeemanagement.domain.task;

/**
 * Trạng thái tiêu hao ngân sách giờ công của công việc.
 */
public enum TaskBudgetBurnStatus {
    /** Chưa đặt ngân sách giờ */
    NOT_SET,
    /** An toàn: Đã dùng < 80% ngân sách */
    SAFE,
    /** Cảnh báo: Đã dùng từ 80% đến < 100% ngân sách */
    WARNING,
    /** Vượt ngân sách / Ăn mòn lợi nhuận: Đã dùng >= 100% ngân sách */
    OVER_BUDGET
}
