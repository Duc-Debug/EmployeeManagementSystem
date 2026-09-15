"use client";

/**
 * NCL-07-CN-003: Modal hiển thị danh sách thông báo khi phân bổ thay đổi.
 * Hỗ trợ phân trang, lọc theo dự án, hiển thị nội dung thông báo đầy đủ.
 * Phù hợp với quyền VT-02 (PM - trong phạm vi dự án) và VT-03 (RM - trong phạm vi chi nhánh).
 */
import { useState, useEffect, useCallback } from "react";
import {
  Bell,
  X,
  Clock,
  ChevronLeft,
  ChevronRight,
  Loader2,
  Info,
  RefreshCw,
} from "lucide-react";
import {
  getAllocationNotifications,
  type AllocationNotificationItemResult,
  type AllocationNotificationPageResult,
} from "@/lib/api/allocations";

interface AllocationNotificationsModalProps {
  open: boolean;
  onClose: () => void;
  projectId?: number;
  userRole?: string;
}

export function AllocationNotificationsModal({
  open,
  onClose,
  projectId,
  userRole,
}: AllocationNotificationsModalProps) {
  const [data, setData] = useState<AllocationNotificationPageResult | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState<number>(0);
  const pageSize = 10;

  const fetchNotifications = useCallback(
    async (page: number) => {
      setLoading(true);
      setError(null);
      try {
        const result = await getAllocationNotifications({
          projectId,
          page,
          size: pageSize,
        });
        setData(result);
        setCurrentPage(page);
      } catch (err: unknown) {
        const message =
          err instanceof Error
            ? err.message
            : "Không thể tải danh sách thông báo phân bổ.";
        setError(message);
      } finally {
        setLoading(false);
      }
    },
    [projectId]
  );

  useEffect(() => {
    if (open) {
      fetchNotifications(0);
    }
  }, [open, fetchNotifications]);

  if (!open) return null;

  const formatTimestamp = (isoString: string) => {
    if (!isoString) return "";
    try {
      const date = new Date(isoString);
      return date.toLocaleString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in">
      <div className="flex h-[85vh] w-full max-w-3xl flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 px-5 py-4">
          <div className="flex items-center gap-3">
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-100 text-sky-700 shadow-2xs">
              <Bell className="h-5 w-5" />
            </span>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-slate-900">
                  Thông Báo Thay Đổi Phân Bổ
                </h3>
                {userRole && (
                  <span className="rounded-full bg-slate-200/80 px-2 py-0.5 text-[11px] font-semibold text-slate-700">
                    {userRole === "VT-03" ? "RM (Phạm vi bộ phận)" : "PM (Phạm vi dự án)"}
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-500">
                Lịch sử các biến động phân bổ nguồn lực (thêm, sửa giờ, chuyển tuần, gỡ phân bổ)
              </p>
            </div>
          </div>

          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => fetchNotifications(currentPage)}
              disabled={loading}
              className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-200 hover:text-slate-800 transition"
              title="Làm mới"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            </button>
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition"
              title="Đóng"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-5">
          {error && (
            <div className="mb-4 flex items-center gap-2.5 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-800">
              <Info className="h-4 w-4 shrink-0 text-rose-600" />
              <span>{error}</span>
            </div>
          )}

          {loading && !data && (
            <div className="flex h-64 flex-col items-center justify-center gap-2 text-slate-400">
              <Loader2 className="h-8 w-8 animate-spin text-sky-600" />
              <p className="text-xs">Đang tải thông báo phân bổ...</p>
            </div>
          )}

          {!loading && (!data || !data.content || data.content.length === 0) && (
            <div className="flex h-64 flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-slate-200 bg-slate-50/50 p-6 text-center text-slate-400">
              <Bell className="h-10 w-10 text-slate-300" />
              <p className="text-sm font-semibold text-slate-600">
                Chưa có thông báo phân bổ nào
              </p>
              <p className="max-w-sm text-xs text-slate-400">
                Khi có sự thay đổi phân bổ nhân sự (thêm mới, chỉnh sửa giờ, chuyển tuần hoặc gỡ bỏ), các thông báo sẽ được lưu trữ và hiển thị tại đây.
              </p>
            </div>
          )}

          {data && data.content && data.content.length > 0 && (
            <div className="space-y-3">
              {data.content.map((item: AllocationNotificationItemResult) => (
                <div
                  key={item.id}
                  className="group relative flex flex-col gap-2 rounded-xl border border-slate-200 bg-white p-4 shadow-2xs transition hover:border-sky-300 hover:shadow-xs"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <span className="flex h-2 w-2 rounded-full bg-sky-500" />
                      <h4 className="text-sm font-semibold text-slate-900">
                        {item.title}
                      </h4>
                    </div>
                    <span className="inline-flex items-center gap-1 text-[11px] text-slate-400 whitespace-nowrap">
                      <Clock className="h-3 w-3" />
                      {formatTimestamp(item.createdAt)}
                    </span>
                  </div>

                  <p className="text-xs leading-relaxed text-slate-600 whitespace-pre-wrap pl-4 border-l-2 border-slate-100">
                    {item.content}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Modal Footer with Pagination */}
        <div className="flex items-center justify-between border-t border-slate-200 bg-slate-50 px-5 py-3 text-xs text-slate-600">
          <div>
            {data && data.totalElements > 0 ? (
              <span>
                Hiển thị{" "}
                <strong>
                  {currentPage * pageSize + 1} -{" "}
                  {Math.min((currentPage + 1) * pageSize, data.totalElements)}
                </strong>{" "}
                trên tổng số <strong>{data.totalElements}</strong> thông báo
              </span>
            ) : (
              <span>0 thông báo</span>
            )}
          </div>

          <div className="flex items-center gap-1.5">
            <button
              type="button"
              disabled={currentPage <= 0 || loading}
              onClick={() => fetchNotifications(currentPage - 1)}
              className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-40 disabled:pointer-events-none transition shadow-2xs"
            >
              <ChevronLeft className="h-3.5 w-3.5" />
              <span>Trước</span>
            </button>
            <span className="px-2 text-xs font-semibold text-slate-700">
              Trang {currentPage + 1} / {Math.max(data?.totalPages || 1, 1)}
            </span>
            <button
              type="button"
              disabled={!data || currentPage >= data.totalPages - 1 || loading}
              onClick={() => fetchNotifications(currentPage + 1)}
              className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-40 disabled:pointer-events-none transition shadow-2xs"
            >
              <span>Sau</span>
              <ChevronRight className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
