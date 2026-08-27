import React from 'react';
import type { ModulePermission, DataScopeConfig, ActionKey } from './access.types';
import { DataScopeSelector } from './DataScopeSelector';
import { Check } from 'lucide-react';

interface Props {
    permissions: Record<string, ModulePermission>;
    onChange: (permissions: Record<string, ModulePermission>) => void;
    isReadOnly?: boolean;
}

const ACTIONS_HEADER: { key: ActionKey; label: string }[] = [
    { key: 'view', label: 'Xem' },
    { key: 'create', label: 'Thêm' },
    { key: 'edit', label: 'Sửa' },
    { key: 'delete', label: 'Xóa' },
    { key: 'approve', label: 'Duyệt' },
    { key: 'export', label: 'Xuất DL' },
];

export const PermissionMatrix: React.FC<Props> = ({ permissions, onChange, isReadOnly }) => {
    const handleActionToggle = (moduleId: string, actionKey: ActionKey) => {
        if (isReadOnly) return;
        const currentActions = { ...permissions[moduleId].actions };
        currentActions[actionKey] = !currentActions[actionKey];

        if (actionKey !== 'view' && currentActions[actionKey]) {
            currentActions.view = true;
        }

        onChange({
            ...permissions,
            [moduleId]: { ...permissions[moduleId], actions: currentActions },
        });
    };

    const handleScopeChange = (moduleId: string, newScope: DataScopeConfig) => {
        if (isReadOnly) return;
        onChange({
            ...permissions,
            [moduleId]: { ...permissions[moduleId], scope: newScope },
        });
    };

    return (
        <div className="w-full overflow-x-auto rounded-lg border border-white/10 bg-white/5">
            <table className="w-full text-left text-sm text-white/90 border-collapse">
                <thead>
                <tr className="border-b border-white/10 bg-white/10 text-xs font-semibold text-white/70 uppercase tracking-wider">
                    <th className="py-3 px-4 min-w-[180px]">Chức năng</th>
                    <th className="py-3 px-4 min-w-[180px]">Phạm vi dữ liệu</th>
                    {ACTIONS_HEADER.map((col) => (
                        <th key={col.key} className="py-3 px-2 text-center w-16">
                            {col.label}
                        </th>
                    ))}
                </tr>
                </thead>
                <tbody className="divide-y divide-white/10">
                {Object.values(permissions).map((module) => (
                    <tr key={module.moduleId} className="hover:bg-white/10 transition-colors">
                        <td className="py-3 px-4 font-medium text-white">{module.moduleName}</td>
                        <td className="py-3 px-4">
                            <DataScopeSelector
                                value={module.scope}
                                onChange={(newScope) => handleScopeChange(module.moduleId, newScope)}
                                disabled={isReadOnly}
                            />
                        </td>
                        {ACTIONS_HEADER.map((col) => {
                            const isChecked = module.actions[col.key];
                            return (
                                <td key={col.key} className="py-3 px-2 text-center">
                                    <button
                                        type="button"
                                        disabled={isReadOnly}
                                        onClick={() => handleActionToggle(module.moduleId, col.key)}
                                        className={`w-5 h-5 rounded border inline-flex items-center justify-center transition-all ${
                                            isChecked
                                                ? 'bg-indigo-600 border-indigo-500 text-white shadow-sm shadow-indigo-500/50'
                                                : 'bg-white/5 border-white/20 hover:border-white/40'
                                        } ${isReadOnly ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'}`}
                                    >
                                        {isChecked && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                                    </button>
                                </td>
                            );
                        })}
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
};