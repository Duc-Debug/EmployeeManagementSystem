"use client";

import { apiRequest } from "../api-client";

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
  const res = await apiRequest<any>(`/reports/recruitment-demand${qs}`);
  if (res && typeof res === "object" && "data" in res && res.data) {
    return res.data as RecruitmentDemandReportData;
  }
  return res as RecruitmentDemandReportData;
}
