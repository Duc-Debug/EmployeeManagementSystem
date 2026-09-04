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
        increase: "bg-emerald-50 text-emerald-700 border border-emerald-200",
        decrease: "bg-rose-50 text-rose-700 border border-rose-200",
        neutral: "bg-slate-100 text-slate-700 border border-slate-200",
    };

    return (
        <div className="rounded-2xl border border-slate-200/90 bg-white p-5 shadow-xs transition hover:shadow-md">
            <div className="flex items-center justify-between">
                <div className={`flex h-10 w-10 items-center justify-center rounded-xl border border-slate-100 shadow-xs ${iconBgColor} ${iconTextColor}`}>
                    {icon}
                </div>
                <span className={`inline-flex items-center gap-0.5 rounded-full px-2 py-0.5 text-xs font-semibold ${badgeStyles[badgeType]}`}>
                    {badgeText}
                </span>
            </div>
            <p className="mt-4 text-xs font-medium text-slate-500">{title}</p>
            <div className="flex items-baseline justify-between">
                <span className="text-2xl font-bold text-slate-900">{value}</span>
                <span className="text-xs text-slate-400">{subtext}</span>
            </div>
        </div>
    );
}