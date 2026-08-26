import { ChevronLeft, ChevronRight, ChevronDown } from "lucide-react";
const TIME_SLOTS = Array.from({ length: 24 }, (_, i) => i);
const DAYS_OF_WEEK = ["Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"];
const ROW_HEIGHT = 50;
interface MainCalendarProps {
    selectedDate: Date;
    now: Date;
    onPrevDay: () => void;
    onNextDay: () => void;
    onGoToday: () => void;
}
export default function MainCalendar({
                                         selectedDate,
                                         now,
                                         onPrevDay,
                                         onNextDay,
                                         onGoToday,
                                     }: MainCalendarProps) {
    const isTodaySelected = selectedDate.toDateString() === now.toDateString();
    const currentHours = now.getHours();
    const currentMinutes = now.getMinutes();
    const topPx = (currentHours + currentMinutes / 60) * ROW_HEIGHT;
    const formattedRealTime = now.toLocaleTimeString("en-US", {
        hour: "numeric",
        minute: "2-digit",
        hour12: true,
    });
    return (
        <div className="overflow-hidden rounded-2xl border border-white/15 bg-white/[0.07] backdrop-blur-xl shadow-[0_8px_24px_rgba(15,10,45,0.15)]">
            {/* Header Lịch */}
            <div className="flex items-center justify-between border-b border-white/10 px-6 py-4">
                <div>
                    <h2 className="text-xl font-semibold text-white">
                        {selectedDate.getDate()} tháng {selectedDate.getMonth() + 1} năm {selectedDate.getFullYear()}
                    </h2>
                    <p className="text-sm text-white/50">
                        {DAYS_OF_WEEK[selectedDate.getDay()]}
                    </p>
                </div>
                <div className="flex items-center gap-4 text-sm text-white/70">
                    <button className="flex items-center gap-1 hover:text-white">
                        Ngày <ChevronDown className="h-4 w-4 text-white/50" />
                    </button>
                    <div className="flex items-center rounded-lg border border-white/15">
                        <button onClick={onPrevDay} className="p-1 hover:bg-white/10">
                            <ChevronLeft className="h-4 w-4 text-white/70" />
                        </button>
                        <button
                            onClick={onGoToday}
                            className="border-x border-white/15 px-2.5 py-0.5 text-xs font-medium hover:bg-white/10"
                        >
                            Hôm nay
                        </button>
                        <button onClick={onNextDay} className="p-1 hover:bg-white/10">
                            <ChevronRight className="h-4 w-4 text-white/70" />
                        </button>
                    </div>
                </div>
            </div>
            <div className="border-b border-white/10 bg-white/[0.03] px-4 py-1.5 text-xs font-semibold text-white/50">
                Ngày
            </div>
            {/* mốc giờ & Vạch */}
            <div className="relative h-[550px] overflow-y-auto">
                <div className="relative min-h-[1200px]">
                    {/* Vạch đỏ thời gian */}
                    {isTodaySelected && (
                        <div
                            className="pointer-events-none absolute left-0 right-0 z-10 flex items-center transition-all duration-300"
                            style={{ top: `${topPx}px` }}
                        >
                            <div className="w-16 flex-none pr-2 text-right">
                                <span className="inline-block rounded bg-red-500 px-1 text-[10px] font-bold text-white shadow-xs">
                                    {formattedRealTime}
                                </span>
                            </div>
                            <div className="h-[1.5px] flex-1 bg-red-500" />
                        </div>
                    )}
                    <div className="divide-y divide-white/10">
                        {TIME_SLOTS.map((hour) => {
                            const label =
                                hour === 0
                                    ? "12 AM"
                                    : hour < 12
                                        ? `${hour} AM`
                                        : hour === 12
                                            ? "12 PM"
                                            : `${hour - 12} PM`;
                            return (
                                <div key={hour} className="flex h-[50px] items-start">
                                    <div className="w-16 flex-none py-1.5 pl-3 pr-2 text-right text-[11px] font-medium text-white/40">
                                        {label}
                                    </div>
                                    <div className="h-full flex-1 border-l border-white/10 px-3 py-1"></div>
                                </div>
                            );
                        })}
                    </div>
                </div>
            </div>
        </div>
    );
}