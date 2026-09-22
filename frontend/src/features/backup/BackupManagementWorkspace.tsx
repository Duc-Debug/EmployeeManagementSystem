"use client";

import { useEffect, useState, useMemo } from "react";
import {
  deleteBackup,
  fetchBackups,
  fetchBackupSummary,
} from "@/lib/api/backup";
import type {
  BackupItem,
  BackupStatus,
  BackupSummary,
  BackupType,
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
  AlertCircle,
  Layers,
  Info,
  SlidersHorizontal,
  FileCheck2,
} from "lucide-react";
import { cn } from "@/lib/utils";

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

  // Client-side quick filter for search keyword
  const filteredBackups = useMemo(() => {
    if (!search.trim()) return backups;
    const kw = search.trim().toLowerCase();
    return backups.filter(
      (b) =>
        b.backupCode.toLowerCase().includes(kw) ||
        b.title.toLowerCase().includes(kw) ||
        (b.description && b.description.toLowerCase().includes(kw)) ||
        (b.checksum && b.checksum.toLowerCase().includes(kw))
    );
  }, [backups, search]);

  if (authUser && authUser.roleCode !== "VT-06") {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center text-center p-6">
        <div className="w-16 h-16 bg-rose-50 border border-rose-200 rounded-2xl flex items-center justify-center text-rose-600 mb-4 shadow-2xs">
          <ShieldAlert className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-bold text-slate-900 mb-1.5">
          Quyền truy cập bị từ chối (403 Forbidden)
        </h2>
        <p className="text-xs text-slate-500 max-w-md mb-6 leading-relaxed">
          Chức năng <strong>Sao lưu và Phục hồi Dữ liệu</strong> chỉ dành riêng cho tài khoản có vai trò <strong>Quản trị viên hệ thống (VT-06)</strong>. Mọi nỗ lực truy cập trái phép đều được ghi nhận vào nhật ký kiểm toán bảo mật.
        </p>
        <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-600 font-mono">
          Vai trò hiện tại: <span className="font-bold text-slate-900">{authUser.roleCode}</span> ({authUser.roleName || "Không xác định"})
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

    fetch(url, {
      headers: {
        Authorization: token ? `Bearer ${token}` : "",
      },
    })
      .then((res) => {
        if (!res.ok) throw new Error("Tải file thất bại hoặc phiên làm việc đã hết hạn");
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
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
            <span>Hoàn tất</span>
          </span>
        );
      case "IN_PROGRESS":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-semibold bg-amber-50 text-amber-700 border border-amber-200">
            <Loader2 className="w-3 h-3 animate-spin text-amber-600" />
            <span>Đang xử lý</span>
          </span>
        );
      case "FAILED":
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-semibold bg-rose-50 text-rose-700 border border-rose-200">
            <XCircle className="w-3 h-3 text-rose-600" />
            <span>Thất bại</span>
          </span>
        );
    }
  };

  const getBackupTypeBadge = (type: BackupType, code: string) => {
    if (code.startsWith("SAFETY-PRE-RESTORE")) {
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-bold bg-amber-50 text-amber-800 border border-amber-200">
          <ShieldCheck className="w-3 h-3 text-amber-600" />
          <span>Điểm an toàn</span>
        </span>
      );
    }
    if (type === "FULL") {
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200/80">
          <Layers className="w-3 h-3 text-indigo-600" />
          <span>Toàn bộ hệ thống</span>
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-bold bg-purple-50 text-purple-700 border border-purple-200/80">
        <Database className="w-3 h-3 text-purple-600" />
        <span>Kế hoạch nguồn lực</span>
      </span>
    );
  };

  return (
    <div className="flex flex-col h-full space-y-4">
      {/* Header Clean White */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-3">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-50 border border-indigo-100 text-indigo-600">
              <Database className="h-4 w-4" />
            </div>
            <span>Sao lưu & Phục hồi Dữ liệu Hệ thống</span>
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Quản trị sao lưu snapshot dữ liệu hệ thống, bảo toàn kế hoạch nguồn lực, cấu hình lịch tự động và khôi phục an toàn 2 bước.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-2xs cursor-pointer disabled:opacity-50"
            onClick={() => loadData()}
            disabled={isLoading}
            title="Làm mới danh sách"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? "animate-spin text-indigo-600" : "text-slate-500"}`} />
            <span>Làm mới</span>
          </button>

          <button
            type="button"
            className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-2xs cursor-pointer"
            onClick={() => setIsUploadOpen(true)}
          >
            <Upload className="h-3.5 w-3.5 text-indigo-600" />
            <span>Tải lên tệp</span>
          </button>

          <button
            type="button"
            className="inline-flex items-center gap-1.5 rounded-xl border border-indigo-200 bg-indigo-50/60 px-3 py-1.5 text-xs font-semibold text-indigo-700 hover:bg-indigo-100/70 transition shadow-2xs cursor-pointer"
            onClick={() => setIsScheduleOpen(true)}
          >
            <Calendar className="h-3.5 w-3.5 text-indigo-600" />
            <span>Cấu hình lịch tự động</span>
          </button>

          <button
            type="button"
            className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3.5 py-1.5 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs cursor-pointer"
            onClick={() => setIsCreateOpen(true)}
          >
            <Plus className="h-3.5 w-3.5" />
            <span>Tạo bản sao lưu ngay</span>
          </button>
        </div>
      </div>

      {/* Enterprise Architecture Safeguard Banner */}
      <div className="rounded-xl border border-indigo-100 bg-gradient-to-r from-indigo-50/70 via-sky-50/40 to-white p-3 text-xs text-slate-700 shadow-2xs">
        <div className="flex items-start gap-2.5">
          <Info className="h-4 w-4 text-indigo-600 shrink-0 mt-0.5" />
          <div className="space-y-1">
            <span className="font-bold text-indigo-900">
              Quy chuẩn An toàn & Bảo toàn Dữ liệu (Safety Snapshot & Two-Step Verification):
            </span>
            <p className="text-slate-600 leading-relaxed">
              Mỗi bản sao lưu được lưu trữ dưới dạng snapshot kèm mã băm <strong className="text-slate-800 font-mono">SHA-256</strong> đối chiếu toàn vẹn. Khi thực hiện phục hồi, hệ thống bắt buộc quy trình <strong className="text-indigo-800">xác nhận 2 bước</strong> (từ khóa <span className="font-mono text-rose-700 font-bold">RESTORE</span> + lý do giải trình) và sẽ <strong>tự động tạo một điểm an toàn dự phòng</strong> trước khi ghi đè dữ liệu.
            </p>
          </div>
        </div>
      </div>

      {/* Global Alert Notification */}
      {notification && (
        <div
          className={cn(
            "p-3 rounded-xl text-xs flex items-center justify-between gap-3 border shadow-2xs transition animate-in fade-in",
            notification.type === "success"
              ? "bg-emerald-50/90 border-emerald-200 text-emerald-900"
              : "bg-rose-50/90 border-rose-200 text-rose-900"
          )}
        >
          <div className="flex items-center gap-2">
            {notification.type === "success" ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            ) : (
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            )}
            <span className="font-medium">{notification.message}</span>
          </div>
          <button
            type="button"
            className="text-[11px] font-bold underline hover:opacity-80 cursor-pointer"
            onClick={() => setNotification(null)}
          >
            Đóng
          </button>
        </div>
      )}

      {/* 4 KPI Summary Cards (Compact & Unified System Style) */}
      <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
        {/* Total Backups */}
        <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-2xs transition hover:border-indigo-300 hover:shadow-xs">
          <div className="flex items-center justify-between">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-50 border border-indigo-100 text-indigo-600">
              <Database className="h-3.5 w-3.5" />
            </div>
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Lưu trữ Dữ liệu
            </span>
          </div>
          <div className="mt-2">
            <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Tổng số Bản sao lưu
            </p>
            <div className="mt-0.5 flex items-baseline gap-1.5">
              <span className="text-xl font-bold text-slate-900">
                {summary ? summary.totalBackups : 0}
              </span>
              <span className="text-[10px] text-slate-400">bản lưu trữ</span>
            </div>
            <div className="mt-1 flex items-center gap-2 text-[10px]">
              <span className="text-emerald-600 font-semibold inline-flex items-center gap-0.5">
                <CheckCircle2 className="h-2.5 w-2.5" /> {backups.filter((b) => b.status === "COMPLETED").length} Sẵn sàng
              </span>
              <span className="text-slate-400">•</span>
              <span className="text-indigo-600 font-medium">
                {backups.filter((b) => b.isAutomatic).length} Tự động
              </span>
            </div>
          </div>
        </div>

        {/* Total Storage Size */}
        <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-2xs transition hover:border-blue-300 hover:shadow-xs">
          <div className="flex items-center justify-between">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-50 border border-blue-100 text-blue-600">
              <HardDrive className="h-3.5 w-3.5" />
            </div>
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Disk Usage
            </span>
          </div>
          <div className="mt-2">
            <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Dung lượng Chiếm dụng
            </p>
            <div className="mt-0.5 flex items-baseline gap-1.5">
              <span className="text-xl font-bold text-slate-900">
                {summary ? summary.formattedTotalSize : "0 B"}
              </span>
              <span className="text-[10px] text-slate-400">trên máy chủ</span>
            </div>
            <div className="mt-1 text-[10px] text-blue-700 font-medium truncate" title="Thư mục: storage/backups/">
              Vị trí: storage/backups/
            </div>
          </div>
        </div>

        {/* Latest Backup */}
        <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-2xs transition hover:border-emerald-300 hover:shadow-xs">
          <div className="flex items-center justify-between">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-50 border border-emerald-100 text-emerald-600">
              <Clock className="h-3.5 w-3.5" />
            </div>
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Mới nhất
            </span>
          </div>
          <div className="mt-2">
            <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Bản sao lưu Gần nhất
            </p>
            {summary?.latestCompletedBackup ? (
              <>
                <div className="mt-0.5 flex items-baseline gap-1">
                  <span className="text-sm font-bold text-slate-900 truncate block max-w-[200px]" title={summary.latestCompletedBackup.title}>
                    {summary.latestCompletedBackup.title}
                  </span>
                </div>
                <div className="mt-1 flex items-center gap-1.5 text-[10px] text-slate-500">
                  <span className="font-semibold text-slate-700">
                    {new Date(summary.latestCompletedBackup.createdAt).toLocaleDateString("vi-VN")}
                  </span>
                  <span>lúc</span>
                  <span className="font-mono text-slate-600">
                    {new Date(summary.latestCompletedBackup.createdAt).toLocaleTimeString("vi-VN", {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </span>
                </div>
              </>
            ) : (
              <div className="mt-1 text-xs text-slate-400 font-medium">Chưa có bản sao lưu</div>
            )}
          </div>
        </div>

        {/* Schedule Status */}
        <div
          onClick={() => setIsScheduleOpen(true)}
          className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-3 shadow-2xs transition hover:border-purple-300 hover:shadow-xs"
        >
          <div className="flex items-center justify-between">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-purple-50 border border-purple-100 text-purple-600">
              <Calendar className="h-3.5 w-3.5" />
            </div>
            <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-purple-600 group-hover:translate-x-0.5 transition">
              Cấu hình →
            </span>
          </div>
          <div className="mt-2">
            <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Lịch sao lưu Tự động
            </p>
            <div className="mt-0.5">
              {summary?.schedule?.isEnabled ? (
                <div className="flex items-baseline gap-1.5">
                  <span className="inline-flex items-center gap-1 text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                    <CheckCircle2 className="h-3 w-3 text-emerald-600" />
                    Bật ({summary.schedule.frequencyLabel})
                  </span>
                </div>
              ) : (
                <span className="inline-flex items-center gap-1 text-xs font-semibold text-slate-600 bg-slate-100 px-2 py-0.5 rounded-md">
                  <XCircle className="h-3 w-3 text-slate-400" />
                  Đang tắt
                </span>
              )}
            </div>
            <div className="mt-1 text-[10px] text-purple-700 font-medium truncate">
              {summary?.schedule?.isEnabled
                ? `Khung giờ chạy: ${summary.schedule.scheduledTime}`
                : "Chỉ sao lưu thủ công theo yêu cầu"}
            </div>
          </div>
        </div>
      </div>

      {/* Subtabs Switcher */}
      <div className="flex items-center gap-1.5 border-b border-slate-200 pb-2">
        <button
          type="button"
          onClick={() => setActiveTab("backups")}
          className={cn(
            "inline-flex items-center gap-2 rounded-xl px-3.5 py-1.5 text-xs font-semibold transition cursor-pointer",
            activeTab === "backups"
              ? "bg-indigo-600 text-white shadow-xs"
              : "bg-white text-slate-600 hover:bg-slate-50 border border-slate-200"
          )}
        >
          <Database className="w-3.5 h-3.5" />
          <span>Danh sách Bản sao lưu</span>
          <span
            className={cn(
              "px-1.5 py-0.2 rounded-full text-[10px] font-bold",
              activeTab === "backups" ? "bg-white/20 text-white" : "bg-slate-100 text-slate-700"
            )}
          >
            {backups.length}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab("audit")}
          className={cn(
            "inline-flex items-center gap-2 rounded-xl px-3.5 py-1.5 text-xs font-semibold transition cursor-pointer",
            activeTab === "audit"
              ? "bg-indigo-600 text-white shadow-xs"
              : "bg-white text-slate-600 hover:bg-slate-50 border border-slate-200"
          )}
        >
          <ShieldCheck className="w-3.5 h-3.5" />
          <span>Nhật ký Kiểm toán & An toàn</span>
        </button>
      </div>

      {/* Tab 1: Backups Table Workspace */}
      {activeTab === "backups" && (
        <div className="space-y-3">
          {/* Filters Bar */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2.5 rounded-xl border border-slate-200 bg-white p-2.5 shadow-2xs">
            <form onSubmit={handleSearchSubmit} className="relative flex-1 max-w-sm">
              <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                className="w-full pl-8 pr-3 py-1.5 border border-slate-200 rounded-lg bg-slate-50/50 text-slate-900 text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none placeholder:text-slate-400"
                placeholder="Tìm mã bản sao, tiêu đề, checksum..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </form>

            <div className="flex flex-wrap items-center gap-2">
              <div className="flex items-center gap-1.5">
                <SlidersHorizontal className="w-3 h-3 text-slate-400" />
                <span className="text-[11px] font-medium text-slate-500">Phạm vi:</span>
                <select
                  className="px-2.5 py-1 border border-slate-200 rounded-lg bg-slate-50/50 text-slate-800 text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:outline-none cursor-pointer"
                  value={typeFilter}
                  onChange={(e) => setTypeFilter(e.target.value as BackupType | "ALL")}
                >
                  <option value="ALL">Tất cả phạm vi</option>
                  <option value="FULL">Toàn bộ hệ thống (FULL)</option>
                  <option value="RESOURCE_PLAN">Kế hoạch nguồn lực (PLAN)</option>
                </select>
              </div>

              <div className="flex items-center gap-1.5">
                <span className="text-[11px] font-medium text-slate-500">Trạng thái:</span>
                <select
                  className="px-2.5 py-1 border border-slate-200 rounded-lg bg-slate-50/50 text-slate-800 text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:outline-none cursor-pointer"
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

          {/* Table Container */}
          <div className="rounded-2xl border border-slate-200 bg-white shadow-2xs overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                    <th className="py-2.5 px-3.5">Mã bản sao</th>
                    <th className="py-2.5 px-3.5">Tiêu đề & Mục đích</th>
                    <th className="py-2.5 px-3.5">Phân loại</th>
                    <th className="py-2.5 px-3.5">Thời điểm tạo</th>
                    <th className="py-2.5 px-3.5">Dung lượng</th>
                    <th className="py-2.5 px-3.5">Mã băm Checksum (SHA-256)</th>
                    <th className="py-2.5 px-3.5">Trạng thái</th>
                    <th className="py-2.5 px-3.5 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {isLoading ? (
                    <tr>
                      <td colSpan={8} className="py-12 text-center text-slate-500">
                        <Loader2 className="w-6 h-6 animate-spin mx-auto mb-2 text-indigo-600" />
                        <span className="text-xs font-medium">Đang tải danh sách bản sao lưu...</span>
                      </td>
                    </tr>
                  ) : filteredBackups.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="py-12 text-center text-slate-500">
                        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-50 border border-slate-200 text-slate-400 mx-auto mb-2.5">
                          <Database className="w-6 h-6" />
                        </div>
                        <h4 className="text-sm font-bold text-slate-800">
                          Chưa có bản sao lưu nào
                        </h4>
                        <p className="text-xs text-slate-400 mt-0.5 max-w-sm mx-auto">
                          Tạo bản sao lưu snapshot đầu tiên để bảo vệ an toàn dữ liệu kế hoạch nguồn lực của doanh nghiệp.
                        </p>
                        <button
                          type="button"
                          className="mt-3.5 inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold shadow-xs transition cursor-pointer"
                          onClick={() => setIsCreateOpen(true)}
                        >
                          <Plus className="w-3.5 h-3.5" />
                          <span>Tạo bản sao lưu ngay</span>
                        </button>
                      </td>
                    </tr>
                  ) : (
                    filteredBackups.map((b) => (
                      <tr key={b.id} className="hover:bg-indigo-50/20 transition-colors">
                        {/* Backup Code */}
                        <td className="py-2.5 px-3.5 whitespace-nowrap">
                          <span className="font-mono text-xs font-bold text-slate-900 px-2 py-0.5 bg-slate-100 border border-slate-200 rounded-md inline-block">
                            {b.backupCode}
                          </span>
                        </td>

                        {/* Title & Description */}
                        <td className="py-2.5 px-3.5 max-w-xs">
                          <span className="font-semibold text-slate-900 block truncate" title={b.title}>
                            {b.title}
                          </span>
                          {b.description && (
                            <span className="text-[11px] text-slate-500 block truncate mt-0.5" title={b.description}>
                              {b.description}
                            </span>
                          )}
                          {b.errorMessage && (
                            <span className="text-[11px] text-rose-600 block truncate mt-0.5 font-medium" title={b.errorMessage}>
                              Lỗi: {b.errorMessage}
                            </span>
                          )}
                        </td>

                        {/* Classification Badge */}
                        <td className="py-2.5 px-3.5 whitespace-nowrap">
                          {getBackupTypeBadge(b.backupType, b.backupCode)}
                        </td>

                        {/* Created At & Trigger */}
                        <td className="py-2.5 px-3.5 whitespace-nowrap">
                          <span className="text-slate-900 font-medium block text-xs">
                            {new Date(b.createdAt).toLocaleDateString("vi-VN")}
                          </span>
                          <span className="text-slate-400 block text-[10px]">
                            {new Date(b.createdAt).toLocaleTimeString("vi-VN")} ·{" "}
                            {b.isAutomatic ? (
                              <span className="text-purple-600 font-semibold">Tự động (Cron)</span>
                            ) : (
                              <span className="text-slate-500 font-medium">Thủ công</span>
                            )}
                          </span>
                        </td>

                        {/* File Size */}
                        <td className="py-2.5 px-3.5 whitespace-nowrap font-mono text-xs font-semibold text-slate-900">
                          {b.formattedFileSize}
                        </td>

                        {/* Checksum with Copy */}
                        <td className="py-2.5 px-3.5 whitespace-nowrap">
                          {b.checksum ? (
                            <div className="flex items-center gap-1.5 font-mono text-[11px] text-slate-600">
                              <span className="px-1.5 py-0.5 bg-slate-50 border border-slate-200 rounded font-mono text-[10px] text-slate-700" title={b.checksum}>
                                {b.checksum.substring(0, 12)}...
                              </span>
                              <button
                                type="button"
                                className="p-1 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded transition cursor-pointer"
                                onClick={() => b.checksum && handleCopyChecksum(b.checksum)}
                                title="Sao chép toàn bộ mã SHA-256"
                              >
                                {copiedCode === b.checksum ? (
                                  <Check className="w-3.5 h-3.5 text-emerald-600" />
                                ) : (
                                  <Copy className="w-3.5 h-3.5" />
                                )}
                              </button>
                            </div>
                          ) : (
                            <span className="text-slate-400 text-xs italic">Đang tính...</span>
                          )}
                        </td>

                        {/* Status Badge */}
                        <td className="py-2.5 px-3.5 whitespace-nowrap">
                          {getStatusBadge(b.status)}
                        </td>

                        {/* Actions */}
                        <td className="py-2.5 px-3.5 whitespace-nowrap text-right">
                          <div className="flex items-center justify-end gap-1">
                            {/* Phục hồi button (Chỉ kích hoạt khi bản sao COMPLETED) */}
                            <button
                              type="button"
                              className={cn(
                                "inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold transition cursor-pointer",
                                b.status === "COMPLETED"
                                  ? "bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100 hover:text-rose-800 shadow-2xs"
                                  : "opacity-40 bg-slate-50 text-slate-400 border border-slate-200 cursor-not-allowed"
                              )}
                              onClick={() => setRestoreTargetBackup(b)}
                              disabled={b.status !== "COMPLETED"}
                              title={b.status === "COMPLETED" ? "Phục hồi an toàn 2 bước về bản sao này" : "Bản sao không ở trạng thái sẵn sàng để phục hồi"}
                            >
                              <RotateCcw className="w-3 h-3 text-rose-600" />
                              <span>Phục hồi</span>
                            </button>

                            {/* Tải về */}
                            <button
                              type="button"
                              className="p-1.5 text-slate-600 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition border border-transparent hover:border-indigo-100 cursor-pointer"
                              onClick={() => handleDownload(b)}
                              title="Tải về tệp sao lưu"
                            >
                              <Download className="w-3.5 h-3.5" />
                            </button>

                            {/* Xóa */}
                            <button
                              type="button"
                              className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition border border-transparent hover:border-rose-100 cursor-pointer"
                              onClick={() => handleDelete(b)}
                              title="Xóa bản sao lưu"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Table Footer / Summary Bar */}
            <div className="flex items-center justify-between border-t border-slate-100 bg-slate-50/50 px-4 py-2 text-[11px] text-slate-500">
              <div>
                Hiển thị <span className="font-semibold text-slate-900">{filteredBackups.length}</span> bản sao lưu
              </div>
              <div className="flex items-center gap-2">
                <FileCheck2 className="w-3.5 h-3.5 text-emerald-600" />
                <span>Toàn vẹn dữ liệu được xác thực tự động bởi hệ thống</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tab 2: Audit Logs Table */}
      {activeTab === "audit" && <BackupAuditLogsTable />}

      {/* Modals */}
      <CreateBackupModal
        open={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={(newBackup) => {
          setNotification({
            type: "success",
            message: `Khởi tạo bản sao lưu ${newBackup.backupCode} thành công! Hệ thống đã tính toán checksum SHA-256 an toàn.`,
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
            message: `Phục hồi cơ sở dữ liệu thành công về thời điểm bản sao lưu! Điểm an toàn tự động đã được lưu lại và ghi nhận vào nhật ký kiểm toán.`,
          });
          loadData();
        }}
      />

      <BackupScheduleModal
        open={isScheduleOpen}
        onClose={() => setIsScheduleOpen(false)}
        onSuccess={() => {
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

export default BackupManagementWorkspace;
