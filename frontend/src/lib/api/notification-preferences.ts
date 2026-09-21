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
