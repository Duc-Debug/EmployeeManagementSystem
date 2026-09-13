import type { ModuleDef, RoleTheme, DataScope, Role, Department, ModulePermission } from "./access.types";

export interface ThemeOption {
    key: RoleTheme;
    label: string;
}

export const THEME_OPTIONS: ThemeOption[] = [
    { key: "blue", label: "Xanh dương" },
    { key: "indigo", label: "Chàm" },
    { key: "emerald", label: "Xanh lá" },
    { key: "amber", label: "Vàng cam" },
    { key: "purple", label: "Tím" },
    { key: "slate", label: "Xám" },
];

export const THEME_SOLID_BG: Record<RoleTheme, string> = {
    blue: "bg-blue-600",
    indigo: "bg-indigo-600",
    emerald: "bg-emerald-600",
    amber: "bg-amber-600",
    purple: "bg-purple-600",
    rose: "bg-rose-600",
    slate: "bg-slate-600",
};

export const THEME_STYLES: Record<
    RoleTheme,
    { iconBg: string; iconText: string; chipBorder: string }
> = {
    blue: { iconBg: "bg-blue-500/10", iconText: "text-blue-600", chipBorder: "border-blue-500/20" },
    indigo: { iconBg: "bg-indigo-500/10", iconText: "text-indigo-600", chipBorder: "border-indigo-500/20" },
    emerald: { iconBg: "bg-emerald-500/10", iconText: "text-emerald-600", chipBorder: "border-emerald-500/20" },
    amber: { iconBg: "bg-amber-500/10", iconText: "text-amber-600", chipBorder: "border-amber-500/20" },
    purple: { iconBg: "bg-purple-500/10", iconText: "text-purple-600", chipBorder: "border-purple-500/20" },
    rose: { iconBg: "bg-rose-500/10", iconText: "text-rose-600", chipBorder: "border-rose-500/20" },
    slate: { iconBg: "bg-slate-500/10", iconText: "text-slate-600", chipBorder: "border-slate-500/20" },
};

export const SCOPE_OPTIONS: { value: DataScope; label: string; code: string }[] = [
    { value: "all", label: "Toàn công ty (COMPANY)", code: "COMPANY" },
    { value: "department_managed", label: "Nhánh đơn vị trực thuộc (ORGANIZATION_BRANCH)", code: "ORGANIZATION_BRANCH" },
    { value: "personal", label: "Cá nhân (SELF)", code: "SELF" },
];

export const DEPARTMENTS: Department[] = [
    { id: "all", name: "Toàn công ty" },
    { id: "dept_tech", name: "Khối Kỹ thuật & Công nghệ" },
    { id: "dept_hr", name: "Phòng Nhân sự & Tuyển dụng" },
];

export const MODULES: ModuleDef[] = [
    {
        id: "users",
        name: "Quản lý tài khoản (/users)",
        category: "Quản trị Hệ thống",
        defaultScope: "all",
        defaultActions: { view: false, create: false, edit: false, delete: false, approve: false, export: false },
    },
    {
        id: "organization",
        name: "Cơ cấu tổ chức (/departments)",
        category: "Quản trị Hệ thống",
        defaultScope: "all",
        defaultActions: { view: true, create: false, edit: false, delete: false, approve: false, export: false },
    },
    {
        id: "access",
        name: "Phân quyền truy cập (/access)",
        category: "Quản trị Hệ thống",
        defaultScope: "all",
        defaultActions: { view: false, create: false, edit: false, delete: false, approve: false, export: false },
    },
    {
        id: "roles",
        name: "Vai trò chuyên môn (/roles)",
        category: "Quản trị Hệ thống",
        defaultScope: "all",
        defaultActions: { view: true, create: false, edit: false, delete: false, approve: false, export: false },
    },
    {
        id: "hrprofile",
        name: "Hồ sơ nhân sự (/hrprofile)",
        category: "Nhân sự & Hồ sơ",
        defaultScope: "all",
        defaultActions: { view: true, create: false, edit: false, delete: false, approve: false, export: true },
    },
    {
        id: "projects",
        name: "Quản lý dự án & WBS (/project)",
        category: "Dự án & Kế hoạch",
        defaultScope: "department_managed",
        defaultActions: { view: true, create: false, edit: false, delete: false, approve: false, export: false },
    },
    {
        id: "capacity",
        name: "Bảng năng lực & Phân bổ (/capacity)",
        category: "Nguồn lực & Năng lực",
        defaultScope: "department_managed",
        defaultActions: { view: true, create: false, edit: false, delete: false, approve: false, export: true },
    },
    {
        id: "availability",
        name: "Giờ khả dụng (/availability)",
        category: "Nguồn lực & Năng lực",
        defaultScope: "personal",
        defaultActions: { view: true, create: true, edit: true, delete: false, approve: false, export: false },
    },
    {
        id: "calendar",
        name: "Lịch & Ngày lễ (/working-calendar)",
        category: "Vận hành & Lịch",
        defaultScope: "all",
        defaultActions: { view: true, create: false, edit: false, delete: false, approve: false, export: false },
    },
    {
        id: "skills",
        name: "Quản lý & Khai báo Kỹ năng (/skills)",
        category: "Nguồn lực & Năng lực",
        defaultScope: "personal",
        defaultActions: { view: true, create: true, edit: true, delete: false, approve: false, export: false },
    },
    {
        id: "timesheets",
        name: "Bảng chấm công (/attendance)",
        category: "Vận hành & Công",
        defaultScope: "personal",
        defaultActions: { view: true, create: true, edit: true, delete: false, approve: false, export: false },
    },
    {
        id: "leaves",
        name: "Đơn nghỉ phép (/leave)",
        category: "Vận hành & Công",
        defaultScope: "personal",
        defaultActions: { view: true, create: true, edit: false, delete: true, approve: false, export: false },
    },
];

/** Ma trận phân quyền chuẩn của 6 vai trò nghiệp vụ (RBAC Specification) */
export const ROLE_DEFAULT_PERMISSIONS: Record<string, Record<string, ModulePermission>> = {
    "VT-01": {
        users: { moduleId: "users", moduleName: "Quản lý tài khoản", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        organization: { moduleId: "organization", moduleName: "Cơ cấu tổ chức", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        access: { moduleId: "access", moduleName: "Phân quyền truy cập", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        roles: { moduleId: "roles", moduleName: "Vai trò chuyên môn", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        hrprofile: { moduleId: "hrprofile", moduleName: "Hồ sơ nhân sự", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: true } },
        projects: { moduleId: "projects", moduleName: "Quản lý dự án & WBS", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: true } },
        capacity: { moduleId: "capacity", moduleName: "Bảng năng lực & Phân bổ", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: true } },
        availability: { moduleId: "availability", moduleName: "Giờ khả dụng", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        calendar: { moduleId: "calendar", moduleName: "Lịch & Ngày lễ", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        skills: { moduleId: "skills", moduleName: "Quản lý & Khai báo Kỹ năng", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: true } },
        timesheets: { moduleId: "timesheets", moduleName: "Bảng chấm công", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: true } },
        leaves: { moduleId: "leaves", moduleName: "Đơn nghỉ phép", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
    },
    "VT-02": {
        users: { moduleId: "users", moduleName: "Quản lý tài khoản", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        organization: { moduleId: "organization", moduleName: "Cơ cấu tổ chức", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        access: { moduleId: "access", moduleName: "Phân quyền truy cập", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        roles: { moduleId: "roles", moduleName: "Vai trò chuyên môn", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        hrprofile: { moduleId: "hrprofile", moduleName: "Hồ sơ nhân sự", scope: { type: "personal" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        projects: { moduleId: "projects", moduleName: "Quản lý dự án & WBS", scope: { type: "personal" }, actions: { view: true, create: true, edit: true, delete: true, approve: true, export: true } },
        capacity: { moduleId: "capacity", moduleName: "Bảng năng lực & Phân bổ", scope: { type: "personal" }, actions: { view: true, create: true, edit: false, delete: false, approve: false, export: true } },
        availability: { moduleId: "availability", moduleName: "Giờ khả dụng", scope: { type: "personal" }, actions: { view: true, create: true, edit: true, delete: false, approve: false, export: false } },
        calendar: { moduleId: "calendar", moduleName: "Lịch & Ngày lễ", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        skills: { moduleId: "skills", moduleName: "Quản lý & Khai báo Kỹ năng", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        timesheets: { moduleId: "timesheets", moduleName: "Bảng chấm công", scope: { type: "personal" }, actions: { view: true, create: true, edit: true, delete: false, approve: true, export: true } },
        leaves: { moduleId: "leaves", moduleName: "Đơn nghỉ phép", scope: { type: "personal" }, actions: { view: true, create: true, edit: false, delete: true, approve: false, export: false } },
    },
    "VT-03": {
        users: { moduleId: "users", moduleName: "Quản lý tài khoản", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        organization: { moduleId: "organization", moduleName: "Cơ cấu tổ chức", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        access: { moduleId: "access", moduleName: "Phân quyền truy cập", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        roles: { moduleId: "roles", moduleName: "Vai trò chuyên môn", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        hrprofile: { moduleId: "hrprofile", moduleName: "Hồ sơ nhân sự", scope: { type: "department_managed" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: true } },
        projects: { moduleId: "projects", moduleName: "Quản lý dự án & WBS", scope: { type: "department_managed" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        capacity: { moduleId: "capacity", moduleName: "Bảng năng lực & Phân bổ", scope: { type: "department_managed" }, actions: { view: true, create: true, edit: true, delete: true, approve: true, export: true } },
        availability: { moduleId: "availability", moduleName: "Giờ khả dụng", scope: { type: "department_managed" }, actions: { view: true, create: true, edit: true, delete: false, approve: false, export: false } },
        calendar: { moduleId: "calendar", moduleName: "Lịch & Ngày lễ", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        skills: { moduleId: "skills", moduleName: "Quản lý & Khai báo Kỹ năng", scope: { type: "department_managed" }, actions: { view: true, create: false, edit: true, delete: false, approve: true, export: true } },
        timesheets: { moduleId: "timesheets", moduleName: "Bảng chấm công", scope: { type: "department_managed" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: true } },
        leaves: { moduleId: "leaves", moduleName: "Đơn nghỉ phép", scope: { type: "department_managed" }, actions: { view: true, create: true, edit: false, delete: true, approve: true, export: true } },
    },
    "VT-04": {
        users: { moduleId: "users", moduleName: "Quản lý tài khoản", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        organization: { moduleId: "organization", moduleName: "Cơ cấu tổ chức", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        access: { moduleId: "access", moduleName: "Phân quyền truy cập", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        roles: { moduleId: "roles", moduleName: "Vai trò chuyên môn", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        hrprofile: { moduleId: "hrprofile", moduleName: "Hồ sơ nhân sự", scope: { type: "personal" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        projects: { moduleId: "projects", moduleName: "Quản lý dự án & WBS", scope: { type: "personal" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        capacity: { moduleId: "capacity", moduleName: "Bảng năng lực & Phân bổ", scope: { type: "personal" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        availability: { moduleId: "availability", moduleName: "Giờ khả dụng", scope: { type: "personal" }, actions: { view: true, create: true, edit: true, delete: false, approve: false, export: false } },
        calendar: { moduleId: "calendar", moduleName: "Lịch & Ngày lễ", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        skills: { moduleId: "skills", moduleName: "Quản lý & Khai báo Kỹ năng", scope: { type: "personal" }, actions: { view: true, create: true, edit: true, delete: true, approve: false, export: false } },
        timesheets: { moduleId: "timesheets", moduleName: "Bảng chấm công", scope: { type: "personal" }, actions: { view: true, create: true, edit: true, delete: false, approve: false, export: false } },
        leaves: { moduleId: "leaves", moduleName: "Đơn nghỉ phép", scope: { type: "personal" }, actions: { view: true, create: true, edit: false, delete: true, approve: false, export: false } },
    },
    "VT-05": {
        users: { moduleId: "users", moduleName: "Quản lý tài khoản", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        organization: { moduleId: "organization", moduleName: "Cơ cấu tổ chức", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        access: { moduleId: "access", moduleName: "Phân quyền truy cập", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        roles: { moduleId: "roles", moduleName: "Vai trò chuyên môn", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        hrprofile: { moduleId: "hrprofile", moduleName: "Hồ sơ nhân sự", scope: { type: "all" }, actions: { view: true, create: true, edit: true, delete: true, approve: true, export: true } },
        projects: { moduleId: "projects", moduleName: "Quản lý dự án & WBS", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        capacity: { moduleId: "capacity", moduleName: "Bảng năng lực & Phân bổ", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        availability: { moduleId: "availability", moduleName: "Giờ khả dụng", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        calendar: { moduleId: "calendar", moduleName: "Lịch & Ngày lễ", scope: { type: "all" }, actions: { view: true, create: true, edit: true, delete: true, approve: true, export: true } },
        skills: { moduleId: "skills", moduleName: "Quản lý & Khai báo Kỹ năng", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        timesheets: { moduleId: "timesheets", moduleName: "Bảng chấm công", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: true } },
        leaves: { moduleId: "leaves", moduleName: "Đơn nghỉ phép", scope: { type: "all" }, actions: { view: true, create: false, edit: true, delete: false, approve: true, export: true } },
    },
    "VT-06": {
        users: { moduleId: "users", moduleName: "Quản lý tài khoản", scope: { type: "all" }, actions: { view: true, create: true, edit: true, delete: true, approve: false, export: true } },
        organization: { moduleId: "organization", moduleName: "Cơ cấu tổ chức", scope: { type: "all" }, actions: { view: true, create: true, edit: true, delete: true, approve: true, export: true } },
        access: { moduleId: "access", moduleName: "Phân quyền truy cập", scope: { type: "all" }, actions: { view: true, create: true, edit: true, delete: true, approve: true, export: true } },
        roles: { moduleId: "roles", moduleName: "Vai trò chuyên môn", scope: { type: "all" }, actions: { view: true, create: true, edit: true, delete: true, approve: true, export: true } },
        hrprofile: { moduleId: "hrprofile", moduleName: "Hồ sơ nhân sự", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: true } },
        projects: { moduleId: "projects", moduleName: "Quản lý dự án & WBS", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        capacity: { moduleId: "capacity", moduleName: "Bảng năng lực & Phân bổ", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        availability: { moduleId: "availability", moduleName: "Giờ khả dụng", scope: { type: "all" }, actions: { view: true, create: false, edit: false, delete: false, approve: false, export: false } },
        calendar: { moduleId: "calendar", moduleName: "Lịch & Ngày lễ", scope: { type: "all" }, actions: { view: true, create: true, edit: true, delete: true, approve: true, export: true } },
        skills: { moduleId: "skills", moduleName: "Quản lý & Khai báo Kỹ năng", scope: { type: "all" }, actions: { view: true, create: true, edit: true, delete: true, approve: true, export: true } },
        timesheets: { moduleId: "timesheets", moduleName: "Bảng chấm công", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
        leaves: { moduleId: "leaves", moduleName: "Đơn nghỉ phép", scope: { type: "all" }, actions: { view: false, create: false, edit: false, delete: false, approve: false, export: false } },
    },
};

export const ROLES: Omit<Role, "permissions">[] = [
    {
        id: "VT-01",
        name: "Ban giám đốc",
        description: "Điều hành toàn diện, theo dõi năng lực và mức độ tải của toàn bộ nhân sự",
        isSystemRole: true,
        userCount: 0,
        theme: "blue",
        departmentId: "all",
    },
    {
        id: "VT-02",
        name: "Quản lý dự án (PM)",
        description: "Chịu trách nhiệm các dự án được giao: Tạo WBS, giao việc, đặt ngân sách giờ và duyệt công",
        isSystemRole: true,
        userCount: 0,
        theme: "indigo",
        departmentId: "all",
    },
    {
        id: "VT-03",
        name: "Quản lý nguồn lực (RM)",
        description: "Trưởng bộ phận chuyên môn: Điều phối nhân sự theo tuần, giữ chỗ nguồn lực, duyệt nghỉ phép",
        isSystemRole: true,
        userCount: 0,
        theme: "emerald",
        departmentId: "all",
    },
    {
        id: "VT-04",
        name: "Nhân viên chuyên môn",
        description: "Thực hiện công việc dự án, xem lịch phân bổ cá nhân, nộp chấm công, gửi đơn nghỉ phép, khai báo kỹ năng",
        isSystemRole: true,
        userCount: 0,
        theme: "slate",
        departmentId: "all",
    },
    {
        id: "VT-05",
        name: "Nhân sự (HR)",
        description: "Quản lý hồ sơ nhân sự, hợp đồng lao động, lịch làm việc chuẩn và danh sách ngày nghỉ lễ",
        isSystemRole: true,
        userCount: 0,
        theme: "amber",
        departmentId: "dept_hr",
    },
    {
        id: "VT-06",
        name: "Quản trị viên (Admin)",
        description: "Toàn quyền quản trị kỹ thuật, quản lý tài khoản (/users), cơ cấu tổ chức (/departments) và phân quyền",
        isSystemRole: true,
        userCount: 0,
        theme: "purple",
        departmentId: "all",
    },
];