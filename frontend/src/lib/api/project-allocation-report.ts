"use client";

import { apiRequest } from "../api-client";

export interface AllocatedMemberDetailItem {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  professionalRole: string;
  allocatedHours: number;
  allocationPercentage: number | null;
}

export interface WeeklyRoleAllocationItem {
  year: number;
  weekNumber: number;
  startDate: string;
  endDate: string;
  weekLabel: string;
  demandHours: number;
  allocatedHours: number;
  shortfallHours: number;
  surplusHours: number;
  allocatedMembers: AllocatedMemberDetailItem[];
}

export interface RoleAllocationBreakdownItem {
  roleId: number;
  roleCode: string;
  roleName: string;
  totalDemandHours: number;
  totalAllocatedHours: number;
  totalShortfallHours: number;
  totalSurplusHours: number;
  fulfillmentRate: number;
  weeklyRoleMetrics: WeeklyRoleAllocationItem[];
}

export interface WeeklyProjectSummaryItem {
  year: number;
  weekNumber: number;
  startDate: string;
  endDate: string;
  weekLabel: string;
  demandHours: number;
  allocatedHours: number;
  shortfallHours: number;
  surplusHours: number;
  fulfillmentRate: number;
  status: "SHORTAGE" | "SUFFICIENT" | "SURPLUS";
}

export interface ShortageAlertItem {
  year: number;
  weekNumber: number;
  weekLabel: string;
  roleId: number;
  roleName: string;
  demandHours: number;
  allocatedHours: number;
  missingHours: number;
  severity: "HIGH" | "MEDIUM";
  message: string;
}

export interface ProjectAllocationReportData {
  projectId: number;
  projectCode: string;
  projectName: string;
  status: string;
  orgUnitId: number | null;
  orgUnitName: string;
  managerId: number | null;
  managerName: string;
  startDate: string | null;
  endDate: string | null;
  fromYear: number;
  fromWeek: number;
  toYear: number;
  toWeek: number;
  totalEstimatedHours: number;
  totalDemandHours: number;
  totalAllocatedHours: number;
  totalShortfallHours: number;
  totalSurplusHours: number;
  fulfillmentRate: number;
  shortageWeeksCount: number;
  weeklySummaries: WeeklyProjectSummaryItem[];
  roleBreakdowns: RoleAllocationBreakdownItem[];
  shortageAlerts: ShortageAlertItem[];
  generatedAt: string;
}

export interface ProjectAllocationReportQuery {
  projectId: number;
  fromYear?: number;
  fromWeek?: number;
  toYear?: number;
  toWeek?: number;
}

/**
 * Lấy dữ liệu báo cáo phân bổ theo dự án (NCL-10-CN-006).
 */
export async function getProjectAllocationReport(
  query: ProjectAllocationReportQuery,
  signal?: AbortSignal
): Promise<ProjectAllocationReportData> {
  const params = new URLSearchParams();
  if (query.fromYear != null) params.append("fromYear", String(query.fromYear));
  if (query.fromWeek != null) params.append("fromWeek", String(query.fromWeek));
  if (query.toYear != null) params.append("toYear", String(query.toYear));
  if (query.toWeek != null) params.append("toWeek", String(query.toWeek));

  const queryStr = params.toString();
  const url = `/reports/project-allocation/${query.projectId}${queryStr ? `?${queryStr}` : ""}`;

  return await apiRequest<ProjectAllocationReportData>(url, { signal });
}

/**
 * Tải file CSV báo cáo phân bổ theo dự án.
 */
export async function downloadProjectAllocationReport(
  query: ProjectAllocationReportQuery
): Promise<void> {
  const params = new URLSearchParams();
  if (query.fromYear != null) params.append("fromYear", String(query.fromYear));
  if (query.fromWeek != null) params.append("fromWeek", String(query.fromWeek));
  if (query.toYear != null) params.append("toYear", String(query.toYear));
  if (query.toWeek != null) params.append("toWeek", String(query.toWeek));

  const queryStr = params.toString();
  const url = `/reports/project-allocation/${query.projectId}/export${queryStr ? `?${queryStr}` : ""}`;

  const token = typeof window !== "undefined" ? localStorage.getItem("accessToken") : null;
  const baseUrl = "/api/v1";

  const response = await fetch(`${baseUrl}${url}`, {
    method: "GET",
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  if (!response.ok) {
    throw new Error("Xuất báo cáo thất bại");
  }

  const blob = await response.blob();
  const downloadUrl = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = downloadUrl;
  const disposition = response.headers.get("content-disposition");
  let filename = `Bao_cao_phan_bo_du_an_${query.projectId}.csv`;
  if (disposition && disposition.includes("filename=")) {
    const match = disposition.match(/filename="?([^"]+)"?/);
    if (match && match[1]) {
      filename = match[1];
    }
  }
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(downloadUrl);
}
