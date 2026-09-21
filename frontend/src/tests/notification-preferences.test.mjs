import test from "node:test";
import assert from "node:assert/strict";

test("NCL-11-CN-002: Cấu hình kênh và tần suất nhận thông báo Logic Tests", async (t) => {
  await t.test("TC-01: Ràng buộc kênh thông báo trọng yếu (BR-03: Không được tắt cả 2 kênh)", () => {
    const validateCriticalChannels = (scheduleConflictChannel, allocationChangedChannel) => {
      if (scheduleConflictChannel === "NONE") {
        return { valid: false, error: "Cảnh báo xung đột lịch bắt buộc phải bật ít nhất 1 kênh" };
      }
      if (allocationChangedChannel === "NONE") {
        return { valid: false, error: "Cảnh báo thay đổi phân bổ bắt buộc phải bật ít nhất 1 kênh" };
      }
      return { valid: true };
    };

    assert.equal(validateCriticalChannels("ALL", "ALL").valid, true);
    assert.equal(validateCriticalChannels("IN_APP_ONLY", "EMAIL_ONLY").valid, true);
    assert.equal(validateCriticalChannels("NONE", "ALL").valid, false);
    assert.equal(validateCriticalChannels("ALL", "NONE").valid, false);
    assert.equal(validateCriticalChannels("NONE", "NONE").valid, false);
  });

  await t.test("TC-02: Kiểm tra số ngày nhắc việc sắp đến hạn hợp lệ [1..14] ngày", () => {
    const isValidReminderDays = (days) => {
      return Number.isInteger(days) && days >= 1 && days <= 14;
    };

    assert.equal(isValidReminderDays(1), true);
    assert.equal(isValidReminderDays(3), true);
    assert.equal(isValidReminderDays(7), true);
    assert.equal(isValidReminderDays(14), true);
    assert.equal(isValidReminderDays(0), false);
    assert.equal(isValidReminderDays(15), false);
    assert.equal(isValidReminderDays(-1), false);
  });

  await t.test("TC-03: Kiểm tra tính năng khung giờ yên tĩnh (Quiet Hours)", () => {
    const isInQuietHours = (enabled, startStr, endStr, targetStr) => {
      if (!enabled || !startStr || !endStr || !targetStr) return false;
      const [sh, sm] = startStr.split(":").map(Number);
      const [eh, em] = endStr.split(":").map(Number);
      const [th, tm] = targetStr.split(":").map(Number);

      const start = sh * 60 + sm;
      const end = eh * 60 + em;
      const target = th * 60 + tm;

      if (start < end) {
        return target >= start && target < end;
      } else {
        // Qua đêm (ví dụ 22:00 -> 07:00)
        return target >= start || target < end;
      }
    };

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
    const createDefaultForm = () => ({
      inAppEnabled: true,
      emailEnabled: true,
      taskAssignedChannel: "ALL",
      taskDueReminderChannel: "ALL",
      taskCommentChannel: "IN_APP_ONLY",
      timesheetReminderChannel: "ALL",
      allocationChangedChannel: "ALL",
      scheduleConflictChannel: "ALL",
      frequency: "IMMEDIATE",
      taskDueReminderDays: 3,
      quietHoursEnabled: false,
      quietHoursStart: "22:00",
      quietHoursEnd: "07:00",
    });

    const defaultForm = createDefaultForm();
    assert.equal(defaultForm.inAppEnabled, true);
    assert.equal(defaultForm.emailEnabled, true);
    assert.equal(defaultForm.scheduleConflictChannel, "ALL");
    assert.equal(defaultForm.frequency, "IMMEDIATE");
    assert.equal(defaultForm.taskDueReminderDays, 3);
    assert.equal(defaultForm.quietHoursEnabled, false);
  });

  await t.test("TC-05: Chuẩn hóa payload gửi lên API cập nhật", () => {
    const preparePayload = (form) => {
      return {
        ...form,
        quietHoursStart: form.quietHoursEnabled && form.quietHoursStart
          ? (form.quietHoursStart.length === 5 ? `${form.quietHoursStart}:00` : form.quietHoursStart)
          : null,
        quietHoursEnd: form.quietHoursEnabled && form.quietHoursEnd
          ? (form.quietHoursEnd.length === 5 ? `${form.quietHoursEnd}:00` : form.quietHoursEnd)
          : null,
      };
    };

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
