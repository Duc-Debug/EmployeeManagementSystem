import test from "node:test";
import assert from "node:assert/strict";
import React from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { SIDEBAR_GROUPS, canAccessTab } from "../components/dashboard/SideBar.tsx";
import { TimesheetBudgetWarning } from "../components/timesheet/TimesheetBudgetWarning.tsx";
import { ProjectTaskModal } from "../components/project/ProjectTaskModal.tsx";
import { TaskBoardCard } from "../components/task/TaskBoardCard.tsx";
import { resolveActiveTab } from "../components/dashboard/dashboard-routing.ts";

test("PM sidebar exposes the 15 allowed modules and hides the 9 others", () => {
  const visible = SIDEBAR_GROUPS.flatMap(group => group.items).filter(item => canAccessTab("VT-02", item.id, "SELF")).map(item => item.id);
  assert.deepEqual(visible, ["overview", "capacity-dashboard", "project", "capacity", "schedule-conflict", "my-schedule", "attendance", "availability", "unavailability", "leave", "working-calendar", "project-allocation-report", "departments", "skills", "roles"]);
  for (const role of ["VT-01", "VT-03", "VT-05"]) assert.equal(canAccessTab(role, "attendance"), false);
  assert.equal(resolveActiveTab("/projects/123"), "project");
});

test("80% budget badge accounts for all pending entries before approval", () => {
  const render = entry => renderToStaticMarkup(React.createElement(TimesheetBudgetWarning, { entry }));
  assert.equal(render({ hours: 1, taskBudgetHours: 100, taskActualHours: 70, taskPendingHours: 9 }), "");
  const atThreshold = render({ hours: 1, taskBudgetHours: 100, taskActualHours: 70, taskPendingHours: 10 });
  assert.match(atThreshold, /80% ngân sách/);
  assert.match(atThreshold, /80h \/ Ngân sách: 100h/);
  assert.match(atThreshold, /10h đang chờ duyệt/);
  assert.equal(render({ hours: 8, taskBudgetHours: 0 }), "");
  assert.equal(render({ hours: 8, taskBudgetHours: null }), "");
});

test("closed project task modal disables submit and explains why", () => {
  const markup = renderToStaticMarkup(React.createElement(ProjectTaskModal, {
    open: true, isClosed: true, categories: [], members: [], onClose() {}, onSubmit() {},
  }));
  assert.match(markup, /type="submit" disabled=""/);
  assert.match(markup, /Dự án đã đóng, không thể tạo hoặc giao thêm công việc/);
});

test("compact Kanban cards defer description until detail is opened", () => {
  const markup = renderToStaticMarkup(React.createElement(TaskBoardCard, {
    card: { taskId: 1, taskCode: "TASK-1", name: "Build report", description: "Detailed task description", projectId: 1, projectName: "Project", projectCode: "PRJ", status: "TODO", assignees: [], canMove: false },
    onDragStart() {},
  }));
  assert.match(markup, /Xem chi tiết Build report/);
  assert.doesNotMatch(markup, /Detailed task description/);
  assert.match(markup, /draggable="false"/);
});
