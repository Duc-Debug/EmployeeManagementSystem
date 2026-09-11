"use client";

import { useState, useEffect, useMemo } from "react";
import {
    ChevronLeft,
    ChevronRight,
    Calendar as CalendarIcon,
    CalendarDays,
    AlertTriangle,
    Users,
    FileText,
    Sliders,
    GitBranch,
    X,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuthUser } from "@/lib/auth-session";
import { getOrgTree } from "@/lib/api/org-units";
import {
    getDepartmentMonthlyLeaveCalendar,
    type DepartmentMonthlyLeaveCalendarDto,
    type DailyLeaveSummaryDto,
} from "@/lib/api/leave";
import { OrgUnitCombobox, type OrgUnitOption } from "@/components/ui/OrgUnitCombobox";
import type { OrgUnitTreeNode } from "@/types/hrm";

const LEAVE_TYPE_LABELS: Record<string, string> = {
    ANNUAL: "Nghỉ phép năm",
    UNPAID: "Không lương",
    SICK: "Nghỉ ốm",
    PERSONAL: "Việc riêng",
};

const LEAVE_TYPE_BADGES: Record<string, string> = {
    ANNUAL: "bg-blue-50 text-blue-700 border-blue-200",
    UNPAID: "bg-amber-50 text-amber-700 border-amber-200",
    SICK: "bg-rose-50 text-rose-700 border-rose-200",
    PERSONAL: "bg-purple-50 text-purple-700 border-purple-200",
};

const WEEKDAY_NAMES = [
    "Thứ 2",
    "Thứ 3",
    "Thứ 4",
    "Thứ 5",
    "Thứ 6",
    "Thứ 7",
    "Chủ Nhật",
];

function flattenOrgTree(nodes: readonly OrgUnitTreeNode[], depth = 0): OrgUnitOption[] {
    const result: OrgUnitOption[] = [];
    for (const node of nodes) {
        result.push({
            id: node.id,
            unitCode: node.unitCode,
            unitName: node.unitName,
            unitType: node.unitType,
            depth,
        });
        if (node.children && node.children.length > 0) {
            result.push(...flattenOrgTree(node.children, depth + 1));
        }
    }
    return result;
}

export default function DepartmentLeaveCalendarView() {
    const user = useAuthUser();

    // 1. Quản lý trạng thái bộ lọc & ngày tháng
    const [selectedDate, setSelectedDate] = useState<Date>(new Date());
    const [orgOptions, setOrgOptions] = useState<OrgUnitOption[]>([]);
    const [selectedOrgUnitId, setSelectedOrgUnitId] = useState<string>("");
    const [warningThreshold, setWarningThreshold] = useState<number>(0.5); // 50%
    const [includeSubUnits, setIncludeSubUnits] = useState<boolean>(false);

    // 2. Trạng thái dữ liệu lịch
    const [calendarData, setCalendarData] = useState<DepartmentMonthlyLeaveCalendarDto | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    // 3. Modal xem chi tiết ngày được chọn
    const [selectedDayDetail, setSelectedDayDetail] = useState<DailyLeaveSummaryDto | null>(null);

    const year = selectedDate.getFullYear();
    const month = selectedDate.getMonth() + 1;

    // Tải cây phòng ban để hiển thị Combobox
    useEffect(() => {
        let isMounted = true;
        async function fetchOrgs() {
            try {
                const tree = await getOrgTree();
                if (!isMounted) return;
                const flat = flattenOrgTree(tree);
                setOrgOptions(flat);

                // Ưu tiên chọn phòng ban của User
                if (!selectedOrgUnitId && flat.length > 0) {
                    const preferredId = user?.scopeOrgUnitId || user?.orgUnitId;
                    const exists = flat.find((o) => o.id === preferredId);
                    if (exists) {
                        setSelectedOrgUnitId(String(exists.id));
                    } else {
                        setSelectedOrgUnitId(String(flat[0].id));
                    }
                }
            } catch (err) {
                console.warn("Không thể tải danh sách đơn vị tổ chức:", err);
            }
        }
        fetchOrgs();
        return () => {
            isMounted = false;
        };
    }, [user?.scopeOrgUnitId, user?.orgUnitId]);

    // Tải dữ liệu lịch nghỉ từ Backend API (NCL-05-CN-006)
    const loadCalendarData = async () => {
        if (!selectedOrgUnitId) return;
        setIsLoading(true);
        setError(null);
        try {
            const data = await getDepartmentMonthlyLeaveCalendar({
                orgUnitId: Number(selectedOrgUnitId),
                year,
                month,
                warningThreshold,
                includeSubUnits,
            });
            setCalendarData(data);
        } catch (err: any) {
            console.error("Lỗi khi tải lịch nghỉ bộ phận:", err);
            setError(err?.message || "Không thể tải lịch nghỉ bộ phận. Vui lòng kiểm tra quyền truy cập.");
            setCalendarData(null);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadCalendarData();
    }, [selectedOrgUnitId, year, month, warningThreshold, includeSubUnits]);

    // Điều hướng tháng
    const handlePrevMonth = () => {
        setSelectedDate(new Date(year, month - 2, 1));
    };

    const handleNextMonth = () => {
        setSelectedDate(new Date(year, month, 1));
    };

    const handleToday = () => {
        setSelectedDate(new Date());
    };

    // Tính toán các ô trống trước ngày 1 của tháng (Thứ 2 = index 0 -> CN = index 6)
    const blankLeadingDays = useMemo(() => {
        const firstDayOfWeek = new Date(year, month - 1, 1).getDay();
        return (firstDayOfWeek + 6) % 7;
    }, [year, month]);

    // Danh sách ngày có cảnh báo vượt ngưỡng
    const warningDays = useMemo(() => {
        if (!calendarData?.dailySummaries) return [];
        return calendarData.dailySummaries.filter((d) => d.isWarning);
    }, [calendarData]);

    return (
        <div className="space-y-6 animate-in fade-in duration-150">
            {/* Thanh điều khiển trên cùng (Bộ lọc phòng ban, tháng, ngưỡng) */}
            <div className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-xs lg:flex-row lg:items-center lg:justify-between">
                {/* Chọn phòng ban */}
                <div className="w-full max-w-sm">
                    <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1.5">
                        Đơn vị / Bộ phận
                    </label>
                    <OrgUnitCombobox
                        id="dept-leave-org-select"
                        options={orgOptions}
                        value={selectedOrgUnitId}
                        onChange={(val) => setSelectedOrgUnitId(val)}
                        placeholder="Chọn phòng ban cần xem..."
                    />
                </div>

                {/* Bộ chọn Tháng / Năm & Nút Hôm nay */}
                <div className="flex items-center gap-2">
                    <div className="flex items-center rounded-xl border border-slate-200 bg-slate-50/80 p-1">
                        <button
                            type="button"
                            onClick={handlePrevMonth}
                            className="rounded-lg p-1.5 text-slate-600 hover:bg-white hover:text-slate-900 transition shadow-2xs cursor-pointer"
                            title="Tháng trước"
                        >
                            <ChevronLeft className="size-4" />
                        </button>

                        <div className="flex items-center gap-1.5 px-3">
                            <CalendarIcon className="size-4 text-indigo-600" />
                            <span className="text-xs font-bold text-slate-900 min-w-[120px] text-center">
                                Tháng {month < 10 ? `0${month}` : month} / {year}
                            </span>
                        </div>

                        <button
                            type="button"
                            onClick={handleNextMonth}
                            className="rounded-lg p-1.5 text-slate-600 hover:bg-white hover:text-slate-900 transition shadow-2xs cursor-pointer"
                            title="Tháng sau"
                        >
                            <ChevronRight className="size-4" />
                        </button>
                    </div>

                    <button
                        type="button"
                        onClick={handleToday}
                        className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-bold text-slate-700 hover:bg-slate-50 transition shadow-2xs cursor-pointer"
                    >
                        Hôm nay
                    </button>
                </div>

                {/* Bộ lọc Ngưỡng cảnh báo & Cờ includeSubUnits */}
                <div className="flex flex-wrap items-center gap-3">
                    <div className="flex items-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50/80 px-3 py-1.5">
                        <Sliders className="size-3.5 text-slate-500" />
                        <span className="text-xs font-semibold text-slate-600">Ngưỡng cảnh báo:</span>
                        <select
                            value={warningThreshold}
                            onChange={(e) => setWarningThreshold(Number(e.target.value))}
                            className="rounded-lg border-0 bg-transparent text-xs font-bold text-indigo-600 outline-none cursor-pointer"
                        >
                            <option value={0.3}>30% (Nhạy)</option>
                            <option value={0.4}>40%</option>
                            <option value={0.5}>50% (Chuẩn)</option>
                            <option value={0.6}>60%</option>
                            <option value={0.7}>70%</option>
                            <option value={0.8}>80% (Nới lỏng)</option>
                        </select>
                    </div>

                    {/* Cờ includeSubUnits (P2) */}
                    <label className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50/80 px-3 py-2 text-xs font-semibold text-slate-700 cursor-pointer hover:bg-slate-100/70 transition select-none">
                        <input
                            type="checkbox"
                            checked={includeSubUnits}
                            onChange={(e) => setIncludeSubUnits(e.target.checked)}
                            className="size-3.5 rounded text-indigo-600 focus:ring-indigo-500 cursor-pointer"
                        />
                        <GitBranch className="size-3.5 text-slate-500" />
                        <span>Bao gồm bộ phận con</span>
                    </label>
                </div>
            </div>

            {/* Thẻ thống kê nhanh KPI */}
            <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
                <div className="rounded-2xl border border-blue-200 bg-blue-50/60 p-4">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-bold uppercase tracking-wider text-blue-800">
                            Nhân sự bộ phận
                        </span>
                        <Users className="size-4 text-blue-600" />
                    </div>
                    <p className="mt-2 text-2xl font-black text-blue-950">
                        {isLoading ? "..." : (calendarData?.totalDepartmentEmployees ?? "--")} người
                    </p>
                    <p className="mt-1 text-[11px] font-semibold text-blue-600">
                        {includeSubUnits ? "Đã gồm các nhóm/team con" : "Chỉ tính nhân sự trực tiếp"}
                    </p>
                </div>

                <div className="rounded-2xl border border-purple-200 bg-purple-50/60 p-4">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-bold uppercase tracking-wider text-purple-800">
                            Khoảng nghỉ tháng
                        </span>
                        <FileText className="size-4 text-purple-600" />
                    </div>
                    <p className="mt-2 text-2xl font-black text-purple-950">
                        {isLoading ? "..." : (calendarData?.totalLeaveRequests ?? "--")} đơn
                    </p>
                    <p className="mt-1 text-[11px] font-semibold text-purple-600">
                        Đang chờ duyệt hoặc đã duyệt
                    </p>
                </div>

                <div className={cn(
                    "rounded-2xl border p-4 transition",
                    (calendarData?.warningDaysCount ?? 0) > 0
                        ? "border-rose-300 bg-rose-50/80 shadow-xs"
                        : "border-slate-200 bg-white"
                )}>
                    <div className="flex items-center justify-between">
                        <span className={cn(
                            "text-xs font-bold uppercase tracking-wider",
                            (calendarData?.warningDaysCount ?? 0) > 0 ? "text-rose-800" : "text-slate-600"
                        )}>
                            Ngày vượt ngưỡng
                        </span>
                        <AlertTriangle className={cn(
                            "size-4",
                            (calendarData?.warningDaysCount ?? 0) > 0 ? "text-rose-600" : "text-slate-400"
                        )} />
                    </div>
                    <p className={cn(
                        "mt-2 text-2xl font-black",
                        (calendarData?.warningDaysCount ?? 0) > 0 ? "text-rose-950" : "text-slate-900"
                    )}>
                        {isLoading ? "..." : (calendarData?.warningDaysCount ?? 0)} ngày
                    </p>
                    <p className={cn(
                        "mt-1 text-[11px] font-semibold",
                        (calendarData?.warningDaysCount ?? 0) > 0 ? "text-rose-600" : "text-slate-400"
                    )}>
                        {(calendarData?.warningDaysCount ?? 0) > 0
                            ? "Nguy cơ thiếu hụt năng lực làm việc"
                            : "Nguồn lực ổn định"}
                    </p>
                </div>

                <div className="rounded-2xl border border-indigo-200 bg-indigo-50/60 p-4">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-bold uppercase tracking-wider text-indigo-800">
                            Ngưỡng an toàn
                        </span>
                        <CalendarDays className="size-4 text-indigo-600" />
                    </div>
                    <p className="mt-2 text-2xl font-black text-indigo-950">
                        {((calendarData?.warningThresholdPercentage ?? warningThreshold) * 100).toFixed(0)}%
                    </p>
                    <p className="mt-1 text-[11px] font-semibold text-indigo-600">
                        Báo động khi số người nghỉ ≥ ngưỡng
                    </p>
                </div>
            </div>

            {/* Banner Cảnh Báo Nếu Có Ngày Vượt Ngưỡng (TC-02) */}
            {warningDays.length > 0 && (
                <div className="rounded-2xl border border-amber-300 bg-amber-50/90 p-4 text-xs shadow-xs animate-in fade-in duration-200">
                    <div className="flex items-center gap-2 font-bold text-amber-900">
                        <AlertTriangle className="size-4 text-amber-600 shrink-0" />
                        <span>Cảnh báo nguy cơ thiếu hụt nhân sự trong tháng {month}/{year}:</span>
                    </div>
                    <div className="mt-2 flex flex-wrap gap-2">
                        {warningDays.map((day) => (
                            <button
                                key={day.date}
                                type="button"
                                onClick={() => setSelectedDayDetail(day)}
                                className="inline-flex items-center gap-1.5 rounded-lg border border-amber-300 bg-white px-2.5 py-1 text-xs font-semibold text-amber-900 hover:bg-amber-100/60 transition cursor-pointer shadow-2xs"
                            >
                                <span className="font-mono font-bold">{day.date.slice(8, 10)}/{day.date.slice(5, 7)}:</span>
                                <span>{day.totalOnLeave} người nghỉ ({day.totalLeaveHours}h)</span>
                            </button>
                        ))}
                    </div>
                </div>
            )}

            {/* Lỗi hiển thị nếu có */}
            {error && (
                <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-xs font-semibold text-rose-700 flex items-center gap-2">
                    <AlertTriangle className="size-4 text-rose-600 shrink-0" />
                    <span>{error}</span>
                </div>
            )}

            {/* Lưới Lịch Tháng (Monthly Calendar Grid) */}
            <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xs">
                {/* Header Ngày Trong Tuần (7 cột) */}
                <div className="grid grid-cols-7 border-b border-slate-200 bg-slate-50 text-center text-[11px] font-bold uppercase tracking-wider text-slate-600">
                    {WEEKDAY_NAMES.map((name, idx) => (
                        <div
                            key={name}
                            className={cn(
                                "py-3 px-2",
                                idx >= 5 && "text-slate-400 bg-slate-100/50"
                            )}
                        >
                            {name} {idx >= 5 ? "(Nghỉ)" : ""}
                        </div>
                    ))}
                </div>

                {/* Các ô ngày trên lịch */}
                <div className="grid grid-cols-7 divide-x divide-y divide-slate-100">
                    {/* Ô trống đầu tháng */}
                    {Array.from({ length: blankLeadingDays }).map((_, index) => (
                        <div key={`blank-${index}`} className="min-h-[120px] bg-slate-50/40 p-2" />
                    ))}

                    {/* Dữ liệu từng ngày trong tháng */}
                    {calendarData?.dailySummaries.map((summary) => {
                        const dayNum = Number(summary.date.slice(8, 10));
                        const hasLeaves = summary.totalOnLeave > 0;
                        const isWeekend = !summary.isWorkingDay && !summary.isHoliday;

                        return (
                            <div
                                key={summary.date}
                                onClick={() => hasLeaves && setSelectedDayDetail(summary)}
                                className={cn(
                                    "min-h-[125px] p-2 transition flex flex-col justify-between",
                                    hasLeaves ? "cursor-pointer hover:bg-indigo-50/30" : "",
                                    isWeekend ? "bg-slate-50/60" : "bg-white",
                                    summary.isHoliday ? "bg-rose-50/30 border-rose-200" : "",
                                    summary.isWarning ? "border-2 border-amber-500 bg-amber-50/50 ring-1 ring-amber-300" : ""
                                )}
                            >
                                {/* Hàng đầu của ô: Ngày & Huy hiệu */}
                                <div className="flex items-start justify-between gap-1">
                                    <span
                                        className={cn(
                                            "inline-flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold",
                                            summary.isWarning
                                                ? "bg-amber-500 text-white shadow-xs"
                                                : isWeekend
                                                ? "text-slate-400 font-semibold"
                                                : "text-slate-800"
                                        )}
                                    >
                                        {dayNum}
                                    </span>

                                    <div className="flex flex-col items-end gap-1">
                                        {/* Huy hiệu Ngày lễ (P1) */}
                                        {summary.isHoliday && (
                                            <span className="rounded bg-rose-100 px-1.5 py-0.5 text-[9px] font-bold text-rose-700 border border-rose-200">
                                                Lễ công ty
                                            </span>
                                        )}

                                        {/* Huy hiệu Cảnh báo quá tải nghỉ (TC-02) */}
                                        {summary.isWarning && (
                                            <span
                                                className="inline-flex items-center gap-0.5 rounded bg-amber-100 px-1.5 py-0.5 text-[9px] font-extrabold text-amber-800 border border-amber-300"
                                                title={summary.warningMessage || "Vượt ngưỡng an toàn"}
                                            >
                                                <AlertTriangle className="size-2.5 text-amber-700" />
                                                Vượt ngưỡng
                                            </span>
                                        )}
                                    </div>
                                </div>

                                {/* Thông tin tổng quan ngày nghỉ: Số người & Tổng giờ (P2) */}
                                {hasLeaves ? (
                                    <div className="mt-1 space-y-1">
                                        <div className="flex items-center justify-between text-[10px] font-bold">
                                            <span className={cn(
                                                "rounded-md px-1.5 py-0.5",
                                                summary.isWarning
                                                    ? "bg-amber-200/80 text-amber-950 font-extrabold"
                                                    : "bg-indigo-50 text-indigo-700"
                                            )}>
                                                {summary.totalOnLeave} người nghỉ
                                            </span>

                                            {/* Tổng giờ nghỉ trong ngày (P2) */}
                                            {summary.totalLeaveHours > 0 && (
                                                <span className="font-mono text-slate-600 bg-slate-100 px-1 rounded">
                                                    {summary.totalLeaveHours}h
                                                </span>
                                            )}
                                        </div>

                                        {/* Danh sách chip nhân sự nghỉ trong ngày (tối đa 2 người hiển thị trực tiếp) */}
                                        <div className="space-y-0.5">
                                            {summary.leaveItems.slice(0, 2).map((item) => (
                                                <div
                                                    key={`${item.leaveRequestId}-${item.employeeId}`}
                                                    className="truncate rounded border border-slate-200 bg-white/90 px-1.5 py-0.5 text-[10px] text-slate-700 shadow-2xs flex items-center justify-between gap-1"
                                                    title={`${item.fullName} (${item.employeeCode}) - ${LEAVE_TYPE_LABELS[item.leaveType] || item.leaveType}`}
                                                >
                                                    <span className="truncate font-semibold">{item.fullName}</span>
                                                    <span className={cn(
                                                        "h-1.5 w-1.5 rounded-full shrink-0",
                                                        item.status === "APPROVED" ? "bg-emerald-500" : "bg-amber-500"
                                                    )} />
                                                </div>
                                            ))}
                                            {summary.leaveItems.length > 2 && (
                                                <p className="text-[9px] font-bold text-slate-400 pl-1">
                                                    +{summary.leaveItems.length - 2} người khác...
                                                </p>
                                            )}
                                        </div>
                                    </div>
                                ) : (
                                    <div className="h-8" />
                                )}

                                {/* Chân ô ngày: Tình trạng phân bổ */}
                                <div className="mt-1 flex items-center justify-between text-[9px] text-slate-400">
                                    {hasLeaves && (
                                        <span>
                                            {summary.approvedCount > 0 && (
                                                <span className="text-emerald-600 font-semibold mr-1">
                                                    ✓ {summary.approvedCount}
                                                </span>
                                            )}
                                            {summary.pendingCount > 0 && (
                                                <span className="text-amber-600 font-semibold">
                                                    ⏳ {summary.pendingCount}
                                                </span>
                                            )}
                                        </span>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>
            </div>

            {/* Modal Xem Chi Tiết Ngày Nghỉ (Day Detail Dialog) */}
            {selectedDayDetail && (
                <div
                    className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4"
                    role="dialog"
                    aria-modal="true"
                >
                    <div className="w-full max-w-xl rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
                        {/* Header Modal */}
                        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                            <div className="flex items-center gap-2.5">
                                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-700 font-black">
                                    {selectedDayDetail.date.slice(8, 10)}
                                </div>
                                <div>
                                    <h3 className="text-sm font-bold text-slate-900">
                                        Chi tiết lịch nghỉ ngày {selectedDayDetail.date}
                                    </h3>
                                    <p className="text-[11px] text-slate-500 font-medium">
                                        {selectedDayDetail.dayOfWeek} • {calendarData?.orgUnitName}
                                    </p>
                                </div>
                            </div>

                            <button
                                type="button"
                                onClick={() => setSelectedDayDetail(null)}
                                className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition cursor-pointer"
                            >
                                <X className="size-5" />
                            </button>
                        </div>

                        {/* Thống kê nhanh trong ngày */}
                        <div className="mt-4 grid grid-cols-3 gap-3 text-center">
                            <div className="rounded-xl border border-slate-200 bg-slate-50 p-2.5">
                                <p className="text-[10px] font-bold text-slate-400 uppercase">Tổng nhân sự nghỉ</p>
                                <p className="text-base font-black text-slate-900">{selectedDayDetail.totalOnLeave} người</p>
                            </div>
                            <div className="rounded-xl border border-slate-200 bg-slate-50 p-2.5">
                                <p className="text-[10px] font-bold text-slate-400 uppercase">Tổng giờ nghỉ (P2)</p>
                                <p className="text-base font-black text-indigo-600">{selectedDayDetail.totalLeaveHours} giờ</p>
                            </div>
                            <div className="rounded-xl border border-slate-200 bg-slate-50 p-2.5">
                                <p className="text-[10px] font-bold text-slate-400 uppercase">Trạng thái duyệt</p>
                                <p className="text-xs font-bold text-slate-700 mt-1">
                                    <span className="text-emerald-600">{selectedDayDetail.approvedCount} đã duyệt</span> •{" "}
                                    <span className="text-amber-600">{selectedDayDetail.pendingCount} chờ</span>
                                </p>
                            </div>
                        </div>

                        {/* Cảnh báo nếu có */}
                        {selectedDayDetail.isWarning && (
                            <div className="mt-3 rounded-xl border border-amber-300 bg-amber-50 p-3 text-xs text-amber-800 flex items-start gap-2">
                                <AlertTriangle className="size-4 text-amber-600 shrink-0 mt-0.5" />
                                <div>
                                    <p className="font-bold">Cảnh báo thiếu hụt nhân sự:</p>
                                    <p className="mt-0.5 leading-relaxed">{selectedDayDetail.warningMessage}</p>
                                </div>
                            </div>
                        )}

                        {/* Danh sách chi tiết các đơn nghỉ */}
                        <div className="mt-4 max-h-[300px] overflow-y-auto space-y-2 pr-1">
                            {selectedDayDetail.leaveItems.map((item) => (
                                <div
                                    key={`${item.leaveRequestId}-${item.employeeId}`}
                                    className="rounded-xl border border-slate-200 bg-white p-3 shadow-2xs hover:border-slate-300 transition text-xs"
                                >
                                    <div className="flex items-center justify-between">
                                        <div className="flex items-center gap-2">
                                            <span className="font-bold text-slate-900">{item.fullName}</span>
                                            <span className="font-mono text-[10px] text-slate-400 font-semibold">
                                                {item.employeeCode}
                                            </span>
                                        </div>
                                        <span className={cn(
                                            "rounded-md border px-2 py-0.5 text-[10px] font-bold uppercase",
                                            item.status === "APPROVED"
                                                ? "bg-emerald-50 text-emerald-700 border-emerald-200"
                                                : "bg-amber-50 text-amber-700 border-amber-200"
                                        )}>
                                            {item.status === "APPROVED" ? "Đã duyệt" : "Chờ phê duyệt"}
                                        </span>
                                    </div>

                                    <div className="mt-2 flex flex-wrap items-center gap-3 text-[11px] text-slate-500">
                                        <span className={cn("rounded border px-2 py-0.5 font-semibold", LEAVE_TYPE_BADGES[item.leaveType])}>
                                            {LEAVE_TYPE_LABELS[item.leaveType] || item.leaveType}
                                        </span>
                                        <span className="font-mono">
                                            Thời gian: {item.startDate} → {item.endDate}
                                        </span>
                                        <span className="font-bold text-slate-700">
                                            {item.hoursDeducted}h nghỉ
                                        </span>
                                    </div>

                                    {item.reason && (
                                        <p className="mt-1.5 text-[11px] text-slate-600 bg-slate-50 rounded-lg p-2 italic">
                                            Lý do: {item.reason}
                                        </p>
                                    )}
                                </div>
                            ))}
                        </div>

                        {/* Nút đóng */}
                        <div className="mt-4 flex justify-end border-t border-slate-100 pt-3">
                            <button
                                type="button"
                                onClick={() => setSelectedDayDetail(null)}
                                className="rounded-xl bg-slate-900 px-4 py-2 text-xs font-bold text-white hover:bg-slate-800 transition cursor-pointer shadow-xs"
                            >
                                Đóng
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
