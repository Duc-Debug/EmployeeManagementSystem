import test from "node:test";
import assert from "node:assert/strict";

function getIsoWeekParts(date) {
  const utc = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  const day = utc.getUTCDay() || 7;
  utc.setUTCDate(utc.getUTCDate() + 4 - day);
  const isoYear = utc.getUTCFullYear();
  const yearStart = new Date(Date.UTC(isoYear, 0, 1));
  return { year: isoYear, week: Math.ceil(((utc.getTime() - yearStart.getTime()) / 86400000 + 1) / 7) };
}

function getIsoWeeksInYear(year) {
  const dec28 = new Date(Date.UTC(year, 11, 28));
  return getIsoWeekParts(dec28).week;
}

function addIsoWeeks(year, week, weeksToAdd) {
  const jan4 = new Date(Date.UTC(year, 0, 4));
  const day = jan4.getUTCDay() || 7;
  const monday = new Date(jan4);
  monday.setUTCDate(jan4.getUTCDate() - day + 1 + (week - 1 + weeksToAdd) * 7);
  return getIsoWeekParts(monday);
}

test("Timesheet Variance ISO-8601 Week & Year Calculations", async (t) => {
  await t.test("TC-01: Year transition edge case - Jan 1st 2027 belongs to 2026-W53", () => {
    const jan1_2027 = new Date(Date.UTC(2027, 0, 1));
    const details = getIsoWeekParts(jan1_2027);
    assert.deepEqual(details, { year: 2026, week: 53 });
  });

  await t.test("TC-02: Year transition edge case - Dec 31st 2024 belongs to 2025-W1", () => {
    const dec31_2024 = new Date(Date.UTC(2024, 11, 31));
    const details = getIsoWeekParts(dec31_2024);
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
});
