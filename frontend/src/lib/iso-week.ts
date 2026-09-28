export function getIsoWeekPartsFromYMD(year: number, month: number, dayOfMonth: number): { year: number; week: number } {
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

export const getMaxIsoWeeks = (year: number): number => getIsoWeeksInYear(year);

export function getIsoWeeksInYear(year: number): number {
  return getIsoWeekPartsFromYMD(year, 11, 28).week;
}

export function addIsoWeeks(year: number, week: number, weeksToAdd: number): { year: number; week: number } {
  const jan4 = new Date(Date.UTC(year, 0, 4));
  const dayOfWeek = jan4.getUTCDay() || 7;
  const targetDate = new Date(Date.UTC(year, 0, 4 - dayOfWeek + 1 + (week - 1 + weeksToAdd) * 7));
  return getIsoWeekPartsFromYMD(
    targetDate.getUTCFullYear(),
    targetDate.getUTCMonth(),
    targetDate.getUTCDate()
  );
}

export function addIsoWeeksPreset(startYear: number, startWeek: number, count: number): { year: number; week: number } {
  if (count <= 1) {
    return { year: startYear, week: startWeek };
  }
  return addIsoWeeks(startYear, startWeek, count - 1);
}

export function getIsoWeekDetails(date: Date): { year: number; week: number } {
  // Use local calendar date fields (getFullYear, getMonth, getDate) so that timezone offsets
  // (e.g. UTC+7 early Monday morning) reflect the user's actual local calendar day.
  return getIsoWeekPartsFromYMD(date.getFullYear(), date.getMonth(), date.getDate());
}

/**
 * Tính ngày bắt đầu (Thứ Hai) và ngày kết thúc (Chủ Nhật) của một tuần ISO.
 */
export function getIsoWeekDateRange(isoYear: number, weekNumber: number): { startDate: Date; endDate: Date } {
  const jan4 = new Date(Date.UTC(isoYear, 0, 4));
  const dayOfWeek = jan4.getUTCDay() || 7;
  const mondayWeek1Time = jan4.getTime() - (dayOfWeek - 1) * 86400000;
  const startMs = mondayWeek1Time + (weekNumber - 1) * 7 * 86400000;
  const endMs = startMs + 6 * 86400000;
  return {
    startDate: new Date(startMs),
    endDate: new Date(endMs),
  };
}

/**
 * Format khoảng ngày Thứ Hai - Chủ Nhật của tuần ISO (ví dụ: "31/08 – 06/09" hoặc "31/08/2026 – 06/09/2026").
 */
export function formatIsoWeekDateRange(startDate: Date, endDate: Date, includeYear = false): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  const startD = pad(startDate.getUTCDate());
  const startM = pad(startDate.getUTCMonth() + 1);
  const endD = pad(endDate.getUTCDate());
  const endM = pad(endDate.getUTCMonth() + 1);
  if (includeYear) {
    const startY = startDate.getUTCFullYear();
    const endY = endDate.getUTCFullYear();
    return `${startD}/${startM}/${startY} – ${endD}/${endM}/${endY}`;
  }
  return `${startD}/${startM} – ${endD}/${endM}`;
}

export interface MonthIsoWeek {
  year: number;
  week: number;
  startDate: Date;
  endDate: Date;
}

/**
 * Xác định danh sách các tuần ISO thuộc về một tháng theo chuẩn ISO-8601:
 * Một tuần ISO thuộc tháng chứa ngày Thứ Năm của tuần đó.
 * calendarMonth: 1-indexed (1 = Tháng 1, 12 = Tháng 12)
 */
export function getIsoWeeksForMonth(calendarYear: number, calendarMonth: number): MonthIsoWeek[] {
  const lastDay = new Date(Date.UTC(calendarYear, calendarMonth, 0)).getUTCDate();
  const weeks: MonthIsoWeek[] = [];

  for (let day = 1; day <= lastDay; day++) {
    const d = new Date(Date.UTC(calendarYear, calendarMonth - 1, day));
    if (d.getUTCDay() === 4) { // Thursday
      const { year: isoYear, week: isoWeek } = getIsoWeekPartsFromYMD(calendarYear, calendarMonth - 1, day);
      const range = getIsoWeekDateRange(isoYear, isoWeek);
      weeks.push({
        year: isoYear,
        week: isoWeek,
        startDate: range.startDate,
        endDate: range.endDate,
      });
    }
  }

  return weeks;
}


