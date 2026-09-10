"use client";

import { apiRequest } from "../api-client";
import { type TaskNodeResult } from "./projects";

export interface TaskBudgetResult {
  taskId: number;
  taskCode: string;
  name: string;
  budgetHours: number;
  actualHours: number;
  burnedPercentage: number;
  burnStatus: 'NOT_SET' | 'SAFE' | 'WARNING' | 'OVER_BUDGET';
  remainingHours: number;
  overBudgetHours?: number;
  isOverBudget: boolean;
}

export interface SetTaskBudgetPayload {
  budgetHours: number;
}

export interface CloneProjectWbsResult {
  targetProjectId: number;
  sourceProjectId: number;
  totalClonedTasks: number;
  totalCategories: number;
  wbsTree: TaskNodeResult[];
}

/**
 * Đặt ngân sách giờ công cho công việc trong dự án
 */
export async function setTaskBudget(
  projectId: number | string,
  taskId: number | string,
  budgetHours: number
): Promise<TaskBudgetResult> {
  return await apiRequest<TaskBudgetResult>(`/projects/${projectId}/tasks/${taskId}/budget`, {
    method: "PATCH",
    body: JSON.stringify({ budgetHours }),
  });
}

/**
 * Nhân bản cây công việc WBS từ một dự án cũ sang dự án đích
 */
export async function cloneProjectWbs(
  targetProjectId: number | string,
  sourceProjectId: number | string
): Promise<CloneProjectWbsResult> {
  return await apiRequest<CloneProjectWbsResult>(`/projects/${targetProjectId}/wbs/clone`, {
    method: "POST",
    body: JSON.stringify({ sourceProjectId: Number(sourceProjectId) }),
  });
}