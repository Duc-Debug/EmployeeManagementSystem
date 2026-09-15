import { useState, useEffect } from "react";
import {
  TrendingUp,
  AlertTriangle,
  CheckCircle2,
  Users,
  RefreshCw,
  Building2,
  Calendar,
  Clock,
  Bookmark,
  ShieldAlert,
  Info
} from "lucide-react";
import { cn } from "@/lib/utils";
import {
  getCapacityForecastReport,
  type CapacityForecastReportData,
  type WeeklyForecastItem,
  type ForecastStatus
} from "@/lib/api/capacity-forecast";
import { getIsoWeeksInYear, getIsoWeekDetails } from "@/lib/iso-week";
import { getOrgTree } from "@/lib/api/org-units";
import type { OrgUnitTreeNode } from "@/types/hrm";

export default function CapacityForecastReportView() {
  const now = new Date();
  const currentIsoDetails = getIsoWeekDetails(now);
  const [fromYear, setFromYear] = useState<number>(currentIsoDetails.year);
  const [fromWeek, setFromWeek] = useState<number>(currentIsoDetails.week);
  const [durationWeeks, setDurationWeeks] = useState<number>(12);
  const [selectedOrgUnitId, setSelectedOrgUnitId] = useState<string>("");

  const [reportData, setReportData] = useState<CapacityForecastReportData | null>(null);
  const [orgUnits, setOrgUnits] = useState<OrgUnitTreeNode[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [forbidden, setForbidden] = useState<boolean>(false);
  const [hoveredWeek, setHoveredWeek] = useState<WeeklyForecastItem | null>(null);

  // Tải danh sách bộ phận cho dropdown filter
  useEffect(() => {
    async function loadOrgUnitsData() {
      try {
        const tree = await getOrgTree();
        const flatten = (nodes: readonly OrgUnitTreeNode[]): OrgUnitTreeNode[] => {
          let list: OrgUnitTreeNode[] = [];
          for (const n of nodes) {
            list.push(n);
            if (n.children && n.children.length > 0) {
              list = list.concat(flatten(n.children));
            }
          }
          return list;
        };
        setOrgUnits(flatten([...(tree || [])]));
      } catch (err) {
        console.warn("Không thể tải danh sách phòng ban:", err);
      }
    }
    loadOrgUnitsData();
  }, []);

  const fetchReport = async () => {
    setIsLoading(true);
    setError(null);
    setForbidden(false);
    try {
      const data = await getCapacityForecastReport({
        fromYear,
        fromWeek,
        durationWeeks,
        orgUnitId: selectedOrgUnitId ? Number(selectedOrgUnitId) : undefined
      });
      setReportData(data);
    } catch (err: any) {
      if (err?.status === 403 || String(err?.message || "").includes("403")) {
        setForbidden(true);
      } else {
        setError(err?.message || "Đã xảy ra lỗi khi nạp dữ liệu báo cáo dự báo năng lực.");
      }
      setReportData(null);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchReport();
  }, [fromYear, fromWeek, durationWeeks, selectedOrgUnitId]);

  const maxWeeksInFromYear = getIsoWeeksInYear(fromYear);

  const formatHours = (val: number | null | undefined): string => {
    if (val === null || val === undefined) return "0.0";
    return val.toLocaleString("vi-VN", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
  };

  const renderStatusBadge = (status: ForecastStatus) => {
    switch (status) {
      case "OVER_CAPACITY":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold rounded-full bg-red-100 text-red-700 border border-red-200 dark:bg-red-950/40 dark:text-red-400 dark:border-red-900/50">
            <AlertTriangle className="w-3.5 h-3.5" /> Vượt năng lực
          </span>
        );
      case "NEAR_FULL":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold rounded-full bg-amber-100 text-amber-700 border border-amber-200 dark:bg-amber-950/40 dark:text-amber-400 dark:border-amber-900/50">
            <Clock className="w-3.5 h-3.5" /> Gần đầy
          </span>
        );
      case "AVAILABLE":
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold rounded-full bg-emerald-100 text-emerald-700 border border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-400 dark:border-emerald-900/50">
            <CheckCircle2 className="w-3.5 h-3.5" /> Còn trống
          </span>
        );
    }
  };

  // Helper tính tọa độ SVG Chart
  const renderChart = (weeks: WeeklyForecastItem[]) => {
    if (!weeks || weeks.length === 0) return null;

    const maxVal = Math.max(
      ...weeks.flatMap(w => [w.availableHours, w.committedHours, Math.abs(w.projectedRemainingHours)]),
      100
    );

    const svgWidth = 800;
    const svgHeight = 220;
    const padding = 35;
    const chartWidth = svgWidth - padding * 2;
    const chartHeight = svgHeight - padding * 2;

    const getX = (index: number) => {
      if (weeks.length === 1) return svgWidth / 2;
      return padding + (index / (weeks.length - 1)) * chartWidth;
    };

    const getY = (val: number) => {
      const normalized = Math.max(0, val) / maxVal;
      return svgHeight - padding - normalized * chartHeight;
    };

    const availPoints = weeks.map((w, i) => `${getX(i)},${getY(w.availableHours)}`).join(" ");
    const commitPoints = weeks.map((w, i) => `${getX(i)},${getY(w.committedHours)}`).join(" ");
    const remainingPoints = weeks.map((w, i) => `${getX(i)},${getY(w.projectedRemainingHours)}`).join(" ");

    return (
      <div className="relative w-full overflow-x-auto">
        <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} className="w-full h-auto max-h-64 select-none">
          {/* Grid lines */}
          {[0, 0.25, 0.5, 0.75, 1].map((ratio, i) => {
            const y = svgHeight - padding - ratio * chartHeight;
            const valLabel = Math.round(ratio * maxVal);
            return (
              <g key={i}>
                <line x1={padding} y1={y} x2={svgWidth - padding} y2={y} stroke="currentColor" strokeDasharray="3 3" className="text-gray-200 dark:text-gray-800" />
                <text x={padding - 6} y={y + 3} textAnchor="end" className="text-[10px] fill-gray-400 font-mono">{valLabel}h</text>
              </g>
            );
          })}

          {/* Area & Lines */}
          <polyline fill="none" stroke="#3b82f6" strokeWidth="2.5" points={availPoints} strokeLinecap="round" strokeLinejoin="round" />
          <polyline fill="none" stroke="#8b5cf6" strokeWidth="2.5" points={commitPoints} strokeLinecap="round" strokeLinejoin="round" />
          <polyline fill="none" stroke="#10b981" strokeWidth="2.5" points={remainingPoints} strokeLinecap="round" strokeLinejoin="round" strokeDasharray="4 2" />

          {/* Dots */}
          {weeks.map((w, i) => {
            const cx = getX(i);
            const cyAvail = getY(w.availableHours);
            const cyCommit = getY(w.committedHours);
            const cyRem = getY(w.projectedRemainingHours);
            const isHovered = hoveredWeek?.weekNumber === w.weekNumber && hoveredWeek?.year === w.year;

            return (
              <g key={i} className="cursor-pointer" onMouseEnter={() => setHoveredWeek(w)} onMouseLeave={() => setHoveredWeek(null)}>
                {/* Vertical hover guide */}
                {isHovered && (
                  <line x1={cx} y1={padding} x2={cx} y2={svgHeight - padding} stroke="#94a3b8" strokeWidth="1" strokeDasharray="2 2" />
                )}
                <circle cx={cx} cy={cyAvail} r={isHovered ? "6" : "4"} fill="#3b82f6" className="transition-all" />
                <circle cx={cx} cy={cyCommit} r={isHovered ? "6" : "4"} fill="#8b5cf6" className="transition-all" />
                <circle cx={cx} cy={cyRem} r={isHovered ? "6" : "4"} fill={w.projectedRemainingHours < 0 ? "#ef4444" : "#10b981"} className="transition-all" />

                <text x={cx} y={svgHeight - padding + 16} textAnchor="middle" className={cn("text-[10px] font-medium fill-gray-500", isHovered && "fill-blue-600 font-bold")}>
                  T{w.weekNumber}
                </text>
              </g>
            );
          })}
        </svg>

        {/* Legend */}
        <div className="flex flex-wrap items-center justify-center gap-6 mt-2 text-xs font-medium text-gray-600 dark:text-gray-400">
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-blue-500"></span>
            <span>Tổng giờ khả dụng</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-purple-500"></span>
            <span>Giờ đã cam kết (Phân bổ)</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-emerald-500"></span>
            <span>Giờ còn trống dự kiến</span>
          </div>
        </div>
      </div>
    );
  };

  if (forbidden) {
    return (
      <div className="p-8 max-w-4xl mx-auto">
        <div className="rounded-xl border border-red-200 bg-red-50 p-6 dark:border-red-900/50 dark:bg-red-950/30 text-center">
          <ShieldAlert className="w-12 h-12 text-red-600 dark:text-red-400 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-red-900 dark:text-red-200">Từ chối truy cập (403 Forbidden)</h3>
          <p className="mt-2 text-sm text-red-700 dark:text-red-300">
            Bạn không có quyền xem báo cáo dự báo năng lực. Chức năng này dành riêng cho Ban giám đốc (VT-01) và Quản lý nguồn lực (VT-03).
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <TrendingUp className="w-7 h-7 text-blue-600 dark:text-blue-400" />
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Báo cáo dự báo năng lực các tuần tới</h1>
          </div>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Theo dõi khả dụng ròng, giờ cam kết chính thức và giờ giữ chỗ trong 4 đến 16 tuần ISO tương lai.
          </p>
        </div>

        <button
          onClick={fetchReport}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-4 py-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-lg text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 shadow-sm transition"
        >
          <RefreshCw className={cn("w-4 h-4", isLoading && "animate-spin")} />
          Tải lại dữ liệu
        </button>
      </div>

      {/* Filter Bar */}
      <div className="bg-white dark:bg-gray-800 rounded-xl p-4 border border-gray-200 dark:border-gray-700 shadow-sm grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div>
          <label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 mb-1 flex items-center gap-1.5">
            <Building2 className="w-3.5 h-3.5" /> Đơn vị tổ chức
          </label>
          <select
            value={selectedOrgUnitId}
            onChange={(e) => setSelectedOrgUnitId(e.target.value)}
            className="w-full px-3 py-2 bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg text-sm focus:ring-2 focus:ring-blue-500"
          >
            <option value="">-- Toàn công ty --</option>
            {orgUnits.map((u) => (
              <option key={u.id} value={u.id}>
                {u.unitName}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 mb-1 flex items-center gap-1.5">
            <Calendar className="w-3.5 h-3.5" /> Năm bắt đầu
          </label>
          <select
            value={fromYear}
            onChange={(e) => setFromYear(Number(e.target.value))}
            className="w-full px-3 py-2 bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg text-sm focus:ring-2 focus:ring-blue-500"
          >
            {[fromYear - 1, fromYear, fromYear + 1].map((y) => (
              <option key={y} value={y}>Năm {y}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 mb-1 flex items-center gap-1.5">
            <Clock className="w-3.5 h-3.5" /> Tuần bắt đầu (ISO)
          </label>
          <select
            value={fromWeek}
            onChange={(e) => setFromWeek(Number(e.target.value))}
            className="w-full px-3 py-2 bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg text-sm focus:ring-2 focus:ring-blue-500"
          >
            {Array.from({ length: maxWeeksInFromYear }, (_, i) => i + 1).map((w) => (
              <option key={w} value={w}>Tuần {w}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 mb-1 flex items-center gap-1.5">
            <TrendingUp className="w-3.5 h-3.5" /> Số tuần dự báo
          </label>
          <select
            value={durationWeeks}
            onChange={(e) => setDurationWeeks(Number(e.target.value))}
            className="w-full px-3 py-2 bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-lg text-sm focus:ring-2 focus:ring-blue-500"
          >
            <option value={4}>4 tuần</option>
            <option value={8}>8 tuần</option>
            <option value={12}>12 tuần (Mặc định)</option>
            <option value={16}>16 tuần</option>
          </select>
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-red-700 dark:border-red-900/50 dark:bg-red-950/30 text-sm flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 flex-shrink-0 text-red-500" />
          <span>{error}</span>
        </div>
      )}

      {/* Loading state */}
      {isLoading ? (
        <div className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 animate-pulse">
            {[1, 2, 3, 4].map((n) => (
              <div key={n} className="h-28 bg-gray-200 dark:bg-gray-800 rounded-xl"></div>
            ))}
          </div>
          <div className="h-64 bg-gray-200 dark:bg-gray-800 rounded-xl animate-pulse"></div>
        </div>
      ) : reportData ? (
        <>
          {/* KPI Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white dark:bg-gray-800 p-5 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-gray-500 dark:text-gray-400">Tổng giờ khả dụng</span>
                <div className="p-2 bg-blue-50 dark:bg-blue-950/50 rounded-lg text-blue-600">
                  <Users className="w-5 h-5" />
                </div>
              </div>
              <p className="text-2xl font-bold text-gray-900 dark:text-white mt-2">
                {formatHours(reportData.summary.totalAvailableHours)} <span className="text-xs font-normal text-gray-500">giờ</span>
              </p>
              <p className="text-xs text-gray-400 mt-1">Trong {reportData.durationWeeks} tuần báo cáo</p>
            </div>

            <div className="bg-white dark:bg-gray-800 p-5 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-gray-500 dark:text-gray-400">Tổng giờ đã cam kết</span>
                <div className="p-2 bg-purple-50 dark:bg-purple-950/50 rounded-lg text-purple-600">
                  <CheckCircle2 className="w-5 h-5" />
                </div>
              </div>
              <p className="text-2xl font-bold text-gray-900 dark:text-white mt-2">
                {formatHours(reportData.summary.totalCommittedHours)} <span className="text-xs font-normal text-gray-500">giờ</span>
              </p>
              <p className="text-xs text-gray-400 mt-1">Từ các dòng phân bổ chính thức</p>
            </div>

            <div className="bg-white dark:bg-gray-800 p-5 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-gray-500 dark:text-gray-400">Tổng giờ giữ chỗ</span>
                <div className="p-2 bg-amber-50 dark:bg-amber-950/50 rounded-lg text-amber-600">
                  <Bookmark className="w-5 h-5" />
                </div>
              </div>
              <p className="text-2xl font-bold text-gray-900 dark:text-white mt-2">
                {formatHours(reportData.summary.totalReservedHours)} <span className="text-xs font-normal text-gray-500">giờ</span>
              </p>
              <p className="text-xs text-amber-600 dark:text-amber-400 mt-1 font-medium">Giữ chỗ chưa là cam kết</p>
            </div>

            <div className={cn(
              "p-5 rounded-xl border shadow-sm transition",
              reportData.summary.totalProjectedRemainingHours < 0 || reportData.summary.overCapacityWeeks > 0
                ? "bg-red-50 dark:bg-red-950/30 border-red-200 dark:border-red-900/50"
                : "bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-700"
            )}>
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-gray-500 dark:text-gray-400">Còn trống dự kiến</span>
                <div className={cn(
                  "p-2 rounded-lg",
                  reportData.summary.totalProjectedRemainingHours < 0
                    ? "bg-red-100 text-red-600"
                    : "bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600"
                )}>
                  {reportData.summary.totalProjectedRemainingHours < 0 ? <AlertTriangle className="w-5 h-5" /> : <TrendingUp className="w-5 h-5" />}
                </div>
              </div>
              <p className={cn(
                "text-2xl font-bold mt-2",
                reportData.summary.totalProjectedRemainingHours < 0 ? "text-red-600 dark:text-red-400" : "text-gray-900 dark:text-white"
              )}>
                {formatHours(reportData.summary.totalProjectedRemainingHours)} <span className="text-xs font-normal text-gray-500">giờ</span>
              </p>
              <p className="text-xs mt-1 font-medium text-gray-500">
                {reportData.summary.overCapacityWeeks > 0 ? (
                  <span className="text-red-600 dark:text-red-400 font-semibold">⚠️ {reportData.summary.overCapacityWeeks} tuần vượt năng lực</span>
                ) : (
                  <span className="text-emerald-600 dark:text-emerald-400">Cân đối năng lực tốt</span>
                )}
              </p>
            </div>
          </div>

          {/* Interactive Visual Chart */}
          <div className="bg-white dark:bg-gray-800 p-6 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm">
            <h3 className="text-base font-bold text-gray-900 dark:text-white mb-4">
              Biểu đồ xu hướng năng lực ({reportData.durationWeeks} tuần)
            </h3>
            {renderChart(reportData.weeks)}

            {/* Hovered Tooltip detail */}
            {hoveredWeek && (
              <div className="mt-4 p-3 bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900/50 rounded-lg text-xs grid grid-cols-2 sm:grid-cols-4 gap-2">
                <div><span className="font-semibold">Tuần {hoveredWeek.weekNumber} ({hoveredWeek.startDate} - {hoveredWeek.endDate})</span></div>
                <div>Khả dụng: <span className="font-bold">{formatHours(hoveredWeek.availableHours)}h</span></div>
                <div>Cam kết: <span className="font-bold text-purple-600">{formatHours(hoveredWeek.committedHours)}h</span></div>
                <div>Giữ chỗ: <span className="font-bold text-amber-600">{formatHours(hoveredWeek.reservedHours)}h</span></div>
              </div>
            )}
          </div>

          {/* Detailed Data Table */}
          <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden">
            <div className="p-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
              <h3 className="text-base font-bold text-gray-900 dark:text-white">Chi tiết số liệu theo tuần ISO</h3>
              <span className="text-xs text-gray-500">Đơn vị: {reportData.orgUnitName}</span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="text-xs text-gray-500 uppercase bg-gray-50 dark:bg-gray-900/60 border-b border-gray-200 dark:border-gray-700">
                  <tr>
                    <th className="px-4 py-3">Tuần (ISO)</th>
                    <th className="px-4 py-3 text-right">Khả dụng</th>
                    <th className="px-4 py-3 text-right">Cam kết</th>
                    <th className="px-4 py-3 text-right">Giữ chỗ</th>
                    <th className="px-4 py-3 text-right">Còn trống (Chưa giữ chỗ)</th>
                    <th className="px-4 py-3 text-right">Còn trống (Cả giữ chỗ)</th>
                    <th className="px-4 py-3 text-right">% Cam kết</th>
                    <th className="px-4 py-3 text-right">% Dự kiến</th>
                    <th className="px-4 py-3 text-center">Trạng thái</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                  {reportData.weeks.map((w) => (
                    <tr
                      key={`${w.year}-${w.weekNumber}`}
                      className={cn(
                        "hover:bg-gray-50 dark:hover:bg-gray-700/50 transition",
                        w.projectedRemainingHours < 0 && "bg-red-50/40 dark:bg-red-950/20"
                      )}
                    >
                      <td className="px-4 py-3 font-semibold text-gray-900 dark:text-white whitespace-nowrap">
                        T{w.weekNumber} <span className="text-xs font-normal text-gray-400">({w.startDate} - {w.endDate})</span>
                      </td>
                      <td className="px-4 py-3 text-right font-medium text-gray-700 dark:text-gray-300">
                        {formatHours(w.availableHours)}h
                      </td>
                      <td className="px-4 py-3 text-right font-medium text-purple-700 dark:text-purple-300">
                        {formatHours(w.committedHours)}h
                      </td>
                      <td className="px-4 py-3 text-right font-medium text-amber-700 dark:text-amber-300">
                        {formatHours(w.reservedHours)}h
                      </td>
                      <td className={cn("px-4 py-3 text-right font-semibold", w.committedRemainingHours < 0 ? "text-red-600" : "text-gray-800 dark:text-gray-200")}>
                        {formatHours(w.committedRemainingHours)}h
                      </td>
                      <td className={cn("px-4 py-3 text-right font-bold", w.projectedRemainingHours < 0 ? "text-red-600 dark:text-red-400" : "text-emerald-600 dark:text-emerald-400")}>
                        {formatHours(w.projectedRemainingHours)}h
                      </td>
                      <td className="px-4 py-3 text-right text-xs">
                        {w.committedUtilization !== null ? `${w.committedUtilization}%` : "Không xác định"}
                      </td>
                      <td className="px-4 py-3 text-right text-xs font-semibold">
                        {w.projectedUtilization !== null ? `${w.projectedUtilization}%` : "Không xác định"}
                      </td>
                      <td className="px-4 py-3 text-center whitespace-nowrap">
                        {renderStatusBadge(w.status)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Note legend */}
            <div className="p-4 bg-gray-50 dark:bg-gray-900/40 border-t border-gray-200 dark:border-gray-700 text-xs text-gray-500 flex items-start gap-2">
              <Info className="w-4 h-4 text-blue-500 flex-shrink-0 mt-0.5" />
              <span>
                <strong>Ghi chú nghiệp vụ:</strong> Giữ chỗ (Resource Reservations) đại diện cho dự kiến nguồn lực của các cơ hội dự án, không phải cam kết phân bổ chính thức. Giá trị âm ở phần Giờ còn trống thể hiện số giờ vượt năng lực cần lưu ý lấp đầy hoặc tuyển bổ sung.
              </span>
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}

