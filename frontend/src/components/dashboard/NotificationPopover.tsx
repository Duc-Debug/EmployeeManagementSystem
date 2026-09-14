import { useState, useEffect, useRef } from "react";
import { Bell, CheckCheck, MessageSquare, AtSign, Clock } from "lucide-react";
import { getMyNotifications, markNotificationRead, markAllNotificationsRead, type NotificationItem } from "@/lib/api/notifications";

interface NotificationPopoverProps {
  onSelectTask?: (taskId: number) => void;
}

export function NotificationPopover({ onSelectTask }: NotificationPopoverProps) {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const popoverRef = useRef<HTMLDivElement>(null);

  const fetchNotifications = async () => {
    try {
      setIsLoading(true);
      const data = await getMyNotifications();
      setNotifications(data || []);
    } catch {
      // Fallback or ignore if not logged in or backend down
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
    // Refresh every 30 seconds
    const interval = setInterval(fetchNotifications, 30000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (popoverRef.current && !popoverRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen]);

  const unreadCount = notifications.filter((n) => !n.read).length;

  const handleMarkAllRead = async () => {
    try {
      await markAllNotificationsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    } catch (e) {
      console.error(e);
    }
  };

  const handleItemClick = async (n: NotificationItem) => {
    if (!n.read) {
      try {
        await markNotificationRead(n.id);
        setNotifications((prev) =>
          prev.map((item) => (item.id === n.id ? { ...item, read: true } : item))
        );
      } catch (e) {
        console.error(e);
      }
    }
    setIsOpen(false);

    if (n.targetType === "TASK" && n.targetId) {
      if (onSelectTask) {
        onSelectTask(n.targetId);
      } else {
        // Dispatch a window custom event so any page listening can open the task drawer
        window.dispatchEvent(
          new CustomEvent("openTaskDiscussion", {
            detail: { taskId: n.targetId },
          })
        );
      }
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

  return (
    <div className="relative" ref={popoverRef}>
      <button
        onClick={() => setIsOpen((prev) => !prev)}
        className="relative rounded-full p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-700 transition"
        title="Thông báo"
        type="button"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute top-1 right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-bold text-white shadow-xs animate-pulse">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 sm:w-96 rounded-xl border border-slate-200 bg-white shadow-xl z-50 overflow-hidden animate-in fade-in-50 zoom-in-95 duration-100">
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3 bg-slate-50/70">
            <div className="flex items-center gap-2">
              <span className="font-semibold text-slate-800 text-sm">Thông báo</span>
              {unreadCount > 0 && (
                <span className="rounded-full bg-indigo-100 px-2 py-0.5 text-xs font-semibold text-indigo-700">
                  {unreadCount} mới
                </span>
              )}
            </div>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllRead}
                className="flex items-center gap-1 text-xs font-medium text-indigo-600 hover:text-indigo-800 transition"
              >
                <CheckCheck className="h-3.5 w-3.5" />
                Đọc tất cả
              </button>
            )}
          </div>

          <div className="max-h-96 overflow-y-auto divide-y divide-slate-100">
            {isLoading && notifications.length === 0 ? (
              <div className="p-8 text-center text-xs text-slate-400">
                Đang tải thông báo...
              </div>
            ) : notifications.length === 0 ? (
              <div className="p-8 text-center">
                <div className="mx-auto mb-2 flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-slate-400">
                  <Bell className="h-5 w-5" />
                </div>
                <p className="text-xs font-medium text-slate-600">Bạn chưa có thông báo nào</p>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Khi đồng nghiệp nhắc tên bạn trong công việc, thông báo sẽ hiển thị tại đây.
                </p>
              </div>
            ) : (
              notifications.map((n) => (
                <button
                  key={n.id}
                  onClick={() => handleItemClick(n)}
                  className={`w-full text-left p-3.5 transition flex gap-3 items-start hover:bg-slate-50 ${
                    !n.read ? "bg-indigo-50/40" : ""
                  }`}
                >
                  <div
                    className={`mt-0.5 flex h-8 w-8 flex-none items-center justify-center rounded-lg ${
                      n.type === "TASK_MENTION"
                        ? "bg-amber-100 text-amber-600"
                        : "bg-indigo-100 text-indigo-600"
                    }`}
                  >
                    {n.type === "TASK_MENTION" ? (
                      <AtSign className="h-4 w-4" />
                    ) : (
                      <MessageSquare className="h-4 w-4" />
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-1 mb-0.5">
                      <p
                        className={`text-xs truncate ${
                          !n.read ? "font-bold text-slate-900" : "font-medium text-slate-700"
                        }`}
                      >
                        {n.title}
                      </p>
                      {!n.read && (
                        <span className="flex-none h-2 w-2 rounded-full bg-indigo-600" />
                      )}
                    </div>
                    {n.content && (
                      <p className="text-[11px] text-slate-500 line-clamp-2 leading-relaxed">
                        "{n.content}"
                      </p>
                    )}
                    <div className="flex items-center gap-1 mt-1 text-[10px] text-slate-400">
                      <Clock className="h-3 w-3" />
                      <span>{formatTimeAgo(n.createdAt)}</span>
                    </div>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
