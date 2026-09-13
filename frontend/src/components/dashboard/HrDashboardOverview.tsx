import { useState, useEffect } from "react";
import {
    Users,
    CalendarIcon,
    Building2,
    Sparkles,
    ArrowUpRight,
    RefreshCw,
    CheckCircle2,
    Clock,
    Plus,
    FileText,
    ShieldCheck,
    ChevronRight,
    CalendarDays,
    HeartHandshake,
} from "lucide-react";
import { useAuthUser } from "@/lib/auth-session";
import { getEmployees, type EmployeeProfile } from "@/lib/api/employees";
import { getPendingLeaveRequests, approveLeaveRequest, type LeaveRequestDto } from "@/lib/api/leave";
import { getOrgTree } from "@/lib/api/org-units";
import { getPendingSkills, type PendingEmployeeSkillItem } from "@/lib/api/skills";
import type { OrgUnitTreeNode } from "@/types/hrm";
import MiniCalendar from "../calendar/MiniCalendar";

interface HrDashboardOverviewProps {
    onNavigate: (tabId: string) => void;
}

export default function HrDashboardOverview({ onNavigate }: HrDashboardOverviewProps) {
    const user = useAuthUser();
    const [loading, setLoading] = useState(true);
    const [employees, setEmployees] = useState<EmployeeProfile[]>([]);
    const [pendingLeaves, setPendingLeaves] = useState<LeaveRequestDto[]>([]);
    const [pendingSkills, setPendingSkills] = useState<PendingEmployeeSkillItem[]>([]);
    const [orgTree, setOrgTree] = useState<readonly OrgUnitTreeNode[]>([]);
    const [selectedDate, setSelectedDate] = useState<Date>(new Date());
    const [miniCalMonth, setMiniCalMonth] = useState<Date>(new Date());
    const [now, setNow] = useState<Date>(new Date());
    const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 60000);
        return () => clearInterval(timer);
    }, []);

    const countOrgUnits = (nodes: readonly OrgUnitTreeNode[]): number => {
        let count = 0;
        for (const node of nodes) {
            count += 1;
            if (node.children && node.children.length > 0) {
                count += countOrgUnits(node.children);
            }
        }
        return count;
    };

    const loadHrData = async () => {
        setLoading(true);
        try {
            const [empRes, leavesRes, skillsRes, orgRes] = await Promise.allSettled([
                getEmployees(1, 100),
                getPendingLeaveRequests(),
                getPendingSkills(),
                getOrgTree(),
            ]);

            if (empRes.status === "fulfilled" && empRes.value) {
                setEmployees(empRes.value.content || []);
            }
            if (leavesRes.status === "fulfilled" && leavesRes.value) {
                setPendingLeaves(leavesRes.value || []);
            }
            if (skillsRes.status === "fulfilled" && skillsRes.value) {
                setPendingSkills(skillsRes.value || []);
            }
            if (orgRes.status === "fulfilled" && orgRes.value) {
                setOrgTree(orgRes.value || []);
            }
        } catch (err) {
            console.error("Failed to load HR dashboard data:", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadHrData();
    }, []);

    const handleQuickApproveLeave = async (leaveId: number) => {
        setActionLoadingId(leaveId);
        try {
            await approveLeaveRequest(leaveId, "Đã duyệt nhanh bởi Nhân sự HR");
            setPendingLeaves((prev) => prev.filter((l) => l.id !== leaveId));
        } catch (err) {
            console.error("Lỗi khi duyệt nhanh đơn nghỉ phép:", err);
            alert("Có lỗi xảy ra khi duyệt đơn nghỉ phép. Vui lòng kiểm tra lại tại trang Nghỉ phép.");
        } finally {
            setActionLoadingId(null);
        }
    };

    const today = new Date();
    const formattedDate = today.toLocaleDateString("vi-VN", {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
    });
    const formattedDateCapitalized =
        formattedDate.charAt(0).toUpperCase() + formattedDate.slice(1);
    const userName = user?.fullName || user?.username || "Chuyên viên Nhân sự";

    const handleChangeMonth = (offset: number) => {
        setMiniCalMonth(new Date(miniCalMonth.getFullYear(), miniCalMonth.getMonth() + offset, 1));
    };

    const totalStaff = employees.length;
    const orgUnitsCount = countOrgUnits(orgTree);

    return (
        <div className="space-y-4">
            {/* Header Clean White */}
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-3">
                <div>
                    <div className="flex items-center gap-2">
                        <h1 className="text-xl font-bold tracking-tight text-slate-900">
                            Xin chào, {userName}!
                        </h1>
                        <span className="inline-flex items-center gap-1 rounded-md bg-teal-50 border border-teal-200 px-2 py-0.5 text-[11px] font-bold text-teal-800">
                            <HeartHandshake className="h-3 w-3 text-teal-700" /> Quản trị Nhân sự (HR)
                        </span>
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">
                        {formattedDateCapitalized} · Bảng điều khiển hồ sơ nhân sự, quản lý nghỉ phép, cơ cấu tổ chức và phê duyệt chuyên môn.
                    </p>
                </div>

                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={loadHrData}
                        disabled={loading}
                        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-2xs cursor-pointer disabled:opacity-50"
                        title="Tải lại dữ liệu"
                    >
                        <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin text-teal-600" : "text-slate-500"}`} />
                        <span>Làm mới</span>
                    </button>
                    <button
                        type="button"
                        onClick={() => onNavigate("hrprofile")}
                        className="inline-flex items-center gap-1.5 rounded-xl bg-teal-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-teal-700 transition shadow-xs cursor-pointer"
                    >
                        <Plus className="h-3.5 w-3.5" />
                        <span>Thêm hồ sơ nhân sự</span>
                    </button>
                </div>
            </div>

            {/* KPI Cards (4 Cards HR Overview) */}
            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
                {/* 1. Tổng quy mô Nhân sự */}
                <div
                    onClick={() => onNavigate("hrprofile")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-teal-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-teal-50 border border-teal-100 text-teal-600">
                            <Users className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-teal-600 group-hover:translate-x-0.5 transition">
                            Hồ sơ <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Tổng số Nhân sự
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{totalStaff}</span>
                            <span className="text-[10px] text-slate-400">nhân sự hoạt động</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-teal-700">
                            Toàn quyền quản lý hồ sơ nhân sự
                        </div>
                    </div>
                </div>

                {/* 2. Đơn nghỉ phép chờ duyệt */}
                <div
                    onClick={() => onNavigate("leave")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-amber-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-amber-50 border border-amber-100 text-amber-600">
                            <CalendarIcon className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-amber-600 group-hover:translate-x-0.5 transition">
                            Nghỉ phép <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Đơn nghỉ phép chờ xử lý
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{pendingLeaves.length}</span>
                            <span className="text-[10px] text-slate-400">đơn cần duyệt</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-amber-700">
                            {pendingLeaves.length > 0 ? "Cần xử lý kịp thời theo QTN-10" : "Đã duyệt hết yêu cầu"}
                        </div>
                    </div>
                </div>

                {/* 3. Cơ cấu Tổ chức & Phòng ban */}
                <div
                    onClick={() => onNavigate("departments")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-emerald-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-50 border border-emerald-100 text-emerald-600">
                            <Building2 className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-emerald-600 group-hover:translate-x-0.5 transition">
                            Cơ cấu <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Cơ cấu Tổ chức
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{orgUnitsCount || 11}</span>
                            <span className="text-[10px] text-slate-400">đơn vị / phòng ban</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-emerald-700">
                            Bao gồm các phòng ban và đơn vị trực thuộc
                        </div>
                    </div>
                </div>

                {/* 4. Khai báo kỹ năng chờ duyệt */}
                <div
                    onClick={() => onNavigate("skills")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-indigo-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-50 border border-indigo-100 text-indigo-600">
                            <Sparkles className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-indigo-600 group-hover:translate-x-0.5 transition">
                            Kỹ năng <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Kỹ năng chờ chuẩn hóa
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{pendingSkills.length}</span>
                            <span className="text-[10px] text-slate-400">yêu cầu thẩm định</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-indigo-700">
                            Ma trận năng lực chuyên môn
                        </div>
                    </div>
                </div>
            </div>

            {/* Quick Actions Bar */}
            <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-2xs">
                <div className="flex items-center justify-between mb-2">
                    <h2 className="text-xs font-bold uppercase tracking-wider text-slate-500">
                        Lối tắt Nghiệp vụ Nhân sự (HR Quick Actions)
                    </h2>
                </div>
                <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-6">
                    <button
                        type="button"
                        onClick={() => onNavigate("hrprofile")}
                        className="flex flex-col items-center justify-center gap-1.5 rounded-lg border border-slate-100 bg-slate-50/70 p-2.5 text-center transition hover:border-teal-200 hover:bg-teal-50/50 cursor-pointer"
                    >
                        <FileText className="h-4 w-4 text-teal-600" />
                        <span className="text-xs font-semibold text-slate-700">Hồ sơ nhân sự</span>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("leave")}
                        className="flex flex-col items-center justify-center gap-1.5 rounded-lg border border-slate-100 bg-slate-50/70 p-2.5 text-center transition hover:border-amber-200 hover:bg-amber-50/50 cursor-pointer"
                    >
                        <CalendarIcon className="h-4 w-4 text-amber-600" />
                        <span className="text-xs font-semibold text-slate-700">Quản lý Nghỉ phép</span>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("departments")}
                        className="flex flex-col items-center justify-center gap-1.5 rounded-lg border border-slate-100 bg-slate-50/70 p-2.5 text-center transition hover:border-emerald-200 hover:bg-emerald-50/50 cursor-pointer"
                    >
                        <Building2 className="h-4 w-4 text-emerald-600" />
                        <span className="text-xs font-semibold text-slate-700">Cơ cấu Phòng ban</span>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("working-calendar")}
                        className="flex flex-col items-center justify-center gap-1.5 rounded-lg border border-slate-100 bg-slate-50/70 p-2.5 text-center transition hover:border-blue-200 hover:bg-blue-50/50 cursor-pointer"
                    >
                        <CalendarDays className="h-4 w-4 text-blue-600" />
                        <span className="text-xs font-semibold text-slate-700">Lịch & Ngày lễ</span>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("skills")}
                        className="flex flex-col items-center justify-center gap-1.5 rounded-lg border border-slate-100 bg-slate-50/70 p-2.5 text-center transition hover:border-indigo-200 hover:bg-indigo-50/50 cursor-pointer"
                    >
                        <Sparkles className="h-4 w-4 text-indigo-600" />
                        <span className="text-xs font-semibold text-slate-700">Khung Kỹ năng</span>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("attendance")}
                        className="flex flex-col items-center justify-center gap-1.5 rounded-lg border border-slate-100 bg-slate-50/70 p-2.5 text-center transition hover:border-purple-200 hover:bg-purple-50/50 cursor-pointer"
                    >
                        <Clock className="h-4 w-4 text-purple-600" />
                        <span className="text-xs font-semibold text-slate-700">Chấm công</span>
                    </button>
                </div>
            </div>

            {/* Main Content: Left Split / Right Split */}
            <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
                {/* Left (2 Cols) */}
                <div className="space-y-4 lg:col-span-2">
                    {/* 1. Đơn nghỉ phép cần duyệt gấp */}
                    <div className="rounded-xl border border-slate-200 bg-white p-3.5 shadow-2xs">
                        <div className="flex items-center justify-between mb-3">
                            <div className="flex items-center gap-2">
                                <div className="flex h-6 w-6 items-center justify-center rounded-md bg-amber-50 text-amber-600">
                                    <Clock className="h-3.5 w-3.5" />
                                </div>
                                <h3 className="text-sm font-bold text-slate-900">
                                    Đơn nghỉ phép chờ duyệt ({pendingLeaves.length})
                                </h3>
                            </div>
                            <button
                                onClick={() => onNavigate("leave")}
                                className="inline-flex items-center gap-1 text-xs font-semibold text-amber-600 hover:text-amber-700 cursor-pointer"
                            >
                                Quản lý đơn <ChevronRight className="h-3 w-3" />
                            </button>
                        </div>

                        {pendingLeaves.length === 0 ? (
                            <div className="py-6 text-center text-xs text-slate-500 border border-dashed border-slate-200 rounded-lg">
                                Tuyệt vời! Hiện không có đơn nghỉ phép nào đang chờ duyệt.
                            </div>
                        ) : (
                            <div className="space-y-2">
                                {pendingLeaves.slice(0, 4).map((lr) => (
                                    <div
                                        key={lr.id}
                                        className="flex flex-wrap items-center justify-between gap-2 p-2.5 rounded-lg border border-slate-100 hover:bg-amber-50/30 transition"
                                    >
                                        <div className="min-w-0 flex-1">
                                            <div className="flex items-center gap-2">
                                                <span className="text-xs font-bold text-slate-900">
                                                    {lr.employeeName || `Nhân sự #${lr.employeeId}`}
                                                </span>
                                                <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] text-slate-600">
                                                    {lr.department || "Nhân viên"}
                                                </span>
                                                <span className="text-[11px] font-semibold text-amber-700">
                                                    {lr.leaveType === "ANNUAL" ? "Phép năm" : lr.leaveType === "SICK" ? "Nghỉ ốm" : lr.leaveType === "UNPAID" ? "Không lương" : "Việc riêng"} ({lr.daysCount} ngày / {lr.hoursDeducted}h)
                                                </span>
                                            </div>
                                            <p className="text-[11px] text-slate-500 mt-1">
                                                Từ {lr.startDate} đến {lr.endDate} {lr.reason && `· "${lr.reason}"`}
                                            </p>
                                        </div>
                                        <div className="flex items-center gap-1.5">
                                            <button
                                                type="button"
                                                onClick={() => handleQuickApproveLeave(lr.id)}
                                                disabled={actionLoadingId === lr.id}
                                                className="inline-flex items-center gap-1 rounded-md bg-emerald-600 px-2.5 py-1 text-[11px] font-semibold text-white hover:bg-emerald-700 transition cursor-pointer disabled:opacity-50"
                                            >
                                                {actionLoadingId === lr.id ? (
                                                    <RefreshCw className="h-3 w-3 animate-spin" />
                                                ) : (
                                                    <CheckCircle2 className="h-3 w-3" />
                                                )}
                                                Duyệt nhanh
                                            </button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* 2. Danh sách nhân sự trong hệ thống */}
                    <div className="rounded-xl border border-slate-200 bg-white p-3.5 shadow-2xs">
                        <div className="flex items-center justify-between mb-3">
                            <div className="flex items-center gap-2">
                                <div className="flex h-6 w-6 items-center justify-center rounded-md bg-teal-50 text-teal-600">
                                    <Users className="h-3.5 w-3.5" />
                                </div>
                                <h3 className="text-sm font-bold text-slate-900">
                                    Hồ sơ nhân sự ({totalStaff})
                                </h3>
                            </div>
                            <button
                                onClick={() => onNavigate("hrprofile")}
                                className="inline-flex items-center gap-1 text-xs font-semibold text-teal-600 hover:text-teal-700 cursor-pointer"
                            >
                                Xem toàn bộ hồ sơ <ChevronRight className="h-3 w-3" />
                            </button>
                        </div>

                        {employees.length === 0 ? (
                            <div className="py-6 text-center text-xs text-slate-500 border border-dashed border-slate-200 rounded-lg">
                                Đang tải danh sách hồ sơ nhân sự...
                            </div>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="w-full text-left text-xs">
                                    <thead className="border-b border-slate-100 bg-slate-50/50 text-slate-500 uppercase text-[10px]">
                                        <tr>
                                            <th className="py-2 px-2.5 font-semibold">Mã NV</th>
                                            <th className="py-2 px-2.5 font-semibold">Họ và tên</th>
                                            <th className="py-2 px-2.5 font-semibold">Phòng ban</th>
                                            <th className="py-2 px-2.5 font-semibold">Chức danh / Vai trò</th>
                                            <th className="py-2 px-2.5 font-semibold text-right">Giờ chuẩn/Tuần</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-100 text-slate-700">
                                        {employees.slice(0, 5).map((emp) => (
                                            <tr key={emp.id} className="hover:bg-slate-50/60 transition">
                                                <td className="py-2 px-2.5 font-mono font-bold text-slate-900">
                                                    {emp.employeeCode}
                                                </td>
                                                <td className="py-2 px-2.5 font-semibold text-slate-900">
                                                    {emp.fullName}
                                                </td>
                                                <td className="py-2 px-2.5 text-slate-600">
                                                    {emp.orgUnitName || "Chưa gán"}
                                                </td>
                                                <td className="py-2 px-2.5 text-slate-500">
                                                    {emp.professionalRole || "Chuyên viên"}
                                                </td>
                                                <td className="py-2 px-2.5 text-right font-medium text-slate-700">
                                                    {emp.standardHoursPerWeek || 40}h
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                </div>

                {/* Right (1 Col) */}
                <div className="space-y-4">
                    {/* MiniCalendar */}
                    <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-2xs">
                        <MiniCalendar
                            miniCalMonth={miniCalMonth}
                            selectedDate={selectedDate}
                            now={now}
                            onSelectDate={setSelectedDate}
                            onChangeMonth={handleChangeMonth}
                        />
                    </div>

                    {/* HR Policy & Guidelines */}
                    <div className="rounded-xl border border-teal-100 bg-teal-50/40 p-3.5 text-xs text-teal-900">
                        <div className="flex items-center gap-1.5 font-bold mb-1 text-teal-800">
                            <ShieldCheck className="h-3.5 w-3.5 text-teal-600" /> Nhiệm vụ trọng tâm của Quản trị Nhân sự
                        </div>
                        <ul className="space-y-1.5 text-[11px] text-teal-800/90 list-disc list-inside mt-2">
                            <li><strong>Quản lý hồ sơ:</strong> Khởi tạo, cập nhật hợp đồng và thông tin nhân sự kịp thời.</li>
                            <li><strong>Duyệt phép & Ngày lễ:</strong> Phê duyệt đơn xin nghỉ và cập nhật lịch nghỉ lễ công ty theo QTN-10.</li>
                            <li><strong>Đồng bộ giờ chuẩn:</strong> Đảm bảo cấu hình giờ khả dụng chuẩn 40h/tuần để RM phân bổ dự án chính xác.</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    );
}
