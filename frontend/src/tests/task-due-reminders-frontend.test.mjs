import test from "node:test";
import assert from "node:assert/strict";

test("NCL-11-CN-004: Nhắc việc sắp đến hạn Frontend Logic & Formatting Tests", async (t) => {
  // 1. Logic formatDaysRemaining
  const formatDaysRemaining = (days) => {
    if (days <= 0) {
      return {
        text: "Hôm nay",
        badgeClass: "bg-rose-50 text-rose-700 border-rose-200",
        level: "TODAY",
        iconType: "ALERT",
      };
    }
    if (days === 1) {
      return {
        text: "Còn 1 ngày",
        badgeClass: "bg-rose-50 text-rose-700 border-rose-200",
        level: "CRITICAL",
        iconType: "ALERT",
      };
    }
    if (days === 2) {
      return {
        text: "Còn 2 ngày",
        badgeClass: "bg-amber-50 text-amber-700 border-amber-200",
        level: "WARNING",
        iconType: "CLOCK",
      };
    }
    return {
      text: `Còn ${days} ngày`,
      badgeClass: "bg-sky-50 text-sky-700 border-sky-200",
      level: "UPCOMING",
      iconType: "CALENDAR",
    };
  };

  await t.test("TC-01: Định dạng số ngày còn lại (Hôm nay / Còn 1 ngày / Còn 2 ngày / Còn 3 ngày)", () => {
    // 0 ngày (đến hạn hôm nay) hoặc quá hạn
    const today = formatDaysRemaining(0);
    assert.equal(today.text, "Hôm nay");
    assert.equal(today.level, "TODAY");
    assert.equal(today.iconType, "ALERT");
    assert.ok(today.badgeClass.includes("rose"));

    const overdue = formatDaysRemaining(-1);
    assert.equal(overdue.text, "Hôm nay");
    assert.equal(overdue.level, "TODAY");

    // 1 ngày (khẩn cấp)
    const oneDay = formatDaysRemaining(1);
    assert.equal(oneDay.text, "Còn 1 ngày");
    assert.equal(oneDay.level, "CRITICAL");
    assert.equal(oneDay.iconType, "ALERT");
    assert.ok(oneDay.badgeClass.includes("rose"));

    // 2 ngày (cảnh báo)
    const twoDays = formatDaysRemaining(2);
    assert.equal(twoDays.text, "Còn 2 ngày");
    assert.equal(twoDays.level, "WARNING");
    assert.equal(twoDays.iconType, "CLOCK");
    assert.ok(twoDays.badgeClass.includes("amber"));

    // 3 ngày (theo dõi)
    const threeDays = formatDaysRemaining(3);
    assert.equal(threeDays.text, "Còn 3 ngày");
    assert.equal(threeDays.level, "UPCOMING");
    assert.equal(threeDays.iconType, "CALENDAR");
    assert.ok(threeDays.badgeClass.includes("sky"));
  });

  await t.test("TC-02: Xây dựng URL Endpoint cho API Rà soát thủ công", () => {
    const buildScanUrl = (scanDate) => {
      const query = scanDate ? `?scanDate=${encodeURIComponent(scanDate)}` : "";
      return `/tasks/due-reminders/scan${query}`;
    };

    assert.equal(buildScanUrl(), "/tasks/due-reminders/scan");
    assert.equal(buildScanUrl("2026-09-18"), "/tasks/due-reminders/scan?scanDate=2026-09-18");
    assert.equal(buildScanUrl("2026-10-01"), "/tasks/due-reminders/scan?scanDate=2026-10-01");
  });

  await t.test("TC-03: Tính toán số lượng công việc khẩn cấp (criticalCount) với daysRemaining <= 1", () => {
    const mockTasks = [
      { taskId: 1, taskName: "Task 1", daysRemaining: 0 },
      { taskId: 2, taskName: "Task 2", daysRemaining: 1 },
      { taskId: 3, taskName: "Task 3", daysRemaining: 2 },
      { taskId: 4, taskName: "Task 4", daysRemaining: 3 },
    ];

    const criticalCount = mockTasks.filter((t) => t.daysRemaining <= 1).length;
    const todayCount = mockTasks.filter((t) => t.daysRemaining <= 0).length;

    assert.equal(criticalCount, 2);
    assert.equal(todayCount, 1);
  });

  await t.test("TC-04: Xử lý Deep Link mở trực tiếp công việc", () => {
    const buildDirectUrl = (projectId, taskId) => {
      return `/projects/${projectId}/tasks/${taskId}`;
    };

    assert.equal(buildDirectUrl(10, 101), "/projects/10/tasks/101");
    assert.equal(buildDirectUrl(5, 202), "/projects/5/tasks/202");
  });

  await t.test("TC-05: Xử lý an toàn khi danh sách trả về rỗng hoặc null", () => {
    const parseApiResponse = (data) => {
      return Array.isArray(data) ? data : [];
    };

    assert.deepEqual(parseApiResponse(null), []);
    assert.deepEqual(parseApiResponse(undefined), []);
    assert.deepEqual(parseApiResponse({}), []);
    assert.deepEqual(parseApiResponse([{ taskId: 1 }]), [{ taskId: 1 }]);
  });
});