import { useState, useEffect, useMemo, useCallback } from "react";
import {
    AlertTriangle,
    RefreshCw,
    Send,
    CheckCircle2,
    Search,
    Filter,
    Calendar,
    Briefcase,
    CalendarX,
    UserCheck,
    Clock,
    ShieldAlert,
    RotateCcw,
    FileText,
} from "lucide-react";
import {
    getScheduleConflicts,
    scanScheduleConflicts,
    notifyScheduleConflict,
} from "@/lib/api/schedule-conflict";
import type {
    ScheduleConflict,
    ConflictType,
    ScheduleConflictStatus,
} from "@/lib/api/schedule-conflict";

import ReplacementSuggestionModal from "./ReplacementSuggestionModal";
import ConflictResolutionModal from "./ConflictResolutionModal";

export default function ScheduleConflictWarningView() {
    const [conflicts, setConflicts] = useState<ScheduleConflict[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [scanning, setScanning] = useState<boolean>(false);
    const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
    const [errorMsg, setErrorMsg] = useState<string | null>(null);
    const [selectedConflictForReplacement, setSelectedConflictForReplacement] = useState<ScheduleConflict | null>(null);
    const [selectedConflictForResolution, setSelectedConflictForResolution] = useState<ScheduleConflict | null>(null);

    // Filters
    const [searchTerm, setSearchTerm] = useState<string>("");
    const [conflictTypeFilter, setConflictTypeFilter] = useState<string>("ALL");
    const [statusFilter, setStatusFilter] = useState<string>("ALL");
    const [yearFilter, setYearFilter] = useState<number>(new Date().getFullYear());
    const [startWeekFilter, setStartWeekFilter] = useState<number>(37);
    const [endWeekFilter, setEndWeekFilter] = useState<number>(42);

    const loadData = useCallback(async () => {
        setLoading(true);
        setErrorMsg(null);
        try {
            const data = await getScheduleConflicts({
                yearNumber: yearFilter,
                startWeek: startWeekFilter,
                endWeek: endWeekFilter,
                conflictType: conflictTypeFilter !== "ALL" ? (conflictTypeFilter as ConflictType) : undefined,
                status: statusFilter !== "ALL" ? (statusFilter as ScheduleConflictStatus) : undefined,
            });
            setConflicts(data);
        } catch (err: any) {
            console.error("Lỗi khi tải danh sách xung đột lịch:", err);
            setErrorMsg(err.message || "Không thể tải danh sách xung đột lịch. Vui lòng kiểm tra quyền hạn tài khoản.");
        } finally {
            setLoading(false);
        }
    }, [yearFilter, startWeekFilter, endWeekFilter, conflictTypeFilter, statusFilter]);

    useEffect(() => {
        loadData();
    }, [loadData]);

    const handleScan = async () => {
        setScanning(true);
        setErrorMsg(null);
        try {
            const scannedData = await scanScheduleConflicts(yearFilter, startWeekFilter, endWeekFilter);
            setConflicts(scannedData);
        } catch (err: any) {
            console.error("Lỗi khi rà soát xung đột lịch:", err);
            setErrorMsg(err.message || "Không thể thực thi rà soát xung đột lịch.");
        } finally {
            setScanning(false);
        }
    };

    const handleNotify = async (id: number) => {
        setActionLoadingId(id);
        try {
            const updated = await notifyScheduleConflict(id);
            setConflicts((prev) => prev.map((item) => (item.id === id ? updated : item)));
        } catch (err: any) {
            alert(err.message || "Lỗi khi gửi thông báo xung đột lịch.");
        } finally {
            setActionLoadingId(null);
        }
    };

    // Filtered data based on search text
    const filteredConflicts = useMemo(() => {
        return conflicts.filter((c) => {
            if (!searchTerm.trim()) return true;
            const term = searchTerm.toLowerCase();
            return (
                c.employeeName.toLowerCase().includes(term) ||
                c.employeeCode.toLowerCase().includes(term) ||
                c.departmentName.toLowerCase().includes(term) ||
                (c.projectNames && c.projectNames.toLowerCase().includes(term)) ||
                (c.assignedHandlerName && c.assignedHandlerName.toLowerCase().includes(term)) ||
                (c.resolutionNote && c.resolutionNote.toLowerCase().includes(term)) ||
                (c.details && c.details.toLowerCase().includes(term))
            );
        });
    }, [conflicts, searchTerm]);

    // Statistics
    const stats = useMemo(() => {
        const total = conflicts.length;
        const openOrReopened = conflicts.filter((c) => c.status === "OPEN" || c.status === "NOTIFIED" || c.status === "REOPENED").length;
        const recurrent = conflicts.filter((c) => c.isRecurrent || c.status === "REOPENED").length;
        const resolved = conflicts.filter((c) => c.status === "RESOLVED").length;
        const totalExcessHours = conflicts.reduce((acc, curr) => acc + (curr.excessHours || 0), 0);
        return { total, openOrReopened, recurrent, resolved, totalExcessHours };
    }, [conflicts]);

    return (
        <div className="space-y-6 animate-in fade-in duration-200">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
                <div>
                    <h1 className="text-xl font-bold text-slate-900">Danh sách Xung đột Lịch cần Xử lý (NCL-07-CN-005)</h1>
                    <p className="text-xs text-slate-500 mt-1">
                        Hệ thống gom các xung đột đang mở thành danh sách, gán người chịu trách nhiệm, ghi nhận cách xử lý và theo dõi các xung đột tái phát.
                    </p>
                </div>

                <div className="flex items-center gap-3">
                    <button
                        onClick={handleScan}
                        disabled={scanning || loading}
                        className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs disabled:opacity-50"
                    >
                        <RefreshCw className={`h-4 w-4 ${scanning ? "animate-spin" : ""}`} />
                        <span>{scanning ? "Đang rà soát..." : "Rà soát & Phát hiện xung đột"}</span>
                    </button>
                </div>
            </div>

            {/* Error Message Display */}
            {errorMsg && (
                <div className="flex items-center gap-3 rounded-2xl bg-rose-50 p-4 border border-rose-200 text-rose-700 text-xs shadow-xs">
                    <ShieldAlert className="h-5 w-5 shrink-0 text-rose-600" />
                    <div className="flex-1">
                        <p className="font-bold">Từ chối truy cập / Lỗi hệ thống</p>
                        <p>{errorMsg}</p>
                    </div>
                </div>
            )}

            {/* Stat Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs flex items-center gap-4">
                    <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-amber-50 text-amber-600 border border-amber-100 shrink-0">
                        <AlertTriangle className="h-6 w-6" />
                    </div>
                    <div>
                        <p className="text-xs font-medium text-slate-500">Đang chờ xử lý</p>
                        <p className="text-xl font-bold text-slate-900 mt-0.5">{stats.openOrReopened}</p>
                    </div>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs flex items-center gap-4">
                    <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-rose-50 text-rose-600 border border-rose-100 shrink-0">
                        <RotateCcw className="h-6 w-6" />
                    </div>
                    <div>
                        <p className="text-xs font-medium text-slate-500">Xung đột tái phát</p>
                        <p className="text-xl font-bold text-slate-900 mt-0.5">{stats.recurrent}</p>
                    </div>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs flex items-center gap-4">
                    <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-100 shrink-0">
                        <CheckCircle2 className="h-6 w-6" />
                    </div>
                    <div>
                        <p className="text-xs font-medium text-slate-500">Đã giải quyết</p>
                        <p className="text-xl font-bold text-slate-900 mt-0.5">{stats.resolved}</p>
                    </div>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs flex items-center gap-4">
                    <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100 shrink-0">
                        <Clock className="h-6 w-6" />
                    </div>
                    <div>
                        <p className="text-xs font-medium text-slate-500">Tổng giờ vượt/quá tải</p>
                        <p className="text-xl font-bold text-slate-900 mt-0.5">{stats.totalExcessHours.toFixed(1)}h</p>
                    </div>
                </div>
            </div>

            {/* Filter Bar */}
            <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-xs">
                <div className="flex flex-wrap items-center gap-3 flex-1">
                    {/* Search */}
                    <div className="relative min-w-[220px]">
                        <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                        <input
                            type="text"
                            placeholder="Tìm nhân sự, người xử lý, cách xử lý..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full rounded-xl border border-slate-200 bg-slate-50 pl-9 pr-3 py-2 text-xs text-slate-800 placeholder-slate-400 focus:border-indigo-500 focus:bg-white focus:outline-none transition"
                        />
                    </div>

                    {/* Conflict Type Filter */}
                    <div className="flex items-center gap-2">
                        <Filter className="h-4 w-4 text-slate-400 shrink-0" />
                        <select
                            value={conflictTypeFilter}
                            onChange={(e) => setConflictTypeFilter(e.target.value)}
                            className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-medium text-slate-700 focus:border-indigo-500 focus:outline-none"
                        >
                            <option value="ALL">Tất cả loại xung đột</option>
                            <option value="MULTI_PROJECT_ALLOCATION">Trùng nhiều dự án</option>
                            <option value="LEAVE_ALLOCATION_CONFLICT">Trùng nghỉ phép đã duyệt</option>
                        </select>
                    </div>

                    {/* Status Filter */}
                    <div className="flex items-center gap-2">
                        <select
                            value={statusFilter}
                            onChange={(e) => setStatusFilter(e.target.value)}
                            className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-medium text-slate-700 focus:border-indigo-500 focus:outline-none"
                        >
                            <option value="ALL">Tất cả trạng thái</option>
                            <option value="OPEN">MỚI PHÁT HIỆN</option>
                            <option value="NOTIFIED">ĐÃ THÔNG BÁO</option>
                            <option value="REOPENED">TÁI PHÁT</option>
                            <option value="RESOLVED">ĐÃ XỬ LÝ</option>
                        </select>
                    </div>
                </div>

                {/* Week Range Controls */}
                <div className="flex items-center gap-2 border-l border-slate-200 pl-4">
                    <Calendar className="h-4 w-4 text-slate-400" />
                    <span className="text-xs text-slate-500 font-medium">Năm:</span>
                    <input
                        type="number"
                        value={yearFilter}
                        onChange={(e) => setYearFilter(Number(e.target.value))}
                        className="w-16 rounded-xl border border-slate-200 bg-slate-50 px-2 py-1.5 text-xs text-center font-bold text-slate-800"
                    />

                    <span className="text-xs text-slate-500 font-medium">Tuần:</span>
                    <input
                        type="number"
                        value={startWeekFilter}
                        onChange={(e) => setStartWeekFilter(Number(e.target.value))}
                        className="w-14 rounded-xl border border-slate-200 bg-slate-50 px-2 py-1.5 text-xs text-center font-bold text-slate-800"
                    />
                    <span className="text-xs text-slate-400">→</span>
                    <input
                        type="number"
                        value={endWeekFilter}
                        onChange={(e) => setEndWeekFilter(Number(e.target.value))}
                        className="w-14 rounded-xl border border-slate-200 bg-slate-50 px-2 py-1.5 text-xs text-center font-bold text-slate-800"
                    />
                </div>
            </div>

            {/* Main Content Area */}
            {loading ? (
                <div className="flex flex-col items-center justify-center p-12 bg-white rounded-3xl border border-slate-200 shadow-xs">
                    <RefreshCw className="h-8 w-8 text-indigo-600 animate-spin mb-3" />
                    <p className="text-xs text-slate-500 font-medium">Đang rà soát và tải danh sách xung đột cần xử lý...</p>
                </div>
            ) : filteredConflicts.length === 0 ? (
                <div className="flex flex-col items-center justify-center p-12 bg-white rounded-3xl border border-slate-200 shadow-xs text-center">
                    <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600 mb-4 border border-emerald-100">
                        <UserCheck className="h-8 w-8" />
                    </div>
                    <h3 className="text-base font-bold text-slate-900 mb-1">Không có xung đột lịch nào cần xử lý</h3>
                    <p className="text-xs text-slate-500 max-w-md mb-6 leading-relaxed">
                        Tất cả xung đột phân bổ lịch đã được xử lý xong hoặc không phát hiện vấn đề nào trong khoảng thời gian đã chọn.
                    </p>
                    <button
                        onClick={handleScan}
                        className="inline-flex items-center gap-2 rounded-xl bg-slate-100 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-200 transition"
                    >
                        <RefreshCw className="h-3.5 w-3.5" />
                        <span>Thử rà soát lại dữ liệu</span>
                    </button>
                </div>
            ) : (
                /* Conflict List Table */
                <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-xs">
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-xs border-collapse">
                            <thead>
                                <tr className="border-b border-slate-200 bg-slate-50/80 text-slate-500 font-semibold uppercase tracking-wider">
                                    <th className="px-5 py-3.5">Nhân sự</th>
                                    <th className="px-5 py-3.5">Thời gian</th>
                                    <th className="px-5 py-3.5">Loại xung đột</th>
                                    <th className="px-5 py-3.5">Người xử lý & Cách xử lý</th>
                                    <th className="px-5 py-3.5 text-center">Giờ vượt</th>
                                    <th className="px-5 py-3.5 text-center">Trạng thái</th>
                                    <th className="px-5 py-3.5 text-right">Thao tác xử lý</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100 text-slate-700">
                                {filteredConflicts.map((c) => (
                                    <tr key={c.id} className={`hover:bg-slate-50/60 transition ${c.status === "REOPENED" ? "bg-rose-50/30" : ""}`}>
                                        {/* Employee */}
                                        <td className="px-5 py-4">
                                            <div className="font-bold text-slate-900">{c.employeeName}</div>
                                            <div className="text-[11px] text-slate-400 mt-0.5">
                                                {c.employeeCode} • {c.departmentName}
                                            </div>
                                        </td>

                                        {/* Timeframe */}
                                        <td className="px-5 py-4 whitespace-nowrap">
                                            <div className="font-semibold text-slate-800">{c.weekLabel}</div>
                                            <div className="text-[10px] text-slate-400">Năm {c.yearNumber}</div>
                                        </td>

                                        {/* Conflict Type Badge */}
                                        <td className="px-5 py-4 whitespace-nowrap">
                                            {c.conflictType === "MULTI_PROJECT_ALLOCATION" ? (
                                                <span className="inline-flex items-center gap-1.5 rounded-lg bg-sky-50 px-2.5 py-1 text-[11px] font-semibold text-sky-700 border border-sky-200">
                                                    <Briefcase className="h-3 w-3" />
                                                    <span>Trùng nhiều dự án</span>
                                                </span>
                                            ) : (
                                                <span className="inline-flex items-center gap-1.5 rounded-lg bg-amber-50 px-2.5 py-1 text-[11px] font-semibold text-amber-700 border border-amber-200">
                                                    <CalendarX className="h-3 w-3" />
                                                    <span>Trùng nghỉ phép đã duyệt</span>
                                                </span>
                                            )}
                                            {c.projectNames && (
                                                <div className="text-[11px] text-slate-600 font-medium mt-1 truncate max-w-[200px]" title={c.projectNames}>
                                                    {c.projectNames}
                                                </div>
                                            )}
                                        </td>

                                        {/* Handler & Resolution Note */}
                                        <td className="px-5 py-4 max-w-xs">
                                            {c.assignedHandlerName ? (
                                                <div className="flex items-center gap-1.5 text-slate-800 font-bold">
                                                    <UserCheck className="h-3.5 w-3.5 text-indigo-600 shrink-0" />
                                                    <span>{c.assignedHandlerName}</span>
                                                </div>
                                            ) : (
                                                <span className="text-slate-400 italic text-[11px]">Chưa gán người xử lý</span>
                                            )}

                                            {c.resolutionNote && (
                                                <div className="mt-1 flex items-start gap-1 rounded-xl bg-slate-100 p-2 text-[11px] text-slate-700 border border-slate-200">
                                                    <FileText className="h-3.5 w-3.5 text-slate-500 shrink-0 mt-0.5" />
                                                    <span className="line-clamp-2" title={c.resolutionNote}>{c.resolutionNote}</span>
                                                </div>
                                            )}

                                            {c.recurrentNote && c.status === "REOPENED" && (
                                                <div className="mt-1 flex items-start gap-1 rounded-xl bg-rose-100 p-2 text-[11px] text-rose-800 border border-rose-200 font-medium">
                                                    <RotateCcw className="h-3.5 w-3.5 text-rose-600 shrink-0 mt-0.5" />
                                                    <span>{c.recurrentNote}</span>
                                                </div>
                                            )}
                                        </td>

                                        {/* Excess Hours */}
                                        <td className="px-5 py-4 text-center whitespace-nowrap">
                                            <span className="inline-flex items-center justify-center rounded-xl bg-rose-50 px-2.5 py-1 text-xs font-bold text-rose-600 border border-rose-200">
                                                +{c.excessHours}h
                                            </span>
                                        </td>

                                        {/* Status */}
                                        <td className="px-5 py-4 text-center whitespace-nowrap">
                                            {c.status === "RESOLVED" ? (
                                                <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-1 text-[10px] font-bold text-emerald-700 border border-emerald-200">
                                                    <CheckCircle2 className="h-3 w-3" />
                                                    <span>ĐÃ XỬ LÝ</span>
                                                </span>
                                            ) : c.status === "REOPENED" ? (
                                                <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2.5 py-1 text-[10px] font-bold text-rose-800 border border-rose-300 animate-pulse">
                                                    <RotateCcw className="h-3 w-3 text-rose-600" />
                                                    <span>TÁI PHÁT</span>
                                                </span>
                                            ) : c.status === "NOTIFIED" ? (
                                                <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2.5 py-1 text-[10px] font-bold text-amber-700 border border-amber-200">
                                                    <Send className="h-3 w-3" />
                                                    <span>ĐÃ THÔNG BÁO</span>
                                                </span>
                                            ) : (
                                                <span className="inline-flex items-center gap-1 rounded-full bg-rose-50 px-2.5 py-1 text-[10px] font-bold text-rose-700 border border-rose-200">
                                                    <AlertTriangle className="h-3 w-3" />
                                                    <span>MỚI PHÁT HIỆN</span>
                                                </span>
                                            )}
                                        </td>

                                        {/* Action Buttons */}
                                        <td className="px-5 py-4 text-right whitespace-nowrap">
                                            <div className="flex items-center justify-end gap-2">
                                                <button
                                                    onClick={() => setSelectedConflictForResolution(c)}
                                                    title="Gán người chịu trách nhiệm và ghi nhận cách xử lý"
                                                    className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-emerald-700 transition shadow-xs"
                                                >
                                                    <CheckCircle2 className="h-3.5 w-3.5" />
                                                    <span>Xử lý xung đột</span>
                                                </button>

                                                {c.status !== "RESOLVED" && (
                                                    <button
                                                        onClick={() => setSelectedConflictForReplacement(c)}
                                                        title="Gợi ý nhân sự thay thế có cùng kỹ năng và còn giờ rảnh"
                                                        className="inline-flex items-center gap-1.5 rounded-xl border border-indigo-300 bg-indigo-50 px-3 py-1.5 text-xs font-semibold text-indigo-700 hover:bg-indigo-100 transition"
                                                    >
                                                        <UserCheck className="h-3.5 w-3.5" />
                                                        <span>Thay thế</span>
                                                    </button>
                                                )}

                                                {c.status !== "RESOLVED" && (
                                                    <button
                                                        onClick={() => handleNotify(c.id)}
                                                        disabled={actionLoadingId === c.id}
                                                        title="Gửi thông báo thương lượng"
                                                        className="inline-flex items-center gap-1 rounded-xl border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-100 transition disabled:opacity-50"
                                                    >
                                                        <Send className="h-3.5 w-3.5" />
                                                    </button>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* Modal Xử lý Xung đột Lịch */}
            {selectedConflictForResolution && (
                <ConflictResolutionModal
                    conflict={selectedConflictForResolution}
                    isOpen={!!selectedConflictForResolution}
                    onClose={() => setSelectedConflictForResolution(null)}
                    onSuccess={async (resolvedConflict) => {
                        setConflicts((current) => current.filter((item) => item.id !== resolvedConflict.id));
                        await loadData();
                    }}
                />
            )}

            {/* Modal Đề xuất Nhân sự Thay thế */}
            {selectedConflictForReplacement && (
                <ReplacementSuggestionModal
                    conflict={selectedConflictForReplacement}
                    isOpen={!!selectedConflictForReplacement}
                    onClose={() => setSelectedConflictForReplacement(null)}
                    onSuccess={loadData}
                />
            )}
        </div>
    );
}
