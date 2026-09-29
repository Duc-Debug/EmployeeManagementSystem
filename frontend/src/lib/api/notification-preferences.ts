"use client";

import { apiRequest } from "../api-client";

export type NotificationDeliveryChannel = "ALL" | "IN_APP_ONLY" | "EMAIL_ONLY" | "NONE";
export type NotificationFrequency = "IMMEDIATE" | "DAILY_DIGEST" | "WEEKLY_DIGEST";

export interface NotificationPreference {
  id: number;
  userId: number;
  inAppEnabled: boolean;
  emailEnabled: boolean;
  taskAssignedChannel: NotificationDeliveryChannel;
  taskDueReminderChannel: NotificationDeliveryChannel;
  taskCommentChannel: NotificationDeliveryChannel;
  timesheetReminderChannel: NotificationDeliveryChannel;
  allocationChangedChannel: NotificationDeliveryChannel;
  scheduleConflictChannel: NotificationDeliveryChannel;
  frequency: NotificationFrequency;
  taskDueReminderDays: number;
  quietHoursEnabled: boolean;
  quietHoursStart: string | null;
  quietHoursEnd: string | null;
  version: number;
}

export interface UpdateNotificationPreferenceRequest {
  inAppEnabled?: boolean;
  emailEnabled?: boolean;
  taskAssignedChannel?: NotificationDeliveryChannel;
  taskDueReminderChannel?: NotificationDeliveryChannel;
  taskCommentChannel?: NotificationDeliveryChannel;
  timesheetReminderChannel?: NotificationDeliveryChannel;
  allocationChangedChannel?: NotificationDeliveryChannel;
  scheduleConflictChannel?: NotificationDeliveryChannel;
  frequency?: NotificationFrequency;
  taskDueReminderDays?: number;
  quietHoursEnabled?: boolean;
  quietHoursStart?: string | null;
  quietHoursEnd?: string | null;
}

/**
 * Lấy cấu hình nhận thông báo của người dùng hiện tại (NCL-11-CN-002)
 */
export async function getMyNotificationPreference(): Promise<NotificationPreference> {
  return await apiRequest<NotificationPreference>("/notification-preferences/me");
}

/**
 * Cập nhật cấu hình nhận thông báo của người dùng hiện tại (NCL-11-CN-002)
 */
export async function updateMyNotificationPreference(
  data: UpdateNotificationPreferenceRequest
): Promise<NotificationPreference> {
  return await apiRequest<NotificationPreference>("/notification-preferences/me", {
    method: "PATCH",
    body: JSON.stringify(data),
  });
}

/**
 * Khôi phục cấu hình nhận thông báo về mặc định của hệ thống (NCL-11-CN-002)
 */
export async function resetMyNotificationPreference(): Promise<NotificationPreference> {
  return await apiRequest<NotificationPreference>("/notification-preferences/me/reset", {
    method: "POST",
  });
}

export function validateCriticalChannels(scheduleConflictChannel: string, allocationChangedChannel: string) {
  if (scheduleConflictChannel === "NONE") {
    return { valid: false, error: "Cảnh báo xung đột lịch bắt buộc phải bật ít nhất 1 kênh" };
  }
  if (allocationChangedChannel === "NONE") {
    return { valid: false, error: "Cảnh báo thay đổi phân bổ bắt buộc phải bật ít nhất 1 kênh" };
  }
  return { valid: true };
}

export function isValidReminderDays(days: unknown): boolean {
  return typeof days === "number" && Number.isInteger(days) && days >= 1 && days <= 14;
}

export function isInQuietHours(enabled: boolean, startStr?: string | null, endStr?: string | null, targetStr?: string | null): boolean {
  if (!enabled || !startStr || !endStr || !targetStr) return false;
  const [sh, sm] = startStr.split(":").map(Number);
  const [eh, em] = endStr.split(":").map(Number);
  const [th, tm] = targetStr.split(":").map(Number);

  const start = sh * 60 + sm;
  const end = eh * 60 + em;
  const target = th * 60 + tm;

  if (start < end) {
    return target >= start && target < end;
  } else {
    // Qua đêm (ví dụ 22:00 -> 07:00)
    return target >= start || target < end;
  }
}

export function createDefaultNotificationPreferenceForm() {
  return {
    inAppEnabled: true,
    emailEnabled: true,
    taskAssignedChannel: "ALL" as NotificationDeliveryChannel,
    taskDueReminderChannel: "ALL" as NotificationDeliveryChannel,
    taskCommentChannel: "IN_APP_ONLY" as NotificationDeliveryChannel,
    timesheetReminderChannel: "ALL" as NotificationDeliveryChannel,
    allocationChangedChannel: "ALL" as NotificationDeliveryChannel,
    scheduleConflictChannel: "ALL" as NotificationDeliveryChannel,
    frequency: "IMMEDIATE" as NotificationFrequency,
    taskDueReminderDays: 3,
    quietHoursEnabled: false,
    quietHoursStart: "22:00",
    quietHoursEnd: "07:00",
  };
}

export function prepareNotificationPreferencePayload(form: Record<string, any>) {
  return {
    ...form,
    quietHoursStart: form.quietHoursEnabled && form.quietHoursStart
      ? (form.quietHoursStart.length === 5 ? `${form.quietHoursStart}:00` : form.quietHoursStart)
      : null,
    quietHoursEnd: form.quietHoursEnabled && form.quietHoursEnd
      ? (form.quietHoursEnd.length === 5 ? `${form.quietHoursEnd}:00` : form.quietHoursEnd)
      : null,
  };
}
