import { Users, Calendar as CalendarIcon, ArrowUpRight, ArrowDownRight, Briefcase, UserCheck } from "lucide-react";
import KpiCard from "./KpiCard.tsx";

export default function KpiStatsSection() {
    return (
        <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <KpiCard
                title="Tổng nhân viên"
                value="128"
                subtext="so với tháng trước"
                badgeText="+12%"
                badgeType="increase"
                icon={<><ArrowUpRight className="sr-only" /><Users className="h-5 w-5" /></>}
                iconBgColor="bg-purple-50"
                iconTextColor="text-purple-600"
            />

            <KpiCard
                title="Đang làm việc"
                value="114"
                subtext="tổng nhân sự"
                badgeText="89.1%"
                badgeType="increase"
                icon={<UserCheck className="h-5 w-5" />}
                iconBgColor="bg-blue-50"
                iconTextColor="text-blue-600"
            />

            <KpiCard
                title="Đang nghỉ phép"
                value="08"
                subtext="so với tuần trước"
                badgeText="-4.2%"
                badgeType="decrease"
                icon={<><ArrowDownRight className="sr-only" /><CalendarIcon className="h-5 w-5" /></>}
                iconBgColor="bg-amber-50"
                iconTextColor="text-amber-600"
            />

            <KpiCard
                title="Vị trí tuyển dụng"
                value="06"
                subtext="vị trí mới"
                badgeText="+2"
                badgeType="increase"
                icon={<Briefcase className="h-5 w-5" />}
                iconBgColor="bg-rose-50"
                iconTextColor="text-rose-600"
            />
        </div>
    );
}