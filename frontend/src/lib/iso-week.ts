function getIsoWeekPartsFromYMD(year: number, month: number, dayOfMonth: number): { year: number; week: number } {
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

export function getIsoWeekDetails(date: Date): { year: number; week: number } {
  // Use local calendar date fields (getFullYear, getMonth, getDate) so that timezone offsets
  // (e.g. UTC+7 early Monday morning) reflect the user's actual local calendar day.
  return getIsoWeekPartsFromYMD(date.getFullYear(), date.getMonth(), date.getDate());
}

