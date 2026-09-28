import test from "node:test";
import assert from "node:assert/strict";

import {
  convertPercentageToHours,
  convertHoursToPercentage,
} from "../lib/percentage-allocation.ts";

test("Percentage to Hours Conversion Tests (NCL-06-CN-007)", async (t) => {
  await t.test("TC-01: Standard 40h week with presets", () => {
    const available = 40.0;
    assert.equal(convertPercentageToHours(25, available), 10.0);
    assert.equal(convertPercentageToHours(50, available), 20.0);
    assert.equal(convertPercentageToHours(75, available), 30.0);
    assert.equal(convertPercentageToHours(100, available), 40.0);
  });

  await t.test("TC-02: Reduced availability week (e.g. 32h due to Holiday/Leave)", () => {
    const available = 32.0;
    assert.equal(convertPercentageToHours(25, available), 8.0);
    assert.equal(convertPercentageToHours(50, available), 16.0);
    assert.equal(convertPercentageToHours(75, available), 24.0);
    assert.equal(convertPercentageToHours(100, available), 32.0);
  });

  await t.test("Edge case: 0 available hours (Full week leave)", () => {
    assert.equal(convertPercentageToHours(50, 0), 0.0);
    assert.equal(convertHoursToPercentage(20, 0), 0);
  });

  await t.test("Clamped percentage boundary (0% - 100%)", () => {
    assert.equal(convertPercentageToHours(-10, 40), 0.0);
    assert.equal(convertPercentageToHours(150, 40), 40.0);
  });
});
