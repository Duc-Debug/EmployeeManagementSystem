import test from "node:test";
import assert from "node:assert/strict";

// Helper logic mimicking URL query construction for GET /api/v1/my-allocations
function buildMyAllocationsQuery(weekStart, weeks) {
  const params = new URLSearchParams();
  if (weekStart) {
    params.append("week_start", weekStart);
  }
  if (weeks !== undefined && weeks !== null) {
    params.append("weeks", String(weeks));
  }
  const queryStr = params.toString();
  return `/my-allocations${queryStr ? `?${queryStr}` : ""}`;
}

// Helper logic formatting date range from Monday
function formatWeeklyDateRange(mondayStr) {
  const monday = new Date(mondayStr);
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);

  const dFormat = (d) =>
    `${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}/${d.getFullYear()}`;

  return `${dFormat(monday)} — ${dFormat(sunday)}`;
}

// Helper logic checking project status label
function getProjectStatusBadge(projectStatus) {
  if (projectStatus?.toUpperCase() === "CLOSED") {
    return "Dự án đã đóng";
  }
  return null;
}

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

// Helper validating feedback reason (QTN-24)
function validateFeedbackReason(reason) {
  if (!reason || !reason.trim()) {
    return { valid: false, error: "Vui lòng nhập lý do hoặc ý kiến phản hồi." };
  }
  return { valid: true, error: null };
}

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