"use client";

import { apiRequest } from "../api-client";

export interface ProjectRoleResponse {
  id: number;
  code: string;
  name: string;
  description?: string;
  skillGroupId?: number;
  skillGroupName?: string;
  status: "ACTIVE" | "INACTIVE";
}

export interface CreateProjectRolePayload {
  code: string;
  name: string;
  skillGroupId: number;
  description?: string;
}

export interface UpdateProjectRolePayload {
  name: string;
  skillGroupId: number;
  description?: string;
}

export interface ProjectRoleUsageResponse {
  roleId: number;
  demandCount: number;
  employeeCount: number;
  inUse: boolean;
}

export async function getProjectRoles(includeInactive = false): Promise<ProjectRoleResponse[]> {
  const roles = await apiRequest<ProjectRoleResponse[]>(
    `/project-roles?includeInactive=${includeInactive}`
  );
  return Array.isArray(roles) ? roles : [];
}

export async function createProjectRole(
  payload: CreateProjectRolePayload
): Promise<ProjectRoleResponse> {
  return await apiRequest<ProjectRoleResponse>("/project-roles", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateProjectRole(
  id: number,
  payload: UpdateProjectRolePayload
): Promise<ProjectRoleResponse> {
  return await apiRequest<ProjectRoleResponse>(`/project-roles/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function deactivateProjectRole(
  id: number
): Promise<ProjectRoleResponse> {
  return await apiRequest<ProjectRoleResponse>(`/project-roles/${id}/deactivate`, {
    method: "PATCH",
  });
}

export async function activateProjectRole(
  id: number
): Promise<ProjectRoleResponse> {
  return await apiRequest<ProjectRoleResponse>(`/project-roles/${id}/activate`, {
    method: "PATCH",
  });
}

export async function checkProjectRoleUsage(
  id: number
): Promise<ProjectRoleUsageResponse> {
  return await apiRequest<ProjectRoleUsageResponse>(`/project-roles/${id}/usage`, {
    method: "GET",
  });
}
