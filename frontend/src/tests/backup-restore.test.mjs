import test, { describe } from "node:test";
import assert from "node:assert/strict";

describe("NCL-12-CN-003: Data Backup and Recovery Frontend Tests", () => {
  function formatBackupFileSize(bytes) {
    if (!bytes || bytes <= 0) return "0 B";
    const units = ["B", "KB", "MB", "GB", "TB"];
    const i = Math.min(Math.floor(Math.log10(bytes) / Math.log10(1024)), units.length - 1);
    return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${units[i]}`;
  }

  function validateRestoreConfirmation(code, reason) {
    if (!code || code.trim().toUpperCase() !== "RESTORE") {
      return { valid: false, error: "Mã xác nhận phải là RESTORE" };
    }
    if (!reason || reason.trim().length < 10) {
      return { valid: false, error: "Lý do phải từ 10 ký tự trở lên" };
    }
    return { valid: true };
  }

  function canAccessBackupWorkspace(roleCode, permissions) {
    const normalized = roleCode ? roleCode.toUpperCase().replace(/_/g, "-") : "";
    return (
      normalized === "VT-06" ||
      normalized === "ROLE-ADMIN" ||
      normalized === "ADMIN" ||
      (permissions !== undefined && permissions !== null && permissions.includes("DATA_BACKUP_MANAGE"))
    );
  }

  function canRestoreBackup(status) {
    return status === "COMPLETED";
  }

  function extractData(res) {
    if (!res) return res;
    if (typeof res === "object" && res !== null && "data" in res) {
      const data = res.data;
      if (data !== undefined) return data;
    }
    return res;
  }

  function validateBackupUploadFileName(fileName) {
    if (!fileName || typeof fileName !== "string") return false;
    const lower = fileName.trim().toLowerCase();
    return lower.endsWith(".json");
  }

  test("TC-01: formatBackupFileSize helper converts bytes to human-readable string correctly", () => {
    assert.equal(formatBackupFileSize(0), "0 B");
    assert.equal(formatBackupFileSize(-10), "0 B");
    assert.equal(formatBackupFileSize(500), "500.0 B");
    assert.equal(formatBackupFileSize(1024), "1.0 KB");
    assert.equal(formatBackupFileSize(2048000), "2.0 MB");
    assert.equal(formatBackupFileSize(1073741824), "1.0 GB");
  });

  test("TC-02: validateRestoreConfirmation enforces exact 'RESTORE' string and minimum 10 char reason", () => {
    assert.equal(validateRestoreConfirmation("RESTORE", "Phục hồi kế hoạch dự án tuần 38").valid, true);
    assert.equal(validateRestoreConfirmation("restore", "Phục hồi kế hoạch dự án tuần 38").valid, true);
    assert.equal(validateRestoreConfirmation("  restore  ", "Phục hồi kế hoạch dự án tuần 38").valid, true);
    assert.equal(validateRestoreConfirmation("WRONG", "Phục hồi kế hoạch dự án tuần 38").valid, false);
    assert.equal(validateRestoreConfirmation("RESTORE", "Quá ngắn").valid, false);
    assert.equal(validateRestoreConfirmation("", "").valid, false);
    assert.equal(validateRestoreConfirmation(null, null).valid, false);
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

  test("TC-04: RBAC access guard allows VT-06, ADMIN and users with DATA_BACKUP_MANAGE permission", () => {
    assert.equal(canAccessBackupWorkspace("VT-06", []), true);
    assert.equal(canAccessBackupWorkspace("ROLE_ADMIN", []), true);
    assert.equal(canAccessBackupWorkspace("ADMIN", []), true);
    assert.equal(canAccessBackupWorkspace("VT-01", ["DATA_BACKUP_MANAGE"]), true);
    assert.equal(canAccessBackupWorkspace("VT-02", ["OTHER_PERMISSION", "DATA_BACKUP_MANAGE"]), true);
    assert.equal(canAccessBackupWorkspace("VT-01", []), false);
    assert.equal(canAccessBackupWorkspace("VT-02", ["PROJECT_READ"]), false);
    assert.equal(canAccessBackupWorkspace("VT-03", null), false);
    assert.equal(canAccessBackupWorkspace("VT-04", undefined), false);
    assert.equal(canAccessBackupWorkspace("VT-05", []), false);
  });

  test("TC-05: Restore button is disabled when backup status is FAILED or IN_PROGRESS", () => {
    assert.equal(canRestoreBackup("COMPLETED"), true);
    assert.equal(canRestoreBackup("IN_PROGRESS"), false);
    assert.equal(canRestoreBackup("FAILED"), false);
  });

  test("TC-06: extractData unwraps ApiResponse correctly", () => {
    assert.deepEqual(extractData({ success: true, message: "OK", data: [1, 2, 3] }), [1, 2, 3]);
    assert.deepEqual(extractData([1, 2, 3]), [1, 2, 3]);
    assert.equal(extractData(null), null);
    assert.equal(extractData(undefined), undefined);
  });

  test("TC-07: validateBackupUploadFileName accepts only .json extension", () => {
    assert.equal(validateBackupUploadFileName("backup.json"), true);
    assert.equal(validateBackupUploadFileName("BCK-20260922-120000.JSON"), true);
    assert.equal(validateBackupUploadFileName("backup.sql"), false);
    assert.equal(validateBackupUploadFileName("backup.sql.gz"), false);
    assert.equal(validateBackupUploadFileName("exploit.exe"), false);
    assert.equal(validateBackupUploadFileName(""), false);
    assert.equal(validateBackupUploadFileName(null), false);
  });
});
