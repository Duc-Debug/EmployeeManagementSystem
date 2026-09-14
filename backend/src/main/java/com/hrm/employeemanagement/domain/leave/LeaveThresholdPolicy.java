package com.hrm.employeemanagement.domain.leave;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Domain Policy xác định quy tắc cảnh báo khi số người nghỉ cùng ngày vượt ngưỡng
 * (NCL-05-CN-006 & TC-02).
 */
public final class LeaveThresholdPolicy {

    public static final double DEFAULT_WARNING_THRESHOLD_PERCENTAGE = 0.50;

    private LeaveThresholdPolicy() {
        // utility class
    }

    /**
     * Kiểm tra xem ngày có cảnh báo số người nghỉ hay không (đạt hoặc vượt ngưỡng).
     */
    public static boolean isWarningExceeded(
            int totalEmployeesInDept,
            int onLeaveCount,
            Double thresholdRate) {
        return isWarningExceeded(
                totalEmployeesInDept,
                onLeaveCount,
                thresholdRate,
                true
        );
    }

    /**
     * Kiểm tra cảnh báo, kèm điều kiện ngày làm việc của công ty.
     * Không cảnh báo vào cuối tuần hoặc ngày nghỉ lễ.
     */
    public static boolean isWarningExceeded(
            int totalEmployeesInDept,
            int onLeaveCount,
            Double thresholdRate,
            boolean isCompanyWorkingDay) {
        if (!isCompanyWorkingDay) {
            return false;
        }

        if (totalEmployeesInDept <= 0 || onLeaveCount <= 0) {
            return false;
        }

        double threshold =
                thresholdRate != null && thresholdRate > 0 && thresholdRate <= 1.0
                        ? thresholdRate
                        : DEFAULT_WARNING_THRESHOLD_PERCENTAGE;

        double currentRatio = (double) onLeaveCount / totalEmployeesInDept;
        return currentRatio >= threshold;
    }

    /**
     * Tạo thông điệp cảnh báo khi đạt hoặc vượt ngưỡng.
     */
    public static String buildWarningMessage(
            LocalDate date,
            int totalEmployeesInDept,
            int onLeaveCount,
            Double thresholdRate) {
        return buildWarningMessage(
                date,
                totalEmployeesInDept,
                onLeaveCount,
                thresholdRate,
                true
        );
    }

    public static String buildWarningMessage(
            LocalDate date,
            int totalEmployeesInDept,
            int onLeaveCount,
            Double thresholdRate,
            boolean isCompanyWorkingDay) {
        if (!isWarningExceeded(
                totalEmployeesInDept,
                onLeaveCount,
                thresholdRate,
                isCompanyWorkingDay)) {
            return null;
        }

        double threshold =
                thresholdRate != null && thresholdRate > 0 && thresholdRate <= 1.0
                        ? thresholdRate
                        : DEFAULT_WARNING_THRESHOLD_PERCENTAGE;

        double currentPercentage =
                ((double) onLeaveCount / totalEmployeesInDept) * 100.0;
        double thresholdPercentage = threshold * 100.0;

        return String.format(
                Locale.ROOT,
                "Cảnh báo: Có %d/%d nhân sự (%s) nghỉ ngày %s, đạt hoặc vượt ngưỡng quy định (%s)",
                onLeaveCount,
                totalEmployeesInDept,
                String.format(Locale.ROOT, "%.1f%%", currentPercentage),
                date.toString(),
                String.format(Locale.ROOT, "%.1f%%", thresholdPercentage)
        );
    }
}