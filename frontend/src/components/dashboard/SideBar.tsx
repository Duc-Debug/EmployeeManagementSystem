import {
    LayoutDashboard,
    Users,
    Clock,
    Calendar as CalendarIcon,
    Target,
    Building2,
    BarChart3,
    ShieldCheck,
    Settings,
    ChevronRight,
    User,
} from "lucide-react";
import { cn } from "@/lib/utils";

const SIDEBAR_WORKSPACE = [
    { name: "Tổng quan", icon: LayoutDashboard, id: "overview" },
    { name: "Nhân sự", icon: Users, id: "employees" },
    { name: "Chấm công", icon: Clock, id: "attendance" },
    { name: "Nghỉ phép", icon: CalendarIcon, id: "leave" },
    { name: "Khai báo kỹ năng", icon: Target, id: "skills" },
    { name: "Phòng ban", icon: Building2, id: "departments" },
    { name: "Báo cáo", icon: BarChart3, id: "reports" },
];

const SIDEBAR_SETTINGS = [
    { name: "Quyền truy cập", icon: ShieldCheck, id: "access" },
    { name: "Thiết lập hệ thống", icon: Settings, id: "settings" },
];

interface SideBarUser {
    name: string;
    position: string;
    avatarUrl?: string;
}

interface SideBarProps {
    activeTab: string;
    setActiveTab: (tab: string) => void;
    isOpen: boolean;
    user?: SideBarUser;
}

export default function SideBar({
                                    activeTab,
                                    setActiveTab,
                                    isOpen,
                                    user = { name: "Người dùng", position: "Chưa cập nhật chức vụ" },
                                }: SideBarProps) {
    return (
        <aside
            className={cn(
                "flex flex-col justify-between border-r border-white/15 bg-white/[0.06] backdrop-blur-2xl transition-all duration-300 ease-in-out overflow-hidden",
                isOpen
                    ? "w-[240px] p-4 opacity-100 translate-x-0"
                    : "w-0 p-0 opacity-0 -translate-x-full border-r-0 pointer-events-none"
            )}
        >
            <div className="w-[208px] space-y-6 flex-none">
                <div>
                    <p className="mb-3 text-[11px] font-bold uppercase tracking-wider text-white/40">
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
                                            ? "bg-white/15 text-white border border-white/20"
                                            : "text-white/65 hover:bg-white/10 hover:text-white"
                                    )}
                                >
                                    <div className="flex items-center gap-3">
                                        <Icon className="h-4 w-4 shrink-0" />
                                        <span className="whitespace-nowrap">{item.name}</span>
                                    </div>
                                    {isActive && <ChevronRight className="h-4 w-4 shrink-0" />}
                                </button>
                            );
                        })}
                    </nav>
                </div>
                <div>
                    <p className="mb-3 text-[11px] font-bold uppercase tracking-wider text-white/40">
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
                                            ? "bg-white/15 text-white border border-white/20"
                                            : "text-white/65 hover:bg-white/10 hover:text-white"
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

            <div className="w-[208px] flex items-center gap-3 rounded-2xl border border-white/15 bg-white/[0.06] p-3 flex-none backdrop-blur-xl">
                <div className="flex h-9 w-9 shrink-0 items-center justify-center overflow-hidden rounded-full bg-white/20 text-white">
                    {user.avatarUrl ? (
                        <img
                            src={user.avatarUrl}
                            alt={user.name}
                            className="h-full w-full object-cover"
                        />
                    ) : (
                        <User className="h-4 w-4" />
                    )}
                </div>
                <div className="min-w-0">
                    <p className="truncate text-xs font-bold text-white">{user.name}</p>
                    <p className="truncate text-[11px] text-white/60">{user.position}</p>
                </div>
            </div>
        </aside>
    );
}