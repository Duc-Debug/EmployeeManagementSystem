import test from "node:test";
import assert from "node:assert/strict";
import {
  formatBadge,
  getLevelStyle,
  buildNotificationQueryParams as buildQueryParams,
  resolveDeepLink,
  deleteNotificationFromList as deleteItem,
} from "../lib/api/notifications.ts";

test("NCL-11-CN-001: Trung tâm thông báo Frontend Logic & Formatting Tests", async (t) => {
  await t.test("TC-01: Định dạng số lượng badge chưa đọc (unreadCount)", () => {
    assert.equal(formatBadge(0), null);
    assert.equal(formatBadge(5), "5");
    assert.equal(formatBadge(99), "99");
    assert.equal(formatBadge(100), "99+");
    assert.equal(formatBadge(999), "99+");
  });

  await t.test("TC-02: Phân loại 3 mức độ (CAO = Đỏ, TRUNG_BINH = Vàng, THAP = Xanh)", () => {
    assert.deepEqual(getLevelStyle("CAO"), { color: "rose", label: "Cao" });
    assert.deepEqual(getLevelStyle("TRUNG_BINH"), { color: "amber", label: "Trung bình" });
    assert.deepEqual(getLevelStyle("THAP"), { color: "sky", label: "Thấp" });
    assert.deepEqual(getLevelStyle("UNKNOWN"), { color: "sky", label: "Thấp" });
  });

  await t.test("TC-03: Xây dựng Query Parameters URL cho API Trung tâm thông báo", () => {
    assert.equal(buildQueryParams({ status: "ALL", level: "ALL", page: 0, size: 20 }), "page=0&size=20");
    assert.equal(buildQueryParams({ status: "UNREAD", level: "CAO", page: 1, size: 10 }), "status=UNREAD&level=CAO&page=1&size=10");
    assert.equal(buildQueryParams({ status: "READ", level: "TRUNG_BINH" }), "status=READ&level=TRUNG_BINH");
  });

  await t.test("TC-04: Ánh xạ Deep Link URL từ relatedEntityType và relatedEntityId", () => {
    assert.equal(resolveDeepLink("CAPACITY_WEEK", "2026-W38"), "/capacity?week=2026-W38");
    assert.equal(resolveDeepLink("PROJECT", "42"), "/projects/42");
    assert.equal(resolveDeepLink("PROJECT_ALLOCATION", "105"), "/projects/105");
    assert.equal(resolveDeepLink("LEAVE_REQUEST", "99"), "/leave?requestId=99");
    assert.equal(resolveDeepLink(null, null), null);
  });

  await t.test("TC-05: Cập nhật state lạc quan khi xóa mềm (Soft-delete)", () => {
    const initialItems = [
      { id: 1, title: "Item 1", isRead: false },
      { id: 2, title: "Item 2", isRead: true },
      { id: 3, title: "Item 3", isRead: false },
    ];

    const updated = deleteItem(initialItems, 2);
    assert.equal(updated.length, 2);
    assert.equal(updated.find((n) => n.id === 2), undefined);

    const updatedAgain = deleteItem(updated, 1);
    assert.equal(updatedAgain.length, 1);
    assert.equal(updatedAgain[0].id, 3);
  });
});
