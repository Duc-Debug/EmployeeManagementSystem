import { test, describe } from "node:test";
import assert from "node:assert/strict";
import {
  SPECIALIST_TASK_STATUSES,
  PROGRESS_STATUS_METADATA,
  isValidSpecialistStatus,
  getNextSuggestedStatus,
  sortTasksByDeadline,
  calculateTaskStats,
  filterAssignedTasks,
  buildUpdateTaskProgressPayload,
  formatTaskProgressError,
} from "../lib/api/taskProgress.ts";

describe("NCL-04-CN-002: Task Progress Frontend Logic & Validation", () => {


  // Sample tasks for testing
  const sampleTasks = [
    {
      taskId: 101,
      taskCode: "TSK-001",
      taskName: "Thiết kế cơ sở dữ liệu",
      projectName: "Dự án Nền tảng số",
      status: "TODO",
      plannedEndDate: "2026-09-30",
      isPrimary: true,
    },
    {
      taskId: 102,
      taskCode: "TSK-002",
      taskName: "Xây dựng RESTful API",
      projectName: "Dự án Nền tảng số",
      status: "IN_PROGRESS",
      plannedEndDate: "2026-09-20",
      isPrimary: true,
    },
    {
      taskId: 103,
      taskCode: "TSK-003",
      taskName: "Viết Unit Test cho Service",
      projectName: "Dự án Nền tảng số",
      status: "IN_REVIEW",
      plannedEndDate: "2026-10-15",
      isPrimary: false,
    },
    {
      taskId: 104,
      taskCode: "TSK-004",
      taskName: "Kiểm thử bảo mật hệ thống",
      projectName: "Dự án Alpha",
      status: "DONE",
      plannedEndDate: null, // No deadline
      isPrimary: true,
    },
    {
      taskId: 105,
      taskCode: "TSK-005",
      taskName: "Nghiên cứu tài liệu cũ",
      projectName: "Dự án Alpha",
      status: "CANCELLED",
      plannedEndDate: null, // No deadline
      isPrimary: false,
    },
  ];

  test("TC-01: Specialist status whitelist validation enforces 4 valid states and rejects CANCELLED", () => {
    assert.equal(isValidSpecialistStatus("TODO"), true);
    assert.equal(isValidSpecialistStatus("IN_PROGRESS"), true);

    assert.equal(isValidSpecialistStatus("IN_REVIEW"), true);
    assert.equal(isValidSpecialistStatus("DONE"), true);

    // Cancelled is strictly forbidden for specialist updates
    assert.equal(isValidSpecialistStatus("CANCELLED"), false);
    assert.equal(isValidSpecialistStatus(""), false);
    assert.equal(isValidSpecialistStatus(null), false);
    assert.equal(isValidSpecialistStatus(undefined), false);
    assert.equal(isValidSpecialistStatus("INVALID_STATUS"), false);
  });

  test("TC-02: Status metadata mapping returns exact Vietnamese labels and descriptions", () => {
    assert.equal(PROGRESS_STATUS_METADATA.TODO.label, "Chờ thực hiện");
    assert.equal(PROGRESS_STATUS_METADATA.IN_PROGRESS.label, "Đang thực hiện");
    assert.equal(PROGRESS_STATUS_METADATA.IN_REVIEW.label, "Chờ duyệt / Nghiệm thu");
    assert.equal(PROGRESS_STATUS_METADATA.DONE.label, "Hoàn thành");
  });

  test("TC-03: Payload builder skips duplicate status (idempotency guard)", () => {
    const check = buildUpdateTaskProgressPayload("IN_PROGRESS", "IN_PROGRESS");
    assert.equal(check.shouldSend, false);
    assert.equal(check.reason, "SAME_STATUS");
  });

  test("TC-04: Payload builder rejects CANCELLED status transition attempts", () => {
    // Attempting to set CANCELLED
    assert.throws(
      () => buildUpdateTaskProgressPayload("IN_PROGRESS", "CANCELLED"),
      /không hợp lệ cho Chuyên viên/
    );

    // Attempting to update an already CANCELLED task
    assert.throws(
      () => buildUpdateTaskProgressPayload("CANCELLED", "DONE"),
      /Không thể cập nhật công việc đã bị hủy/
    );
  });

  test("TC-05: Payload builder creates correct endpoint and body for PATCH", () => {
    const payload = buildUpdateTaskProgressPayload("TODO", "IN_PROGRESS");
    assert.equal(payload.shouldSend, true);
    assert.equal(payload.method, "PATCH");
    assert.equal(payload.endpoint(101), "/tasks/101/progress");
    assert.deepEqual(payload.body, { status: "IN_PROGRESS" });
  });

  test("TC-06: Backend error parser handles 403, 400, 404, and fallback messages", () => {
    // 403: Not assigned
    const err403 = formatTaskProgressError({ status: 403 });
    assert.match(err403, /không được phân công/);

    // 400: Project closed
    const err400 = formatTaskProgressError({ status: 400, message: "Dự án đã đóng hoặc kết thúc" });
    assert.match(err400, /Dự án đã đóng hoặc kết thúc/);

    // 404: Not found
    const err404 = formatTaskProgressError({ status: 404 });
    assert.match(err404, /Không tìm thấy công việc/);

    // Generic fallback
    const err500 = formatTaskProgressError({ status: 500, message: "Lỗi máy chủ nội bộ" });
    assert.equal(err500, "Lỗi máy chủ nội bộ");
  });

  test("TC-07: Filter logic correctly filters by status tab", () => {
    const all = filterAssignedTasks(sampleTasks, "ALL", "");
    assert.equal(all.length, 5);

    const inProgress = filterAssignedTasks(sampleTasks, "IN_PROGRESS", "");
    assert.equal(inProgress.length, 1);
    assert.equal(inProgress[0].taskCode, "TSK-002");

    const done = filterAssignedTasks(sampleTasks, "DONE", "");
    assert.equal(done.length, 1);
    assert.equal(done[0].taskCode, "TSK-004");
  });

  test("TC-08: Filter logic filters by search query across code, name, and project", () => {
    // Search by code
    const byCode = filterAssignedTasks(sampleTasks, "ALL", "TSK-003");
    assert.equal(byCode.length, 1);
    assert.equal(byCode[0].taskName, "Viết Unit Test cho Service");

    // Search by name
    const byName = filterAssignedTasks(sampleTasks, "ALL", "bảo mật");
    assert.equal(byName.length, 1);
    assert.equal(byName[0].taskCode, "TSK-004");

    // Search by project
    const byProject = filterAssignedTasks(sampleTasks, "ALL", "Alpha");
    assert.equal(byProject.length, 2);
  });

  test("TC-09: KPI statistics calculates accurate counts for each progress stage", () => {
    const stats = calculateTaskStats(sampleTasks);
    assert.equal(stats.total, 5);
    assert.equal(stats.todo, 1);
    assert.equal(stats.inProgress, 1);
    assert.equal(stats.inReview, 1);
    assert.equal(stats.done, 1);
  });

  test("TC-10: Next suggested status progression returns correct sequential step", () => {
    assert.equal(getNextSuggestedStatus("TODO"), "IN_PROGRESS");
    assert.equal(getNextSuggestedStatus("IN_PROGRESS"), "IN_REVIEW");
    assert.equal(getNextSuggestedStatus("IN_REVIEW"), "DONE");

    // Terminal or invalid statuses have no next step
    assert.equal(getNextSuggestedStatus("DONE"), null);
    assert.equal(getNextSuggestedStatus("CANCELLED"), null);
    assert.equal(getNextSuggestedStatus("UNKNOWN"), null);
  });

  test("TC-11: sortTasksByDeadline correctly prioritizes earliest deadlines with nulls placed at end", () => {
    // Ascending: earliest deadline first
    const sortedAsc = sortTasksByDeadline(sampleTasks, "asc");
    assert.equal(sortedAsc[0].taskCode, "TSK-002"); // 2026-09-20
    assert.equal(sortedAsc[1].taskCode, "TSK-001"); // 2026-09-30
    assert.equal(sortedAsc[2].taskCode, "TSK-003"); // 2026-10-15
    assert.equal(sortedAsc[3].plannedEndDate, null); // null at end
    assert.equal(sortedAsc[4].plannedEndDate, null); // null at end

    // Descending: latest deadline first
    const sortedDesc = sortTasksByDeadline(sampleTasks, "desc");
    assert.equal(sortedDesc[0].taskCode, "TSK-003"); // 2026-10-15
    assert.equal(sortedDesc[1].taskCode, "TSK-001"); // 2026-09-30
    assert.equal(sortedDesc[2].taskCode, "TSK-002"); // 2026-09-20
    assert.equal(sortedDesc[3].plannedEndDate, null);
    assert.equal(sortedDesc[4].plannedEndDate, null);
  });

  test("TC-12: Empty task list and edge case handling for sorting and KPI", () => {
    const emptyList = [];
    const sortedEmpty = sortTasksByDeadline(emptyList, "asc");
    assert.deepEqual(sortedEmpty, []);

    const emptyStats = calculateTaskStats(emptyList);
    assert.deepEqual(emptyStats, {
      total: 0,
      todo: 0,
      inProgress: 0,
      inReview: 0,
      done: 0,
    });
  });
});

