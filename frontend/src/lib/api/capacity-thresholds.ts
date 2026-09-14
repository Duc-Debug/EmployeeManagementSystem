"use client";

import { apiRequest } from "../api-client";

export type CapacityThresholdScope = "COMPANY" | "ORG_UNIT";

export interface CapacityThresholdResult {
  id: number | null;
  scopeType: CapacityThresholdScope;
  orgUnitId: number | null;
  overloadThreshold: number;
  idleThreshold: number;
  isDefault: boolean;
  isInherited: boolean;
  version: number | null;
  updatedAt: string | null;
  updatedBy: number | null;
  updaterName: string | null;
}

export interface ConfigureCapacityThresholdPayload {
  scopeType?: CapacityThresholdScope;
  orgUnitId?: number | null;
  overloadThreshold: number;
  idleThreshold: number;
  version?: number | null;
}

export interface CapacityThresholdHistoryResult {
  id: number;
  userId: number | null;
  userName: string | null;
  action: string;
  oldValue: string | null;
  newValue: string | null;
  createdAt: string;
}

/**
 * Lấy cấu hình ngưỡng cảnh báo năng lực (NCL-07-CN-004 / QTN-23)
 */
export async function getCapacityThreshold(
  scopeType: CapacityThresholdScope = "COMPANY",
  orgUnitId?: number
): Promise<CapacityThresholdResult> {
  const query = new URLSearchParams();
  query.append("scopeType", scopeType);
  if (orgUnitId !== undefined && orgUnitId !== null) {
    query.append("orgUnitId", String(orgUnitId));
  }
  return apiRequest<CapacityThresholdResult>(
    `/capacity-thresholds?${query.toString()}`
  );
}

/**
 * Cập nhật cấu hình ngưỡng cảnh báo năng lực (NCL-07-CN-004 / QTN-23)
 * Chỉ dành cho Ban Giám đốc (VT-01).
 */
export async function configureCapacityThreshold(
  payload: ConfigureCapacityThresholdPayload
): Promise<CapacityThresholdResult> {
  return apiRequest<CapacityThresholdResult>("/capacity-thresholds", {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

/**
 * Lấy lịch sử thay đổi cấu hình ngưỡng cảnh báo năng lực (NCL-07-CN-004 / QTN-23)
 */
export async function getCapacityThresholdHistory(
  scopeType: CapacityThresholdScope = "COMPANY",
  orgUnitId?: number
): Promise<CapacityThresholdHistoryResult[]> {
  const query = new URLSearchParams();
  query.append("scopeType", scopeType);
  if (orgUnitId !== undefined && orgUnitId !== null) {
    query.append("orgUnitId", String(orgUnitId));
  }
  return apiRequest<CapacityThresholdHistoryResult[]>(
    `/capacity-thresholds/history?${query.toString()}`
  );
}
