import React, { useState, useEffect, useCallback } from "react";
import {
  X,
  Bell,
  Mail,
  Clock,
  RotateCcw,
  Save,
  AlertCircle,
  CheckCircle2,
  Moon,
  Settings,
  Calendar,
  CheckSquare,
  MessageSquare,
  AlertTriangle,
} from "lucide-react";
import {
  getMyNotificationPreference,
  updateMyNotificationPreference,
  resetMyNotificationPreference,
  type NotificationPreference,
  type NotificationDeliveryChannel,
  type NotificationFrequency,
  type UpdateNotificationPreferenceRequest,
} from "@/lib/api/notification-preferences";

interface NotificationSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function NotificationSettingsModal({
  isOpen,
  onClose,
}: NotificationSettingsModalProps) {
  const [preference, setPreference] = useState<NotificationPreference | null>(null);
  const [formData, setFormData] = useState<UpdateNotificationPreferenceRequest>({});
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [isResetting, setIsResetting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const fetchPreference = useCallback(async () => {
    try {
      setIsLoading(true);
      setErrorMessage(null);
      const res = await getMyNotificationPreference();
      setPreference(res);
      setFormData({
        inAppEnabled: res.inAppEnabled,
        emailEnabled: res.emailEnabled,
        taskAssignedChannel: res.taskAssignedChannel,
        taskDueReminderChannel: res.taskDueReminderChannel,
        taskCommentChannel: res.taskCommentChannel,
        timesheetReminderChannel: res.timesheetReminderChannel,
        allocationChangedChannel: res.allocationChangedChannel,
        scheduleConflictChannel: res.scheduleConflictChannel,
        frequency: res.frequency,
        taskDueReminderDays: res.taskDueReminderDays,
        quietHoursEnabled: res.quietHoursEnabled,
        quietHoursStart: res.quietHoursStart ? res.quietHoursStart.slice(0, 5) : "22:00",
        quietHoursEnd: res.quietHoursEnd ? res.quietHoursEnd.slice(0, 5) : "07:00",
      });
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : "Không thể tải cấu hình thông báo");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isOpen) {
      fetchPreference();
    } else {
      setErrorMessage(null);
      setSuccessMessage(null);
    }
  }, [isOpen, fetchPreference]);

  if (!isOpen) return null;

  const handleChannelChange = (
    field: keyof UpdateNotificationPreferenceRequest,
    value: NotificationDeliveryChannel
  ) => {
    // BR-03 check: Không cho phép chọn NONE cho Schedule Conflict hoặc Allocation Changed
    if (
      (field === "scheduleConflictChannel" || field === "allocationChangedChannel") &&
      value === "NONE"
    ) {
      setErrorMessage("Cảnh báo xung đột lịch và phân bổ là thông tin trọng yếu, bắt buộc phải bật ít nhất 1 kênh.");
      return;
    }
    setErrorMessage(null);
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    // Validation
    if (formData.scheduleConflictChannel === "NONE") {
      setErrorMessage("Cảnh báo xung đột lịch bắt buộc phải chọn ít nhất 1 kênh nhận.");
      return;
    }
    if (formData.allocationChangedChannel === "NONE") {
      setErrorMessage("Cảnh báo thay đổi phân bổ bắt buộc phải chọn ít nhất 1 kênh nhận.");
      return;
    }

    try {
      setIsSaving(true);
      const payload: UpdateNotificationPreferenceRequest = {
        ...formData,
        quietHoursStart: formData.quietHoursEnabled && formData.quietHoursStart
          ? (formData.quietHoursStart.length === 5 ? `${formData.quietHoursStart}:00` : formData.quietHoursStart)
          : null,
        quietHoursEnd: formData.quietHoursEnabled && formData.quietHoursEnd
          ? (formData.quietHoursEnd.length === 5 ? `${formData.quietHoursEnd}:00` : formData.quietHoursEnd)
          : null,
      };

      const updated = await updateMyNotificationPreference(payload);
      setPreference(updated);
      setSuccessMessage("Lưu cài đặt thông báo thành công!");
      setTimeout(() => {
        setSuccessMessage(null);
      }, 3000);
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : "Không thể cập nhật cấu hình");
    } finally {
      setIsSaving(false);
    }
  };

  const handleReset = async () => {
    if (!window.confirm("Bạn có chắc chắn muốn khôi phục toàn bộ cài đặt thông báo về mặc định của hệ thống?")) {
      return;
    }
    try {
      setIsResetting(true);
      setErrorMessage(null);
      const res = await resetMyNotificationPreference();
      setPreference(res);
      setFormData({
        inAppEnabled: res.inAppEnabled,
        emailEnabled: res.emailEnabled,
        taskAssignedChannel: res.taskAssignedChannel,
        taskDueReminderChannel: res.taskDueReminderChannel,
        taskCommentChannel: res.taskCommentChannel,
        timesheetReminderChannel: res.timesheetReminderChannel,
        allocationChangedChannel: res.allocationChangedChannel,
        scheduleConflictChannel: res.scheduleConflictChannel,
        frequency: res.frequency,
        taskDueReminderDays: res.taskDueReminderDays,
        quietHoursEnabled: res.quietHoursEnabled,
        quietHoursStart: res.quietHoursStart ? res.quietHoursStart.slice(0, 5) : "22:00",
        quietHoursEnd: res.quietHoursEnd ? res.quietHoursEnd.slice(0, 5) : "07:00",
      });
      setSuccessMessage("Đã khôi phục cài đặt về mặc định của hệ thống.");
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : "Không thể khôi phục mặc định");
    } finally {
      setIsResetting(false);
    }
  };

  const channelOptions: { value: NotificationDeliveryChannel; label: string }[] = [
    { value: "ALL", label: "Cả In-App & Email" },
    { value: "IN_APP_ONLY", label: "Chỉ In-App" },
    { value: "EMAIL_ONLY", label: "Chỉ Email" },
    { value: "NONE", label: "Tắt thông báo" },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="w-full max-w-2xl rounded-2xl bg-white shadow-2xl border border-slate-200 text-slate-800 flex flex-col max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 bg-slate-50/80">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-100 text-indigo-700">
              <Settings className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900">
                Cấu hình kênh & tần suất nhận thông báo
              </h2>
              <p className="text-xs text-slate-500">
                Tùy chỉnh cách thức, tần suất và loại sự kiện bạn muốn nhận thông báo
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-200 hover:text-slate-700 transition"
            type="button"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Body Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {errorMessage && (
            <div className="flex items-start gap-2 rounded-xl bg-rose-50 p-3 text-xs font-medium text-rose-700 border border-rose-200">
              <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-rose-600" />
              <span>{errorMessage}</span>
            </div>
          )}

          {successMessage && (
            <div className="flex items-start gap-2 rounded-xl bg-emerald-50 p-3 text-xs font-medium text-emerald-700 border border-emerald-200">
              <CheckCircle2 className="h-4 w-4 shrink-0 mt-0.5 text-emerald-600" />
              <span>{successMessage}</span>
            </div>
          )}

          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-12 text-slate-500">
              <div className="h-8 w-8 animate-spin rounded-full border-2 border-indigo-600 border-t-transparent mb-3" />
              <p className="text-xs font-medium">Đang tải cấu hình thông báo...</p>
            </div>
          ) : (
            <form id="notif-pref-form" onSubmit={handleSave} className="space-y-6">
              {/* 1. Kênh tiếp nhận tổng thể (Master Toggles) */}
              <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-4">
                <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-3">
                  Kênh tiếp nhận chính
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <label className="flex items-center justify-between p-3 rounded-lg bg-white border border-slate-200 cursor-pointer hover:border-indigo-300 transition">
                    <div className="flex items-center gap-2.5">
                      <Bell className="h-4 w-4 text-indigo-600" />
                      <div>
                        <p className="text-xs font-semibold text-slate-800">Thông báo ứng dụng (In-App)</p>
                        <p className="text-[11px] text-slate-500">Hiển thị trong chuông thông báo</p>
                      </div>
                    </div>
                    <input
                      type="checkbox"
                      checked={formData.inAppEnabled ?? true}
                      onChange={(e) => setFormData((prev) => ({ ...prev, inAppEnabled: e.target.checked }))}
                      className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
                    />
                  </label>

                  <label className="flex items-center justify-between p-3 rounded-lg bg-white border border-slate-200 cursor-pointer hover:border-indigo-300 transition">
                    <div className="flex items-center gap-2.5">
                      <Mail className="h-4 w-4 text-sky-600" />
                      <div>
                        <p className="text-xs font-semibold text-slate-800">Thông báo qua Email</p>
                        <p className="text-[11px] text-slate-500">Gửi về hòm thư điện tử cá nhân</p>
                      </div>
                    </div>
                    <input
                      type="checkbox"
                      checked={formData.emailEnabled ?? true}
                      onChange={(e) => setFormData((prev) => ({ ...prev, emailEnabled: e.target.checked }))}
                      className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
                    />
                  </label>
                </div>
              </div>

              {/* 2. Cấu hình chi tiết theo từng loại sự kiện */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                    Kênh nhận cho từng loại sự kiện
                  </h3>
                  <span className="text-[11px] text-slate-500">
                    (*) Cảnh báo xung đột & phân bổ là trọng yếu
                  </span>
                </div>

                <div className="divide-y divide-slate-100 rounded-xl border border-slate-200 overflow-hidden bg-white text-xs">
                  {/* Row 1: Xung đột lịch (Trọng yếu) */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 gap-2 bg-amber-50/40">
                    <div className="flex items-start gap-2.5">
                      <AlertTriangle className="h-4 w-4 text-amber-600 mt-0.5 shrink-0" />
                      <div>
                        <div className="flex items-center gap-1.5">
                          <span className="font-semibold text-slate-900">Xung đột lịch & Quá tải nguồn lực</span>
                          <span className="px-1.5 py-0.2 rounded text-[10px] font-bold bg-rose-100 text-rose-700">
                            Bắt buộc
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500">
                          Cảnh báo trùng lịch, vượt giờ khả dụng hoặc điều chuyển nhân sự
                        </p>
                      </div>
                    </div>
                    <select
                      value={formData.scheduleConflictChannel || "ALL"}
                      onChange={(e) =>
                        handleChannelChange(
                          "scheduleConflictChannel",
                          e.target.value as NotificationDeliveryChannel
                        )
                      }
                      className="border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs bg-white text-slate-800 font-medium focus:ring-1 focus:ring-indigo-500"
                    >
                      <option value="ALL">Cả In-App & Email</option>
                      <option value="IN_APP_ONLY">Chỉ In-App</option>
                      <option value="EMAIL_ONLY">Chỉ Email</option>
                    </select>
                  </div>

                  {/* Row 2: Thay đổi phân bổ (Trọng yếu) */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 gap-2 bg-amber-50/40">
                    <div className="flex items-start gap-2.5">
                      <Calendar className="h-4 w-4 text-amber-600 mt-0.5 shrink-0" />
                      <div>
                        <div className="flex items-center gap-1.5">
                          <span className="font-semibold text-slate-900">Thay đổi phân bổ dự án</span>
                          <span className="px-1.5 py-0.2 rounded text-[10px] font-bold bg-rose-100 text-rose-700">
                            Bắt buộc
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500">
                          Thêm, điều chỉnh số giờ hoặc gỡ phân bổ vào dự án trong tuần
                        </p>
                      </div>
                    </div>
                    <select
                      value={formData.allocationChangedChannel || "ALL"}
                      onChange={(e) =>
                        handleChannelChange(
                          "allocationChangedChannel",
                          e.target.value as NotificationDeliveryChannel
                        )
                      }
                      className="border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs bg-white text-slate-800 font-medium focus:ring-1 focus:ring-indigo-500"
                    >
                      <option value="ALL">Cả In-App & Email</option>
                      <option value="IN_APP_ONLY">Chỉ In-App</option>
                      <option value="EMAIL_ONLY">Chỉ Email</option>
                    </select>
                  </div>

                  {/* Row 3: Nhắc việc sắp đến hạn */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 gap-2">
                    <div className="flex items-start gap-2.5">
                      <Clock className="h-4 w-4 text-indigo-600 mt-0.5 shrink-0" />
                      <div>
                        <span className="font-semibold text-slate-800">Nhắc việc sắp đến hạn chót</span>
                        <p className="text-[11px] text-slate-500">
                          Thông báo công việc của bạn có hạn hoàn thành trong vài ngày tới
                        </p>
                      </div>
                    </div>
                    <select
                      value={formData.taskDueReminderChannel || "ALL"}
                      onChange={(e) =>
                        handleChannelChange(
                          "taskDueReminderChannel",
                          e.target.value as NotificationDeliveryChannel
                        )
                      }
                      className="border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs bg-white text-slate-800 font-medium focus:ring-1 focus:ring-indigo-500"
                    >
                      {channelOptions.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* Row 4: Giao việc mới */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 gap-2">
                    <div className="flex items-start gap-2.5">
                      <CheckSquare className="h-4 w-4 text-emerald-600 mt-0.5 shrink-0" />
                      <div>
                        <span className="font-semibold text-slate-800">Được phân công công việc mới</span>
                        <p className="text-[11px] text-slate-500">
                          Thông báo khi Quản lý dự án giao task hoặc WBS mới cho bạn
                        </p>
                      </div>
                    </div>
                    <select
                      value={formData.taskAssignedChannel || "ALL"}
                      onChange={(e) =>
                        handleChannelChange(
                          "taskAssignedChannel",
                          e.target.value as NotificationDeliveryChannel
                        )
                      }
                      className="border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs bg-white text-slate-800 font-medium focus:ring-1 focus:ring-indigo-500"
                    >
                      {channelOptions.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* Row 5: Nhắc nộp & duyệt bảng chấm công tuần */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 gap-2">
                    <div className="flex items-start gap-2.5">
                      <Calendar className="h-4 w-4 text-purple-600 mt-0.5 shrink-0" />
                      <div>
                        <span className="font-semibold text-slate-800">Chấm công tuần (Timesheet)</span>
                        <p className="text-[11px] text-slate-500">
                          Nhắc nộp giờ làm cuối tuần hoặc thông báo duyệt/từ chối chấm công
                        </p>
                      </div>
                    </div>
                    <select
                      value={formData.timesheetReminderChannel || "ALL"}
                      onChange={(e) =>
                        handleChannelChange(
                          "timesheetReminderChannel",
                          e.target.value as NotificationDeliveryChannel
                        )
                      }
                      className="border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs bg-white text-slate-800 font-medium focus:ring-1 focus:ring-indigo-500"
                    >
                      {channelOptions.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* Row 6: Thảo luận & Đề cập */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 gap-2">
                    <div className="flex items-start gap-2.5">
                      <MessageSquare className="h-4 w-4 text-sky-600 mt-0.5 shrink-0" />
                      <div>
                        <span className="font-semibold text-slate-800">Thảo luận & Nhắc tên (Mentions)</span>
                        <p className="text-[11px] text-slate-500">
                          Khi có ai đó bình luận hoặc gắn thẻ bạn trong công việc
                        </p>
                      </div>
                    </div>
                    <select
                      value={formData.taskCommentChannel || "IN_APP_ONLY"}
                      onChange={(e) =>
                        handleChannelChange(
                          "taskCommentChannel",
                          e.target.value as NotificationDeliveryChannel
                        )
                      }
                      className="border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs bg-white text-slate-800 font-medium focus:ring-1 focus:ring-indigo-500"
                    >
                      {channelOptions.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>

              {/* 3. Tần suất gửi & Tùy biến nhắc việc */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="rounded-xl border border-slate-200 p-4 bg-slate-50/50">
                  <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                    Tần suất nhận thông báo
                  </h3>
                  <p className="text-[11px] text-slate-500 mb-3">
                    Chọn cách thức hệ thống gửi thông báo cho bạn
                  </p>
                  <select
                    value={formData.frequency || "IMMEDIATE"}
                    onChange={(e) =>
                      setFormData((prev) => ({
                        ...prev,
                        frequency: e.target.value as NotificationFrequency,
                      }))
                    }
                    className="w-full border border-slate-300 rounded-lg px-3 py-2 text-xs bg-white text-slate-800 font-medium focus:ring-1 focus:ring-indigo-500"
                  >
                    <option value="IMMEDIATE">⚡ Tức thời (Gửi ngay khi có sự kiện)</option>
                    <option value="DAILY_DIGEST">📅 Bản tin tổng hợp hàng ngày (17:00)</option>
                    <option value="WEEKLY_DIGEST">📊 Bản tin tổng hợp hàng tuần (Sáng T2)</option>
                  </select>
                </div>

                <div className="rounded-xl border border-slate-200 p-4 bg-slate-50/50">
                  <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                    Thời gian nhắc trước hạn chót
                  </h3>
                  <p className="text-[11px] text-slate-500 mb-3">
                    Bắt đầu nhắc việc trước khi deadline tới
                  </p>
                  <select
                    value={formData.taskDueReminderDays ?? 3}
                    onChange={(e) =>
                      setFormData((prev) => ({
                        ...prev,
                        taskDueReminderDays: Number(e.target.value),
                      }))
                    }
                    className="w-full border border-slate-300 rounded-lg px-3 py-2 text-xs bg-white text-slate-800 font-medium focus:ring-1 focus:ring-indigo-500"
                  >
                    <option value={1}>1 ngày trước hạn</option>
                    <option value={2}>2 ngày trước hạn</option>
                    <option value={3}>3 ngày trước hạn (Chuẩn mặc định)</option>
                    <option value={5}>5 ngày trước hạn</option>
                    <option value={7}>1 tuần trước hạn</option>
                  </select>
                </div>
              </div>

              {/* 4. Khung giờ yên tĩnh (Quiet Hours) */}
              <div className="rounded-xl border border-slate-200 p-4 bg-slate-50/50">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <Moon className="h-4 w-4 text-indigo-600" />
                    <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                      Khung giờ yên tĩnh (Không làm phiền)
                    </h3>
                  </div>
                  <input
                    type="checkbox"
                    checked={formData.quietHoursEnabled ?? false}
                    onChange={(e) =>
                      setFormData((prev) => ({
                        ...prev,
                        quietHoursEnabled: e.target.checked,
                      }))
                    }
                    className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 cursor-pointer"
                  />
                </div>
                <p className="text-[11px] text-slate-500 mb-3">
                  Hoãn gửi email thông báo không khẩn cấp trong khoảng thời gian nghỉ ngơi
                </p>

                {formData.quietHoursEnabled && (
                  <div className="flex items-center gap-3 pt-2">
                    <div className="flex items-center gap-1.5">
                      <span className="text-xs text-slate-600">Từ:</span>
                      <input
                        type="time"
                        value={formData.quietHoursStart || "22:00"}
                        onChange={(e) =>
                          setFormData((prev) => ({
                            ...prev,
                            quietHoursStart: e.target.value,
                          }))
                        }
                        className="border border-slate-300 rounded-lg px-2.5 py-1 text-xs bg-white text-slate-800 focus:ring-1 focus:ring-indigo-500"
                      />
                    </div>
                    <div className="flex items-center gap-1.5">
                      <span className="text-xs text-slate-600">Đến:</span>
                      <input
                        type="time"
                        value={formData.quietHoursEnd || "07:00"}
                        onChange={(e) =>
                          setFormData((prev) => ({
                            ...prev,
                            quietHoursEnd: e.target.value,
                          }))
                        }
                        className="border border-slate-300 rounded-lg px-2.5 py-1 text-xs bg-white text-slate-800 focus:ring-1 focus:ring-indigo-500"
                      />
                    </div>
                  </div>
                )}
              </div>
            </form>
          )}
        </div>

        {/* Footer Actions */}
        <div className="flex items-center justify-between border-t border-slate-200 px-6 py-4 bg-slate-50/80">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={handleReset}
              disabled={isLoading || isSaving || isResetting}
              className="flex items-center gap-1.5 text-xs font-semibold text-slate-600 hover:text-slate-800 hover:bg-slate-200/60 px-3 py-2 rounded-xl transition disabled:opacity-50 cursor-pointer"
            >
              <RotateCcw className={`h-3.5 w-3.5 ${isResetting ? "animate-spin" : ""}`} />
              Khôi phục mặc định
            </button>
            {preference?.version !== undefined && (
              <span className="text-[10px] font-mono text-slate-400">
                v{preference.version}
              </span>
            )}
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-200/60 rounded-xl transition cursor-pointer"
            >
              Đóng
            </button>
            <button
              type="submit"
              form="notif-pref-form"
              disabled={isLoading || isSaving || isResetting}
              className="flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-xs hover:bg-indigo-700 transition disabled:opacity-50 cursor-pointer"
            >
              <Save className={`h-3.5 w-3.5 ${isSaving ? "animate-spin" : ""}`} />
              {isSaving ? "Đang lưu..." : "Lưu cài đặt"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
