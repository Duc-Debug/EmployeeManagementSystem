"use client";

import { apiRequest } from "../api-client";

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