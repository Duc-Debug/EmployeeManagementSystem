"use client";

import { apiRequest } from "../api-client";

export type MilestoneStatus = "ON_TRACK" | "DELAYED" | "COMPLETED";

export interface MilestoneResult {
  id: number;
  projectId: number;
  name: string;
  description?: string;
  plannedDate: string; // ISO yyyy-MM-dd
  actualDate?: string; // ISO yyyy-MM-dd
  status: MilestoneStatus;
  delayDays: number;
  totalLinkedTasks: number;
  completedLinkedTasks: number;
  linkedTaskIds: number[];
  createdBy?: number;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface CreateMilestonePayload {
  name: string;
  description?: string;
  plannedDate: string;
  linkedTaskIds: number[];
}

export interface UpdateMilestonePayload {
  name?: string;
  description?: string;
  plannedDate?: string;
  actualDate?: string;
  linkedTaskIds?: number[];
}

/**
 * Lấy danh sách mốc tiến độ của dự án (AC-02 / TC-02)
 */
export async function getProjectMilestones(projectId: number | string): Promise<MilestoneResult[]> {
  try {
    const data = await apiRequest<MilestoneResult[]>(`/projects/${projectId}/milestones`);
    return Array.isArray(data) ? data : [];
  } catch (error) {
    console.warn(`[Milestone API] Không thể tải mốc tiến độ cho dự án ${projectId}:`, error);
    throw error;
  }
}

/**
 * Khai báo mốc tiến độ mới (AC-01 / TC-01)
 */
export async function createMilestone(
  projectId: number | string,
  payload: CreateMilestonePayload
): Promise<MilestoneResult> {
  return await apiRequest<MilestoneResult>(`/projects/${projectId}/milestones`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/**
 * Cập nhật thông tin mốc tiến độ (AC-03)
 */
export async function updateMilestone(
  projectId: number | string,
  milestoneId: number | string,
  payload: UpdateMilestonePayload
): Promise<MilestoneResult> {
  return await apiRequest<MilestoneResult>(`/projects/${projectId}/milestones/${milestoneId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

/**
 * Đánh dấu hoàn thành mốc tiến độ (Cập nhật ngày hoàn thành actualDate)
 */
export async function completeMilestone(
  projectId: number | string,
  milestoneId: number | string,
  actualDate?: string
): Promise<MilestoneResult> {
  const completionDate = actualDate || new Date().toISOString().split("T")[0];
  return await apiRequest<MilestoneResult>(`/projects/${projectId}/milestones/${milestoneId}`, {
    method: "PATCH",
    body: JSON.stringify({
      actualDate: completionDate,
    }),
  });
}

/**
 * Xóa mốc tiến độ
 */
export async function deleteMilestone(
  projectId: number | string,
  milestoneId: number | string
): Promise<void> {
  await apiRequest<void>(`/projects/${projectId}/milestones/${milestoneId}`, {
    method: "DELETE",
  });
}
