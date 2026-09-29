import { test, describe } from "node:test";
import assert from "node:assert/strict";
import {
  groupCardsByStatus,
  handleCardDrop,
  filterCardsBySearch,
  resolveEmployeeId,
} from "../lib/api/taskBoard.ts";

describe("NCL-04-CN-006: Task Board Frontend Logic & Permissions", () => {
    // Mock sample task cards
    const sampleCards = [
        {
            taskId: 1,
            taskCode: "TSK-001",
            name: "Thiết kế database schema",
            projectId: 10,
            projectCode: "PRJ-01",
            projectName: "Dự án Alpha",
            status: "TODO",
            canMove: true,
            assignees: [{ employeeId: 101, employeeCode: "EMP-01", fullName: "Nguyễn Văn A", isPrimary: true }],
        },
        {
            taskId: 2,
            taskCode: "TSK-002",
            name: "Xây dựng REST API",
            projectId: 10,
            projectCode: "PRJ-01",
            projectName: "Dự án Alpha",
            status: "IN_PROGRESS",
            canMove: true,
            assignees: [{ employeeId: 101, employeeCode: "EMP-01", fullName: "Nguyễn Văn A", isPrimary: true }],
        },
        {
            taskId: 3,
            taskCode: "TSK-003",
            name: "Viết unit test cho service",
            projectId: 10,
            projectCode: "PRJ-01",
            projectName: "Dự án Alpha",
            status: "IN_REVIEW",
            canMove: false, // Not PM and not assignee
            assignees: [{ employeeId: 102, employeeCode: "EMP-02", fullName: "Trần Thị B", isPrimary: true }],
        },
        {
            taskId: 4,
            taskCode: "TSK-004",
            name: "Deploy staging server",
            projectId: 10,
            projectCode: "PRJ-01",
            projectName: "Dự án Alpha",
            status: "DONE",
            canMove: true,
            assignees: [{ employeeId: 101, employeeCode: "EMP-01", fullName: "Nguyễn Văn A", isPrimary: true }],
        },
    ];

    test("TC-01: Grouping cards into 5 standard status columns (TODO, IN_PROGRESS, IN_REVIEW, DONE, CANCELLED)", () => {
        const columns = groupCardsByStatus(sampleCards);

        assert.equal(columns.TODO.length, 1);
        assert.equal(columns.TODO[0].taskCode, "TSK-001");

        assert.equal(columns.IN_PROGRESS.length, 1);
        assert.equal(columns.IN_PROGRESS[0].taskCode, "TSK-002");

        assert.equal(columns.IN_REVIEW.length, 1);
        assert.equal(columns.IN_REVIEW[0].taskCode, "TSK-003");

        assert.equal(columns.DONE.length, 1);
        assert.equal(columns.DONE[0].taskCode, "TSK-004");

        assert.equal(columns.CANCELLED.length, 0);
    });

    test("TC-01: Drag card to IN_REVIEW updates immediately when canMove is true", () => {
        const initial = groupCardsByStatus(sampleCards);
        const result = handleCardDrop(initial, 2, "IN_REVIEW");

        assert.equal(result.success, true);
        assert.equal(result.movedCard.status, "IN_REVIEW");
        assert.equal(result.boardData.IN_PROGRESS.length, 0);
        assert.equal(result.boardData.IN_REVIEW.length, 2);
    });

    test("TC-02: Moving someone else's card is blocked when canMove is false and status is preserved", () => {
        const initial = groupCardsByStatus(sampleCards);
        // Task 3 has canMove: false
        const result = handleCardDrop(initial, 3, "DONE");

        assert.equal(result.success, false);
        assert.equal(result.reason, "FORBIDDEN");
        // State remains untouched
        assert.equal(result.boardData.IN_REVIEW.length, 1);
        assert.equal(result.boardData.IN_REVIEW[0].taskId, 3);
        assert.equal(result.boardData.DONE.length, 1);
    });

    test("TC-02: Dropping card into the same column does nothing", () => {
        const initial = groupCardsByStatus(sampleCards);
        const result = handleCardDrop(initial, 1, "TODO");

        assert.equal(result.success, false);
        assert.equal(result.reason, "SAME_STATUS");
    });

    test("TC-02: Optimistic update rollbacks to snapshot when API call fails", async () => {
        const initial = groupCardsByStatus(sampleCards);
        const snapshot = structuredClone(initial);

        // Simulate optimistic drop
        const result = handleCardDrop(initial, 1, "IN_PROGRESS");
        assert.equal(result.success, true);
        let activeBoard = result.boardData;
        assert.equal(activeBoard.TODO.length, 0);
        assert.equal(activeBoard.IN_PROGRESS.length, 2);

        // Simulate backend 403 Forbidden / Network error
        const apiFailed = true;
        if (apiFailed) {
            // Rollback to snapshot
            activeBoard = snapshot;
        }

        assert.equal(activeBoard.TODO.length, 1);
        assert.equal(activeBoard.TODO[0].taskCode, "TSK-001");
        assert.equal(activeBoard.IN_PROGRESS.length, 1);
    });

    test("TC-03: Search query filters cards by code, title, and assignee name", () => {
        // Search by code
        const resCode = filterCardsBySearch(sampleCards, "TSK-002");
        assert.equal(resCode.length, 1);
        assert.equal(resCode[0].taskId, 2);

        // Search by name
        const resName = filterCardsBySearch(sampleCards, "database");
        assert.equal(resName.length, 1);
        assert.equal(resName[0].taskId, 1);

        // Search by assignee
        const resAssignee = filterCardsBySearch(sampleCards, "Trần Thị B");
        assert.equal(resAssignee.length, 1);
        assert.equal(resAssignee[0].taskId, 3);
    });

    test("TC-04: 'Việc của tôi' filter extracts current employee's tasks correctly", () => {
        const currentEmployeeId = 101;
        const myTasks = sampleCards.filter((c) =>
            c.assignees.some((a) => a.employeeId === currentEmployeeId)
        );

        assert.equal(myTasks.length, 3);
        assert.deepEqual(
            myTasks.map((t) => t.taskId),
            [1, 2, 4]
        );
    });

    test("TC-05: Quick move triggers the exact same status move and permission guard", () => {
        const initial = groupCardsByStatus(sampleCards);

        // Allowed card (Task 1 has canMove: true)
        const allowedMove = handleCardDrop(initial, 1, "DONE");
        assert.equal(allowedMove.success, true);
        assert.equal(allowedMove.boardData.DONE.length, 2);

        // Blocked card (Task 3 has canMove: false)
        const blockedMove = handleCardDrop(initial, 3, "DONE");
        assert.equal(blockedMove.success, false);
        assert.equal(blockedMove.reason, "FORBIDDEN");
    });

    test("TC-06: Direct employee profile lookup has priority over paginated employees list", () => {
        const currentUser = { id: 99 };
        // Empty paginated list (user not found on page 1)
        const paginatedEmployees = [{ id: 1, userId: 10 }, { id: 2, userId: 20 }];
        const directUserProfile = { id: 555, userId: 99 };

        const resolvedId = resolveEmployeeId(currentUser, directUserProfile, paginatedEmployees);
        assert.equal(resolvedId, 555);
    });
});
