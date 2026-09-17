"use client";

import { apiRequest } from "../api-client";

export interface WeeklyCapacityDashboardItem {
    year: number;
    weekNumber: number;
    startDate: string;
    endDate: string;
    label: string;
    availableHours: number;
    allocatedHours: number;
    remainingHours: number;
    utilizationRate: number;
    overloadedEmployeesCount: number;
}

export interface DepartmentCapacityItem {
    orgUnitId?: number | null;
    orgUnitName: string;
    employeeCount: number;
    allocatedHours: number;
    availableHours: number;
    freeHours: number;
    utilizationRate: number;
    overloadedEmployeesCount: number;
}

export interface OverloadedEmployeeItem {
    employeeId: number;
    employeeCode: string;
    fullName: string;
    orgUnitId?: number | null;
    orgUnitName: string;
    professionalRole: string;
    totalAllocatedHours: number;
    totalAvailableHours: number;
    averageUtilizationRate: number;
    overloadedWeeksCount: number;
}

export interface UnresolvedConflictItem {
    conflictId: number;
    employeeId: number;
    employeeCode: string;
    employeeName: string;
    conflictType: string;
    yearNumber: number;
    weekNumber: number;
    conflictingProjectsCount: number;
    totalAllocatedHours: number;
    status: string;
    details?: string;
}

export interface ActiveProjectSummaryItem {
    projectId: number;
    projectCode: string;
    projectName: string;
    orgUnitId?: number | null;
    orgUnitName: string;
    pmName: string;
    startDate?: string | null;
    endDate?: string | null;
    estimatedHours?: number | null;
    memberCount: number;
    status: string;
}

export interface CapacityDashboardResult {
    orgUnitId?: number | null;
    orgUnitName: string;
    fromYear: number;
    fromWeek: number;
    durationWeeks: number;
    averageCapacityUtilization: number;
    overloadedEmployeesCount: number;
    departmentFreeHours: number;
    unresolvedScheduleConflictsCount: number;
    activeProjectsCount: number;
    totalAvailableHours: number;
    totalAllocatedHours: number;
    weeklyMetrics: WeeklyCapacityDashboardItem[];
    departmentBreakdown: DepartmentCapacityItem[];
    overloadedEmployees: OverloadedEmployeeItem[];
    unresolvedConflicts: UnresolvedConflictItem[];
    activeProjects: ActiveProjectSummaryItem[];
    generatedAt: string;
}

export interface CapacityDashboardQueryParams {
    orgUnitId?: number;
    fromYear?: number;
    fromWeek?: number;
    durationWeeks?: number;
}

export async function getCapacityDashboard(
    params?: CapacityDashboardQueryParams
): Promise<CapacityDashboardResult> {
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

    const query = searchParams.toString();
    const url = "/capacity-dashboard" + (query ? "?" + query : "");
    return apiRequest<CapacityDashboardResult>(url);
}
