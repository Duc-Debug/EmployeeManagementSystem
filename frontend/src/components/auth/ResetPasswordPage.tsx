"use client";

import { useState, type FormEvent, type KeyboardEvent, useEffect, useRef } from "react";
import { useSearchParams, useNavigate, Link } from "react-router-dom";
import {
  Lock,
  Eye,
  EyeOff,
  KeyRound,
  Loader2,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { resetPassword } from "@/lib/api/auth";
import InteractiveParticleBackground from "./InteractiveParticleBackground";

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const queryToken = searchParams.get("token") || "";
  const [token, setToken] = useState(queryToken);
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const newPasswordRef = useRef<HTMLInputElement>(null);
  const confirmPasswordRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    document.title = "Đặt lại mật khẩu | Employee Management System";
    if (queryToken) {
      setToken(queryToken);
    }
  }, [queryToken]);

  const handleTokenKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      newPasswordRef.current?.focus();
    }
  };

  const handleNewPwKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      confirmPasswordRef.current?.focus();
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!token.trim()) {
      setError("Vui lòng cung cấp mã token khôi phục mật khẩu.");
      return;
    }
    if (newPassword.length < 8) {
      setError("Mật khẩu mới phải có tối thiểu 8 ký tự.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("Mật khẩu xác nhận không khớp.");
      return;
    }

    setIsSubmitting(true);
    try {
      await resetPassword({
        confirmPassword,
        newPassword,
        token: token.trim(),
      });
      setSuccess(true);
      setTimeout(() => {
        navigate("/login?resetSuccess=true");
      }, 2500);
    } catch (err: unknown) {
      setError(
        err instanceof Error
          ? err.message
          : "Đặt lại mật khẩu thất bại. Mã Token có thể không chính xác hoặc đã hết hạn 15 phút."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="relative min-h-screen flex flex-col justify-center items-center bg-slate-200/75 px-4 py-8 antialiased text-slate-800 overflow-hidden">
      {/* Interactive Particle Attraction Canvas */}
      <InteractiveParticleBackground />

      {/* Moving Ambient Lights */}
      <div className="pointer-events-none fixed inset-0 z-0 overflow-hidden" aria-hidden="true">
        <div className="absolute -top-32 -left-32 w-96 h-96 rounded-full bg-indigo-300/35 blur-[100px] animate-float-slow" />
        <div className="absolute -bottom-32 -right-32 w-96 h-96 rounded-full bg-sky-300/30 blur-[100px] animate-float-reverse" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[450px] h-[450px] rounded-full bg-violet-200/30 blur-[120px] animate-pulse-slow" />
        
        <div
          className="absolute inset-0 opacity-45"
          style={{
            backgroundImage: "radial-gradient(#94a3b8 1.2px, transparent 1.2px)",
            backgroundSize: "22px 22px",
          }}
        />
      </div>

      {/* Card */}
      <div className="relative z-10 w-full max-w-[380px] rounded-2xl border border-slate-300/80 bg-white p-6 sm:p-7 shadow-[0_10px_35px_rgba(0,0,0,0.06)] backdrop-blur-xl transition-all">
        {/* Header */}
        <div className="text-center mb-6">
          <div className="inline-flex h-11 w-11 items-center justify-center rounded-xl bg-indigo-600 text-white font-bold text-sm shadow-md shadow-indigo-600/20 mb-3">
            EM
          </div>
          <h1 className="text-lg font-bold text-slate-900 tracking-tight">
            Đặt lại mật khẩu
          </h1>
          <p className="mt-1 text-xs text-slate-500">
            Nhập mật khẩu mới cho tài khoản của bạn
          </p>
        </div>

        {success ? (
          <div className="space-y-4 py-3 text-center">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-50 text-emerald-600 border border-emerald-100">
              <CheckCircle2 className="h-6 w-6" />
            </div>
            <h2 className="text-sm font-bold text-slate-900">
              Đổi mật khẩu thành công!
            </h2>
            <p className="text-xs text-slate-600 leading-relaxed">
              Mật khẩu đã được cập nhật. Đang tự động chuyển hướng về trang Đăng nhập...
            </p>
            <div className="pt-2">
              <Link
                to="/login"
                className="inline-flex h-10 w-full items-center justify-center gap-1.5 rounded-xl bg-indigo-600 px-4 text-xs font-semibold text-white shadow-xs transition hover:bg-indigo-700"
              >
                <span>Đăng nhập ngay</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-3.5">
            {error && (
              <div
                role="alert"
                className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-2.5 text-xs text-red-800"
              >
                <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-red-600" />
                <span>{error}</span>
              </div>
            )}

            <div>
              <label
                htmlFor="reset-page-token"
                className="mb-1.5 block text-xs font-semibold text-slate-700"
              >
                Mã Token khôi phục
              </label>
              <div className="relative group">
                <KeyRound className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600" />
                <input
                  id="reset-page-token"
                  type="text"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  onKeyDown={handleTokenKeyDown}
                  placeholder="Dán mã Token từ email"
                  required
                  className="w-full h-10 rounded-xl border border-slate-300/80 bg-slate-50/70 pl-9 pr-3.5 text-xs text-slate-900 font-mono placeholder:text-slate-400 outline-none transition focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-600/10"
                />
              </div>
            </div>

            <div>
              <label
                htmlFor="reset-page-new-pw"
                className="mb-1.5 block text-xs font-semibold text-slate-700"
              >
                Mật khẩu mới (Tối thiểu 8 ký tự)
              </label>
              <div className="relative group">
                <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600" />
                <input
                  ref={newPasswordRef}
                  id="reset-page-new-pw"
                  type={showNewPassword ? "text" : "password"}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  onKeyDown={handleNewPwKeyDown}
                  placeholder="••••••••"
                  required
                  className="w-full h-10 rounded-xl border border-slate-300/80 bg-slate-50/70 pl-9 pr-9 text-xs text-slate-900 placeholder:text-slate-400 outline-none transition focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-600/10"
                />
                <button
                  type="button"
                  onClick={() => setShowNewPassword((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  {showNewPassword ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </button>
              </div>
            </div>

            <div>
              <label
                htmlFor="reset-page-confirm-pw"
                className="mb-1.5 block text-xs font-semibold text-slate-700"
              >
                Xác nhận mật khẩu mới
              </label>
              <div className="relative group">
                <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600" />
                <input
                  ref={confirmPasswordRef}
                  id="reset-page-confirm-pw"
                  type={showConfirmPassword ? "text" : "password"}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="•••••••• (Enter để xác nhận)"
                  required
                  className="w-full h-10 rounded-xl border border-slate-300/80 bg-slate-50/70 pl-9 pr-9 text-xs text-slate-900 placeholder:text-slate-400 outline-none transition focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-600/10"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  {showConfirmPassword ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className={cn(
                "flex w-full h-10 items-center justify-center gap-1.5 rounded-xl bg-indigo-600 text-xs font-semibold text-white shadow-sm transition hover:bg-indigo-700 active:bg-indigo-800",
                "disabled:cursor-not-allowed disabled:opacity-60"
              )}
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  <span>Đang xử lý...</span>
                </>
              ) : (
                <>
                  <span>Cập nhật Mật khẩu</span>
                  <ArrowRight className="h-3.5 w-3.5" />
                </>
              )}
            </button>

            <div className="pt-2 text-center">
              <Link
                to="/login"
                className="text-xs text-slate-500 hover:text-indigo-600 transition hover:underline"
              >
                &larr; Quay lại Đăng nhập
              </Link>
            </div>
          </form>
        )}
      </div>

      <div className="relative z-10 mt-6 text-center text-xs text-slate-500">
        © {new Date().getFullYear()} EMS Enterprise Platform
      </div>
    </div>
  );
}

