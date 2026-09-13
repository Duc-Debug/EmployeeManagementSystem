"use client";

import { useState, type FormEvent, useEffect } from "react";
import {
  Mail,
  Lock,
  Eye,
  EyeOff,
  KeyRound,
  X,
  Loader2,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { forgotPassword, resetPassword } from "@/lib/api/auth";

interface ForgotPasswordModalProps {
  initialTab?: "request" | "reset";
  initialToken?: string;
  isOpen: boolean;
  onClose: () => void;
  onResetSuccess?: () => void;
}

export default function ForgotPasswordModal({
  initialTab = "request",
  initialToken = "",
  isOpen,
  onClose,
  onResetSuccess,
}: ForgotPasswordModalProps) {
  const [tab, setTab] = useState<"request" | "reset">(initialTab);
  
  // Tab 1: Request fields
  const [identity, setIdentity] = useState("");
  const [requestSent, setRequestSent] = useState(false);
  const [countdown, setCountdown] = useState(0);

  // Tab 2: Reset fields
  const [token, setToken] = useState(initialToken);
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Status
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    if (initialToken) {
      setToken(initialToken);
      setTab("reset");
    }
  }, [initialToken]);

  useEffect(() => {
    let timer: any;
    if (countdown > 0) {
      timer = setTimeout(() => setCountdown((c) => c - 1), 1000);
    }
    return () => clearTimeout(timer);
  }, [countdown]);

  if (!isOpen) return null;

  const handleClose = () => {
    setError(null);
    setSuccessMessage(null);
    onClose();
  };

  const handleRequestSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);

    if (!identity.trim()) {
      setError("Vui lòng nhập email hoặc tên đăng nhập.");
      return;
    }

    setIsSubmitting(true);
    try {
      const msg = await forgotPassword(identity.trim());
      setRequestSent(true);
      setSuccessMessage(msg);
      setCountdown(60); // 60s cooldown according to BE rate limiter
    } catch (err: unknown) {
      setError(
        err instanceof Error
          ? err.message
          : "Không thể gửi yêu cầu khôi phục. Vui lòng thử lại sau ít phút."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResetSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);

    if (!token.trim()) {
      setError("Vui lòng nhập mã token khôi phục.");
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
      const msg = await resetPassword({
        confirmPassword,
        newPassword,
        token: token.trim(),
      });
      setSuccessMessage(msg);
      setTimeout(() => {
        handleClose();
        if (onResetSuccess) {
          onResetSuccess();
        }
      }, 1500);
    } catch (err: unknown) {
      setError(
        err instanceof Error
          ? err.message
          : "Đặt lại mật khẩu thất bại. Mã token có thể không hợp lệ hoặc đã hết hạn."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs transition-opacity"
        onClick={handleClose}
      />

      {/* Modal Container */}
      <div
        role="dialog"
        aria-modal="true"
        className="relative z-10 w-full max-w-[420px] rounded-2xl border border-slate-300/80 bg-white p-6 text-slate-800 shadow-[0_15px_40px_rgba(0,0,0,0.12)] transition-all"
      >
        {/* Close button */}
        <button
          type="button"
          onClick={handleClose}
          className="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
          aria-label="Đóng"
        >
          <X className="h-4 w-4" />
        </button>

        {/* Modal Header */}
        <div className="flex items-center gap-3 mb-5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 border border-indigo-100 text-indigo-600">
            <KeyRound className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900 tracking-tight">
              Khôi phục mật khẩu
            </h2>
            <p className="text-xs text-slate-500">
              Hệ thống Quản trị EMS
            </p>
          </div>
        </div>

        {/* Tabs Switcher */}
        <div className="mb-5 flex rounded-xl bg-slate-100 p-1">
          <button
            type="button"
            onClick={() => {
              setTab("request");
              setError(null);
            }}
            className={cn(
              "flex-1 rounded-lg py-1.5 text-xs font-medium transition",
              tab === "request"
                ? "bg-white text-slate-900 shadow-xs font-semibold"
                : "text-slate-600 hover:text-slate-900"
            )}
          >
            1. Gửi mã xác nhận
          </button>
          <button
            type="button"
            onClick={() => {
              setTab("reset");
              setError(null);
            }}
            className={cn(
              "flex-1 rounded-lg py-1.5 text-xs font-medium transition",
              tab === "reset"
                ? "bg-white text-slate-900 shadow-xs font-semibold"
                : "text-slate-600 hover:text-slate-900"
            )}
          >
            2. Đặt lại mật khẩu
          </button>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="mb-4 flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-2.5 text-xs text-red-800">
            <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-red-600" />
            <span>{error}</span>
          </div>
        )}

        {/* Success Alert */}
        {successMessage && (
          <div className="mb-4 flex items-start gap-2 rounded-xl border border-emerald-200 bg-emerald-50 p-2.5 text-xs text-emerald-800">
            <CheckCircle2 className="h-4 w-4 shrink-0 mt-0.5 text-emerald-600" />
            <span>{successMessage}</span>
          </div>
        )}

        {/* Tab 1: Request Form */}
        {tab === "request" ? (
          <form onSubmit={handleRequestSubmit} className="space-y-4">
            <p className="text-xs text-slate-600 leading-relaxed">
              Nhập email hoặc tên đăng nhập. Nếu thông tin khớp hồ sơ nhân sự, hệ thống sẽ gửi mã Token khôi phục mật khẩu (hiệu lực trong 15 phút).
            </p>

            <div>
              <label
                htmlFor="forgot-identity"
                className="mb-1.5 block text-xs font-semibold text-slate-700"
              >
                Email hoặc Tên đăng nhập
              </label>
              <div className="relative group">
                <Mail className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600" />
                <input
                  id="forgot-identity"
                  type="text"
                  value={identity}
                  onChange={(e) => setIdentity(e.target.value)}
                  placeholder="user@company.com hoặc username"
                  required
                  className="w-full h-10 rounded-xl border border-slate-300/80 bg-slate-50/70 pl-9 pr-3.5 text-xs text-slate-900 placeholder:text-slate-400 outline-none transition focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-600/10"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isSubmitting || countdown > 0}
              className={cn(
                "flex w-full h-10 items-center justify-center gap-1.5 rounded-xl bg-indigo-600 text-xs font-semibold text-white shadow-sm transition hover:bg-indigo-700 active:bg-indigo-800",
                "disabled:cursor-not-allowed disabled:opacity-60"
              )}
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  <span>Đang gửi yêu cầu...</span>
                </>
              ) : countdown > 0 ? (
                `Vui lòng đợi ${countdown}s để gửi lại`
              ) : (
                <>
                  <span>Gửi mã xác nhận qua Email</span>
                  <ArrowRight className="h-3.5 w-3.5" />
                </>
              )}
            </button>

            {requestSent && (
              <div className="mt-3 pt-3 border-t border-slate-100 flex justify-center">
                <button
                  type="button"
                  onClick={() => {
                    setTab("reset");
                    setError(null);
                  }}
                  className="text-xs text-indigo-600 hover:text-indigo-700 hover:underline font-medium flex items-center gap-1"
                >
                  Đã nhận được mã? Chuyển sang Đặt lại mật khẩu &rarr;
                </button>
              </div>
            )}
          </form>
        ) : (
          /* Tab 2: Reset Form */
          <form onSubmit={handleResetSubmit} className="space-y-3.5">
            <div>
              <label
                htmlFor="reset-token"
                className="mb-1.5 block text-xs font-semibold text-slate-700"
              >
                Mã Token khôi phục
              </label>
              <div className="relative group">
                <KeyRound className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600" />
                <input
                  id="reset-token"
                  type="text"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  placeholder="Dán mã Token nhận từ email"
                  required
                  className="w-full h-10 rounded-xl border border-slate-300/80 bg-slate-50/70 pl-9 pr-3.5 text-xs text-slate-900 font-mono placeholder:text-slate-400 outline-none transition focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-600/10"
                />
              </div>
            </div>

            <div>
              <label
                htmlFor="reset-new-password"
                className="mb-1.5 block text-xs font-semibold text-slate-700"
              >
                Mật khẩu mới (Tối thiểu 8 ký tự)
              </label>
              <div className="relative group">
                <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600" />
                <input
                  id="reset-new-password"
                  type={showNewPassword ? "text" : "password"}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
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
                htmlFor="reset-confirm-password"
                className="mb-1.5 block text-xs font-semibold text-slate-700"
              >
                Xác nhận mật khẩu mới
              </label>
              <div className="relative group">
                <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600" />
                <input
                  id="reset-confirm-password"
                  type={showConfirmPassword ? "text" : "password"}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
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
                  <span>Đang cập nhật...</span>
                </>
              ) : (
                <>
                  <span>Xác nhận Đặt lại mật khẩu</span>
                  <ArrowRight className="h-3.5 w-3.5" />
                </>
              )}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
