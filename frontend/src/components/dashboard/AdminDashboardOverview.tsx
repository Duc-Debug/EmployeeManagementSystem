import { useState, useEffect } from "react";
import {
    Users,
    Building2,
    ShieldCheck,
    Briefcase,
    Lock,
    UserCheck,
    ArrowUpRight,
    Layers,
    CalendarDays,
    BookOpen,
    KeyRound,
    RefreshCw,
    Activity,
} from "lucide-react";
import { getUsers } from "@/lib/api/users";
import { getOrgTree } from "@/lib/api/org-units";
import { getProjectRoles } from "@/lib/api/project-roles";
import { getSkills } from "@/lib/api/skills";
import type { User, OrgUnitTreeNode } from "@/types/hrm";

interface AdminDashboardOverviewProps {
    onNavigate: (tabId: string) => void;
}

function countOrgUnits(nodes: readonly OrgUnitTreeNode[] | OrgUnitTreeNode | null | undefined): number {
    if (!nodes) return 0;
    if (Array.isArray(nodes)) {
        return nodes.reduce((acc, node) => acc + 1 + countOrgUnits(node.children), 0);
    }
    const singleNode = nodes as OrgUnitTreeNode;
    return 1 + countOrgUnits(singleNode.children);
}

export default function AdminDashboardOverview({ onNavigate }: AdminDashboardOverviewProps) {
    const [loading, setLoading] = useState(true);
    const [users, setUsers] = useState<User[]>([]);
    const [totalUsers, setTotalUsers] = useState(0);
    const [activeUsersCount, setActiveUsersCount] = useState(0);
    const [lockedUsersCount, setLockedUsersCount] = useState(0);
    const [orgUnitsCount, setOrgUnitsCount] = useState(0);
    const [projectRolesCount, setProjectRolesCount] = useState(0);
    const [skillsCount, setSkillsCount] = useState(0);

    const loadData = async () => {
        setLoading(true);
        try {
            const [usersRes, orgTreeRes, pRolesRes, skillsRes] = await Promise.allSettled([
                getUsers(0, 100),
                getOrgTree(),
                getProjectRoles(true),
                getSkills(),
            ]);

            if (usersRes.status === "fulfilled" && usersRes.value) {
                const list = usersRes.value.content || [];
                setUsers(list);
                setTotalUsers(usersRes.value.totalElements || list.length);
                setActiveUsersCount(list.filter((u) => u.status === "ACTIVE").length);
                setLockedUsersCount(list.filter((u) => u.status !== "ACTIVE").length);
            }

            if (orgTreeRes.status === "fulfilled" && orgTreeRes.value) {
                setOrgUnitsCount(countOrgUnits(orgTreeRes.value));
            }

            if (pRolesRes.status === "fulfilled" && pRolesRes.value) {
                setProjectRolesCount(pRolesRes.value.length);
            }

            if (skillsRes.status === "fulfilled" && skillsRes.value) {
                setSkillsCount(skillsRes.value.length);
            }
        } catch (err) {
            console.error("Failed to load admin overview data:", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, []);

    const today = new Date();
    const formattedDate = today.toLocaleDateString("vi-VN", {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
    });
    const formattedDateCapitalized =
        formattedDate.charAt(0).toUpperCase() + formattedDate.slice(1);

    return (
        <div className="space-y-4">
            {/* Header Clean White */}
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-3">
                <div>
                    <h1 className="text-xl font-bold tracking-tight text-slate-900">
                        Tổng quan Quản trị Hệ thống
                    </h1>
                    <p className="text-xs text-slate-500 mt-0.5">
                        {formattedDateCapitalized} · Giám sát tài nguyên, tài khoản và an toàn bảo mật toàn hệ thống.
                    </p>
                </div>

                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={loadData}
                        disabled={loading}
                        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-2xs cursor-pointer disabled:opacity-50"
                        title="Tải lại dữ liệu"
                    >
                        <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin text-indigo-600" : "text-slate-500"}`} />
                        <span>Làm mới</span>
                    </button>
                </div>
            </div>

            {/* KPI Cards (Thu nhỏ thêm 25% - Cực kỳ tinh gọn) */}
            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
                {/* Tài khoản */}
                <div
                    onClick={() => onNavigate("users")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-indigo-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-50 border border-indigo-100 text-indigo-600">
                            <Users className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-indigo-600 group-hover:translate-x-0.5 transition">
                            Chi tiết <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Tài khoản Người dùng
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{totalUsers}</span>
                            <span className="text-[10px] text-slate-400">tổng số</span>
                        </div>
                        <div className="mt-1 flex items-center gap-2 text-[10px]">
                            <span className="inline-flex items-center gap-0.5 text-emerald-600 font-semibold">
                                <UserCheck className="h-3 w-3" /> {activeUsersCount} Active
                            </span>
                            <span className="inline-flex items-center gap-0.5 text-rose-500 font-semibold">
                                <Lock className="h-3 w-3" /> {lockedUsersCount} Khóa
                            </span>
                        </div>
                    </div>
                </div>

                {/* Cơ cấu tổ chức */}
                <div
                    onClick={() => onNavigate("departments")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-emerald-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-50 border border-emerald-100 text-emerald-600">
                            <Building2 className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-emerald-600 group-hover:translate-x-0.5 transition">
                            Xem cây <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Cơ cấu Tổ chức
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{orgUnitsCount || 11}</span>
                            <span className="text-[10px] text-slate-400">đơn vị / phòng</span>
                        </div>
                        <div className="mt-1 text-[10px] text-emerald-700 font-medium">
                            Cây tổ chức phân cấp
                        </div>
                    </div>
                </div>

                {/* Danh mục Chuyên môn */}
                <div
                    onClick={() => onNavigate("roles")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-purple-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-purple-50 border border-purple-100 text-purple-600">
                            <Briefcase className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-purple-600 group-hover:translate-x-0.5 transition">
                            Cấu hình <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Vai trò Dự án & Kỹ năng
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-lg font-bold text-slate-900">{projectRolesCount}</span>
                            <span className="text-[10px] text-slate-400">vai trò</span>
                        </div>
                        <div className="mt-1 flex items-center gap-1 text-[10px] text-purple-700 font-medium">
                            <Layers className="h-2.5 w-2.5 text-purple-500" />
                            <span>{skillsCount} kỹ năng chuẩn</span>
                        </div>
                    </div>
                </div>

                {/* Trạng thái An ninh */}
                <div
                    onClick={() => onNavigate("access")}
                    className="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-2.5 sm:p-3 shadow-2xs transition hover:border-sky-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-sky-50 border border-sky-100 text-sky-600">
                            <ShieldCheck className="h-3.5 w-3.5" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-sky-600 group-hover:translate-x-0.5 transition">
                            Phân quyền <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-1.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Kiểm soát Phân quyền
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-sm font-bold text-slate-900">RBAC + Scope</span>
                        </div>
                        <div className="mt-1 text-[10px] font-semibold text-indigo-700">
                            6 Vai trò chuẩn (VT-01 → VT-06)
                        </div>
                    </div>
                </div>
            </div>

            {/* Quick Actions Bar (Thu nhỏ gọn gàng) */}
            <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-2xs">
                <div className="flex items-center gap-1.5 mb-2">
                    <Activity className="h-3.5 w-3.5 text-indigo-600" />
                    <h3 className="text-[10px] font-bold text-slate-900 uppercase tracking-wider">
                        Lối tắt tác vụ Quản trị nhanh
                    </h3>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-2">
                    <button
                        type="button"
                        onClick={() => onNavigate("users")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-indigo-300 hover:bg-indigo-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-indigo-600 text-white shadow-2xs">
                            <Users className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Quản lý Tài khoản</span>
                            <p className="text-[9px] text-slate-400 truncate">Tạo, sửa, khóa</p>
                        </div>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("access")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-sky-300 hover:bg-sky-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-sky-600 text-white shadow-2xs">
                            <KeyRound className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Phân quyền Truy cập</span>
                            <p className="text-[9px] text-slate-400 truncate">Vai trò & DataScope</p>
                        </div>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("departments")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-emerald-300 hover:bg-emerald-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-emerald-600 text-white shadow-2xs">
                            <Building2 className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Cơ cấu Tổ chức</span>
                            <p className="text-[9px] text-slate-400 truncate">Cây phòng ban</p>
                        </div>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("working-calendar")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-amber-300 hover:bg-amber-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-amber-600 text-white shadow-2xs">
                            <CalendarDays className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Lịch & Ngày lễ</span>
                            <p className="text-[9px] text-slate-400 truncate">Giờ chuẩn & Lễ</p>
                        </div>
                    </button>

                    <button
                        type="button"
                        onClick={() => onNavigate("skills")}
                        className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50/50 p-2 text-left transition hover:border-purple-300 hover:bg-purple-50/40 cursor-pointer"
                    >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-purple-600 text-white shadow-2xs">
                            <BookOpen className="h-3 w-3" />
                        </div>
                        <div className="min-w-0">
                            <span className="block text-[11px] font-semibold text-slate-900 truncate">Kỹ năng & Ma trận</span>
                            <p className="text-[9px] text-slate-400 truncate">Danh mục & Rà soát</p>
                        </div>
                    </button>
                </div>
            </div>

            {/* Recent Accounts Table (Full width, Clean & Compact) */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                <div className="flex items-center justify-between mb-3">
                    <div>
                        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                            Danh sách Tài khoản gần đây
                        </h3>
                        <p className="text-[11px] text-slate-400 mt-0.5">
                            Các tài khoản người dùng đang được quản trị trên hệ thống
                        </p>
                    </div>
                    <button
                        type="button"
                        onClick={() => onNavigate("users")}
                        className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition cursor-pointer"
                    >
                        Xem tất cả ({totalUsers}) →
                    </button>
                </div>

                <div className="overflow-x-auto rounded-xl border border-slate-100">
                    <table className="w-full text-left text-xs border-collapse">
                        <thead>
                            <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                                <th className="px-4 py-2.5">Người dùng</th>
                                <th className="px-4 py-2.5">Tên đăng nhập</th>
                                <th className="px-4 py-2.5">Vai trò</th>
                                <th className="px-4 py-2.5">Phạm vi (Data Scope)</th>
                                <th className="px-4 py-2.5 text-right">Trạng thái</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {users.slice(0, 8).map((u) => (
                                <tr key={u.id} className="hover:bg-slate-50/70 transition">
                                    <td className="px-4 py-2.5">
                                        <div className="flex items-center gap-2.5">
                                            <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-[10px] font-bold text-indigo-700">
                                                {(u.fullName || u.username).substring(0, 2).toUpperCase()}
                                            </div>
                                            <div>
                                                <p className="font-semibold text-slate-900">{u.fullName || u.username}</p>
                                                <p className="text-[10px] text-slate-400">{u.employeeId ? `Mã NV: ${u.employeeId}` : "Chưa gán hồ sơ"}</p>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-4 py-2.5 font-mono text-[11px] text-slate-700">
                                        {u.username}
                                    </td>
                                    <td className="px-4 py-2.5">
                                        <span className="inline-flex rounded-md bg-slate-100 px-2 py-0.5 font-semibold text-slate-700 text-[11px]">
                                            {u.roleName || u.roleCode}
                                        </span>
                                    </td>
                                    <td className="px-4 py-2.5 text-slate-600 text-[11px]">
                                        {u.dataScope === "COMPANY"
                                            ? "Toàn công ty"
                                            : u.dataScope === "ORGANIZATION_BRANCH"
                                            ? "Nhánh đơn vị"
                                            : "Cá nhân"}
                                    </td>
                                    <td className="px-4 py-2.5 text-right">
                                        <span
                                            className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                                                u.status === "ACTIVE"
                                                    ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                                                    : "bg-rose-50 text-rose-700 border border-rose-200"
                                            }`}
                                        >
                                            <span className={`h-1.5 w-1.5 rounded-full ${u.status === "ACTIVE" ? "bg-emerald-500" : "bg-rose-500"}`} />
                                            {u.status === "ACTIVE" ? "Hoạt động" : "Đã khóa"}
                                        </span>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
