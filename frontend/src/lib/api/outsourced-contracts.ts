"use client";

import { apiRequest } from "../api-client";

export type AffectedAllocationType = "SPANS_OVER_EXPIRY" | "AFTER_EXPIRY";

export type OutsourcedContractStatus = "ACTIVE_SAFE" | "EXPIRING_SOON" | "EXPIRED";

export interface OutsourcedYearWeek {
  year: number;
  weekNumber: number;
}

export interface OutsourcedContractAffectedAllocation {
  allocationId: number;
  projectId: number;
  projectName: string;
  yearWeek: OutsourcedYearWeek;
  weekStartDate: string;
  weekEndDate: string;
  allocatedHours: number;
  affectedType: AffectedAllocationType;
  reason: string;
}

export interface ExpiringOutsourcedContract {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  professionalRole: string | null;
  orgUnitId: number | null;
  orgUnitName: string;
  contractStartDate: string | null;
  contractEndDate: string;
  daysRemaining: number;
  status: OutsourcedContractStatus;
  affectedAllocations: OutsourcedContractAffectedAllocation[];
}

export interface ExpiringOutsourcedContractListResult {
  totalExpiringContracts: number;
  items: ExpiringOutsourcedContract[];
}

export interface ScanOutsourcedContractsResult {
  scannedAt: string;
  totalScanned: number;
  expiringContractsFound: number;
  notificationsSent: number;
  details: string;
}

export interface AcknowledgeOutsourcedContractPayload {
  actionNote?: string | null;
  expectedResolutionDate?: string | null;
}

export interface AcknowledgeOutsourcedContractResult {
  employeeId: number;
  acknowledgedAt: string;
  status: string;
  message: string;
}

// ==================== In-Memory Query Cache ====================

interface CacheEntry<T> {
  data: T;
  timestamp: number;
}

export const OUTSOURCED_CONTRACT_CACHE_TTL_MS = 60_000; // 60 seconds
const cache = new Map<number, CacheEntry<ExpiringOutsourcedContractListResult>>();

/**
 * Xóa bộ nhớ đệm hợp đồng thuê ngoài
 */
export function clearOutsourcedContractsCache(): void {
  cache.clear();
}

/**
 * Lấy danh sách hợp đồng thuê ngoài sắp hết hạn kèm bộ đệm (Query Cache) chống nhấp nháy khi chuyển tab
 */
export async function getExpiringOutsourcedContracts(
  thresholdDays: number = 30,
  forceRefresh: boolean = false
): Promise<ExpiringOutsourcedContractListResult> {
  const now = Date.now();
  const cached = cache.get(thresholdDays);

  if (!forceRefresh && cached && now - cached.timestamp < OUTSOURCED_CONTRACT_CACHE_TTL_MS) {
    return cached.data;
  }

  const result = await apiRequest<ExpiringOutsourcedContractListResult>(
    `/outsourced-contracts/expiring?thresholdDays=${thresholdDays}`
  );

  cache.set(thresholdDays, {
    data: result,
    timestamp: now,
  });

  return result;
}

/**
 * Kích hoạt quét và phát cảnh báo hợp đồng thuê ngoài thủ công (NCL-14-CN-003)
 */
export async function scanOutsourcedContractsManually(): Promise<ScanOutsourcedContractsResult> {
  clearOutsourcedContractsCache();
  return apiRequest<ScanOutsourcedContractsResult>(
    "/outsourced-contracts/scan",
    {
      method: "POST",
    }
  );
}

/**
 * Xác nhận ghi nhận/xử lý cảnh báo hợp đồng thuê ngoài (NCL-14-CN-003-TC-04)
 */
export async function acknowledgeOutsourcedContractWarning(
  employeeId: number,
  payload: AcknowledgeOutsourcedContractPayload
): Promise<AcknowledgeOutsourcedContractResult> {
  clearOutsourcedContractsCache();
  return apiRequest<AcknowledgeOutsourcedContractResult>(
    `/outsourced-contracts/${employeeId}/acknowledge`,
    {
      method: "POST",
      body: JSON.stringify(payload),
    }
  );
}

/**
 * Xuất danh sách hợp đồng cảnh báo ra file CSV định dạng UTF-8 BOM chuẩn cho Excel
 */
export function exportOutsourcedContractsToCsv(contracts: ExpiringOutsourcedContract[]): string {
  const headers = [
    "Mã NV",
    "Họ và Tên",
    "Vị Trí Chuyên Môn",
    "Đơn Vị / Chi Nhánh",
    "Ngày Hết Hạn",
    "Số Ngày Còn Lại",
    "Trạng Thái Hợp Đồng",
    "Số Phân Bổ Vi Phạm QTN-21",
  ];

  const rows = contracts.map((c) => [
    `"${c.employeeCode}"`,
    `"${c.fullName.replace(/"/g, '""')}"`,
    `"${(c.professionalRole || "N/A").replace(/"/g, '""')}"`,
    `"${(c.orgUnitName || "N/A").replace(/"/g, '""')}"`,
    `"${c.contractEndDate}"`,
    c.daysRemaining,
    `"${c.status === "EXPIRED" ? "Đã quá hạn" : "Sắp hết hạn"}"`,
    c.affectedAllocations ? c.affectedAllocations.length : 0,
  ]);

  const csvRows = [headers.join(","), ...rows.map((r) => r.join(","))];
  return "\uFEFF" + csvRows.join("\r\n");
}
