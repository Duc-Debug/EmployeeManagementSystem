import { API_BASE_URL, apiRequest } from "../api-client";
import { getAuthToken } from "../auth-session";

export interface ImportEmployeeRowDto {
  rowNumber: number;
  employeeCode: string;
  fullName: string;
  username: string;
  email: string | null;
  orgUnitIdentifier: string;
  resolvedOrgUnitId: number | null;
  resolvedOrgUnitName: string | null;
  roleCode: string | null;
  professionalRole: string | null;
  standardHoursPerWeek: number | null;
  startDate: string | null;
  contractEndDate: string | null;
  isOutsourced: boolean;
  valid: boolean;
  errors: string[];
}

export interface ImportEmployeePreviewResult {
  totalRows: number;
  validRows: number;
  invalidRows: number;
  rows: ImportEmployeeRowDto[];
  canProceed: boolean;
  message: string;
}

export interface ConfirmEmployeeImportCommand {
  rows: ImportEmployeeRowDto[];
}

export interface ImportExecutionResult {
  importedCount: number;
  skippedCount: number;
  errors: string[];
  message: string;
  executedAt: string;
}

/**
 * Gửi tệp (.xlsx, .xls, .csv) lên backend để phân tích cú pháp và kiểm tra hợp lệ
 */
export async function previewEmployeeImport(file: File): Promise<ImportEmployeePreviewResult> {
  const formData = new FormData();
  formData.append("file", file);

  return apiRequest<ImportEmployeePreviewResult>("/api/v1/imports/employees/preview", {
    method: "POST",
    body: formData,
  });
}

/**
 * Xác nhận thực hiện nhập các dòng dữ liệu hợp lệ vào cơ sở dữ liệu
 */
export async function confirmEmployeeImport(rows: ImportEmployeeRowDto[]): Promise<ImportExecutionResult> {
  return apiRequest<ImportExecutionResult>("/api/v1/imports/employees/confirm", {
    method: "POST",
    body: JSON.stringify({ rows }),
  });
}

/**
 * Tải tệp biểu mẫu chuẩn (.xlsx hoặc .csv)
 */
export async function downloadEmployeeTemplate(format: "xlsx" | "csv" = "xlsx"): Promise<void> {
  const token = getAuthToken();
  const url = `${API_BASE_URL}/imports/employees/template?format=${format}`;

  const headers: HeadersInit = {};
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    method: "GET",
    headers,
  });

  if (!response.ok) {
    throw new Error(`Không thể tải biểu mẫu (Mã lỗi ${response.status})`);
  }

  const blob = await response.blob();
  const downloadUrl = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = downloadUrl;
  a.download = format === "xlsx" ? "Bieu_mau_nhap_nhan_vien.xlsx" : "Bieu_mau_nhap_nhan_vien.csv";
  document.body.appendChild(a);
  a.click();
  window.URL.revokeObjectURL(downloadUrl);
  document.body.removeChild(a);
}

/**
 * Xuất danh sách các dòng lỗi ra tệp CSV để người dùng dễ dàng đối soát và chỉnh sửa
 */
export function exportErrorRowsToCsv(rows: ImportEmployeeRowDto[]): void {
  const invalidRows = rows.filter((r) => !r.valid);
  if (invalidRows.length === 0) return;

  const headers = [
    "Dòng",
    "Mã nhân viên",
    "Họ và tên",
    "Tên đăng nhập",
    "Email",
    "Phòng ban / Đơn vị",
    "Mã vai trò",
    "Chức danh chuyên môn",
    "Giờ chuẩn",
    "Ngày bắt đầu",
    "Ngày kết thúc HĐ",
    "Thuê ngoài",
    "Chi tiết lỗi",
  ];

  const escapeCsv = (val: unknown): string => {
    if (val === null || val === undefined) return '""';
    const str = String(val).replace(/"/g, '""');
    return `"${str}"`;
  };

  const csvRows = [
    headers.map(escapeCsv).join(","),
    ...invalidRows.map((r) =>
      [
        r.rowNumber,
        r.employeeCode || "",
        r.fullName || "",
        r.username || "",
        r.email || "",
        r.orgUnitIdentifier || "",
        r.roleCode || "",
        r.professionalRole || "",
        r.standardHoursPerWeek ?? "",
        r.startDate || "",
        r.contractEndDate || "",
        r.isOutsourced ? "TRUE" : "FALSE",
        r.errors.join("; "),
      ]
        .map(escapeCsv)
        .join(",")
    ),
  ];

  const csvContent = "\uFEFF" + csvRows.join("\r\n");
  const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
  const downloadUrl = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = downloadUrl;
  a.download = `Danh_sach_nhan_vien_loi_${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(a);
  a.click();
  window.URL.revokeObjectURL(downloadUrl);
  document.body.removeChild(a);
}
