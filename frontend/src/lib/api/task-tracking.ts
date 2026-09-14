"use client";

import { apiRequest } from "../api-client";

export type TaskBudgetBurnStatus = "NOT_SET" | "SAFE" | "WARNING" | "OVER_BUDGET";

export interface TaskTrackingAssignee {
    employeeId: number;
    employeeCode: string;
    fullName: string;
    email?: string;
    avatarUrl?: string;
}

export interface TaskTrackingItem {
    taskId: number;
    taskCode: string;
    name: string;
    categoryId: number | null;
    categoryName: string;
    assignees: TaskTrackingAssignee[];
    status: string;
    plannedStartDate: string | null;
    plannedEndDate: string | null;
    budgetHours: number;
    actualHours: number;
    burnedPercentage: number;
    isOverBudget: boolean;
    budgetBurnStatus: TaskBudgetBurnStatus;
    isOverdue: boolean;
    overdueDays: number;
    sortOrder: number;
}

export interface ProjectTaskTrackingResult {
    projectId: number;
    projectCode: string;
    projectName: string;
    projectStatus: string;
    totalTasks: number;
    overdueTasks: number;
    completedTasks: number;
    inProgressTasks: number;
    totalBudgetHours: number;
    totalActualHours: number;
    suggestionMessage: string | null;
    tasks: TaskTrackingItem[];
}

export interface TaskTrackingFilterParams {
    employeeId?: number;
    status?: string;
    overdueOnly?: boolean;
    keyword?: string;
}

/**
 * Lấy dữ liệu Bảng theo dõi công việc của dự án (NCL-04-CN-003)
 */
export async function getProjectTaskTracking(
    projectId: number | string,
    params?: TaskTrackingFilterParams,
    signal?: AbortSignal
): Promise<ProjectTaskTrackingResult> {
    const query = new URLSearchParams();
    if (params?.employeeId) {
        query.append("employeeId", String(params.employeeId));
    }
    if (params?.status && params.status !== "ALL") {
        query.append("status", params.status);
    }
    if (params?.overdueOnly) {
        query.append("overdueOnly", "true");
    }
    if (params?.keyword && params.keyword.trim()) {
        query.append("keyword", params.keyword.trim());
    }

    const queryString = query.toString();
    const endpoint = `/projects/${projectId}/task-tracking${queryString ? `?${queryString}` : ""}`;
    return await apiRequest<ProjectTaskTrackingResult>(endpoint, {
        method: "GET",
        signal,
    });
}
