import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { Check, ChevronDown, Search, X } from "lucide-react";

export interface TaskSelectOption {
    id: string;
    label: string;
    sublabel?: string;
}

interface TaskSelectProps {
    value: string | null;
    options: TaskSelectOption[];
    onChange: (id: string) => void;
    placeholder?: string;
    searchPlaceholder?: string;
    emptyText?: string;
    disabled?: boolean;
    allowClear?: boolean;
    hideSearch?: boolean;
    icon?: ReactNode;
    buttonClassName?: string;
}

export default function TaskSelect({
    value,
    options,
    onChange,
    placeholder = "Chọn...",
    searchPlaceholder = "Tìm kiếm...",
    emptyText = "Không tìm thấy kết quả phù hợp.",
    disabled = false,
    allowClear = false,
    hideSearch = false,
    icon,
    buttonClassName,
}: TaskSelectProps) {
    const [open, setOpen] = useState(false);
    const [query, setQuery] = useState("");
    const rootRef = useRef<HTMLDivElement>(null);

    const selected = useMemo(() => options.find((o) => o.id === value) ?? null, [options, value]);

    const filtered = useMemo(() => {
        const q = query.trim().toLowerCase();
        if (!q) return options;
        return options.filter(
            (o) => o.label.toLowerCase().includes(q) || (o.sublabel ?? "").toLowerCase().includes(q)
        );
    }, [options, query]);

    useEffect(() => {
        function handleClickOutside(e: MouseEvent) {
            if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
                setOpen(false);
                setQuery("");
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    return (
        <div className="relative w-full" ref={rootRef}>
            <button
                type="button"
                disabled={disabled}
                onClick={() => setOpen((o) => !o)}
                className={[
                    "flex w-full items-center justify-between gap-2 text-xs font-medium outline-none transition rounded-xl border border-slate-300 bg-white px-3 py-2 text-slate-800 hover:border-slate-400",
                    buttonClassName ? buttonClassName : "",
                    disabled ? "cursor-not-allowed border-slate-200 bg-slate-100 text-slate-400" : "",
                    open ? "border-indigo-500 ring-2 ring-indigo-500/20" : "",
                ].join(" ")}
            >
                <span className="flex min-w-0 items-center gap-2">
                    {icon}
                    <span className={`truncate ${selected ? "text-slate-800 font-medium" : "text-slate-400 font-normal"}`}>
                        {selected ? selected.label : placeholder}
                    </span>
                </span>
                <span className="flex shrink-0 items-center gap-1">
                    {allowClear && selected && !disabled && (
                        <span
                            role="button"
                            onClick={(e) => {
                                e.stopPropagation();
                                onChange("");
                            }}
                            className="rounded-md p-0.5 text-slate-400 transition hover:bg-slate-200 hover:text-slate-600"
                        >
                            <X className="h-3 w-3" />
                        </span>
                    )}
                    <ChevronDown className={`h-3.5 w-3.5 text-slate-400 transition ${open ? "rotate-180" : ""}`} />
                </span>
            </button>

            {open && !disabled && (
                <div className="absolute z-50 mt-1.5 w-full overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl">
                    {!hideSearch && (
                        <div className="flex items-center gap-2 border-b border-slate-100 px-3 py-2">
                            <Search className="h-3.5 w-3.5 shrink-0 text-slate-400" />
                            <input
                                autoFocus
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                                placeholder={searchPlaceholder}
                                className="w-full bg-transparent text-xs text-slate-800 outline-none placeholder:text-slate-400"
                            />
                        </div>
                    )}
                    <div className="max-h-52 overflow-y-auto py-1">
                        {filtered.length === 0 ? (
                            <p className="px-3 py-3 text-center text-xs text-slate-400">{emptyText}</p>
                        ) : (
                            filtered.map((opt) => (
                                <button
                                    key={opt.id}
                                    type="button"
                                    onClick={() => {
                                        onChange(opt.id);
                                        setOpen(false);
                                        setQuery("");
                                    }}
                                    className={[
                                        "flex items-center justify-between gap-2 mx-1 px-3 py-2 text-left text-xs transition rounded-xl hover:bg-slate-100/80",
                                        opt.id === value ? "bg-indigo-50 font-semibold text-indigo-700" : "text-slate-700",
                                    ].join(" ")}
                                >
                                    <span className="min-w-0">
                                        <span className="block truncate">{opt.label}</span>
                                        {opt.sublabel && (
                                            <span className="block truncate text-[10px] font-normal text-slate-400">
                                                {opt.sublabel}
                                            </span>
                                        )}
                                    </span>
                                    {opt.id === value && <Check className="h-3.5 w-3.5 shrink-0 text-indigo-600" />}
                                </button>
                            ))
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

