import React, { useState, useEffect, useMemo, useCallback } from "react";
import {
  CalendarDays,
  CalendarCheck2,
  Plus,
  Edit2,
  Trash2,
  AlertCircle,
  CheckCircle2,
  Clock,
  Loader2,
  X,
  RotateCcw,
} from "lucide-react";
import { useAuthUser } from "@/lib/auth-session";
import {
  getWorkingCalendar,
  updateWorkingCalendar,
  getHolidays,
  createHoliday,
  updateHoliday,
  deleteHoliday,
  type DayOfWeek,
  type WorkingCalendarDay,
  type Holiday,
} from "@/lib/api/working-calendar";
import { ApiError } from "@/lib/api-client";

const DAY_LABELS: Record<DayOfWeek, { label: string; short: string }> = {
  MONDAY: { label: "Thứ Hai", short: "T2" },
  TUESDAY: { label: "Thứ Ba", short: "T3" },
  WEDNESDAY: { label: "Thứ Tư", short: "T4" },
  THURSDAY: { label: "Thứ Năm", short: "T5" },
  FRIDAY: { label: "Thứ Sáu", short: "T6" },
  SATURDAY: { label: "Thứ Bảy", short: "T7" },
  SUNDAY: { label: "Chủ Nhật", short: "CN" },
};

const DAY_ORDER: DayOfWeek[] = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

export default function WorkingCalendarConfigView() {
  const currentUser = useAuthUser();
  const roleCode = currentUser?.roleCode?.toUpperCase().replace(/_/g, "-") || "";
  const isHR = roleCode === "VT-05";
  const isAdmin = roleCode === "VT-06";
  const canManage = isHR || isAdmin;

  // Working Calendar Days State
  const [workingDays, setWorkingDays] = useState<WorkingCalendarDay[]>([]);
  const [initialDays, setInitialDays] = useState<WorkingCalendarDay[]>([]);
  const [isLoadingCalendar, setIsLoadingCalendar] = useState<boolean>(true);
  const [isSavingCalendar, setIsSavingCalendar] = useState<boolean>(false);

  // Holidays State
  const currentYear = new Date().getFullYear();
  const [selectedYear, setSelectedYear] = useState<number>(currentYear);
  const [holidays, setHolidays] = useState<Holiday[]>([]);
  const [isLoadingHolidays, setIsLoadingHolidays] = useState<boolean>(true);

  // Notification Banner
  const [notification, setNotification] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  const showNotification = (type: "success" | "error", message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 5000);
  };

  // Holiday Modal State
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [editingHoliday, setEditingHoliday] = useState<Holiday | null>(null);
  const [holidayDate, setHolidayDate] = useState<string>("");
  const [holidayName, setHolidayName] = useState<string>("");
  const [workingHoursDeducted, setWorkingHoursDeducted] = useState<number>(8);
  const [modalError, setModalError] = useState<string | null>(null);
  const [isSubmittingHoliday, setIsSubmittingHoliday] = useState<boolean>(false);

  // Delete Confirmation Modal State
  const [deletingHoliday, setDeletingHoliday] = useState<Holiday | null>(null);
  const [isDeleting, setIsDeleting] = useState<boolean>(false);

  // 1. Fetch Working Calendar
  const fetchCalendar = useCallback(async () => {
    setIsLoadingCalendar(true);
    try {
      const res = await getWorkingCalendar();
      if (res && res.days) {
        // Sort according to standard order
        const sorted = [...res.days].sort(
          (a, b) => DAY_ORDER.indexOf(a.dayOfWeek) - DAY_ORDER.indexOf(b.dayOfWeek)
        );
        setWorkingDays(sorted);
        setInitialDays(sorted);
      }
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : "Không thể tải cấu hình lịch làm việc";
      showNotification("error", msg);
    } finally {
      setIsLoadingCalendar(false);
    }
  }, []);

  // 2. Fetch Holidays for selected year
  const fetchHolidaysData = useCallback(async (year: number) => {
    setIsLoadingHolidays(true);
    try {
      const res = await getHolidays(year);
      setHolidays(res || []);
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : "Không thể tải danh sách ngày lễ";
      showNotification("error", msg);
    } finally {
      setIsLoadingHolidays(false);
    }
  }, []);

  useEffect(() => {
    fetchCalendar();
  }, [fetchCalendar]);

  useEffect(() => {
    fetchHolidaysData(selectedYear);
  }, [selectedYear, fetchHolidaysData]);

  // Handle Working Day Toggle
  const handleToggleDay = (dayOfWeek: DayOfWeek) => {
    if (!canManage) return;
    setWorkingDays((prev) =>
      prev.map((d) => (d.dayOfWeek === dayOfWeek ? { ...d, isWorkingDay: !d.isWorkingDay } : d))
    );
  };

  // Check if calendar has changes
  const hasCalendarChanges = useMemo(() => {
    if (workingDays.length === 0 || initialDays.length === 0) return false;
    return workingDays.some((d) => {
      const initial = initialDays.find((init) => init.dayOfWeek === d.dayOfWeek);
      return initial ? initial.isWorkingDay !== d.isWorkingDay : true;
    });
  }, [workingDays, initialDays]);

  const workingDaysCount = useMemo(
    () => workingDays.filter((d) => d.isWorkingDay).length,
    [workingDays]
  );

  // Save Working Calendar
  const handleSaveCalendar = async () => {
    if (!canManage) return;
    if (workingDaysCount === 0) {
      showNotification("error", "Lịch làm việc phải có ít nhất 1 ngày làm việc trong tuần!");
      return;
    }

    setIsSavingCalendar(true);
    try {
      const res = await updateWorkingCalendar(workingDays);
      if (res && res.days) {
        const sorted = [...res.days].sort(
          (a, b) => DAY_ORDER.indexOf(a.dayOfWeek) - DAY_ORDER.indexOf(b.dayOfWeek)
        );
        setWorkingDays(sorted);
        setInitialDays(sorted);
      }
      showNotification("success", "Cập nhật cấu hình ngày làm việc thành công!");
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : "Không thể cập nhật lịch làm việc";
      showNotification("error", msg);
    } finally {
      setIsSavingCalendar(false);
    }
  };

  // Reset Calendar to initial
  const handleResetCalendar = () => {
    setWorkingDays(initialDays);
  };

  // Open Modal to Add Holiday
  const handleOpenAddHoliday = () => {
    setEditingHoliday(null);
    // default date in current selected year
    const today = new Date();
    const defaultMonth = String(today.getMonth() + 1).padStart(2, "0");
    const defaultDay = String(today.getDate()).padStart(2, "0");
    setHolidayDate(`${selectedYear}-${defaultMonth}-${defaultDay}`);
    setHolidayName("");
    setWorkingHoursDeducted(8);
    setModalError(null);
    setIsModalOpen(true);
  };

  // Open Modal to Edit Holiday
  const handleOpenEditHoliday = (holiday: Holiday) => {
    setEditingHoliday(holiday);
    setHolidayDate(holiday.holidayDate);
    setHolidayName(holiday.name);
    setWorkingHoursDeducted(holiday.workingHoursDeducted);
    setModalError(null);
    setIsModalOpen(true);
  };

  // Submit Holiday Form
  const handleSubmitHoliday = async (e: React.FormEvent) => {
    e.preventDefault();
    setModalError(null);

    if (!holidayDate) {
      setModalError("Vui lòng chọn ngày nghỉ lễ");
      return;
    }
    if (!holidayName.trim()) {
      setModalError("Vui lòng nhập tên ngày nghỉ lễ");
      return;
    }
    if (workingHoursDeducted < 1 || workingHoursDeducted > 24) {
      setModalError("Số giờ khấu trừ phải từ 1 đến 24 giờ");
      return;
    }

    setIsSubmittingHoliday(true);
    try {
      if (editingHoliday) {
        await updateHoliday(editingHoliday.id, {
          holidayDate,
          name: holidayName.trim(),
          workingHoursDeducted,
        });
        showNotification("success", `Cập nhật ngày lễ "${holidayName.trim()}" thành công!`);
      } else {
        await createHoliday({
          holidayDate,
          name: holidayName.trim(),
          workingHoursDeducted,
        });
        showNotification("success", `Thêm ngày lễ "${holidayName.trim()}" thành công!`);
      }
      setIsModalOpen(false);
      // Refresh list
      fetchHolidaysData(selectedYear);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setModalError(`Ngày nghỉ lễ ${holidayDate} đã tồn tại trong hệ thống! Vui lòng chọn ngày khác.`);
        } else {
          setModalError(err.message || "Đã xảy ra lỗi khi lưu ngày nghỉ lễ.");
        }
      } else {
        setModalError("Đã xảy ra lỗi không xác định khi lưu ngày nghỉ lễ.");
      }
    } finally {
      setIsSubmittingHoliday(false);
    }
  };

  // Confirm Delete Holiday
  const handleConfirmDelete = async () => {
    if (!deletingHoliday) return;
    setIsDeleting(true);
    try {
      await deleteHoliday(deletingHoliday.id);
      showNotification("success", `Đã xóa ngày lễ "${deletingHoliday.name}" thành công!`);
      setDeletingHoliday(null);
      fetchHolidaysData(selectedYear);
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : "Không thể xóa ngày nghỉ lễ";
      showNotification("error", msg);
    } finally {
      setIsDeleting(false);
    }
  };

  // Helper to get day name for date string
  const getDayNameFromDateStr = (dateStr: string) => {
    try {
      const [y, m, d] = dateStr.split("-").map(Number);
      const date = new Date(y, m - 1, d);
      const dayIndex = date.getDay(); // 0 is Sunday, 1 is Monday...
      const map: Record<number, string> = {
        0: "Chủ Nhật",
        1: "Thứ Hai",
        2: "Thứ Ba",
        3: "Thứ Tư",
        4: "Thứ Năm",
        5: "Thứ Sáu",
        6: "Thứ Bảy",
      };
      return map[dayIndex] || "";
    } catch {
      return "";
    }
  };

  // Format date to DD/MM/YYYY
  const formatDateDisplay = (dateStr: string) => {
    try {
      const [y, m, d] = dateStr.split("-");
      return `${d}/${m}/${y}`;
    } catch {
      return dateStr;
    }
  };

  // Summary statistics for holidays
  const totalHolidaysCount = holidays.length;
  const totalDeductedHours = useMemo(
    () => holidays.reduce((sum, h) => sum + (h.workingHoursDeducted || 0), 0),
    [holidays]
  );

  return (
    <div className="space-y-6 pb-12">
      {/* Header View */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            Khai báo lịch làm việc & Ngày lễ
          </h1>
        </div>
      </div>

      {/* Notification Banner */}
      {notification && (
        <div
          className={`flex items-center justify-between p-4 rounded-xl border text-sm font-medium animate-in fade-in duration-200 ${
            notification.type === "success"
              ? "bg-emerald-50 border-emerald-200 text-emerald-800"
              : "bg-rose-50 border-rose-200 text-rose-800"
          }`}
        >
          <div className="flex items-center gap-2.5">
            {notification.type === "success" ? (
              <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0" />
            ) : (
              <AlertCircle className="h-5 w-5 text-rose-600 shrink-0" />
            )}
            <span>{notification.message}</span>
          </div>
          <button
            onClick={() => setNotification(null)}
            className="text-slate-400 hover:text-slate-600 p-1"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {/* ========================================================= */}
      {/* SECTION 1: CẤU HÌNH NGÀY LÀM VIỆC TRONG TUẦN              */}
      {/* ========================================================= */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-5 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
              <CalendarDays className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900">
                Lịch làm việc hàng tuần của công ty
              </h2>
              <p className="text-xs text-slate-500">
                Xác định các ngày làm việc chính thức. Các ngày nghỉ cuối tuần sẽ không bị khấu trừ thêm khi có lịch nghỉ lễ.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold px-2.5 py-1 rounded-lg bg-slate-100 text-slate-700">
              {workingDaysCount} / 7 ngày làm việc
            </span>
          </div>
        </div>

        {isLoadingCalendar ? (
          <div className="flex items-center justify-center py-10 text-slate-400 text-sm">
            <Loader2 className="h-5 w-5 animate-spin mr-2 text-indigo-600" />
            Đang tải cấu hình lịch làm việc...
          </div>
        ) : (
          <div className="pt-6">
            <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-7 gap-3">
              {workingDays.map((day) => {
                const info = DAY_LABELS[day.dayOfWeek] || { label: day.dayOfWeek, short: day.dayOfWeek };
                const isWork = day.isWorkingDay;

                return (
                  <div
                    key={day.dayOfWeek}
                    onClick={() => canManage && handleToggleDay(day.dayOfWeek)}
                    className={`relative flex flex-col items-center p-4 rounded-xl border transition-all select-none ${
                      isWork
                        ? "bg-indigo-50/50 border-indigo-200 text-slate-900 shadow-xs"
                        : "bg-slate-50/70 border-slate-200 text-slate-500"
                    } ${canManage ? "cursor-pointer hover:border-indigo-400" : "cursor-default"}`}
                  >
                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">
                      {info.short}
                    </span>
                    <span className="text-sm font-semibold mb-3">{info.label}</span>

                    <span
                      className={`text-[11px] font-semibold px-2 py-0.5 rounded-full ${
                        isWork
                          ? "bg-indigo-600 text-white"
                          : "bg-slate-200 text-slate-600"
                      }`}
                    >
                      {isWork ? "Ngày làm" : "Nghỉ"}
                    </span>

                    {canManage && (
                      <div className="mt-3">
                        <input
                          type="checkbox"
                          checked={isWork}
                          onChange={() => handleToggleDay(day.dayOfWeek)}
                          className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 cursor-pointer"
                        />
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Actions for Calendar */}
            {canManage && (
              <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between">
                <span className="text-xs text-slate-500 italic">
                  * Yêu cầu bắt buộc: Phải chọn ít nhất 1 ngày làm việc trong tuần.
                </span>
                <div className="flex items-center gap-3">
                  {hasCalendarChanges && (
                    <button
                      type="button"
                      onClick={handleResetCalendar}
                      disabled={isSavingCalendar}
                      className="flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold text-slate-600 bg-white border border-slate-200 rounded-xl hover:bg-slate-50 transition shadow-xs"
                    >
                      <RotateCcw className="h-3.5 w-3.5" />
                      Hoàn tác
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={handleSaveCalendar}
                    disabled={isSavingCalendar || !hasCalendarChanges}
                    className={`flex items-center gap-1.5 px-4 py-2 text-xs font-semibold rounded-xl shadow-xs transition ${
                      hasCalendarChanges
                        ? "bg-indigo-600 text-white hover:bg-indigo-700"
                        : "bg-slate-100 text-slate-400 cursor-not-allowed"
                    }`}
                  >
                    {isSavingCalendar && <Loader2 className="h-3.5 w-3.5 animate-spin mr-1" />}
                    Lưu cấu hình ngày làm việc
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* ========================================================= */}
      {/* SECTION 2: DANH MỤC NGÀY NGHỈ LỄ THƯỜNG NIÊN              */}
      {/* ========================================================= */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-5 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-100">
              <CalendarCheck2 className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900">
                Danh sách ngày nghỉ lễ theo năm
              </h2>
              <p className="text-xs text-slate-500">
                Khai báo danh mục ngày nghỉ lễ hàng năm. Mỗi ngày lễ khi rơi vào ngày làm việc sẽ tự động trừ giờ khả dụng tuần.
              </p>
            </div>
          </div>

          {/* Controls: Year selector + Add Button */}
          <div className="flex items-center gap-3 flex-wrap">
            <div className="flex items-center gap-2 bg-slate-50 px-3 py-1.5 rounded-xl border border-slate-200">
              <span className="text-xs font-medium text-slate-500">Năm:</span>
              <select
                value={selectedYear}
                onChange={(e) => setSelectedYear(Number(e.target.value))}
                className="bg-transparent text-xs font-bold text-slate-800 border-none outline-hidden cursor-pointer"
              >
                {[currentYear - 1, currentYear, currentYear + 1, currentYear + 2].map((y) => (
                  <option key={y} value={y}>
                    Năm {y}
                  </option>
                ))}
              </select>
            </div>

            {canManage && (
              <button
                type="button"
                onClick={handleOpenAddHoliday}
                className="flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-indigo-600 rounded-xl hover:bg-indigo-700 transition shadow-xs"
              >
                <Plus className="h-4 w-4" />
                Thêm ngày lễ mới
              </button>
            )}
          </div>
        </div>

        {/* Stats summary bar */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 my-4">
          <div className="flex items-center gap-3 p-3.5 rounded-xl bg-slate-50 border border-slate-100">
            <div className="p-2 rounded-lg bg-white border border-slate-200 text-slate-600">
              <CalendarCheck2 className="h-4 w-4" />
            </div>
            <div>
              <span className="text-[11px] font-medium text-slate-500 block">Tổng số ngày lễ trong năm</span>
              <span className="text-sm font-bold text-slate-900">{totalHolidaysCount} ngày lễ</span>
            </div>
          </div>

          <div className="flex items-center gap-3 p-3.5 rounded-xl bg-slate-50 border border-slate-100">
            <div className="p-2 rounded-lg bg-white border border-slate-200 text-indigo-600">
              <Clock className="h-4 w-4" />
            </div>
            <div>
              <span className="text-[11px] font-medium text-slate-500 block">Tổng giờ khấu trừ</span>
              <span className="text-sm font-bold text-indigo-600">{totalDeductedHours} giờ</span>
            </div>
          </div>
        </div>

        {/* Holidays Table */}
        {isLoadingHolidays ? (
          <div className="flex items-center justify-center py-12 text-slate-400 text-sm">
            <Loader2 className="h-5 w-5 animate-spin mr-2 text-indigo-600" />
            Đang tải danh sách ngày lễ năm {selectedYear}...
          </div>
        ) : holidays.length === 0 ? (
          <div className="text-center py-12 bg-slate-50/50 rounded-xl border border-dashed border-slate-200">
            <CalendarDays className="h-10 w-10 text-slate-300 mx-auto mb-2" />
            <p className="text-sm font-semibold text-slate-700">Chưa có ngày nghỉ lễ nào trong năm {selectedYear}</p>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              {canManage
                ? 'Nhấn nút "Thêm ngày lễ mới" phía trên để bắt đầu khai báo lịch nghỉ lễ cho công ty.'
                : "Danh mục ngày lễ cho năm này hiện chưa được thiết lập bởi bộ phận Nhân sự."}
            </p>
            {canManage && (
              <button
                type="button"
                onClick={handleOpenAddHoliday}
                className="mt-4 inline-flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-semibold text-indigo-600 bg-indigo-50 border border-indigo-200 rounded-xl hover:bg-indigo-100 transition"
              >
                <Plus className="h-3.5 w-3.5" />
                Thêm ngày lễ
              </button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto rounded-xl border border-slate-200">
            <table className="w-full text-left text-xs text-slate-700">
              <thead className="bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="py-3 px-4 w-12 text-center">STT</th>
                  <th className="py-3 px-4 w-36">Ngày nghỉ lễ</th>
                  <th className="py-3 px-4 w-28">Thứ</th>
                  <th className="py-3 px-4">Tên ngày lễ</th>
                  <th className="py-3 px-4 w-36 text-center">Giờ khấu trừ</th>
                  {canManage && <th className="py-3 px-4 w-28 text-right">Thao tác</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {holidays.map((item, idx) => (
                  <tr key={item.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-3 px-4 text-center text-slate-400 font-medium">
                      {idx + 1}
                    </td>
                    <td className="py-3 px-4 font-semibold text-slate-900">
                      <span className="font-mono text-xs bg-slate-100 px-2 py-0.5 rounded-md text-slate-800">
                        {formatDateDisplay(item.holidayDate)}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-600 font-medium">
                      {getDayNameFromDateStr(item.holidayDate)}
                    </td>
                    <td className="py-3 px-4 font-medium text-slate-900">
                      {item.name}
                    </td>
                    <td className="py-3 px-4 text-center">
                      <span className="inline-flex items-center gap-1 font-semibold px-2.5 py-0.5 rounded-full bg-rose-50 text-rose-700 border border-rose-100 text-[11px]">
                        -{item.workingHoursDeducted} giờ
                      </span>
                    </td>
                    {canManage && (
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => handleOpenEditHoliday(item)}
                            title="Sửa ngày lễ"
                            className="p-1.5 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"
                          >
                            <Edit2 className="h-3.5 w-3.5" />
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeletingHoliday(item)}
                            title="Xóa ngày lễ"
                            className="p-1.5 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* ========================================================= */}
      {/* MODAL: THÊM / SỬA NGÀY LỄ                                  */}
      {/* ========================================================= */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="w-full max-w-md bg-white rounded-2xl border border-slate-200 shadow-xl overflow-hidden animate-in zoom-in-95 duration-150">
            {/* Modal Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
              <div className="flex items-center gap-2.5">
                <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
                  <CalendarCheck2 className="h-4 w-4" />
                </div>
                <h3 className="text-sm font-bold text-slate-900">
                  {editingHoliday ? "Cập nhật ngày nghỉ lễ" : "Thêm ngày nghỉ lễ mới"}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {/* Modal Form */}
            <form onSubmit={handleSubmitHoliday}>
              <div className="p-6 space-y-4">
                {modalError && (
                  <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-start gap-2">
                    <AlertCircle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
                    <span className="font-medium leading-relaxed">{modalError}</span>
                  </div>
                )}

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                    Ngày nghỉ lễ <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="date"
                    value={holidayDate}
                    onChange={(e) => {
                      setHolidayDate(e.target.value);
                      setModalError(null);
                    }}
                    required
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition"
                  />
                  {holidayDate && (
                    <p className="text-[11px] text-slate-500 mt-1">
                      Ngày đã chọn: <strong>{getDayNameFromDateStr(holidayDate)}</strong>, {formatDateDisplay(holidayDate)}
                    </p>
                  )}
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                    Tên ngày lễ <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    placeholder="VD: Lễ Quốc Khánh, Tết Dương Lịch..."
                    value={holidayName}
                    onChange={(e) => {
                      setHolidayName(e.target.value);
                      setModalError(null);
                    }}
                    required
                    maxLength={100}
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                    Số giờ làm việc khấu trừ <span className="text-rose-500">*</span>
                  </label>
                  <div className="flex items-center gap-2">
                    <input
                      type="number"
                      min={1}
                      max={24}
                      value={workingHoursDeducted}
                      onChange={(e) => setWorkingHoursDeducted(Number(e.target.value))}
                      required
                      className="w-24 px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition"
                    />
                    <span className="text-xs text-slate-500 font-medium">giờ (mặc định: 8 giờ)</span>
                  </div>
                </div>
              </div>

              {/* Modal Footer */}
              <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  disabled={isSubmittingHoliday}
                  className="px-4 py-2 text-xs font-semibold text-slate-700 bg-white border border-slate-200 rounded-xl hover:bg-slate-50 transition"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingHoliday}
                  className="flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-indigo-600 rounded-xl hover:bg-indigo-700 transition shadow-xs"
                >
                  {isSubmittingHoliday && <Loader2 className="h-3.5 w-3.5 animate-spin mr-1" />}
                  {editingHoliday ? "Lưu thay đổi" : "Thêm ngày lễ"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* MODAL: XÁC NHẬN XÓA NGÀY LỄ                                */}
      {/* ========================================================= */}
      {deletingHoliday && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="w-full max-w-md bg-white rounded-2xl border border-slate-200 shadow-xl overflow-hidden animate-in zoom-in-95 duration-150">
            <div className="p-6">
              <div className="flex items-center gap-3 mb-4">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-rose-50 text-rose-600 border border-rose-100">
                  <Trash2 className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Xác nhận xóa ngày nghỉ lễ</h3>
                  <p className="text-xs text-slate-500">Thao tác này không thể hoàn tác</p>
                </div>
              </div>

              <p className="text-xs text-slate-600 leading-relaxed">
                Bạn có chắc chắn muốn xóa ngày lễ{" "}
                <strong className="text-slate-900">"{deletingHoliday.name}"</strong> vào ngày{" "}
                <strong className="text-slate-900">{formatDateDisplay(deletingHoliday.holidayDate)}</strong>?
              </p>
              <p className="text-[11px] text-slate-400 mt-2">
                Hệ thống sẽ cập nhật lại tính toán giờ làm việc khả dụng.
              </p>
            </div>

            <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-2.5">
              <button
                type="button"
                onClick={() => setDeletingHoliday(null)}
                disabled={isDeleting}
                className="px-4 py-2 text-xs font-semibold text-slate-700 bg-white border border-slate-200 rounded-xl hover:bg-slate-50 transition"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-rose-600 rounded-xl hover:bg-rose-700 transition shadow-xs"
              >
                {isDeleting && <Loader2 className="h-3.5 w-3.5 animate-spin mr-1" />}
                Xóa ngày lễ
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
