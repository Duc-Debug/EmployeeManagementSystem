import { useState, useEffect, useCallback, useMemo } from "react";
import {
  Clock,
  Calendar,
  ChevronLeft,
  ChevronRight,
  Plus,
  Edit2,
  Trash2,
  CheckCircle2,
  AlertCircle,
  Briefcase,
  Loader2,
  DollarSign,
  Layers,
  Sparkles,
  Info,
  Send,
  Lock,
} from "lucide-react";
import {
  getMyWeeklyTimesheet,
  deleteWorkLog,
  submitWeeklyTimesheet,
  type WeeklyTimesheetResult,
  type WorkLogResult,
  type DailyWorkLogGroupDto,
} from "@/lib/api/work-logs";
import WorkLogModal from "./WorkLogModal";
import { useAuthUser } from "@/lib/auth-session";

export default function WorkLogView() {
  const user = useAuthUser();
  const [currentDate, setCurrentDate] = useState<string>(
    () => new Date().toISOString().split("T")[0]
  );
  const [weeklyData, setWeeklyData] = useState<WeeklyTimesheetResult | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [isSubmitModalOpen, setIsSubmitModalOpen] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [editingEntry, setEditingEntry] = useState<WorkLogResult | null>(null);
  const [targetDateForNewLog, setTargetDateForNewLog] = useState<string | undefined>(undefined);

  // Toast Notification
  const [toast, setToast] = useState<{ message: string; visible: boolean; type?: "success" | "error" }>({
    message: "",
    visible: false,
  });

  const showToast = (message: string, type: "success" | "error" = "success") => {
    setToast({ message, visible: true, type });
    setTimeout(() => setToast((prev) => ({ ...prev, visible: false })), 4000);
  };

  const loadWeeklyTimesheet = useCallback(async (dateStr: string) => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const data = await getMyWeeklyTimesheet(dateStr);
      setWeeklyData(data);
    } catch (err: unknown) {
      console.error("Lỗi khi tải bảng chấm công:", err);
      setErrorMessage(
        err instanceof Error
          ? err.message
          : "Không thể kết nối đến máy chủ để tải bảng chấm công."
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadWeeklyTimesheet(currentDate);
  }, [currentDate, loadWeeklyTimesheet]);

  // Navigate week
  const handleNavigateWeek = (daysOffset: number) => {
    const d = new Date(currentDate);
    d.setDate(d.getDate() + daysOffset);
    setCurrentDate(d.toISOString().split("T")[0]);
  };

  const handleResetToToday = () => {
    setCurrentDate(new Date().toISOString().split("T")[0]);
  };

  const handleOpenCreateModal = (date?: string) => {
    setEditingEntry(null);
    setTargetDateForNewLog(date || currentDate);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (entry: WorkLogResult) => {
    setEditingEntry(entry);
    setTargetDateForNewLog(entry.workDate);
    setIsModalOpen(true);
  };

  const handleDeleteEntry = async (entry: WorkLogResult) => {
    const confirmDelete = window.confirm(
      `Bạn có chắc chắn muốn xóa dòng ghi giờ (${Number(entry.hours || 0).toFixed(1)}h) cho công việc "${entry.taskName}" không?`
    );
    if (!confirmDelete) return;

    try {
      await deleteWorkLog(entry.id);
      showToast("Đã xóa dòng ghi giờ thành công.");
      loadWeeklyTimesheet(currentDate);
    } catch (err: unknown) {
      console.error("Lỗi xóa dòng ghi giờ:", err);
      showToast(
        err instanceof Error ? err.message : "Không thể xóa dòng ghi giờ.",
        "error"
      );
    }
  };

  const handleSubmitWeeklyTimesheet = async () => {
    setIsSubmitting(true);
    try {
      const result = await submitWeeklyTimesheet({
        dateInWeek: currentDate,
        timesheetId: weeklyData?.timesheetId ?? undefined,
      });
      setWeeklyData(result);
      setIsSubmitModalOpen(false);
      showToast("Nộp bảng chấm công theo tuần thành công! Trạng thái: Chờ duyệt.", "success");
    } catch (err: unknown) {
      console.error("Lỗi nộp bảng chấm công:", err);
      showToast(
        err instanceof Error ? err.message : "Không thể nộp bảng chấm công.",
        "error"
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const weekRangeFormatted = useMemo(() => {
    if (!weeklyData?.weekStartDate || !weeklyData?.weekEndDate) return "";
    const start = new Date(weeklyData.weekStartDate);
    const end = new Date(weeklyData.weekEndDate);
    const startFormatted = start.toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
    });
    const endFormatted = end.toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
    return `${startFormatted} — ${endFormatted}`;
  }, [weeklyData]);

  // Totals calculations with full null safety
  const totalHours = Number(weeklyData?.totalHours ?? 0);
  const allEntries = weeklyData?.allEntries ?? [];
  const totalBillableHours = allEntries
    .filter((e) => e.isBillable)
    .reduce((sum, e) => sum + Number(e.hours || 0), 0);
  const totalNonBillableHours = allEntries
    .filter((e) => !e.isBillable)
    .reduce((sum, e) => sum + Number(e.hours || 0), 0);

  const isTimesheetEditable = weeklyData?.isEditable !== false && (!weeklyData?.status || weeklyData.status === "DRAFT");
  const canSubmitTimesheet = isTimesheetEditable && allEntries.length > 0 && totalHours > 0;

  const dailyGroups: DailyWorkLogGroupDto[] = weeklyData?.dailyGroups ?? [];

  return (
    <div className="space-y-6">
      {/* Header & Controls */}
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-extrabold tracking-tight text-slate-900">
              Ghi giờ công theo công việc
            </h1>
          </div>
          <p className="mt-1 text-xs font-semibold text-slate-500 sm:text-sm">
            Ghi nhận số giờ làm việc thực tế cho từng dự án &amp; công việc WBS được phân công.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          {/* Week Navigator */}
          <div className="flex items-center rounded-xl border border-slate-200 bg-white p-1 shadow-xs">
            <button
              onClick={() => handleNavigateWeek(-7)}
              title="Tuần trước"
              type="button"
              className="rounded-lg p-1.5 text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition cursor-pointer"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button
              onClick={handleResetToToday}
              type="button"
              className="px-3 py-1 text-xs font-bold text-slate-700 hover:text-indigo-600 transition cursor-pointer"
            >
              Tuần này
            </button>
            <button
              onClick={() => handleNavigateWeek(7)}
              title="Tuần sau"
              type="button"
              className="rounded-lg p-1.5 text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition cursor-pointer"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>

          {/* Date Picker */}
          <div className="relative">
            <input
              type="date"
              value={currentDate}
              onChange={(e) => setCurrentDate(e.target.value)}
              className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 shadow-xs focus:border-indigo-500 focus:outline-none"
            />
          </div>

          {/* Add Work Log Button */}
          <button
            type="button"
            onClick={() => handleOpenCreateModal()}
            disabled={!isTimesheetEditable}
            className="flex min-h-10 items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-xs transition hover:bg-indigo-700 disabled:opacity-50 cursor-pointer"
          >
            <Plus className="h-4 w-4" />
            <span>Ghi giờ công</span>
          </button>

          {/* Submit Timesheet Button (NCL-09-CN-002) */}
          {isTimesheetEditable && (
            <button
              type="button"
              onClick={() => setIsSubmitModalOpen(true)}
              disabled={!canSubmitTimesheet || isSubmitting}
              className="flex min-h-10 items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-bold text-white shadow-xs transition hover:bg-emerald-700 disabled:opacity-50 cursor-pointer"
            >
              <Send className="h-4 w-4" />
              <span>Nộp bảng công tuần</span>
            </button>
          )}
        </div>
      </div>

      {/* Week Range & Status Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
            <Calendar className="h-5 w-5" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">
              Khung thời gian tuần
            </div>
            <div className="text-sm font-bold text-slate-900">
              {weekRangeFormatted || "Đang tải dữ liệu..."}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="text-right">
            <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
              Trạng thái bảng công
            </div>
            <div>
              {weeklyData?.status === "APPROVED" ? (
                <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-bold text-emerald-800">
                  <CheckCircle2 className="h-3.5 w-3.5" /> Đã phê duyệt
                </span>
              ) : weeklyData?.status === "SUBMITTED" ? (
                <span className="inline-flex items-center gap-1 rounded-full bg-blue-100 px-2.5 py-0.5 text-xs font-bold text-blue-800">
                  <Clock className="h-3.5 w-3.5" /> Đã nộp (Chờ quản lý duyệt)
                </span>
              ) : weeklyData?.status === "REJECTED" ? (
                <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2.5 py-0.5 text-xs font-bold text-rose-800">
                  <AlertCircle className="h-3.5 w-3.5" /> Bị từ chối (Cần sửa &amp; nộp lại)
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-bold text-slate-700">
                  <Edit2 className="h-3 w-3 text-slate-500" /> Bản nháp (Đang ghi)
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Lock Notice Banner if not editable */}
      {!isTimesheetEditable && (
        <div className="flex items-center gap-3 rounded-2xl border border-blue-200 bg-blue-50/80 p-4 text-xs font-semibold text-blue-900 shadow-2xs">
          <Lock className="h-5 w-5 shrink-0 text-blue-600" />
          <div>
            <span>Bảng chấm công tuần này đã ở trạng thái <strong>{weeklyData?.status === "APPROVED" ? "ĐÃ PHÊ DUYỆT" : "ĐÃ NỘP CHỜ DUYỆT"}</strong>. Dữ liệu đã được khóa không thể thêm, sửa hoặc xóa.</span>
          </div>
        </div>
      )}

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {/* Total Weekly Hours */}
        <div className="rounded-2xl border border-indigo-100 bg-indigo-50/60 p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-indigo-800">Tổng giờ tuần</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-indigo-100 text-indigo-700">
              <Clock className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-1">
            <span className="text-2xl font-black text-indigo-950 font-mono">
              {totalHours.toFixed(1)}
            </span>
            <span className="text-xs font-bold text-indigo-700">giờ</span>
          </div>
          <p className="mt-1 text-[11px] text-indigo-600">Định mức chuẩn: 40.0h / tuần</p>
        </div>

        {/* Billable Hours */}
        <div className="rounded-2xl border border-emerald-100 bg-emerald-50/60 p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-emerald-800">Giờ tính phí (Billable)</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
              <DollarSign className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-1">
            <span className="text-2xl font-black text-emerald-950 font-mono">
              {totalBillableHours.toFixed(1)}
            </span>
            <span className="text-xs font-bold text-emerald-700">giờ</span>
          </div>
          <p className="mt-1 text-[11px] text-emerald-600">Tính trực tiếp cho khách hàng</p>
        </div>

        {/* Non-Billable Hours */}
        <div className="rounded-2xl border border-sky-100 bg-sky-50/60 p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-sky-800">Giờ nội bộ (Non-billable)</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-sky-100 text-sky-700">
              <Briefcase className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-1">
            <span className="text-2xl font-black text-sky-950 font-mono">
              {totalNonBillableHours.toFixed(1)}
            </span>
            <span className="text-xs font-bold text-sky-700">giờ</span>
          </div>
          <p className="mt-1 text-[11px] text-sky-600">Họp, đào tạo, việc nội bộ</p>
        </div>

        {/* Employee Info */}
        <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-600">Nhân sự thực hiện</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-slate-100 text-slate-700">
              <Layers className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-2 text-sm font-bold text-slate-900 truncate">
            {weeklyData?.employeeName || user?.fullName || user?.username || "Nhân viên"}
          </div>
          <p className="mt-1 text-[11px] text-slate-500 font-mono">
            Mã NV: {user?.employeeCode || `ID #${user?.id || ""}`}
          </p>
        </div>
      </div>

      {/* Error Banner */}
      {errorMessage && (
        <div className="flex items-center gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-xs font-semibold text-rose-800">
          <AlertCircle className="h-5 w-5 shrink-0 text-rose-600" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Loading state */}
      {isLoading ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-xs">
          <Loader2 className="h-8 w-8 animate-spin text-indigo-600 mb-2" />
          <p className="text-xs font-bold text-slate-600">Đang tải dữ liệu bảng chấm công theo tuần...</p>
        </div>
      ) : (
        /* Daily Work Logs Breakdown */
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-bold text-slate-900">Chi tiết giờ công các ngày trong tuần</h2>
            <div className="flex items-center gap-2 text-xs text-slate-500">
              <Info className="h-3.5 w-3.5 text-slate-400" />
              <span>Quy tắc QTN-09: Tối đa 12.0h / ngày</span>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4">
            {dailyGroups.map((dayGroup: DailyWorkLogGroupDto) => {
              const dayTotal = Number(dayGroup.totalHours ?? 0);
              const isToday =
                new Date().toISOString().split("T")[0] === dayGroup.date;
              const entries = dayGroup.entries ?? [];
              const hasEntries = entries.length > 0;
              const isOverLimit = dayGroup.isExceededLimit || dayTotal > 12.0;
              const isOverStandard = dayTotal > 8.0;

              return (
                <div
                  key={dayGroup.date}
                  className={`rounded-2xl border transition-all ${
                    isToday
                      ? "border-indigo-300 bg-indigo-50/20 shadow-xs"
                      : "border-slate-200 bg-white shadow-xs"
                  }`}
                >
                  {/* Day Header */}
                  <div className="flex flex-wrap items-center justify-between border-b border-slate-100 px-5 py-3.5">
                    <div className="flex items-center gap-3">
                      <div
                        className={`flex h-8 w-8 items-center justify-center rounded-xl text-xs font-extrabold ${
                          isToday
                            ? "bg-indigo-600 text-white"
                            : "bg-slate-100 text-slate-700"
                        }`}
                      >
                        {dayGroup.date ? dayGroup.date.split("-")[2] : ""}
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-bold text-slate-900">
                            {dayGroup.dayOfWeek}
                          </span>
                          {isToday && (
                            <span className="rounded-md bg-indigo-100 px-1.5 py-0.5 text-[10px] font-extrabold text-indigo-700">
                              Hôm nay
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      {/* Daily Total Hours Badge */}
                      <div className="flex items-center gap-1.5">
                        <span className="text-xs font-semibold text-slate-500">Tổng:</span>
                        <span
                          className={`rounded-lg px-2.5 py-1 text-xs font-bold font-mono ${
                            isOverLimit
                              ? "bg-rose-100 text-rose-800"
                              : isOverStandard
                              ? "bg-amber-100 text-amber-800"
                              : dayTotal > 0
                              ? "bg-emerald-100 text-emerald-800"
                              : "bg-slate-100 text-slate-600"
                          }`}
                        >
                          {dayTotal.toFixed(1)} hrs
                        </span>
                      </div>

                      {/* Quick Add For This Day */}
                      {isTimesheetEditable && (
                        <button
                          type="button"
                          onClick={() => handleOpenCreateModal(dayGroup.date)}
                          className="flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:text-indigo-600 transition cursor-pointer"
                        >
                          <Plus className="h-3.5 w-3.5" />
                          <span>Thêm việc</span>
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Day Content: List of entries */}
                  <div className="p-4">
                    {!hasEntries ? (
                      <div className="py-2 text-center text-xs font-medium text-slate-400 italic">
                        Chưa có bản ghi giờ công nào trong ngày này.
                      </div>
                    ) : (
                      <div className="space-y-2.5">
                        {entries.map((entry: WorkLogResult) => (
                          <div
                            key={entry.id}
                            className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-xl border border-slate-100 bg-slate-50/50 p-3 hover:bg-slate-50 hover:border-slate-200 transition"
                          >
                            <div className="space-y-1 flex-1 min-w-0">
                              <div className="flex flex-wrap items-center gap-2">
                                <span className="inline-flex items-center gap-1 rounded-md bg-indigo-100 px-2 py-0.5 text-xs font-bold text-indigo-800">
                                  <Briefcase className="h-3 w-3" />
                                  {entry.projectName}
                                </span>
                                <span className="text-xs font-bold text-slate-800">
                                  {entry.taskName}
                                </span>
                                {entry.isBillable ? (
                                  <span className="rounded-md bg-emerald-50 border border-emerald-200 px-1.5 py-0.5 text-[10px] font-bold text-emerald-700">
                                    Tính phí
                                  </span>
                                ) : (
                                  <span className="rounded-md bg-slate-200 px-1.5 py-0.5 text-[10px] font-semibold text-slate-600">
                                    Nội bộ
                                  </span>
                                )}
                              </div>
                              <p className="text-xs text-slate-600 font-normal leading-relaxed pl-1">
                                {entry.description}
                              </p>
                            </div>

                            <div className="flex items-center justify-between sm:justify-end gap-3 flex-none">
                              <div className="text-right font-mono font-bold text-sm text-indigo-700 bg-white border border-indigo-100 px-3 py-1 rounded-xl shadow-2xs">
                                {Number(entry.hours || 0).toFixed(1)} hrs
                              </div>

                              {isTimesheetEditable && (
                                <div className="flex items-center gap-1">
                                  <button
                                    type="button"
                                    onClick={() => handleOpenEditModal(entry)}
                                    title="Chỉnh sửa"
                                    className="rounded-lg p-1.5 text-slate-400 hover:bg-white hover:text-indigo-600 hover:shadow-2xs transition cursor-pointer"
                                  >
                                    <Edit2 className="h-4 w-4" />
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => handleDeleteEntry(entry)}
                                    title="Xóa"
                                    className="rounded-lg p-1.5 text-slate-400 hover:bg-white hover:text-rose-600 hover:shadow-2xs transition cursor-pointer"
                                  >
                                    <Trash2 className="h-4 w-4" />
                                  </button>
                                </div>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Work Log Modal */}
      <WorkLogModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={() => {
          showToast(
            editingEntry
              ? "Cập nhật giờ làm việc thành công!"
              : "Đã ghi nhận giờ công thành công!"
          );
          loadWeeklyTimesheet(currentDate);
        }}
        initialData={editingEntry}
        defaultDate={targetDateForNewLog}
      />

      {/* Submit Timesheet Confirmation Modal */}
      {isSubmitModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 backdrop-blur-xs p-4">
          <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl space-y-4">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600">
                <Send className="h-6 w-6" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900">
                  Xác nhận nộp bảng chấm công tuần
                </h3>
                <p className="text-xs text-slate-500 font-medium">
                  {weekRangeFormatted}
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-slate-100 bg-slate-50 p-3 space-y-2 text-xs">
              <div className="flex justify-between font-medium text-slate-600">
                <span>Tổng số dòng ghi giờ:</span>
                <span className="font-bold text-slate-900">{allEntries.length} dòng</span>
              </div>
              <div className="flex justify-between font-medium text-slate-600">
                <span>Tổng số giờ làm việc:</span>
                <span className="font-bold text-emerald-700 font-mono">{totalHours.toFixed(1)} hrs</span>
              </div>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              Sau khi nộp, bảng chấm công sẽ chuyển sang trạng thái <strong>Chờ duyệt (SUBMITTED)</strong>. Bạn sẽ không thể chỉnh sửa hoặc xóa các dòng ghi giờ trừ khi bị Quản lý từ chối duyệt.
            </p>

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setIsSubmitModalOpen(false)}
                disabled={isSubmitting}
                className="rounded-xl px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-100 transition cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleSubmitWeeklyTimesheet}
                disabled={isSubmitting}
                className="flex items-center gap-1.5 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-bold text-white shadow-xs hover:bg-emerald-700 transition disabled:opacity-50 cursor-pointer"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span>Đang nộp...</span>
                  </>
                ) : (
                  <>
                    <Send className="h-4 w-4" />
                    <span>Xác nhận nộp</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toast Notification */}
      <div
        role="status"
        aria-live="polite"
        className={`pointer-events-none fixed bottom-6 right-6 z-50 transition-all duration-300 ${
          toast.visible ? "translate-y-0 opacity-100" : "translate-y-20 opacity-0"
        }`}
      >
        <div
          className={`flex items-center gap-3 rounded-xl border px-4 py-3 shadow-2xl backdrop-blur-xl ${
            toast.type === "error"
              ? "border-rose-500/30 bg-slate-950/90 text-rose-200"
              : "border-white/25 bg-slate-950/90 text-white"
          }`}
        >
          {toast.type === "error" ? (
            <AlertCircle className="size-5 text-rose-400" />
          ) : (
            <CheckCircle2 className="size-5 text-emerald-400" />
          )}
          <span className="text-xs font-bold">{toast.message}</span>
        </div>
      </div>
    </div>
  );
}
