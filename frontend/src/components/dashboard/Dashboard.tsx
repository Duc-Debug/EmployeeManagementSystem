import { useState } from "react";
import {
    Menu,
    Bell,
    HelpCircle,
    Settings,
    LayoutDashboard,
    Users,
    Clock,
    Calendar as CalendarIcon,
    Building2,
    BarChart3,
    ShieldCheck,
    Download,
    Plus,
    ChevronDown,
    ChevronRight,
    ChevronLeft,
    List,
    FolderKanban,
    Network,
    ArrowUpRight,
    ArrowDownRight,
    Briefcase,
    UserCheck,
    MoreHorizontal,
} from "lucide-react";
import { cn } from "@/lib/utils";
import OrgChart from "./OrgChart.tsx";

const TOP_NAV = [
    "Bản tin",
    "Trình nhắn tin",
    "Lịch",
    "Tài liệu",
    "Bảng",
    "Drive",
    "Webmail",
    "Nhóm làm việc",
];

const SIDEBAR_WORKSPACE = [
    { name: "Tổng quan", icon: LayoutDashboard, id: "overview" },
    { name: "Nhân viên", icon: Users, id: "employees" },
    { name: "Chấm công", icon: Clock, id: "attendance" },
    { name: "Nghỉ phép", icon: CalendarIcon, id: "leave" },
    { name: "Phòng ban", icon: Building2, id: "departments" },
    { name: "Báo cáo", icon: BarChart3, id: "reports" },
];

const SIDEBAR_SETTINGS = [
    { name: "Quyền truy cập", icon: ShieldCheck, id: "access" },
    { name: "Thiết lập hệ thống", icon: Settings, id: "settings" },
];

const DEPARTMENTS_DATA = [
    { id: "dept-1", name: "Phòng Nhân sự", manager: "Nguyễn Minh Anh", count: 12, budget: "150M" },
    { id: "dept-2", name: "Phòng Công nghệ", manager: "Trần Quốc Bảo", count: 45, budget: "600M" },
    { id: "dept-3", name: "Phòng Marketing", manager: "Lê Thu Hà", count: 18, budget: "250M" },
    { id: "dept-4", name: "Phòng Kinh doanh", manager: "Phạm Hoàng Nam", count: 32, budget: "400M" },
    { id: "dept-5", name: "Phòng Tài chính", manager: "Võ Ngọc Linh", count: 8, budget: "120M" },
];

const TIME_SLOTS = [
    "12 AM",
    "9 AM",
    "10 AM",
    "11 AM",
    "12 PM",
    "1 PM",
    "2 PM",
    "3 PM",
    "4 PM",
    "5 PM",
    "6 PM",
    "7 PM",
    "12 PM",
];

export default function Dashboard() {
    const [activeTab, setActiveTab] = useState("overview");
    const [departmentSubTab, setDepartmentSubTab] = useState<"list" | "tree">("list");
    const [selectedDate, setSelectedDate] = useState(24);

    return (
        <div className="flex h-screen w-full flex-col bg-[#f4f5fa] text-[#1e1b4b] antialiased">
            {/* ---------- TOP HEADER ---------- */}
            <header className="flex h-[56px] w-full flex-none items-center justify-between bg-[#4338ca] px-4 text-white shadow-sm">
                <div className="flex items-center gap-6">
                    <button className="rounded-lg p-1 hover:bg-white/10">
                        <Menu className="h-5 w-5" />
                    </button>

                    <div className="flex items-center gap-2">
                        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-white text-xs font-black text-[#4338ca]">
                            P
                        </div>
                        <span className="text-lg font-bold tracking-tight">PeopleFlow</span>
                    </div>

                    <nav className="ml-4 hidden items-center gap-1 text-sm font-medium xl:flex">
                        {TOP_NAV.map((item, idx) => (
                            <button
                                key={idx}
                                className={cn(
                                    "rounded-md px-3 py-1.5 transition hover:bg-white/10",
                                    idx === 0 ? "text-white" : "text-white/80"
                                )}
                            >
                                {item}
                            </button>
                        ))}
                        <button className="flex items-center gap-1 rounded-md px-3 py-1.5 text-white/80 transition hover:bg-white/10">
                            Thêm <ChevronDown className="h-3.5 w-3.5" />
                        </button>
                    </nav>
                </div>

                <div className="flex items-center gap-3">
                    <button className="rounded-full p-1.5 hover:bg-white/10">
                        <HelpCircle className="h-5 w-5 text-white/80" />
                    </button>
                    <button className="rounded-full p-1.5 hover:bg-white/10">
                        <Bell className="h-5 w-5 text-white/80" />
                    </button>

                    <div className="ml-2 flex items-center gap-2 rounded-full bg-white/10 py-1 pl-1 pr-3 hover:bg-white/15 cursor-pointer">
                        <div className="flex h-7 w-7 items-center justify-center rounded-full bg-indigo-200 text-xs font-bold text-indigo-900">
                            QL
                        </div>
                        <span className="text-sm font-semibold">Quản lý</span>
                        <ChevronDown className="h-3.5 w-3.5 opacity-70" />
                    </div>

                    <button className="rounded-full p-1.5 hover:bg-white/10">
                        <Settings className="h-5 w-5 text-white/80" />
                    </button>
                </div>
            </header>

            {/* ---------- MAIN CONTAINER ---------- */}
            <div className="flex flex-1 overflow-hidden">
                {/* ---------- SIDEBAR LEFT ---------- */}
                <aside className="flex w-[240px] flex-none flex-col justify-between border-r border-slate-200 bg-white p-4">
                    <div className="space-y-6">
                        <div>
                            <p className="mb-3 text-[11px] font-bold uppercase tracking-wider text-slate-400">
                                KHÔNG GIAN LÀM VIỆC
                            </p>
                            <nav className="space-y-1">
                                {SIDEBAR_WORKSPACE.map((item) => {
                                    const Icon = item.icon;
                                    const isActive = activeTab === item.id;
                                    return (
                                        <button
                                            key={item.id}
                                            onClick={() => setActiveTab(item.id)}
                                            className={cn(
                                                "flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-sm font-medium transition",
                                                isActive
                                                    ? "bg-indigo-50 text-[#4338ca]"
                                                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                                            )}
                                        >
                                            <div className="flex items-center gap-3">
                                                <Icon className="h-4 w-4" />
                                                <span>{item.name}</span>
                                            </div>
                                            {isActive && <ChevronRight className="h-4 w-4" />}
                                        </button>
                                    );
                                })}
                            </nav>
                        </div>

                        <div>
                            <p className="mb-3 text-[11px] font-bold uppercase tracking-wider text-slate-400">
                                CÀI ĐẶT
                            </p>
                            <nav className="space-y-1">
                                {SIDEBAR_SETTINGS.map((item) => {
                                    const Icon = item.icon;
                                    const isActive = activeTab === item.id;
                                    return (
                                        <button
                                            key={item.id}
                                            onClick={() => setActiveTab(item.id)}
                                            className={cn(
                                                "flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-sm font-medium transition",
                                                isActive
                                                    ? "bg-indigo-50 text-[#4338ca]"
                                                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                                            )}
                                        >
                                            <div className="flex items-center gap-3">
                                                <Icon className="h-4 w-4" />
                                                <span>{item.name}</span>
                                            </div>
                                        </button>
                                    );
                                })}
                            </nav>
                        </div>
                    </div>

                    <div className="rounded-2xl bg-indigo-50/60 p-4">
                        <p className="text-xs font-bold text-indigo-900">Cần hỗ trợ?</p>
                        <p className="mt-1 text-xs text-indigo-600/80">Xem hướng dẫn quản lý nhân sự.</p>
                        <a href="#" className="mt-2 block text-xs font-semibold text-[#4338ca] hover:underline">
                            Tìm hiểu thêm →
                        </a>
                    </div>
                </aside>

                {/* ---------- CONTENT AREA ---------- */}
                <main className="flex-1 overflow-y-auto p-6">
                    {activeTab === "departments" ? (
                        <div className="space-y-6">
                            <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 pb-4">
                                <div>
                                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                                        Quản lý phòng ban
                                    </h1>
                                    <p className="text-sm text-slate-500">
                                        Xem và điều chỉnh sơ đồ cơ cấu tổ chức doanh nghiệp
                                    </p>
                                </div>

                                <div className="flex rounded-xl bg-slate-200/70 p-1">
                                    <button
                                        onClick={() => setDepartmentSubTab("list")}
                                        className={cn(
                                            "flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-bold transition",
                                            departmentSubTab === "list"
                                                ? "bg-white text-[#4338ca] shadow-sm"
                                                : "text-slate-600 hover:text-slate-900"
                                        )}
                                    >
                                        <List className="h-4 w-4" />
                                        Danh sách phòng ban
                                    </button>
                                    <button
                                        onClick={() => setDepartmentSubTab("tree")}
                                        className={cn(
                                            "flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-bold transition",
                                            departmentSubTab === "tree"
                                                ? "bg-white text-[#4338ca] shadow-sm"
                                                : "text-slate-600 hover:text-slate-900"
                                        )}
                                    >
                                        <Network className="h-4 w-4" />
                                        Sơ đồ tổ chức
                                    </button>
                                </div>
                            </div>

                            {departmentSubTab === "list" ? (
                                <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
                                    <div className="mb-4 flex items-center justify-between">
                                        <h2 className="text-base font-bold text-slate-900">Các phòng ban hiện tại</h2>
                                        <button className="flex items-center gap-2 rounded-xl bg-[#4338ca] px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-indigo-700">
                                            <Plus className="h-4 w-4" /> Thêm phòng ban
                                        </button>
                                    </div>
                                    <div className="overflow-x-auto">
                                        <table className="w-full text-left text-xs">
                                            <thead>
                                            <tr className="border-b border-slate-100 text-slate-400">
                                                <th className="pb-3 font-semibold">Tên phòng ban</th>
                                                <th className="pb-3 font-semibold">Trưởng phòng</th>
                                                <th className="pb-3 font-semibold">Số nhân sự</th>
                                                <th className="pb-3 font-semibold text-right">Thao tác</th>
                                            </tr>
                                            </thead>
                                            <tbody className="divide-y divide-slate-50">
                                            {DEPARTMENTS_DATA.map((dept) => (
                                                <tr key={dept.id} className="hover:bg-slate-50/60">
                                                    <td className="py-3.5 font-bold text-slate-900">
                                                        <div className="flex items-center gap-2.5">
                                                            <FolderKanban className="h-4 w-4 text-indigo-500" />
                                                            {dept.name}
                                                        </div>
                                                    </td>
                                                    <td className="py-3.5 font-medium text-slate-700">{dept.manager}</td>
                                                    <td className="py-3.5 text-slate-600">{dept.count} thành viên</td>
                                                    <td className="py-3.5 text-right">
                                                        <button className="rounded-md p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600">
                                                            <MoreHorizontal className="h-4 w-4" />
                                                        </button>
                                                    </td>
                                                </tr>
                                            ))}
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            ) : (
                                <div className="h-[750px] overflow-hidden rounded-2xl border border-slate-100 bg-white p-2 shadow-sm">
                                    <OrgChart />
                                </div>
                            )}
                        </div>
                    ) : (
                        /* MÀN HÌNH TỔNG QUAN MAC ĐỊNH */
                        <div>
                            <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
                                <div>
                                    <p className="text-xs font-medium text-slate-500">
                                        Thứ Hai, 24 tháng 8, 2026
                                    </p>
                                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                                        Xin chào, Quản lý!
                                    </h1>
                                    <p className="text-sm text-slate-500">Đây là tình hình nhân sự hôm nay.</p>
                                </div>

                                <div className="flex items-center gap-3">
                                    <button className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm hover:bg-slate-50">
                                        <Download className="h-4 w-4" />
                                        Xuất báo cáo
                                    </button>
                                    <button className="flex items-center gap-2 rounded-xl bg-[#4338ca] px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-indigo-700">
                                        <Plus className="h-4 w-4" />
                                        Thêm nhân viên
                                    </button>
                                </div>
                            </div>

                            {/* KPI Cards */}
                            <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                                <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
                                    <div className="flex items-center justify-between">
                                        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-purple-50 text-purple-600">
                                            <Users className="h-5 w-5" />
                                        </div>
                                        <span className="inline-flex items-center gap-0.5 rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-semibold text-emerald-600">
                                            <ArrowUpRight className="h-3 w-3" /> +12%
                                        </span>
                                    </div>
                                    <p className="mt-4 text-xs font-medium text-slate-500">Tổng nhân viên</p>
                                    <div className="flex items-baseline justify-between">
                                        <span className="text-2xl font-bold text-slate-900">128</span>
                                        <span className="text-xs text-slate-400">so với tháng trước</span>
                                    </div>
                                </div>

                                <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
                                    <div className="flex items-center justify-between">
                                        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                                            <UserCheck className="h-5 w-5" />
                                        </div>
                                        <span className="inline-flex items-center gap-0.5 rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-semibold text-emerald-600">
                                            89.1%
                                        </span>
                                    </div>
                                    <p className="mt-4 text-xs font-medium text-slate-500">Đang làm việc</p>
                                    <div className="flex items-baseline justify-between">
                                        <span className="text-2xl font-bold text-slate-900">114</span>
                                        <span className="text-xs text-slate-400">tổng nhân sự</span>
                                    </div>
                                </div>

                                <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
                                    <div className="flex items-center justify-between">
                                        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
                                            <CalendarIcon className="h-5 w-5" />
                                        </div>
                                        <span className="inline-flex items-center gap-0.5 rounded-full bg-rose-50 px-2 py-0.5 text-xs font-semibold text-rose-600">
                                            <ArrowDownRight className="h-3 w-3" /> -4.2%
                                        </span>
                                    </div>
                                    <p className="mt-4 text-xs font-medium text-slate-500">Đang nghỉ phép</p>
                                    <div className="flex items-baseline justify-between">
                                        <span className="text-2xl font-bold text-slate-900">08</span>
                                        <span className="text-xs text-slate-400">so với tuần trước</span>
                                    </div>
                                </div>

                                <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
                                    <div className="flex items-center justify-between">
                                        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-rose-50 text-rose-600">
                                            <Briefcase className="h-5 w-5" />
                                        </div>
                                        <span className="inline-flex items-center gap-0.5 rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-semibold text-emerald-600">
                                            +2
                                        </span>
                                    </div>
                                    <p className="mt-4 text-xs font-medium text-slate-500">Vị trí tuyển dụng</p>
                                    <div className="flex items-baseline justify-between">
                                        <span className="text-2xl font-bold text-slate-900">06</span>
                                        <span className="text-xs text-slate-400">vị trí mới</span>
                                    </div>
                                </div>
                            </div>

                            {/* LỊCH THEO BỐ CỤC ẢNH MẪU */}
                            <div className="grid grid-cols-1 gap-6 lg:grid-cols-4">
                                {/* KHU VỰC LỊCH CHÍNH (3 Cột) */}
                                <div className="lg:col-span-3">
                                    <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
                                        {/* Header Lịch */}
                                        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4">
                                            <div>
                                                <h2 className="text-xl font-semibold text-slate-700">
                                                    {selectedDate} tháng 8 năm 2026
                                                </h2>
                                                <p className="text-sm text-slate-400">Thứ Hai</p>
                                            </div>

                                            <div className="flex items-center gap-4 text-sm text-slate-600">
                                                <button className="flex items-center gap-1 hover:text-slate-900">
                                                    Ngày <ChevronDown className="h-4 w-4 text-slate-400" />
                                                </button>
                                                <button className="flex items-center gap-1 hover:text-slate-900">
                                                    <ChevronLeft className="h-4 w-4" /> Hôm nay <ChevronRight className="h-4 w-4" />
                                                </button>
                                            </div>
                                        </div>

                                        {/* Sub-header "Ngày" */}
                                        <div className="border-b border-slate-100 bg-slate-50/50 px-4 py-1.5 text-xs font-semibold text-slate-500">
                                            Ngày
                                        </div>

                                        {/* Grid Thời Gian */}
                                        <div className="relative max-h-[500px] overflow-y-auto">
                                            {/* Đường mốc thời gian màu đỏ */}
                                            <div className="absolute top-[82px] left-0 right-0 z-10 flex items-center">
                                                <span className="pl-1 text-[10px] font-bold text-red-500">9:52 AM</span>
                                                <div className="h-[1.5px] w-full bg-red-500" />
                                            </div>

                                            <div className="divide-y divide-slate-100">
                                                {TIME_SLOTS.map((time, idx) => (
                                                    <div key={idx} className="flex min-h-[48px] items-start">
                                                        <div className="w-16 flex-none py-1.5 pl-3 pr-2 text-right text-[11px] font-medium text-slate-400">
                                                            {time}
                                                        </div>
                                                        <div className="h-full flex-1 border-l border-slate-100 py-1 px-3">
                                                            {/* Ô trống chứa sự kiện */}
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                {/* KHU VỰC MINI CALENDAR (1 Cột) */}
                                <div className="lg:col-span-1">
                                    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                                        <div className="mb-4 flex items-center justify-between text-slate-600">
                                            <button className="rounded p-1 hover:bg-slate-100">
                                                <ChevronLeft className="h-4 w-4" />
                                            </button>
                                            <span className="text-sm font-semibold text-slate-700">tháng 8 2026</span>
                                            <button className="rounded p-1 hover:bg-slate-100">
                                                <ChevronRight className="h-4 w-4" />
                                            </button>
                                        </div>

                                        <div className="grid grid-cols-7 gap-1 text-center text-xs">
                                            {["T2", "T3", "T4", "T5", "T6", "T7", "CN"].map((day, idx) => (
                                                <div key={idx} className="font-semibold text-slate-500 py-1">
                                                    {day}
                                                </div>
                                            ))}

                                            {/* Ngày tháng trước */}
                                            {[27, 28, 29, 30, 31].map((d) => (
                                                <div key={`prev-${d}`} className="py-1 text-slate-300">
                                                    {d}
                                                </div>
                                            ))}

                                            {/* Ngày tháng hiện tại */}
                                            {Array.from({ length: 31 }, (_, i) => i + 1).map((d) => {
                                                const isSelected = selectedDate === d;
                                                return (
                                                    <button
                                                        key={`curr-${d}`}
                                                        onClick={() => setSelectedDate(d)}
                                                        className={cn(
                                                            "mx-auto flex h-7 w-7 items-center justify-center rounded-full text-xs font-medium transition",
                                                            isSelected
                                                                ? "bg-[#00a8ff] text-white shadow-sm font-bold"
                                                                : "text-slate-700 hover:bg-slate-100"
                                                        )}
                                                    >
                                                        {d}
                                                    </button>
                                                );
                                            })}

                                            {/* Ngày tháng sau */}
                                            {[1, 2, 3, 4, 5, 6].map((d) => (
                                                <div key={`next-${d}`} className="py-1 text-slate-300">
                                                    {d}
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </main>
            </div>
        </div>
    );
}