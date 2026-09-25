import React, { Fragment, useState, useEffect, useCallback } from "react";
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
            setError