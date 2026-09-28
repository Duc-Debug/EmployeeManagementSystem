import { useState, useRef, useEffect } from "react";
import { CalendarDays, X } from "lucide-react";
import { cn } from "@/lib/utils";
import MiniCalendar from "./MiniCalendar";

interface DatePickerInputProps {
    value?: string; // YYYY-MM-DD
    onChange: (value: string) => void;
    placeholder?: string;
    min?: string; // YYYY-MM-DD
    className?: string;
    disabled?: boolean;
}

export default function DatePickerInput({
    value,
    onChange,
    placeholder = "dd/mm/yyyy",
    min,
    className,
    disabled = false,
}: DatePickerInputProps) {
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef<HTMLDivElement>(null);

    const now = new Date();

    const parseValue = (valStr?: string): Date => {
        if (!valStr) return now;
        const [y, m, d] = valStr.split("-").map(Number);
        if (y && m && d) return new Date(y, m - 1, d);
        return now;
    };

    const selectedDate = parseValue(value);
    const [miniCalMonth, setMiniCalMonth] = useState<Date>(selectedDate);

    useEffect(() => {
        if (value) {
            setMiniCalMonth(parseValue(value));
        }
    }, [value]);

    useEffect(() => {
        function handleClickOutside(e: MouseEvent) {
            if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
                setIsOpen(false);
            }
        }
        if (isOpen) {
            document.addEventListener("mousedown", handleClickOutside);
        }
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, [isOpen]);

    const handleSelectDate = (date: Date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        const dateStr = `${year}-${month}-${day}`;

        if (min && dateStr < min) return;

        onChange(dateStr);
        setIsOpen(false);
    };

    const handleChangeMonth = (offset: number) => {
        setMiniCalMonth((prev) => new Date(prev.getFullYear(), prev.getMonth() + offset, 1));
    };

    const formatDisplay = (valStr?: string) => {
        if (!valStr) return "";
        const [y, m, d] = valStr.split("-");
        if (y && m && d) return `${d}/${m}/${y}`;
        return valStr;
    };

    return (
        <div className="relative w-full" ref={containerRef}>
            <div
                onClick={() => !disabled && setIsOpen((prev) => !prev)}
                className={cn(
                    "w-full rounded-xl border border-slate-200 bg-slate-50/70 px-3.5 py-2 text-xs font-semibold text-slate-800 flex items-center justify-between cursor-pointer outline-none transition focus-within:border-indigo-500 focus-within:bg-white focus-within:ring-2 focus-within:ring-indigo-100",
                    isOpen && "border-indigo-500 bg-white ring-2 ring-indigo-100",
                    disabled && "cursor-not-allowed bg-slate-100 text-slate-400",
                    className
                )}
            >
                <span className={cn("truncate", !value && "text-slate-400 font-normal")}>
                    {value ? formatDisplay(value) : placeholder}
                </span>
                <div className="flex items-center gap-1">
                    {value && !disabled && (
                        <button
                            type="button"
                            onClick={(e) => {
                                e.stopPropagation();
                                onChange("");
                            }}
                            className="rounded-md p-0.5 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition"
                        >
                            <X className="size-3" />
                        </button>
                    )}
                    <CalendarDays className="size-4 shrink-0 text-slate-400" />
                </div>
            </div>

            {isOpen && !disabled && (
                <div className="absolute left-0 z-50 mt-1.5 w-72 shadow-xl animate-in fade-in zoom-in-95 duration-150">
                    <MiniCalendar
                        miniCalMonth={miniCalMonth}
                        selectedDate={selectedDate}
                        now={now}
                        onSelectDate={handleSelectDate}
                        onChangeMonth={handleChangeMonth}
                    />
                </div>
            )}
        </div>
    );
}