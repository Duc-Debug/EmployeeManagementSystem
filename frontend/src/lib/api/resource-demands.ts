"use client";

import { apiRequest } from "../api-client";

export interface WeeklyDemandItem {
  year: number;
  weekNumber: number;
  startDate: string;
  endDate: string;
  requiredHours: number;
}

export interface RoleResourceDemand {
  roleId: number;
  roleCode: string;
  roleName: string;
  totalRoleHours: number;
  weeklyDemands: WeeklyDemandItem[];
}

export interface ProjectResourceDemandSummaryResult {
  projectId: number;
  projectCode: string;
  projectName: string;
  projectEstimatedHours: number;
  totalDemandHours: number;
  exceedsEstimatedHours: boolean;
  warningMessage: string | null;
  demandsByRole: RoleResourceDemand[];
}

export interface EstimateResourceDemandPayload {
  roleId: number;
  hoursPerWeek: number;
}

export async function getProjectResourceDemands(
  projectId: number
): Promise<ProjectResourceDemandSummaryResult> {
  return apiRequest<ProjectResourceDemandSummaryResult>(
    `/projects/${projectId}/resource-demands`
  );
}

export async function estimateResourceDemand(
  projectId: number,
  payload: EstimateResourceDemandPayload
): Promise<ProjectResourceDemandSummaryResult> {
  return apiRequest<ProjectResourceDemandSummaryResult>(
    `/projects/${projectId}/resource-demands`,
    {
      method: "POST",
      body: JSON.stringify(payload),
    }
  );
}

export async function deleteResourceDemand(
  projectId: number,
  roleId: number
): Promise<ProjectResourceDemandSummaryResult> {
  return apiRequest<ProjectResourceDemandSummaryResult>(
    `/projects/${projectId}/resource-demands/${roleId}`,
    {
      method: "DELETE",
    }
  );
}
