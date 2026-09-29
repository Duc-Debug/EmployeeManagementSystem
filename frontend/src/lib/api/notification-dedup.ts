"use client";

import { apiRequest } from "../api-client";
import { normalizeRoleCode } from "../role-utils";

export interface NotificationDedupConfig {
  isEnabled: boolean;
  dedupWindowDays: number;
  scanIntervalMinutes: number;
  updatedBy: number | null;
  updatedAt: string | null;
  version: number;
}

export interface UpdateNotificationDedupConfigCommand {
  isEnabled: boolean;
  dedupWindowDays: number;
  scanIntervalMinutes: number;
}

export interface OverloadScanResult {
  totalScanned: number;
  overloadedCount: number;
  newlyAlertedCount: number;
  skippedDedupCount: number;
  resolvedCount: number;
}

/**
 * Lấy cấu hình chống gửi trùng thông báo (TC-03)
 */
export async function getNotificationDedupConfig(): Promise<NotificationDedupConfig> {
  return await apiRequest<NotificationDedupConfig>("/admin/notification-dedup-config");
}

/**
 * Cập nhật cấu hình chống gửi trùng thông báo (TC-04)
 */
export async function updateNotificationDedupConfig(
  command: UpdateNotificationDedupConfigCommand
): Promise<NotificationDedupConfig> {
  return await apiRequest<NotificationDedupConfig>("/admin/notification-dedup-config", {
    method: "PUT",
    body: JSON.stringify(command),
  });
}

/**
 * Kích hoạt quét quá tải thủ công (Admin utility)
 */
export async function triggerOverloadScan(
  year?: number,
  weekNumber?: number
): Promise<OverloadScanResult> {
  const params = new URLSearchParams();
  if (year !== undefined) params.append("year", year.toString());
  if (weekNumber !== undefined) params.append("weekNumber", weekNumber.toString());

  const query = params.toString();
  const url = query
    ? `/admin/notification-dedup-config/trigger-scan?${query}`
    : "/admin/notification-dedup-config/trigger-scan";

  return await apiRequest<OverloadScanResult>(url, {
    method: "POST",
  });
}

export function buildDedupKey(
  eventType: string,
  entityType: string,
  entityId: string | number,
  yearWeek: string,
  recipientId: string | number
): string {
  return `${eventType.trim().toUpperCase()}:${entityType.trim().toUpperCase()}:${entityId.toString().trim()}:${yearWeek.trim()}:USER:${recipientId}`;
}

export function canAccessDedupConfig(user?: { roleCode?: string | null; permissions?: string[] | null } | null): boolean {
  if (!user) return false;
  const canonical = normalizeRoleCode(user.roleCode);
  const hasPermission = Array.isArray(user.permissions) && user.permissions.includes("NOTIFICATION_DEDUPLICATION_MANAGE");
  return canonical === "VT-06" || hasPermission;
}

export function validateDedupConfig(windowDays: number, intervalMinutes: number) {
  const errors: string[] = [];
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
}

export function formatChangeSummary(isEnabled: boolean, windowDays: number, scanInterval: number) {
  return {
    statusText: isEnabled ? "Bật" : "Tắt",
    windowText: `${windowDays} ngày`,
    scanText: `${scanInterval} phút`,
    summary: `Trạng thái: ${isEnabled ? "Bật" : "Tắt"}, Cửa sổ: ${windowDays} ngày, Chu kỳ: ${scanInterval} phút`,
  };
}
