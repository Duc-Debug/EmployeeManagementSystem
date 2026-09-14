import React, { useState, useEffect, useMemo } from 'react';
import {
    ShieldCheck,
    Users,
    Info,
    Search,
    ChevronRight,
    TableProperties,
    RefreshCw,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { getUsers } from '@/lib/api/users';
import { getRoles } from '@/lib/api/roles';
import type { User } from '@/types/hrm';
import {
    ROLES,
    MODULES,
    ROLE_DEFAULT_PERMISSIONS,
    THEME_STYLES,
} from './access.constants';
import type { Role, RoleTheme } from './access.types';

export const AccessControlView: React.FC = () => {
    const [selectedRoleId, setSelectedRoleId] = useState<string>('VT-01');
    const [activeSubTab, setActiveSubTab] = useState<'matrix' | 'users' | 'overview_all'>('matrix');
    const [loading, setLoading] = useState(true);
    const [userList, setUserList] = useState<User[]>([]);
    const [searchKeyword, setSearchKeyword] = useState('');

    const loadData = async () => {
        setLoading(true);
        try {
            const [usersRes] = await Promise.allSettled([
                getUsers(0, 100),
                getRoles(),
            ]);

            if (usersRes.status === 'fulfilled' && usersRes.value) {
                setUserList(usersRes.value.content || []);
            }
        } catch (err) {
            console.error('Failed to load users for access control:', err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, []);

    // Tính toán số lượng user thực tế cho từng role
    const rolesWithCounts = useMemo<Role[]>(() => {
        return ROLES.map((r) => {
            const normalized = r.id.toUpperCase().replace(/_/g, '-');
            const matchingUsers = userList.filter(
                (u) => u.roleCode?.toUpperCase().replace(/_/g, '-') === normalized
            );
            return {
                ...r,
                userCount: matchingUsers.length,
                permissions: ROLE_DEFAULT_PERMISSIONS[r.id] || {},
            };
        });
    }, [userList]);

    const selectedRole = rolesWithCounts.find((r) => r.id === selectedRoleId) || rolesWithCounts[0];

    // Lọc danh sách user thuộc role đang chọn
    const usersInSelectedRole = useMemo(() => {
        if (!selectedRole) return [];
        const normalized = selectedRole.id.toUpperCase().replace(/_/g, '-');
        return userList.filter(
            (u) => u.roleCode?.toUpperCase().replace(/_/g, '-') === normalized
        );
    }, [userList, selectedRole]);

    const filteredRoles = useMemo(() => {
        const kw = searchKeyword.trim().toLowerCase();
        if (!kw) return rolesWithCounts;
        return rolesWithCounts.filter(
            (r) => r.name.toLowerCase().includes(kw) || r.id.toLowerCase().includes(kw) || r.description.toLowerCase().includes(kw)
        );
    }, [rolesWithCounts, searchKeyword]);

    return (
        <div className="flex flex-col h-full space-y-4">
            {/* Header */}
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-3">
                <div>
                    <h1 className="text-xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
                        <ShieldCheck className="h-5 w-5 text-indigo-600" />
                        Quản lý Vai trò & Phân quyền Hệ thống (RBAC)
                    </h1>
                    <p className="text-xs text-slate-500 mt-0.5">
                        Kiểm soát phân hệ truy cập và phạm vi dữ liệu (Data Scope) theo 6 vai trò nghiệp vụ chuẩn hóa của doanh nghiệp.
                    </p>
                </div>

                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={() => setActiveSubTab(activeSubTab === 'overview_all' ? 'matrix' : 'overview_all')}
                        className={cn(
                            'inline-flex items-center gap-1.5 rounded-xl border px-3 py-1.5 text-xs font-semibold transition cursor-pointer',
                            activeSubTab === 'overview_all'
                                ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                                : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                        )}
                    >
                        <TableProperties className="h-3.5 w-3.5" />
                        <span>{activeSubTab === 'overview_all' ? 'Xem chi tiết vai trò' : 'Bảng ma trận đối chiếu 6 vai trò'}</span>
                    </button>

                    <button
                        type="button"
                        onClick={loadData}
                        disabled={loading}
                        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-2xs cursor-pointer disabled:opacity-50"
                        title="Tải lại dữ liệu"
                    >
                        <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin text-indigo-600' : 'text-slate-500'}`} />
                        <span>Làm mới</span>
                    </button>
                </div>
            </div>

            {/* Architecture Explanatory Banner */}
            <div className="rounded-xl border border-indigo-100 bg-gradient-to-r from-indigo-50/70 via-sky-50/40 to-white p-3 text-xs text-slate-700">
                <div className="flex items-start gap-2.5">
                    <Info className="h-4 w-4 text-indigo-600 shrink-0 mt-0.5" />
                    <div className="space-y-1">
                        <span className="font-bold text-indigo-900">Quy chuẩn kiến trúc phân quyền hệ thống (Fail-Closed RBAC & Data Scope):</span>
                        <p className="text-slate-600 leading-relaxed">
                            Hệ thống áp dụng <strong>6 Vai trò nghiệp vụ bất biến</strong> (VT-01 → VT-06). Khi phân quyền tài khoản tại mục <strong className="text-indigo-700">Quản lý tài khoản</strong>, mỗi người dùng được gán kèm 1 trong 3 cấp độ <strong>Data Scope</strong>: <span className="font-semibold text-slate-900">COMPANY</span> (Toàn công ty), <span className="font-semibold text-slate-900">ORGANIZATION_BRANCH</span> (Nhánh đơn vị trực thuộc) hoặc <span className="font-semibold text-slate-900">SELF</span> (Cá nhân).
                        </p>
                    </div>
                </div>
            </div>

            {/* Main Content Area */}
            {activeSubTab === 'overview_all' ? (
                /* Bảng Ma trận Tổng quan Đối chiếu 6 Vai trò */
                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs overflow-hidden">
                    <div className="mb-3 flex items-center justify-between">
                        <div>
                            <h3 className="text-sm font-bold text-slate-900">Ma trận Phân quyền 6 Vai trò Nghiệp vụ (All-in-one Screen Matrix)</h3>
                            <p className="text-xs text-slate-500">Đối chiếu quyền truy cập của từng vai trò trên tất cả 12 phân hệ nghiệp vụ</p>
                        </div>
                    </div>

                    <div className="overflow-x-auto rounded-xl border border-slate-200">
                        <table className="w-full text-left text-xs border-collapse">
                            <thead>
                                <tr className="bg-slate-50 border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-600">
                                    <th className="px-4 py-3 min-w-[200px]">Phân hệ / Màn hình</th>
                                    <th className="px-3 py-3 text-center min-w-[120px]">VT-01<br/><span className="text-[10px] font-normal text-slate-500">Ban giám đốc</span></th>
                                    <th className="px-3 py-3 text-center min-w-[120px]">VT-02<br/><span className="text-[10px] font-normal text-slate-500">Quản lý dự án</span></th>
                                    <th className="px-3 py-3 text-center min-w-[120px]">VT-03<br/><span className="text-[10px] font-normal text-slate-500">Quản lý nguồn lực</span></th>
                                    <th className="px-3 py-3 text-center min-w-[120px]">VT-04<br/><span className="text-[10px] font-normal text-slate-500">Nhân viên</span></th>
                                    <th className="px-3 py-3 text-center min-w-[120px]">VT-05<br/><span className="text-[10px] font-normal text-slate-500">Nhân sự</span></th>
                                    <th className="px-3 py-3 text-center min-w-[120px]">VT-06<br/><span className="text-[10px] font-normal text-slate-500">Quản trị viên</span></th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {MODULES.map((mod) => {
                                    const p1 = ROLE_DEFAULT_PERMISSIONS['VT-01']?.[mod.id];
                                    const p2 = ROLE_DEFAULT_PERMISSIONS['VT-02']?.[mod.id];
                                    const p3 = ROLE_DEFAULT_PERMISSIONS['VT-03']?.[mod.id];
                                    const p4 = ROLE_DEFAULT_PERMISSIONS['VT-04']?.[mod.id];
                                    const p5 = ROLE_DEFAULT_PERMISSIONS['VT-05']?.[mod.id];
                                    const p6 = ROLE_DEFAULT_PERMISSIONS['VT-06']?.[mod.id];

                                    const renderCellBadge = (p?: any) => {
                                        if (!p || !p.actions.view) {
                                            return <span className="inline-block rounded-md bg-rose-50 px-2 py-0.5 text-[11px] font-semibold text-rose-600 border border-rose-100">❌ Ẩn</span>;
                                        }
                                        if (p.actions.create && p.actions.edit && (p.actions.delete || p.actions.approve)) {
                                            return <span className="inline-block rounded-md bg-emerald-50 px-2 py-0.5 text-[11px] font-bold text-emerald-700 border border-emerald-200">✅ Toàn quyền</span>;
                                        }
                                        if (p.actions.create && !p.actions.delete) {
                                            return <span className="inline-block rounded-md bg-indigo-50 px-2 py-0.5 text-[11px] font-semibold text-indigo-700 border border-indigo-200">📝 Ghi / Đề xuất</span>;
                                        }
                                        return <span className="inline-block rounded-md bg-sky-50 px-2 py-0.5 text-[11px] font-medium text-sky-700 border border-sky-200">👁️ Xem ({p.scope?.type === 'all' ? 'Cty' : p.scope?.type === 'department_managed' ? 'Nhánh' : 'Team/Mình'})</span>;
                                    };

                                    return (
                                        <tr key={mod.id} className="hover:bg-slate-50/70 transition">
                                            <td className="px-4 py-2.5 font-semibold text-slate-900">
                                                <span>{mod.name}</span>
                                                <span className="block text-[10px] font-normal text-slate-400">{mod.category}</span>
                                            </td>
                                            <td className="px-3 py-2.5 text-center">{renderCellBadge(p1)}</td>
                                            <td className="px-3 py-2.5 text-center">{renderCellBadge(p2)}</td>
                                            <td className="px-3 py-2.5 text-center">{renderCellBadge(p3)}</td>
                                            <td className="px-3 py-2.5 text-center">{renderCellBadge(p4)}</td>
                                            <td className="px-3 py-2.5 text-center">{renderCellBadge(p5)}</td>
                                            <td className="px-3 py-2.5 text-center">{renderCellBadge(p6)}</td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>
            ) : (
                /* Chi tiết từng Vai trò */
                <div className="grid grid-cols-12 gap-4">
                    {/* Left: Role Selection List */}
                    <div className="col-span-12 lg:col-span-4 rounded-2xl border border-slate-200 bg-white p-3.5 shadow-2xs">
                        <div className="mb-3 flex items-center justify-between px-1">
                            <h2 className="text-xs font-bold uppercase tracking-wider text-slate-800">Danh sách Vai trò ({rolesWithCounts.length})</h2>
                            <span className="text-[11px] font-semibold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md border border-indigo-100">
                                RBAC Chuẩn
                            </span>
                        </div>

                        <div className="relative mb-3">
                            <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                            <input
                                type="text"
                                value={searchKeyword}
                                onChange={(e) => setSearchKeyword(e.target.value)}
                                placeholder="Tìm mã hoặc tên vai trò..."
                                className="w-full rounded-xl border border-slate-200 bg-slate-50/70 py-1.5 pl-8 pr-3 text-xs text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white"
                            />
                        </div>

                        <div className="space-y-1.5 max-h-[calc(100vh-340px)] overflow-y-auto pr-1">
                            {filteredRoles.map((role) => {
                                const currentTheme: RoleTheme = role.theme || 'blue';
                                const theme = THEME_STYLES[currentTheme] || THEME_STYLES.blue;
                                const isSelected = role.id === selectedRoleId;

                                return (
                                    <div
                                        key={role.id}
                                        onClick={() => setSelectedRoleId(role.id)}
                                        className={cn(
                                            'group cursor-pointer rounded-xl border p-2.5 transition-all',
                                            isSelected
                                                ? 'border-indigo-300 bg-indigo-50/70 shadow-xs'
                                                : 'border-slate-200/80 bg-slate-50/40 hover:border-slate-300 hover:bg-white'
                                        )}
                                    >
                                        <div className="flex items-center justify-between gap-2">
                                            <div className="flex items-center gap-2.5 min-w-0">
                                                <span className={cn('flex h-7 w-7 shrink-0 items-center justify-center rounded-lg border text-xs font-bold', theme.iconBg, theme.iconText, theme.chipBorder)}>
                                                    {role.id.replace('VT-0', '')}
                                                </span>
                                                <div className="min-w-0">
                                                    <p className={cn('truncate text-xs font-bold', isSelected ? 'text-indigo-900' : 'text-slate-800')}>
                                                        {role.name}
                                                    </p>
                                                    <p className="text-[10px] text-slate-400 font-mono">{role.id}</p>
                                                </div>
                                            </div>

                                            <div className="flex items-center gap-1.5 shrink-0">
                                                <span className="inline-flex items-center gap-1 rounded-md bg-white px-2 py-0.5 text-[10px] font-semibold text-slate-700 border border-slate-200">
                                                    <Users className="h-2.5 w-2.5 text-slate-400" />
                                                    {role.userCount}
                                                </span>
                                                <ChevronRight className={cn('h-3.5 w-3.5 text-slate-400 transition', isSelected && 'text-indigo-600 translate-x-0.5')} />
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>

                    {/* Right: Role Detail & Permission Matrix */}
                    <div className="col-span-12 lg:col-span-8 rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                        {selectedRole && (
                            <div className="space-y-4">
                                {/* Role Header Card */}
                                <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-100 pb-3">
                                    <div>
                                        <div className="flex items-center gap-2">
                                            <span className="rounded-lg bg-indigo-600 px-2 py-0.5 text-xs font-mono font-bold text-white shadow-2xs">
                                                {selectedRole.id}
                                            </span>
                                            <h2 className="text-base font-bold text-slate-900">{selectedRole.name}</h2>
                                            <span className="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-[10px] font-bold uppercase text-amber-700">
                                                Vai trò hệ thống
                                            </span>
                                        </div>
                                        <p className="mt-1 text-xs text-slate-500 leading-relaxed max-w-xl">
                                            {selectedRole.description}
                                        </p>
                                    </div>

                                    {/* Subtabs switcher */}
                                    <div className="flex rounded-xl border border-slate-200 bg-slate-100 p-0.5 text-xs font-semibold gap-0.5">
                                        <button
                                            type="button"
                                            onClick={() => setActiveSubTab('matrix')}
                                            className={cn(
                                                'rounded-lg px-3 py-1.5 transition cursor-pointer',
                                                activeSubTab === 'matrix'
                                                    ? 'bg-white text-indigo-700 shadow-2xs font-bold'
                                                    : 'text-slate-600 hover:text-slate-900'
                                            )}
                                        >
                                            Ma trận quyền hạn
                                        </button>
                                        <button
                                            type="button"
                                            onClick={() => setActiveSubTab('users')}
                                            className={cn(
                                                'rounded-lg px-3 py-1.5 transition cursor-pointer flex items-center gap-1.5',
                                                activeSubTab === 'users'
                                                    ? 'bg-white text-indigo-700 shadow-2xs font-bold'
                                                    : 'text-slate-600 hover:text-slate-900'
                                            )}
                                        >
                                            <span>Người dùng</span>
                                            <span className="rounded-full bg-indigo-100 px-1.5 py-0.2 text-[10px] font-bold text-indigo-700">
                                                {selectedRole.userCount}
                                            </span>
                                        </button>
                                    </div>
                                </div>

                                {activeSubTab === 'matrix' ? (
                                    /* Permission Table */
                                    <div className="space-y-3">
                                        <div className="overflow-x-auto rounded-xl border border-slate-200">
                                            <table className="w-full text-left text-xs border-collapse">
                                                <thead>
                                                    <tr className="bg-slate-50 border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                                                        <th className="px-4 py-2.5">Phân hệ Chức năng</th>
                                                        <th className="px-3 py-2.5">Mức truy cập</th>
                                                        <th className="px-3 py-2.5">Phạm vi (Data Scope)</th>
                                                        <th className="px-2 py-2.5 text-center">Xem</th>
                                                        <th className="px-2 py-2.5 text-center">Thêm</th>
                                                        <th className="px-2 py-2.5 text-center">Sửa</th>
                                                        <th className="px-2 py-2.5 text-center">Xóa</th>
                                                        <th className="px-2 py-2.5 text-center">Duyệt</th>
                                                    </tr>
                                                </thead>
                                                <tbody className="divide-y divide-slate-100">
                                                    {MODULES.map((mod) => {
                                                        const p = selectedRole.permissions[mod.id];
                                                        const isView = p?.actions.view ?? false;
                                                        const isCreate = p?.actions.create ?? false;
                                                        const isEdit = p?.actions.edit ?? false;
                                                        const isDelete = p?.actions.delete ?? false;
                                                        const isApprove = p?.actions.approve ?? false;

                                                        let accessBadge = (
                                                            <span className="inline-block rounded-md bg-rose-50 px-2 py-0.5 text-[10px] font-bold text-rose-600 border border-rose-100">
                                                                ❌ Bị ẩn (Chặn 403)
                                                            </span>
                                                        );
                                                        if (isView && isCreate && isEdit && (isDelete || isApprove)) {
                                                            accessBadge = (
                                                                <span className="inline-block rounded-md bg-emerald-50 px-2 py-0.5 text-[10px] font-bold text-emerald-700 border border-emerald-200">
                                                                    ✅ Toàn quyền
                                                                </span>
                                                            );
                                                        } else if (isView && isCreate) {
                                                            accessBadge = (
                                                                <span className="inline-block rounded-md bg-indigo-50 px-2 py-0.5 text-[10px] font-bold text-indigo-700 border border-indigo-200">
                                                                    📝 Ghi / Đề xuất
                                                                </span>
                                                            );
                                                        } else if (isView) {
                                                            accessBadge = (
                                                                <span className="inline-block rounded-md bg-sky-50 px-2 py-0.5 text-[10px] font-semibold text-sky-700 border border-sky-200">
                                                                    👁️ Xem (Read-only)
                                                                </span>
                                                            );
                                                        }

                                                        const renderDot = (active: boolean) => (
                                                            <span className={cn('inline-block h-2 w-2 rounded-full', active ? 'bg-indigo-600' : 'bg-slate-200')} />
                                                        );

                                                        return (
                                                            <tr key={mod.id} className="hover:bg-slate-50/70 transition">
                                                                <td className="px-4 py-2.5 font-semibold text-slate-900">
                                                                    {mod.name}
                                                                </td>
                                                                <td className="px-3 py-2.5">
                                                                    {accessBadge}
                                                                </td>
                                                                <td className="px-3 py-2.5 text-[11px] text-slate-600">
                                                                    {p?.scope?.type === 'all'
                                                                        ? 'Toàn công ty (COMPANY)'
                                                                        : p?.scope?.type === 'department_managed'
                                                                        ? 'Nhánh bộ phận (BRANCH)'
                                                                        : 'Cá nhân (SELF)'}
                                                                </td>
                                                                <td className="px-2 py-2.5 text-center">{renderDot(isView)}</td>
                                                                <td className="px-2 py-2.5 text-center">{renderDot(isCreate)}</td>
                                                                <td className="px-2 py-2.5 text-center">{renderDot(isEdit)}</td>
                                                                <td className="px-2 py-2.5 text-center">{renderDot(isDelete)}</td>
                                                                <td className="px-2 py-2.5 text-center">{renderDot(isApprove)}</td>
                                                            </tr>
                                                        );
                                                    })}
                                                </tbody>
                                            </table>
                                        </div>

                                        <div className="flex items-center justify-between text-[11px] text-slate-500 pt-1">
                                            <span>Để phân vai trò và phạm vi dữ liệu cụ thể cho từng nhân viên, vui lòng thao tác tại mục <strong>Quản lý tài khoản</strong>.</span>
                                            <div className="flex items-center gap-3">
                                                <span className="flex items-center gap-1 font-medium"><span className="h-2 w-2 rounded-full bg-indigo-600 inline-block"/> Có quyền</span>
                                                <span className="flex items-center gap-1 font-medium"><span className="h-2 w-2 rounded-full bg-slate-200 inline-block"/> Không có quyền</span>
                                            </div>
                                        </div>
                                    </div>
                                ) : (
                                    /* User List for this Role */
                                    <div className="space-y-3">
                                        <div className="overflow-x-auto rounded-xl border border-slate-200">
                                            <table className="w-full text-left text-xs border-collapse">
                                                <thead>
                                                    <tr className="bg-slate-50 border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                                                        <th className="px-4 py-2.5">Người dùng</th>
                                                        <th className="px-4 py-2.5">Tên đăng nhập</th>
                                                        <th className="px-4 py-2.5">Đơn vị / Phòng ban</th>
                                                        <th className="px-4 py-2.5">Phạm vi dữ liệu</th>
                                                        <th className="px-4 py-2.5 text-right">Trạng thái</th>
                                                    </tr>
                                                </thead>
                                                <tbody className="divide-y divide-slate-100">
                                                    {usersInSelectedRole.length === 0 ? (
                                                        <tr>
                                                            <td colSpan={5} className="text-center py-8 text-slate-400">
                                                                Hiện chưa có tài khoản nào được gán vai trò {selectedRole.name} ({selectedRole.id}).
                                                            </td>
                                                        </tr>
                                                    ) : (
                                                        usersInSelectedRole.map((u) => (
                                                            <tr key={u.id} className="hover:bg-slate-50/70 transition">
                                                                <td className="px-4 py-2.5">
                                                                    <div className="flex items-center gap-2.5">
                                                                        <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-[10px] font-bold text-indigo-700">
                                                                            {(u.fullName || u.username).substring(0, 2).toUpperCase()}
                                                                        </div>
                                                                        <div>
                                                                            <p className="font-semibold text-slate-900">{u.fullName || u.username}</p>
                                                                            <p className="text-[10px] text-slate-400">{u.employeeId ? `Mã NV: ${u.employeeId}` : 'Chưa gán hồ sơ'}</p>
                                                                        </div>
                                                                    </div>
                                                                </td>
                                                                <td className="px-4 py-2.5 font-mono text-[11px] text-slate-700">
                                                                    {u.username}
                                                                </td>
                                                                <td className="px-4 py-2.5 text-slate-600">
                                                                    {u.orgUnitName || 'Chưa gán phòng'}
                                                                </td>
                                                                <td className="px-4 py-2.5">
                                                                    <span className="inline-flex rounded-md bg-slate-100 px-2 py-0.5 font-semibold text-slate-700 text-[10px]">
                                                                        {u.dataScope === 'COMPANY'
                                                                            ? 'Toàn công ty'
                                                                            : u.dataScope === 'ORGANIZATION_BRANCH'
                                                                            ? 'Nhánh đơn vị'
                                                                            : 'Cá nhân'}
                                                                    </span>
                                                                </td>
                                                                <td className="px-4 py-2.5 text-right">
                                                                    <span
                                                                        className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                                                                            u.status === 'ACTIVE'
                                                                                ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                                                                                : 'bg-rose-50 text-rose-700 border border-rose-200'
                                                                        }`}
                                                                    >
                                                                        <span className={`h-1.5 w-1.5 rounded-full ${u.status === 'ACTIVE' ? 'bg-emerald-500' : 'bg-rose-500'}`} />
                                                                        {u.status === 'ACTIVE' ? 'Hoạt động' : 'Đã khóa'}
                                                                    </span>
                                                                </td>
                                                            </tr>
                                                        ))
                                                    )}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default AccessControlView;