import React, { useState } from 'react';
import { Building2, Plus, Trash2, X } from 'lucide-react';
import type { Department } from './access.types';

interface Props {
    open: boolean;
    departments: Department[];
    onClose: () => void;
    onAdd: (name: string) => void;
    onRemove: (id: string) => void;
}

/** Quản lý danh sách phòng ban dùng làm gốc cho việc gán vai trò và cho
 *  phạm vi "Cây đơn vị tùy chỉnh" — tương ứng modal "Danh sách phòng ban"
 *  trong file mẫu HTML. */
export const DepartmentManagermodal: React.FC<Props> = ({ open, departments, onClose, onAdd, onRemove }) => {
    const [name, setName] = useState('');

    if (!open) return null;

    const handleAdd = () => {
        const trimmed = name.trim();
        if (!trimmed) return;
        onAdd(trimmed);
        setName('');
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
            <div className="relative flex max-h-[85vh] w-full max-w-md flex-col rounded-2xl border border-purple-200 bg-white p-6 shadow-2xl">
                <button
                    type="button"
                    onClick={onClose}
                    className="absolute right-4 top-4 p-2 text-gray-400 hover:text-gray-700"
                >
                    <X className="w-4 h-4" />
                </button>

                <div className="mb-4 flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-xl border border-purple-200 bg-purple-100 text-purple-600">
                        <Building2 className="w-4.5 h-4.5" />
                    </div>
                    <div>
                        <h3 className="text-base font-bold text-gray-900">Danh sách phòng ban</h3>
                        <p className="text-xs text-gray-500">Quản lý các đơn vị dùng để phân quyền dữ liệu</p>
                    </div>
                </div>

                <div className="mb-3 flex items-center gap-2">
                    <input
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && handleAdd()}
                        placeholder="Tên phòng ban mới..."
                        className="flex-1 rounded-full border border-gray-200 bg-gray-50 px-4 py-2.5 text-xs text-gray-900 placeholder:text-gray-400 outline-none transition focus:border-purple-400 focus:bg-white"
                    />
                    <button
                        type="button"
                        onClick={handleAdd}
                        className="flex shrink-0 items-center gap-1.5 rounded-full bg-purple-600 px-4 py-2.5 text-xs font-semibold text-white transition hover:bg-purple-500"
                    >
                        <Plus className="h-3.5 w-3.5" /> Thêm
                    </button>
                </div>

                <div className="flex-1 space-y-2 overflow-y-auto pr-1">
                    {departments.map((dept) => (
                        <div key={dept.id} className="flex items-center justify-between rounded-xl border border-gray-100 bg-gray-50 p-3">
                            <div className="flex items-center gap-3">
                                <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-purple-200 bg-purple-100 text-purple-600">
                                    <Building2 className="h-3.5 w-3.5" />
                                </div>
                                <span className="text-xs font-semibold text-gray-900">{dept.name}</span>
                            </div>
                            <button
                                type="button"
                                onClick={() => onRemove(dept.id)}
                                className="rounded-lg p-1.5 text-rose-500 transition hover:bg-rose-100 hover:text-rose-600"
                            >
                                <Trash2 className="h-3.5 w-3.5" />
                            </button>
                        </div>
                    ))}
                    {departments.length === 0 && (
                        <p className="py-6 text-center text-xs text-gray-400">Chưa có phòng ban nào.</p>
                    )}
                </div>

                <div className="mt-5 flex items-center justify-end border-t border-gray-100 pt-4">
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-xl border border-gray-200 bg-gray-50 px-4 py-2 text-xs font-medium text-gray-600 hover:bg-gray-100 hover:text-gray-900"
                    >
                        Đóng
                    </button>
                </div>
            </div>
        </div>
    );
};

export default DepartmentManagermodal;