import { apiRequest } from "../../../lib/api-client";
import type { MyAllocationsResponse, ConfirmScheduleResponse, ProvideFeedbackResponse } from "../types";

export const myScheduleApi = {
  getMyAllocations: async (weekStart?: string, weeks?: number): Promise<MyAllocationsResponse> => {
    const params = new URLSearchParams();
    if (weekStart) {
      params.append("week_start", weekStart);
    }
    if (weeks !== undefined && weeks !== null) {
      params.append("weeks", weeks.toString());
    }

    const queryString = params.toString();
    const endpoint = queryString ? `/my-allocations?${queryString}` : "/my-allocations";
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