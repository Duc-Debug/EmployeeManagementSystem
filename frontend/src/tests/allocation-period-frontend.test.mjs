import test from "node:test";
import assert from "node:assert/strict";

/**
 * NCL-06-CN-009: Frontend Logic & Validation Tests
 * Thực thi quy tắc nghiệp vụ QTN-18: Khóa kế hoạch phân bổ của kỳ
 */

// 1. Helper kiểm tra tuần thuộc kỳ kế hoạch
function isWeekWithinPeriod(period, year, weekNumber) {
  if (!period) return false;
  return (
    period.year === year &&
    weekNumber >= period.startWeek &&
    weekNumber <= period.endWeek
  );
}

// 2. Helper validation form tạo kỳ kế hoạch mới
function validateCreatePeriodForm({ name, periodType, year, startWeek, endWeek }) {
  const errors = [];
  const trimmedName = name ? name.trim() : "";

  if (!trimmedName) {
    errors.push("Tên kỳ kế hoạch không được để trống.");
  }
  if (!year || year < 2020 || year > 2050) {
    errors.push("Năm áp dụng không hợp lệ.");
  }
  if (!startWeek || startWeek < 1 || startWeek > 53) {
    errors.push("Tuần bắt đầu phải từ 1 đến 53.");
  }
  if (!endWeek || endWeek < 1 || endWeek > 53) {
    errors.push("Tuần kết thúc phải từ 1 đến 53.");
  }
  if (startWeek && endWeek && startWeek > endWeek) {
    errors.push("Tuần bắt đầu không được lớn hơn tuần kết thúc.");
  }

  return {
    isValid: errors.length === 0,
    errors,
  };
}

// 3. Helper validation mở lại kỳ kế hoạch phân bổ (TC-04)
function validateUnlockPeriodForm(reason) {
  const trimmed = reason ? reason.trim() : "";
  if (!trimmed) {
    return {
      isValid: false,
      error: "Lý do mở lại kỳ là bắt buộc.",
    };
  }
  if (trimmed.length < 10) {
    return {
      isValid: false,
      error: "Lý do mở lại kỳ phải có ít nhất 10 ký tự.",
    };
  }
  return {
    isValid: true,
    error: null,
  };
}

// 4. Helper kiểm tra phân quyền người dùng theo vai trò (RBAC)
function checkPeriodPermissions(roleCode) {
  const normalized = roleCode ? roleCode.toUpperCase().replace(/_/g, "-") : "";
  const canManage = normalized === "VT-03"; // Quản lý nguồn lực
  const canView = ["VT-01", "VT-02", "VT-03"].includes(normalized); // Ban Giám Đốc, PM, Quản lý nguồn lực
  return {
    canManage,
    canView,
  };
}

test("Allocation Planning Period Frontend Logic & QTN-18 Validation Tests (NCL-06-CN-009)", async (t) => {
  await t.test("TC-01: Kiểm tra tuần thuộc kỳ kế hoạch (isWeekWithinPeriod)", () => {
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

  await t.test("TC-02: Validation form tạo kỳ mới thành công với dữ liệu hợp lệ", () => {
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

  await t.test("TC-03: Validation form tạo kỳ từ chối tên rỗng hoặc dải tuần sai", () => {
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

  await t.test("TC-04: Validation mở lại kỳ phân bổ bắt buộc lý do >= 10 ký tự (TC-04)", () => {
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

  await t.test("TC-05: Kiểm tra phân quyền truy cập giao diện theo vai trò (RBAC)", () => {
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
});
