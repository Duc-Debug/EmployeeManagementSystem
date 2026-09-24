"use client";

import { useEffect, useState, useMemo } from "react";
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
  ShieldCheck,
  User,
  Globe,
  SlidersHorizontal,
} from "lucide-react";
import { cn } from "@/lib/utils";

export function BackupAuditLogsTable() {
  const [logs, setLogs] = useState<BackupAuditLog[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");

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

  const filteredLogs = useMemo(() => {
    return logs.filter((log) => {
      if (statusFilter !== "ALL" && log.status !== statusFilter) return false;
      if (!search.trim()) return true;
      const s = search.toLowerCase();
      return (
        log.action.toLowerCase().includes(s) ||
        (log.actionDescription && log.actionDescription.toLowerCase().includes(s)) ||
        (log.userEmail && log.userEmail.toLowerCase().includes(s)) ||
        (log.reason && log.reason.toLowerCase().includes(s)) ||
        (log.details && log.details.toLowerCase().includes(s)) ||
        (log.ipAddress && log.ipAddress.toLowerCase().includes(s))
      );
    });
  }, [logs, search, statusFilter]);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "SUCCESS":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
            <span>Thành công</span>
          </span>
        );
      case "FORBIDDEN":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg text-[11px] font-bold bg-rose-50 text-rose-700 border border-rose-200">
            <ShieldAlert className="w-3 h-3 text-rose-600" />
            <span>Chặn 403 (Không có quyền)</span>
          </span>
        );
      case "FAILED":
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg text-[11px] font-semibold bg-amber-50 text-amber-800 border border-amber-200">
            <XCircle className="w-3 h-3 text-amber-600" />
            <span>Thất bại</span>
          </span>
        );
    }
  };

  const getActionBadge = (action: string, desc: string) => {
    let color = "bg-slate-100 text-slate-700 border border-slate-200";
    if (action.includes("CREATE") || action.includes("UPLOAD")) {
      color = "bg-indigo-50 text-indigo-700 border border-indigo-200";
    } else if (action.includes("RESTORE")) {
      color = "bg-rose-50 text-rose-700 border border-rose-200";
    } else if (action.includes("DELETE")) {
      color = "bg-amber-50 text-amber-800 border border-amber-200";
    } else if (action.includes("DENIED")) {
      color = "bg-red-100 text-red-800 border border-red-300 font-bold";
    }

    return (
      <span className={cn("inline-block px-2.5 py-0.5 rounded-md text-[11px] font-semibold", color)}>
        {desc || action}
      </span>
    );
  };

  return (
    <div className="space-y-3">
      {/* Search & Filter Header */}
      <div className="flex flex-col sm:flex-row justify-between items-stretch sm:items-center gap-2.5 rounded-xl border border-slate-200 bg-white p-2.5 shadow-2xs">
        <div className="relative flex-1 max-w-sm">
          <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            className="w-full pl-8 pr-3 py-1.5 border border-slate-200 rounded-lg bg-slate-50/50 text-slate-900 text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none placeholder:text-slate-400"
            placeholder="Tìm theo người dùng, hành động, lý do, IP..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1.5">
            <SlidersHorizontal className="w-3 h-3 text-slate-400" />
            <span className="text-[11px] font-medium text-slate-500">Trạng thái:</span>
            <select
              className="px-2.5 py-1 border border-slate-200 rounded-lg bg-slate-50/50 text-slate-800 text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:outline-none cursor-pointer"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="ALL">Tất cả</option>
              <option value="SUCCESS">Thành công</option>
              <option value="FORBIDDEN">Bị chặn (403)</option>
              <option value="FAILED">Thất bại</option>
            </select>
          </div>

          <button
            type="button"
            className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-2xs cursor-pointer disabled:opacity-50"
            onClick={loadLogs}
            disabled={isLoading}
            title="Tải lại nhật ký"
          >
            <RefreshCw className={`h-3 w-3 ${isLoading ? "animate-spin text-indigo-600" : "text-slate-500"}`} />
            <span>Làm mới</span>
          </button>
        </div>
      </div>

      {/* Audit Log Table Container */}
      <div className="rounded-2xl border border-slate-200 bg-white shadow-2xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                <th className="py-2.5 px-3.5">Thời điểm</th>
                <th className="py-2.5 px-3.5">Người thực hiện</th>
                <th className="py-2.5 px-3.5">Hành động</th>
                <th className="py-2.5 px-3.5">Trạng thái</th>
                <th className="py-2.5 px-3.5">Địa chỉ IP</th>
                <th className="py-2.5 px-3.5">Nội dung & Lý do giải trình</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-500">
                    <Loader2 className="w-6 h-6 animate-spin mx-auto mb-2 text-indigo-600" />
                    <span className="text-xs font-medium">Đang nạp nhật ký kiểm toán...</span>
                  </td>
                </tr>
              ) : filteredLogs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-500">
                    <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-50 border border-slate-200 text-slate-400 mx-auto mb-2">
                      <Clock className="w-6 h-6" />
                    </div>
                    <h4 className="text-sm font-bold text-slate-800">
                      Không có bản ghi nhật ký phù hợp
                    </h4>
                    <p className="text-xs text-slate-400 mt-0.5">
                      Thử xóa từ khóa tìm kiếm hoặc làm mới dữ liệu.
                    </p>
                  </td>
                </tr>
              ) : (
                filteredLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-indigo-50/20 transition-colors">
                    {/* Timestamp */}
                    <td className="py-2.5 px-3.5 whitespace-nowrap font-mono text-[11px] text-slate-600">
                      {new Date(log.createdAt).toLocaleString("vi-VN")}
                    </td>

                    {/* User */}
                    <td className="py-2.5 px-3.5 whitespace-nowrap">
                      <div className="flex items-center gap-1.5">
                        <div className="flex h-5 w-5 items-center justify-center rounded-full bg-slate-100 text-slate-600">
                          <User className="h-3 w-3" />
                        </div>
                        <span className="font-semibold text-slate-900 text-xs">
                          {log.userEmail || (log.userId ? `User #${log.userId}` : "Hệ thống (Scheduler)")}
                        </span>
                      </div>
                    </td>

                    {/* Action */}
                    <td className="py-2.5 px-3.5 whitespace-nowrap">
                      {getActionBadge(log.action, log.actionDescription)}
                    </td>

                    {/* Status */}
                    <td className="py-2.5 px-3.5 whitespace-nowrap">
                      {getStatusBadge(log.status)}
                    </td>

                    {/* IP */}
                    <td className="py-2.5 px-3.5 whitespace-nowrap text-slate-500 font-mono text-[11px]">
                      <div className="flex items-center gap-1">
                        <Globe className="h-3 w-3 text-slate-400" />
                        <span>{log.ipAddress || "127.0.0.1"}</span>
                      </div>
                    </td>

                    {/* Reason & Details */}
                    <td className="py-2.5 px-3.5 text-xs text-slate-700 max-w-sm">
                      <span className="font-medium block text-slate-900 truncate" title={log.reason}>
                        {log.reason || "—"}
                      </span>
                      {log.details && (
                        <span className="text-slate-500 block text-[11px] font-mono truncate mt-0.5" title={log.details}>
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

        {/* Footer */}
        <div className="flex items-center justify-between border-t border-slate-100 bg-slate-50/50 px-4 py-2 text-[11px] text-slate-500">
          <div>
            Tổng số <span className="font-semibold text-slate-900">{filteredLogs.length}</span> sự kiện kiểm toán bảo mật
          </div>
          <div className="flex items-center gap-1 text-slate-400">
            <ShieldCheck className="h-3 w-3 text-indigo-600" />
            <span>Bảo vệ quyền truy cập và kiểm soát thay đổi bất biến</span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default BackupAuditLogsTable;
