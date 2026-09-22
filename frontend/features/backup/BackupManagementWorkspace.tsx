"use client";

import { useEffect, useState } from "react";
import {
  BackupItem,
  BackupStatus,
  BackupSummary,
  BackupType,
  deleteBackup,
  fetchBackups,
  fetchBackupSummary,
} from "@/lib/api/backup";
import { CreateBackupModal } from "./CreateBackupModal";
import { TwoStepRestoreModal } from "./TwoStepRestoreModal";
import { BackupScheduleModal } from "./BackupScheduleModal";
import { UploadBackupModal } from "./UploadBackupModal";
import { BackupAuditLogsTable } from "./BackupAuditLogsTable";
import { API_BASE_URL } from "@/lib/api-client";
import { getAuthToken, useAuthUser } from "@/lib/auth-session";
import {
  Database,
  Calendar,
  RotateCcw,
  Download,
  Trash2,
  Upload,
  Plus,
  RefreshCw,
  Search,
  HardDrive,
  Clock,
  ShieldCheck,
  ShieldAlert,
  CheckCircle2,
  XCircle,
  Loader2,
  Copy,
  Check,
  FileText,
  AlertCircle,
} from "lucide-react";

export function BackupManagementWorkspace() {
  const authUser = useAuthUser();
  const [activeTab, setActiveTab] = useState<"backups" | "audit">("backups");
  const [backups, setBackups] = useState<BackupItem[]>([]);
  const [summary, setSummary] = useState<BackupSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Filters
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState<BackupType | "ALL">("ALL");
  const [statusFilter, setStatusFilter] = useState<BackupStatus | "ALL">("ALL");

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isScheduleOpen, setIsScheduleOpen] = useState(false);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [restoreTargetBackup, setRestoreTargetBackup] = useState<BackupItem | null>(null);

  // Notification / Alert
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  const loadData = async () => {
    try {
      setIsLoading(true);
      const [backupsData, summaryData] = await Promise.all([
        fetchBackups(
          typeFilter === "ALL" ? undefined : typeFilter,
          statusFilter === "ALL" ? undefined : statusFilter,
          search
        ),
        fetchBackupSummary(),
      ]);
      setBackups(backupsData);
      setSummary(summaryData);
    } catch (err: unknown) {
      console.error("Lỗi khi tải dữ liệu sao lưu:", err);
      const msg = err instanceof Error ? err.message : "Tải dữ liệu sao lưu thất bại";
      setNotification({ type: "error", message: msg });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (authUser?.roleCode === "VT-06") {
      loadData();
    }
  }, [typeFilter, statusFilter, authUser?.roleCode]);

  if (authUser && authUser.roleCode !== "VT-06") {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center text-center p-6">
        <div className="w-16 h-16 bg-red-100 dark:bg-red-950/60 rounded-full flex items-center justify-center text-red-600 dark:text-red-400 mb-4 shadow-sm">
          <ShieldAlert className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-bold text-slate-900 dark:text-white mb-2">
          Quyền truy cập bị từ chối (403 Forbidden)
        </h2>
        <p className="text-sm text-slate-500 dark:text-slate-400 max-w-md mb-6 leading-relaxed">
          Chức năng <strong>Sao lưu và Phục hồi Dữ liệu</strong> chỉ dành riêng cho tài khoản có vai trò <strong>Quản trị viên hệ thống (VT-06)</strong>. Mọi nỗ lực truy cập trái phép đều được ghi nhận vào nhật ký kiểm toán bảo mật.
        </p>
        <div className="p-3 bg-slate-100 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl text-xs text-slate-600 dark:text-slate-400 font-mono">
          Vai trò hiện tại: {authUser.roleCode} ({authUser.roleName || "Không xác định"})
        </div>
      </div>
    );
  }

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    loadData();
  };

  const handleDownload = (backup: BackupItem) => {
    const token = getAuthToken();
    const url = `${API_BASE_URL}/backups/${backup.id}/download`;
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = backup.fileName;
    // For authenticated download
    fetch(url, {
      headers: {
        Authorization: token ? `Bearer ${token}` : "",
      },
    })
      .then((res) => {
        if (!res.ok) throw new Error("Tải file thất bại");
        return res.blob();
      })
      .then((blob) => {
        const objectUrl = window.URL.createObjectURL(blob);
        anchor.href = objectUrl;
        anchor.click();
        window.URL.revokeObjectURL(objectUrl);
        setNotification({ type: "success", message: `Đã tải về thành công tệp ${backup.fileName}` });
      })
      .catch((err) => {
        setNotification({ type: "error", message: err.message || "Tải về tệp thất bại" });
      });
  };

  const handleDelete = async (backup: BackupItem) => {
    if (!window.confirm(`Bạn có chắc chắn muốn xóa bản sao lưu "${backup.backupCode}" (${backup.title}) không?`)) {
      return;
    }

    try {
      await deleteBackup(backup.id, "Người dùng yêu cầu xóa từ giao diện quản trị");
      setNotification({ type: "success", message: `Đã xóa bản sao lưu ${backup.backupCode} thành công.` });
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Xóa bản sao lưu thất bại";
      setNotification({ type: "error", message: msg });
    }
  };

  const handleCopyChecksum = (code: string) => {
    navigator.clipboard.writeText(code);
    setCopiedCode(code);
    setTimeout(() => setCopiedCode(null), 2000);
  };

  const getStatusBadge = (status: BackupStatus) => {
    switch (status) {
      case "COMPLETED":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>Hoàn tất</span>
          </span>
        );
      case "IN_PROGRESS":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300">
            <Loader2 className="w-3.5 h-3.5 animate-spin" />
            <span>Đang xử lý</span>
          </span>
        );
      case "FAILED":
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-100 dark:bg-red-950/60 text-red-800 dark:text-red-300">
            <XCircle className="w-3.5 h-3.5" />
            <span>Thất bại</span>
          </span>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2.5">
            <Database className="w-7 h-7 text-indigo-600" />
            <span>Sao lưu & Phục hồi Dữ liệu</span>
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Quản trị sao lưu cơ sở dữ liệu hệ thống, cấu hình lịch tự động và khôi phục an toàn dữ liệu kế hoạch nguồn lực.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <button
            type="button"
            className="px-3.5 py-2 border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 rounded-xl text-sm font-medium flex items-center gap-2 transition shadow-sm"
            onClick={() => loadData()}
            disabled={isLoading}
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? "animate-spin" : ""}`} />
            <span>Làm mới</span>
          </button>

          <button
            type="button"
            className="px-3.5 py-2 border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 rounded-xl text-sm font-medium flex items-center gap-2 transition shadow-sm"
            onClick={() => setIsUploadOpen(true)}
          >
            <Upload className="w-4 h-4 text-indigo-500" />
            <span>Tải lên tệp</span>
          </button>

          <button
            type="button"
            className="px-3.5 py-2 border border-indigo-200 dark:border-indigo-900/50 bg-indigo-50/50 dark:bg-indigo-950/40 hover:bg-indigo-100 dark:hover:bg-indigo-900/60 text-indigo-700 dark:text-indigo-300 rounded-xl text-sm font-medium flex items-center gap-2 transition shadow-sm"
            onClick={() => setIsScheduleOpen(true)}
          >
            <Calendar className="w-4 h-4 text-indigo-600" />
            <span>Cấu hình lịch tự động</span>
          </button>

          <button
            type="button"
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-sm font-semibold flex items-center gap-2 transition shadow-sm"
            onClick={() => setIsCreateOpen(true)}
          >
            <Plus className="w-4 h-4" />
            <span>Tạo bản sao lưu ngay</span>
          </button>
        </div>
      </div>

      {/* Global Alert Notification */}
      {notification && (
        <div
          className={`p-4 rounded-xl text-sm flex items-start justify-between gap-3 border shadow-sm transition ${
            notification.type === "success"
              ? "bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-900 text-emerald-800 dark:text-emerald-200"
              : "bg-red-50 dark:bg-red-950/40 border-red-200 dark:border-red-900 text-red-800 dark:text-red-200"
          }`}
        >
          <div className="flex items-center gap-2.5">
            {notification.type === "success" ? (
              <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0" />
            ) : (
              <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0" />
            )}
            <span>{notification.message}</span>
          </div>
          <button
            type="button"
            className="text-xs font-semibold hover:underline"
            onClick={() => setNotification(null)}
          >
            Đóng
          </button>
        </div>
      )}

      {/* KPI Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Backups */}
        <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Tổng số bản sao
            </span>
            <div className="p-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600">
              <Database className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-slate-900 dark:text-white">
              {summary ? summary.totalBackups : "—"}
            </span>
            <span className="text-xs text-slate-500 dark:text-slate-400">bản lưu trữ</span>
          </div>
        </div>

        {/* Total File Size */}
        <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Dung lượng lưu trữ
            </span>
            <div className="p-2 rounded-xl bg-blue-50 dark:bg-blue-950/50 text-blue-600">
              <HardDrive className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-slate-900 dark:text-white">
              {summary ? summary.formattedTotalSize : "—"}
            </span>
            <span className="text-xs text-slate-500 dark:text-slate-400">trên máy chủ</span>
          </div>
        </div>

        {/* Latest Backup */}
        <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Bản sao lưu gần nhất
            </span>
            <div className="p-2 rounded-xl bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600">
              <Clock className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-2">
            {summary?.latestCompletedBackup ? (
              <div>
                <span className="text-sm font-bold text-slate-900 dark:text-white block truncate">
                  {summary.latestCompletedBackup.title}
                </span>
                <span className="text-xs text-slate-500 dark:text-slate-400">
                  {new Date(summary.latestCompletedBackup.createdAt).toLocaleDateString("vi-VN")} lúc{" "}
                  {new Date(summary.latestCompletedBackup.createdAt).toLocaleTimeString("vi-VN", {
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </span>
              </div>
            ) : (
              <span className="text-sm text-slate-400 font-medium">Chưa có bản sao lưu</span>
            )}
          </div>
        </div>

        {/* Schedule Status */}
        <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Lịch sao lưu tự động
            </span>
            <div className="p-2 rounded-xl bg-purple-50 dark:bg-purple-950/50 text-purple-600">
              <Calendar className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-2">
            {summary?.schedule?.isEnabled ? (
              <div>
                <span className="inline-flex items-center gap-1 text-xs font-semibold text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/50 px-2 py-0.5 rounded-md">
                  <CheckCircle2 className="w-3 h-3" />
                  <span>Đang bật ({summary.schedule.frequencyLabel})</span>
                </span>
                <span className="text-xs text-slate-500 dark:text-slate-400 block mt-1">
                  Giờ chạy: {summary.schedule.scheduledTime}
                </span>
              </div>
            ) : (
              <div>
                <span className="inline-flex items-center gap-1 text-xs font-semibold text-slate-600 dark:text-slate-400 bg-slate-100 dark:bg-slate-800 px-2 py-0.5 rounded-md">
                  <XCircle className="w-3 h-3" />
                  <span>Đang tắt tự động</span>
                </span>
                <span className="text-xs text-slate-500 dark:text-slate-400 block mt-1">
                  Chỉ sao lưu khi yêu cầu
                </span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="border-b border-slate-200 dark:border-slate-800">
        <nav className="flex space-x-8">
          <button
            type="button"
            className={`pb-4 px-1 border-b-2 font-medium text-sm transition flex items-center gap-2 ${
              activeTab === "backups"
                ? "border-indigo-600 text-indigo-600 dark:text-indigo-400 font-semibold"
                : "border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300 dark:text-slate-400"
            }`}
            onClick={() => setActiveTab("backups")}
          >
            <Database className="w-4 h-4" />
            <span>Danh sách bản sao lưu</span>
            <span className="px-2 py-0.5 rounded-full text-xs bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-bold">
              {backups.length}
            </span>
          </button>

          <button
            type="button"
            className={`pb-4 px-1 border-b-2 font-medium text-sm transition flex items-center gap-2 ${
              activeTab === "audit"
                ? "border-indigo-600 text-indigo-600 dark:text-indigo-400 font-semibold"
                : "border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300 dark:text-slate-400"
            }`}
            onClick={() => setActiveTab("audit")}
          >
            <ShieldCheck className="w-4 h-4" />
            <span>Nhật ký kiểm toán & An toàn</span>
          </button>
        </nav>
      </div>

      {/* Tab 1: Backups List */}
      {activeTab === "backups" && (
        <div className="space-y-4">
          {/* Filters Bar */}
          <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3 bg-white dark:bg-slate-900 p-3.5 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm">
            <form onSubmit={handleSearchSubmit} className="relative flex-1 max-w-md">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                className="w-full pl-9 pr-3 py-2 border border-slate-300 dark:border-slate-700 rounded-xl bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                placeholder="Tìm theo mã bản sao, tiêu đề..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </form>

            <div className="flex flex-wrap items-center gap-3">
              <div className="flex items-center gap-2">
                <span className="text-xs font-medium text-slate-500">Loại:</span>
                <select
                  className="px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-xl bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-white text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  value={typeFilter}
                  onChange={(e) => setTypeFilter(e.target.value as BackupType | "ALL")}
                >
                  <option value="ALL">Tất cả loại</option>
                  <option value="FULL">Toàn bộ hệ thống (FULL)</option>
                  <option value="RESOURCE_PLAN">Kế hoạch nguồn lực (PLAN)</option>
                </select>
              </div>

              <div className="flex items-center gap-2">
                <span className="text-xs font-medium text-slate-500">Trạng thái:</span>
                <select
                  className="px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-xl bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-white text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value as BackupStatus | "ALL")}
                >
                  <option value="ALL">Tất cả trạng thái</option>
                  <option value="COMPLETED">Hoàn tất</option>
                  <option value="IN_PROGRESS">Đang xử lý</option>
                  <option value="FAILED">Thất bại</option>
                </select>
              </div>
            </div>
          </div>

          {/* Table */}
          <div className="border border-slate-200 dark:border-slate-800 rounded-2xl overflow-hidden bg-white dark:bg-slate-950 shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="border-b border-slate-200 dark:border-slate-800 bg-slate-50/75 dark:bg-slate-900/75 text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    <th className="py-3.5 px-4">Mã bản sao</th>
                    <th className="py-3.5 px-4">Tiêu đề & Mục đích</th>
                    <th className="py-3.5 px-4">Phân loại</th>
                    <th className="py-3.5 px-4">Thời điểm & Loại</th>
                    <th className="py-3.5 px-4">Dung lượng</th>
                    <th className="py-3.5 px-4">Mã băm SHA-256</th>
                    <th className="py-3.5 px-4">Trạng thái</th>
                    <th className="py-3.5 px-4 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                  {isLoading ? (
                    <tr>
                      <td colSpan={8} className="py-16 text-center text-slate-500">
                        <Loader2 className="w-8 h-8 animate-spin mx-auto mb-2 text-indigo-600" />
                        <span className="text-sm">Đang tải danh sách bản sao lưu...</span>
                      </td>
                    </tr>
                  ) : backups.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="py-16 text-center text-slate-500">
                        <Database className="w-10 h-10 mx-auto mb-3 text-slate-300 dark:text-slate-600" />
                        <h4 className="text-base font-semibold text-slate-700 dark:text-slate-300">
                          Chưa có bản sao lưu nào
                        </h4>
                        <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm mx-auto">
                          Tạo bản sao lưu đầu tiên để bảo vệ dữ liệu kế hoạch nguồn lực của đơn vị.
                        </p>
                        <button
                          type="button"
                          className="mt-4 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold inline-flex items-center gap-2 transition"
                          onClick={() => setIsCreateOpen(true)}
                        >
                          <Plus className="w-4 h-4" />
                          <span>Tạo bản sao lưu ngay</span>
                        </button>
                      </td>
                    </tr>
                  ) : (
                    backups.map((b) => (
                      <tr key={b.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-900/50 transition">
                        <td className="py-3.5 px-4 whitespace-nowrap">
                          <span className="font-mono text-xs font-semibold text-slate-900 dark:text-white px-2 py-1 bg-slate-100 dark:bg-slate-800 rounded-md">
                            {b.backupCode}
                          </span>
                        </td>
                        <td className="py-3.5 px-4 max-w-xs">
                          <span className="font-medium text-slate-900 dark:text-white block truncate">
                            {b.title}
                          </span>
                          {b.description && (
                            <span className="text-xs text-slate-500 dark:text-slate-400 block truncate mt-0.5">
                              {b.description}
                            </span>
                          )}
                          {b.errorMessage && (
                            <span className="text-xs text-red-600 dark:text-red-400 block truncate mt-0.5">
                              Lỗi: {b.errorMessage}
                            </span>
                          )}
                        </td>
                        <td className="py-3.5 px-4 whitespace-nowrap">
                          <span
                            className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${
                              b.backupType === "FULL"
                                ? "bg-blue-100 dark:bg-blue-950/60 text-blue-800 dark:text-blue-300"
                                : "bg-purple-100 dark:bg-purple-950/60 text-purple-800 dark:text-purple-300"
                            }`}
                          >
                            {b.backupTypeLabel}
                          </span>
                        </td>
                        <td className="py-3.5 px-4 whitespace-nowrap text-xs">
                          <span className="text-slate-900 dark:text-white font-medium block">
                            {new Date(b.createdAt).toLocaleDateString("vi-VN")}
                          </span>
                          <span className="text-slate-500 dark:text-slate-400 block text-[11px]">
                            {new Date(b.createdAt).toLocaleTimeString("vi-VN")} •{" "}
                            {b.isAutomatic ? (
                              <span className="text-purple-600 dark:text-purple-400 font-medium">Tự động</span>
                            ) : (
                              <span className="text-slate-600 dark:text-slate-400">Thủ công</span>
                            )}
                          </span>
                        </td>
                        <td className="py-3.5 px-4 whitespace-nowrap font-mono text-xs font-medium text-slate-900 dark:text-white">
                          {b.formattedFileSize}
                        </td>
                        <td className="py-3.5 px-4 whitespace-nowrap">
                          {b.checksum ? (
                            <div className="flex items-center gap-1.5 font-mono text-xs text-slate-600 dark:text-slate-400">
                              <span title={b.checksum}>
                                {b.checksum.substring(0, 10)}...
                              </span>
                              <button
                                type="button"
                                className="p-1 hover:bg-slate-200 dark:hover:bg-slate-800 rounded transition text-slate-500"
                                onClick={() => handleCopyChecksum(b.checksum!)}
                                title="Sao chép toàn bộ mã SHA-256"
                              >
                                {copiedCode === b.checksum ? (
                                  <Check className="w-3 h-3 text-emerald-600" />
                                ) : (
                                  <Copy className="w-3 h-3" />
                                )}
                              </button>
                            </div>
                          ) : (
                            <span className="text-xs text-slate-400 font-mono">—</span>
                          )}
                        </td>
                        <td className="py-3.5 px-4 whitespace-nowrap">
                          {getStatusBadge(b.status)}
                        </td>
                        <td className="py-3.5 px-4 whitespace-nowrap text-right">
                          <div className="flex items-center justify-end gap-1">
                            {/* Restore button */}
                            <button
                              type="button"
                              className="p-1.5 rounded-lg text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition disabled:opacity-40 disabled:cursor-not-allowed"
                              disabled={b.status !== "COMPLETED"}
                              onClick={() => setRestoreTargetBackup(b)}
                              title={
                                b.status === "COMPLETED"
                                  ? "Phục hồi dữ liệu về bản sao lưu này"
                                  : "Không thể phục hồi từ bản sao lưu bị lỗi hoặc chưa hoàn tất"
                              }
                            >
                              <RotateCcw className="w-4 h-4" />
                            </button>

                            {/* Download button */}
                            <button
                              type="button"
                              className="p-1.5 rounded-lg text-indigo-600 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 transition disabled:opacity-40 disabled:cursor-not-allowed"
                              disabled={b.status !== "COMPLETED"}
                              onClick={() => handleDownload(b)}
                              title="Tải tệp sao lưu về máy"
                            >
                              <Download className="w-4 h-4" />
                            </button>

                            {/* Delete button */}
                            <button
                              type="button"
                              className="p-1.5 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/40 transition"
                              onClick={() => handleDelete(b)}
                              title="Xóa bản sao lưu"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Tab 2: Audit Logs */}
      {activeTab === "audit" && <BackupAuditLogsTable />}

      {/* Dialogs */}
      <CreateBackupModal
        open={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={(created) => {
          setNotification({
            type: "success",
            message: `Tạo bản sao lưu ${created.backupCode} thành công (${created.formattedFileSize})!`,
          });
          loadData();
        }}
      />

      <TwoStepRestoreModal
        backup={restoreTargetBackup}
        open={Boolean(restoreTargetBackup)}
        onClose={() => setRestoreTargetBackup(null)}
        onSuccess={() => {
          setNotification({
            type: "success",
            message: `Phục hồi cơ sở dữ liệu thành công về thời điểm bản sao lưu! Hệ thống đã ghi nhận nhật ký kiểm toán.`,
          });
          loadData();
        }}
      />

      <BackupScheduleModal
        open={isScheduleOpen}
        onClose={() => setIsScheduleOpen(false)}
        onSuccess={(schedule) => {
          setNotification({
            type: "success",
            message: `Cập nhật cấu hình lịch sao lưu tự động thành công!`,
          });
          loadData();
        }}
      />

      <UploadBackupModal
        open={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        onSuccess={(uploaded) => {
          setNotification({
            type: "success",
            message: `Tải lên tệp bản sao lưu ${uploaded.backupCode} thành công!`,
          });
          loadData();
        }}
      />
    </div>
  );
}
