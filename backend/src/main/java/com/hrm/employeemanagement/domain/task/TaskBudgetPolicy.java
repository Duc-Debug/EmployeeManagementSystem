package com.hrm.employeemanagement.domain.task;

import java.math.BigDecimal;

/**
 * Chính sách và quy tắc nghiệp vụ liên quan đến trạng thái tiêu hao ngân sách giờ công (Burn Rate Policy):
 * - Dưới 80%: An toàn (SAFE).
 * - Từ 80% đến dưới 100%: Cảnh báo tiệm cận ngân sách (WARNING).
 * - Từ 100% trở lên: Vượt ngân sách, nguy cơ ăn mòn lợi nhuận dự án (OVER_BUDGET).
 */
public final class TaskBudgetPolicy {

    /**
     * Ngưỡng tỷ lệ tiêu hao bắt đầu kích hoạt mức CẢNH BÁO (WARNING): 80%
     */
    public static final BigDecimal WARNING_THRESHOLD_PERCENT = new BigDecimal("80.00");

    /**
     * Ngưỡng tỷ lệ tiêu hao kích hoạt mức VƯỢT NGÂN SÁCH (OVER_BUDGET): 100%
     */
    public static final BigDecimal OVER_BUDGET_THRESHOLD_PERCENT = new BigDecimal("100.00");

    private TaskBudgetPolicy() {
        // Utility / Policy class - Chặn khởi tạo thể hiện
    }

    /**
     * Kiểm tra công việc có thực sự bị vượt ngân sách (actualHours > budgetHours) hay không.
     */
    public static boolean isOverBudget(BigDecimal budgetHours, BigDecimal actualHours) {
        return budgetHours != null
                && budgetHours.compareTo(BigDecimal.ZERO) > 0
                && actualHours != null
                && actualHours.compareTo(budgetHours) > 0;
    }

    /**
     * Xác định trạng thái tiêu hao ngân sách dựa trên tỷ lệ phần trăm đã dùng và ngân sách.
     * - Dưới 80%: SAFE
     * - Từ 80% đến 100%: WARNING (Đạt trần ngân sách nhưng chưa vượt)
     * - Lớn hơn 100%: OVER_BUDGET (Vượt quá hạn mức ngân sách, ăn mòn lợi nhuận)
     *
     * @param budgetHours Số giờ ngân sách của công việc
     * @param burnedPercentage Tỷ lệ phần trăm ngân sách đã sử dụng (%)
     * @return TaskBudgetBurnStatus trạng thái đánh giá rủi ro ngân sách
     */
    public static TaskBudgetBurnStatus determineBurnStatus(BigDecimal budgetHours, BigDecimal burnedPercentage) {
        if (budgetHours == null || budgetHours.compareTo(BigDecimal.ZERO) <= 0 || burnedPercentage == null) {
            return TaskBudgetBurnStatus.NOT_SET;
        }

        if (burnedPercentage.compareTo(WARNING_THRESHOLD_PERCENT) < 0) {
            return TaskBudgetBurnStatus.SAFE;
        }

        if (burnedPercentage.compareTo(OVER_BUDGET_THRESHOLD_PERCENT) <= 0) {
            return TaskBudgetBurnStatus.WARNING;
        }

        return TaskBudgetBurnStatus.OVER_BUDGET;
    }
}
