import test from "node:test";
import assert from "node:assert/strict";

import {
  getIsoWeekPartsFromYMD,
  getIsoWeeksInYear,
  addIsoWeeks,
  getIsoWeekDetails,
} from "../lib/iso-week.ts";

test("Timesheet Variance ISO-8601 Week & Year Calculations", async (t) => {
  await t.test("Year transition edge case - Jan 1st 2027 belongs to 2026-W53", () => {
    const jan1_2027 = new Date(2027, 0, 1);
    const details = getIsoWeekDetails(jan1_2027);
    assert.deepEqual(details, { year: 2026, week: 53 });
  });

  await t.test("Year transition edge case - Dec 31st 2024 belongs to 2025-W1", () => {
    const dec31_2024 = new Date(2024, 11, 31);
    const details = getIsoWeekDetails(dec31_2024);
    assert.deepEqual(details, { year: 2025, week: 1 });
  });

  await t.test("Default -4 weeks across year boundaries (from 2027-W2 back 4 weeks)", () => {
    const res = addIsoWeeks(2027, 2, -4);
    assert.deepEqual(res, { year: 2026, week: 51 });
  });

  await t.test("Max weeks in year (2025: 52, 2026: 53, 2027: 52)", () => {
    assert.equal(getIsoWeeksInYear(2025), 52);
    assert.equal(getIsoWeeksInYear(2026), 53);
    assert.equal(getIsoWeeksInYear(2027), 52);
  });

  await t.test("Local timezone edge case - Monday 00:30 local time is not shifted back to Sunday", () => {
    // E.g. Monday morning 00:30 on 2026-09-21 (Week 39)
    const earlyMonday = new Date(2026, 8, 21, 0, 30, 0);
    const details = getIsoWeekDetails(earlyMonday);
    assert.deepEqual(details, { year: 2026, week: 39 });
  });
});
