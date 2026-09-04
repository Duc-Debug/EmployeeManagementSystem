"use client";

const PHONE_STORAGE_PREFIX = "hrm_emp_phone_";

export function getStoredPhone(key?: string | number | null): string | undefined {
  if (!key || typeof window === "undefined") return undefined;
  return localStorage.getItem(`${PHONE_STORAGE_PREFIX}${key}`) || undefined;
}

export function saveStoredPhone(keys: Array<string | number | undefined | null>, phone?: string): void {
  if (typeof window === "undefined") return;
  const cleanPhone = phone?.trim();
  keys.forEach((key) => {
    if (!key) return;
    if (!cleanPhone) {
      localStorage.removeItem(`${PHONE_STORAGE_PREFIX}${key}`);
    } else {
      localStorage.setItem(`${PHONE_STORAGE_PREFIX}${key}`, cleanPhone);
    }
  });
}

export interface StoredEmployeeDates {
  joinDate?: string;
  contractEndDate?: string;
}

const DATES_STORAGE_PREFIX = "hrm_emp_dates_";

export function getStoredDates(key?: string | number | null): StoredEmployeeDates | undefined {
  if (!key || typeof window === "undefined") return undefined;
  const raw = localStorage.getItem(`${DATES_STORAGE_PREFIX}${key}`);
  if (!raw) return undefined;
  try {
    return JSON.parse(raw);
  } catch {
    return undefined;
  }
}

export function saveStoredDates(
  keys: Array<string | number | undefined | null>,
  dates: StoredEmployeeDates
): void {
  if (typeof window === "undefined") return;
  const hasValues = Boolean(dates.joinDate?.trim() || dates.contractEndDate?.trim());
  const cleanDates: StoredEmployeeDates = {
    joinDate: dates.joinDate?.trim() || undefined,
    contractEndDate: dates.contractEndDate?.trim() || undefined,
  };
  keys.forEach((key) => {
    if (!key) return;
    if (!hasValues) {
      localStorage.removeItem(`${DATES_STORAGE_PREFIX}${key}`);
    } else {
      localStorage.setItem(`${DATES_STORAGE_PREFIX}${key}`, JSON.stringify(cleanDates));
    }
  });
}

/** Formats any date string (YYYY-MM-DD or DD/MM/YYYY) to HTML5 input value (YYYY-MM-DD) */
export function formatToDateInput(val?: string): string {
  if (!val) return "";
  const trimmed = val.trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return trimmed;
  if (/^\d{2}\/\d{2}\/\d{4}$/.test(trimmed)) {
    const [d, m, y] = trimmed.split("/");
    return `${y}-${m}-${d}`;
  }
  return trimmed;
}

/** Formats any date string (YYYY-MM-DD or DD/MM/YYYY) to Vietnamese display (DD/MM/YYYY) */
export function formatDisplayDate(val?: string): string {
  if (!val) return "";
  const trimmed = val.trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
    const [y, m, d] = trimmed.split("-");
    return `${d}/${m}/${y}`;
  }
  return trimmed;
}

