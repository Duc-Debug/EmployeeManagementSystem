import {
    LayoutDashboard,
    Users,
    Clock,
    Calendar as CalendarIcon,
    Building2,
    Settings,
    ChevronRight,
    ClipboardList,
    FolderKanban,
    FileText,
    ShieldCheck,
    CalendarClock,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuthUser } from "@/lib/auth-session";

const SIDEBAR_WORKSPACE = [
    { name: "Tổng quan", icon: LayoutDashboard, id: "overview" },
    { name: "Quản lý tài khoản", icon: Users, id: "users" },
    { name: "Hồ sơ nhân sự", icon: FileText, id: "hrprofile" },
    { name: "Giờ khả dụng", icon: CalendarClock, id: "availability" },
    { name: "Chấm công", icon: Clock, id: "attendance" },
    { name: "Nghỉ phép", icon: CalendarIcon, id: "leave" },
    { name: "Phòng ban", icon: Building2, id: "departments" },
    { name: "Khai báo kỹ năng", icon: ClipboardList, id: "skills" },
    { name: "Dự án", icon: FolderKanban, id: "project" },
];

const SIDEBAR_SETTINGS = [
    { name: "Phân quyền truy cập", icon: ShieldCheck, id: "access" },
    { name: "Thiết lập hệ thống", icon: Settings, id: "settings" },
];

export function canAccessTab(roleCode: string | undefined | null, tabId: string): boolean {
    if (!roleCode) return true;
    const normalized = roleCode.toUpperCase().replace(/_/g, "-");

    switch (tabId) {
        case "overview":
            // Tất cả 6 vai trò (VT-01 -> VT-06) đều có quyền truy cập trang Tổng quan
            return true;

        case "access":
        case "settings":
        case "users":
            // Quản lý tài khoản, Phân quyền & Thiết lập hệ thống: Dành riêng cho Quản trị viên (VT-06)
            return normalized === "VT-06";

        case "departments":
        case "organization":
            // Cây cơ cấu tổ chức: VT-01, VT-02, VT-03, VT-04, VT-05 được xem (Read-only); VT-06 Toàn quyền
            return ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05", "VT-06"].includes(normalized);

        case "hrprofile":
        case "employees":
            // Hồ sơ nhân sự: VT-05 Toàn quyền; VT-01, VT-02, VT-03, VT-04, VT-06 được Xem theo Data Scope
            return ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05", "VT-06"].includes(normalized);

        case "availability":
        case "weekly-availability":
            // Giờ khả dụng: VT-01, VT-02, VT-03, VT-04, VT-05, VT-06 (phân quyền theo DataScope)
            return ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05", "VT-06"].includes(normalized);

        case "project":
        case "projects":
            // Quản lý dự án & WBS: VT-01 (Xem), VT-02 (Dự án của mình), VT-03 (Xem), VT-04 (Dự án tham gia); HR (VT-05) & Admin (VT-06) bị ẩn (❌)
            return ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05", "VT-06"].includes(normalized);

        case "attendance":
        case "timesheets":
            // Bảng chấm công: VT-01, VT-02, VT-03, VT-04, VT-05 có quyền; Admin (VT-06) bị ẩn (❌)
            return ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05"].includes(normalized);

        case "leave":
        case "leave-requests":
            // Đơn nghỉ phép: VT-01, VT-02, VT-03, VT-04, VT-05 có quyền; Admin (VT-06) bị ẩn (❌)
            return ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05"].includes(normalized);

        case "skills":
            // Khai báo & Duyệt kỹ năng: VT-01, VT-02, VT-03, VT-04, VT-05
            return ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05"].includes(normalized);

        case "reports":
            // Báo cáo & Mô phỏng năng lực: VT-01 (Toàn công ty), VT-02 (Dự án phụ trách), VT-03 (Bộ phận phụ trách)
            return ["VT-01", "VT-02", "VT-03"].includes(normalized);

        default:
            return true;
    }
}

interface SideBarProps {
    activeTab: string;
    setActiveTab: (tab: string) => void;
    isOpen: boolean;
}

export default function SideBar({ activeTab, setActiveTab, isOpen }: SideBarProps) {
    const user = useAuthUser();
    const roleCode = user?.roleCode;

    const visibleWorkspace = SIDEBAR_WORKSPACE.filter((item) => canAccessTab(roleCode, item.id));
    const visibleSettings = SIDEBAR_SETTINGS.filter((item) => canAccessTab(roleCode, item.id));

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
                        {visibleWorkspace.map((item) => {
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
                {visibleSettings.length > 0 && (
                    <div>
                        <p className="mb-3 text-[11px] font-bold uppercase tracking-wider text-slate-400">
                            CÀI ĐẶT
                        </p>
                        <nav className="space-y-1">
                            {visibleSettings.map((item) => {
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
                )}
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
