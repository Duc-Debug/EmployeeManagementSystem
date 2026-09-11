import React, { useState, useEffect } from 'react';
import {
  X,
  AlertTriangle,
  CheckCircle2,
  Clock,
  ShieldAlert,
  Calendar,
  Layers,
  Flag,
  ArrowRight,
  RefreshCw,
  FileCheck,
} from 'lucide-react';
import {
  evaluateCascadeDelay,
  updateTaskActualEndDate,
  type CascadeDelayWarningResult,
} from '@/lib/api/cascadeDelay';

interface CascadeDelayWarningModalProps {
  open: boolean;
  projectId: number;
  taskId: number;
  taskName: string;
  taskCode?: string;
  currentDueDate?: string;
  canManage?: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export const CascadeDelayWarningModal: React.FC<CascadeDelayWarningModalProps> = ({
  open,
  projectId,
  taskId,
  taskName,
  taskCode,
  currentDueDate,
  canManage = true,
  onClose,
  onSuccess,
}) => {
  const [newActualEndDate, setNewActualEndDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [result, setResult] = useState<CascadeDelayWarningResult | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  useEffect(() => {
    if (open && currentDueDate) {
      // Default to 3 days after due date for simulation (TC-01 scenario)
      const dateObj = new Date(currentDueDate);
      if (!isNaN(dateObj.getTime())) {
        dateObj.setDate(dateObj.getDate() + 3);
        setNewActualEndDate(dateObj.toISOString().split('T')[0]);
      }
    }
  }, [open, currentDueDate]);

  const handleEvaluate = async () => {
    if (!newActualEndDate) {
      setErrorMsg('Vui lòng chọn ngày kết thúc thực tế');
      return;
    }

    setIsLoading(true);
    setErrorMsg(null);
    setSuccessMsg(null);

    try {
      const res = await evaluateCascadeDelay(projectId, taskId, newActualEndDate);
      setResult(res);
    } catch (err: any) {
      const msg = err?.message || 'Không thể đánh giá ảnh hưởng trễ dây chuyền. Bạn có thể không có quyền truy cập.';
      setErrorMsg(msg);
      setResult(null);
    } finally {
      setIsLoading(false);
    }
  };

  const handleConfirmUpdate = async () => {
    if (!newActualEndDate) {
      setErrorMsg('Vui lòng chọn ngày kết thúc thực tế');
      return;
    }

    setIsSubmitting(true);
    setErrorMsg(null);
    setSuccessMsg(null);

    try {
      const res = await updateTaskActualEndDate(projectId, taskId, newActualEndDate);
      setResult(res);
      setSuccessMsg('Đã cập nhật ngày kết thúc thực tế và lưu nhật ký lịch sử thành công!');
      if (onSuccess) {
        onSuccess();
      }
    } catch (err: any) {
      const msg = err?.message || 'Từ chối thao tác: Bạn không có quyền quản lý cập nhật ngày kết thúc thực tế.';
      setErrorMsg(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in overflow-y-auto">
      <div className="w-full max-w-4xl overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all my-8">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 px-5 py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-100 text-amber-700">
              <AlertTriangle className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-900">
                Cảnh Báo Trễ Dây Chuyền Khi Công Việc Trượt (Cascade Delay Warning)
              </h3>
              <p className="text-[11px] text-slate-500">
                Phân tích ảnh hưởng lan truyền theo chuỗi phụ thuộc & thời gian dự phòng Slack Time
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition cursor-pointer"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="p-5 space-y-5">
          {/* Notifications */}
          {errorMsg && (
            <div className="flex items-start gap-2.5 rounded-xl bg-rose-50 p-3.5 text-xs text-rose-700 border border-rose-200">
              <ShieldAlert className="h-4 w-4 shrink-0 mt-0.5 text-rose-600" />
              <div className="flex-1 font-medium">
                <p className="font-bold mb-0.5">Thông báo từ chối / Lỗi hệ thống:</p>
                <p>{errorMsg}</p>
              </div>
            </div>
          )}

          {successMsg && (
            <div className="flex items-center gap-2.5 rounded-xl bg-emerald-50 p-3.5 text-xs text-emerald-700 border border-emerald-200">
              <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
              <p className="font-medium">{successMsg}</p>
            </div>
          )}

          {/* Root Task Summary Card */}
          <div className="rounded-xl bg-slate-50 p-4 border border-slate-200">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="rounded bg-indigo-100 px-2 py-0.5 text-[10px] font-bold uppercase text-indigo-700">
                    {taskCode || `#${taskId}`}
                  </span>
                  <h4 className="text-xs font-bold text-slate-800">{taskName}</h4>
                </div>
                {currentDueDate && (
                  <p className="text-[11px] text-slate-500 flex items-center gap-1.5">
                    <Calendar className="h-3.5 w-3.5 text-slate-400" />
                    <span>Ngày kế hoạch (Due Date): <strong className="text-slate-700">{currentDueDate}</strong></span>
                  </p>
                )}
              </div>

              {/* Form chọn ngày thực tế */}
              <div className="flex items-center gap-2.5 w-full sm:w-auto">
                <div>
                  <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                    Ngày kết thúc thực tế mới:
                  </label>
                  <input
                    type="date"
                    value={newActualEndDate}
                    onChange={(e) => setNewActualEndDate(e.target.value)}
                    className="rounded-xl border border-slate-300 bg-white px-3 py-1.5 text-xs font-medium text-slate-800 focus:border-indigo-500 focus:outline-none shadow-2xs"
                  />
                </div>

                <button
                  type="button"
                  onClick={handleEvaluate}
                  disabled={isLoading}
                  className="mt-5 inline-flex items-center gap-1.5 rounded-xl bg-slate-800 px-3.5 py-2 text-xs font-semibold text-white hover:bg-slate-900 disabled:opacity-50 transition cursor-pointer"
                >
                  <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
                  {isLoading ? 'Đang tính...' : 'Đánh giá'}
                </button>
              </div>
            </div>
          </div>

          {/* Results Display */}
          {result && (
            <div className="space-y-4">
              {/* Summary Alert Banner */}
              {result.chainOnTimeDueToSlack ? (
                <div className="flex items-center gap-3 rounded-xl bg-emerald-50 p-4 border border-emerald-200 text-emerald-800">
                  <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0" />
                  <div>
                    <h5 className="text-xs font-bold">{result.summaryMessage}</h5>
                    <p className="text-[11px] text-emerald-700">
                      Công việc trễ {result.slipDays} ngày, tuy nhiên tất cả công việc phụ thuộc phía sau đều có thời gian dự phòng (Slack time) đủ để bù đắp. Chuỗi tiến độ tổng thể không bị ảnh hưởng.
                    </p>
                  </div>
                </div>
              ) : result.slipDays > 0 ? (
                <div className="flex items-center gap-3 rounded-xl bg-rose-50 p-4 border border-rose-200 text-rose-800">
                  <AlertTriangle className="h-5 w-5 text-rose-600 shrink-0" />
                  <div>
                    <h5 className="text-xs font-bold">Cảnh báo lan truyền trễ dây chuyền ({result.slipDays} ngày)</h5>
                    <p className="text-[11px] text-rose-700">{result.summaryMessage}</p>
                  </div>
                </div>
              ) : (
                <div className="flex items-center gap-3 rounded-xl bg-indigo-50 p-4 border border-indigo-200 text-indigo-800">
                  <CheckCircle2 className="h-5 w-5 text-indigo-600 shrink-0" />
                  <div>
                    <h5 className="text-xs font-bold">Công việc đúng hạn hoặc hoàn thành sớm</h5>
                    <p className="text-[11px] text-indigo-700">{result.summaryMessage}</p>
                  </div>
                </div>
              )}

              {/* Grid 2 cột: Danh sách Tasks bị ảnh hưởng & Milestones bị ảnh hưởng */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Affected Tasks */}
                <div className="rounded-xl border border-slate-200 bg-white p-4">
                  <div className="flex items-center gap-2 mb-3 border-b border-slate-100 pb-2">
                    <Layers className="h-4 w-4 text-indigo-600" />
                    <h5 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                      Công việc phía sau ({result.affectedTasks.length})
                    </h5>
                  </div>

                  {result.affectedTasks.length === 0 ? (
                    <div className="py-6 text-center text-xs text-slate-400">
                      Không có công việc phụ thuộc nào phía sau.
                    </div>
                  ) : (
                    <div className="space-y-2.5 max-h-56 overflow-y-auto pr-1">
                      {result.affectedTasks.map((t) => (
                        <div
                          key={t.taskId}
                          className={`p-3 rounded-lg border text-xs transition ${
                            t.protectedBySlack
                              ? 'bg-emerald-50/50 border-emerald-200'
                              : 'bg-rose-50/50 border-rose-200'
                          }`}
                        >
                          <div className="flex items-center justify-between font-medium mb-1">
                            <span className="font-bold text-slate-800 truncate">
                              {t.taskCode ? `[${t.taskCode}] ` : ''}{t.taskName}
                            </span>
                            {t.protectedBySlack ? (
                              <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-bold text-emerald-700 shrink-0">
                                An toàn (Slack: {t.slackDays}d)
                              </span>
                            ) : (
                              <span className="rounded-full bg-rose-100 px-2 py-0.5 text-[10px] font-bold text-rose-700 shrink-0">
                                Trễ +{t.delayDays}d
                              </span>
                            )}
                          </div>
                          <div className="flex items-center justify-between text-[11px] text-slate-500">
                            <span>Hạn ban đầu: {t.originalDueDate || 'N/A'}</span>
                            <ArrowRight className="h-3 w-3 text-slate-400" />
                            <span>Mới: <strong className="text-slate-800">{t.newCalculatedEndDate}</strong></span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Affected Milestones */}
                <div className="rounded-xl border border-slate-200 bg-white p-4">
                  <div className="flex items-center gap-2 mb-3 border-b border-slate-100 pb-2">
                    <Flag className="h-4 w-4 text-purple-600" />
                    <h5 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                      Mốc tiến độ bị ảnh hưởng ({result.affectedMilestones.length})
                    </h5>
                  </div>

                  {result.affectedMilestones.length === 0 ? (
                    <div className="py-6 text-center text-xs text-slate-400">
                      Không có mốc tiến độ nào liên kết bị ảnh hưởng.
                    </div>
                  ) : (
                    <div className="space-y-2.5 max-h-56 overflow-y-auto pr-1">
                      {result.affectedMilestones.map((m) => (
                        <div
                          key={m.milestoneId}
                          className={`p-3 rounded-lg border text-xs transition ${
                            m.protectedBySlack
                              ? 'bg-emerald-50/50 border-emerald-200'
                              : 'bg-purple-50/50 border-purple-200'
                          }`}
                        >
                          <div className="flex items-center justify-between font-medium mb-1">
                            <span className="font-bold text-slate-800 truncate">{m.milestoneName}</span>
                            {m.protectedBySlack ? (
                              <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-bold text-emerald-700 shrink-0">
                                An toàn
                              </span>
                            ) : (
                              <span className="rounded-full bg-purple-100 px-2 py-0.5 text-[10px] font-bold text-purple-700 shrink-0">
                                Lùi +{m.delayDays}d
                              </span>
                            )}
                          </div>
                          <div className="flex items-center justify-between text-[11px] text-slate-500">
                            <span>Kế hoạch: {m.plannedDate}</span>
                            <ArrowRight className="h-3 w-3 text-slate-400" />
                            <span>Ước tính mới: <strong className="text-slate-800">{m.newCalculatedDate}</strong></span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between border-t border-slate-200 bg-slate-50 px-5 py-3.5">
          <div className="text-[11px] text-slate-500 flex items-center gap-1.5">
            <Clock className="h-3.5 w-3.5 text-slate-400" />
            <span>Thao tác sẽ tự động ghi lịch sử nhật ký kiểm toán (AuditLog)</span>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition cursor-pointer"
            >
              Đóng
            </button>

            {canManage && result && (
              <button
                type="button"
                onClick={handleConfirmUpdate}
                disabled={isSubmitting}
                className="inline-flex items-center gap-1.5 rounded-xl bg-amber-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-amber-200 hover:bg-amber-700 disabled:opacity-50 transition active:scale-95 cursor-pointer"
              >
                <FileCheck className="h-4 w-4" />
                {isSubmitting ? 'Đang lưu...' : 'Xác nhận cập nhật & Lưu lịch sử'}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
