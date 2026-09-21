import { useState, useEffect } from "react";
import {
    LayoutDashboard,
    Activity,
    Users,
    Clock,
    Calendar as CalendarIcon,
    Building2,
    ChevronRight,
    ChevronDown,
    ClipboardList,
    FolderKanban,
    FileText,
    CalendarClock,
    CalendarDays,
    TrendingUp,
    CalendarRange,
    Briefcase,
    AlertTriangle,
    Sparkles,
    CalendarX,
    DollarSign,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuthUser } from "@/lib/auth-session";

export interface SidebarItemDef {
    name: string;
    icon: LucideIcon;
    id: string;
}

export interface SidebarGroupDef {
    id: string;
    title: string;
    items: SidebarItemDef[];
}

export const SIDEBAR_GROUPS: SidebarGroupDef[] = [
    {
        id: "dashboards",
        title: "Tổng quan & Điều hành",
        items: [
            { name: "Tổng quan", icon: LayoutDashboard, id: "overview" },
            { name: "Bảng điều khiển năng lực", icon: Activity, id: "capacity-dashboard" },
        ],
    },
    {
        id: "projects_resources",
        title: "Dự án & Nguồn lực",
        items: [
            { name: "Quản lý Dự án", icon: FolderKanban, id: "project" },
            { name: "Bảng năng lực & Phân bổ", icon: CalendarRange, id: "capacity" },
            { name: "Cảnh báo xung đột lịch", icon: AlertTriangle, id: "schedule-conflict" },
            { name: "Mô phỏng kịch bản", icon: Sparkles, id: "simulation-scenarios" },
        ],
    },
    {
        id: "time_attendance",
        title: "Thời gian & Lịch trình",
        items: [
            { name: "Lịch phân bổ tuần", icon: CalendarRange, id: "my-schedule" },
            { name: "Chấm công & Giờ làm", icon: Clock, id: "attendance" },
            { name: "Giờ khả dụng", icon: CalendarClock, id: "availability" },
            { name: "Thời gian không sẵn sàng", icon: CalendarX, id: "unavailability" },
            { name: "Nghỉ phép", icon: CalendarIcon, id: "leave" },
            { name: "Lịch & Ngày lễ", icon: CalendarDays, id: "working-calendar" },
        ],
    },
    {
        id: "reports_analytics",
        title: "Báo cáo & Phân tích",
        items: [
            { name: "Tỷ lệ giờ tính phí", icon: DollarSign, id: "billable-rate" },
            { name: "Phân bổ theo dự án", icon: FolderKanban, id: "project-allocation-report" },
            { name: "Đối chiếu giờ công", icon: Clock, id: "timesheet-variance" },
            { name: "Nhu cầu tuyển dụng", icon: TrendingUp, id: "recruitment-demand" },
            { name: "Dự báo năng lực", icon: TrendingUp, id: "capacity-forecast" },
        ],
    },
    {
        id: "organization_hr",
        title: "Tổ chức & Nhân sự",
        items: [
            { name: "Hồ sơ nhân sự", icon: FileText, id: "hrprofile" },
            { name: "Phòng ban", icon: Building2, id: "departments" },
            { name: "Quản lý Năng lực & Kỹ năng", icon: ClipboardList, id: "skills" },
            { name: "Quản lý tài khoản", icon: Users, id: "users" },
        ],
    },
    {
        id: "settings",
        title: "Cài đặt & Danh mục",
        items: [
            { name: "Vai trò chuyên môn", icon: Briefcase, id: "roles" },
        ],
    },
];

export function canAccessTab(
    roleCode: string | undefined | null,
    tabId: string,
    dataScope?: string | null,
    permissions?: readonly string[] | null
): boolean {
    if (!roleCode && !dataScope && !permissions) return true;
    const normalized = roleCode ? roleCode.toUpperCase().replace(/_/g, "-") : "";

    switch (tabId) {
        case "overview":
            // Tất cả vai trò đều có quyền truy cập trang Tổng quan
            return true;

        case "billable-rate":
        case "billable-report":
        case "billable-hours":
            // NCL-10-CN-002: Báo cáo tỷ lệ giờ tính phí (Ban giám đốc VT-01, Quản lý nguồn lực VT-03, Admin VT-06)
            return permissions?.includes("BILLABLE_HOURS_REPORT_READ") === true ||
                ["VT-01", "VT-03", "VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalized);

        case "capacity-forecast":
        case "forecast":
            // NCL-10-CN-004: Báo cáo dự báo năng lực các tuần tới (Ban giám đốc VT-01, Quản lý nguồn lực VT-03, Admin VT-06)
            return permissions?.includes("CAPACITY_FORECAST_REPORT_READ") === true ||
                ["VT-01", "VT-03", "VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalized);

        case "timesheet-variance":
        case "variance-report":
            // NCL-09-CN-004: Báo cáo đối chiếu giờ công (Ban giám đốc VT-01, Quản lý nguồn lực VT-03, Admin VT-06)
            return permissions?.includes("TIMESHEET_VARIANCE_READ") === true ||
                ["VT-01", "VT-03", "VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalized);

        case "recruitment-demand":
        case "recruitment":
            // NCL-10-CN-005: Báo cáo nhu cầu tuyển dụng theo kỹ năng (Ban Giám Đốc VT-01, RM VT-03, Admin VT-06)
            return permissions?.includes("RECRUITMENT_DEMAND_REPORT_READ") === true ||
                ["VT-01", "VT-03", "VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalized);

        case "capacity-dashboard":
        case "dashboard-capacity":
            // NCL-10-CN-001: Bảng điều khiển năng lực dành cho Ban Giám Đốc (VT-01), Quản lý dự án (VT-02), Quản lý nguồn lực (VT-03), Quản trị viên (VT-06)
            return permissions?.includes("CAPACITY_DASHBOARD_READ") === true ||
                ["VT-01", "VT-02", "VT-03", "VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalized);

        case "capacity":
        case "weekly-capacity":
            // NCL-06 / NCL-06-CN-002: Bảng năng lực chỉ dành cho VT-01 (Ban giám đốc), VT-02 (Quản lý dự án), VT-03 (Quản lý nguồn lực).
            // VT-04 (Nhân viên), VT-05 (Nhân sự), VT-06 (Admin) KHÔNG có quyền phân bổ.
            return ["VT-01", "VT-02", "VT-03"].includes(normalized);

        case "simulation-scenarios":
        case "simulation-scenario":
        case "scenarios":
            // NCL-08-CN-001: Mô phỏng kịch bản nhận dự án chỉ dành cho VT-01 (Ban giám đốc) và VT-03 (Quản lý nguồn lực).
            return permissions?.includes("RESOURCE_SCENARIO_READ") === true ||
                permissions?.includes("RESOURCE_SCENARIO_MANAGE") === true ||
                ["VT-01", "VT-03"].includes(normalized);

        case "schedule-conflict":
        case "conflict-warning":
            // NCL-07-CN-001: Cảnh báo xung đột lịch dành cho VT-02 (PM), VT-03 (RM), VT-06 (Admin)
            return permissions?.includes("RESOURCE_SCHEDULE_CONFLICT_READ") === true ||
                ["VT-02", "VT-03", "VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalized);

        case "project":
        case "projects":
            // Quản lý dự án: VT-01 (Xem toàn bộ), VT-02 (Dự án của mình), VT-03 (Xem dự án liên quan), VT-04 (Dự án tham gia); HR (VT-05) & Admin (VT-06) bị ẩn theo quy tắc vai trò
            return ["VT-01", "VT-02", "VT-03", "VT-04"].includes(normalized);

        case "my-schedule":
        case "my-allocations":
            // NCL-13-CN-001: Tất cả nhân sự đều có quyền xem và xác nhận lịch phân bổ tuần của chính mình
            return true;

        case "attendance":
        case "timesheets":
            // Bảng chấm công & Giờ làm việc: VT-01 -> VT-06 (VT-04 ghi giờ, VT-02 duyệt, VT-03/05/06 xem)
            return ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05", "VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalized);

        case "leave":
        case "leave-requests":
            // Đơn nghỉ phép: VT-01, VT-02, VT-03, VT-04, VT-05 có quyền; Admin (VT-06) bị ẩn vì không thuộc nghiệp vụ vận hành
            return ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05"].includes(normalized);

        case "availability":
        case "weekly-availability":
            // Giờ khả dụng: VT-01 -> VT-06 (phân quyền theo DataScope)
            return ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05", "VT-06"].includes(normalized);

        case "unavailability":
        case "unavailability-declarations":
            // NCL-13-CN-003: Khai báo và quản lý thời gian không sẵn sàng (VT-01 -> VT-06)
            return permissions?.includes("UNAVAILABILITY_DECLARE") === true ||
                permissions?.includes("UNAVAILABILITY_READ") === true ||
                permissions?.includes("UNAVAILABILITY_APPROVE") === true ||
                ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05", "VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalized);

        case "working-calendar":
        case "calendar-config":
            // Lịch làm việc và ngày lễ: VT-01 -> VT-06
            return ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05", "VT-06"].includes(normalized);

        case "hrprofile":
        case "employees":
            // Hồ sơ nhân sự (NCL-02): Dành riêng cho VT-05 (HR), VT-01 (Ban Giám Đốc), VT-06 (Admin).
            // PM (VT-02), RM (VT-03), NV (VT-04) bị ẩn vì không thuộc nghiệp vụ hành chính nhân sự.
            return ["VT-01", "VT-05", "VT-06", "ROLE-HR", "HR", "ROLE-ADMIN", "ADMIN"].includes(normalized);

        case "departments":
        case "organization":
            // Cây cơ cấu tổ chức: VT-01 -> VT-05 được xem (Read-only); VT-06 Toàn quyền
            return ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05", "VT-06"].includes(normalized);

        case "skills":
            // Khai báo, Quản lý & Duyệt kỹ năng: VT-01 -> VT-06 (VT-04 khai báo, VT-03 duyệt, VT-06 quản lý danh mục)
            return ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05", "VT-06"].includes(normalized);

        case "access":
        case "users":
            // Quản lý tài khoản & Phân quyền: Dành riêng cho Quản trị viên (VT-06)
            return normalized === "VT-06" || normalized === "ROLE-ADMIN" || normalized === "ADMIN";

        case "roles":
        case "project-roles":
            // Danh mục vai trò chuyên môn (NCL-12-CN-001): VT-01 -> VT-06 đều có quyền xem
            return ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05", "VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalized);

        case "data-import":
        case "employee-import":
            // NCL-12-CN-004: Nhập dữ liệu nhân sự từ tệp (Quản trị viên VT-06 hoặc quyền DATA_IMPORT)
            return permissions?.includes("DATA_IMPORT") === true ||
                ["VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalized);

        case "project-allocation-report":
        case "project-allocation":
            // Báo cáo phân bổ theo dự án (NCL-10-CN-006): VT-01 (Ban Giám Đốc), VT-02 (PM), VT-03 (RM), VT-06 (Admin)
            return permissions?.includes("PROJECT_ALLOCATION_REPORT_READ") === true ||
                ["VT-01", "VT-02", "VT-03", "VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalized);

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
    const dataScope = user?.dataScope;

    const normalizedRole = roleCode ? roleCode.toUpperCase().replace(/_/g, "-") : "";
    const isEmployeeOnly = normalizedRole === "VT-04";

    // Filter groups and items based strictly on role permissions
    const visibleGroups = SIDEBAR_GROUPS.map((group) => {
        const visibleItems = group.items
            .filter((item) => canAccessTab(roleCode, item.id, dataScope, user?.permissions))
            .map((item) => {
                if (item.id === "skills") {
                    return {
                        ...item,
                        name: isEmployeeOnly ? "Khai báo kỹ năng" : "Quản lý Năng lực & Kỹ năng",
                    };
                }
                if (item.id === "availability") {
                    return {
                        ...item,
                        name: isEmployeeOnly || dataScope === "SELF" ? "Giờ khả dụng của tôi" : "Quản lý Giờ khả dụng",
                    };
                }
                return item;
            });

        return {
            ...group,
            items: visibleItems,
        };
    }).filter((group) => group.items.length > 0);

    // State for tracking expanded/collapsed groups
    const [openGroups, setOpenGroups] = useState<Record<string, boolean>>(() => {
        const initial: Record<string, boolean> = {};
        SIDEBAR_GROUPS.forEach((g) => {
            initial[g.id] = true; // Default open for clear visibility
        });
        return initial;
    });

    // Automatically expand the group containing the active tab
    useEffect(() => {
        const activeGroup = visibleGroups.find((g) => g.items.some((i) => i.id === activeTab));
        if (activeGroup && !openGroups[activeGroup.id]) {
            setOpenGroups((prev) => ({ ...prev, [activeGroup.id]: true }));
        }
    }, [activeTab, visibleGroups]);

    const toggleGroup = (groupId: string) => {
        setOpenGroups((prev) => ({
            ...prev,
            [groupId]: !prev[groupId],
        }));
    };

    return (
        <aside
            className={cn(
                "flex flex-col justify-between border-r border-slate-200 bg-white text-slate-700 transition-all duration-300 ease-in-out overflow-hidden shadow-xs h-full",
                isOpen
                    ? "w-[240px] p-3 opacity-100 translate-x-0"
                    : "w-0 p-0 opacity-0 -translate-x-full border-r-0 pointer-events-none"
            )}
        >
            {/* Scrollable Navigation Area */}
            <div className="w-[214px] flex-1 overflow-y-auto pr-1 space-y-4 select-none scrollbar-thin scrollbar-thumb-slate-200 scrollbar-track-transparent">
                {visibleGroups.map((group) => {
                    const isExpanded = openGroups[group.id] ?? true;
                    const hasActiveChild = group.items.some((item) => item.id === activeTab);

                    return (
                        <div key={group.id} className="space-y-1">
                            {/* Group Accordion Header */}
                            <button
                                type="button"
                                onClick={() => toggleGroup(group.id)}
                                className={cn(
                                    "flex w-full items-center justify-between px-2 py-1.5 text-[11px] font-bold uppercase tracking-wider rounded-lg transition-colors group",
                                    hasActiveChild
                                        ? "text-indigo-600 hover:bg-indigo-50/50"
                                        : "text-slate-400 hover:text-slate-700 hover:bg-slate-100/60"
                                )}
                            >
                                <span className="truncate">{group.title}</span>
                                <div className="flex items-center gap-1">
                                    <span className="text-[10px] font-semibold text-slate-400 group-hover:text-slate-600">
                                        ({group.items.length})
                                    </span>
                                    {isExpanded ? (
                                        <ChevronDown className="h-3.5 w-3.5 text-slate-400 transition-transform duration-200" />
                                    ) : (
                                        <ChevronRight className="h-3.5 w-3.5 text-slate-400 transition-transform duration-200" />
                                    )}
                                </div>
                            </button>

                            {/* Group Items */}
                            {isExpanded && (
                                <nav className="space-y-0.5 pt-0.5 animate-in fade-in-50 duration-200">
                                    {group.items.map((item) => {
                                        const Icon = item.icon;
                                        const isActive = activeTab === item.id;
                                        return (
                                            <button
                                                key={item.id}
                                                type="button"
                                                onClick={() => setActiveTab(item.id)}
                                                className={cn(
                                                    "flex w-full items-center justify-between rounded-xl px-2.5 py-2 text-xs font-medium transition-all",
                                                    isActive
                                                        ? "bg-indigo-50 text-indigo-700 border border-indigo-200 font-bold shadow-xs translate-x-0.5"
                                                        : "text-slate-600 hover:bg-slate-50 hover:text-slate-900 border border-transparent"
                                                )}
                                            >
                                                <div className="flex items-center gap-2.5 truncate">
                                                    <Icon className={cn("h-4 w-4 shrink-0", isActive ? "text-indigo-600" : "text-slate-400")} />
                                                    <span className="truncate text-[13px]">{item.name}</span>
                                                </div>
                                                {isActive && <ChevronRight className="h-3.5 w-3.5 shrink-0 text-indigo-600" />}
                                            </button>
                                        );
                                    })}
                                </nav>
                            )}
                        </div>
                    );
                })}
            </div>

            {/* Bottom Support Box */}
            <div className="w-[214px] rounded-2xl border border-slate-200 bg-slate-50 p-3 mt-3 flex-none shadow-xs">
                <p className="text-xs font-bold text-slate-800">Cần hỗ trợ?</p>
                <p className="mt-0.5 text-[11px] text-slate-500">Xem tài liệu & hướng dẫn sử dụng.</p>
                <a
                    href="#"
                    onClick={(e) => e.preventDefault()}
                    className="mt-1.5 block text-xs font-semibold text-indigo-600 hover:text-indigo-800 hover:underline"
                >
                    Tìm hiểu thêm →
                </a>
            </div>
        </aside>
    );
}
