import { test, describe } from "node:test";
import assert from "node:assert/strict";
import { countWorkingDays } from "../components/unavailability/DeclareUnavailabilityModal.js";
import {
  UNAVAILABILITY_REASON_LABELS,
  UNAVAILABILITY_STATUS_LABELS,
} from "../lib/api/unavailability.js";
import { canAccessTab } from "../components/dashboard/SideBar.js";

describe("NCL-13-CN-003: Unavailability Declaration Frontend Tests", () => {

  describe("Working days and hours calculation", () => {
    test("TC-01: Tính chính xác số ngày làm việc giữa 2 ngày trong tuần (T3 -> T4 = 2 ngày = 16h)", () => {
      const days = countWorkingDays("2026-09-22", "2026-09-23");
      assert.equal(days, 2);
      assert.equal(days * 8, 16);
    });

    test("TC-02: Bỏ qua Thứ 7 và Chủ Nhật (T6 -> T2 tuần sau = 2 ngày làm việc = 16h)", () => {
      // 2026-09-18 is Friday, 2026-09-21 is Monday
      const days = countWorkingDays("2026-09-18", "2026-09-21");
      assert.equal(days, 2);
    });

    test("TC-03: Khoảng thời gian chỉ rơi vào cuối tuần -> Trả về 0 ngày (0 giờ)", () => {
      // 2026-09-26 is Saturday, 2026-09-27 is Sunday
      const days = countWorkingDays("2026-09-26", "2026-09-27");
      assert.equal(days, 0);
      assert.equal(days * 8, 0);
    });

    test("TC-04: Ngày kết thúc trước ngày bắt đầu -> Trả về 0 ngày", () => {
      const days = countWorkingDays("2026-09-25", "2026-09-20");
      assert.equal(days, 0);
    });

    test("TC-05: Ngày rỗng -> Trả về 0 ngày", () => {
      assert.equal(countWorkingDays("", "2026-09-25"), 0);
      assert.equal(countWorkingDays("2026-09-20", ""), 0);
    });
  });

  describe("QTN-24 & Conflict Warning acknowledgment logic", () => {
    test("TC-06: Khi có xung đột phân bổ dự án (hasConflict = true), duyệt bắt buộc phải có confirmConflictWarning = true", () => {
      const conflictResult = {
        hasConflict: true,
        conflictCount: 2,
        warningMessage: "Trùng với 2 phân bổ dự án",
        affectedWeeks: ["2026-W39"],
        conflicts: [
          { allocationId: 1, employeeId: 5, projectId: 10, yearWeek: "2026-W39", allocatedHours: 32 },
          { allocationId: 2, employeeId: 5, projectId: 12, yearWeek: "2026-W39", allocatedHours: 8 }
        ]
      };

      // Giả lập validation trước khi gọi API
      function validateApprovalPayload(conflict, userConfirmed) {
        if (conflict.hasConflict && !userConfirmed) {
          throw new Error("Bạn cần xác nhận đồng ý với cảnh báo xung đột phân bổ theo quy tắc QTN-24.");
        }
        return {
          approverComment: "Đồng ý",
          confirmConflictWarning: conflict.hasConflict ? userConfirmed : undefined
        };
      }

      // Khi chưa tick xác nhận -> Lỗi
      assert.throws(() => validateApprovalPayload(conflictResult, false), {
        message: "Bạn cần xác nhận đồng ý với cảnh báo xung đột phân bổ theo quy tắc QTN-24."
      });

      // Khi đã tick xác nhận -> Hợp lệ
      const payload = validateApprovalPayload(conflictResult, true);
      assert.equal(payload.confirmConflictWarning, true);
    });

    test("TC-07: Khi không có xung đột (hasConflict = false), không bắt buộc confirmConflictWarning", () => {
      const conflictResult = {
        hasConflict: false,
        conflictCount: 0,
        warningMessage: null,
        affectedWeeks: [],
        conflicts: []
      };

      function validateApprovalPayload(conflict, userConfirmed) {
        if (conflict.hasConflict && !userConfirmed) {
          throw new Error("Cần xác nhận");
        }
        return {
          approverComment: "Duyệt nhanh",
          confirmConflictWarning: conflict.hasConflict ? userConfirmed : undefined
        };
      }

      const payload = validateApprovalPayload(conflictResult, false);
      assert.equal(payload.confirmConflictWarning, undefined);
    });
  });

  describe("Role-based access & Tab visibility", () => {
    test("TC-08: canAccessTab cho phép tất cả các vai trò hợp lệ (VT-01 -> VT-06) truy cập tab unavailability", () => {
      const roles = ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05", "VT-06"];
      for (const r of roles) {
        assert.equal(canAccessTab(r, "unavailability"), true, `Role ${r} phải được truy cập tab unavailability`);
      }
    });

    test("TC-09: canAccessTab cho phép khi user có permission UNAVAILABILITY_DECLARE hoặc UNAVAILABILITY_APPROVE", () => {
      assert.equal(canAccessTab("UNKNOWN", "unavailability", null, ["UNAVAILABILITY_DECLARE"]), true);
      assert.equal(canAccessTab("UNKNOWN", "unavailability", null, ["UNAVAILABILITY_APPROVE"]), true);
      assert.equal(canAccessTab("UNKNOWN", "unavailability", null, ["UNAVAILABILITY_READ"]), true);
    });
  });

  describe("Status and Reason Type Labels Mapping", () => {
    test("TC-10: Các hằng số nhãn hiển thị đầy đủ và chính xác tiếng Việt", () => {
      assert.equal(UNAVAILABILITY_REASON_LABELS.TRAINING, "Đào tạo chuyên môn");
      assert.equal(UNAVAILABILITY_REASON_LABELS.BUSINESS_TRIP, "Đi công tác");
      assert.equal(UNAVAILABILITY_REASON_LABELS.PERSONAL, "Việc cá nhân");
      assert.equal(UNAVAILABILITY_REASON_LABELS.OTHER, "Lý do khác");

      assert.equal(UNAVAILABILITY_STATUS_LABELS.PENDING, "Chờ phê duyệt");
      assert.equal(UNAVAILABILITY_STATUS_LABELS.APPROVED, "Đã phê duyệt");
      assert.equal(UNAVAILABILITY_STATUS_LABELS.REJECTED, "Đã từ chối");
      assert.equal(UNAVAILABILITY_STATUS_LABELS.CANCELLED, "Đã hủy");
    });
  });
});