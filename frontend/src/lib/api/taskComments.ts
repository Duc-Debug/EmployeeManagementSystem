"use client";

import { apiRequest } from "../api-client";

export interface TaskAttachmentItem {
  id: number;
  commentId: number | null;
  taskId: number;
  fileName: string;
  fileDownloadUrl: string;
  fileSize: number;
  fileType: string | null;
  uploadedById: number;
  uploadedByName: string;
  uploadedAt: string;
}

export interface MentionedUserItem {
  userId: number;
  username: string;
  fullName: string;
  email: string;
}

export interface TaskCommentItem {
  id: number;
  taskId: number;
  authorId: number;
  authorName: string;
  authorEmail: string;
  authorRole: string;
  content: string;
  attachments: TaskAttachmentItem[];
  mentions: MentionedUserItem[];
  createdAt: string;
  updatedAt?: string | null;
  version: number;
}

/**
 * Lấy toàn bộ dòng thời gian trao đổi/ghi chú của một công việc
 */
export async function getTaskComments(taskId: number): Promise<TaskCommentItem[]> {
  return await apiRequest<TaskCommentItem[]>(`/tasks/${taskId}/comments`);
}

/**
 * Đăng ghi chú/trao đổi mới lên công việc, hỗ trợ đính kèm tệp và nhắc tên đồng nghiệp
 */
export async function createTaskComment(
  taskId: number,
  content: string,
  mentionedUserIds: number[] = [],
  files: File[] = []
): Promise<TaskCommentItem> {
  const formData = new FormData();
  formData.append("content", content);

  mentionedUserIds.forEach((uid) => {
    formData.append("mentionedUserIds", uid.toString());
  });

  files.forEach((file) => {
    formData.append("files", file);
  });

  return await apiRequest<TaskCommentItem>(`/tasks/${taskId}/comments`, {
    method: "POST",
    body: formData,
  });
}

/**
 * Xóa một trao đổi/ghi chú khỏi công việc
 */
export async function deleteTaskComment(taskId: number, commentId: number): Promise<void> {
  await apiRequest<void>(`/tasks/${taskId}/comments/${commentId}`, {
    method: "DELETE",
  });
}

