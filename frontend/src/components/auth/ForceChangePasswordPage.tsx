"use client";

import { useState, type FormEvent, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Lock,
  Eye,
  EyeOff,
  KeyRound,
  Loader2,
  CheckCircle2,
  AlertCircle,
  ShieldAlert,
  LogOut,
} from "lucide-react";
import { changePassword } from "@/lib/api/auth";
import { clearAuthSession, getAuthToken, getStoredUser } from "@/lib/auth-session";
import { isValidPassword, PASSWORD_POLICY_MESSAGE } from "@/lib/password-policy";
import InteractiveParticleBackground from "./InteractiveParticleBackground";

export default function ForceChangePasswordPage() {
  const navigate = useNavigate();

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const user = getStoredUser();
  const token = getAuthToken();

  useEffect(() => {
    document.title = "Đổi mật khẩu lần đầu | Employee Management System";
    if (!token) {
      navigate("/login", { replace: true });
      return;
    }
    if (user && !user.requiresPasswordChange) {
      navigate("/", { replace: true });
    }
  }, [token, user, navigate]);

  const handleLogout = () => {
    clearAuthSession();
    navigate("/login", { replace: true });
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!currentPassword) {
      setError("Vui lòng nhập mật khẩu hiện tại.");
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
    if (newPassword !== confirmPassword) {
      setError("Mật khẩu xác nhận không khớp.");
      return;
    }

    setIsSubmitting(true);
    try {
      await changePassword({
        currentPassword,
        newPassword,
        confirmPassword,
      });
      setSuccess(true);
      // Clean old session since backend invalidates session version
      clearAuthSession();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Đổi mật khẩu thất bại. Vui lòng thử lại.";
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="relative min-h-screen flex flex-col justify-center items-center bg-slate-200/75 px-4 py-8 antialiased text-slate-800 overflow-hidden font-sans">
      {/* Interactive Particle Attraction Canvas */}
      <InteractiveParticleBackground />

      {/* Moving Ambient Lights (Nền sáng đồng bộ với Login) */}
      <div className="pointer-events-none fixed inset-0 z-0 overflow-hidden" aria-hidden="true">
        <div className="absolute -top-32 -left-32 w-96 h-96 rounded-full bg-indigo-300/35 blur-[100px] animate-float-slow" />
        <div className="absolute -bottom-32 -right-32 w-96 h-96 rounded-full bg-sky-300/30 blur-[100px] animate-float-reverse" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[450px] h-[450px] rounded-full bg-violet-200/30 blur-[120px] animate-pulse-slow" />

        {/* Subtle Dot Pattern */}
        <div
          className="absolute inset-0 opacity-45"
          style={{
            backgroundImage: "radial-gradient(#94a3b8 1.2px, transparent 1.2px)",
            backgroundSize: "22px 22px",
          }}
        />
      </div>

      <div className="w-full max-w-[400px] relative z-10">
        <div className="rounded-2xl border border-slate-300/80 bg-white p-6 sm:p-7 shadow-[0_10px_35px_rgba(0,0,0,0.06)] space-y-5">
          {/* Header */}
          <div className="text-center space-y-1.5">
            <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-amber-50 border border-amber-200 text-amber-600 mb-1 shadow-2xs">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <h1 className="text-xl font-bold tracking-tight text-slate-900">
              Bắt buộc đổi mật khẩu
            </h1>
            <p className="text-xs text-slate-500 max-w-sm mx-auto leading-relaxed">
              Tài khoản <span className="font-semibold text-slate-800">{user?.username || "của bạn"}</span> đang sử dụng mật khẩu khởi tạo hoặc chưa từng đổi mật khẩu. Vui lòng thiết lập mật khẩu mới để tiếp tục.
            </p>
          </div>

          {/* Success State */}
          {success ? (
            <div className="space-y-4 py-3 text-center">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-50 text-emerald-600 border border-emerald-100">
                <CheckCircle2 className="w-6 h-6" />
              </div>
              <div className="space-y-1">
                <h3 className="text-base font-bold text-slate-900">
                  Đổi mật khẩu thành công!
                </h3>
                <p className="text-xs text-slate-500 leading-relaxed">
                  Tất cả các phiên làm việc trước đã kết thúc vì lý do an toàn. Vui lòng đăng nhập lại với mật khẩu mới.
                </p>
              </div>
              <div className="pt-2">
                <button
                  type="button"
                  onClick={() => navigate("/login", { replace: true })}
                  className="w-full h-10 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs shadow-md shadow-indigo-600/20 transition-all flex items-center justify-center focus:outline-none focus:ring-2 focus:ring-indigo-600/50 cursor-pointer"
                >
                  Đăng nhập lại ngay
                </button>
              </div>
            </div>
          ) : (
            /* Form */
            <form onSubmit={handleSubmit} className="space-y-4">
              {error && (
                <div role="alert" className="flex items-start gap-2.5 p-2.5 rounded-xl border border-red-200 bg-red-50 text-red-800 text-xs animate-in fade-in">
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-red-600" />
                  <span>{error}</span>
                </div>
              )}

              {/* Current Password */}
              <div className="space-y-1.5">
                <label
                  htmlFor="currentPassword"
                  className="block text-xs font-semibold text-slate-700"
                >
                  Mật khẩu hiện tại
                </label>
                <div className="relative group">
                  <KeyRound className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600" />
                  <input
                    id="currentPassword"
                    type={showCurrentPassword ? "text" : "password"}
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="Nhập mật khẩu hiện tại"
                    required
                    disabled={isSubmitting}
                    className="w-full h-10 rounded-xl border border-slate-300/80 bg-slate-50/70 pl-9 pr-10 text-xs text-slate-900 placeholder:text-slate-400 outline-none transition duration-150 focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-600/10 disabled:opacity-50"
                  />
                  <button
                    type="button"
                    onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                    tabIndex={-1}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 transition-colors cursor-pointer"
                  >
                    {showCurrentPassword ? (
                      <EyeOff className="w-4 h-4" />
                    ) : (
                      <Eye className="w-4 h-4" />
                    )}
                  </button>
                </div>
              </div>

              {/* New Password */}
              <div className="space-y-1.5">
                <label
                  htmlFor="newPassword"
                  className="block text-xs font-semibold text-slate-700"
                >
                  Mật khẩu mới (tối thiểu 8 ký tự)
                </label>
                <div className="relative group">
                  <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600" />
                  <input
                    id="newPassword"
                    type={showNewPassword ? "text" : "password"}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Tối thiểu 8 ký tự"
                    required
                    disabled={isSubmitting}
                    className="w-full h-10 rounded-xl border border-slate-300/80 bg-slate-50/70 pl-9 pr-10 text-xs text-slate-900 placeholder:text-slate-400 outline-none transition duration-150 focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-600/10 disabled:opacity-50"
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    tabIndex={-1}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 transition-colors cursor-pointer"
                  >
                    {showNewPassword ? (
                      <EyeOff className="w-4 h-4" />
                    ) : (
                      <Eye className="w-4 h-4" />
                    )}
                  </button>
                </div>
              </div>

              {/* Confirm Password */}
              <div className="space-y-1.5">
                <label
                  htmlFor="confirmPassword"
                  className="block text-xs font-semibold text-slate-700"
                >
                  Xác nhận mật khẩu mới
                </label>
                <div className="relative group">
                  <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600" />
                  <input
                    id="confirmPassword"
                    type={showConfirmPassword ? "text" : "password"}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Nhập lại mật khẩu mới"
                    required
                    disabled={isSubmitting}
                    className="w-full h-10 rounded-xl border border-slate-300/80 bg-slate-50/70 pl-9 pr-10 text-xs text-slate-900 placeholder:text-slate-400 outline-none transition duration-150 focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-600/10 disabled:opacity-50"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    tabIndex={-1}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 transition-colors cursor-pointer"
                  >
                    {showConfirmPassword ? (
                      <EyeOff className="w-4 h-4" />
                    ) : (
                      <Eye className="w-4 h-4" />
                    )}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full mt-2 h-10 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs shadow-md shadow-indigo-600/20 transition-all flex items-center justify-center gap-2 focus:outline-none focus:ring-2 focus:ring-indigo-600/50 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer active:scale-[0.99]"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Đang cập nhật...</span>
                  </>
                ) : (
                  <span>Cập nhật mật khẩu</span>
                )}
              </button>

              <div className="pt-2 text-center">
                <button
                  type="button"
                  onClick={handleLogout}
                  className="inline-flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-800 transition-colors cursor-pointer"
                >
                  <LogOut className="w-3.5 h-3.5" />
                  <span>Đăng xuất khỏi tài khoản</span>
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
