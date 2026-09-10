"use client";

import { useState, useEffect } from "react";
import {
    Calendar as CalendarIcon,
    Plus,
    Check,
    X,
    Clock,
    CheckCircle2,
    FileText,
    CalendarDays,
    ListFilter,
    AlertCircle,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuthUser } from "@/lib/auth-session";
import { submitLeaveRequest, getMyLeaveRequests, cancelLeaveRequest } from "@/lib/api/leave";
import CalendarView from "../calendar/CalendarView";

export interface LeaveRequest {
    id: string;
    employeeId: string;
    employeeName: string;
    department: string;
    leaveType: "ANNUAL" | "UNPAID" | "SICK" | "PERSONAL";
    startDate: string;
    endDate: string;
    daysCount: number;
    reason: string;
    status: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
    createdAt: string;
    approverComment?: string;
}

const LEAVE_TYPE_LABELS: Record<LeaveRequest["leaveType"], string> = {
    ANNUAL: "Nghỉ phép năm",
    UNPAID: "Nghỉ không hưởng lương",
    SICK: "Nghỉ ốm / Khám bệnh",
    PERSONAL: "Nghỉ việc riêng / Hiếu hỷ",
};

const LEAVE_TYPE_COLORS: Record<LeaveRequest["leaveType"], string> = {
    ANNUAL: "bg-blue-50 text-blue-700 border-blue-200",
    UNPAID: "bg-amber-50 text-amber-700 border-amber-200",
    SICK: "bg-rose-50 text-rose-700 border-rose-200",
    PERSONAL: "bg-purple-50 text-purple-700 border-purple-200",
};

const INITIAL_LEAVE_DATA: LeaveRequest[] = [
    {
        id: "LV-2026-001",
        employeeId: "1",
        employeeName: "Nguyễn Văn Đức",
        department: "Phòng Phát triển Phần mềm",
        leaveType: "ANNUAL",
        startDate: "2026-09-15",
        endDate: "2026-09-16",
        daysCount: 2,
        reason: "Nghỉ việc gia đình",
        status: "APPROVED",
        createdAt: "2026-09-01",
    },
    {
        id: "LV-2026-002",
        employeeId: "2",
        employeeName: "Trần Thị Mai",
        department: "Phòng Kinh doanh",
        leaveType: "SICK",
        startDate: "2026-09-18",
        endDate: "2026-09-18",
        daysCount: 1,
        reason: "Khám sức khỏe định kỳ",
        status: "PENDING",
        createdAt: "2026-09-08",
    },
    {
        id: "LV-2026-003",
        employeeId: "3",
        employeeName: "Lê Hoàng Nam",
        department: "Phòng Phát triển Phần mềm",
        leaveType: "ANNUAL",
        startDate: "2026-09-22",
        endDate: "2026-09-24",
        daysCount: 3,
        reason: "Du lịch cá nhân",
        status: "PENDING",
        createdAt: "2026-09-08",
    },
    {
        id: "LV-2026-004",
        employeeId: "4",
        employeeName: "Phạm Minh Tuấn",
        department: "Phòng Nhân sự",
        leaveType: "PERSONAL",
        startDate: "2026-09-10",
        endDate: "2026-09-10",
        daysCount: 1,
        reason: "Giải quyết thủ tục hành chính cá nhân",
        status: "APPROVED",
        createdAt: "2026-09-05",
    },
];

const STORAGE_KEY = "sys_leave_requests";

export default function LeaveManagementView() {
    const user = useAuthUser();
    const roleCode = user?.roleCode?.toUpperCase().replace(/_/g, "-") || "";

    const isEmployee = roleCode === "VT-04";
    const isRM = roleCode === "VT-03";
    const isHR = roleCode === "VT-05" || roleCode === "VT-06";

    const [viewMode, setViewMode] = useState<"list" | "calendar">("list");
    const [requests, setRequests] = useState<LeaveRequest[]>(() => {
        try {
            const saved = localStorage.getItem(STORAGE_KEY);
            if (saved) {
                const parsed = JSON.parse(saved);
                if (Array.isArray(parsed) && parsed.length > 0) return parsed;
            }
        } catch {}
        return INITIAL_LEAVE_DATA;
    });

    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [newLeave, setNewLeave] = useState({
        leaveType: "ANNUAL" as LeaveRequest["leaveType"],
        startDate: new Date().toISOString().slice(0, 10),
        endDate: new Date().toISOString().slice(0, 10),
        reason: "",
    });

    const [filterStatus, setFilterStatus] = useState<string>("ALL");
    const [toastMessage, setToastMessage] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const showToast = (msg: string) => {
        setToastMessage(msg);
        setTimeout(() => setToastMessage(null), 3500);
    };

    const saveRequests = (data: LeaveRequest[]) => {
        setRequests(data);
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
        } catch {}
    };

    // Tải dữ liệu thật từ Backend nếu là nhân viên chuyên môn (VT-04)
    const loadLeaveData = async () => {
        if (!isEmployee) return;
        setIsLoading(true);
        try {
            const data = await getMyLeaveRequests();
            if (Array.isArray(data)) {
                const mapped: LeaveRequest[] = data.map((item) => ({
                    id: `LV-${item.id}`,
                    employeeId: String(item.employeeId),
                    employeeName: user?.fullName || user?.username || "Tôi (Nhân viên)",
                    department: user?.orgUnitName || "Phòng chuyên môn",
                    leaveType: item.leaveType,
                    startDate: item.startDate,
                    endDate: item.endDate,
                    daysCount: item.daysCount,
                    reason: item.reason || "",
                    status: item.status as any,
                    createdAt: item.createdAt ? item.createdAt.slice(0, 10) : "",
                }));
                setRequests(mapped);
            }
        } catch (err: any) {
            console.warn("Không thể tải danh sách đơn nghỉ phép từ server:", err);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadLeaveData();
    }, [user?.id, roleCode]);

    // Lọc danh sách theo vai trò
    const currentUserName = (user?.fullName || user?.username || "").trim().toLowerCase();
    const currentUserIdStr = user?.id != null ? String(user.id) : "";

    const userRequests = requests.filter((r) => {
        if (isEmployee) {
            return (
                (currentUserIdStr && r.employeeId === currentUserIdStr) ||
                (currentUserName && r.employeeName.toLowerCase().includes(currentUserName))
            );
        }
        return true;
    });

    const filteredRequests = userRequests.filter((r) => {
        if (filterStatus === "ALL") return true;
        return r.status === filterStatus;
    });

    // Thống kê quỹ phép cá nhân hoặc toàn công ty
    const totalAnnualLeave = 12;
    const usedDays = userRequests
        .filter((r) => r.status === "APPROVED" && r.leaveType === "ANNUAL")
        .reduce((sum, r) => sum + r.daysCount, 0);
    const remainingDays = Math.max(0, totalAnnualLeave - usedDays);
    const pendingCount = userRequests.filter((r) => r.status === "PENDING").length;

    // Duyệt / Từ chối đơn
    const handleApprove = (id: string) => {
        const next = requests.map((r) => (r.id === id ? { ...r, status: "APPROVED" as const } : r));
        saveRequests(next);
        showToast("Đã phê duyệt đơn nghỉ phép thành công!");
    };

    const handleReject = (id: string) => {
        const next = requests.map((r) => (r.id === id ? { ...r, status: "REJECTED" as const } : r));
        saveRequests(next);
        showToast("Đã từ chối đơn nghỉ phép.");
    };

    // Hủy đơn (dành cho người nộp)
    const handleCancelRequest = async (id: string) => {
        if (!window.confirm("Bạn có chắc chắn muốn hủy đơn xin nghỉ phép này không?")) {
            return;
        }
        try {
            await cancelLeaveRequest(id);
            showToast("Đã hủy đơn xin nghỉ phép thành công.");
            await loadLeaveData();
        } catch (err: any) {
            console.error("Lỗi khi hủy đơn:", err);
            showToast(err?.message || "Không thể hủy đơn nghỉ phép lúc này.");
        }
    };

    // Gửi đơn mới qua Backend API (TC-01, TC-02, TC-03)
    const handleSubmitNewLeave = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!newLeave.reason.trim()) {
            showToast("Vui lòng nhập lý do nghỉ phép.");
            return;
        }

        const start = new Date(newLeave.startDate);
        const end = new Date(newLeave.endDate);
        if (end < start) {
            showToast("Ngày kết thúc không được sớm hơn ngày bắt đầu.");
            return;
        }

        try {
            setIsSubmitting(true);
            await submitLeaveRequest({
                leaveType: newLeave.leaveType,
                startDate: newLeave.startDate,
                endDate: newLeave.endDate,
                reason: newLeave.reason.trim(),
            });

            showToast("Gửi đơn nghỉ phép thành công. Đang chờ quản lý phê duyệt.");
            setIsCreateModalOpen(false);
            setNewLeave({
                leaveType: "ANNUAL",
                startDate: new Date().toISOString().slice(0, 10),
                endDate: new Date().toISOString().slice(0, 10),
                reason: "",
            });
            await loadLeaveData();
        } catch (err: any) {
            showToast(err.message || "Không thể gửi đơn nghỉ phép. Vui lòng kiểm tra lại.");
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                <div>
                    <h1 className="text-2xl font-extrabold tracking-tight text-slate-900">
                        Quản lý &amp; Đơn nghỉ phép
                    </h1>
                    <p className="mt-1 text-xs font-semibold text-slate-500 sm:text-sm">
                        {isEmployee
                            ? "Xem quỹ phép năm, theo dõi tiến độ phê duyệt và nộp đơn nghỉ phép cá nhân."
                            : "Theo dõi lịch nghỉ phép, phê duyệt đơn nghỉ của bộ phận và cân đối kế hoạch nguồn lực."}
                    </p>
                </div>

                <div className="flex items-center gap-2.5">
                    {/* Switch chế độ xem: Danh sách / Lịch */}
                    <div className="flex rounded-xl border border-slate-200 bg-white p-1 shadow-2xs">
                        <button
                            type="button"
                            onClick={() => setViewMode("list")}
                            className={cn(
                                "flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition cursor-pointer",
                                viewMode === "list"
                                    ? "bg-indigo-600 text-white shadow-xs"
                                    : "text-slate-600 hover:text-slate-900"
                            )}
                        >
                            <FileText className="size-3.5" />
                            <span>Danh sách đơn</span>
                        </button>
                        <button
                            type="button"
                            onClick={() => setViewMode("calendar")}
                            className={cn(
                                "flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition cursor-pointer",
                                viewMode === "calendar"
                                    ? "bg-indigo-600 text-white shadow-xs"
                                    : "text-slate-600 hover:text-slate-900"
                            )}
                        >
                            <CalendarDays className="size-3.5" />
                            <span>Lịch Workspace</span>
                        </button>
                    </div>

                    {/* Nút nộp đơn nghỉ phép: Chỉ hiển thị cho vai trò Nhân viên chuyên môn VT-04 (TC-04) */}
                    {isEmployee && (
                        <button
                            type="button"
                            onClick={() => setIsCreateModalOpen(true)}
                            className="flex min-h-10 items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-xs transition hover:bg-indigo-700 active:scale-95 cursor-pointer"
                        >
                            <Plus className="size-4" />
                            <span>Gửi đơn nghỉ phép</span>
                        </button>
                    )}
                </div>
            </div>

            {/* Thẻ thống kê quỹ phép */}
            <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
                <div className="rounded-2xl border border-blue-200 bg-blue-50/70 p-4.5">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-bold uppercase tracking-wider text-blue-800">
                            {isEmployee ? "Tổng phép năm" : "Tiêu chuẩn phép năm"}
                        </span>
                        <CalendarIcon className="size-4 text-blue-600" />
                    </div>
                    <p className="mt-2 text-2xl font-black text-blue-950">{totalAnnualLeave} ngày</p>
                    <p className="mt-1 text-[11px] font-semibold text-blue-600">Quy định luật lao động</p>
                </div>

                <div className="rounded-2xl border border-emerald-200 bg-emerald-50/70 p-4.5">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-bold uppercase tracking-wider text-emerald-800">
                            {isEmployee ? "Phép còn lại" : "Tỷ lệ khả dụng"}
                        </span>
                        <CheckCircle2 className="size-4 text-emerald-600" />
                    </div>
                    <p className="mt-2 text-2xl font-black text-emerald-950">{remainingDays} ngày</p>
                    <p className="mt-1 text-[11px] font-semibold text-emerald-600">Có thể đăng ký nghỉ</p>
                </div>

                <div className="rounded-2xl border border-purple-200 bg-purple-50/70 p-4.5">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-bold uppercase tracking-wider text-purple-800">
                            {isEmployee ? "Đã sử dụng" : "Tổng ngày đã nghỉ"}
                        </span>
                        <CalendarDays className="size-4 text-purple-600" />
                    </div>
                    <p className="mt-2 text-2xl font-black text-purple-950">{usedDays} ngày</p>
                    <p className="mt-1 text-[11px] font-semibold text-purple-600">Đã được phê duyệt</p>
                </div>

                <div className="rounded-2xl border border-amber-200 bg-amber-50/70 p-4.5">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-bold uppercase tracking-wider text-amber-800">
                            Chờ phê duyệt
                        </span>
                        <Clock className="size-4 text-amber-600" />
                    </div>
                    <p className="mt-2 text-2xl font-black text-amber-950">{pendingCount} đơn</p>
                    <p className="mt-1 text-[11px] font-semibold text-amber-600">Đang chờ xử lý</p>
                </div>
            </div>

            {/* Nội dung chính */}
            {viewMode === "list" ? (
                <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xs">
                    {/* Bộ lọc bảng */}
                    <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 p-4">
                        <div className="flex items-center gap-2">
                            <ListFilter className="size-4 text-slate-400" />
                            <span className="text-xs font-bold text-slate-700">Bộ lọc trạng thái:</span>
                            <div className="flex gap-1.5">
                                {[
                                    { id: "ALL", label: "Tất cả" },
                                    { id: "PENDING", label: "Chờ duyệt" },
                                    { id: "APPROVED", label: "Đã duyệt" },
                                    { id: "REJECTED", label: "Từ chối" },
                                ].map((tab) => (
                                    <button
                                        key={tab.id}
                                        type="button"
                                        onClick={() => setFilterStatus(tab.id)}
                                        className={cn(
                                            "rounded-lg px-2.5 py-1 text-xs font-semibold transition cursor-pointer",
                                            filterStatus === tab.id
                                                ? "bg-slate-900 text-white"
                                                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                                        )}
                                    >
                                        {tab.label}
                                    </button>
                                ))}
                            </div>
                        </div>

                        <span className="text-xs text-slate-400">
                            Hiển thị {filteredRequests.length} / {userRequests.length} đơn
                        </span>
                    </div>

                    {/* Table */}
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-xs text-slate-800">
                            <thead className="border-b border-slate-200 bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                                <tr>
                                    <th className="px-4 py-3.5">Mã đơn / Nhân viên</th>
                                    <th className="px-4 py-3.5">Loại nghỉ</th>
                                    <th className="px-4 py-3.5">Thời gian nghỉ</th>
                                    <th className="px-4 py-3.5">Số ngày</th>
                                    <th className="px-4 py-3.5">Lý do</th>
                                    <th className="px-4 py-3.5">Trạng thái</th>
                                    <th className="px-4 py-3.5 text-right">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {isLoading ? (
                                    <tr>
                                        <td colSpan={7} className="p-8 text-center text-slate-400">
                                            Đang tải dữ liệu đơn nghỉ phép...
                                        </td>
                                    </tr>
                                ) : filteredRequests.length === 0 ? (
                                    <tr>
                                        <td colSpan={7} className="p-8 text-center text-slate-400">
                                            Không có đơn nghỉ phép nào phù hợp với bộ lọc.
                                        </td>
                                    </tr>
                                ) : (
                                    filteredRequests.map((req) => (
                                        <tr key={req.id} className="transition hover:bg-slate-50/80">
                                            <td className="px-4 py-3">
                                                <div>
                                                    <p className="font-bold text-slate-900">{req.employeeName}</p>
                                                    <p className="font-mono text-[10px] text-slate-400">
                                                        {req.id} • {req.department}
                                                    </p>
                                                </div>
                                            </td>
                                            <td className="px-4 py-3">
                                                <span
                                                    className={cn(
                                                        "rounded-md border px-2 py-0.5 text-[11px] font-semibold",
                                                        LEAVE_TYPE_COLORS[req.leaveType]
                                                    )}
                                                >
                                                    {LEAVE_TYPE_LABELS[req.leaveType]}
                                                </span>
                                            </td>
                                            <td className="px-4 py-3 font-mono font-medium text-slate-700">
                                                {req.startDate === req.endDate
                                                    ? req.startDate
                                                    : `${req.startDate} → ${req.endDate}`}
                                            </td>
                                            <td className="px-4 py-3 font-bold text-slate-900">
                                                {req.daysCount} ngày
                                            </td>
                                            <td className="px-4 py-3 max-w-xs truncate text-slate-600" title={req.reason}>
                                                {req.reason}
                                            </td>
                                            <td className="px-4 py-3">
                                                {req.status === "APPROVED" && (
                                                    <span className="inline-flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-0.5 text-[11px] font-bold text-emerald-700">
                                                        <Check className="size-3" /> Đã duyệt
                                                    </span>
                                                )}
                                                {req.status === "REJECTED" && (
                                                    <span className="inline-flex items-center gap-1 rounded-full border border-rose-200 bg-rose-50 px-2.5 py-0.5 text-[11px] font-bold text-rose-700">
                                                        <X className="size-3" /> Từ chối
                                                    </span>
                                                )}
                                                {req.status === "CANCELLED" && (
                                                    <span className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-slate-100 px-2.5 py-0.5 text-[11px] font-bold text-slate-500">
                                                        Đã hủy
                                                    </span>
                                                )}
                                                {req.status === "PENDING" && (
                                                    <span className="inline-flex items-center gap-1 rounded-full border border-amber-200 bg-amber-50 px-2.5 py-0.5 text-[11px] font-bold text-amber-700">
                                                        <Clock className="size-3" /> Chờ duyệt
                                                    </span>
                                                )}
                                            </td>
                                            <td className="px-4 py-3 text-right">
                                                {/* Thao tác Phê duyệt cho RM (VT-03), HR (VT-05) */}
                                                {(isRM || isHR) && req.status === "PENDING" && (
                                                    <div className="flex items-center justify-end gap-1.5">
                                                        <button
                                                            type="button"
                                                            onClick={() => handleApprove(req.id)}
                                                            className="inline-flex items-center gap-1 rounded-lg border border-emerald-300 bg-emerald-50 px-2 py-1 text-[11px] font-bold text-emerald-700 hover:bg-emerald-100 transition cursor-pointer"
                                                            title="Phê duyệt đơn"
                                                        >
                                                            <Check className="size-3" />
                                                            <span>Duyệt</span>
                                                        </button>
                                                        <button
                                                            type="button"
                                                            onClick={() => handleReject(req.id)}
                                                            className="inline-flex items-center gap-1 rounded-lg border border-rose-300 bg-rose-50 px-2 py-1 text-[11px] font-bold text-rose-700 hover:bg-rose-100 transition cursor-pointer"
                                                            title="Từ chối đơn"
                                                        >
                                                            <X className="size-3" />
                                                            <span>Từ chối</span>
                                                        </button>
                                                    </div>
                                                )}

                                                {/* Thao tác Hủy đơn cho chính nhân viên */}
                                                {isEmployee && req.status === "PENDING" && (
                                                    <button
                                                        type="button"
                                                        onClick={() => handleCancelRequest(req.id)}
                                                        className="rounded-lg border border-slate-200 px-2 py-1 text-[11px] font-semibold text-slate-500 hover:bg-slate-50 hover:text-rose-600 transition cursor-pointer"
                                                    >
                                                        Hủy đơn
                                                    </button>
                                                )}

                                                {req.status !== "PENDING" && (
                                                    <span className="text-[11px] text-slate-400">Đã xử lý</span>
                                                )}
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            ) : (
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs">
                    <CalendarView />
                </div>
            )}

            {/* Modal Gửi đơn nghỉ phép */}
            {isCreateModalOpen && (
                <div
                    className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4"
                    role="dialog"
                    aria-modal="true"
                >
                    <div className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
                        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                            <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                                <CalendarDays className="size-5 text-indigo-600" />
                                <span>Gửi đơn xin nghỉ phép</span>
                            </h3>
                            <button
                                type="button"
                                onClick={() => setIsCreateModalOpen(false)}
                                className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition cursor-pointer"
                            >
                                <X className="size-5" />
                            </button>
                        </div>

                        <form onSubmit={handleSubmitNewLeave} className="mt-4 space-y-4 text-xs">
                            <div>
                                <label className="block font-bold text-slate-700 mb-1">
                                    Loại nghỉ phép <span className="text-rose-500">*</span>
                                </label>
                                <select
                                    value={newLeave.leaveType}
                                    onChange={(e) =>
                                        setNewLeave((prev) => ({
                                            ...prev,
                                            leaveType: e.target.value as LeaveRequest["leaveType"],
                                        }))
                                    }
                                    className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-semibold text-slate-800 focus:border-indigo-500 focus:bg-white focus:outline-none"
                                >
                                    <option value="ANNUAL">Nghỉ phép năm (Trừ vào 12 ngày tiêu chuẩn)</option>
                                    <option value="UNPAID">Nghỉ không hưởng lương</option>
                                    <option value="SICK">Nghỉ ốm đau / Khám bệnh</option>
                                    <option value="PERSONAL">Nghỉ việc riêng / Hiếu hỷ</option>
                                </select>
                            </div>

                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <label className="block font-bold text-slate-700 mb-1">
                                        Từ ngày <span className="text-rose-500">*</span>
                                    </label>
                                    <input
                                        type="date"
                                        required
                                        value={newLeave.startDate}
                                        onChange={(e) =>
                                            setNewLeave((prev) => ({ ...prev, startDate: e.target.value }))
                                        }
                                        className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-800 focus:border-indigo-500 focus:outline-none"
                                    />
                                </div>
                                <div>
                                    <label className="block font-bold text-slate-700 mb-1">
                                        Đến ngày <span className="text-rose-500">*</span>
                                    </label>
                                    <input
                                        type="date"
                                        required
                                        value={newLeave.endDate}
                                        onChange={(e) =>
                                            setNewLeave((prev) => ({ ...prev, endDate: e.target.value }))
                                        }
                                        className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-800 focus:border-indigo-500 focus:outline-none"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block font-bold text-slate-700 mb-1">
                                    Lý do nghỉ phép <span className="text-rose-500">*</span>
                                </label>
                                <textarea
                                    required
                                    rows={3}
                                    placeholder="Nêu rõ lý do nghỉ phép và người hỗ trợ bàn giao công việc..."
                                    value={newLeave.reason}
                                    onChange={(e) =>
                                        setNewLeave((prev) => ({ ...prev, reason: e.target.value }))
                                    }
                                    className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs text-slate-800 focus:border-indigo-500 focus:outline-none"
                                />
                            </div>

                            <div className="rounded-xl border border-amber-200 bg-amber-50/70 p-3 text-[11px] text-amber-800 flex items-start gap-2">
                                <AlertCircle className="size-4 text-amber-600 shrink-0 mt-0.5" />
                                <span>
                                    Đơn nghỉ phép sẽ được gửi trực tiếp đến Trưởng bộ phận (RM) hoặc Bộ phận Nhân sự (HR) để phê duyệt.
                                </span>
                            </div>

                            <div className="flex justify-end gap-2 border-t border-slate-100 pt-3">
                                <button
                                    type="button"
                                    onClick={() => setIsCreateModalOpen(false)}
                                    className="rounded-xl border border-slate-200 px-4 py-2 font-semibold text-slate-600 hover:bg-slate-50 transition cursor-pointer"
                                >
                                    Đóng
                                </button>
                                <button
                                    type="submit"
                                    disabled={isSubmitting}
                                    className={cn(
                                        "rounded-xl bg-indigo-600 px-4 py-2 font-bold text-white shadow-xs hover:bg-indigo-700 transition cursor-pointer",
                                        isSubmitting && "opacity-60 cursor-not-allowed"
                                    )}
                                >
                                    {isSubmitting ? "Đang gửi..." : "Nộp đơn nghỉ phép"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Toast popup */}
            {toastMessage && (
                <div className="fixed bottom-6 right-6 z-50 flex items-center gap-2 rounded-xl bg-slate-900/90 px-4 py-2.5 text-xs font-bold text-white shadow-xl backdrop-blur-xs animate-in fade-in duration-200">
                    <CheckCircle2 className="size-4 text-emerald-400" />
                    <span>{toastMessage}</span>
                </div>
            )}
        </div>
    );
}

