import React, { useState } from 'react';
import RoleList from './RoleList';
import RoleModal from './RoleModal';
import { PermissionMatrix } from './PermissionMatrix';
import DepartmentManagermodal from './DepartmentManagermodal'; // Sửa import khớp tên file thực tế
import Toast from './Toast';

import type { Role, ModulePermission, RoleBasicInfo, Department } from './access.types';

import { ROLES, MODULES, DEPARTMENTS } from './access.constants';
import { ShieldCheck, Save, RotateCcw, AlertCircle, Pencil, Trash2, Building2 } from 'lucide-react';

const buildInitialPermissions = (): Record<string, ModulePermission> => {
    const result: Record<string, ModulePermission> = {};
    MODULES.forEach((mod) => {
        result[mod.id] = {
            moduleId: mod.id,
            moduleName: mod.name,
            scope: { type: mod.defaultScope },
            actions: { ...mod.defaultActions },
        };
    });
    return result;
};

export const AccessControlView: React.FC = () => {
    const [roles, setRoles] = useState<Role[]>(() =>
        ROLES.map((role) => ({ ...role, permissions: buildInitialPermissions() }))
    );
    const [departments, setDepartments] = useState<Department[]>(DEPARTMENTS);
    const [selectedRoleId, setSelectedRoleId] = useState<string>(ROLES[0]?.id ?? '');

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingRole, setEditingRole] = useState<RoleBasicInfo | null>(null);
    // Đổi mỗi lần mở RoleModal để component remount với state khởi tạo mới,
    // thay cho việc RoleModal tự dùng useEffect đồng bộ lại initialData.
    const [modalKey, setModalKey] = useState(0);
    const [isDeptModalOpen, setIsDeptModalOpen] = useState(false);

    // Toast notification helper
    const [toast, setToast] = useState<{ message: string; visible: boolean }>({ message: '', visible: false });
    const showToast = (message: string) => {
        setToast({ message, visible: true });
        window.setTimeout(() => setToast((prev) => ({ ...prev, visible: false })), 3000);
    };

    const selectedRole = roles.find((r) => r.id === selectedRoleId);

    const [draftPermissions, setDraftPermissions] = useState<Record<string, ModulePermission> | null>(
        selectedRole?.permissions ?? null
    );
    const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);

    const [lastSyncedRoleId, setLastSyncedRoleId] = useState(selectedRoleId);
    if (selectedRoleId !== lastSyncedRoleId) {
        setLastSyncedRoleId(selectedRoleId);
        setDraftPermissions(selectedRole?.permissions ?? null);
        setHasUnsavedChanges(false);
    }

    const handlePermissionsChange = (updated: Record<string, ModulePermission>) => {
        setDraftPermissions(updated);
        setHasUnsavedChanges(true);
    };

    const handleSaveChanges = () => {
        if (!draftPermissions) return;
        setRoles((prev) =>
            prev.map((r) => (r.id === selectedRoleId ? { ...r, permissions: draftPermissions } : r))
        );
        setHasUnsavedChanges(false);
        showToast('Đã lưu thay đổi phân quyền cho vai trò!');
    };

    const handleDiscardChanges = () => {
        setDraftPermissions(selectedRole?.permissions ?? null);
        setHasUnsavedChanges(false);
    };

    const openEditModalFor = (role: RoleBasicInfo | Role) => {
        setEditingRole({
            id: role.id,
            name: role.name,
            description: role.description,
            theme: role.theme,
            departmentId: role.departmentId,
            isSystemRole: role.isSystemRole,
        });
        setIsModalOpen(true);
        setModalKey((k) => k + 1);
    };

    const handleSaveRole = (data: RoleBasicInfo) => {
        if (data.id) {
            setRoles((prev) =>
                prev.map((r) =>
                    r.id === data.id
                        ? {
                            ...r,
                            name: data.name,
                            description: data.description,
                            theme: data.theme,
                            departmentId: data.departmentId,
                            isSystemRole: Boolean(data.isSystemRole),
                        }
                        : r
                )
            );
            showToast('Đã cập nhật thông tin vai trò!');
        } else {
            const newRole: Role = {
                id: `role_${Date.now()}`,
                name: data.name,
                description: data.description,
                theme: data.theme,
                departmentId: data.departmentId,
                userCount: 0,
                isSystemRole: Boolean(data.isSystemRole),
                permissions: buildInitialPermissions(),
            };
            setRoles((prev) => [...prev, newRole]);
            setSelectedRoleId(newRole.id);
            showToast('Đã khởi tạo vai trò mới!');
        }
        setIsModalOpen(false);
        setEditingRole(null);
    };

    const handleDeleteRole = (id: string) => {
        setRoles((prev) => {
            const remaining = prev.filter((r) => r.id !== id);
            if (selectedRoleId === id) {
                setSelectedRoleId(remaining[0]?.id ?? '');
            }
            return remaining;
        });
        showToast('Đã xóa vai trò!');
    };

    const handleAddDepartment = (name: string) => {
        const newDept: Department = { id: `dept_${Date.now()}`, name };
        setDepartments((prev) => [...prev, newDept]);
        showToast(`Đã thêm phòng ban "${name}"!`);
    };

    const handleRemoveDepartment = (id: string) => {
        if (departments.length <= 1) {
            showToast('Hệ thống cần ít nhất 1 phòng ban!');
            return;
        }
        setDepartments((prev) => prev.filter((d) => d.id !== id));
        showToast('Đã xóa phòng ban!');
    };

    const selectedRoleDept =
        selectedRole && selectedRole.departmentId !== 'all'
            ? departments.find((d) => d.id === selectedRole.departmentId)
            : null;

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4 border-b border-white/15 pb-4">
                <div className="flex items-center gap-3">
                    <div className="rounded-xl border border-purple-400/30 bg-purple-600/30 p-2.5 text-purple-300">
                        <ShieldCheck className="h-6 w-6" />
                    </div>
                    <div>
                        <h1 className="text-2xl font-bold tracking-tight text-white md:text-3xl">
                            Quản lý Vai trò &amp; Phân quyền
                        </h1>
                        <p className="mt-0.5 text-sm text-purple-200/70">
                            Cấu hình vai trò người dùng, phạm vi dữ liệu và quyền thao tác theo phòng ban.
                        </p>
                    </div>
                </div>

                <div className="flex items-center gap-3">
                    <button
                        type="button"
                        onClick={() => setIsDeptModalOpen(true)}
                        className="flex items-center gap-2 rounded-xl border border-white/15 bg-white/[0.07] px-4 py-2.5 text-sm font-medium text-white/80 backdrop-blur-xl transition hover:text-purple-200"
                    >
                        <Building2 className="h-4 w-4 text-purple-400" />
                        <span>Danh sách phòng ban</span>
                        <span className="ml-1 rounded-full border border-purple-400/30 bg-purple-500/30 px-2 py-0.5 text-xs text-purple-200">
                            {departments.length}
                        </span>
                    </button>
                    <button
                        type="button"
                        onClick={handleSaveChanges}
                        disabled={!hasUnsavedChanges}
                        className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-purple-900/40 transition hover:from-purple-500 hover:to-indigo-500 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                        <Save className="h-4 w-4" />
                        <span>Lưu thay đổi</span>
                    </button>
                </div>
            </div>

            <div className="grid grid-cols-12 gap-6">
                <div className="col-span-12 flex flex-col lg:col-span-4">
                    <RoleList
                        roles={roles}
                        departments={departments}
                        selectedId={selectedRoleId}
                        onSelect={setSelectedRoleId}
                        onAdd={() => {
                            setEditingRole(null);
                            setIsModalOpen(true);
                            setModalKey((k) => k + 1);
                        }}
                        onEdit={openEditModalFor}
                        onDelete={handleDeleteRole}
                    />
                </div>

                <div className="col-span-12 rounded-2xl border border-purple-500/20 bg-white/[0.05] p-5 backdrop-blur-xl shadow-xl md:p-6 lg:col-span-8">
                    {selectedRole && draftPermissions ? (
                        <div className="space-y-4">
                            <div className="flex flex-col gap-3 border-b border-white/15 pb-4 sm:flex-row sm:items-center sm:justify-between">
                                <div>
                                    <div className="flex items-center gap-2">
                                        <h2 className="text-lg font-bold text-white sm:text-xl">{selectedRole.name}</h2>
                                        {selectedRole.isSystemRole && (
                                            <span className="rounded-full border border-amber-400/40 bg-amber-500/20 px-2.5 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-amber-300">
                                                Vai trò hệ thống
                                            </span>
                                        )}
                                    </div>
                                    <p className="mt-1 text-xs text-purple-200/70 md:text-sm">{selectedRole.description}</p>
                                </div>

                                <div className="flex items-center gap-2">
                                    <button
                                        type="button"
                                        onClick={() => openEditModalFor(selectedRole)}
                                        className="flex items-center gap-1.5 rounded-xl border border-white/15 bg-white/[0.07] px-3 py-1.5 text-xs font-medium text-white/70 backdrop-blur-xl transition hover:bg-white/15 hover:text-white"
                                    >
                                        <Pencil className="w-3.5 h-3.5" />
                                        <span>Sửa thông tin</span>
                                    </button>
                                    {!selectedRole.isSystemRole && (
                                        <button
                                            type="button"
                                            onClick={() => handleDeleteRole(selectedRole.id)}
                                            className="flex items-center gap-1.5 rounded-xl border border-white/15 bg-white/[0.07] px-3 py-1.5 text-xs font-medium text-white/70 backdrop-blur-xl transition hover:border-rose-400/40 hover:bg-rose-500/15 hover:text-rose-300"
                                        >
                                            <Trash2 className="w-3.5 h-3.5" />
                                            <span>Xóa</span>
                                        </button>
                                    )}
                                </div>
                            </div>

                            <div className="flex flex-col gap-3 rounded-xl border border-purple-400/20 bg-purple-900/40 p-3.5 sm:flex-row sm:items-center sm:justify-between">
                                <div className="flex items-center gap-3">
                                    <div className="flex h-9 w-9 items-center justify-center rounded-lg border border-purple-400/30 bg-purple-500/20 text-purple-300">
                                        <Building2 className="h-4 w-4" />
                                    </div>
                                    <div>
                                        <div className="text-xs font-semibold text-purple-200">Phòng ban liên kết vai trò</div>
                                        <div className="mt-0.5 text-xs text-purple-300/80">
                                            {selectedRoleDept
                                                ? `Áp dụng riêng cho: ${selectedRoleDept.name}`
                                                : 'Áp dụng cho: Tất cả phòng ban trong công ty'}
                                        </div>
                                    </div>
                                </div>
                                <button
                                    type="button"
                                    onClick={() => openEditModalFor(selectedRole)}
                                    className="flex items-center gap-1.5 self-start whitespace-nowrap rounded-lg border border-purple-400/40 bg-purple-600/40 px-3 py-1.5 text-xs font-medium text-purple-100 transition hover:bg-purple-600/60 sm:self-auto"
                                >
                                    <Building2 className="h-3.5 w-3.5" />
                                    <span>Đổi phòng ban</span>
                                </button>
                            </div>

                            <PermissionMatrix
                                permissions={draftPermissions}
                                departments={departments}
                                isReadOnly={selectedRole.isSystemRole}
                                onChange={handlePermissionsChange}
                            />

                            {hasUnsavedChanges && (
                                <div className="flex items-center justify-between rounded-lg border border-indigo-300/30 bg-indigo-500/15 p-3 backdrop-blur-md">
                                    <div className="flex items-center gap-2 text-xs text-indigo-100">
                                        <AlertCircle className="w-4 h-4 text-indigo-300" />
                                        <span>Bạn có thay đổi chưa được lưu cho vai trò này.</span>
                                    </div>
                                    <div className="flex gap-2">
                                        <button
                                            type="button"
                                            onClick={handleDiscardChanges}
                                            className="flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-medium text-white/70 hover:bg-white/10"
                                        >
                                            <RotateCcw className="w-3.5 h-3.5" /> Hủy
                                        </button>
                                        <button
                                            type="button"
                                            onClick={handleSaveChanges}
                                            className="flex items-center gap-1.5 rounded-md bg-indigo-600 px-4 py-1.5 text-xs font-medium text-white shadow-md shadow-indigo-600/30 hover:bg-indigo-500"
                                        >
                                            <Save className="w-3.5 h-3.5" /> Lưu thay đổi
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="flex items-center justify-center py-16 text-sm text-white/50">
                            Vui lòng chọn một vai trò để cấu hình phân quyền.
                        </div>
                    )}
                </div>
            </div>

            <RoleModal
                key={modalKey}
                open={isModalOpen}
                initialData={editingRole}
                departments={departments}
                onClose={() => {
                    setIsModalOpen(false);
                    setEditingRole(null);
                }}
                onSave={handleSaveRole}
            />

            <DepartmentManagermodal
                open={isDeptModalOpen}
                departments={departments}
                onClose={() => setIsDeptModalOpen(false)}
                onAdd={handleAddDepartment}
                onRemove={handleRemoveDepartment}
            />

            <Toast message={toast.message} visible={toast.visible} />
        </div>
    );
};

export default AccessControlView;