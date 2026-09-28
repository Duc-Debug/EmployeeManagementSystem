import { useState, useEffect } from "react";
import MainCalendar from "./MainCalendar";
import MiniCalendar from "./MiniCalendar";
export default function CalendarView() {
    const [selectedDate, setSelectedDate] = useState<Date>(new Date());
    const [miniCalMonth, setMiniCalMonth] = useState<Date>(new Date());
    const [now, setNow] = useState<Date>(new Date());

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 60000);
        return () => clearInterval(timer);
    }, []);

    useEffect(() => {
        const handleDateSync = (e: Event) => {
            const customEvent = e as CustomEvent<Date>;
            if (customEvent.detail && customEvent.detail instanceof Date) {
                setSelectedDate(customEvent.detail);
                setMiniCalMonth(customEvent.detail);
            }
        };
        window.addEventListener("calendar_date_changed", handleDateSync);
        return () => window.removeEventListener("calendar_date_changed", handleDateSync);
    }, []);

    const updateDate = (date: Date) => {
        setSelectedDate(date);
        setMiniCalMonth(date);
        window.dispatchEvent(new CustomEvent("calendar_date_changed", { detail: date }));
    };

    const handleGoToday = () => {
        updateDate(new Date());
    };

    const handlePrevDay = () => {
        const prev = new Date(selectedDate);
        prev.setDate(selectedDate.getDate() - 1);
        updateDate(prev);
    };

    const handleNextDay = () => {
        const next = new Date(selectedDate);
        next.setDate(selectedDate.getDate() + 1);
        updateDate(next);
    };

    const handleChangeMonth = (offset: number) => {
        setMiniCalMonth(
            new Date(miniCalMonth.getFullYear(), miniCalMonth.getMonth() + offset, 1)
        );
    };

    return (
        <div className="flex flex-col gap-6 lg:flex-row items-start">
            {/* MainCalendar */}
            <div className="w-full flex-1 min-w-0">
                <MainCalendar
                    selectedDate={selectedDate}
                    now={now}
                    onPrevDay={handlePrevDay}
                    onNextDay={handleNextDay}
                    onGoToday={handleGoToday}
                />
            </div>

            {/* MiniCalendar - Đã bỏ khung thừa bên ngoài */}
            <div className="w-full lg:w-[280px] shrink-0">
                <div className="sticky top-6">
                    <MiniCalendar
                        miniCalMonth={miniCalMonth}
                        selectedDate={selectedDate}
                        now={now}
                        onSelectDate={updateDate}
                        onChangeMonth={handleChangeMonth}
                    />
                </div>
            </div>
        </div>
    );
}
