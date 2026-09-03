import {
    LayoutDashboard,
    Users,
    Clock,
    Calendar as CalendarIcon,
    Building2,
    BarChart3,
    ShieldCheck,
    Settings,
    ChevronRight,
} from "lucide-react";
import { cn } from "@/lib/utils";

const SIDEBAR_WORKSPACE = [
    { name: "Tổng quan", icon: LayoutDashboard, id: "overview" },
    { name: "Nhân sự", icon: Users, id: "employees" },
    { name: "Chấm công", icon: Clock, id: "attendance" },
    { name: "Nghỉ phép", icon: CalendarIcon, id: "leave" },
    { name: "Phòng ban", icon: Building2, id: "departments" },
    { name: "Báo cáo", icon: BarChart3, id: "reports" },
];

const SIDEBAR_SETTINGS = [
    { name: "Quyền truy cập", icon: ShieldCheck, id: "access" },
    { name: "Thiết lập hệ thống", icon: Settings, id: "settings" },
];

interface SideBarProps {
    activeTab: string;
    setActiveTab: (tab: string) => void;
    isOpen: boolean;
}

export default function SideBar({ activeTab, setActiveTab, isOpen }: SideBarProps) {
    return (
        <aside
            className={cn(
                "flex flex-col justify-between border-r border-slate-200 bg-white text-slate-700 transition-all duration-300 ease-in-out overflow-hidden shadow-xs",
                isOpen
                    ? "w-[240px] p-4 opacity-100 translate-x-0"
                    : "w-0 p-0 opacity-0 -translate-x-full border-r-0 pointer-events-none"
            )}
        >
            <div className="w-[208px] space-y-6 flex-none">
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
                                        "flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
                                        isActive
                                            ? "bg-indigo-50 text-indigo-700 border border-indigo-200 font-bold shadow-xs"
                                            : "text-slate-600 hover:bg-slate-50 hover:text-slate-900 border border-transparent"
                                    )}
                                >
                                    <div className="flex items-center gap-3">
                                        <Icon className="h-4 w-4 shrink-0" />
                                        <span className="whitespace-nowrap">{item.name}</span>
                                    </div>
                                    {isActive && <ChevronRight className="h-4 w-4 shrink-0 text-indigo-600" />}
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
                                        "flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
                                        isActive
                                            ? "bg-indigo-50 text-indigo-700 border border-indigo-200 font-bold shadow-xs"
                                            : "text-slate-600 hover:bg-slate-50 hover:text-slate-900 border border-transparent"
                                    )}
                                >
                                    <div className="flex items-center gap-3">
                                        <Icon className="h-4 w-4 shrink-0" />
                                        <span className="whitespace-nowrap">{item.name}</span>
                                    </div>
                                </button>
                            );
                        })}
                    </nav>
                </div>
            </div>

            <div className="w-[208px] rounded-2xl border border-slate-200 bg-slate-50 p-4 flex-none shadow-xs">
                <p className="text-xs font-bold text-slate-800">Cần hỗ trợ?</p>
                <p className="mt-1 text-xs text-slate-500">Xem hướng dẫn quản lý nhân sự.</p>
                <a href="#" className="mt-2 block text-xs font-semibold text-indigo-600 hover:text-indigo-800 hover:underline">
                    Tìm hiểu thêm →
                </a>
            </div>
        </aside>
    );
}