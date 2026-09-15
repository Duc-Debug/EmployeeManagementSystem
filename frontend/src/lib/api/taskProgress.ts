"use client";

import { apiRequest } from "../api-client";

/**
 * Danh sách trạng thái tiến độ hợp lệ dành cho Chuyên viên (Người thực hiện).
 * Chú ý: Tuyệt đối không bao gồm CANCELLED vì quyền hủy việc thuộc về Quản lý dự án (PM).
 */
export type SpecialistTaskProgressStatus = "TODO" | "IN_PROGRESS" | "IN_REVIEW" | "DONE";

export const SPECIALIST_TASK_STATUSES: readonly SpecialistTaskProgressStatus[] = [
  "TODO",
  "IN_PROGRESS",
  "IN_REVIEW",
  "DONE",
] as const;

export interface TaskProgressResult {
  taskId: number;
  projectId: number;
  projectCode: string;
  projectName: string;
  taskCode: string;
  name: string;
  previousStatus: string;
  currentStatus: SpecialistTaskProgressStatus;
  updatedAt: string;
}

export interface TaskProgressApiResponse {
  success: boolean;
  message: string;
  data: TaskProgressResult;
}

export interface UpdateTaskProgressPayload {
  status: SpecialistTaskProgressStatus;
}

export interface MyAssignedTaskItem {
  taskId: number;
  taskCode: string;
  taskName: string;
  projectId: number;
  projectName: string;
  status: string;
  plannedStartDate?: string;
  plannedEndDate?: string;
  isPrimary?: boolean;
  assignedAt?: string;
}

export interface ProgressStatusMeta {
  status: SpecialistTaskProgressStatus;
  label: string;
  shortLabel: string;
  description: string;
  badgeClass: string;
  badgeActiveRing: string;
  indicatorClass: string;
}

export const PROGRESS_STATUS_METADATA: Record<SpecialistTaskProgressStatus, ProgressStatusMeta> = {
  TODO: {
    status: "TODO",
    label: "Chờ thực hiện",
    shortLabel: "Chờ làm",
    description: "Công việc đã được giao, chưa bắt đầu triển khai",
    badgeClass: "bg-slate-100 text-slate-700 border-slate-200 hover:bg-slate-200/70",
    badgeActiveRing: "ring-slate-400 border-slate-400 bg-slate-50",
    indicatorClass: "bg-slate-400",
  },
  IN_PROGRESS: {
    status: "IN_PROGRESS",
    label: "Đang thực hiện",
    shortLabel: "Đang làm",
    description: "Đang trong quá trình triển khai xử lý công việc",
    badgeClass: "bg-sky-50 text-sky-700 border-sky-200 hover:bg-sky-100/70",
    badgeActiveRing: "ring-sky-500 border-sky-400 bg-sky-50/70",
    indicatorClass: "bg-sky-500",
  },
  IN_REVIEW: {
    status: "IN_REVIEW",
    label: "Chờ duyệt / Nghiệm thu",
    shortLabel: "Chờ duyệt",
    description: "Đã hoàn tất xử lý, đang gửi PM hoặc Tech Lead kiểm tra nghiệm thu",
    badgeClass: "bg-amber-50 text-amber-700 border-amber-200 hover:bg-amber-100/70",
    badgeActiveRing: "ring-amber-500 border-amber-400 bg-amber-50/70",
    indicatorClass: "bg-amber-500",
  },
  DONE: {
    status: "DONE",
    label: "Hoàn thành",
    shortLabel: "Xong",
    description: "Công việc đã nghiệm thu xong và đóng hoàn tất",
    badgeClass: "bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100/70",
    badgeActiveRing: "ring-emerald-500 border-emerald-400 bg-emerald-50/70",
    indicatorClass: "bg-emerald-500",
  },
};

/**
 * Kiểm tra xem một chuỗi có phải là trạng thái tiến độ hợp lệ của Chuyên viên hay không
 */
export function isValidSpecialistStatus(status: unknown): status is SpecialistTaskProgressStatus {
  return typeof status === "string" && (SPECIALIST_TASK_STATUSES as readonly string[]).includes(status);
}

/**
 * Xác định trạng thái tiến độ gợi ý kế tiếp cho Chuyên viên theo quy trình:
 * TODO -> IN_PROGRESS -> IN_REVIEW -> DONE
 */
export function getNextSuggestedStatus(currentStatus: string): SpecialistTaskProgressStatus | null {
  switch (currentStatus) {
    case "TODO":
      return "IN_PROGRESS";
    case "IN_PROGRESS":
      return "IN_REVIEW";
    case "IN_REVIEW":
      return "DONE";
    default:
      return null;
  }
}

/**
 * Nhãn hành động nhanh cho việc nâng tiến độ 1-click
 */
export const QUICK_ADVANCE_LABELS: Record<SpecialistTaskProgressStatus, string> = {
  TODO: "Bắt đầu làm",
  IN_PROGRESS: "Gửi nghiệm thu",
  IN_REVIEW: "Hoàn thành",
  DONE: "Đã hoàn tất",
};

/**
 * Sắp xếp danh sách công việc theo hạn chót (plannedEndDate)
 * Các công việc chưa có hạn chót được xếp về cuối danh sách
 */
export function sortTasksByDeadline(
  tasks: MyAssignedTaskItem[],
  direction: "asc" | "desc" = "asc"
): MyAssignedTaskItem[] {
  return [...tasks].sort((a, b) => {
    const dateA = a.plannedEndDate ? new Date(a.plannedEndDate).getTime() : null;
    const dateB = b.plannedEndDate ? new Date(b.plannedEndDate).getTime() : null;

    if (dateA === null && dateB === null) return 0;
    if (dateA === null) return 1;
    if (dateB === null) return -1;

    return direction === "asc" ? dateA - dateB : dateB - dateA;
  });
}

/**
 * Cập nhật tiến độ công việc dành cho chuyên viên được phân công
 * Endpoint: PATCH /api/v1/tasks/{taskId}/progress
 */
export async function updateTaskProgress(
  taskId: number | string,
  status: SpecialistTaskProgressStatus
): Promise<TaskProgressResult> {
  if (!isValidSpecialistStatus(status)) {
    throw new Error(
      `Trạng thái '${status}' không hợp lệ. Chuyên viên chỉ được cập nhật: ${SPECIALIST_TASK_STATUSES.join(", ")}`
    );
  }

  const response = await apiRequest<TaskProgressApiResponse>(`/tasks/${taskId}/progress`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });

  if (!response?.data) {
    throw new Error(response?.message || "Không nhận được phản hồi dữ liệu từ máy chủ");
  }

  return response.data;
}

/**
 * Lấy danh sách toàn bộ công việc được phân công cho nhân sự đang đăng nhập
 * Endpoint: GET /api/v1/tasks/me
 */
export async function getMyAssignedTasks(): Promise<MyAssignedTaskItem[]> {
  return await apiRequest<MyAssignedTaskItem[]>("/tasks/me");
}

export interface TaskProgressKpiStats {
  total: number;
  todo: number;
  inProgress: number;
  inReview: number;
  done: number;
}

/**
 * Tính toán thống kê KPI theo từng giai đoạn tiến độ
 */
export function calculateTaskStats(tasks: MyAssignedTaskItem[]): TaskProgressKpiStats {
  return {
    total: tasks.length,
    todo: tasks.filter((t) => t.status === "TODO").length,
    inProgress: tasks.filter((t) => t.status === "IN_PROGRESS").length,
    inReview: tasks.filter((t) => t.status === "IN_REVIEW").length,
    done: tasks.filter((t) => t.status === "DONE").length,
  };
}

/**
 * Lọc danh sách công việc theo trạng thái (tab) và từ khóa tìm kiếm
 */
export function filterAssignedTasks(
  tasks: MyAssignedTaskItem[],
  statusTab: string,
  query?: string
): MyAssignedTaskItem[] {
  return tasks.filter((task) => {
    if (statusTab !== "ALL" && task.status !== statusTab) {
      return false;
    }
    if (query && query.trim()) {
      const q = query.toLowerCase().trim();
      const matchCode = task.taskCode?.toLowerCase().includes(q);
      const matchName = task.taskName?.toLowerCase().includes(q);
      const matchProject = task.projectName?.toLowerCase().includes(q);
      if (!matchCode && !matchName && !matchProject) {
        return false;
      }
    }
    return true;
  });
}

export interface TaskProgressPayloadResult {
  shouldSend: boolean;
  reason?: string;
  endpoint?: (taskId: number | string) => string;
  method?: string;
  body?: UpdateTaskProgressPayload;
}

/**
 * Xây dựng payload và kiểm tra tiền điều kiện trước khi gửi request cập nhật tiến độ
 */
export function buildUpdateTaskProgressPayload(
  currentStatus: string,
  newStatus: SpecialistTaskProgressStatus
): TaskProgressPayloadResult {
  if (!isValidSpecialistStatus(newStatus)) {
    throw new Error(`Trạng thái '${newStatus}' không hợp lệ cho Chuyên viên`);
  }
  if (currentStatus === newStatus) {
    return { shouldSend: false, reason: "SAME_STATUS" };
  }
  if (currentStatus === "CANCELLED") {
    throw new Error("Không thể cập nhật công việc đã bị hủy");
  }
  return {
    shouldSend: true,
    endpoint: (taskId: number | string) => `/tasks/${taskId}/progress`,
    method: "PATCH",
    body: { status: newStatus },
  };
}

/**
 * Chuẩn hóa thông báo lỗi trả về từ Backend hoặc mạng khi cập nhật tiến độ
 */
export function formatTaskProgressError(err: unknown): string {
  if (err && typeof err === "object") {
    const errorWithStatus = err as { status?: number; message?: string };
    if (errorWithStatus.status === 403) {
      return "Bạn không được phân công thực hiện công việc này. Vui lòng liên hệ PM để kiểm tra.";
    }
    if (errorWithStatus.status === 400) {
      return errorWithStatus.message || "Dự án đã đóng hoặc kết thúc, không được phép cập nhật tiến độ công việc.";
    }
    if (errorWithStatus.status === 404) {
      return "Không tìm thấy công việc tương ứng trên hệ thống.";
    }
    if (errorWithStatus.message) {
      return errorWithStatus.message;
    }
  }
  return err instanceof Error ? err.message : "Đã xảy ra lỗi khi cập nhật tiến độ công việc.";
}

