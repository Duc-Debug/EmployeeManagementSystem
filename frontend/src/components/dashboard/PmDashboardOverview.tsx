import { useState, useEffect } from "react";
import {
    FolderKanban,
    Users,
    Clock,
    AlertTriangle,
    ArrowUpRight,
    Plus,
    RefreshCw,
    Briefcase,
    CalendarDays,
    BookOpen,
    Layers,
    CheckCircle2,
    Calendar as CalendarIcon,
    ChevronRight,
    Sparkles,
} from "lucide-react";
import { getProjects, type ProjectResult } from "@/lib/api/projects";
import { getResourceReservations, type ResourceReservationResult } from "@/lib/api/allocations";
import { useAuthUser } from "@/lib/auth-session";
import MiniCalendar from "../calendar/MiniCalendar";

interface PmDashboardOverviewProps {
    onNavigate: (tabId: string) => void;
}

export default function PmDashboardOverview({ onNavigate }: PmDashboardOverviewProps) {
    const user = useAuthUser();
    const [loading, setLoading] = useState(true);
    const [projects, setProjects] = useState<ProjectResult[]>([]);
    const [reservations, setReservations] = useState<ResourceReservationResult[]>([]);
    const [selectedDate, setSelectedDate] = useState<Date>(new Date());
    const [miniCalMonth, setMiniCalMonth] = useState<Date>(new Date());
    const [now, setNow] = useState<Date>(new Date());

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 60000);
        return () => clearInterval(timer);
    }, []);

    const loadPmData = async () => {
        setLoading(true);
        try {
            const [projectsRes, reservationsRes] = await Promise.allSettled([
                getProjects(0, 100),
                getResourceReservations(),
            ]);

            if (projectsRes.status === "fulfilled" && projectsRes.value) {
                setProjects(projectsRes.value.content || []);
            }
            if (reservationsRes.status === "fulfilled" && reservationsRes.value) {
                setReservations(reservationsRes.value || []);
            }
        } catch (err) {
            console.error("Failed to load PM dashboard data:", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadPmData();
    }, []);

    // Derived statistics
    const activeProjects = projects.filter((p) => p.status === "ACTIVE" || p.status === "PLANNED");
    const totalEstimatedHours = activeProjects.reduce((sum, p) => sum + (p.estimatedHours || 0), 0);
    const missingEndDateProjects = projects.filter((p) => !p.endDate && p.status !== "CLOSED");
    const activeReservationsCount = reservations.filter((r) => r.status === "ACTIVE").length;

    const today = new Date();
    const formattedDate = today.toLocaleDateString("vi-VN", {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
    });
    const formattedDateCapitalized =
        formattedDate.charAt(0).toUpperCase() + formattedDate.slice(1);
    const userName = user?.fullName || user?.username || "Quản lý Dự án";

    const handleChangeMonth = (offset: number) => {
        setMiniCalMonth(new Date(miniCalMonth.getFullYear(), miniCalMonth.getMonth() + offset, 1));
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
                        <span className="inline-flex items-center gap-1 rounded-md bg-indigo-50 border border-indigo-200 px-2 py-0.5 text-[11px] font-bold text-indigo-700">
                            <Briefcase className="h-3 w-3" /> Project Manager
                        </span>
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">
                        {formattedDateCapitalized} · Bảng điều khiển tiến độ, phân bổ nguồn lực và nhiệm vụ dự án.
                    </p>
                </div>

                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={loadPmData}
                        disabled={loading}
                        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-2xs cursor-pointer disabled:opacity-50"
                        title="Tải lại dữ liệu"
                    >
                        <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin text-indigo-600" : "text-slate-500"}`} />
                        <span>Làm mới</span>
                    </button>
                    <button
                        type="button"
                        onClick={() => onNavigate("project")}
                        className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs cursor-pointer"
                    >
                        <Plus className="h-3.5 w-3.5" />
                        <span>+ Dự án mới</span>
                    </button>
                </div>
            </div>

            {/* KPI Cards (Gọn gàng, chuẩn UX) */}
            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
                {/* 1. Dự án quản lý */}
                <div
                    onClick={() => onNavigate("project")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-indigo-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-50 border border-indigo-100 text-indigo-600">
                            <FolderKanban className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-indigo-600 group-hover:translate-x-0.5 transition">
                            Chi tiết <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Dự án đang thực hiện
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{activeProjects.length}</span>
                            <span className="text-[10px] text-slate-400">/ {projects.length} tổng dự án</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-indigo-600">
                            {projects.filter(p => p.status === "ACTIVE").length} Đang chạy · {projects.filter(p => p.status === "PLANNED").length} Kế hoạch
                        </div>
                    </div>
                </div>

                {/* 2. Tổng Giờ Kế hoạch */}
                <div
                    onClick={() => onNavigate("project")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-blue-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-50 border border-blue-100 text-blue-600">
                            <Clock className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-blue-600 group-hover:translate-x-0.5 transition">
                            WBS <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Tổng Giờ Kế hoạch (WBS)
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{totalEstimatedHours.toLocaleString("vi-VN")}</span>
                            <span className="text-[10px] text-slate-400">giờ</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-blue-600">
                            Theo khối lượng công việc
                        </div>
                    </div>
                </div>

                {/* 3. Giữ chỗ Nguồn lực */}
                <div
                    onClick={() => onNavigate("capacity")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-purple-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-purple-50 border border-purple-100 text-purple-600">
                            <Users className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-purple-600 group-hover:translate-x-0.5 transition">
                            Phân bổ <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Giữ chỗ Nguồn lực (QTN-13)
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{activeReservationsCount}</span>
                            <span className="text-[10px] text-slate-400">yêu cầu active</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-purple-600">
                            {reservations.length} lượt giữ chỗ tổng cộng
                        </div>
                    </div>
                </div>

                {/* 4. Cảnh báo rủi ro */}
                <div
                    onClick={() => onNavigate("project")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-amber-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-amber-50 border border-amber-100 text-amber-600">
                            <AlertTriangle className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-amber-600 group-hover:translate-x-0.5 transition">
                            Kiểm tra <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Cần cập nhật ngày kết thúc
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className={`text-lg font-bold ${missingEndDateProjects.length > 0 ? "text-amber-600" : "text-emerald-600"}`}>
                                {missingEndDateProjects.length}
                            </span>
                            <span className="text-[10px] text-slate-400">dự án thiếu ngày KT</span>
                        </div>
                        <div className="mt-1 text-[10px] font-medium text-amber-700">
                            {missingEndDateProjects.length > 0 ? "Ảnh hưởng ước lượng nhân lực" : "Dữ liệu thời gian đầy đủ"}
                        </div>
                    </div>
                </div>
            </div>

            {/* Quick Actions Bar for PM */}
            <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-2xs">
                <div className="flex items-center gap-1.5 mb-2">
                    <Sparkles className="h-3.5 w-3.5 text-indigo-600" />
                    <h3 className="text-[10px] font-bold text-slate-900 uppercase tracking-wider">
                        Lối tắt Tác vụ Quản lý Dự án (PM Shortcuts)
                    </h3>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                    <button
                        type="button"
                        onClick={() => onNavigate("project")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-indigo-300 hover:bg-indigo-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-indigo-600 text-white shadow-2xs">
                            <FolderKanban className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Quản lý Dự án & WBS</span>
                            <p className="text-[9px] text-slate-400 truncate">Phân rã việc, gán task</p>
                        </div>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("capacity")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-purple-300 hover:bg-purple-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-purple-600 text-white shadow-2xs">
                            <Layers className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Bảng Năng lực & Giữ chỗ</span>
                            <p className="text-[9px] text-slate-400 truncate">QTN-13 Giữ chỗ tuần</p>
                        </div>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("skills")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-emerald-300 hover:bg-emerald-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-emerald-600 text-white shadow-2xs">
                            <BookOpen className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Tìm kiếm Kỹ năng</span>
                            <p className="text-[9px] text-slate-400 truncate">Tra cứu nhân sự phù hợp</p>
                        </div>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("availability")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-amber-300 hover:bg-amber-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-amber-600 text-white shadow-2xs">
                            <CalendarDays className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Giờ Khả dụng Cá nhân</span>
                            <p className="text-[9px] text-slate-400 truncate">Xem giờ trống theo tuần</p>
                        </div>
                    </button>
                </div>
            </div>

            {/* Bảng Dự án đang phụ trách */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                <div className="flex items-center justify-between mb-3">
                    <div>
                        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                            Danh sách Dự án Đang Phụ Trách
                        </h3>
                        <p className="text-[11px] text-slate-400 mt-0.5">
                            Các dự án đang hoạt động và được phân quyền quản lý
                        </p>
                    </div>
                    <button
                        type="button"
                        onClick={() => onNavigate("project")}
                        className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition cursor-pointer"
                    >
                        Mở trang Dự án ({projects.length}) →
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
                                        Chưa có dữ liệu dự án nào. Bấm <b>+ Dự án mới</b> để bắt đầu.
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
                                                    <div className="min-w-0 max-w-[240px]">
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
                                                {p.estimatedHours ? `${p.estimatedHours}h` : "--"}
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
                                                    Vào WBS <ChevronRight className="h-3 w-3" />
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

            {/* 2 Cột dưới: Lịch làm việc & Hướng dẫn Quy trình Quản lý */}
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

                {/* Cột phải 2/3: Quy trình Quản lý Dự án & Nguồn lực Chuẩn */}
                <div className="lg:col-span-2 rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                    <div className="flex items-center gap-2 mb-3">
                        <CheckCircle2 className="h-4 w-4 text-indigo-600" />
                        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                            Quy trình Vận hành Nguồn lực & Dự án (PM Workflow)
                        </h3>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        <div className="rounded-xl border border-slate-100 bg-slate-50/60 p-3">
                            <div className="flex items-center gap-2 mb-1.5">
                                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-600 text-[10px] font-bold text-white">1</span>
                                <h4 className="text-xs font-bold text-slate-900">Thiết lập Dự án & WBS</h4>
                            </div>
                            <p className="text-[11px] text-slate-500 leading-relaxed">
                                Tạo dự án, cập nhật ngày bắt đầu - kết thúc và phân rã các hạng mục công việc (Task).
                            </p>
                        </div>

                        <div className="rounded-xl border border-slate-100 bg-slate-50/60 p-3">
                            <div className="flex items-center gap-2 mb-1.5">
                                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-600 text-[10px] font-bold text-white">2</span>
                                <h4 className="text-xs font-bold text-slate-900">Ước lượng Nhu cầu Nguồn lực</h4>
                            </div>
                            <p className="text-[11px] text-slate-500 leading-relaxed">
                                Khai báo số giờ cần thiết theo từng vai trò chuyên môn (Dev, Tester, BA, UI/UX).
                            </p>
                        </div>

                        <div className="rounded-xl border border-slate-100 bg-slate-50/60 p-3">
                            <div className="flex items-center gap-2 mb-1.5">
                                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-600 text-[10px] font-bold text-white">3</span>
                                <h4 className="text-xs font-bold text-slate-900">Giữ chỗ Nguồn lực (QTN-13)</h4>
                            </div>
                            <p className="text-[11px] text-slate-500 leading-relaxed">
                                Đăng ký giữ chỗ trước nhân sự theo tuần tại bảng năng lực để đảm bảo nguồn lực cho dự án.
                            </p>
                        </div>

                        <div className="rounded-xl border border-slate-100 bg-slate-50/60 p-3">
                            <div className="flex items-center gap-2 mb-1.5">
                                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-600 text-[10px] font-bold text-white">4</span>
                                <h4 className="text-xs font-bold text-slate-900">Giao việc & Theo dõi</h4>
                            </div>
                            <p className="text-[11px] text-slate-500 leading-relaxed">
                                Phân công công việc cụ thể cho các thành viên trong dự án và theo dõi tiến độ hoàn thành.
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
