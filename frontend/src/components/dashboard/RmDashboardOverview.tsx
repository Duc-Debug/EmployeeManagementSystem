import { useState, useEffect, useMemo } from "react";
import {
    Users,
    TrendingUp,
    AlertTriangle,
    CalendarRange,
    ArrowUpRight,
    RefreshCw,
    Briefcase,
    CalendarDays,
    BookOpen,
    CalendarCheck,
    CheckCircle2,
    Sparkles,
    ChevronRight,
} from "lucide-react";
import {
    getCompanyWeeklyCapacityMatrix,
    getResourceReservations,
    type EmployeeCapacityRow,
    type CapacityMatrixSummary,
    type ResourceReservationResult,
} from "@/lib/api/allocations";
import { getCurrentIsoWeek } from "../availability/availability.types";
import { useAuthUser } from "@/lib/auth-session";
import MiniCalendar from "../calendar/MiniCalendar";

interface RmDashboardOverviewProps {
    onNavigate: (tabId: string) => void;
}

export default function RmDashboardOverview({ onNavigate }: RmDashboardOverviewProps) {
    const user = useAuthUser();
    const currentIso = useMemo(() => getCurrentIsoWeek(), []);
    const [loading, setLoading] = useState(true);
    const [summary, setSummary] = useState<CapacityMatrixSummary | null>(null);
    const [rows, setRows] = useState<EmployeeCapacityRow[]>([]);
    const [reservations, setReservations] = useState<ResourceReservationResult[]>([]);
    const [selectedDate, setSelectedDate] = useState<Date>(new Date());
    const [miniCalMonth, setMiniCalMonth] = useState<Date>(new Date());
    const [now, setNow] = useState<Date>(new Date());

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 60000);
        return () => clearInterval(timer);
    }, []);

    const loadRmData = async () => {
        setLoading(true);
        try {
            const [matrixRes, reservationsRes] = await Promise.allSettled([
                getCompanyWeeklyCapacityMatrix({
                    fromYear: currentIso.year,
                    fromWeek: currentIso.weekNumber,
                    durationWeeks: 1,
                    size: 50,
                }),
                getResourceReservations({
                    year: currentIso.year,
                    weekNumber: currentIso.weekNumber,
                }),
            ]);

            if (matrixRes.status === "fulfilled" && matrixRes.value) {
                setSummary(matrixRes.value.summary);
                setRows(matrixRes.value.rows || []);
            }
            if (reservationsRes.status === "fulfilled" && reservationsRes.value) {
                setReservations(reservationsRes.value || []);
            }
        } catch (err) {
            console.error("Failed to load RM dashboard data:", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadRmData();
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
    const userName = user?.fullName || user?.username || "Quản lý Nguồn lực";

    const handleChangeMonth = (offset: number) => {
        setMiniCalMonth(new Date(miniCalMonth.getFullYear(), miniCalMonth.getMonth() + offset, 1));
    };

    const totalStaffCount = summary?.totalEmployees || rows.length;
    const avgUtilization = summary?.averageUtilization != null ? Math.round(summary.averageUtilization) : 0;
    const overloadedCount = summary?.overloadedEmployeesCount || 0;
    const underutilizedCount = summary?.underutilizedCellsCount || 0;
    const activeReservationsCount = reservations.filter((r) => r.status === "ACTIVE").length;

    return (
        <div className="space-y-4">
            {/* Header Clean White */}
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-3">
                <div>
                    <div className="flex items-center gap-2">
                        <h1 className="text-xl font-bold tracking-tight text-slate-900">
                            Xin chào, {userName}!
                        </h1>
                        <span className="inline-flex items-center gap-1 rounded-md bg-emerald-50 border border-emerald-200 px-2 py-0.5 text-[11px] font-bold text-emerald-700">
                            <Briefcase className="h-3 w-3" /> Resource Manager
                        </span>
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">
                        {formattedDateCapitalized} · Bảng điều khiển năng lực nhân sự, phân bổ công suất và điều phối nguồn lực (Tuần {currentIso.weekNumber}/{currentIso.year}).
                    </p>
                </div>

                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={loadRmData}
                        disabled={loading}
                        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-2xs cursor-pointer disabled:opacity-50"
                        title="Tải lại dữ liệu"
                    >
                        <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin text-emerald-600" : "text-slate-500"}`} />
                        <span>Làm mới</span>
                    </button>
                    <button
                        type="button"
                        onClick={() => onNavigate("capacity")}
                        className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-emerald-700 transition shadow-xs cursor-pointer"
                    >
                        <CalendarRange className="h-3.5 w-3.5" />
                        <span>Phân bổ nguồn lực</span>
                    </button>
                </div>
            </div>

            {/* KPI Cards (Gọn gàng, chuẩn UX Supply-Side) */}
            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
                {/* 1. Tổng nhân sự phụ trách */}
                <div
                    onClick={() => onNavigate("hrprofile")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-emerald-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-50 border border-emerald-100 text-emerald-600">
                            <Users className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-emerald-600 group-hover:translate-x-0.5 transition">
                            Hồ sơ <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Nhân sự Quản lý
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{totalStaffCount}</span>
                            <span className="text-[10px] text-slate-400">thành viên</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-emerald-600">
                            Phạm vi nguồn lực bộ phận
                        </div>
                    </div>
                </div>

                {/* 2. Hiệu suất bình quân tuần */}
                <div
                    onClick={() => onNavigate("capacity")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-blue-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-50 border border-blue-100 text-blue-600">
                            <TrendingUp className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-blue-600 group-hover:translate-x-0.5 transition">
                            Ma trận <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Hiệu suất Tuần {currentIso.weekNumber}
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{avgUtilization}%</span>
                            <span className="text-[10px] text-slate-400">công suất</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-blue-600">
                            {avgUtilization >= 80 && avgUtilization <= 100 ? "Mức độ sử dụng tối ưu" : avgUtilization > 100 ? "Cần san tải công việc" : "Còn nhiều giờ trống"}
                        </div>
                    </div>
                </div>

                {/* 3. Cảnh báo quá tải / dưới tải */}
                <div
                    onClick={() => onNavigate("capacity")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-amber-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-amber-50 border border-amber-100 text-amber-600">
                            <AlertTriangle className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-amber-600 group-hover:translate-x-0.5 transition">
                            Chi tiết <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Cảnh báo Phân bổ
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-2">
                            <span className={`text-lg font-bold ${overloadedCount > 0 ? "text-rose-600" : "text-slate-900"}`}>
                                {overloadedCount} <span className="text-[10px] font-normal text-slate-500">quá tải</span>
                            </span>
                            <span className="text-slate-300">|</span>
                            <span className="text-lg font-bold text-amber-600">
                                {underutilizedCount} <span className="text-[10px] font-normal text-slate-500">non tải</span>
                            </span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-amber-700">
                            {overloadedCount > 0 ? "Cần điều phối giảm tải" : "Không có nhân sự quá tải"}
                        </div>
                    </div>
                </div>

                {/* 4. Giữ chỗ Nguồn lực */}
                <div
                    onClick={() => onNavigate("capacity")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-purple-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-purple-50 border border-purple-100 text-purple-600">
                            <CalendarRange className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-purple-600 group-hover:translate-x-0.5 transition">
                            QTN-13 <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Giữ chỗ Nguồn lực (Tuần {currentIso.weekNumber})
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{activeReservationsCount}</span>
                            <span className="text-[10px] text-slate-400">yêu cầu active</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-purple-600">
                            {reservations.length} lượt giữ chỗ trong tuần
                        </div>
                    </div>
                </div>
            </div>

            {/* Quick Actions Bar for RM */}
            <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-2xs">
                <div className="flex items-center gap-1.5 mb-2">
                    <Sparkles className="h-3.5 w-3.5 text-emerald-600" />
                    <h3 className="text-[10px] font-bold text-slate-900 uppercase tracking-wider">
                        Lối tắt Tác vụ Quản lý Nguồn lực (RM Shortcuts)
                    </h3>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                    <button
                        type="button"
                        onClick={() => onNavigate("capacity")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-emerald-300 hover:bg-emerald-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-emerald-600 text-white shadow-2xs">
                            <CalendarRange className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Bảng Năng lực & Phân bổ</span>
                            <p className="text-[9px] text-slate-400 truncate">Ma trận tải & Phân bổ</p>
                        </div>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("skills")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-blue-300 hover:bg-blue-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-blue-600 text-white shadow-2xs">
                            <BookOpen className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Quản lý & Duyệt Kỹ năng</span>
                            <p className="text-[9px] text-slate-400 truncate">Đánh giá & Duyệt Level</p>
                        </div>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("leave")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-amber-300 hover:bg-amber-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-amber-600 text-white shadow-2xs">
                            <CalendarCheck className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Lịch Nghỉ Bộ phận</span>
                            <p className="text-[9px] text-slate-400 truncate">Duyệt & Nắm lịch nghỉ</p>
                        </div>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("availability")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-purple-300 hover:bg-purple-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-purple-600 text-white shadow-2xs">
                            <CalendarDays className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Giờ Khả dụng Nhân sự</span>
                            <p className="text-[9px] text-slate-400 truncate">Giờ chuẩn trừ lễ/nghỉ</p>
                        </div>
                    </button>
                </div>
            </div>

            {/* Bảng Giám sát Tải Nhân sự Tuần này */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                <div className="flex items-center justify-between mb-3">
                    <div>
                        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                            Giám sát Năng lực Nhân sự Tuần {currentIso.weekNumber}/{currentIso.year}
                        </h3>
                        <p className="text-[11px] text-slate-400 mt-0.5">
                            Tỷ lệ sử dụng năng lực và số giờ đã phân bổ vào dự án
                        </p>
                    </div>
                    <button
                        type="button"
                        onClick={() => onNavigate("capacity")}
                        className="text-xs font-semibold text-emerald-600 hover:text-emerald-800 transition cursor-pointer"
                    >
                        Mở Bảng năng lực đầy đủ ({rows.length}) →
                    </button>
                </div>

                <div className="overflow-x-auto rounded-xl border border-slate-100">
                    <table className="w-full text-left text-xs border-collapse">
                        <thead>
                            <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                                <th className="px-4 py-2.5">Nhân sự</th>
                                <th className="px-4 py-2.5">Phòng ban</th>
                                <th className="px-4 py-2.5">Vị trí</th>
                                <th className="px-4 py-2.5 text-center">Khả dụng</th>
                                <th className="px-4 py-2.5 text-center">Đã phân bổ</th>
                                <th className="px-4 py-2.5 w-48">Tỷ lệ tải (%)</th>
                                <th className="px-4 py-2.5 text-center">Trạng thái</th>
                                <th className="px-4 py-2.5 text-right">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {rows.length === 0 ? (
                                <tr>
                                    <td colSpan={8} className="py-8 text-center text-slate-400 text-xs">
                                        Chưa có dữ liệu năng lực nhân sự tuần này.
                                    </td>
                                </tr>
                            ) : (
                                rows.slice(0, 8).map((r) => {
                                    const cell = r.cells && r.cells.length > 0 ? r.cells[0] : null;
                                    const availHours = cell?.availableHours ?? 40;
                                    const allocHours = cell?.allocatedHours ?? 0;
                                    const utilPct = cell?.utilizationPercentage != null ? Math.round(cell.utilizationPercentage) : 0;
                                    const isOver = cell?.isOverloaded || utilPct > 100;
                                    const isOptimal = utilPct >= 80 && utilPct <= 100;

                                    return (
                                        <tr key={r.employeeId} className="hover:bg-slate-50/70 transition">
                                            <td className="px-4 py-2.5">
                                                <div className="flex items-center gap-2.5">
                                                    <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-[10px] font-bold text-emerald-800">
                                                        {r.fullName.substring(0, 2).toUpperCase()}
                                                    </div>
                                                    <div className="min-w-0">
                                                        <p className="font-semibold text-slate-900 truncate">{r.fullName}</p>
                                                        <p className="text-[10px] text-slate-400 font-mono">{r.employeeCode}</p>
                                                    </div>
                                                </div>
                                            </td>
                                            <td className="px-4 py-2.5 text-slate-600 text-[11px]">
                                                {r.orgUnitName || "Chưa phân bổ"}
                                            </td>
                                            <td className="px-4 py-2.5 text-slate-700 font-medium text-[11px]">
                                                {r.professionalRole || "Nhân viên"}
                                            </td>
                                            <td className="px-4 py-2.5 text-center font-mono font-semibold text-slate-700">
                                                {availHours}h
                                            </td>
                                            <td className="px-4 py-2.5 text-center font-mono font-bold text-indigo-700">
                                                {allocHours}h
                                            </td>
                                            <td className="px-4 py-2.5">
                                                <div className="flex items-center gap-2">
                                                    <div className="h-2 flex-1 rounded-full bg-slate-100 overflow-hidden">
                                                        <div
                                                            className={`h-full rounded-full transition-all ${
                                                                isOver
                                                                    ? "bg-rose-500"
                                                                    : isOptimal
                                                                    ? "bg-emerald-500"
                                                                    : "bg-amber-400"
                                                            }`}
                                                            style={{ width: `${Math.min(100, utilPct)}%` }}
                                                        />
                                                    </div>
                                                    <span className={`text-[10px] font-bold font-mono w-9 text-right ${
                                                        isOver ? "text-rose-600" : isOptimal ? "text-emerald-700" : "text-amber-700"
                                                    }`}>
                                                        {utilPct}%
                                                    </span>
                                                </div>
                                            </td>
                                            <td className="px-4 py-2.5 text-center">
                                                <span
                                                    className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                                                        isOver
                                                            ? "bg-rose-50 text-rose-700 border border-rose-200"
                                                            : isOptimal
                                                            ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                                                            : "bg-amber-50 text-amber-700 border border-amber-200"
                                                    }`}
                                                >
                                                    <span className={`h-1.5 w-1.5 rounded-full ${
                                                        isOver ? "bg-rose-500" : isOptimal ? "bg-emerald-500" : "bg-amber-500"
                                                    }`} />
                                                    {isOver ? "Quá tải" : isOptimal ? "Tối ưu" : "Non tải"}
                                                </span>
                                            </td>
                                            <td className="px-4 py-2.5 text-right">
                                                <button
                                                    type="button"
                                                    onClick={() => onNavigate("capacity")}
                                                    className="inline-flex items-center gap-0.5 text-xs font-semibold text-emerald-600 hover:text-emerald-800 transition cursor-pointer"
                                                >
                                                    Phân bổ <ChevronRight className="h-3 w-3" />
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

            {/* 2 Cột dưới: Lịch làm việc & Quy trình Quản lý Cung ứng Nguồn lực */}
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

                {/* Cột phải 2/3: Sơ đồ Quy trình Vận hành Nguồn lực RM */}
                <div className="lg:col-span-2 rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                    <div className="flex items-center gap-2 mb-3">
                        <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                            Quy trình Cung ứng & Điều phối Nguồn lực (RM Workflow)
                        </h3>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        <div className="rounded-xl border border-slate-100 bg-slate-50/60 p-3">
                            <div className="flex items-center gap-2 mb-1.5">
                                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-emerald-600 text-[10px] font-bold text-white">1</span>
                                <h4 className="text-xs font-bold text-slate-900">Giám sát Khả dụng & Lịch nghỉ</h4>
                            </div>
                            <p className="text-[11px] text-slate-500 leading-relaxed">
                                Kiểm tra số giờ khả dụng thực tế của từng nhân viên (đã trừ ngày lễ và lịch nghỉ phép đã duyệt).
                            </p>
                        </div>

                        <div className="rounded-xl border border-slate-100 bg-slate-50/60 p-3">
                            <div className="flex items-center gap-2 mb-1.5">
                                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-emerald-600 text-[10px] font-bold text-white">2</span>
                                <h4 className="text-xs font-bold text-slate-900">Quản lý Kỹ năng & Duyệt Level</h4>
                            </div>
                            <p className="text-[11px] text-slate-500 leading-relaxed">
                                Đánh giá và phê duyệt trình độ kỹ năng (Level 1-5) để đảm bảo nhân sự đáp ứng đúng tiêu chuẩn dự án.
                            </p>
                        </div>

                        <div className="rounded-xl border border-slate-100 bg-slate-50/60 p-3">
                            <div className="flex items-center gap-2 mb-1.5">
                                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-emerald-600 text-[10px] font-bold text-white">3</span>
                                <h4 className="text-xs font-bold text-slate-900">Phân bổ Nguồn lực Nhiều tuần</h4>
                            </div>
                            <p className="text-[11px] text-slate-500 leading-relaxed">
                                Sử dụng tính năng phân bổ hàng loạt theo % hoặc số giờ cố định vào các dự án theo yêu cầu của PM.
                            </p>
                        </div>

                        <div className="rounded-xl border border-slate-100 bg-slate-50/60 p-3">
                            <div className="flex items-center gap-2 mb-1.5">
                                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-emerald-600 text-[10px] font-bold text-white">4</span>
                                <h4 className="text-xs font-bold text-slate-900">San tải & Xử lý Quá tải (QTN-11)</h4>
                            </div>
                            <p className="text-[11px] text-slate-500 leading-relaxed">
                                Phát hiện kịp thời các tuần vượt công suất (&gt;100%) và xác nhận lý do quá tải hoặc điều phối san sẻ nhân sự.
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
