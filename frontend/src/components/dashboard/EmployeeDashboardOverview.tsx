import { useState, useEffect } from "react";
import {
    Calendar as CalendarIcon,
    Briefcase,
    Sparkles,
    ArrowUpRight,
    RefreshCw,
    CheckCircle2,
    Clock,
    Plus,
    CalendarDays,
    Star,
    UserCheck,
    ChevronRight,
    AlertCircle,
    Building2,
    CalendarClock,
    Shield,
} from "lucide-react";
import { useAuthUser } from "@/lib/auth-session";
import { getMyLeaveRequests, getMyLeaveBalance, type LeaveRequestDto, type LeaveBalanceDto } from "@/lib/api/leave";
import { getMySkills, type EmployeeSkillResponse } from "@/lib/api/skills";
import { getProjects, type ProjectResult } from "@/lib/api/projects";
import MiniCalendar from "../calendar/MiniCalendar";

interface EmployeeDashboardOverviewProps {
    onNavigate: (tabId: string) => void;
}

export default function EmployeeDashboardOverview({ onNavigate }: EmployeeDashboardOverviewProps) {
    const user = useAuthUser();
    const [loading, setLoading] = useState(true);
    const [leaveRequests, setLeaveRequests] = useState<LeaveRequestDto[]>([]);
    const [leaveBalance, setLeaveBalance] = useState<LeaveBalanceDto | null>(null);
    const [mySkills, setMySkills] = useState<EmployeeSkillResponse[]>([]);
    const [projects, setProjects] = useState<ProjectResult[]>([]);
    const [selectedDate, setSelectedDate] = useState<Date>(new Date());
    const [miniCalMonth, setMiniCalMonth] = useState<Date>(new Date());
    const [now, setNow] = useState<Date>(new Date());

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 60000);
        return () => clearInterval(timer);
    }, []);

    const loadEmployeeData = async () => {
        setLoading(true);
        try {
            const [leavesRes, balanceRes, skillsRes, projectsRes] = await Promise.allSettled([
                getMyLeaveRequests(),
                getMyLeaveBalance(),
                getMySkills(),
                getProjects(0, 50),
            ]);

            if (leavesRes.status === "fulfilled" && leavesRes.value) {
                setLeaveRequests(leavesRes.value);
            }
            if (balanceRes.status === "fulfilled" && balanceRes.value) {
                setLeaveBalance(balanceRes.value);
            }
            if (skillsRes.status === "fulfilled" && skillsRes.value) {
                setMySkills(skillsRes.value);
            }
            if (projectsRes.status === "fulfilled" && projectsRes.value) {
                setProjects(projectsRes.value.content || []);
            }
        } catch (err) {
            console.error("Failed to load employee dashboard data:", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadEmployeeData();
    }, []);

    const today = new Date();
    const formattedDate = today.toLocaleDateString("vi-VN", {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
    });
    const formattedDateCapitalized =
        formattedDate.charAt(0).toUpperCase() + formattedDate.slice(1);
    const userName = user?.fullName || user?.username || "Nhân viên";

    const handleChangeMonth = (offset: number) => {
        setMiniCalMonth(new Date(miniCalMonth.getFullYear(), miniCalMonth.getMonth() + offset, 1));
    };

    // Derived statistics
    const activeProjects = projects.filter((p) => p.status === "ACTIVE");
    const pendingLeaves = leaveRequests.filter((l) => l.status === "PENDING");
    const approvedSkillsCount = mySkills.filter((s) => s.status === "APPROVED").length;

    const remainingDays = leaveBalance?.remainingDays ?? 12;
    const usedDays = leaveBalance?.usedDays ?? 0;
    const pendingDays = leaveBalance?.pendingDays ?? pendingLeaves.reduce((acc, cur) => acc + (cur.daysCount || 0), 0);

    const getLeaveStatusBadge = (status: string) => {
        switch (status) {
            case "APPROVED":
                return (
                    <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 border border-emerald-200 px-2 py-0.5 text-[10px] font-semibold text-emerald-700">
                        <CheckCircle2 className="h-2.5 w-2.5 text-emerald-600" /> Đã duyệt
                    </span>
                );
            case "PENDING":
                return (
                    <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 border border-amber-200 px-2 py-0.5 text-[10px] font-semibold text-amber-700">
                        <Clock className="h-2.5 w-2.5 text-amber-600" /> Chờ duyệt
                    </span>
                );
            case "REJECTED":
                return (
                    <span className="inline-flex items-center gap-1 rounded-full bg-rose-50 border border-rose-200 px-2 py-0.5 text-[10px] font-semibold text-rose-700">
                        <AlertCircle className="h-2.5 w-2.5 text-rose-600" /> Từ chối
                    </span>
                );
            default:
                return (
                    <span className="inline-flex items-center rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-600">
                        {status}
                    </span>
                );
        }
    };

    const getSkillStatusBadge = (status: string) => {
        switch (status) {
            case "APPROVED":
                return (
                    <span className="inline-flex items-center gap-0.5 rounded-full bg-emerald-50 px-2 py-0.5 text-[9px] font-semibold text-emerald-700">
                        <CheckCircle2 className="h-2.5 w-2.5" /> Đã chuẩn hóa
                    </span>
                );
            case "PENDING":
                return (
                    <span className="inline-flex items-center gap-0.5 rounded-full bg-amber-50 px-2 py-0.5 text-[9px] font-semibold text-amber-700">
                        <Clock className="h-2.5 w-2.5" /> Chờ RM duyệt
                    </span>
                );
            default:
                return (
                    <span className="inline-flex items-center rounded-full bg-slate-100 px-2 py-0.5 text-[9px] font-medium text-slate-600">
                        {status}
                    </span>
                );
        }
    };

    return (
        <div className="space-y-4">
            {/* Header Clean White */}
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-3">
                <div>
                    <div className="flex items-center gap-2">
                        <h1 className="text-xl font-bold tracking-tight text-slate-900">
                            Xin chào, {userName}!
                        </h1>
                        <span className="inline-flex items-center gap-1 rounded-md bg-sky-50 border border-sky-200 px-2 py-0.5 text-[11px] font-bold text-sky-800">
                            <UserCheck className="h-3 w-3 text-sky-700" /> Thành viên / Nhân sự
                        </span>
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">
                        {formattedDateCapitalized} · Bảng điều khiển cá nhân, lịch làm việc, phép năm và kỹ năng chuyên môn.
                    </p>
                </div>

                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={loadEmployeeData}
                        disabled={loading}
                        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-2xs cursor-pointer disabled:opacity-50"
                        title="Tải lại dữ liệu"
                    >
                        <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin text-sky-600" : "text-slate-500"}`} />
                        <span>Làm mới</span>
                    </button>
                    <button
                        type="button"
                        onClick={() => onNavigate("leave")}
                        className="inline-flex items-center gap-1.5 rounded-xl bg-sky-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-sky-700 transition shadow-xs cursor-pointer"
                    >
                        <Plus className="h-3.5 w-3.5" />
                        <span>Tạo đơn nghỉ phép</span>
                    </button>
                </div>
            </div>

            {/* KPI Cards (4 Cards Employee Overview) */}
            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
                {/* 1. Dự án đang tham gia */}
                <div
                    onClick={() => onNavigate("project")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-indigo-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-50 border border-indigo-100 text-indigo-600">
                            <Briefcase className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-indigo-600 group-hover:translate-x-0.5 transition">
                            Dự án <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Dự án tham gia
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{activeProjects.length}</span>
                            <span className="text-[10px] text-slate-400">dự án đang chạy</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-indigo-700">
                            {projects.length} tổng dự án trong hệ thống
                        </div>
                    </div>
                </div>

                {/* 2. Quỹ ngày phép năm */}
                <div
                    onClick={() => onNavigate("leave")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-emerald-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-50 border border-emerald-100 text-emerald-600">
                            <CalendarIcon className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-emerald-600 group-hover:translate-x-0.5 transition">
                            Phép năm <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Phép còn lại (Năm {today.getFullYear()})
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{remainingDays}</span>
                            <span className="text-[10px] text-slate-400">ngày khả dụng</span>
                        </div>
                        <div className="mt-1 flex items-center gap-1.5 text-[10px] font-medium">
                            <span className="text-slate-600">Đã nghỉ: <strong className="text-slate-800">{usedDays}d</strong></span>
                            <span className="text-slate-300">·</span>
                            <span className="text-amber-700 font-semibold">Chờ duyệt: {pendingDays}d</span>
                        </div>
                    </div>
                </div>

                {/* 3. Kỹ năng & Hồ sơ chuyên môn */}
                <div
                    onClick={() => onNavigate("skills")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-amber-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-amber-50 border border-amber-100 text-amber-600">
                            <Sparkles className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-amber-600 group-hover:translate-x-0.5 transition">
                            Kỹ năng <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Hồ sơ Kỹ năng
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{mySkills.length}</span>
                            <span className="text-[10px] text-slate-400">kỹ năng đã khai báo</span>
                        </div>
                        <div className="mt-1 flex items-center gap-1.5 text-[10px] font-medium">
                            <span className="text-emerald-700 font-bold">{approvedSkillsCount} Đã duyệt</span>
                            <span className="text-slate-300">·</span>
                            <span className="text-amber-700 font-bold">{mySkills.length - approvedSkillsCount} Chờ duyệt</span>
                        </div>
                    </div>
                </div>

                {/* 4. Giờ chuẩn tuần & Năng lực */}
                <div
                    onClick={() => onNavigate("availability")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-sky-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-sky-50 border border-sky-100 text-sky-600">
                            <CalendarClock className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-sky-600 group-hover:translate-x-0.5 transition">
                            Khả dụng <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Giờ chuẩn tuần
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">40h</span>
                            <span className="text-[10px] text-slate-400">/ tuần tiêu chuẩn</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-sky-700">
                            Đã tích hợp trừ tự động giờ nghỉ & lễ
                        </div>
                    </div>
                </div>
            </div>

            {/* Quick Actions Bar */}
            <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-2xs">
                <div className="flex items-center justify-between mb-2">
                    <h2 className="text-xs font-bold uppercase tracking-wider text-slate-500">
                        Lối tắt Thao tác Nhanh (Personal Actions)
                    </h2>
                </div>
                <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-6">
                    <button
                        type="button"
                        onClick={() => onNavigate("leave")}
                        className="flex flex-col items-center justify-center gap-1.5 rounded-lg border border-slate-100 bg-slate-50/70 p-2.5 text-center transition hover:border-sky-200 hover:bg-sky-50/50 cursor-pointer"
                    >
                        <CalendarIcon className="h-4 w-4 text-sky-600" />
                        <span className="text-xs font-semibold text-slate-700">Xin nghỉ phép</span>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("skills")}
                        className="flex flex-col items-center justify-center gap-1.5 rounded-lg border border-slate-100 bg-slate-50/70 p-2.5 text-center transition hover:border-amber-200 hover:bg-amber-50/50 cursor-pointer"
                    >
                        <Sparkles className="h-4 w-4 text-amber-600" />
                        <span className="text-xs font-semibold text-slate-700">Khai báo kỹ năng</span>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("availability")}
                        className="flex flex-col items-center justify-center gap-1.5 rounded-lg border border-slate-100 bg-slate-50/70 p-2.5 text-center transition hover:border-blue-200 hover:bg-blue-50/50 cursor-pointer"
                    >
                        <CalendarClock className="h-4 w-4 text-blue-600" />
                        <span className="text-xs font-semibold text-slate-700">Giờ khả dụng</span>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("project")}
                        className="flex flex-col items-center justify-center gap-1.5 rounded-lg border border-slate-100 bg-slate-50/70 p-2.5 text-center transition hover:border-indigo-200 hover:bg-indigo-50/50 cursor-pointer"
                    >
                        <Briefcase className="h-4 w-4 text-indigo-600" />
                        <span className="text-xs font-semibold text-slate-700">Dự án tham gia</span>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("attendance")}
                        className="flex flex-col items-center justify-center gap-1.5 rounded-lg border border-slate-100 bg-slate-50/70 p-2.5 text-center transition hover:border-emerald-200 hover:bg-emerald-50/50 cursor-pointer"
                    >
                        <Clock className="h-4 w-4 text-emerald-600" />
                        <span className="text-xs font-semibold text-slate-700">Chấm công</span>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("working-calendar")}
                        className="flex flex-col items-center justify-center gap-1.5 rounded-lg border border-slate-100 bg-slate-50/70 p-2.5 text-center transition hover:border-purple-200 hover:bg-purple-50/50 cursor-pointer"
                    >
                        <CalendarDays className="h-4 w-4 text-purple-600" />
                        <span className="text-xs font-semibold text-slate-700">Lịch & Ngày lễ</span>
                    </button>
                </div>
            </div>

            {/* Main Content: Left Split / Right Split */}
            <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
                {/* Left (2 Cols) */}
                <div className="space-y-4 lg:col-span-2">
                    {/* 1. Dự án đang tham gia / Hoạt động */}
                    <div className="rounded-xl border border-slate-200 bg-white p-3.5 shadow-2xs">
                        <div className="flex items-center justify-between mb-3">
                            <div className="flex items-center gap-2">
                                <div className="flex h-6 w-6 items-center justify-center rounded-md bg-indigo-50 text-indigo-600">
                                    <Briefcase className="h-3.5 w-3.5" />
                                </div>
                                <h3 className="text-sm font-bold text-slate-900">
                                    Dự án đang triển khai ({activeProjects.length})
                                </h3>
                            </div>
                            <button
                                onClick={() => onNavigate("project")}
                                className="inline-flex items-center gap-1 text-xs font-semibold text-indigo-600 hover:text-indigo-700 cursor-pointer"
                            >
                                Xem tất cả <ChevronRight className="h-3 w-3" />
                            </button>
                        </div>

                        {activeProjects.length === 0 ? (
                            <div className="py-8 text-center text-xs text-slate-500 border border-dashed border-slate-200 rounded-lg">
                                Hiện chưa có dự án đang hoạt động nào.
                            </div>
                        ) : (
                            <div className="space-y-2">
                                {activeProjects.slice(0, 4).map((proj) => (
                                    <div
                                        key={proj.id}
                                        onClick={() => onNavigate("project")}
                                        className="flex flex-wrap items-center justify-between gap-2 p-2.5 rounded-lg border border-slate-100 hover:border-indigo-200 hover:bg-slate-50/60 transition cursor-pointer"
                                    >
                                        <div className="min-w-0 flex-1">
                                            <div className="flex items-center gap-2">
                                                <span className="text-xs font-bold text-slate-900 truncate">
                                                    {proj.projectName}
                                                </span>
                                                <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-mono text-slate-600">
                                                    {proj.projectCode}
                                                </span>
                                            </div>
                                            <div className="flex items-center gap-3 mt-1 text-[11px] text-slate-500">
                                                <span>Quản lý: <strong className="text-slate-700">{proj.managerId ? `PM #${proj.managerId}` : "Chưa gán"}</strong></span>
                                                <span>·</span>
                                                <span>Bắt đầu: {proj.startDate || "N/A"}</span>
                                                {proj.endDate && (
                                                    <>
                                                        <span>·</span>
                                                        <span>Hạn: {proj.endDate}</span>
                                                    </>
                                                )}
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <span className="inline-flex items-center rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-semibold text-emerald-700 border border-emerald-200">
                                                Active
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* 2. Đơn nghỉ phép cá nhân gần đây */}
                    <div className="rounded-xl border border-slate-200 bg-white p-3.5 shadow-2xs">
                        <div className="flex items-center justify-between mb-3">
                            <div className="flex items-center gap-2">
                                <div className="flex h-6 w-6 items-center justify-center rounded-md bg-sky-50 text-sky-600">
                                    <CalendarIcon className="h-3.5 w-3.5" />
                                </div>
                                <h3 className="text-sm font-bold text-slate-900">
                                    Lịch sử đơn nghỉ phép ({leaveRequests.length})
                                </h3>
                            </div>
                            <button
                                onClick={() => onNavigate("leave")}
                                className="inline-flex items-center gap-1 text-xs font-semibold text-sky-600 hover:text-sky-700 cursor-pointer"
                            >
                                Xem chi tiết & Tạo đơn <ChevronRight className="h-3 w-3" />
                            </button>
                        </div>

                        {leaveRequests.length === 0 ? (
                            <div className="py-6 text-center text-xs text-slate-500 border border-dashed border-slate-200 rounded-lg">
                                Bạn chưa có yêu cầu nghỉ phép nào gần đây.
                            </div>
                        ) : (
                            <div className="space-y-2">
                                {leaveRequests.slice(0, 4).map((lr) => (
                                    <div
                                        key={lr.id}
                                        className="flex flex-wrap items-center justify-between gap-2 p-2.5 rounded-lg border border-slate-100 hover:bg-slate-50/50 transition"
                                    >
                                        <div>
                                            <div className="flex items-center gap-2">
                                                <span className="text-xs font-semibold text-slate-800">
                                                    {lr.leaveType === "ANNUAL" ? "Nghỉ phép năm" : lr.leaveType === "SICK" ? "Nghỉ ốm" : lr.leaveType === "UNPAID" ? "Nghỉ không lương" : "Nghỉ việc riêng"}
                                                </span>
                                                <span className="text-xs text-slate-400">({lr.daysCount} ngày / {lr.hoursDeducted}h)</span>
                                            </div>
                                            <p className="text-[11px] text-slate-500 mt-0.5">
                                                Thời gian: {lr.startDate} → {lr.endDate} {lr.reason && `· Lý do: ${lr.reason}`}
                                            </p>
                                        </div>
                                        <div>
                                            {getLeaveStatusBadge(lr.status)}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* 3. Kỹ năng đã khai báo */}
                    <div className="rounded-xl border border-slate-200 bg-white p-3.5 shadow-2xs">
                        <div className="flex items-center justify-between mb-3">
                            <div className="flex items-center gap-2">
                                <div className="flex h-6 w-6 items-center justify-center rounded-md bg-amber-50 text-amber-600">
                                    <Sparkles className="h-3.5 w-3.5" />
                                </div>
                                <h3 className="text-sm font-bold text-slate-900">
                                    Kỹ năng cá nhân ({mySkills.length})
                                </h3>
                            </div>
                            <button
                                onClick={() => onNavigate("skills")}
                                className="inline-flex items-center gap-1 text-xs font-semibold text-amber-600 hover:text-amber-700 cursor-pointer"
                            >
                                Khai báo thêm <ChevronRight className="h-3 w-3" />
                            </button>
                        </div>

                        {mySkills.length === 0 ? (
                            <div className="py-6 text-center text-xs text-slate-500 border border-dashed border-slate-200 rounded-lg">
                                Bạn chưa khai báo kỹ năng chuyên môn nào.
                            </div>
                        ) : (
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                                {mySkills.map((sk) => (
                                    <div
                                        key={sk.id}
                                        className="flex items-center justify-between p-2.5 rounded-lg border border-slate-100 bg-slate-50/40"
                                    >
                                        <div>
                                            <div className="text-xs font-bold text-slate-800">{sk.skillName}</div>
                                            <div className="flex items-center gap-1.5 mt-1">
                                                <div className="flex items-center text-amber-500">
                                                    {[...Array(5)].map((_, i) => (
                                                        <Star
                                                            key={i}
                                                            className={`h-3 w-3 ${i < (sk.proficiencyLevel || 1) ? "fill-amber-400 text-amber-400" : "text-slate-200"}`}
                                                        />
                                                    ))}
                                                </div>
                                                <span className="text-[10px] text-slate-500 font-medium">
                                                    ({sk.yearsOfExperience || 0} năm KN)
                                                </span>
                                            </div>
                                        </div>
                                        <div>
                                            {getSkillStatusBadge(sk.status)}
                                        </div>
                                    </div>
                                ))}
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

                    {/* Personal Card Profile */}
                    <div className="rounded-xl border border-slate-200 bg-white p-3.5 shadow-2xs">
                        <div className="flex items-center gap-2 mb-3">
                            <div className="flex h-6 w-6 items-center justify-center rounded-md bg-blue-50 text-blue-600">
                                <Building2 className="h-3.5 w-3.5" />
                            </div>
                            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500">
                                Thông tin tài khoản
                            </h3>
                        </div>

                        <div className="space-y-2 text-xs">
                            <div className="flex justify-between py-1 border-b border-slate-100">
                                <span className="text-slate-500">Mã nhân sự:</span>
                                <span className="font-mono font-bold text-slate-800">{user?.employeeCode || "NV001"}</span>
                            </div>
                            <div className="flex justify-between py-1 border-b border-slate-100">
                                <span className="text-slate-500">Họ và tên:</span>
                                <span className="font-semibold text-slate-800">{user?.fullName || user?.username}</span>
                            </div>
                            <div className="flex justify-between py-1 border-b border-slate-100">
                                <span className="text-slate-500">Phòng ban:</span>
                                <span className="font-semibold text-slate-800">{user?.orgUnitName || "Ban Phát triển Phần mềm"}</span>
                            </div>
                            <div className="flex justify-between py-1 border-b border-slate-100">
                                <span className="text-slate-500">Vai trò hệ thống:</span>
                                <span className="font-semibold text-sky-700">{user?.roleName || "Nhân viên"}</span>
                            </div>
                            <div className="flex justify-between py-1">
                                <span className="text-slate-500">Email:</span>
                                <span className="text-slate-700 truncate max-w-[160px]">{user?.email || "employee@company.com"}</span>
                            </div>
                        </div>
                    </div>

                    {/* Workplace Guidelines */}
                    <div className="rounded-xl border border-sky-100 bg-sky-50/40 p-3.5 text-xs text-sky-900">
                        <div className="flex items-center gap-1.5 font-bold mb-1 text-sky-800">
                            <Shield className="h-3.5 w-3.5 text-sky-600" /> Lưu ý & Quy định nội bộ
                        </div>
                        <ul className="space-y-1 text-[11px] text-sky-800/80 list-disc list-inside mt-2">
                            <li>Nộp đơn nghỉ phép trước ít nhất <strong>03 ngày</strong> làm việc đối với phép năm.</li>
                            <li>Khai báo kỹ năng chuyên môn định kỳ để được RM và Trưởng nhóm xem xét nâng cấp bậc.</li>
                            <li>Tuân thủ giờ làm việc chuẩn 40h/tuần và theo dõi bảng phân bổ dự án.</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    );
}
