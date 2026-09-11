"use client";

import { apiRequest } from "../api-client";

export interface WeeklyCapacityResult {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  year: number;
  weekNumber: number;
  standardHours: number;
  netAvailableHours: number;
  totalAllocatedHours: number;
  remainingAvailableHours: number;
  isOverAllocated: boolean;
  warningMessage?: string;
}

export async function getWeeklyCapacities(
  employeeIds: readonly number[],
  year: number,
  weekNumber: number,
): Promise<WeeklyCapacityResult[]> {
  const params = new URLSearchParams({ year: String(year), weekNumber: String(weekNumber) });
  employeeIds.forEach((id) => params.append("employeeIds", String(id)));
  return apiRequest<WeeklyCapacityResult[]>(`/allocations/capacity?${params.toString()}`);
}

export interface ProjectWeeklyAllocationResult {
  employeeId: number;
  projectId: number;
  year: number;
  weekNumber: number;
  allocatedHours: number;
}

export async function getProjectWeeklyAllocations(
  projectId: number,
  year: number,
  startWeek: number,
  endWeek: number,
): Promise<ProjectWeeklyAllocationResult[]> {
  const params = new URLSearchParams({
    year: String(year),
    startWeek: String(startWeek),
    endWeek: String(endWeek),
  });
  return apiRequest<ProjectWeeklyAllocationResult[]>(`/projects/${projectId}/allocations?${params.toString()}`);
}

export async function allocateProjectHours(payload: {
  employeeId: number;
  projectId: number;
  year: number;
  weekNumber: number;
  allocatedHours: number;
}): Promise<WeeklyCapacityResult> {
  return apiRequest<WeeklyCapacityResult>('/allocations', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export type CapacityStatus = "OVERLOADED" | "OPTIMAL" | "UNDERUTILIZED";

export interface HeaderWeekInfo {
  year: number;
  weekNumber: number;
  startDate: string;
  endDate: string;
  label: string;
}

export interface CapacityMatrixCell {
  year: number;
  weekNumber: number;
  allocatedHours: number;
  availableHours: number;
  remainingHours: number;
  utilizationPercentage: number | null;
  isOverloaded: boolean;
  excessHours: number;
  status: CapacityStatus;
}

export interface EmployeeCapacityRow {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  orgUnitId?: number | null;
  orgUnitName?: string | null;
  professionalRole?: string | null;
  cells: CapacityMatrixCell[];
  totalAllocatedHours: number;
  totalAvailableHours: number;
  averageUtilization: number;
  overloadedWeeksCount: number;
}

export interface CapacityMatrixSummary {
  pageEmployeesCount?: number;
  totalEmployees?: number;
  totalWeeks: number;
  overloadedEmployeesCount: number;
  overloadedCellsCount: number;
  underutilizedCellsCount: number;
  averageUtilization: number;
}

export interface CompanyWeeklyCapacityMatrixData {
  orgUnitId?: number | null;
  orgUnitName?: string | null;
  fromYear: number;
  fromWeek: number;
  durationWeeks: number;
  weeks: HeaderWeekInfo[];
  rows: EmployeeCapacityRow[];
  summary: CapacityMatrixSummary;
  page: number;
  pageSize: number;
  totalEmployees: number;
  totalPages: number;
}

export interface CompanyWeeklyCapacityMatrixParams {
  orgUnitId?: number;
  fromYear?: number;
  fromWeek?: number;
  durationWeeks?: number;
  page?: number;
  size?: number;
  search?: string;
  status?: CapacityStatus;
}

/**
 * NCL-06-CN-002: Lấy dữ liệu bảng năng lực theo tuần của công ty / bộ phận
 */
export async function getCompanyWeeklyCapacityMatrix(
  params?: CompanyWeeklyCapacityMatrixParams
): Promise<CompanyWeeklyCapacityMatrixData> {
  const searchParams = new URLSearchParams();
  if (params?.orgUnitId != null) {
    searchParams.append("orgUnitId", String(params.orgUnitId));
  }
  if (params?.fromYear != null) {
    searchParams.append("fromYear", String(params.fromYear));
  }
  if (params?.fromWeek != null) {
    searchParams.append("fromWeek", String(params.fromWeek));
  }
  if (params?.durationWeeks != null) {
    searchParams.append("durationWeeks", String(params.durationWeeks));
  }
  if (params?.page != null) {
    searchParams.append("page", String(params.page));
  }
  if (params?.size != null) {
    searchParams.append("size", String(params.size));
  }
  if (params?.search != null && params.search.trim() !== "") {
    searchParams.append("search", params.search.trim());
  }
  if (params?.status != null) {
    searchParams.append("status", params.status);
  }
  const queryStr = searchParams.toString();
  return apiRequest<CompanyWeeklyCapacityMatrixData>(
    `/allocations/weekly-matrix${queryStr ? `?${queryStr}` : ""}`
  );
}
