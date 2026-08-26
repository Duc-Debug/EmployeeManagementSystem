import type { ReactNode } from "react";

export interface KpiCardProps {
    title: string;
    value: string;
    subtext: string;
    badgeText: string;
    badgeType?: "increase" | "decrease" | "neutral";
    icon: ReactNode;
    iconBgColor: string;
    iconTextColor: string;
}

export default function KpiCard({
                                    title,
                                    value,
                                    subtext,
                                    badgeText,
                                    badgeType = "increase",
                                    icon,
                                    iconBgColor,
                                    iconTextColor,
                                }: KpiCardProps) {
    const badgeStyles = {
        increase: "bg-emerald-400/20 text-emerald-300",
        decrease: "bg-rose-400/20 text-rose-300",
        neutral: "bg-white/15 text-white/70",
    };

    return (
        <div className="rounded-2xl border border-white/15 bg-white/[0.07] p-5 backdrop-blur-xl shadow-[0_8px_24px_rgba(15,10,45,0.15)]">
            <div className="flex items-center justify-between">
                <div className={`flex h-10 w-10 items-center justify-center rounded-xl border border-white/15 ${iconBgColor} ${iconTextColor}`}>
                    {icon}
                </div>
                <span className={`inline-flex items-center gap-0.5 rounded-full px-2 py-0.5 text-xs font-semibold ${badgeStyles[badgeType]}`}>
                    {badgeText}
                </span>
            </div>
            <p className="mt-4 text-xs font-medium text-white/60">{title}</p>
            <div className="flex items-baseline justify-between">
                <span className="text-2xl font-bold text-white">{value}</span>
                <span className="text-xs text-white/40">{subtext}</span>
            </div>
        </div>
    );
}