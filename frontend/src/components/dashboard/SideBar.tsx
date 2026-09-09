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
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuthUser } from "@/lib/auth-session";

const SIDEBAR_WORKSPACE = [
    { name: "Tổng quan", icon: LayoutDashboard, id: "overview" },
    { name: "Quản lý tài khoản", icon: Users, id: "employees" },
    { name: "Hồ sơ nhân sự", icon: FileText, id: "hrprofile" },
    { name: "Chấm công", icon: Clock, id: "attendance" },
    { name: "Nghỉ phép", icon: CalendarIcon, id: "leave" },
    { name: "Phòng ban", icon: Building2, id: "departments" },
    { name: "Khai báo kỹ năng", icon: ClipboardList, id: "skills" },
    { name: "Dự án", icon: FolderKanban, id: "project" },
];

const SIDEBAR_SETTINGS = [
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

        case "project":
        case "projects":
            // Quản lý dự án & WBS: VT-01 (Xem), VT-02 (Dự án của mình), VT-03 (Xem), VT-04 (Dự án tham gia); HR (VT-05) & Admin (VT-06) bị ẩn (❌)
            return ["VT-01", "VT-02", "VT-03", "VT-04"].includes(normalized);

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
            return