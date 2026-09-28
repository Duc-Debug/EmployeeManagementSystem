"use client";

import { apiRequest } from "../api-client";

export interface RoleAllocationTemplateItem {
  id: number;
  roleId: number;
  roleCode: string;
  roleName: string;
  hoursPerWeek: number;
}

export interface RoleAllocationTemplateSummary {
  id: number;
  templateCode: string;
  name: string;
  description: string | null;
  sourceProjectId: number | null;
  itemsCount: number;
  createdBy: number | null;
  createdAt: string;
}

export interface RoleAllocationTemplateDetail {
  id: number;
  templateCode: string;
  name: string;
  description: string | null;
  sourceProjectId: number | null;
  createdBy: number | null;
  createdAt: string;
  updatedAt: string | null;
  version: number;
  items: RoleAllocationTemplateItem[];
}

export interface ProjectRoleStructureItem {
  roleId: number;
  roleCode: string;
  roleName: string;
  hoursPerWeek: number;
}

export interface CreateRoleAllocationTemplatePayload {
  templateCode: string;
  name: string;
  description?: string;
  sourceProjectId?: number | null;
  items: {
    roleId: number;
    hoursPerWeek: number;
  }[];
}

export interface RoleSuggestionItem {
  roleId: number;
  roleCode: string;
  roleName: string;
  hoursPerWeek: number;
  suggestedEmployeeId: number | null;
  suggestedEmployeeName: string | null;
  suggestedEmployeeCode: string | null;
  assigned: boolean;
  warningMessage: string | null;
}

export interface PreviewRoleAllocationResult {
  templateId: number;
  templateCode: string;
  templateName: string;
  targetProjectId: number;
  targetProjectName: string;
  targetTotalWeeks: number;
  suggestions: RoleSuggestionItem[];
  hasUnassignedRoles: boolean;
  warnings: string[];
}

export interface ApplyRoleAllocationTemplatePayload {
  assignments: {
    roleId: number;
    employeeId: number | null;
    hoursPerWeek: number;
  }[];
}

export interface ApplyRoleAllocationTemplateResult {
  templateId: number;
  targetProjectId: number;
  appliedRolesCount: number;
  allocatedEmployeesCount: number;
  unassignedRolesCount: number;
  warnings: string[];
  message: string;
}

export async function getRoleAllocationTemplates(): Promise<RoleAllocationTemplateSummary[]> {
  return apiRequest<RoleAllocationTemplateSummary[]>("/role-allocation-templates");
}

export async function getRoleAllocationTemplate(id: number): Promise<RoleAllocationTemplateDetail> {
  return apiRequest<RoleAllocationTemplateDetail>(`/role-allocation-templates/${id}`);
}

export async function extractRoleStructureFromProject(projectId: number): Promise<ProjectRoleStructureItem[]> {
  return apiRequest<ProjectRoleStructureItem[]>(`/role-allocation-templates/extract-from-project/${projectId}`);
}

export async function createRoleAllocationTemplate(
  payload: CreateRoleAllocationTemplatePayload
): Promise<RoleAllocationTemplateDetail> {
  return apiRequest<RoleAllocationTemplateDetail>("/role-allocation-templates", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function previewRoleAllocationSuggestion(
  templateId: number,
  targetProjectId: number
): Promise<PreviewRoleAllocationResult> {
  return apiRequest<PreviewRoleAllocationResult>(
    `/role-allocation-templates/${templateId}/preview-apply/${targetProjectId}`,
    {
      method: "POST",
    }
  );
}

export async function confirmApplyRoleAllocationTemplate(
  templateId: number,
  targetProjectId: number,
  payload: ApplyRoleAllocationTemplatePayload
): Promise<ApplyRoleAllocationTemplateResult> {
  return apiRequest<ApplyRoleAllocationTemplateResult>(
    `/role-allocation-templates/${templateId}/confirm-apply/${targetProjectId}`,
    {
      method: "POST",
      body: JSON.stringify(payload),
    }
  );
}

