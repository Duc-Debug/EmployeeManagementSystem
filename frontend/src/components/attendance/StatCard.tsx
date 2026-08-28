import { cn } from "@/lib/utils"

const STAT_LABEL = "text-xs font-bold text-white/80"

interface StatCardProps {
    icon: React.ReactNode
    tone: string
    label: string
    value: string
    valueClass?: string
}

export function StatCard({ icon, tone, label, value, valueClass }: StatCardProps) {
    return (
        <div className="flex items-center gap-4 rounded-2xl border border-white/20 bg-white/[0.08] p-4 shadow-[0_8px_24px_rgba(15,10,45,0.2)] backdrop-blur-xl">
            <div className={cn("flex size-11 items-center justify-center rounded-xl backdrop-blur-md", tone)}>
                {icon}
            </div>
            <div>
                <p className={STAT_LABEL}>{label}</p>
                <h3 className={cn("mt-0.5 text-2xl font-extrabold text-white", valueClass)}>{value}</h3>
            </div>
        </div>
    )
}

export default StatCard