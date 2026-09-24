import React, { useState, useEffect } from "react";
import {
  CheckCircle2,
  XCircle,
  Loader2,
  AlertCircle,
  Briefcase,
  CalendarDays,
  Clock,
} from "lucide-react";
import type { WorkLogResult } from "../../lib/api/work-logs";
import { TimesheetBudgetWarning } from "./TimesheetBudgetWarning";
import {
  getPendingApprovals,
  approveTimesheetEntry,
  rejectTimesheetEntry,
} from "../../lib/api/timesheet-approvals";

export const TimesheetApprovalView: React.FC = () => {
  const [entries, setEntries] = useState<WorkLogResult[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const [isProcessing, setIsProcessing] = useState<number | null>(null);
  
  const [rejectId, setRejectId] = useState<number | null>(null);
  const [rejectionReason, setRejectionReason] = useState("");

  const loadData = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await getPendingApprovals();
      setEntries(data);
    } catch (err: any) {
      setError(err.message || "Lỗi khi tải danh sách chờ duyệt");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleApprove = async (id: number, version: number) => {
    try {
      setIsProcessing(id);
      const res = await approveTimesheetEntry(id, version);
      setWarnings(res.warnings || []);
      await loadData();
    } catch (err: any) {
      setError(err.message || "Không thể duyệt giờ công");
    } finally {
      setIsProcessing(null);
    }
  };

  const handleReject = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!rejectId || !rejectionReason.trim()) return;
    
    // Find the version of the entry being rejected
    const entryToReject = entries.find(e => e.id === rejectId);
    if (!entryToReject) return;

    try {
      setIsProcessing(rejectId);
      await rejectTimesheetEntry(rejectId, entryToReject.version, rejectionReason);
      await loadData();
      setRejectId(null);
      setRejectionReason("");
    } catch (err: any) {
      setError("Lỗi từ chối: " + (err.message || "Không xác định"));
    } finally {
      setIsProcessing(null);
    }
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">
            Duyệt giờ công
          </h1>
          <p className="mt-1 text-sm font-medium text-slate-500">
            Quản lý và phê duyệt giờ làm việc của thành viên dự án
          </p>
        </div>
      </div>

      {warnings.length > 0 && (
        <div role="status" className="mb-4 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
          {warnings.join(" ")}
        </div>
      )}
      {error && (
        <div className="mb-6 flex items-center gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-800">
          <AlertCircle className="h-5 w-5 shrink-0 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {isLoading ? (
        <div className="flex h-64 flex-col items-center justify-center rounded-2xl border border-slate-200 bg-white shadow-xs">
          <Loader2 className="h-8 w-8 animate-spin text-indigo-600 mb-4" />
          <p className="text-sm font-bold text-slate-600">Đang tải danh sách chờ duyệt...</p>
        </div>
      ) : entries.length === 0 ? (
        <div className="flex h-64 flex-col items-center justify-center rounded-2xl border border-slate-200 bg-white shadow-xs">
          <CheckCircle2 className="h-12 w-12 text-emerald-400 mb-4" />
          <p className="text-lg font-bold text-slate-700">Tất cả đã được duyệt</p>
          <p className="text-sm font-medium text-slate-500 mt-1">
            Không có dòng giờ công nào đang chờ duyệt.
          </p>
        </div>
      ) : (
        <div className="rounded-2xl border border-slate-200 bg-white shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-6 py-4 text-left text-xs font-bold uppercase tracking-wider text-slate-500">
                    Nhân viên
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-bold uppercase tracking-wider text-slate-500">
                    Dự án / Task
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-bold uppercase tracking-wider text-slate-500">
                    Ngày làm việc
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-bold uppercase tracking-wider text-slate-500">
                    Thời gian
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-bold uppercase tracking-wider text-slate-500">
                    Mô tả
                  </th>
                  <th className="px-6 py-4 text-right text-xs font-bold uppercase tracking-wider text-slate-500">
                    Thao tác
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white">
                {entries.map((entry) => (
                  <tr key={entry.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-indigo-100 text-indigo-700 font-bold">
                          {entry.employeeName ? entry.employeeName.charAt(0) : "?"}
                        </div>
                        <div>
                          <div className="text-sm font-bold text-slate-900">
                            {entry.employeeName}
                          </div>
                          <div className="text-xs text-slate-500">ID: {entry.employeeId}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="text-sm font-bold text-slate-900 line-clamp-1">
                        {entry.projectName}
                      </div>
                      <div className="mt-1 flex items-center gap-1.5 text-xs font-medium text-slate-500">
                        <Briefcase className="h-3.5 w-3.5 shrink-0" />
                        <span className="line-clamp-1">{entry.taskName}</span>
                      </div>
                      <TimesheetBudgetWarning entry={entry} />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-2 text-sm font-bold text-slate-700">
                        <CalendarDays className="h-4 w-4 text-slate-400" />
                        <span>{entry.workDate}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-2 text-sm font-black text-indigo-700 bg-indigo-50 px-3 py-1 rounded-lg w-max">
                        <Clock className="h-4 w-4" />
                        <span>{entry.hours}h</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <p className="text-sm font-medium text-slate-600 line-clamp-2 max-w-xs">
                        {entry.description}
                      </p>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => handleApprove(entry.id, entry.version)}
                          disabled={isProcessing !== null}
                          className="flex items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 py-2 text-sm font-bold text-white shadow-sm transition-all hover:bg-emerald-700 focus:ring-4 focus:ring-emerald-600/20 disabled:opacity-50"
                        >
                          {isProcessing === entry.id ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                          ) : (
                            <CheckCircle2 className="h-4 w-4" />
                          )}
                          Duyệt
                        </button>
                        <button
                          onClick={() => setRejectId(entry.id)}
                          disabled={isProcessing !== null}
                          className="flex items-center justify-center gap-2 rounded-xl bg-white border border-rose-200 px-4 py-2 text-sm font-bold text-rose-700 shadow-sm transition-all hover:bg-rose-50 hover:border-rose-300 focus:ring-4 focus:ring-rose-600/20 disabled:opacity-50"
                        >
                          Từ chối
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Reject Modal */}
      {rejectId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
          <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-start gap-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-rose-100 text-rose-600">
                <XCircle className="h-6 w-6" />
              </div>
              <div>
                <h3 className="text-lg font-black text-slate-900">Từ chối giờ công</h3>
                <p className="mt-1 text-sm font-medium text-slate-500">
                  Vui lòng nhập lý do từ chối để nhân viên có thể sửa lại.
                </p>
              </div>
            </div>

            <form onSubmit={handleReject}>
              <textarea
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                className="w-full rounded-2xl border-slate-200 bg-slate-50 p-4 text-sm font-medium text-slate-900 focus:border-rose-500 focus:ring-4 focus:ring-rose-500/20"
                rows={4}
                placeholder="Ví dụ: Mô tả chưa rõ ràng, số giờ không hợp lý..."
                required
              />
              
              <div className="mt-6 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => {
                    setRejectId(null);
                    setRejectionReason("");
                  }}
                  className="rounded-xl px-5 py-2.5 text-sm font-bold text-slate-600 hover:bg-slate-100"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  disabled={!rejectionReason.trim() || isProcessing === rejectId}
                  className="flex items-center gap-2 rounded-xl bg-rose-600 px-5 py-2.5 text-sm font-bold text-white shadow-sm hover:bg-rose-700 focus:ring-4 focus:ring-rose-600/20 disabled:opacity-50"
                >
                  {isProcessing === rejectId && <Loader2 className="h-4 w-4 animate-spin" />}
                  Xác nhận từ chối
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
