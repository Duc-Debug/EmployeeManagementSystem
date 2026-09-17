"use client";

import React, { useState, useEffect, useCallback, useMemo } from "react";
import {
  ArrowLeft,
  AlertTriangle,
  Users,
  ShieldCheck,
  CheckCircle2,
  Clock,
  Building2,
  CalendarRange,
  RefreshCw,
  Award,
  Flame,
  ChevronDown,
  ChevronRight,
  TrendingUp,
  Briefcase,
  Layers,
  Download,
} from "lucide-react";
import {
  compareScenarios,
  type ScenarioComparisonResult,
} from "@/lib/api/simulation-scenarios";
import { cn } from "@/lib/utils";

interface SimulationScenarioComparisonViewProps {
  scenarioIds: number[];
  onBack: () => void;
}

export const SimulationScenarioComparisonView: React.FC<SimulationScenarioComparisonViewProps> = ({
  scenarioIds,
  onBack,
}) => {
  const [data, setData] = useState<ScenarioComparisonResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedSection, setExpandedSection] = useState<"weekly" | "employees" | "both">("both");

  const loadComparison = useCallback(async () => {
    if (scenarioIds.length < 2) {
      setError("Cần chọn ít nhất 2 kịch bản để thực hiện so sánh.");
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res = await compareScenarios({ scenarioIds });
      setData(res);
    } catch (err: unknown) {
      setError(
        err instanceof Error
          ? err.message
          : "Không thể tải dữ liệu so sánh các kịch bản. Vui lòng thử lại."
      );
    } finally {
      setLoading(false);
    }
  }, [scenarioIds]);

  useEffect(() => {
    loadComparison();
  }, [loadComparison]);

  // Logic phát hiện kịch bản tối ưu nhất (Khuyến nghị cho BGĐ)
  const recommendedScenarioId = useMemo<number | null>(() => {
    if (!data || data.scenarios.length === 0) return null;
    const sorted = [...data.scenarios].sort((a, b) => {
      // 1. Ít nhân sự quá tải hơn
      if (a.overloadedEmployeesCount !== b.overloadedEmployeesCount) {
        return a.overloadedEmployeesCount - b.overloadedEmployeesCount;
      }
      // 2. Tổng giờ thiếu hụt ít hơn
      if (a.totalShortfallHours !== b.totalShortfallHours) {
        return a.totalShortfallHours - b.totalShortfallHours;
      }
      // 3. Đỉnh tải thấp hơn
      if (a.peakUtilizationPercentage !== b.peakUtilizationPercentage) {
        return a.peakUtilizationPercentage - b.peakUtilizationPercentage;
      }
      // 4. Giờ làm thêm cần thiết ít hơn
      if (a.totalRequiredAdditionalHours !== b.totalRequiredAdditionalHours) {
        return a.totalRequiredAdditionalHours - b.totalRequiredAdditionalHours;
      }
      // 5. Tỷ lệ tải trung bình tối ưu hơn
      return (a.averageUtilizationPercentage ?? 0) - (b.averageUtilizationPercentage ?? 0);
    });
    return sorted[0]?.scenarioId ?? null;
  }, [data]);

  const formatNumber = (num: number | undefined | null, decimals = 1) => {
    if (num === undefined || num === null || Number.isNaN(Number(num))) return "0";
    return Number(num).toLocaleString("vi-VN", {
      minimumFractionDigits: 0,
      maximumFractionDigits: decimals,
    });
  };

  const formatPercent = (num: number | undefined | null) => {
    if (num === undefined || num === null || Number.isNaN(Number(num))) return "0%";
    return `${Number(num).toFixed(1)}%`;
  };

  const formatDateTime = (dateStr: string) => {
    if (!dateStr) return "--:--";
    try {
      const d = new Date(dateStr);
      return d.toLocaleString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return dateStr;
    }
  };

  const getUtilizationBadge = (util: number) => {
    if (util <= 100) {
      return {
        bg: "bg-emerald-50 text-emerald-700 border-emerald-200",
        label: "An toàn",
      };
    }
    if (util <= 110) {
      return {
        bg: "bg-amber-50 text-amber-700 border-amber-200",
        label: "Cận ngưỡng",
      };
    }
    return {
      bg: "bg-rose-50 text-rose-700 border-rose-200",
      label: "Vượt tải",
    };
  };

  // Thu thập danh sách tuần hợp nhất để so sánh từng tuần
  const allWeekKeys = useMemo(() => {
    if (!data) return [];
    const set = new Set<string>();
    for (const scn of data.scenarios) {
      for (const wm of scn.weeklyMetrics) {
        set.add(`${wm.year}-W${String(wm.weekNumber).padStart(2, "0")}`);
      }
    }
    return Array.from(set).sort();
  }, [data]);

  // Xuất bảng tổng hợp so sánh ra file CSV chuẩn UTF-8 BOM cho Ban Giám Đốc (VT-01)
  const handleExportCsv = useCallback(() => {
    if (!data || data.scenarios.length === 0) return;

    const rows: string[][] = [
      ["BÁO CÁO ĐỐI CHIẾU KỊCH BẢN MÔ PHỎNG NGUỒN LỰC (NCL-08-CN-004)"],
      [`Thời gian đối chiếu: ${formatDateTime(data.comparedAt)}`],
      ["Nguyên tắc QTN-14 Sandbox: Dữ liệu mô phỏng độc lập, không làm thay đổi phân bổ thật."],
      [],
      [
        "Mã kịch bản",
        "Tên kịch bản",
        "Đơn vị / Phòng ban",
        "Trạng thái",
        "Tuần bắt đầu",
        "Năm",
        "Số tuần",
        "Số nhân sự quá tải",
        "Tổng giờ thiếu hụt (h)",
        "Giờ làm thêm cần thiết (h)",
        "Giờ nhu cầu giả định (h)",
        "Tổng khối lượng (h)",
        "Giờ khả dụng (h)",
        "Tải trung bình (%)",
        "Đỉnh tải (%)",
        "Khuyến nghị",
      ],
    ];

    for (const scn of data.scenarios) {
      const isRec = scn.scenarioId === recommendedScenarioId;
      rows.push([
        `"${(scn.scenarioCode || "").replace(/"/g, '""')}"`,
        `"${(scn.scenarioName || "").replace(/"/g, '""')}"`,
        `"${(scn.orgUnitName || "").replace(/"/g, '""')}"`,
        `"${scn.status || ""}"`,
        `${scn.fromWeek}`,
        `${scn.fromYear}`,
        `${scn.durationWeeks}`,
        `${scn.overloadedEmployeesCount}`,
        `${scn.totalShortfallHours ?? 0}`,
        `${scn.totalRequiredAdditionalHours ?? 0}`,
        `${scn.totalDemandHours ?? 0}`,
        `${scn.totalWorkloadHours ?? 0}`,
        `${scn.totalAvailableHours ?? 0}`,
        `${Number(scn.averageUtilizationPercentage ?? 0).toFixed(1)}%`,
        `${Number(scn.peakUtilizationPercentage ?? 0).toFixed(1)}%`,
        isRec ? "Tối ưu nhất" : "Phương án",
      ]);
    }

    // Weekly metrics section
    rows.push([]);
    rows.push(["CHI TIẾT TIẾN ĐỘ TẢI TỪNG TUẦN"]);
    const weeklyHeader = ["Tuần / Năm"];
    for (const scn of data.scenarios) {
      weeklyHeader.push(`${scn.scenarioCode} - Khối lượng (h)`);
      weeklyHeader.push(`${scn.scenarioCode} - Tải (%)`);
    }
    rows.push(weeklyHeader);

    for (const weekKey of allWeekKeys) {
      const [yearStr, weekStr] = weekKey.split("-W");
      const yr = Number(yearStr);
      const wk = Number(weekStr);
      const weekRow = [`Tuần ${wk}, ${yr}`];

      for (const scn of data.scenarios) {
        const wm = scn.weeklyMetrics.find((m) => m.year === yr && m.weekNumber === wk);
        if (wm) {
          weekRow.push(`${wm.scenarioWorkloadHours ?? wm.totalWorkloadHours ?? 0}`);
          weekRow.push(`${Number(wm.utilizationPercentage ?? 0).toFixed(1)}%`);
        } else {
          weekRow.push("--");
          weekRow.push("--");
        }
      }
      rows.push(weekRow);
    }

    // Overloaded employee section
    rows.push([]);
    rows.push(["DANH SÁCH NHÂN SỰ QUÁ TẢI GIỮA CÁC KỊCH BẢN"]);
    rows.push([
      "Mã kịch bản",
      "Mã NV",
      "Họ và tên",
      "Chức danh",
      "Số tuần quá tải",
      "Giờ vượt tối đa (h)",
      "Đỉnh tải (%)",
    ]);

    for (const scn of data.scenarios) {
      if (scn.overloadedEmployees && scn.overloadedEmployees.length > 0) {
        for (const emp of scn.overloadedEmployees) {
          rows.push([
            `"${scn.scenarioCode}"`,
            `"${emp.employeeCode || ""}"`,
            `"${(emp.fullName || "").replace(/"/g, '""')}"`,
            `"${(emp.professionalRole || "Chuyên viên").replace(/"/g, '""')}"`,
            `${emp.overloadedWeeksCount}`,
            `${emp.maxExcessHours ?? 0}`,
            `${Number(emp.peakUtilizationPercentage ?? 0).toFixed(1)}%`,
          ]);
        }
      }
    }

    const csvContent = "\uFEFF" + rows.map((r) => r.join(",")).join("\r\n");
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    const timestamp = new Date().toISOString().replace(/[-:T]/g, "").slice(0, 12);
    link.setAttribute("href", url);
    link.setAttribute("download", `so_sanh_kich_ban_${timestamp}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }, [data, recommendedScenarioId, allWeekKeys]);

  return (
    <div className="space-y-6 pb-16 animate-in fade-in duration-150">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
        <div className="space-y-1">
          <div className="flex items-center space-x-3">
            <button
              onClick={onBack}
              className="p-2 -ml-2 rounded-xl text-slate-500 hover:text-slate-900 hover:bg-slate-100 transition"
              title="Quay lại danh sách kịch bản"
            >
              <ArrowLeft className="h-5 w-5" />
            </button>
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 border border-indigo-100 shadow-xs">
              <Layers className="h-5 w-5" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h1 className="text-xl font-bold text-slate-900">
                  So Sánh Đa Kịch Bản Mô Phỏng
                </h1>
                <span className="font-mono text-xs font-semibold bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-md border border-indigo-100">
                  NCL-08-CN-004
                </span>
              </div>
              <p className="text-xs text-slate-500">
                Đối chiếu năng lực, nhân sự quá tải và tổng giờ thiếu hụt giữa {scenarioIds.length} phương án kịch bản
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center space-x-2 self-start md:self-auto">
          <button
            onClick={handleExportCsv}
            disabled={loading || !data}
            className="rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-xs flex items-center space-x-1.5 disabled:opacity-50 cursor-pointer"
            title="Xuất bảng đối chiếu ra file CSV"
          >
            <Download className="h-3.5 w-3.5 text-indigo-600" />
            <span>Xuất CSV</span>
          </button>
          <button
            onClick={loadComparison}
            disabled={loading}
            className="rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-xs flex items-center space-x-1.5 disabled:opacity-50 cursor-pointer"
          >
            <RefreshCw className={cn("h-3.5 w-3.5", loading && "animate-spin")} />
            <span>Làm mới</span>
          </button>
        </div>
      </div>

      {/* QTN-14 Sandbox Isolation Banner */}
      <div className="flex items-center space-x-2.5 text-xs text-slate-600 bg-slate-50 px-4 py-3 rounded-xl border border-slate-200">
        <ShieldCheck className="h-4 w-4 text-emerald-600 shrink-0" />
        <span>
          <strong>Nguyên tắc QTN-14 Sandbox:</strong> Việc so sánh và đánh giá các kịch bản này hoàn toàn độc lập, không làm thay đổi các phân bổ dự án và dữ liệu năng lực thực tế trong hệ thống.
        </span>
      </div>

      {/* Loading state */}
      {loading && (
        <div className="flex items-center justify-center min-h-[350px] bg-white rounded-2xl border border-slate-200">
          <div className="flex flex-col items-center space-y-3">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-indigo-600 border-t-transparent" />
            <p className="text-xs text-slate-500 font-medium">
              Đang tổng hợp và đối chiếu số liệu các kịch bản...
            </p>
          </div>
        </div>
      )}

      {/* Error state */}
      {!loading && error && (
        <div className="p-8 text-center bg-white rounded-2xl border border-slate-200 space-y-3">
          <AlertTriangle className="h-8 w-8 text-rose-500 mx-auto" />
          <h3 className="text-sm font-bold text-slate-900">Không thể thực hiện so sánh</h3>
          <p className="text-xs text-slate-600 max-w-md mx-auto">{error}</p>
          <button
            onClick={loadComparison}
            className="text-xs font-semibold text-indigo-600 hover:text-indigo-700 underline"
          >
            Thử lại
          </button>
        </div>
      )}

      {/* Comparison Content */}
      {!loading && !error && data && (
        <>
          {/* Alignment Warnings */}
          {(!data.isTimeframeAligned || !data.isOrgUnitAligned) && (
            <div className="space-y-2">
              {!data.isTimeframeAligned && (
                <div className="flex items-start space-x-2.5 text-xs text-amber-800 bg-amber-50 px-4 py-3 rounded-xl border border-amber-200">
                  <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
                  <span>
                    <strong>Lưu ý về khung thời gian:</strong> Các kịch bản được chọn có tuần bắt đầu, năm hoặc số tuần mô phỏng không đồng nhất. Các chỉ số lũy kế (tổng giờ) phản ánh theo từng phạm vi tuần riêng biệt của mỗi kịch bản.
                  </span>
                </div>
              )}
              {!data.isOrgUnitAligned && (
                <div className="flex items-start space-x-2.5 text-xs text-blue-800 bg-blue-50 px-4 py-3 rounded-xl border border-blue-200">
                  <Building2 className="h-4 w-4 text-blue-600 shrink-0 mt-0.5" />
                  <span>
                    <strong>Lưu ý về phòng ban:</strong> Các kịch bản thuộc các đơn vị/phòng ban khác nhau. So sánh giúp đánh giá sức chịu tải tương quan giữa các phòng ban.
                  </span>
                </div>
              )}
            </div>
          )}

          {/* Smart Recommendation Banner for Board of Directors (VT-01) */}
          {recommendedScenarioId !== null && (
            <div className="bg-gradient-to-r from-emerald-50 via-teal-50 to-indigo-50 p-4 rounded-2xl border border-emerald-200 shadow-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
              <div className="flex items-center space-x-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-600 text-white shadow-xs shrink-0">
                  <Award className="h-5 w-5" />
                </div>
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="text-xs font-bold text-emerald-900 uppercase tracking-wide">
                      Khuyến Nghị Cho Ban Giám Đốc
                    </span>
                    <span className="text-[11px] bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded-md font-semibold">
                      Tối ưu nhất
                    </span>
                  </div>
                  <p className="text-xs text-slate-700 mt-0.5">
                    Kịch bản{" "}
                    <strong className="text-emerald-950 font-bold">
                      {data.scenarios.find((s) => s.scenarioId === recommendedScenarioId)?.scenarioName}
                    </strong>{" "}
                    (
                    <span className="font-mono font-semibold">
                      {data.scenarios.find((s) => s.scenarioId === recommendedScenarioId)?.scenarioCode}
                    </span>
                    ) có chỉ số rủi ro quá tải nhân sự và thiếu hụt năng lực thấp nhất trong danh sách đối chiếu.
                  </p>
                </div>
              </div>
              <div className="text-[11px] text-slate-500 shrink-0">
                Đối chiếu lúc: {formatDateTime(data.comparedAt)}
              </div>
            </div>
          )}

          {/* Side-by-Side Comparison Cards Grid */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-bold text-slate-900 flex items-center space-x-2">
                <Briefcase className="h-4 w-4 text-indigo-600" />
                <span>Bảng Đối Chiếu Chỉ Số Tổng Quan</span>
              </h2>
              <span className="text-xs text-slate-500">
                {data.scenarios.length} kịch bản
              </span>
            </div>

            <div className="overflow-x-auto pb-2">
              <div
                className="grid gap-4 min-w-[760px]"
                style={{
                  gridTemplateColumns: `repeat(${data.scenarios.length}, minmax(280px, 1fr))`,
                }}
              >
                {data.scenarios.map((scn) => {
                  const isRec = scn.scenarioId === recommendedScenarioId;
                  const avgBadge = getUtilizationBadge(scn.averageUtilizationPercentage);
                  const peakBadge = getUtilizationBadge(scn.peakUtilizationPercentage);

                  return (
                    <div
                      key={scn.scenarioId}
                      className={cn(
                        "bg-white rounded-2xl border p-5 shadow-xs flex flex-col justify-between space-y-4 transition",
                        isRec
                          ? "border-emerald-400 ring-2 ring-emerald-500/20 shadow-md"
                          : "border-slate-200 hover:border-indigo-200"
                      )}
                    >
                      {/* Scenario Title Header */}
                      <div className="space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="font-mono text-xs font-bold text-indigo-600 bg-indigo-50 px-2.5 py-0.5 rounded-md border border-indigo-100">
                            {scn.scenarioCode}
                          </span>
                          {isRec ? (
                            <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                              <CheckCircle2 className="h-3 w-3" />
                              <span>Đề xuất</span>
                            </span>
                          ) : (
                            <span className="text-[11px] font-medium text-slate-400">
                              Phương án
                            </span>
                          )}
                        </div>

                        <h3 className="text-sm font-bold text-slate-900 line-clamp-2" title={scn.scenarioName}>
                          {scn.scenarioName}
                        </h3>

                        <div className="pt-2 space-y-1 text-xs text-slate-600 border-t border-slate-100">
                          <div className="flex items-center text-[11px]">
                            <Building2 className="h-3.5 w-3.5 mr-1.5 text-slate-400 shrink-0" />
                            <span className="truncate">{scn.orgUnitName}</span>
                          </div>
                          <div className="flex items-center text-[11px]">
                            <CalendarRange className="h-3.5 w-3.5 mr-1.5 text-slate-400 shrink-0" />
                            <span>
                              Tuần {scn.fromWeek} → Tuần {scn.fromWeek + scn.durationWeeks - 1}, {scn.fromYear} ({scn.durationWeeks} tuần)
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Primary Metrics Grid */}
                      <div className="space-y-2.5 pt-2 border-t border-slate-100">
                        {/* Nhân sự quá tải */}
                        <div className="flex items-center justify-between p-2 rounded-xl bg-slate-50 text-xs">
                          <span className="text-slate-600 flex items-center">
                            <Users className="h-3.5 w-3.5 mr-1 text-slate-400" />
                            Nhân sự quá tải:
                          </span>
                          <span
                            className={cn(
                              "font-bold px-2 py-0.5 rounded-md text-xs",
                              scn.overloadedEmployeesCount > 0
                                ? "bg-rose-100 text-rose-700 font-extrabold"
                                : "bg-emerald-100 text-emerald-700 font-bold"
                            )}
                          >
                            {scn.overloadedEmployeesCount} người
                          </span>
                        </div>

                        {/* Tổng giờ thiếu hụt */}
                        <div className="flex items-center justify-between p-2 rounded-xl bg-slate-50 text-xs">
                          <span className="text-slate-600 flex items-center">
                            <Flame className="h-3.5 w-3.5 mr-1 text-amber-500" />
                            Giờ thiếu hụt:
                          </span>
                          <span
                            className={cn(
                              "font-bold text-xs",
                              scn.totalShortfallHours > 0 ? "text-rose-600" : "text-emerald-600"
                            )}
                          >
                            {formatNumber(scn.totalShortfallHours)}h
                          </span>
                        </div>

                        {/* Giờ OT cần bổ sung */}
                        <div className="flex items-center justify-between p-2 rounded-xl bg-slate-50 text-xs">
                          <span className="text-slate-600 flex items-center">
                            <Clock className="h-3.5 w-3.5 mr-1 text-slate-400" />
                            Giờ thêm cần thiết:
                          </span>
                          <span className="font-semibold text-slate-900">
                            {formatNumber(scn.totalRequiredAdditionalHours)}h
                          </span>
                        </div>

                        {/* Tỷ lệ tải TB */}
                        <div className="flex items-center justify-between p-2 rounded-xl bg-slate-50 text-xs">
                          <span className="text-slate-600 flex items-center">
                            <TrendingUp className="h-3.5 w-3.5 mr-1 text-slate-400" />
                            Tải trung bình:
                          </span>
                          <span className={cn("font-bold px-2 py-0.5 rounded-md border text-xs", avgBadge.bg)}>
                            {formatPercent(scn.averageUtilizationPercentage)}
                          </span>
                        </div>

                        {/* Đỉnh tải */}
                        <div className="flex items-center justify-between p-2 rounded-xl bg-slate-50 text-xs">
                          <span className="text-slate-600 flex items-center">
                            <AlertTriangle className="h-3.5 w-3.5 mr-1 text-slate-400" />
                            Đỉnh tải cao nhất:
                          </span>
                          <span className={cn("font-bold px-2 py-0.5 rounded-md border text-xs", peakBadge.bg)}>
                            {formatPercent(scn.peakUtilizationPercentage)}
                          </span>
                        </div>

                        {/* Tổng khối lượng vs Khả dụng */}
                        <div className="p-2 rounded-xl bg-slate-50 text-xs space-y-1">
                          <div className="flex justify-between text-slate-500 text-[11px]">
                            <span>Khối lượng / Khả dụng</span>
                            <span className="font-semibold text-slate-700">
                              {formatNumber(scn.totalWorkloadHours)}h / {formatNumber(scn.totalAvailableHours)}h
                            </span>
                          </div>
                          {/* Mini progress bar */}
                          <div className="h-1.5 w-full bg-slate-200 rounded-full overflow-hidden">
                            <div
                              className={cn(
                                "h-full rounded-full transition-all duration-300",
                                scn.averageUtilizationPercentage > 110
                                  ? "bg-rose-500"
                                  : scn.averageUtilizationPercentage > 100
                                  ? "bg-amber-500"
                                  : "bg-emerald-500"
                              )}
                              style={{
                                width: `${Math.max(0, Math.min(Number(scn.averageUtilizationPercentage || 0), 100))}%`,
                              }}
                            />
                          </div>
                        </div>

                        {/* Giờ nhu cầu giả định */}
                        <div className="flex items-center justify-between text-slate-500 text-[11px] px-1">
                          <span>Giờ nhu cầu giả định:</span>
                          <span className="font-semibold text-indigo-600">
                            +{formatNumber(scn.totalDemandHours)}h
                          </span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Detailed Section: So Sánh Tải Từng Tuần (Weekly Timeline Comparison) */}
          <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-4">
            <button
              onClick={() =>
                setExpandedSection((prev) =>
                  prev === "weekly" ? "employees" : prev === "both" ? "employees" : "both"
                )
              }
              className="w-full flex items-center justify-between text-left group"
            >
              <div className="flex items-center space-x-2">
                <CalendarRange className="h-4 w-4 text-indigo-600" />
                <h3 className="text-sm font-bold text-slate-900 group-hover:text-indigo-600 transition">
                  So Sánh Tiến Độ & Tải Từng Tuần (Weekly Breakdown)
                </h3>
              </div>
              <div className="flex items-center space-x-1 text-xs text-slate-400">
                <span>{allWeekKeys.length} tuần</span>
                {expandedSection === "weekly" || expandedSection === "both" ? (
                  <ChevronDown className="h-4 w-4" />
                ) : (
                  <ChevronRight className="h-4 w-4" />
                )}
              </div>
            </button>

            {(expandedSection === "weekly" || expandedSection === "both") && (
              <div className="overflow-x-auto border border-slate-200 rounded-xl">
                <table className="w-full text-xs text-left">
                  <thead className="bg-slate-50 text-slate-700 font-semibold border-b border-slate-200">
                    <tr>
                      <th className="px-4 py-3 whitespace-nowrap sticky left-0 bg-slate-50 z-10">
                        Tuần / Năm
                      </th>
                      {data.scenarios.map((scn) => (
                        <th
                          key={scn.scenarioId}
                          colSpan={2}
                          className={cn(
                            "px-4 py-3 text-center border-l border-slate-200",
                            scn.scenarioId === recommendedScenarioId && "bg-emerald-50/70"
                          )}
                        >
                          <div className="font-bold text-slate-900">{scn.scenarioCode}</div>
                          <div className="text-[10px] text-slate-500 font-normal truncate max-w-[150px] mx-auto">
                            {scn.scenarioName}
                          </div>
                        </th>
                      ))}
                    </tr>
                    <tr className="border-t border-slate-200 text-[11px] text-slate-500 bg-slate-50/50">
                      <th className="px-4 py-2 sticky left-0 bg-slate-50/50 z-10"></th>
                      {data.scenarios.map((scn) => (
                        <React.Fragment key={`sub-${scn.scenarioId}`}>
                          <th className="px-2 py-2 text-right border-l border-slate-200">
                            Khối lượng
                          </th>
                          <th className="px-2 py-2 text-right">Tỷ lệ tải</th>
                        </React.Fragment>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {allWeekKeys.map((weekKey) => {
                      const [yearStr, weekStr] = weekKey.split("-W");
                      const yr = Number(yearStr);
                      const wk = Number(weekStr);

                      return (
                        <tr key={weekKey} className="hover:bg-slate-50/80">
                          <td className="px-4 py-2.5 font-medium text-slate-800 whitespace-nowrap sticky left-0 bg-white hover:bg-slate-50 z-10 border-r border-slate-100">
                            Tuần {wk}, {yr}
                          </td>
                          {data.scenarios.map((scn) => {
                            const wm = scn.weeklyMetrics.find(
                              (m) => m.year === yr && m.weekNumber === wk
                            );

                            if (!wm) {
                              return (
                                <React.Fragment key={`${scn.scenarioId}-${weekKey}`}>
                                  <td className="px-2 py-2.5 text-right text-slate-300 border-l border-slate-100">
                                    --
                                  </td>
                                  <td className="px-2 py-2.5 text-right text-slate-300">--</td>
                                </React.Fragment>
                              );
                            }

                            const util = wm.utilizationPercentage ?? 0;
                            const isOver = util > 100;

                            return (
                              <React.Fragment key={`${scn.scenarioId}-${weekKey}`}>
                                <td
                                  className={cn(
                                    "px-2 py-2.5 text-right font-medium text-slate-700 border-l border-slate-100",
                                    scn.scenarioId === recommendedScenarioId && "bg-emerald-50/20"
                                  )}
                                >
                                  {formatNumber(wm.scenarioWorkloadHours ?? wm.totalWorkloadHours)}h
                                </td>
                                <td
                                  className={cn(
                                    "px-2 py-2.5 text-right font-bold",
                                    scn.scenarioId === recommendedScenarioId && "bg-emerald-50/20",
                                    isOver
                                      ? util > 110
                                        ? "text-rose-600"
                                        : "text-amber-600"
                                      : "text-emerald-600"
                                  )}
                                >
                                  {formatPercent(util)}
                                </td>
                              </React.Fragment>
                            );
                          })}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Detailed Section: Danh Sách Nhân Sự Quá Tải Giữa Các Kịch Bản */}
          <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-4">
            <button
              onClick={() =>
                setExpandedSection((prev) =>
                  prev === "employees" ? "weekly" : prev === "both" ? "weekly" : "both"
                )
              }
              className="w-full flex items-center justify-between text-left group"
            >
              <div className="flex items-center space-x-2">
                <Users className="h-4 w-4 text-indigo-600" />
                <h3 className="text-sm font-bold text-slate-900 group-hover:text-indigo-600 transition">
                  Chi Tiết Nhân Sự Quá Tải Giữa Các Kịch Bản
                </h3>
              </div>
              <div className="flex items-center space-x-1 text-xs text-slate-400">
                {expandedSection === "employees" || expandedSection === "both" ? (
                  <ChevronDown className="h-4 w-4" />
                ) : (
                  <ChevronRight className="h-4 w-4" />
                )}
              </div>
            </button>

            {(expandedSection === "employees" || expandedSection === "both") && (
              <div
                className="grid gap-4"
                style={{
                  gridTemplateColumns: `repeat(auto-fit, minmax(320px, 1fr))`,
                }}
              >
                {data.scenarios.map((scn) => (
                  <div
                    key={`emp-panel-${scn.scenarioId}`}
                    className="border border-slate-200 rounded-xl p-4 bg-slate-50/50 space-y-3"
                  >
                    <div className="flex items-center justify-between border-b border-slate-200 pb-2">
                      <div className="flex items-center space-x-2">
                        <span className="font-mono text-xs font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded border border-indigo-100">
                          {scn.scenarioCode}
                        </span>
                        <span className="text-xs font-bold text-slate-800 truncate max-w-[180px]">
                          {scn.scenarioName}
                        </span>
                      </div>
                      <span
                        className={cn(
                          "text-[11px] font-bold px-2 py-0.5 rounded-full",
                          scn.overloadedEmployeesCount > 0
                            ? "bg-rose-100 text-rose-700"
                            : "bg-emerald-100 text-emerald-700"
                        )}
                      >
                        {scn.overloadedEmployeesCount} nhân sự vượt tải
                      </span>
                    </div>

                    {scn.overloadedEmployees.length === 0 ? (
                      <div className="py-6 text-center text-xs text-slate-400 flex flex-col items-center space-y-1">
                        <CheckCircle2 className="h-5 w-5 text-emerald-500" />
                        <span>Không có nhân sự nào bị vượt tải trong kịch bản này</span>
                      </div>
                    ) : (
                      <div className="space-y-2 max-h-[280px] overflow-y-auto pr-1">
                        {scn.overloadedEmployees.map((emp) => (
                          <div
                            key={`${scn.scenarioId}-emp-${emp.employeeId}`}
                            className="bg-white p-3 rounded-lg border border-slate-200 shadow-2xs space-y-1.5"
                          >
                            <div className="flex items-center justify-between">
                              <span className="text-xs font-bold text-slate-900 truncate">
                                {emp.fullName}
                              </span>
                              <span className="font-mono text-[11px] text-slate-500">
                                {emp.employeeCode}
                              </span>
                            </div>
                            <div className="flex items-center justify-between text-[11px] text-slate-600">
                              <span className="truncate max-w-[140px] text-slate-500">
                                {emp.professionalRole || "Chuyên viên"}
                              </span>
                              <div className="flex items-center space-x-2 font-medium">
                                <span className="text-amber-600">
                                  {emp.overloadedWeeksCount}w quá tải
                                </span>
                                <span className="text-rose-600 font-bold">
                                  +{formatNumber(emp.maxExcessHours)}h max
                                </span>
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};
