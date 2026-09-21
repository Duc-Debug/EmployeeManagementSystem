import { useState, useEffect, useMemo, useCallback } from "react";
import {
    AlertTriangle,
    RefreshCw,
    Search,
    ShieldAlert,
    CheckCircle2,
    Calendar,
    Building2,
    Clock,
    ChevronDown,
    ChevronRight,
    Users,
    FolderKanban,
    Sparkles,
    FileCheck2,
    Loader2,
    Download,
    ChevronLeft,
} from "lucide-react";
import {
    getExpiringOutsourcedContracts,
    scanOutsourcedContractsManually,
    exportOutsourcedContractsToCsv,
    type ExpiringOutsourcedContract,
} from "@/lib/api/outsourced-contracts";
import AcknowledgeContractModal from "./AcknowledgeContractModal";

export default function OutsourcedContractWarningView() {
    const [contracts, setContracts] = useState<ExpiringOutsourcedContract[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [scanning, setScanning] = useState<boolean>(false);
    const [errorMsg, setErrorMsg] = useState<string | null>(null);
    const [successBanner, setSuccessBanner] = useState<string | null>(null);

    // Search and debounce
    const [searchTerm, setSearchTerm] = useState<string>("");
    const [debouncedSearch, setDebouncedSearch] = useState<string>("");

    // Filter by status tab
    const [statusFilter, setStatusFilter] = useState<string>("ALL");
    const [thresholdDays, setThresholdDays] = useState<number>(30);

    // Pagination
    const [currentPage, setCurrentPage] = useState<number>(1);
    const pageSize = 10;

    // Expanded rows
    const [expandedEmployeeIds, setExpandedEmployeeIds] = useState<Set<number>>(new Set());

    // Selected contract for acknowledge modal
    const [selectedContractForAck, setSelectedContractForAck] = useState<ExpiringOutsourcedContract | null>(null);

    // Debounce search input
    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedSearch(searchTerm);
            setCurrentPage(1); // Reset to page 1 on search
        }, 300);
        return () => clearTimeout(timer);
    }, [searchTerm]);

    const loadData = useCallback(async (forceRefresh: boolean = false) => {
        setLoading(true);
        setErrorMsg(null);
        try {
            const res = await getExpiringOutsourcedContracts(thresholdDays, forceRefresh);
            setContracts(res.items || []);
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : "Không thể tải danh sách hợp đồng thuê ngoài.";
            setErrorMsg(msg);
        } finally {
            setLoading(false);
        }
    }, [thresholdDays]);

    useEffect(() => {
        loadData(false);
    }, [loadData]);

    const handleManualScan = async () => {
        setScanning(true);
        setErrorMsg(null);
        setSuccessBanner(null);
        try {
            const res = await scanOutsourcedContractsManually();
            setSuccessBanner(res.details);
            await loadData(true);
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : "Quét rà soát hợp đồng thất bại.";
            setErrorMsg(msg);
        } finally {
            setScanning(false);
        }
    };

    const handleExportCsv = () => {
        if (!contracts.length) return;
        const csv = exportOutsourcedContractsToCsv(filteredContracts);
        const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, "");
        link.href = url;
        link.setAttribute("download", `Bao_cao_hop_dong_thue_ngoai_${dateStr}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    };

    const toggleExpandRow = (empId: number) => {
        setExpandedEmployeeIds((prev) => {
            const next = new Set(prev);
            if (next.has(empId)) next.delete(empId);
            else next.add(empId);
            return next;
        });
    };

    // Filtered data
    const filteredContracts = useMemo(() => {
        return contracts.filter((c) => {
            const matchSearch =
                !debouncedSearch.trim() ||
                c.fullName.toLowerCase().includes(debouncedSearch.toLowerCase()) ||
                c.employeeCode.toLowerCase().includes(debouncedSearch.toLowerCase()) ||
                (c.orgUnitName && c.orgUnitName.toLowerCase().includes(debouncedSearch.toLowerCase()));

            if (!matchSearch) return false;

            if (statusFilter === "EXPIRING_SOON") return c.status === "EXPIRING_SOON";
            if (statusFilter === "EXPIRED") return c.status === "EXPIRED";
            if (statusFilter === "AFFECTED_ONLY") return c.affectedAllocations && c.affectedAllocations.length > 0;

            return true;
        });
    }, [contracts, debouncedSearch, statusFilter]);

    // Paginated items
    const totalPages = Math.max(1, Math.ceil(filteredContracts.length / pageSize));
    const paginatedContracts = useMemo(() => {
        const start = (currentPage - 1) * pageSize;
        return filteredContracts.slice(start, start + pageSize);
    }, [filteredContracts, currentPage, pageSize]);

    // Summary statistics
    const stats = useMemo(() => {
        const total = contracts.length;
        const expiringSoon = contracts.filter((c) => c.status === "EXPIRING_SOON").length;
        const expired = contracts.filter((c) => c.status === "EXPIRED").length;
        const totalAffectedAllocations = contracts.reduce(
            (acc, c) => acc + (c.affectedAllocations ? c.affectedAllocations.length : 0),
            0
        );
        return { total, expiringSoon, expired, totalAffectedAllocations };
    }, [contracts]);

    return (
        <div className="space-y-6">
            {/* Page Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <div className="flex items-center gap-2.5">
                        <div className="rounded-lg bg-indigo-50 p-2 text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-400">
                            <ShieldAlert className="h-6 w-6" />
                        </div>
                        <div>
                            <h1 className="text-xl font-bold text-slate-900 dark:text-white">
                                Theo Dõi Thời Hạn Hợp Đồng Thuê Ngoài
                            </h1>
                            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                                Quản lý rủi ro hết hạn hợp đồng thuê ngoài và giám sát các phân bổ dự án vi phạm quy tắc QTN-21
                            </p>
                        </div>
                    </div>
                </div>

                <div className="flex flex-wrap items-center gap-2.5">
                    <button
                        onClick={handleExportCsv}
                        disabled={loading || filteredContracts.length === 0}
                        className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700 transition"
                    >
                        <Download className="h-4 w-4 text-slate-500" />
                        <span>Xuất CSV</span>
                    </button>

                    <button
                        onClick={() => loadData(true)}
                        disabled={loading || scanning}
                        className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700 transition"
                    >
                        <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin text-indigo-600" : ""}`} />
                        <span>Làm mới</span>
                    </button>

                    <button
                        onClick={handleManualScan}
                        disabled={loading || scanning}
                        className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-3.5 py-2 text-xs font-medium text-white shadow-sm hover:bg-indigo-700 disabled:opacity-50 dark:bg-indigo-500 dark:hover:bg-indigo-600 transition"
                    >
                        {scanning ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
                        <span>Rà soát & Gửi cảnh báo ngay</span>
                    </button>
                </div>
            </div>

            {/* Banners */}
            {successBanner && (
                <div className="flex items-center justify-between rounded-lg bg-emerald-50 px-4 py-3 text-xs text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
                    <div className="flex items-center gap-2">
                        <CheckCircle2 className="h-4 w-4 shrink-0" />
                        <span>{successBanner}</span>
                    </div>
                    <button
                        onClick={() => setSuccessBanner(null)}
                        className="text-emerald-600 hover:text-emerald-800 font-bold ml-2"
                    >
                        ✕
                    </button>
                </div>
            )}

            {errorMsg && (
                <div className="flex items-center justify-between rounded-lg bg-rose-50 px-4 py-3 text-xs text-rose-800 dark:bg-rose-950/40 dark:text-rose-300 border border-rose-200 dark:border-rose-800">
                    <div className="flex items-center gap-2">
                        <AlertTriangle className="h-4 w-4 shrink-0" />
                        <span>{errorMsg}</span>
                    </div>
                    <button
                        onClick={() => setErrorMsg(null)}
                        className="text-rose-600 hover:text-rose-800 font-bold ml-2"
                    >
                        ✕
                    </button>
                </div>
            )}

            {/* 4 Stat Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div
                    onClick={() => { setStatusFilter("ALL"); setCurrentPage(1); }}
                    className={`cursor-pointer rounded-xl border p-4 shadow-sm transition hover:shadow-md ${
                        statusFilter === "ALL"
                            ? "border-indigo-500 ring-2 ring-indigo-500/20 bg-indigo-50/20 dark:bg-indigo-950/20"
                            : "border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900"
                    }`}
                >
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-medium text-slate-500 dark:text-slate-400">Tổng Cảnh Báo</span>
                        <div className="rounded-lg bg-slate-100 p-2 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                            <Users className="h-4 w-4" />
                        </div>
                    </div>
                    <div className="mt-3">
                        <span className="text-2xl font-bold text-slate-900 dark:text-white">{stats.total}</span>
                        <span className="ml-1.5 text-xs text-slate-500">nhân sự</span>
                    </div>
                </div>

                <div
                    onClick={() => { setStatusFilter("EXPIRING_SOON"); setCurrentPage(1); }}
                    className={`cursor-pointer rounded-xl border p-4 shadow-sm transition hover:shadow-md ${
                        statusFilter === "EXPIRING_SOON"
                            ? "border-amber-500 ring-2 ring-amber-500/20 bg-amber-50/40 dark:bg-amber-950/40"
                            : "border-amber-200 bg-amber-50/20 dark:border-amber-900/40 dark:bg-amber-950/20"
                    }`}
                >
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-medium text-amber-800 dark:text-amber-300">Sắp Hết Hạn (&le; {thresholdDays} ngày)</span>
                        <div className="rounded-lg bg-amber-100 p-2 dark:bg-amber-900/60 text-amber-700 dark:text-amber-300">
                            <Clock className="h-4 w-4" />
                        </div>
                    </div>
                    <div className="mt-3">
                        <span className="text-2xl font-bold text-amber-900 dark:text-amber-100">{stats.expiringSoon}</span>
                        <span className="ml-1.5 text-xs text-amber-700 dark:text-amber-400">hợp đồng</span>
                    </div>
                </div>

                <div
                    onClick={() => { setStatusFilter("EXPIRED"); setCurrentPage(1); }}
                    className={`cursor-pointer rounded-xl border p-4 shadow-sm transition hover:shadow-md ${
                        statusFilter === "EXPIRED"
                            ? "border-rose-500 ring-2 ring-rose-500/20 bg-rose-50/40 dark:bg-rose-950/40"
                            : "border-rose-200 bg-rose-50/20 dark:border-rose-900/40 dark:bg-rose-950/20"
                    }`}
                >
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-medium text-rose-800 dark:text-rose-300">Đã Quá Hạn Hợp Đồng</span>
                        <div className="rounded-lg bg-rose-100 p-2 dark:bg-rose-900/60 text-rose-700 dark:text-rose-300">
                            <AlertTriangle className="h-4 w-4" />
                        </div>
                    </div>
                    <div className="mt-3">
                        <span className="text-2xl font-bold text-rose-900 dark:text-rose-100">{stats.expired}</span>
                        <span className="ml-1.5 text-xs text-rose-700 dark:text-rose-400">hợp đồng</span>
                    </div>
                </div>

                <div
                    onClick={() => { setStatusFilter("AFFECTED_ONLY"); setCurrentPage(1); }}
                    className={`cursor-pointer rounded-xl border p-4 shadow-sm transition hover:shadow-md ${
                        statusFilter === "AFFECTED_ONLY"
                            ? "border-purple-500 ring-2 ring-purple-500/20 bg-purple-50/40 dark:bg-purple-950/40"
                            : "border-purple-200 bg-purple-50/20 dark:border-purple-900/40 dark:bg-purple-950/20"
                    }`}
                >
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-medium text-purple-800 dark:text-purple-300">Vi Phạm Phân Bổ (QTN-21)</span>
                        <div className="rounded-lg bg-purple-100 p-2 dark:bg-purple-900/60 text-purple-700 dark:text-purple-300">
                            <FolderKanban className="h-4 w-4" />
                        </div>
                    </div>
                    <div className="mt-3">
                        <span className="text-2xl font-bold text-purple-900 dark:text-purple-100">{stats.totalAffectedAllocations}</span>
                        <span className="ml-1.5 text-xs text-purple-700 dark:text-purple-400">tuần phân bổ</span>
                    </div>
                </div>
            </div>

            {/* Filter Toolbar */}
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white p-3 shadow-sm dark:border-slate-800 dark:bg-slate-900">
                <div className="flex flex-1 items-center gap-3">
                    <div className="relative flex-1 max-w-sm">
                        <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                        <input
                            type="text"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            placeholder="Tìm theo họ tên, mã NV, phòng ban..."
                            className="w-full rounded-lg border border-slate-300 pl-9 pr-3 py-1.5 text-xs focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                        />
                    </div>

                    <div className="flex items-center gap-1.5">
                        <span className="text-xs text-slate-500 dark:text-slate-400 hidden sm:inline">Bộ lọc:</span>
                        <div className="inline-flex rounded-lg border border-slate-200 p-0.5 bg-slate-100 dark:border-slate-700 dark:bg-slate-800 text-xs">
                            <button
                                onClick={() => { setStatusFilter("ALL"); setCurrentPage(1); }}
                                className={`px-2.5 py-1 rounded-md font-medium transition ${
                                    statusFilter === "ALL"
                                        ? "bg-white text-slate-900 shadow-xs dark:bg-slate-700 dark:text-white"
                                        : "text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white"
                                }`}
                            >
                                Tất cả
                            </button>
                            <button
                                onClick={() => { setStatusFilter("EXPIRING_SOON"); setCurrentPage(1); }}
                                className={`px-2.5 py-1 rounded-md font-medium transition ${
                                    statusFilter === "EXPIRING_SOON"
                                        ? "bg-white text-amber-700 shadow-xs dark:bg-slate-700 dark:text-amber-300"
                                        : "text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white"
                                }`}
                            >
                                Sắp hết hạn
                            </button>
                            <button
                                onClick={() => { setStatusFilter("EXPIRED"); setCurrentPage(1); }}
                                className={`px-2.5 py-1 rounded-md font-medium transition ${
                                    statusFilter === "EXPIRED"
                                        ? "bg-white text-rose-700 shadow-xs dark:bg-slate-700 dark:text-rose-300"
                                        : "text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white"
                                }`}
                            >
                                Đã quá hạn
                            </button>
                            <button
                                onClick={() => { setStatusFilter("AFFECTED_ONLY"); setCurrentPage(1); }}
                                className={`px-2.5 py-1 rounded-md font-medium transition ${
                                    statusFilter === "AFFECTED_ONLY"
                                        ? "bg-white text-purple-700 shadow-xs dark:bg-slate-700 dark:text-purple-300"
                                        : "text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white"
                                }`}
                            >
                                Vi phạm QTN-21
                            </button>
                        </div>
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    <span className="text-xs text-slate-500 dark:text-slate-400">Ngưỡng cảnh báo:</span>
                    <select
                        value={thresholdDays}
                        onChange={(e) => {
                            setThresholdDays(Number(e.target.value));
                            setCurrentPage(1);
                        }}
                        className="rounded-lg border border-slate-300 px-2.5 py-1.5 text-xs font-medium focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                    >
                        <option value={15}>15 ngày</option>
                        <option value={30}>30 ngày (chuẩn)</option>
                        <option value={60}>60 ngày</option>
                        <option value={90}>90 ngày</option>
                    </select>
                </div>
            </div>

            {/* Master-Detail Contracts Table */}
            <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse text-xs">
                        <thead>
                            <tr className="border-b border-slate-200 bg-slate-50/75 dark:border-slate-800 dark:bg-slate-800/50 text-slate-600 dark:text-slate-300 font-semibold">
                                <th className="py-3 px-3 w-10"></th>
                                <th className="py-3 px-4">Nhân Sự Thuê Ngoài</th>
                                <th className="py-3 px-4">Đơn Vị / Chi Nhánh</th>
                                <th className="py-3 px-4">Ngày Hết Hạn</th>
                                <th className="py-3 px-4">Thời Gian Còn Lại</th>
                                <th className="py-3 px-4">Trạng Thái & Rủi Ro QTN-21</th>
                                <th className="py-3 px-4 text-right">Thao Tác</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                            {loading ? (
                                <tr>
                                    <td colSpan={7} className="py-12 text-center text-slate-500">
                                        <div className="flex flex-col items-center justify-center gap-2">
                                            <Loader2 className="h-6 w-6 animate-spin text-indigo-600" />
                                            <span>Đang tải danh sách hợp đồng thuê ngoài...</span>
                                        </div>
                                    </td>
                                </tr>
                            ) : paginatedContracts.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="py-12 text-center text-slate-500 dark:text-slate-400">
                                        <div className="flex flex-col items-center justify-center gap-2">
                                            <CheckCircle2 className="h-8 w-8 text-emerald-500" />
                                            <p className="font-semibold text-sm text-slate-700 dark:text-slate-300">
                                                Không có hợp đồng thuê ngoài nào cần cảnh báo
                                            </p>
                                            <p className="text-xs text-slate-400">
                                                Tất cả nhân sự thuê ngoài đều có hợp đồng an toàn hoặc không khớp với bộ lọc hiện tại.
                                            </p>
                                        </div>
                                    </td>
                                </tr>
                            ) : (
                                paginatedContracts.map((c) => {
                                    const isExpanded = expandedEmployeeIds.has(c.employeeId);
                                    const hasAllocations = c.affectedAllocations && c.affectedAllocations.length > 0;

                                    return (
                                        <tbody key={c.employeeId} className="group">
                                            <tr className="hover:bg-slate-50/75 dark:hover:bg-slate-800/40 transition-colors">
                                                <td className="py-3 px-3 text-center">
                                                    {hasAllocations && (
                                                        <button
                                                            onClick={() => toggleExpandRow(c.employeeId)}
                                                            className="rounded p-1 text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700 hover:text-slate-700 dark:hover:text-slate-200"
                                                        >
                                                            {isExpanded ? (
                                                                <ChevronDown className="h-4 w-4" />
                                                            ) : (
                                                                <ChevronRight className="h-4 w-4" />
                                                            )}
                                                        </button>
                                                    )}
                                                </td>

                                                <td className="py-3 px-4">
                                                    <div className="font-semibold text-slate-900 dark:text-white">
                                                        {c.fullName}
                                                    </div>
                                                    <div className="flex items-center gap-1.5 text-slate-500 text-[11px] mt-0.5">
                                                        <span className="font-mono bg-slate-100 dark:bg-slate-800 px-1.5 py-0.5 rounded">
                                                            {c.employeeCode}
                                                        </span>
                                                        <span>•</span>
                                                        <span>{c.professionalRole || "Chưa phân vai trò"}</span>
                                                    </div>
                                                </td>

                                                <td className="py-3 px-4 text-slate-600 dark:text-slate-300">
                                                    <div className="flex items-center gap-1.5">
                                                        <Building2 className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                                                        <span>{c.orgUnitName || "N/A"}</span>
                                                    </div>
                                                </td>

                                                <td className="py-3 px-4 font-mono text-slate-700 dark:text-slate-300">
                                                    <div className="flex items-center gap-1.5">
                                                        <Calendar className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                                                        <span>{c.contractEndDate}</span>
                                                    </div>
                                                </td>

                                                <td className="py-3 px-4">
                                                    {c.daysRemaining < 0 ? (
                                                        <span className="inline-flex items-center rounded-full bg-rose-100 px-2.5 py-0.5 text-[11px] font-semibold text-rose-700 dark:bg-rose-950/60 dark:text-rose-300">
                                                            Đã quá hạn {Math.abs(c.daysRemaining)} ngày
                                                        </span>
                                                    ) : c.daysRemaining <= 15 ? (
                                                        <span className="inline-flex items-center rounded-full bg-rose-100 px-2.5 py-0.5 text-[11px] font-semibold text-rose-700 dark:bg-rose-950/60 dark:text-rose-300">
                                                            Còn {c.daysRemaining} ngày (Khẩn cấp)
                                                        </span>
                                                    ) : (
                                                        <span className="inline-flex items-center rounded-full bg-amber-100 px-2.5 py-0.5 text-[11px] font-semibold text-amber-700 dark:bg-amber-950/60 dark:text-amber-300">
                                                            Còn {c.daysRemaining} ngày
                                                        </span>
                                                    )}
                                                </td>

                                                <td className="py-3 px-4">
                                                    {hasAllocations ? (
                                                        <button
                                                            onClick={() => toggleExpandRow(c.employeeId)}
                                                            className="inline-flex items-center gap-1 rounded-full bg-purple-100 px-2.5 py-0.5 text-[11px] font-semibold text-purple-700 hover:bg-purple-200 dark:bg-purple-950/60 dark:text-purple-300 transition"
                                                        >
                                                            <AlertTriangle className="h-3 w-3 shrink-0" />
                                                            <span>{c.affectedAllocations.length} phân bổ vi phạm QTN-21</span>
                                                        </button>
                                                    ) : (
                                                        <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-0.5 text-[11px] text-slate-600 dark:bg-slate-800 dark:text-slate-400">
                                                            <span>Chưa có phân bổ vi phạm</span>
                                                        </span>
                                                    )}
                                                </td>

                                                <td className="py-3 px-4 text-right">
                                                    <button
                                                        onClick={() => setSelectedContractForAck(c)}
                                                        className="inline-flex items-center gap-1 rounded-lg border border-slate-300 bg-white px-2.5 py-1.5 text-[11px] font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700 transition"
                                                    >
                                                        <FileCheck2 className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-400" />
                                                        <span>Xác nhận xử lý</span>
                                                    </button>
                                                </td>
                                            </tr>

                                            {/* Accordion Row: Affected Allocations per QTN-21 */}
                                            {isExpanded && hasAllocations && (
                                                <tr className="bg-slate-50/60 dark:bg-slate-850/40">
                                                    <td colSpan={7} className="px-6 py-3 border-t border-b border-slate-100 dark:border-slate-800">
                                                        <div className="rounded-lg border border-slate-200 bg-white p-3.5 dark:border-slate-700/60 dark:bg-slate-900/80 shadow-inner">
                                                            <div className="flex items-center justify-between mb-2.5">
                                                                <span className="font-semibold text-xs text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                                                                    <FolderKanban className="h-3.5 w-3.5 text-indigo-500" />
                                                                    Chi tiết các tuần phân bổ bị ảnh hưởng theo quy tắc QTN-21:
                                                                </span>
                                                                <span className="text-[11px] text-slate-400">
                                                                    Hạn hợp đồng: {c.contractEndDate}
                                                                </span>
                                                            </div>

                                                            <div className="divide-y divide-slate-100 dark:divide-slate-800">
                                                                {c.affectedAllocations.map((alloc) => {
                                                                    const isSpans = alloc.affectedType === "SPANS_OVER_EXPIRY";
                                                                    return (
                                                                        <div
                                                                            key={alloc.allocationId}
                                                                            className="py-2 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs"
                                                                        >
                                                                            <div className="flex items-center gap-2">
                                                                                {isSpans ? (
                                                                                    <span className="inline-flex items-center rounded bg-amber-100 px-2 py-0.5 text-[10px] font-bold text-amber-800 dark:bg-amber-950/70 dark:text-amber-300 shrink-0">
                                                                                        VẮT QUA NGÀY HẾT HẠN
                                                                                    </span>
                                                                                ) : (
                                                                                    <span className="inline-flex items-center rounded bg-rose-100 px-2 py-0.5 text-[10px] font-bold text-rose-800 dark:bg-rose-950/70 dark:text-rose-300 shrink-0">
                                                                                        SAU NGÀY HẾT HẠN
                                                                                    </span>
                                                                                )}
                                                                                <span className="font-medium text-slate-800 dark:text-slate-200">
                                                                                    {alloc.projectName}
                                                                                </span>
                                                                                <span className="text-slate-400">•</span>
                                                                                <span className="text-slate-500 dark:text-slate-400">
                                                                                    Tuần {alloc.yearWeek.weekNumber}/{alloc.yearWeek.year} ({alloc.weekStartDate} đến {alloc.weekEndDate})
                                                                                </span>
                                                                            </div>

                                                                            <div className="flex items-center gap-3">
                                                                                <span className="font-semibold text-slate-700 dark:text-slate-300">
                                                                                    {alloc.allocatedHours} giờ
                                                                                </span>
                                                                                <p className="text-[11px] text-slate-500 dark:text-slate-400 italic">
                                                                                    {alloc.reason}
                                                                                </p>
                                                                            </div>
                                                                        </div>
                                                                    );
                                                                })}
                                                            </div>
                                                        </div>
                                                    </td>
                                                </tr>
                                            )}
                                        </tbody>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>

                {/* Pagination Footer */}
                {filteredContracts.length > pageSize && (
                    <div className="flex items-center justify-between border-t border-slate-200 bg-slate-50/50 px-4 py-3 dark:border-slate-800 dark:bg-slate-800/30 text-xs text-slate-600 dark:text-slate-400">
                        <div>
                            Hiển thị <span className="font-semibold">{(currentPage - 1) * pageSize + 1}</span> đến{" "}
                            <span className="font-semibold">
                                {Math.min(currentPage * pageSize, filteredContracts.length)}
                            </span>{" "}
                            trong tổng số <span className="font-semibold">{filteredContracts.length}</span> hợp đồng
                        </div>

                        <div className="flex items-center gap-1.5">
                            <button
                                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                                disabled={currentPage === 1}
                                className="inline-flex items-center gap-1 rounded-lg border border-slate-300 bg-white px-2.5 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-40 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                            >
                                <ChevronLeft className="h-3.5 w-3.5" />
                                <span>Trước</span>
                            </button>

                            <span className="px-2 font-medium text-slate-700 dark:text-slate-300">
                                {currentPage} / {totalPages}
                            </span>

                            <button
                                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                                disabled={currentPage === totalPages}
                                className="inline-flex items-center gap-1 rounded-lg border border-slate-300 bg-white px-2.5 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-40 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                            >
                                <span>Sau</span>
                                <ChevronRight className="h-3.5 w-3.5" />
                            </button>
                        </div>
                    </div>
                )}
            </div>

            {/* Acknowledge Modal */}
            {selectedContractForAck && (
                <AcknowledgeContractModal
                    contract={selectedContractForAck}
                    isOpen={!!selectedContractForAck}
                    onClose={() => setSelectedContractForAck(null)}
                    onSuccess={(_empId, message) => {
                        setSuccessBanner(message);
                        loadData(true);
                    }}
                />
            )}
        </div>
    );
}
