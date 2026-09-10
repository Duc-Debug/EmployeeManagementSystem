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
  const [isSubmitting, setIsSubmitting] = useState(false);

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
    setErrorMsg(null);
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

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!predecessorId || !successorId) {
      setErrorMsg('Vui lòng chọn đầy đủ Công việc Tiền đề và Công việc Phụ thuộc');
      return;
    }
    if (predecessorId === successorId) {
      setErrorMsg('Công việc không thể phụ thuộc vào chính nó');
      return;
    }

    setIsSubmitting(true);
    setErrorMsg(null);
    setSuccessMsg(null);

    try {
      await createTaskDependency(projectId, {
        predecessorId: Number(predecessorId),
        successorId: Number(successorId),
        dependencyType: 'FINISH_TO_START',
        lagDays: 0,
      });
      setSuccessMsg('Khai báo phụ thuộc công việc thành công');
      setPredecessorId('');
      setSuccessorId('');
      await loadDependencies();
    } catch (err: any) {
      const msg = err?.message || 'Không thể tạo quan hệ phụ thuộc. Phát hiện vòng lặp hoặc dữ liệu không hợp lệ.';
      setErrorMsg(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (depId: number) => {
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      await deleteTaskDependency(projectId, depId);
      setSuccessMsg('Đã xóa phụ thuộc công việc');
      setDependencies((prev) => prev.filter((d) => d.id !== depId));
    } catch (err: any) {
      setErrorMsg(err?.message || 'Xóa phụ thuộc công việc thất bại');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-4xl rounded-2xl bg-white p-6 shadow-2xl dark:bg-gray-900 border border-gray-100 dark:border-gray-800">
        {/* Header */}
        <div className="flex items-center justify-between border-b pb-4 dark:border-gray-800">
          <div className="flex items-center gap-2.5 text-indigo-600 dark:text-indigo-400">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-200 dark:border-indigo-800">
              <GitCommit className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-gray-900 dark:text-white">
                Khai báo Phụ thuộc Công việc (Task Dependencies)
              </h2>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Phân nhóm theo Hạng mục & Cây công việc WBS (Finish-to-Start)
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-700 transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Notifications */}
        {errorMsg && (
          <div className="mt-4 flex items-start gap-2 rounded-xl bg-red-50 p-3.5 text-sm text-red-700 dark:bg-red-900/30 dark:text-red-400 border border-red-200 dark:border-red-800">
            <ShieldAlert className="h-5 w-5 shrink-0 mt-0.5" />
            <p className="flex-1 font-medium">{errorMsg}</p>
          </div>
        )}

        {successMsg && (
          <div className="mt-4 flex items-center gap-2 rounded-xl bg-emerald-50 p-3.5 text-sm text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800">
            <CheckCircle2 className="h-5 w-5 shrink-0" />
            <p className="font-medium">{successMsg}</p>
          </div>
        )}

        {/* Form thêm mới */}
        {canManage && (
          <form onSubmit={handleCreate} className="mt-4 rounded-xl bg-slate-50 p-4 dark:bg-gray-800/60 border border-slate-200 dark:border-gray-700/60">
            <div className="flex items-center gap-2 mb-3">
              <Folder className="h-4 w-4 text-indigo-500" />
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-700 dark:text-gray-300">
                Khai báo phụ thuộc mới (WBS Tree View)
              </h3>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-5 gap-3 items-center">
              {/* Task Tiền đề */}
              <div className="md:col-span-2">
                <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 mb-1">
                  1. Công việc Tiền đề (Xong trước)
                </label>
                <select
                  value={predecessorId}
                  onChange={(e) => setPredecessorId(e.target.value ? Number(e.target.value) : '')}
                  className="w-full rounded-xl border border-gray-300 p-2.5 text-xs font-medium dark:bg-gray-800 dark:border-gray-600 dark:text-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
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

              <div className="flex justify-center text-indigo-500 hidden md:flex pt-4">
                <div className="flex items-center justify-center h-8 w-8 rounded-full bg-indigo-100 dark:bg-indigo-900/50">
                  <ArrowRight className="h-4 w-4" />
                </div>
              </div>

              {/* Task Phụ thuộc */}
              <div className="md:col-span-2">
                <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 mb-1">
                  2. Công việc Phụ thuộc (Bắt đầu sau)
                </label>
                <select
                  value={successorId}
                  onChange={(e) => setSuccessorId(e.target.value ? Number(e.target.value) : '')}
                  className="w-full rounded-xl border border-gray-300 p-2.5 text-xs font-medium dark:bg-gray-800 dark:border-gray-600 dark:text-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
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

            <div className="mt-3.5 flex justify-end">
              <button
                type="submit"
                disabled={isSubmitting}
                className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-indigo-200 dark:shadow-none hover:bg-indigo-700 disabled:opacity-50 transition active:scale-95 cursor-pointer"
              >
                <Plus className="h-4 w-4 stroke-[2.5]" />
                {isSubmitting ? 'Đang lưu...' : 'Thêm phụ thuộc (Finish-to-Start)'}
              </button>
            </div>
          </form>
        )}

        {/* Danh sách quan hệ hiện tại dạng Tree Cards */}
        <div className="mt-6">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 mb-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-gray-700 dark:text-gray-300 flex items-center gap-2">
              <span>Danh sách Đồ thị Phụ thuộc ({filteredDependencies.length})</span>
            </h3>

            {dependencies.length > 0 && (
              <div className="relative w-full sm:w-64">
                <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-gray-400" />
                <input
                  type="text"
                  placeholder="Tìm theo task hoặc hạng mục..."
                  value={searchFilter}
                  onChange={(e) => setSearchFilter(e.target.value)}
                  className="w-full rounded-lg border border-gray-200 pl-8 pr-3 py-1.5 text-xs dark:bg-gray-800 dark:border-gray-700 dark:text-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
              </div>
            )}
          </div>

          {isLoading ? (
            <div className="py-8 text-center text-xs text-gray-500">Đang tải dữ liệu đồ thị phụ thuộc WBS...</div>
          ) : filteredDependencies.length === 0 ? (
            <div className="py-10 text-center text-xs text-gray-500 border border-dashed rounded-xl dark:border-gray-800">
              {searchFilter ? 'Không tìm thấy quan hệ phụ thuộc phù hợp.' : 'Chưa có quan hệ phụ thuộc nào được khai báo cho dự án này.'}
            </div>
          ) : (
            <div className="max-h-72 overflow-y-auto space-y-2.5 pr-1">
              {filteredDependencies.map((dep) => {
                const predCat = taskCategoryMap.get(dep.predecessorId);
                const succCat = taskCategoryMap.get(dep.successorId);

                return (
                  <div
                    key={dep.id}
                    className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 p-3.5 rounded-xl border border-slate-200 dark:border-gray-800 bg-white dark:bg-gray-800/80 hover:shadow-sm hover:border-indigo-200 dark:hover:border-indigo-900 transition"
                  >
                    <div className="flex flex-1 flex-col sm:flex-row items-start sm:items-center gap-3 text-xs w-full">
                      {/* Node Tiền đề */}
                      <div className="flex-1 bg-slate-50 dark:bg-gray-900/70 p-2.5 rounded-lg border border-slate-200/80 dark:border-gray-700/60 w-full sm:w-auto">
                        {predCat && (
                          <div className="flex items-center gap-1 text-[11px] font-medium text-slate-500 dark:text-gray-400 mb-1">
                            <Folder className="h-3 w-3 text-indigo-500 shrink-0" />
                            <span className="truncate">{predCat}</span>
                          </div>
                        )}
                        <div className="flex items-center gap-2">
                          <FileText className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                          <span className="font-bold text-indigo-600 dark:text-indigo-400 font-mono">
                            {dep.predecessorTaskCode || `#${dep.predecessorId}`}
                          </span>
                          <span className="font-semibold text-slate-800 dark:text-slate-200 truncate">
                            {dep.predecessorTaskName}
                          </span>
                        </div>
                      </div>

                      {/* Icon liên kết */}
                      <div className="flex items-center gap-1.5 self-center text-indigo-500 font-medium text-[10px] bg-indigo-50 dark:bg-indigo-950/80 px-2.5 py-1 rounded-full border border-indigo-200 dark:border-indigo-800/80 shrink-0">
                        <span>FS (Phải xong mới được bắt đầu)</span>
                        <ArrowRight className="h-3.5 w-3.5" />
                      </div>

                      {/* Node Phụ thuộc */}
                      <div className="flex-1 bg-slate-50 dark:bg-gray-900/70 p-2.5 rounded-lg border border-slate-200/80 dark:border-gray-700/60 w-full sm:w-auto">
                        {succCat && (
                          <div className="flex items-center gap-1 text-[11px] font-medium text-slate-500 dark:text-gray-400 mb-1">
                            <Folder className="h-3 w-3 text-purple-500 shrink-0" />
                            <span className="truncate">{succCat}</span>
                          </div>
                        )}
                        <div className="flex items-center gap-2">
                          <FileText className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                          <span className="font-bold text-purple-600 dark:text-purple-400 font-mono">
                            {dep.successorTaskCode || `#${dep.successorId}`}
                          </span>
                          <span className="font-semibold text-slate-800 dark:text-slate-200 truncate">
                            {dep.successorTaskName}
                          </span>
                        </div>
                      </div>
                    </div>

                    {canManage && (
                      <button
                        onClick={() => handleDelete(dep.id)}
                        className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-950/50 transition self-end sm:self-center cursor-pointer"
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

        {/* Footer */}
        <div className="mt-6 flex justify-end border-t pt-4 dark:border-gray-800">
          <button
            onClick={onClose}
            className="rounded-xl border border-gray-300 px-4 py-2 text-xs font-semibold text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-800 transition cursor-pointer"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
};
