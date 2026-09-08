import { useState, useRef, useEffect } from 'react';
import { ChevronDown, Search, Plus, Check } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { CatalogSkill } from './Types.ts';

interface SkillSelectProps {
    catalog: CatalogSkill[];
    value: string;
    disabled?: boolean;
    onChange: (id: string) => void;
    onAddNewSkill?: (name: string) => void;
}

export function SkillSelect({
                                catalog,
                                value,
                                disabled = false,
                                onChange,
                                onAddNewSkill,
                            }: SkillSelectProps) {
    const [open, setOpen] = useState(false);
    const [search, setSearch] = useState('');
    const containerRef = useRef<HTMLDivElement>(null);

    const selectedSkill = catalog.find((s) => String(s.id) === value);

    const filteredCatalog = catalog.filter((s) =>
        s.name.toLowerCase().includes(search.toLowerCase()) ||
        s.category.toLowerCase().includes(search.toLowerCase())
    );

    const isExactMatch = catalog.some(
        (s) => s.name.toLowerCase() === search.trim().toLowerCase()
    );

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
                setOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleSelect = (id: number) => {
        onChange(String(id));
        setOpen(false);
        setSearch('');
    };

    const handleCreateNew = () => {
        if (!search.trim() || !onAddNewSkill) return;
        onAddNewSkill(search.trim());
        setOpen(false);
        setSearch('');
    };

    return (
        <div ref={containerRef} className="relative w-full">
            {/* Nút chọn Skill (Light Mode) */}
            <button
                type="button"
                disabled={disabled}
                onClick={() => setOpen(!open)}
                className={cn(
                    "flex w-full items-center justify-between rounded-xl border bg-white px-3.5 py-2.5 text-sm transition focus:outline-none focus:ring-2 focus:ring-violet-500/20",
                    disabled
                        ? "cursor-not-allowed border-slate-200 bg-slate-100 text-slate-400"
                        : "border-slate-300 text-slate-900 hover:border-slate-400 shadow-sm",
                    open && "border-violet-500 ring-2 ring-violet-500/20"
                )}
            >
                <span className={cn("truncate font-medium", !selectedSkill && "text-slate-400 font-normal")}>
                    {selectedSkill ? `${selectedSkill.name} (${selectedSkill.category})` : "— Chọn kỹ năng —"}
                </span>
                <ChevronDown className={cn("h-4 w-4 text-slate-400 transition-transform duration-200", open && "rotate-180")} />
            </button>

            {/* Menu Dropdown Popup (Light Mode) */}
            {open && !disabled && (
                <div className="absolute z-50 mt-1.5 w-full rounded-xl border border-slate-200 bg-white p-2 shadow-xl ring-1 ring-black/5 space-y-1.5">
                    {/* Ô tìm kiếm */}
                    <div className="relative flex items-center">
                        <Search className="absolute left-3 h-4 w-4 text-slate-400" />
                        <input
                            type="text"
                            autoFocus
                            placeholder="Tìm hoặc nhập tên kỹ năng..."
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-9 pr-3 text-xs text-slate-900 placeholder-slate-400 focus:border-violet-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-violet-500"
                        />
                    </div>

                    {/* Danh sách Kỹ năng */}
                    <div className="max-h-52 overflow-y-auto space-y-0.5 pr-1">
                        {filteredCatalog.map((s) => {
                            const isSelected = String(s.id) === value;
                            return (
                                <button
                                    key={s.id}
                                    type="button"
                                    onClick={() => handleSelect(s.id)}
                                    className={cn(
                                        "flex w-full items-center justify-between rounded-lg px-3 py-2 text-xs transition text-left",
                                        isSelected
                                            ? "bg-violet-50 font-semibold text-violet-700"
                                            : "text-slate-700 hover:bg-slate-100 hover:text-slate-900"
                                    )}
                                >
                                    <span className="truncate font-medium">{s.name}</span>
                                    <div className="flex items-center gap-2">
                                        <span className={cn(
                                            "rounded-md border px-2 py-0.5 text-[10px] font-medium transition",
                                            isSelected
                                                ? "border-violet-200 bg-violet-100 text-violet-700"
                                                : "border-slate-200 bg-slate-100 text-slate-600"
                                        )}>
                                            {s.category}
                                        </span>
                                        {isSelected && <Check className="h-3.5 w-3.5 text-violet-600" />}
                                    </div>
                                </button>
                            );
                        })}

                        {filteredCatalog.length === 0 && !search.trim() && (
                            <div className="p-3 text-center text-xs text-slate-400">Không tìm thấy kỹ năng phù hợp</div>
                        )}

                        {/* Thêm nhanh kỹ năng mới */}
                        {search.trim() && !isExactMatch && onAddNewSkill && (
                            <button
                                type="button"
                                onClick={handleCreateNew}
                                className="flex w-full items-center gap-2 rounded-lg border border-dashed border-violet-300 bg-violet-50/50 px-3 py-2 text-xs font-semibold text-violet-700 hover:bg-violet-100/70 transition"
                            >
                                <Plus className="h-4 w-4 text-violet-600" />
                                <span>Thêm kỹ năng: "<strong>{search.trim()}</strong>"</span>
                            </button>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}