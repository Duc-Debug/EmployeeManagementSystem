"use client"

import { useEffect, useState } from "react"
import { LogIn, LogOut } from "lucide-react"

interface ClockStationProps {
    onClockIn: () => string
    onClockOut: () => boolean
    onNotify: (message: string) => void
}

export function ClockStation({ onClockIn, onClockOut, onNotify }: ClockStationProps) {
    const [clock, setClock] = useState("--:--:--")
    const [date, setDate] = useState("")
    const [statusText, setStatusText] = useState<React.ReactNode>("Chưa ghi nhận vào ca")
    const [checkedIn, setCheckedIn] = useState(false)

    useEffect(() => {
        const update = () => {
            const now = new Date()
            setClock(now.toLocaleTimeString("en-US", { hour12: false }))
            setDate(
                now.toLocaleDateString("vi-VN", {
                    weekday: "long",
                    day: "numeric",
                    month: "long",
                    year: "numeric",
                }),
            )
        }
        update()
        const id = setInterval(update, 1000)
        return () => clearInterval(id)
    }, [])

    const handleClockIn = () => {
        const t = onClockIn()
        setCheckedIn(true)
        setStatusText(<span className="font-bold text-emerald-300">Đã Check-In lúc {t}</span>)
    }

    const handleClockOut = () => {
        if (!checkedIn) {
            onNotify("Vui lòng thực hiện Check-In trước khi Check-Out!")
            return
        }
        const t = onClockOut()
        if (t) {
            setStatusText(<span className="font-bold text-rose-300">Đã Check-Out</span>)
        }
    }

    return (
        <div className="flex flex-col justify-between rounded-2xl border border-slate-200/90 bg-white p-5 shadow-xs text-slate-800">
            <div>
                <div className="mb-3 flex items-center justify-between">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-900">
                        Trạm chấm công nhanh
                    </span>
                    <span className="rounded-md border border-emerald-200 bg-emerald-50 px-2 py-0.5 text-[10px] font-bold text-emerald-700">
                        Ca hành chính
                    </span>
                </div>
                <div className="py-2 text-center">
                    <p className="text-xs font-medium text-slate-400">Thời gian hệ thống</p>
                    <h2 className="mt-1 font-mono text-3xl font-extrabold tracking-tight text-slate-900">{clock}</h2>
                    <p className="mt-1 text-xs font-semibold capitalize text-slate-500">{date}</p>
                </div>
            </div>

            <div className="mt-4 space-y-2">
                <div className="mb-2 text-center text-xs font-bold text-slate-700" role="status" aria-live="polite">
                    {statusText}
                </div>
                <div className="grid grid-cols-2 gap-3">
                    <button
                        type="button"
                        disabled={checkedIn}
                        onClick={handleClockIn}
                        aria-label="Vào ca — ghi nhận giờ check-in"
                        className="flex min-h-11 items-center justify-center gap-1.5 rounded-xl border border-emerald-600 bg-emerald-600 px-3 py-2.5 text-xs font-bold text-white shadow-xs transition hover:bg-emerald-700 active:scale-95 disabled:pointer-events-none disabled:opacity-40"
                    >
                        <LogIn className="size-4" />
                        <span>Vào Ca (In)</span>
                    </button>
                    <button
                        type="button"
                        onClick={handleClockOut}
                        aria-label="Ra ca — ghi nhận giờ check-out"
                        className="flex min-h-11 items-center justify-center gap-1.5 rounded-xl border border-rose-600 bg-rose-600 px-3 py-2.5 text-xs font-bold text-white shadow-xs transition hover:bg-rose-700 active:scale-95"
                    >
                        <LogOut className="size-4" />
                        <span>Ra Ca (Out)</span>
                    </button>
                </div>
            </div>
        </div>
    )
}

export default ClockStation