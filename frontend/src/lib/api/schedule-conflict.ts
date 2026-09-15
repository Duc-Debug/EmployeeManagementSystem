"use client";

import { apiRequest } from "../api-client";

export type ConflictType = "MULTI_PROJECT_ALLOCATION" | "LEAVE_ALLOCATION_CONFLICT";
export type ScheduleConflictStatus = "OPEN" | "NOTIFIED" | "RESOLVED" | "REOPENED";

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
    notifiedByName?: string;
    assignedHandlerId?: number;
    assignedHandlerCode?: string;
    assignedHandlerName?: string;
    resolutionNote?: string;
    isRecurrent?: boolean;
    recurrentNote?: string;
    resolvedAt?: string;
    resolvedBy?: number;
    resolvedByName?: string;
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
    return apiRequest<ScheduleConflict[]>(url);
}

export async function scanScheduleConflicts(yearNumber?: number, startWeek?: number, endWeek?: number): Promise<ScheduleConflict[]> {
    const params = new URLSearchParams();
    if (yearNumber) params.append("yearNumber", String(yearNumber));
    if (startWeek) params.append("startWeek", String(startWeek));
    if (endWeek) params.append("endWeek", String(endWeek));

    const queryString = params.toString();
    const url = `/schedule-conflicts/scan${queryString ? `?${queryString}` : ""}`;
    return apiRequest<ScheduleConflict[]>(url, { method: "POST" });
}

export async function notifyScheduleConflict(id: number): Promise<ScheduleConflict> {
    return apiRequest<ScheduleConflict>(`/schedule-conflicts/${id}/notify`, { method: "POST" });
}

export async function resolveScheduleConflict(id: number): Promise<ScheduleConflict> {
    return apiRequest<ScheduleConflict>(`/schedule-conflicts/${id}/resolve`, { method: "POST" });
}

export async function resolveScheduleConflictWithNote(
    id: number,
    payload: { assignedHandlerId?: number; resolutionNote: string }
): Promise<ScheduleConflict> {
    return apiRequest<ScheduleConflict>(`/schedule-conflicts/${id}/resolve-with-note`, {
        method: "POST",
        body: JSON.stringify(payload),
    });
}

export async function assignScheduleConflictHandler(
    id: number,
    assignedHandlerId?: number
): Promise<ScheduleConflict> {
    return apiRequest<ScheduleConflict>(`/schedule-conflicts/${id}/assign-handler`, {
        method: "POST",
        body: JSON.stringify({ assignedHandlerId }),
    });
}
export interface ReplacementCandidate {
    employeeId: number;
    employeeCode: string;
    fullName: string;
    orgUnitId?: number;
    departmentName: string;
    skillId: number;
    skillName: string;
    proficiencyLevel: number;
    proficiencyLevelName: string;
    yearsOfExperience?: number;
    freeHours: number;
    standardHoursPerWeek: number;
    totalAllocatedHours: number;
    contractEndDate?: string;
}

export interface ReplacementSuggestionResult {
    conflictId: number;
    conflictedEmployeeId: number;
    conflictedEmployeeCode: string;
    conflictedEmployeeName: string;
    departmentName: string;
    yearNumber: number;
    weekNumber: number;
    weekLabel: string;
    skillId?: number;
    skillName: string;
    requiredProficiencyLevel: number;
    excessHours: number;
    candidates: ReplacementCandidate[];
    hasAvailableReplacements: boolean;
    recommendationMessage?: string;
}

export interface ConfirmReplacementProposalPayload {
    conflictId: number;
    replacementEmployeeId: number;
    skillId?: number;
    proficiencyLevel?: number;
    notes?: string;
}

export interface ReplacementProposalResult {
    proposalId: number;
    conflictId: number;
    originalEmployeeId: number;
    originalEmployeeName: string;
    replacementEmployeeId: number;
    replacementEmployeeName: string;
    skillId?: number;
    skillName: string;
    proficiencyLevel: number;
    freeHours: number;
    status: string;
    notes?: string;
    createdBy: number;
    createdByName: string;
    createdAt: string;
}

export async function getReplacementSuggestions(
    conflictId: number,
    skillId?: number,
    minProficiencyLevel?: number
): Promise<ReplacementSuggestionResult> {
    const params = new URLSearchParams();
    if (skillId) params.append("skillId", String(skillId));
    if (minProficiencyLevel) params.append("minProficiencyLevel", String(minProficiencyLevel));

    const queryString = params.toString();
    const url = `/schedule-conflicts/${conflictId}/replacement-suggestions${queryString ? `?${queryString}` : ""}`;
    return apiRequest<ReplacementSuggestionResult>(url);
}

export async function confirmReplacementProposal(
    conflictId: number,
    payload: ConfirmReplacementProposalPayload
): Promise<ReplacementProposalResult> {
    return apiRequest<ReplacementProposalResult>(`/schedule-conflicts/${conflictId}/confirm-replacement`, {
        method: "POST",
        body: JSON.stringify(payload),
    });
}

