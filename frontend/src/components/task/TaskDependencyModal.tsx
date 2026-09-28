import React, { useState, useEffect, useMemo } from 'react';
import {
  X,
  Plus,
  Trash2,
  GitCommit,
  ArrowRight,
  ShieldAlert,
  CheckCircle2,
  Folder,
  FileText,
  Search,
  Clock,
  HelpCircle,
} from 'lucide-react';
import {
  getTaskDependencies,
  createTaskDependency,
  deleteTaskDependency,
  type TaskDependencyResult,
} from '@/lib/api/taskDependencies';

export interface TaskItem {
  id: number;
  taskCode?: string;
  name: string;
  categoryName?: string;
}

interface TaskDependencyModalProps {
  open: boolean;
  projectId: number;
  tasks: TaskItem[];
  canManage?: boolean;
  onClose: () => void;
}

export interface DependencyTypeOption {
  value: string;
  label: string;
  shortLabel: string;
  description: string;
  badgeBg: string;
  badgeText: string;
  badgeBorder: string;
}

export const DEPENDENCY_TYPE_OPTIONS: DependencyTypeOption[] = [
  {
    value: 'FINISH_TO_START',
    label: 'FS - Kết thúc để Bắt đầu (Finish-to-Start)',
    shortLabel: 'FS (Kết thúc - Bắt đầu)',
    description: 'Công việc Tiền đề phải hoàn thành xong thì Công việc Phụ thuộc mới được bắt đầu.',
    badgeBg: 'bg-indigo-50',
    badgeText: 'text-indigo-700',
    badgeBorder: 'border-indigo-200',
  },
  {
    value: 'START_TO_START',
    label: 'SS - Cùng Bắt đầu (Start-to-Start)',
    shortLabel: 'SS (Bắt đầu - Bắt đầu)',
    description: 'Công việc Tiền đề bắt đầu thì Công việc Phụ thuộc mới có thể cùng bắt đầu.',
    badgeBg: 'bg-sky-50',
    badgeText: 'text-sky-700',
    badgeBorder: 'border-sky-200',
  },
  {
    value: 'FINISH_TO_FINISH',
    label: 'FF - Cùng Kết thúc (Finish-to-Finish)',
    shortLabel: 'FF (Kết thúc - Kết thúc)',
    description: 'Công việc Tiền đề phải hoàn thành xong thì Công việc Phụ thuộc mới được phép kết thúc.',
    badgeBg: 'bg-purple-50',
    badgeText: 'text-purple-700',
    badgeBorder: 'border-purple-200',
  },
  {
    value: 'START_TO_FINISH',
    label: 'SF - Bắt đầu để Kết thúc (Start-to-Finish)',
    shortLabel: 'SF (Bắt đầu - Kết thúc)',
    description: 'Công việc Tiền đề bắt đầu thì Công việc Phụ thuộc mới được hoàn thành.',
    badgeBg: 'bg-amber-50',
    badgeText: 'text-amber-700',
    badgeBorder: 'border-amber-200',
  },
];

const DEPENDENCY_TYPE_CONFIG: Record<string, { label: string; bg: string; text: string; border: string }> = {
  FINISH_TO_START: {
    label: 'FS (Kết thúc - Bắt đầu)',
    bg: 'bg-indigo-50',
    text: 'text-indigo-700',
    border: 'border-indigo-200',
  },
  START_TO_START: {
    label: 'SS (Bắt đầu - Bắt đầu)',
    bg: 'bg-sky-50',
    text: 'text-sky-700',
    border: 'border-sky-200',
  },
  FINISH_TO_FINISH: {
    label: 'FF (Kết thúc - Kết thúc)',
    bg: 'bg-purple-50',
    text: 'text-purple-700',
    border: 'border-purple-200',
  },
  START_TO_FINISH: {
    label: 'SF (Bắt đầu - Kết thúc)',
    bg: 'bg-amber-50',
    text: 'text-amber-700',
    border: 'border-amber-200',
  },
};

export const TaskDependencyModal: React.FC<TaskDependencyModalProps> = ({
  open,
  projectId,
  tasks,
  canManage = true,
  onClose,
}) => {
  const [dependencies, setDependencies] = useState<TaskDependencyResult[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [searchFilter, setSearchFilter] = useState('');

  // Form states
  const [predecessorId, setPredecessorId] = useState<number | ''>('');
  const [successorId, setSuccessorId] = useState<number | ''>('');
  const [dependencyType, setDependencyType] = useState<string>('FINISH_TO_START');
  const [lagDays, setLagDays] = useState<number>(0);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Auto-dismiss notifications after timeout
  useEffect(() => {
    if (!errorMsg) return;
    const timer = setTimeout(() => {
      setErrorMsg(null);
    }, 4000);
    return () => clearTimeout(timer);
  }, [errorMsg]);

  useEffect(() => {
    if (!successMsg) return;
    const timer = setTimeout(() => {
      setSuccessMsg(null);
    }, 3500);
    return () => clearTimeout(timer);
  }, [successMsg]);

  const triggerError = (msg: string) => {
    setSuccessMsg(null);
    setErrorMsg(msg);
  };

  const triggerSuccess = (msg: string) => {
    setErrorMsg(null);
    setSuccessMsg(msg);
  };

  // Group tasks by category/folder
  const groupedTasks = useMemo(() => {
    const groups: Record<string, TaskItem[]> = {};
    tasks.forEach((t) => {
      const cat = t.categoryName || 'Khác / Chưa phân nhóm';
      if (!groups[cat]) groups[cat] = [];
      groups[cat].push(t);
    });
    return groups;
  }, [tasks]);

  // Quick lookup for task category by ID
  const taskCategoryMap = useMemo(() => {
    const map = new Map<number, string>();
    tasks.forEach((t) => {
      if (t.categoryName) map.set(t.id, t.categoryName);
    });
    return map;
  }, [tasks]);

  const loadDependencies = async () => {
    setIsLoading(true);
    try {
      const graph = await getTaskDependencies(projectId);
      setDependencies(graph.dependencies || []);
    } catch (err: any) {
      console.warn('Load task dependencies error:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (open && projectId) {
      setErrorMsg(null);
      setSuccessMsg(null);
      loadDependencies();
    }
  }, [open, projectId]);

  const filteredDependencies = useMemo(() => {
    if (!searchFilter.trim()) return dependencies;
    const q = searchFilter.toLowerCase();
    return dependencies.filter((d) => {
      const pCat = (taskCategoryMap.get(d.predecessorId) || '').toLowerCase();
      const sCat = (taskCategoryMap.get(d.successorId) || '').toLowerCase();
      return (
        d.predecessorTaskName.toLowerCase().includes(q) ||
        d.successorTaskName.toLowerCase().includes(q) ||
        (d.predecessorTaskCode && d.predecessorTaskCode.toLowerCase().includes(q)) ||
        (d.successorTaskCode && d.successorTaskCode.toLowerCase().includes(q)) ||
        pCat.includes(q) ||
        sCat.includes(q)
      );
    });
  }, [dependencies, searchFilter, taskCategoryMap]);

  if (!open) return null;

  const currentTypeInfo = DEPENDENCY_TYPE_OPTIONS.find((opt) => opt.value === dependencyType) || DEPENDENCY_TYPE_OPTIONS[0];

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!predecessorId || !successorId) {
      triggerError('Vui lòng chọn đầy đủ Công việc Tiền đề và Công việc Phụ thuộc');
      return;
    }
    if (predecessorId === successorId) {
      triggerError('Công việc không thể phụ thuộc vào chính nó');
      return;
    }

    setIsSubmitting(true);

    try {
      await createTaskDependency(projectId, {
        predecessorId: Number(predecessorId),
        successorId: Number(successorId),
        dependencyType,
        lagDays: Math.max(0, Number(lagDays) || 0),
      });
      triggerSuccess('Khai báo phụ thuộc công việc thành công');
      setPredecessorId('');
      setSuccessorId('');
      setLagDays(0);
      await loadDependencies();
    } catch (err: any) {
      const msg = err?.message || 'Không thể tạo quan hệ phụ thuộc. Phát hiện vòng lặp hoặc dữ liệu không hợp lệ.';
      triggerError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (depId: number) => {
    try {
      await deleteTaskDependency(projectId, depId);
      triggerSuccess('Đã xóa phụ thuộc công việc thành công');
      setDependencies((prev) => prev.filter((d) => d.id !== depId));
    } catch (err: any) {
      triggerError(err?.message || 'Xóa phụ thuộc công việc thất bại');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in overflow-y-auto">
      <div className="w-full max-w-4xl overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all my-8">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 px-5 py-4">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-100 text-indigo-700">
              <GitCommit className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-900">
                Khai Báo Phụ Thuộc Công Việc
              </h3>
              <p className="text-[11px] text-slate-500">
                Hỗ trợ đa dạng loại phụ thuộc (FS, SS, FF, SF) & Phân nhóm theo Cây công việc WBS
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition cursor-pointer"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="p-5 space-y-4">
          {/* Notifications with Auto-Dismiss and manual close button */}
          {errorMsg && (
            <div className="flex items-center justify-between gap-2.5 rounded-xl bg-rose-50 p-3.5 text-xs text-rose-700 border border-rose-200 shadow-xs animate-in fade-in duration-200">
              <div className="flex items-start gap-2.5">
                <ShieldAlert className="h-4 w-4 shrink-0 mt-0.5 text-rose-600" />
                <p className="font-medium">{errorMsg}</p>
              </div>
              <button
                type="button"
                onClick={() => setErrorMsg(null)}
                className="text-rose-400 hover:text-rose-700 transition p-1 cursor-pointer rounded"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            </div>
          )}

          {successMsg && (
            <div className="flex items-center justify-between gap-2.5 rounded-xl bg-emerald-50 p-3.5 text-xs text-emerald-700 border border-emerald-200 shadow-xs animate-in fade-in duration-200">
              <div className="flex items-center gap-2.5">
                <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
                <p className="font-medium">{successMsg}</p>
              </div>
              <button
                type="button"
                onClick={() => setSuccessMsg(null)}
                className="text-emerald-400 hover:text-emerald-700 transition p-1 cursor-pointer rounded"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            </div>
          )}

          {/* Form thêm mới */}
          {canManage && (
            <form onSubmit={handleCreate} className="rounded-xl bg-slate-50 p-4 border border-slate-200 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Folder className="h-4 w-4 text-indigo-600" />
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-800">
                    Thêm quan hệ phụ thuộc mới
                  </h4>
                </div>
                <span className="text-[11px] text-slate-500 hidden sm:inline-block">
                  Chọn 2 công việc và thiết lập loại phụ thuộc liên kết
                </span>
              </div>

              {/* Hàng 1: Task Tiền đề - Loại phụ thuộc - Task Kế thừa */}
              <div className="grid grid-cols-1 md:grid-cols-12 gap-3 items-end">
                {/* Task Tiền đề */}
                <div className="md:col-span-5">
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    1. Công việc Tiền đề (Tiền nhiệm) <span className="text-rose-500">*</span>
                  </label>
                  <select
                    value={predecessorId}
                    onChange={(e) => {
                      setPredecessorId(e.target.value ? Number(e.target.value) : '');
                      setErrorMsg(null);
                    }}
                    className="w-full rounded-xl border border-slate-300 bg-white p-2.5 text-xs font-medium text-slate-800 shadow-2xs focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 transition"
                  >
                    <option value="">-- Chọn công việc tiền đề --</option>
                    {Object.entries(groupedTasks).map(([catName, catTasks]) => (
                      <optgroup key={catName} label={`📁 Hạng mục: ${catName}`}>
                        {catTasks.map((t) => (
                          <option key={t.id} value={t.id}>
                            {t.taskCode ? `[${t.taskCode}] ` : ''}{t.name}
                          </option>
                        ))}
                      </optgroup>
                    ))}
                  </select>
                </div>

                {/* Loại phụ thuộc */}
                <div className="md:col-span-3">
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Loại phụ thuộc <span className="text-rose-500">*</span>
                  </label>
                  <select
                    value={dependencyType}
                    onChange={(e) => setDependencyType(e.target.value)}
                    className="w-full rounded-xl border border-indigo-200 bg-indigo-50/50 p-2.5 text-xs font-bold text-indigo-900 shadow-2xs focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 transition"
                  >
                    {DEPENDENCY_TYPE_OPTIONS.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.shortLabel}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Task Phụ thuộc */}
                <div className="md:col-span-4">
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    2. Công việc Phụ thuộc (Kế nhiệm) <span className="text-rose-500">*</span>
                  </label>
                  <select
                    value={successorId}
                    onChange={(e) => {
                      setSuccessorId(e.target.value ? Number(e.target.value) : '');
                      setErrorMsg(null);
                    }}
                    className="w-full rounded-xl border border-slate-300 bg-white p-2.5 text-xs font-medium text-slate-800 shadow-2xs focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 transition"
                  >
                    <option value="">-- Chọn công việc phụ thuộc --</option>
                    {Object.entries(groupedTasks).map(([catName, catTasks]) => (
                      <optgroup key={catName} label={`📁 Hạng mục: ${catName}`}>
                        {catTasks.map((t) => (
                          <option key={t.id} value={t.id}>
                            {t.taskCode ? `[${t.taskCode}] ` : ''}{t.name}
                          </option>
                        ))}
                      </optgroup>
                    ))}
                  </select>
                </div>
              </div>

              {/* Hàng 2: Giải thích loại phụ thuộc & Số ngày chờ & Nút thêm */}
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pt-2 border-t border-slate-200/80">
                <div className="flex items-center gap-4 flex-1">
                  <div className="flex items-center gap-1.5 text-xs text-slate-600 bg-white px-3 py-1.5 rounded-lg border border-slate-200">
                    <HelpCircle className="h-3.5 w-3.5 text-indigo-600 shrink-0" />
                    <span>{currentTypeInfo.description}</span>
                  </div>

                  <div className="flex items-center gap-1.5 shrink-0">
                    <Clock className="h-3.5 w-3.5 text-slate-400" />
                    <label className="text-xs text-slate-600 font-medium">Độ trễ (ngày):</label>
                    <input
                      type="number"
                      min={0}
                      value={lagDays}
                      onChange={(e) => setLagDays(Math.max(0, parseInt(e.target.value, 10) || 0))}
                      className="w-16 rounded-lg border border-slate-300 bg-white px-2 py-1 text-xs text-center font-bold text-slate-800 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-indigo-200 hover:bg-indigo-700 disabled:opacity-50 transition active:scale-95 cursor-pointer shrink-0"
                >
                  <Plus className="h-4 w-4 stroke-[2.5]" />
                  {isSubmitting ? 'Đang lưu...' : `Thêm phụ thuộc (${currentTypeInfo.value.split('_').map(w => w[0]).join('')})`}
                </button>
              </div>
            </form>
          )}

          {/* Danh sách quan hệ hiện tại dạng Tree Cards */}
          <div>
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 mb-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
                <span>Danh sách Đồ thị Phụ thuộc ({filteredDependencies.length})</span>
              </h4>

              {dependencies.length > 0 && (
                <div className="relative w-full sm:w-64">
                  <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-slate-400" />
                  <input
                    type="text"
                    placeholder="Tìm theo task hoặc hạng mục..."
                    value={searchFilter}
                    onChange={(e) => setSearchFilter(e.target.value)}
                    className="w-full rounded-xl border border-slate-200 bg-white pl-8 pr-3 py-1.5 text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-indigo-500 shadow-2xs"
                  />
                </div>
              )}
            </div>

            {isLoading ? (
              <div className="py-8 text-center text-xs text-slate-500">Đang tải dữ liệu đồ thị phụ thuộc WBS...</div>
            ) : filteredDependencies.length === 0 ? (
              <div className="py-10 text-center text-xs text-slate-400 border border-dashed rounded-xl border-slate-200 bg-slate-50/50">
                {searchFilter ? 'Không tìm thấy quan hệ phụ thuộc phù hợp.' : 'Chưa có quan hệ phụ thuộc nào được khai báo cho dự án này.'}
              </div>
            ) : (
              <div className="max-h-72 overflow-y-auto space-y-2.5 pr-1">
                {filteredDependencies.map((dep) => {
                  const predCat = taskCategoryMap.get(dep.predecessorId);
                  const succCat = taskCategoryMap.get(dep.successorId);
                  const typeCfg = DEPENDENCY_TYPE_CONFIG[dep.dependencyType] || {
                    label: dep.dependencyType || 'FS (Kết thúc - Bắt đầu)',
                    bg: 'bg-indigo-50',
                    text: 'text-indigo-700',
                    border: 'border-indigo-200',
                  };

                  return (
                    <div
                      key={dep.id}
                      className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 p-3.5 rounded-xl border border-slate-200 bg-white hover:shadow-sm hover:border-indigo-200 transition"
                    >
                      <div className="flex flex-1 flex-col sm:flex-row items-start sm:items-center gap-3 text-xs w-full">
                        {/* Node Tiền đề */}
                        <div className="flex-1 bg-slate-50 p-2.5 rounded-lg border border-slate-200 w-full sm:w-auto">
                          {predCat && (
                            <div className="flex items-center gap-1 text-[11px] font-medium text-slate-500 mb-1">
                              <Folder className="h-3 w-3 text-indigo-500 shrink-0" />
                              <span className="truncate">{predCat}</span>
                            </div>
                          )}
                          <div className="flex items-center gap-2">
                            <FileText className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                            <span className="font-bold text-indigo-600 font-mono">
                              {dep.predecessorTaskCode || `#${dep.predecessorId}`}
                            </span>
                            <span className="font-semibold text-slate-800 truncate">
                              {dep.predecessorTaskName}
                            </span>
                          </div>
                        </div>

                        {/* Icon liên kết & Loại phụ thuộc */}
                        <div className="flex flex-col items-center gap-1 self-center shrink-0">
                          <div className={`flex items-center gap-1.5 font-medium text-[10px] ${typeCfg.bg} ${typeCfg.text} px-2.5 py-1 rounded-full border ${typeCfg.border}`}>
                            <span>{typeCfg.label}</span>
                            <ArrowRight className="h-3.5 w-3.5" />
                          </div>
                          {dep.lagDays > 0 && (
                            <span className="text-[10px] text-slate-500 font-mono font-medium">
                              +{dep.lagDays} ngày trễ
                            </span>
                          )}
                        </div>

                        {/* Node Phụ thuộc */}
                        <div className="flex-1 bg-slate-50 p-2.5 rounded-lg border border-slate-200 w-full sm:w-auto">
                          {succCat && (
                            <div className="flex items-center gap-1 text-[11px] font-medium text-slate-500 mb-1">
                              <Folder className="h-3 w-3 text-purple-500 shrink-0" />
                              <span className="truncate">{succCat}</span>
                            </div>
                          )}
                          <div className="flex items-center gap-2">
                            <FileText className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                            <span className="font-bold text-purple-600 font-mono">
                              {dep.successorTaskCode || `#${dep.successorId}`}
                            </span>
                            <span className="font-semibold text-slate-800 truncate">
                              {dep.successorTaskName}
                            </span>
                          </div>
                        </div>
                      </div>

                      {canManage && (
                        <button
                          type="button"
                          onClick={() => handleDelete(dep.id)}
                          className="rounded-lg p-2 text-slate-400 hover:bg-rose-50 hover:text-rose-600 transition self-end sm:self-center cursor-pointer"
                          title="Xóa phụ thuộc này"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="flex justify-end border-t border-slate-200 bg-slate-50 px-5 py-3.5">
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition cursor-pointer"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
};

