import { Download, Trash2, FileText, Image as ImageIcon, Archive, Clock } from "lucide-react";
import type { TaskCommentItem } from "@/lib/api/taskComments";
import { API_BASE_URL } from "@/lib/api-client";

interface TaskCommentTimelineProps {
  comments: TaskCommentItem[];
  currentUserId?: number;
  onDeleteComment?: (commentId: number) => Promise<void>;
}

export function TaskCommentTimeline({
  comments,
  currentUserId,
  onDeleteComment,
}: TaskCommentTimelineProps) {
  const formatTimeAgo = (dateStr: string) => {
    try {
      const date = new Date(dateStr);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffMin = Math.floor(diffMs / 60000);
      if (diffMin < 1) return "Vừa xong";
      if (diffMin < 60) return `${diffMin} phút trước`;
      const diffHours = Math.floor(diffMin / 60);
      if (diffHours < 24) return `${diffHours} giờ trước`;
      const diffDays = Math.floor(diffHours / 24);
      if (diffDays < 7) return `${diffDays} ngày trước`;
      return date.toLocaleDateString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return dateStr;
    }
  };

  const formatFileSize = (bytes: number) => {
    if (!bytes) return "0 B";
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const getFileIcon = (fileName: string) => {
    const ext = fileName.split(".").pop()?.toLowerCase();
    if (["png", "jpg", "jpeg", "gif", "svg", "webp"].includes(ext || "")) {
      return <ImageIcon className="h-4 w-4 text-emerald-500" />;
    }
    if (["zip", "rar", "7z", "tar", "gz"].includes(ext || "")) {
      return <Archive className="h-4 w-4 text-amber-500" />;
    }
    return <FileText className="h-4 w-4 text-indigo-500" />;
  };

  // Render text highlighting @mentions
  const renderContentWithMentions = (content: string) => {
    const parts = content.split(/(@[a-zA-Z0-9._-]+)/g);
    return parts.map((part, index) => {
      if (part.startsWith("@")) {
        return (
          <span
            key={index}
            className="inline-flex items-center rounded-md bg-indigo-50 px-1.5 py-0.5 text-xs font-semibold text-indigo-700 border border-indigo-100"
          >
            {part}
          </span>
        );
      }
      return <span key={index}>{part}</span>;
    });
  };

  if (!comments || comments.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center p-12 text-center">
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400 mb-3">
          <FileText className="h-6 w-6" />
        </div>
        <h4 className="text-sm font-semibold text-slate-700">Chưa có trao đổi nào</h4>
        <p className="text-xs text-slate-400 max-w-sm mt-1">
          Hãy để lại ghi chú, nhắc tên đồng nghiệp (@) hoặc đính kèm tệp mô phỏng để mọi người cùng nắm bắt ngữ cảnh công việc.
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col space-y-4 p-4 overflow-y-auto">
      {comments.map((comment) => {
        const isAuthor = currentUserId && comment.authorId === currentUserId;
        const initial = comment.authorName ? comment.authorName.charAt(0).toUpperCase() : "U";

        return (
          <div
            key={comment.id}
            className="group relative flex gap-3 rounded-xl border border-slate-100 bg-white p-3.5 shadow-xs transition hover:border-slate-200"
          >
            {/* Avatar */}
            <div className="flex-none">
              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-linear-to-br from-indigo-500 to-indigo-700 text-xs font-bold text-white shadow-xs">
                {initial}
              </div>
            </div>

            {/* Content Body */}
            <div className="flex-1 min-w-0">
              {/* Header: Author name, Role badge, Timestamp */}
              <div className="flex flex-wrap items-center justify-between gap-1 mb-1">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-slate-800">{comment.authorName}</span>
                  {comment.authorRole && (
                    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-600">
                      {comment.authorRole}
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-2 text-[11px] text-slate-400">
                  <Clock className="h-3 w-3" />
                  <span>{formatTimeAgo(comment.createdAt)}</span>

                  {onDeleteComment && (isAuthor || !currentUserId) && (
                    <button
                      onClick={() => onDeleteComment(comment.id)}
                      className="opacity-0 group-hover:opacity-100 transition p-1 hover:text-rose-600 rounded"
                      title="Xóa trao đổi này"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  )}
                </div>
              </div>

              {/* Message text */}
              {comment.content && comment.content.trim().length > 0 && (
                <div className="text-xs leading-relaxed text-slate-700 whitespace-pre-wrap break-words">
                  {renderContentWithMentions(comment.content)}
                </div>
              )}

              {/* Attachments */}
              {comment.attachments && comment.attachments.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-2 pt-2 border-t border-slate-100">
                  {comment.attachments.map((att) => {
                    const downloadUrl = att.fileDownloadUrl.startsWith("http")
                      ? att.fileDownloadUrl
                      : `${API_BASE_URL}${att.fileDownloadUrl}`;

                    return (
                      <a
                        key={att.id}
                        href={downloadUrl}
                        target="_blank"
                        rel="noreferrer"
                        download={att.fileName}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-xs text-slate-700 hover:bg-indigo-50 hover:border-indigo-200 transition group/att"
                      >
                        {getFileIcon(att.fileName)}
                        <span className="font-medium truncate max-w-[200px]">{att.fileName}</span>
                        <span className="text-[10px] text-slate-400">
                          ({formatFileSize(att.fileSize)})
                        </span>
                        <Download className="h-3.5 w-3.5 text-slate-400 group-hover/att:text-indigo-600 transition ml-0.5" />
                      </a>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
