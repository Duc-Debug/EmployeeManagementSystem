import test from "node:test";
import assert from "node:assert/strict";
import {
  validateCriticalChannels,
  isValidReminderDays,
  isInQuietHours,
  createDefaultNotificationPreferenceForm as createDefaultForm,
  prepareNotificationPreferencePayload as preparePayload,
} from "../lib/api/notification-preferences.ts";

test("NCL-11-CN-002: Cấu hình kênh và tần suất nhận thông báo Logic Tests", async (t) => {
  await t.test("TC-01: Ràng buộc kênh thông báo trọng yếu (BR-03: Không được tắt cả 2 kênh)", () => {
    assert.equal(validateCriticalChannels("ALL", "ALL").valid, true);
    assert.equal(validateCriticalChannels("IN_APP_ONLY", "EMAIL_ONLY").valid, true);
    assert.equal(validateCriticalChannels("NONE", "ALL").valid, false);
    assert.equal(validateCriticalChannels("ALL", "NONE").valid, false);
    assert.equal(validateCriticalChannels("NONE", "NONE").valid, false);
  });

  await t.test("TC-02: Kiểm tra số ngày nhắc việc sắp đến hạn hợp lệ [1..14] ngày", () => {
    assert.equal(isValidReminderDays(1), true);
    assert.equal(isValidReminderDays(3), true);
    assert.equal(isValidReminderDays(7), true);
    assert.equal(isValidReminderDays(14), true);
    assert.equal(isValidReminderDays(0), false);
    assert.equal(isValidReminderDays(15), false);
    assert.equal(isValidReminderDays(-1), false);
  });

  await t.test("TC-03: Kiểm tra tính năng khung giờ yên tĩnh (Quiet Hours)", () => {
    // Khi disabled
    assert.equal(isInQuietHours(false, "22:00", "07:00", "23:00"), false);

    // Khi enabled qua đêm
    assert.equal(isInQuietHours(true, "22:00", "07:00", "23:00"), true);
    assert.equal(isInQuietHours(true, "22:00", "07:00", "05:30"), true);
    assert.equal(isInQuietHours(true, "22:00", "07:00", "07:00"), false);
    assert.equal(isInQuietHours(true, "22:00", "07:00", "14:00"), false);

    // Cùng ngày (12:00 -> 13:30)
    assert.equal(isInQuietHours(true, "12:00", "13:30", "12:30"), true);
    assert.equal(isInQuietHours(true, "12:00", "13:30", "14:00"), false);
  });

  await t.test("TC-04: Khởi tạo giá trị mặc định cho cấu hình mới (Default Preference)", () => {
    const defaultForm = createDefaultForm();
    assert.equal(defaultForm.inAppEnabled, true);
    assert.equal(defaultForm.emailEnabled, true);
    assert.equal(defaultForm.scheduleConflictChannel, "ALL");
    assert.equal(defaultForm.frequency, "IMMEDIATE");
    assert.equal(defaultForm.taskDueReminderDays, 3);
    assert.equal(defaultForm.quietHoursEnabled, false);
  });

  await t.test("TC-05: Chuẩn hóa payload gửi lên API cập nhật", () => {
    const formWithQuiet = {
      inAppEnabled: true,
      emailEnabled: false,
      quietHoursEnabled: true,
      quietHoursStart: "22:30",
      quietHoursEnd: "06:30",
    };
    const payload1 = preparePayload(formWithQuiet);
    assert.equal(payload1.quietHoursStart, "22:30:00");
    assert.equal(payload1.quietHoursEnd, "06:30:00");

    const formWithoutQuiet = {
      inAppEnabled: true,
      emailEnabled: true,
      quietHoursEnabled: false,
      quietHoursStart: "22:30",
      quietHoursEnd: "06:30",
    };
    const payload2 = preparePayload(formWithoutQuiet);
    assert.equal(payload2.quietHoursStart, null);
    assert.equal(payload2.quietHoursEnd, null);
  });
});
