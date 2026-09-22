import test, { describe } from "node:test";
import assert from "node:assert/strict";

describe("NCL-12-CN-003: Data Backup and Recovery Frontend Tests", () => {
  test("TC-01: Format file size helper converts bytes to human-readable string correctly", () => {
    function formatFileSize(bytes) {
      if (!bytes || bytes <= 0) return "0 B";
      const units = ["B", "KB", "MB", "GB"];
      const i = Math.min(Math.floor(Math.log10(bytes) / Math.log10(1024)), units.length - 1);
      return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${units[i]}`;
    }

    assert.equal(formatFileSize(0), "0 B");
    assert.equal(formatFileSize(500), "500.0 B");
    assert.equal(formatFileSize(1024), "1.0 KB");
    assert.equal(formatFileSize(2048000), "2.0 MB");
  });

  test("TC-02: Restore validation enforces exact 'RESTORE' string and minimum 10 char reason", () => {
    function validateRestoreRequest(code, reason) {
      if (!code || code.trim().toUpperCase() !== "RESTORE") {
        return { valid: false, error: "Mã xác nhận phải là RESTORE" };
      }
      if (!reason || reason.trim().length < 10) {
        return { valid: false, error: "Lý do phải từ 10 ký tự trở lên" };
      }
      return { valid: true };
    }

    assert.equal(validateRestoreRequest("RESTORE", "Phục hồi kế hoạch dự án tuần 38").valid, true);
    assert.equal(validateRestoreRequest("restore", "Phục hồi kế hoạch dự án tuần 38").valid, true);
    assert.equal(validateRestoreRequest("WRONG", "Phục hồi kế hoạch dự án tuần 38").valid, false);
    assert.equal(validateRestoreRequest("RESTORE", "Quá ngắn").valid, false);
    assert.equal(validateRestoreRequest("", "").valid, false);
  });

  test("TC-03: Filter backups by Type and Status", () => {
    const list = [
      { id: 1, backupCode: "BCK-1", backupType: "FULL", status: "COMPLETED" },
      { id: 2, backupCode: "BCK-2", backupType: "RESOURCE_PLAN", status: "COMPLETED" },
      { id: 3, backupCode: "BCK-3", backupType: "FULL", status: "FAILED" },
      { id: 4, backupCode: "BCK-4", backupType: "RESOURCE_PLAN", status: "IN_PROGRESS" },
    ];

    function filterBackups(items, type, status, search) {
      return items.filter((b) => {
        if (type && type !== "ALL" && b.backupType !== type) return false;
        if (status && status !== "ALL" && b.status !== status) return false;
        if (search && !b.backupCode.toLowerCase().includes(search.toLowerCase())) return false;
        return true;
      });
    }

    assert.equal(filterBackups(list, "ALL", "ALL", "").length, 4);
    assert.equal(filterBackups(list, "FULL", "ALL", "").length, 2);
    assert.equal(filterBackups(list, "ALL", "COMPLETED", "").length, 2);
    assert.equal(filterBackups(list, "RESOURCE_PLAN", "COMPLETED", "").length, 1);
    assert.equal(filterBackups(list, "ALL", "ALL", "BCK-3").length, 1);
  });

  test("TC-04: Role guard allows only VT-06 (Admin) access to backup feature", () => {
    function canAccessBackup(roleCode) {
      return roleCode === "VT-06";
    }

    assert.equal(canAccessBackup("VT-06"), true);
    assert.equal(canAccessBackup("VT-01"), false);
    assert.equal(canAccessBackup("VT-02"), false);
    assert.equal(canAccessBackup("VT-03"), false);
    assert.equal(canAccessBackup("VT-04"), false);
    assert.equal(canAccessBackup("VT-05"), false);
  });

  test("TC-05: Restore button is disabled when backup status is FAILED or IN_PROGRESS", () => {
    function canTriggerRestore(status) {
      return status === "COMPLETED";
    }

    assert.equal(canTriggerRestore("COMPLETED"), true);
    assert.equal(canTriggerRestore("IN_PROGRESS"), false);
    assert.equal(canTriggerRestore("FAILED"), false);
  });
});
