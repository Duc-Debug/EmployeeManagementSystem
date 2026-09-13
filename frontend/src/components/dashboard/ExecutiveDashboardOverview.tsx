import { useState, useEffect, useMemo } from "react";
import {
    Building2,
    FolderKanban,
    TrendingUp,
    ArrowUpRight,
    RefreshCw,
    BookOpen,
    ShieldCheck,
    Sparkles,
    ChevronRight,
    AlertTriangle,
    Calendar as CalendarIcon,
} from "lucide-react";
import { getProjects, type ProjectResult } from "@/lib/api/projects";
import { getEmployees, type EmployeeProfile } from "@/lib/api/employees";
import { getOrgTree } from "@/lib/api/org-units";
import { flattenActiveOrgTree } from "@/lib/organization";
import { getCompanyWeeklyCapacityMatrix, type CapacityMatrixSummary } from "@/lib/api/allocations";
import { getSkills } from "@/lib/api/skills";
import { getCurrentIsoWeek } from "../availability/availability.types";
import { useAuthUser } from "@/lib/auth-session";
import MiniCalendar from "../calendar/MiniCalendar";

interface ExecutiveDashboardOverviewProps {
    onNavigate: (tabId: string) => void;
}

export default function ExecutiveDashboardOverview({ onNavigate }: ExecutiveDashboardOverviewProps) {
    const user = useAuthUser();
    const currentIso = useMemo(() => getCurrentIsoWeek(), []);
    const [loading, setLoading] = useState(true);
    const [projects, setProjects] = useState<ProjectResult[]>([]);
    const [employees, setEmployees] = useState<EmployeeProfile[]>([]);
    const [orgUnitsCount, setOrgUnitsCount] = useState(0);
    const [departmentsList, setDepartmentsList] = useState<{ id: number; name: string; managerName?: string; count: number }[]>([]);
    const [skillsCount, setSkillsCount] = useState(0);
    const [capacitySummary, setCapacitySummary] = useState<CapacityMatrixSummary | null>(null);

    const [selectedDate, setSelectedDate] = useState<Date>(new Date());
    const [miniCalMonth, setMiniCalMonth] = useState<Date>(new Date());
    const [now, setNow] = useState<Date>(new Date());

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 60000);
        return () => clearInterval(timer);
    }, []);

    const loadExecutiveData = async () => {
        setLoading(true);
        try {
            const [projectsRes, empRes, treeRes, matrixRes, skillsRes] = await Promise.allSettled([
                getProjects(0, 100),
                getEmployees(1, 100),
                getOrgTree(),
                getCompanyWeeklyCapacityMatrix({
                    fromYear: currentIso.year,
                    fromWeek: currentIso.weekNumber,
                    durationWeeks: 1,
                    size: 100,
                }),
                getSkills(),
            ]);

            if (projectsRes.status === "fulfilled" && projectsRes.value) {
                setProjects(projectsRes.value.content || []);
            }
            if (empRes.status === "fulfilled" && empRes.value) {
                setEmployees(empRes.value.content || []);
            }
            if (treeRes.status === "fulfilled" && treeRes.value) {
                const flat = flattenActiveOrgTree(treeRes.value);
                setOrgUnitsCount(flat.length);
                const depts = flat.map((u) => ({
                    id: u.id,
                    name: u.unitName,
                    managerName: (u as any).managerName,
                    count: (u as any).employeeCount || 0,
                }));
                setDepartmentsList(depts);
            }
            if (matrixRes.status === "fulfilled" && matrixRes.value) {
                setCapacitySummary(matrixRes.value.summary);
            }
            if (skillsRes.status === "fulfilled" && skillsRes.value) {
                setSkillsCount(skillsRes.value.length);
            }
        } catch (err) {
            console.error("Failed to load executive overview data:", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadExecutiveData();
    }, [currentIso.year, currentIso.weekNumber]);

    const today = new Date();
    const formattedDate = today.toLocaleDateString("vi-VN", {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
    });
    const formattedDateCapitalized =
        formattedDate.charAt(0).toUpperCase() + formattedDate.slice(1);
    const userName = user?.fullName || user?.username || "Ban Giám Đốc";

    const handleChangeMonth = (offset: number) => {
        setMiniCalMonth(new Date(miniCalMonth.getFullYear(), miniCalMonth.getMonth() + offset, 1));
    };

    // Derived metrics
    const totalStaff = employees.length || capacitySummary?.totalEmployees || 0;
    const activeProjects = projects.filter((p) => p.status === "ACTIVE");
    const plannedProjects = projects.filter((p) => p.status === "PLANNED");
    const closedProjects = projects.filter((p) => p.status === "CLOSED");
    const totalHours = activeProjects.reduce((sum, p) => sum + (p.estimatedHours || 0), 0);
    const avgUtilization = capacitySummary?.averageUtilization != null ? Math.round(capacitySummary.averageUtilization) : 0;
    const overloadedStaffCount = capacitySummary?.overloadedEmployeesCount || 0;

    return (
        <div className="space-y-4">
            {/* Header Clean White */}
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-3">
                <div>
                    <div className="flex items-center gap-2">
                        <h1 className="text-xl font-bold tracking-tight text-slate-900">
                            Xin chào, {userName}!
                        </h1>
                        <span className="inline-flex items-center gap-1 rounded-md bg-amber-50 border border-amber-200 px-2 py-0.5 text-[11px] font-bold text-amber-800">
                            <ShieldCheck className="h-3 w-3 text-amber-700" /> Ban Giám Đốc
                        </span>
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">
                        {formattedDateCapitalized} · Báo cáo tổng quan chiến lược, tài nguyên và danh mục dự án toàn doanh nghiệp.
                    </p>
                </div>

                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={loadExecutiveData}
                        disabled={loading}
                        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-2xs cursor-pointer disabled:opacity-50"
                        title="Tải lại dữ liệu"
                    >
                        <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin text-amber-600" : "text-slate-500"}`} />
                        <span>Làm mới</span>
                    </button>
                </div>
            </div>

            {/* KPI Cards (4 Cards C-Level Overview) */}
            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
                {/* 1. Tổng quy mô Nhân sự & Phòng ban */}
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
                            Quy mô Tổ chức
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{totalStaff}</span>
                            <span className="text-[10px] text-slate-400">nhân sự</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-emerald-700">
                            {orgUnitsCount || 11} đơn vị / phòng ban trực thuộc
                        </div>
                    </div>
                </div>

                {/* 2. Danh mục Dự án Toàn công ty */}
                <div
                    onClick={() => onNavigate("project")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-indigo-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-50 border border-indigo-100 text-indigo-600">
                            <FolderKanban className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-indigo-600 group-hover:translate-x-0.5 transition">
                            Dự án <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Danh mục Dự án
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{projects.length}</span>
                            <span className="text-[10px] text-slate-400">tổng số dự án</span>
                        </div>
                        <div className="mt-1 flex items-center gap-1.5 text-[10px] font-medium">
                            <span className="text-emerald-700 font-bold">{activeProjects.length} Chạy</span>
                            <span className="text-slate-300">·</span>
                            <span className="text-blue-700 font-bold">{plannedProjects.length} KH</span>
                            <span className="text-slate-300">·</span>
                            <span className="text-slate-500 font-bold">{closedProjects.length} Đóng</span>
                        </div>
                    </div>
                </div>

                {/* 3. Hiệu suất & Tải Nguồn lực Doanh nghiệp */}
                <div
                    onClick={() => onNavigate("capacity")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-blue-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-50 border border-blue-100 text-blue-600">
                            <TrendingUp className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-blue-600 group-hover:translate-x-0.5 transition">
                            Công suất <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Hiệu suất Công suất Tuần {currentIso.weekNumber}
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{avgUtilization}%</span>
                            <span className="text-[10px] text-slate-400">sử dụng</span>
                        </div>
                        <div className="mt-1 flex items-center gap-1.5 text-[10px] font-medium text-blue-700">
                            <span>{totalHours.toLocaleString("vi-VN")}h kế hoạch</span>
                            {overloadedStaffCount > 0 && (
                                <span className="text-rose-600 font-bold">({overloadedStaffCount} quá tải)</span>
                            )}
                        </div>
                    </div>
                </div>

                {/* 4. Năng lực & Tài sản Kỹ năng */}
                <div
                    onClick={() => onNavigate("skills")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-purple-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-purple-50 border border-purple-100 text-purple-600">
                            <BookOpen className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-purple-600 group-hover:translate-x-0.5 transition">
                            Ma trận <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Năng lực & Kỹ năng Chuẩn
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{skillsCount || 15}</span>
                            <span className="text-[10px] text-slate-400">kỹ năng chuẩn hóa</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-purple-700">
                            Ma trận năng lực Level 1 → 5
                        </div>
                    </div>
                </div>
            </div>

            {/* Strategic Quick Actions Bar for Executive */}
            <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-2xs">
                <div className="flex items-center gap-1.5 mb-2">
                    <Sparkles className="h-3.5 w-3.5 text-amber-600" />
                    <h3 className="text-[10px] font-bold text-slate-900 uppercase tracking-wider">
                        Lối tắt Tác vụ Ban Giám Đốc (Executive Shortcuts)
                    </h3>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                    <button
                        type="button"
                        onClick={() => onNavigate("departments")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-emerald-300 hover:bg-emerald-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-emerald-600 text-white shadow-2xs">
                            <Building2 className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Cây Cơ cấu Tổ chức</span>
                            <p className="text-[9px] text-slate-400 truncate">Phòng ban, nhân sự</p>
                        </div>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("capacity")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-blue-300 hover:bg-blue-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-blue-600 text-white shadow-2xs">
                            <TrendingUp className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Báo cáo Năng lực Tuần</span>
                            <p className="text-[9px] text-slate-400 truncate">Công suất toàn công ty</p>
                        </div>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("project")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-indigo-300 hover:bg-indigo-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-indigo-600 text-white shadow-2xs">
                            <FolderKanban className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Danh mục Dự án</span>
                            <p className="text-[9px] text-slate-400 truncate">Tiến độ & Giám sát WBS</p>
                        </div>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("skills")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-purple-300 hover:bg-purple-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-purple-600 text-white shadow-2xs">
                            <BookOpen className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Ma trận Kỹ năng</span>
                            <p className="text-[9px] text-slate-400 truncate">Năng lực đội ngũ</p>
                        </div>
                    </button>
                </div>
            </div>

            {/* Bảng Danh mục Dự án Chiến lược Đang Hoạt động */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                <div className="flex items-center justify-between mb-3">
                    <div>
                        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                            Danh mục Dự án Chiến lược Đang Hoạt động
                        </h3>
                        <p className="text-[11px] text-slate-400 mt-0.5">
                            Các dự án đang triển khai trên toàn doanh nghiệp
                        </p>
                    </div>
                    <button
                        type="button"
                        onClick={() => onNavigate("project")}
                        className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition cursor-pointer"
                    >
                        Xem tất cả ({projects.length}) →
                    </button>
                </div>

                <div className="overflow-x-auto rounded-xl border border-slate-100">
                    <table className="w-full text-left text-xs border-collapse">
                        <thead>
                            <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                                <th className="px-4 py-2.5">Dự án</th>
                                <th className="px-4 py-2.5">Mã DA</th>
                                <th className="px-4 py-2.5">Thời gian thực hiện</th>
                                <th className="px-4 py-2.5 text-right">Giờ kế hoạch</th>
                                <th className="px-4 py-2.5 text-center">Trạng thái</th>
                                <th className="px-4 py-2.5 text-right">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {projects.length === 0 ? (
                                <tr>
                                    <td colSpan={6} className="py-8 text-center text-slate-400 text-xs">
                                        Chưa có dự án nào được ghi nhận.
                                    </td>
                                </tr>
                            ) : (
                                projects.slice(0, 6).map((p) => {
                                    const hasNoEndDate = !p.endDate;
                                    return (
                                        <tr key={p.id} className="hover:bg-slate-50/70 transition">
                                            <td className="px-4 py-2.5">
                                                <div className="flex items-center gap-2.5">
                                                    <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600 border border-indigo-100">
                                                        <FolderKanban className="h-3.5 w-3.5" />
                                                    </div>
                                                    <div className="min-w-0 max-w-[260px]">
                                                        <p className="font-semibold text-slate-900 truncate">{p.projectName}</p>
                                                        <p className="text-[10px] text-slate-400 truncate">{p.description || "Chưa có mô tả"}</p>
                                                    </div>
                                                </div>
                                            </td>
                                            <td className="px-4 py-2.5 font-mono text-[11px] text-slate-700">
                                                {p.projectCode}
                                            </td>
                                            <td className="px-4 py-2.5 text-slate-600 text-[11px]">
                                                <div className="flex items-center gap-1.5">
                                                    <CalendarIcon className="h-3 w-3 text-slate-400" />
                                                    <span>{p.startDate || "--"}</span>
                                                    <span>→</span>
                                                    {hasNoEndDate ? (
                                                        <span className="inline-flex items-center gap-0.5 rounded px-1.5 py-0.5 text-[10px] font-semibold bg-amber-50 text-amber-700 border border-amber-200">
                                                            <AlertTriangle className="h-2.5 w-2.5" /> Chưa có
                                                        </span>
                                                    ) : (
                                                        <span>{p.endDate}</span>
                                                    )}
                                                </div>
                                            </td>
                                            <td className="px-4 py-2.5 text-right font-medium text-slate-700">
                                                {p.estimatedHours ? `${p.estimatedHours.toLocaleString("vi-VN")}h` : "--"}
                                            </td>
                                            <td className="px-4 py-2.5 text-center">
                                                <span
                                                    className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                                                        p.status === "ACTIVE"
                                                            ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                                                            : p.status === "PLANNED"
                                                            ? "bg-blue-50 text-blue-700 border border-blue-200"
                                                            : p.status === "CLOSED"
                                                            ? "bg-slate-100 text-slate-600 border border-slate-200"
                                                            : "bg-rose-50 text-rose-700 border border-rose-200"
                                                    }`}
                                                >
                                                    <span className={`h-1.5 w-1.5 rounded-full ${
                                                        p.status === "ACTIVE" ? "bg-emerald-500" :
                                                        p.status === "PLANNED" ? "bg-blue-500" :
                                                        p.status === "CLOSED" ? "bg-slate-400" : "bg-rose-500"
                                                    }`} />
                                                    {p.status === "ACTIVE" ? "Đang chạy" :
                                                     p.status === "PLANNED" ? "Kế hoạch" :
                                                     p.status === "CLOSED" ? "Đã đóng" : p.status}
                                                </span>
                                            </td>
                                            <td className="px-4 py-2.5 text-right">
                                                <button
                                                    type="button"
                                                    onClick={() => onNavigate("project")}
                                                    className="inline-flex items-center gap-0.5 text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition cursor-pointer"
                                                >
                                                    Xem WBS <ChevronRight className="h-3 w-3" />
                                                </button>
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* 2 Cột dưới: Lịch làm việc & Danh sách Phòng ban Trực thuộc */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
                {/* Cột trái 1/3: MiniCalendar */}
                <div className="lg:col-span-1">
                    <MiniCalendar
                        miniCalMonth={miniCalMonth}
                        selectedDate={selectedDate}
                        now={now}
                        onSelectDate={setSelectedDate}
                        onChangeMonth={handleChangeMonth}
                    />
                </div>

                {/* Cột phải 2/3: Phân bố Cơ cấu Đơn vị / Phòng ban */}
                <div className="lg:col-span-2 rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                    <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center gap-2">
                            <Building2 className="h-4 w-4 text-emerald-600" />
                            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                                Quy mô Các Đơn vị Trực thuộc
                            </h3>
                        </div>
                        <button
                            type="button"
                            onClick={() => onNavigate("departments")}
                            className="text-xs font-semibold text-emerald-600 hover:text-emerald-800 transition cursor-pointer"
                        >
                            Xem Cây tổ chức →
                        </button>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
                        {departmentsList.slice(0, 6).map((dept) => (
                            <div
                                key={dept.id}
                                onClick={() => onNavigate("departments")}
                                className="group rounded-xl border border-slate-100 bg-slate-50/70 p-3 hover:border-emerald-300 hover:bg-emerald-50/30 transition cursor-pointer"
                            >
                                <div className="flex items-center justify-between">
                                    <span className="text-xs font-bold text-slate-900 group-hover:text-emerald-700 transition truncate">
                                        {dept.name}
                                    </span>
                                    <span className="rounded-md bg-emerald-100 px-1.5 py-0.5 text-[10px] font-bold text-emerald-800">
                                        {dept.count} nv
                                    </span>
                                </div>
                                <p className="mt-1 text-[10px] text-slate-400 truncate">
                                    Trưởng phòng: {dept.managerName || "Chưa bổ nhiệm"}
                                </p>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}
