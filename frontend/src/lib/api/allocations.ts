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
  allocationPercentage?: number;
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
  allocatedHours?: number;
  allocationPercentage?: number;
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
  reservedHours?: number;
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

// NCL-06-CN-005: Quản lý giữ chỗ nguồn lực (Resource Reservation - QTN-13)
export type ReservationStatus = "ACTIVE" | "CONVERTED" | "CANCELLED";

export interface ResourceReservationResult {
  id: number;
  projectId: number;
  projectCode: string;
  projectName: string;
  employeeId: number;
  employeeCode: string;
  employeeFullName: string;
  year: number;
  weekNumber: number;
  reservedHours: number;
  note?: string | null;
  status: ReservationStatus;
  createdBy: number;
  createdAt: string;
  updatedAt?: string | null;
}

export interface CreateReservationPayload {
  projectId: number;
  employeeId: number;
  year: number;
  weekNumber: number;
  reservedHours: number;
  note?: string;
}

export interface CancelReservationPayload {
  reason: string;
}

export async function createResourceReservation(
  payload: CreateReservationPayload
): Promise<ResourceReservationResult> {
  return apiRequest<ResourceReservationResult>("/resource-reservations", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function cancelResourceReservation(
  id: number,
  payload: CancelReservationPayload
): Promise<ResourceReservationResult> {
  return apiRequest<ResourceReservationResult>(`/resource-reservations/${id}/cancel`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export async function getResourceReservations(params?: {
  projectId?: number;
  employeeId?: number;
  year?: number;
  weekNumber?: number;
  status?: ReservationStatus;
}): Promise<ResourceReservationResult[]> {
  const searchParams = new URLSearchParams();
  if (params?.projectId != null) searchParams.append("projectId", String(params.projectId));
  if (params?.employeeId != null) searchParams.append("employeeId", String(params.employeeId));
  if (params?.year != null) searchParams.append("year", String(params.year));
  if (params?.weekNumber != null) searchParams.append("weekNumber", String(params.weekNumber));
  if (params?.status != null) searchParams.append("status", params.status);
  const queryStr = searchParams.toString();
  return apiRequest<ResourceReservationResult[]>(
    `/resource-reservations${queryStr ? `?${queryStr}` : ""}`
  );
}

export async function autoCancelProjectReservations(
  projectId: number,
  payload: { reason: string }
): Promise<{ cancelledCount: number }> {
  return apiRequest<{ cancelledCount: number }>(
    `/resource-reservations/projects/${projectId}/auto-cancel`,
    {
      method: "POST",
      body: JSON.stringify(payload),
    }
  );
}

export async function autoConvertProjectReservations(
  projectId: number
): Promise<{ convertedCount: number }> {
  return apiRequest<{ convertedCount: number }>(
    `/resource-reservations/projects/${projectId}/auto-convert`,
    {
      method: "POST",
    }
  );
}

