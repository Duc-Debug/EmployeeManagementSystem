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
    const handleGoToday = () => {
        const today = new Date();
        setSelectedDate(today);
        setMiniCalMonth(today);
    };
    const handlePrevDay = () => {
        const prev = new Date(selectedDate);
        prev.setDate(selectedDate.getDate() - 1);
        setSelectedDate(prev);
    };
    const handleNextDay = () => {
        const next = new Date(selectedDate);
        next.setDate(selectedDate.getDate() + 1);
        setSelectedDate(next);
    };
    const handleChangeMonth = (offset: number) => {
        setMiniCalMonth(
            new Date(miniCalMonth.getFullYear(), miniCalMonth.getMonth() + offset, 1)
        );
    };
    return (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-4">
            <div className="lg:col-span-3">
                <MainCalendar
                    selectedDate={selectedDate}
                    now={now}
                    onPrevDay={handlePrevDay}
                    onNextDay={handleNextDay}
                    onGoToday={handleGoToday}
                />
            </div>
            <div className="lg:col-span-1">
                <MiniCalendar
                    miniCalMonth={miniCalMonth}
                    selectedDate={selectedDate}
                    now={now}
                    onSelectDate={setSelectedDate}
                    onChangeMonth={handleChangeMonth}
                />
            </div>
        </div>
    );
}