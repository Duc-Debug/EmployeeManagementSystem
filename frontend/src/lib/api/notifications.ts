"use client";

import { apiRequest } from "../api-client";

export interface NotificationItem {
  id: number;
  recipientId: number;
  senderId: number | null;
  senderName: string;
  senderEmail: string;
  type: string;
  targetType: string;
  targetId: number;
  title: string;
  content: string;
  read: boolean;
  createdAt: string;
}

/**
 * Lấy danh sách thông báo của người dùng hiện tại
 */
export async function getMyNotifications(): Promise<NotificationItem[]> {
  return await apiRequest<NotificationItem[]>("/notifications");
}

/**
 * Đánh dấu một thông báo là đã đọc
 */
export async function markNotificationRead(id: number): Promise<void> {
  await apiRequest<void>(`/notifications/${id}/read`, {
    method: "PATCH",
  });
}

/**
 * Đánh dấu tất cả thông báo là đã đọc
 */
export async function markAllNotificationsRead(): Promise<void> {
  await apiRequest<void>("/notifications/read-all", {
    method: "PATCH",
  });
}

