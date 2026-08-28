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
        <div className="flex flex-col justify-between rounded-2xl border border-white/20 bg-white/[0.08] p-5 shadow-[0_8px_24px_rgba(15,10,45,0.2)] backdrop-blur-xl">
            <div>
                <div className="mb-3 flex items-center justify-between">
                    <span className="text-xs font-extrabold uppercase tracking-wider text-white">
                        Trạm chấm công nhanh
                    </span>
                    <span className="rounded border border-emerald-400/40 bg-emerald-500/30 px-2 py-0.5 text-[10px] font-bold text-emerald-200">
                        Ca hành chính
                    </span>
                </div>
                <div className="py-2 text-center">
                    <p className="text-xs font-bold text-white/70">Thời gian hệ thống</p>
                    <h2 className="mt-1 font-mono text-3xl font-black tracking-tight text-white">{clock}</h2>
                    <p className="mt-1 text-xs font-semibold capitalize text-white/80">{date}</p>
                </div>
            </div>

            <div className="mt-4 space-y-2">
                <div className="mb-2 text-center text-xs font-bold text-white/90" role="status" aria-live="polite">
                    {statusText}
                </div>
                <div className="grid grid-cols-2 gap-3">
                    <button
                        type="button"
                        disabled={checkedIn}
                        onClick={handleClockIn}
                        aria-label="Vào ca — ghi nhận giờ check-in"
                        className="flex min-h-11 items-center justify-center gap-1.5 rounded-xl border border-emerald-400/50 bg-emerald-600/40 px-3 py-2.5 text-xs font-bold text-emerald-100 shadow-lg backdrop-blur-md transition hover:bg-emerald-600/60 active:scale-95 disabled:pointer-events-none disabled:opacity-40"
                    >
                        <LogIn className="size-4" />
                        <span>Vào Ca (In)</span>
                    </button>
                    <button
                        type="button"
                        onClick={handleClockOut}
                        aria-label="Ra ca — ghi nhận giờ check-out"
                        className="flex min-h-11 items-center justify-center gap-1.5 rounded-xl border border-rose-400/50 bg-rose-600/40 px-3 py-2.5 text-xs font-bold text-rose-100 shadow-lg backdrop-blur-md transition hover:bg-rose-600/60 active:scale-95"
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