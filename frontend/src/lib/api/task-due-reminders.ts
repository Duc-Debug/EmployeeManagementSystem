"use client";

import { apiRequest } from "../api-client";

export interface UpcomingDueTaskResult {
  taskId: number;
  projectId: number;
  taskCode: string;
  taskName: string;
  dueDate: string;       // YYYY-MM-DD
  daysRemaining: number; // 0: Hôm nay, 1, 2, 3 ngày
  status: string;        // 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW'
  directUrl: string;     // '/projects/{projectId}/tasks/{taskId}'
}

export interface TaskDueReminderScanResult {
  scanDate: string;
  totalScanned: number;
  sentCount: number;
  skippedDuplicateCount: number;
  skippedCompletedCount: number;
  notifiedTaskIds: number[];
}

export type DueUrgencyLevel = "TODAY" | "CRITICAL" | "WARNING" | "UPCOMING";

export interface UrgencyFormatting {
  text: string;
  badgeClass: string;
  level: DueUrgencyLevel;
  iconType: "ALERT" | "CLOCK" | "CALENDAR";
}

/**
 * Định dạng nhãn và phong cách hiển thị trực quan theo số ngày còn lại (QTN-19 / TC-01).
 * - 0 ngày (hoặc quá hạn): Đỏ báo động (Hôm nay / Cần xử lý ngay)
 * - 1 ngày: Đỏ cảnh báo khẩn (Còn 1 ngày)
 * - 2 ngày: Vàng/Cam (Còn 2 ngày)
 * - 3 ngày: Xanh/Tím (Còn 3 ngày)
 */
export function formatDaysRemaining(days: number): UrgencyFormatting {
  if (days <= 0) {
    return {
      text: "Hôm nay",
      badgeClass: "bg-rose-50 text-rose-700 border-rose-200",
      level: "TODAY",
      iconType: "ALERT",
    };
  }
  if (days === 1) {
    return {
      text: "Còn 1 ngày",
      badgeClass: "bg-rose-50 text-rose-700 border-rose-200",
      level: "CRITICAL",
      iconType: "ALERT",
    };
  }
  if (days === 2) {
    return {
      text: "Còn 2 ngày",
      badgeClass: "bg-amber-50 text-amber-700 border-amber-200",
      level: "WARNING",
      iconType: "CLOCK",
    };
  }
  return {
    text: `Còn ${days} ngày`,
    badgeClass: "bg-sky-50 text-sky-700 border-sky-200",
    level: "UPCOMING",
    iconType: "CALENDAR",
  };
}

/**
 * Lấy danh sách công việc sắp đến hạn trong 3 ngày tới dành cho Nhân viên chuyên môn (VT-04) (TC-03).
 * Endpoint: GET /api/v1/tasks/due-reminders/my-tasks
 */
export async function getMyUpcomingDueTasks(): Promise<UpcomingDueTaskResult[]> {
  const result = await apiRequest<UpcomingDueTaskResult[]>("/tasks/due-reminders/my-tasks");
  return Array.isArray(result) ? result : [];
}

/**
 * Kích hoạt rà soát và gửi thông báo nhắc việc sắp đến hạn theo QTN-19 (Hỗ trợ kiểm thử và chạy thủ công).
 * Endpoint: POST /api/v1/tasks/due-reminders/scan?scanDate=YYYY-MM-DD
 */
export async function triggerScanDueReminders(scanDate?: string): Promise<TaskDueReminderScanResult> {
  const query = scanDate ? `?scanDate=${encodeURIComponent(scanDate)}` : "";
  return await apiRequest<TaskDueReminderScanResult>(`/tasks/due-reminders/scan${query}`, {
    method: "POST",
  });
}

/**
 * Định dạng chuỗi ngày YYYY-MM-DD sang định dạng tiếng Việt DD/MM/YYYY trực quan.
 */
export function formatDueDateVietnamese(dateStr?: string | null): string {
  if (!dateStr || typeof dateStr !== "string") return "Chưa cập nhật";
  const parts = dateStr.trim().split("-");
  if (parts.length === 3 && parts[0].length === 4) {
    const [year, month, day] = parts;
    return `${day}/${month}/${year}`;
  }
  return dateStr;
}