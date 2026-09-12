import { Calendar, ChevronLeft, ChevronRight, Lightbulb, Lock, AlertTriangle } from 'lucide-react';
import type { ProjectMember, ProjectMonth, MonthWeek } from './projectData';

function isWeekPastContractEnd(w: MonthWeek, contractEndDateStr?: string): boolean {
    if (!contractEndDateStr) return false;
    const contractDate = new Date(contractEndDateStr);
    if (isNaN(contractDate.getTime())) return false;

    if (w.year && w.weekNumber) {
        const simple = new Date(w.year, 0, 1 + (w.weekNumber - 1) * 7);
        const dow = simple.getDay();
        const ISOweekStart = new Date(simple);
        if (dow <= 4) {
            ISOweekStart.setDate(simple.getDate() - (simple.getDay() || 7) + 1);
        } else {
            ISOweekStart.setDate(simple.getDate() + 8 - (simple.getDay() || 7));
        }
        return contractDate < ISOweekStart;
    }
    return false;
}

interface ProjectWeeklyMatrixProps {
    month: ProjectMonth;
    members: ProjectMember[];
    selectedRole: string;
    searchTerm: string;
    isClosed?: boolean;
    onNavigateMonth: (direction: number) => void;
    onOpenAdjustModal: (memberId: string, weekKey: string, weekLabel: string) => void;
}

export function ProjectWeeklyMatrix({
    month,
    members,
    selectedRole,
    searchTerm,
    isClosed = false,
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
                        <div className="flex flex-wrap items-center gap-2">
                            <h2 className="text-sm font-bold text-slate-900">Phân Bổ Nhân Lực Các Tuần Trong Tháng</h2>
                            {isClosed && (
                                <span className="inline-flex items-center gap-1 rounded-full border border-rose-200 bg-rose-50 px-2 py-0.5 text-[10px] font-semibold text-rose-700">
                                    <Lock className="h-3 w-3" /> Khóa phân bổ (QTN-08)
                                </span>
                            )}
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
                                                    <div className="truncate text-xs font-semibold text-slate-800 flex items-center gap-1">
                                                        <span>{member.name}</span>
                                                        {member.status && member.status !== 'ACTIVE' && (
                                                            <span className="rounded bg-rose-100 px-1 py-0.2 text-[8px] font-bold text-rose-700">
                                                                Đã nghỉ
                                                            </span>
                                                        )}
                                                    </div>
                                                    <div className="truncate text-[10px] text-slate-400 flex items-center gap-1">
                                                        <span>{member.role}</span>
                                                        {member.contractEndDate && (
                                                            <span className="text-[9px] text-amber-600 font-mono">
                                                                (HĐ: {member.contractEndDate})
                                                            </span>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        </td>

                                        {/* Weeks of the Month Cells */}
                                        {monthWeeks.map((w) => {
                                            const hours = member.weeklyHours[w.key] || 0;
                                            const cellStyle = getHeatmapStyle(hours, member.capacity);
                                            const isPastContract = isWeekPastContractEnd(w, member.contractEndDate) || (member.status && member.status !== 'ACTIVE');
                                            const hasExpiredAllocationWarning = hours > 0 && isPastContract;

                                            return (
                                                <td
                                                    key={w.key}
                                                    className={`px-2 py-2 text-center ${w.isCurrent ? 'bg-indigo-50/30' : ''}`}
                                                >
                                                    <div
                                                        onClick={() => !isClosed && onOpenAdjustModal(member.id, w.key, w.label)}
                                                        title={
                                                            isClosed
                                                                ? 'Dự án đã đóng, không thể điều chỉnh phân bổ nguồn lực (QTN-08)'
                                                                : hasExpiredAllocationWarning
                                                                ? `Cảnh báo: Nhân sự đã nghỉ việc / hết hạn HĐ (${member.contractEndDate || 'Đã nghỉ'}), phân bổ ${hours}h này vắt qua ngày nghỉ việc!`
                                                                : 'Bấm để điều chỉnh giờ phân bổ'
                                                        }
                                                        className={`select-none rounded-lg p-1.5 transition border ${
                                                            isClosed
                                                                ? 'cursor-not-allowed opacity-70'
                                                                : 'cursor-pointer transform hover:scale-105 active:scale-95'
                                                        } ${
                                                            hasExpiredAllocationWarning
                                                                ? 'bg-amber-100 border-amber-400 text-amber-900 ring-2 ring-amber-300'
                                                                : `${cellStyle.bg} ${cellStyle.border} ${cellStyle.text}`
                                                        }`}
                                                    >
                                                        <div className="text-xs font-bold flex items-center justify-center gap-0.5">
                                                            {hasExpiredAllocationWarning && (
                                                                <AlertTriangle className="h-3 w-3 text-amber-600 shrink-0" />
                                                            )}
                                                            <span>{hours}h</span>
                                                        </div>
                                                        <div className="mt-0.5 text-[9px] font-medium leading-none opacity-90">
                                                            {hasExpiredAllocationWarning ? '⚠ Quá hạn HĐ' : cellStyle.label}
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
            {(() => {
                const overloadedEntries: { memberName: string; role: string; weekLabel: string; hours: number; pct: number }[] = [];
                filteredMembers.forEach((m) => {
                    monthWeeks.forEach((w) => {
                        const h = m.weeklyHours[w.key] || 0;
                        const cap = m.capacity || 40;
                        if (h > cap) {
                            overloadedEntries.push({
                                memberName: m.name,
                                role: m.role,
                                weekLabel: w.label,
                                hours: h,
                                pct: Math.round((h / cap) * 100),
                            });
                        }
                    });
                });

                if (overloadedEntries.length > 0) {
                    const top = overloadedEntries[0];
                    return (
                        <div className="flex items-start gap-2.5 border-t border-rose-200/80 bg-rose-50/70 p-3 text-xs text-rose-900">
                            <Lightbulb className="mt-0.5 h-4 w-4 text-rose-600 shrink-0" />
                            <div>
                                <strong className="font-semibold">Cảnh báo tải trọng nhân lực tháng này:</strong> Nhân sự{' '}
                                <span className="font-bold underline decoration-rose-400">{top.memberName} ({top.role})</span>{' '}
                                đang được phân bổ <strong>{top.hours}h</strong> ở {top.weekLabel} (vượt <strong>{top.pct}%</strong> định mức).
                                Cần cân nhắc san sẻ bớt công việc cho nhân sự khác còn trống giờ hoặc giãn tiến độ sang các tuần tiếp theo.
                            </div>
                        </div>
                    );
                }

                return (
                    <div className="flex items-start gap-2.5 border-t border-emerald-200/80 bg-emerald-50/70 p-3 text-xs text-emerald-900">
                        <Lightbulb className="mt-0.5 h-4 w-4 text-emerald-600 shrink-0" />
                        <div>
                            <strong className="font-semibold">Đánh giá tải trọng nhân lực:</strong> Phân bổ công suất toàn bộ đội ngũ trong tháng này đang ở mức an toàn, không có nhân sự nào vượt quá định mức 40h/tuần. Nhấp vào từng ô giờ để điều chỉnh phân bổ chi tiết.
                        </div>
                    </div>
                );
            })()}
        </section>
    );
}