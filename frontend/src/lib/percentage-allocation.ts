/**
 * Pure functions for percentage-based resource allocations (NCL-06-CN-007)
 */

export function convertPercentageToHours(percentage: number, availableHours: number): number {
  const validPct = Math.max(0, Math.min(100, percentage));
  const validAvail = Math.max(0, availableHours);
  return Number(((validAvail * validPct) / 100).toFixed(2));
}

export function convertHoursToPercentage(hours: number, availableHours: number): number {
  if (!availableHours || availableHours <= 0) return 0;
  return Math.round((Math.max(0, hours) / availableHours) * 100);
}

export interface BulkPayloadParams {
  employeeId: string | number;
  projectId: string | number;
  startYear: number;
  startWeek: number;
  endYear: number;
  endWeek: number;
  allocationMode: "percentage" | "hours";
  allocatedHours?: number;
  allocationPercentage?: number;
  description?: string;
}

export function buildBulkPayload({
  employeeId,
  projectId,
  startYear,
  startWeek,
  endYear,
  endWeek,
  allocationMode,
  allocatedHours,
  allocationPercentage,
  description,
}: BulkPayloadParams) {
  const payload: {
    employeeId: string | number;
    projectId: string | number;
    startYear: number;
    startWeek: number;
    endYear: number;
    endWeek: number;
    description?: string;
    allocationPercentagePerWeek?: number;
    allocatedHoursPerWeek?: number;
  } = {
    employeeId,
    projectId,
    startYear,
    startWeek,
    endYear,
    endWeek,
    description: description || undefined,
  };

  if (allocationMode === "percentage") {
    payload.allocationPercentagePerWeek = allocationPercentage;
  } else {
    payload.allocatedHoursPerWeek = allocatedHours;
  }

  return payload;
}

export interface WeekAvailabilityItem {
  year: number;
  week: number;
  netAvailableHours: number;
}

export function computeProjectedBulkAllocation(weeks: WeekAvailabilityItem[], percentage: number) {
  return weeks.map((w) => {
    const netAvailable = w.netAvailableHours;
    const hours = Number(((netAvailable * percentage) / 100).toFixed(2));
    return {
      year: w.year,
      week: w.week,
      netAvailableHours: netAvailable,
      projectedHours: hours,
      percentage,
    };
  });
}
