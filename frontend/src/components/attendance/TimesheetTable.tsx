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
            return "border-amber-200 bg-amber-50 text-amber-700"
        case "Vắng mặt":
            return "border-rose-200 bg-rose-50 text-rose-700"
        case "Đã điều chỉnh":
            return "border-blue-200 bg-blue-50 text-blue-700"
        default:
            return "border-emerald-200 bg-emerald-50 text-emerald-700"
    }
}

const formatDisplayDate = (date: Date) =>
    date.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" })

const POPOVER_WIDTH = 280

export function TimesheetTable({ records, onEditRecord }: TimesheetTableProps) {
    const [selectedDate, setSelectedDate] = useState(() => new Date(2026, 7, 27))
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
        const handleClickOutside = (event: MouseEvent) => {
            const target = event.target as Node
            if (
                popoverRef.current &&
                !popoverRef.current.contains(target) &&
                triggerRef.current &&
                !triggerRef.current.contains(target)
            ) {
                setIsCalendarOpen(false)
            }
        }
        if (isCalendarOpen) {
            document.addEventListener("mousedown", handleClickOutside)
        }
        return () => {
            document.removeEventListener("mousedown", handleClickOutside)
        }
    }, [isCalendarOpen])

    useEffect(() => {
        if (!isCalendarOpen) return
        const handleScrollOrResize = () => {
            computePosition()
        }
        window.addEventListener("scroll", handleScrollOrResize, true)
        window.addEventListener("resize", handleScrollOrResize)
        return () => {
            window.removeEventListener("scroll", handleScrollOrResize, true)
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
        <div className="overflow-hidden rounded-2xl border border-slate-200/90 bg-white shadow-xs text-slate-800">
            {/* Table Header Controls */}
            <div className="flex flex-col items-start justify-between gap-3 border-b border-slate-100 p-4 sm:flex-row sm:items-center">
                <h2 className="text-base font-bold text-slate-900">
                    Bảng tổng hợp chấm công ngày hôm nay
                </h2>

                <button
                    ref={triggerRef}
                    type="button"
                    onClick={toggleCalendar}
                    aria-haspopup="dialog"
                    aria-expanded={isCalendarOpen}
                    aria-label="Chọn ngày xem chấm công"
                    className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3.5 py-1.5 text-xs font-semibold text-slate-700 shadow-xs transition hover:bg-slate-50"
                >
                    <CalendarDays className="size-3.5 text-slate-500" />
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
                <table className="w-full text-left text-xs text-slate-800">
                    <thead className="border-b border-slate-200 bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-500">
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
                    <tbody className="divide-y divide-slate-100">
                    {records.map((rec) => (
                        <tr key={rec.id} className="transition hover:bg-slate-50/80">
                            <td className="px-4 py-3">
                                <div className="flex items-center gap-2.5">
                                        <span className="flex size-7 items-center justify-center rounded-lg border border-indigo-100 bg-indigo-50 text-xs font-black text-indigo-700 shadow-2xs">
                                            {rec.name.charAt(0)}
                                        </span>
                                    <div>
                                        <p className="font-bold text-slate-900">{rec.name}</p>
                                        <p className="font-mono text-[10px] font-semibold text-slate-400">{rec.id}</p>
                                    </div>
                                </div>
                            </td>
                            <td className="px-4 py-3 font-medium text-slate-600">{rec.dept}</td>
                            <td className="px-4 py-3 font-mono font-bold text-emerald-600">{rec.inTime}</td>
                            <td className="px-4 py-3 font-mono font-bold text-rose-600">{rec.outTime}</td>
                            <td className="px-4 py-3 font-bold text-slate-900">{rec.hours}</td>
                            <td className="px-4 py-3 font-medium text-slate-600">{rec.ot}</td>
                            <td className="px-4 py-3">
                                    <span
                                        className={cn(
                                            "rounded-full border px-2.5 py-0.5 text-[11px] font-bold",
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
                                    className="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
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