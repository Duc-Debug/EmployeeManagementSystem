import { useState, useEffect, useCallback, useMemo } from "react";
import { useAuthUser } from "@/lib/auth-session";
import {
  getMyUpcomingDueTasks,
  triggerScanDueReminders,
  type UpcomingDueTaskResult,
  type TaskDueReminderScanResult,
} from "@/lib/api/task-due-reminders";

export type DueTaskFilterTab = "ALL" | "CRITICAL" | "UPCOMING_DAYS";

export interface UseUpcomingDueTasksResult {
  tasks: UpcomingDueTaskResult[];
  filteredTasks: UpcomingDueTaskResult[];
  activeFilter: DueTaskFilterTab;
  setActiveFilter: (filter: DueTaskFilterTab) => void;
  loading: boolean;
  refreshing: boolean;
  error: string | null;
  criticalCount: number;
  todayCount: number;
  isSpecialist: boolean;
  reload: (silent?: boolean) => Promise<void>;
  triggerManualScan: (scanDate?: string) => Promise<TaskDueReminderScanResult>;
}

/**
 * Custom Hook quản lý trạng thái tải dữ liệu công việc sắp đến hạn (NCL-11-CN-004).
 * Cung cấp danh sách công việc, số lượng khẩn cấp, bộ lọc nhanh và kiểm soát vai trò VT-04.
 */
export function useUpcomingDueTasks(): UseUpcomingDueTasksResult {
  const currentUser = useAuthUser();
  const normalizedRole = currentUser?.roleCode ? currentUser.roleCode.toUpperCase().replace(/_/g, "-") : "";

  // Phân quyền TC-03: Chỉ Nhân viên chuyên môn (VT-04) mới có quyền truy cập
  const isSpecialist = useMemo(() => {
    if (!currentUser) return false;
    return ["VT-04", "ROLE-EMPLOYEE", "EMPLOYEE", "MEMBER", "DEVELOPER"].includes(normalizedRole) ||
      (currentUser.roleName ? currentUser.roleName.toLowerCase().includes("chuyên môn") || currentUser.roleName.toLowerCase().includes("nhân viên") : false);
  }, [currentUser, normalizedRole]);

  const [tasks, setTasks] = useState<UpcomingDueTaskResult[]>([]);
  const [activeFilter, setActiveFilter] = useState<DueTaskFilterTab>("ALL");
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async (silent = false) => {
    // Nếu không phải VT-04, tránh gọi API ngầm để không gây lỗi 403 và không ghi rác vào audit_logs
    if (!isSpecialist) {
      setLoading(false);
      setRefreshing(false);
      setTasks([]);
      return;
    }

    if (silent) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setError(null);
    try {
      const data = await getMyUpcomingDueTasks();
      setTasks(data);
    } catch (err: unknown) {
      const message =
        err instanceof Error
          ? err.message
          : "Không thể tải danh sách công việc sắp đến hạn.";
      setError(message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [isSpecialist]);

  useEffect(() => {
    reload();
  }, [reload]);

  const criticalCount = useMemo(() => {
    return tasks.filter((t) => t.daysRemaining <= 1).length;
  }, [tasks]);

  const todayCount = useMemo(() => {
    return tasks.filter((t) => t.daysRemaining <= 0).length;
  }, [tasks]);

  // Lọc công việc theo tab
  const filteredTasks = useMemo(() => {
    if (activeFilter === "CRITICAL") {
      return tasks.filter((t) => t.daysRemaining <= 1);
    }
    if (activeFilter === "UPCOMING_DAYS") {
      return tasks.filter((t) => t.daysRemaining >= 2);
    }
    return tasks;
  }, [tasks, activeFilter]);

  const triggerManualScan = useCallback(async (scanDate?: string) => {
    const result = await triggerScanDueReminders(scanDate);
    // Sau khi rà soát, nạp lại danh sách công việc
    await reload(true);
    return result;
  }, [reload]);

  return {
    tasks,
    filteredTasks,
    activeFilter,
    setActiveFilter,
    loading,
    refreshing,
    error,
    criticalCount,
    todayCount,
    isSpecialist,
    reload,
    triggerManualScan,
  };
}