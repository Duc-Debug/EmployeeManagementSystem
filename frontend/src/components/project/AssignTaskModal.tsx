import React, { useState, useEffect } from 'react';
import { X, Search, Calendar, UserCheck, Users, AlertCircle, AlertTriangle, Loader2 } from 'lucide-react';
import { assignTask, type TaskAssignmentResult } from '@/lib/api/projects';
import type { ProjectMember, TaskItem } from './projectData';

interface AssignTaskModalProps {
    open: boolean;
    task: TaskItem | null;
    projectId: number | null;
    employees: ProjectMember[];
    onClose: () => void;
    onSuccess: (result: TaskAssignmentResult) => void;
}

export function AssignTaskModal({
    open,
    task,
    projectId,
    employees,
    onClose,
    onSuccess,
}: AssignTaskModalProps) {
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedEmployeeIds, setSelectedEmployeeIds] = useState<number[]>([]);
    const [plannedStartDate, setPlannedStartDate] = useState('');
    const [plannedEndDate, setPlannedEndDate] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    useEffect(() => {
        if (open && task) {
            setSearchTerm('');
            setErrorMessage(null);
            
            const initialIds: number[] = [];
            if (task.assigneeIds && task.assigneeIds.length > 0) {
                task.assigneeIds.forEach((idStr) => {
                    const num = parseInt(idStr.replace(/\D/g, ''), 10);
                    if (!isNaN(num) && !initialIds.includes(num)) {
                        initialIds.push(num);
                    }
                });
            } else if (task.assigneeId) {
                const num = parseInt(task.assigneeId.replace(/\D/g, ''), 10);
                if (!isNaN(num)) {
                    initialIds.push(num);
                }
            }
            setSelectedEmployeeIds(initialIds);

            setPlannedStartDate(task.plannedStartDate || '');
            setPlannedEndDate(task.plannedEndDate || '');
        }
    }, [open, task]);

    if (!open || !task) return null;

    // Phát hiện các nhân sự đang được chọn nhưng bị quá hạn hợp đồng trước ngày bắt đầu công việc
    const expiredSelectedEmployees = employees.filter((emp) => {
        const empId = emp.employeeId ?? parseInt(emp.id.replace(/\D/g, ''), 10);
        if (!selectedEmployeeIds.includes(empId)) return false;
        if (emp.status && emp.status !== 'ACTIVE') return true;
        if (plannedStartDate && emp.contractEndDate && emp.contractEndDate < plannedStartDate) {
            return true;
        }
        return false;
    });

    const filteredEmployees = employees.filter((emp) => {
        // 0. Loại trừ Quản trị viên hệ thống (VT-06) - Chỉ nhân sự thực thi dự án mới được giao việc
        if (emp.role === 'VT-06' || emp.name?.toLowerCase().includes('quản trị viên') || emp.employeeId === 1) {
            return false;
        }

        // 1. Không giao việc cho nhân sự đã nghỉ việc
        if (emp.status && emp.status !== 'ACTIVE') {
            return false;
        }

        // 2. Nhân sự có ngày kết thúc hợp đồng trước ngày bắt đầu công việc thì ẩn khỏi danh sách chọn
        if (plannedStartDate && emp.contractEndDate) {
            if (emp.contractEndDate < plannedStartDate) {
                return false;
            }
        }

        const query = searchTerm.toLowerCase().trim();
        if (!query) return true;
        return (
            emp.name.toLowerCase().includes(query) ||
            emp.role.toLowerCase().includes(query) ||
            (emp.employeeId && String(emp.employeeId).includes(query))
        );
    });

    const toggleEmployee = (empId: number) => {
        setSelectedEmployeeIds((prev) => {
            if (prev.includes(empId)) {
                return prev.filter((id) => id !== empId);
            } else {
                return [...prev, empId];
            }
        });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!projectId) {
            setErrorMessage('Không xác định được mã dự án.');
            return;
        }

        if (plannedStartDate && plannedEndDate && plannedStartDate > plannedEndDate) {
            setErrorMessage('Ngày bắt đầu không được sau ngày kết thúc.');
            return;
        }

        if (expiredSelectedEmployees.length > 0) {
            setErrorMessage(`Không thể phân công: Nhân sự [${expiredSelectedEmployees.map(e => e.name).join(', ')}] đã hết hạn hợp đồng trước ngày bắt đầu công việc.`);
            return;
        }

        setIsSubmitting(true);
        setErrorMessage(null);

        try {
            const taskIdNum = parseInt(task.id.replace(/\D/g, ''), 10);
            const result = await assignTask(projectId, isNaN(taskIdNum) ? task.id : taskIdNum, {
                employeeIds: selectedEmployeeIds,
                plannedStartDate: plannedStartDate || undefined,
                plannedEndDate: plannedEndDate || undefined,
            });

            onSuccess(result);
            onClose();
        } catch (err: any) {
            console.error('Lỗi khi phân công công việc:', err);
            setErrorMessage(err.message || 'Không thể lưu phân công công việc. Vui lòng thử lại.');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-in fade-in duration-150">
            <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl border border-slate-100 overflow-hidden flex flex-col max-h-[90vh]">
                <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-slate-50/50">
                    <div className="flex items-center gap-2.5">
                        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
                            <Users className="h-5 w-5" />
                        </div>
                        <div>
                            <h3 className="text-sm font-bold text-slate-800">Giao việc cho nhân sự</h3>
                            <p className="text-xs text-slate-500 font-mono">
                                {task.code}: {task.name}
                            </p>
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="flex flex-col flex-1 overflow-hidden">
                    <div className="p-6 space-y-4 overflow-y-auto flex-1">
                        {errorMessage && (
                            <div className="flex items-center gap-2 rounded-lg bg-rose-50 border border-rose-200 p-3 text-xs text-rose-700">
                                <AlertCircle className="h-4 w-4 shrink-0" />
                                <span>{errorMessage}</span>
                            </div>
                        )}

                        {expiredSelectedEmployees.length > 0 && (
                            <div className="flex items-start gap-2 rounded-lg bg-amber-50 border border-amber-200 p-3 text-xs text-amber-800">
                                <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
                                <div>
                                    <span className="font-semibold">Cảnh báo hợp đồng:</span>
                                    <p className="mt-0.5">
                                        Nhân sự <strong>{expiredSelectedEmployees.map(e => e.name).join(', ')}</strong> đã hết hạn hợp đồng trước ngày bắt đầu công việc. Vui lòng bỏ chọn nhân sự này hoặc chọn ngày bắt đầu sớm hơn.
                                    </p>
                                </div>
                            </div>
                        )}

                        <div className="grid grid-cols-2 gap-3">
                            <div>
                                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                                    <Calendar className="inline h-3.5 w-3.5 mr-1 text-slate-400" />
                                    Ngày bắt đầu mong muốn
                                </label>
                                <input
                                    type="date"
                                    value={plannedStartDate}
                                    onChange={(e) => setPlannedStartDate(e.target.value)}
                                    className="w-full rounded-lg border border-slate-200 px-3 py-2 text-xs text-slate-800 focus:border-indigo-500 focus:outline-hidden focus:ring-1 focus:ring-indigo-500"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                                    <Calendar className="inline h-3.5 w-3.5 mr-1 text-slate-400" />
                                    Ngày kết thúc mong muốn
                                </label>
                                <input
                                    type="date"
                                    value={plannedEndDate}
                                    onChange={(e) => setPlannedEndDate(e.target.value)}
                                    className="w-full rounded-lg border border-slate-200 px-3 py-2 text-xs text-slate-800 focus:border-indigo-500 focus:outline-hidden focus:ring-1 focus:ring-indigo-500"
                                />
                            </div>
                        </div>

                        <div>
                            <div className="flex items-center justify-between mb-1.5">
                                <label className="text-xs font-semibold text-slate-700">
                                    Chọn nhân sự thực hiện ({selectedEmployeeIds.length} đã chọn)
                                </label>
                                {selectedEmployeeIds.length > 0 && (
                                    <button
                                        type="button"
                                        onClick={() => setSelectedEmployeeIds([])}
                                        className="text-[11px] text-slate-400 hover:text-rose-600 transition"
                                    >
                                        Bỏ chọn tất cả
                                    </button>
                                )}
                            </div>

                            <div className="relative mb-2.5">
                                <Search className="absolute left-3 top-2.5 h-3.5 w-3.5 text-slate-400" />
                                <input
                                    type="text"
                                    placeholder="Tìm theo tên hoặc chức danh..."
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                    className="w-full rounded-lg border border-slate-200 pl-8 pr-3 py-2 text-xs text-slate-800 placeholder:text-slate-400 focus:border-indigo-500 focus:outline-hidden focus:ring-1 focus:ring-indigo-500"
                                />
                            </div>

                            <div className="divide-y divide-slate-100 rounded-lg border border-slate-200 max-h-56 overflow-y-auto bg-white">
                                {filteredEmployees.length === 0 ? (
                                    <div className="p-4 text-center text-xs text-slate-400 italic">
                                        Không tìm thấy nhân sự phù hợp (đã ẩn nhân sự nghỉ việc hoặc hết hạn hợp đồng).
                                    </div>
                                ) : (
                                    filteredEmployees.map((emp) => {
                                        const empId = emp.employeeId ?? parseInt(emp.id.replace(/\D/g, ''), 10);
                                        const isSelected = selectedEmployeeIds.includes(empId);
                                        const isPrimary = selectedEmployeeIds.length > 0 && selectedEmployeeIds[0] === empId;

                                        // Cảnh báo nếu hợp đồng vắt qua giai đoạn thực hiện task
                                        const isContractEndingDuringTask = Boolean(
                                            emp.contractEndDate &&
                                            plannedStartDate &&
                                            plannedEndDate &&
                                            emp.contractEndDate >= plannedStartDate &&
                                            emp.contractEndDate <= plannedEndDate
                                        );

                                        return (
                                            <div
                                                key={emp.id}
                                                onClick={() => toggleEmployee(empId)}
                                                className={`flex items-center justify-between px-3 py-2.5 text-xs cursor-pointer transition ${
                                                    isSelected ? 'bg-indigo-50/50 hover:bg-indigo-50' : 'hover:bg-slate-50'
                                                }`}
                                            >
                                                <div className="flex items-center gap-2.5 min-w-0">
                                                    <input
                                                        type="checkbox"
                                                        checked={isSelected}
                                                        onChange={() => {}}
                                                        className="h-3.5 w-3.5 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
                                                    />
                                                    <div className="min-w-0">
                                                        <div className="flex items-center gap-1.5 flex-wrap">
                                                            <span className="font-medium text-slate-800 truncate">
                                                                {emp.name}
                                                            </span>
                                                            {isPrimary && (
                                                                <span className="rounded bg-indigo-100 px-1.5 py-0.2 text-[9px] font-bold text-indigo-700">
                                                                    Chịu trách nhiệm chính
                                                                </span>
                                                            )}
                                                            {isContractEndingDuringTask && (
                                                                <span
                                                                    className="inline-flex items-center gap-1 rounded bg-amber-50 border border-amber-200 px-1.5 py-0.2 text-[9px] font-medium text-amber-700"
                                                                    title={`Hợp đồng hết hạn vào ngày ${emp.contractEndDate}, vắt qua thời gian làm công việc`}
                                                                >
                                                                    <AlertTriangle className="h-2.5 w-2.5 text-amber-500 shrink-0" />
                                                                    HĐ hết hạn: {emp.contractEndDate}
                                                                </span>
                                                            )}
                                                        </div>
                                                        <p className="text-[11px] text-slate-500 truncate">
                                                            {emp.role}
                                                            {emp.contractEndDate && !isContractEndingDuringTask && (
                                                                <span className="text-slate-400 ml-1.5">
                                                                    (HĐ đến: {emp.contractEndDate})
                                                                </span>
                                                            )}
                                                        </p>
                                                    </div>
                                                </div>

                                                {isSelected && (
                                                    <UserCheck className="h-4 w-4 text-indigo-600 shrink-0" />
                                                )}
                                            </div>
                                        );
                                    })
                                )}
                            </div>
                            <p className="mt-1 text-[11px] text-slate-400 italic">
                                * Người được chọn đầu tiên sẽ là người chịu trách nhiệm chính (Primary Assignee). Nếu nhân sự chưa thuộc dự án, hệ thống sẽ tự động thêm vào dự án.
                            </p>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-2 border-t border-slate-100 bg-slate-50/50 px-6 py-3.5">
                        <button
                            type="button"
                            onClick={onClose}
                            disabled={isSubmitting}
                            className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 transition"
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            disabled={isSubmitting}
                            className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-xs hover:bg-indigo-700 transition disabled:opacity-50 cursor-pointer"
                        >
                            {isSubmitting ? (
                                <>
                                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                                    Đang lưu...
                                </>
                            ) : (
                                'Xác nhận phân công'
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
