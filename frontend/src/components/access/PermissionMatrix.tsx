import React from 'react';
import type { ModulePermission, DataScopeConfig, ActionKey, Department } from './access.types';
import { DataScopeSelector } from './DataScopeSelector';
import { Check } from 'lucide-react';

/** Checkbox hành động dạng "custom-checkbox": input checkbox thật (giữ hành vi
 *  native, hỗ trợ bàn phím) với icon Check phủ lên khi checked — theo đúng
 *  cấu trúc và tông màu tím trong file mẫu HTML. */
const ActionCheckbox: React.FC<{
    checked: boolean;
    disabled?: boolean;
    onChange: () => void;
}> = ({ checked, disabled, onChange }) => (
    <label
        className={`relative inline-flex h-5 w-5 items-center justify-center ${
            disabled ? 'cursor-not-allowed' : 'cursor-pointer'
        }`}
    >
        <input
            type="checkbox"
            checked={checked}
            disabled={disabled}
            onChange={onChange}
            className={`peer h-5 w-5 appearance-none rounded-md border transition-all ${
                'border-white/30 bg-white/5 checked:border-purple-400 checked:bg-purple-600 hover:border-purple-400 hover:bg-purple-500/15'
            } ${disabled ? 'opacity-60' : ''}`}
        />
        <Check className="pointer-events-none absolute h-3.5 w-3.5 stroke-[3] text-white opacity-0 peer-checked:opacity-100" />
    </label>
);

interface Props {
    permissions: Record<string, ModulePermission>;
    departments: Department[];
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

export const PermissionMatrix: React.FC<Props> = ({ permissions, departments, onChange, isReadOnly }) => {
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
        <div className="w-full overflow-hidden rounded-lg border border-purple-500/20 bg-white/5">
            <div className="overflow-x-auto">
                <table className="w-full text-left text-sm text-white/90 border-collapse">
                    <thead>
                    <tr className="border-b border-white/10 bg-white/10 text-xs font-semibold uppercase tracking-wider text-purple-200/80">
                        <th className="py-3 px-4 min-w-[180px]">Chức năng</th>
                        <th className="py-3 px-4 min-w-[200px]">Phạm vi dữ liệu</th>
                        {ACTIONS_HEADER.map((col) => (
                            <th key={col.key} className="py-3 px-2 text-center w-16">
                                {col.label}
                            </th>
                        ))}
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-white/10">
                    {Object.values(permissions).map((module) => (
                        <tr key={module.moduleId} className="transition-colors hover:bg-white/5">
                            <td className="py-3 px-4 font-medium text-white">{module.moduleName}</td>
                            <td className="py-3 px-4">
                                <DataScopeSelector
                                    value={module.scope}
                                    departments={departments}
                                    onChange={(newScope) => handleScopeChange(module.moduleId, newScope)}
                                    disabled={isReadOnly}
                                />
                            </td>
                            {ACTIONS_HEADER.map((col) => {
                                const isChecked = module.actions[col.key];
                                return (
                                    <td key={col.key} className="py-3 px-2 text-center">
                                        <ActionCheckbox
                                            checked={isChecked}
                                            disabled={isReadOnly}
                                            onChange={() => handleActionToggle(module.moduleId, col.key)}
                                        />
                                    </td>
                                );
                            })}
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            <div className="flex flex-col items-center justify-between gap-2 border-t border-white/10 px-4 py-3 text-xs text-purple-300/200 sm:flex-row">
                <span>
                    Bấm vào ô <strong className="text-[#efc500]">Phạm vi dữ liệu</strong> để chọn đơn vị cụ thể.
                </span>
                <div className="flex items-center gap-3">
                    <span className="inline-flex items-center gap-1">
                        <span className="inline-block h-2.5 w-2.5 rounded-full bg-purple-500" /> Có quyền
                    </span>
                    <span className="inline-flex items-center gap-1">
                        <span className="inline-block h-2.5 w-2.5 rounded-full bg-white/20" /> Không có quyền
                    </span>
                </div>
            </div>
        </div>
    );
};