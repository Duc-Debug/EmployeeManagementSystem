/**
 * NCL-06-CN-009: Modal Quản lý kỳ kế hoạch phân bổ (Allocation Planning Periods)
 * Thực thi quy tắc QTN-18: Khóa kế hoạch của kỳ, bảo vệ dải tuần và lưu bản chụp baseline.
 * Nâng cấp: Phím tắt Escape, Backdrop click, Hộp thoại xác nhận khóa chuẩn UI (thay thế window.confirm), Auto-dismiss alert.
 */
import { useState, useEffect, useCallback, useRef } from "react";
import {
  X,
  Lock,
  LockOpen,
  Calendar,
  CalendarPlus,
  Camera,
  RotateCcw,
  CheckCircle2,
  AlertTriangle,
  AlertCircle,
  Loader2,
  Shield,
  Search,
} from "lucide-react";
import { useAuthUser } from "@/lib/auth-session";
import {
  getAllocationPeriods,
  lockAllocationPeriod,
  type AllocationPeriodResult,
  type AllocationPeriodStatus,
} from "@/lib/api/allocation-periods";
import { CreateAllocationPeriodModal } from "./CreateAllocationPeriodModal";
import { UnlockAllocationPeriodModal } from "./UnlockAllocationPeriodModal";
import { AllocationPlanSnapshotModal } from "./AllocationPlanSnapshotModal";

interface AllocationPeriodManagementModalProps {
  open: boolean;
  onClose: () => void;
  currentYear?: number;
  onPeriodChanged?: () => void;
}

export function AllocationPeriodManagementModal({
  open,
  onClose,
  currentYear = new Date().getFullYear(),
  onPeriodChanged,
}: AllocationPeriodManagementModalProps) {
  const currentUser = useAuthUser();
  const normalizedRole = currentUser?.roleCode
    ? currentUser.roleCode.toUpperCase().replace(/_/g, "-")
    : "";

  // VT-03 có toàn quyền tạo kỳ, khóa kỳ và mở lại kỳ
  const canManagePeriods = normalizedRole === "VT-03";

  // Data states
  const [periods, setPeriods] = useState<AllocationPeriodResult[]>([]);
  const [selectedYear, setSelectedYear] = useState<number>(currentYear);
  const [statusFilter, setStatusFilter] = useState<"ALL" | AllocationPeriodStatus>("ALL");
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
  const [feedbackMessage, setFeedbackMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);

  // Sub-modal states
  const [isCreateOpen, setIsCreateOpen] = useState<boolean>(false);
  const [unlockTargetPeriod, setUnlockTargetPeriod] = useState<AllocationPeriodResult | null>(null);
  const [snapshotTargetPeriod, setSnapshotTargetPeriod] = useState<{
    id: number;
    name: string;
  } | null>(null);
  const [lockConfirmTarget, setLockConfirmTarget] = useState<AllocationPeriodResult | null>(null);

  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Phím tắt Escape để đóng modal chính (khi không có sub-modal nào mở)
  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        if (!isCreateOpen && !unlockTargetPeriod && !snapshotTargetPeriod && !lockConfirmTarget) {
          onClose();
        }
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [open, isCreateOpen, unlockTargetPeriod, snapshotTargetPeriod, lockConfirmTarget, onClose]);

  // Tự động tắt feedbackMessage sau 5 giây
  const showFeedback = (type: "success" | "error", text: string) => {
    if (timerRef.current) clearTimeout(timerRef.current);
    setFeedbackMessage({ type, text });
    if (type === "success") {
      timerRef.current = setTimeout(() => {
        setFeedbackMessage(null);
      }, 5000);
    }
  };

  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);

  const loadPeriods = useCallback(async () => {
    try {
      setIsLoading(true);
      setFeedbackMessage(null);
      const data = await getAllocationPeriods({
        year: selectedYear,
        status: statusFilter === "ALL" ? undefined : statusFilter,
      });
      setPeriods(data);
    } catch (err: unknown) {
      showFeedback(
        "error",
        err instanceof Error
          ? err.message
          : "Không thể tải danh sách kỳ kế hoạch phân bổ."
      );
    } finally {
      setIsLoading(false);
    }
  }, [selectedYear, statusFilter]);

  useEffect(() => {
    if (open) {
      loadPeriods();
    }
  }, [open, loadPeriods]);

  // Xác nhận khóa kỳ thực sự
  const handleConfirmLockPeriod = async () => {
    if (!lockConfirmTarget || !canManagePeriods) return;
    const period = lockConfirmTarget;

    try {
      setActionLoadingId(period.id);
      setFeedbackMessage(null);
      const res = await lockAllocationPeriod(period.id);
      setPeriods((prev) =>
        prev.map((p) => (p.id === res.id ? res : p))
      );
      showFeedback(
        "success",
        `Đã khóa kỳ "${period.name}" thành công và tạo bản chụp kế hoạch v${res.latestSnapshot?.snapshotVersion ?? 1} (${res.latestSnapshot?.totalAllocations ?? 0} lượt phân bổ, ${res.latestSnapshot?.totalAllocatedHours ?? 0}h).`
      );
      setLockConfirmTarget(null);
      if (onPeriodChanged) onPeriodChanged();
    } catch (err: unknown) {
      showFeedback(
        "error",
        err instanceof Error
          ? err.message
          : "Không thể khóa kỳ kế hoạch. Vui lòng thử lại."
      );
    } finally {
      setActionLoadingId(null);
    }
  };

  const handlePeriodCreated = (newPeriod: AllocationPeriodResult) => {
    setPeriods((prev) => [newPeriod, ...prev]);
    showFeedback("success", `Đã tạo mới kỳ kế hoạch "${newPeriod.name}" thành công.`);
    if (onPeriodChanged) onPeriodChanged();
  };

  const handlePeriodUnlocked = (updated: AllocationPeriodResult) => {
    setPeriods((prev) =>
      prev.map((p) => (p.id === updated.id ? updated : p))
    );
    showFeedback(
      "success",
      `Đã mở lại kỳ "${updated.name}" thành công. Lý do: "${updated.unlockReason}".`
    );
    if (onPeriodChanged) onPeriodChanged();
  };

  if (!open) return null;

  const filteredPeriods = periods.filter((p) => {
    if (!searchTerm.trim()) return true;
    return p.name.toLowerCase().includes(searchTerm.toLowerCase());
  });

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="w-full max-w-5xl max-h-[90vh] flex flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 p-4 shrink-0">
          <div className="flex items-center gap-2.5">
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-100 text-indigo-700">
              <Calendar className="h-5 w-5" />
            </span>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-sm font-bold text-slate-800">
                  Quản Lý Khóa Kế Hoạch Phân Bổ Kỳ (NCL-06-CN-009)
                </h3>
                <span className="rounded-full bg-indigo-50 border border-indigo-200 px-2 py-0.5 text-[10px] font-bold text-indigo-700">
                  Quy tắc QTN-18
                </span>
              </div>
              <p className="text-[11px] text-slate-500">
                Chốt và khóa kỳ kế hoạch phân bổ nguồn lực theo Quý/Tháng/Năm, lưu bản chụp Baseline
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

        {/* Toolbar & Filters */}
        <div className="border-b border-slate-200 bg-white p-4 shrink-0 space-y-3">
          {/* Feedback Alerts */}
          {feedbackMessage && (
            <div
              className={`flex items-center justify-between gap-2 rounded-xl p-3 text-xs ${
                feedbackMessage.type === "success"
                  ? "border border-emerald-200 bg-emerald-50 text-emerald-800"
                  : "border border-rose-200 bg-rose-50 text-rose-800"
              }`}
            >
              <div className="flex items-center gap-2">
                {feedbackMessage.type === "success" ? (
                  <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
                ) : (
                  <AlertCircle className="h-4 w-4 text-rose-600 shrink-0" />
                )}
                <span>{feedbackMessage.text}</span>
              </div>
              <button
                type="button"
                onClick={() => setFeedbackMessage(null)}
                className="text-slate-400 hover:text-slate-600"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          )}

          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-2.5">
              {/* Year Filter */}
              <div className="flex items-center gap-1.5 text-xs text-slate-600">
                <span className="font-semibold">Năm:</span>
                <select
                  value={selectedYear}
                  onChange={(e) => setSelectedYear(Number(e.target.value))}
                  className="rounded-xl border border-slate-300 bg-white px-2.5 py-1.5 text-xs font-medium text-slate-800 focus:border-indigo-500 focus:outline-none"
                >
                  {[currentYear - 1, currentYear, currentYear + 1].map((y) => (
                    <option key={y} value={y}>
                      Năm {y}
                    </option>
                  ))}
                </select>
              </div>

              {/* Status Filter */}
              <div className="flex items-center gap-1.5 text-xs text-slate-600">
                <span className="font-semibold">Trạng thái:</span>
                <select
                  value={statusFilter}
                  onChange={(e) =>
                    setStatusFilter(e.target.value as "ALL" | AllocationPeriodStatus)
                  }
                  className="rounded-xl border border-slate-300 bg-white px-2.5 py-1.5 text-xs font-medium text-slate-800 focus:border-indigo-500 focus:outline-none"
                >
                  <option value="ALL">Tất cả trạng thái</option>
                  <option value="OPEN">Đang mở (OPEN)</option>
                  <option value="LOCKED">Đã khóa (LOCKED)</option>
                </select>
              </div>

              {/* Search input */}
              <div className="relative">
                <Search className="absolute left-2.5 top-2 h-3.5 w-3.5 text-slate-400" />
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Tìm tên kỳ..."
                  className="rounded-xl border border-slate-300 pl-8 pr-3 py-1.5 text-xs text-slate-800 placeholder-slate-400 focus:border-indigo-500 focus:outline-none w-44"
                />
              </div>

              <button
                type="button"
                onClick={loadPeriods}
                className="rounded-xl border border-slate-200 bg-slate-50 p-2 text-slate-600 hover:bg-slate-100 transition"
                title="Tải lại danh sách"
              >
                <RotateCcw className="h-3.5 w-3.5" />
              </button>
            </div>

            {/* Action button: Create Period */}
            {canManagePeriods ? (
              <button
                type="button"
                onClick={() => setIsCreateOpen(true)}
                className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3.5 py-2 text-xs font-semibold text-white shadow-xs hover:bg-indigo-700 transition"
              >
                <CalendarPlus className="h-4 w-4" />
                <span>+ Tạo kỳ mới</span>
              </button>
            ) : (
              <div className="flex items-center gap-1 text-[11px] text-slate-400 bg-slate-50 border border-slate-200 rounded-xl px-2.5 py-1.5">
                <Shield className="h-3.5 w-3.5 text-slate-400" />
                <span>Chế độ xem (Read-only)</span>
              </div>
            )}
          </div>
        </div>

        {/* Table Content */}
        <div className="flex-1 overflow-y-auto p-4">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center p-12 text-slate-400 space-y-2">
              <Loader2 className="h-7 w-7 animate-spin text-indigo-600" />
              <span className="text-xs">Đang tải danh sách kỳ kế hoạch...</span>
            </div>
          ) : filteredPeriods.length === 0 ? (
            <div className="flex flex-col items-center justify-center p-12 text-center rounded-2xl border border-dashed border-slate-200 bg-slate-50">
              <Calendar className="h-10 w-10 text-slate-300 mb-2" />
              <p className="text-xs font-semibold text-slate-600">
                Không có kỳ kế hoạch phân bổ nào trong năm {selectedYear}
              </p>
              <p className="text-[11px] text-slate-400 mt-1 max-w-sm">
                {canManagePeriods
                  ? 'Bấm nút "+ Tạo kỳ mới" ở trên để khởi tạo kỳ kế hoạch phân bổ theo Quý hoặc Tháng.'
                  : "Chưa có kỳ kế hoạch nào được thiết lập cho năm này."}
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-slate-200">
              <table className="w-full text-left text-xs text-slate-600">
                <thead className="bg-slate-100 text-slate-700 font-semibold border-b border-slate-200">
                  <tr>
                    <th className="p-3">Tên kỳ kế hoạch</th>
                    <th className="p-3">Loại kỳ</th>
                    <th className="p-3 text-center">Dải tuần áp dụng</th>
                    <th className="p-3 text-center">Trạng thái</th>
                    <th className="p-3">Bản chụp (Baseline)</th>
                    <th className="p-3">Thông tin Khóa / Mở lại</th>
                    <th className="p-3 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white">
                  {filteredPeriods.map((p) => {
                    const isLocked = p.status === "LOCKED";
                    const isActionLoading = actionLoadingId === p.id;

                    return (
                      <tr key={p.id} className="hover:bg-slate-50 transition">
                        {/* Tên kỳ */}
                        <td className="p-3">
                          <span className="font-bold text-slate-900 block">{p.name}</span>
                          <span className="text-[10px] text-slate-400">
                            Năm {p.year} • Tạo ngày {new Date(p.createdAt).toLocaleDateString("vi-VN")}
                          </span>
                        </td>

                        {/* Loại kỳ */}
                        <td className="p-3">
                          <span className="rounded-md bg-slate-100 px-2 py-0.5 text-[11px] font-semibold text-slate-700">
                            {p.periodType === "QUARTER"
                              ? "Theo Quý"
                              : p.periodType === "MONTH"
                              ? "Theo Tháng"
                              : "Theo Năm"}
                          </span>
                        </td>

                        {/* Dải tuần */}
                        <td className="p-3 text-center font-semibold text-slate-800">
                          Tuần {p.startWeek} → Tuần {p.endWeek}
                        </td>

                        {/* Trạng thái */}
                        <td className="p-3 text-center">
                          {isLocked ? (
                            <span className="inline-flex items-center gap-1 rounded-full bg-rose-50 border border-rose-200 px-2.5 py-1 text-[11px] font-bold text-rose-700">
                              <Lock className="h-3 w-3 text-rose-600" />
                              ĐÃ KHÓA
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 border border-emerald-200 px-2.5 py-1 text-[11px] font-bold text-emerald-700">
                              <LockOpen className="h-3 w-3 text-emerald-600" />
                              ĐANG MỞ
                            </span>
                          )}
                        </td>

                        {/* Bản chụp */}
                        <td className="p-3">
                          {p.latestSnapshot ? (
                            <button
                              type="button"
                              onClick={() =>
                                setSnapshotTargetPeriod({ id: p.id, name: p.name })
                              }
                              className="inline-flex items-center gap-1 text-[11px] font-semibold text-indigo-600 hover:text-indigo-800 hover:underline"
                            >
                              <Camera className="h-3.5 w-3.5" />
                              <span>v{p.latestSnapshot.snapshotVersion} ({p.latestSnapshot.totalAllocations} lượt)</span>
                            </button>
                          ) : (
                            <span className="text-[11px] text-slate-400">Chưa có bản chụp</span>
                          )}
                        </td>

                        {/* Thông tin Khóa / Mở lại */}
                        <td className="p-3 text-[11px]">
                          {isLocked && p.lockedAt ? (
                            <div>
                              <span className="text-slate-700 font-medium">
                                Khóa bởi: {p.lockedByName || `ID ${p.lockedBy}`}
                              </span>
                              <span className="text-slate-400 block text-[10px]">
                                {new Date(p.lockedAt).toLocaleString("vi-VN")}
                              </span>
                            </div>
                          ) : p.unlockedAt ? (
                            <div>
                              <span className="text-amber-700 font-medium">
                                Mở lại bởi: {p.unlockedByName || `ID ${p.unlockedBy}`}
                              </span>
                              {p.unlockReason && (
                                <span className="text-slate-500 block italic truncate max-w-xs" title={p.unlockReason}>
                                  Lý do: &quot;{p.unlockReason}&quot;
                                </span>
                              )}
                            </div>
                          ) : (
                            <span className="text-slate-400">--</span>
                          )}
                        </td>

                        {/* Nút thao tác */}
                        <td className="p-3 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            {/* Nút xem snapshot */}
                            <button
                              type="button"
                              onClick={() =>
                                setSnapshotTargetPeriod({ id: p.id, name: p.name })
                              }
                              className="rounded-lg border border-slate-200 bg-white p-1.5 text-slate-600 hover:bg-indigo-50 hover:text-indigo-700 transition"
                              title="Xem lịch sử bản chụp (Baselines)"
                            >
                              <Camera className="h-4 w-4" />
                            </button>

                            {/* Thao tác Khóa / Mở lại chỉ dành cho VT-03 */}
                            {canManagePeriods && (
                              <>
                                {isLocked ? (
                                  <button
                                    type="button"
                                    onClick={() => setUnlockTargetPeriod(p)}
                                    disabled={isActionLoading}
                                    className="inline-flex items-center gap-1 rounded-lg border border-amber-300 bg-amber-50 px-2.5 py-1.5 text-xs font-semibold text-amber-800 hover:bg-amber-100 transition disabled:opacity-50"
                                  >
                                    <LockOpen className="h-3.5 w-3.5" />
                                    <span>Mở lại</span>
                                  </button>
                                ) : (
                                  <button
                                    type="button"
                                    onClick={() => setLockConfirmTarget(p)}
                                    disabled={isActionLoading}
                                    className="inline-flex items-center gap-1 rounded-lg bg-rose-600 px-2.5 py-1.5 text-xs font-semibold text-white hover:bg-rose-700 transition disabled:opacity-50"
                                  >
                                    {isActionLoading ? (
                                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                                    ) : (
                                      <Lock className="h-3.5 w-3.5" />
                                    )}
                                    <span>Khóa kỳ</span>
                                  </button>
                                )}
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between border-t border-slate-200 bg-slate-50 px-5 py-3 shrink-0">
          <div className="flex items-center gap-1.5 text-[11px] text-slate-500">
            <AlertTriangle className="h-3.5 w-3.5 text-amber-500" />
            <span>Quy tắc QTN-18: Kỳ đã khóa sẽ từ chối mọi thao tác phân bổ mới hoặc cập nhật giờ phân bổ.</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl bg-slate-200 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-300 transition"
          >
            Đóng
          </button>
        </div>
      </div>

      {/* Sub-modal: Tạo kỳ mới */}
      <CreateAllocationPeriodModal
        open={isCreateOpen}
        initialYear={selectedYear}
        onClose={() => setIsCreateOpen(false)}
        onCreated={handlePeriodCreated}
      />

      {/* Sub-modal: Mở lại kỳ */}
      <UnlockAllocationPeriodModal
        open={!!unlockTargetPeriod}
        period={unlockTargetPeriod}
        onClose={() => setUnlockTargetPeriod(null)}
        onUnlocked={handlePeriodUnlocked}
      />

      {/* Sub-modal: Xem bản chụp Baseline */}
      <AllocationPlanSnapshotModal
        open={!!snapshotTargetPeriod}
        periodId={snapshotTargetPeriod?.id ?? null}
        periodName={snapshotTargetPeriod?.name}
        onClose={() => setSnapshotTargetPeriod(null)}
      />

      {/* Sub-modal: Hộp thoại xác nhận Khóa kỳ (Chuẩn UI Tailwind thay thế window.confirm) */}
      {lockConfirmTarget && (
        <div
          className="fixed inset-0 z-60 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-xs animate-in fade-in"
          onClick={(e) => {
            if (e.target === e.currentTarget) setLockConfirmTarget(null);
          }}
        >
          <div className="w-full max-w-md overflow-hidden rounded-2xl border border-rose-200 bg-white shadow-2xl transition-all">
            <div className="flex items-center justify-between border-b border-rose-100 bg-rose-50/80 p-4">
              <div className="flex items-center gap-2.5">
                <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-rose-100 text-rose-700">
                  <Lock className="h-5 w-5" />
                </span>
                <h3 className="text-sm font-bold text-rose-900">
                  Xác Nhận Khóa Kế Hoạch Kỳ (QTN-18)
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setLockConfirmTarget(null)}
                className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="p-5 space-y-3.5 text-xs text-slate-700">
              <p className="leading-relaxed">
                Bạn đang yêu cầu khóa kỳ:{" "}
                <strong className="text-slate-900 font-bold">
                  {lockConfirmTarget.name}
                </strong>{" "}
                (Tuần {lockConfirmTarget.startWeek} - {lockConfirmTarget.endWeek} / Năm {lockConfirmTarget.year}).
              </p>

              <div className="rounded-xl border border-rose-100 bg-rose-50/50 p-3 text-[11px] text-rose-800 space-y-1.5">
                <div className="font-bold flex items-center gap-1 text-rose-900">
                  <AlertTriangle className="h-3.5 w-3.5 text-rose-600 shrink-0" />
                  <span>Hệ quả nghiệp vụ sau khi khóa:</span>
                </div>
                <ul className="list-disc list-inside space-y-1 text-rose-700">
                  <li>Tự động lưu toàn bộ dữ liệu phân bổ hiện tại làm bản chụp chuẩn (Baseline).</li>
                  <li>Ngăn chặn thêm/sửa mọi phân bổ vào các tuần thuộc kỳ này trên toàn hệ thống.</li>
                </ul>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2.5 border-t border-slate-200 bg-slate-50 px-5 py-3">
              <button
                type="button"
                onClick={() => setLockConfirmTarget(null)}
                disabled={actionLoadingId === lockConfirmTarget.id}
                className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmLockPeriod}
                disabled={actionLoadingId === lockConfirmTarget.id}
                className="inline-flex items-center gap-1.5 rounded-xl bg-rose-600 px-4 py-2 text-xs font-semibold text-white shadow-xs hover:bg-rose-700 disabled:opacity-50 transition"
              >
                {actionLoadingId === lockConfirmTarget.id && (
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                )}
                <span>Xác nhận khóa</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
