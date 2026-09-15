import test from "node:test";
import assert from "node:assert/strict";

/**
 * NCL-07-CN-006: Prolonged Idleness Warning Frontend Logic & Validation Tests
 * Thực thi quy tắc nghiệp vụ QTN-23: Ngưỡng quá tải và ngưỡng nhàn rỗi do Ban Giám Đốc đặt
 */

// 1. Helper phân quyền RBAC cho chức năng cảnh báo nhàn rỗi kéo dài
function canAccessProlongedIdleness(roleCode) {
  if (!roleCode) return false;
  const normalized = roleCode.toUpperCase().replace(/_/g, "-").replace(/^ROLE-/, "");
  return ["VT-01", "VT-03", "VT-06", "ADMIN"].includes(normalized);
}

// 2. Helper validation form xác nhận xử lý cảnh báo (TC-04)
function validateAcknowledgeForm({ actionTaken, notes }) {
  const errors = [];
  const trimmedAction = actionTaken ? actionTaken.trim() : "";

  if (!trimmedAction) {
    errors.push("Hành động xử lý không được để trống.");
  } else if (trimmedAction.length < 5) {
    errors.push("Hành động xử lý phải có ít nhất 5 ký tự.");
  }

  if (notes && notes.length > 500) {
    errors.push("Ghi chú không được vượt quá 500 ký tự.");
  }

  return {
    isValid: errors.length === 0,
    errors,
  };
}

// 3. Helper tính mức độ nghiêm trọng của cảnh báo (Severity Level)
function classifyIdlenessSeverity(consecutiveWeeks, threshold = 3) {
  if (consecutiveWeeks >= threshold + 2) {
    return { level: "CRITICAL", label: "Nghiêm trọng", color: "rose" };
  }
  if (consecutiveWeeks >= threshold) {
    return { level: "WARNING", label: "Cảnh báo", color: "amber" };
  }
  return { level: "NORMAL", label: "Bình thường", color: "emerald" };
}

// 4. Helper xây dựng query string cho API rà soát
function buildProlongedIdlenessQueryParams({
  orgUnitId,
  fromYear,
  fromWeek,
  durationWeeks = 4,
  consecutiveThreshold = 3,
  status,
  search,
  page = 0,
  size = 10,
}) {
  const params = new URLSearchParams();
  if (orgUnitId !== undefined && orgUnitId !== null) params.append("orgUnitId", String(orgUnitId));
  if (fromYear !== undefined && fromYear !== null) params.append("fromYear", String(fromYear));
  if (fromWeek !== undefined && fromWeek !== null) params.append("fromWeek", String(fromWeek));
  params.append("durationWeeks", String(Math.max(1, durationWeeks)));
  params.append("consecutiveThreshold", String(Math.max(1, consecutiveThreshold)));
  if (status) params.append("status", status);
  if (search && search.trim()) params.append("search", search.trim());
  params.append("page", String(Math.max(0, page)));
  params.append("size", String(Math.max(1, size)));
  return params.toString();
}

// 5. Helper tính toán tổng quan thống kê nhân sự nhàn rỗi
function calculateIdlenessMetrics(items, reportTotalEmptyHours = null) {
  if (!items || items.length === 0) {
    return { totalIdle: 0, totalEmptyHours: 0, averageUtil: 0 };
  }
  const totalIdle = items.length;
  const totalEmptyHours = reportTotalEmptyHours !== null ? reportTotalEmptyHours : items.reduce((sum, item) => sum + (item.totalEmptyHours || 0), 0);
  const sumUtil = items.reduce((sum, item) => sum + (item.averageUtilization || 0), 0);
  const averageUtil = Number((sumUtil / totalIdle).toFixed(1));
  return { totalIdle, totalEmptyHours, averageUtil };
}

// 6. Helper sinh nội dung CSV xuất báo cáo cảnh báo UTF-8 BOM
function generateIdlenessCsvContent(items) {
  const headers = [
    "Mã nhân viên",
    "Họ và tên",
    "Phòng ban",
    "Vị trí chuyên môn",
    "Số tuần nhàn rỗi liên tiếp",
    "Tỷ lệ sử dụng trung bình (%)",
    "Tổng giờ trống (h)",
    "Trạng thái",
    "Hành động can thiệp",
  ];

  const rows = items.map((item) => [
    `"${item.employeeCode}"`,
    `"${(item.fullName || "").replace(/"/g, '""')}"`,
    `"${(item.departmentName || "").replace(/"/g, '""')}"`,
    `"${(item.positionTitle || "").replace(/"/g, '""')}"`,
    item.consecutiveIdleWeeks,
    item.averageUtilization,
    item.totalEmptyHours,
    `"${item.status === 'ACKNOWLEDGED' ? 'Đã xử lý' : 'Chưa xử lý'}"`,
    `"${(item.actionTaken || '').replace(/"/g, '""')}"`,
  ]);

  return "\uFEFF" + [headers.join(","), ...rows.map((r) => r.join(","))].join("\n");
}

test("Prolonged Idleness Warning Frontend Logic & QTN-23 Tests (NCL-07-CN-006)", async (t) => {
  await t.test("TC-01: Phân quyền RBAC — Chỉ VT-01 (BGĐ), VT-03 (RM), VT-06 (Admin) được truy cập", () => {
    // Allowed
    assert.strictEqual(canAccessProlongedIdleness("VT-03"), true);
    assert.strictEqual(canAccessProlongedIdleness("ROLE_VT_03"), true);
    assert.strictEqual(canAccessProlongedIdleness("VT-01"), true);
    assert.strictEqual(canAccessProlongedIdleness("VT-06"), true);
    assert.strictEqual(canAccessProlongedIdleness("ADMIN"), true);

    // Denied
    assert.strictEqual(canAccessProlongedIdleness("VT-02"), false); // PM
    assert.strictEqual(canAccessProlongedIdleness("VT-04"), false); // Nhân viên
    assert.strictEqual(canAccessProlongedIdleness("VT-05"), false); // HR
    assert.strictEqual(canAccessProlongedIdleness(null), false);
    assert.strictEqual(canAccessProlongedIdleness(""), false);
  });

  await t.test("TC-02: Form Validation xử lý cảnh báo (TC-04) — Bắt buộc hành động xử lý", () => {
    // Valid
    const validResult = validateAcknowledgeForm({
      actionTaken: "Điều chuyển sang dự án mới Portal",
      notes: "Đã trao đổi với PM",
    });
    assert.strictEqual(validResult.isValid, true);
    assert.strictEqual(validResult.errors.length, 0);

    // Action rỗng
    const emptyActionResult = validateAcknowledgeForm({
      actionTaken: "   ",
      notes: "ghi chú",
    });
    assert.strictEqual(emptyActionResult.isValid, false);
    assert.ok(emptyActionResult.errors.includes("Hành động xử lý không được để trống."));

    // Action quá ngắn (< 5 ký tự)
    const shortActionResult = validateAcknowledgeForm({
      actionTaken: "ABC",
    });
    assert.strictEqual(shortActionResult.isValid, false);
    assert.ok(shortActionResult.errors.includes("Hành động xử lý phải có ít nhất 5 ký tự."));

    // Notes quá dài (> 500 ký tự)
    const longNotes = "a".repeat(501);
    const longNotesResult = validateAcknowledgeForm({
      actionTaken: "Điều chuyển dự án",
      notes: longNotes,
    });
    assert.strictEqual(longNotesResult.isValid, false);
    assert.ok(longNotesResult.errors.includes("Ghi chú không được vượt quá 500 ký tự."));
  });

  await t.test("TC-03: Phân loại mức độ nghiêm trọng dựa trên chuỗi tuần liên tiếp (QTN-23)", () => {
    // Ngưỡng chuẩn 3 tuần
    const normal = classifyIdlenessSeverity(2, 3);
    assert.strictEqual(normal.level, "NORMAL");

    const warning = classifyIdlenessSeverity(3, 3);
    assert.strictEqual(warning.level, "WARNING");
    assert.strictEqual(warning.color, "amber");

    const critical = classifyIdlenessSeverity(5, 3);
    assert.strictEqual(critical.level, "CRITICAL");
    assert.strictEqual(critical.color, "rose");
  });

  await t.test("TC-04: Xây dựng Query Parameters URL chính xác cho API backend", () => {
    const query = buildProlongedIdlenessQueryParams({
      orgUnitId: 10,
      fromYear: 2026,
      fromWeek: 38,
      durationWeeks: 4,
      consecutiveThreshold: 3,
      search: "Nguyen Van A",
      page: 0,
      size: 10,
    });

    assert.ok(query.includes("orgUnitId=10"));
    assert.ok(query.includes("fromYear=2026"));
    assert.ok(query.includes("fromWeek=38"));
    assert.ok(query.includes("durationWeeks=4"));
    assert.ok(query.includes("consecutiveThreshold=3"));
    assert.ok(query.includes("search=Nguyen+Van+A"));
    assert.ok(query.includes("page=0"));
    assert.ok(query.includes("size=10"));
  });

  await t.test("TC-05: Tính toán tổng quan KPI số người nhàn rỗi và tổng giờ trống", () => {
    const mockStaff = [
      { employeeId: 101, consecutiveIdleWeeks: 3, totalEmptyHours: 90.0, averageUtilization: 25.0 },
      { employeeId: 102, consecutiveIdleWeeks: 4, totalEmptyHours: 120.0, averageUtilization: 15.0 },
    ];

    const metrics = calculateIdlenessMetrics(mockStaff);
    assert.strictEqual(metrics.totalIdle, 2);
    assert.strictEqual(metrics.totalEmptyHours, 210.0);
    assert.strictEqual(metrics.averageUtil, 20.0);

    const emptyMetrics = calculateIdlenessMetrics([]);
    assert.strictEqual(emptyMetrics.totalIdle, 0);
    assert.strictEqual(emptyMetrics.totalEmptyHours, 0);
    assert.strictEqual(emptyMetrics.averageUtil, 0);
  });

  await t.test("TC-06: Xuất file CSV báo cáo cảnh báo nhàn rỗi chuẩn định dạng UTF-8 BOM", () => {
    const mockStaff = [
      {
        employeeCode: "EMP0101",
        fullName: 'Nguyễn Văn "Pro" A',
        departmentName: "Khối Kỹ Thuật",
        positionTitle: "Backend Dev",
        consecutiveIdleWeeks: 3,
        averageUtilization: 25.0,
        totalEmptyHours: 90.0,
      },
    ];

    const csv = generateIdlenessCsvContent(mockStaff);
    assert.ok(csv.startsWith("\uFEFF"));
    assert.ok(csv.includes("Mã nhân viên,Họ và tên,Phòng ban"));
    assert.ok(csv.includes('"EMP0101"'));
    assert.ok(csv.includes('"Nguyễn Văn ""Pro"" A"'));
    assert.ok(csv.includes("90"));
  });
});
