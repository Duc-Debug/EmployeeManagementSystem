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
                className={`inline-flex max-w-[220px] items-center justify-between gap-2 truncate rounded-lg border px-3 py-1.5 text-xs font-semibold transition-colors shadow-2xs ${
                    disabled
                        ? 'cursor-not-allowed border-slate-200 bg-slate-100 text-slate-400'
                        : isUnresolvedCustomTree
                            ? 'border-amber-300 bg-amber-50 text-amber-900 hover:bg-amber-100 hover:border-amber-400'
                            : 'border-indigo-200 bg-indigo-50/90 text-indigo-700 hover:border-indigo-300 hover:bg-indigo-100'
                }`}
            >
                <span className="flex items-center gap-1.5 truncate">
                    {SCOPE_ICONS[currentOption.value]}
                    <span className="truncate">{currentLabel}</span>
                </span>
                {!disabled && <ChevronDown className="w-3.5 h-3.5 shrink-0 text-indigo-500 opacity-80" />}
            </button>

            {isOpen && !disabled && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-sm animate-in fade-in duration-150">
                    <div className="relative flex max-h-[85vh] w-full max-w-md flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl text-slate-800 animate-in zoom-in-95 duration-150">
                        <button
                            type="button"
                            onClick={() => setIsOpen(false)}
                            className="absolute right-4 top-4 rounded-xl p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
                        >
                            <X className="w-4 h-4" />
                        </button>

                        {!showTreeStep ? (
                            <>
                                <div className="mb-4 flex items-center gap-3">
                                    <div className="flex h-10 w-10 items-center justify-center rounded-xl border border-indigo-100 bg-indigo-50 text-indigo-600">
                                        <Shield className="w-5 h-5" />
                                    </div>
                                    <div>
                                        <h3 className="text-base font-bold text-slate-900">Chọn phạm vi dữ liệu</h3>
                                        <p className="text-xs text-slate-500">Áp dụng cho chức năng đang cấu hình</p>
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
                                                        ? 'border-indigo-500 bg-indigo-50/80 text-indigo-900 shadow-xs'
                                                        : 'border-slate-200 bg-slate-50/60 text-slate-700 hover:border-slate-300 hover:bg-slate-100/80'
                                                }`}
                                            >
                                                <span className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border ${
                                                    isSelected
                                                        ? 'border-indigo-200 bg-indigo-100 text-indigo-700'
                                                        : 'border-slate-200 bg-white text-slate-500'
                                                }`}>
                                                    {SCOPE_ICONS[option.value]}
                                                </span>
                                                <span>{option.label}</span>
                                            </button>
                                        );
                                    })}
                                </div>

                                <div className="mt-5 flex items-center justify-end border-t border-slate-100 pt-4">
                                    <button
                                        type="button"
                                        onClick={() => setIsOpen(false)}
                                        className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
                                    >
                                        Đóng
                                    </button>
                                </div>
                            </>
                        ) : (
                            <>
                                <div className="mb-4 flex items-center gap-3">
                                    <div className="flex h-10 w-10 items-center justify-center rounded-xl border border-indigo-100 bg-indigo-50 text-indigo-600">
                                        <TreePine className="w-5 h-5" />
                                    </div>
                                    <div>
                                        <h3 className="text-base font-bold text-slate-900">Chọn đơn vị áp dụng</h3>
                                        <p className="text-xs text-slate-500">Cấu hình danh sách đơn vị cho chức năng này</p>
                                    </div>
                                </div>

                                <div className="mb-3 flex items-center justify-between rounded-xl border border-slate-200 bg-slate-50 p-3">
                                    <span className="text-xs font-semibold text-slate-700">Chọn tất cả đơn vị</span>
                                    <button
                                        type="button"
                                        onClick={toggleAllDraftIds}
                                        className="text-xs font-bold text-indigo-600 hover:text-indigo-700 hover:underline"
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
                                                className="flex cursor-pointer select-none items-center justify-between rounded-xl border border-slate-200 bg-slate-50/50 p-3 text-left text-xs font-semibold text-slate-800 transition hover:border-indigo-300 hover:bg-indigo-50/40"
                                            >
                                                <span className="flex items-center gap-3">
                                                    <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-500">
                                                        <Building2 className="w-3.5 h-3.5" />
                                                    </span>
                                                    {dept.name}
                                                </span>
                                                <span
                                                    className={`relative inline-flex h-5 w-5 shrink-0 items-center justify-center rounded border transition-all ${
                                                        isChecked ? 'border-indigo-600 bg-indigo-600 text-white' : 'border-slate-300 bg-white'
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
                                        <p className="py-6 text-center text-xs text-slate-400 font-medium">Chưa có đơn vị nào được thiết lập.</p>
                                    )}
                                </div>

                                <div className="mt-5 flex items-center justify-end gap-3 border-t border-slate-100 pt-4">
                                    <button
                                        type="button"
                                        onClick={() => setShowTreeStep(false)}
                                        className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
                                    >
                                        Quay lại
                                    </button>
                                    <button
                                        type="button"
                                        onClick={confirmTreeSelection}
                                        className="rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs"
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