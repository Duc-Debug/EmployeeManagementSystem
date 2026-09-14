"use client";

import { apiRequest } from "../api-client";

export interface WorkLogResult {
  id: number;
  timesheetId: number;
  employeeId: number;
  employeeName?: string;
  projectId: number;
  projectCode?: string;
  projectName: string;
  taskId: number;
  taskCode?: string;
  taskName: string;
  workDate: string;
  hours: number;
  isBillable: boolean;
  description: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DailyWorkLogGroupDto {
  date: string;
  dayOfWeek: string;
  totalHours: number;
  isExceededLimit: boolean;
  entries: WorkLogResult[];
}

export interface WeeklyTimesheetResult {
  timesheetId: number | null;
  employeeId: number;
  employeeName: string;
  weekStartDate: string;
  weekEndDate: string;
  totalHours: number;
  status: "DRAFT" | "SUBMITTED" | "APPROVED" | "REJECTED" | string;
  isEditable: boolean;
  dailyGroups: DailyWorkLogGroupDto[];
  allEntries: WorkLogResult[];
}

export interface AssignedTaskOptionResult {
  projectId: number;
  projectName: string;
  projectCode: string;
  taskId: number;
  taskName: string;
  taskStatus: string;
}

export interface CreateWorkLogRequest {
  projectId: number;
  taskId: number;
  workDate: string;
  hours: number;
  isBillable: boolean;
  description: string;
}

export interface UpdateWorkLogRequest {
  projectId: number;
  taskId: number;
  workDate: string;
  hours: number;
  isBillable: boolean;
  description: string;
}

/**
 * NCL-09-CN-001: Lấy danh sách công việc được phân công cho nhân viên đăng nhập
 */
export async function getMyAssignedTasks(): Promise<AssignedTaskOptionResult[]> {
  return apiRequest<AssignedTaskOptionResult[]>("/work-logs/my-tasks");
}

/**
 * NCL-09-CN-001: Lấy bảng chấm công theo tuần của nhân viên đăng nhập
 */
export async function getMyWeeklyTimesheet(date?: string): Promise<WeeklyTimesheetResult> {
  const query = date ? `?date=${date}` : "";
  return apiRequest<WeeklyTimesheetResult>(`/work-logs/my-week${query}`);
}

/**
 * NCL-09-CN-001: Ghi giờ công mới (TC-01 -> TC-05)
 */
export async function createWorkLog(payload: CreateWorkLogRequest): Promise<WorkLogResult> {
  return apiRequest<WorkLogResult>("/work-logs", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/**
 * NCL-09-CN-001: Chỉnh sửa dòng ghi giờ công
 */
export async function updateWorkLog(id: number, payload: UpdateWorkLogRequest): Promise<WorkLogResult> {
  return apiRequest<WorkLogResult>(`/work-logs/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

/**
 * NCL-09-CN-001: Xóa dòng ghi giờ công
 */
export async function deleteWorkLog(id: number): Promise<void> {
  await apiRequest<void>(`/work-logs/${id}`, {
    method: "DELETE",
  });
}

/**
 * NCL-09-CN-002: Nộp bảng chấm công theo tuần
 */
export interface SubmitWeeklyTimesheetRequest {
  dateInWeek?: string;
  timesheetId?: number;
}

export async function submitWeeklyTimesheet(payload?: SubmitWeeklyTimesheetRequest): Promise<WeeklyTimesheetResult> {
  return apiRequest<WeeklyTimesheetResult>("/work-logs/my-week/submit", {
    method: "POST",
    body: JSON.stringify(payload || {}),
  });
}

