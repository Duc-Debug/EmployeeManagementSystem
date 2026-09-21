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

/**
 * Lấy danh sách hợp đồng thuê ngoài sắp hết hạn hoặc quá hạn kèm phân bổ bị ảnh hưởng (QTN-21 / NCL-14-CN-003)
 */
export async function getExpiringOutsourcedContracts(
  thresholdDays: number = 30
): Promise<ExpiringOutsourcedContractListResult> {
  return apiRequest<ExpiringOutsourcedContractListResult>(
    `/outsourced-contracts/expiring?thresholdDays=${thresholdDays}`
  );
}

/**
 * Kích hoạt quét và phát cảnh báo hợp đồng thuê ngoài thủ công (NCL-14-CN-003)
 */
export async function scanOutsourcedContractsManually(): Promise<ScanOutsourcedContractsResult> {
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
  return apiRequest<AcknowledgeOutsourcedContractResult>(
    `/outsourced-contracts/${employeeId}/acknowledge`,
    {
      method: "POST",
      body: JSON.stringify(payload),
    }
  );
}
