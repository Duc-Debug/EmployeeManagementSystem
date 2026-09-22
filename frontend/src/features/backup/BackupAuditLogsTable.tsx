"use client";

import { useEffect, useState } from "react";
import { fetchBackupAuditLogs } from "@/lib/api/backup";
import type { BackupAuditLog } from "@/lib/api/backup";
import {
  ShieldAlert,
  CheckCircle2,
  XCircle,
  Clock,
  RefreshCw,
  Search,
  Loader2,
} from "lucide-react";

export function BackupAuditLogsTable() {
  const [logs, setLogs] = useState<BackupAuditLog[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState("");

  const loadLogs = async () => {
    try {
      setIsLoading(true);
      const data = await fetchBackupAuditLogs();
      setLogs(data);
    } catch (err) {
      console.error("Lỗi khi tải nhật ký kiểm toán:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
  }, []);

  const filteredLogs = logs.filter((log) => {
    if (!search.trim()) return true;
    const s = search.toLowerCase();
    return (
      log.action.toLowerCase().includes(s) ||
      log.actionDescription?.toLowerCase().includes(s) ||
      log.userEmail?.toLowerCase().includes(s) ||
      log.reason?.toLowerCase().includes(s) ||
      log.details?.toLowerCase().includes(s) ||
      log.ipAddress?.toLowerCase().includes(s)
    );
  });

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "SUCCESS":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300">
            <CheckCircle2 className="w-3 h-3" />
            <span>Thành công</span>
          </span>
        );
      case "FORBIDDEN":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 dark:bg-red-950/60 text-red-800 dark:text-red-300">
            <ShieldAlert className="w-3 h-3" />
            <span>Bị chặn (403)</span>
          </span>
        );
      case "FAILED":
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-100 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300">
            <XCircle className="w-3 h-3" />
            <span>Thất bại</span>
          </span>
        );
    }
  };

  const getActionBadge = (action: string, desc: string) => {
    let color = "bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-300";
    if (action.includes("CREATE") || action.includes("UPLOAD")) {
      color = "bg-indigo-100 dark:bg-indigo-950/60 text-indigo-800 dark:text-indigo-300";
    } else if (action.includes("RESTORE")) {
      color = "bg-rose-100 dark:bg-rose-950/60 text-rose-800 dark:text-rose-300";
    } else if (action.includes("DELETE")) {
      color = "bg-amber-100 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300";
    } else if (action.includes("DENIED")) {
      color = "bg-red-100 dark:bg-red-950/60 text-red-800 dark:text-red-300 font-semibold";
    }

    return (
      <span className={`inline-block px-2.5 py-1 rounded-lg text-xs font-medium ${color}`}>
        {desc || action}
      </span>
    );
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row justify-between items-stretch sm:items-center gap-3">
        <div className="relative flex-1 max-w-md">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            className="w-full pl-9 pr-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            placeholder="Tìm kiếm theo người dùng, hành động, lý do, IP..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <button
          type="button"
          className="px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 text-sm font-medium flex items-center justify-center gap-2 transition"
          onClick={loadLogs}
          disabled={isLoading}
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? "animate-spin" : ""}`} />
          <span>Làm mới nhật ký</span>
        </button>
      </div>

      <div className="border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden bg-white dark:bg-slate-950 shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="border-b border-slate-200 dark:border-slate-800 bg-slate-50/75 dark:bg-slate-900/75 text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                <th className="py-3 px-4">Thời điểm</th>
                <th className="py-3 px-4">Người thực hiện</th>
                <th className="py-3 px-4">Hành động</th>
                <th className="py-3 px-4">Trạng thái</th>
                <th className="py-3 px-4">Địa chỉ IP</th>
                <th className="py-3 px-4">Nội dung & Lý do giải trình</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-500">
                    <Loader2 className="w-6 h-6 animate-spin mx-auto mb-2 text-indigo-600" />
                    <span>Đang nạp nhật ký kiểm toán...</span>
                  </td>
                </tr>
              ) : filteredLogs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-500">
                    <Clock className="w-8 h-8 mx-auto mb-2 text-slate-400" />
                    <span>Không có bản ghi nhật ký nào phù hợp.</span>
                  </td>
                </tr>
              ) : (
                filteredLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-900/50 transition">
                    <td className="py-3 px-4 whitespace-nowrap text-xs text-slate-600 dark:text-slate-400 font-mono">
                      {new Date(log.createdAt).toLocaleString("vi-VN")}
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap text-xs font-medium text-slate-900 dark:text-white">
                      {log.userEmail || (log.userId ? `User #${log.userId}` : "Hệ thống (Auto)")}
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      {getActionBadge(log.action, log.actionDescription)}
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      {getStatusBadge(log.status)}
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap text-xs font-mono text-slate-500 dark:text-slate-400">
                      {log.ipAddress || "127.0.0.1"}
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-700 dark:text-slate-300 max-w-xs truncate">
                      <span className="font-medium block">{log.reason || "—"}</span>
                      {log.details && (
                        <span className="text-slate-500 dark:text-slate-400 block text-[11px] font-mono truncate">
                          {log.details}
                        </span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
