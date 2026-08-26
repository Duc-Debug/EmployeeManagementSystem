import { useState, useEffect } from "react";
import { Edit3, Save, X } from "lucide-react";
interface UserInfo {
    name: string;
    email: string;
    role: string;
    department: string;
}
interface EditProfileModalProps {
    isOpen: boolean;
    userInfo: UserInfo;
    onClose: () => void;
    onSave: (updatedData: UserInfo) => void;
}
export default function EditProfileModal({ isOpen, userInfo, onClose, onSave }: EditProfileModalProps) {
    const [formData, setFormData] = useState<UserInfo>({ ...userInfo });
    useEffect(() => {
        setFormData({ ...userInfo });
    }, [userInfo, isOpen]);
    if (!isOpen) return null;
    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onSave(formData);
    };
    return (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm animate-fadeIn">
            <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl border border-slate-100 text-slate-800">
                <div className="flex items-center justify-between border-b pb-3 mb-4 border-slate-100">
                    <div className="flex items-center gap-2">
                        <Edit3 className="h-5 w-5 text-[#4338ca]" />
                        <h2 className="text-lg font-bold text-slate-800">Chỉnh sửa thông tin</h2>
                    </div>
                    <button
                        onClick={onClose}
                        className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="grid grid-cols-2 gap-3 text-xs">
                        <div className="col-span-2 sm:col-span-1">
                            <label className="block mb-1 font-medium text-slate-600">Họ và tên</label>
                            <input
                                type="text"
                                value={formData.name}
                                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-slate-800 focus:border-[#4338ca] focus:outline-none"
                                required
                            />
                        </div>
                        <div className="col-span-2 sm:col-span-1">
                            <label className="block mb-1 font-medium text-slate-600">Email</label>
                            <input
                                type="email"
                                value={formData.email}
                                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-slate-800 focus:border-[#4338ca] focus:outline-none"
                                required
                            />
                        </div>
                        <div className="col-span-2 sm:col-span-1">
                            <label className="block mb-1 font-medium text-slate-600">Chức vụ</label>
                            <input
                                type="text"
                                value={formData.role}
                                onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-slate-800 focus:border-[#4338ca] focus:outline-none"
                            />
                        </div>
                        <div className="col-span-2 sm:col-span-1">
                            <label className="block mb-1 font-medium text-slate-600">Phòng ban</label>
                            <input
                                type="text"
                                value={formData.department}
                                onChange={(e) => setFormData({ ...formData, department: e.target.value })}
                                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-slate-800 focus:border-[#4338ca] focus:outline-none"
                            />
                        </div>
                    </div>
                    <div className="mt-6 flex items-center justify-end gap-2 border-t border-slate-100 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition"
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            className="flex items-center gap-1.5 rounded-xl bg-[#4338ca] px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-[#3730a3] transition"
                        >
                            <Save className="h-3.5 w-3.5" /> Lưu thay đổi
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}