"use client";

import { apiRequest } from "../api-client";

export interface WorkLog {
  id: number;
  timesheetId: number;
  employeeId: number;
  employeeName: string;
  projectId: number;
  projectCode: string;
  projectName: string;
  taskId: number;
  taskCode: string;
  taskName: string;
  workDate: string;
  hours: number;
  isBillable: boolean;
  description: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface DailyWorkLogGroup {
  date: string;
  dayOfWeek: string;
  totalHours: number;
  isExceededLimit: boolean;
  entries: WorkLog[];
}

export interface WeeklyTimesheet {
  timesheetId: number | null;
  employeeId: number;
  employeeName: string;
  weekStartDate: string;
  weekEndDate: string;
  totalHours: number;
  status: string;
  isEditable: boolean;
  dailyGroups: DailyWorkLogGroup[];
  allEntries: WorkLog[];
}

export interface AssignedTaskOption {
  projectId: number;
  projectCode: string;
  projectName: string;
  projectStatus: string;
  taskId: number;
  taskCode: string;
  taskName: string;
  taskStatus: string;
}

export interface CreateWorkLogPayload {
  projectId: number;
  taskId: number;
  workDate: string;
  hours: number;
  isBillable?: boolean;
  description: string;
}

export interface UpdateWorkLogPayload {
  projectId: number;
  taskId: number;
  workDate: string;
  hours: number;
  isBillable?: boolean;
  description: string;
}

/**
 * 1. Lấy thông tin bảng chấm công tuần (mặc định tuần hiện tại nếu không truyền date)
 */
export async function getWeeklyTimesheet(date?: string): Promise<WeeklyTimesheet> {
  const query = date ? `?date=${encodeURIComponent(date)}` : "";
  return await apiRequest<WeeklyTimesheet>(`/work-logs/my-week${query}`, {
    method: "GET",
  });
}

/**
 * 2. Lấy danh sách công việc được phân công để ghi giờ
 */
export async function getMyAssignedTasks(): Promise<AssignedTaskOption[]> {
  return await apiRequest<AssignedTaskOption[]>("/work-logs/my-tasks", {
    method: "GET",
  });
}

/**
 * 3. Tạo dòng ghi giờ công mới
 */
export async function createWorkLog(payload: CreateWorkLogPayload): Promise<WorkLog> {
  return await apiRequest<WorkLog>("/work-logs", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/**
 * 4. Cập nhật dòng ghi giờ công
 */
export async function updateWorkLog(id: number, payload: UpdateWorkLogPayload): Promise<WorkLog> {
  return await apiRequest<WorkLog>(`/work-logs/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

/**
 * 5. Xóa dòng ghi giờ công
 */
export async function deleteWorkLog(id: number): Promise<void> {
  await apiRequest<void>(`/work-logs/${id}`, {
    method: "DELETE",
  });
}
