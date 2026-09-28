import { useState, useEffect, useCallback } from "react";
import {
  X,
  Clock,
  Calendar,
  Trash2,
  FileText,
  History,
  AlertTriangle,
  CheckCircle2,
  Loader2,
  ArrowRight,
} from "lucide-react";
import {
  adjustAllocation,
  removeAllocation,
  saveAllocationVarianceNote,
  getAllocationHistory,
  type AdjustmentAction,
  type AllocationChangeLogResult,
} from "@/lib/api/allocations";

export interface AllocationItem {
  id: number;
  employeeId: number;
  employeeName: string;
  projectId: number;
  projectName?: string;
  year: number;
  weekNumber: number;
  allocatedHours: number;
  allocationPercentage?: number;
  varianceNote?: string | null;
}

interface AllocationAdjustmentModalProps {
  open: boolean;
  allocation: AllocationItem | null;
  onClose: () => void;
  onSuccess: (message?: string) => void;
  canManage?: boolean;
}

type TabType = "EDIT_HOURS" | "MOVE_WEEK" | "REMOVE" | "NOTE_VARIANCE" | "HISTORY";

export function AllocationAdjustmentModal({
  open,
  allocation,
  onClose,
  onSuccess,
  canManage = true,
}: AllocationAdjustmentModalProps) {
  const [activeTab, setActiveTab] = useState<TabType>("EDIT_HOURS");
  const [currentAllocationId, setCurrentAllocationId] = useState<number>(allocation?.id || 1);

  // Edit hours state
  const [newHours, setNewHours] = useState<number | ''>(20);
  const [overloadReason, setOverloadReason] = useState<string>("");

  // Move week state
  const [targetYear, setTargetYear] = useState<number | ''>(new Date().getFullYear());
  const [targetWeek, setTargetWeek] = useState<number | ''>(1);

  // Variance note state
  const [varianceReason, setVarianceReason] = useState<string>("");

  // History state
  const [historyList, setHistoryList] = useState<AllocationChangeLogResult[]>([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState<boolean>(false);

  // Loading & Error states
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [conflictPromptVariance, setConflictPromptVariance] = useState<boolean>(false);

  // Initialize form state when allocation changes
  useEffect(() => {
    if (allocation) {
      setCurrentAllocationId(allocation.id > 0 ? allocation.id : 1);
      setNewHours(allocation.allocatedHours || 20);
      setOverloadReason("");
      setTargetYear(allocation.year);
      setTargetWeek(allocation.weekNumber < 52 ? allocation.weekNumber + 1 : 1);
      setVarianceReason(allocation.varianceNote || "");
      setErrorMessage(null);
      setConflictPromptVariance(false);
      setActiveTab(canManage ? "EDIT_HOURS" : "HISTORY");
    }
  }, [allocation, canManage]);

  const loadHistory = useCallback(async () => {
    const idToLoad = currentAllocationId;
    if (!idToLoad) return;
    setIsLoadingHistory(true);
    setErrorMessage(null);
    try {
      const logs = await getAllocationHistory(idToLoad);
      setHistoryList(logs);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Không thể tải lịch sử thay đổi";
      setErrorMessage(msg);
    } finally {
      setIsLoadingHistory(false);
    }
  }, [currentAllocationId]);

  useEffect(() => {
    if (open && activeTab === "HISTORY" && currentAllocationId) {
      loadHistory();
    }
  }, [open, activeTab, currentAllocationId, loadHistory]);

  if (!open || !allocation) return null;

  // Handle Edit Hours (TC-01, TC-03)
  const handleEditHours = async () => {
    const hours = Number(newHours);
    if (!hours || hours <= 0 || hours > 168) {
      setErrorMessage("Số giờ phân bổ phải lớn hơn 0 và không vượt quá 168 giờ/tuần");
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await adjustAllocation(currentAllocationId, {
        action: "EDIT_HOURS",
        newHours: hours,
        overloadReason: overloadReason.trim() || undefined,
      });
      onSuccess(`Đã cập nhật số giờ phân bổ thành ${hours}h/tuần`);
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Điều chỉnh số giờ thất bại";
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle Move Week
  const handleMoveWeek = async () => {
    const tYear = Number(targetYear);
    const tWeek = Number(targetWeek);
    if (!tYear || !tWeek) {
      setErrorMessage("Vui lòng nhập năm đích và tuần đích hợp lệ");
      return;
    }
    if (tYear === allocation.year && tWeek === allocation.weekNumber) {
      setErrorMessage("Tuần đích phải khác tuần hiện tại đang phân bổ");
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await adjustAllocation(currentAllocationId, {
        action: "MOVE_WEEK",
        targetYear: tYear,
        targetWeek: tWeek,
      });
      onSuccess(`Đã chuyển phân bổ sang Tuần ${tWeek}/${tYear}`);
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Chuyển tuần phân bổ thất bại";
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle Remove Allocation (TC-02)
  const handleRemove = async () => {
    setIsSubmitting(true);
    setErrorMessage(null);
    setConflictPromptVariance(false);
    try {
      await removeAllocation(currentAllocationId);
      onSuccess("Đã gỡ bỏ dòng phân bổ nguồn lực thành công");
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Gỡ phân bổ thất bại";
      setErrorMessage(msg);
      // TC-02: Nếu trả về 409 hoặc lỗi tuần đã kết thúc và có giờ công thực tế
      if (
        msg.includes("409") ||
        msg.toLowerCase().includes("thực tế") ||
        msg.toLowerCase().includes("kết thúc") ||
        msg.toLowerCase().includes("chênh lệch")
      ) {
        setConflictPromptVariance(true);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle Save Variance Note (TC-02 alternative)
  const handleSaveVariance = async () => {
    if (!varianceReason.trim()) {
      setErrorMessage("Vui lòng nhập lý do chênh lệch");
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await saveAllocationVarianceNote(currentAllocationId, {
        varianceReason: varianceReason.trim(),
      });
      onSuccess("Đã lưu ghi chú lý do chênh lệch thành công");
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Ghi chú chênh lệch thất bại";
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatActionName = (action: AdjustmentAction) => {
    switch (action) {
      case "EDIT_HOURS":
        return "Sửa số giờ";
      case "MOVE_WEEK":
        return "Chuyển tuần";
      case "REMOVE":
        return "Gỡ phân bổ";
      case "NOTE_VARIANCE":
        return "Ghi chú chênh lệch";
      default:
        return action;
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-xs animate-in fade-in">
      <div className="flex flex-col w-full max-w-2xl max-h-[90vh] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50/80 px-6 py-4">
          <div>
            <h3 className="text-base font-bold text-slate-900">
              Điều chỉnh phân bổ nguồn lực
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              {allocation.employeeName} • {allocation.projectName || `Dự án #${allocation.projectId}`} • Tuần {allocation.weekNumber}/{allocation.year}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl p-1.5 text-slate-400 hover:bg-slate-200/60 hover:text-slate-700 transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="flex border-b border-slate-200 bg-slate-50/40 px-6 gap-2 text-xs font-semibold overflow-x-auto">
          {canManage && (
            <>
              <button
                type="button"
                onClick={() => { setActiveTab("EDIT_HOURS"); setErrorMessage(null); }}
                className={`flex items-center gap-1.5 py-3 px-3 border-b-2 transition whitespace-nowrap ${
                  activeTab === "EDIT_HOURS"
                    ? "border-indigo-600 text-indigo-600"
                    : "border-transparent text-slate-500 hover:text-slate-800"
                }`}
              >
                <Clock className="h-4 w-4" />
                <span>Sửa giờ</span>
              </button>
              <button
                type="button"
                onClick={() => { setActiveTab("MOVE_WEEK"); setErrorMessage(null); }}
                className={`flex items-center gap-1.5 py-3 px-3 border-b-2 transition whitespace-nowrap ${
                  activeTab === "MOVE_WEEK"
                    ? "border-indigo-600 text-indigo-600"
                    : "border-transparent text-slate-500 hover:text-slate-800"
                }`}
              >
                <Calendar className="h-4 w-4" />
                <span>Chuyển tuần</span>
              </button>
              <button
                type="button"
                onClick={() => { setActiveTab("REMOVE"); setErrorMessage(null); }}
                className={`flex items-center gap-1.5 py-3 px-3 border-b-2 transition whitespace-nowrap ${
                  activeTab === "REMOVE"
                    ? "border-rose-600 text-rose-600"
                    : "border-transparent text-slate-500 hover:text-slate-800"
                }`}
              >
                <Trash2 className="h-4 w-4" />
                <span>Gỡ phân bổ</span>
              </button>
              <button
                type="button"
                onClick={() => { setActiveTab("NOTE_VARIANCE"); setErrorMessage(null); }}
                className={`flex items-center gap-1.5 py-3 px-3 border-b-2 transition whitespace-nowrap ${
                  activeTab === "NOTE_VARIANCE"
                    ? "border-amber-600 text-amber-600"
                    : "border-transparent text-slate-500 hover:text-slate-800"
                }`}
              >
                <FileText className="h-4 w-4" />
                <span>Ghi chú chênh lệch</span>
              </button>
            </>
          )}
          <button
            type="button"
            onClick={() => { setActiveTab("HISTORY"); setErrorMessage(null); }}
            className={`flex items-center gap-1.5 py-3 px-3 border-b-2 transition whitespace-nowrap ${
              activeTab === "HISTORY"
                ? "border-indigo-600 text-indigo-600"
                : "border-transparent text-slate-500 hover:text-slate-800"
            }`}
          >
            <History className="h-4 w-4" />
            <span>Lịch sử thay đổi</span>
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 overflow-y-auto space-y-4 flex-1">
          {errorMessage && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-800 flex items-start gap-2">
              <AlertTriangle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
              <div className="flex-1">
                <p className="font-semibold">Đã xảy ra lỗi:</p>
                <p className="mt-0.5">{errorMessage}</p>
                {conflictPromptVariance && (
                  <div className="mt-2 pt-2 border-t border-rose-200">
                    <p className="font-medium text-rose-900">
                      Không thể gỡ dòng phân bổ khi tuần đã kết thúc và nhân sự đã có giờ làm thực tế.
                    </p>
                    <button
                      type="button"
                      onClick={() => {
                        setActiveTab("NOTE_VARIANCE");
                        setConflictPromptVariance(false);
                      }}
                      className="mt-2 inline-flex items-center gap-1.5 rounded-lg bg-amber-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-amber-700 transition"
                    >
                      <FileText className="h-3.5 w-3.5" />
                      <span>Chuyển sang ghi chú lý do chênh lệch</span>
                    </button>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* TAB 1: Sửa giờ (EDIT_HOURS) */}
          {activeTab === "EDIT_HOURS" && (
            <div className="space-y-4">
              <div className="rounded-xl border border-slate-200 bg-slate-50/60 p-4 space-y-2">
                <div className="flex justify-between text-xs text-slate-600">
                  <span>Số giờ hiện tại:</span>
                  <span className="font-bold text-slate-900">{allocation.allocatedHours} giờ</span>
                </div>
                {allocation.allocationPercentage != null && (
                  <div className="flex justify-between text-xs text-slate-600">
                    <span>Tỷ lệ phân bổ hiện tại:</span>
                    <span className="font-semibold text-slate-800">{allocation.allocationPercentage}%</span>
                  </div>
                )}
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">
                  Số giờ phân bổ mới (giờ/tuần) <span className="text-rose-500">*</span>
                </label>
                <input
                  type="number"
                  min="1"
                  max="168"
                  step="0.5"
                  value={newHours}
                  onChange={(e) => {
                    const v = e.target.value;
                    setNewHours(v === '' ? '' : parseFloat(v));
                  }}
                  className="w-full rounded-xl border border-slate-300 px-3.5 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Lý do quá tải
                </label>
                <textarea
                  rows={2}
                  value={overloadReason}
                  onChange={(e) => setOverloadReason(e.target.value)}
                  placeholder="Nhập lý do phê duyệt quá tải nếu cần thiết..."
                  className="w-full rounded-xl border border-slate-300 px-3.5 py-2 text-xs text-slate-900 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={onClose}
                  className="rounded-xl border border-slate-300 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
                >
                  Hủy
                </button>
                <button
                  type="button"
                  disabled={isSubmitting}
                  onClick={handleEditHours}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 disabled:opacity-50 transition"
                >
                  {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
                  <span>Cập nhật số giờ</span>
                </button>
              </div>
            </div>
          )}

          {/* TAB 2: Chuyển tuần (MOVE_WEEK) */}
          {activeTab === "MOVE_WEEK" && (
            <div className="space-y-4">
              <div className="rounded-xl border border-slate-200 bg-slate-50/60 p-4 text-xs text-slate-600 space-y-1">
                <p>
                  Tuần hiện tại: <span className="font-bold text-slate-900">Tuần {allocation.weekNumber} / Năm {allocation.year}</span>
                </p>
                <p className="text-slate-500 text-[11px]">
                  Lưu ý: Không thể chuyển phân bổ đến tuần đã kết thúc trong quá khứ.
                </p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5">
                    Năm đích <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="number"
                    min="2020"
                    max="2035"
                    value={targetYear}
                    onChange={(e) => {
                      const v = e.target.value;
                      setTargetYear(v === '' ? '' : parseInt(v, 10));
                    }}
                    onBlur={() => {
                      if (targetYear === '' || targetYear < 2020) setTargetYear(2020);
                      else if (targetYear > 2035) setTargetYear(2035);
                    }}
                    className="w-full rounded-xl border border-slate-300 px-3.5 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5">
                    Tuần đích (1-53) <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="53"
                    value={targetWeek}
                    onChange={(e) => {
                      const v = e.target.value;
                      setTargetWeek(v === '' ? '' : parseInt(v, 10));
                    }}
                    onBlur={() => {
                      if (targetWeek === '' || targetWeek < 1) setTargetWeek(1);
                      else if (targetWeek > 53) setTargetWeek(53);
                    }}
                    className="w-full rounded-xl border border-slate-300 px-3.5 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={onClose}
                  className="rounded-xl border border-slate-300 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
                >
                  Hủy
                </button>
                <button
                  type="button"
                  disabled={isSubmitting}
                  onClick={handleMoveWeek}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 disabled:opacity-50 transition"
                >
                  {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <ArrowRight className="h-4 w-4" />}
                  <span>Chuyển tuần phân bổ</span>
                </button>
              </div>
            </div>
          )}

          {/* TAB 3: Gỡ phân bổ (REMOVE) */}
          {activeTab === "REMOVE" && (
            <div className="space-y-4">
              <div className="rounded-xl border border-rose-200 bg-rose-50/60 p-4 text-xs text-rose-900 space-y-2">
                <div className="flex items-center gap-2 font-bold text-rose-800">
                  <AlertTriangle className="h-4 w-4 text-rose-600" />
                  <span>Xác nhận gỡ dòng phân bổ</span>
                </div>
                <p>
                  Bạn có chắc chắn muốn gỡ bỏ hoàn toàn dòng phân bổ ({allocation.allocatedHours} giờ) của{" "}
                  <strong>{allocation.employeeName}</strong> trong Tuần {allocation.weekNumber}/{allocation.year} không?
                </p>
                <p className="text-[11px] text-rose-700 font-medium">
                  Hệ thống sẽ kiểm tra và chặn thao tác nếu tuần này đã trôi qua và nhân sự đã có giờ làm thực tế.
                </p>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={onClose}
                  className="rounded-xl border border-slate-300 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
                >
                  Hủy
                </button>
                <button
                  type="button"
                  disabled={isSubmitting}
                  onClick={handleRemove}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-rose-600 px-4 py-2 text-xs font-semibold text-white hover:bg-rose-700 disabled:opacity-50 transition"
                >
                  {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
                  <span>Xác nhận gỡ phân bổ</span>
                </button>
              </div>
            </div>
          )}

          {/* TAB 4: Ghi chú chênh lệch (NOTE_VARIANCE) */}
          {activeTab === "NOTE_VARIANCE" && (
            <div className="space-y-4">
              <div className="rounded-xl border border-amber-200 bg-amber-50/60 p-4 text-xs text-amber-900 space-y-1">
                <p className="font-bold text-amber-800">Ghi chú lý do chênh lệch</p>
                <p className="text-[11px] text-amber-700">
                  Sử dụng khi kế hoạch phân bổ khác với thực tế hoặc khi không thể gỡ phân bổ của tuần đã kết thúc.
                </p>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">
                  Lý do chênh lệch <span className="text-rose-500">*</span>
                </label>
                <textarea
                  rows={4}
                  value={varianceReason}
                  onChange={(e) => setVarianceReason(e.target.value)}
                  placeholder="Ví dụ: Nhân sự hoàn thành công việc sớm hơn dự kiến / Khách hàng hoãn triển khai..."
                  className="w-full rounded-xl border border-slate-300 px-3.5 py-2 text-xs text-slate-900 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={onClose}
                  className="rounded-xl border border-slate-300 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
                >
                  Hủy
                </button>
                <button
                  type="button"
                  disabled={isSubmitting || !varianceReason.trim()}
                  onClick={handleSaveVariance}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-amber-600 px-4 py-2 text-xs font-semibold text-white hover:bg-amber-700 disabled:opacity-50 transition"
                >
                  {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileText className="h-4 w-4" />}
                  <span>Lưu lý do chênh lệch</span>
                </button>
              </div>
            </div>
          )}

          {/* TAB 5: Lịch sử thay đổi (HISTORY - TC-04) */}
          {activeTab === "HISTORY" && (
            <div className="space-y-3">
              {isLoadingHistory ? (
                <div className="flex items-center justify-center p-8 text-xs text-slate-500">
                  <Loader2 className="h-5 w-5 animate-spin mr-2 text-indigo-600" />
                  <span>Đang tải lịch sử thay đổi...</span>
                </div>
              ) : historyList.length === 0 ? (
                <div className="rounded-xl border border-slate-200 bg-slate-50 p-6 text-center text-xs text-slate-500">
                  Chưa có lịch sử điều chỉnh nào cho dòng phân bổ này.
                </div>
              ) : (
                <div className="space-y-2.5">
                  {historyList.map((log) => (
                    <div
                      key={log.id}
                      className="rounded-xl border border-slate-200 bg-white p-3.5 shadow-2xs hover:border-slate-300 transition text-xs"
                    >
                      <div className="flex items-center justify-between gap-2 border-b border-slate-100 pb-2 mb-2">
                        <span className="font-bold text-slate-800">
                          {formatActionName(log.action)}
                        </span>
                        <span className="text-[11px] text-slate-400">
                          {new Date(log.changedAt).toLocaleString("vi-VN")}
                        </span>
                      </div>
                      <div className="grid grid-cols-2 gap-2 text-slate-600">
                        <div>
                          <span className="text-[10px] uppercase font-bold text-slate-400 block">Trước điều chỉnh:</span>
                          <span className="font-mono text-slate-700">{log.oldValue || "(Trống)"}</span>
                        </div>
                        <div>
                          <span className="text-[10px] uppercase font-bold text-slate-400 block">Sau điều chỉnh:</span>
                          <span className="font-mono text-indigo-600 font-semibold">{log.newValue || "(Trống)"}</span>
                        </div>
                      </div>
                      <div className="flex items-center justify-between text-[11px] text-slate-400 mt-2.5 pt-2 border-t border-slate-50">
                        <span>Thực hiện bởi: <strong className="text-slate-600">{log.changedByName}</strong></span>
                        {log.notifiedPmIds && (
                          <span className="text-emerald-600 font-medium">✓ Đã thông báo QLDA</span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
