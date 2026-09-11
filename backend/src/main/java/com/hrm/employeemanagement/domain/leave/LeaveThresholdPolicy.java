package com.hrm.employeemanagement.domain.leave;

import java.time.LocalDate;

/**
 * Domain Policy xác định quy tắc cảnh báo khi số người nghỉ cùng ngày vượt ngưỡng (NCL-05-CN-006 & TC-02).
 */
public final class LeaveThresholdPolicy {

    public static final double DEFAULT_WARNING_THRESHOLD_PERCENTAGE = 0.50; // 50% số người trong bộ phận

    private LeaveThresholdPolicy() {
        // utility class
    }

    /**
     * Kiểm tra xem ngày có vượt ngưỡng cảnh báo số người nghỉ hay không.
     *
     * @param totalEmployeesInDept Tổng số nhân sự đang hoạt động trong bộ phận.
     * @param onLeaveCount          Số lượng nhân sự có lịch nghỉ (APPROVED hoặc PENDING) trong ngày đó.
     * @param thresholdRate         Tỷ lệ ngưỡng cảnh báo (0.0 < thresholdRate <= 1.0). Mặc định 0.5 (50%).
     * @return true nếu số người nghỉ vượt hoặc bằng ngưỡng quy định.
     */
    public static boolean isWarningExceeded(int totalEmployeesInDept, int onLeaveCount, Double thresholdRate) {
        return isWarningExceeded(totalEmployeesInDept, onLeaveCount, thresholdRate, true);
    }

    /**
     * Kiểm tra xem ngày có vượt ngưỡng cảnh báo số người nghỉ hay không, kèm điều kiện là ngày làm việc công ty.
     * Bỏ qua cảnh báo nếu là ngày nghỉ cuối tuần (Thứ 7/CN) hoặc ngày nghỉ lễ (NCL-05-CN-006 Cải tiến P1).
     */
    public static boolean isWarningExceeded(int totalEmployeesInDept, int onLeaveCount, Double thresholdRate, boolean isCompanyWorkingDay) {
        if (!isCompanyWorkingDay) {
            return false;
        }
        if (totalEmployeesInDept <= 0 || onLeaveCount <= 0) {
            return false;
        }
        double threshold = (thresholdRate != null && thresholdRate > 0 && thresholdRate <= 1.0)
                ? thresholdRate
                : DEFAULT_WARNING_THRESHOLD_PERCENTAGE;

        double currentRatio = (double) onLeaveCount / totalEmployeesInDept;
        return currentRatio >= threshold;
    }

    /**
     * Tạo thông điệp cảnh báo định dạng rõ ràng khi vượt ngưỡng.
     */
    public static String buildWarningMessage(LocalDate date, int totalEmployeesInDept, int onLeaveCount, Double thresholdRate) {
        return buildWarningMessage(date, totalEmployeesInDept, onLeaveCount, thresholdRate, true);
    }

    public static String buildWarningMessage(LocalDate date, int totalEmployeesInDept, int onLeaveCount, Double thresholdRate, boolean isCompanyWorkingDay) {
        if (!isWarningExceeded(totalEmployeesInDept, onLeaveCount, thresholdRate, isCompanyWorkingDay)) {
            return null;
        }
        double threshold = (thresholdRate != null && thresholdRate > 0 && thresholdRate <= 1.0)
                ? thresholdRate
                : DEFAULT_WARNING_THRESHOLD_PERCENTAGE;

        double currentPercentage = ((double) onLeaveCount / totalEmployeesInDept) * 100.0;
        double thresholdPercentage = threshold * 100.0;

        return String.format(
                "Cảnh báo: Có %d/%d nhân sự (%s) nghỉ ngày %s, vượt ngưỡng quy định (%s)",
                onLeaveCount,
                totalEmployeesInDept,
                String.format("%.1f%%", currentPercentage),
                date.toString(),
                String.format("%.1f%%", thresholdPercentage)
        );
    }
}
