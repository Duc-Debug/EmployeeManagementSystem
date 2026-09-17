"use client";

import { apiRequest } from "../api-client";

export type NotificationLevel = "CAO" | "TRUNG_BINH" | "THAP";

export interface NotificationCenterItem {
  id: number; // notification_recipient.id
  eventId: number;
  eventType: string;
  level: NotificationLevel;
  title: string;
  message: string;
  relatedEntityType: string | null;
  relatedEntityId: string | null;
  isRead: boolean;
  readAt: string | null;
  createdAt: string;
}

// Backward compatibility interface
export type NotificationItem = NotificationCenterItem;

export interface NotificationCenterPage {
  items: NotificationCenterItem[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  unreadCount: number;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

export interface NotificationQueryParams {
  status?: "ALL" | "UNREAD" | "READ" | string;
  level?: "ALL" | "CAO" | "TRUNG_BINH" | "THAP" | string;
  page?: number;
  size?: number;
}

/**
 * Lấy danh sách thông báo phân trang của người dùng hiện tại (NCL-11-CN-001)
 */
export async function getNotificationCenter(params?: NotificationQueryParams): Promise<NotificationCenterPage> {
  const query = new URLSearchParams();
  if (params?.status) query.append("status", params.status);
  if (params?.level) query.append("level", params.level);
  if (params?.page !== undefined) query.append("page", params.page.toString());
  if (params?.size !== undefined) query.append("size", params.size.toString());

  const queryString = query.toString();
  const url = queryString ? `/notifications?${queryString}` : "/notifications";

  return await apiRequest<NotificationCenterPage>(url);
}

/**
 * Lấy danh sách thông báo tương thích ngược (trả về mảng items)
 */
export async function getMyNotifications(params?: NotificationQueryParams): Promise<NotificationCenterItem[]> {
  const pageResult = await getNotificationCenter(params);
  return pageResult?.items || [];
}

/**
 * Lấy số lượng thông báo chưa đọc phục vụ badge
 */
export async function getUnreadNotificationCount(): Promise<number> {
  const res = await apiRequest<UnreadCountResponse>("/notifications/unread-count");
  return res?.unreadCount ?? 0;
}

/**
 * Lấy chi tiết thông báo (đọc thuần túy, không side effect)
 */
export async function getNotificationDetail(id: number): Promise<NotificationCenterItem> {
  return await apiRequest<NotificationCenterItem>(`/notifications/${id}`);
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

/**
 * Xóa mềm một thông báo khỏi trung tâm thông báo của người dùng hiện tại
 */
export async function deleteNotification(id: number): Promise<void> {
  await apiRequest<void>(`/notifications/${id}`, {
    method: "DELETE",
  });
}
