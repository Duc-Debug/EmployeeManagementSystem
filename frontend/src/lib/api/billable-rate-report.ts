import { apiRequest } from "../api-client";

export interface BillableRateItem {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  orgUnitId: number | null;
  orgUnitName: string;
  standardHours: number;
  holidayHours: number;
  approvedLeaveHours: number;
  netAvailableHours: number;
  billableHours: number;
  nonBillableHours: number;
  totalActualHours: number;
  billableRate: number | null;
  hasAvailableHours: boolean;
  status: string;
}

export interface DepartmentBillableRateSummary {
  orgUnitId: number | null;
  orgUnitName: string;
  totalStandardHours: number;
  totalApprovedLeaveHours: number;
  totalAvailableHours: number;
  totalBillableHours: number;
  totalNonBillableHours: number;
  totalActualHours: number;
  billableRate: number | null;
  employeeCount: number;
}

export interface BillableRateSummary {
  totalStandardHours: number;
  totalHolidayHours: number;
  totalApprovedLeaveHours: number;
  totalAvailableHours: number;
  totalBillableHours: number;
  totalNonBillableHours: number;
  totalActualHours: number;
  overallBillableRate: number | null;
  totalEmployees: number;
  totalDepartments: number;
  targetWeeksCount: number;
}

export interface BillableRateResult {
  orgUnitId: number | null;
  orgUnitName: string;
  fromYear: number;
  fromWeek: number;
  toYear: number;
  toWeek: number;
  startDate: string;
  endDate: string;
  departmentBreakdown: DepartmentBillableRateSummary[];
  employeeBreakdown: BillableRateItem[];
  summary: BillableRateSummary;
  hasData: boolean;
  message: string;
  generatedAt: string;
}

export interface BillableRateQuery {
  orgUnitId?: number;
  employeeId?: number;
  fromYear?: number;
  fromWeek?: number;
  toYear?: number;
  toWeek?: number;
}

/**
 * Lấy dữ liệu báo cáo tỷ lệ giờ tính phí (NCL-10-CN-002).
 */
export async function getBillableRateReport(
  query: BillableRateQuery,
  signal?: AbortSignal
): Promise<BillableRateResult> {
  const params = new URLSearchParams();
  if (query.orgUnitId != null) params.append("orgUnitId", String(query.orgUnitId));
  if (query.employeeId != null) params.append("employeeId", String(query.employeeId));
  if (query.fromYear != null) params.append("fromYear", String(query.fromYear));
  if (query.fromWeek != null) params.append("fromWeek", String(query.fromWeek));
  if (query.toYear != null) params.append("toYear", String(query.toYear));
  if (query.toWeek != null) params.append("toWeek", String(query.toWeek));

  const queryStr = params.toString();
  const url = `/reports/billable-rate${queryStr ? `?${queryStr}` : ""}`;

  return await apiRequest<BillableRateResult>(url, { signal });
}

/**
 * Tải file CSV báo cáo tỷ lệ giờ tính phí.
 */
export async function downloadBillableRateReport(
  query: BillableRateQuery
): Promise<void> {
  const params = new URLSearchParams();
  if (query.orgUnitId != null) params.append("orgUnitId", String(query.orgUnitId));
  if (query.employeeId != null) params.append("employeeId", String(query.employeeId));
  if (query.fromYear != null) params.append("fromYear", String(query.fromYear));
  if (query.fromWeek != null) params.append("fromWeek", String(query.fromWeek));
  if (query.toYear != null) params.append("toYear", String(query.toYear));
  if (query.toWeek != null) params.append("toWeek", String(query.toWeek));

  const queryStr = params.toString();
  const url = `/reports/billable-rate/export${queryStr ? `?${queryStr}` : ""}`;

  const token = typeof window !== "undefined" ? localStorage.getItem("accessToken") : null;
  const baseUrl = "/api/v1";

  const response = await fetch(`${baseUrl}${url}`, {
    method: "GET",
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  if (!response.ok) {
    throw new Error("Xuất báo cáo thất bại");
  }

  const blob = await response.blob();
  const downloadUrl = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = downloadUrl;
  const disposition = response.headers.get("content-disposition");
  let filename = `bao-cao-ty-le-gio-tinh-phi.csv`;
  if (disposition && disposition.includes("filename=")) {
    const match = disposition.match(/filename="?([^"]+)"?/);
    if (match && match[1]) {
      filename = match[1];
    }
  }
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(downloadUrl);
}
