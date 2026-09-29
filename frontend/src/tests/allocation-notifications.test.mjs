import test from "node:test";
import assert from "node:assert/strict";

import {
  checkCanAccessAllocationNotifications,
  buildNotificationQuery,
  formatConsecutiveWeekRange,
} from "../lib/api/allocations.ts";

test("RBAC: VT-02 (PM) và VT-03 (RM) được phép truy cập màn hình thông báo phân bổ", () => {
  assert.equal(checkCanAccessAllocationNotifications("VT-02"), true);
  assert.equal(checkCanAccessAllocationNotifications("ROLE_VT_02"), true);
  assert.equal(checkCanAccessAllocationNotifications("vt_02"), true);

  assert.equal(checkCanAccessAllocationNotifications("VT-03"), true);
  assert.equal(checkCanAccessAllocationNotifications("ROLE_VT_03"), true);
  assert.equal(checkCanAccessAllocationNotifications("vt_03"), true);
});

test("RBAC: Các vai trò khác (VT-01, VT-04, VT-05, VT-06) bị chặn truy cập màn hình thông báo ", () => {
  assert.equal(checkCanAccessAllocationNotifications("VT-01"), false);
  assert.equal(checkCanAccessAllocationNotifications("VT-04"), false);
  assert.equal(checkCanAccessAllocationNotifications("VT-05"), false);
  assert.equal(checkCanAccessAllocationNotifications("VT-06"), false);
  assert.equal(checkCanAccessAllocationNotifications(null), false);
  assert.equal(checkCanAccessAllocationNotifications(undefined), false);
  assert.equal(checkCanAccessAllocationNotifications(""), false);
});

test("API Query: Tạo URL truy vấn phân trang và lọc dự án chính xác", () => {
  assert.equal(buildNotificationQuery(), "/allocations/notifications");
  assert.equal(
    buildNotificationQuery({ page: 0, size: 10 }),
    "/allocations/notifications?page=0&size=10"
  );
  assert.equal(
    buildNotificationQuery({ projectId: 101, page: 1, size: 20 }),
    "/allocations/notifications?projectId=101&page=1&size=20"
  );
});

test("Hiển thị chuỗi tuần liên tiếp: Đơn tuần và đa tuần", () => {
  assert.equal(formatConsecutiveWeekRange(2026, 38, 2026, 38), "tuần 38/2026");
  assert.equal(
    formatConsecutiveWeekRange(2026, 38, 2026, 42),
    "từ tuần 38/2026 đến tuần 42/2026"
  );
  assert.equal(
    formatConsecutiveWeekRange(2026, 50, 2027, 4),
    "từ tuần 50/2026 đến tuần 4/2027"
  );
});
