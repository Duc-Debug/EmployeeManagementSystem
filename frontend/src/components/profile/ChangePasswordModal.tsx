import { useState } from "react";
import { Lock, Eye, EyeOff, UserCheck, X } from "lucide-react";
interface ChangePasswordModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}
export default function ChangePasswordModal({ isOpen, onClose, onSuccess }: ChangePasswordModalProps) {
    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    if (!isOpen) return null;
    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setError("");
        if (!currentPassword || !newPassword || !confirmPassword) {
            setError("Vui lòng điền đầy đủ các trường mật khẩu.");
            return;
        }
        if (newPassword !== confirmPassword) {
            setError("Mật khẩu mới và xác nhận không khớp.");
            return;
        }
        if (newPassword.length < 6) {
            setError("Mật khẩu phải chứa ít nhất 6 ký tự.");
            return;
        }
        setCurrentPassword("");
        setNewPassword("");
        setConfirmPassword("");
        onSuccess();
    };
    return (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm animate-fadeIn">
            <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl border border-slate-100 text-slate-800">
                <div className="flex items-center justify-between border-b pb-3 mb-4 border-slate-100">
                    <div className="flex items-center gap-2">
                        <Lock className="h-5 w-5 text-[#4338ca]" />
                        <h2 className="text-lg font-bold text-slate-800">Đổi mật khẩu</h2>
                    </div>
                    <button
                        onClick={onClose}
                        className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>
                {error && (
                    <div className="mb-3 rounded-lg bg-rose-50 p-2.5 text-xs font-medium text-rose-700 border border-rose-200">
                        {error}
                    </div>
                )}
                <form onSubmit={handleSubmit} className="space-y-3 text-xs">
                    <div>
                        <label className="block mb-1 font-medium text-slate-600">Mật khẩu hiện tại</label>
                        <input
                            type={showPassword ? "text" : "password"}
                            value={currentPassword}
                            onChange={(e) => setCurrentPassword(e.target.value)}
                            placeholder="Nhập mật khẩu hiện tại"
                            className="w-full rounded-lg border border-slate-200 px-3 py-2 text-slate-800 focus:border-[#4338ca] focus:outline-none"
                        />
                    </div>
                    <div>
                        <label className="block mb-1 font-medium text-slate-600">Mật khẩu mới</label>
                        <input
                            type={showPassword ? "text" : "password"}
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            placeholder="Nhập mật khẩu mới"
                            className="w-full rounded-lg border border-slate-200 px-3 py-2 text-slate-800 focus:border-[#4338ca] focus:outline-none"
                        />
                    </div>
                    <div>
                        <label className="block mb-1 font-medium text-slate-600">Xác nhận mật khẩu mới</label>
                        <input
                            type={showPassword ? "text" : "password"}
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            placeholder="Nhập lại mật khẩu mới"
                            className="w-full rounded-lg border border-slate-200 px-3 py-2 text-slate-800 focus:border-[#4338ca] focus:outline-none"
                        />
                    </div>
                    <div className="flex items-center justify-between pt-1">
                        <button
                            type="button"
                            onClick={() => setShowPassword(!showPassword)}
                            className="flex items-center gap-1 text-[11px] font-medium text-slate-500 hover:text-slate-800"
                        >
                            {showPassword ? <EyeOff className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
                            {showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                        </button>
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
                            <UserCheck className="h-3.5 w-3.5" /> Cập nhật
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}