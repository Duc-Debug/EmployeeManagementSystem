import test from "node:test";
import assert from "node:assert/strict";

import {
  getMaxIsoWeeks,
  addIsoWeeksPreset as addIsoWeeks,
} from "../lib/iso-week.ts";

test("ISO-8601 Week Calculations & 53-Week Years (NCL-06-CN-006 Bugfix)", async (t) => {
  await t.test("TC-01: Correct max weeks in year (2025 has 52 weeks, 2026 has 53 weeks)", () => {
    assert.equal(getMaxIsoWeeks(2025), 52);
    assert.equal(getMaxIsoWeeks(2026), 53);
    assert.equal(getMaxIsoWeeks(2020), 53);
    assert.equal(getMaxIsoWeeks(2024), 52);
  });

  await t.test("TC-02: 12-week preset starting at 2026-W52 traverses through W53 to 2027-W10 (Total 12 weeks)", () => {
    const res = addIsoWeeks(2026, 52, 12);
    // 2026-W52 (1), 2026-W53 (2), 2027-W1 (3), 2027-W2 (4) ... 2027-W10 (12)
    assert.deepEqual(res, { year: 2027, week: 10 });
  });

  await t.test("TC-03: 4-week preset in regular 52-week year (2025-W50)", () => {
    const res = addIsoWeeks(2025, 50, 4);
    // 2025-W50 (1), 2025-W51 (2), 2025-W52 (3), 2026-W1 (4)
    assert.deepEqual(res, { year: 2026, week: 1 });
  });

  await t.test("TC-04: Single week (count = 1)", () => {
    const res = addIsoWeeks(2026, 15, 1);
    assert.deepEqual(res, { year: 2026, week: 15 });
  });
});
