import test from "node:test";
import assert from "node:assert/strict";

test("NCL-11-CN-001: Trung tâm thông báo Frontend Logic & Formatting Tests", async (t) => {
  await t.test("TC-01: Định dạng số lượng badge chưa đọc (unreadCount)", () => {
    const formatBadge = (count) => {
      if (count <= 0) return null;
      if (count > 99) return "99+";
      return count.toString();
    };

    assert.equal(formatBadge(0), null);
    assert.equal(formatBadge(5), "5");
    assert.equal(formatBadge(99), "99");
    assert.equal(formatBadge(100), "99+");
    assert.equal(formatBadge(999), "99+");
  });

  await t.test("TC-02: Phân loại 3 mức độ (CAO = Đỏ, TRUNG_BINH = Vàng, THAP = Xanh)", () => {
    const getLevelStyle = (level) => {
      switch (level) {
        case "CAO":
          return { color: "rose", label: "Cao" };
        case "TRUNG_BINH":
          return { color: "amber", label: "Trung bình" };
        case "THAP":
        default:
          return { color: "sky", label: "Thấp" };
      }
    };

    assert.deepEqual(getLevelStyle("CAO"), { color: "rose", label: "Cao" });
    assert.deepEqual(getLevelStyle("TRUNG_BINH"), { color: "amber", label: "Trung bình" });
    assert.deepEqual(getLevelStyle("THAP"), { color: "sky", label: "Thấp" });
    assert.deepEqual(getLevelStyle("UNKNOWN"), { color: "sky", label: "Thấp" });
  });

  await t.test("TC-03: Xây dựng Query Parameters URL cho API Trung tâm thông báo", () => {
    const buildQueryParams = (params) => {
      const query = new URLSearchParams();
      if (params.status && params.status !== "ALL") query.append("status", params.status);
      if (params.level && params.level !== "ALL") query.append("level", params.level);
      if (params.page !== undefined) query.append("page", params.page.toString());
      if (params.size !== undefined) query.append("size", params.size.toString());
      return query.toString();
    };

    assert.equal(buildQueryParams({ status: "ALL", level: "ALL", page: 0, size: 20 }), "page=0&size=20");
    assert.equal(buildQueryParams({ status: "UNREAD", level: "CAO", page: 1, size: 10 }), "status=UNREAD&level=CAO&page=1&size=10");
    assert.equal(buildQueryParams({ status: "READ", level: "TRUNG_BINH" }), "status=READ&level=TRUNG_BINH");
  });

  await t.test("TC-04: Ánh xạ Deep Link URL từ relatedEntityType và relatedEntityId", () => {
    const resolveDeepLink = (entityType, entityId) => {
      if (!entityType || !entityId) return null;
      switch (entityType.toUpperCase()) {
        case "CAPACITY_WEEK":
          return `/capacity?week=${encodeURIComponent(entityId)}`;
        case "PROJECT":
        case "PROJECT_ALLOCATION":
        case "ALLOCATION":
          return `/projects/${encodeURIComponent(entityId)}`;
        case "LEAVE_REQUEST":
          return `/leave?requestId=${encodeURIComponent(entityId)}`;
        default:
          return null;
      }
    };

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

    const deleteItem = (items, idToDelete) => items.filter((n) => n.id !== idToDelete);

    const updated = deleteItem(initialItems, 2);
    assert.equal(updated.length, 2);
    assert.equal(updated.find((n) => n.id === 2), undefined);

    const updatedAgain = deleteItem(updated, 1);
    assert.equal(updatedAgain.length, 1);
    assert.equal(updatedAgain[0].id, 3);
  });
});
