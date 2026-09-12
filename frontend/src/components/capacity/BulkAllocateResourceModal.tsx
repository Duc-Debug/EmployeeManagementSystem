/**
 * NCL-06-CN-006 & NCL-06-CN-007: Modal phân bổ nguồn lực hàng loạt cho nhiều tuần.
 * Cho phép Quản lý nguồn lực chọn nhân sự, dự án, khoảng tuần và số giờ/tuần hoặc tỷ lệ phần trăm (%) để phân bổ trong 1 thao tác.
 */
import { useState, useEffect } from 'react';
import { X, Layers, AlertCircle, Loader2, Sparkles, Percent, Clock } from 'lucide-react';
import { bulkAllocateResource, type BulkAllocationResult } from '@/lib/api/allocations';
import { getProjects, type ProjectResult } from '@/lib/api/projects';

export interface BulkAllocateCandidate {
  id: number;
  code: string;
  name: string;
}

interface BulkAllocateResourceModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: (result: BulkAllocationResult) => void;
  employees: BulkAllocateCandidate[];
  initialEmployeeId?: number;
  initialYear?: number;
  initialWeek?: number;
}

export function getMaxIsoWeeks(year: number): number {
  const dec28 = new Date(Date.UTC(year, 11, 28));
  const day = dec28.getUTCDay() || 7;
  dec28.setUTCDate(dec28.getUTCDate() + 4 - day);
  const yearStart = new Date(Date.UTC(dec28.getUTCFullYear(), 0, 1));
  return Math.ceil(((dec28.getTime() - yearStart.getTime()) / 86400000 + 1) / 7);
}

export function addIsoWeeks(startYear: number, startWeek: number, count: number): { year: number; week: number } {
  let curYear = startYear;
  let curWeek = startWeek;
  for (let i = 1; i < count; i++) {
    curWeek++;
    const maxWeeks = getMaxIsoWeeks(curYear);
    if (curWeek > maxWeeks) {
      curYear++;
      curWeek = 1;
    }
  }
  return { year: curYear, week: curWeek };
}

export function BulkAllocateResourceModal({
  open,
  onClose,
  onSuccess,
  employees,
  initialEmployeeId,
  initialYear = new Date().getFullYear(),
  initialWeek = 1,
}: BulkAllocateResourceModalProps) {
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<number | ''>('');
  const [selectedProjectId, setSelectedProjectId] = useState<number | ''>('');
  const [fromYear, setFromYear] = useState<number>(initialYear);
  const [fromWeek, setFromWeek] = useState<number>(initialWeek);
  const [toYear, setToYear] = useState<number>(initialYear);
  const [toWeek, setToWeek] = useState<number>(12);

  // Allocation mode (Hours vs Percentage)
  const [mode, setMode] = useState<'PERCENTAGE' | 'HOURS'>('PERCENTAGE');
  const [allocatedHours, setAllocatedHours] = useState<number>(20);
  const [percentage, setPercentage] = useState<number>(50);

  const [projects, setProjects] = useState<ProjectResult[]>([]);
  const [isLoadingProjects, setIsLoadingProjects] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Initialize or reset state when modal opens
  useEffect(() => {
    if (open) {
      if (initialEmployeeId) {
        setSelectedEmployeeId(initialEmployeeId);
      } else if (employees.length > 0) {
        setSelectedEmployeeId(employees[0].id);
      }
      setFromYear(initialYear);
      setFromWeek(initialWeek);
      
      // Mặc định 12 tuần (1 Quý) theo chuẩn ISO-8601 (xử lý chính xác cả năm 52 và 53 tuần)
      const range = addIsoWeeks(initialYear, initialWeek, 12);
      setToYear(range.year);
      setToWeek(range.week);
      setMode('PERCENTAGE');
      setPercentage(50);
      setAllocatedHours(20);
      setErrorMessage(null);
    }
  }, [open, initialEmployeeId, initialYear, initialWeek, employees]);

  // Load Active Projects
  useEffect(() => {
    if (!open) return;
    let isMounted = true;
    async function loadActiveProjects() {
      setIsLoadingProjects(true);
      try {
        const res = await getProjects(0, 100);
        if (!isMounted) return;
        const activeList = (res.content || []).filter((p) => p.status === 'ACTIVE');
        setProjects(activeList);
        if (activeList.length > 0 && selectedProjectId === '') {
          setSelectedProjectId(activeList[0].id);
        }
      } catch (err) {
        console.error('Không thể tải danh sách dự án:', err);
      } finally {
        if (isMounted) setIsLoadingProjects(false);
      }
    }
    loadActiveProjects();
    return () => {
      isMounted = false;
    };
  }, [open]);

  // Preset buttons handler (4, 8, 12 tuần) theo ISO-8601
  const applyPresetWeeks = (weeksCount: number) => {
    const range = addIsoWeeks(fromYear, fromWeek, weeksCount);
    setToYear(range.year);
    setToWeek(range.week);
  };

  const handlePercentagePreset = (pct: number) => {
    setPercentage(pct);
    setAllocatedHours(Math.round((40 * pct) / 100));
  };

  const handlePercentageSlider = (pct: number) => {
    const clamped = Math.max(0, Math.min(100, pct));
    setPercentage(clamped);
    setAllocatedHours(Number(((40 * clamped) / 100).toFixed(1)));
  };

  const handleHoursSlider = (hours: number) => {
    const clamped = Math.max(0, Math.min(168, hours));
    setAllocatedHours(clamped);
    setPercentage(Math.round((clamped / 40) * 100));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!selectedEmployeeId) {
      setErrorMessage('Vui lòng chọn nhân sự');
      return;
    }
    if (!selectedProjectId) {
      setErrorMessage('Vui lòng chọn dự án đang hoạt động');
      return;
    }

    if (mode === 'HOURS') {
      if (allocatedHours <= 0 || allocatedHours > 168) {
        setErrorMessage('Số giờ mỗi tuần phải lớn hơn 0 và không vượt quá 168 giờ');
        return;
      }
    } else {
      if (percentage < 0 || percentage > 100) {
        setErrorMessage('Tỷ lệ phần trăm phải nằm trong khoảng từ 0% đến 100%');
        return;
      }
    }

    setIsSubmitting(true);
    try {
      const result = await bulkAllocateResource({
        employeeId: Number(selectedEmployeeId),
        projectId: Number(selectedProjectId),
        fromYear,
        fromWeek,
        toYear,
        toWeek,
        allocatedHoursPerWeek: mode === 'HOURS' ? allocatedHours : undefined,
        allocationPercentagePerWeek: mode === 'PERCENTAGE' ? percentage : undefined,
      });

      onSuccess(result);
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Phân bổ hàng loạt thất bại';
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in">
      <div className="w-full max-w-lg overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 p-4">
          <div className="flex items-center gap-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-100 text-indigo-700">
              <Layers className="h-4 w-4" />
            </span>
            <div>
              <h3 className="text-sm font-bold text-slate-800">
                Phân Bổ Nguồn Lực Hàng Loạt Nhiều Tuần
              </h3>
              <p className="text-[11px] text-slate-500">
                NCL-06-CN-006 & NCL-06-CN-007 • Hỗ trợ theo Số Giờ và Tỷ Lệ %
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition cursor-pointer"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4 text-xs">
          {errorMessage && (
            <div className="flex items-start gap-2 rounded-xl border border-rose-200 bg-rose-50 p-3 text-rose-800">
              <AlertCircle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
              <div className="text-xs leading-relaxed">{errorMessage}</div>
            </div>
          )}

          {/* 1. Chọn Nhân sự */}
          <div>
            <label className="mb-1 block font-semibold text-slate-700">
              Nhân sự cần phân bổ <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              <select
                value={selectedEmployeeId}
                onChange={(e) => setSelectedEmployeeId(Number(e.target.value))}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                required
              >
                <option value="">-- Chọn nhân sự --</option>
                {employees.map((emp) => (
                  <option key={emp.id} value={emp.id}>
                    [{emp.code}] {emp.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* 2. Chọn Dự án */}
          <div>
            <label className="mb-1 block font-semibold text-slate-700">
              Dự án tiếp nhận <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              {isLoadingProjects ? (
                <div className="flex items-center gap-2 text-slate-400 py-2">
                  <Loader2 className="h-4 w-4 animate-spin text-indigo-600" />
                  <span>Đang tải danh sách dự án...</span>
                </div>
              ) : (
                <select
                  value={selectedProjectId}
                  onChange={(e) => setSelectedProjectId(Number(e.target.value))}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  required
                >
                  <option value="">-- Chọn dự án đang hoạt động --</option>
                  {projects.map((proj) => (
                    <option key={proj.id} value={proj.id}>
                      [{proj.projectCode}] {proj.projectName}
                    </option>
                  ))}
                </select>
              )}
            </div>
          </div>

          {/* 3. Khoảng thời gian phân bổ */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="font-semibold text-slate-700">
                Khoảng tuần phân bổ <span className="text-rose-500">*</span>
              </label>
              {/* Presets */}
              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => applyPresetWeeks(4)}
                  className="rounded-md border border-slate-200 bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-600 hover:bg-indigo-50 hover:text-indigo-600 hover:border-indigo-200 transition cursor-pointer"
                >
                  4 tuần
                </button>
                <button
                  type="button"
                  onClick={() => applyPresetWeeks(8)}
                  className="rounded-md border border-slate-200 bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-600 hover:bg-indigo-50 hover:text-indigo-600 hover:border-indigo-200 transition cursor-pointer"
                >
                  8 tuần
                </button>
                <button
                  type="button"
                  onClick={() => applyPresetWeeks(12)}
                  className="rounded-md border border-indigo-200 bg-indigo-50 px-2 py-0.5 text-[10px] font-bold text-indigo-700 hover:bg-indigo-100 transition cursor-pointer"
                >
                  12 tuần (1 Quý)
                </button>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 rounded-xl border border-slate-200 bg-slate-50/50 p-3">
              {/* Từ tuần */}
              <div>
                <span className="text-[11px] font-medium text-slate-500">Từ:</span>
                <div className="mt-1 flex items-center gap-1.5">
                  <input
                    type="number"
                    min="1"
                    max="53"
                    value={fromWeek}
                    onChange={(e) => setFromWeek(Number(e.target.value))}
                    className="w-16 rounded-md border border-slate-300 bg-white px-2 py-1 text-center font-bold text-slate-800"
                    title="Tuần bắt đầu"
                  />
                  <span className="text-slate-400">/</span>
                  <input
                    type="number"
                    min="2000"
                    max="2100"
                    value={fromYear}
                    onChange={(e) => setFromYear(Number(e.target.value))}
                    className="w-20 rounded-md border border-slate-300 bg-white px-2 py-1 text-center font-bold text-slate-800"
                    title="Năm bắt đầu"
                  />
                </div>
              </div>

              {/* Đến tuần */}
              <div>
                <span className="text-[11px] font-medium text-slate-500">Đến:</span>
                <div className="mt-1 flex items-center gap-1.5">
                  <input
                    type="number"
                    min="1"
                    max="53"
                    value={toWeek}
                    onChange={(e) => setToWeek(Number(e.target.value))}
                    className="w-16 rounded-md border border-slate-300 bg-white px-2 py-1 text-center font-bold text-slate-800"
                    title="Tuần kết thúc"
                  />
                  <span className="text-slate-400">/</span>
                  <input
                    type="number"
                    min="2000"
                    max="2100"
                    value={toYear}
                    onChange={(e) => setToYear(Number(e.target.value))}
                    className="w-20 rounded-md border border-slate-300 bg-white px-2 py-1 text-center font-bold text-slate-800"
                    title="Năm kết thúc"
                  />
                </div>
              </div>
            </div>
          </div>

          {/* 4. Định mức phân bổ: Toggle Mode (% vs Hours) */}
          <div className="space-y-2">
            <div className="flex items-center rounded-xl bg-slate-100 p-1">
              <button
                type="button"
                onClick={() => setMode('PERCENTAGE')}
                className={`flex flex-1 items-center justify-center gap-1.5 rounded-lg py-1.5 text-xs font-semibold transition cursor-pointer ${
                  mode === 'PERCENTAGE'
                    ? 'bg-white text-indigo-600 shadow-2xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Percent className="h-3.5 w-3.5" />
                <span>Theo Tỷ Lệ Phần Trăm (%)</span>
              </button>
              <button
                type="button"
                onClick={() => setMode('HOURS')}
                className={`flex flex-1 items-center justify-center gap-1.5 rounded-lg py-1.5 text-xs font-semibold transition cursor-pointer ${
                  mode === 'HOURS'
                    ? 'bg-white text-indigo-600 shadow-2xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Clock className="h-3.5 w-3.5" />
                <span>Theo Số Giờ Cố Định (h)</span>
              </button>
            </div>

            {mode === 'PERCENTAGE' ? (
              <div className="space-y-2.5 rounded-xl border border-indigo-100 bg-indigo-50/40 p-3">
                {/* Presets */}
                <div className="grid grid-cols-4 gap-1.5">
                  {[25, 50, 75, 100].map((preset) => (
                    <button
                      key={preset}
                      type="button"
                      onClick={() => handlePercentagePreset(preset)}
                      className={`rounded-lg py-1 text-xs font-bold border transition cursor-pointer ${
                        percentage === preset
                          ? 'border-indigo-600 bg-indigo-600 text-white'
                          : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                      }`}
                    >
                      {preset}%
                    </button>
                  ))}
                </div>

                <div>
                  <div className="mb-1 flex items-center justify-between">
                    <label className="font-semibold text-slate-700">Tỷ lệ phân bổ mỗi tuần:</label>
                    <span className="rounded-md bg-indigo-100 px-2 py-0.5 text-[11px] font-bold text-indigo-700">
                      {percentage}% khả dụng
                    </span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    step="5"
                    value={percentage}
                    onChange={(e) => handlePercentageSlider(Number(e.target.value))}
                    className="w-full cursor-pointer accent-indigo-600"
                  />
                  <div className="flex justify-between text-[10px] text-slate-400 mt-0.5">
                    <span>0%</span>
                    <span>25%</span>
                    <span>50%</span>
                    <span>75%</span>
                    <span>100%</span>
                  </div>
                </div>

                {/* Live Preview */}
                <div className="text-[11px] text-indigo-900 font-medium bg-white/80 p-2 rounded-lg border border-indigo-100">
                  <span>Quy đổi dự kiến: </span>
                  <strong className="text-indigo-700 font-bold">{percentage}%</strong>
                  <span> khả dụng mỗi tuần (khoảng </span>
                  <strong className="text-indigo-700 font-bold">{allocatedHours}h/tuần</strong>
                  <span> đối với tuần chuẩn 40h). Số giờ thực tế sẽ tự động tính theo giờ khả dụng từng tuần (đã trừ nghỉ phép/lễ).</span>
                </div>
              </div>
            ) : (
              <div className="space-y-2 rounded-xl border border-slate-200 bg-slate-50/50 p-3">
                <div className="flex items-center justify-between mb-1">
                  <label className="font-semibold text-slate-700">
                    Số giờ cố định mỗi tuần:
                  </label>
                  <span className="rounded-md bg-emerald-50 px-2 py-0.5 text-[11px] font-bold text-emerald-700 border border-emerald-200">
                    {allocatedHours}h / tuần ({percentage}%)
                  </span>
                </div>
                <input
                  type="range"
                  min="2"
                  max="40"
                  step="2"
                  value={allocatedHours}
                  onChange={(e) => handleHoursSlider(Number(e.target.value))}
                  className="w-full cursor-pointer accent-indigo-600"
                />
                <div className="flex justify-between text-[10px] text-slate-400 mt-0.5">
                  <span>10h (Part-time)</span>
                  <span className="font-bold text-indigo-600">20h (50%)</span>
                  <span className="font-bold text-slate-600">40h (Full-time)</span>
                </div>
              </div>
            )}
          </div>

          {/* Note on QTN-11 */}
          <div className="rounded-xl border border-indigo-100 bg-indigo-50/50 p-2.5 text-[11px] text-indigo-900 leading-relaxed">
            <span className="font-semibold">💡 Lưu ý nghiệp vụ:</span> Hệ thống sẽ tự động kiểm tra năng lực khả dụng từng tuần (QTN-11) và hợp đồng nhân sự (QTN-05). Các tuần đủ điều kiện sẽ được phân bổ thành công, các tuần bị vướng sẽ được báo cáo chi tiết trong kết quả.
          </div>

          {/* Footer Buttons */}
          <div className="flex items-center justify-end gap-2 border-t border-slate-200 pt-4">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2 font-medium text-slate-700 hover:bg-slate-50 transition cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2 font-semibold text-white shadow-xs hover:bg-indigo-700 disabled:opacity-50 transition cursor-pointer"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>Đang xử lý...</span>
                </>
              ) : (
                <>
                  <Sparkles className="h-4 w-4" />
                  <span>Tiến hành phân bổ</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
