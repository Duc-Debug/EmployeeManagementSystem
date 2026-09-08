"use client";

import { apiRequest } from "../api-client";
import type { OrgUnitTreeNode, OrgUnitType } from "@/types/hrm";

export interface CreateOrgUnitPayload {
  description?: string | null;
  managerId?: number | null;
  parentId?: number | null;
  unitCode: string;
  unitName: string;
  unitType: OrgUnitType;
}

export interface UpdateOrgUnitPayload {
  description?: string | null;
  managerId?: number | null;
  unitName: string;
  unitType: OrgUnitType;
}

export async function getOrgTree(): Promise<OrgUnitTreeNode[]> {
  const tree = await apiRequest<OrgUnitTreeNode[]>("/org-units/tree", {
    method: "GET",
  });
  return tree || [];
}

export async function createOrgUnit(payload: CreateOrgUnitPayload): Promise<OrgUnitTreeNode> {
  return await apiRequest<OrgUnitTreeNode>("/org-units", {
    body: JSON.stringify(payload),
    method: "POST",
  });
}

export async function updateOrgUnit(id: number, payload: UpdateOrgUnitPayload): Promise<OrgUnitTreeNode> {
  return await apiRequest<OrgUnitTreeNode>(`/org-units/${id}`, {
    body: JSON.stringify(payload),
    method: "PUT",
  });
}

export async function moveOrgUnit(id: number, newParentId: number): Promise<OrgUnitTreeNode> {
  return await apiRequest<OrgUnitTreeNode>(`/org-units/${id}/move`, {
    body: JSON.stringify({ newParentId }),
    method: "PATCH",
  });
}

export async function deactivateOrgUnit(id: number): Promise<OrgUnitTreeNode> {
  return await apiRequest<OrgUnitTreeNode>(`/org-units/${id}/deactivate`, {
    method: "PATCH",
  });
}

export async function activateOrgUnit(id: number): Promise<OrgUnitTreeNode> {
  return await apiRequest<OrgUnitTreeNode>(`/org-units/${id}/activate`, {
    method: "PATCH",
  });
}
