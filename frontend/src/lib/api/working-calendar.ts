"use client";

import { apiRequest } from "../api-client";

export type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

export interface WorkingCalendarDay {
  dayOfWeek: DayOfWeek;
  isWorkingDay: boolean;
}

export interface CompanyWorkingCalendar {
  days: WorkingCalendarDay[];
}

export interface Holiday {
  id: number;
  holidayDate: string; // YYYY-MM-DD
  name: string;
  workingHoursDeducted: number;
}

export interface CreateHolidayPayload {
  holidayDate: string;
  name: string;
  workingHoursDeducted: number;
}

export interface UpdateHolidayPayload {
  holidayDate: string;
  name: string;
  workingHoursDeducted: number;
}

export async function getWorkingCalendar(): Promise<CompanyWorkingCalendar> {
  return await apiRequest<CompanyWorkingCalendar>("/working-calendar", {
    method: "GET",
  });
}

export async function updateWorkingCalendar(
  days: WorkingCalendarDay[]
): Promise<CompanyWorkingCalendar> {
  return await apiRequest<CompanyWorkingCalendar>("/working-calendar", {
    method: "PUT",
    body: JSON.stringify({ days }),
  });
}

export async function getHolidays(year?: number): Promise<Holiday[]> {
  const query = year ? `?year=${year}` : "";
  return await apiRequest<Holiday[]>(`/holidays${query}`, {
    method: "GET",
  });
}

export async function createHoliday(payload: CreateHolidayPayload): Promise<Holiday> {
  return await apiRequest<Holiday>("/holidays", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateHoliday(
  id: number,
  payload: UpdateHolidayPayload
): Promise<Holiday> {
  return await apiRequest<Holiday>(`/holidays/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function deleteHoliday(id: number): Promise<void> {
  await apiRequest<void>(`/holidays/${id}`, {
    method: "DELETE",
  });
}
