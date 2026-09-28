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

export function calculateWeeklyForecast(availableHours: number, committedHours: number, reservedHours: number) {
  const committedRemainingHours = availableHours - committedHours;
  const projectedRemainingHours = availableHours - committedHours - reservedHours;

  let committedUtilization: number | null = null;
  if (availableHours > 0) {
    committedUtilization = Number(((committedHours / availableHours) * 100).toFixed(1));
  } else if (committedHours === 0) {
    committedUtilization = 0.0;
  }

  let projectedUtilization: number | null = null;
  if (availableHours > 0) {
    projectedUtilization = Number((((committedHours + reservedHours) / availableHours) * 100).toFixed(1));
  } else if (committedHours + reservedHours === 0) {
    projectedUtilization = 0.0;
  }

  let status: ForecastStatus = "AVAILABLE";
  if (
    projectedRemainingHours < 0 ||
    (availableHours === 0 && committedHours + reservedHours > 0) ||
    (projectedUtilization !== null && projectedUtilization > 100)
  ) {
    status = "OVER_CAPACITY";
  } else if (projectedUtilization !== null && projectedUtilization >= 80) {
    status = "NEAR_FULL";
  }

  return {
    committedRemainingHours,
    projectedRemainingHours,
    committedUtilization,
    projectedUtilization,
    status,
  };
}

export function canAccessCapacityForecastTab(permissions?: string[] | null, roleCode?: string | null) {
  const normalized = roleCode ? roleCode.toUpperCase().replace(/_/g, "-") : "";
  if (permissions && permissions.length > 0) {
    return permissions.includes("CAPACITY_FORECAST_REPORT_READ");
  }
  return ["VT-01", "VT-03", "ROLE-VT-01", "ROLE-VT-03", "DIRECTOR", "RESOURCE-MANAGER"].includes(normalized);
}

export interface OrgTreeNode {
  id: number;
  name?: string;
  children?: OrgTreeNode[];
}

export function visibleOrgUnitIdsForRole(tree: OrgTreeNode[], roleCode?: string | null, scopeOrgUnitId?: number | null): number[] {
  const flatten = (nodes: OrgTreeNode[]): OrgTreeNode[] => nodes.flatMap((node) => [node, ...flatten(node.children || [])]);
  if (roleCode !== "VT-03" || scopeOrgUnitId == null) return flatten(tree).map((node) => node.id);

  const findScopeRoot = (nodes: OrgTreeNode[]): OrgTreeNode | null => {
    for (const node of nodes) {
      if (node.id === scopeOrgUnitId) return node;
      const match = findScopeRoot(node.children || []);
      if (match) return match;
    }
    return null;
  };
  const root = findScopeRoot(tree);
  return root ? flatten([root]).map((node) => node.id) : [];
}

