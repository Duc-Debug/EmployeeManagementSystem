"use client";

import { apiRequest } from "../api-client";
import type { DayOfWeek } from "./working-calendar";

export type CapacityUnit = "HOURS" | "DAYS" | "FTE";
export type WeekStartDay = "MONDAY" | "SUNDAY";

export interface StandardWorkWeekDay {
  dayOfWeek: DayOfWeek;
  isWorkingDay: boolean;
  workingHours: number;
}

export interface StandardWorkWeekConfig {
  id: number;
  scopeType: "COMPANY" | "ORG_UNIT";
  orgUnitId: number | null;
  scopeKey: string;
  capacityUnit: CapacityUnit;
  weekStartDay: WeekStartDay;
  standardHoursPerDay: number;
  standardHoursPerWeek: number;
  days: StandardWorkWeekDay[];
  createdBy: number | null;
  updatedBy: number | null;
  updatedAt: string | null;
  version: number;
  isInherited: boolean;
}

export interface UpdateStandardWorkWeekPayload {
  scopeType?: string;
  orgUnitId?: number | null;
  capacityUnit?: CapacityUnit;
  weekStartDay?: WeekStartDay;
  standardHoursPerDay?: number;
  days: StandardWorkWeekDay[];
  version?: number;
}

export interface CapacityConversionPayload {
  value: number;
  fromUnit: CapacityUnit;
  toUnit: CapacityUnit;
  scopeType?: string;
  orgUnitId?: number | null;
}

export interface CapacityConversionResult {
  originalValue: number;
  fromUnit: string;
  convertedValue: number;
  toUnit: string;
  formulaDescription: string;
}

export async function getStandardWorkWeekConfig(
  scopeType: string = "COMPANY",
  orgUnitId?: number | null
): Promise<StandardWorkWeekConfig> {
  const params = new URLSearchParams();
  params.set("scopeType", scopeType);
  if (orgUnitId) {
    params.set("orgUnitId", String(orgUnitId));
  }
  return await apiRequest<StandardWorkWeekConfig>(`/work-week-configs?${params.toString()}`, {
    method: "GET",
  });
}

export async function updateStandardWorkWeekConfig(
  payload: UpdateStandardWorkWeekPayload
): Promise<StandardWorkWeekConfig> {
  return await apiRequest<StandardWorkWeekConfig>("/work-week-configs", {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function convertCapacity(
  payload: CapacityConversionPayload
): Promise<CapacityConversionResult> {
  return await apiRequest<CapacityConversionResult>("/work-week-configs/convert", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

