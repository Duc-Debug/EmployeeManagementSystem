package com.hrm.employeemanagement.domain.allocation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.domain.availability.YearWeek;

/**
 * Pure Java Domain Policy xử lý logic nghiệp vụ thông báo phân bổ thay đổi (NCL-07-CN-003):
 * - Gộp các tuần liên tiếp thành một dải tuần duy nhất (BR-04, TC-02).
 * - Chuẩn hóa nội dung thông báo đầy đủ người thực hiện, loại thay đổi, giá trị cũ/mới (BR-03, TC-04).
 */
public final class AllocationNotificationPolicy {

    private AllocationNotificationPolicy() {
    }

    public record YearWeekRange(YearWeek startWeek, YearWeek endWeek, int weekCount) {
        public YearWeekRange {
            Objects.requireNonNull(startWeek, "startWeek không được null");
            Objects.requireNonNull(endWeek, "endWeek không được null");
            if (weekCount <= 0) {
                throw new IllegalArgumentException("weekCount phải lớn hơn 0");
            }
        }

        public static YearWeekRange ofSingle(YearWeek week) {
            return new YearWeekRange(week, week, 1);
        }

        public String toDisplayString() {
            if (startWeek.equals(endWeek)) {
                return "tuần " + startWeek.weekNumber() + "/" + startWeek.year();
            }
            return "từ tuần " + startWeek.weekNumber() + "/" + startWeek.year()
                    + " đến tuần " + endWeek.weekNumber() + "/" + endWeek.year();
        }
    }

    /**
     * Gộp danh sách các tuần thành các dải tuần liên tiếp (BR-04).
     * Hai tuần được coi là liên tiếp nếu ngày bắt đầu của tuần sau liền kề đúng 7 ngày so với tuần trước.
     */
    public static List<YearWeekRange> mergeConsecutiveWeeks(List<YearWeek> weeks) {
        if (weeks == null || weeks.isEmpty()) {
            return Collections.emptyList();
        }

        List<YearWeek> sortedWeeks = weeks.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(YearWeek::getStartDate))
                .toList();

        if (sortedWeeks.isEmpty()) {
            return Collections.emptyList();
        }

        List<YearWeekRange> ranges = new ArrayList<>();
        YearWeek rangeStart = sortedWeeks.get(0);
        YearWeek previousWeek = rangeStart;
        int count = 1;

        for (int i = 1; i < sortedWeeks.size(); i++) {
            YearWeek currentWeek = sortedWeeks.get(i);
            LocalDate expectedStartDate = previousWeek.getStartDate().plusWeeks(1);

            if (currentWeek.getStartDate().isEqual(expectedStartDate)) {
                previousWeek = currentWeek;
                count++;
            } else {
                ranges.add(new YearWeekRange(rangeStart, previousWeek, count));
                rangeStart = currentWeek;
                previousWeek = currentWeek;
                count = 1;
            }
        }

        ranges.add(new YearWeekRange(rangeStart, previousWeek, count));
        return Collections.unmodifiableList(ranges);
    }

    /**
     * Tạo tiêu đề thông báo thay đổi phân bổ.
     */
    public static String formatTitle(String projectName, String changeType) {
        String safeProject = projectName != null && !projectName.isBlank() ? projectName.trim() : "Dự án";
        return "Thông báo thay đổi phân bổ: " + safeProject;
    }

    /**
     * Tạo nội dung thông báo đầy đủ theo BR-03 (loại thay đổi, giá trị cũ/mới, người thực hiện).
     */
    public static String formatContent(
            String actorName,
            String changeType,
            String employeeName,
            String projectName,
            YearWeekRange weekRange,
            String oldValue,
            String newValue
    ) {
        String safeActor = (actorName != null && !actorName.isBlank()) ? actorName.trim() : "Người quản lý nguồn lực";
        String safeEmployee = (employeeName != null && !employeeName.isBlank()) ? employeeName.trim() : "Nhân sự";
        String safeProject = (projectName != null && !projectName.isBlank()) ? projectName.trim() : "Dự án";
        String rangeStr = weekRange != null ? weekRange.toDisplayString() : "tuần liên quan";

        String normalizedType = changeType != null ? changeType.toUpperCase().trim() : "EDIT";

        return switch (normalizedType) {
            case "ADD" -> String.format(
                    "%s đã thêm phân bổ cho nhân sự %s trong dự án '%s' tại %s: %s.",
                    safeActor, safeEmployee, safeProject, rangeStr,
                    newValue != null ? newValue : "Đã phân bổ"
            );
            case "REMOVE" -> String.format(
                    "%s đã gỡ bỏ phân bổ của nhân sự %s khỏi dự án '%s' tại %s (phân bổ cũ: %s).",
                    safeActor, safeEmployee, safeProject, rangeStr,
                    oldValue != null ? oldValue : "Chưa xác định"
            );
            default -> String.format(
                    "%s đã điều chỉnh phân bổ của nhân sự %s trong dự án '%s' tại %s (cũ: %s -> mới: %s).",
                    safeActor, safeEmployee, safeProject, rangeStr,
                    oldValue != null ? oldValue : "N/A",
                    newValue != null ? newValue : "N/A"
            );
        };
    }
}
