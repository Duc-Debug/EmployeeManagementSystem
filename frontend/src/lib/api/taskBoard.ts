"use client";

import { apiRequest } from "../api-client";

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE' | 'CANCELLED';

export interface TaskBoardAssignee {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  isPrimary: boolean;
}

export interface TaskBoardCard {
  taskId: number;
  taskCode: string;
  name: string;
  description?: string;
  projectId: number;
  projectCode: string;
  projectName: string;
  status: TaskStatus;
  estimatedHours?: number;
  actualHours?: number;
  plannedStartDate?: string;
  plannedEndDate?: string;
  sortOrder?: number;
  assignees: TaskBoardAssignee[];
  canMove: boolean;
}

export interface TaskBoardData {
  todoTasks: TaskBoardCard[];
  inProgressTasks: TaskBoardCard[];
  inReviewTasks: TaskBoardCard[];
  doneTasks: TaskBoardCard[];
  cancelledTasks: TaskBoardCard[];
  totalTasks: number;
  projectIdFilter?: number;
  employeeIdFilter?: number;
}

export interface MoveTaskBoardStatusPayload {
  newStatus: TaskStatus;
}

/**
 * Lấy danh sách công việc trên bảng Kanban theo các cột trạng thái
 */
export async function getTaskBoard(params?: {
  projectId?: number | string;
  employeeId?: number | string;
}): Promise<TaskBoardData> {
  const searchParams = new URLSearchParams();
  if (params?.projectId) {
    searchParams.append("projectId", String(params.projectId));
  }
  if (params?.employeeId) {
    searchParams.append("employeeId", String(params.employeeId));
  }

  const queryString = searchParams.toString();
  const endpoint = queryString ? `/tasks/board?${queryString}` : "/tasks/board";
  return await apiRequest<TaskBoardData>(endpoint);
}

/**
 * Cập nhật trạng thái công việc khi kéo thả trên bảng Kanban
 */
export async function moveTaskBoardStatus(
  taskId: number | string,
  newStatus: TaskStatus
): Promise<TaskBoardCard> {
  return await apiRequest<TaskBoardCard>(`/tasks/${taskId}/board-status`, {
    method: "PATCH",
    body: JSON.stringify({ newStatus }),
  });
}
