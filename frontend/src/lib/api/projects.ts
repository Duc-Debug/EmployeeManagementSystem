"use client";

import { apiRequest } from "../api-client";

export interface PageResult<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ProjectResult {
  id: number;
  projectCode: string;
  projectName: string;
  orgUnitId: number;
  managerId?: number;
  status: 'PLANNING' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED' | 'CLOSED';
  startDate?: string;
  endDate?: string;
  estimatedHours?: number;
  description?: string;
  createdBy?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateProjectPayload {
  projectName: string;
  orgUnitId: number;
  managerId?: number;
  startDate?: string;
  endDate?: string;
  estimatedHours?: number;
  description?: string;
}

export interface UpdateProjectPayload {
  projectName?: string;
  managerId?: number;
  startDate?: string;
  endDate?: string;
  estimatedHours?: number;
  description?: string;
}

export type BackendTaskType = 'CATEGORY' | 'TASK';
export type BackendTaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';

export interface TaskNodeResult {
  id: number;
  projectId: number;
  parentId?: number;
  taskCode: string;
  name: string;
  description?: string;
  taskType: BackendTaskType;
  assigneeId?: number;
  estimatedHours?: number;
  actualHours?: number;
  budgetHours?: number;
  status: BackendTaskStatus;
  sortOrder?: number;
  createdBy?: number;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
  children: TaskNodeResult[];
}

export interface TaskResult {
  id: number;
  projectId: number;
  parentId?: number;
  taskCode: string;
  name: string;
  description?: string;
  taskType: BackendTaskType;
  assigneeId?: number;
  estimatedHours?: number;
  actualHours?: number;
  budgetHours?: number;
  status: BackendTaskStatus;
  sortOrder?: number;
  createdBy?: number;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface CreateTaskPayload {
  parentId?: number | null;
  name: string;
  description?: string;
  taskType?: BackendTaskType;
  assigneeId?: number | null;
  estimatedHours?: number;
  sortOrder?: number;
}

export interface UpdateTaskPayload {
  parentId?: number | null;
  name?: string;
  description?: string;
  assigneeId?: number | null;
  estimatedHours?: number;
  sortOrder?: number;
  status?: BackendTaskStatus;
}

/**
 * Lấy danh sách dự án có phân trang
 */
export async function getProjects(page = 0, size = 50): Promise<PageResult<ProjectResult>> {
  return await apiRequest<PageResult<ProjectResult>>(`/projects?page=${page}&size=${size}`);
}

/**
 * Lấy chi tiết dự án theo ID
 */
export async function getProjectById(id: number | string): Promise<ProjectResult> {
  return await apiRequest<ProjectResult>(`/projects/${id}`);
}

/**
 * Tạo mới dự án
 */
export async function createProject(payload: CreateProjectPayload): Promise<ProjectResult> {
  return await apiRequest<ProjectResult>('/projects', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/**
 * Cập nhật thông tin dự án
 */
export async function updateProject(
  id: number | string,
  payload: UpdateProjectPayload
): Promise<ProjectResult> {
  return await apiRequest<ProjectResult>(`/projects/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

/**
 * Lấy cây công việc WBS của dự án
 */
export async function getProjectWbs(projectId: number | string): Promise<TaskNodeResult[]> {
  return await apiRequest<TaskNodeResult[]>(`/projects/${projectId}/wbs`);
}

/**
 * Tạo công việc / hạng mục trong dự án
 */
export async function createTask(
  projectId: number | string,
  payload: CreateTaskPayload
): Promise<TaskResult> {
  return await apiRequest<TaskResult>(`/projects/${projectId}/tasks`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/**
 * Cập nhật công việc / hạng mục trong dự án
 */
export async function updateTask(
  projectId: number | string,
  taskId: number | string,
  payload: UpdateTaskPayload
): Promise<TaskResult> {
  return await apiRequest<TaskResult>(`/projects/${projectId}/tasks/${taskId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

