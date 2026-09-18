import { useState, useEffect, useCallback, useMemo } from "react";
import {
  getMyUpcomingDueTasks,
  triggerScanDueReminders,
  type UpcomingDueTaskResult,
  type TaskDueReminderScanResult,
} from "@/lib/api/task-due-reminders";

export interface UseUpcomingDueTasksResult {
  tasks: UpcomingDueTaskResult[];
  loading: boolean;
  refreshing: boolean;
  error: string | null;
  criticalCount: number;
  todayCount: number;
  reload: (silent?: boolean) => Promise<void>;
  triggerManualScan: (scanDate?: string) => Promise<TaskDueReminderScanResult>;
}

/**
 * Custom Hook quản lý trạng thái tải dữ liệu công việc sắp đến hạn (NCL-11-CN-004).
 * Cung cấp danh sách công việc, số lượng khẩn cấp và các thao tác làm mới/rà soát thủ công.
 */
export function useUpcomingDueTasks(): UseUpcomingDueTasksResult {
  const [tasks, setTasks] = useState<UpcomingDueTaskResult[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async (silent = false) => {
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
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const criticalCount = useMemo(() => {
    return tasks.filter((t) => t.daysRemaining <= 1).length;
  }, [tasks]);

  const todayCount = useMemo(() => {
    return tasks.filter((t) => t.daysRemaining <= 0).length;
  }, [tasks]);

  const triggerManualScan = useCallback(async (scanDate?: string) => {
    const result = await triggerScanDueReminders(scanDate);
    // Sau khi rà soát, nạp lại danh sách công việc của tôi
    await reload(true);
    return result;
  }, [reload]);

  return {
    tasks,
    loading,
    refreshing,
    error,
    criticalCount,
    todayCount,
    reload,
    triggerManualScan,
  };
}