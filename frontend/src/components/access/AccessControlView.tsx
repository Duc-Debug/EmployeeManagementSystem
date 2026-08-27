import React, { useState } from 'react';
import RoleList from './RoleList';
import RoleModal from './RoleModal';
import { PermissionMatrix } from './PermissionMatrix';

import type { Role, ModulePermission, RoleBasicInfo } from './access.types';

import { ROLES, MODULES } from './access.constants';
import { ShieldCheck, Save, RotateCcw, AlertCircle } from 'lucide-react';

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
    const [selectedRoleId, setSelectedRoleId] = useState<string>(ROLES[0]?.id ?? '');

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingRole, setEditingRole] = useState<RoleBasicInfo | null>(null);

    const selectedRole = roles.find((r) => r.id === selectedRoleId);

    // The matrix edits a local draft rather than `roles` directly, so
    // "Lưu thay đổi" / "Hủy" have something real to commit or discard.
    const [draftPermissions, setDraftPermissions] = useState<Record<string, ModulePermission> | null>(
        selectedRole?.permissions ?? null
    );
    const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);

    // Reset the draft when the *selected role* changes — not every time
    // `roles` changes (e.g. right after a save). Doing this during render
    // (React's "adjusting state when a prop changes" pattern) instead of
    // in a useEffect avoids an extra commit/render pass: React applies
    // this update before painting, so there's no flash of stale draft
    // data and no cascading re-render.
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
    };

    const handleDiscardChanges = () => {
        setDraftPermissions(selectedRole?.permissions ?? null);
        setHasUnsavedChanges(false);
    };

    const handleSaveRole = (data: RoleBasicInfo) => {
        if (data.id) {
            setRoles((prev) =>
                prev.map((r) => (r.id === data.id ? { ...r, name: data.name, description: data.description, theme: data.theme } : r))
            );
        } else {
            const newRole: Role = {
                id: `role_${Date.now()}`,
                name: data.name,
                description: data.description,
                theme: data.theme,
                userCount: 0,
                isSystemRole: false,
                permissions: buildInitialPermissions(),
            };
            setRoles((prev) => [...prev, newRole]);
            setSelectedRoleId(newRole.id);
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
    };

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4 border-b border-white/15 pb-4">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
                        <ShieldCheck className="w-6 h-6 text-white" />
                        Phân quyền Vai trò & Phạm vi Dữ liệu
                    </h1>
                    <p className="text-sm text-white/60 mt-1">
                        Thiết lập vai trò, phạm vi dữ liệu chi tiết và quyền thao tác cho từng chức năng trong hệ thống.
                    </p>
                </div>

                <div className="flex items-center gap-6 text-xs text-white/80 bg-white/[0.07] backdrop-blur-xl px-4 py-2 rounded-xl border border-white/15">
                    <div>Tổng số vai trò: <span className="font-semibold text-white">{roles.length}</span></div>
                    <div className="h-4 w-px bg-white/20" />
                    <div>Người dùng đang áp dụng: <span className="font-semibold text-white">89</span></div>
                </div>
            </div>

            <div className="grid grid-cols-12 gap-6">
                <div className="col-span-12 lg:col-span-3 flex flex-col">
                    <RoleList
                        roles={roles}
                        selectedId={selectedRoleId}
                        onSelect={setSelectedRoleId}
                        onAdd={() => {
                            setEditingRole(null);
                            setIsModalOpen(true);
                        }}
                        onEdit={(role: Role) => {
                            setEditingRole({
                                id: role.id,
                                name: role.name,
                                description: role.description,
                                theme: role.theme,
                            });
                            setIsModalOpen(true);
                        }}
                        onDelete={handleDeleteRole}
                    />
                </div>

                <div className="col-span-12 lg:col-span-9 rounded-2xl border border-white/15 bg-white/[0.05] backdrop-blur-xl p-5">
                    {selectedRole && draftPermissions ? (
                        <div className="space-y-4">
                            <div className="flex justify-between items-center pb-2 border-b border-white/15">
                                <div>
                                    <h2 className="text-lg font-semibold text-white flex items-center gap-2">
                                        {selectedRole.name}
                                        {selectedRole.isSystemRole && (
                                            <span className="text-[10px] uppercase px-2 py-0.5 rounded bg-amber-500/15 text-amber-300 border border-amber-400/30">
                                                Vai trò hệ thống
                                            </span>
                                        )}
                                    </h2>
                                    <p className="text-xs text-white/60">{selectedRole.description}</p>
                                </div>
                            </div>

                            <PermissionMatrix
                                permissions={draftPermissions}
                                isReadOnly={selectedRole.isSystemRole}
                                onChange={handlePermissionsChange}
                            />

                            {hasUnsavedChanges && (
                                <div className="flex items-center justify-between bg-indigo-500/15 backdrop-blur-md border border-indigo-300/30 p-3 rounded-lg">
                                    <div className="flex items-center gap-2 text-xs text-indigo-100">
                                        <AlertCircle className="w-4 h-4 text-indigo-300" />
                                        <span>Bạn có thay đổi chưa được lưu cho vai trò này.</span>
                                    </div>
                                    <div className="flex gap-2">
                                        <button
                                            type="button"
                                            onClick={handleDiscardChanges}
                                            className="flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium text-white/70 hover:bg-white/10"
                                        >
                                            <RotateCcw className="w-3.5 h-3.5" /> Hủy
                                        </button>
                                        <button
                                            type="button"
                                            onClick={handleSaveChanges}
                                            className="flex items-center gap-1.5 px-4 py-1.5 rounded-md text-xs font-medium bg-indigo-600 hover:bg-indigo-500 text-white shadow-md shadow-indigo-600/30"
                                        >
                                            <Save className="w-3.5 h-3.5" /> Lưu thay đổi
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="flex items-center justify-center py-16 text-white/50 text-sm">
                            Vui lòng chọn một vai trò để cấu hình phân quyền.
                        </div>
                    )}
                </div>
            </div>

            <RoleModal
                open={isModalOpen}
                initialData={editingRole}
                onClose={() => {
                    setIsModalOpen(false);
                    setEditingRole(null);
                }}
                onSave={handleSaveRole}
            />
        </div>
    );
};

export default AccessControlView;