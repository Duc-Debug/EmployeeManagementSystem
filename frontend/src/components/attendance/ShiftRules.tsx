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
        <div className="flex h-full flex-col justify-between rounded-xl border border-white/15 bg-white/[0.05] p-5 backdrop-blur-md transition hover:bg-white/10">
            <div className="space-y-3">
                <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-white/80">{title}</span>
                    <div className="text-white/70">{icon}</div>
                </div>
                <p className="text-base font-extrabold text-white sm:text-lg">{value}</p>
            </div>
            <div className="mt-4 border-t border-white/10 pt-2.5">
                <span className="text-xs font-semibold text-white/70">{note}</span>
            </div>
        </div>
    )
}

export function ShiftRules({ rules }: { rules?: ShiftRulesData }) {
    const startTime = rules?.startTime || "08:00 AM"
    const endTime = rules?.endTime || "05:30 PM"
    const lunchBreak = rules?.lunchBreak || "12:00 PM - 01:30 PM"

    return (
        <div className="flex flex-col rounded-2xl border border-white/20 bg-white/[0.08] p-5 shadow-[0_8px_24px_rgba(15,10,45,0.2)] backdrop-blur-xl lg:col-span-2">
            <h3 className="mb-4 flex items-center gap-2 text-base font-extrabold text-white">
                <CalendarCheck className="size-4 text-purple-300" />
                <span>Quy định giờ làm việc chuẩn (Standard Work Shift)</span>
            </h3>
            <div className="grid flex-1 grid-cols-1 gap-4 text-xs md:grid-cols-3">
                <RuleCard
                    icon={<Clock className="size-4 text-white/70" />}
                    title="Giờ bắt đầu / Kết thúc"
                    value={`${startTime} - ${endTime}`}
                    note={`Yêu cầu check-in trước ${startTime}`}
                />
                <RuleCard
                    icon={<Coffee className="size-4 text-white/70" />}
                    title="Nghỉ trưa cố định"
                    value={lunchBreak}
                    note="Thời gian nghỉ cố định ca"
                />
                <RuleCard
                    icon={<ShieldAlert className="size-4 text-white/70" />}
                    title="Tối đa ca tiêu chuẩn"
                    value="8.0 Giờ / Ngày"
                    note={`Hệ số OT x1.5 sau ${endTime}`}
                />
            </div>
        </div>
    )
}

export default ShiftRules