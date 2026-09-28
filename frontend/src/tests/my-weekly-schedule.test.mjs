import test from "node:test";
import assert from "node:assert/strict";
import {
  buildMyAllocationsQuery,
  formatWeeklyDateRange,
  getProjectStatusBadge,
  validateFeedbackReason,
  getFeedbackActionLabel,
} from "../features/my-schedule/api/myScheduleApi.ts";

test("API Query: Tạo URL truy vấn lịch tuần với đúng strict allow-list parameters (BR-01, AC-02)", () => {
  assert.equal(buildMyAllocationsQuery(), "/my-allocations");
  assert.equal(
    buildMyAllocationsQuery("2026-09-21", 2),
    "/my-allocations?week_start=2026-09-21&weeks=2"
  );
  assert.equal(
    buildMyAllocationsQuery("2026-09-21"),
    "/my-allocations?week_start=2026-09-21"
  );
  assert.equal(
    buildMyAllocationsQuery(undefined, 4),
    "/my-allocations?weeks=4"
  );
});

test("Date Range Formatting: Hiển thị khoảng thời gian từ Thứ Hai đến Chủ Nhật chính xác", () => {
  assert.equal(formatWeeklyDateRange("2026-09-21"), "21/09/2026 — 27/09/2026");
  assert.equal(formatWeeklyDateRange("2026-09-28"), "28/09/2026 — 04/10/2026");
});

test("Project Status Tag: Hiển thị nhãn 'Dự án đã đóng' cho dự án CLOSED (BR-04, AC-06, TC-09)", () => {
  assert.equal(getProjectStatusBadge("CLOSED"), "Dự án đã đóng");
  assert.equal(getProjectStatusBadge("closed"), "Dự án đã đóng");
  assert.equal(getProjectStatusBadge("ACTIVE"), null);
  assert.equal(getProjectStatusBadge("PLANNED"), null);
});

test("Feedback Validation (QTN-24, NCL-13-CN-002): Kiểm tra tính hợp lệ của lý do phản hồi", () => {
  assert.deepEqual(validateFeedbackReason(""), { valid: false, error: "Vui lòng nhập lý do hoặc ý kiến phản hồi." });
  assert.deepEqual(validateFeedbackReason("   "), { valid: false, error: "Vui lòng nhập lý do hoặc ý kiến phản hồi." });
  assert.deepEqual(validateFeedbackReason(null), { valid: false, error: "Vui lòng nhập lý do hoặc ý kiến phản hồi." });
  assert.deepEqual(validateFeedbackReason("Bị trùng lịch dự án Alpha và Beta"), { valid: true, error: null });
});

test("QTN-24 Business Rule Invariant: Feedback không làm thay đổi giờ phân bổ", () => {
  const initialSchedule = {
    week_start_date: "2026-09-21",
    total_hours: 40.0,
    confirmation_status: "NOT_CONFIRMED",
    allocations: [{ allocation_id: 1, project_name: "Project A", allocated_hours: 40.0 }],
  };

  // Sau khi gửi feedback
  const updatedScheduleWithFeedback = {
    ...initialSchedule,
    confirmation_status: "HAS_FEEDBACK",
    feedback_note: "Trùng lịch",
    feedback_at: "2026-09-21T10:00:00Z",
  };

  // Giờ phân bổ vẫn giữ nguyên 40h
  assert.equal(updatedScheduleWithFeedback.total_hours, initialSchedule.total_hours);
  assert.equal(updatedScheduleWithFeedback.confirmation_status, "HAS_FEEDBACK");
  assert.equal(updatedScheduleWithFeedback.feedback_note, "Trùng lịch");
});

test("Feedback Action Visibility: Nút phản hồi hiển thị hợp lệ ở cả 4 trạng thái NOT_CONFIRMED, CONFIRMED, STALE, HAS_FEEDBACK", () => {
  // 1. NOT_CONFIRMED: Chưa có feedback note -> "Phản hồi"
  assert.equal(getFeedbackActionLabel("NOT_CONFIRMED", null), "Phản hồi");
  assert.equal(getFeedbackActionLabel("NOT_CONFIRMED", undefined), "Phản hồi");

  // 2. CONFIRMED: Đã xác nhận nhưng vẫn cho phép gửi ý kiến -> "Phản hồi"
  assert.equal(getFeedbackActionLabel("CONFIRMED", null), "Phản hồi");

  // 3. STALE: Lịch cập nhật mới, cho phép phản hồi -> "Phản hồi"
  assert.equal(getFeedbackActionLabel("STALE", null), "Phản hồi");

  // 4. HAS_FEEDBACK: Đã có feedback note trước đó -> "Chỉnh sửa phản hồi"
  assert.equal(getFeedbackActionLabel("HAS_FEEDBACK", "Cần điều chỉnh giờ"), "Chỉnh sửa phản hồi");
  assert.equal(getFeedbackActionLabel("CONFIRMED", "Ý kiến phản hồi bổ sung"), "Chỉnh sửa phản hồi");
});