import { useState, useEffect, useMemo, useCallback } from "react";
import {
  ChevronLeft,
  ChevronRight,
  RotateCcw,
  Search,
  Building2,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Users,
  Loader2,
  CalendarDays,
  ShieldAlert,
  SlidersHorizontal,
  TrendingUp,
} from "lucide-react";
import { useAuthUser } from "@/lib/auth-session";
import {
  getCompanyWeeklyCapacityMatrix,
  type CompanyWeeklyCapacityMatrixData,
  type EmployeeCapacityRow,
  type CapacityMatrixCell,
} from "@/lib/api/allocations";
import { getOrgTree } from "@/lib/api/org-units";
import type { OrgUnitTreeNode } from "@/types/hrm";
import { getCurrentIsoWeek } from "@/components/availability/availability.types";

export default function CompanyWeeklyCapacityView() {
  const currentUser = useAuthUser();
  const roleCode = currentUser?.roleCode?.toUpperCase().replace(/_/g, "-") || "";
  const isDirector = roleCode === "VT-01";

  // Current ISO week state
  const currentIso = useMemo(() => getCurrentIsoWeek(), []);
  const [selectedYear, setSelectedYear] = useState<number>(currentIso.year);
  const [selectedWeek, setSelectedWeek] = useState<number>(currentIso.weekNumber);
  const [durationWeeks] = useState<number>(8); // Mặc định 8 tuần theo TC-01

  // Filter states
  const [selectedOrgUnitId, setSelectedOrgUnitId] = useState<number | undefined>(
    currentUser?.scopeOrgUnitId ?? undefined
  );
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | "OVERLOADED" | "OPTIMAL" | "UNDERUTILIZED">("ALL");

  // Data states
  const [matrixData, setMatrixData] = useState<CompanyWeeklyCapacityMatrixData | null>(null);
  const [orgUnits, setOrgUnits] = useState<{ id: number; name: string }[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // 1. Tải danh mục phòng ban
  useEffect(() => {
    let isMounted = true;
    async function loadOrgUnits() {
      try {
        const tree = await getOrgTree();
        if (!isMounted) return;
        const flatList: { id: number; name: string }[] = [];
        const flatten = (nodes: readonly OrgUnitTreeNode[]) => {
          for (const node of nodes) {
            flatList.push({ id: node.id, name: node.unitName });
            if (node.children && node.children.length > 0) {
              flatten(node.children);
            }
          }
        };
        flatten(tree);
        setOrgUnits(flatList);
      } catch (err) {
        console.warn("Không thể tải danh sách đơn vị:", err);
      }
    }
    loadOrgUnits();
    return () => {
      isMounted = false;
    };
  }, []);

  // 2. Tải dữ liệu ma trận năng lực theo tuần
  const fetchMatrix = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const data = await getCompanyWeeklyCapacityMatrix({
        orgUnitId: selectedOrgUnitId,
        fromYear: selectedYear,
        fromWeek: selectedWeek,
        durationWeeks,
      });
      setMatrixData(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Đã xảy ra lỗi khi tải bảng năng lực";
      setErrorMessage(msg);
      setMatrixData(null);
    } finally {
      setIsLoading(false);
    }
  }, [selectedOrgUnitId, selectedYear, selectedWeek, durationWeeks]);

  useEffect(() => {
    fetchMatrix();
  }, [fetchMatrix]);

  // Điều hướng tuần
  const handleNavigateWeek = (delta: number) => {
    let nextWeek = selectedWeek + delta;
    let nextYear = selectedYear;
    if (nextWeek < 1) {
      nextYear -= 1;
      nextWeek = 52;
    } else if (nextWeek > 52) {
      nextYear += 1;
      nextWeek = 1;
    }
    setSelectedYear(nextYear);
    setSelectedWeek(nextWeek);
  };

  const handleResetCurrentWeek = () => {
    const iso = getCurrentIsoWeek();
    setSelectedYear(iso.year);
    setSelectedWeek(iso.weekNumber);
  };

  const rows = matrixData?.rows;

  // Lọc các hàng nhân sự theo ô tìm kiếm và trạng thái
  const filteredRows = useMemo(() => {
    if (!rows) return [];
    return rows.filter((row: EmployeeCapacityRow) => {
      const q = searchTerm.trim().toLowerCase();
      const matchSearch =
        !q ||
        row.fullName.toLowerCase().includes(q) ||
        row.employeeCode.toLowerCase().includes(q) ||
        (row.professionalRole && row.professionalRole.toLowerCase().includes(q));

      if (!matchSearch) return false;

      if (statusFilter === "ALL") return true;
      if (statusFilter === "OVERLOADED") return row.overloadedWeeksCount > 0;
      if (statusFilter === "UNDERUTILIZED") {
        return row.cells.some((c) => c.status === "UNDERUTILIZED");
      }
      if (statusFilter === "OPTIMAL") {
        return row.cells.some((c) => c.status === "OPTIMAL");
      }
      return true;
    });
  }, [rows, searchTerm, statusFilter]);

  // Phân trang Client-side mượt mà tránh DOM Lag khi danh sách lớn
  const [currentPage, setCurrentPage] = useState<number>(1);
  const pageSize = 20;

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, statusFilter, selectedOrgUnitId, selectedYear, selectedWeek]);

  const totalPages = Math.ceil(filteredRows.length / pageSize) || 1;
  const paginatedRows = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredRows.slice(start, start + pageSize);
  }, [filteredRows, currentPage, pageSize]);

  // Render 1 ô dữ liệu trong ma trận
  const renderCell = (cell: CapacityMatrixCell) => {
    const isZeroAvailability = cell.availableHours === 0;

    if (isZeroAvailability && cell.allocatedHours === 0) {
      return (
        <div
          className="flex flex-col items-center justify-center p-2 rounded-xl bg-slate-50 border border-slate-200 text-slate-400 text-xs min-h-[58px]"
          title="Nhân viên không có giờ khả dụng trong tuần (Nghỉ phép cả tuần)"
        >
          <span className="font-semibold text-slate-500">Nghỉ phép</span>
          <span className="text-[10px] text-slate-400">0h / 0h</span>
        </div>
      );
    }

    if (cell.isOverloaded || cell.status === "OVERLOADED") {
      return (
        <div
          className="flex flex-col items-center justify-center p-2 rounded-xl bg-rose-50 border border-rose-300 text-rose-800 text-xs min-h-[58px] shadow-xs hover:ring-2 hover:ring-rose-400 transition"
          title={`Quá tải: Tổng phân bổ ${cell.allocatedHours}h vượt quá ${cell.availableHours}h khả dụng!`}
        >
          <div className="flex items-center gap-1 font-bold text-rose-700">
            <AlertTriangle className="h-3.5 w-3.5 text-rose-600 animate-pulse" />
            <span>{cell.utilizationPercentage != null ? `${cell.utilizationPercentage}%` : "Vô cực"}</span>
          </div>
          <span className="text-[11px] font-medium text-rose-600">
            {cell.allocatedHours}h / {cell.availableHours}h
          </span>
          <span className="inline-block mt-0.5 px-1.5 py-0.2 rounded-full bg-rose-200/80 text-[10px] font-bold text-rose-900">
            + {cell.excessHours}h
          </span>
        </div>
      );
    }

    if (cell.status === "UNDERUTILIZED") {
      return (
        <div
          className="flex flex-col items-center justify-center p-2 rounded-xl bg-amber-50/70 border border-amber-200 text-amber-800 text-xs min-h-[58px] hover:ring-2 hover:ring-amber-300 transition"
          title={`Nhàn rỗi: Phân bổ ${cell.allocatedHours}h trên ${cell.availableHours}h khả dụng (${cell.utilizationPercentage}%)`}
        >
          <span className="font-bold text-amber-700">
            {cell.utilizationPercentage != null ? `${cell.utilizationPercentage}%` : "0%"}
          </span>
          <span className="text-[11px] text-amber-600">
            {cell.allocatedHours}h / {cell.availableHours}h
          </span>
          <span className="text-[10px] font-semibold text-amber-600/80">Nhàn rỗi</span>
        </div>
      );
    }

    // Trạng thái tối ưu (50% - 100%)
    return (
      <div
        className="flex flex-col items-center justify-center p-2 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs min-h-[58px] hover:ring-2 hover:ring-emerald-300 transition"
        title={`Tối ưu: Phân bổ ${cell.allocatedHours}h trên ${cell.availableHours}h khả dụng (${cell.utilizationPercentage}%)`}
      >
        <div className="flex items-center gap-1 font-bold text-emerald-700">
          <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
          <span>{cell.utilizationPercentage != null ? `${cell.utilizationPercentage}%` : "100%"}</span>
        </div>
        <span className="text-[11px] text-emerald-600">
          {cell.allocatedHours}h / {cell.availableHours}h
        </span>
      </div>
    );
  };

  return (
    <div className="space-y-6">
      {/* 1. Header & Navigation Controls */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100 shadow-2xs">
              <CalendarDays className="h-5 w-5" />
            </div>
            <h1 className="text-xl font-bold tracking-tight text-slate-900">
              Bảng Năng Lực Theo Tuần
            </h1>
          </div>
          <p className="mt-1 text-xs text-slate-500">
            Theo dõi tổng quan công suất phân bổ theo tuần của nhân sự thay vì phải mở từng dự án
            thủ công (NCL-06-CN-002)
          </p>
        </div>

        {/* Bộ điều hướng tuần */}
        <div className="flex flex-wrap items-center gap-2">
          <div className="inline-flex items-center gap-1 rounded-2xl border border-slate-200 bg-white p-1 shadow-2xs">
            <button
              type="button"
              onClick={() => handleNavigateWeek(-1)}
              className="rounded-xl p-1.5 text-slate-600 hover:bg-slate-100 transition"
              title="Tuần trước"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <span className="px-3 text-xs font-bold text-slate-800">
              Tuần {selectedWeek} / {selectedYear}
            </span>
            <button
              type="button"
              onClick={() => handleNavigateWeek(1)}
              className="rounded-xl p-1.5 text-slate-600 hover:bg-slate-100 transition"
              title="Tuần tiếp theo"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>

          <button
            type="button"
            onClick={handleResetCurrentWeek}
            className="inline-flex items-center gap-1.5 rounded-2xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-2xs"
          >
            <RotateCcw className="h-3.5 w-3.5 text-slate-400" />
            <span>Tuần hiện tại</span>
          </button>
        </div>
      </div>

      {/* 2. Thẻ KPI Thống Kê Tổng Quan */}
      {matrixData?.summary && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500">Tổng nhân sự</span>
              <span className="rounded-xl bg-indigo-50 p-2 text-indigo-600">
                <Users className="h-4 w-4" />
              </span>
            </div>
            <div className="mt-2 text-2xl font-bold text-slate-900">
              {matrixData.summary.totalEmployees}
            </div>
            <p className="text-[11px] text-slate-400 mt-0.5">
              Đơn vị: {matrixData.orgUnitName || "Toàn công ty"}
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500">Nhân sự quá tải</span>
              <span className="rounded-xl bg-rose-50 p-2 text-rose-600">
                <AlertTriangle className="h-4 w-4" />
              </span>
            </div>
            <div className="mt-2 text-2xl font-bold text-rose-600">
              {matrixData.summary.overloadedEmployeesCount}
            </div>
            <p className="text-[11px] text-slate-400 mt-0.5">
              {matrixData.summary.overloadedCellsCount} ô tuần vượt &gt; 100% (QTN-12)
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500">Ô tuần nhàn rỗi</span>
              <span className="rounded-xl bg-amber-50 p-2 text-amber-600">
                <Clock className="h-4 w-4" />
              </span>
            </div>
            <div className="mt-2 text-2xl font-bold text-amber-600">
              {matrixData.summary.underutilizedCellsCount}
            </div>
            <p className="text-[11px] text-slate-400 mt-0.5">
              Số ô có mức phân bổ &lt; 50%
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500">Công suất trung bình</span>
              <span className="rounded-xl bg-emerald-50 p-2 text-emerald-600">
                <TrendingUp className="h-4 w-4" />
              </span>
            </div>
            <div className="mt-2 text-2xl font-bold text-slate-900">
              {matrixData.summary.averageUtilization}%
            </div>
            <p className="text-[11px] text-slate-400 mt-0.5">
              Khung thời gian {matrixData.durationWeeks} tuần
            </p>
          </div>
        </div>
      )}

      {/* 3. Bộ lọc tìm kiếm & Scope phòng ban */}
      <div className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-xs sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 flex-wrap items-center gap-3">
          {/* Lọc phòng ban (TC-03) */}
          <div className="flex items-center gap-2">
            <Building2 className="h-4 w-4 text-slate-400" />
            <select
              value={selectedOrgUnitId ?? ""}
              onChange={(e) => {
                const val = e.target.value ? Number(e.target.value) : undefined;
                setSelectedOrgUnitId(val);
              }}
              className="rounded-xl border border-slate-200 bg-slate-50/50 px-3 py-1.5 text-xs font-semibold text-slate-700 outline-none focus:border-indigo-500 focus:bg-white transition"
            >
              {isDirector && <option value="">Tất cả phòng ban (Toàn công ty)</option>}
              {orgUnits.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name}
                </option>
              ))}
            </select>
          </div>

          {/* Ô tìm kiếm */}
          <div className="relative flex-1 min-w-[200px] max-w-sm">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm theo tên hoặc mã nhân sự..."
              className="w-full rounded-xl border border-slate-200 bg-slate-50/50 pl-8 pr-3 py-1.5 text-xs text-slate-700 placeholder:text-slate-400 outline-none focus:border-indigo-500 focus:bg-white transition"
            />
          </div>
        </div>

        {/* Lọc trạng thái */}
        <div className="flex items-center gap-2">
          <SlidersHorizontal className="h-3.5 w-3.5 text-slate-400" />
          <select
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(
                e.target.value as "ALL" | "OVERLOADED" | "OPTIMAL" | "UNDERUTILIZED"
              )
            }
            className="rounded-xl border border-slate-200 bg-slate-50/50 px-3 py-1.5 text-xs font-semibold text-slate-700 outline-none focus:border-indigo-500 focus:bg-white transition"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="OVERLOADED">Chỉ người quá tải (⚠ &gt; 100%)</option>
            <option value="OPTIMAL">Tối ưu (50% - 100%)</option>
            <option value="UNDERUTILIZED">Nhàn rỗi (&lt; 50%)</option>
          </select>
        </div>
      </div>

      {/* 4. Ma trận Năng lực (Grid) */}
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xs">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center p-12 text-slate-400">
            <Loader2 className="h-8 w-8 animate-spin text-indigo-600 mb-2" />
            <span className="text-xs font-semibold">Đang tổng hợp dữ liệu năng lực theo tuần...</span>
          </div>
        ) : errorMessage ? (
          <div className="flex flex-col items-center justify-center p-8 text-center text-rose-600">
            <ShieldAlert className="h-10 w-10 text-rose-500 mb-2" />
            <span className="text-sm font-bold">Không thể tải dữ liệu bảng năng lực</span>
            <p className="text-xs text-slate-500 max-w-md mt-1">{errorMessage}</p>
            <button
              onClick={fetchMatrix}
              className="mt-4 rounded-xl bg-indigo-600 px-4 py-1.5 text-xs font-semibold text-white hover:bg-indigo-700 transition"
            >
              Thử lại
            </button>
          </div>
        ) : !matrixData || filteredRows.length === 0 ? (
          <div className="flex flex-col items-center justify-center p-12 text-center text-slate-400">
            <Users className="h-10 w-10 text-slate-300 mb-2" />
            <span className="text-sm font-bold text-slate-700">
              Không tìm thấy dữ liệu nhân sự phù hợp
            </span>
            <p className="text-xs text-slate-500 mt-1">
              Thử thay đổi điều kiện tìm kiếm hoặc chọn phòng ban khác.
            </p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-xs">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/80">
                  <th className="sticky left-0 z-20 min-w-[200px] border-r border-slate-200 bg-slate-50/95 px-4 py-3 font-bold text-slate-700 backdrop-blur-xs">
                    Nhân sự
                  </th>
                  {matrixData.weeks.map((w) => (
                    <th
                      key={`${w.year}-${w.weekNumber}`}
                      className="min-w-[110px] px-3 py-3 font-bold text-slate-700 text-center border-r border-slate-200 last:border-r-0"
                    >
                      <div className="text-xs">{w.label}</div>
                      <div className="text-[10px] font-normal text-slate-400 mt-0.5">
                        Năm {w.year}
                      </div>
                    </th>
                  ))}
                  <th className="min-w-[130px] px-4 py-3 font-bold text-slate-700 text-center bg-slate-50/95">
                    Tổng kết
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white">
                {paginatedRows.map((row: EmployeeCapacityRow) => (
                  <tr key={row.employeeId} className="hover:bg-slate-50/50 transition">
                    {/* Cột Nhân sự cố định bên trái */}
                    <td className="sticky left-0 z-10 border-r border-slate-200 bg-white/95 px-4 py-3 backdrop-blur-xs">
                      <div className="font-bold text-slate-900">{row.fullName}</div>
                      <div className="flex items-center gap-1.5 text-[11px] text-slate-400 mt-0.5">
                        <span className="font-mono text-slate-500">{row.employeeCode}</span>
                        <span>•</span>
                        <span>{row.professionalRole}</span>
                      </div>
                      <div className="text-[10px] text-indigo-600 mt-0.5 font-medium">
                        {row.orgUnitName}
                      </div>
                    </td>

                    {/* Các cột tuần */}
                    {row.cells.map((cell: CapacityMatrixCell) => (
                      <td
                        key={`${cell.year}-${cell.weekNumber}`}
                        className="p-2 border-r border-slate-200 align-middle text-center last:border-r-0"
                      >
                        {renderCell(cell)}
                      </td>
                    ))}

                    {/* Cột Tổng kết của nhân sự */}
                    <td className="px-4 py-3 text-center align-middle bg-slate-50/30">
                      <div className="font-bold text-slate-900">
                        {row.averageUtilization != null ? (
                          `${row.averageUtilization}%`
                        ) : row.totalAllocatedHours > 0 ? (
                          <span className="text-rose-600 font-bold">Quá tải (∞)</span>
                        ) : (
                          "0%"
                        )}
                      </div>
                      <div className="text-[11px] text-slate-500 mt-0.5">
                        {row.totalAllocatedHours}h / {row.totalAvailableHours}h
                      </div>
                      {row.overloadedWeeksCount > 0 ? (
                        <span className="inline-block mt-1 px-1.5 py-0.5 rounded-full bg-rose-100 text-[10px] font-bold text-rose-700">
                          {row.overloadedWeeksCount} tuần quá tải
                        </span>
                      ) : (
                        <span className="inline-block mt-1 px-1.5 py-0.5 rounded-full bg-emerald-100 text-[10px] font-semibold text-emerald-700">
                          Cân bằng
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Thanh phân trang Client-side */}
          {filteredRows.length > pageSize && (
            <div className="flex flex-col sm:flex-row items-center justify-between gap-3 border-t border-slate-200 px-4 py-3 bg-white">
              <div className="text-xs text-slate-500">
                Hiển thị <span className="font-semibold text-slate-700">{(currentPage - 1) * pageSize + 1}</span> -{" "}
                <span className="font-semibold text-slate-700">
                  {Math.min(currentPage * pageSize, filteredRows.length)}
                </span>{" "}
                trong tổng số <span className="font-semibold text-slate-700">{filteredRows.length}</span> nhân sự
              </div>
              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  disabled={currentPage <= 1}
                  onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                  className="rounded-lg border border-slate-200 px-2.5 py-1 text-xs font-medium text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:pointer-events-none transition shadow-xs"
                >
                  Trang trước
                </button>
                <span className="px-2 text-xs font-medium text-slate-600">
                  Trang {currentPage} / {totalPages}
                </span>
                <button
                  type="button"
                  disabled={currentPage >= totalPages}
                  onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                  className="rounded-lg border border-slate-200 px-2.5 py-1 text-xs font-medium text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:pointer-events-none transition shadow-xs"
                >
                  Trang sau
                </button>
              </div>
            </div>
          )}
        </>
        )}
      </div>

      {/* 5. Chú thích màu sắc và quy tắc (QTN-12) */}
      <div className="flex flex-wrap items-center gap-4 rounded-xl border border-slate-200 bg-slate-50/60 p-3 text-xs text-slate-600">
        <span className="font-bold text-slate-700">Chú giải trạng thái:</span>
        <div className="flex items-center gap-1.5">
          <span className="h-3 w-3 rounded-md bg-rose-500" />
          <span>Quá tải (&gt; 100% giờ khả dụng - QTN-12)</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="h-3 w-3 rounded-md bg-emerald-500" />
          <span>Tối ưu (50% - 100%)</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="h-3 w-3 rounded-md bg-amber-400" />
          <span>Nhàn rỗi (&lt; 50%)</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="h-3 w-3 rounded-md bg-slate-300" />
          <span>Nghỉ phép / Chưa phân bổ</span>
        </div>
      </div>
    </div>
  );
}