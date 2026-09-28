/**
 * Pure functions and validation logic for Allocation Adjustments (NCL-06-CN-004 / QTN-15)
 */

export interface AdjustPayloadParams {
  action: string;
  newHours?: number | null;
  allocationPercentage?: number | null;
  targetYear?: number | null;
  targetWeek?: number | null;
  overloadReason?: string | null;
  varianceReason?: string | null;
}

export function buildAdjustPayload({
  action,
  newHours,
  allocationPercentage,
  targetYear,
  targetWeek,
  overloadReason,
  varianceReason,
}: AdjustPayloadParams) {
  if (!action) {
    throw new Error("Hành động điều chỉnh không được để trống");
  }

  const payload: Record<string, any> = { action };

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

export function validateMoveWeek(
  currentYear: number,
  currentWeek: number,
  targetYear: number,
  targetWeek: number,
  todayYear = 2026,
  todayWeek = 38
): boolean {
  if (targetYear === currentYear && targetWeek === currentWeek) {
    throw new Error("Tuần đích phải khác tuần hiện tại đang phân bổ");
  }
  if (targetYear < todayYear || (targetYear === todayYear && targetWeek < todayWeek)) {
    throw new Error("Không thể chuyển phân bổ đến tuần đã kết thúc trong quá khứ");
  }
  return true;
}

export function checkUserCanAdjust(roleCode?: string | null): boolean {
  if (!roleCode) return false;
  const normalized = roleCode.toUpperCase().replace(/_/g, "-").replace(/^ROLE-/, "");
  return normalized === "VT-03";
}

export function formatActionName(action: string): string {
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

export function handleRemoveError(errorMessage: string) {
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
