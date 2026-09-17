import { apiRequest } from "../api-client";

export interface TimesheetVarianceItem {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  orgUnitId: number | null;
  orgUnitName: string;
  projectId: number;
  projectName: string;
  year: number;
  weekNumber: number;
  weekStartDate: string;
  weekEndDate: string;
  allocatedHours: number;
  actualApprovedHours: number;
  varianceHours: number;
  variancePercentage: number;
  hasActualData: boolean;
  varianceStatus: "POSITIVE_VARIANCE" | "NEGATIVE_VARIANCE" | "ON_TRACK" | "NO_ACTUAL_DATA";
}

export interface TimesheetVarianceSummary {
  totalAllocatedHours: number;
  totalActualApprovedHours: number;
  totalVarianceHours: number;
  totalEmployees: number;
  totalWeeks: number;
  positiveVarianceCount: number;
  negativeVarianceCount: number;
  onTrackCount: number;
  noActualDataCount: number;
}

export interface TimesheetVarianceResult {
  orgUnitId: number | null;
  orgUnitName: string;
  fromYear: number;
  fromWeek: number;
  toYear: number;
  toWeek: number;
  items: TimesheetVarianceItem[];
  summary: TimesheetVarianceSummary;
  hasAnyActualData: boolean;
  message: string;
  generatedAt: string;
}

export interface TimesheetVarianceQuery {
  orgUnitId?: number;
  employeeId?: number;
  projectId?: number;
  fromYear?: number;
  fromWeek?: number;
  toYear?: number;
  toWeek?: number;
}

export async function getTimesheetVarianceReport(
  query: TimesheetVarianceQuery = {}
): Promise<TimesheetVarianceResult> {
  const params = new URLSearchParams();
  if (query.orgUnitId) params.append("orgUnitId", query.orgUnitId.toString());
  if (query.employeeId) params.append("employeeId", query.employeeId.toString());
  if (query.projectId) params.append("projectId", query.projectId.toString());
  if (query.fromYear) params.append("fromYear", query.fromYear.toString());
  if (query.fromWeek) params.append("fromWeek", query.fromWeek.toString());
  if (query.toYear) params.append("toYear", query.toYear.toString());
  if (query.toWeek) params.append("toWeek", query.toWeek.toString());

  const queryString = params.toString();
  const url = `/reports/timesheet-variance${queryString ? `?${queryString}` : ""}`;

  const response = await apiRequest<{
    success: boolean;
    message: string;
    data: TimesheetVarianceResult;
  }>(url, {
    method: "GET",
  });

  return response.data;
}
