import React from "react";
import { CheckCircle2, AlertTriangle, Clock, MessageSquareQuote } from "lucide-react";
import type { ConfirmationStatus } from "../types";

interface ConfirmationStatusBadgeProps {
  status: ConfirmationStatus;
  confirmedAt: string | null;
  feedbackAt?: string | null;
  feedbackNote?: string | null;
  onConfirm: () => void;
  onOpenFeedback?: () => void;
  isConfirming?: boolean;
}

export const ConfirmationStatusBadge: React.FC<ConfirmationStatusBadgeProps> = ({
  status,
  confirmedAt,
  feedbackAt,
  feedbackNote,
  onConfirm,
  onOpenFeedback,
  isConfirming = false,
}) => {
  const formatTime = (isoString: string | null | undefined) => {
    if (!isoString) return "";
    try {
      const date = new Date(isoString);
      return date.toLocaleString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return isoString;
    }
  };

  const renderStatusIndicator = () => {
    switch (status) {
      case "HAS_FEEDBACK":
        return (
          <div
            className="flex items-center gap-1.5 text-purple-700 bg-purple-50 border border-purple-200 px-3 py-1.5 rounded-full text-xs font-medium shadow-xs"
            title={feedbackNote ? `Ý kiến: "${feedbackNote}" (${formatTime(feedbackAt)})` : "Đã gửi ý kiến phản hồi"}
          >
            <MessageSquareQuote className="w-3.5 h-3.5 text-purple-600" />
            <span>Đã gửi phản hồi</span>
            {feedbackAt && (
              <span className="text-purple-600 text-[11px] font-normal border-l border-purple-200 pl-1.5 ml-1">
                {formatTime(feedbackAt)}
              </span>
            )}
          </div>
        );

      case "CONFIRMED":
        return (
          <div
            className="flex items-center gap-1.5 text-emerald-700 bg-emerald-50 border border-emerald-200 px-3 py-1.5 rounded-full text-xs font-medium shadow-xs"
            title={confirmedAt ? `Đã xác nhận lúc: ${formatTime(confirmedAt)}` : "Đã xác nhận"}
          >
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
            <span>Đã xác nhận</span>
            {confirmedAt && (
              <span className="text-emerald-600 text-[11px] font-normal border-l border-emerald-200 pl-1.5 ml-1">
                {formatTime(confirmedAt)}
              </span>
            )}
          </div>
        );

      case "STALE":
        return (
          <div className="flex flex-wrap items-center gap-2">
            <div
              className="flex items-center gap-1.5 text-amber-800 bg-amber-50 border border-amber-300 px-2.5 py-1 rounded-full text-xs font-medium"
              title={confirmedAt ? `Đã xác nhận phiên bản trước lúc: ${formatTime(confirmedAt)}` : ""}
            >
              <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
              <span>Lịch có cập nhật mới — Cần xác nhận lại</span>
            </div>
            <button
              type="button"
              onClick={onConfirm}
              disabled={isConfirming}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-amber-600 hover:bg-amber-700 active:bg-amber-800 text-white rounded-md text-xs font-medium transition shadow-xs disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              <Clock className="w-3.5 h-3.5" />
              <span>{isConfirming ? "Đang xác nhận..." : "Xác nhận lại"}</span>
            </button>
          </div>
        );

      case "NOT_CONFIRMED":
      default:
        return (
          <button
            type="button"
            onClick={onConfirm}
            disabled={isConfirming}
            className="inline-flex items-center gap-1.5 px-4 py-2 bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white rounded-lg text-xs font-medium transition shadow-xs disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
          >
            <CheckCircle2 className="w-4 h-4" />
            <span>{isConfirming ? "Đang xác nhận..." : "Xác nhận đã xem lịch"}</span>
          </button>
        );
    }
  };

  return (
    <div className="flex flex-wrap items-center gap-2">
      {renderStatusIndicator()}
      {onOpenFeedback && (
        <button
          type="button"
          onClick={onOpenFeedback}
          className={`inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium transition cursor-pointer ${
            feedbackNote
              ? "bg-purple-100 hover:bg-purple-200 text-purple-700 border border-purple-200"
              : "bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-200"
          }`}
        >
          <MessageSquareQuote className="w-3.5 h-3.5 opacity-70" />
          <span>{feedbackNote ? "Chỉnh sửa phản hồi" : "Phản hồi"}</span>
        </button>
      )}
    </div>
  );
};