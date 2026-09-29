import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Lock, Eye, EyeOff, CheckCircle2, AlertCircle, Loader2, ArrowRight, LogOut } from "lucide-react";
import { changePassword } from "@/lib/api/auth";
import { useAuthUser, setStoredUser, clearAuthSession } from "@/lib/auth-session";
import { isValidPassword, PASSWORD_POLICY_MESSAGE } from "@/lib/password-policy";

export default function ChangePasswordPage() {
    const user = useAuthUser();
    const navigate = useNavigate();

    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [successMessage, setSuccessMessage] = useState("");

    const isFirstTimeRequired = user?.requiresPasswordChange === true;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError("");
        setSuccessMessage("");

        if (!currentPassword || !newPassword || !confirmPassword) {
            setError("Vui lòng điền đầy đủ các trường mật khẩu.");
            return;
        }

        if (newPassword !== confirmPassword) {
            setError("Mật khẩu mới và xác nhận mật khẩu không khớp.");
            return;
        }

        if (!isValidPassword(newPassword)) {
            setError(PASSWORD_POLICY_MESSAGE);
            return;
        }

        if (newPassword === currentPassword) {
            setError("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
            return;
        }

        try {
            setIsSubmitting(true);
            await changePassword({
                currentPassword,
                newPassword,
                confirmPassword,
            });

            if (user) {
                const updated = { ...user, requiresPasswordChange: false };
                setStoredUser(updated);
            }

            setSuccessMessage("Đổi mật khẩu thành công! Đang chuyển hướng vào hệ thống...");
            setTimeout(() => {
                navigate("/dashboard/overview", { replace: true });
            }, 1200);
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : "Đổi mật khẩu thất bại. Vui lòng kiểm tra lại mật khẩu hiện tại.";
            setError(message);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleLogout = () => {
        clearAuthSession();
        localStorage.removeItem("accessToken");
        localStorage.removeItem("token");
        localStorage.removeItem("currentUser");
        sessionStorage.clear();
        navigate("/login", { replace: true });
    };

    return (
        <div className="min-h-screen w-full flex flex-col items-center justify-center bg-[#f8fafc] text-slate-800 p-6 antialiased">
            <div className="pointer-events-none fixed inset-0 z-0 bg-slate-50/60" aria-hidden="true" />

            <div className="relative z-10 max-w-md w-full bg-white rounded-3xl p-8 border border-slate-200/80 shadow-sm animate-in fade-in zoom-in-95 duration-200">
                <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center gap-3">
                        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 border border-indigo-100 shadow-2xs">
                            <Lock className="h-6 w-6" />
                        </div>
                        <div>
                            <h1 className="text-xl font-bold text-slate-900">
                                {isFirstTimeRequired ? "Đổi mật khẩu lần đầu" : "Cập nhật mật khẩu"}
                            </h1>
                            <p className="text-xs text-slate-500">
                                {user?.username ? `Tài khoản: ${user.username}` : "Bảo mật tài khoản"}
                            </p>
                        </div>
                    </div>
                </div>

                {isFirstTimeRequired && (
                    <div className="mb-5 rounded-2xl bg-amber-50 p-3.5 border border-amber-200 text-xs text-amber-800 leading-relaxed">
                        <p className="font-semibold mb-0.5">Yêu cầu bảo mật bắt buộc</p>
                        Đây là lần đầu đăng nhập hoặc quản trị viên đã đặt lại mật khẩu của bạn. Vui lòng đổi mật khẩu mới để kích hoạt tài khoản.
                    </div>
                )}

                {error && (
                    <div className="mb-4 flex items-center gap-2 rounded-2xl bg-rose-50 p-3 text-xs font-medium text-rose-700 border border-rose-200">
                        <AlertCircle className="h-4 w-4 shrink-0 text-rose-500" />
                        <span>{error}</span>
                    </div>
                )}

                {successMessage && (
                    <div className="mb-4 flex items-center gap-2 rounded-2xl bg-emerald-50 p-3 text-xs font-medium text-emerald-700 border border-emerald-200">
                        <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-500" />
                        <span>{successMessage}</span>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block mb-1.5 text-xs font-semibold text-slate-700">Mật khẩu hiện tại</label>
                        <div className="relative">
                            <input
                                type={showPassword ? "text" : "password"}
                                value={currentPassword}
                                onChange={(e) => setCurrentPassword(e.target.value)}
                                placeholder="Nhập mật khẩu hiện tại"
                                required
                                className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-xs text-slate-900 transition focus:border-indigo-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-100"
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block mb-1.5 text-xs font-semibold text-slate-700">Mật khẩu mới</label>
                        <div className="relative">
                            <input
                                type={showPassword ? "text" : "password"}
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                placeholder="Nhập mật khẩu mới (tối thiểu 8 ký tự, gồm chữ và số)"
                                required
                                minLength={8}
                                className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-xs text-slate-900 transition focus:border-indigo-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-100"
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block mb-1.5 text-xs font-semibold text-slate-700">Xác nhận mật khẩu mới</label>
                        <div className="relative">
                            <input
                                type={showPassword ? "text" : "password"}
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                placeholder="Nhập lại mật khẩu mới"
                                required
                                minLength={8}
                                className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-xs text-slate-900 transition focus:border-indigo-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-100"
                            />
                        </div>
                    </div>

                    <div className="flex items-center justify-between pt-1">
                        <button
                            type="button"
                            onClick={() => setShowPassword(!showPassword)}
                            className="flex items-center gap-1.5 text-xs font-medium text-slate-500 hover:text-slate-800 transition"
                        >
                            {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                            {showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                        </button>
                    </div>

                    <div className="pt-2 flex flex-col gap-2.5">
                        <button
                            type="submit"
                            disabled={isSubmitting || !!successMessage}
                            className="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-xs font-bold text-white shadow-xs hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {isSubmitting ? (
                                <>
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                    Đang xử lý...
                                </>
                            ) : (
                                <>
                                    <span>Lưu mật khẩu mới</span>
                                    <ArrowRight className="h-4 w-4" />
                                </>
                            )}
                        </button>

                        <button
                            type="button"
                            onClick={handleLogout}
                            className="w-full inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition"
                        >
                            <LogOut className="h-3.5 w-3.5 text-slate-400" />
                            Đăng xuất
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
