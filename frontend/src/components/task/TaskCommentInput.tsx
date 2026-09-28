import { useState, useRef } from "react";
import { Send, Paperclip, AtSign, X, FileText, Loader2 } from "lucide-react";
import type { ProjectMember } from "../project/projectData";

interface TaskCommentInputProps {
  taskId?: number;
  members: ProjectMember[];
  onSubmit: (content: string, mentionedUserIds: number[], files: File[]) => Promise<void>;
}

export function TaskCommentInput({ members, onSubmit }: TaskCommentInputProps) {
  const [content, setContent] = useState("");
  const [files, setFiles] = useState<File[]>([]);
  const [mentionedUserIds, setMentionedUserIds] = useState<number[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showMentionDropdown, setShowMentionDropdown] = useState(false);
  const [mentionQuery, setMentionQuery] = useState("");
  const fileInputRef = useRef<HTMLInputElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleTextChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const text = e.target.value;
    setContent(text);

    // Kiểm tra ký tự @ gần nhất
    const cursor = e.target.selectionStart;
    const textBeforeCursor = text.slice(0, cursor);
    const lastAtIndex = textBeforeCursor.lastIndexOf("@");

    if (lastAtIndex !== -1 && (lastAtIndex === 0 || /\s/.test(textBeforeCursor[lastAtIndex - 1]))) {
      const query = textBeforeCursor.slice(lastAtIndex + 1);
      if (!query.includes(" ")) {
        setMentionQuery(query.toLowerCase());
        setShowMentionDropdown(true);
        return;
      }
    }
    setShowMentionDropdown(false);
  };

  const handleSelectMention = (member: ProjectMember) => {
    if (!textareaRef.current) return;
    const cursor = textareaRef.current.selectionStart;
    const textBeforeCursor = content.slice(0, cursor);
    const textAfterCursor = content.slice(cursor);
    const lastAtIndex = textBeforeCursor.lastIndexOf("@");

    const mentionName = `@${member.name.replace(/\s+/g, "")} `;
    const newText = textBeforeCursor.slice(0, lastAtIndex) + mentionName + textAfterCursor;
    setContent(newText);
    setShowMentionDropdown(false);

    // Track user ID
    const memberIdNum = typeof member.id === "number" ? member.id : parseInt(String(member.id).replace(/\D/g, ""), 10) || 1;
    if (!mentionedUserIds.includes(memberIdNum)) {
      setMentionedUserIds((prev) => [...prev, memberIdNum]);
    }

    // Refocus textarea
    setTimeout(() => {
      if (textareaRef.current) {
        textareaRef.current.focus();
      }
    }, 50);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const selected = Array.from(e.target.files);
      setFiles((prev) => [...prev, ...selected]);
    }
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const removeFile = (index: number) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim() && files.length === 0) return;

    try {
      setIsSubmitting(true);
      await onSubmit(content.trim(), mentionedUserIds, files);
      setContent("");
      setFiles([]);
      setMentionedUserIds([]);
      setShowMentionDropdown(false);
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredMembers = members.filter((m) =>
    m.name.toLowerCase().includes(mentionQuery) || m.role.toLowerCase().includes(mentionQuery)
  );

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="relative border-t border-slate-200 bg-white p-4">
      {/* Gợi ý danh sách đồng nghiệp khi gõ @ */}
      {showMentionDropdown && (
        <div className="absolute bottom-full left-4 mb-2 w-72 rounded-xl border border-slate-200 bg-white shadow-xl z-20 max-h-48 overflow-y-auto">
          <div className="p-2 border-b border-slate-100 bg-slate-50 text-[11px] font-semibold text-slate-500">
            Nhắc tên đồng nghiệp trong dự án
          </div>
          {filteredMembers.length === 0 ? (
            <div className="p-3 text-center text-xs text-slate-400">Không tìm thấy thành viên</div>
          ) : (
            filteredMembers.map((m) => (
              <button
                key={m.id}
                type="button"
                onClick={() => handleSelectMention(m)}
                className="w-full text-left px-3 py-2 text-xs flex items-center gap-2 hover:bg-indigo-50 transition"
              >
                <div className="h-6 w-6 rounded-full bg-indigo-100 text-indigo-700 font-bold flex items-center justify-center text-[10px]">
                  {m.name.charAt(0)}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="font-semibold text-slate-800 truncate">{m.name}</div>
                  <div className="text-[10px] text-slate-400 truncate">{m.role}</div>
                </div>
              </button>
            ))
          )}
        </div>
      )}

      {/* Danh sách tệp đính kèm chờ gửi */}
      {files.length > 0 && (
        <div className="flex flex-wrap gap-2 mb-3">
          {files.map((file, idx) => (
            <div
              key={idx}
              className="flex items-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs text-slate-700 shadow-xs"
            >
              <FileText className="h-3.5 w-3.5 text-indigo-600" />
              <span className="font-medium truncate max-w-[180px]">{file.name}</span>
              <span className="text-[10px] text-slate-400">({formatFileSize(file.size)})</span>
              <button
                type="button"
                onClick={() => removeFile(idx)}
                className="ml-1 text-slate-400 hover:text-rose-600 transition"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Form nhập ghi chú */}
      <form onSubmit={handleSubmit} className="flex flex-col gap-2">
        <textarea
          ref={textareaRef}
          value={content}
          onChange={handleTextChange}
          placeholder="Viết ghi chú trao đổi trên công việc... (gõ @ để nhắc tên đồng nghiệp)"
          rows={3}
          className="w-full resize-none rounded-xl border border-slate-200 p-3 text-xs leading-relaxed text-slate-800 placeholder-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-100 transition"
        />

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            {/* Nút nhắc tên @ */}
            <button
              type="button"
              onClick={() => {
                setShowMentionDropdown((prev) => !prev);
                setMentionQuery("");
              }}
              className="flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs font-medium text-slate-600 hover:bg-slate-50 hover:text-indigo-600 transition"
              title="Nhắc tên đồng nghiệp (@)"
            >
              <AtSign className="h-3.5 w-3.5 text-indigo-500" />
              <span>Nhắc tên</span>
            </button>

            {/* Nút đính kèm tệp / tệp mô phỏng */}
            <input
              ref={fileInputRef}
              type="file"
              multiple
              onChange={handleFileChange}
              className="hidden"
            />
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs font-medium text-slate-600 hover:bg-slate-50 hover:text-indigo-600 transition"
              title="Đính kèm tệp mô phỏng / tài liệu"
            >
              <Paperclip className="h-3.5 w-3.5 text-indigo-500" />
              <span>Đính kèm tệp</span>
            </button>
          </div>

          <button
            type="submit"
            disabled={(!content.trim() && files.length === 0) || isSubmitting}
            className="flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-xs hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                <span>Đang gửi...</span>
              </>
            ) : (
              <>
                <Send className="h-3.5 w-3.5" />
                <span>Gửi trao đổi</span>
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
