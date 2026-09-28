import test from "node:test";
import assert from "node:assert/strict";

function getIsoWeekPartsFromYMD(year, month, dayOfMonth) {
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

function getIsoWeeksInYear(year) {
  return getIsoWeekPartsFromYMD(year, 11, 28).week;
}

function addIsoWeeks(year, week, weeksToAdd) {
  const jan4 = new Date(Date.UTC(year, 0, 4));
  const dayOfWeek = jan4.getUTCDay() || 7;
  const targetDate = new Date(Date.UTC(year, 0, 4 - dayOfWeek + 1 + (week - 1 + weeksToAdd) * 7));
  return getIsoWeekPartsFromYMD(
    targetDate.getUTCFullYear(),
    targetDate.getUTCMonth(),
    targetDate.getUTCDate()
  );
}

function getIsoWeekDetails(date) {
  return getIsoWeekPartsFromYMD(date.getFullYear(), date.getMonth(), date.getDate());
}

test("Timesheet Variance ISO-8601 Week & Year Calculations", async (t) => {
  await t.test("TC-01: Year transition edge case - Jan 1st 2027 belongs to 2026-W53", () => {
    const jan1_2027 = new Date(2027, 0, 1);
    const details = getIsoWeekDetails(jan1_2027);
    assert.deepEqual(details, { year: 2026, week: 53 });
  });

  await t.test("TC-02: Year transition edge case - Dec 31st 2024 belongs to 2025-W1", () => {
    const dec31_2024 = new Date(2024, 11, 31);
    const details = getIsoWeekDetails(dec31_2024);
    assert.deepEqual(details, { year: 2025, week: 1 });
  });

  await t.test("TC-03: Default -4 weeks across year boundaries (from 2027-W2 back 4 weeks)", () => {
    const res = addIsoWeeks(2027, 2, -4);
    assert.deepEqual(res, { year: 2026, week: 51 });
  });

  await t.test("TC-04: Max weeks in year (2025: 52, 2026: 53, 2027: 52)", () => {
    assert.equal(getIsoWeeksInYear(2025), 52);
    assert.equal(getIsoWeeksInYear(2026), 53);
    assert.equal(getIsoWeeksInYear(2027), 52);
  });

  await t.test("TC-05: Local timezone edge case - Monday 00:30 local time is not shifted back to Sunday", () => {
    // E.g. Monday morning 00:30 on 2026-09-21 (Week 39)
    const earlyMonday = new Date(2026, 8, 21, 0, 30, 0);
    const details = getIsoWeekDetails(earlyMonday);
    assert.deepEqual(details, { year: 2026, week: 39 });
  });
});
