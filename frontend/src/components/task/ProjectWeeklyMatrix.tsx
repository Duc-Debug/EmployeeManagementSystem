import { Calendar, ChevronLeft, ChevronRight, Lightbulb } from 'lucide-react';
import type { ProjectMember, ProjectMonth } from './projectData';

interface ProjectWeeklyMatrixProps {
    month: ProjectMonth;
    members: ProjectMember[];
    selectedRole: string;
    searchTerm: string;
    canEdit?: boolean;
    onNavigateMonth: (direction: number) => void;
    onOpenAdjustModal: (memberId: string, weekKey: string, weekLabel: string) => void;
}

export function ProjectWeeklyMatrix({
    month,
    members,
    selectedRole,
    searchTerm,
    canEdit = false,
    onNavigateMonth,
    onOpenAdjustModal,
}: ProjectWeeklyMatrixProps) {
    const monthWeeks = month.weeks;

    // Filter members based on search and role
    const filteredMembers = members.filter((m) => {
        const q = searchTerm.trim().toLowerCase();
        const matchRole = selectedRole === 'ALL' || m.role === selectedRole;
        const matchSearch =
            !q || m.name.toLowerCase().includes(q) || m.role.toLowerCase().includes(q);
        return matchRole && matchSearch;
    });

    const weekTotals: Record<string, number> = {};
    monthWeeks.forEach((w) => {
        weekTotals[w.key] = 0;
    });
    let grandTotalMonth = 0;

    const getHeatmapStyle = (hours: number, capacity = 40) => {
        const percentage = (hours / capacity) * 100;
        if (hours === 0) {
            return {
                bg: 'bg-slate-50 hover:bg-slate-100',
                border: 'border-slate-200',
                text: 'text-slate-400',
                label: '0%',
            };
        }
        if (percentage < 55) {
            return {
                bg: 'bg-slate-100 hover:bg-slate-200',
                border: 'border-slate-300',
                text: 'text-slate-600',
                label: `${Math.round(percentage)}%`,
            };
        }
        if (percentage <= 100) {
            return {
                bg: 'bg-emerald-50 hover:bg-emerald-100',
                border: 'border-emerald-200',
                text: 'text-emerald-800 font-bold',
                label: `${Math.round(percentage)}%`,
            };
        }
        // Overloaded (> 100%)
        return {
            bg: 'bg-rose-100 hover:bg-rose-200 ring-1 ring-rose-300',
            border: 'border-rose-300',
            text: 'text-rose-900 font-extrabold animate-pulse',
            label: `⚠ ${Math.round(percentage)}%`,
        };
    };

    return (
        <section className="flex flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs transition-all">
            {/* Matrix Header */}
            <div className="flex flex-col justify-between gap-3 border-b border-slate-200 bg-slate-50/70 p-4 sm:flex-row sm:items-center">
                <div className="flex items-center gap-2.5">
                    <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-100 text-xs font-bold text-emerald-700">
                        <Calendar className="h-4 w-4" />
                    </span>
                    <div>
                        <div className="flex items-center gap-2">
                            <h2 className="text-sm font-bold text-slate-900">Phân Bổ Nhân Lực Các Tuần Trong Tháng</h2>
                            {/* Month Navigator Controls */}
                            <div className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2 py-0.5 shadow-2xs">
                                <button
                                    type="button"
                                    onClick={() => onNavigateMonth(-1)}
                                    className="p-0.5 text-[10px] text-slate-400 hover:text-slate-700 transition"
                                    title="Tháng trước"
                                >
                                    <ChevronLeft className="h-3 w-3" />
                                </button>
                                <span className="px-1 text-[11px] font-bold text-indigo-700">{month.name}</span>
                                <button
                                    type="button"
                                    onClick={() => onNavigateMonth(1)}
                                    className="p-0.5 text-[10px] text-slate-400 hover:text-slate-700 transition"
                                    title="Tháng sau"
                                >
                                    <ChevronRight className="h-3 w-3" />
                                </button>
                            </div>
                        </div>
                        <p className="text-[11px] text-slate-500">Giám sát giờ công các tuần của 1 tháng & kiểm soát tải trọng</p>
                    </div>
                </div>

                {/* Heatmap Legend & Scroll Hint */}
                <div className="flex flex-wrap items-center gap-3 text-[11px]">
                    <span className="inline-flex items-center gap-1 rounded-md bg-indigo-50 px-2 py-0.5 font-semibold text-indigo-600">
                        ↔ Cuộn ngang để xem đủ các tuần
                    </span>
                    <span className="flex items-center gap-1 text-slate-500">
                        <span className="h-2.5 w-2.5 rounded-xs border border-slate-300 bg-slate-200" /> Trống (&lt;20h)
                    </span>
                    <span className="flex items-center gap-1 font-medium text-emerald-700">
                        <span className="h-2.5 w-2.5 rounded-xs bg-emerald-500" /> Tối ưu (20-40h)
                    </span>
                    <span className="flex items-center gap-1 font-medium text-rose-700">
                        <span className="h-2.5 w-2.5 rounded-xs bg-rose-500" /> Quá tải (&gt;40h)
                    </span>
                </div>
            </div>

            {/* Matrix Scrollable Container */}
            <div className="overflow-x-auto overflow-y-auto max-h-[550px] pb-2">
                <table className="w-full border-collapse text-left text-xs">
                    <thead>
                        <tr className="border-b border-slate-200 bg-slate-100/80 text-[10px] font-semibold uppercase tracking-wider text-slate-600">
                            <th className="sticky left-0 z-10 min-w-[170px] bg-slate-100 px-3 py-3 shadow-xs">
                                Nhân sự & Vai trò
                            </th>
                            {monthWeeks.map((w) => (
                                <th
                                    key={w.key}
                                    className={`min-w-[75px] px-2 py-3 text-center ${
                                        w.isCurrent ? 'border-x border-indigo-100 bg-indigo-50/70 text-indigo-900' : ''
                                    }`}
                                >
                                    <div className="flex items-center justify-center gap-1">
                                        <span>{w.label}</span>
                                        {w.isCurrent && (
                                            <span className="h-1.5 w-1.5 rounded-full bg-indigo-600" title="Tuần hiện tại" />
                                        )}
                                    </div>
                                    <div
                                        className={`font-normal text-[9px] lowercase ${
                                            w.isCurrent ? 'text-indigo-500' : 'text-slate-400'
                                        }`}
                                    >
                                        {w.dates}
                                    </div>
                                </th>
                            ))}
                            <th className="min-w-[80px] border-l border-slate-200 bg-slate-100/90 px-2 py-3 text-center">
                                <div>Tổng tháng</div>
                                <div className="font-normal text-[9px] text-slate-400 lowercase">Giờ lũy kế</div>
                            </th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {filteredMembers.length === 0 ? (
                            <tr>
                                <td colSpan={monthWeeks.length + 2} className="py-8 text-center text-xs text-slate-400">
                                    Không tìm thấy nhân sự phù hợp với bộ lọc.
                                </td>
                            </tr>
                        ) : (
                            filteredMembers.map((member) => {
                                let memberMonthTotal = 0;
                                monthWeeks.forEach((w) => {
                                    const val = member.weeklyHours[w.key] || 0;
                                    weekTotals[w.key] = (weekTotals[w.key] || 0) + val;
                                    memberMonthTotal += val;
                                });
                                grandTotalMonth += memberMonthTotal;

                                const maxMonthlyCapacity = monthWeeks.length * member.capacity;
                                const monthPct = Math.round((memberMonthTotal / maxMonthlyCapacity) * 100);

                                return (
                                    <tr key={member.id} className="group transition hover:bg-slate-50/90">
                                        {/* Member Identity */}
                                        <td className="sticky left-0 z-10 border-r border-slate-100 bg-white px-3 py-2.5 shadow-xs group-hover:bg-slate-50/90">
                                            <div className="flex items-center gap-2.5 min-w-0">
                                                <div className="relative shrink-0">
                                                    {member.avatar ? (
                                                        <img
                                                            className="h-8 w-8 rounded-full border border-slate-200 object-cover"
                                                            src={member.avatar}
                                                            alt={member.name}
                                                            onError={(e) => {
                                                                (e.target as HTMLElement).style.display = 'none';
                                                            }}
                                                        />
                                                    ) : (
                                                        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-indigo-500 to-indigo-600 text-xs font-bold text-white border border-indigo-200 shadow-2xs">
                                                            {member.name.trim().charAt(0).toUpperCase() || 'N'}
                                                        </div>
                                                    )}
                                                    <span className="absolute -bottom-0.5 -right-0.5 h-2.5 w-2.5 rounded-full border-2 border-white bg-emerald-500" />
                                                </div>
                                                <div className="min-w-0">
                                                    <div className="truncate text-xs font-semibold text-slate-800">
                                                        {member.name}
                                                    </div>
                                                    <div className="truncate text-[10px] text-slate-400">{member.role}</div>
                                                </div>
                                            </div>
                                        </td>

                                        {/* Weeks of the Month Cells */}
                                        {monthWeeks.map((w) => {
                                            const hours = member.weeklyHours[w.key] || 0;
                                            const cellStyle = getHeatmapStyle(hours, member.capacity);

                                            return (
                                                <td
                                                    key={w.key}
                                                    className={`px-2 py-2 text-center ${w.isCurrent ? 'bg-indigo-50/30' : ''}`}
                                                >
                                                    <div
                                                        onClick={() => canEdit && onOpenAdjustModal(member.id, w.key, w.label)}
                                                        className={`select-none rounded-lg p-1.5 transition border ${cellStyle.bg} ${cellStyle.border} ${cellStyle.text} ${
                                                            canEdit ? 'cursor-pointer hover:scale-105 active:scale-95' : 'cursor-default'
                                                        }`}
                                                        title={canEdit ? "Nhấn để điều chỉnh giờ phân bổ" : undefined}
                                                    >
                                                        <div className="text-xs font-bold">{hours}h</div>
                                                        <div className="mt-0.5 text-[9px] font-medium leading-none opacity-90">
                                                            {cellStyle.label}
                                                        </div>
                                                    </div>
                                                </td>
                                            );
                                        })}

                                        {/* Total Month Cell */}
                                        <td className="border-l border-slate-100 bg-slate-50/50 px-2 py-2 text-center">
                                            <div className="text-xs font-bold text-slate-800">{memberMonthTotal}h</div>
                                            <div className="text-[9px] font-medium text-slate-400">{monthPct}% định mức</div>
                                        </td>
                                    </tr>
                                );
                            })
                        )}
                    </tbody>
                    <tfoot>
                        <tr className="border-t-2 border-slate-200 bg-slate-50 text-slate-700 font-bold">
                            <td className="sticky left-0 z-10 border-r border-slate-200 bg-slate-50 px-3 py-3">
                                <div className="flex items-center justify-between text-xs">
                                    <span>Tổng giờ toàn dự án:</span>
                                    <span className="text-[11px] font-normal text-slate-400">
                                        (/ {filteredMembers.length * 40}h)
                                    </span>
                                </div>
                            </td>
                            {monthWeeks.map((w) => {
                                const maxWeeklyTotal = filteredMembers.length * 40 || 240;
                                const tot = weekTotals[w.key] || 0;
                                const isHigh = tot > maxWeeklyTotal;
                                const pct = Math.round((tot / maxWeeklyTotal) * 100);

                                return (
                                    <td
                                        key={w.key}
                                        className={`px-2 py-2.5 text-center ${
                                            w.isCurrent ? 'bg-indigo-100/50 text-indigo-950' : 'text-slate-800'
                                        }`}
                                    >
                                        <div className="text-xs font-bold">{tot}h</div>
                                        <div
                                            className={`text-[10px] font-medium ${
                                                isHigh ? 'font-bold text-rose-600' : 'text-slate-500'
                                            }`}
                                        >
                                            {pct}% tải
                                        </div>
                                    </td>
                                );
                            })}
                            <td className="border-l border-slate-200 bg-slate-100/60 px-2 py-2.5 text-center font-bold text-indigo-950">
                                <div className="text-xs">{grandTotalMonth}h</div>
                                <div className="text-[9px] font-normal text-slate-500">Cả tháng</div>
                            </td>
                        </tr>
                    </tfoot>
                </table>
            </div>

            {/* Weekly Insights Banner */}
            <div className="flex items-start gap-2.5 border-t border-amber-200/60 bg-amber-50/60 p-3 text-xs text-amber-900">
                <Lightbulb className="mt-0.5 h-4 w-4 text-amber-500 shrink-0" />
                <div>
                    <strong className="font-semibold">Gợi ý cân đối nguồn lực tháng này:</strong> Nhân sự{' '}
                    <span className="font-bold underline decoration-amber-400">Lê Quốc Bảo (BE)</span> đang gánh 48 giờ ở Tuần 3
                    (vượt 120% định mức 40h). Hãy chuyển bớt task <em>API Tích hợp cổng thanh toán</em> sang cho{' '}
                    <span className="font-bold">Vũ Tuấn Kiệt</span> hoặc dời lịch sang Tuần 4.
                </div>
            </div>
        </section>
    );
}

