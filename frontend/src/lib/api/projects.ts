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
  status: 'ACTIVE' | 'INACTIVE' | 'CLOSED';
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
  burnedPercentage?: number;
  burnStatus?: 'NOT_SET' | 'SAFE' | 'WARNING' | 'OVER_BUDGET';
  isOverBudget?: boolean;
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

/**
 * Đóng dự án
 */
export async function closeProject(
  id: number | string,
  closureReason?: string
): Promise<ProjectResult> {
  return await apiRequest<ProjectResult>(`/projects/${id}/close`, {
    method: 'POST',
    body: JSON.stringify(closureReason ? { closureReason } : {}),
  });
}

/**
 * Mở lại dự án đã đóng
 */
export async function reopenProject(
  id: number | string,
  reopenReason: string
): Promise<ProjectResult> {
  return await apiRequest<ProjectResult>(`/projects/${id}/reopen`, {
    method: 'POST',
    body: JSON.stringify({ reopenReason }),
  });
}

export interface ProjectMemberResult {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  email?: string;
  orgUnitId?: number;
  orgUnitName?: string;
  roleInProject: 'PROJECT_MANAGER' | 'MEMBER';
  status: string;
}

/**
 * Lấy danh sách thành viên dự án
 */
export async function getProjectMembers(
  projectId: number | string
): Promise<ProjectMemberResult[]> {
  return await apiRequest<ProjectMemberResult[]>(`/projects/${projectId}/members`);
}

/**
 * Thêm thành viên vào dự án
 */
export async function addProjectMember(
  projectId: number | string,
  employeeId: number
): Promise<ProjectMemberResult> {
  return await apiRequest<ProjectMemberResult>(`/projects/${projectId}/members`, {
    method: 'POST',
    body: JSON.stringify({ employeeId }),
  });
}

/**
 * Xóa thành viên khỏi dự án
 */
export async function removeProjectMember(
  projectId: number | string,
  employeeId: number
): Promise<void> {
  await apiRequest<void>(`/projects/${projectId}/members/${employeeId}`, {
    method: 'DELETE',
  });
}

export interface MilestoneResult {
  id: number;
  projectId: number;
  name: string;
  targetDate: string;
  completionDate?: string;
  description?: string;
  status: 'PENDING' | 'REACHED' | 'MISSED';
}

export interface CreateMilestonePayload {
  name: string;
  targetDate: string;
  completionDate?: string;
  description?: string;
  status?: 'PENDING' | 'REACHED' | 'MISSED';
}

export interface UpdateMilestonePayload {
  name?: string;
  targetDate?: string;
  completionDate?: string;
  description?: string;
  status?: 'PENDING' | 'REACHED' | 'MISSED';
}

/**
 * Lấy danh sách mốc tiến độ dự án
 */
export async function getProjectMilestones(
  projectId: number | string
): Promise<MilestoneResult[]> {
  return await apiRequest<MilestoneResult[]>(`/projects/${projectId}/milestones`);
}

/**
 * Khai báo mốc tiến độ mới cho dự án
 */
export async function createMilestone(
  projectId: number | string,
  payload: CreateMilestonePayload
): Promise<MilestoneResult> {
  return await apiRequest<MilestoneResult>(`/projects/${projectId}/milestones`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/**
 * Cập nhật mốc tiến độ
 */
export async function updateMilestone(
  projectId: number | string,
  milestoneId: number | string,
  payload: UpdateMilestonePayload
): Promise<MilestoneResult> {
  return await apiRequest<MilestoneResult>(`/projects/${projectId}/milestones/${milestoneId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

/**
 * Xóa mốc tiến độ
 */
export async function deleteMilestone(
  projectId: number | string,
  milestoneId: number | string
): Promise<void> {
  await apiRequest<void>(`/projects/${projectId}/milestones/${milestoneId}`, {
    method: 'DELETE',
  });
}

export interface ResourceDemandRoleSummary {
  roleId: number;
  roleCode?: string;
  roleName?: string;
  hoursPerWeek: number;
}

export interface ProjectResourceDemandSummaryResult {
  projectId: number;
  totalDemandHoursPerWeek: number;
  totalEstimatedHours: number;
  exceedsEstimatedHours: boolean;
  roleDemands: ResourceDemandRoleSummary[];
}

export interface EstimateResourceDemandPayload {
  roleId: number;
  hoursPerWeek: number;
}

/**
 * Lấy bảng ước lượng nhu cầu nhân sự của dự án
 */
export async function getProjectResourceDemands(
  projectId: number | string
): Promise<ProjectResourceDemandSummaryResult> {
  return await apiRequest<ProjectResourceDemandSummaryResult>(`/projects/${projectId}/resource-demands`);
}

/**
 * Ước lượng nhu cầu nhân sự cho một vai trò
 */
export async function estimateResourceDemand(
  projectId: number | string,
  payload: EstimateResourceDemandPayload
): Promise<ProjectResourceDemandSummaryResult> {
  return await apiRequest<ProjectResourceDemandSummaryResult>(`/projects/${projectId}/resource-demands`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

