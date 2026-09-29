import test from "node:test";
import assert from "node:assert/strict";

import {
  buildAdjustPayload,
  validateMoveWeek,
  checkUserCanAdjust,
  formatActionName,
  handleRemoveError,
} from "../lib/allocation-adjustment.ts";

test("Allocation Adjustment Frontend Unit Tests", async (t) => {
  await t.test("Edit hours payload building and validation", () => {
    // Valid hours
    const payload = buildAdjustPayload({
      action: "EDIT_HOURS",
      newHours: 15,
      overloadReason: "Dự án cấp bách",
    });
    assert.equal(payload.action, "EDIT_HOURS");
    assert.equal(payload.newHours, 15);
    assert.equal(payload.overloadReason, "Dự án cấp bách");

    // Invalid: hours <= 0
    assert.throws(
      () => buildAdjustPayload({ action: "EDIT_HOURS", newHours: 0 }),
      /Số giờ phân bổ mới phải lớn hơn 0/
    );

    // Invalid: hours > 168
    assert.throws(
      () => buildAdjustPayload({ action: "EDIT_HOURS", newHours: 200 }),
      /Số giờ phân bổ không được vượt quá 168 giờ/
    );
  });

  await t.test("Move week payload and target week validations", () => {
    const payload = buildAdjustPayload({
      action: "MOVE_WEEK",
      targetYear: 2026,
      targetWeek: 45,
    });
    assert.equal(payload.action, "MOVE_WEEK");
    assert.equal(payload.targetYear, 2026);
    assert.equal(payload.targetWeek, 45);

    // Cannot move to identical week
    assert.throws(
      () => validateMoveWeek(2026, 40, 2026, 40),
      /Tuần đích phải khác tuần hiện tại/
    );

    // Cannot move to past week
    assert.throws(
      () => validateMoveWeek(2026, 40, 2026, 30, 2026, 38),
      /Không thể chuyển phân bổ đến tuần đã kết thúc/
    );

    // Valid future week
    assert.equal(validateMoveWeek(2026, 40, 2026, 45, 2026, 38), true);
  });

  await t.test("Remove allocation and 409 Conflict fallback to variance note", () => {
    // Normal error
    const normalErr = handleRemoveError("Lỗi kết nối mạng");
    assert.equal(normalErr.isConflict, false);
    assert.equal(normalErr.promptVarianceNote, false);

    // 409 conflict when week ended + actual hours exist
    const conflictErr = handleRemoveError("409 Conflict: Không thể gỡ bỏ dòng phân bổ vì tuần đã kết thúc và nhân sự đã có giờ làm việc thực tế");
    assert.equal(conflictErr.isConflict, true);
    assert.equal(conflictErr.promptVarianceNote, true);
    assert.match(conflictErr.userMessage, /Vui lòng ghi chú lý do chênh lệch/);
  });

  await t.test("Note variance payload building and validation", () => {
    const payload = buildAdjustPayload({
      action: "NOTE_VARIANCE",
      varianceReason: "Khách hàng dời lịch kiểm thử UAT",
    });
    assert.equal(payload.action, "NOTE_VARIANCE");
    assert.equal(payload.varianceReason, "Khách hàng dời lịch kiểm thử UAT");

    // Empty variance reason
    assert.throws(
      () => buildAdjustPayload({ action: "NOTE_VARIANCE", varianceReason: "   " }),
      /Lý do chênh lệch không được để trống/
    );
  });

  await t.test("RBAC permission check - Only VT-03 (Resource Manager) can adjust", () => {
    assert.equal(checkUserCanAdjust("VT-03"), true);
    assert.equal(checkUserCanAdjust("ROLE_VT_03"), true);
    assert.equal(checkUserCanAdjust("VT-01"), false); // Nhân viên
    assert.equal(checkUserCanAdjust("VT-02"), false); // PM (QLDA)
    assert.equal(checkUserCanAdjust("VT-06"), false); // Admin
    assert.equal(checkUserCanAdjust(null), false);
  });

  await t.test("Action name localization for history view", () => {
    assert.equal(formatActionName("EDIT_HOURS"), "Sửa số giờ");
    assert.equal(formatActionName("MOVE_WEEK"), "Chuyển tuần");
    assert.equal(formatActionName("REMOVE"), "Gỡ phân bổ");
    assert.equal(formatActionName("NOTE_VARIANCE"), "Ghi chú chênh lệch");
  });
});
