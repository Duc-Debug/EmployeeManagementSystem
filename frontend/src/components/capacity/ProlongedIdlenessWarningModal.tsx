"use client";

import { useState, useEffect, useCallback } from "react";
import {
  X,
  AlertTriangle,
  Users,
  Clock,
  Search,
  RotateCcw,
  Sliders,
  ChevronLeft,
  ChevronRight,
  Loader2,
  CheckCircle2,
  TrendingDown,
} from "lucide-react";
import {
  getProlongedIdleStaff,
  type ProlongedIdlenessReportResult,
  type ProlongedIdleStaffItem,
} from "@/lib/api/prolonged-idleness";
import { getOrgTree } from "@/lib/api/org-units";
import type { OrgUnitTreeNode } from "@/types/hrm";
import { useAuthUser } from "@/lib/auth-session";
import { AcknowledgeIdlenessDialog } from "./AcknowledgeIdlenessDialog";

interface ProlongedIdlenessWarningModalProps {
  open: boolean;
  onClose: () => void;
  initialYear?: number;
  initialWeek?: number;
  initialOrgUnitId?: number;
}

export function ProlongedIdlenessWarningModal({
  open,
  onClose,
  initialYear,
  initialWeek,
  initialOrgUnitId,
}: ProlongedIdlenessWarningModalProps) {
  const currentUser = useAuthUser();
  const normalizedRole = currentUser?.roleCode
    ? currentUser.roleCode.toUpperCase().replace(/_/g, "-").replace(/^ROLE-/, "")
    : "";
  const canAcknowledge = normalizedRole === "VT-03" || normalizedRole === "VT-06";

  // Filter states
  const now = new Date();
  const currentYear = initialYear ?? now.getFullYear();
  const [fromYear, setFromYear] = useState<number>(currentYear);
  const [fromWeek, setFromWeek] = useState<number>(initialWeek ?? 1);
  const [durationWeeks, setDurationWeeks] = useState<number>(4);
  const [consecutiveThreshold, setConsecutiveThreshold] = useState<number>(3);
  const [selectedOrgUnitId, setSelectedOrgUnitId] = useState<number | undefined>(initialOrgUnitId);
  const [search, setSearch] = useState<string>("");
  const [page, setPage] = useState<number>(0);
  const [pageSize] = useState<number>(10);

  // Data states
  const [report, setReport] = useState<ProlongedIdlenessReportResult | null>(null);
  const [orgUnits, setOrgUnits] = useState<{ id: number; name: string }[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successToast, setSuccessToast] = useState<string | null>(null);

  // Dialog state
  const [selectedStaffToAcknowledge, setSelectedStaffToAcknowledge] =
    useState<ProlongedIdleStaffItem | null>(null);

  // Load org units tree
  useEffect(() => {
    if (!open) return;
    let isMounted = true;
    async function loadUnits() {
      try {
        const tree = await getOrgTree();
        if (!isMounted) return;
        const flat: { id: number; name: string }[] = [];
        function traverse(nodes: readonly OrgUnitTreeNode[]) {
          for (const node of nodes) {
            flat.push({ id: node.id, name: node.unitName });
            if (node.children && node.children.length > 0) {
              traverse(node.children);
            }
          }
        }
        traverse(tree);
        setOrgUnits(flat);
      } catch (err) {
        console.warn("Không thể tải cây phòng ban:", err);
      }
    }
    loadUnits();
    return () => {
      isMounted = false;
    };
  }, [open]);

  // Load report data
  const fetchData = useCallback(async () => {
    if (!open) return;
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const data = await getProlongedIdleStaff({
        orgUnitId: selectedOrgUnitId,
        fromYear,
        fromWeek,
        durationWeeks,
        consecutiveThreshold,
        search: search.trim() || undefined,
        page,
        size: pageSize,
      });
      setReport(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Không thể tải danh sách cảnh báo nhàn rỗi.";
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  }, [open, selectedOrgUnitId, fromYear, fromWeek, durationWeeks, consecutiveThreshold, search, page, pageSize]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  if (!open) return null;

  const handleAcknowledgeSuccess = () => {
    setSuccessToast("Đã ghi nhận phương án xử lý cảnh báo thành công (TC-04).");
    fetchData();
    setTimeout(() => {
      setSuccessToast(null);
    }, 4000);
  };

  const totalEmptyHoursInReport =
    report?.items.reduce((sum, item) => sum + (item.totalEmptyHours || 0), 0) || 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="relative flex flex-col w-full max-w-5xl max-h-[92vh] overflow-hidden rounded-3xl bg-white shadow-2xl border border-slate-200">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 bg-gradient-to-r from-amber-500/10 via-orange-500/5 to-white">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-amber-500 text-white shadow-xs">
              <AlertTriangle className="h-6 w-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-bold text-slate-900">
                  Cảnh Báo Nhân Sự Nhàn Rỗi Kéo Dài
                </h2>
                <span className="rounded-full bg-amber-100 px-2.5 py-0.5 text-[11px] font-bold text-amber-800">
                  QTN-23
                </span>
              </div>
              <p className="text-xs text-slate-500">
                Rà soát nhân sự có mức sử dụng dưới ngưỡng Ban Giám Đốc quy định trong nhiều tuần liên tiếp
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Success Toast */}
        {successToast && (
          <div className="mx-6 mt-4 flex items-center gap-2.5 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-xs font-semibold text-emerald-800 animate-in fade-in">
            <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
            <span>{successToast}</span>
          </div>
        )}

        {/* KPI Cards & Controls */}
        <div className="p-6 space-y-4 overflow-y-auto">
          {/* Quick Metrics */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3.5">
            <div className="rounded-2xl border border-amber-200 bg-amber-50/50 p-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-amber-900">Cảnh báo nhàn rỗi</span>
                <Users className="h-4 w-4 text-amber-600" />
              </div>
              <div className="mt-2 flex items-baseline gap-2">
                <span className="text-2xl font-black text-amber-900">
                  {report?.totalIdleEmployees ?? 0}
                </span>
                <span className="text-xs text-amber-700">nhân sự</span>
              </div>
              <p className="mt-1 text-[11px] text-amber-700/80">
                Chuỗi vi phạm &ge; {consecutiveThreshold} tuần liên tiếp
              </p>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-700">Ngưỡng nhàn rỗi (QTN-23)</span>
                <Sliders className="h-4 w-4 text-slate-500" />
              </div>
              <div className="mt-2 flex items-baseline gap-2">
                <span className="text-2xl font-black text-slate-900">
                  {report?.effectiveIdleThreshold ?? 30.0}%
                </span>
                <span className="text-xs text-slate-500">công suất</span>
              </div>
              <p className="mt-1 text-[11px] text-slate-500">
                Do Ban Giám Đốc ban hành cho {report?.orgUnitName || "công ty"}
              </p>
            </div>

            <div className="rounded-2xl border border-rose-200 bg-rose-50/50 p-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-rose-900">Tổng giờ trống phát hiện</span>
                <Clock className="h-4 w-4 text-rose-600" />
              </div>
              <div className="mt-2 flex items-baseline gap-2">
                <span className="text-2xl font-black text-rose-900">
                  {totalEmptyHoursInReport.toFixed(1)}
                </span>
                <span className="text-xs text-rose-700">giờ</span>
              </div>
              <p className="mt-1 text-[11px] text-rose-700/80">
                Tổng năng lực chưa khai thác trong kỳ rà soát
              </p>
            </div>
          </div>

          {/* Filter Bar */}
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs space-y-3">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
              {/* Tuần bắt đầu */}
              <div>
                <label className="text-[11px] font-bold text-slate-700 block mb-1">
                  Tuần bắt đầu
                </label>
                <input
                  type="number"
                  min={1}
                  max={53}
                  value={fromWeek}
                  onChange={(e) => setFromWeek(Number(e.target.value))}
                  className="w-full rounded-xl border border-slate-200 px-3 py-1.5 text-xs text-slate-800 focus:outline-none focus:border-amber-500"
                />
              </div>

              {/* Năm */}
              <div>
                <label className="text-[11px] font-bold text-slate-700 block mb-1">
                  Năm
                </label>
                <input
                  type="number"
                  min={2020}
                  max={2030}
                  value={fromYear}
                  onChange={(e) => setFromYear(Number(e.target.value))}
                  className="w-full rounded-xl border border-slate-200 px-3 py-1.5 text-xs text-slate-800 focus:outline-none focus:border-amber-500"
                />
              </div>

              {/* Dải tuần rà soát */}
              <div>
                <label className="text-[11px] font-bold text-slate-700 block mb-1">
                  Số tuần rà soát
                </label>
                <select
                  value={durationWeeks}
                  onChange={(e) => setDurationWeeks(Number(e.target.value))}
                  className="w-full rounded-xl border border-slate-200 px-3 py-1.5 text-xs text-slate-800 focus:outline-none focus:border-amber-500"
                >
                  <option value={4}>4 tuần (1 tháng)</option>
                  <option value={8}>8 tuần (2 tháng)</option>
                  <option value={12}>12 tuần (1 quý)</option>
                </select>
              </div>

              {/* Ngưỡng tuần nhàn rỗi */}
              <div>
                <label className="text-[11px] font-bold text-slate-700 block mb-1">
                  Số tuần liên tiếp &ge;
                </label>
                <select
                  value={consecutiveThreshold}
                  onChange={(e) => setConsecutiveThreshold(Number(e.target.value))}
                  className="w-full rounded-xl border border-slate-200 px-3 py-1.5 text-xs text-slate-800 focus:outline-none focus:border-amber-500"
                >
                  <option value={2}>2 tuần liên tiếp</option>
                  <option value={3}>3 tuần liên tiếp (Chuẩn)</option>
                  <option value={4}>4 tuần liên tiếp</option>
                  <option value={5}>5 tuần liên tiếp</option>
                </select>
              </div>

              {/* Phòng ban */}
              <div>
                <label className="text-[11px] font-bold text-slate-700 block mb-1">
                  Phòng ban
                </label>
                <select
                  value={selectedOrgUnitId ?? ""}
                  onChange={(e) => setSelectedOrgUnitId(e.target.value ? Number(e.target.value) : undefined)}
                  className="w-full rounded-xl border border-slate-200 px-3 py-1.5 text-xs text-slate-800 focus:outline-none focus:border-amber-500"
                >
                  <option value="">Toàn công ty</option>
                  {orgUnits.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Ô tìm kiếm & Refresh */}
            <div className="flex items-center gap-2 pt-1 border-t border-slate-100">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
                <input
                  type="text"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Tìm kiếm theo họ tên, mã nhân viên, vị trí chuyên môn..."
                  className="w-full rounded-xl border border-slate-200 pl-9 pr-3 py-1.5 text-xs text-slate-800 focus:outline-none focus:border-amber-500"
                />
              </div>
              <button
                type="button"
                onClick={() => {
                  setPage(0);
                  fetchData();
                }}
                disabled={isLoading}
                className="inline-flex items-center gap-1.5 rounded-xl bg-slate-800 px-3.5 py-1.5 text-xs font-semibold text-white hover:bg-slate-900 transition shadow-2xs"
              >
                <RotateCcw className={`h-3.5 w-3.5 ${isLoading ? "animate-spin" : ""}`} />
                <span>Rà soát</span>
              </button>
            </div>
          </div>

          {/* Table Data */}
          {errorMessage ? (
            <div className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center text-xs text-rose-700">
              <AlertTriangle className="h-8 w-8 text-rose-500 mx-auto mb-2" />
              <p className="font-semibold">{errorMessage}</p>
            </div>
          ) : isLoading ? (
            <div className="flex flex-col items-center justify-center p-12 text-slate-400">
              <Loader2 className="h-8 w-8 animate-spin text-amber-500 mb-2" />
              <span className="text-xs">Đang phân tích năng lực và chuỗi tuần nhàn rỗi...</span>
            </div>
          ) : !report || report.items.length === 0 ? (
            <div className="flex flex-col items-center justify-center rounded-2xl border border-slate-200 bg-slate-50/50 p-10 text-center">
              <CheckCircle2 className="h-10 w-10 text-emerald-500 mb-2" />
              <h4 className="text-sm font-bold text-slate-800">Không phát hiện nhân sự nhàn rỗi kéo dài</h4>
              <p className="text-xs text-slate-500 mt-1 max-w-sm">
                Tất cả nhân sự trong phạm vi đã chọn đều có mức phân bổ trên ngưỡng quy định hoặc được miễn trừ nghỉ phép dài ngày (TC-02).
              </p>
            </div>
          ) : (
            <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xs">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-600">
                  <thead className="bg-slate-50 text-[11px] font-bold text-slate-700 uppercase tracking-wider border-b border-slate-200">
                    <tr>
                      <th className="px-4 py-3">Nhân sự</th>
                      <th className="px-3 py-3">Phòng ban & Vị trí</th>
                      <th className="px-3 py-3 text-center">Chuỗi nhàn rỗi</th>
                      <th className="px-3 py-3 text-center">Sử dụng TB</th>
                      <th className="px-3 py-3 text-right">Giờ trống</th>
                      <th className="px-4 py-3 text-center">Dải tuần chi tiết</th>
                      {canAcknowledge && <th className="px-4 py-3 text-center">Hành động</th>}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {report.items.map((item) => {
                      const utilPercent = item.averageUtilization || 0;

                      return (
                        <tr key={item.employeeId} className="hover:bg-slate-50/80 transition">
                          {/* Cột 1: Thông tin nhân sự */}
                          <td className="px-4 py-3.5">
                            <div className="flex items-center gap-2.5">
                              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-amber-400 to-orange-500 text-white font-bold text-xs">
                                {item.fullName.charAt(0)}
                              </div>
                              <div>
                                <div className="font-bold text-slate-900">{item.fullName}</div>
                                <div className="text-[11px] text-slate-400">{item.employeeCode}</div>
                              </div>
                            </div>
                          </td>

                          {/* Cột 2: Phòng ban & Chức danh */}
                          <td className="px-3 py-3.5">
                            <div className="font-medium text-slate-800">{item.departmentName}</div>
                            <div className="text-[11px] text-slate-500">{item.positionTitle}</div>
                          </td>

                          {/* Cột 3: Chuỗi nhàn rỗi */}
                          <td className="px-3 py-3.5 text-center">
                            <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2.5 py-1 text-xs font-extrabold text-rose-800">
                              <TrendingDown className="h-3 w-3" />
                              <span>{item.consecutiveIdleWeeks} tuần</span>
                            </span>
                          </td>

                          {/* Cột 4: Tỷ lệ sử dụng trung bình */}
                          <td className="px-3 py-3.5 text-center">
                            <div className="inline-flex flex-col items-center">
                              <span
                                className={`text-xs font-bold ${
                                  utilPercent < 20 ? "text-rose-600" : "text-amber-600"
                                }`}
                              >
                                {utilPercent}%
                              </span>
                              <div className="w-16 h-1.5 bg-slate-100 rounded-full mt-1 overflow-hidden">
                                <div
                                  className={`h-full ${
                                    utilPercent < 20 ? "bg-rose-500" : "bg-amber-500"
                                  }`}
                                  style={{ width: `${Math.min(utilPercent, 100)}%` }}
                                />
                              </div>
                            </div>
                          </td>

                          {/* Cột 5: Giờ trống */}
                          <td className="px-3 py-3.5 text-right font-bold text-slate-900">
                            {item.totalEmptyHours.toFixed(1)}h
                          </td>

                          {/* Cột 6: Dải tuần chi tiết */}
                          <td className="px-4 py-3.5">
                            <div className="flex items-center justify-center gap-1">
                              {item.weeklyDetails.map((w) => {
                                let badgeColor = "bg-emerald-100 text-emerald-800 border-emerald-300";

                                if (w.isFullLeaveWeek) {
                                  badgeColor = "bg-slate-100 text-slate-600 border-slate-300";
                                } else if (w.isUnderutilized) {
                                  badgeColor = "bg-rose-100 text-rose-800 border-rose-300 font-bold";
                                }

                                return (
                                  <div
                                    key={`${w.year}-W${w.weekNumber}`}
                                    className={`relative group px-1.5 py-0.5 rounded text-[10px] border ${badgeColor} cursor-default`}
                                    title={`Tuần ${w.weekNumber}/${w.year}: Phân bổ ${w.allocatedHours}h / ${w.availableHours}h khả dụng (Trống ${w.emptyHours}h)`}
                                  >
                                    <span>T{w.weekNumber}</span>
                                  </div>
                                );
                              })}
                            </div>
                          </td>

                          {/* Cột 7: Nút Xử lý cảnh báo (TC-04) */}
                          {canAcknowledge && (
                            <td className="px-4 py-3.5 text-center">
                              <button
                                type="button"
                                onClick={() => setSelectedStaffToAcknowledge(item)}
                                className="inline-flex items-center gap-1 rounded-xl bg-amber-500/10 border border-amber-300/80 px-2.5 py-1 text-xs font-semibold text-amber-900 hover:bg-amber-500 hover:text-white transition shadow-2xs"
                              >
                                <span>Xử lý</span>
                              </button>
                            </td>
                          )}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {/* Phân trang */}
              <div className="flex items-center justify-between border-t border-slate-100 px-4 py-3 bg-slate-50/50">
                <div className="text-xs text-slate-500">
                  Hiển thị <span className="font-semibold text-slate-800">{report.items.length}</span> /{" "}
                  <span className="font-semibold text-slate-800">{report.totalIdleEmployees}</span> nhân sự nhàn rỗi
                </div>

                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                    disabled={report.page <= 0}
                    className="rounded-lg p-1.5 text-slate-600 hover:bg-slate-200 disabled:opacity-40 disabled:cursor-not-allowed transition"
                    title="Trang trước"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </button>
                  <span className="px-2 text-xs font-semibold text-slate-700">
                    Trang {report.page + 1} / {Math.max(1, report.totalPages)}
                  </span>
                  <button
                    type="button"
                    onClick={() => setPage((prev) => prev + 1)}
                    disabled={report.page + 1 >= report.totalPages}
                    className="rounded-lg p-1.5 text-slate-600 hover:bg-slate-200 disabled:opacity-40 disabled:cursor-not-allowed transition"
                    title="Trang sau"
                  >
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Dialog Xử lý Cảnh Báo */}
        <AcknowledgeIdlenessDialog
          open={selectedStaffToAcknowledge !== null}
          onClose={() => setSelectedStaffToAcknowledge(null)}
          staff={selectedStaffToAcknowledge}
          onSuccess={handleAcknowledgeSuccess}
        />
      </div>
    </div>
  );
}
