import { useState, useEffect } from "react";
import { X, CheckCircle2, UserCheck, AlertCircle, FileText } from "lucide-react";
import { resolveScheduleConflictWithNote } from "@/lib/api/schedule-conflict";
import type { ScheduleConflict } from "@/lib/api/schedule-conflict";
import { getEmployees } from "@/lib/api/employees";
import type { EmployeeProfile } from "@/lib/api/employees";

interface ConflictResolutionModalProps {
    conflict: ScheduleConflict;
    isOpen: boolean;
    onClose: () => void;
    onSuccess: (resolvedConflict: ScheduleConflict) => void | Promise<void>;
}

export default function ConflictResolutionModal({
    conflict,
    isOpen,
    onClose,
    onSuccess,
}: ConflictResolutionModalProps) {
    const [assignedHandlerId, setAssignedHandlerId] = useState<string>(
        conflict.assignedHandlerId ? String(conflict.assignedHandlerId) : ""
    );
    const [resolutionNote, setResolutionNote] = useState<string>(conflict.resolutionNote || "");
    const [employees, setEmployees] = useState<EmployeeProfile[]>([]);
    const [loadingEmployees, setLoadingEmployees] = useState<boolean>(false);
    const [submitting, setSubmitting] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (conflict && isOpen) {
            setAssignedHandlerId(conflict.assignedHandlerId ? String(conflict.assignedHandlerId) : "");
            setResolutionNote(conflict.resolutionNote || "");
            setError(null);
            loadEmployeeList();
        }
    }, [conflict, isOpen]);

    const loadEmployeeList = async () => {
        try {
            setLoadingEmployees(true);
            const res = await getEmployees(1, 100);
            if (res && res.content) {
                setEmployees(res.content);
            }
        } catch (err) {
            console.error("Lỗi khi tải danh sách nhân viên:", err);
        } finally {
            setLoadingEmployees(false);
        }
    };

    if (!isOpen || !conflict) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!resolutionNote.trim()) {
            setError("Vui lòng ghi nhận cách xử lý xung đột.");
            return;
        }

        setSubmitting(true);
        setError(null);

        try {
            const handlerIdNum = assignedHandlerId ? Number(assignedHandlerId) : undefined;
            const resolvedConflict = await resolveScheduleConflictWithNote(conflict.id, {
                assignedHandlerId: handlerIdNum,
                resolutionNote: resolutionNote.trim(),
            });
            await onSuccess(resolvedConflict);
            onClose();
        } catch (err: any) {
            console.error("Lỗi khi xử lý xung đột:", err);
            setError(err.message || "Không thể lưu thông tin xử lý xung đột. Vui lòng thử lại.");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-in fade-in duration-200">
            <div className="w-full max-w-lg rounded-3xl bg-white p-6 shadow-xl border border-slate-200">
                {/* Modal Header */}
                <div className="flex items-center justify-between border-b border-slate-100 pb-4 mb-4">
                    <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600 border border-emerald-100">
                            <CheckCircle2 className="h-5 w-5" />
                        </div>
                        <div>
                            <h2 className="text-base font-bold text-slate-900">Ghi nhận Xử lý Xung đột Lịch</h2>
                            <p className="text-xs text-slate-500">Mã xung đột #{conflict.id} • {conflict.employeeName}</p>
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        className="rounded-xl p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                {/* Error Banner */}
                {error && (
                    <div className="mb-4 flex items-center gap-2 rounded-2xl bg-rose-50 p-3 border border-rose-200 text-xs text-rose-700">
                        <AlertCircle className="h-4 w-4 shrink-0 text-rose-600" />
                        <span>{error}</span>
                    </div>
                )}

                {/* Conflict Overview Card */}
                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 mb-4 text-xs space-y-2">
                    <div className="flex justify-between text-slate-700 font-medium">
                        <span>Nhân sự:</span>
                        <span className="font-bold text-slate-900">{conflict.employeeName} ({conflict.employeeCode})</span>
                    </div>
                    <div className="flex justify-between text-slate-700">
                        <span>Thời gian:</span>
                        <span className="font-semibold">{conflict.weekLabel}</span>
                    </div>
                    <div className="flex justify-between text-slate-700">
                        <span>Loại xung đột:</span>
                        <span className="font-semibold text-sky-700">{conflict.conflictTypeLabel}</span>
                    </div>
                    {conflict.projectNames && (
                        <div className="flex justify-between text-slate-700">
                            <span>Dự án:</span>
                            <span className="font-semibold text-slate-800">{conflict.projectNames}</span>
                        </div>
                    )}
                    <div className="flex justify-between text-slate-700">
                        <span>Giờ quá tải/vượt:</span>
                        <span className="font-bold text-rose-600">+{conflict.excessHours}h</span>
                    </div>
                </div>

                {/* Form Inputs */}
                <form onSubmit={handleSubmit} className="space-y-4 text-xs">
                    {/* Assigned Handler ID Select */}
                    <div>
                        <label className="block font-semibold text-slate-700 mb-1 flex items-center gap-1.5">
                            <UserCheck className="h-3.5 w-3.5 text-indigo-600" />
                            <span>Người xử lý / Người chịu trách nhiệm:</span>
                        </label>
                        <select
                            value={assignedHandlerId}
                            onChange={(e) => setAssignedHandlerId(e.target.value)}
                            disabled={loadingEmployees}
                            className="w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-xs text-slate-800 focus:border-indigo-500 focus:outline-none transition disabled:opacity-50"
                        >
                            <option value="">-- Chưa gán người xử lý --</option>
                            {employees.map((emp) => (
                                <option key={emp.id} value={emp.id}>
                                    {emp.fullName} ({emp.employeeCode}) - {emp.orgUnitName || "Chưa phân phòng"}
                                </option>
                            ))}
                        </select>
                        <p className="text-[11px] text-slate-400 mt-1">
                            Người chịu trách nhiệm theo dõi và giải quyết dứt điểm xung đột này.
                        </p>
                    </div>

                    {/* Resolution Note */}
                    <div>
                        <label className="block font-semibold text-slate-700 mb-1 flex items-center gap-1.5">
                            <FileText className="h-3.5 w-3.5 text-indigo-600" />
                            <span>Ghi cách xử lý (Bắt buộc):</span>
                        </label>
                        <textarea
                            rows={3}
                            placeholder="Mô tả phương án/thỏa thuận đã thực hiện để giải quyết xung đột (ví dụ: Đã giảm 10h dự án X, dời đơn nghỉ phép sang tuần sau...)"
                            value={resolutionNote}
                            onChange={(e) => setResolutionNote(e.target.value)}
                            className="w-full rounded-xl border border-slate-200 bg-white p-3 text-xs text-slate-800 placeholder-slate-400 focus:border-indigo-500 focus:outline-none transition"
                            required
                        />
                    </div>

                    {/* Modal Footer Actions */}
                    <div className="flex items-center justify-end gap-3 border-t border-slate-100 pt-4 mt-6">
                        <button
                            type="button"
                            onClick={onClose}
                            className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            disabled={submitting}
                            className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-5 py-2 text-xs font-semibold text-white hover:bg-emerald-700 transition shadow-xs disabled:opacity-50"
                        >
                            <CheckCircle2 className="h-4 w-4" />
                            <span>{submitting ? "Đang lưu..." : "Xác nhận đã xử lý"}</span>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
