import test from "node:test";
import assert from "node:assert/strict";

// Helper logic mimicking AllocationAdjustmentModal & validation policies
function buildAdjustPayload({
  action,
  newHours,
  allocationPercentage,
  targetYear,
  targetWeek,
  overloadReason,
  varianceReason,
}) {
  if (!action) {
    throw new Error("Hành động điều chỉnh không được để trống");
  }

  const payload = { action };

  if (action === "EDIT_HOURS") {
    if (newHours == null || newHours <= 0) {
      throw new Error("Số giờ phân bổ mới phải lớn hơn 0");
    }
    if (newHours > 168) {
      throw new Error("Số giờ phân bổ không được vượt quá 168 giờ");
    }
    payload.newHours = newHours;
    if (allocationPercentage != null) payload.allocationPercentage = allocationPercentage;
    if (overloadReason?.trim()) payload.overloadReason = overloadReason.trim();
  } else if (action === "MOVE_WEEK") {
    if (targetYear == null || targetWeek == null) {
      throw new Error("Tuần đích và năm đích không được để trống khi chuyển tuần");
    }
    payload.targetYear = targetYear;
    payload.targetWeek = targetWeek;
  } else if (action === "NOTE_VARIANCE") {
    if (!varianceReason || !varianceReason.trim()) {
      throw new Error("Lý do chênh lệch không được để trống");
    }
    payload.varianceReason = varianceReason.trim();
  }

  return payload;
}

function validateMoveWeek(currentYear, currentWeek, targetYear, targetWeek, todayYear = 2026, todayWeek = 38) {
  if (targetYear === currentYear && targetWeek === currentWeek) {
    throw new Error("Tuần đích phải khác tuần hiện tại đang phân bổ");
  }
  if (targetYear < todayYear || (targetYear === todayYear && targetWeek < todayWeek)) {
    throw new Error("Không thể chuyển phân bổ đến tuần đã kết thúc trong quá khứ");
  }
  return true;
}

function checkUserCanAdjust(roleCode) {
  if (!roleCode) return false;
  const normalized = roleCode.toUpperCase().replace(/_/g, "-").replace(/^ROLE-/, "");
  return normalized === "VT-03";
}

function formatActionName(action) {
  switch (action) {
    case "EDIT_HOURS":
      return "Sửa số giờ";
    case "MOVE_WEEK":
      return "Chuyển tuần";
    case "REMOVE":
      return "Gỡ phân bổ";
    case "NOTE_VARIANCE":
      return "Ghi chú chênh lệch";
    default:
      return action;
  }
}

function handleRemoveError(errorMessage) {
  const isConflict =
    errorMessage.includes("409") ||
    errorMessage.toLowerCase().includes("thực tế") ||
    errorMessage.toLowerCase().includes("kết thúc") ||
    errorMessage.toLowerCase().includes("chênh lệch");

  return {
    isConflict,
    promptVarianceNote: isConflict,
    userMessage: isConflict
      ? "Không thể gỡ phân bổ vì tuần đã kết thúc và nhân sự đã có giờ làm thực tế. Vui lòng ghi chú lý do chênh lệch thay thế."
      : errorMessage,
  };
}

test("Allocation Adjustment Frontend Unit Tests (NCL-06-CN-004)", async (t) => {
  await t.test("TC-01: Edit hours payload building and validation", () => {
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

  await t.test("TC-01: Move week payload and target week validations", () => {
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

  await t.test("TC-02: Remove allocation and 409 Conflict fallback to variance note", () => {
    // Normal error
    const normalErr = handleRemoveError("Lỗi kết nối mạng");
    assert.equal(normalErr.isConflict, false);
    assert.equal(normalErr.promptVarianceNote, false);

    // TC-02: 409 conflict when week ended + actual hours exist
    const conflictErr = handleRemoveError("409 Conflict: Không thể gỡ bỏ dòng phân bổ vì tuần đã kết thúc và nhân sự đã có giờ làm việc thực tế");
    assert.equal(conflictErr.isConflict, true);
    assert.equal(conflictErr.promptVarianceNote, true);
    assert.match(conflictErr.userMessage, /Vui lòng ghi chú lý do chênh lệch/);
  });

  await t.test("TC-02: Note variance payload building and validation", () => {
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

  await t.test("TC-03: RBAC permission check - Only VT-03 (Resource Manager) can adjust", () => {
    assert.equal(checkUserCanAdjust("VT-03"), true);
    assert.equal(checkUserCanAdjust("ROLE_VT_03"), true);
    assert.equal(checkUserCanAdjust("VT-01"), false); // Nhân viên
    assert.equal(checkUserCanAdjust("VT-02"), false); // PM (QLDA)
    assert.equal(checkUserCanAdjust("VT-06"), false); // Admin
    assert.equal(checkUserCanAdjust(null), false);
  });

  await t.test("TC-04: Action name localization for history view", () => {
    assert.equal(formatActionName("EDIT_HOURS"), "Sửa số giờ");
    assert.equal(formatActionName("MOVE_WEEK"), "Chuyển tuần");
    assert.equal(formatActionName("REMOVE"), "Gỡ phân bổ");
    assert.equal(formatActionName("NOTE_VARIANCE"), "Ghi chú chênh lệch");
  });
});
