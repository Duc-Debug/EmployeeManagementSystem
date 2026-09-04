"use client"

import { useEffect, useRef, useState } from "react"
import { createPortal } from "react-dom"
import { CalendarDays, Pencil } from "lucide-react"
import type { AttendanceRecord } from "@/lib/hr-data"
import { cn } from "@/lib/utils"
import MiniCalendar from "../calendar/MiniCalendar"

interface TimesheetTableProps {
    records: AttendanceRecord[]
    onEditRecord: (id: string) => void
}

const badgeToneFor = (status: AttendanceRecord["status"]) => {
    switch (status) {
        case "Đi muộn":
            return "border-amber-400/40 bg-amber-500/30 text-amber-200"
        case "Vắng mặt":
            return "border-rose-400/40 bg-rose-500/30 text-rose-200"
        case "Đã điều chỉnh":
            return "border-blue-400/40 bg-blue-500/30 text-blue-200"
        default:
            return "border-emerald-400/40 bg-emerald-500/30 text-emerald-200"
    }
}

const formatDisplayDate = (date: Date) =>
    date.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" })

const POPOVER_WIDTH = 280

export function TimesheetTable({ records, onEditRecord }: TimesheetTableProps) {
    // Mặc định chọn ngày hiện tại (thời gian thật của thiết bị), thay vì một
    // ngày cố định — để đồng bộ với đồng hồ thời gian thực ở Trạm chấm công.
    const [selectedDate, setSelectedDate] = useState(() => new Date())
    const [miniCalMonth, setMiniCalMonth] = useState(selectedDate)
    const [isCalendarOpen, setIsCalendarOpen] = useState(false)
    const [popoverPos, setPopoverPos] = useState<{ top: number; left: number } | null>(null)

    const triggerRef = useRef<HTMLButtonElement>(null)
    const popoverRef = useRef<HTMLDivElement>(null)

    const computePosition = () => {
        const rect = triggerRef.current?.getBoundingClientRect()
        if (!rect) return
        setPopoverPos({
            top: rect.bottom + 8,
            left: Math.min(
                Math.max(8, rect.right - POPOVER_WIDTH),
                window.innerWidth - POPOVER_WIDTH - 8,
            ),
        })
    }

    const toggleCalendar = () => {
        if (isCalendarOpen) {
            setIsCalendarOpen(false)
            return
        }
        computePosition()
        setIsCalendarOpen(true)
    }

    useEffect(() => {
        if (!isCalendarOpen) return

        const handleClickOutside = (e: MouseEvent) => {
            const target = e.target as Node
            const clickedTrigger = triggerRef.current?.contains(target)
            const clickedPopover = popoverRef.current?.contains(target)
            if (!clickedTrigger && !clickedPopover) setIsCalendarOpen(false)
        }
        const handleScrollOrResize = () => setIsCalendarOpen(false)

        document.addEventListener("mousedown", handleClickOutside)
        document.addEventListener("scroll", handleScrollOrResize, true)
        window.addEventListener("resize", handleScrollOrResize)
        return () => {
            document.removeEventListener("mousedown", handleClickOutside)
            document.removeEventListener("scroll", handleScrollOrResize, true)
            window.removeEventListener("resize", handleScrollOrResize)
        }
    }, [isCalendarOpen])

    const handleSelectDate = (date: Date) => {
        setSelectedDate(date)
        setMiniCalMonth(date)
        setIsCalendarOpen(false)
    }

    const handleChangeMonth = (offset: number) => {
        setMiniCalMonth((prev) => new Date(prev.getFullYear(), prev.getMonth() + offset, 1))
    }

    return (
        <div className="overflow-hidden rounded-2xl border border-white/20 bg-white/[0.08] shadow-[0_8px_24px_rgba(15,10,45,0.2)] backdrop-blur-xl">
            {/* Table Header Controls */}
            <div className="flex flex-col items-start justify-between gap-3 border-b border-white/15 p-4 sm:flex-row sm:items-center">
                <h2 className="text-base font-extrabold text-white">
                    Bảng tổng hợp chấm công ngày hôm nay
                </h2>

                <button
                    ref={triggerRef}
                    type="button"
                    onClick={toggleCalendar}
                    aria-haspopup="dialog"
                    aria-expanded={isCalendarOpen}
                    aria-label="Chọn ngày xem chấm công"
                    className="flex items-center gap-2 rounded-lg border border-white/20 bg-white/10 px-3.5 py-1.5 text-xs font-bold text-white transition hover:bg-white/20"
                >
                    <CalendarDays className="size-3.5 text-white" />
                    <span>{formatDisplayDate(selectedDate)}</span>
                </button>

                {isCalendarOpen &&
                    popoverPos &&
                    createPortal(
                        <div
                            ref={popoverRef}
                            role="dialog"
                            style={{
                                position: "fixed",
                                top: popoverPos.top,
                                left: popoverPos.left,
                                width: POPOVER_WIDTH,
                            }}
                            className="z-50"
                        >
                            <MiniCalendar
                                miniCalMonth={miniCalMonth}
                                selectedDate={selectedDate}
                                now={new Date()}
                                onSelectDate={handleSelectDate}
                                onChangeMonth={handleChangeMonth}
                            />
                        </div>,
                        document.body,
                    )}
            </div>

            {/* Table Content */}
            <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-white">
                    <thead className="border-b border-white/15 bg-white/[0.05] text-[11px] font-extrabold uppercase tracking-wider text-white/90">
                    <tr>
                        <th className="px-4 py-3.5">Mã NV / Nhân viên</th>
                        <th className="px-4 py-3.5">Phòng ban</th>
                        <th className="px-4 py-3.5">Giờ Vào (In)</th>
                        <th className="px-4 py-3.5">Giờ Ra (Out)</th>
                        <th className="px-4 py-3.5">Giờ Làm</th>
                        <th className="px-4 py-3.5">Tăng Ca (OT)</th>
                        <th className="px-4 py-3.5">Trạng Thái</th>
                        <th className="px-4 py-3.5 text-right">Thao Tác</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-white/10">
                    {records.map((rec) => (
                        <tr key={rec.id} className="transition hover:bg-white/10">
                            <td className="px-4 py-3">
                                <div className="flex items-center gap-2.5">
                                        <span className="flex size-7 items-center justify-center rounded-lg border border-white/20 bg-white/15 text-xs font-black text-white">
                                            {rec.name.charAt(0)}
                                        </span>
                                    <div>
                                        <p className="font-bold text-white">{rec.name}</p>
                                        <p className="font-mono text-[10px] font-semibold text-white/70">{rec.id}</p>
                                    </div>
                                </div>
                            </td>
                            <td className="px-4 py-3 font-semibold text-white/80">{rec.dept}</td>
                            <td className="px-4 py-3 font-mono font-bold text-emerald-300">{rec.inTime}</td>
                            <td className="px-4 py-3 font-mono font-bold text-rose-300">{rec.outTime}</td>
                            <td className="px-4 py-3 font-bold text-white">{rec.hours}</td>
                            <td className="px-4 py-3 font-bold text-white/90">{rec.ot}</td>
                            <td className="px-4 py-3">
                                    <span
                                        className={cn(
                                            "rounded-full border px-2.5 py-0.5 text-[11px] font-bold backdrop-blur-md",
                                            badgeToneFor(rec.status),
                                        )}
                                    >
                                        {rec.status}
                                    </span>
                            </td>
                            <td className="px-4 py-3 text-right">
                                <button
                                    type="button"
                                    onClick={() => onEditRecord(rec.id)}
                                    aria-label={`Sửa bản ghi chấm công của ${rec.name}`}
                                    className="rounded-lg p-1.5 text-white/70 transition hover:bg-white/20 hover:text-white"
                                >
                                    <Pencil className="size-3.5" />
                                </button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    )
}

export default TimesheetTable