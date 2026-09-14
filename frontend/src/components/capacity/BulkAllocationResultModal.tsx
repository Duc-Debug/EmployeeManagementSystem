/**
 * NCL-06-CN-006: Modal hiển thị kết quả phân bổ hàng loạt nhiều tuần.
 * Báo cáo chi tiết số tuần thành công và các tuần bị chặn kèm lý do vi phạm ràng buộc (TC-01, TC-02).
 */
import { X, CheckCircle2, AlertTriangle, AlertCircle } from 'lucide-react';
import type { BulkAllocationResult } from '@/lib/api/allocations';

interface BulkAllocationResultModalProps {
  open: boolean;
  result: BulkAllocationResult | null;
  onClose: () => void;
}

export function BulkAllocationResultModal({
  open,
  result,
  onClose,
}: BulkAllocationResultModalProps) {
  if (!open || !result) return null;

  const isAllSuccess = result.blockedCount === 0;
  const isAllBlocked = result.successCount === 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in">
      <div className="w-full max-w-2xl overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 p-4">
          <div className="flex items-center gap-2.5">
            <span
              className={`flex h-8 w-8 items-center justify-center rounded-lg ${
                isAllSuccess
                  ? 'bg-emerald-100 text-emerald-700'
                  : isAllBlocked
                  ? 'bg-rose-100 text-rose-700'
                  : 'bg-amber-100 text-amber-700'
              }`}
            >
              {isAllSuccess ? (
                <CheckCircle2 className="h-5 w-5" />
              ) : (
                <AlertTriangle className="h-5 w-5" />
              )}
            </span>
            <div>
              <h3 className="text-sm font-bold text-slate-800">
                Kết Quả Phân Bổ Nguồn Lực Hàng Loạt
              </h3>
              <p className="text-[11px] text-slate-500">
                Tổng số tuần yêu cầu: <strong>{result.totalRequestedWeeks}</strong> tuần
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Overview Stats */}
        <div className="p-5 space-y-4 max-h-[70vh] overflow-y-auto">
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-3 text-center">
              <div className="text-[11px] font-medium text-slate-500">Tổng tuần</div>
              <div className="mt-1 text-lg font-bold text-slate-800">
                {result.totalRequestedWeeks}
              </div>
            </div>
            <div className="rounded-xl border border-emerald-200 bg-emerald-50/70 p-3 text-center">
              <div className="text-[11px] font-medium text-emerald-700">Thành công</div>
              <div className="mt-1 text-lg font-bold text-emerald-800">
                {result.successCount}
              </div>
            </div>
            <div
              className={`rounded-xl border p-3 text-center col-span-2 sm:col-span-1 ${
                result.blockedCount > 0
                  ? 'border-rose-200 bg-rose-50/70 text-rose-800'
                  : 'border-slate-200 bg-slate-50/70 text-slate-400'
              }`}
            >
              <div className="text-[11px] font-medium">Bị chặn / Lỗi</div>
              <div className="mt-1 text-lg font-bold">
                {result.blockedCount}
              </div>
            </div>
          </div>

          {/* Blocked Weeks Details (TC-02) */}
          {result.blockedWeeks && result.blockedWeeks.length > 0 && (
            <div className="space-y-2">
              <div className="flex items-center gap-1.5 text-xs font-bold text-rose-800">
                <AlertCircle className="h-4 w-4 text-rose-600 shrink-0" />
                <span>Danh sách {result.blockedCount} tuần bị chặn kèm lý do:</span>
              </div>
              <div className="divide-y divide-rose-100 rounded-xl border border-rose-200 bg-rose-50/40 overflow-hidden text-xs">
                {result.blockedWeeks.map((bw) => (
                  <div key={`${bw.year}-${bw.weekNumber}`} className="p-3">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-rose-900">
                        Tuần {bw.weekNumber} / Năm {bw.year}
                      </span>
                      <span className="rounded-md bg-rose-200/80 px-2 py-0.5 text-[10px] font-bold text-rose-800">
                        {bw.reasonCode === 'CAPACITY_EXCEEDED'
                          ? 'Vượt khả dụng'
                          : bw.reasonCode === 'CONTRACT_EXPIRED'
                          ? 'Hết hạn HĐ'
                          : 'Bị chặn'}
                      </span>
                    </div>
                    <p className="mt-1 text-[11px] text-rose-700 leading-relaxed">
                      {bw.reasonMessage}
                    </p>
                    {bw.reasonCode === 'CAPACITY_EXCEEDED' && (
                      <div className="mt-1.5 flex flex-wrap items-center gap-3 text-[10px] text-slate-600 bg-white/70 rounded-md p-1.5 border border-rose-100">
                        <span>Giờ khả dụng: <strong>{bw.netAvailableHours}h</strong></span>
                        <span>Đã giao dự án khác: <strong>{bw.currentAllocatedHours}h</strong></span>
                        <span>Yêu cầu thêm: <strong>{bw.requestedHours}h</strong></span>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Success Weeks Details (TC-01) */}
          {result.successWeeks && result.successWeeks.length > 0 && (
            <div className="space-y-2">
              <div className="flex items-center gap-1.5 text-xs font-bold text-emerald-800">
                <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
                <span>Danh sách {result.successCount} tuần phân bổ thành công:</span>
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                {result.successWeeks.map((sw) => (
                  <div
                    key={`${sw.year}-${sw.weekNumber}`}
                    className="flex flex-col rounded-lg border border-emerald-200 bg-emerald-50/50 p-2 text-center"
                  >
                    <span className="text-[11px] font-bold text-emerald-900">
                      T{sw.weekNumber}/{sw.year}
                    </span>
                    <span className="mt-0.5 text-[11px] font-medium text-emerald-700">
                      +{sw.allocatedHours}h
                    </span>
                    <span className="text-[9px] text-slate-500">
                      (còn trống {sw.remainingHours}h)
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end border-t border-slate-200 bg-slate-50 px-5 py-3">
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg bg-indigo-600 px-4 py-2 text-xs font-medium text-white shadow-xs hover:bg-indigo-700 transition"
          >
            Đã hiểu & Đóng
          </button>
        </div>
      </div>
    </div>
  );
}
