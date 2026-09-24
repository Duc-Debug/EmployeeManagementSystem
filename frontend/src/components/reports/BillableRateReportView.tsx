import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  DollarSign,
  TrendingUp,
  Building2,
  Calendar,
  Clock,
  ShieldAlert,
  Download,
  RefreshCw,
  Search,
  CheckCircle2,
  AlertCircle,
  Users,
  BarChart3,
  Sparkles,
  Info
} from 'lucide-react';
import { cn } from '@/lib/utils';
import {
  getBillableRateReport,
  downloadBillableRateReport,
  type BillableRateResult,
} from '@/lib/api/billable-rate-report';
import { getOrgTree } from '@/lib/api/org-units';
import { useAuthUser } from '@/lib/auth-session';
import { getIsoWeeksInYear, getIsoWeekDetails, addIsoWeeks } from '@/lib/iso-week';
import type { OrgUnitTreeNode } from '@/types/hrm';

export default function BillableRateReportView() {
  const user = useAuthUser();

  const currentIso = getIsoWeekDetails(new Date());
  const initialFrom = addIsoWeeks(currentIso.year, currentIso.week, -3);

  const [fromYear, setFromYear] = useState<number>(initialFrom.year);
  const [fromWeek, setFromWeek] = useState<number>(initialFrom.week);
  const [toYear, setToYear] = useState<number>(currentIso.year);
  const [toWeek, setToWeek] = useState<number>(currentIso.week);
  const [selectedOrgUnitId, setSelectedOrgUnitId] = useState<string>('');
  const [searchEmployeeText, setSearchEmployeeText] = useState<string>('');
  const [activeTab, setActiveTab] = useState<'departments' | 'employees' | 'charts'>('departments');

  const [reportData, setReportData] = useState<BillableRateResult | null>(null);
  const [orgUnits, setOrgUnits] = useState<OrgUnitTreeNode[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [forbidden, setForbidden] = useState<boolean>(false);

  const availableYears = useMemo(() => {
    const current = currentIso.year;
    return [current - 2, current - 1, current, current + 1];
  }, [currentIso.year]);

  useEffect(() => {
    async function loadOrgUnitsData() {
      try {
        const tree = await getOrgTree();
        const flatten = (nodes: readonly OrgUnitTreeNode[]): OrgUnitTreeNode[] => {
          let list: OrgUnitTreeNode[] = [];
          for (const n of nodes) {
            list.push(n);
            if (n.children && n.children.length > 0) {
              list = list.concat(flatten(n.children));
            }
          }
          return list;
        };
        const allNodes = [...(tree || [])];
        if (user?.dataScope === 'ORGANIZATION_BRANCH' && user?.scopeOrgUnitId != null) {
          const findScopeRoot = (nodes: readonly OrgUnitTreeNode[]): OrgUnitTreeNode | null => {
            for (const node of nodes) {
              if (Number(node.id) === Number(user.scopeOrgUnitId)) return node;
              const match = findScopeRoot(node.children || []);
              if (match) return match;
            }
            return null;
          };
          const scopeRoot = findScopeRoot(allNodes);
          setOrgUnits(scopeRoot ? flatten([scopeRoot]) : []);
        } else {
          setOrgUnits(flatten(allNodes));
        }
      } catch (err) {
        console.warn('Không thể tải danh sách phòng ban:', err);
      }
    }
    loadOrgUnitsData();
  }, [user?.dataScope, user?.scopeOrgUnitId]);

  const fetchReport = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    setForbidden(false);

    try {
      const data = await getBillableRateReport({
        fromYear,
        fromWeek,
        toYear,
        toWeek,
        orgUnitId: selectedOrgUnitId ? Number(selectedOrgUnitId) : undefined,
      });
      setReportData(data);
    } catch (err: unknown) {
      const e = err as { status?: number; code?: string; message?: string };
      if (e?.status === 403 || e?.code === 'FORBIDDEN' || e?.message?.includes('403')) {
        setForbidden(true);
      } else {
        setError(e?.message || 'Không thể tải dữ liệu báo cáo tỷ lệ giờ tính phí.');
      }
    } finally {
      setIsLoading(false);
    }
  }, [fromYear, fromWeek, toYear, toWeek, selectedOrgUnitId]);

  useEffect(() => {
    fetchReport();
  }, [fetchReport]);

  const applyPreset = (type: 'this_week' | 'last_4_weeks' | 'last_8_weeks' | 'last_12_weeks') => {
    const cur = getIsoWeekDetails(new Date());
    setToYear(cur.year);
    setToWeek(cur.week);

    if (type === 'this_week') {
      setFromYear(cur.year);
      setFromWeek(cur.week);
    } else if (type === 'last_4_weeks') {
      const start = addIsoWeeks(cur.year, cur.week, -3);
      setFromYear(start.year);
      setFromWeek(start.week);
    } else if (type === 'last_8_weeks') {
      const start = addIsoWeeks(cur.year, cur.week, -7);
      setFromYear(start.year);
      setFromWeek(start.week);
    } else if (type === 'last_12_weeks') {
      const start = addIsoWeeks(cur.year, cur.week, -11);
      setFromYear(start.year);
      setFromWeek(start.week);
    }
  };

  const handleExportCsv = async () => {
    setIsExporting(true);
    try {
      await downloadBillableRateReport({
        fromYear,
        fromWeek,
        toYear,
        toWeek,
        orgUnitId: selectedOrgUnitId ? Number(selectedOrgUnitId) : undefined,
      });
    } catch (err: unknown) {
      const e = err as { message?: string };
      alert('Lỗi khi xuất tệp: ' + (e?.message || 'Không xác định'));
    } finally {
      setIsExporting(false);
    }
  };

  const employeeBreakdown = reportData?.employeeBreakdown;
  const filteredEmployees = useMemo(() => {
    if (!employeeBreakdown) return [];
    if (!searchEmployeeText.trim()) return employeeBreakdown;
    const q = searchEmployeeText.toLowerCase();
    return employeeBreakdown.filter(
      (e) =>
        e.fullName.toLowerCase().includes(q) ||
        e.employeeCode.toLowerCase().includes(q) ||
        e.orgUnitName.toLowerCase().includes(q)
    );
  }, [employeeBreakdown, searchEmployeeText]);

  const renderRateBadge = (rate: number | null, hasAvailable: boolean) => {
    if (!hasAvailable || rate === null) {
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-500 border border-slate-200">
          N/A (0h khả dụng)
        </span>
      );
    }
    if (rate >= 85) {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 shadow-xs">
          <CheckCircle2 className="w-3 h-3 text-emerald-600" />
          {rate.toFixed(1)}% (Rất cao)
        </span>
      );
    }
    if (rate >= 70) {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-200 shadow-xs">
          <Sparkles className="w-3 h-3 text-indigo-600" />
          {rate.toFixed(1)}% (Đạt chuẩn)
        </span>
      );
    }
    if (rate > 0) {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-50 text-amber-700 border border-amber-200 shadow-xs">
          <AlertCircle className="w-3 h-3 text-amber-600" />
          {rate.toFixed(1)}% (Thấp)
        </span>
      );
    }
    return (
      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200">
        0.0% (Chưa tính phí)
      </span>
    );
  };

  if (forbidden) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[450px] p-8 text-center bg-white rounded-3xl border border-slate-200 shadow-xs">
        <div className="w-16 h-16 rounded-2xl bg-rose-50 border border-rose-100 flex items-center justify-center text-rose-600 mb-4">
          <ShieldAlert className="w-8 h-8" />
        </div>
        <h3 className="text-base font-bold text-slate-900 mb-1">Không có quyền truy cập</h3>
        <p className="text-xs text-slate-500 max-w-md mb-6 leading-relaxed">
          Chức năng báo cáo tỷ lệ giờ tính phí chỉ dành riêng cho Ban Giám Đốc, Quản lý Nguồn lực và Quản trị viên.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-12">
      {/* Header & Controls */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
        <div className="space-y-1">
          <div className="flex items-center gap-2.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-indigo-600 text-white shadow-xs">
              <DollarSign className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-xl font-black tracking-tight text-slate-900">
                Báo cáo tỷ lệ giờ tính phí
              </h1>
              <p className="text-xs font-medium text-slate-500">
                Chỉ số sống còn đo lường tỷ trọng giờ công làm ra tiền trên tổng giờ khả dụng thực tế của công ty dịch vụ
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={fetchReport}
            disabled={isLoading}
            className="flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200/80 rounded-xl transition shadow-xs disabled:opacity-50"
          >
            <RefreshCw className={cn('w-3.5 h-3.5 text-slate-500', isLoading && 'animate-spin')} />
            Làm mới
          </button>
          <button
            type="button"
            onClick={handleExportCsv}
            disabled={isExporting || isLoading || !reportData?.hasData}
            className="flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl transition shadow-xs disabled:opacity-50"
          >
            <Download className="w-3.5 h-3.5" />
            {isExporting ? 'Đang xuất...' : 'Xuất CSV'}
          </button>
        </div>
      </div>

      {/* Dynamic Filter Section */}
      <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-xs space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-3">
          <span className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
            <Calendar className="w-3.5 h-3.5 text-indigo-600" />
            Chọn nhanh kỳ phân tích
          </span>
          <div className="flex flex-wrap gap-1.5">
            <button
              type="button"
              onClick={() => applyPreset('this_week')}
              className="px-2.5 py-1 text-xs font-medium text-slate-600 bg-slate-50 hover:bg-indigo-50 hover:text-indigo-700 rounded-lg border border-slate-200 transition"
            >
              Tuần này
            </button>
            <button
              type="button"
              onClick={() => applyPreset('last_4_weeks')}
              className="px-2.5 py-1 text-xs font-medium text-slate-600 bg-slate-50 hover:bg-indigo-50 hover:text-indigo-700 rounded-lg border border-slate-200 transition"
            >
              4 tuần gần nhất
            </button>
            <button
              type="button"
              onClick={() => applyPreset('last_8_weeks')}
              className="px-2.5 py-1 text-xs font-medium text-slate-600 bg-slate-50 hover:bg-indigo-50 hover:text-indigo-700 rounded-lg border border-slate-200 transition"
            >
              8 tuần
            </button>
            <button
              type="button"
              onClick={() => applyPreset('last_12_weeks')}
              className="px-2.5 py-1 text-xs font-medium text-slate-600 bg-slate-50 hover:bg-indigo-50 hover:text-indigo-700 rounded-lg border border-slate-200 transition"
            >
              12 tuần (Quý)
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          <div className="space-y-1">
            <label className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
              Từ tuần & Năm
            </label>
            <div className="grid grid-cols-2 gap-1.5">
              <select
                value={fromWeek}
                onChange={(e) => setFromWeek(Number(e.target.value))}
                className="w-full rounded-xl border border-slate-200 bg-slate-50 px-2.5 py-2 text-xs font-medium text-slate-700 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                {Array.from({ length: getIsoWeeksInYear(fromYear) }, (_, i) => i + 1).map((w) => (
                  <option key={w} value={w}>
                    Tuần {w}
                  </option>
                ))}
              </select>
              <select
                value={fromYear}
                onChange={(e) => {
                  const y = Number(e.target.value);
                  setFromYear(y);
                  const max = getIsoWeeksInYear(y);
                  if (fromWeek > max) setFromWeek(max);
                }}
                className="w-full rounded-xl border border-slate-200 bg-slate-50 px-2.5 py-2 text-xs font-medium text-slate-700 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                {availableYears.map((y) => (
                  <option key={y} value={y}>
                    Năm {y}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
              Đến tuần & Năm
            </label>
            <div className="grid grid-cols-2 gap-1.5">
              <select
                value={toWeek}
                onChange={(e) => setToWeek(Number(e.target.value))}
                className="w-full rounded-xl border border-slate-200 bg-slate-50 px-2.5 py-2 text-xs font-medium text-slate-700 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                {Array.from({ length: getIsoWeeksInYear(toYear) }, (_, i) => i + 1).map((w) => (
                  <option key={w} value={w}>
                    Tuần {w}
                  </option>
                ))}
              </select>
              <select
                value={toYear}
                onChange={(e) => {
                  const y = Number(e.target.value);
                  setToYear(y);
                  const max = getIsoWeeksInYear(y);
                  if (toWeek > max) setToWeek(max);
                }}
                className="w-full rounded-xl border border-slate-200 bg-slate-50 px-2.5 py-2 text-xs font-medium text-slate-700 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                {availableYears.map((y) => (
                  <option key={y} value={y}>
                    Năm {y}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
              Phòng ban / Đơn vị
            </label>
            <div className="relative">
              <Building2 className="absolute left-3 top-2.5 h-3.5 w-3.5 text-slate-400" />
              <select
                value={selectedOrgUnitId}
                onChange={(e) => setSelectedOrgUnitId(e.target.value)}
                className="w-full rounded-xl border border-slate-200 bg-slate-50 pl-9 pr-3 py-2 text-xs font-medium text-slate-700 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="">-- Toàn công ty --</option>
                {orgUnits.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.unitName}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
              Lọc theo nhân sự
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-2.5 h-3.5 w-3.5 text-slate-400" />
              <input
                type="text"
                placeholder="Tìm mã hoặc tên nhân sự..."
                value={searchEmployeeText}
                onChange={(e) => setSearchEmployeeText(e.target.value)}
                className="w-full rounded-xl border border-slate-200 bg-slate-50 pl-9 pr-3 py-2 text-xs font-medium text-slate-700 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Rule Notice Banner */}
      <div className="bg-indigo-50/60 border border-indigo-100 p-4 rounded-2xl flex items-start gap-3">
        <Info className="w-4 h-4 text-indigo-600 mt-0.5 shrink-0" />
        <div className="text-xs text-indigo-900 leading-relaxed space-y-0.5">
          <p className="font-semibold">
            Quy tắc tính toán tỷ lệ giờ tính phí:
          </p>
          <p className="text-indigo-700">
            Tỷ lệ (%) = (Tổng giờ tính phí đã duyệt ÷ Tổng giờ khả dụng ròng) × 100%.
            <span className="font-medium"> Đặc biệt:</span> Giờ nghỉ phép đã duyệt được tự động khấu trừ khỏi mẫu số (giờ khả dụng) thay vì coi là thời gian không hiệu quả.
          </p>
        </div>
      </div>

      {/* Error State */}
      {error && (
        <div className="bg-rose-50 border border-rose-200 p-4 rounded-2xl flex items-center gap-3 text-rose-700 text-xs font-medium">
          <AlertCircle className="w-4 h-4 shrink-0" />
          {error}
        </div>
      )}

      {/* KPI Cards */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 animate-pulse">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-28 bg-white rounded-3xl border border-slate-200 p-4" />
          ))}
        </div>
      ) : reportData?.summary ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-xs relative overflow-hidden flex flex-col justify-between">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
                Tỷ lệ giờ tính phí
              </span>
              <div className="w-8 h-8 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
                <TrendingUp className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3 space-y-1">
              <div className="text-3xl font-black text-slate-900 tracking-tight">
                {reportData.summary.overallBillableRate != null
                  ? `${reportData.summary.overallBillableRate.toFixed(1)}%`
                  : 'N/A'}
              </div>
              <div className="w-full bg-slate-100 rounded-full h-2 mt-2 overflow-hidden">
                <div
                  className={cn(
                    'h-full rounded-full transition-all duration-500',
                    (reportData.summary.overallBillableRate ?? 0) >= 80
                      ? 'bg-emerald-500'
                      : (reportData.summary.overallBillableRate ?? 0) >= 65
                        ? 'bg-indigo-600'
                        : 'bg-amber-500'
                  )}
                  style={{
                    width: `${Math.min(100, Math.max(0, reportData.summary.overallBillableRate ?? 0))}%`,
                  }}
                />
              </div>
              <p className="text-[11px] font-medium text-slate-400 pt-1">
                Mục tiêu đề xuất: &ge; 75.0%
              </p>
            </div>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-xs flex flex-col justify-between">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
                Tổng giờ tính phí
              </span>
              <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <DollarSign className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3 space-y-0.5">
              <div className="text-3xl font-black text-emerald-600 tracking-tight">
                {reportData.summary.totalBillableHours.toFixed(1)}
                <span className="text-sm font-semibold text-slate-500 ml-1">giờ</span>
              </div>
              <p className="text-[11px] text-slate-400">
                Đã duyệt từ các dự án khách hàng
              </p>
            </div>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-xs flex flex-col justify-between">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
                Giờ khả dụng ròng
              </span>
              <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                <Clock className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3 space-y-0.5">
              <div className="text-3xl font-black text-slate-900 tracking-tight">
                {reportData.summary.totalAvailableHours.toFixed(1)}
                <span className="text-sm font-semibold text-slate-500 ml-1">giờ</span>
              </div>
              <p className="text-[11px] text-slate-400">
                Đã trừ {reportData.summary.totalApprovedLeaveHours.toFixed(1)}h nghỉ phép &amp; {reportData.summary.totalHolidayHours.toFixed(1)}h lễ
              </p>
            </div>
          </div>

          <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-xs flex flex-col justify-between">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
                Không tính phí &amp; Quy mô
              </span>
              <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
                <Users className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3 space-y-0.5">
              <div className="text-2xl font-black text-slate-800 tracking-tight">
                {reportData.summary.totalNonBillableHours.toFixed(1)}h{' '}
                <span className="text-xs font-normal text-slate-400">
                  ({reportData.summary.totalEmployees} nhân sự)
                </span>
              </div>
              <p className="text-[11px] text-slate-400">
                Nội bộ, đào tạo, hỗ trợ kỹ thuật
              </p>
            </div>
          </div>
        </div>
      ) : null}

      {/* Main Tabs Navigation */}
      <div className="bg-white rounded-3xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="flex items-center gap-1 border-b border-slate-100 p-2 bg-slate-50/50">
          <button
            type="button"
            onClick={() => setActiveTab('departments')}
            className={cn(
              'flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-2xl transition',
              activeTab === 'departments'
                ? 'bg-white text-indigo-600 shadow-xs border border-slate-200/80'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/60'
            )}
          >
            <Building2 className="w-3.5 h-3.5" />
            Theo phòng ban ({reportData?.departmentBreakdown?.length || 0})
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('employees')}
            className={cn(
              'flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-2xl transition',
              activeTab === 'employees'
                ? 'bg-white text-indigo-600 shadow-xs border border-slate-200/80'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/60'
            )}
          >
            <Users className="w-3.5 h-3.5" />
            Chi tiết từng nhân sự ({filteredEmployees.length})
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('charts')}
            className={cn(
              'flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-2xl transition',
              activeTab === 'charts'
                ? 'bg-white text-indigo-600 shadow-xs border border-slate-200/80'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/60'
            )}
          >
            <BarChart3 className="w-3.5 h-3.5" />
            Biểu đồ trực quan
          </button>
        </div>

        {/* Tab 1: Department Breakdown */}
        {activeTab === 'departments' && (
          <div className="p-6 space-y-4">
            {reportData?.departmentBreakdown && reportData.departmentBreakdown.length > 0 ? (
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                {reportData.departmentBreakdown.map((dept) => {
                  const rate = dept.billableRate ?? 0;
                  return (
                    <div
                      key={dept.orgUnitId ?? 'unassigned'}
                      className="p-5 rounded-2xl border border-slate-200 bg-white hover:border-indigo-200 transition space-y-4 shadow-2xs"
                    >
                      <div className="flex items-center justify-between">
                        <div className="space-y-0.5">
                          <h4 className="text-sm font-bold text-slate-900">
                            {dept.orgUnitName}
                          </h4>
                          <p className="text-xs text-slate-400">
                            {dept.employeeCount} nhân sự
                          </p>
                        </div>
                        {renderRateBadge(dept.billableRate, dept.totalAvailableHours > 0)}
                      </div>

                      <div className="space-y-1">
                        <div className="flex justify-between text-xs text-slate-500 font-medium">
                          <span>Tiến độ tính phí</span>
                          <span className="font-bold text-slate-700">
                            {dept.totalBillableHours.toFixed(1)} / {dept.totalAvailableHours.toFixed(1)} giờ
                          </span>
                        </div>
                        <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                          <div
                            className={cn(
                              'h-full rounded-full transition-all duration-500',
                              rate >= 80 ? 'bg-emerald-500' : rate >= 65 ? 'bg-indigo-600' : 'bg-amber-500'
                            )}
                            style={{ width: `${Math.min(100, Math.max(0, rate))}%` }}
                          />
                        </div>
                      </div>

                      <div className="grid grid-cols-4 gap-2 pt-2 border-t border-slate-100 text-center">
                        <div className="space-y-0.5">
                          <span className="text-[10px] uppercase font-bold text-slate-400">Chuẩn</span>
                          <p className="text-xs font-bold text-slate-700">{dept.totalStandardHours.toFixed(1)}h</p>
                        </div>
                        <div className="space-y-0.5">
                          <span className="text-[10px] uppercase font-bold text-slate-400">Nghỉ phép</span>
                          <p className="text-xs font-bold text-amber-600">-{dept.totalApprovedLeaveHours.toFixed(1)}h</p>
                        </div>
                        <div className="space-y-0.5">
                          <span className="text-[10px] uppercase font-bold text-slate-400">Tính phí</span>
                          <p className="text-xs font-bold text-emerald-600">{dept.totalBillableHours.toFixed(1)}h</p>
                        </div>
                        <div className="space-y-0.5">
                          <span className="text-[10px] uppercase font-bold text-slate-400">Nội bộ</span>
                          <p className="text-xs font-bold text-slate-600">{dept.totalNonBillableHours.toFixed(1)}h</p>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="py-12 text-center text-slate-400 text-xs">
                Không có dữ liệu phòng ban trong kỳ phân tích đã chọn.
              </div>
            )}
          </div>
        )}

        {/* Tab 2: Employee Detailed Table */}
        {activeTab === 'employees' && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/70 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                  <th className="py-3.5 px-4">Nhân sự</th>
                  <th className="py-3.5 px-4">Phòng ban</th>
                  <th className="py-3.5 px-3 text-right">Giờ chuẩn</th>
                  <th className="py-3.5 px-3 text-right">Nghỉ phép</th>
                  <th className="py-3.5 px-3 text-right font-bold text-indigo-900">Khả dụng ròng</th>
                  <th className="py-3.5 px-3 text-right font-bold text-emerald-700">Tính phí (Billable)</th>
                  <th className="py-3.5 px-3 text-right">Không tính phí</th>
                  <th className="py-3.5 px-3 text-right">Tổng thực tế</th>
                  <th className="py-3.5 px-4 text-center">Tỷ lệ giờ tính phí</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                {filteredEmployees.length > 0 ? (
                  filteredEmployees.map((emp) => (
                    <tr key={emp.employeeId} className="hover:bg-slate-50/60 transition">
                      <td className="py-3 px-4">
                        <div className="font-bold text-slate-900">{emp.fullName}</div>
                        <div className="text-[11px] text-slate-400">{emp.employeeCode}</div>
                      </td>
                      <td className="py-3 px-4 text-slate-600">
                        {emp.orgUnitName}
                      </td>
                      <td className="py-3 px-3 text-right text-slate-500">
                        {emp.standardHours.toFixed(1)}h
                      </td>
                      <td className="py-3 px-3 text-right">
                        {emp.approvedLeaveHours > 0 ? (
                          <span className="text-amber-600 font-semibold">
                            -{emp.approvedLeaveHours.toFixed(1)}h
                          </span>
                        ) : (
                          <span className="text-slate-300">-</span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-right font-bold text-indigo-700">
                        {emp.netAvailableHours.toFixed(1)}h
                      </td>
                      <td className="py-3 px-3 text-right font-bold text-emerald-600">
                        {emp.billableHours.toFixed(1)}h
                      </td>
                      <td className="py-3 px-3 text-right text-slate-500">
                        {emp.nonBillableHours.toFixed(1)}h
                      </td>
                      <td className="py-3 px-3 text-right font-semibold text-slate-800">
                        {emp.totalActualHours.toFixed(1)}h
                      </td>
                      <td className="py-3 px-4 text-center">
                        {renderRateBadge(emp.billableRate, emp.hasAvailableHours)}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={9} className="py-12 text-center text-slate-400 text-xs">
                      Không tìm thấy nhân sự nào phù hợp với điều kiện tìm kiếm.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Tab 3: Visual Charts */}
        {activeTab === 'charts' && (
          <div className="p-6 space-y-6">
            <div className="bg-slate-50 p-5 rounded-2xl border border-slate-200">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-600 mb-4 flex items-center gap-2">
                <BarChart3 className="w-4 h-4 text-indigo-600" />
                So sánh Tỷ lệ tính phí theo Phòng ban
              </h4>
              <div className="space-y-3">
                {reportData?.departmentBreakdown?.map((dept) => {
                  const rate = dept.billableRate ?? 0;
                  return (
                    <div key={dept.orgUnitId ?? 'unassigned'} className="space-y-1">
                      <div className="flex justify-between text-xs font-semibold text-slate-700">
                        <span>{dept.orgUnitName}</span>
                        <span className="text-indigo-600 font-bold">
                          {dept.billableRate != null ? `${dept.billableRate.toFixed(1)}%` : 'N/A'}
                        </span>
                      </div>
                      <div className="w-full bg-slate-200 rounded-full h-3 overflow-hidden">
                        <div
                          className={cn(
                            'h-full rounded-full transition-all duration-500',
                            rate >= 80 ? 'bg-emerald-500' : rate >= 65 ? 'bg-indigo-600' : 'bg-amber-500'
                          )}
                          style={{ width: `${Math.min(100, Math.max(0, rate))}%` }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
