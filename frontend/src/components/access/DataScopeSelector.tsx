import React, { useState } from 'react';
import type { DataScopeConfig, DataScope, Department } from './access.types';
import { SCOPE_OPTIONS } from './access.constants';
import { Building2, Shield, Users, User, TreePine, ChevronDown, X, Check } from 'lucide-react';

interface Props {
    value: DataScopeConfig;
    departments: Department[];
    onChange: (newScope: DataScopeConfig) => void;
    disabled?: boolean;
}

const SCOPE_ICONS: Record<DataScope, React.ReactNode> = {
    all: <Shield className="w-3.5 h-3.5" />,
    department_managed: <Building2 className="w-3.5 h-3.5" />,
    department_own: <Users className="w-3.5 h-3.5" />,
    personal: <User className="w-3.5 h-3.5" />,
    custom_tree: <TreePine className="w-3.5 h-3.5" />,
};

export const DataScopeSelector: React.FC<Props> = ({ value, departments, onChange, disabled }) => {
    const [isOpen, setIsOpen] = useState(false);
    const [draftIds, setDraftIds] = useState<string[]>(value.selectedNodeIds ?? []);
    const [showTreeStep, setShowTreeStep] = useState(false);

    const currentOption = SCOPE_OPTIONS.find((opt) => opt.value === value.type) || SCOPE_OPTIONS[0];

    const currentLabel = (() => {
        if (value.type !== 'custom_tree') return currentOption.label;
        const count = value.selectedNodeIds?.length ?? 0;
        if (count === 0) return 'Chưa chọn đơn vị';
        const names = (value.selectedNodeIds ?? [])
            .map((id) => departments.find((d) => d.id === id)?.name ?? id)
            .join(', ');
        return `${count} đơn vị (${names})`;
    })();

    const isUnresolvedCustomTree = value.type === 'custom_tree' && (value.selectedNodeIds?.length ?? 0) === 0;

    const openModal = () => {
        setDraftIds(value.selectedNodeIds ?? []);
        setShowTreeStep(value.type === 'custom_tree');
        setIsOpen(true);
    };

    const handlePick = (option: (typeof SCOPE_OPTIONS)[number]) => {
        if (option.value === 'custom_tree') {
            setDraftIds(value.selectedNodeIds ?? []);
            setShowTreeStep(true);
            return;
        }
        onChange({ type: option.value });
        setIsOpen(false);
    };

    const toggleDraftId = (id: string) => {
        setDraftIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
    };

    const toggleAllDraftIds = () => {
        setDraftIds((prev) => (prev.length === departments.length ? [] : departments.map((d) => d.id)));
    };

    const confirmTreeSelection = () => {
        onChange({ type: 'custom_tree', selectedNodeIds: draftIds });
        setIsOpen(false);
    };

    return (
        <>
            <button
                type="button"
                disabled={disabled}
                onClick={openModal}
                className={`inline-flex max-w-[220px] items-center justify-between gap-2 truncate rounded-lg border px-3 py-1.5 text-xs font-medium transition-colors ${
                    disabled
                        ? 'cursor-not-allowed border-white/10 bg-white/5 text-white/40'
                        : isUnresolvedCustomTree
                            ? 'border-amber-400/40 bg-amber-500/15 text-amber-200 hover:bg-amber-500/25'
                            : 'border-indigo-300/40 bg-indigo-500/20 text-indigo-100 hover:border-indigo-200 hover:bg-indigo-500/30'
                }`}
            >
                <span className="flex items-center gap-1.5 truncate">
                    {SCOPE_ICONS[currentOption.value]}
                    <span className="truncate">{currentLabel}</span>
                </span>
                {!disabled && <ChevronDown className="w-3 h-3 shrink-0 opacity-70" />}
            </button>

            {isOpen && !disabled && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm">
                    <div className="relative flex max-h-[85vh] w-full max-w-md flex-col overflow-hidden rounded-2xl border border-purple-500/30 bg-[#1c1338]/95 p-6 shadow-2xl backdrop-blur-xl">
                        <button
                            type="button"
                            onClick={() => setIsOpen(false)}
                            className="absolute right-4 top-4 p-2 text-purple-200/70 hover:text-white"
                        >
                            <X className="w-4 h-4" />
                        </button>

                        {!showTreeStep ? (
                            <>
                                <div className="mb-4 flex items-center gap-3">
                                    <div className="flex h-10 w-10 items-center justify-center rounded-xl border border-purple-400/30 bg-purple-600/30 text-purple-300">
                                        <Shield className="w-4.5 h-4.5" />
                                    </div>
                                    <div>
                                        <h3 className="text-base font-bold text-white">Chọn phạm vi dữ liệu</h3>
                                        <p className="text-xs text-purple-200/70">Áp dụng cho chức năng đang cấu hình</p>
                                    </div>
                                </div>

                                <div className="flex-1 space-y-2 overflow-y-auto pr-1">
                                    {SCOPE_OPTIONS.map((option) => {
                                        const isSelected = value.type === option.value;
                                        return (
                                            <button
                                                key={option.value}
                                                type="button"
                                                onClick={() => handlePick(option)}
                                                className={`flex w-full items-center gap-3 rounded-xl border p-3 text-left text-xs font-semibold transition ${
                                                    isSelected
                                                        ? 'border-purple-400/50 bg-purple-600/30 text-white'
                                                        : 'border-white/10 bg-white/[0.03] text-white/80 hover:border-purple-400/30 hover:bg-white/[0.07]'
                                                }`}
                                            >
                                                <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-purple-400/30 bg-purple-500/20 text-purple-300">
                                                    {SCOPE_ICONS[option.value]}
                                                </span>
                                                {option.label}
                                            </button>
                                        );
                                    })}
                                </div>

                                <div className="mt-5 flex items-center justify-end border-t border-white/10 pt-4">
                                    <button
                                        type="button"
                                        onClick={() => setIsOpen(false)}
                                        className="rounded-xl border border-white/15 bg-white/[0.07] px-4 py-2 text-xs font-medium text-white/70 hover:text-white"
                                    >
                                        Đóng
                                    </button>
                                </div>
                            </>
                        ) : (
                            <>
                                <div className="mb-4 flex items-center gap-3">
                                    <div className="flex h-10 w-10 items-center justify-center rounded-xl border border-purple-400/30 bg-purple-600/30 text-purple-300">
                                        <TreePine className="w-4.5 h-4.5" />
                                    </div>
                                    <div>
                                        <h3 className="text-base font-bold text-white">Chọn đơn vị áp dụng</h3>
                                        <p className="text-xs text-purple-200/70">Cấu hình danh sách đơn vị cho chức năng này</p>
                                    </div>
                                </div>

                                <div className="mb-3 flex items-center justify-between rounded-xl border border-white/10 bg-white/[0.03] p-3">
                                    <span className="text-xs font-medium text-white/90">Chọn tất cả đơn vị</span>
                                    <button
                                        type="button"
                                        onClick={toggleAllDraftIds}
                                        className="text-xs font-semibold text-purple-300 underline hover:text-purple-200"
                                    >
                                        {draftIds.length === departments.length && departments.length > 0
                                            ? 'Bỏ chọn tất cả'
                                            : 'Chọn tất cả'}
                                    </button>
                                </div>

                                <div className="my-1 max-h-[300px] flex-1 space-y-2 overflow-y-auto pr-1">
                                    {departments.map((dept) => {
                                        const isChecked = draftIds.includes(dept.id);
                                        return (
                                            <label
                                                key={dept.id}
                                                className="flex cursor-pointer select-none items-center justify-between rounded-xl border border-white/10 bg-white/[0.03] p-3 text-left text-xs font-semibold text-white/90 transition hover:border-purple-400/30 hover:bg-white/[0.07]"
                                            >
                                                <span className="flex items-center gap-3">
                                                    <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-purple-400/30 bg-purple-500/20 text-purple-300">
                                                        <Building2 className="w-3.5 h-3.5" />
                                                    </span>
                                                    {dept.name}
                                                </span>
                                                <span
                                                    className={`relative inline-flex h-5 w-5 shrink-0 items-center justify-center rounded border transition-all ${
                                                        isChecked ? 'border-purple-400 bg-purple-600' : 'border-white/30 bg-white/10'
                                                    }`}
                                                >
                                                    <input
                                                        type="checkbox"
                                                        checked={isChecked}
                                                        onChange={() => toggleDraftId(dept.id)}
                                                        className="absolute inset-0 h-full w-full cursor-pointer opacity-0"
                                                    />
                                                    {isChecked && <Check className="h-3.5 w-3.5 stroke-[3] text-white" />}
                                                </span>
                                            </label>
                                        );
                                    })}
                                    {departments.length === 0 && (
                                        <p className="py-6 text-center text-xs text-white/50">Chưa có đơn vị nào được thiết lập.</p>
                                    )}
                                </div>

                                <div className="mt-5 flex items-center justify-end gap-3 border-t border-white/10 pt-4">
                                    <button
                                        type="button"
                                        onClick={() => setShowTreeStep(false)}
                                        className="rounded-xl border border-white/15 bg-white/[0.07] px-4 py-2 text-xs font-medium text-white/70 hover:text-white"
                                    >
                                        Quay lại
                                    </button>
                                    <button
                                        type="button"
                                        onClick={confirmTreeSelection}
                                        className="rounded-xl bg-purple-600 px-4 py-2 text-xs font-semibold text-white hover:bg-purple-500"
                                    >
                                        Xác nhận
                                    </button>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            )}
        </>
    );
};