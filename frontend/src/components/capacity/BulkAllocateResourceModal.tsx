/**
 * NCL-06-CN-006: Modal phân bổ nguồn lực hàng loạt cho nhiều tuần.
 * Cho phép Quản lý nguồn lực chọn nhân sự, dự án, khoảng tuần và số giờ/tuần để phân bổ trong 1 thao tác.
 */
import { useState, useEffect } from 'react';
import { X, Layers, AlertCircle, Loader2, Sparkles } from 'lucide-react';
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
  const [toWeek, setToWeek] = useState<number>(Math.min(initialWeek + 11, 52));
  const [allocatedHours, setAllocatedHours] = useState<number>(20);

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
      
      // Mặc định 12 tuần (1 Quý) theo TC-01
      let endW = initialWeek + 11;
      let endY = initialYear;
      if (endW > 52) {
        endW -= 52;
        endY += 1;
      }
      setToYear(endY);
      setToWeek(endW);
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

  // Preset buttons handler
  const applyPresetWeeks = (weeksCount: number) => {
    let endW = fromWeek + weeksCount - 1;
    let endY = fromYear;
    if (endW > 52) {
      endW -= 52;
      endY += 1;
    }
    setToYear(endY);
    setToWeek(endW);
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
    if (allocatedHours <= 0 || allocatedHours > 168) {
      setErrorMessage('Số giờ mỗi tuần phải lớn hơn 0 và không vượt quá 168 giờ');
      return;
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
        allocatedHoursPerWeek: allocatedHours,
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
                User Story NCL-06-CN-006 • Lập kế hoạch phân bổ theo quý
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition"
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
                  className="rounded-md border border-slate-200 bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-600 hover:bg-indigo-50 hover:text-indigo-600 hover:border-indigo-200 transition"
                >
                  4 tuần
                </button>
                <button
                  type="button"
                  onClick={() => applyPresetWeeks(8)}
                  className="rounded-md border border-slate-200 bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-600 hover:bg-indigo-50 hover:text-indigo-600 hover:border-indigo-200 transition"
                >
                  8 tuần
                </button>
                <button
                  type="button"
                  onClick={() => applyPresetWeeks(12)}
                  className="rounded-md border border-indigo-200 bg-indigo-50 px-2 py-0.5 text-[10px] font-bold text-indigo-700 hover:bg-indigo-100 transition"
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

          {/* 4. Số giờ phân bổ mỗi tuần */}
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="font-semibold text-slate-700">
                Số giờ phân bổ mỗi tuần <span className="text-rose-500">*</span>
              </label>
              <span className="rounded-md bg-emerald-50 px-2 py-0.5 text-[11px] font-bold text-emerald-700 border border-emerald-200">
                {allocatedHours}h / tuần
              </span>
            </div>
            <input
              type="range"
              min="2"
              max="40"
              step="2"
              value={allocatedHours}
              onChange={(e) => setAllocatedHours(Number(e.target.value))}
              className="w-full cursor-pointer accent-indigo-600"
            />
            <div className="flex justify-between text-[10px] text-slate-400 mt-0.5">
              <span>10h (Part-time)</span>
              <span className="font-bold text-indigo-600">20h (50%)</span>
              <span className="font-bold text-slate-600">40h (Full-time)</span>
            </div>
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
              className="rounded-lg border border-slate-300 bg-white px-4 py-2 font-medium text-slate-700 hover:bg-slate-50 transition"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2 font-semibold text-white shadow-xs hover:bg-indigo-700 disabled:opacity-50 transition"
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
