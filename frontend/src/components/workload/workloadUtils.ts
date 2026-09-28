/**
 * Business & UI Utilities for Upcoming Workload Feature (NCL-13-CN-004)
 */

function getIsoWeekPartsFromYMD(year: number, month: number, dayOfMonth: number) {
    const target = new Date(Date.UTC(year, month, dayOfMonth));
    const dayOfWeek = target.getUTCDay() || 7;
    target.setUTCDate(target.getUTCDate() + 4 - dayOfWeek);
    const isoYear = target.getUTCFullYear();
    const yearStart = new Date(Date.UTC(isoYear, 0, 1));
    return {
        year: isoYear,
        week: Math.ceil(((target.getTime() - yearStart.getTime()) / 86400000 + 1) / 7),
    };
}

/**
 * Returns the number of ISO weeks in a given year (52 or 53).
 * December 28th is always in the last ISO week of the year.
 */
export function getISOWeeksInYear(year: number): number {
    return getIsoWeekPartsFromYMD(year, 11, 28).week;
}

/**
 * Calculates net available hours given standard weekly hours, holiday hours, and approved leave hours.
 */
export function calculateNetAvailableHours(
    standardHours: number,
    holidayHours = 0,
    approvedLeaveHours = 0
): number {
    return Math.max(0, standardHours - holidayHours - approvedLeaveHours);
}

/**
 * Calculates utilization percentage rounded to 1 decimal place.
 */
export function calculateUtilization(
    allocatedHours: number,
    netAvailableHours: number
): number {
    if (netAvailableHours <= 0) {
        return allocatedHours > 0 ? 100 : 0;
    }
    return Math.round((allocatedHours / netAvailableHours) * 1000) / 10;
}

/**
 * Determines workload status based on utilization and thresholds.
 */
export function getWorkloadStatus(
    utilizationRate: number,
    overloadThreshold = 100,
    idleThreshold = 70
): "OVERLOADED" | "NORMAL" | "IDLE" {
    if (utilizationRate > overloadThreshold) {
        return "OVERLOADED";
    }
    if (utilizationRate < idleThreshold) {
        return "IDLE";
    }
    return "NORMAL";
}

/**
 * Returns color classes corresponding to workload status.
 */
export function getStatusColorClass(status: string) {
    switch (status) {
        case "OVERLOADED":
            return {
                bar: "bg-red-500",
                badge: "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-400 border-red-200 dark:border-red-800",
                border: "border-red-300 dark:border-red-800",
                text: "text-red-600 dark:text-red-400"
            };
        case "IDLE":
            return {
                bar: "bg-amber-500",
                badge: "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-400 border-amber-200 dark:border-amber-800",
                border: "border-amber-300 dark:border-amber-800",
                text: "text-amber-600 dark:text-amber-400"
            };
        case "NORMAL":
        default:
            return {
                bar: "bg-emerald-500",
                badge: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800",
                border: "border-emerald-300 dark:border-emerald-800",
                text: "text-emerald-600 dark:text-emerald-400"
            };
    }
}

/**
 * Checks whether a given role code corresponds to a Specialist (VT-04).
 */
export function isSpecialistRole(role?: string | null): boolean {
    if (!role) return false;
    const normalized = String(role).toUpperCase().trim();
    return ["VT-04", "ROLE-VT-04", "SPECIALIST"].includes(normalized);
}
