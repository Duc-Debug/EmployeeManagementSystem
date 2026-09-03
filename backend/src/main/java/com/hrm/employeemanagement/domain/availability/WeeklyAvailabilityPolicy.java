package com.hrm.employeemanagement.domain.availability;

import com.hrm.employeemanagement.domain.exception.availability.InvalidAvailabilityHoursException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Triển khai quy tắc nghiệp vụ QTN-10:
 * "Năng lực khả dụng trừ ngày lễ và nghỉ phép:
 *  Số giờ khả dụng của một người trong tuần bằng giờ chuẩn trừ ngày lễ và số giờ nghỉ phép đã duyệt.
 *  Không trừ các đơn nghỉ phép còn đang chờ duyệt."
 */
public class WeeklyAvailabilityPolicy {

    public static final int DEFAULT_HOURS_PER_HOLIDAY = 8;
    public static final int MAX_HOURS_PER_WEEK = 168;

    /**
     * Xác thực số giờ làm việc chuẩn trong tuần.
     */
    public static void validateStandardHours(Integer standardHours) {
        if (standardHours == null || standardHours <= 0) {
            throw new InvalidAvailabilityHoursException("Số giờ làm việc chuẩn mỗi tuần phải lớn hơn 0");
        }
        if (standardHours > MAX_HOURS_PER_WEEK) {
            throw new InvalidAvailabilityHoursException("Số giờ làm việc chuẩn mỗi tuần không được vượt quá " + MAX_HOURS_PER_WEEK + " giờ");
        }
    }

    /**
     * Tính tổng số giờ nghỉ lễ trong một tuần.
     * Chỉ tính các ngày lễ rơi vào ngày làm việc hành chính (Thứ 2 đến Thứ 6).
     */
    public static int calculateHolidayHours(YearWeek yearWeek, List<LocalDate> holidayDates) {
        if (holidayDates == null || holidayDates.isEmpty()) {
            return 0;
        }

        LocalDate startDate = yearWeek.getStartDate();
        LocalDate endDate = yearWeek.getEndDate();

        int workingDayHolidayCount = 0;
        for (LocalDate holiday : holidayDates) {
            if (holiday != null && !holiday.isBefore(startDate) && !holiday.isAfter(endDate)) {
                DayOfWeek dow = holiday.getDayOfWeek();
                if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                    workingDayHolidayCount++;
                }
            }
        }

        return workingDayHolidayCount * DEFAULT_HOURS_PER_HOLIDAY;
    }

    /**
     * Tính số giờ khả dụng thực tế của nhân sự trong tuần theo QTN-10.
     *
     * @param standardHours Giờ chuẩn trong tuần của nhân viên
     * @param holidayHours Tổng số giờ ngày lễ trong tuần
     * @param approvedLeaveHours Tổng số giờ nghỉ phép ĐÃ DUYỆT (APPROVED) trong tuần
     * @return Số giờ khả dụng ròng (luôn >= 0.0)
     */
    public static BigDecimal calculateNetAvailableHours(int standardHours, int holidayHours, BigDecimal approvedLeaveHours) {
        validateStandardHours(standardHours);

        int safeHolidayHours = Math.max(0, holidayHours);
        BigDecimal safeLeaveHours = approvedLeaveHours != null && approvedLeaveHours.compareTo(BigDecimal.ZERO) > 0
                ? approvedLeaveHours
                : BigDecimal.ZERO;

        BigDecimal baseHours = BigDecimal.valueOf(standardHours);
        BigDecimal deductions = BigDecimal.valueOf(safeHolidayHours).add(safeLeaveHours);
        BigDecimal netHours = baseHours.subtract(deductions);

        if (netHours.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return netHours.setScale(2, RoundingMode.HALF_UP);
    }
}
