import { useState, useEffect } from 'react';
import {
    X,
    Copy,
    AlertCircle,
    Search,
    Check,
    Loader2,
    Layers,
    FolderKanban,
    Eye,
} from 'lucide-react';
import { getProjectWbs, type ProjectResult, type TaskNodeResult } from '@/lib/api/projects';
import { cloneProjectWbs, type CloneProjectWbsResult } from '@/lib/api/tasks';

interface CloneWbsModalProps {
    isOpen: boolean;
    onClose: () => void;
    targetProject: ProjectResult | null;
    projectsList: ProjectResult[];
    onSuccess: (result: CloneProjectWbsResult) => void;
}

export function CloneWbsModal({
    isOpen,
    onClose,
    targetProject,
    projectsList,
    onSuccess,
}: CloneWbsModalProps) {
    const [selectedSourceId, setSelectedSourceId] = useState<number | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(false);
    const [loadingPreview, setLoadingPreview] = useState(false);
    const [previewWbs, setPreviewWbs] = useState<TaskNodeResult[]>([]);
    const [error, setError] = useState<string | null>(null);

    // Reset state khi mở modal
    useEffect(() => {
        if (isOpen) {
            setSelectedSourceId(null);
            setSearchTerm('');
            setError(null);
            setPreviewWbs([]);
        }
    }, [isOpen]);

    // Tự động tải xem trước WBS khi chọn dự án nguồn
    useEffect(() => {
        if (!selectedSourceId) {
            setPreviewWbs([]);
            return;
        }

        let isMounted = true;
        async function fetchPreview() {
            setLoadingPreview(true);
            try {
                const nodes = await getProjectWbs(selectedSourceId!);
                if (isMounted) {
                    setPreviewWbs(nodes || []);
                }
            } catch (err) {
                console.warn('Không thể tải xem trước WBS:', err);
                if (isMounted) {
                    setPreviewWbs([]);
                }
            } finally {
                if (isMounted) {
                    setLoadingPreview(false);
                }
            }
        }

        fetchPreview();
        return () => {
            isMounted = false;
        };
    }, [selectedSourceId]);

    if (!isOpen || !targetProject) return null;

    // Loại bỏ chính dự án hiện tại ra khỏi danh sách nguồn
    const availableSources = projectsList
        .filter((p) => p.id !== targetProject.id)
        .filter((p) => {
            const q = searchTerm.trim().toLowerCase();
            return !q || p.projectName.toLowerCase().includes(q) || p.projectCode.toLowerCase().includes(q);
        });

    const handleConfirmClone = async () => {
        if (!selectedSourceId) {
            setError('Vui lòng chọn một dự án nguồn để nhân bản');
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const result = await cloneProjectWbs(targetProject.id, selectedSourceId);
            onSuccess(result);
            onClose();
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : 'Đã xảy ra lỗi khi nhân bản cây công việc';
            setError(message);
        } finally {
            setLoading(false);
        }
    };

    // Đếm số category và tasks con trong preview
    const countCategories = previewWbs.filter((n) => n.taskType === 'CATEGORY').length;
    const countTasks = previewWbs.reduce((acc, cat) => acc + (cat.children?.length || (cat.taskType === 'TASK' ? 1 : 0)), 0);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 overflow-y-auto">
            <div className="relative w-full max-w-xl rounded-2xl bg-white p-6 shadow-2xl border border-slate-100 animate-in fade-in zoom-in-95 duration-150">
                {/* Header */}
                <div className="flex items-start justify-between border-b border-slate-100 pb-4 mb-4">
                    <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 shrink-0">
                            <Copy className="h-5 w-5" />
                        </div>
                        <div>
                            <h3 className="text-base font-bold text-slate-900">Nhân Bản Cây Công Việc WBS</h3>
                            <p className="text-xs text-slate-500">
                                Sao chép cấu trúc sang: <span className="font-semibold text-indigo-700">{targetProject.projectName}</span>{' '}
                                <span className="font-mono text-slate-400">({targetProject.projectCode})</span>
                            </p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={loading}
                        className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 transition cursor-pointer"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                {/* Banner cảnh báo nghiệp vụ */}
                <div className="mb-4 flex items-start gap-2.5 rounded-xl bg-amber-50/80 p-3.5 text-xs text-amber-900 border border-amber-200/80 leading-relaxed">
                    <AlertCircle className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
                    <div>
                        <strong>Quy tắc nhân bản tự động:</strong>
                        <ul className="list-disc ml-4 mt-1 space-y-0.5 text-amber-800 text-[11px]">
                            <li>Sao chép toàn bộ <strong>Hạng mục</strong>, <strong>Công việc con</strong> và <strong>Ngân sách giờ</strong>.</li>
                            <li><strong>Người phụ trách</strong> sẽ để trống (chưa gán) để PM chủ động phân bổ mới.</li>
                            <li><strong>Giờ công thực tế</strong> được đặt về <code>0h</code>; trạng thái chuyển về <code>Chưa làm (TODO)</code>.</li>
                        </ul>
                    </div>
                </div>

                {/* Error Box */}
                {error && (
                    <div className="mb-4 rounded-xl bg-rose-50 p-3 text-xs text-rose-700 border border-rose-200 flex items-center gap-2">
                        <AlertCircle className="h-4 w-4 shrink-0 text-rose-600" />
                        <span>{error}</span>
                    </div>
                )}

                {/* Ô tìm kiếm dự án mẫu */}
                <div className="space-y-1.5 mb-3">
                    <label className="block text-xs font-bold text-slate-700">
                        Chọn dự án nguồn làm mẫu (Source Project) <span className="text-rose-500">*</span>
                    </label>
                    <div className="relative">
                        <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                        <input
                            type="text"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            placeholder="Tìm theo tên hoặc mã dự án nguồn..."
                            className="w-full rounded-xl border border-slate-200 bg-slate-50/50 py-2 pl-9 pr-4 text-xs text-slate-800 placeholder-slate-400 focus:border-indigo-500 focus:bg-white focus:outline-hidden transition"
                        />
                    </div>
                </div>

                {/* Danh sách dự án nguồn */}
                <div className="max-h-52 overflow-y-auto space-y-2 pr-1 mb-4">
                    {availableSources.length === 0 ? (
                        <div className="p-8 text-center text-xs text-slate-400 border border-dashed border-slate-200 rounded-xl">
                            <FolderKanban className="mx-auto mb-2 h-7 w-7 text-slate-300" />
                            Không tìm thấy dự án mẫu nào phù hợp.
                        </div>
                    ) : (
                        availableSources.map((proj) => {
                            const isSelected = selectedSourceId === proj.id;
                            return (
                                <div
                                    key={proj.id}
                                    onClick={() => setSelectedSourceId(proj.id)}
                                    className={`flex cursor-pointer items-center justify-between rounded-xl p-3 text-xs transition border ${
                                        isSelected
                                            ? 'border-indigo-600 bg-indigo-50/40 text-indigo-900 font-medium'
                                            : 'border-slate-200 hover:border-indigo-300 hover:bg-slate-50/70 text-slate-700'
                                    }`}
                                >
                                    <div className="flex items-center gap-3">
                                        <div
                                            className={`flex h-8 w-8 items-center justify-center rounded-lg font-mono text-xs font-bold ${
                                                isSelected
                                                    ? 'bg-indigo-600 text-white'
                                                    : 'bg-slate-100 text-slate-600'
                                            }`}
                                        >
                                            {proj.projectCode.substring(0, 3)}
                                        </div>
                                        <div>
                                            <div className="font-bold text-slate-900 flex items-center gap-2">
                                                {proj.projectName}
                                            </div>
                                            <div className="text-[11px] text-slate-500 mt-0.5 flex items-center gap-2">
                                                <span className="font-mono text-slate-600">{proj.projectCode}</span>
                                                <span>•</span>
                                                <span>{proj.status === 'CLOSED' ? 'Đã đóng' : 'Đang thực hiện'}</span>
                                                {proj.estimatedHours ? (
                                                    <>
                                                        <span>•</span>
                                                        <span>Định mức: {proj.estimatedHours}h</span>
                                                    </>
                                                ) : null}
                                            </div>
                                        </div>
                                    </div>
                                    {isSelected && (
                                        <div className="h-5 w-5 rounded-full bg-indigo-600 text-white flex items-center justify-center shrink-0">
                                            <Check className="h-3.5 w-3.5 stroke-[3]" />
                                        </div>
                                    )}
                                </div>
                            );
                        })
                    )}
                </div>

                {/* Khung Xem trước cây WBS (Live Preview từ Backend API) */}
                {selectedSourceId && (
                    <div className="mb-5 rounded-xl border border-slate-200 bg-slate-50/70 p-3 text-xs animate-in fade-in duration-150">
                        <div className="flex items-center justify-between mb-2">
                            <span className="font-bold text-slate-700 flex items-center gap-1.5">
                                <Eye className="h-3.5 w-3.5 text-indigo-600" />
                                Xem trước cấu trúc sẽ nhân bản:
                            </span>
                            {loadingPreview ? (
                                <span className="inline-flex items-center gap-1 text-[11px] text-indigo-600">
                                    <Loader2 className="h-3 w-3 animate-spin" /> Đang tải WBS...
                                </span>
                            ) : (
                                <span className="font-semibold text-indigo-700 text-[11px]">
                                    {countCategories} giai đoạn · {countTasks} công việc
                                </span>
                            )}
                        </div>

                        <div className="bg-white rounded-lg border border-slate-200 p-2.5 font-mono text-[11px] text-slate-600 space-y-1 max-h-28 overflow-y-auto">
                            {previewWbs.length === 0 && !loadingPreview ? (
                                <div className="text-amber-600 italic">
                                    Dự án nguồn này chưa có cây công việc WBS nào trong Database.
                                </div>
                            ) : (
                                previewWbs.map((node) => (
                                    <div key={node.id} className="space-y-0.5">
                                        <div className="font-bold text-slate-900 flex items-center gap-1">
                                            <Layers className="h-3 w-3 text-indigo-600" />
                                            <span>{node.name}</span>
                                            {node.budgetHours ? (
                                                <span className="font-normal text-slate-500">({node.budgetHours}h)</span>
                                            ) : null}
                                        </div>
                                        {node.children && node.children.length > 0 && (
                                            <div className="pl-4 space-y-0.5 text-slate-600">
                                                {node.children.map((child) => (
                                                    <div key={child.id} className="flex items-center gap-1">
                                                        <span className="text-slate-400">├─</span>
                                                        <span>{child.name}</span>
                                                        {child.budgetHours ? (
                                                            <span className="text-indigo-600 font-semibold">({child.budgetHours}h)</span>
                                                        ) : null}
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                )}

                {/* Modal Footer Actions */}
                <div className="flex items-center justify-end gap-2.5 border-t border-slate-100 pt-4">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={loading}
                        className="rounded-xl px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 transition cursor-pointer"
                    >
                        Hủy bỏ
                    </button>
                    <button
                        type="button"
                        onClick={handleConfirmClone}
                        disabled={loading || !selectedSourceId || (previewWbs.length === 0 && !loadingPreview)}
                        className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-indigo-100 hover:bg-indigo-700 active:scale-95 transition disabled:opacity-50 cursor-pointer"
                    >
                        {loading ? (
                            <>
                                <Loader2 className="h-4 w-4 animate-spin" />
                                <span>Đang nhân bản dữ liệu...</span>
                            </>
                        ) : (
                            <>
                                <Copy className="h-4 w-4" />
                                <span>Xác nhận nhân bản WBS</span>
                            </>
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
}
