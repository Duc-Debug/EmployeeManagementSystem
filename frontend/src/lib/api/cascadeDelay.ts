import { apiRequest } from '../api-client';

export interface AffectedTaskResult {
  taskId: number;
  taskCode?: string;
  taskName: string;
  originalDueDate?: string;
  newCalculatedEndDate?: string;
  delayDays: number;
  slackDays: number;
  protectedBySlack: boolean;
  statusDescription: string;
}

export interface AffectedMilestoneResult {
  milestoneId: number;
  milestoneName: string;
  plannedDate: string;
  newCalculatedDate: string;
  delayDays: number;
  protectedBySlack: boolean;
  statusDescription: string;
}

export interface CascadeDelayWarningResult {
  rootTaskId: number;
  rootTaskCode?: string;
  rootTaskName: string;
  originalDueDate?: string;
  newActualEndDate: string;
  slipDays: number;
  chainOnTimeDueToSlack: boolean;
  affectedTasks: AffectedTaskResult[];
  affectedMilestones: AffectedMilestoneResult[];
  summaryMessage: string;
}

export async function evaluateCascadeDelay(
  projectId: number,
  taskId: number,
  newActualEndDate: string
): Promise<CascadeDelayWarningResult> {
  return await apiRequest<CascadeDelayWarningResult>(
    `/projects/${projectId}/tasks/${taskId}/cascade-delay-warning/evaluate`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ newActualEndDate }),
    }
  );
}

export async function updateTaskActualEndDate(
  projectId: number,
  taskId: number,
  newActualEndDate: string
): Promise<CascadeDelayWarningResult> {
  return await apiRequest<CascadeDelayWarningResult>(
    `/projects/${projectId}/tasks/${taskId}/actual-end-date`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ newActualEndDate }),
    }
  );
}
