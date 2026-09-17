"use client";

import { useCallback, useEffect, useState } from "react";
import { PageHeader } from "@/components/layout/PageHeader";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { Icon } from "@/components/ui/Icon";
import {
  type TimesheetVarianceItem,
  type TimesheetVarianceResult,
  type TimesheetVarianceSummary,
  getTimesheetVarianceReport,
} from "@/lib/api/timesheetVarianceApi";
import { getOrgUnitsTree, type OrgUnit } from "@/lib/api/org-units";
import { ApiError } from "@/lib/api-client";

export default function TimesheetVarianceReportPage() {
  const currentYear = new Date().getFullYear();
  // Get ISO week number approximately
  const today = new Date();
  const d = new Date(Date.UTC(today.getFullYear(), today.getMonth(), today.getDate()));
  const dayNum = d.getUTCDay() || 7;
  d.setUTCDate(d.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  const currentWeek = Math.ceil(((d.getTime() - yearStart.getTime()) / 86400000 + 1) / 7);

  const [fromYear, setFromYear] = useState<number>(currentYear);
  const [fromWeek, setFromWeek] = useState<number>(Math.max(1, currentWeek - 4));
  const [toYear, setToYear] = useState<number>(currentYear);
  const [toWeek, setToWeek] = useState<number>(currentWeek);
  const [selectedOrgUnitId, setSelectedOrgUnitId] = useState<string>("");
  const [orgUnits, setOrgUnits] = useState<OrgUnit[]>([]);

  const [data, setData] = useState<TimesheetVarianceResult | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [errorMessage, setErrorMessage] = useState<string>("");

  // Load OrgUnits tree for filter
  useEffect(() => {
    getOrgUnitsTree()
      .then((tree) => {
        const flatten = (nodes: OrgUnit[]): OrgUnit[] => {
          let list: OrgUnit[] = [];
          for (const n of nodes) {
            list.push(n);
            if (n.children && n.children.length > 0) {
              list = list.concat(flatten(n.children));
            }
          }
          return list;
        };
        setOrgUnits(flatten(tree));
      })
      .catch(() => {
        // Ignore org units fetch error if not permitted
      });
  }, []);

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage("");
    try {
      const res = await getTimesheetVarianceReport({
        fromYear,
        fromWeek,
        toYear,
        toWeek,
        orgUnitId: selectedOrgUnitId ? Number(selectedOrgUnitId) : undefined,
      });
      setData(res);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage("Không thể tải báo cáo đối chiếu. Vui lòng thử lại.");
      }
    } finally {
      setIsLoading(false);
    }
  }, [fromYear, fromWeek, toYear, toWeek, selectedOrgUnitId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleExportCsv = () => {
    if (!data || !data.items || data.items.length === 0) return;

    const headers = [
      "Mã NV",
      "Họ và Tên",
      "Đơn vị",
      "Dự án",
      "Tuần",
      "Năm",
      "Từ ngày",
      "Đến ngày",
      "Giờ phân bổ (Kế hoạch)",
      "Giờ thực tế đã duyệt",
      "Chênh lệch (h)",
      "% Chênh lệch",
      "Trạng thái",
    ];

    const rows = data.items.map((item) => [
      `"${item.employeeCode}"`,
      `"${item.fullName}"`,
      `"${item.orgUnitName}"`,
      `"${item.projectName}"`,
      `"T${item.weekNumber}"`,
      item.year,
      item.weekStartDate,
      item.weekEndDate,
      item.allocatedHours,
      item.actualApprovedHours,
      item.varianceHours,
      `${item.variancePercentage}%`,
      `"${getVarianceStatusLabel(item.varianceStatus)}"`,
    ]);

    const csvContent =
      "data:text/csv;charset=utf-8,\uFEFF" +
      [headers.join(","), ...rows.map((e) => e.join(","))].join("\n");

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute(
      "download",
      `doi_chieu_gio_cong_T${fromWeek}_T${toWeek}_${fromYear}.csv`
    );
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const getVarianceStatusLabel = (status: string) => {
    switch (status) {
      case "POSITIVE_VARIANCE":
        return "Thực tế vượt KH (+)";
      case "NEGATIVE_VARIANCE":
        return "Thực tế dưới KH (-)";
      case "ON_TRACK":
        return "Đúng kế hoạch";
      case "NO_ACTUAL_DATA":
        return "Chưa có giờ duyệt";
      default:
        return status;
    }
  };

  const renderStatusBadge = (item: TimesheetVarianceItem) => {
    if (!item.hasActualData && item.allocatedHours > 0) {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400">
          Chưa có giờ duyệt
        </span>
      );
    }
    if (item.varianceHours > 0) {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-rose-100 text-rose-800 dark:bg-rose-900/30 dark:text-rose-400">
          +{item.varianceHours}h (Vượt KH)
        </span>
      );
    }
    if (item.varianceHours < 0) {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400">
          {item.varianceHours}h (Thấp hơn KH)
        </span>
      );
    }
    return (
      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400">
        Khớp kế hoạch (0h)
      </span>
    );
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Báo cáo đối chiếu giờ phân bổ với thực tế"
        description="So sánh số giờ đã phân bổ theo kế hoạch với số giờ công thực tế đã được phê duyệt (NCL-09-CN-004)"
      >
        <button
          onClick={handleExportCsv}
          disabled={!data || !data.items || data.items.length === 0}
          className="inline-flex items-center px-4 py-2 border border-slate-300 dark:border-slate-600 rounded-lg text-sm font-medium text-slate-700 dark:text-slate-200 bg-white dark:bg-slate-800 hover:bg-slate-50 dark:hover:bg-slate-700 disabled:opacity-50 shadow-sm"
        >
          <Icon name="arrow-down-tray" className="w-4 h-4 mr-2" />
          Xuất dữ liệu CSV
        </button>
      </PageHeader>

      {/* Filter Bar */}
      <div className="bg-white dark:bg-slate-800 rounded-xl p-4 shadow-sm border border-slate-200 dark:border-slate-700 flex flex-wrap gap-4 items-end">
        <div>
          <label className="block text-xs font-medium text-slate-500 dark:text-slate-400 mb-1">
            Từ tuần
          </label>
          <div className="flex gap-2">
            <select
              value={fromWeek}
              onChange={(e) => setFromWeek(Number(e.target.value))}
              className="bg-slate-50 dark:bg-slate-900 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-slate-100 text-sm rounded-lg p-2"
            >
              {Array.from({ length: 53 }, (_, i) => i + 1).map((w) => (
                <option key={w} value={w}>
                  Tuần {w}
                </option>
              ))}
            </select>
            <input
              type="number"
              value={fromYear}
              onChange={(e) => setFromYear(Number(e.target.value))}
              className="w-24 bg-slate-50 dark:bg-slate-900 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-slate-100 text-sm rounded-lg p-2"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-medium text-slate-500 dark:text-slate-400 mb-1">
            Đến tuần
          </label>
          <div className="flex gap-2">
            <select
              value={toWeek}
              onChange={(e) => setToWeek(Number(e.target.value))}
              className="bg-slate-50 dark:bg-slate-900 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-slate-100 text-sm rounded-lg p-2"
            >
              {Array.from({ length: 53 }, (_, i) => i + 1).map((w) => (
                <option key={w} value={w}>
                  Tuần {w}
                </option>
              ))}
            </select>
            <input
              type="number"
              value={toYear}
              onChange={(e) => setToYear(Number(e.target.value))}
              className="w-24 bg-slate-50 dark:bg-slate-900 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-slate-100 text-sm rounded-lg p-2"
            />
          </div>
        </div>

        {orgUnits.length > 0 && (
          <div className="min-w-[200px]">
            <label className="block text-xs font-medium text-slate-500 dark:text-slate-400 mb-1">
              Đơn vị / Bộ phận
            </label>
            <select
              value={selectedOrgUnitId}
              onChange={(e) => setSelectedOrgUnitId(e.target.value)}
              className="w-full bg-slate-50 dark:bg-slate-900 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-slate-100 text-sm rounded-lg p-2"
            >
              <option value="">-- Tất cả trong phạm vi --</option>
              {orgUnits.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name}
                </option>
              ))}
            </select>
          </div>
        )}

        <button
          onClick={fetchData}
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg shadow transition"
        >
          Lọc báo cáo
        </button>
      </div>

      {errorMessage && (
        <div className="p-4 bg-rose-50 dark:bg-rose-900/20 border border-rose-200 dark:border-rose-800 rounded-xl text-rose-700 dark:text-rose-300 text-sm flex items-center gap-2">
          <Icon name="exclamation-triangle" className="w-5 h-5 flex-shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Summary KPI Cards */}
      {data && data.summary && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm">
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
              Tổng giờ phân bổ (KH)
            </span>
            <div className="mt-2 text-2xl font-bold text-slate-900 dark:text-slate-100">
              {data.summary.totalAllocatedHours.toLocaleString()}h
            </div>
            <div className="mt-1 text-xs text-slate-400">
              {data.summary.totalEmployees} nhân sự • {data.summary.totalWeeks} tuần
            </div>
          </div>

          <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm">
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
              Tổng giờ thực tế (Đã duyệt)
            </span>
            <div className="mt-2 text-2xl font-bold text-emerald-600 dark:text-emerald-400">
              {data.summary.totalActualApprovedHours.toLocaleString()}h
            </div>
            <div className="mt-1 text-xs text-slate-400">
              Chỉ tính giờ công Approved
            </div>
          </div>

          <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm">
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
              Tổng chênh lệch ròng (Δ)
            </span>
            <div
              className={`mt-2 text-2xl font-bold ${
                data.summary.totalVarianceHours > 0
                  ? "text-rose-600 dark:text-rose-400"
                  : data.summary.totalVarianceHours < 0
                  ? "text-blue-600 dark:text-blue-400"
                  : "text-slate-900 dark:text-slate-100"
              }`}
            >
              {data.summary.totalVarianceHours > 0 ? "+" : ""}
              {data.summary.totalVarianceHours.toLocaleString()}h
            </div>
            <div className="mt-1 text-xs text-slate-400">
              {data.summary.positiveVarianceCount} lượt vượt • {data.summary.negativeVarianceCount} lượt thấp hơn
            </div>
          </div>

          <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm">
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
              Trạng thái dữ liệu
            </span>
            <div className="mt-2 text-lg font-semibold text-slate-800 dark:text-slate-200">
              {data.hasAnyActualData ? (
                <span className="text-emerald-600 dark:text-emerald-400 flex items-center gap-1.5">
                  <Icon name="check-circle" className="w-5 h-5" />
                  Đã có dữ liệu đối chiếu
                </span>
              ) : (
                <span className="text-amber-600 dark:text-amber-400 flex items-center gap-1.5">
                  <Icon name="exclamation-circle" className="w-5 h-5" />
                  Chưa đủ dữ liệu thực tế
                </span>
              )}
            </div>
            <div className="mt-1 text-xs text-slate-400">
              {data.summary.noActualDataCount} dòng chưa có giờ duyệt
            </div>
          </div>
        </div>
      )}

      {/* Main Table / Empty State */}
      <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 overflow-hidden shadow-sm">
        {isLoading ? (
          <div className="py-16 text-center text-slate-500">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-indigo-500 border-t-transparent mb-2"></div>
            <div>Đang tải và tổng hợp dữ liệu đối chiếu...</div>
          </div>
        ) : !data || data.items.length === 0 ? (
          <EmptyState
            title="Không có dữ liệu phân bổ hoặc giờ công"
            description="Không tìm thấy bản ghi phân bổ hoặc giờ công thực tế nào trong khoảng thời gian đã chọn."
            icon="document-chart-bar"
          />
        ) : !data.hasAnyActualData ? (
          <div className="p-8 text-center space-y-3">
            <div className="inline-flex p-3 rounded-full bg-amber-100 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400">
              <Icon name="exclamation-triangle" className="w-8 h-8" />
            </div>
            <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
              Chưa đủ dữ liệu thực tế để đối chiếu
            </h3>
            <p className="text-sm text-slate-500 dark:text-slate-400 max-w-md mx-auto">
              Khoảng thời gian này đã có kế hoạch phân bổ nhưng chưa có bảng chấm công nào được Quản lý dự án phê duyệt (APPROVED).
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 dark:divide-slate-700">
              <thead className="bg-slate-50 dark:bg-slate-900/50 text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider text-left">
                <tr>
                  <th className="px-4 py-3">Nhân sự</th>
                  <th className="px-4 py-3">Đơn vị</th>
                  <th className="px-4 py-3">Dự án</th>
                  <th className="px-4 py-3">Tuần</th>
                  <th className="px-4 py-3 text-right">Giờ KH</th>
                  <th className="px-4 py-3 text-right">Giờ TT đã duyệt</th>
                  <th className="px-4 py-3 text-right">Chênh lệch (Δ)</th>
                  <th className="px-4 py-3 text-right">% Sai lệch</th>
                  <th className="px-4 py-3 text-center">Đánh giá</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-700 text-sm">
                {data.items.map((item, idx) => (
                  <tr
                    key={`${item.employeeId}_${item.projectId}_${item.year}_${item.weekNumber}_${idx}`}
                    className="hover:bg-slate-50 dark:hover:bg-slate-700/30 transition"
                  >
                    <td className="px-4 py-3 font-medium text-slate-900 dark:text-slate-100">
                      <div>{item.fullName}</div>
                      <div className="text-xs text-slate-400 font-normal">
                        {item.employeeCode}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-slate-600 dark:text-slate-300">
                      {item.orgUnitName}
                    </td>
                    <td className="px-4 py-3 text-slate-600 dark:text-slate-300 font-medium">
                      {item.projectName}
                    </td>
                    <td className="px-4 py-3 text-slate-600 dark:text-slate-300">
                      <div>Tuần {item.weekNumber}/{item.year}</div>
                      <div className="text-xs text-slate-400">
                        {item.weekStartDate} ~ {item.weekEndDate}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-right font-semibold text-slate-800 dark:text-slate-200">
                      {item.allocatedHours}h
                    </td>
                    <td className="px-4 py-3 text-right font-semibold text-emerald-600 dark:text-emerald-400">
                      {item.actualApprovedHours}h
                    </td>
                    <td
                      className={`px-4 py-3 text-right font-bold ${
                        item.varianceHours > 0
                          ? "text-rose-600 dark:text-rose-400"
                          : item.varianceHours < 0
                          ? "text-blue-600 dark:text-blue-400"
                          : "text-slate-600 dark:text-slate-400"
                      }`}
                    >
                      {item.varianceHours > 0 ? "+" : ""}
                      {item.varianceHours}h
                    </td>
                    <td className="px-4 py-3 text-right text-slate-600 dark:text-slate-400 font-mono text-xs">
                      {item.variancePercentage > 0 ? "+" : ""}
                      {item.variancePercentage}%
                    </td>
                    <td className="px-4 py-3 text-center">
                      {renderStatusBadge(item)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
