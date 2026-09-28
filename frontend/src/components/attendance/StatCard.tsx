import { cn } from "@/lib/utils"

const STAT_LABEL = "text-xs font-medium text-slate-500"

interface StatCardProps {
    icon: React.ReactNode
    tone: string
    label: string
    value: string
    valueClass?: string
}

export function StatCard({ icon, tone, label, value, valueClass }: StatCardProps) {
    return (
        <div className="flex items-center gap-4 rounded-2xl border border-slate-200/90 bg-white p-4 shadow-xs">
            <div className={cn("flex size-11 items-center justify-center rounded-xl", tone)}>
                {icon}
            </div>
            <div>
                <p className={STAT_LABEL}>{label}</p>
                <h3 className={cn("mt-0.5 text-2xl font-bold text-slate-900", valueClass)}>{value}</h3>
            </div>
        </div>
    )
}

export default StatCard