"use client";

import { apiRequest } from "../api-client";

export type ConflictType = "MULTI_PROJECT_ALLOCATION" | "LEAVE_ALLOCATION_CONFLICT";
export type ScheduleConflictStatus = "OPEN" | "NOTIFIED" | "RESOLVED";

export interface ScheduleConflict {
    id: number;
    employeeId: number;
    employeeCode: string;
    employeeName: string;
    departmentName: string;
    yearNumber: number;
    weekNumber: number;
    weekLabel: string;
    conflictType: ConflictType;
    conflictTypeLabel: string;
    projectIds?: string;
    projectNames?: string;
    leaveRequestId?: number;
    leaveInfo?: string;
    totalAllocatedHours: number;
    netAvailableHours: number;
    excessHours: number;
    status: ScheduleConflictStatus;
    statusLabel: string;
    details?: string;
    notifiedAt?: string;
    notifiedBy?: number;
    createdAt?: string;
    updatedAt?: string;
}

export interface ScheduleConflictQuery {
    yearNumber?: number;
    startWeek?: number;
    endWeek?: number;
    employeeId?: number;
    projectId?: number;
    conflictType?: ConflictType;
    status?: ScheduleConflictStatus;
}

export interface ApiResponse<T> {
    success: boolean;
    errorCode?: string;
    message: string;
    data: T;
    timestamp: string;
}

export async function getScheduleConflicts(query?: ScheduleConflictQuery): Promise<ScheduleConflict[]> {
    const params = new URLSearchParams();
    if (query?.yearNumber) params.append("yearNumber", String(query.yearNumber));
    if (query?.startWeek) params.append("startWeek", String(query.startWeek));
    if (query?.endWeek) params.append("endWeek", String(query.endWeek));
    if (query?.employeeId) params.append("employeeId", String(query.employeeId));
    if (query?.projectId) params.append("projectId", String(query.projectId));
    if (query?.conflictType) params.append("conflictType", query.conflictType);
    if (query?.status) params.append("status", query.status);

    const queryString = params.toString();
    const url = `/schedule-conflicts${queryString ? `?${queryString}` : ""}`;
    const res = await apiRequest<ApiResponse<ScheduleConflict[]>>(url);
    return res.data || [];
}

export async function scanScheduleConflicts(yearNumber?: number, startWeek?: number, endWeek?: number): Promise<ScheduleConflict[]> {
    const params = new URLSearchParams();
    if (yearNumber) params.append("yearNumber", String(yearNumber));
    if (startWeek) params.append("startWeek", String(startWeek));
    if (endWeek) params.append("endWeek", String(endWeek));

    const queryString = params.toString();
    const url = `/schedule-conflicts/scan${queryString ? `?${queryString}` : ""}`;
    const res = await apiRequest<ApiResponse<ScheduleConflict[]>>(url, { method: "POST" });
    return res.data || [];
}

export async function notifyScheduleConflict(id: number): Promise<ScheduleConflict> {
    const res = await apiRequest<ApiResponse<ScheduleConflict>>(`/schedule-conflicts/${id}/notify`, { method: "POST" });
    return res.data;
}

export async function resolveScheduleConflict(id: number): Promise<ScheduleConflict> {
    const res = await apiRequest<ApiResponse<ScheduleConflict>>(`/schedule-conflicts/${id}/resolve`, { method: "POST" });
    return res.data;
}
