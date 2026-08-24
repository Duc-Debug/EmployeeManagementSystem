import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";
interface MiniCalendarProps {
    miniCalMonth: Date;
    selectedDate: Date;
    now: Date;
    onSelectDate: (date: Date) => void;
    onChangeMonth: (offset: number) => void;
}
export default function MiniCalendar({
                                         miniCalMonth,
                                         selectedDate,
                                         now,
                                         onSelectDate,
                                         onChangeMonth,
                                     }: MiniCalendarProps) {
    const isSameDay = (d1: Date, d2: Date) =>
        d1.getDate() === d2.getDate() &&
        d1.getMonth() === d2.getMonth() &&
        d1.getFullYear() === d2.getFullYear();
    const getMiniCalendarDays = () => {
        const year = miniCalMonth.getFullYear();
        const month = miniCalMonth.getMonth();
        const firstDay = new Date(year, month, 1);
        const lastDay = new Date(year, month + 1, 0);
        let dayOfWeek = firstDay.getDay() - 1;
        if (dayOfWeek === -1) dayOfWeek = 6;
        const days = [];
        const prevMonthLastDay = new Date(year, month, 0).getDate();
        for (let i = dayOfWeek - 1; i >= 0; i--) {
            days.push({
                date: new Date(year, month - 1, prevMonthLastDay - i),
                isCurrentMonth: false,
            });
        }
        for (let d = 1; d <= lastDay.getDate(); d++) {
            days.push({
                date: new Date(year, month, d),
                isCurrentMonth: true,
            });
        }
        const remainingSlots = 42 - days.length;
        for (let i = 1; i <= remainingSlots; i++) {
            days.push({
                date: new Date(year, month + 1, i),
                isCurrentMonth: false,
            });
        }
        return days;
    };
    return (
        <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <div className="mb-4 flex items-center justify-between text-slate-600">
                <button
                    onClick={() => onChangeMonth(-1)}
                    className="rounded p-1 hover:bg-slate-100"
                >
                    <ChevronLeft className="h-4 w-4" />
                </button>
                <span className="text-sm font-semibold text-slate-700">
                    tháng {miniCalMonth.getMonth() + 1} {miniCalMonth.getFullYear()}
                </span>
                <button
                    onClick={() => onChangeMonth(1)}
                    className="rounded p-1 hover:bg-slate-100"
                >
                    <ChevronRight className="h-4 w-4" />
                </button>
            </div>

            <div className="grid grid-cols-7 gap-1 text-center text-xs">
                {["T2", "T3", "T4", "T5", "T6", "T7", "CN"].map((day, idx) => (
                    <div key={idx} className="py-1 font-semibold text-slate-500">
                        {day}
                    </div>
                ))}
                {getMiniCalendarDays().map(({ date, isCurrentMonth }, idx) => {
                    const isSelected = isSameDay(date, selectedDate);
                    const isToday = isSameDay(date, now);
                    return (
                        <button
                            key={idx}
                            onClick={() => onSelectDate(date)}
                            className={cn(
                                "mx-auto flex h-7 w-7 items-center justify-center rounded-full text-xs font-medium transition",
                                isSelected
                                    ? "bg-[#00a8ff] font-bold text-white shadow-sm"
                                    : isToday
                                        ? "border border-[#00a8ff] font-bold text-[#00a8ff]"
                                        : isCurrentMonth
                                            ? "text-slate-700 hover:bg-slate-100"
                                            : "text-slate-300"
                            )}
                        >
                            {date.getDate()}
                        </button>
                    );
                })}
            </div>
        </div>
    );
}