import React, { useState, useEffect, useCallback, useMemo } from "react";
import { ChevronLeft, ChevronRight, CalendarDays, CalendarX, Loader2, AlertCircle, CheckCircle2, MessageSquare, Info, X } from "lucide-react";
import type { WeeklySchedule } from "../types";
import { myScheduleApi } from "../api/myScheduleApi";
import { WeeklyScheduleCard } from "../components/WeeklyScheduleCard";
import { useAuthUser } from "@/lib/auth-session";
import { getEmployeeProfileByUserId, type EmployeeProfile } from "@/lib/api/employees";
import DeclareUnavailabilityModal from "@/components/unavailability/DeclareUnavailabilityModal";

export const MyWeeklySchedulePage: React.FC = () => {
  const user = useAuthUser();
  const [weeksData, setWeeksData] = useState<WeeklySchedule[]>([]);
  const [currentWeekStart, setCurrentWeekStart] = useState<string>("");
  const [weeksCount, setWeeksCount] = useState<number>(2);
  const [loading, setLoading] = useState<boolean>(true);
  const [confirmingWeek, setConfirmingWeek] = useState<string | null>(null);
  const [feedbackMessage, setFeedbackMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  // Unavailability Modal State
  const [isUnavailabilityModalOpen, setIsUnavailabilityModalOpen] = useState<boolean>(false);
  const [currentEmployee, setCurrentEmployee] = useState<EmployeeProfile | null>(null);

  useEffect(() => {
    if (!user?.id) return;
    getEmployeeProfileByUserId(user.id)
      .then((emp) => setCurrentEmployee(emp))
      .catch(() => setCurrentEmployee(null));
  }, [user?.id]);

  // Feedback Modal State (NCL-13-CN-002, QTN-24)
  const [feedbackModalWeek, setFeedbackModalWeek] = useState<string | null>(null);
  const [feedbackReason, setFeedbackReason] = useState<string>("");
  const [feedbackError, setFeedbackError] = useState<string | null>(null);
  const [submittingFeedback, setSubmittingFeedback] = useState<boolean>(false);

  const getThisMonday = (): string => {
    const d = new Date();
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    const monday = new Date(d.setDate(diff));
    return monday.toISOString().split("T")[0];
  };

  const getMondayFromDate = (date: Date): string => {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    d.setDate(diff);
    return d.toISOString().split("T")[0];
  };

  const loadSchedule = useCallback(async (startDate?: string, weeks?: number) => {
    setLoading(true);
    setFeedbackMessage(null);
    try {
      const response = await myScheduleApi.getMyAllocations(startDate, weeks);
      setWeeksData(response.weeks || []);
      if (response.weeks && response.weeks.length > 0) {
        setCurrentWeekStart(response.weeks[0].week_start_date);
      }
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : "Không thể tải dữ liệu lịch phân bổ tuần.";
      setFeedbackMessage({ type: "error", text: errorMsg });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const monday = getThisMonday();
    setCurrentWeekStart(monday);
    loadSchedule(monday, weeksCount);
  }, [loadSchedule, weeksCount]);

  const handleNavigateWeeks = (deltaWeeks: number) => {
    if (!currentWeekStart) return;
    const curr = new Date(currentWeekStart);
    curr.setDate(curr.getDate() + deltaWeeks * 7);
    const nextMonday = curr.toISOString().split("T")[0];
    setCurrentWeekStart(nextMonday);
    loadSchedule(nextMonday, weeksCount);
  };

  const handleResetToCurrentWeek = () => {
    const thisMonday = getThisMonday();
    setCurrentWeekStart(thisMonday);
    loadSchedule(thisMonday, weeksCount);
  };

  const handleConfirm = async (weekStart: string) => {
    setConfirmingWeek(weekStart);
    setFeedbackMessage(null);
    try {
      const res = await myScheduleApi.confirmScheduleViewed(weekStart);
      if (res.already_confirmed) {
        setFeedbackMessage({ type: "success", text: `Tuần ${weekStart} đã được xác nhận trước đó.` });
      } else if (res.previous_confirmation_was_stale) {
        setFeedbackMessage({ type: "success", text: `Đã xác nhận lại thành công cho tuần ${weekStart} sau khi có cập nhật mới!` });
      } else {
        setFeedbackMessage({ type: "success", text: `Đã xác nhận thành công lịch phân bổ tuần ${weekStart}!` });
      }

      await loadSchedule(currentWeekStart, weeksCount);
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : "Có lỗi xảy ra khi xác nhận lịch.";
      setFeedbackMessage({ type: "error", text: errorMsg });
    } finally {
      setConfirmingWeek(null);
    }
  };

  const handleOpenFeedback = (weekStart: string) => {
    const targetWeek = weeksData.find((w) => w.week_start_date === weekStart);
    setFeedbackModalWeek(weekStart);
    setFeedbackReason(targetWeek?.feedback_note || "");
    setFeedbackError(null);
  };

  const handleCloseFeedback = () => {
    setFeedbackModalWeek(null);
    setFeedbackReason("");
    setFeedbackError(null);
  };

  const handleSubmitFeedback = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!feedbackModalWeek) return;

    if (!feedbackReason || !feedbackReason.trim()) {
      setFeedbackError("Vui lòng nhập lý do hoặc ý kiến phản hồi.");
      return;
    }

    setSubmittingFeedback(true);
    setFeedbackError(null);
    try {
      await myScheduleApi.provideFeedback(feedbackModalWeek, feedbackReason.trim());
      handleCloseFeedback();
      setFeedbackMessage({
        type: "success",
        text: `Đã gửi phản hồi cho tuần ${feedbackModalWeek}. Quản lý dự án sẽ rà soát và điều chỉnh nếu cần thiết.`,
      });
      await loadSchedule(currentWeekStart, weeksCount);
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : "Có lỗi xảy ra khi gửi ý kiến phản hồi.";
      setFeedbackError(errorMsg);
    } finally {
      setSubmittingFeedback(false);
    }
  };

  const handleDateChange = (dateStr: string) => {
    if (!dateStr) return;
    const [y, m, d] = dateStr.split("-").map(Number);
    const mon = getMondayFromDate(new Date(y, m - 1, d));
    setCurrentWeekStart(mon);
    loadSchedule(mon, weeksCount);
  };

  const sortedWeeks = useMemo(() => {
    return [...weeksData].sort((a, b) => {
      const aNeeds = a.confirmation_status === "NOT_CONFIRMED" || a.confirmation_status === "STALE";
      const bNeeds = b.confirmation_status === "NOT_CONFIRMED" || b.confirmation_status === "STALE";
      if (aNeeds && !bNeeds) return -1;
      if (!aNeeds && bNeeds) return 1;
      return a.week_start_date.localeCompare(b.week_start_date);
    });
  }, [weeksData]);

  const unconfirmedCount = useMemo(() => {
    return weeksData.filter((w) => w.confirmation_status === "NOT_CONFIRMED" || w.confirmation_status === "STALE").length;
  }, [weeksData]);

  return (
    <div className="max-w-5xl mx-auto p-4 md:p-6 space-y-6">
      {/* Header Panel */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-blue-50 text-blue-600 rounded-lg">
              <CalendarDays className="w-5 h-5" />
            </div>
            <h1 className="text-xl font-bold text-slate-900">Lịch phân bổ tuần của tôi</h1>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Xem kế hoạch phân bổ giờ làm việc theo từng dự án, xác nhận đã xem hoặc gửi phản hồi khi có vấn đề.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <button
            type="button"
            onClick={() => setIsUnavailabilityModalOpen(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold text-amber-900 bg-amber-50 border border-amber-300 rounded-lg hover:bg-amber-100 hover:border-amber-400 transition cursor-pointer shadow-2xs"
          >
            <CalendarX className="w-3.5 h-3.5 text-amber-700" />
            <span>Khai báo không sẵn sàng</span>
          </button>

          {/* Week Navigator */}
          <div className="flex items-center rounded-xl border border-slate-200 bg-white p-1 shadow-xs">
            <button
              type="button"
              onClick={() => handleNavigateWeeks(-1)}
              className="rounded-lg p-1.5 text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition cursor-pointer"
              title="Tuần trước"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={handleResetToCurrentWeek}
              className="px-3 py-1 text-xs font-bold text-slate-700 hover:text-indigo-600 transition cursor-pointer"
              title="Quay lại tuần hiện tại"
            >
              {currentWeekStart === getThisMonday() ? "Tuần này" : "Hiện tại"}
            </button>
            <button
              type="button"
              onClick={() => handleNavigateWeeks(1)}
              className="rounded-lg p-1.5 text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition cursor-pointer"
              title="Tuần kế tiếp"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          {/* Date Picker */}
          <div className="flex items-center gap-1.5 text-xs text-slate-600 border border-slate-200 rounded-xl px-2.5 py-1 bg-white shadow-xs">
            <span className="text-slate-500 font-medium">Chọn ngày:</span>
            <input
              type="date"
              value={currentWeekStart}
              onChange={(e) => handleDateChange(e.target.value)}
              className="bg-transparent font-medium text-slate-800 outline-hidden cursor-pointer"
              title="Chọn ngày để chuyển đến tuần đó"
            />
          </div>

          <div className="flex items-center gap-1.5 text-xs text-slate-600 border border-slate-200 rounded-xl px-3 py-1.5 bg-white shadow-xs">
            <span>Hiển thị:</span>
            <select
              value={weeksCount}
              onChange={(e) => setWeeksCount(Number(e.target.value))}
              className="bg-transparent font-semibold text-slate-800 outline-hidden cursor-pointer"
            >
              <option value={1}>1 tuần</option>
              <option value={2}>2 tuần (Chuẩn)</option>
              <option value={4}>4 tuần</option>
              <option value={8}>8 tuần</option>
            </select>
          </div>
        </div>
      </div>

      {/* Priority Banner for Unconfirmed Weeks */}
      {unconfirmedCount > 0 && (
        <div className="flex items-center justify-between p-3.5 rounded-xl bg-amber-50/90 border border-amber-200 text-amber-900 text-xs shadow-2xs">
          <div className="flex items-center gap-2.5">
            <AlertCircle className="w-4 h-4 text-amber-600 shrink-0" />
            <div>
              <span className="font-bold">Ưu tiên xử lý:</span> Bạn có <strong>{unconfirmedCount} tuần</strong> phân bổ chưa xác nhận hoặc vừa có thay đổi (đã được ưu tiên đưa lên đầu).
            </div>
          </div>
        </div>
      )}

      {feedbackMessage && (
        <div
          className={`p-4 rounded-xl flex items-center gap-3 text-sm transition ${
            feedbackMessage.type === "success"
              ? "bg-emerald-50 text-emerald-800 border border-emerald-200"
              : "bg-red-50 text-red-800 border border-red-200"
          }`}
        >
          {feedbackMessage.type === "success" ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          ) : (
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
          )}
          <span>{feedbackMessage.text}</span>
        </div>
      )}

      {loading ? (
        <div className="flex flex-col items-center justify-center py-20 bg-white rounded-xl border border-slate-200">
          <Loader2 className="w-8 h-8 text-blue-600 animate-spin mb-3" />
          <p className="text-sm font-medium text-slate-500">Đang tải lịch phân bổ tuần...</p>
        </div>
      ) : sortedWeeks.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-xl border border-slate-200">
          <CalendarDays className="w-10 h-10 text-slate-300 mx-auto mb-3" />
          <p className="text-slate-600 font-medium text-base">Không tìm thấy dữ liệu phân bổ.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {sortedWeeks.map((week) => (
            <WeeklyScheduleCard
              key={week.week_start_date}
              schedule={week}
              onConfirm={handleConfirm}
              onOpenFeedback={handleOpenFeedback}
              isConfirming={confirmingWeek === week.week_start_date}
            />
          ))}
        </div>
      )}

      {/* Modal gửi phản hồi phân bổ (NCL-13-CN-002) */}
      {feedbackModalWeek && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-lg w-full overflow-hidden border border-slate-100 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between p-5 border-b border-slate-100 bg-slate-50/50">
              <div className="flex items-center gap-2.5">
                <div className="p-2 bg-purple-100 text-purple-700 rounded-lg">
                  <MessageSquare className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-slate-900 text-base">Phản hồi phân bổ tuần</h3>
                  <p className="text-xs text-slate-500">Tuần bắt đầu: {feedbackModalWeek}</p>
                </div>
              </div>
              <button
                type="button"
                onClick={handleCloseFeedback}
                className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmitFeedback} className="p-5 space-y-4">
              {feedbackError && (
                <div className="p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{feedbackError}</span>
                </div>
              )}

              <div>
                <label htmlFor="feedback-reason" className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Lý do / Ý kiến phản hồi <span className="text-red-500">*</span>
                </label>
                <textarea
                  id="feedback-reason"
                  rows={4}
                  value={feedbackReason}
                  onChange={(e) => setFeedbackReason(e.target.value)}
                  placeholder="Ví dụ: Trùng lịch với dự án khác, tổng giờ quá tải so với thỏa thuận, cần bổ sung quyền truy cập..."
                  autoComplete="off"
                  spellCheck={false}
                  className="w-full text-xs p-3 border border-slate-200 rounded-lg focus:outline-hidden focus:ring-2 focus:ring-purple-500 focus:border-transparent resize-none leading-relaxed text-slate-800"
                  required
                />
              </div>

              <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 flex items-start gap-2 text-[11px] text-amber-900">
                <Info className="w-3.5 h-3.5 text-amber-600 shrink-0 mt-0.5" />
                <span>
                  <strong>Lưu ý:</strong> Ý kiến phản hồi được gửi đến PM/RM để xem xét. Giờ phân bổ hiện tại trên hệ thống sẽ không tự động thay đổi.
                </span>
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-2">
                <button
                  type="button"
                  onClick={handleCloseFeedback}
                  disabled={submittingFeedback}
                  className="px-4 py-2 text-xs font-medium text-slate-600 hover:bg-slate-100 rounded-lg transition cursor-pointer"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  disabled={submittingFeedback}
                  className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-medium text-white bg-purple-600 hover:bg-purple-700 active:bg-purple-800 rounded-lg transition shadow-xs disabled:opacity-50 cursor-pointer"
                >
                  {submittingFeedback ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      <span>Đang gửi...</span>
                    </>
                  ) : (
                    <span>Gửi phản hồi</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Khai báo thời gian không sẵn sàng */}
      {isUnavailabilityModalOpen && currentEmployee && (
        <DeclareUnavailabilityModal
          isOpen={isUnavailabilityModalOpen}
          onClose={() => setIsUnavailabilityModalOpen(false)}
          onSuccess={(_res) => {
            setIsUnavailabilityModalOpen(false);
            setFeedbackMessage({
              type: "success",
              text: "Đã gửi khai báo thời gian không sẵn sàng thành công!",
            });
            loadSchedule(currentWeekStart, weeksCount);
          }}
          employeeId={currentEmployee.id}
          employeeName={currentEmployee.fullName}
        />
      )}
    </div>
  );
};

export default MyWeeklySchedulePage;