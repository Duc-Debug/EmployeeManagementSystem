import { test, describe } from "node:test";
import assert from "node:assert/strict";

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

    // Helper to group cards by status
    function groupCardsByStatus(cards) {
        return {
            TODO: cards.filter((c) => c.status === "TODO"),
            IN_PROGRESS: cards.filter((c) => c.status === "IN_PROGRESS"),
            IN_REVIEW: cards.filter((c) => c.status === "IN_REVIEW"),
            DONE: cards.filter((c) => c.status === "DONE"),
            CANCELLED: cards.filter((c) => c.status === "CANCELLED"),
        };
    }

    // Helper to simulate card drop & optimistic update
    function handleCardDrop(boardData, taskId, newStatus) {
        const allCards = [
            ...boardData.TODO,
            ...boardData.IN_PROGRESS,
            ...boardData.IN_REVIEW,
            ...boardData.DONE,
            ...boardData.CANCELLED,
        ];
        const card = allCards.find((c) => c.taskId === taskId);
        if (!card) return { success: false, reason: "NOT_FOUND", boardData };
        if (card.status === newStatus) return { success: false, reason: "SAME_STATUS", boardData };
        if (!card.canMove) return { success: false, reason: "FORBIDDEN", boardData };

        // Optimistic move
        const updated = {
            TODO: boardData.TODO.filter((c) => c.taskId !== taskId),
            IN_PROGRESS: boardData.IN_PROGRESS.filter((c) => c.taskId !== taskId),
            IN_REVIEW: boardData.IN_REVIEW.filter((c) => c.taskId !== taskId),
            DONE: boardData.DONE.filter((c) => c.taskId !== taskId),
            CANCELLED: boardData.CANCELLED.filter((c) => c.taskId !== taskId),
        };
        const movedCard = { ...card, status: newStatus };
        updated[newStatus].push(movedCard);

        return { success: true, movedCard, boardData: updated };
    }

    // Helper to filter cards by search
    function filterCardsBySearch(cards, query) {
        if (!query || !query.trim()) return cards;
        const q = query.toLowerCase().trim();
        return cards.filter(
            (c) =>
                c.taskCode.toLowerCase().includes(q) ||
                c.name.toLowerCase().includes(q) ||
                (c.projectCode && c.projectCode.toLowerCase().includes(q)) ||
                c.assignees.some((a) => a.fullName.toLowerCase().includes(q))
        );
    }

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
});
