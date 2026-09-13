/**
 * NCL-06-CN-009: Modal xem lịch sử và chi tiết các bản chụp kế hoạch (Baselines/Snapshots) của kỳ
 * Nâng cấp: Phím tắt Escape, Backdrop click, Xuất file CSV (UTF-8 Excel), Phân trang và Sắp xếp cột.
 */
import { useState, useEffect, useMemo } from "react";
import {
  X,
  Camera,
  History,
  Clock,
  User,
  Layers,
  AlertCircle,
  Loader2,
  Calendar,
  Download,
  ChevronLeft,
  ChevronRight,
  ArrowUpDown,
} from "lucide-react";
import {
  getPeriodSnapshots,
  getSnapshotDetail,
  type AllocationPlanSnapshotResult,
} from "@/lib/api/allocation-periods";

interface AllocationPlanSnapshotModalProps {
  open: boolean;
  periodId: number | null;
  periodName?: string;
  onClose: () => void;
}

type SortField = "employeeFullName" | "employeeCode" | "projectName" | "weekNumber" | "allocatedHours";
type SortOrder = "asc" | "desc";

export function AllocationPlanSnapshotModal({
  open,
  periodId,
  periodName = "Kỳ kế hoạch",
  onClose,
}: AllocationPlanSnapshotModalProps) {
  const [snapshots, setSnapshots] = useState<AllocationPlanSnapshotResult[]>([]);
  const [selectedSnapshot, setSelectedSnapshot] = useState<AllocationPlanSnapshotResult | null>(null);
  const [isLoadingList, setIsLoadingList] = useState<boolean>(true);
  const [isLoadingDetail, setIsLoadingDetail] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [searchItem, setSearchItem] = useState<string>("");

  // Phân trang & Sắp xếp client-side
  const [currentPage, setCurrentPage] = useState<number>(1);
  const pageSize = 10;
  const [sortField, setSortField] = useState<SortField>("employeeFullName");
  const [sortOrder, setSortOrder] = useState<SortOrder>("asc");

  // Phím tắt Escape để đóng modal
  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [open, onClose]);

  useEffect(() => {
    if (!open || !periodId) return;

    let isMounted = true;
    async function loadSnapshots() {
      try {
        setIsLoadingList(true);
        setErrorMessage(null);
        const list = await getPeriodSnapshots(periodId!);
        if (!isMounted) return;
        setSnapshots(list);
        if (list.length > 0) {
          // Mặc định chọn snapshot mới nhất
          const latest = list[list.length - 1];
          handleSelectSnapshot(latest.id);
        } else {
          setSelectedSnapshot(null);
        }
      } catch (err: unknown) {
        if (!isMounted) return;
        setErrorMessage(
          err instanceof Error
            ? err.message
            : "Không thể tải danh sách bản chụp của kỳ này."
        );
      } finally {
        if (isMounted) setIsLoadingList(false);
      }
    }

    loadSnapshots();
    return () => {
      isMounted = false;
    };
  }, [open, periodId]);

  const handleSelectSnapshot = async (snapshotId: number) => {
    if (!periodId) return;
    try {
      setIsLoadingDetail(true);
      const detail = await getSnapshotDetail(periodId, snapshotId);
      setSelectedSnapshot(detail);
      setCurrentPage(1);
    } catch (err: unknown) {
      console.error("Lỗi khi tải chi tiết bản chụp:", err);
    } finally {
      setIsLoadingDetail(false);
    }
  };

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortOrder((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setSortField(field);
      setSortOrder("asc");
    }
    setCurrentPage(1);
  };

  // Lọc và sắp xếp dữ liệu
  const processedItems = useMemo(() => {
    const raw = selectedSnapshot?.items || [];
    let items = raw;

    if (searchItem.trim()) {
      const term = searchItem.toLowerCase();
      items = items.filter(
        (item) =>
          item.employeeFullName.toLowerCase().includes(term) ||
          item.employeeCode.toLowerCase().includes(term) ||
          item.projectName.toLowerCase().includes(term) ||
          item.projectCode.toLowerCase().includes(term)
      );
    }

    return [...items].sort((a, b) => {
      let valA = a[sortField];
      let valB = b[sortField];

      if (typeof valA === "string") {
        const compare = valA.localeCompare(valB as string, "vi-VN");
        return sortOrder === "asc" ? compare : -compare;
      }
      if (typeof valA === "number") {
        return sortOrder === "asc"
          ? (valA as number) - (valB as number)
          : (valB as number) - (valA as number);
      }
      return 0;
    });
  }, [selectedSnapshot?.items, searchItem, sortField, sortOrder]);

  // Phân trang
  const totalItems = processedItems.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
  const paginatedItems = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return processedItems.slice(start, start + pageSize);
  }, [processedItems, currentPage, pageSize]);

  // Xuất file CSV (UTF-8 BOM cho Excel tiếng Việt)
  const handleExportCSV = () => {
    if (!selectedSnapshot || !selectedSnapshot.items.length) return;

    const headers = [
      "Mã Nhân Viên",
      "Họ Và Tên",
      "Mã Dự Án",
      "Tên Dự Án",
      "Năm",
      "Tuần Phân Bổ",
      "Số Giờ Phân Bổ",
    ];

    const rows = selectedSnapshot.items.map((it) => [
      `"${it.employeeCode}"`,
      `"${it.employeeFullName}"`,
      `"${it.projectCode}"`,
      `"${it.projectName.replace(/"/g, '""')}"`,
      it.year,
      it.weekNumber,
      it.allocatedHours,
    ]);

    const csvContent =
      "\uFEFF" +
      [headers.join(","), ...rows.map((r) => r.join(","))].join("\r\n");

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    const safeName = periodName.replace(/[^a-zA-Z0-9_-]/g, "_");
    link.href = url;
    link.download = `Baseline_${safeName}_v${selectedSnapshot.snapshotVersion}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  if (!open || !periodId) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="w-full max-w-4xl max-h-[88vh] flex flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 p-4 shrink-0">
          <div className="flex items-center gap-2.5">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-100 text-indigo-700">
              <Camera className="h-5 w-5" />
            </span>
            <div>
              <h3 className="text-sm font-bold text-slate-800">
                Bản Chụp Kế Hoạch (Baselines) - {periodName}
              </h3>
              <p className="text-[11px] text-slate-500">
                Theo dõi dữ liệu phân bổ được chốt sổ tại thời điểm khóa kỳ (QTN-18)
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition"
            title="Đóng (Esc)"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Body Content */}
        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          {errorMessage && (
            <div className="flex items-start gap-2 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-800">
              <AlertCircle className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          {isLoadingList ? (
            <div className="flex flex-col items-center justify-center p-12 text-slate-400 space-y-2">
              <Loader2 className="h-7 w-7 animate-spin text-indigo-600" />
              <span className="text-xs">Đang tải lịch sử bản chụp...</span>
            </div>
          ) : snapshots.length === 0 ? (
            <div className="flex flex-col items-center justify-center p-12 text-center rounded-2xl border border-dashed border-slate-200 bg-slate-50">
              <Camera className="h-10 w-10 text-slate-300 mb-2" />
              <p className="text-xs font-semibold text-slate-600">Chưa có bản chụp nào</p>
              <p className="text-[11px] text-slate-400 mt-1 max-w-sm">
                Kỳ này chưa từng được khóa. Bản chụp kế hoạch sẽ tự động được hệ thống lưu lại mỗi lần khóa kỳ.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {/* Snapshot Version Selector & Export Button */}
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-3">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-xs font-bold text-slate-600 flex items-center gap-1">
                    <History className="h-3.5 w-3.5 text-slate-400" />
                    Phiên bản chốt:
                  </span>
                  {snapshots.map((s) => {
                    const isSelected = selectedSnapshot?.id === s.id;
                    return (
                      <button
                        key={s.id}
                        type="button"
                        onClick={() => handleSelectSnapshot(s.id)}
                        className={`rounded-xl px-3 py-1.5 text-xs font-semibold transition ${
                          isSelected
                            ? "bg-indigo-600 text-white shadow-xs"
                            : "bg-slate-100 text-slate-700 hover:bg-slate-200"
                        }`}
                      >
                        Bản chụp v{s.snapshotVersion}
                      </button>
                    );
                  })}
                </div>

                {selectedSnapshot && selectedSnapshot.items.length > 0 && (
                  <button
                    type="button"
                    onClick={handleExportCSV}
                    className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-2xs"
                    title="Xuất bảng chốt phân bổ ra file CSV (Excel tiếng Việt)"
                  >
                    <Download className="h-3.5 w-3.5 text-slate-500" />
                    <span>Xuất CSV (Baseline)</span>
                  </button>
                )}
              </div>

              {/* Selected Snapshot Summary Cards */}
              {selectedSnapshot && (
                <div className="space-y-3">
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                    <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-3">
                      <span className="text-[11px] text-slate-500 flex items-center gap-1">
                        <Layers className="h-3 w-3 text-slate-400" />
                        Tổng phân bổ
                      </span>
                      <span className="text-base font-bold text-slate-800 mt-1 block">
                        {selectedSnapshot.totalAllocations} lượt
                      </span>
                    </div>

                    <div className="rounded-xl border border-indigo-200 bg-indigo-50/50 p-3">
                      <span className="text-[11px] text-indigo-700 flex items-center gap-1">
                        <Clock className="h-3 w-3 text-indigo-500" />
                        Tổng giờ kế hoạch
                      </span>
                      <span className="text-base font-bold text-indigo-900 mt-1 block">
                        {selectedSnapshot.totalAllocatedHours} giờ
                      </span>
                    </div>

                    <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-3">
                      <span className="text-[11px] text-slate-500 flex items-center gap-1">
                        <User className="h-3 w-3 text-slate-400" />
                        Người thực hiện khóa
                      </span>
                      <span className="text-xs font-bold text-slate-800 mt-1 block truncate">
                        {selectedSnapshot.lockedByName || `ID: ${selectedSnapshot.lockedBy}`}
                      </span>
                    </div>

                    <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-3">
                      <span className="text-[11px] text-slate-500 flex items-center gap-1">
                        <Calendar className="h-3 w-3 text-slate-400" />
                        Thời điểm khóa
                      </span>
                      <span className="text-xs font-semibold text-slate-700 mt-1 block">
                        {new Date(selectedSnapshot.lockedAt).toLocaleString("vi-VN")}
                      </span>
                    </div>
                  </div>

                  {/* Filter & Detail Table */}
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-700">
                        Chi tiết các dòng phân bổ nguồn lực ({totalItems} dòng):
                      </span>
                      <input
                        type="text"
                        value={searchItem}
                        onChange={(e) => {
                          setSearchItem(e.target.value);
                          setCurrentPage(1);
                        }}
                        placeholder="Tìm nhân viên, dự án..."
                        className="rounded-lg border border-slate-300 px-2.5 py-1 text-xs text-slate-800 placeholder-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 w-48"
                      />
                    </div>

                    {isLoadingDetail ? (
                      <div className="flex items-center justify-center p-8 text-slate-400">
                        <Loader2 className="h-5 w-5 animate-spin mr-2 text-indigo-600" />
                        <span className="text-xs">Đang tải chi tiết phân bổ...</span>
                      </div>
                    ) : paginatedItems.length === 0 ? (
                      <div className="p-6 text-center text-xs text-slate-400 border border-slate-200 rounded-xl bg-slate-50">
                        Không tìm thấy dòng phân bổ nào trong bản chụp này.
                      </div>
                    ) : (
                      <>
                        <div className="overflow-x-auto rounded-xl border border-slate-200">
                          <table className="w-full text-left text-xs text-slate-600">
                            <thead className="bg-slate-100 text-slate-700 font-semibold border-b border-slate-200">
                              <tr>
                                <th
                                  className="p-2.5 cursor-pointer hover:bg-slate-200 transition"
                                  onClick={() => handleSort("employeeFullName")}
                                >
                                  <div className="flex items-center gap-1">
                                    <span>Nhân viên</span>
                                    <ArrowUpDown className="h-3 w-3 text-slate-400" />
                                  </div>
                                </th>
                                <th
                                  className="p-2.5 cursor-pointer hover:bg-slate-200 transition"
                                  onClick={() => handleSort("employeeCode")}
                                >
                                  <div className="flex items-center gap-1">
                                    <span>Mã NV</span>
                                    <ArrowUpDown className="h-3 w-3 text-slate-400" />
                                  </div>
                                </th>
                                <th
                                  className="p-2.5 cursor-pointer hover:bg-slate-200 transition"
                                  onClick={() => handleSort("projectName")}
                                >
                                  <div className="flex items-center gap-1">
                                    <span>Dự án</span>
                                    <ArrowUpDown className="h-3 w-3 text-slate-400" />
                                  </div>
                                </th>
                                <th
                                  className="p-2.5 text-center cursor-pointer hover:bg-slate-200 transition"
                                  onClick={() => handleSort("weekNumber")}
                                >
                                  <div className="flex items-center justify-center gap-1">
                                    <span>Tuần</span>
                                    <ArrowUpDown className="h-3 w-3 text-slate-400" />
                                  </div>
                                </th>
                                <th
                                  className="p-2.5 text-right cursor-pointer hover:bg-slate-200 transition"
                                  onClick={() => handleSort("allocatedHours")}
                                >
                                  <div className="flex items-center justify-end gap-1">
                                    <span>Số giờ</span>
                                    <ArrowUpDown className="h-3 w-3 text-slate-400" />
                                  </div>
                                </th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100 bg-white">
                              {paginatedItems.map((item) => (
                                <tr key={item.id} className="hover:bg-slate-50 transition">
                                  <td className="p-2.5 font-medium text-slate-900">
                                    {item.employeeFullName}
                                  </td>
                                  <td className="p-2.5 text-slate-500">
                                    {item.employeeCode}
                                  </td>
                                  <td className="p-2.5">
                                    <span className="font-semibold text-indigo-700">
                                      {item.projectCode}
                                    </span>{" "}
                                    - {item.projectName}
                                  </td>
                                  <td className="p-2.5 text-center font-semibold">
                                    T{item.weekNumber}/{item.year}
                                  </td>
                                  <td className="p-2.5 text-right font-bold text-slate-800">
                                    {item.allocatedHours}h
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>

                        {/* Phân trang */}
                        {totalPages > 1 && (
                          <div className="flex items-center justify-between pt-2 text-xs text-slate-500">
                            <span>
                              Hiển thị {(currentPage - 1) * pageSize + 1} -{" "}
                              {Math.min(currentPage * pageSize, totalItems)} trên tổng số {totalItems} dòng
                            </span>
                            <div className="flex items-center gap-1.5">
                              <button
                                type="button"
                                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                                disabled={currentPage === 1}
                                className="rounded-lg border border-slate-200 p-1.5 text-slate-600 hover:bg-slate-50 disabled:opacity-40 transition"
                                title="Trang trước"
                              >
                                <ChevronLeft className="h-4 w-4" />
                              </button>
                              <span className="px-2 font-semibold text-slate-700">
                                {currentPage} / {totalPages}
                              </span>
                              <button
                                type="button"
                                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                                disabled={currentPage === totalPages}
                                className="rounded-lg border border-slate-200 p-1.5 text-slate-600 hover:bg-slate-50 disabled:opacity-40 transition"
                                title="Trang sau"
                              >
                                <ChevronRight className="h-4 w-4" />
                              </button>
                            </div>
                          </div>
                        )}
                      </>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end border-t border-slate-200 bg-slate-50 px-5 py-3 shrink-0">
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-xs hover:bg-indigo-700 transition"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
}
