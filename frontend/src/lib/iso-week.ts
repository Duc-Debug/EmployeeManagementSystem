/** ISO-8601 helpers shared by report filters and resource search windows. */
export function getIsoWeeksInYear(year: number): number {
  const dec28 = new Date(Date.UTC(year, 11, 28));
  return getIsoWeekParts(dec28).week;
}

export function addIsoWeeks(year: number, week: number, weeksToAdd: number): { year: number; week: number } {
  const jan4 = new Date(Date.UTC(year, 0, 4));
  const day = jan4.getUTCDay() || 7;
  const monday = new Date(jan4);
  monday.setUTCDate(jan4.getUTCDate() - day + 1 + (week - 1 + weeksToAdd) * 7);
  return getIsoWeekParts(monday);
}

function getIsoWeekParts(date: Date): { year: number; week: number } {
  const utc = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  const day = utc.getUTCDay() || 7;
  utc.setUTCDate(utc.getUTCDate() + 4 - day);
  const isoYear = utc.getUTCFullYear();
  const yearStart = new Date(Date.UTC(isoYear, 0, 1));
  return { year: isoYear, week: Math.ceil(((utc.getTime() - yearStart.getTime()) / 86400000 + 1) / 7) };
}
