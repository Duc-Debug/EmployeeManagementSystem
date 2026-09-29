"use client";

import { apiRequest } from "../api-client";

export interface WeeklyIdlenessDetail {
  year: number;
  weekNumber: number;
  availableHours: number;
  allocatedHours: number;
  emptyHours: number;
  utilizationPercentage: number;
  isUnderutilized: boolean;
  isFullLeaveWeek: boolean;
}

export interface ProlongedIdleStaffItem {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  orgUnitId: number | null;
  departmentName: string;
  positionTitle: string;
  consecutiveIdleWeeks: number;
  totalEmptyHours: number;
  averageUtilization: number;
  weeklyBreakdown?: WeeklyIdlenessDetail[];
  weeklyDetails?: WeeklyIdlenessDetail[];
  status?: "OPEN" | "ACKNOWLEDGED";
  actionTaken?: string | null;
  ackNotes?: string | null;
  acknowledgedAt?: string | null;
  acknowledgedBy?: number | null;
}

export interface ProlongedIdlenessReportResult {
  orgUnitId: number | null;
  orgUnitName: string;
  fromYear: number;
  fromWeek: number;
  durationWeeks: number;
  effectiveIdleThreshold: number;
  consecutiveThreshold: number;
  totalIdleEmployees: number;
  totalEmptyHours: number;
  page: number;
  size: number;
  totalPages: number;
  items: ProlongedIdleStaffItem[];
}

export interface ProlongedIdlenessQueryParams {
  orgUnitId?: number | null;
  fromYear?: number | null;
  fromWeek?: number | null;
  durationWeeks?: number | null;
  consecutiveThreshold?: number | null;
  search?: string | null;
  status?: "ALL" | "OPEN" | "ACKNOWLEDGED" | null;
  page?: number | null;
  size?: number | null;
}

export interface AcknowledgeProlongedIdleStaffPayload {
  employeeId: number;
  actionTaken: string;
  notes?: string | null;
  fromYear?: number | null;
  fromWeek?: number | null;
  durationWeeks?: number | null;
}

export interface AcknowledgeProlongedIdleStaffResult {
  employeeId: number;
  employeeName: string;
  actionTaken: string;
  notes: string | null;
  acknowledgedBy: number;
  acknowledgedAt: string;
  status: string;
}

/**
 * Lấy danh sách nhân sự bị cảnh báo nhàn rỗi kéo dài (NCL-07-CN-006 / QTN-23)
 */
export async function getProlongedIdleStaff(
  params?: ProlongedIdlenessQueryParams
): Promise<ProlongedIdlenessReportResult> {
  const query = new URLSearchParams();

  if (params?.orgUnitId !== undefined && params.orgUnitId !== null) {
    query.append("orgUnitId", String(params.orgUnitId));
  }
  if (params?.fromYear !== undefined && params.fromYear !== null) {
    query.append("fromYear", String(params.fromYear));
  }
  if (params?.fromWeek !== undefined && params.fromWeek !== null) {
    query.append("fromWeek", String(params.fromWeek));
  }
  if (params?.durationWeeks !== undefined && params.durationWeeks !== null) {
    query.append("durationWeeks", String(params.durationWeeks));
  }
  if (params?.consecutiveThreshold !== undefined && params.consecutiveThreshold !== null) {
    query.append("consecutiveThreshold", String(params.consecutiveThreshold));
  }
  if (params?.search && params.search.trim()) {
    query.append("search", params.search.trim());
  }
  if (params?.status) {
    query.append("status", params.status);
  }
  if (params?.page !== undefined && params.page !== null) {
    query.append("page", String(params.page));
  }
  if (params?.size !== undefined && params.size !== null) {
    query.append("size", String(params.size));
  }

  const queryString = query.toString();
  const endpoint = `/capacity/prolonged-idleness${queryString ? `?${queryString}` : ""}`;
  return apiRequest<ProlongedIdlenessReportResult>(endpoint);
}

/**
 * Xác nhận xử lý cảnh báo nhân sự nhàn rỗi kéo dài (NCL-07-CN-006-TC-04)
 */
export async function acknowledgeProlongedIdleStaff(
  payload: AcknowledgeProlongedIdleStaffPayload
): Promise<AcknowledgeProlongedIdleStaffResult> {
  return apiRequest<AcknowledgeProlongedIdleStaffResult>(
    "/capacity/prolonged-idleness/acknowledge",
    {
      method: "POST",
      body: JSON.stringify(payload),
    }
  );
}

/**
 * Tải toàn bộ danh sách nhân sự nhàn rỗi kéo dài phục vụ Export CSV sử dụng phân trang size <= 50 an toàn theo totalPages
 */
export async function fetchAllProlongedIdleStaff(
  params?: Omit<ProlongedIdlenessQueryParams, "page" | "size">,
  fetchPageFn: (params?: ProlongedIdlenessQueryParams) => Promise<ProlongedIdlenessReportResult> = getProlongedIdleStaff
): Promise<ProlongedIdleStaffItem[]> {
  let currentPage = 0;
  const exportPageSize = 50;
  const allItems: ProlongedIdleStaffItem[] = [];

  while (true) {
    const pageData = await fetchPageFn({
      ...params,
      page: currentPage,
      size: exportPageSize,
    });

    const items = pageData?.items || [];
    allItems.push(...items);
    const totalPages = pageData?.totalPages || 1;

    if (items.length === 0 || allItems.length >= (pageData?.totalIdleEmployees || 0) || currentPage + 1 >= totalPages) {
      break;
    }
    currentPage++;
  }

  return allItems;
}

export function canAccessProlongedIdleness(roleCode?: string | null): boolean {
  if (!roleCode) return false;
  const normalized = roleCode.toUpperCase().replace(/_/g, "-").replace(/^ROLE-/, "");
  return ["VT-01", "VT-03", "VT-06", "ADMIN"].includes(normalized);
}

export function validateAcknowledgeForm({ actionTaken, notes }: { actionTaken?: string | null; notes?: string | null }) {
  const errors: string[] = [];
  const trimmedAction = actionTaken ? actionTaken.trim() : "";

  if (!trimmedAction) {
    errors.push("Hành động xử lý không được để trống.");
  } else if (trimmedAction.length < 5) {
    errors.push("Hành động xử lý phải có ít nhất 5 ký tự.");
  }

  if (notes && notes.length > 500) {
    errors.push("Ghi chú không được vượt quá 500 ký tự.");
  }

  return {
    isValid: errors.length === 0,
    errors,
  };
}

export function classifyIdlenessSeverity(consecutiveWeeks: number, threshold = 3) {
  if (consecutiveWeeks >= threshold + 2) {
    return { level: "CRITICAL", label: "Nghiêm trọng", color: "rose" };
  }
  if (consecutiveWeeks >= threshold) {
    return { level: "WARNING", label: "Cảnh báo", color: "amber" };
  }
  return { level: "NORMAL", label: "Bình thường", color: "emerald" };
}

export function buildProlongedIdlenessQueryParams({
  orgUnitId,
  fromYear,
  fromWeek,
  durationWeeks = 4,
  consecutiveThreshold = 3,
  status,
  search,
  page = 0,
  size = 10,
}: {
  orgUnitId?: number | null;
  fromYear?: number | null;
  fromWeek?: number | null;
  durationWeeks?: number | null;
  consecutiveThreshold?: number | null;
  status?: string | null;
  search?: string | null;
  page?: number | null;
  size?: number | null;
} = {}): string {
  const params = new URLSearchParams();
  if (orgUnitId !== undefined && orgUnitId !== null) params.append("orgUnitId", String(orgUnitId));
  if (fromYear !== undefined && fromYear !== null) params.append("fromYear", String(fromYear));
  if (fromWeek !== undefined && fromWeek !== null) params.append("fromWeek", String(fromWeek));
  params.append("durationWeeks", String(Math.max(1, durationWeeks || 4)));
  params.append("consecutiveThreshold", String(Math.max(1, consecutiveThreshold || 3)));
  if (status) params.append("status", status);
  if (search && search.trim()) params.append("search", search.trim());
  params.append("page", String(Math.max(0, page || 0)));
  params.append("size", String(Math.max(1, size || 10)));
  return params.toString();
}

export function calculateIdlenessMetrics(items: ProlongedIdleStaffItem[] | null | undefined, reportTotalEmptyHours: number | null = null) {
  if (!items || items.length === 0) {
    return { totalIdle: 0, totalEmptyHours: 0, averageUtil: 0 };
  }
  const totalIdle = items.length;
  const totalEmptyHours = reportTotalEmptyHours !== null ? reportTotalEmptyHours : items.reduce((sum, item) => sum + (item.totalEmptyHours || 0), 0);
  const sumUtil = items.reduce((sum, item) => sum + (item.averageUtilization || 0), 0);
  const averageUtil = Number((sumUtil / totalIdle).toFixed(1));
  return { totalIdle, totalEmptyHours, averageUtil };
}

export function generateIdlenessCsvContent(items: ProlongedIdleStaffItem[]): string {
  const headers = [
    "Mã nhân viên",
    "Họ và tên",
    "Phòng ban",
    "Vị trí chuyên môn",
    "Số tuần nhàn rỗi liên tiếp",
    "Tỷ lệ sử dụng trung bình (%)",
    "Tổng giờ trống (h)",
    "Trạng thái",
    "Hành động can thiệp",
  ];

  const rows = items.map((item) => [
    `"${item.employeeCode}"`,
    `"${(item.fullName || "").replace(/"/g, '""')}"`,
    `"${(item.departmentName || "").replace(/"/g, '""')}"`,
    `"${(item.positionTitle || "").replace(/"/g, '""')}"`,
    item.consecutiveIdleWeeks,
    item.averageUtilization,
    item.totalEmptyHours,
    `"${item.status === 'ACKNOWLEDGED' ? 'Đã xử lý' : 'Chưa xử lý'}"`,
    `"${(item.actionTaken || '').replace(/"/g, '""')}"`,
  ]);

  return "\uFEFF" + [headers.join(","), ...rows.map((r) => r.join(","))].join("\n");
}
