import { useState, useEffect } from "react";
import {
  X,
  MessageSquare,
  Paperclip,
  Clock,
  User as UserIcon,
  AlertCircle,
  RefreshCw,
  FolderOpen,
  Download,
  FileText
} from "lucide-react";
import type { TaskItem, ProjectMember } from "../project/projectData";
import {
  getTaskComments,
  createTaskComment,
  deleteTaskComment,
  type TaskCommentItem,
  type TaskAttachmentItem
} from "@/lib/api/taskComments";
import { TaskCommentTimeline } from "./TaskCommentTimeline";
import { TaskCommentInput } from "./TaskCommentInput";
import { useAuthUser } from "@/lib/auth-session";
import { API_BASE_URL } from "@/lib/api-client";

interface TaskDetailDrawerProps {
  task: TaskItem | null;
  categoryName?: string;
  members: ProjectMember[];
  isOpen: boolean;
  onClose: () => void;
}

export function TaskDetailDrawer({
  task,
  categoryName,
  members,
  isOpen,
  onClose,
}: TaskDetailDrawerProps) {
  const user = useAuthUser();
  const currentUserId = user?.id;

  const [activeTab, setActiveTab] = useState<"discussion" | "attachments">("discussion");
  const [comments, setComments] = useState<TaskCommentItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const taskIdNum = task ? parseInt(String(task.id).replace(/\D/g, ""), 10) || 1 : null;

  const loadComments = async () => {
    if (!taskIdNum) return;
    try {
      setIsLoading(true);
      setErrorMsg(null);
      const data = await getTaskComments(taskIdNum);
      setComments(data || []);
    } catch (e: any) {
      console.error(e);
      setErrorMsg(e?.message || "Không thể tải danh sách trao đổi");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen && taskIdNum) {
      loadComments();
    }
  }, [isOpen, taskIdNum]);

  if (!isOpen || !task) return null;

  const handleAddComment = async (
    content: string,
    mentionedUserIds: number[],
    files: File[]
  ) => {
    if (!taskIdNum) return;
    try {
      const newComment = await createTaskComment(taskIdNum, content, mentionedUserIds, files);
      setComments((prev) => [...prev, newComment]);
    } catch (e: any) {
      alert("Lỗi khi đăng trao đổi: " + (e?.message || "Vui lòng thử lại"));
      throw e;
    }
  };

  const handleDeleteComment = async (commentId: number) => {
    if (!taskIdNum) return;
    if (!window.confirm("Bạn có chắc chắn muốn xóa trao đổi này?")) return;
    try {
      await deleteTaskComment(taskIdNum, commentId);
      setComments((prev) => prev.filter((c) => c.id !== commentId));
    } catch (e: any) {
      alert("Lỗi khi xóa trao đổi: " + (e?.message || "Vui lòng thử lại"));
    }
  };

  // Aggregate all attachments from all comments
  const allAttachments: TaskAttachmentItem[] = comments.flatMap((c) => c.attachments || []);

  const assignee = members.find((m) => m.id === task.assigneeId);

  const formatFileSize = (bytes: number) => {
    if (!bytes) return "0 B";
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-slate-900/40 backdrop-blur-xs flex justify-end animate-in fade-in duration-150">
      <div className="relative w-full max-w-2xl bg-white shadow-2xl flex flex-col h-full animate-in slide-in-from-right duration-200">
        {/* Drawer Header */}
        <div className="flex items-start justify-between border-b border-slate-200 p-5 bg-slate-50/70">
          <div className="flex-1 pr-4">
            <div className="flex items-center gap-2 mb-1.5">
              <span className="rounded-md bg-indigo-50 px-2 py-0.5 text-xs font-bold text-indigo-700 border border-indigo-100">
                {task.code}
              </span>
              {categoryName && (
                <span className="flex items-center gap-1 text-xs text-slate-500 font-medium">
                  <FolderOpen className="h-3.5 w-3.5" />
                  {categoryName}
                </span>
              )}
              <span
                className={`ml-auto text-xs px-2.5 py-0.5 rounded-full font-medium ${
                  task.status === "Hoàn thành"
                    ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                    : task.status === "Đang làm"
                    ? "bg-sky-50 text-sky-700 border border-sky-200"
                    : "bg-slate-100 text-slate-700 border border-slate-200"
                }`}
              >
                {task.status}
              </span>
            </div>

            <h3 className="text-base font-bold text-slate-900 leading-snug">{task.name}</h3>

            {/* Quick Meta Row */}
            <div className="mt-3 flex flex-wrap items-center gap-4 text-xs text-slate-600">
              <div className="flex items-center gap-1.5">
                <UserIcon className="h-3.5 w-3.5 text-slate-400" />
                <span className="text-slate-400">Phụ trách:</span>
                <span className="font-semibold text-slate-800">
                  {assignee ? assignee.name : "Chưa phân công"}
                </span>
              </div>

              <div className="flex items-center gap-1.5">
                <Clock className="h-3.5 w-3.5 text-slate-400" />
                <span className="text-slate-400">Ước tính:</span>
                <span className="font-semibold text-slate-800">{task.hours}h</span>
              </div>

              {task.budgetHours !== undefined && (
                <div className="flex items-center gap-1.5">
                  <span className="text-slate-400">Ngân sách:</span>
                  <span className="font-semibold text-slate-800">{task.budgetHours}h</span>
                </div>
              )}
            </div>
          </div>

          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center justify-between border-b border-slate-200 px-5 bg-white">
          <div className="flex gap-4">
            <button
              onClick={() => setActiveTab("discussion")}
              className={`flex items-center gap-2 py-3 text-xs font-semibold border-b-2 transition ${
                activeTab === "discussion"
                  ? "border-indigo-600 text-indigo-600"
                  : "border-transparent text-slate-500 hover:text-slate-800"
              }`}
            >
              <MessageSquare className="h-4 w-4" />
              <span>Trao đổi & Dòng thời gian ({comments.length})</span>
            </button>

            <button
              onClick={() => setActiveTab("attachments")}
              className={`flex items-center gap-2 py-3 text-xs font-semibold border-b-2 transition ${
                activeTab === "attachments"
                  ? "border-indigo-600 text-indigo-600"
                  : "border-transparent text-slate-500 hover:text-slate-800"
              }`}
            >
              <Paperclip className="h-4 w-4" />
              <span>Tệp mô phỏng & Đính kèm ({allAttachments.length})</span>
            </button>
          </div>

          <button
            onClick={loadComments}
            disabled={isLoading}
            className="p-1.5 text-slate-400 hover:text-slate-700 transition rounded"
            title="Làm mới dòng thời gian"
          >
            <RefreshCw className={`h-4 w-4 ${isLoading ? "animate-spin text-indigo-600" : ""}`} />
          </button>
        </div>

        {/* Drawer Body */}
        <div className="flex-1 overflow-y-auto bg-slate-50/50">
          {errorMsg && (
            <div className="m-4 flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
              <AlertCircle className="h-4 w-4 flex-none" />
              <span>{errorMsg}</span>
            </div>
          )}

          {activeTab === "discussion" && (
            <TaskCommentTimeline
              comments={comments}
              currentUserId={currentUserId}
              onDeleteComment={handleDeleteComment}
            />
          )}

          {activeTab === "attachments" && (
            <div className="p-4">
              {allAttachments.length === 0 ? (
                <div className="flex flex-col items-center justify-center p-12 text-center">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400 mb-3">
                    <Paperclip className="h-6 w-6" />
                  </div>
                  <h4 className="text-sm font-semibold text-slate-700">Chưa có tệp đính kèm nào</h4>
                  <p className="text-xs text-slate-400 max-w-sm mt-1">
                    Các tệp mô phỏng, tài liệu tính toán hoặc hình ảnh được đính kèm khi trao đổi sẽ xuất hiện tại đây.
                  </p>
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {allAttachments.map((att) => {
                    const downloadUrl = att.fileDownloadUrl.startsWith("http")
                      ? att.fileDownloadUrl
                      : `${API_BASE_URL}${att.fileDownloadUrl}`;

                    return (
                      <div
                        key={att.id}
                        className="flex items-center justify-between gap-2 rounded-xl border border-slate-200 bg-white p-3 shadow-xs hover:border-indigo-200 transition"
                      >
                        <div className="flex items-center gap-2.5 min-w-0">
                          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600 flex-none">
                            <FileText className="h-5 w-5" />
                          </div>
                          <div className="min-w-0">
                            <p className="text-xs font-semibold text-slate-800 truncate" title={att.fileName}>
                              {att.fileName}
                            </p>
                            <div className="flex items-center gap-2 text-[10px] text-slate-400 mt-0.5">
                              <span>{formatFileSize(att.fileSize)}</span>
                              <span>•</span>
                              <span>Bởi {att.uploadedByName}</span>
                            </div>
                          </div>
                        </div>

                        <a
                          href={downloadUrl}
                          target="_blank"
                          rel="noreferrer"
                          download={att.fileName}
                          className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-500 hover:bg-indigo-50 hover:text-indigo-600 hover:border-indigo-200 transition flex-none"
                          title="Tải về"
                        >
                          <Download className="h-4 w-4" />
                        </a>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Drawer Footer: Input Area (Shown in discussion tab) */}
        {activeTab === "discussion" && (
          <TaskCommentInput
            taskId={taskIdNum || 1}
            members={members}
            onSubmit={handleAddComment}
          />
        )}
      </div>
    </div>
  );
}
