import test from "node:test";
import assert from "node:assert/strict";
import {
  buildDedupKey,
  canAccessDedupConfig,
  validateDedupConfig,
  formatChangeSummary,
} from "../lib/api/notification-dedup.ts";

test("NCL-11-CN-003: Chống gửi trùng thông báo Frontend Logic & Validation Tests", async (t) => {
  await t.test("TC-01 / QTN-19: Khóa chống trùng dedup_key đúng format", () => {
    const key = buildDedupKey("OVERLOAD_WARNING", "EMPLOYEE", "101", "2026-W38", 201);
    assert.equal(key, "OVERLOAD_WARNING:EMPLOYEE:101:2026-W38:USER:201");
  });

  await t.test("TC-03: Kiểm tra quyền mở cấu hình chống gửi trùng (Chỉ VT-06 hoặc quyền NOTIFICATION_DEDUPLICATION_MANAGE)", () => {
    assert.equal(canAccessDedupConfig({ roleCode: "VT-06", permissions: [] }), true);
    assert.equal(canAccessDedupConfig({ roleCode: "VT-01", permissions: [] }), false);
    assert.equal(canAccessDedupConfig({ roleCode: "VT-02", permissions: [] }), false);
    assert.equal(canAccessDedupConfig({ roleCode: "VT-03", permissions: [] }), false);
    assert.equal(canAccessDedupConfig({ roleCode: "VT-04", permissions: [] }), false);
    assert.equal(canAccessDedupConfig({ roleCode: "VT-05", permissions: [] }), false);
    assert.equal(canAccessDedupConfig({ roleCode: "VT-02", permissions: ["NOTIFICATION_DEDUPLICATION_MANAGE"] }), true);
  });

  await t.test("TC-04: Validate dữ liệu cấu hình trước khi xác nhận lưu", () => {
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
    const summary = formatChangeSummary(true, 14, 120);
    assert.equal(summary.statusText, "Bật");
    assert.equal(summary.windowText, "14 ngày");
    assert.equal(summary.scanText, "120 phút");
  });
});
