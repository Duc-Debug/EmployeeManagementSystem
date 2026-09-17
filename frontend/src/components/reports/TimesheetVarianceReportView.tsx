import { useState, useEffect, useCallback } from "react";
import {
  AlertTriangle,
  CheckCircle2,
  RefreshCw,
  Building2,
  Calendar,
  Clock,
  ShieldAlert,
  Download,
  FolderKanban
} from "lucide-react";
import { cn } from "@/lib/utils";
import {
  getTimesheetVarianceReport,
  type TimesheetVarianceItem,
  type TimesheetVarianceResult,
} from "@/lib/api/timesheet-variance";
import { getOrgTree } from "@/lib/api/org-units";
import { useAuthUser } from "@/lib/auth-session";
import { getIsoWeeksInYear, getIsoWeekDetails, addIsoWeeks } from "@/lib/iso-week";
import type { OrgUnitTreeNode } from "@/types/hrm";

export default function TimesheetVarianceReportView() {
  const user = useAuthUser();

  // Tính tuần hiện tại và tuần bắt đầu theo chuẩn ISO-8601 week-based year
  const currentIsoDetails = getIsoWeekDetails(new Date());
  const initialFrom = addIsoWeeks(currentIsoDetails.year, currentIsoDetails.week, -4);

  const [fromYear, setFromYear] = useState<number>(initialFrom.year);
  const [fromWeek, setFromWeek] = useState<number>(initialFrom.week);
  const [toYear, setToYear] = useState<number>(currentIsoDetails.year);
  const [toWeek, setToWeek] = useState<number>(currentIsoDetails.week);
  const [selectedOrgUnitId, setSelectedOrgUnitId] = useState<string>("");

  const fromMaxWeeks = getIsoWeeksInYear(fromYear);
  const toMaxWeeks = getIsoWeeksInYear(toYear);

  const handleFromYearChange = (newYear: number) => {
    setFromYear(newYear);
    const maxWeeks = getIsoWeeksInYear(newYear);
    if (fromWeek > maxWeeks) {
      setFromWeek(maxWeeks);
    }
  };

  const handleToYearChange = (newYear: number) => {
    setToYear(newYear);
    const maxWeeks = getIsoWeeksInYear(newYear);
    if (toWeek > maxWeeks) {
      setToWeek(maxWeeks);
    }
  };

  const [reportData, setReportData] = useState<TimesheetVarianceResult | null>(null);
  const [orgUnits, setOrgUnits] = useState<OrgUnitTreeNode[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [forbidden, setForbidden] = useState<boolean>(false);

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
        const allNodes = [...(tree || [])];
        if (user?.dataScope === "ORGANIZATION_BRANCH" && user?.scopeOrgUnitId != null) {
          const findScopeRoot = (nodes: readonly OrgUnitTreeNode[]): OrgUnitTreeNode | null => {
            for (const node of nodes) {
              if (Number(node.id) === Number(user.scopeOrgUnitId)) return node;
              const match = findScopeRoot(node.children || []);
              if (match) return match;
            }
            return null;
          };
          const scopeRoot = findScopeRoot(allNodes);
          setOrgUnits(scopeRoot ? flatten([scopeRoot]) : []);
        } else {
          setOrgUnits(flatten(allNodes));
        }
      } catch (err) {
        console.warn("Không thể tải danh sách phòng ban:", err);
      }
    }
    loadOrgUnitsData();
  }, [user?.dataScope, user?.scopeOrgUnitId]);

  const fetchReport = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    setForbidden(false);

    try {
      const data = await getTimesheetVarianceReport({
        fromYear,
        fromWeek,
        toYear,
        toWeek,
        orgUnitId: selectedOrgUnitId ? Number(selectedOrgUnitId) : undefined,
      });
      setReportData(data);
    } catch (err: unknown) {
      const e = err as { status?: number; code?: string; message?: string };
      if (e?.status === 403 || e?.code === "FORBIDDEN" || e?.message?.includes("403")) {
        setForbidden(true);
      } else {
        setError(e?.message || "Không thể tải báo cáo đối chiếu giờ phân bổ với thực tế.");
      }
    } finally {
      setIsLoading(false);
    }
  }, [fromYear, fromWeek, toYear, toWeek, selectedOrgUnitId]);

  useEffect(() => {
    fetchReport();
  }, [fetchReport]);

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

  const handleExportCsv = () => {
    if (!reportData || !reportData.items || reportData.items.length === 0) return;

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

    const rows = reportData.items.map((item: TimesheetVarianceItem) => [
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
      item.variancePercentage !== null && item.variancePercentage !== undefined
        ? `${item.variancePercentage}%`
        : "N/A",
      `"${getVarianceStatusLabel(item.varianceStatus)}"`,
    ]);

    const csvContent =
      "data:text/csv;charset=utf-8,\uFEFF" +
      [headers.join(","), ...rows.map((r: (string | number)[]) => r.join(","))].join("\n");

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

  const renderStatusBadge = (item: TimesheetVarianceItem) => {
    if (!item.hasActualData && item.allocatedHours > 0) {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
          Chưa có giờ duyệt
        </span>
      );
    }
    if (item.varianceHours > 0) {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200">
          +{item.varianceHours}h (Vượt KH)
        </span>
      );
    }
    if (item.varianceHours < 0) {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
          {item.varianceHours}h (Dưới KH)
        </span>
      );
    }
    return (
      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
        Đúng kế hoạch (0h)
      </span>
    );
  };

  if (forbidden) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] p-8 bg-white rounded-2xl border border-red-100 shadow-sm text-center">
        <div className="w-16 h-16 bg-red-50 text-red-600 rounded-full flex items-center justify-center mb-4 ring-8 ring-red-50/50">
          <ShieldAlert className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-bold text-gray-900 mb-2">Truy cập bị từ chối (403)</h2>
        <p className="text-gray-600 max-w-md mb-6 text-sm leading-relaxed">
          Bạn không có quyền xem báo cáo đối chiếu giờ phân bổ với thực tế (yêu cầu quyền <code className="bg-gray-100 px-1.5 py-0.5 rounded text-red-600 font-mono text-xs">TIMESHEET_VARIANCE_READ</code>).
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2.5 py-0.5 text-xs font-bold bg-indigo-50 text-indigo-700 rounded-md uppercase tracking-wider border border-indigo-100">
              NCL-09-CN-004
            </span>
            <span className="text-xs text-gray-500">• Giờ làm thực tế và đối chiếu kế hoạch</span>
          </div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight flex items-center gap-2.5">
            <Clock className="w-7 h-7 text-indigo-600" />
            Đối chiếu giờ phân bổ với thực tế
          </h1>
          <p className="text-gray-500 text-sm mt-1">
            So sánh số giờ đã phân bổ theo kế hoạch với số giờ công thực tế đã được phê duyệt theo từng nhân sự và tuần.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => fetchReport()}
            disabled={isLoading}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 bg-gray-50 hover:bg-gray-100 rounded-xl border border-gray-200 transition-all shadow-sm active:scale-95 disabled:opacity-50"
          >
            <RefreshCw className={cn("w-4 h-4", isLoading && "animate-spin text-indigo-600")} />
            Làm mới
          </button>
          <button
            onClick={handleExportCsv}
            disabled={!reportData || !reportData.items || reportData.items.length === 0}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl transition-all shadow-sm shadow-indigo-200 active:scale-95 disabled:opacity-50"
          >
            <Download className="w-4 h-4" />
            Xuất file CSV
          </button>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 items-end">
        <div>
          <label className="block text-xs font-bold text-gray-700 mb-1.5 flex items-center gap-1.5">
            <Calendar className="w-3.5 h-3.5 text-indigo-500" />
            Từ tuần
          </label>
          <div className="flex gap-2">
            <select
              value={fromWeek}
              onChange={(e) => setFromWeek(Number(e.target.value))}
              className="w-full bg-gray-50 border border-gray-200 text-gray-900 text-sm rounded-xl p-2.5 focus:ring-2 focus:ring-indigo-500 outline-none transition"
            >
              {Array.from({ length: fromMaxWeeks }, (_, i) => i + 1).map((w) => (
                <option key={w} value={w}>Tuần {w}</option>
              ))}
            </select>
            <input
              type="number"
              value={fromYear}
              onChange={(e) => handleFromYearChange(Number(e.target.value))}
              className="w-24 bg-gray-50 border border-gray-200 text-gray-900 text-sm rounded-xl p-2.5 focus:ring-2 focus:ring-indigo-500 outline-none transition font-medium"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold text-gray-700 mb-1.5 flex items-center gap-1.5">
            <Calendar className="w-3.5 h-3.5 text-indigo-500" />
            Đến tuần
          </label>
          <div className="flex gap-2">
            <select
              value={toWeek}
              onChange={(e) => setToWeek(Number(e.target.value))}
              className="w-full bg-gray-50 border border-gray-200 text-gray-900 text-sm rounded-xl p-2.5 focus:ring-2 focus:ring-indigo-500 outline-none transition"
            >
              {Array.from({ length: toMaxWeeks }, (_, i) => i + 1).map((w) => (
                <option key={w} value={w}>Tuần {w}</option>
              ))}
            </select>
            <input
              type="number"
              value={toYear}
              onChange={(e) => handleToYearChange(Number(e.target.value))}
              className="w-24 bg-gray-50 border border-gray-200 text-gray-900 text-sm rounded-xl p-2.5 focus:ring-2 focus:ring-indigo-500 outline-none transition font-medium"
            />
          </div>
        </div>

        {orgUnits.length > 0 && (
          <div>
            <label className="block text-xs font-bold text-gray-700 mb-1.5 flex items-center gap-1.5">
              <Building2 className="w-3.5 h-3.5 text-indigo-500" />
              Đơn vị / Phòng ban
            </label>
            <select
              value={selectedOrgUnitId}
              onChange={(e) => setSelectedOrgUnitId(e.target.value)}
              className="w-full bg-gray-50 border border-gray-200 text-gray-900 text-sm rounded-xl p-2.5 focus:ring-2 focus:ring-indigo-500 outline-none transition"
            >
              <option value="">-- Tất cả trong phạm vi --</option>
              {orgUnits.map((u: OrgUnitTreeNode) => (
                <option key={u.id} value={u.id}>
                  {u.unitName}
                </option>
              ))}
            </select>
          </div>
        )}

        <div>
          <button
            onClick={() => fetchReport()}
            className="w-full py-2.5 px-4 bg-gray-900 hover:bg-gray-800 text-white text-sm font-semibold rounded-xl shadow-sm transition active:scale-95 cursor-pointer"
          >
            Áp dụng bộ lọc
          </button>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-2xl text-red-700 text-sm flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 flex-shrink-0 text-red-500" />
          <span>{error}</span>
        </div>
      )}

      {/* KPI Cards */}
      {reportData && reportData.summary && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Tổng giờ phân bổ (KH)</span>
            <div className="mt-2 text-2xl font-black text-gray-900">
              {reportData.summary.totalAllocatedHours.toLocaleString()}h
            </div>
            <div className="mt-1 text-xs text-gray-400">
              {reportData.summary.totalEmployees} nhân sự • {reportData.summary.totalWeeks} tuần
            </div>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Tổng giờ thực tế (Đã duyệt)</span>
            <div className="mt-2 text-2xl font-black text-emerald-600">
              {reportData.summary.totalActualApprovedHours.toLocaleString()}h
            </div>
            <div className="mt-1 text-xs text-gray-400">
              Chỉ tính giờ công Approved
            </div>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Tổng chênh lệch ròng (Δ)</span>
            <div
              className={cn(
                "mt-2 text-2xl font-black",
                reportData.summary.totalVarianceHours > 0
                  ? "text-rose-600"
                  : reportData.summary.totalVarianceHours < 0
                  ? "text-blue-600"
                  : "text-gray-900"
              )}
            >
              {reportData.summary.totalVarianceHours > 0 ? "+" : ""}
              {reportData.summary.totalVarianceHours.toLocaleString()}h
            </div>
            <div className="mt-1 text-xs text-gray-400">
              {reportData.summary.positiveVarianceCount} lượt vượt • {reportData.summary.negativeVarianceCount} lượt thấp hơn
            </div>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Tình trạng thực tế</span>
            <div className="mt-2 text-base font-bold text-gray-800">
              {reportData.hasAnyActualData ? (
                <span className="text-emerald-600 flex items-center gap-1.5">
                  <CheckCircle2 className="w-5 h-5" />
                  Đã có dữ liệu đối chiếu
                </span>
              ) : (
                <span className="text-amber-600 flex items-center gap-1.5">
                  <AlertTriangle className="w-5 h-5" />
                  Chưa đủ dữ liệu thực tế
                </span>
              )}
            </div>
            <div className="mt-1 text-xs text-gray-400">
              {reportData.summary.noActualDataCount} dòng chưa có giờ duyệt
            </div>
          </div>
        </div>
      )}

      {/* Main Content / Table */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="py-20 text-center text-gray-500">
            <RefreshCw className="w-8 h-8 animate-spin text-indigo-600 mx-auto mb-3" />
            <div className="text-sm font-medium">Đang tải và tổng hợp dữ liệu đối chiếu...</div>
          </div>
        ) : !reportData || reportData.items.length === 0 ? (
          <div className="py-16 text-center text-gray-400">
            <FolderKanban className="w-12 h-12 text-gray-300 mx-auto mb-2" />
            <div className="text-sm font-medium">Không tìm thấy bản ghi phân bổ hoặc giờ công nào.</div>
          </div>
        ) : !reportData.hasAnyActualData ? (
          <div className="p-12 text-center space-y-3">
            <div className="inline-flex p-3.5 rounded-full bg-amber-50 text-amber-600 ring-8 ring-amber-50/50">
              <AlertTriangle className="w-8 h-8" />
            </div>
            <h3 className="text-lg font-bold text-gray-900">
              Chưa đủ dữ liệu thực tế để đối chiếu
            </h3>
            <p className="text-sm text-gray-500 max-w-md mx-auto">
              Khoảng thời gian này đã có kế hoạch phân bổ nhưng chưa có bảng chấm công nào được Quản lý dự án phê duyệt (APPROVED).
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50/80 border-b border-gray-100 text-[11px] font-bold text-gray-500 uppercase tracking-wider">
                  <th className="py-3.5 px-4">Nhân sự</th>
                  <th className="py-3.5 px-4">Đơn vị</th>
                  <th className="py-3.5 px-4">Dự án</th>
                  <th className="py-3.5 px-4">Tuần</th>
                  <th className="py-3.5 px-4 text-right">Giờ KH</th>
                  <th className="py-3.5 px-4 text-right">Giờ TT đã duyệt</th>
                  <th className="py-3.5 px-4 text-right">Chênh lệch (Δ)</th>
                  <th className="py-3.5 px-4 text-right">% Sai lệch</th>
                  <th className="py-3.5 px-4 text-center">Đánh giá</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-sm">
                {reportData.items.map((item: TimesheetVarianceItem, idx: number) => (
                  <tr
                    key={`${item.employeeId}_${item.projectId}_${item.year}_${item.weekNumber}_${idx}`}
                    className="hover:bg-indigo-50/20 transition-colors"
                  >
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-gray-900">{item.fullName}</div>
                      <div className="text-xs text-gray-400 font-mono">{item.employeeCode}</div>
                    </td>
                    <td className="py-3.5 px-4 text-gray-600 text-xs">
                      {item.orgUnitName}
                    </td>
                    <td className="py-3.5 px-4 text-gray-800 font-medium">
                      {item.projectName}
                    </td>
                    <td className="py-3.5 px-4 text-gray-600">
                      <div className="font-medium text-xs text-gray-800">Tuần {item.weekNumber}/{item.year}</div>
                      <div className="text-[11px] text-gray-400">{item.weekStartDate} ~ {item.weekEndDate}</div>
                    </td>
                    <td className="py-3.5 px-4 text-right font-bold text-gray-900">
                      {item.allocatedHours}h
                    </td>
                    <td className="py-3.5 px-4 text-right font-bold text-emerald-600">
                      {item.actualApprovedHours}h
                    </td>
                    <td
                      className={cn(
                        "py-3.5 px-4 text-right font-black",
                        item.varianceHours > 0
                          ? "text-rose-600"
                          : item.varianceHours < 0
                          ? "text-blue-600"
                          : "text-gray-600"
                      )}
                    >
                      {item.varianceHours > 0 ? "+" : ""}
                      {item.varianceHours}h
                    </td>
                    <td className="py-3.5 px-4 text-right text-xs font-mono font-medium text-gray-600">
                      {item.variancePercentage !== null && item.variancePercentage !== undefined ? (
                        <>
                          {item.variancePercentage > 0 ? "+" : ""}
                          {item.variancePercentage}%
                        </>
                      ) : (
                        <span className="text-gray-400 italic">N/A</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-center">
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
