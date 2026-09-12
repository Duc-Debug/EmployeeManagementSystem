"use client";

import { API_BASE_URL, apiRequest, ApiError } from "../api-client";
import { getAuthToken } from "../auth-session";

export interface RecruitmentSkillDemandItem {
  skillId: number;
  skillCode: string;
  skillName: string;
  category: string;
  requiredDemandHours: number;
  availableCapacityHours: number;
  shortfallHours: number;
  status: "DEFICIT" | "SUFFICIENT";
}

export interface RecruitmentDemandReportData {
  totalSkillsEvaluated: number;
  totalDeficitHours: number;
  skillsWithDeficitCount: number;
  skills: RecruitmentSkillDemandItem[];
  timeRangeText: string;
  generatedAt?: string;
  unmappedDemandHours: number;
  unmappedRoleCount: number;
  unattributedCapacityHours: number;
}

export async function getRecruitmentDemandReport(params?: {
  fromYear?: number;
  fromWeek?: number;
  toYear?: number;
  toWeek?: number;
  orgUnitId?: number;
}): Promise<RecruitmentDemandReportData> {
  const query = new URLSearchParams();
  if (params?.fromYear) query.append("fromYear", String(params.fromYear));
  if (params?.fromWeek) query.append("fromWeek", String(params.fromWeek));
  if (params?.toYear) query.append("toYear", String(params.toYear));
  if (params?.toWeek) query.append("toWeek", String(params.toWeek));
  if (params?.orgUnitId) query.append("orgUnitId", String(params.orgUnitId));

  const qs = query.toString() ? `?${query.toString()}` : "";
  return apiRequest<RecruitmentDemandReportData>(`/reports/recruitment-demand${qs}`);
}

export async function downloadRecruitmentDemandReport(params: {
  fromYear: number; fromWeek: number; toYear: number; toWeek: number; orgUnitId?: number;
}): Promise<void> {
  const query = new URLSearchParams({ fromYear: String(params.fromYear), fromWeek: String(params.fromWeek), toYear: String(params.toYear), toWeek: String(params.toWeek) });
  if (params.orgUnitId !== undefined) query.set("orgUnitId", String(params.orgUnitId));
  const token = getAuthToken();
  const response = await fetch(`${API_BASE_URL}/reports/recruitment-demand/export?${query}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  if (!response.ok) throw new ApiError(`Xuất báo cáo thất bại (${response.status})`, response.status);
  const blob = await response.blob();
  const filename = /filename="?([^";]+)"?/i.exec(response.headers.get("Content-Disposition") || "")?.[1] || "recruitment-demand.csv";
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url; anchor.download = filename; document.body.appendChild(anchor); anchor.click(); anchor.remove(); URL.revokeObjectURL(url);
}
