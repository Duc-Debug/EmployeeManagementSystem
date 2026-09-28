import { useState } from 'react';
import { AlertTriangle, Loader2, X } from 'lucide-react';
import type { RoleResourceDemand } from '@/lib/api/resource-demands';

interface DeleteDemandConfirmModalProps {
    open: boolean;
    roleDemand: RoleResourceDemand | null;
    projectName?: string;
    onClose: () => void;
    onConfirm: (roleId: number) => Promise<void>;
}

export function DeleteDemandConfirmModal({
    open,
    roleDemand,
    projectName,
    onClose,
    onConfirm,
}: DeleteDemandConfirmModalProps) {
    const [isDeleting, setIsDeleting] = useState<boolean>(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    if (!open || !roleDemand) return null;

    const handleConfirm = async () => {
        try {
            setIsDeleting(true);
            setErrorMessage(null);
            await onConfirm(roleDemand.roleId);
            onClose();
        } catch (err: any) {
            console.error('Failed to delete demand:', err);
            setErrorMessage(err?.message || 'Không thể xóa ước lượng nhu cầu vai trò này.');
        } finally {
            setIsDeleting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in">
            <div className="w-full max-w-md overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
                <div className="flex items-center justify-between border-b border-slate-100 bg-rose-50/60 p-4">
                    <div className="flex items-center gap-2.5 text-rose-700">
                        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-rose-100 text-rose-600 shadow-xs">
                            <AlertTriangle className="h-5 w-5" />
                        </span>
                        <div>
                            <h3 className="text-sm font-bold text-slate-800">
                                Xác Nhận Xóa Ước Lượng Nhu Cầu
                            </h3>
                            <p className="text-[11px] text-slate-500">
                                Hành động này sẽ xóa toàn bộ số giờ nhu cầu đã lập của vai trò
                            </p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={isDeleting}
                        className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition disabled:opacity-50"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                <div className="p-5 space-y-3 text-xs">
                    {errorMessage && (
                        <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-rose-700">
                            {errorMessage}
                        </div>
                    )}

                    <p className="text-slate-600 leading-relaxed">
                        Bạn có chắc chắn muốn xóa ước lượng nhu cầu cho vai trò{' '}
                        <strong className="text-slate-900 font-semibold">
                            {roleDemand.roleName} ({roleDemand.roleCode})
                        </strong>{' '}
                        trong dự án <strong className="text-slate-900">{projectName || 'hiện tại'}</strong> không?
                    </p>

                    <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-[11px] space-y-1 text-slate-500">
                        <div className="flex justify-between">
                            <span>Số tuần đã phân rã:</span>
                            <strong className="text-slate-800">{roleDemand.weeklyDemands.length} tuần</strong>
                        </div>
                        <div className="flex justify-between">
                            <span>Tổng số giờ sẽ bị xóa:</span>
                            <strong className="text-rose-600 font-mono">{roleDemand.totalRoleHours} giờ</strong>
                        </div>
                    </div>

                    <p className="text-[11px] text-slate-400">
                        Lưu ý: Sau khi xóa, bạn có thể tạo lại ước lượng mới bất cứ lúc nào khi dự án còn hoạt động.
                    </p>
                </div>

                <div className="flex items-center justify-end gap-2.5 border-t border-slate-100 bg-slate-50/50 p-4">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={isDeleting}
                        className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition disabled:opacity-50 cursor-pointer"
                    >
                        Hủy bỏ
                    </button>
                    <button
                        type="button"
                        onClick={handleConfirm}
                        disabled={isDeleting}
                        className="inline-flex items-center gap-1.5 rounded-xl bg-rose-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-rose-200 hover:bg-rose-700 transition active:scale-95 disabled:opacity-50 cursor-pointer"
                    >
                        {isDeleting && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                        <span>Xác nhận xóa</span>
                    </button>
                </div>
            </div>
        </div>
    );
}
