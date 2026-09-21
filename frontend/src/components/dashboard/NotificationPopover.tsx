import { useState, useEffect, useRef, useCallback } from "react";
import {
  Bell,
  CheckCheck,
  Clock,
  Trash2,
  AlertCircle,
  AlertTriangle,
  Info,
  ExternalLink,
  ChevronDown,
  Settings,
  Sliders,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useAuthUser } from "@/lib/auth-session";
import NotificationDedupConfigModal from "./NotificationDedupConfigModal";
import {
  getNotificationCenter,
  getUnreadNotificationCount,
  markNotificationRead,
  markAllNotificationsRead,
  deleteNotification,
  type NotificationCenterItem,
  type NotificationLevel,
} from "@/lib/api/notifications";
import NotificationSettingsModal from "@/components/notification/NotificationSettingsModal";

interface NotificationPopoverProps {
  onSelectTask?: (taskId: number) => void;
}

export function NotificationPopover({ onSelectTask }: NotificationPopoverProps) {
  const navigate = useNavigate();
  const user = useAuthUser();
  const [notifications, setNotifications] = useState<NotificationCenterItem[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isDedupModalOpen, setIsDedupModalOpen] = useState(false);

  const canManageDedup =
    user?.roleCode?.toUpperCase().replace(/_/g, "-") === "VT-06" ||
    user?.permissions?.includes("NOTIFICATION_DEDUPLICATION_MANAGE");

  // Filters & Pagination
  const [statusFilter, setStatusFilter] = useState<"ALL" | "UNREAD">("ALL");
  const [levelFilter, setLevelFilter] = useState<"ALL" | NotificationLevel>("ALL");
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Item deletion confirmation
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);

  // Cấu hình thông báo (NCL-11-CN-002)
  const [isSettingsOpen, setIsSettingsOpen] = useState<boolean>(false);

  const popoverRef = useRef<HTMLDivElement>(null);

  const fetchUnreadBadge = useCallback(async () => {
    try {
      const count = await getUnreadNotificationCount();
      setUnreadCount(count);
    } catch {
      // Ignore if unauthenticated or network error
    }
  }, []);

  const fetchNotificationList = useCallback(async (pageToLoad = 0, append = false) => {
    try {
      setIsLoading(true);
      const res = await getNotificationCenter({
        status: statusFilter,
        level: levelFilter,
        page: pageToLoad,
        size: 15,
      });

      if (append) {
        setNotifications((prev) => [...prev, ...(res?.items || [])]);
      } else {
        setNotifications(res?.items || []);
      }
      setTotalPages(res?.totalPages || 0);
      setTotalElements(res?.totalElements || 0);
      setCurrentPage(res?.currentPage || 0);
      if (res?.unreadCount !== undefined) {
        setUnreadCount(res.unreadCount);
      }
    } catch (e) {
      console.error("Lỗi khi tải danh sách thông báo:", e);
    } finally {
      setIsLoading(false);
    }
  }, [statusFilter, levelFilter]);

  // Initial fetch and 30s polling
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchUnreadBadge();
    const interval = setInterval(() => {
      fetchUnreadBadge();
      if (isOpen) {
        fetchNotificationList(0, false);
      }
    }, 30000);
    return () => clearInterval(interval);
  }, [fetchUnreadBadge, fetchNotificationList, isOpen]);

  // Fetch when opening popover or changing filters
  useEffect(() => {
    if (isOpen) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      fetchNotificationList(0, false);
    }
  }, [isOpen, statusFilter, levelFilter, fetchNotificationList]);

  // Click outside to close
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (popoverRef.current && !popoverRef.current.contains(e.target as Node)) {
        setIsOpen(false);
        setConfirmDeleteId(null);
      }
    };
    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen]);

  const handleMarkAllRead = async () => {
    try {
      await markAllNotificationsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (e) {
      console.error("Lỗi khi đánh dấu đọc tất cả:", e);
    }
  };

  const handleItemClick = async (n: NotificationCenterItem) => {
    // 1. Mark as read if unread (separate mutation)
    if (!n.isRead) {
      try {
        await markNotificationRead(n.id);
        setNotifications((prev) =>
          prev.map((item) => (item.id === n.id ? { ...item, isRead: true } : item))
        );
        setUnreadCount((prev) => Math.max(0, prev - 1));
      } catch (e) {
        console.error("Lỗi khi đánh dấu đã đọc:", e);
      }
    }

    setIsOpen(false);

    // 2. Deep link navigation based on relatedEntityType and relatedEntityId
    if (n.relatedEntityType && n.relatedEntityId) {
      const type = n.relatedEntityType.toUpperCase();
      const id = n.relatedEntityId;

      if (type === "TASK") {
        const taskIdNum = Number(id);
        if (onSelectTask && !isNaN(taskIdNum)) {
          onSelectTask(taskIdNum);
        } else {
          window.dispatchEvent(
            new CustomEvent("openTaskDiscussion", {
              detail: { taskId: taskIdNum },
            })
          );
        }
      } else if (type === "CAPACITY_WEEK") {
        navigate(`/capacity?week=${encodeURIComponent(id)}`);
      } else if (type === "PROJECT" || type === "PROJECT_ALLOCATION" || type === "ALLOCATION") {
        navigate(`/projects/${encodeURIComponent(id)}`);
      } else if (type === "LEAVE_REQUEST") {
        navigate(`/leave?requestId=${encodeURIComponent(id)}`);
      }
    }
  };

  const handleDeleteItem = async (id: number, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await deleteNotification(id);
      const targetItem = notifications.find((n) => n.id === id);
      if (targetItem && !targetItem.isRead) {
        setUnreadCount((prev) => Math.max(0, prev - 1));
      }
      setNotifications((prev) => prev.filter((n) => n.id !== id));
      setTotalElements((prev) => Math.max(0, prev - 1));
      setConfirmDeleteId(null);
    } catch (err) {
      console.error("Lỗi khi xóa thông báo:", err);
    }
  };

  const handleLoadMore = () => {
    if (currentPage < totalPages - 1 && !isLoading) {
      fetchNotificationList(currentPage + 1, true);
    }
  };

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
      return `${diffDays} ngày trước`;
    } catch {
      return dateStr;
    }
  };

  const renderLevelBadge = (level: NotificationLevel) => {
    switch (level) {
      case "CAO":
        return (
          <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-semibold bg-rose-100 text-rose-700 border border-rose-200">
            <AlertCircle className="h-3 w-3 text-rose-600" />
            Cao
          </span>
        );
      case "TRUNG_BINH":
        return (
          <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-semibold bg-amber-100 text-amber-700 border border-amber-200">
            <AlertTriangle className="h-3 w-3 text-amber-600" />
            Trung bình
          </span>
        );
      case "THAP":
      default:
        return (
          <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-semibold bg-sky-100 text-sky-700 border border-sky-200">
            <Info className="h-3 w-3 text-sky-600" />
            Thấp
          </span>
        );
    }
  };

  return (
    <div className="relative" ref={popoverRef}>
      {/* Icon chuông thông báo với Badge đỏ */}
      <button
        onClick={() => setIsOpen((prev) => !prev)}
        className="relative rounded-full p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-700 transition"
        title="Trung tâm thông báo"
        type="button"
        aria-label="Thông báo"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute top-1 right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-bold text-white shadow-xs animate-pulse">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {/* Popover / Panel Trung tâm thông báo */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 sm:w-[420px] rounded-xl border border-slate-200 bg-white shadow-2xl z-50 overflow-hidden animate-in fade-in-50 zoom-in-95 duration-100 flex flex-col max-h-[580px]">
          {/* Header Panel */}
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3 bg-slate-50/80">
            <div className="flex items-center gap-2">
              <span className="font-semibold text-slate-800 text-sm">Trung tâm thông báo</span>
              {unreadCount > 0 && (
                <span className="rounded-full bg-rose-100 px-2 py-0.5 text-xs font-semibold text-rose-700">
                  {unreadCount} chưa đọc
                </span>
              )}
            </div>
            <div className="flex items-center gap-2">
              {canManageDedup && (
                <button
                  onClick={() => {
                    setIsOpen(false);
                    setIsDedupModalOpen(true);
                  }}
                  className="flex items-center gap-1 rounded px-1.5 py-0.5 text-xs font-medium text-slate-500 transition hover:bg-slate-100 hover:text-indigo-600"
                  type="button"
                  title="Cấu hình chống gửi trùng thông báo (VT-06)"
                >
                  <Sliders className="h-3.5 w-3.5" />
                  <span className="hidden sm:inline">Chống trùng</span>
                </button>
              )}
              {unreadCount > 0 && (
                <button
                  onClick={handleMarkAllRead}
                  className="flex items-center gap-1 text-xs font-medium text-indigo-600 hover:text-indigo-800 transition"
                  type="button"
                  title="Đánh dấu đọc tất cả"
                >
                  <CheckCheck className="h-3.5 w-3.5" />
                  Đọc tất cả
                </button>
              )}
              <button
                onClick={() => {
                  setIsOpen(false);
                  setIsSettingsOpen(true);
                }}
                className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-700 transition"
                type="button"
                title="Cài đặt thông báo"
              >
                <Settings className="h-4 w-4" />
              </button>
            </div>
          </div>

          {/* Filter Bar */}
          <div className="flex items-center justify-between px-4 py-2 border-b border-slate-100 bg-white text-xs gap-2">
            {/* Status Tabs */}
            <div className="flex items-center gap-1 bg-slate-100 p-0.5 rounded-lg">
              <button
                onClick={() => setStatusFilter("ALL")}
                className={`px-2 py-1 rounded-md transition font-medium ${
                  statusFilter === "ALL"
                    ? "bg-white text-slate-800 shadow-xs"
                    : "text-slate-500 hover:text-slate-700"
                }`}
                type="button"
              >
                Tất cả
              </button>
              <button
                onClick={() => setStatusFilter("UNREAD")}
                className={`px-2 py-1 rounded-md transition font-medium ${
                  statusFilter === "UNREAD"
                    ? "bg-white text-indigo-700 shadow-xs"
                    : "text-slate-500 hover:text-slate-700"
                }`}
                type="button"
              >
                Chưa đọc
              </button>
            </div>

            {/* Level Filter Dropdown */}
            <div className="flex items-center gap-1">
              <select
                value={levelFilter}
                onChange={(e) => setLevelFilter(e.target.value as "ALL" | NotificationLevel)}
                className="text-xs bg-slate-50 border border-slate-200 rounded-md px-2 py-1 text-slate-700 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                <option value="ALL">Mọi mức độ</option>
                <option value="CAO">🔴 Cao</option>
                <option value="TRUNG_BINH">🟡 Trung bình</option>
                <option value="THAP">🔵 Thấp</option>
              </select>
            </div>
          </div>

          {/* List Content */}
          <div className="flex-1 overflow-y-auto divide-y divide-slate-100">
            {isLoading && notifications.length === 0 ? (
              <div className="p-10 text-center text-xs text-slate-400">
                Đang tải thông báo...
              </div>
            ) : notifications.length === 0 ? (
              <div className="p-10 text-center">
                <div className="mx-auto mb-2 flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-slate-400">
                  <Bell className="h-5 w-5" />
                </div>
                <p className="text-xs font-medium text-slate-600">Bạn chưa có thông báo nào</p>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Các cảnh báo quá tải, xung đột và nhắc việc sẽ xuất hiện tại đây.
                </p>
              </div>
            ) : (
              notifications.map((n) => (
                <div
                  key={n.id}
                  onClick={() => handleItemClick(n)}
                  className={`group relative p-3.5 transition flex gap-3 items-start cursor-pointer hover:bg-slate-50 ${
                    !n.isRead ? "bg-indigo-50/40" : ""
                  }`}
                >
                  {/* Left Indicator bar */}
                  <div
                    className={`w-1 self-stretch rounded-full flex-none ${
                      n.level === "CAO"
                        ? "bg-rose-500"
                        : n.level === "TRUNG_BINH"
                        ? "bg-amber-500"
                        : "bg-sky-400"
                    }`}
                  />

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-1 mb-1">
                      <div className="flex items-center gap-1.5 truncate">
                        {renderLevelBadge(n.level)}
                        <span
                          className={`text-xs truncate ${
                            !n.isRead ? "font-bold text-slate-900" : "font-medium text-slate-700"
                          }`}
                        >
                          {n.title}
                        </span>
                      </div>
                      {!n.isRead && (
                        <span className="flex-none h-2 w-2 rounded-full bg-indigo-600" />
                      )}
                    </div>

                    {n.message && (
                      <p className="text-[11px] text-slate-600 line-clamp-2 leading-relaxed">
                        {n.message}
                      </p>
                    )}

                    <div className="flex items-center justify-between mt-1.5 text-[10px] text-slate-400">
                      <div className="flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        <span>{formatTimeAgo(n.createdAt)}</span>
                      </div>

                      <div className="flex items-center gap-2">
                        {n.relatedEntityType && (
                          <span className="flex items-center gap-0.5 text-indigo-600 font-medium group-hover:underline">
                            Xem chi tiết
                            <ExternalLink className="h-2.5 w-2.5" />
                          </span>
                        )}

                        {/* Nút Xóa với Confirm */}
                        {confirmDeleteId === n.id ? (
                          <div
                            className="flex items-center gap-1 bg-white border border-rose-300 rounded px-1.5 py-0.5 shadow-sm"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <span className="text-[10px] text-rose-600 font-medium">Xóa?</span>
                            <button
                              onClick={(e) => handleDeleteItem(n.id, e)}
                              className="text-[10px] font-bold text-rose-700 hover:underline"
                              type="button"
                            >
                              Có
                            </button>
                            <span className="text-slate-300">|</span>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setConfirmDeleteId(null);
                              }}
                              className="text-[10px] text-slate-500 hover:underline"
                              type="button"
                            >
                              Không
                            </button>
                          </div>
                        ) : (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setConfirmDeleteId(n.id);
                            }}
                            className="opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-rose-50 text-slate-400 hover:text-rose-600 transition"
                            title="Xóa thông báo khỏi danh sách"
                            type="button"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Footer with Pagination / Load more */}
          {currentPage < totalPages - 1 && (
            <div className="p-2 border-t border-slate-100 bg-slate-50/50 text-center">
              <button
                onClick={handleLoadMore}
                disabled={isLoading}
                className="text-xs text-indigo-600 hover:text-indigo-800 font-medium inline-flex items-center gap-1 disabled:opacity-50"
                type="button"
              >
                <ChevronDown className="h-3.5 w-3.5" />
                {isLoading ? "Đang tải..." : `Xem thêm (${totalElements - notifications.length} thông báo cũ)`}
              </button>
            </div>
          )}
        </div>
      )}

      {/* Modal cài đặt thông báo (NCL-11-CN-002) */}
      <NotificationSettingsModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
      />

      {/* Modal cấu hình chống gửi trùng thông báo (VT-06) */}
      <NotificationDedupConfigModal
        isOpen={isDedupModalOpen}
        onClose={() => setIsDedupModalOpen(false)}
      />
    </div>
  );
}
