package com.hrm.employeemanagement.domain.allocation;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.availability.YearWeek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AllocationNotificationPolicy Domain Tests (BR-03, BR-04, TC-02)")
class AllocationNotificationPolicyTest {

    @Test
    @DisplayName("Gộp tuần rỗng hoặc null trả về danh sách rỗng")
    void mergeConsecutiveWeeks_NullOrEmpty_ReturnsEmpty() {
        assertTrue(AllocationNotificationPolicy.mergeConsecutiveWeeks(null).isEmpty());
        assertTrue(AllocationNotificationPolicy.mergeConsecutiveWeeks(Collections.emptyList()).isEmpty());
    }

    @Test
    @DisplayName("Một tuần đơn lẻ tạo ra 1 dải tuần có count = 1")
    void mergeConsecutiveWeeks_SingleWeek() {
        YearWeek w1 = YearWeek.of(2026, 35);
        List<AllocationNotificationPolicy.YearWeekRange> ranges = AllocationNotificationPolicy.mergeConsecutiveWeeks(List.of(w1));

        assertEquals(1, ranges.size());
        assertEquals(w1, ranges.get(0).startWeek());
        assertEquals(w1, ranges.get(0).endWeek());
        assertEquals(1, ranges.get(0).weekCount());
        assertEquals("tuần 35/2026", ranges.get(0).toDisplayString());
    }

    @Test
    @DisplayName("TC-02 / BR-04: Nhiều tuần liên tiếp được gộp thành 1 dải tuần duy nhất")
    void mergeConsecutiveWeeks_MultipleConsecutiveWeeks_MergedToOneRange() {
        YearWeek w35 = YearWeek.of(2026, 35);
        YearWeek w36 = YearWeek.of(2026, 36);
        YearWeek w37 = YearWeek.of(2026, 37);
        YearWeek w38 = YearWeek.of(2026, 38);

        // Truyền không theo thứ tự để kiểm tra tự động sắp xếp
        List<AllocationNotificationPolicy.YearWeekRange> ranges =
                AllocationNotificationPolicy.mergeConsecutiveWeeks(List.of(w37, w35, w38, w36));

        assertEquals(1, ranges.size());
        assertEquals(w35, ranges.get(0).startWeek());
        assertEquals(w38, ranges.get(0).endWeek());
        assertEquals(4, ranges.get(0).weekCount());
        assertEquals("từ tuần 35/2026 đến tuần 38/2026", ranges.get(0).toDisplayString());
    }

    @Test
    @DisplayName("TC-05 / BR-04: Các tuần gián đoạn (cách quãng) được tách thành nhiều dải riêng biệt")
    void mergeConsecutiveWeeks_NonConsecutiveWeeks_SplitToMultipleRanges() {
        YearWeek w35 = YearWeek.of(2026, 35);
        YearWeek w36 = YearWeek.of(2026, 36);
        // Cách tuần 37
        YearWeek w38 = YearWeek.of(2026, 38);
        YearWeek w39 = YearWeek.of(2026, 39);

        List<AllocationNotificationPolicy.YearWeekRange> ranges =
                AllocationNotificationPolicy.mergeConsecutiveWeeks(List.of(w35, w38, w36, w39));

        assertEquals(2, ranges.size());

        assertEquals(w35, ranges.get(0).startWeek());
        assertEquals(w36, ranges.get(0).endWeek());
        assertEquals(2, ranges.get(0).weekCount());
        assertEquals("từ tuần 35/2026 đến tuần 36/2026", ranges.get(0).toDisplayString());

        assertEquals(w38, ranges.get(1).startWeek());
        assertEquals(w39, ranges.get(1).endWeek());
        assertEquals(2, ranges.get(1).weekCount());
        assertEquals("từ tuần 38/2026 đến tuần 39/2026", ranges.get(1).toDisplayString());
    }

    @Test
    @DisplayName("Gộp tuần xuyên năm hợp lệ (tuần 52 của năm trước và tuần 1 của năm sau)")
    void mergeConsecutiveWeeks_CrossYear_MergedCorrectly() {
        // Năm 2025 có 52 tuần ISO
        YearWeek w52_2025 = YearWeek.of(2025, 52);
        YearWeek w1_2026 = YearWeek.of(2026, 1);

        List<AllocationNotificationPolicy.YearWeekRange> ranges =
                AllocationNotificationPolicy.mergeConsecutiveWeeks(List.of(w52_2025, w1_2026));

        assertEquals(1, ranges.size());
        assertEquals(w52_2025, ranges.get(0).startWeek());
        assertEquals(w1_2026, ranges.get(0).endWeek());
        assertEquals(2, ranges.get(0).weekCount());
        assertEquals("từ tuần 52/2025 đến tuần 1/2026", ranges.get(0).toDisplayString());
    }

    @Test
    @DisplayName("BR-03: Định dạng nội dung thông báo đầy đủ người thực hiện, loại thay đổi, giá trị cũ và mới")
    void formatContent_Completeness() {
        AllocationNotificationPolicy.YearWeekRange range =
                AllocationNotificationPolicy.YearWeekRange.ofSingle(YearWeek.of(2026, 35));

        // Case ADD
        String addContent = AllocationNotificationPolicy.formatContent(
                "Lê Quản Lý", "ADD", "Nguyễn Văn A", "Dự án HRM", range, null, "40h (100%)"
        );
        assertTrue(addContent.contains("Lê Quản Lý"));
        assertTrue(addContent.contains("Nguyễn Văn A"));
        assertTrue(addContent.contains("Dự án HRM"));
        assertTrue(addContent.contains("tuần 35/2026"));
        assertTrue(addContent.contains("40h (100%)"));

        // Case EDIT
        String editContent = AllocationNotificationPolicy.formatContent(
                "Lê Quản Lý", "EDIT", "Nguyễn Văn A", "Dự án HRM", range, "20h", "40h"
        );
        assertTrue(editContent.contains("Lê Quản Lý"));
        assertTrue(editContent.contains("20h"));
        assertTrue(editContent.contains("40h"));

        // Case REMOVE (AC-01)
        String removeContent = AllocationNotificationPolicy.formatContent(
                "Lê Quản Lý", "REMOVE", "Nguyễn Văn A", "Dự án HRM", range, "40h tại tuần 35/2026", null
        );
        assertTrue(removeContent.contains("Lê Quản Lý"));
        assertTrue(removeContent.contains("gỡ bỏ"));
        assertTrue(removeContent.contains("40h tại tuần 35/2026"));
    }
}
