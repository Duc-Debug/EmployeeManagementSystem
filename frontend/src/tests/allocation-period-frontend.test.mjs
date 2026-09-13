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

// 2. Helper tính số tuần ISO-8601 tối đa trong năm (52 hoặc 53 tuần)
function getMaxIsoWeeks(year) {
  const dec28 = new Date(Date.UTC(year, 11, 28));
  const day = dec28.getUTCDay() || 7;
  dec28.setUTCDate(dec28.getUTCDate() + 4 - day);
  const yearStart = new Date(Date.UTC(dec28.getUTCFullYear(), 0, 1));
  return Math.ceil(((dec28.getTime() - yearStart.getTime()) / 86400000 + 1) / 7);
}

// 3. Helper validation form tạo kỳ kế hoạch mới
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

// 4. Helper validation mở lại kỳ kế hoạch phân bổ (TC-04)
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

// 5. Helper kiểm tra phân quyền người dùng theo vai trò (RBAC)
function checkPeriodPermissions(roleCode) {
  const normalized = roleCode ? roleCode.toUpperCase().replace(/_/g, "-") : "";
  const canManage = normalized === "VT-03"; // Quản lý nguồn lực
  const canView = ["VT-01", "VT-02", "VT-03"].includes(normalized); // Ban Giám Đốc, PM, Quản lý nguồn lực
  return {
    canManage,
    canView,
  };
}

// 6. Helper chuyển đổi bản chụp snapshot sang CSV format
function generateSnapshotCSV(snapshot, periodName) {
  if (!snapshot || !snapshot.items) return "";
  const headers = [
    "Mã Nhân Viên",
    "Họ Và Tên",
    "Mã Dự Án",
    "Tên Dự Án",
    "Năm",
    "Tuần Phân Bổ",
    "Số Giờ Phân Bổ",
  ];

  const rows = snapshot.items.map((it) => [
    `"${it.employeeCode}"`,
    `"${it.employeeFullName}"`,
    `"${it.projectCode}"`,
    `"${it.projectName.replace(/"/g, '""')}"`,
    it.year,
    it.weekNumber,
    it.allocatedHours,
  ]);

  return "\uFEFF" + [headers.join(","), ...rows.map((r) => r.join(","))].join("\r\n");
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

  await t.test("TC-06: Preset Quý 4 tự động phát hiện năm 53 tuần ISO-8601 (2026)", () => {
    assert.equal(getMaxIsoWeeks(2025), 52);
    assert.equal(getMaxIsoWeeks(2026), 53);
    assert.equal(getMaxIsoWeeks(2020), 53);

    const q4EndWeek2025 = getMaxIsoWeeks(2025);
    const q4EndWeek2026 = getMaxIsoWeeks(2026);
    assert.equal(q4EndWeek2025, 52);
    assert.equal(q4EndWeek2026, 53);
  });

  await t.test("TC-07: Xuất dữ liệu bản chụp Baseline thành chuỗi CSV UTF-8 đúng định dạng", () => {
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
