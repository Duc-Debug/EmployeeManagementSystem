"use client";

import { apiRequest } from "../api-client";

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE' | 'CANCELLED';

export interface TaskBoardAssignee {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  isPrimary: boolean;
}

export interface TaskBoardCard {
  taskId: number;
  taskCode: string;
  name: string;
  description?: string;
  projectId: number;
  projectCode: string;
  projectName: string;
  status: TaskStatus;
  estimatedHours?: number;
  actualHours?: number;
  plannedStartDate?: string;
  plannedEndDate?: string;
  sortOrder?: number;
  assignees: TaskBoardAssignee[];
  canMove: boolean;
}

export interface TaskBoardData {
  todoTasks: TaskBoardCard[];
  inProgressTasks: TaskBoardCard[];
  inReviewTasks: TaskBoardCard[];
  doneTasks: TaskBoardCard[];
  cancelledTasks: TaskBoardCard[];
  totalTasks: number;
  projectIdFilter?: number;
  employeeIdFilter?: number;
}

export interface MoveTaskBoardStatusPayload {
  newStatus: TaskStatus;
}

/**
 * Lấy danh sách công việc trên bảng Kanban theo các cột trạng thái
 */
export async function getTaskBoard(params?: {
  projectId?: number | string;
  employeeId?: number | string;
}): Promise<TaskBoardData> {
  const searchParams = new URLSearchParams();
  if (params?.projectId) {
    searchParams.append("projectId", String(params.projectId));
  }
  if (params?.employeeId) {
    searchParams.append("employeeId", String(params.employeeId));
  }

  const queryString = searchParams.toString();
  const endpoint = queryString ? `/tasks/board?${queryString}` : "/tasks/board";
  return await apiRequest<TaskBoardData>(endpoint);
}

/**
 * Cập nhật trạng thái công việc khi kéo thả trên bảng Kanban
 */
export async function moveTaskBoardStatus(
  taskId: number | string,
  newStatus: TaskStatus
): Promise<TaskBoardCard> {
  return await apiRequest<TaskBoardCard>(`/tasks/${taskId}/board-status`, {
    method: "PATCH",
    body: JSON.stringify({ newStatus }),
  });
}

export function groupCardsByStatus(cards: TaskBoardCard[]): Record<TaskStatus, TaskBoardCard[]> {
  return {
    TODO: cards.filter((c) => c.status === "TODO"),
    IN_PROGRESS: cards.filter((c) => c.status === "IN_PROGRESS"),
    IN_REVIEW: cards.filter((c) => c.status === "IN_REVIEW"),
    DONE: cards.filter((c) => c.status === "DONE"),
    CANCELLED: cards.filter((c) => c.status === "CANCELLED"),
  };
}

export function handleCardDrop(
  boardData: Record<TaskStatus, TaskBoardCard[]>,
  taskId: number,
  newStatus: TaskStatus
) {
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

  const updated: Record<TaskStatus, TaskBoardCard[]> = {
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

export function filterCardsBySearch(cards: TaskBoardCard[], query?: string | null): TaskBoardCard[] {
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

export function resolveEmployeeId(
  user?: { id?: number } | null,
  directProfile?: { id?: number } | null,
  empList: Array<{ id: number; userId?: number }> = []
): number | null {
  if (directProfile?.id != null) return directProfile.id;
  if (!user?.id) return null;
  const found = empList.find((e) => e.userId === user.id);
  return found ? found.id : null;
}
