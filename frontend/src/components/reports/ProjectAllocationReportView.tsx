import { useState, useEffect, useMemo, useRef, Fragment } from "react";
import {
  FolderKanban,
  AlertTriangle,
  CheckCircle2,
  Users,
  Search,
  RefreshCw,
  FileSpreadsheet,
  Building2,
  Calendar,
  Clock,
  ChevronDown,
  ChevronUp,
  ShieldAlert,
  Sparkles,
  Layers,
  UserCheck,
  UserX
} from "lucide-react";
import { cn } from "@/lib/utils";
import {
  getProjectAllocationReport,
  downloadProjectAllocationReport,
  type ProjectAllocationReportData
} from "@/lib/api/project-allocation-report";
import { getProjects, type ProjectResult } from "@/lib/api/projects";
import { getIsoWeeksInYear, getIsoWeekDetails } from "@/lib/iso-week";

export default function ProjectAllocationReportView() {
  const now = new Date();
  const currentIso = getIsoWeekDetails(now);

  // Danh sách dự án người dùng có thể chọn
  const [projects, setProjects] = useState<ProjectResult[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
  const [isProjectsLoading, setIsProjectsLoading] = useState<boolean>(true);

  // Bộ lọc thời gian tuần
  const [fromYear, setFromYear] = useState<number>(currentIso.year);
  const [fromWeek, setFromWeek] = useState<number>(currentIso.week);
  const [toYear, setToYear] = useState<number>(currentIso.year);
  const [toWeek, setToWeek] = useState<number>(Math.min(currentIso.week + 11, getIsoWeeksInYear(currentIso.year)));

  // Bộ lọc vai trò & tìm kiếm
  const [selectedRoleFilter, setSelectedRoleFilter] = useState<string>("ALL");
  const [searchKeyword, setSearchKeyword] = useState<string>("");
  const [activeTab, setActiveTab] = useState<"matrix" | "weekly" | "alerts">("matrix");
  const [expandedRoleIds, setExpandedRoleIds] = useState<Set<number>>(new Set());

  // Trạng thái báo cáo
  const [reportData, setReportData] = useState<ProjectAllocationReportData | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [forbidden, setForbidden] = useState<boolean>(false);
  const [autoRefresh, setAutoRefresh] = useState<boolean>(false);
  const [isExporting, setIsExporting] = useState<boolean>(false);
  const abortControllerRef = useRef<AbortController | null>(null);

  // 1. Tải danh sách dự án trong phạm vi quyền hạn của người dùng
  useEffect(() => {
    async function loadAccessibleProjects() {
      setIsProjectsLoading(true);
      try {
        const res = await getProjects(0, 100);
        const activeOrAll = res?.content || [];
        setProjects(activeOrAll);
        if (activeOrAll.length > 0 && selectedProjectId == null) {
          setSelectedProjectId(activeOrAll[0].id);
        }
      } catch (err: any) {
        console.warn("Không thể tải danh sách dự án:", err);
      } finally {
        setIsProjectsLoading(false);
      }
    }
    loadAccessibleProjects();
  }, []);

  // 2. Hàm nạp dữ liệu báo cáo phân bổ
  const fetchReport = async (silent = false) => {
    if (!selectedProjectId) return;

    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    const controller = new AbortController();
    abortControllerRef.current = controller;

    if (!silent) {
      setIsLoading(true);
    }
    setError(null);
    setForbidden(false);

    try {
      const data = await getProjectAllocationReport(
        {
          projectId: selectedProjectId,
          fromYear,
          fromWeek,
          toYear,
          toWeek
        },
        controller.signal
      );
      setReportData(data);
      // Mặc định expand các vai trò có thiếu hụt
      const shortageRoles = new Set<number>();
      data.roleBreakdowns.forEach((r) => {
        if (r.totalShortfallHours > 0) {
          shortageRoles.add(r.roleId);
        }
      });
      if (shortageRoles.size > 0) {
        setExpandedRoleIds(shortageRoles);
      }
    } catch (err: any) {
      if (err?.name === "AbortError") return;
      if (err?.status === 403 || String(err?.message || "").includes("403") || String(err?.code || "").includes("FORBIDDEN")) {
        setForbidden(true);
      } else {
        setError(err?.message || "Không thể kết nối đến máy chủ để lấy dữ liệu báo cáo.");
      }
    } finally {
      if (!silent) {
        setIsLoading(false);
      }
    }
  };

  // 3. Tự động tải lại khi đổi dự án hoặc kỳ tuần
  useEffect(() => {
    if (selectedProjectId) {
      fetchReport();
    }
  }, [selectedProjectId, fromYear, fromWeek, toYear, toWeek]);

  // 4. Auto-refresh định kỳ mỗi 30s nếu được bật (đảm bảo "mỗi khi có cập nhật thì luôn thay đổi theo")
  useEffect(() => {
    if (!autoRefresh || !selectedProjectId) return;
    const interval = setInterval(() => {
      fetchReport(true);
    }, 30000);
    return () => clearInterval(interval);
  }, [autoRefresh, selectedProjectId, fromYear, fromWeek, toYear, toWeek]);

  // Đồng bộ kỳ tuần theo ngày bắt đầu / kết thúc của dự án khi chọn
  const handleSelectProject = (projectId: number) => {
    setSelectedProjectId(projectId);
    const p = projects.find((proj) => proj.id === projectId);
    if (p && p.startDate) {
      const startIso = getIsoWeekDetails(new Date(p.startDate));
      setFromYear(startIso.year);
      setFromWeek(startIso.week);
      if (p.endDate) {
        const endIso = getIsoWeekDetails(new Date(p.endDate));
        setToYear(endIso.year);
        setToWeek(endIso.week);
      } else {
        setToYear(startIso.year);
        setToWeek(Math.min(startIso.week + 11, getIsoWeeksInYear(startIso.year)));
      }
    }
  };

  // Áp dụng presets thời gian
  const applyPresetWeeks = (weeksCount: number) => {
    setFromYear(currentIso.year);
    setFromWeek(currentIso.week);
    const targetEndWeek = currentIso.week + weeksCount - 1;
    const maxWeeks = getIsoWeeksInYear(currentIso.year);
    if (targetEndWeek <= maxWeeks) {
      setToYear(currentIso.year);
      setToWeek(targetEndWeek);
    } else {
      setToYear(currentIso.year + 1);
      setToWeek(targetEndWeek - maxWeeks);
    }
  };

  const toggleRoleExpand = (roleId: number) => {
    setExpandedRoleIds((prev) => {
      const next = new Set(prev);
      if (next.has(roleId)) {
        next.delete(roleId);
      } else {
        next.add(roleId);
      }
      return next;
    });
  };

  // Lọc vai trò theo filter và keyword tìm kiếm
  const filteredRoleBreakdowns = useMemo(() => {
    if (!reportData?.roleBreakdowns) return [];
    return reportData.roleBreakdowns.filter((rb) => {
      if (selectedRoleFilter !== "ALL" && String(rb.roleId) !== selectedRoleFilter) {
        return false;
      }
      if (!searchKeyword.trim()) return true;
      const kw = searchKeyword.toLowerCase();
      const matchRole = rb.roleName.toLowerCase().includes(kw) || rb.roleCode.toLowerCase().includes(kw);
      const matchMember = rb.weeklyRoleMetrics.some((wm) =>
        wm.allocatedMembers.some((m) =>
          m.fullName.toLowerCase().includes(kw) || m.employeeCode.toLowerCase().includes(kw)
        )
      );
      return matchRole || matchMember;
    });
  }, [reportData?.roleBreakdowns, selectedRoleFilter, searchKeyword]);

  // Xuất file CSV
  const handleExportCsv = async () => {
    if (!selectedProjectId) return;
    setIsExporting(true);
    try {
      await downloadProjectAllocationReport({
        projectId: selectedProjectId,
        fromYear,
        fromWeek,
        toYear,
        toWeek
      });
    } catch (err: any) {
      alert(err?.message || "Lỗi khi xuất file báo cáo.");
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="space-y-6 pb-12">
      {/* ---------- HEADER BÁO CÁO & THAO TÁC ---------- */}
      <div className="flex flex-col gap-4 rounded-3xl border border-slate-200 bg-white p-6 shadow-xs sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-indigo-50 border border-indigo-100 text-indigo-600 shadow-xs">
            <FolderKanban className="h-7 w-7" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold tracking-tight text-slate-900">
                Báo cáo phân bổ theo dự án
              </h1>
              <span className="rounded-full bg-indigo-100 px-2.5 py-0.5 text-[11px] font-semibold text-indigo-700">
                NCL-10-CN-006
              </span>
            </div>
            <p className="mt-1 text-xs text-slate-500 max-w-xl">
              Theo dõi tổng giờ đã phân bổ theo tuần và vai trò, so sánh với nhu cầu ước lượng và phát hiện sớm các tuần bị hụt nhân sự.
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <button
            type="button"
            onClick={() => setAutoRefresh(!autoRefresh)}
            className={cn(
              "flex items-center gap-1.5 rounded-xl border px-3 py-2 text-xs font-semibold transition shadow-xs",
              autoRefresh
                ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                : "border-slate-200 bg-white text-slate-600 hover:bg-slate-50"
            )}
            title="Tự động cập nhật dữ liệu khi có thay đổi"
          >
            <Sparkles className={cn("h-3.5 w-3.5", autoRefresh ? "text-emerald-600 animate-pulse" : "text-slate-400")} />
            <span>{autoRefresh ? "Tự động cập nhật: Bật" : "Tự động cập nhật"}</span>
          </button>

          <button
            type="button"
            onClick={() => fetchReport()}
            disabled={isLoading || !selectedProjectId}
            className="flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-xs disabled:opacity-50"
          >
            <RefreshCw className={cn("h-3.5 w-3.5", isLoading && "animate-spin text-indigo-600")} />
            <span>Làm mới</span>
          </button>

          <button
            type="button"
            onClick={handleExportCsv}
            disabled={isExporting || !reportData}
            className="flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3.5 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs disabled:opacity-50"
          >
            <FileSpreadsheet className="h-3.5 w-3.5" />
            <span>{isExporting ? "Đang xuất..." : "Xuất báo cáo CSV"}</span>
          </button>
        </div>
      </div>

      {/* ---------- THANH BỘ LỌC DỰ ÁN VÀ THỜI GIAN ---------- */}
      <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-xs">
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-12 items-end">
          {/* Chọn dự án */}
          <div className="lg:col-span-4">
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1.5 flex items-center gap-1.5">
              <FolderKanban className="h-3.5 w-3.5 text-indigo-600" />
              <span>Dự án phụ trách / Cần xem:</span>
            </label>
            <select
              value={selectedProjectId || ""}
              onChange={(e) => handleSelectProject(Number(e.target.value))}
              disabled={isProjectsLoading || projects.length === 0}
              className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3 py-2 text-xs font-semibold text-slate-800 focus:border-indigo-500 focus:bg-white focus:outline-hidden transition"
            >
              {projects.length === 0 ? (
                <option value="">(Không có dự án khả dụng trong quyền hạn)</option>
              ) : (
                projects.map((p) => (
                  <option key={p.id} value={p.id}>
                    [{p.projectCode}] {p.projectName} {p.status === "ACTIVE" ? "" : `(${p.status})`}
                  </option>
                ))
              )}
            </select>
          </div>

          {/* Khoảng tuần bắt đầu -> kết thúc */}
          <div className="lg:col-span-5 grid grid-cols-2 gap-2">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1.5 flex items-center gap-1.5">
                <Calendar className="h-3.5 w-3.5 text-slate-500" />
                <span>Từ Tuần/Năm:</span>
              </label>
              <div className="flex gap-1.5">
                <input
                  type="number"
                  min={1}
                  max={53}
                  value={fromWeek}
                  onChange={(e) => setFromWeek(Number(e.target.value))}
                  className="w-16 rounded-xl border border-slate-200 bg-slate-50/50 px-2 py-2 text-xs font-semibold text-center text-slate-800 focus:border-indigo-500 focus:bg-white focus:outline-hidden"
                  title="Số tuần bắt đầu"
                />
                <input
                  type="number"
                  min={2020}
                  max={2030}
                  value={fromYear}
                  onChange={(e) => setFromYear(Number(e.target.value))}
                  className="w-20 rounded-xl border border-slate-200 bg-slate-50/50 px-2 py-2 text-xs font-semibold text-center text-slate-800 focus:border-indigo-500 focus:bg-white focus:outline-hidden"
                  title="Năm bắt đầu"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1.5 flex items-center gap-1.5">
                <Calendar className="h-3.5 w-3.5 text-slate-500" />
                <span>Đến Tuần/Năm:</span>
              </label>
              <div className="flex gap-1.5">
                <input
                  type="number"
                  min={1}
                  max={53}
                  value={toWeek}
                  onChange={(e) => setToWeek(Number(e.target.value))}
                  className="w-16 rounded-xl border border-slate-200 bg-slate-50/50 px-2 py-2 text-xs font-semibold text-center text-slate-800 focus:border-indigo-500 focus:bg-white focus:outline-hidden"
                  title="Số tuần kết thúc"
                />
                <input
                  type="number"
                  min={2020}
                  max={2030}
                  value={toYear}
                  onChange={(e) => setToYear(Number(e.target.value))}
                  className="w-20 rounded-xl border border-slate-200 bg-slate-50/50 px-2 py-2 text-xs font-semibold text-center text-slate-800 focus:border-indigo-500 focus:bg-white focus:outline-hidden"
                  title="Năm kết thúc"
                />
              </div>
            </div>
          </div>

          {/* Preset nhanh */}
          <div className="lg:col-span-3 flex flex-wrap gap-1.5 justify-end">
            <button
              type="button"
              onClick={() => applyPresetWeeks(4)}
              className="rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-[11px] font-semibold text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition"
            >
              4 tuần tới
            </button>
            <button
              type="button"
              onClick={() => applyPresetWeeks(8)}
              className="rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-[11px] font-semibold text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition"
            >
              8 tuần tới
            </button>
            <button
              type="button"
              onClick={() => applyPresetWeeks(12)}
              className="rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-[11px] font-semibold text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition"
            >
              12 tuần tới
            </button>
          </div>
        </div>
      </div>

      {/* ---------- XỬ LÝ FORBIDDEN (TC-02) HOẶC LỖI ---------- */}
      {forbidden && (
        <div className="flex flex-col items-center justify-center rounded-3xl border border-rose-200 bg-rose-50/70 p-8 text-center animate-in fade-in duration-200 shadow-xs">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-100 text-rose-600 mb-3 border border-rose-200">
            <ShieldAlert className="h-7 w-7" />
          </div>
          <h3 className="text-base font-bold text-slate-900 mb-1">
            Không có quyền truy cập báo cáo dự án này (403 Forbidden)
          </h3>
          <p className="text-xs text-slate-600 max-w-md leading-relaxed">
            Theo chính sách phân quyền dữ liệu (TC-02), bạn chỉ được phép xem báo cáo phân bổ của các dự án thuộc phạm vi phụ trách của mình. Vui lòng chọn dự án khác từ danh sách cho phép.
          </p>
        </div>
      )}

      {error && !forbidden && (
        <div className="flex items-center gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-xs font-semibold text-rose-700">
          <AlertTriangle className="h-4 w-4 shrink-0 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {/* ---------- NỘI DUNG BÁO CÁO KHI CÓ DỮ LIỆU ---------- */}
      {reportData && !forbidden && (
        <>
          {/* Thông tin dự án meta bar */}
          <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-slate-100/60 px-5 py-3 text-xs text-slate-700 shadow-xs">
            <div className="flex flex-wrap items-center gap-4">
              <span className="font-bold text-slate-900 text-sm">
                [{reportData.projectCode}] {reportData.projectName}
              </span>
              <span className="flex items-center gap-1 text-slate-500">
                <Users className="h-3.5 w-3.5 text-indigo-600" /> PM: <strong className="text-slate-800">{reportData.managerName}</strong>
              </span>
              <span className="flex items-center gap-1 text-slate-500">
                <Building2 className="h-3.5 w-3.5 text-slate-500" /> Đơn vị: <strong className="text-slate-800">{reportData.orgUnitName}</strong>
              </span>
              {reportData.startDate && (
                <span className="flex items-center gap-1 text-slate-500">
                  <Clock className="h-3.5 w-3.5 text-slate-500" /> Thời gian: <strong className="text-slate-800">{reportData.startDate} ~ {reportData.endDate || "Chưa xác định"}</strong>
                </span>
              )}
            </div>

            <div className="flex items-center gap-2">
              <span className={cn(
                "rounded-full px-2.5 py-0.5 text-[11px] font-bold uppercase",
                reportData.status === "ACTIVE" ? "bg-emerald-100 text-emerald-800" : "bg-slate-200 text-slate-700"
              )}>
                {reportData.status === "ACTIVE" ? "Đang hoạt động" : reportData.status}
              </span>
              <span className="text-[11px] text-slate-400">
                Kỳ: T{reportData.fromWeek}/{reportData.fromYear} → T{reportData.toWeek}/{reportData.toYear}
              </span>
            </div>
          </div>

          {/* ---------- 6 THẺ CHỈ SỐ KPI TỔNG QUAN ---------- */}
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
            {/* 1. Tổng nhu cầu */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs">
              <p className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Nhu cầu ước lượng</p>
              <p className="mt-1 text-xl font-extrabold text-slate-900">
                {reportData.totalDemandHours.toLocaleString()} <span className="text-xs font-medium text-slate-400">giờ</span>
              </p>
              <p className="mt-1 text-[11px] text-slate-500">Ước lượng theo vai trò</p>
            </div>

            {/* 2. Tổng giờ đã phân bổ */}
            <div className="rounded-2xl border border-indigo-100 bg-indigo-50/40 p-4 shadow-xs">
              <p className="text-[11px] font-bold uppercase tracking-wider text-indigo-600">Đã phân bổ</p>
              <p className="mt-1 text-xl font-extrabold text-indigo-700">
                {reportData.totalAllocatedHours.toLocaleString()} <span className="text-xs font-medium text-indigo-400">giờ</span>
              </p>
              <p className="mt-1 text-[11px] text-indigo-600">Tổng giờ nhân sự thực tế</p>
            </div>

            {/* 3. Tổng giờ thiếu hụt (TC-01) */}
            <div className={cn(
              "rounded-2xl border p-4 shadow-xs",
              reportData.totalShortfallHours > 0
                ? "border-rose-200 bg-rose-50/60"
                : "border-slate-200 bg-white"
            )}>
              <p className={cn(
                "text-[11px] font-bold uppercase tracking-wider",
                reportData.totalShortfallHours > 0 ? "text-rose-600" : "text-slate-400"
              )}>
                Thiếu hụt (Hụt giờ)
              </p>
              <p className={cn(
                "mt-1 text-xl font-extrabold",
                reportData.totalShortfallHours > 0 ? "text-rose-700" : "text-slate-900"
              )}>
                {reportData.totalShortfallHours.toLocaleString()} <span className="text-xs font-medium text-slate-400">giờ</span>
              </p>
              <p className={cn("mt-1 text-[11px]", reportData.totalShortfallHours > 0 ? "text-rose-600 font-semibold" : "text-slate-500")}>
                {reportData.totalShortfallHours > 0 ? "Cần bổ sung nhân sự" : "Đã đáp ứng đủ nhu cầu"}
              </p>
            </div>

            {/* 4. Tổng giờ dôi dư */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs">
              <p className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Dôi dư / Vượt giờ</p>
              <p className="mt-1 text-xl font-extrabold text-slate-900">
                {reportData.totalSurplusHours.toLocaleString()} <span className="text-xs font-medium text-slate-400">giờ</span>
              </p>
              <p className="mt-1 text-[11px] text-slate-500">Phân bổ vượt mức nhu cầu</p>
            </div>

            {/* 5. Tỷ lệ đáp ứng */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs">
              <p className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Tỷ lệ đáp ứng</p>
              <p className={cn(
                "mt-1 text-xl font-extrabold",
                reportData.fulfillmentRate >= 100
                  ? "text-emerald-600"
                  : reportData.fulfillmentRate >= 80
                    ? "text-amber-600"
                    : "text-rose-600"
              )}>
                {reportData.fulfillmentRate}%
              </p>
              <div className="mt-2 h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                <div
                  className={cn(
                    "h-full rounded-full transition-all duration-300",
                    reportData.fulfillmentRate >= 100 ? "bg-emerald-500" : reportData.fulfillmentRate >= 80 ? "bg-amber-500" : "bg-rose-500"
                  )}
                  style={{ width: `${Math.min(reportData.fulfillmentRate, 100)}%` }}
                />
              </div>
            </div>

            {/* 6. Số tuần bị hụt người */}
            <div className={cn(
              "rounded-2xl border p-4 shadow-xs",
              reportData.shortageWeeksCount > 0
                ? "border-amber-200 bg-amber-50/60"
                : "border-slate-200 bg-white"
            )}>
              <p className={cn(
                "text-[11px] font-bold uppercase tracking-wider",
                reportData.shortageWeeksCount > 0 ? "text-amber-700" : "text-slate-400"
              )}>
                Tuần bị thiếu người
              </p>
              <p className={cn(
                "mt-1 text-xl font-extrabold",
                reportData.shortageWeeksCount > 0 ? "text-amber-800" : "text-slate-900"
              )}>
                {reportData.shortageWeeksCount} <span className="text-xs font-medium text-slate-400">tuần</span>
              </p>
              <p className={cn("mt-1 text-[11px]", reportData.shortageWeeksCount > 0 ? "text-amber-700 font-semibold" : "text-slate-500")}>
                {reportData.shortageWeeksCount > 0 ? "Cảnh báo thiếu nhân sự" : "Phân bổ phủ kín các tuần"}
              </p>
            </div>
          </div>

          {/* ---------- BANNER CẢNH BÁO SỚM THIẾU HỤT NHÂN SỰ (TC-01) ---------- */}
          {reportData.shortageAlerts && reportData.shortageAlerts.length > 0 && (
            <div className="rounded-3xl border border-rose-200 bg-gradient-to-r from-rose-50/90 via-amber-50/50 to-white p-5 shadow-xs">
              <div className="flex items-center gap-2 mb-3">
                <div className="flex h-7 w-7 items-center justify-center rounded-xl bg-rose-100 text-rose-600 border border-rose-200">
                  <AlertTriangle className="h-4 w-4" />
                </div>
                <div>
                  <h3 className="text-xs font-bold uppercase tracking-wider text-rose-800">
                    Cảnh báo thiếu hụt nhân sự theo tuần & vai trò ({reportData.shortageAlerts.length} cảnh báo)
                  </h3>
                  <p className="text-[11px] text-slate-500">
                    Nhìn ra tuần nào dự án bị hụt người trước khi tuần đó tới để kịp thời điều chỉnh hoặc phân bổ thêm nhân sự.
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2.5">
                {reportData.shortageAlerts.slice(0, 6).map((alert, idx) => (
                  <div
                    key={idx}
                    className="flex flex-col justify-between rounded-2xl border border-rose-200/80 bg-white p-3 shadow-2xs hover:border-rose-300 transition"
                  >
                    <div className="flex items-center justify-between gap-2 mb-1.5">
                      <span className="rounded-lg bg-rose-100 px-2 py-0.5 text-[11px] font-bold text-rose-700">
                        {alert.weekLabel}
                      </span>
                      <span className="rounded-md bg-slate-100 px-1.5 py-0.5 text-[10px] font-semibold text-slate-600">
                        {alert.roleName}
                      </span>
                    </div>
                    <div className="flex items-baseline justify-between text-xs mt-1">
                      <span className="text-slate-500">Nhu cầu: <strong>{alert.demandHours}h</strong> | Đã phân bổ: <strong>{alert.allocatedHours}h</strong></span>
                      <span className="text-rose-600 font-extrabold">Thiếu {alert.missingHours}h</span>
                    </div>
                  </div>
                ))}
              </div>

              {reportData.shortageAlerts.length > 6 && (
                <p className="mt-2 text-right text-[11px] text-rose-700 font-semibold cursor-pointer hover:underline" onClick={() => setActiveTab("alerts")}>
                  Xem thêm {reportData.shortageAlerts.length - 6} cảnh báo khác trong Tab Cảnh báo →
                </p>
              )}
            </div>
          )}

          {/* ---------- TAB NAVIGATION & TOOLBAR ---------- */}
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-b border-slate-200 pb-3">
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                onClick={() => setActiveTab("matrix")}
                className={cn(
                  "flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-bold transition shadow-xs",
                  activeTab === "matrix"
                    ? "bg-indigo-600 text-white shadow-indigo-100"
                    : "bg-white text-slate-600 hover:bg-slate-50 border border-slate-200"
                )}
              >
                <Layers className="h-3.5 w-3.5" />
                <span>Ma trận Vai trò & Nhân sự</span>
              </button>

              <button
                type="button"
                onClick={() => setActiveTab("weekly")}
                className={cn(
                  "flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-bold transition shadow-xs",
                  activeTab === "weekly"
                    ? "bg-indigo-600 text-white shadow-indigo-100"
                    : "bg-white text-slate-600 hover:bg-slate-50 border border-slate-200"
                )}
              >
                <Calendar className="h-3.5 w-3.5" />
                <span>Tổng quan theo tuần ({reportData.weeklySummaries.length})</span>
              </button>

              <button
                type="button"
                onClick={() => setActiveTab("alerts")}
                className={cn(
                  "flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-bold transition shadow-xs",
                  activeTab === "alerts"
                    ? "bg-rose-600 text-white shadow-rose-100"
                    : "bg-white text-slate-600 hover:bg-slate-50 border border-slate-200"
                )}
              >
                <AlertTriangle className="h-3.5 w-3.5" />
                <span>Cảnh báo thiếu hụt ({reportData.shortageAlerts.length})</span>
              </button>
            </div>

            {/* Bộ lọc tìm kiếm & vai trò */}
            <div className="flex flex-wrap items-center gap-2">
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  placeholder="Tìm vai trò hoặc nhân sự..."
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  className="w-48 sm:w-60 rounded-xl border border-slate-200 bg-white py-1.5 pl-8 pr-3 text-xs text-slate-800 placeholder-slate-400 focus:border-indigo-500 focus:outline-hidden"
                />
              </div>

              <select
                value={selectedRoleFilter}
                onChange={(e) => setSelectedRoleFilter(e.target.value)}
                className="rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 focus:border-indigo-500 focus:outline-hidden"
              >
                <option value="ALL">Tất cả vai trò</option>
                {reportData.roleBreakdowns.map((r) => (
                  <option key={r.roleId} value={String(r.roleId)}>
                    {r.roleName}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* ---------- TAB 1: MA TRẬN VAI TRÒ & NHÂN SỰ ---------- */}
          {activeTab === "matrix" && (
            <div className="rounded-3xl border border-slate-200 bg-white shadow-xs overflow-hidden">
              <div className="p-4 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
                <div>
                  <h3 className="text-xs font-bold uppercase tracking-wider text-slate-700">
                    Phân bổ giờ theo vai trò chuyên môn & chi tiết nhân viên
                  </h3>
                  <p className="text-[11px] text-slate-500">
                    Nhấp vào vai trò để xem danh sách nhân sự được phân bổ trong từng tuần.
                  </p>
                </div>
                <div className="flex items-center gap-3 text-[11px] font-semibold text-slate-500">
                  <span className="flex items-center gap-1">
                    <span className="h-2 w-2 rounded-full bg-emerald-500" /> Đủ nhu cầu (100%)
                  </span>
                  <span className="flex items-center gap-1">
                    <span className="h-2 w-2 rounded-full bg-rose-500" /> Thiếu hụt giờ
                  </span>
                  <span className="flex items-center gap-1">
                    <span className="h-2 w-2 rounded-full bg-indigo-500" /> Vượt nhu cầu
                  </span>
                </div>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-700">
                  <thead className="bg-slate-100/80 text-[11px] font-bold uppercase tracking-wider text-slate-600 border-b border-slate-200">
                    <tr>
                      <th className="px-4 py-3 min-w-[200px] sticky left-0 bg-slate-100 z-10">Vai trò / Nhân sự</th>
                      <th className="px-3 py-3 w-28 text-center">Tổng Nhu cầu</th>
                      <th className="px-3 py-3 w-28 text-center">Tổng Đã gán</th>
                      <th className="px-3 py-3 w-28 text-center">Chênh lệch</th>
                      <th className="px-3 py-3 w-24 text-center">Đáp ứng</th>
                      {reportData.weeklySummaries.map((w) => (
                        <th key={`${w.year}_${w.weekNumber}`} className="px-3 py-3 min-w-[120px] text-center border-l border-slate-200">
                          <div>T{w.weekNumber}</div>
                          <div className="text-[9px] font-normal text-slate-400">{w.startDate.slice(5)} - {w.endDate.slice(5)}</div>
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {filteredRoleBreakdowns.length === 0 ? (
                      <tr>
                        <td colSpan={5 + reportData.weeklySummaries.length} className="py-12 text-center text-slate-400">
                          Không tìm thấy vai trò hoặc nhân sự phù hợp với bộ lọc.
                        </td>
                      </tr>
                    ) : (
                      filteredRoleBreakdowns.map((role) => {
                        const isExpanded = expandedRoleIds.has(role.roleId);
                        const hasShortfall = role.totalShortfallHours > 0;

                        return (
                          <Fragment key={role.roleId}>
                            {/* Dòng chính vai trò */}
                            <tr
                              onClick={() => toggleRoleExpand(role.roleId)}
                              className={cn(
                                "cursor-pointer transition hover:bg-slate-50/80 font-semibold",
                                hasShortfall ? "bg-rose-50/20" : "bg-white"
                              )}
                            >
                              <td className="px-4 py-3 sticky left-0 bg-inherit z-10 flex items-center justify-between gap-2">
                                <div className="flex items-center gap-2">
                                  <div className={cn(
                                    "flex h-6 w-6 shrink-0 items-center justify-center rounded-lg border",
                                    hasShortfall ? "bg-rose-100 text-rose-700 border-rose-200" : "bg-indigo-50 text-indigo-700 border-indigo-100"
                                  )}>
                                    <Users className="h-3 w-3" />
                                  </div>
                                  <div>
                                    <div className="text-slate-900 font-bold">{role.roleName}</div>
                                    <div className="text-[10px] text-slate-400 font-normal">{role.roleCode}</div>
                                  </div>
                                </div>
                                <div className="text-slate-400">
                                  {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                                </div>
                              </td>

                              <td className="px-3 py-3 text-center text-slate-900 font-bold">
                                {role.totalDemandHours}h
                              </td>
                              <td className="px-3 py-3 text-center text-indigo-700 font-bold">
                                {role.totalAllocatedHours}h
                              </td>
                              <td className="px-3 py-3 text-center">
                                {role.totalShortfallHours > 0 ? (
                                  <span className="inline-flex items-center gap-1 rounded-md bg-rose-100 px-2 py-0.5 text-[11px] font-bold text-rose-700">
                                    Thiếu {role.totalShortfallHours}h
                                  </span>
                                ) : role.totalSurplusHours > 0 ? (
                                  <span className="inline-flex items-center gap-1 rounded-md bg-indigo-100 px-2 py-0.5 text-[11px] font-bold text-indigo-700">
                                    +{role.totalSurplusHours}h
                                  </span>
                                ) : (
                                  <span className="text-slate-400 text-xs">0h</span>
                                )}
                              </td>
                              <td className="px-3 py-3 text-center">
                                <span className={cn(
                                  "font-bold text-xs",
                                  role.fulfillmentRate >= 100 ? "text-emerald-600" : role.fulfillmentRate >= 80 ? "text-amber-600" : "text-rose-600"
                                )}>
                                  {role.fulfillmentRate}%
                                </span>
                              </td>

                              {/* Từng tuần của vai trò */}
                              {role.weeklyRoleMetrics.map((wm) => {
                                const isWeekShortfall = wm.shortfallHours > 0;
                                const isWeekSurplus = wm.surplusHours > 0;

                                return (
                                  <td
                                    key={`${wm.year}_${wm.weekNumber}`}
                                    className={cn(
                                      "px-3 py-3 text-center border-l border-slate-100 text-xs",
                                      isWeekShortfall ? "bg-rose-50/50" : isWeekSurplus ? "bg-indigo-50/30" : ""
                                    )}
                                  >
                                    <div className="font-bold text-slate-800">
                                      {wm.allocatedHours} <span className="text-[10px] font-normal text-slate-400">/ {wm.demandHours}h</span>
                                    </div>
                                    {isWeekShortfall && (
                                      <div className="text-[10px] font-bold text-rose-600 mt-0.5">
                                        - {wm.shortfallHours}h
                                      </div>
                                    )}
                                    {isWeekSurplus && (
                                      <div className="text-[10px] font-semibold text-indigo-600 mt-0.5">
                                        + {wm.surplusHours}h
                                      </div>
                                    )}
                                    <div className="text-[9px] text-slate-400 mt-0.5">
                                      {wm.allocatedMembers.length} NV
                                    </div>
                                  </td>
                                );
                              })}
                            </tr>

                            {/* Dòng mở rộng chi tiết từng nhân viên */}
                            {isExpanded && (
                              <>
                                {/* Dòng phân tích chi tiết */}
                                <tr className="bg-slate-50/90 text-[11px] text-slate-500 font-semibold border-t border-slate-100">
                                  <td className="px-8 py-2 sticky left-0 bg-slate-50/90 z-10 italic">
                                    ↳ Danh sách nhân sự gán vào vai trò:
                                  </td>
                                  <td className="px-3 py-2 text-center">Nhu cầu: {role.totalDemandHours}h</td>
                                  <td className="px-3 py-2 text-center">Đã gán: {role.totalAllocatedHours}h</td>
                                  <td colSpan={2 + role.weeklyRoleMetrics.length} className="px-3 py-2"></td>
                                </tr>

                                {/* Liệt kê tất cả nhân sự có trong vai trò */}
                                {(() => {
                                  // Gom danh sách nhân sự duy nhất trong vai trò này
                                  const memberMap = new Map<number, { code: string; name: string }>();
                                  role.weeklyRoleMetrics.forEach((wm) => {
                                    wm.allocatedMembers.forEach((m) => {
                                      memberMap.set(m.employeeId, { code: m.employeeCode, name: m.fullName });
                                    });
                                  });

                                  if (memberMap.size === 0) {
                                    return (
                                      <tr className="bg-slate-50/40 text-[11px] text-slate-400">
                                        <td colSpan={5 + role.weeklyRoleMetrics.length} className="px-8 py-2 italic text-center">
                                          (Chưa có nhân sự nào được phân bổ cho vai trò này trong kỳ báo cáo)
                                        </td>
                                      </tr>
                                    );
                                  }

                                  return Array.from(memberMap.entries()).map(([empId, empInfo]) => (
                                    <tr key={empId} className="bg-slate-50/40 text-[11px] text-slate-600 hover:bg-slate-100/50 transition">
                                      <td className="px-8 py-2 sticky left-0 bg-inherit z-10 flex items-center gap-2">
                                        <UserCheck className="h-3.5 w-3.5 text-indigo-500 shrink-0" />
                                        <span>{empInfo.name}</span>
                                        <span className="text-[10px] text-slate-400">({empInfo.code})</span>
                                      </td>
                                      <td className="px-3 py-2 text-center text-slate-400">-</td>
                                      <td className="px-3 py-2 text-center font-bold text-slate-700">
                                        {role.weeklyRoleMetrics.reduce((sum, wm) => {
                                          const found = wm.allocatedMembers.find((m) => m.employeeId === empId);
                                          return sum + (found?.allocatedHours || 0);
                                        }, 0)}h
                                      </td>
                                      <td className="px-3 py-2 text-center text-slate-400">-</td>
                                      <td className="px-3 py-2 text-center text-slate-400">-</td>

                                      {/* Từng tuần của nhân viên */}
                                      {role.weeklyRoleMetrics.map((wm) => {
                                        const found = wm.allocatedMembers.find((m) => m.employeeId === empId);
                                        return (
                                          <td key={`${wm.year}_${wm.weekNumber}`} className="px-3 py-2 text-center border-l border-slate-100">
                                            {found ? (
                                              <div>
                                                <span className="font-bold text-slate-900">{found.allocatedHours}h</span>
                                                {found.allocationPercentage != null && (
                                                  <span className="text-[9px] text-indigo-600 block">({found.allocationPercentage}%)</span>
                                                )}
                                              </div>
                                            ) : (
                                              <span className="text-slate-300">-</span>
                                            )}
                                          </td>
                                        );
                                      })}
                                    </tr>
                                  ));
                                })()}
                              </>
                            )}
                          </Fragment>
                        );
                      })
                    )}
                  </tbody>

                  {/* Dòng tổng cộng toàn dự án */}
                  <tfoot className="bg-slate-100/90 text-xs font-bold text-slate-900 border-t-2 border-slate-300">
                    <tr>
                      <td className="px-4 py-3.5 sticky left-0 bg-slate-100 z-10">TỔNG CỘNG TOÀN DỰ ÁN</td>
                      <td className="px-3 py-3.5 text-center text-slate-900">{reportData.totalDemandHours}h</td>
                      <td className="px-3 py-3.5 text-center text-indigo-700">{reportData.totalAllocatedHours}h</td>
                      <td className="px-3 py-3.5 text-center">
                        {reportData.totalShortfallHours > 0 ? (
                          <span className="rounded-md bg-rose-100 px-2 py-0.5 text-xs text-rose-700">
                            Thiếu {reportData.totalShortfallHours}h
                          </span>
                        ) : (
                          <span className="rounded-md bg-emerald-100 px-2 py-0.5 text-xs text-emerald-700">
                            Đủ nhu cầu
                          </span>
                        )}
                      </td>
                      <td className="px-3 py-3.5 text-center text-indigo-600">{reportData.fulfillmentRate}%</td>

                      {reportData.weeklySummaries.map((w) => (
                        <td key={`${w.year}_${w.weekNumber}`} className="px-3 py-3.5 text-center border-l border-slate-200">
                          <div className="font-bold text-slate-900">{w.allocatedHours} / {w.demandHours}h</div>
                          {w.shortfallHours > 0 && (
                            <div className="text-[10px] text-rose-600 font-bold">- {w.shortfallHours}h</div>
                          )}
                        </td>
                      ))}
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>
          )}

          {/* ---------- TAB 2: TỔNG QUAN THEO TUẦN ---------- */}
          {activeTab === "weekly" && (
            <div className="rounded-3xl border border-slate-200 bg-white shadow-xs overflow-hidden">
              <div className="p-4 border-b border-slate-100 bg-slate-50/50">
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-700">
                  Bảng tổng quan phân bổ giờ theo từng tuần
                </h3>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-700">
                  <thead className="bg-slate-100/80 text-[11px] font-bold uppercase tracking-wider text-slate-600 border-b border-slate-200">
                    <tr>
                      <th className="px-4 py-3">Tuần</th>
                      <th className="px-4 py-3">Khoảng thời gian</th>
                      <th className="px-4 py-3 text-center">Nhu cầu (h)</th>
                      <th className="px-4 py-3 text-center">Đã phân bổ (h)</th>
                      <th className="px-4 py-3 text-center">Chênh lệch (h)</th>
                      <th className="px-4 py-3 text-center">Tỷ lệ đáp ứng</th>
                      <th className="px-4 py-3 text-center">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {reportData.weeklySummaries.map((w) => {
                      const isShortage = w.status === "SHORTAGE";
                      const isSurplus = w.status === "SURPLUS";

                      return (
                        <tr key={`${w.year}_${w.weekNumber}`} className={cn("hover:bg-slate-50/80 transition", isShortage && "bg-rose-50/20")}>
                          <td className="px-4 py-3.5 font-bold text-slate-900">
                            Tuần {w.weekNumber}/{w.year}
                          </td>
                          <td className="px-4 py-3.5 text-slate-500 text-xs">
                            {w.startDate} → {w.endDate}
                          </td>
                          <td className="px-4 py-3.5 text-center font-semibold text-slate-800">
                            {w.demandHours}h
                          </td>
                          <td className="px-4 py-3.5 text-center font-bold text-indigo-700">
                            {w.allocatedHours}h
                          </td>
                          <td className="px-4 py-3.5 text-center">
                            {isShortage ? (
                              <span className="font-bold text-rose-600">Thiếu {w.shortfallHours}h</span>
                            ) : isSurplus ? (
                              <span className="font-bold text-indigo-600">+{w.surplusHours}h</span>
                            ) : (
                              <span className="text-slate-400">0h</span>
                            )}
                          </td>
                          <td className="px-4 py-3.5 text-center font-bold">
                            <span className={cn(
                              w.fulfillmentRate >= 100 ? "text-emerald-600" : w.fulfillmentRate >= 80 ? "text-amber-600" : "text-rose-600"
                            )}>
                              {w.fulfillmentRate}%
                            </span>
                          </td>
                          <td className="px-4 py-3.5 text-center">
                            {isShortage ? (
                              <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2.5 py-0.5 text-[11px] font-bold text-rose-700">
                                <AlertTriangle className="h-3 w-3" /> Thiếu người
                              </span>
                            ) : isSurplus ? (
                              <span className="inline-flex items-center gap-1 rounded-full bg-indigo-100 px-2.5 py-0.5 text-[11px] font-bold text-indigo-700">
                                <CheckCircle2 className="h-3 w-3" /> Dôi dư
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-[11px] font-bold text-emerald-700">
                                <CheckCircle2 className="h-3 w-3" /> Đủ người
                              </span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* ---------- TAB 3: DANH SÁCH CẢNH BÁO THIẾU HỤT ---------- */}
          {activeTab === "alerts" && (
            <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-xs">
              <h3 className="text-sm font-bold text-slate-900 mb-4 flex items-center gap-2">
                <AlertTriangle className="h-4 w-4 text-rose-600" />
                Danh sách các tuần & vai trò bị hụt nhân sự ({reportData.shortageAlerts.length})
              </h3>

              {reportData.shortageAlerts.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-12 text-center">
                  <CheckCircle2 className="h-12 w-12 text-emerald-500 mb-2" />
                  <p className="text-sm font-bold text-slate-800">Không có cảnh báo thiếu hụt nào</p>
                  <p className="text-xs text-slate-500 mt-1">Dự án đã được phân bổ đủ hoặc vượt nhu cầu cho tất cả các tuần.</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {reportData.shortageAlerts.map((alert, idx) => (
                    <div
                      key={idx}
                      className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-2xl border border-rose-200 bg-rose-50/40 p-4 shadow-2xs hover:bg-rose-50 transition"
                    >
                      <div className="flex items-start gap-3">
                        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-rose-100 text-rose-600 border border-rose-200">
                          <UserX className="h-5 w-5" />
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-slate-900 text-xs">
                              {alert.weekLabel}
                            </span>
                            <span className="rounded-md bg-slate-200/80 px-2 py-0.5 text-[10px] font-bold text-slate-700">
                              {alert.roleName}
                            </span>
                            <span className={cn(
                              "rounded-full px-2 py-0.2 text-[10px] font-bold",
                              alert.severity === "HIGH" ? "bg-rose-600 text-white" : "bg-amber-100 text-amber-800"
                            )}>
                              {alert.severity === "HIGH" ? "Mức độ: Cao" : "Mức độ: Vừa"}
                            </span>
                          </div>
                          <p className="text-xs text-slate-600 mt-1">
                            {alert.message}
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center gap-3 shrink-0 self-end sm:self-center">
                        <div className="text-right">
                          <span className="text-[10px] text-slate-400 block">Số giờ thiếu</span>
                          <span className="text-sm font-extrabold text-rose-600">
                            {alert.missingHours} giờ
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </>
      )}

      {/* Loading Skeleton */}
      {isLoading && (
        <div className="flex flex-col items-center justify-center py-24 text-center">
          <RefreshCw className="h-8 w-8 animate-spin text-indigo-600 mb-3" />
          <p className="text-xs font-semibold text-slate-600">Đang nạp dữ liệu báo cáo phân bổ dự án...</p>
        </div>
      )}
    </div>
  );
}
