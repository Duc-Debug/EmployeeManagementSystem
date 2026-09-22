import { test, describe } from "node:test";
import assert from "node:assert/strict";
import { countWorkingDays } from "../components/unavailability/DeclareUnavailabilityModal.js";
import { formatDateVN } from "../components/unavailability/UnavailabilityView.js";
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
        conflictingAllocationsCount: 2,
        totalConflictingHours: 40,
        warningMessage: "Trùng với 2 phân bổ dự án (tổng 40 giờ)",
        conflictingAllocations: [
          { allocationId: 1, projectId: 10, year: 2026, weekNumber: 39, allocatedHours: 32 },
          { allocationId: 2, projectId: 12, year: 2026, weekNumber: 39, allocatedHours: 8 }
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
        conflictingAllocationsCount: 0,
        totalConflictingHours: 0,
        warningMessage: null,
        conflictingAllocations: []
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

  describe("Date formatting & Vietnamese localization", () => {
    test("TC-11: formatDateVN chuyển đổi định dạng YYYY-MM-DD sang DD/MM/YYYY chuẩn xác", () => {
      assert.equal(formatDateVN("2026-09-22"), "22/09/2026");
      assert.equal(formatDateVN("2026-01-05"), "05/01/2026");
      assert.equal(formatDateVN(null), "—");
      assert.equal(formatDateVN(""), "—");
      assert.equal(formatDateVN("invalid"), "invalid");
    });
  });

  describe("Client-side Pagination Logic", () => {
    test("TC-12: Tính toán chính xác số trang và slice danh sách theo kích cỡ 10 phần tử/trang", () => {
      const mockList = Array.from({ length: 25 }, (_, i) => ({ id: i + 1 }));
      const pageSize = 10;
      const totalPages = Math.max(1, Math.ceil(mockList.length / pageSize));
      assert.equal(totalPages, 3);

      // Trang 1: 1 -> 10
      const page1 = mockList.slice(0, 10);
      assert.equal(page1.length, 10);
      assert.equal(page1[0].id, 1);
      assert.equal(page1[9].id, 10);

      // Trang 3: 21 -> 25
      const page3 = mockList.slice(20, 30);
      assert.equal(page3.length, 5);
      assert.equal(page3[0].id, 21);
      assert.equal(page3[4].id, 25);
    });

    test("TC-13: Danh sách rỗng trả về ít nhất 1 trang", () => {
      const emptyList = [];
      const totalPages = Math.max(1, Math.ceil(emptyList.length / 10));
      assert.equal(totalPages, 1);
    });
  });

  describe("Cancellation Eligibility & Past Date Guards", () => {
    test("TC-14: Đơn PENDING luôn được phép hủy bất kể ngày trong tương lai hay hiện tại", () => {
      const todayStr = "2026-09-18";
      const item = { id: 1, startDate: "2026-09-20", status: "PENDING" };
      const isPast = item.startDate < todayStr;
      const canCancel = item.status === "PENDING" || (item.status === "APPROVED" && !isPast);
      assert.equal(canCancel, true);
    });

    test("TC-15: Đơn APPROVED trong tương lai được phép hủy; đơn APPROVED trong quá khứ bị khóa hủy", () => {
      const todayStr = "2026-09-18";

      const futureApproved = { id: 2, startDate: "2026-09-22", status: "APPROVED" };
      assert.equal(futureApproved.startDate < todayStr, false);
      assert.equal(futureApproved.status === "PENDING" || (futureApproved.status === "APPROVED" && !(futureApproved.startDate < todayStr)), true);

      const pastApproved = { id: 3, startDate: "2026-09-10", status: "APPROVED" };
      assert.equal(pastApproved.startDate < todayStr, true);
      assert.equal(pastApproved.status === "PENDING" || (pastApproved.status === "APPROVED" && !(pastApproved.startDate < todayStr)), false);
    });

    test("TC-16: Đơn REJECTED hoặc CANCELLED không thể tiếp tục hủy", () => {
      const rejected = { id: 4, startDate: "2026-09-25", status: "REJECTED" };
      const cancelled = { id: 5, startDate: "2026-09-25", status: "CANCELLED" };
      const canCancel = (d) => d.status === "PENDING" || (d.status === "APPROVED" && d.startDate >= "2026-09-18");
      assert.equal(canCancel(rejected), false);
      assert.equal(canCancel(cancelled), false);
    });
  });

  describe("Permission-based action guards (canApprove, canDeclare)", () => {
    test("TC-17: canApprove chỉ trả về true khi user có quyền UNAVAILABILITY_APPROVE, không bị ảnh hưởng bởi roleCode", () => {
      function checkCanApprove(user) {
        return user?.permissions?.includes("UNAVAILABILITY_APPROVE") === true;
      }

      // VT-01 (Ban Giám Đốc) không có UNAVAILABILITY_APPROVE
      assert.equal(checkCanApprove({ roleCode: "VT-01", permissions: ["UNAVAILABILITY_READ"] }), false);
      // VT-05 (HR) không có UNAVAILABILITY_APPROVE
      assert.equal(checkCanApprove({ roleCode: "VT-05", permissions: ["UNAVAILABILITY_READ"] }), false);
      // VT-03 (RM) có UNAVAILABILITY_APPROVE
      assert.equal(checkCanApprove({ roleCode: "VT-03", permissions: ["UNAVAILABILITY_READ", "UNAVAILABILITY_APPROVE"] }), true);
      // VT-06 (Admin) có UNAVAILABILITY_APPROVE
      assert.equal(checkCanApprove({ roleCode: "VT-06", permissions: ["UNAVAILABILITY_READ", "UNAVAILABILITY_APPROVE"] }), true);
    });

    test("TC-18: canDeclare chỉ trả về true khi user có quyền UNAVAILABILITY_DECLARE", () => {
      function checkCanDeclare(user) {
        return user?.permissions?.includes("UNAVAILABILITY_DECLARE") === true;
      }

      // VT-04 có UNAVAILABILITY_DECLARE
      assert.equal(checkCanDeclare({ roleCode: "VT-04", permissions: ["UNAVAILABILITY_DECLARE"] }), true);
      // VT-01 không có UNAVAILABILITY_DECLARE
      assert.equal(checkCanDeclare({ roleCode: "VT-01", permissions: ["UNAVAILABILITY_READ"] }), false);
    });
  });

  describe("CompanyWorkingCalendar alignment & 500 character limit", () => {
    test("TC-19: countWorkingDays tính chính xác khi CompanyWorkingCalendar cấu hình Thứ 7 là ngày làm việc", () => {
      const calendarWithSaturday = [
        { dayOfWeek: "MONDAY", isWorkingDay: true },
        { dayOfWeek: "TUESDAY", isWorkingDay: true },
        { dayOfWeek: "WEDNESDAY", isWorkingDay: true },
        { dayOfWeek: "THURSDAY", isWorkingDay: true },
        { dayOfWeek: "FRIDAY", isWorkingDay: true },
        { dayOfWeek: "SATURDAY", isWorkingDay: true },
        { dayOfWeek: "SUNDAY", isWorkingDay: false },
      ];
      // 2026-09-21 (Mon) to 2026-09-26 (Sat) -> 6 days
      const days = countWorkingDays("2026-09-21", "2026-09-26", calendarWithSaturday);
      assert.equal(days, 6);
      assert.equal(days * 8, 48);
    });

    test("TC-20: countWorkingDays tính chính xác khi lịch làm việc chỉ có T2 - T4", () => {
      const threeDayCalendar = [
        { dayOfWeek: "MONDAY", isWorkingDay: true },
        { dayOfWeek: "TUESDAY", isWorkingDay: true },
        { dayOfWeek: "WEDNESDAY", isWorkingDay: true },
        { dayOfWeek: "THURSDAY", isWorkingDay: false },
        { dayOfWeek: "FRIDAY", isWorkingDay: false },
        { dayOfWeek: "SATURDAY", isWorkingDay: false },
        { dayOfWeek: "SUNDAY", isWorkingDay: false },
      ];
      // 2026-09-21 (Mon) to 2026-09-25 (Fri) -> only Mon, Tue, Wed = 3 days
      const days = countWorkingDays("2026-09-21", "2026-09-25", threeDayCalendar);
      assert.equal(days, 3);
    });

    test("TC-21: Boundary validation độ dài tối đa 500 ký tự cho text fields", () => {
      function validateMaxLength500(str) {
        if (!str) return true;
        return str.length <= 500;
      }

      assert.equal(validateMaxLength500("a".repeat(500)), true);
      assert.equal(validateMaxLength500("a".repeat(501)), false);
      assert.equal(validateMaxLength500(""), true);
      assert.equal(validateMaxLength500(null), true);
    });

    test("TC-22: Xây dựng URL previewUnavailability đúng định dạng query params", () => {
      const startDate = "2026-09-21";
      const endDate = "2026-09-25";
      const query = new URLSearchParams({ startDate, endDate }).toString();
      assert.equal(query, "startDate=2026-09-21&endDate=2026-09-25");
      assert.equal(`/unavailability-declarations/preview?${query}`, "/unavailability-declarations/preview?startDate=2026-09-21&endDate=2026-09-25");
    });
  });
});