import test from "node:test";
import assert from "node:assert/strict";

/**
 * NCL-06-CN-009: Frontend Logic & Validation Tests
 * Thực thi quy tắc nghiệp vụ QTN-18: Khóa kế hoạch phân bổ của kỳ
 */

import {
  isWeekWithinPeriod,
  validateCreatePeriodForm,
  validateUnlockPeriodForm,
  checkPeriodPermissions,
  generateSnapshotCSV,
} from "../lib/api/allocation-periods.ts";
import { getMaxIsoWeeks } from "../lib/iso-week.ts";

test("Allocation Planning Period Frontend Logic & Validation Tests", async (t) => {
  await t.test("Kiểm tra tuần thuộc kỳ kế hoạch (isWeekWithinPeriod)", () => {
    const periodQ1 = {
      id: 1,
      name: "Kế hoạch Quý 1/2026",
      year: 2026,
      startWeek: 1,
      endWeek: 13,
      status: "LOCKED",
    };

    // Tuần 1, 7, 13 thuộc Quý 1/2026
    assert.equal(isWeekWithinPeriod(periodQ1, 2026, 1), true);
    assert.equal(isWeekWithinPeriod(periodQ1, 2026, 7), true);
    assert.equal(isWeekWithinPeriod(periodQ1, 2026, 13), true);

    // Tuần 14 hoặc khác năm không thuộc Quý 1/2026
    assert.equal(isWeekWithinPeriod(periodQ1, 2026, 14), false);
    assert.equal(isWeekWithinPeriod(periodQ1, 2025, 5), false);
  });

  await t.test("Validation form tạo kỳ mới thành công với dữ liệu hợp lệ", () => {
    const validPayload = {
      name: "Kế hoạch Quý 2/2026",
      periodType: "QUARTER",
      year: 2026,
      startWeek: 14,
      endWeek: 26,
    };

    const result = validateCreatePeriodForm(validPayload);
    assert.equal(result.isValid, true);
    assert.equal(result.errors.length, 0);
  });

  await t.test("Validation form tạo kỳ từ chối tên rỗng hoặc dải tuần sai", () => {
    // Tên rỗng
    const emptyName = validateCreatePeriodForm({
      name: "   ",
      periodType: "QUARTER",
      year: 2026,
      startWeek: 1,
      endWeek: 13,
    });
    assert.equal(emptyName.isValid, false);
    assert.ok(emptyName.errors.some((e) => e.includes("Tên kỳ")));

    // Tuần bắt đầu > tuần kết thúc
    const invalidWeeks = validateCreatePeriodForm({
      name: "Kỳ lỗi",
      periodType: "MONTH",
      year: 2026,
      startWeek: 20,
      endWeek: 10,
    });
    assert.equal(invalidWeeks.isValid, false);
    assert.ok(invalidWeeks.errors.some((e) => e.includes("lớn hơn")));

    // Tuần ngoài phạm vi 1-53
    const outOfBounds = validateCreatePeriodForm({
      name: "Kỳ ngoài dải",
      periodType: "YEAR",
      year: 2026,
      startWeek: 0,
      endWeek: 55,
    });
    assert.equal(outOfBounds.isValid, false);
    assert.equal(outOfBounds.errors.length, 2);
  });

  await t.test("Validation mở lại kỳ phân bổ bắt buộc lý do >= 10 ký tự ", () => {
    // Rỗng
    assert.equal(validateUnlockPeriodForm("").isValid, false);
    assert.equal(validateUnlockPeriodForm("   ").isValid, false);

    // Dưới 10 ký tự
    const shortReason = validateUnlockPeriodForm("Quá ngắn");
    assert.equal(shortReason.isValid, false);
    assert.ok(shortReason.error.includes("ít nhất 10 ký tự"));

    // Hợp lệ
    const validReason = validateUnlockPeriodForm("Yêu cầu điều chỉnh phân bổ theo chỉ đạo BGĐ");
    assert.equal(validReason.isValid, true);
    assert.equal(validReason.error, null);
  });

  await t.test("Kiểm tra phân quyền truy cập giao diện theo vai trò (RBAC)", () => {
    // VT-03 (Quản lý nguồn lực): Toàn quyền
    const vt03 = checkPeriodPermissions("VT-03");
    assert.equal(vt03.canManage, true);
    assert.equal(vt03.canView, true);

    // VT-01 (Ban Giám Đốc) & VT-02 (PM): Chỉ xem (Read-only)
    const vt01 = checkPeriodPermissions("VT-01");
    assert.equal(vt01.canManage, false);
    assert.equal(vt01.canView, true);

    const vt02 = checkPeriodPermissions("VT-02");
    assert.equal(vt02.canManage, false);
    assert.equal(vt02.canView, true);

    // VT-04, VT-05, VT-06: Không có quyền truy cập
    const vt04 = checkPeriodPermissions("VT-04");
    assert.equal(vt04.canManage, false);
    assert.equal(vt04.canView, false);

    const vt05 = checkPeriodPermissions("VT-05");
    assert.equal(vt05.canView, false);
  });

  await t.test("Preset Quý 4 tự động phát hiện năm 53 tuần ISO-8601 (2026)", () => {
    assert.equal(getMaxIsoWeeks(2025), 52);
    assert.equal(getMaxIsoWeeks(2026), 53);
    assert.equal(getMaxIsoWeeks(2020), 53);

    const q4EndWeek2025 = getMaxIsoWeeks(2025);
    const q4EndWeek2026 = getMaxIsoWeeks(2026);
    assert.equal(q4EndWeek2025, 52);
    assert.equal(q4EndWeek2026, 53);
  });

  await t.test("Xuất dữ liệu bản chụp Baseline thành chuỗi CSV UTF-8 đúng định dạng", () => {
    const mockSnapshot = {
      snapshotVersion: 1,
      items: [
        {
          employeeCode: "NV001",
          employeeFullName: "Nguyễn Văn A",
          projectCode: "PRJ01",
          projectName: 'Dự án "Alpha"',
          year: 2026,
          weekNumber: 10,
          allocatedHours: 40,
        },
      ],
    };

    const csv = generateSnapshotCSV(mockSnapshot, "Kế hoạch Quý 1/2026");
    assert.ok(csv.startsWith("\uFEFF")); // Có BOM UTF-8 cho Excel
    assert.ok(csv.includes("Mã Nhân Viên,Họ Và Tên,Mã Dự Án,Tên Dự Án,Năm,Tuần Phân Bổ,Số Giờ Phân Bổ"));
    assert.ok(csv.includes('"NV001","Nguyễn Văn A","PRJ01","Dự án ""Alpha""",2026,10,40'));
  });
});
