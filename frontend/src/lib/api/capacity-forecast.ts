"use client";

import { apiRequest } from "../api-client";

export type ForecastStatus = "AVAILABLE" | "NEAR_FULL" | "OVER_CAPACITY";

export interface WeeklyForecastItem {
  year: number;
  weekNumber: number;
  startDate: string;
  endDate: string;
  availableHours: number;
  committedHours: number;
  reservedHours: number;
  committedRemainingHours: number;
  projectedRemainingHours: number;
  committedUtilization: number | null;
  projectedUtilization: number | null;
  status: ForecastStatus;
}

export interface CapacityForecastSummary {
  totalAvailableHours: number;
  totalCommittedHours: number;
  totalReservedHours: number;
  totalProjectedRemainingHours: number;
  overCapacityWeeks: number;
}

export interface CapacityForecastReportData {
  orgUnitId: number | null;
  orgUnitName: string;
  fromYear: number;
  fromWeek: number;
  durationWeeks: number;
  weeks: WeeklyForecastItem[];
  summary: CapacityForecastSummary;
  generatedAt: string;
}

export interface CapacityForecastQueryParams {
  orgUnitId?: number | string;
  fromYear?: number;
  fromWeek?: number;
  durationWeeks?: number;
}

export async function getCapacityForecastReport(params?: CapacityForecastQueryParams, signal?: AbortSignal): Promise<CapacityForecastReportData> {
  const query = new URLSearchParams();
  if (params?.orgUnitId) query.append("orgUnitId", String(params.orgUnitId));
  if (params?.fromYear) query.append("fromYear", String(params.fromYear));
  if (params?.fromWeek) query.append("fromWeek", String(params.fromWeek));
  if (params?.durationWeeks) query.append("durationWeeks", String(params.durationWeeks));

  const qs = query.toString() ? `?${query.toString()}` : "";
  return apiRequest<CapacityForecastReportData>(`/reports/capacity-forecast${qs}`, { signal });
}

