"use client";

import { apiRequest } from "../api-client";

export interface ProjectRoleResponse {
  id: number;
  code: string;
  name: string;
  description?: string;
}

export async function getProjectRoles(): Promise<ProjectRoleResponse[]> {
  const roles = await apiRequest<ProjectRoleResponse[]>("/project-roles");
  return Array.isArray(roles) ? roles : [];
}
