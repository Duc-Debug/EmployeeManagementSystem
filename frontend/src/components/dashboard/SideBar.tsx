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
interface SideBarProps {
    activeTab: string;
    setActiveTab: (tab: string) => void;
    isOpen: boolean;
}
export default function SideBar({ activeTab, setActiveTab, isOpen }: SideBarProps) {
    if (!isOpen) return null;
    return (
        <aside className="flex w-[240px] flex-none flex-col justify-between border-r border-slate-200 bg-white p-4 transition-all duration-300">
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
    );
}