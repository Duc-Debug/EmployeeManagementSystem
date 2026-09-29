import { apiRequest } from "../../../lib/api-client";
import type { MyAllocationsResponse, ConfirmScheduleResponse, ProvideFeedbackResponse } from "../types";

export function buildMyAllocationsQuery(weekStart?: string, weeks?: number): string {
  const params = new URLSearchParams();
  if (weekStart) {
    params.append("week_start", weekStart);
  }
  if (weeks !== undefined && weeks !== null) {
    params.append("weeks", String(weeks));
  }
  const queryString = params.toString();
  return `/my-allocations${queryString ? `?${queryString}` : ""}`;
}

export function formatWeeklyDateRange(mondayStr: string): string {
  const monday = new Date(mondayStr);
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);

  const dFormat = (d: Date) =>
    `${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}/${d.getFullYear()}`;

  return `${dFormat(monday)} — ${dFormat(sunday)}`;
}

export function getProjectStatusBadge(projectStatus?: string | null): string | null {
  if (projectStatus?.toUpperCase() === "CLOSED") {
    return "Dự án đã đóng";
  }
  return null;
}

export function validateFeedbackReason(reason?: string | null): { valid: boolean; error: string | null } {
  if (!reason || !reason.trim()) {
    return { valid: false, error: "Vui lòng nhập lý do hoặc ý kiến phản hồi." };
  }
  return { valid: true, error: null };
}

export function getFeedbackActionLabel(status: string, feedbackNote?: string | null): string | null {
  const allowedStatuses = ["NOT_CONFIRMED", "CONFIRMED", "STALE", "HAS_FEEDBACK"];
  if (!allowedStatuses.includes(status)) {
    return null;
  }
  return feedbackNote ? "Chỉnh sửa phản hồi" : "Phản hồi";
}

export const myScheduleApi = {
  getMyAllocations: async (weekStart?: string, weeks?: number): Promise<MyAllocationsResponse> => {
    const endpoint = buildMyAllocationsQuery(weekStart, weeks);
    return apiRequest<MyAllocationsResponse>(endpoint, {
      method: "GET",
    });
  },

  confirmScheduleViewed: async (weekStart: string): Promise<ConfirmScheduleResponse> => {
    return apiRequest<ConfirmScheduleResponse>(`/my-allocations/${weekStart}/confirm-viewed`, {
      method: "POST",
    });
  },

  provideFeedback: async (weekStart: string, reason: string): Promise<ProvideFeedbackResponse> => {
    return apiRequest<ProvideFeedbackResponse>(`/my-allocations/${weekStart}/feedback`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    });
  },
};