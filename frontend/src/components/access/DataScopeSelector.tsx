import React, { useState } from 'react';
import type { DataScopeConfig, DataScope } from './access.types';
import { SCOPE_OPTIONS } from './access.constants';
import { Building2, Shield, Users, User, TreePine, ChevronDown } from 'lucide-react';

interface Props {
    value: DataScopeConfig;
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

export const DataScopeSelector: React.FC<Props> = ({ value, onChange, disabled }) => {
    const [isOpen, setIsOpen] = useState(false);

    const currentOption = SCOPE_OPTIONS.find((opt) => opt.value === value.type) || SCOPE_OPTIONS[0];

    return (
        <div className="relative inline-block text-left">
            <button
                type="button"
                disabled={disabled}
                onClick={() => setIsOpen((open) => !open)}
                className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium border transition-colors ${
                    disabled
                        ? 'opacity-50 cursor-not-allowed bg-white/5 border-white/10 text-white/40'
                        : 'bg-indigo-500/20 border-indigo-300/40 text-indigo-100 hover:border-indigo-200 hover:bg-indigo-500/30'
                }`}
            >
                {SCOPE_ICONS[currentOption.value]}
                <span>{currentOption.label}</span>
                {!disabled && <ChevronDown className="w-3 h-3 ml-1 opacity-70" />}
            </button>

            {isOpen && !disabled && (
                <div className="absolute right-0 z-50 mt-1 w-48 rounded-lg bg-[#2a1d5c]/95 backdrop-blur-xl border border-white/15 shadow-xl py-1 text-xs">
                    {SCOPE_OPTIONS.map((option) => (
                        <button
                            key={option.value}
                            type="button"
                            onClick={() => {
                                onChange({ ...value, type: option.value });
                                setIsOpen(false);
                            }}
                            className={`w-full flex items-center gap-2 px-3 py-2 text-left hover:bg-white/10 transition-colors ${
                                value.type === option.value ? 'text-indigo-200 font-semibold bg-white/10' : 'text-white/70'
                            }`}
                        >
                            {SCOPE_ICONS[option.value]}
                            {option.label}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
};