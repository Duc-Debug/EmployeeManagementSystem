import type { WeeklyAvailabilityResult } from "@/lib/api/availability";
import type { EmployeeProfile } from "@/lib/api/employees";

export interface EmployeeWeeklyCapacityViewItem {
  employee: EmployeeProfile;
  capacity?: WeeklyAvailabilityResult;
  isLoading?: boolean;
  error?: string;
}

export interface WeekOption {
  weekNumber: number;
  year: number;
  label: string;
  startDate: string;
  endDate: string;
}

/**
 * Tính ngày bắt đầu (Thứ Hai) và ngày kết thúc (Chủ Nhật) của một tuần ISO.
 */
export function getIsoWeekDateRange(year: number, weekNumber: number): { startDate: Date; endDate: Date } {
  // 4th January is always in week 1 of year in ISO-8601
  const simple = new Date(Date.UTC(year, 0, 4));
  const dayOfWeek = simple.getUTCDay() || 7; // 1 = Monday, 7 = Sunday
  const mondayWeek1 = new Date(simple);
  mondayWeek1.setUTCDate(simple.getUTCDate() - dayOfWeek + 1);

  const targetMonday = new Date(mondayWeek1);
  targetMonday.setUTCDate(mondayWeek1.getUTCDate() + (weekNumber - 1) * 7);

  const targetSunday = new Date(targetMonday);
  targetSunday.setUTCDate(targetMonday.getUTCDate() + 6);

  return { startDate: targetMonday, endDate: targetSunday };
}

/**
 * Lấy số tuần ISO hiện tại của một ngày.
 */
export function getCurrentIsoWeek(d: Date = new Date()): { year: number; weekNumber: number } {
  const date = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));
  const dayNum = date.getUTCDay() || 7;
  date.setUTCDate(date.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(date.getUTCFullYear(), 0, 1));
  const weekNo = Math.ceil(((date.getTime() - yearStart.getTime()) / 86400000 + 1) / 7);
  return { year: date.getUTCFullYear(), weekNumber: weekNo };
}
