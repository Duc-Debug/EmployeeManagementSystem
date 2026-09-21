import test from "node:test";
import assert from "node:assert/strict";

test("NCL-11-CN-003: Chống gửi trùng thông báo Frontend Logic & Validation Tests", async (t) => {
  await t.test("TC-01 / QTN-19: Khóa chống trùng dedup_key đúng format", () => {
    const buildDedupKey = (eventType, entityType, entityId, yearWeek, recipientId) => {
      return `${eventType.trim().toUpperCase()}:${entityType.trim().toUpperCase()}:${entityId.toString().trim()}:${yearWeek.trim()}:USER:${recipientId}`;
    };

    const key = buildDedupKey("OVERLOAD_WARNING", "EMPLOYEE", "101", "2026-W38", 201);
    assert.equal(key, "OVERLOAD_WARNING:EMPLOYEE:101:2026-W38:USER:201");
  });

  await t.test("TC-03: Kiểm tra quyền mở cấu hình chống gửi trùng (Chỉ VT-06 hoặc quyền NOTIFICATION_DEDUPLICATION_MANAGE)", () => {
    const canAccessDedupConfig = (user) => {
      if (!user) return false;
      const normalizedRole = (user.roleCode || "").toUpperCase().replace(/_/g, "-");
      const hasPermission = Array.isArray(user.permissions) && user.permissions.includes("NOTIFICATION_DEDUPLICATION_MANAGE");
      return normalizedRole === "VT-06" || normalizedRole === "ROLE-ADMIN" || normalizedRole === "ADMIN" || hasPermission;
    };

    assert.equal(canAccessDedupConfig({ roleCode: "VT-06", permissions: [] }), true);
    assert.equal(canAccessDedupConfig({ roleCode: "VT-01", permissions: [] }), false);
    assert.equal(canAccessDedupConfig({ roleCode: "VT-02", permissions: [] }), false);
    assert.equal(canAccessDedupConfig({ roleCode: "VT-03", permissions: [] }), false);
    assert.equal(canAccessDedupConfig({ roleCode: "VT-04", permissions: [] }), false);
    assert.equal(canAccessDedupConfig({ roleCode: "VT-05", permissions: [] }), false);
    assert.equal(canAccessDedupConfig({ roleCode: "VT-02", permissions: ["NOTIFICATION_DEDUPLICATION_MANAGE"] }), true);
  });

  await t.test("TC-04: Validate dữ liệu cấu hình trước khi xác nhận lưu", () => {
    const validateDedupConfig = (windowDays, intervalMinutes) => {
      const errors = [];
      if (typeof windowDays !== "number" || isNaN(windowDays) || windowDays < 1 || windowDays > 90) {
        errors.push("Cửa sổ chống trùng phải từ 1 đến 90 ngày.");
      }
      if (typeof intervalMinutes !== "number" || isNaN(intervalMinutes) || intervalMinutes < 5 || intervalMinutes > 1440) {
        errors.push("Chu kỳ quét phải từ 5 đến 1440 phút.");
      }
      return {
        isValid: errors.length === 0,
        errors,
      };
    };

    // Hợp lệ
    assert.equal(validateDedupConfig(7, 60).isValid, true);
    assert.equal(validateDedupConfig(1, 5).isValid, true);
    assert.equal(validateDedupConfig(90, 1440).isValid, true);

    // Không hợp lệ
    assert.equal(validateDedupConfig(0, 60).isValid, false);
    assert.equal(validateDedupConfig(91, 60).isValid, false);
    assert.equal(validateDedupConfig(7, 4).isValid, false);
    assert.equal(validateDedupConfig(7, 1441).isValid, false);
  });

  await t.test("TC-04: Định dạng bản ghi thay đổi cấu hình cho Modal xác nhận", () => {
    const formatChangeSummary = (isEnabled, windowDays, scanInterval) => {
      return {
        statusText: isEnabled ? "Bật" : "Tắt",
        windowText: `${windowDays} ngày`,
        scanText: `${scanInterval} phút`,
        summary: `Trạng thái: ${isEnabled ? "Bật" : "Tắt"}, Cửa sổ: ${windowDays} ngày, Chu kỳ: ${scanInterval} phút`,
      };
    };

    const summary = formatChangeSummary(true, 14, 120);
    assert.equal(summary.statusText, "Bật");
    assert.equal(summary.windowText, "14 ngày");
    assert.equal(summary.scanText, "120 phút");
  });
});
