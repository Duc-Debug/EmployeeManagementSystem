/**
 * NCL-07-CN-004: Modal Cấu hình ngưỡng cảnh báo quá tải và nhàn rỗi (QTN-23)
 * Dành riêng cho Ban Giám đốc (VT-01) để điều chỉnh ngưỡng quá tải và nhàn rỗi toàn công ty.
 */
import { useState, useEffect, useCallback } from "react";
import {
  X,
  Sliders,
  History,
  AlertTriangle,
  CheckCircle2,
  AlertCircle,
  Loader2,
  RotateCcw,
  Clock,
} from "lucide-react";
import {
  getCapacityThreshold,
  configureCapacityThreshold,
  getCapacityThresholdHistory,
  type CapacityThresholdResult,
  type CapacityThresholdHistoryResult,
  type CapacityThresholdScope,
} from "@/lib/api/capacity-thresholds";

interface CapacityThresholdConfigModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess?: () => void;
  scopeType?: CapacityThresholdScope;
  orgUnitId?: number;
}

export function CapacityThresholdConfigModal({
  open,
  onClose,
  onSuccess,
  scopeType = "COMPANY",
  orgUnitId,
}: CapacityThresholdConfigModalProps) {
  const [activeTab, setActiveTab] = useState<"CONFIG" | "HISTORY">("CONFIG");

  // Form states
  const [overloadThreshold, setOverloadThreshold] = useState<number>(100);
  const [idleThreshold, setIdleThreshold] = useState<number>(50);
  const [currentConfig, setCurrentConfig] = useState<CapacityThresholdResult | null>(null);

  // History states
  const [historyList, setHistoryList] = useState<CapacityThresholdHistoryResult[]>([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState<boolean>(false);

  // UI status
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Load config data
  const loadConfig = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const data = await getCapacityThreshold(scopeType, orgUnitId);
      setCurrentConfig(data);
      setOverloadThreshold(Number(data.overloadThreshold));
      setIdleThreshold(Number(data.idleThreshold));
    } catch (err) {
      setErrorMessage(
        err instanceof Error ? err.message : "Không thể tải cấu hình ngưỡng hiện tại."
      );
    } finally {
      setIsLoading(false);
    }
  }, [scopeType, orgUnitId]);

  // Load history data
  const loadHistory = useCallback(async () => {
    setIsLoadingHistory(true);
    try {
      const history = await getCapacityThresholdHistory(scopeType, orgUnitId);
      setHistoryList(history);
    } catch (err) {
      console.warn("Không thể tải lịch sử cấu hình ngưỡng:", err);
    } finally {
      setIsLoadingHistory(false);
    }
  }, [scopeType, orgUnitId]);

  useEffect(() => {
    if (open) {
      loadConfig();
      setSuccessMessage(null);
      setActiveTab("CONFIG");
    }
  }, [open, loadConfig]);

  useEffect(() => {
    if (open && activeTab === "HISTORY") {
      loadHistory();
    }
  }, [open, activeTab, loadHistory]);

  // Keyboard Escape listener
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && open && !isSaving) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [open, isSaving, onClose]);

  // Client-side Validation (BR-02: idleThreshold < overloadThreshold)
  const validationError = (() => {
    if (isNaN(overloadThreshold) || isNaN(idleThreshold)) {
      return "Vui lòng nhập đầy đủ giá trị ngưỡng hợp lệ.";
    }
    if (idleThreshold < 0) {
      return "Ngưỡng nhàn rỗi không được nhỏ hơn 0%.";
    }
    if (idleThreshold > 100) {
      return "Ngưỡng nhàn rỗi không được vượt quá 100%.";
    }
    if (overloadThreshold <= 0) {
      return "Ngưỡng quá tải phải lớn hơn 0%.";
    }
    if (overloadThreshold > 200) {
      return "Ngưỡng quá tải không được vượt quá 200%.";
    }
    if (idleThreshold >= overloadThreshold) {
      return `Ngưỡng nhàn rỗi (${idleThreshold}%) phải nhỏ hơn ngưỡng quá tải (${overloadThreshold}%).`;
    }
    return null;
  })();

  const handleResetDefaults = () => {
    setOverloadThreshold(100);
    setIdleThreshold(50);
    setErrorMessage(null);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (validationError) return;

    setIsSaving(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      const updated = await configureCapacityThreshold({
        scopeType,
        orgUnitId: orgUnitId ?? null,
        overloadThreshold,
        idleThreshold,
        version: currentConfig?.version ?? null,
      });

      setCurrentConfig(updated);
      setSuccessMessage("Cập nhật cấu hình ngưỡng cảnh báo thành công!");
      if (onSuccess) {
        onSuccess();
      }
      setTimeout(() => {
        setSuccessMessage(null);
      }, 3000);
    } catch (err) {
      if (err instanceof Error && (err.message.includes("409") || err.message.includes("thao tác khác") || err.message.includes("phiên bản") || err.message.includes("xung đột"))) {
        setErrorMessage(
          "Cấu hình đã được cập nhật bởi một người dùng khác cùng thời điểm (xung đột phiên bản). Vui lòng đóng cửa sổ hoặc tải lại để lấy dữ liệu mới nhất."
        );
      } else {
        setErrorMessage(
          err instanceof Error ? err.message : "Đã xảy ra lỗi khi lưu cấu hình."
        );
      }
    } finally {
      setIsSaving(false);
    }
  };

  const formatHistoryValue = (jsonStr: string | null) => {
    if (!jsonStr) return "—";
    try {
      const parsed = JSON.parse(jsonStr);
      const overload = parsed.overloadThreshold ?? "—";
      const idle = parsed.idleThreshold ?? "—";
      return `Quá tải: ${overload}%, Nhàn rỗi: ${idle}%`;
    } catch {
      return jsonStr;
    }
  };

  const formatDateTime = (dateStr: string | null) => {
    if (!dateStr) return "—";
    try {
      const date = new Date(dateStr);
      return date.toLocaleString("vi-VN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return dateStr;
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/50 p-4 backdrop-blur-sm transition-opacity animate-in fade-in">
      <div
        className="relative flex max-h-[90vh] w-full max-w-2xl flex-col rounded-2xl bg-white shadow-2xl ring-1 ring-black/10 transition-all dark:bg-slate-900"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-5 dark:border-slate-800">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950/50 dark:text-indigo-400">
              <Sliders className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                Cấu hình ngưỡng cảnh báo năng lực (QTN-23)
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Thiết lập ngưỡng % tải phục vụ cảnh báo quá tải &amp; nhàn rỗi
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSaving}
            className="rounded-lg p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800 dark:hover:text-slate-200"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Tab Switcher */}
        <div className="flex border-b border-slate-100 px-6 dark:border-slate-800">
          <button
            type="button"
            onClick={() => setActiveTab("CONFIG")}
            className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-semibold transition ${
              activeTab === "CONFIG"
                ? "border-indigo-600 text-indigo-600 dark:border-indigo-400 dark:text-indigo-400"
                : "border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
            }`}
          >
            <Sliders className="h-4 w-4" />
            Cấu hình ngưỡng
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("HISTORY")}
            className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-semibold transition ${
              activeTab === "HISTORY"
                ? "border-indigo-600 text-indigo-600 dark:border-indigo-400 dark:text-indigo-400"
                : "border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
            }`}
          >
            <History className="h-4 w-4" />
            Lịch sử thay đổi
          </button>
        </div>

        {/* Modal Body */}
        <div className="overflow-y-auto p-6">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-indigo-600 dark:text-indigo-400" />
              <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">
                Đang tải cấu hình ngưỡng...
              </p>
            </div>
          ) : activeTab === "CONFIG" ? (
            <form onSubmit={handleSave} className="space-y-6">
              {/* Alert Feedback */}
              {errorMessage && (
                <div className="flex items-center gap-2.5 rounded-xl border border-rose-200 bg-rose-50 p-3.5 text-xs text-rose-700 dark:border-rose-900/50 dark:bg-rose-950/40 dark:text-rose-300">
                  <AlertCircle className="h-4 w-4 shrink-0" />
                  <span>{errorMessage}</span>
                </div>
              )}

              {successMessage && (
                <div className="flex items-center gap-2.5 rounded-xl border border-emerald-200 bg-emerald-50 p-3.5 text-xs text-emerald-700 dark:border-emerald-900/50 dark:bg-emerald-950/40 dark:text-emerald-300">
                  <CheckCircle2 className="h-4 w-4 shrink-0" />
                  <span>{successMessage}</span>
                </div>
              )}

              {/* Status banner */}
              <div className="flex items-center justify-between rounded-xl bg-slate-50 p-3.5 dark:bg-slate-800/60">
                <div className="flex items-center gap-2 text-xs text-slate-600 dark:text-slate-300">
                  <span className="font-semibold">Trạng thái cấu hình:</span>
                  {currentConfig?.isDefault ? (
                    <span className="inline-flex items-center gap-1 rounded-md bg-blue-50 px-2 py-0.5 text-[11px] font-medium text-blue-700 ring-1 ring-inset ring-blue-700/10 dark:bg-blue-950/50 dark:text-blue-300">
                      Mặc định hệ thống (100% / 50%)
                    </span>
                  ) : currentConfig?.isInherited ? (
                    <span className="inline-flex items-center gap-1 rounded-md bg-amber-50 px-2 py-0.5 text-[11px] font-medium text-amber-700 ring-1 ring-inset ring-amber-700/10 dark:bg-amber-950/50 dark:text-amber-300">
                      Kế thừa từ cấu hình Công ty
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 rounded-md bg-emerald-50 px-2 py-0.5 text-[11px] font-medium text-emerald-700 ring-1 ring-inset ring-emerald-700/10 dark:bg-emerald-950/50 dark:text-emerald-300">
                      Đã tùy chỉnh
                    </span>
                  )}
                </div>
                {currentConfig?.updatedAt && !currentConfig.isDefault && (
                  <div className="flex items-center gap-1 text-[11px] text-slate-400">
                    <Clock className="h-3 w-3" />
                    <span>
                      {formatDateTime(currentConfig.updatedAt)} bởi{" "}
                      {currentConfig.updaterName || "Hệ thống"}
                    </span>
                  </div>
                )}
              </div>

              {/* Input: Overload Threshold */}
              <div className="space-y-2 rounded-xl border border-slate-200 p-4 dark:border-slate-800">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="h-3 w-3 rounded-full bg-rose-500" />
                    <label
                      htmlFor="overload-input"
                      className="text-sm font-semibold text-slate-900 dark:text-white"
                    >
                      Ngưỡng cảnh báo Quá tải (%)
                    </label>
                  </div>
                  <div className="flex items-center gap-1">
                    <input
                      id="overload-input"
                      type="number"
                      min={1}
                      max={200}
                      step={1}
                      value={overloadThreshold}
                      onChange={(e) => setOverloadThreshold(Number(e.target.value))}
                      className="w-20 rounded-lg border border-slate-300 px-2.5 py-1 text-right text-sm font-bold text-slate-900 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                    />
                    <span className="text-xs font-semibold text-slate-500">%</span>
                  </div>
                </div>
                <input
                  type="range"
                  min={50}
                  max={200}
                  step={1}
                  value={overloadThreshold}
                  onChange={(e) => setOverloadThreshold(Number(e.target.value))}
                  className="h-2 w-full cursor-pointer appearance-none rounded-lg bg-rose-100 accent-rose-600 dark:bg-rose-950"
                />
                <p className="text-[11px] text-slate-500 dark:text-slate-400">
                  Nhân sự có tổng tỷ lệ phân bổ &ge; <strong>{overloadThreshold}%</strong> sẽ
                  được tô màu đỏ và xếp loại <em>Quá tải</em>.
                </p>
              </div>

              {/* Input: Idle Threshold */}
              <div className="space-y-2 rounded-xl border border-slate-200 p-4 dark:border-slate-800">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="h-3 w-3 rounded-full bg-amber-400" />
                    <label
                      htmlFor="idle-input"
                      className="text-sm font-semibold text-slate-900 dark:text-white"
                    >
                      Ngưỡng cảnh báo Nhàn rỗi (%)
                    </label>
                  </div>
                  <div className="flex items-center gap-1">
                    <input
                      id="idle-input"
                      type="number"
                      min={0}
                      max={100}
                      step={1}
                      value={idleThreshold}
                      onChange={(e) => setIdleThreshold(Number(e.target.value))}
                      className="w-20 rounded-lg border border-slate-300 px-2.5 py-1 text-right text-sm font-bold text-slate-900 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                    />
                    <span className="text-xs font-semibold text-slate-500">%</span>
                  </div>
                </div>
                <input
                  type="range"
                  min={0}
                  max={100}
                  step={1}
                  value={idleThreshold}
                  onChange={(e) => setIdleThreshold(Number(e.target.value))}
                  className="h-2 w-full cursor-pointer appearance-none rounded-lg bg-amber-100 accent-amber-500 dark:bg-amber-950"
                />
                <p className="text-[11px] text-slate-500 dark:text-slate-400">
                  Nhân sự có tổng tỷ lệ phân bổ &lt; <strong>{idleThreshold}%</strong> sẽ được
                  tô màu vàng và xếp loại <em>Nhàn rỗi</em>.
                </p>
              </div>

              {/* Validation Alert */}
              {validationError && (
                <div className="flex items-center gap-2.5 rounded-xl border border-amber-200 bg-amber-50 p-3.5 text-xs font-medium text-amber-800 dark:border-amber-900/50 dark:bg-amber-950/40 dark:text-amber-300">
                  <AlertTriangle className="h-4 w-4 shrink-0 text-amber-600" />
                  <span>{validationError}</span>
                </div>
              )}

              {/* Live Preview Bar */}
              <div className="space-y-2 rounded-xl bg-slate-50 p-4 dark:bg-slate-800/40">
                <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                  Trực quan hóa dải phân loại năng lực (Preview):
                </span>
                <div className="grid grid-cols-3 gap-2 text-center text-xs font-medium">
                  <div className="flex flex-col items-center justify-center rounded-lg border border-amber-200 bg-amber-50 py-2 text-amber-800 dark:border-amber-800 dark:bg-amber-950/60 dark:text-amber-300">
                    <span className="font-bold">Nhàn rỗi</span>
                    <span className="text-[11px] opacity-80">&lt; {idleThreshold}%</span>
                  </div>
                  <div className="flex flex-col items-center justify-center rounded-lg border border-emerald-200 bg-emerald-50 py-2 text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
                    <span className="font-bold">Tối ưu</span>
                    <span className="text-[11px] opacity-80">
                      {idleThreshold}% - {overloadThreshold}%
                    </span>
                  </div>
                  <div className="flex flex-col items-center justify-center rounded-lg border border-rose-200 bg-rose-50 py-2 text-rose-800 dark:border-rose-800 dark:bg-rose-950/60 dark:text-rose-300">
                    <span className="font-bold">Quá tải</span>
                    <span className="text-[11px] opacity-80">&ge; {overloadThreshold}%</span>
                  </div>
                </div>
              </div>

              {/* Bottom Actions */}
              <div className="flex items-center justify-between border-t border-slate-100 pt-4 dark:border-slate-800">
                <button
                  type="button"
                  onClick={handleResetDefaults}
                  disabled={isSaving}
                  className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-semibold text-slate-600 transition hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-200"
                >
                  <RotateCcw className="h-3.5 w-3.5" />
                  Khôi phục mặc định (100% / 50%)
                </button>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={onClose}
                    disabled={isSaving}
                    className="rounded-lg border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 transition hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                  >
                    Hủy
                  </button>
                  <button
                    type="submit"
                    disabled={isSaving || Boolean(validationError)}
                    className="flex items-center gap-1.5 rounded-lg bg-indigo-600 px-5 py-2 text-xs font-semibold text-white shadow transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-indigo-500 dark:hover:bg-indigo-600"
                  >
                    {isSaving ? (
                      <>
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                        Đang lưu...
                      </>
                    ) : (
                      <>
                        <CheckCircle2 className="h-3.5 w-3.5" />
                        Lưu cấu hình
                      </>
                    )}
                  </button>
                </div>
              </div>
            </form>
          ) : (
            /* History Tab */
            <div className="space-y-4">
              {isLoadingHistory ? (
                <div className="flex flex-col items-center justify-center py-12">
                  <Loader2 className="h-8 w-8 animate-spin text-indigo-600 dark:text-indigo-400" />
                  <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">
                    Đang tải lịch sử thay đổi...
                  </p>
                </div>
              ) : historyList.length === 0 ? (
                <div className="rounded-xl border border-dashed border-slate-200 py-12 text-center text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
                  <History className="mx-auto mb-2 h-8 w-8 text-slate-400" />
                  Chưa có lịch sử thay đổi cấu hình nào.
                </div>
              ) : (
                <div className="overflow-hidden rounded-xl border border-slate-200 dark:border-slate-800">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                      <tr>
                        <th className="px-4 py-3 font-semibold">Thời gian</th>
                        <th className="px-4 py-3 font-semibold">Người thực hiện</th>
                        <th className="px-4 py-3 font-semibold">Giá trị mới</th>
                        <th className="px-4 py-3 font-semibold">Giá trị trước đó</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                      {historyList.map((log) => (
                        <tr
                          key={log.id}
                          className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30"
                        >
                          <td className="whitespace-nowrap px-4 py-3 text-slate-700 dark:text-slate-300">
                            {formatDateTime(log.createdAt)}
                          </td>
                          <td className="whitespace-nowrap px-4 py-3 text-slate-900 font-medium dark:text-white">
                            {log.userName || `User #${log.userId || "—"}`}
                          </td>
                          <td className="px-4 py-3 text-indigo-600 font-medium dark:text-indigo-400">
                            {formatHistoryValue(log.newValue)}
                          </td>
                          <td className="px-4 py-3 text-slate-400">
                            {formatHistoryValue(log.oldValue)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
