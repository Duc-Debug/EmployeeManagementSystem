import React, { useState, useEffect } from 'react';
import {
  X,
  Plus,
  Trash2,
  AlertCircle,
  GitCommit,
  ArrowRight,
  ShieldAlert,
  CheckCircle2,
} from 'lucide-react';
import {
  getTaskDependencies,
  createTaskDependency,
  deleteTaskDependency,
  TaskDependencyResult,
} from '@/lib/api/taskDependencies';

export interface TaskItem {
  id: number;
  taskCode?: string;
  name: string;
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

  // Form states
  const [predecessorId, setPredecessorId] = useState<number | ''>('');
  const [successorId, setSuccessorId] = useState<number | ''>('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadDependencies = async () => {
    setIsLoading(true);
    setErrorMsg(null);
    try {
      const graph = await getTaskDependencies(projectId);
      setDependencies(graph.dependencies || []);
    } catch (err: any) {
      console.warn('Load task dependencies error:', err);
      // fallback
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (open && projectId) {
      loadDependencies();
    }
  }, [open, projectId]);

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
      <div className="w-full max-w-3xl rounded-xl bg-white p-6 shadow-2xl dark:bg-gray-900 border border-gray-100 dark:border-gray-800">
        {/* Header */}
        <div className="flex items-center justify-between border-b pb-4 dark:border-gray-800">
          <div className="flex items-center gap-2 text-indigo-600 dark:text-indigo-400">
            <GitCommit className="h-6 w-6" />
            <h2 className="text-xl font-bold text-gray-900 dark:text-white">
              Khai báo phụ thuộc giữa các công việc (NCL-04-CN-004)
            </h2>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Notifications */}
        {errorMsg && (
          <div className="mt-4 flex items-start gap-2 rounded-lg bg-red-50 p-3 text-sm text-red-700 dark:bg-red-900/30 dark:text-red-400 border border-red-200 dark:border-red-800">
            <ShieldAlert className="h-5 w-5 shrink-0 mt-0.5" />
            <p className="flex-1 font-medium">{errorMsg}</p>
          </div>
        )}

        {successMsg && (
          <div className="mt-4 flex items-center gap-2 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800">
            <CheckCircle2 className="h-5 w-5 shrink-0" />
            <p className="font-medium">{successMsg}</p>
          </div>
        )}

        {/* Form thêm mới */}
        {canManage && (
          <form onSubmit={handleCreate} className="mt-4 rounded-lg bg-gray-50 p-4 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
              Thêm quan hệ phụ thuộc mới (Finish-to-Start)
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-5 gap-3 items-center">
              <div className="md:col-span-2">
                <label className="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">
                  Công việc Tiền đề (Phải xong trước)
                </label>
                <select
                  value={predecessorId}
                  onChange={(e) => setPredecessorId(e.target.value ? Number(e.target.value) : '')}
                  className="w-full rounded-md border border-gray-300 p-2 text-sm dark:bg-gray-800 dark:border-gray-600 dark:text-white"
                >
                  <option value="">-- Chọn công việc --</option>
                  {tasks.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.taskCode ? `[${t.taskCode}] ` : ''}{t.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex justify-center text-gray-400 hidden md:flex">
                <ArrowRight className="h-5 w-5" />
              </div>

              <div className="md:col-span-2">
                <label className="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">
                  Công việc Phụ thuộc (Bắt đầu sau)
                </label>
                <select
                  value={successorId}
                  onChange={(e) => setSuccessorId(e.target.value ? Number(e.target.value) : '')}
                  className="w-full rounded-md border border-gray-300 p-2 text-sm dark:bg-gray-800 dark:border-gray-600 dark:text-white"
                >
                  <option value="">-- Chọn công việc --</option>
                  {tasks.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.taskCode ? `[${t.taskCode}] ` : ''}{t.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="mt-3 flex justify-end">
              <button
                type="submit"
                disabled={isSubmitting}
                className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50 transition"
              >
                <Plus className="h-4 w-4" />
                {isSubmitting ? 'Đang tạo...' : 'Khai báo phụ thuộc'}
              </button>
            </div>
          </form>
        )}

        {/* Danh sách quan hệ hiện tại */}
        <div className="mt-6">
          <h3 className="text-sm font-semibold text-gray-800 dark:text-gray-200 mb-2">
            Danh sách quan hệ phụ thuộc hiện tại ({dependencies.length})
          </h3>

          {isLoading ? (
            <div className="py-8 text-center text-sm text-gray-500">Đang tải dữ liệu đồ thị...</div>
          ) : dependencies.length === 0 ? (
            <div className="py-8 text-center text-sm text-gray-500 border border-dashed rounded-lg dark:border-gray-800">
              Chưa có mối quan hệ phụ thuộc nào được khai báo cho dự án này.
            </div>
          ) : (
            <div className="max-h-60 overflow-y-auto rounded-lg border border-gray-200 dark:border-gray-800 divide-y dark:divide-gray-800">
              {dependencies.map((dep) => (
                <div
                  key={dep.id}
                  className="flex items-center justify-between p-3 hover:bg-gray-50 dark:hover:bg-gray-800/40 transition"
                >
                  <div className="flex items-center gap-3 text-sm">
                    <span className="font-semibold text-gray-900 dark:text-white bg-indigo-50 dark:bg-indigo-950 px-2 py-0.5 rounded border border-indigo-200 dark:border-indigo-800">
                      {dep.predecessorTaskCode || `Task #${dep.predecessorId}`}
                    </span>
                    <span className="text-gray-700 dark:text-gray-300 font-medium">
                      {dep.predecessorTaskName}
                    </span>

                    <ArrowRight className="h-4 w-4 text-indigo-500 shrink-0" />

                    <span className="font-semibold text-gray-900 dark:text-white bg-purple-50 dark:bg-purple-950 px-2 py-0.5 rounded border border-purple-200 dark:border-purple-800">
                      {dep.successorTaskCode || `Task #${dep.successorId}`}
                    </span>
                    <span className="text-gray-700 dark:text-gray-300 font-medium">
                      {dep.successorTaskName}
                    </span>
                  </div>

                  {canManage && (
                    <button
                      onClick={() => handleDelete(dep.id)}
                      className="rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-950/50 transition"
                      title="Xóa phụ thuộc này"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="mt-6 flex justify-end border-t pt-4 dark:border-gray-800">
          <button
            onClick={onClose}
            className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-800"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
};
