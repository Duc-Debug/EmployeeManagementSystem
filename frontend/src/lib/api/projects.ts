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
  closureReason?: string;
  closedAt?: string;
  closedBy?: number;
  reopenReason?: string;
  reopenedAt?: string;
  reopenedBy?: number;
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
  assigneeIds?: number[];
  estimatedHours?: number;
  actualHours?: number;
  budgetHours?: number;
  burnedPercentage?: number;
  burnStatus?: 'NOT_SET' | 'SAFE' | 'WARNING' | 'OVER_BUDGET';
  isOverBudget?: boolean;
  status: BackendTaskStatus;
  plannedStartDate?: string;
  plannedEndDate?: string;
  sortOrder?: number;
  startDate?: string;
  dueDate?: string;
  actualEndDate?: string;
  slackDays?: number;
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
  plannedStartDate?: string;
  plannedEndDate?: string;
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

export interface ProjectTemplateSummary {
  id: number;
  templateCode: string;
  name: string;
  description?: string;
  active: boolean;
  totalEstimatedHours: number;
  categoriesCount: number;
  tasksCount: number;
}

export interface ProjectTemplateTask {
  id: number;
  parentId?: number;
  name: string;
  description?: string;
  taskType: BackendTaskType;
  estimatedHours: number;
  sortOrder: number;
}

export interface ProjectTemplateDetail {
  id: number;
  templateCode: string;
  name: string;
  description?: string;
  active: boolean;
  totalEstimatedHours: number;
  categoriesCount: number;
  tasksCount: number;
  tasks: ProjectTemplateTask[];
}

export interface CreateProjectFromTemplatePayload {
  templateId: number;
  projectName: string;
  orgUnitId: number;
  managerId?: number;
  startDate?: string;
  endDate?: string;
  description?: string;
}

/**
 * Lấy danh sách mẫu dự án đang hoạt động
 */
export async function getProjectTemplates(): Promise<ProjectTemplateSummary[]> {
  return await apiRequest<ProjectTemplateSummary[]>('/projects/templates');
}

/**
 * Lấy chi tiết mẫu dự án kèm cây công việc mẫu
 */
export async function getProjectTemplateDetail(templateId: number | string): Promise<ProjectTemplateDetail> {
  return await apiRequest<ProjectTemplateDetail>(`/projects/templates/${templateId}`);
}

/**
 * Tạo mới dự án từ mẫu có sẵn
 */
export async function createProjectFromTemplate(payload: CreateProjectFromTemplatePayload): Promise<ProjectResult> {
  return await apiRequest<ProjectResult>('/projects/from-template', {
    method: 'POST',
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
  contractEndDate?: string;
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

export interface AssignTaskPayload {
  employeeIds: number[];
  plannedStartDate?: string;
  plannedEndDate?: string;
}

export interface TaskAssignmentResult {
  taskId: number;
  taskCode: string;
  taskName: string;
  assigneeIds: number[];
  plannedStartDate?: string;
  plannedEndDate?: string;
}

export interface MyTaskResult {
  taskId: number;
  taskCode: string;
  taskName: string;
  projectId: number;
  projectName: string;
  status: BackendTaskStatus;
  plannedStartDate?: string;
  plannedEndDate?: string;
  isPrimary: boolean;
  assignedAt?: string;
}

/**
 * Lấy danh sách toàn bộ nhân sự hoạt động trong công ty có thể phân công vào dự án/công việc
 */
export async function getAssignableEmployees(startDate?: string): Promise<ProjectMemberResult[]> {
  const url = startDate
    ? `/projects/assignable-employees?startDate=${encodeURIComponent(startDate)}`
    : '/projects/assignable-employees';
  return await apiRequest<ProjectMemberResult[]>(url);
}

/**
 * Phân công một hoặc nhiều người thực hiện công việc kèm ngày bắt đầu và ngày kết thúc mong muốn
 */
export async function assignTask(
  projectId: number | string,
  taskId: number | string,
  payload: AssignTaskPayload
): Promise<TaskAssignmentResult> {
  return await apiRequest<TaskAssignmentResult>(`/projects/${projectId}/tasks/${taskId}/assignment`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

/**
 * Lấy danh sách công việc được giao cho nhân sự đang đăng nhập
 */
export async function getMyTasks(): Promise<MyTaskResult[]> {
  return await apiRequest<MyTaskResult[]>('/tasks/me');
}


