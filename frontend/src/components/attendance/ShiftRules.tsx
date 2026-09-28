import { CalendarCheck, Clock, Coffee, ShieldAlert } from "lucide-react"
import type { ShiftRulesData } from "./ShiftConfigModal"

interface RuleCardProps {
    icon: React.ReactNode
    title: string
    value: string
    note: string
}

function RuleCard({ icon, title, value, note }: RuleCardProps) {
    return (
        <div className="flex h-full flex-col justify-between rounded-xl border border-slate-200 bg-slate-50/70 p-4 transition hover:bg-slate-50 hover:border-slate-300">
            <div className="space-y-3">
                <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-500">{title}</span>
                    <div className="text-slate-400">{icon}</div>
                </div>
                <p className="text-base font-bold text-slate-900 sm:text-lg">{value}</p>
            </div>
            <div className="mt-4 border-t border-slate-200/80 pt-2.5">
                <span className="text-xs font-medium text-slate-500">{note}</span>
            </div>
        </div>
    )
}

export function ShiftRules({ rules }: { rules?: ShiftRulesData }) {
    const startTime = rules?.startTime || "08:00 AM"
    const endTime = rules?.endTime || "05:30 PM"
    const lunchBreak = rules?.lunchBreak || "12:00 PM - 01:30 PM"

    return (
        <div className="flex flex-col rounded-2xl border border-slate-200/90 bg-white p-5 shadow-xs text-slate-800 lg:col-span-2">
            <h3 className="mb-4 flex items-center gap-2 text-base font-bold text-slate-900">
                <CalendarCheck className="size-4 text-indigo-600" />
                <span>Quy định giờ làm việc chuẩn (Standard Work Shift)</span>
            </h3>
            <div className="grid flex-1 grid-cols-1 gap-4 text-xs md:grid-cols-3">
                <RuleCard
                    icon={<Clock className="size-4 text-slate-400" />}
                    title="Giờ bắt đầu / Kết thúc"
                    value={`${startTime} - ${endTime}`}
                    note={`Yêu cầu check-in trước ${startTime}`}
                />
                <RuleCard
                    icon={<Coffee className="size-4 text-slate-400" />}
                    title="Nghỉ trưa cố định"
                    value={lunchBreak}
                    note="Thời gian nghỉ cố định ca"
                />
                <RuleCard
                    icon={<ShieldAlert className="size-4 text-slate-400" />}
                    title="Tối đa ca tiêu chuẩn"
                    value="8.0 Giờ / Ngày"
                    note={`Hệ số OT x1.5 sau ${endTime}`}
                />
            </div>
        </div>
    )
}

export default ShiftRules