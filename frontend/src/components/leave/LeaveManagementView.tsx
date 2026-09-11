"use client";

import { useState, useEffect, useMemo } from "react";
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
    Info,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuthUser } from "@/lib/auth-session";
import {
    submitLeaveRequest,
    getMyLeaveRequests,
    cancelLeaveRequest,
    getMyLeaveBalance,
    type LeaveBalanceDto,
} from "@/lib/api/leave";
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

// Hàm tiện ích: Tính số ngày làm việc (T2 -> T6) giữa 2 mốc thời gian
function calculateWorkingDays(startDateStr: string, endDateStr: string): number {
    if (!startDateStr || !endDateStr) return 0;
    const start = new Date(startDateStr);
    const end = new Date(endDateStr);
    if (end < start) return 0;

    let count = 0;
    const cur = new Date(start);
    while (cur <= end) {
        const day = cur.getDay();
        if (day !== 0 && day !== 6) {
            count++;
        }
        cur.setDate(cur.getDate() + 1);
    }
    return count;
}

export default function LeaveManagementView() {
    const user = useAuthUser();
    const roleCode = user?.roleCode?.toUpperCase().replace(/_/g, "-") || "";

    const isEmployee = roleCode === "VT-04";

    const [viewMode, setViewMode] = useState<"list" | "calendar">("list");
    const [requests, setRequests] = useState<LeaveRequest[]>([]);
    const [balance, setBalance] = useState<LeaveBalanceDto | null>(null);

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

    // Tải dữ liệu thật từ Backend (Đơn nghỉ phép + Quỹ phép cá nhân)
    const loadLeaveData = async () => {
        if (!isEmployee) return;
        setIsLoading(true);
        try {
            const [requestsData, balanceData] = await Promise.allSettled([
                getMyLeaveRequests(),
                getMyLeaveBalance(),
            ]);

            if (requestsData.status === "fulfilled" && Array.isArray(requestsData.value)) {
                const mapped: LeaveRequest[] = requestsData.value.map((item) => ({
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

            if (balanceData.status === "fulfilled" && balanceData.value) {
                setBalance(balanceData.value);
            }
        } catch (err: any) {
            console.warn("Không thể tải thông tin nghỉ phép từ server:", err);
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

    // Thống kê quỹ phép: Ưu tiên dữ liệu chuẩn xác từ LeaveBalanceDto do Backend tính toán
    const totalAllocated = balance ? balance.totalAllocatedDays : 12;
    const usedDays = balance
        ? balance.approvedDays
        : userRequests
              .filter((r) => r.status === "APPROVED" && r.leaveType === "ANNUAL")
              .reduce((sum, r) => sum + r.daysCount, 0);
    const pendingDays = balance
        ? balance.pendingDays
        : userRequests
              .filter((r) => r.status === "PENDING" && r.leaveType === "ANNUAL")
              .reduce((sum, r) => sum + r.daysCount, 0);
    const remainingDays = balance ? balance.remainingDays : Math.max(0, totalAllocated - usedDays - pendingDays);
    const pendingCount = userRequests.filter((r) => r.status === "PENDING").length;

    // Tính toán số ngày nghỉ đang chọn trong form
    const estimatedWorkingDays = useMemo(() => {
        return calculateWorkingDays(newLeave.startDate, newLeave.endDate);
    }, [newLeave.startDate, newLeave.endDate]);

    // Kiểm tra có bị vượt hạn mức phép năm không (AC-02 & TC-02)
    const isExceedingAnnualLeave =
        newLeave.leaveType === "ANNUAL" &&
        balance !== null &&
        estimatedWorkingDays > balance.remainingDays;

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

        // AC-02 Validation: Chặn gửi nếu vượt quá quỹ phép năm còn lại
        if (isExceedingAnnualLeave) {
            showToast(
                `Số ngày nghỉ (${estimatedWorkingDays} ngày) vượt quá số phép năm còn lại (${balance?.remainingDays} ngày). Vui lòng chọn loại 'Nghỉ không hưởng lương' hoặc điều chỉnh ngày nghỉ.`
            );
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
            // Hiển thị trực tiếp thông báo lỗi từ Backend nếu vi phạm hạn mức
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

                    {/* Nút nộp đơn nghỉ phép: Chỉ hiển thị cho vai trò Nhân viên chuyên môn VT-04 */}
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

            {/* Thẻ thống kê quỹ phép - Tích hợp số liệu thực tế */}
            <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
                <div className="rounded-2xl border border-blue-200 bg-blue-50/70 p-4.5">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-bold uppercase tracking-wider text-blue-800">
                            {isEmployee ? "Tổng phép năm" : "Tiêu chuẩn phép năm"}
                        </span>
                        <CalendarIcon className="size-4 text-blue-600" />
                    </div>
                    <p className="mt-2 text-2xl font-black text-blue-950">{totalAllocated} ngày</p>
                    <p className="mt-1 text-[11px] font-semibold text-blue-600">
                        {balance && balance.carriedOverDays > 0
                            ? `Tiêu chuẩn ${balance.entitledDays} + Chuyển tiếp ${balance.carriedOverDays}`
                            : "Quy định luật lao động"}
                    </p>
                </div>

                <div className="rounded-2xl border border-emerald-200 bg-emerald-50/70 p-4.5">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-bold uppercase tracking-wider text-emerald-800">
                            {isEmployee ? "Phép còn lại" : "Tỷ lệ khả dụng"}
                        </span>
                        <CheckCircle2 className="size-4 text-emerald-600" />
                    </div>
                    <p className="mt-2 text-2xl font-black text-emerald-950">{remainingDays} ngày</p>
                    <p className="mt-1 text-[11px] font-semibold text-emerald-600">Khả dụng đăng ký nghỉ</p>
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
                            Đang chờ duyệt
                        </span>
                        <Clock className="size-4 text-amber-600" />
                    </div>
                    <p className="mt-2 text-2xl font-black text-amber-950">
                        {balance ? `${balance.pendingDays} ngày` : `${pendingCount} đơn`}
                    </p>
                    <p className="mt-1 text-[11px] font-semibold text-amber-600">
                        {pendingCount} đơn đang chờ xử lý
                    </p>
                </div>
            </div>

            {/* Nội dung chính: Danh sách đơn */}
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

                        <span className="text-xs font-semibold text-slate-400">
                            Tổng cộng: {filteredRequests.length} đơn
                        </span>
                    </div>

                    {/* Danh sách table */}
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-xs text-slate-600">
                            <thead className="bg-slate-50/80 text-[11px] font-bold uppercase tracking-wider text-slate-500 border-b border-slate-100">
                                <tr>
                                    <th className="px-4 py-3">Mã đơn</th>
                                    {!isEmployee && <th className="px-4 py-3">Nhân viên</th>}
                                    <th className="px-4 py-3">Loại nghỉ</th>
                                    <th className="px-4 py-3">Thời gian nghỉ</th>
                                    <th className="px-4 py-3">Số ngày</th>
                                    <th className="px-4 py-3">Lý do</th>
                                    <th className="px-4 py-3">Trạng thái</th>
                                    <th className="px-4 py-3 text-right">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {isLoading ? (
                                    <tr>
                                        <td colSpan={8} className="py-8 text-center text-slate-400">
                                            Đang tải dữ liệu đơn nghỉ phép...
                                        </td>
                                    </tr>
                                ) : filteredRequests.length === 0 ? (
                                    <tr>
                                        <td colSpan={8} className="py-8 text-center text-slate-400">
                                            Không tìm thấy đơn nghỉ phép nào phù hợp.
                                        </td>
                                    </tr>
                                ) : (
                                    filteredRequests.map((req) => (
                                        <tr key={req.id} className="hover:bg-slate-50/60 transition">
                                            <td className="px-4 py-3.5 font-bold text-slate-900">{req.id}</td>
                                            {!isEmployee && (
                                                <td className="px-4 py-3.5">
                                                    <p className="font-bold text-slate-900">{req.employeeName}</p>
                                                    <p className="text-[11px] text-slate-400">{req.department}</p>
                                                </td>
                                            )}
                                            <td className="px-4 py-3.5">
                                                <span
                                                    className={cn(
                                                        "inline-flex items-center rounded-md border px-2 py-0.5 text-[11px] font-semibold",
                                                        LEAVE_TYPE_COLORS[req.leaveType]
                                                    )}
                                                >
                                                    {LEAVE_TYPE_LABELS[req.leaveType]}
                                                </span>
                                            </td>
                                            <td className="px-4 py-3.5 font-medium text-slate-700">
                                                {req.startDate} <span className="text-slate-400">&rarr;</span> {req.endDate}
                                            </td>
                                            <td className="px-4 py-3.5 font-bold text-slate-900">
                                                {req.daysCount} ngày
                                            </td>
                                            <td className="max-w-[200px] truncate px-4 py-3.5 text-slate-500" title={req.reason}>
                                                {req.reason}
                                            </td>
                                            <td className="px-4 py-3.5">
                                                {req.status === "PENDING" && (
                                                    <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2.5 py-0.5 text-[11px] font-bold text-amber-700 border border-amber-200">
                                                        <Clock className="size-3" /> Chờ duyệt
                                                    </span>
                                                )}
                                                {req.status === "APPROVED" && (
                                                    <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-[11px] font-bold text-emerald-700 border border-emerald-200">
                                                        <Check className="size-3" /> Đã duyệt
                                                    </span>
                                                )}
                                                {req.status === "REJECTED" && (
                                                    <span className="inline-flex items-center gap-1 rounded-full bg-rose-50 px-2.5 py-0.5 text-[11px] font-bold text-rose-700 border border-rose-200">
                                                        <X className="size-3" /> Từ chối
                                                    </span>
                                                )}
                                                {req.status === "CANCELLED" && (
                                                    <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-0.5 text-[11px] font-bold text-slate-600 border border-slate-200">
                                                        Đã hủy
                                                    </span>
                                                )}
                                            </td>
                                            <td className="px-4 py-3.5 text-right">
                                                {isEmployee && req.status === "PENDING" && (
                                                    <button
                                                        type="button"
                                                        onClick={() => handleCancelRequest(req.id)}
                                                        className="rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-[11px] font-bold text-slate-700 hover:bg-rose-50 hover:text-rose-600 hover:border-rose-200 transition cursor-pointer"
                                                    >
                                                        Hủy đơn
                                                    </button>
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
                /* Chế độ xem Lịch Workspace */
                <CalendarView />
            )}

            {/* Modal Gửi đơn nghỉ phép */}
            {isCreateModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs">
                    <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl animate-in zoom-in-95 duration-150">
                        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                            <h2 className="text-base font-bold text-slate-900">Gửi đơn xin nghỉ phép</h2>
                            <button
                                type="button"
                                onClick={() => setIsCreateModalOpen(false)}
                                className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition cursor-pointer"
                            >
                                <X className="size-4" />
                            </button>
                        </div>

                        <form onSubmit={handleSubmitNewLeave} className="mt-4 space-y-4 text-xs">
                            {/* Thông tin quỹ phép khả dụng */}
                            {balance && (
                                <div className="rounded-xl border border-blue-200 bg-blue-50/70 p-3 text-blue-900">
                                    <div className="flex items-center gap-1.5 font-bold">
                                        <Info className="size-4 text-blue-600 shrink-0" />
                                        <span>Quỹ phép năm hiện tại:</span>
                                    </div>
                                    <div className="mt-1 grid grid-cols-3 gap-2 text-[11px] text-blue-700">
                                        <div>Được cấp: <span className="font-bold">{balance.totalAllocatedDays}</span></div>
                                        <div>Đã nghỉ: <span className="font-bold">{balance.approvedDays}</span></div>
                                        <div>Còn lại: <span className="font-black text-blue-950">{balance.remainingDays} ngày</span></div>
                                    </div>
                                </div>
                            )}

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
                                    <option value="ANNUAL">
                                        Nghỉ phép năm ({balance ? `còn ${balance.remainingDays} ngày` : "trừ vào quỹ phép năm"})
                                    </option>
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

                            {/* Cảnh báo tính toán số ngày & cảnh báo vượt phép (AC-02 & TC-02) */}
                            <div className="text-[11px] text-slate-500 flex justify-between items-center px-1">
                                <span>Số ngày nghỉ phép dự kiến:</span>
                                <span className="font-bold text-slate-900">{estimatedWorkingDays} ngày</span>
                            </div>

                            {isExceedingAnnualLeave && (
                                <div className="rounded-xl border border-rose-300 bg-rose-50 p-3 text-[11px] text-rose-800 flex items-start gap-2">
                                    <AlertCircle className="size-4 text-rose-600 shrink-0 mt-0.5" />
                                    <span>
                                        <strong>Vượt quá quỹ phép năm!</strong> Bạn đang chọn nghỉ <strong>{estimatedWorkingDays} ngày</strong> làm việc nhưng quỹ phép còn lại chỉ có <strong>{balance?.remainingDays} ngày</strong>. Vui lòng chuyển loại nghỉ sang <em>"Nghỉ không hưởng lương"</em> hoặc rút ngắn số ngày.
                                    </span>
                                </div>
                            )}

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
                                    disabled={isSubmitting || isExceedingAnnualLeave}
                                    className={cn(
                                        "rounded-xl bg-indigo-600 px-4 py-2 font-bold text-white shadow-xs hover:bg-indigo-700 transition cursor-pointer",
                                        (isSubmitting || isExceedingAnnualLeave) && "opacity-50 cursor-not-allowed"
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
