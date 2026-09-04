import { Users, Calendar as CalendarIcon, ArrowUpRight, ArrowDownRight, Building2, UserCheck } from "lucide-react";
import KpiCard from "./KpiCard.tsx";
import type { Department } from "../department/DepartmentModal";
interface KpiStatsSectionProps {
    departments?: Department[];
}
export default function KpiStatsSection({ departments = [] }: KpiStatsSectionProps) {
    const departmentCount = String(departments.length).padStart(2, "0");
    return (
        <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <KpiCard
                title="Tổng nhân viên"
                value="128"
                subtext="so với tháng trước"
                badgeText="+12%"
                badgeType="increase"
                icon={<><ArrowUpRight className="sr-only" /><Users className="h-5 w-5" /></>}
                iconBgColor="bg-purple-50 border-purple-200"
                iconTextColor="text-purple-600"
            />
            <KpiCard
                title="Đang làm việc"
                value="114"
                subtext="tổng nhân sự"
                badgeText="89.1%"
                badgeType="increase"
                icon={<UserCheck className="h-5 w-5" />}
                iconBgColor="bg-blue-50 border-blue-200"
                iconTextColor="text-blue-600"
            />
            <KpiCard
                title="Đang nghỉ phép"
                value="08"
                subtext="so với tuần trước"
                badgeText="-4.2%"
                badgeType="decrease"
                icon={<><ArrowDownRight className="sr-only" /><CalendarIcon className="h-5 w-5" /></>}
                iconBgColor="bg-amber-50 border-amber-200"
                iconTextColor="text-amber-600"
            />
            <KpiCard
                title="Số phòng ban"
                value={departmentCount}
                subtext="phòng ban hoạt động"
                badgeText="Trực thuộc"
                badgeType="increase"
                icon={<Building2 className="h-5 w-5" />}
                iconBgColor="bg-emerald-50 border-emerald-200"
                iconTextColor="text-emerald-600"
            />
        </div>
    );
}