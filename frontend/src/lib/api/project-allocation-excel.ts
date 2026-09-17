"use client";

import { API_BASE_URL, ApiError } from "../api-client";
import { getAuthToken } from "../auth-session";
import { getIsoWeeksInYear } from "../iso-week";

/**
 * Tham số đầu vào cho API xuất báo cáo phân bổ dự án theo tuần (NCL-10-CN-003).
 */
export interface ExportProjectAllocationExcelParams {
  projectId: number;
  fromYear?: number;
  fromWeek?: number;
  toYear?: number;
  toWeek?: number;
  all?: boolean;
}

/**
 * Kết quả trả về sau khi xuất file Excel.
 */
export interface ExportProjectAllocationExcelResult {
  filename: string;
  blob: Blob;
}

/**
 * Tạo URL endpoint gọi backend cùng các query parameters.
 */
export function buildExportProjectAllocationUrl(params: ExportProjectAllocationExcelParams): string {
  const query = new URLSearchParams();
  query.append("projectId", String(params.projectId));
  if (params.all) {
    query.append("all", "true");
  } else {
    if (params.fromYear !== undefined && params.fromYear !== null) {
      query.append("fromYear", String(params.fromYear));
    }
    if (params.fromWeek !== undefined && params.fromWeek !== null) {
      query.append("fromWeek", String(params.fromWeek));
    }
    if (params.toYear !== undefined && params.toYear !== null) {
      query.append("toYear", String(params.toYear));
    }
    if (params.toWeek !== undefined && params.toWeek !== null) {
      query.append("toWeek", String(params.toWeek));
    }
  }
  return `${API_BASE_URL}/reports/export/excel/project-allocation?${query.toString()}`;
}

/**
 * Trích xuất tên file từ header Content-Disposition trả về từ backend.
 */
export function extractFilenameFromContentDisposition(
  contentDisposition: string | null | undefined,
  fallback: string = "Bao_Cao_Phan_Bo_Du_An.xlsx"
): string {
  if (!contentDisposition) return fallback;

  // Hỗ trợ cả định dạng chuẩn RFC 5987 (filename*=UTF-8''...)
  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
  if (utf8Match && utf8Match[1]) {
    try {
      return decodeURIComponent(utf8Match[1]);
    } catch {
      return utf8Match[1];
    }
  }

  // Hỗ trợ định dạng thông thường (filename="...")
  const standardMatch = /filename="?([^";]+)"?/i.exec(contentDisposition);
  if (standardMatch && standardMatch[1]) {
    return standardMatch[1].trim();
  }

  return fallback;
}

/**
 * Kiểm tra tính hợp lệ của dải thời gian xuất báo cáo.
 */
export function validateExportPeriod(
  fromYear?: number,
  fromWeek?: number,
  toYear?: number,
  toWeek?: number
): { isValid: boolean; error?: string } {
  // Nếu chỉ nhập 1 trong 2 giá trị của kỳ bắt đầu
  if ((fromYear !== undefined && fromWeek === undefined) || (fromYear === undefined && fromWeek !== undefined)) {
    return { isValid: false, error: "Vui lòng chọn đầy đủ cả năm và tuần bắt đầu." };
  }

  // Nếu chỉ nhập 1 trong 2 giá trị của kỳ kết thúc
  if ((toYear !== undefined && toWeek === undefined) || (toYear === undefined && toWeek !== undefined)) {
    return { isValid: false, error: "Vui lòng chọn đầy đủ cả năm và tuần kết thúc." };
  }

  if (fromYear !== undefined && fromWeek !== undefined) {
    const maxFromWeek = getIsoWeeksInYear(fromYear);
    if (fromWeek < 1 || fromWeek > maxFromWeek) {
      return { isValid: false, error: `Tuần bắt đầu không hợp lệ (năm ${fromYear} có từ 1 đến ${maxFromWeek} tuần).` };
    }
  }

  if (toYear !== undefined && toWeek !== undefined) {
    const maxToWeek = getIsoWeeksInYear(toYear);
    if (toWeek < 1 || toWeek > maxToWeek) {
      return { isValid: false, error: `Tuần kết thúc không hợp lệ (năm ${toYear} có từ 1 đến ${maxToWeek} tuần).` };
    }
  }

  if (fromYear !== undefined && fromWeek !== undefined && toYear !== undefined && toWeek !== undefined) {
    if (fromYear > toYear || (fromYear === toYear && fromWeek > toWeek)) {
      return { isValid: false, error: "Thời gian bắt đầu (Từ tuần/năm) không được lớn hơn thời gian kết thúc (Đến tuần/năm)." };
    }
  }

  return { isValid: true };
}

/**
 * Kiểm tra quyền hạn của người dùng đối với chức năng xuất báo cáo Excel.
 * Bắt buộc thuộc vai trò Ban Giám Đốc (VT-01) hoặc Quản lý dự án (VT-02)
 * VÀ phải có quyền RESOURCE_ALLOCATION_READ.
 */
export function canExportProjectAllocationExcel(
  roleCode?: string | null,
  permissions?: readonly string[] | null,
  currentEmployeeId?: number | string | null,
  projectManagerId?: number | string | null
): boolean {
  if (permissions && !permissions.includes("RESOURCE_ALLOCATION_READ")) {
    return false;
  }
  if (!roleCode) return false;
  const normalized = roleCode.toUpperCase().replace(/_/g, "-");

  // Ban Giám đốc (VT-01) có toàn quyền xuất
  if (["VT-01", "ROLE-EXECUTIVE", "EXECUTIVE", "DIRECTOR"].includes(normalized)) {
    return true;
  }

  // Quản lý dự án (VT-02): Nếu có thông tin PM và Employee ID thì phải đúng là PM của dự án đó
  if (["VT-02", "ROLE-PM", "PM", "PROJECT-MANAGER"].includes(normalized)) {
    if (currentEmployeeId != null && projectManagerId != null) {
      return String(currentEmployeeId) === String(projectManagerId);
    }
    return true;
  }

  return false;
}

/**
 * Thực hiện gọi API tải file Excel phân bổ dự án và kích hoạt tải về trình duyệt.
 */
export async function exportProjectAllocationExcel(
  params: ExportProjectAllocationExcelParams,
  triggerBrowserDownload: boolean = true
): Promise<ExportProjectAllocationExcelResult> {
  if (!params.projectId) {
    throw new ApiError("Mã dự án (projectId) là bắt buộc.", 400);
  }

  if (!params.all) {
    const validation = validateExportPeriod(params.fromYear, params.fromWeek, params.toYear, params.toWeek);
    if (!validation.isValid) {
      throw new ApiError(validation.error || "Khoảng thời gian không hợp lệ.", 400);
    }
  }

  const url = buildExportProjectAllocationUrl(params);
  const token = getAuthToken();

  const headers: Record<string, string> = {};
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    method: "GET",
    headers,
  });

  if (!response.ok) {
    let errorMessage = `Xuất báo cáo thất bại với mã lỗi ${response.status}`;
    try {
      const contentType = response.headers.get("Content-Type");
      if (contentType && contentType.includes("application/json")) {
        const errorJson = await response.json();
        if (errorJson && typeof errorJson === "object") {
          const err = errorJson as Record<string, unknown>;
          if (err.code === "REPORT_NO_DATA") {
            errorMessage = "Không có dữ liệu phân bổ trong kỳ đã chọn để xuất báo cáo.";
          } else if (err.code === "FORBIDDEN") {
            errorMessage = "Bạn không có quyền xuất báo cáo phân bổ cho dự án này.";
          } else if (err.code === "PROJECT_NOT_FOUND") {
            errorMessage = "Không tìm thấy thông tin dự án.";
          } else if (typeof err.message === "string" && err.message.trim().length > 0) {
            errorMessage = err.message;
          }
        }
      } else {
        const text = await response.text();
        if (text && text.trim().length > 0) {
          errorMessage = text;
        }
      }
    } catch {
      // Fallback to generic message
    }
    throw new ApiError(errorMessage, response.status);
  }

  const blob = await response.blob();
  const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, "");
  const fallbackFilename = `Bao_Cao_Phan_Bo_Du_An_${params.projectId}_${dateStr}.xlsx`;
  const filename = extractFilenameFromContentDisposition(
    response.headers.get("Content-Disposition"),
    fallbackFilename
  );

  if (triggerBrowserDownload && typeof window !== "undefined" && typeof document !== "undefined") {
    const blobUrl = window.URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = blobUrl;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.URL.revokeObjectURL(blobUrl);
  }

  return { filename, blob };
}
