"use client";

import { useState, useEffect, useMemo, useCallback } from "react";
import {
    Activity,
    AlertTriangle,
    ArrowUpRight,
    Building2,
    ChevronRight,
    Clock,
    FolderKanban,
    RefreshCw,
    TrendingUp,
    Zap,
    CheckCircle2,
    SlidersHorizontal,
} from "lucide-react";
import {
    getCapacityDashboard,
    type CapacityDashboardResult,
} from "@/lib/api/capacity-dashboard";
import { getOrgTree } from "@/lib/api/org-units";
import { flattenActiveOrgTree } from "@/lib/organization";
import { getCurrentIsoWeek } from "../availability/availability.types";
import { cn } from "@/lib/utils";

interface CapacityDashboardViewProps {
    onNavigate?: (tabId: string) => void;
}

const DURATION_OPTIONS = [
    { label: "4 tuần tới", value: 4 },
    { label: "8 tuần tới (Chuẩn)", value: 8 },
    { label: "12 tuần tới (Quý)", value: 12 },
    { label: "16 tuần tới", value: 16 },
];

export default function CapacityDashboardView({ onNavigate }: CapacityDashboardViewProps) {
    const currentIso = useMemo(() => getCurrentIsoWeek(), []);

    // Filters state
    const [fromYear, setFromYear] = useState<number>(currentIso.year);
    const [fromWeek, setFromWeek] = useState<number>(currentIso.weekNumber);
    const [durationWeeks, setDurationWeeks] = useState<number>(8);
    const [selectedOrgUnitId, setSelectedOrgUnitId] = useState<number | undefined>(undefined);

    // Data state
    const [data, setData] = useState<CapacityDashboardResult | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [orgUnits, setOrgUnits] = useState<{ id: number; name: string }[]>([]);

    // Fetch org units for filter dropdown
    useEffect(() => {
        async function loadOrgUnits() {
            try {
                const tree = await getOrgTree();
                const flat = flattenActiveOrgTree(tree);
                setOrgUnits(flat.map((u) => ({ id: u.id, name: u.unitName })));
            } catch {
                // Ignore if cannot load org tree
            }
        }
        loadOrgUnits();
    }, []);

    // Main fetch dashboard data
    const loadDashboardData = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await getCapacityDashboard({
                orgUnitId: selectedOrgUnitId,
                fromYear,
                fromWeek,
                durationWeeks,
            });
            setData(res);
        } catch (err: any) {
            console.error("Failed to load capacity dashboard:", err);
            setError(err?.message || "Không thể tải dữ liệu bảng điều khiển năng lực");
        } finally {
            setLoading(false);
        }
    }, [selectedOrgUnitId, fromYear, fromWeek, durationWeeks]);

    useEffect(() => {
        loadDashboardData();
    }, [loadDashboardData]);

    const handleNavigate = (tabId: string) => {
        if (onNavigate) {
            onNavigate(tabId);
        }
    };

    // Calculate metrics
    const avgUtil = data?.averageCapacityUtilization ?? 0;
    const overloadedCount = data?.overloadedEmployeesCount ?? 0;
    const freeHours = data?.departmentFreeHours ?? 0;
    const conflictCount = data?.unresolvedScheduleConflictsCount ?? 0;
    const activePrjCount = data?.activeProjectsCount ?? 0;
    const totalAvail = data?.totalAvailableHours ?? 0;
    const totalAlloc = data?.totalAllocatedHours ?? 0;

    const getUtilizationColor = (util: number) => {
        if (util > 100) return { text: "text-rose-600", bg: "bg-rose-50", border: "border-rose-200", badge: "bg-rose-100 text-rose-800", label: "Quá tải" };
        if (util >= 75) return { text: "text-emerald-600", bg: "bg-emerald-50", border: "border-emerald-200", badge: "bg-emerald-100 text-emerald-800", label: "Tối ưu" };
        if (util > 0) return { text: "text-amber-600", bg: "bg-amber-50", border: "border-amber-200", badge: "bg-amber-100 text-amber-800", label: "Dư năng lực" };
        return { text: "text-slate-500", bg: "bg-slate-50", border: "border-slate-200", badge: "bg-slate-100 text-slate-700", label: "Chưa có phân bổ" };
    };

    const utilStyle = getUtilizationColor(avgUtil);

    return (
        <div className="space-y-5 pb-10">
            {/* Header section */}
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-4">
                <div>
                    <div className="flex items-center gap-2.5">
                        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-600 text-white shadow-xs">
                            <Activity className="h-5 w-5" />
                        </div>
                        <div>
                            <div className="flex items-center gap-2">
                                <h1 className="text-xl font-bold tracking-tight text-slate-900">
                                    Bảng Điều Khiển Năng Lực (Capacity Dashboard)
                                </h1>
                                <span className="inline-flex items-center gap-1 rounded-md bg-indigo-50 border border-indigo-200 px-2 py-0.5 text-[11px] font-bold text-indigo-700">
                                    <Zap className="h-3 w-3" /> NCL-10-CN-001
                                </span>
                            </div>
                            <p className="text-xs text-slate-500 mt-0.5">
                                Nắm bắt bức tranh tổng quan nguồn lực, công suất, cảnh báo quá tải và xung đột lịch theo thời gian thực.
                            </p>
                        </div>
                    </div>
                </div>

                {/* Top Action Buttons */}
                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={loadDashboardData}
                        disabled={loading}
                        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-2xs cursor-pointer disabled:opacity-50"
                        title="Tải lại dữ liệu"
                    >
                        <RefreshCw className={cn("h-3.5 w-3.5", loading ? "animate-spin text-indigo-600" : "text-slate-500")} />
                        <span>{loading ? "Đang tải..." : "Làm mới"}</span>
                    </button>
                </div>
            </div>

            {/* Filter Toolbar */}
            <div className="rounded-2xl border border-slate-200 bg-white p-3.5 shadow-2xs">
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="flex flex-wrap items-center gap-3">
                        <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700">
                            <SlidersHorizontal className="h-4 w-4 text-indigo-600" />
                            <span>Bộ lọc kỳ:</span>
                        </div>

                        {/* Start Week Picker */}
                        <div className="flex items-center gap-1.5">
                            <label className="text-[11px] font-medium text-slate-500">Từ tuần:</label>
                            <input
                                type="number"
                                min={1}
                                max={53}
                                value={fromWeek}
                                onChange={(e) => setFromWeek(Math.max(1, Math.min(53, Number(e.target.value) || 1)))}
                                className="w-16 rounded-lg border border-slate-200 bg-slate-50/70 px-2 py-1 text-xs font-semibold text-slate-800 focus:bg-white focus:border-indigo-500 focus:outline-hidden"
                            />
                            <span className="text-xs text-slate-400">/</span>
                            <input
                                type="number"
                                min={2020}
                                max={2030}
                                value={fromYear}
                                onChange={(e) => setFromYear(Number(e.target.value) || currentIso.year)}
                                className="w-20 rounded-lg border border-slate-200 bg-slate-50/70 px-2 py-1 text-xs font-semibold text-slate-800 focus:bg-white focus:border-indigo-500 focus:outline-hidden"
                            />
                        </div>

                        {/* Duration Options */}
                        <div className="flex items-center gap-1.5">
                            <label className="text-[11px] font-medium text-slate-500">Khoảng thời gian:</label>
                            <div className="flex rounded-lg border border-slate-200 bg-slate-100/80 p-0.5">
                                {DURATION_OPTIONS.map((opt) => (
                                    <button
                                        key={opt.value}
                                        type="button"
                                        onClick={() => setDurationWeeks(opt.value)}
                                        className={cn(
                                            "rounded-md px-2.5 py-1 text-xs font-semibold transition cursor-pointer",
                                            durationWeeks === opt.value
                                                ? "bg-white text-indigo-700 shadow-2xs"
                                                : "text-slate-600 hover:text-slate-900"
                                        )}
                                    >
                                        {opt.label}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {/* OrgUnit Filter */}
                        <div className="flex items-center gap-1.5">
                            <label className="text-[11px] font-medium text-slate-500">Phòng ban:</label>
                            <select
                                value={selectedOrgUnitId ?? ""}
                                onChange={(e) => setSelectedOrgUnitId(e.target.value ? Number(e.target.value) : undefined)}
                                className="rounded-lg border border-slate-200 bg-slate-50/70 px-2.5 py-1 text-xs font-semibold text-slate-800 focus:bg-white focus:border-indigo-500 focus:outline-hidden cursor-pointer"
                            >
                                <option value="">Toàn công ty</option>
                                {orgUnits.map((u) => (
                                    <option key={u.id} value={u.id}>
                                        {u.name}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>

                    {/* Scope info tag */}
                    <div className="text-[11px] font-medium text-slate-400">
                        Phạm vi: <span className="font-bold text-slate-700">{data?.orgUnitName || "Toàn công ty"}</span> · Kỳ {durationWeeks} tuần từ T{fromWeek}/{fromYear}
                    </div>
                </div>
            </div>

            {error && (
                <div className="flex items-center gap-2.5 rounded-xl border border-rose-200 bg-rose-50 p-3.5 text-xs text-rose-700">
                    <AlertTriangle className="h-4 w-4 shrink-0 text-rose-600" />
                    <span>{error}</span>
                </div>
            )}

            {/* 5 HERO KPI CARDS */}
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-5">
                {/* KPI 1: Tỷ lệ sử dụng năng lực trung bình */}
                <div
                    onClick={() => handleNavigate("capacity")}
                    className="group relative cursor-pointer rounded-2xl border border-slate-200 bg-white p-3.5 shadow-2xs transition hover:border-indigo-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className={cn("flex h-8 w-8 items-center justify-center rounded-xl border", utilStyle.bg, utilStyle.border, utilStyle.text)}>
                            <TrendingUp className="h-4 w-4" />
                        </div>
                        <span className={cn("rounded-md px-1.5 py-0.5 text-[10px] font-bold", utilStyle.badge)}>
                            {utilStyle.label}
                        </span>
                    </div>
                    <div className="mt-2.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Hiệu suất sử dụng
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-2xl font-bold text-slate-900">{avgUtil}%</span>
                            <span className="text-[10px] text-slate-400">công suất</span>
                        </div>
                        <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
                            <div
                                className={cn("h-full transition-all duration-500", avgUtil > 100 ? "bg-rose-500" : avgUtil >= 75 ? "bg-emerald-500" : "bg-amber-500")}
                                style={{ width: `${Math.min(100, avgUtil)}%` }}
                            />
                        </div>
                        <div className="mt-1.5 flex items-center justify-between text-[10px] text-slate-400">
                            <span>Phân bổ: {totalAlloc.toLocaleString("vi-VN")}h</span>
                            <span>Khả dụng: {totalAvail.toLocaleString("vi-VN")}h</span>
                        </div>
                    </div>
                </div>

                {/* KPI 2: Số người quá tải */}
                <div
                    onClick={() => handleNavigate("capacity")}
                    className="group relative cursor-pointer rounded-2xl border border-slate-200 bg-white p-3.5 shadow-2xs transition hover:border-rose-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className={cn(
                            "flex h-8 w-8 items-center justify-center rounded-xl border",
                            overloadedCount > 0 ? "bg-rose-50 border-rose-200 text-rose-600" : "bg-emerald-50 border-emerald-200 text-emerald-600"
                        )}>
                            <AlertTriangle className="h-4 w-4" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-rose-600 group-hover:translate-x-0.5 transition">
                            Chi tiết <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-2.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Nhân sự quá tải
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className={cn("text-2xl font-bold", overloadedCount > 0 ? "text-rose-600" : "text-slate-900")}>
                                {overloadedCount}
                            </span>
                            <span className="text-[10px] text-slate-400">nhân viên</span>
                        </div>
                        <div className="mt-2 text-[10px] font-medium">
                            {overloadedCount > 0 ? (
                                <span className="text-rose-600 font-bold">Cần điều phối & san tải ngay</span>
                            ) : (
                                <span className="text-emerald-600 font-semibold">Tất cả nhân sự trong ngưỡng</span>
                            )}
                        </div>
                    </div>
                </div>

                {/* KPI 3: Số giờ còn rảnh */}
                <div
                    onClick={() => handleNavigate("capacity")}
                    className="group relative cursor-pointer rounded-2xl border border-slate-200 bg-white p-3.5 shadow-2xs transition hover:border-blue-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-8 w-8 items-center justify-center rounded-xl border border-blue-200 bg-blue-50 text-blue-600">
                            <Clock className="h-4 w-4" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-blue-600 group-hover:translate-x-0.5 transition">
                            Dự phòng <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-2.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Số giờ còn rảnh
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-2xl font-bold text-slate-900">
                                {freeHours.toLocaleString("vi-VN")}
                            </span>
                            <span className="text-[10px] text-slate-400">giờ</span>
                        </div>
                        <div className="mt-2 text-[10px] font-medium text-blue-700 truncate">
                            Năng lực sẵn sàng nhận dự án
                        </div>
                    </div>
                </div>

                {/* KPI 4: Xung đột lịch chưa xử lý */}
                <div
                    onClick={() => handleNavigate("schedule-conflict")}
                    className="group relative cursor-pointer rounded-2xl border border-slate-200 bg-white p-3.5 shadow-2xs transition hover:border-amber-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className={cn(
                            "flex h-8 w-8 items-center justify-center rounded-xl border",
                            conflictCount > 0 ? "bg-amber-50 border-amber-200 text-amber-600" : "bg-emerald-50 border-emerald-200 text-emerald-600"
                        )}>
                            <AlertTriangle className="h-4 w-4" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-amber-600 group-hover:translate-x-0.5 transition">
                            Xử lý <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-2.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Xung đột lịch chưa xử lý
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className={cn("text-2xl font-bold", conflictCount > 0 ? "text-amber-600" : "text-slate-900")}>
                                {conflictCount}
                            </span>
                            <span className="text-[10px] text-slate-400">vụ việc</span>
                        </div>
                        <div className="mt-2 text-[10px] font-medium">
                            {conflictCount > 0 ? (
                                <span className="text-amber-700 font-bold">Chồng lấn lịch & nghỉ phép</span>
                            ) : (
                                <span className="text-emerald-600 font-semibold">Không có xung đột</span>
                            )}
                        </div>
                    </div>
                </div>

                {/* KPI 5: Số dự án đang chạy */}
                <div
                    onClick={() => handleNavigate("project")}
                    className="group relative cursor-pointer rounded-2xl border border-slate-200 bg-white p-3.5 shadow-2xs transition hover:border-emerald-300 hover:shadow-xs"
                >
                    <div className="flex items-center justify-between">
                        <div className="flex h-8 w-8 items-center justify-center rounded-xl border border-emerald-200 bg-emerald-50 text-emerald-600">
                            <FolderKanban className="h-4 w-4" />
                        </div>
                        <span className="inline-flex items-center gap-0.5 text-[10px] font-semibold text-emerald-600 group-hover:translate-x-0.5 transition">
                            Dự án <ArrowUpRight className="h-2.5 w-2.5" />
                        </span>
                    </div>
                    <div className="mt-2.5">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            Dự án đang chạy
                        </p>
                        <div className="mt-0.5 flex items-baseline gap-1">
                            <span className="text-2xl font-bold text-slate-900">
                                {activePrjCount}
                            </span>
                            <span className="text-[10px] text-slate-400">dự án ACTIVE</span>
                        </div>
                        <div className="mt-2 text-[10px] font-medium text-emerald-700">
                            Đang trong giai đoạn triển khai
                        </div>
                    </div>
                </div>
            </div>

            {/* WEEKLY CAPACITY TREND BAR CHART */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                <div className="flex flex-wrap items-center justify-between gap-2 mb-4">
                    <div>
                        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                            Xu hướng Năng lực & Mức độ Sử dụng theo Tuần ({durationWeeks} tuần)
                        </h3>
                        <p className="text-[11px] text-slate-400 mt-0.5">
                            So sánh giờ phân bổ thực tế so với năng lực khả dụng ròng từng tuần
                        </p>
                    </div>
                    <div className="flex items-center gap-3 text-[11px]">
                        <div className="flex items-center gap-1.5">
                            <span className="h-2.5 w-2.5 rounded-xs bg-indigo-600" />
                            <span className="text-slate-600">Phân bổ</span>
                        </div>
                        <div className="flex items-center gap-1.5">
                            <span className="h-2.5 w-2.5 rounded-xs bg-slate-200" />
                            <span className="text-slate-600">Khả dụng</span>
                        </div>
                        <div className="flex items-center gap-1.5">
                            <span className="h-2.5 w-2.5 rounded-xs bg-rose-500" />
                            <span className="text-slate-600">Quá tải</span>
                        </div>
                    </div>
                </div>

                {/* Grid of Weeks */}
                <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-2.5">
                    {data?.weeklyMetrics?.map((w) => {
                        const wUtil = w.utilizationRate || 0;
                        const isOver = wUtil > 100 || w.overloadedEmployeesCount > 0;
                        const isOpt = wUtil >= 75 && wUtil <= 100;
                        return (
                            <div
                                key={`${w.year}-${w.weekNumber}`}
                                className={cn(
                                    "rounded-xl border p-2.5 transition hover:shadow-xs",
                                    isOver
                                        ? "border-rose-200 bg-rose-50/40"
                                        : isOpt
                                        ? "border-emerald-200 bg-emerald-50/30"
                                        : "border-slate-200 bg-slate-50/60"
                                )}
                            >
                                <div className="flex items-center justify-between">
                                    <span className="text-xs font-bold text-slate-900">T{w.weekNumber}</span>
                                    <span
                                        className={cn(
                                            "rounded-md px-1.5 py-0.5 text-[10px] font-bold",
                                            isOver
                                                ? "bg-rose-100 text-rose-800"
                                                : isOpt
                                                ? "bg-emerald-100 text-emerald-800"
                                                : "bg-slate-100 text-slate-700"
                                        )}
                                    >
                                        {wUtil}%
                                    </span>
                                </div>
                                <p className="text-[9px] text-slate-400 mt-0.5">
                                    {w.startDate?.slice(5)} - {w.endDate?.slice(5)}
                                </p>

                                <div className="mt-2 space-y-1 text-[10px]">
                                    <div className="flex justify-between">
                                        <span className="text-slate-400">Đã gán:</span>
                                        <span className="font-semibold text-slate-800">{w.allocatedHours}h</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-slate-400">Khả dụng:</span>
                                        <span className="font-medium text-slate-600">{w.availableHours}h</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-slate-400">Còn rảnh:</span>
                                        <span className="font-medium text-blue-600">{w.remainingHours}h</span>
                                    </div>
                                </div>

                                {w.overloadedEmployeesCount > 0 && (
                                    <div className="mt-2 flex items-center gap-1 rounded bg-rose-100/80 px-1.5 py-0.5 text-[9px] font-bold text-rose-700">
                                        <AlertTriangle className="h-2.5 w-2.5" />
                                        <span>{w.overloadedEmployeesCount} người quá tải</span>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            </div>

            {/* DEPARTMENT CAPACITY BREAKDOWN */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                <div className="flex items-center justify-between mb-3">
                    <div>
                        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                            Cơ cấu Năng lực theo Phòng ban / Bộ phận
                        </h3>
                        <p className="text-[11px] text-slate-400 mt-0.5">
                            Phân bổ tải và số giờ rảnh theo từng đơn vị
                        </p>
                    </div>
                    <button
                        type="button"
                        onClick={() => handleNavigate("departments")}
                        className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition cursor-pointer"
                    >
                        Xem cây tổ chức →
                    </button>
                </div>

                <div className="overflow-x-auto rounded-xl border border-slate-100">
                    <table className="w-full text-left text-xs border-collapse">
                        <thead>
                            <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                                <th className="px-4 py-2.5">Đơn vị / Phòng ban</th>
                                <th className="px-4 py-2.5 text-center">Quy mô</th>
                                <th className="px-4 py-2.5 text-right">Giờ phân bổ</th>
                                <th className="px-4 py-2.5 text-right">Giờ khả dụng</th>
                                <th className="px-4 py-2.5 text-right">Giờ còn rảnh</th>
                                <th className="px-4 py-2.5 text-center">Tỷ lệ tải</th>
                                <th className="px-4 py-2.5 text-center">Quá tải</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {!data?.departmentBreakdown || data.departmentBreakdown.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="py-6 text-center text-slate-400 text-xs">
                                        Chưa có dữ liệu phòng ban nào trong phạm vi đã chọn.
                                    </td>
                                </tr>
                            ) : (
                                data.departmentBreakdown.map((dept, idx) => {
                                    const dUtil = dept.utilizationRate || 0;
                                    const isOver = dUtil > 100 || dept.overloadedEmployeesCount > 0;
                                    return (
                                        <tr key={dept.orgUnitId ?? idx} className="hover:bg-slate-50/70 transition">
                                            <td className="px-4 py-2.5 font-semibold text-slate-900">
                                                <div className="flex items-center gap-2">
                                                    <Building2 className="h-3.5 w-3.5 text-slate-400" />
                                                    <span>{dept.orgUnitName}</span>
                                                </div>
                                            </td>
                                            <td className="px-4 py-2.5 text-center font-medium text-slate-700">
                                                <span className="rounded-md bg-slate-100 px-1.5 py-0.5 text-[10px] font-bold text-slate-700">
                                                    {dept.employeeCount} nhân sự
                                                </span>
                                            </td>
                                            <td className="px-4 py-2.5 text-right font-medium text-slate-800">
                                                {dept.allocatedHours?.toLocaleString("vi-VN")}h
                                            </td>
                                            <td className="px-4 py-2.5 text-right text-slate-600">
                                                {dept.availableHours?.toLocaleString("vi-VN")}h
                                            </td>
                                            <td className="px-4 py-2.5 text-right font-semibold text-blue-600">
                                                {dept.freeHours?.toLocaleString("vi-VN")}h
                                            </td>
                                            <td className="px-4 py-2.5 text-center">
                                                <div className="flex items-center justify-center gap-2">
                                                    <div className="w-16 h-1.5 overflow-hidden rounded-full bg-slate-100">
                                                        <div
                                                            className={cn("h-full", isOver ? "bg-rose-500" : dUtil >= 75 ? "bg-emerald-500" : "bg-amber-500")}
                                                            style={{ width: `${Math.min(100, dUtil)}%` }}
                                                        />
                                                    </div>
                                                    <span className={cn("text-xs font-bold", isOver ? "text-rose-600" : dUtil >= 75 ? "text-emerald-700" : "text-amber-700")}>
                                                        {dUtil}%
                                                    </span>
                                                </div>
                                            </td>
                                            <td className="px-4 py-2.5 text-center">
                                                {dept.overloadedEmployeesCount > 0 ? (
                                                    <span className="inline-flex items-center gap-1 rounded-md bg-rose-50 border border-rose-200 px-2 py-0.5 text-[10px] font-bold text-rose-700">
                                                        <AlertTriangle className="h-2.5 w-2.5" />
                                                        {dept.overloadedEmployeesCount} người
                                                    </span>
                                                ) : (
                                                    <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-600">
                                                        <CheckCircle2 className="h-3 w-3" /> Bình thường
                                                    </span>
                                                )}
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* 2 COLUMNS: OVERLOADED RESOURCES & UNRESOLVED CONFLICTS */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                {/* Column 1: Top Overloaded Resources */}
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                    <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center gap-2">
                            <AlertTriangle className="h-4 w-4 text-rose-600" />
                            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                                Danh sách Nhân sự Quá tải ({data?.overloadedEmployees?.length || 0})
                            </h3>
                        </div>
                        <button
                            type="button"
                            onClick={() => handleNavigate("capacity")}
                            className="text-xs font-semibold text-rose-600 hover:text-rose-800 transition cursor-pointer"
                        >
                            Xem ma trận phân bổ →
                        </button>
                    </div>

                    {!data?.overloadedEmployees || data.overloadedEmployees.length === 0 ? (
                        <div className="flex flex-col items-center justify-center py-10 text-center">
                            <CheckCircle2 className="h-8 w-8 text-emerald-500 mb-2" />
                            <p className="text-xs font-bold text-slate-800">Không có nhân sự nào bị quá tải</p>
                            <p className="text-[11px] text-slate-400 mt-0.5">Tất cả nhân sự đều trong ngưỡng phân bổ an toàn trong kỳ đã chọn.</p>
                        </div>
                    ) : (
                        <div className="space-y-2">
                            {data.overloadedEmployees.slice(0, 5).map((emp) => (
                                <div
                                    key={emp.employeeId}
                                    className="flex items-center justify-between rounded-xl border border-rose-100 bg-rose-50/40 p-2.5 transition hover:bg-rose-50/80"
                                >
                                    <div className="min-w-0 flex-1">
                                        <div className="flex items-center gap-2">
                                            <span className="font-semibold text-xs text-slate-900 truncate">{emp.fullName}</span>
                                            <span className="font-mono text-[10px] text-slate-400">({emp.employeeCode})</span>
                                            <span className="rounded-md bg-rose-100 px-1.5 py-0.2 text-[9px] font-bold text-rose-800">
                                                {emp.overloadedWeeksCount} tuần quá tải
                                            </span>
                                        </div>
                                        <p className="text-[10px] text-slate-500 mt-0.5 truncate">
                                            {emp.professionalRole} · {emp.orgUnitName}
                                        </p>
                                    </div>
                                    <div className="text-right pl-3 shrink-0">
                                        <div className="text-xs font-bold text-rose-600">
                                            {emp.averageUtilizationRate}% tải
                                        </div>
                                        <div className="text-[10px] text-slate-400">
                                            {emp.totalAllocatedHours}h / {emp.totalAvailableHours}h
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Column 2: Unresolved Schedule Conflicts */}
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                    <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center gap-2">
                            <AlertTriangle className="h-4 w-4 text-amber-600" />
                            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                                Cảnh báo Xung đột Lịch Chưa Xử lý ({data?.unresolvedConflicts?.length || 0})
                            </h3>
                        </div>
                        <button
                            type="button"
                            onClick={() => handleNavigate("schedule-conflict")}
                            className="text-xs font-semibold text-amber-600 hover:text-amber-800 transition cursor-pointer"
                        >
                            Quản lý xung đột →
                        </button>
                    </div>

                    {!data?.unresolvedConflicts || data.unresolvedConflicts.length === 0 ? (
                        <div className="flex flex-col items-center justify-center py-10 text-center">
                            <CheckCircle2 className="h-8 w-8 text-emerald-500 mb-2" />
                            <p className="text-xs font-bold text-slate-800">Không có xung đột lịch</p>
                            <p className="text-[11px] text-slate-400 mt-0.5">Không phát hiện sự chồng chéo lịch dự án hay nghỉ phép trong kỳ này.</p>
                        </div>
                    ) : (
                        <div className="space-y-2">
                            {data.unresolvedConflicts.slice(0, 5).map((conf) => (
                                <div
                                    key={conf.conflictId}
                                    className="flex items-center justify-between rounded-xl border border-amber-100 bg-amber-50/40 p-2.5 transition hover:bg-amber-50/80"
                                >
                                    <div className="min-w-0 flex-1">
                                        <div className="flex items-center gap-2">
                                            <span className="font-semibold text-xs text-slate-900 truncate">{conf.employeeName}</span>
                                            <span className="font-mono text-[10px] text-slate-400">({conf.employeeCode})</span>
                                            <span className="rounded-md bg-amber-100 px-1.5 py-0.2 text-[9px] font-bold text-amber-800">
                                                T{conf.weekNumber}/{conf.yearNumber}
                                            </span>
                                        </div>
                                        <p className="text-[10px] text-slate-500 mt-0.5 truncate">
                                            {conf.details || `Chồng lấn ${conf.conflictingProjectsCount} dự án, tổng ${conf.totalAllocatedHours}h`}
                                        </p>
                                    </div>
                                    <div className="text-right pl-3 shrink-0">
                                        <span className="inline-flex items-center rounded-md bg-amber-100 px-2 py-0.5 text-[10px] font-bold text-amber-800">
                                            {conf.status}
                                        </span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>

            {/* ACTIVE PROJECTS PORTFOLIO SUMMARY */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                        <FolderKanban className="h-4 w-4 text-indigo-600" />
                        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                            Danh mục Dự án Chiến lược Đang Hoạt động ({data?.activeProjects?.length || 0})
                        </h3>
                    </div>
                    <button
                        type="button"
                        onClick={() => handleNavigate("project")}
                        className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition cursor-pointer"
                    >
                        Quản lý dự án →
                    </button>
                </div>

                <div className="overflow-x-auto rounded-xl border border-slate-100">
                    <table className="w-full text-left text-xs border-collapse">
                        <thead>
                            <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                                <th className="px-4 py-2.5">Mã & Tên Dự án</th>
                                <th className="px-4 py-2.5">Phòng ban</th>
                                <th className="px-4 py-2.5">PM Quản lý</th>
                                <th className="px-4 py-2.5">Thời gian thực hiện</th>
                                <th className="px-4 py-2.5 text-right">Giờ kế hoạch</th>
                                <th className="px-4 py-2.5 text-center">Nhân sự tham gia</th>
                                <th className="px-4 py-2.5 text-right">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {!data?.activeProjects || data.activeProjects.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="py-6 text-center text-slate-400 text-xs">
                                        Không có dự án nào đang chạy trong phạm vi đã chọn.
                                    </td>
                                </tr>
                            ) : (
                                data.activeProjects.slice(0, 6).map((prj) => (
                                    <tr key={prj.projectId} className="hover:bg-slate-50/70 transition">
                                        <td className="px-4 py-2.5">
                                            <div className="flex items-center gap-2">
                                                <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-indigo-50 text-indigo-600 font-mono text-[10px] font-bold">
                                                    {prj.projectCode.slice(-3)}
                                                </div>
                                                <div>
                                                    <p className="font-semibold text-slate-900 truncate max-w-[200px]">{prj.projectName}</p>
                                                    <p className="font-mono text-[10px] text-slate-400">{prj.projectCode}</p>
                                                </div>
                                            </div>
                                        </td>
                                        <td className="px-4 py-2.5 text-slate-600 text-[11px]">
                                            {prj.orgUnitName}
                                        </td>
                                        <td className="px-4 py-2.5 font-medium text-slate-700">
                                            {prj.pmName}
                                        </td>
                                        <td className="px-4 py-2.5 text-slate-500 text-[11px]">
                                            {prj.startDate || "--"} → {prj.endDate || "--"}
                                        </td>
                                        <td className="px-4 py-2.5 text-right font-semibold text-slate-800">
                                            {prj.estimatedHours ? `${prj.estimatedHours.toLocaleString("vi-VN")}h` : "--"}
                                        </td>
                                        <td className="px-4 py-2.5 text-center">
                                            <span className="rounded-md bg-indigo-50 border border-indigo-100 px-2 py-0.5 text-[10px] font-bold text-indigo-700">
                                                {prj.memberCount} thành viên
                                            </span>
                                        </td>
                                        <td className="px-4 py-2.5 text-right">
                                            <button
                                                type="button"
                                                onClick={() => handleNavigate("project")}
                                                className="inline-flex items-center gap-0.5 text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition cursor-pointer"
                                            >
                                                Xem WBS <ChevronRight className="h-3 w-3" />
                                            </button>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
