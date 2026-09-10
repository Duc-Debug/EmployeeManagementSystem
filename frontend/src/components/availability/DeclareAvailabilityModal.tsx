import { useState, useMemo, type FormEvent } from "react";
import { X, CalendarClock, Clock, User, AlertCircle, Loader2 } from "lucide-react";
import type { EmployeeProfile } from "@/lib/api/employees";
import { declareWeeklyAvailability, type WeeklyAvailabilityResult } from "@/lib/api/availability";
import { getIsoWeekDateRange } from "./availability.types";

interface DeclareAvailabilityModalProps {
  open: boolean;
  employees: EmployeeProfile[];
  initialEmployeeId?: number;
  initialYear?: number;
  initialWeekNumber?: number;
  initialStandardHours?: number;
  onClose: () => void;
  onSuccess: (result: WeeklyAvailabilityResult) => void;
}

export default function DeclareAvailabilityModal({
  open,
  employees,
  initialEmployeeId,
  initialYear = new Date().getFullYear(),
  initialWeekNumber = 1,
  initialStandardHours = 40,
  onClose,
  onSuccess,
}: DeclareAvailabilityModalProps) {
  const [employeeId, setEmployeeId] = useState<number>(() => {
    if (initialEmployeeId) return initialEmployeeId;
    return employees.length > 0 ? employees[0].id : 1;
  });
  const [year, setYear] = useState<number>(initialYear);
  const [weekNumber, setWeekNumber] = useState<number>(initialWeekNumber);
  const [standardHours, setStandardHours] = useState<number>(initialStandardHours);

  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string>("");

  // Compute date range for the selected week
  const dateRangeText = useMemo(() => {
    try {
      const { startDate, endDate } = getIsoWeekDateRange(year, weekNumber);
      const startStr = startDate.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" });
      const endStr = endDate.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
      return `(Thứ 2, ${startStr} — Chủ Nhật, ${endStr})`;
    } catch {
      return "";
    }
  }, [year, weekNumber]);

  if (!open) return null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErrorMessage("");

    if (!employeeId) {
      setErrorMessage("Vui lòng chọn nhân sự.");
      return;
    }
    if (weekNumber < 1 || weekNumber > 53) {
      setErrorMessage("Số tuần phải từ 1 đến 53.");
      return;
    }
    if (standardHours <= 0 || standardHours > 168) {
      setErrorMessage("Số giờ chuẩn mỗi tuần phải từ 1 đến 168 giờ.");
      return;
    }

    setIsLoading(true);
    try {
      const res = await declareWeeklyAvailability(employeeId, {
        year,
        weekNumber,
        standardHours,
      });
      onSuccess(res);
      onClose();
    } catch (err: any) {
      setErrorMessage(err?.message || "Khai báo giờ chuẩn tuần thất bại.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative flex max-h-[90vh] w-full max-w-lg flex-col rounded-3xl border border-slate-200 bg-white shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-2xl border border-indigo-100 bg-indigo-50 text-indigo-600 shadow-2xs">
              <CalendarClock className="size-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900">
                Khai báo giờ chuẩn tuần
              </h2>
              <p className="text-xs text-slate-500">
                NCL-02-CN-003: Định mức giờ làm việc chuẩn cho từng tuần cụ thể
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="rounded-xl p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600 disabled:opacity-50"
            title="Đóng"
          >
            <X className="size-4" />
          </button>
        </div>

        {/* Form Body */}
        <form id="declare-availability-form" onSubmit={handleSubmit} className="p-6 space-y-4 text-xs">
          {errorMessage && (
            <div className="flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3.5 py-2.5 font-semibold text-rose-700">
              <AlertCircle className="size-4 shrink-0 text-rose-600" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Chọn nhân sự */}
          <div className="space-y-1.5">
            <label className="font-semibold text-slate-700 flex items-center gap-1.5">
              <User className="size-3.5 text-slate-400" />
              <span>Nhân sự *</span>
            </label>
            <select
              value={employeeId}
              onChange={(e) => setEmployeeId(Number(e.target.value))}
              disabled={isLoading || employees.length === 0}
              className="w-full rounded-xl border border-slate-200 bg-slate-50/70 px-3.5 py-2.5 font-semibold text-slate-800 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
            >
              {employees.map((emp) => (
                <option key={emp.id} value={emp.id}>
                  {emp.fullName} ({emp.employeeCode}) • {emp.orgUnitName || "Chưa gán phòng"}
                </option>
              ))}
            </select>
          </div>

          {/* Năm & Tuần */}
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <label className="font-semibold text-slate-700">Năm *</label>
              <input
                type="number"
                min={2000}
                max={2100}
                value={year}
                onChange={(e) => setYear(Number(e.target.value))}
                disabled={isLoading}
                required
                className="w-full rounded-xl border border-slate-200 bg-slate-50/70 px-3.5 py-2 font-semibold text-slate-800 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
              />
            </div>

            <div className="space-y-1.5">
              <label className="font-semibold text-slate-700">Số tuần (1 - 53) *</label>
              <input
                type="number"
                min={1}
                max={53}
                value={weekNumber}
                onChange={(e) => setWeekNumber(Number(e.target.value))}
                disabled={isLoading}
                required
                className="w-full rounded-xl border border-slate-200 bg-slate-50/70 px-3.5 py-2 font-semibold text-slate-800 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
              />
            </div>
          </div>

          {/* Preview Range */}
          {dateRangeText && (
            <p className="text-[11px] font-medium text-indigo-600 bg-indigo-50/60 rounded-xl px-3 py-1.5 border border-indigo-100/80">
              Tuần {weekNumber} năm {year}: {dateRangeText}
            </p>
          )}

          {/* Số giờ làm việc chuẩn */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="font-semibold text-slate-700 flex items-center gap-1.5">
                <Clock className="size-3.5 text-slate-400" />
                <span>Số giờ chuẩn tuần *</span>
              </label>
              <span className="text-[11px] text-slate-400 font-medium">Toàn thời gian: 40h | Bán thời gian: 20h</span>
            </div>
            <input
              type="number"
              min={1}
              max={168}
              step={1}
              value={standardHours}
              onChange={(e) => setStandardHours(Number(e.target.value))}
              disabled={isLoading}
              required
              placeholder="40"
              className="w-full rounded-xl border border-slate-200 bg-slate-50/70 px-3.5 py-2 font-bold text-slate-800 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
            />
            <p className="text-[11px] text-slate-400 leading-relaxed">
              * Hệ thống sẽ tự động trừ các ngày nghỉ lễ và đơn nghỉ phép đã phê duyệt của tuần này theo quy tắc QTN-10 để tính giờ khả dụng thực tế.
            </p>
          </div>
        </form>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 border-t border-slate-100 bg-slate-50/50 px-6 py-3.5">
          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-600 shadow-xs transition hover:bg-slate-50 hover:text-slate-800 disabled:opacity-50"
          >
            Hủy
          </button>
          <button
            type="submit"
            form="declare-availability-form"
            disabled={isLoading}
            className="inline-flex items-center gap-2 rounded-xl border border-indigo-600 bg-indigo-600 px-5 py-2 text-xs font-semibold text-white shadow-xs transition hover:bg-indigo-700 active:scale-95 disabled:opacity-50"
          >
            {isLoading && <Loader2 className="size-3.5 animate-spin" />}
            <span>{isLoading ? "Đang lưu..." : "Xác nhận khai báo"}</span>
          </button>
        </div>
      </div>
    </div>
  );
}
