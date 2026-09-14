import { test, describe } from "node:test";
import assert from "node:assert/strict";

describe("NCL-04-CN-002: Task Progress Frontend Logic & Validation", () => {
  // Constant whitelist according to domain policy
  const SPECIALIST_TASK_STATUSES = ["TODO", "IN_PROGRESS", "IN_REVIEW", "DONE"];

  const PROGRESS_STATUS_METADATA = {
    TODO: {
      status: "TODO",
      label: "Chờ thực hiện",
      description: "Công việc đã được giao, chưa bắt đầu triển khai",
    },
    IN_PROGRESS: {
      status: "IN_PROGRESS",
      label: "Đang thực hiện",
      description: "Đang trong quá trình triển khai xử lý công việc",
    },
    IN_REVIEW: {
      status: "IN_REVIEW",
      label: "Chờ duyệt / Nghiệm thu",
      description: "Đã hoàn tất xử lý, đang gửi PM hoặc Tech Lead kiểm tra nghiệm thu",
    },
    DONE: {
      status: "DONE",
      label: "Hoàn thành",
      description: "Công việc đã nghiệm thu xong và đóng hoàn tất",
    },
  };

  function isValidSpecialistStatus(status) {
    return typeof status === "string" && SPECIALIST_TASK_STATUSES.includes(status);
  }

  function buildUpdatePayload(currentStatus, newStatus) {
    if (!isValidSpecialistStatus(newStatus)) {
      throw new Error(`Trạng thái '${newStatus}' không hợp lệ cho Chuyên viên`);
    }
    if (currentStatus === newStatus) {
      return { shouldSend: false, reason: "SAME_STATUS" };
    }
    if (currentStatus === "CANCELLED") {
      throw new Error("Không thể cập nhật công việc đã bị hủy");
    }
    return {
      shouldSend: true,
      endpoint: (taskId) => `/tasks/${taskId}/progress`,
      method: "PATCH",
      body: { status: newStatus },
    };
  }

  function parseBackendError(status, message) {
    if (status === 403) {
      return "Bạn không được phân công thực hiện công việc này. Vui lòng liên hệ PM để kiểm tra.";
    }
    if (status === 400) {
      return message || "Dự án đã đóng hoặc kết thúc, không được phép cập nhật tiến độ công việc.";
    }
    if (status === 404) {
      return "Không tìm thấy công việc tương ứng trên hệ thống.";
    }
    return message || "Đã xảy ra lỗi khi cập nhật tiến độ công việc.";
  }

  function filterTasks(tasks, statusTab, query) {
    return tasks.filter((task) => {
      if (statusTab !== "ALL" && task.status !== statusTab) {
        return false;
      }
      if (query && query.trim()) {
        const q = query.toLowerCase().trim();
        const matchCode = task.taskCode?.toLowerCase().includes(q);
        const matchName = task.taskName?.toLowerCase().includes(q);
        const matchProject = task.projectName?.toLowerCase().includes(q);
        if (!matchCode && !matchName && !matchProject) {
          return false;
        }
      }
      return true;
    });
  }

  function calculateKpiStats(tasks) {
    return {
      total: tasks.length,
      todo: tasks.filter((t) => t.status === "TODO").length,
      inProgress: tasks.filter((t) => t.status === "IN_PROGRESS").length,
      inReview: tasks.filter((t) => t.status === "IN_REVIEW").length,
      done: tasks.filter((t) => t.status === "DONE").length,
    };
  }

  // Sample tasks for testing
  const sampleTasks = [
    {
      taskId: 101,
      taskCode: "TSK-001",
      taskName: "Thiết kế cơ sở dữ liệu",
      projectName: "Dự án Nền tảng số",
      status: "TODO",
      isPrimary: true,
    },
    {
      taskId: 102,
      taskCode: "TSK-002",
      taskName: "Xây dựng RESTful API",
      projectName: "Dự án Nền tảng số",
      status: "IN_PROGRESS",
      isPrimary: true,
    },
    {
      taskId: 103,
      taskCode: "TSK-003",
      taskName: "Viết Unit Test cho Service",
      projectName: "Dự án Nền tảng số",
      status: "IN_REVIEW",
      isPrimary: false,
    },
    {
      taskId: 104,
      taskCode: "TSK-004",
      taskName: "Kiểm thử bảo mật hệ thống",
      projectName: "Dự án Alpha",
      status: "DONE",
      isPrimary: true,
    },
    {
      taskId: 105,
      taskCode: "TSK-005",
      taskName: "Nghiên cứu tài liệu cũ",
      projectName: "Dự án Alpha",
      status: "CANCELLED",
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
    const check = buildUpdatePayload("IN_PROGRESS", "IN_PROGRESS");
    assert.equal(check.shouldSend, false);
    assert.equal(check.reason, "SAME_STATUS");
  });

  test("TC-04: Payload builder rejects CANCELLED status transition attempts", () => {
    // Attempting to set CANCELLED
    assert.throws(
      () => buildUpdatePayload("IN_PROGRESS", "CANCELLED"),
      /không hợp lệ cho Chuyên viên/
    );

    // Attempting to update an already CANCELLED task
    assert.throws(
      () => buildUpdatePayload("CANCELLED", "DONE"),
      /Không thể cập nhật công việc đã bị hủy/
    );
  });

  test("TC-05: Payload builder creates correct endpoint and body for PATCH", () => {
    const payload = buildUpdatePayload("TODO", "IN_PROGRESS");
    assert.equal(payload.shouldSend, true);
    assert.equal(payload.method, "PATCH");
    assert.equal(payload.endpoint(101), "/tasks/101/progress");
    assert.deepEqual(payload.body, { status: "IN_PROGRESS" });
  });

  test("TC-06: Backend error parser handles 403, 400, 404, and fallback messages", () => {
    // 403: Not assigned
    const err403 = parseBackendError(403);
    assert.match(err403, /không được phân công/);

    // 400: Project closed
    const err400 = parseBackendError(400, "Dự án đã đóng hoặc kết thúc");
    assert.match(err400, /Dự án đã đóng hoặc kết thúc/);

    // 404: Not found
    const err404 = parseBackendError(404);
    assert.match(err404, /Không tìm thấy công việc/);

    // Generic fallback
    const err500 = parseBackendError(500, "Lỗi máy chủ nội bộ");
    assert.equal(err500, "Lỗi máy chủ nội bộ");
  });

  test("TC-07: Filter logic correctly filters by status tab", () => {
    const all = filterTasks(sampleTasks, "ALL", "");
    assert.equal(all.length, 5);

    const inProgress = filterTasks(sampleTasks, "IN_PROGRESS", "");
    assert.equal(inProgress.length, 1);
    assert.equal(inProgress[0].taskCode, "TSK-002");

    const done = filterTasks(sampleTasks, "DONE", "");
    assert.equal(done.length, 1);
    assert.equal(done[0].taskCode, "TSK-004");
  });

  test("TC-08: Filter logic filters by search query across code, name, and project", () => {
    // Search by code
    const byCode = filterTasks(sampleTasks, "ALL", "TSK-003");
    assert.equal(byCode.length, 1);
    assert.equal(byCode[0].taskName, "Viết Unit Test cho Service");

    // Search by name
    const byName = filterTasks(sampleTasks, "ALL", "bảo mật");
    assert.equal(byName.length, 1);
    assert.equal(byName[0].taskCode, "TSK-004");

    // Search by project
    const byProject = filterTasks(sampleTasks, "ALL", "Alpha");
    assert.equal(byProject.length, 2);
  });

  test("TC-09: KPI statistics calculates accurate counts for each progress stage", () => {
    const stats = calculateKpiStats(sampleTasks);
    assert.equal(stats.total, 5);
    assert.equal(stats.todo, 1);
    assert.equal(stats.inProgress, 1);
    assert.equal(stats.inReview, 1);
    assert.equal(stats.done, 1);
  });
});
