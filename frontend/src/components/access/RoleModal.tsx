import { useState, useSyncExternalStore } from 'react';
import { createPortal } from 'react-dom';
import { ChevronUp, ChevronDown, Check, X } from 'lucide-react';
import type { Department, RoleBasicInfo, RoleTheme } from './access.types';
import { THEME_OPTIONS, THEME_SOLID_BG } from './access.constants';

export interface RoleModalProps {
    /** Khớp với prop AccessControlView đang truyền vào (`open={isModalOpen}`). */
    open: boolean;
    onClose: () => void;
    /** Danh sách phòng ban thật lấy từ state, thay vì hard-code. */
    departments: Department[];
    /** null/undefined => đang thêm mới. Có `id` => đang sửa vai trò đó. */
    initialData?: RoleBasicInfo | null;
    onSave: (data: RoleBasicInfo) => void;
}

const emptyForm: RoleBasicInfo = {
    name: '',
    description: '',
    theme: 'blue',
    departmentId: 'all',
};

const emptySubscribe = () => () => {};

/** Thay cho pattern "useEffect(() => setMounted(true), [])": tránh gọi
 *  setState bên trong effect (ESLint: react-hooks/set-state-in-effect) mà
 *  vẫn an toàn cho SSR — server luôn trả `false`, client luôn trả `true`. */
function useIsClient() {
    return useSyncExternalStore(
        emptySubscribe,
        () => true,
        () => false
    );
}

export default function RoleModal({ open, onClose, departments, initialData, onSave }: RoleModalProps) {
    const isClient = useIsClient();

    // Không dùng useEffect để "đồng bộ lại form khi initialData/open đổi" —
    // AccessControlView truyền `key` khác nhau mỗi lần mở modal, nên React
    // sẽ tự remount component này và state khởi tạo ở đây luôn đúng dữ liệu
    // mới nhất, không gây thêm 1 lần render phụ (set-state-in-effect).
    const [form, setForm] = useState<RoleBasicInfo>(() => initialData ?? emptyForm);
    const [isDeptOpen, setIsDeptOpen] = useState(false);

    const isEdit = Boolean(initialData?.id);

    if (!open || !isClient) return null;

    const departmentOptions = [{ id: 'all', name: 'Tất cả phòng ban' }, ...departments];
    const selectedDept = departmentOptions.find((d) => d.id === form.departmentId) ?? departmentOptions[0];

    const handleSave = () => {
        if (!form.name.trim()) return;
        onSave({ ...form, id: initialData?.id, isSystemRole: initialData?.isSystemRole });
    };

    return createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
            {/* Nền mờ phía sau */}
            <div className="fixed inset-0" onClick={onClose} />

            {/* Bảng Modal chính */}
            <div className="relative z-10 w-full max-w-md bg-white rounded-2xl shadow-2xl p-6 space-y-5">

                {/* Header */}
                <div className="flex items-center justify-between border-b pb-3 border-slate-100">
                    <h3 className="text-lg font-bold text-slate-800">
                        {isEdit ? "Sửa vai trò" : "Thêm vai trò"}
                    </h3>
                    <button
                        type="button"
                        onClick={onClose}
                        className="text-slate-400 hover:text-slate-600 p-1 rounded-lg transition-colors"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Body */}
                <div className="space-y-4">
                    {/* Ô nhập tên vai trò */}
                    <div>
                        <label className="block text-xs font-semibold text-slate-600 mb-1.5">
                            Tên vai trò <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="text"
                            value={form.name}
                            onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
                            placeholder="Nhập tên vai trò"
                            className="w-full px-4 py-2.5 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400"
                        />
                    </div>

                    {/* Mô tả vai trò */}
                    <div>
                        <label className="block text-xs font-semibold text-slate-600 mb-1.5">
                            Mô tả
                        </label>
                        <textarea
                            value={form.description}
                            onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
                            placeholder="Mô tả ngắn về vai trò"
                            rows={2}
                            className="w-full px-4 py-2.5 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 resize-none"
                        />
                    </div>

                    {/* Custom Dropdown Chọn Phòng ban */}
                    <div className="relative">
                        <label className="block text-xs font-semibold text-slate-600 mb-1.5">
                            Phòng ban <span className="text-indigo-500">*</span>
                        </label>
                        <button
                            type="button"
                            onClick={() => setIsDeptOpen(!isDeptOpen)}
                            className="w-full px-4 py-2.5 bg-white border border-indigo-400 rounded-2xl flex items-center justify-between text-sm font-semibold text-slate-800 shadow-sm focus:outline-none"
                        >
                            <span>{selectedDept.name}</span>
                            {isDeptOpen ? (
                                <ChevronUp className="w-4 h-4 text-indigo-400 stroke-[2.5]" />
                            ) : (
                                <ChevronDown className="w-4 h-4 text-slate-400 stroke-[2.5]" />
                            )}
                        </button>

                        {isDeptOpen && (
                            <div className="absolute left-0 right-0 mt-2 p-2 bg-white border border-slate-100 rounded-2xl shadow-xl z-50 space-y-1 max-h-48 overflow-y-auto">
                                {departmentOptions.map((dept) => (
                                    <button
                                        key={dept.id}
                                        type="button"
                                        onClick={() => {
                                            setForm((prev) => ({ ...prev, departmentId: dept.id }));
                                            setIsDeptOpen(false);
                                        }}
                                        className={`w-full px-4 py-2 text-left text-sm rounded-xl font-medium flex items-center justify-between transition-colors ${
                                            form.departmentId === dept.id
                                                ? 'bg-indigo-50 text-indigo-600 font-bold'
                                                : 'text-slate-700 hover:bg-slate-50'
                                        }`}
                                    >
                                        <span>{dept.name}</span>
                                        {form.departmentId === dept.id && <Check className="w-4 h-4 text-indigo-600 stroke-[2.5]" />}
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* Chọn màu chủ đề */}
                    <div>
                        <label className="block text-xs font-semibold text-slate-600 mb-1.5">
                            Màu chủ đề
                        </label>
                        <div className="flex flex-wrap gap-2">
                            {THEME_OPTIONS.map((opt) => (
                                <button
                                    key={opt.key}
                                    type="button"
                                    title={opt.label}
                                    onClick={() => setForm((prev) => ({ ...prev, theme: opt.key as RoleTheme }))}
                                    className={`h-7 w-7 rounded-full ${THEME_SOLID_BG[opt.key]} flex items-center justify-center ring-offset-2 transition ${
                                        form.theme === opt.key ? 'ring-2 ring-slate-400' : ''
                                    }`}
                                >
                                    {form.theme === opt.key && <Check className="w-3.5 h-3.5 text-white stroke-[3]" />}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>

                {/* Footer */}
                <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
                    <button
                        type="button"
                        onClick={onClose}
                        className="px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 rounded-xl"
                    >
                        Hủy
                    </button>
                    <button
                        type="button"
                        onClick={handleSave}
                        className="px-4 py-2 text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-sm"
                    >
                        Lưu thay đổi
                    </button>
                </div>

            </div>
        </div>,
        document.body
    );
}