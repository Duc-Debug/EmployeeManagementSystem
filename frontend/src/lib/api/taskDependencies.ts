import { apiRequest } from '../api-client';

export interface TaskDependencyResult {
  id: number;
  projectId: number;
  predecessorId: number;
  predecessorTaskCode: string;
  predecessorTaskName: string;
  successorId: number;
  successorTaskCode: string;
  successorTaskName: string;
  dependencyType: string;
  lagDays: number;
  createdBy?: number;
  createdAt: string;
}

export interface TaskDependencyGraphResult {
  projectId: number;
  projectCode: string;
  projectName: string;
  dependencies: TaskDependencyResult[];
}

export interface CreateTaskDependencyPayload {
  predecessorId: number;
  successorId: number;
  dependencyType?: string;
  lagDays?: number;
}

export async function getTaskDependencies(projectId: number): Promise<TaskDependencyGraphResult> {
  return await apiRequest<TaskDependencyGraphResult>(`/projects/${projectId}/tasks/dependencies`);
}

export async function createTaskDependency(
  projectId: number,
  payload: CreateTaskDependencyPayload
): Promise<TaskDependencyResult> {
  return await apiRequest<TaskDependencyResult>(`/projects/${projectId}/tasks/dependencies`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function deleteTaskDependency(projectId: number, dependencyId: number): Promise<void> {
  await apiRequest<void>(`/projects/${projectId}/tasks/dependencies/${dependencyId}`, {
    method: 'DELETE',
  });
}
