/**
 * NCL-06-CN-009: Modal tạo kỳ kế hoạch phân bổ mới
 * Nâng cấp UX: Phím tắt Escape, Backdrop click, Hỗ trợ năm 53 tuần (ISO-8601), Auto-focus.
 */
import { useState, useEffect } from "react";
import { X, CalendarPlus, AlertCircle, Loader2 } from "lucide-react";
import {
  createAllocationPeriod,
  type AllocationPeriodResult,
  type AllocationPeriodType,
} from "@/lib/api/allocation-periods";

interface CreateAllocationPeriodModalProps {
  open: boolean;
  onClose: () => void;
  onCreated: (newPeriod: AllocationPeriodResult) => void;
  initialYear?: number;
}

// Tính số tuần tối đa theo chuẩn ISO-8601 (52 hoặc 53 tuần)
export function getMaxIsoWeeks(year: number): number {
  const dec28 = new Date(Date.UTC(year, 11, 28));
  const day = dec28.getUTCDay() || 7;
  dec28.setUTCDate(dec28.getUTCDate() + 4 - day);
  const yearStart = new Date(Date.UTC(dec28.getUTCFullYear(), 0, 1));
  return Math.ceil(((dec28.getTime() - yearStart.getTime()) / 86400000 + 1) / 7);
}

export function CreateAllocationPeriodModal({
  open,
  onClose,
  onCreated,
  initialYear = new Date().getFullYear(),
}: CreateAllocationPeriodModalProps) {
  const [name, setName] = useState<string>(`Kế hoạch Quý 1/${initialYear}`);
  const [periodType, setPeriodType] = useState<AllocationPeriodType>("QUARTER");
  const [year, setYear] = useState<number>(initialYear);
  const [startWeek, setStartWeek] = useState<number>(1);
  const [endWeek, setEndWeek] = useState<number>(13);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Lắng nghe phím Escape để đóng modal
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

  // Reset form khi mở modal
  useEffect(() => {
    if (open) {
      setName(`Kế hoạch Quý 1/${initialYear}`);
      setPeriodType("QUARTER");
      setYear(initialYear);
      setStartWeek(1);
      setEndWeek(13);
      setErrorMessage(null);
    }
  }, [open, initialYear]);

  const handleApplyQuarterPreset = (q: 1 | 2 | 3 | 4) => {
    setPeriodType("QUARTER");
    if (q === 1) {
      setName(`Kế hoạch Quý 1/${year}`);
      setStartWeek(1);
      setEndWeek(13);
    } else if (q === 2) {
      setName(`Kế hoạch Quý 2/${year}`);
      setStartWeek(14);
      setEndWeek(26);
    } else if (q === 3) {
      setName(`Kế hoạch Quý 3/${year}`);
      setStartWeek(27);
      setEndWeek(39);
    } else {
      const maxWeeks = getMaxIsoWeeks(year);
      setName(`Kế hoạch Quý 4/${year}`);
      setStartWeek(40);
      setEndWeek(maxWeeks);
    }
  };

  const handleYearChange = (newYear: number) => {
    setYear(newYear);
    // Nếu tên kỳ đang ở định dạng chuẩn theo quý, đồng bộ cập nhật năm
    if (name.includes("Kế hoạch Quý")) {
      setName((prev) => prev.replace(/\/\d{4}$/, `/${newYear}`));
    }
    // Nếu đang ở Quý 4, cập nhật lại endWeek theo số tuần ISO của năm mới
    if (startWeek === 40) {
      setEndWeek(getMaxIsoWeeks(newYear));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const trimmedName = name.trim();
    if (!trimmedName) {
      setErrorMessage("Vui lòng nhập tên kỳ kế hoạch phân bổ.");
      return;
    }
    if (startWeek < 1 || startWeek > 53) {
      setErrorMessage("Tuần bắt đầu phải từ tuần 1 đến tuần 53.");
      return;
    }
    if (endWeek < 1 || endWeek > 53) {
      setErrorMessage("Tuần kết thúc phải từ tuần 1 đến tuần 53.");
      return;
    }
    if (startWeek > endWeek) {
      setErrorMessage("Tuần bắt đầu không được lớn hơn tuần kết thúc.");
      return;
    }

    try {
      setIsSubmitting(true);
      const res = await createAllocationPeriod({
        name: trimmedName,
        periodType,
        year,
        startWeek,
        endWeek,
      });
      onCreated(res);
      onClose();
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : "Không thể tạo kỳ kế hoạch phân bổ. Vui lòng thử lại.";
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="w-full max-w-lg overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 p-4">
          <div className="flex items-center gap-2.5">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-100 text-indigo-700">
              <CalendarPlus className="h-5 w-5" />
            </span>
            <div>
              <h3 className="text-sm font-bold text-slate-800">
                Tạo Kỳ Kế Hoạch Phân Bổ Mới
              </h3>
              <p className="text-[11px] text-slate-500">
                Thiết lập dải tuần áp dụng chốt kế hoạch (NCL-06-CN-009 / QTN-18)
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

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          {errorMessage && (
            <div className="flex items-start gap-2 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-800">
              <AlertCircle className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Presets Quý */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Chọn nhanh theo Quý:
            </label>
            <div className="grid grid-cols-4 gap-2">
              {[1, 2, 3, 4].map((q) => {
                const maxWeeks = getMaxIsoWeeks(year);
                return (
                  <button
                    key={q}
                    type="button"
                    onClick={() => handleApplyQuarterPreset(q as 1 | 2 | 3 | 4)}
                    className="rounded-xl border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-xs font-medium text-slate-700 hover:bg-indigo-50 hover:text-indigo-700 hover:border-indigo-300 transition text-center"
                  >
                    <div>Quý {q}</div>
                    <div className="text-[10px] text-slate-400">
                      {q === 1
                        ? "T1-T13"
                        : q === 2
                        ? "T14-T26"
                        : q === 3
                        ? "T27-T39"
                        : `T40-T${maxWeeks}`}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Tên kỳ */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Tên kỳ kế hoạch <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              autoFocus
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="VD: Kế hoạch Quý 1/2026..."
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-xs text-slate-800 placeholder-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>

          {/* Loại kỳ & Năm */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Loại kỳ
              </label>
              <select
                value={periodType}
                onChange={(e) => setPeriodType(e.target.value as AllocationPeriodType)}
                className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                <option value="QUARTER">Quý (Quarter)</option>
                <option value="MONTH">Tháng (Month)</option>
                <option value="YEAR">Năm (Year)</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Năm áp dụng
              </label>
              <input
                type="number"
                min={2020}
                max={2050}
                value={year}
                onChange={(e) => handleYearChange(Number(e.target.value))}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-xs text-slate-800 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>
          </div>

          {/* Dải tuần */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Từ tuần (1 - 53)
              </label>
              <input
                type="number"
                min={1}
                max={53}
                value={startWeek}
                onChange={(e) => setStartWeek(Number(e.target.value))}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-xs text-slate-800 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Đến tuần (1 - 53)
              </label>
              <input
                type="number"
                min={1}
                max={53}
                value={endWeek}
                onChange={(e) => setEndWeek(Number(e.target.value))}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-xs text-slate-800 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>
          </div>

          {/* Footer actions */}
          <div className="flex items-center justify-end gap-2.5 border-t border-slate-200 pt-4 mt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-xs hover:bg-indigo-700 disabled:opacity-50 transition"
            >
              {isSubmitting && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
              <span>Tạo kỳ kế hoạch</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
