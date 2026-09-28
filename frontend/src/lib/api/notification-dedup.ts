"use client";

import { apiRequest } from "../api-client";

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
