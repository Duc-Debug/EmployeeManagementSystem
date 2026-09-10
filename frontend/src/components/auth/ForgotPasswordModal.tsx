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
        className="fixed inset-0 bg-black/60 backdrop-blur-sm transition-opacity"
        onClick={handleClose}
      />

      {/* Modal Container */}
      <div
        role="dialog"
        aria-modal="true"
        className="relative z-10 w-full max-w-[480px] rounded-[32px] border border-white/20 bg-[#1e144a]/95 p-7 text-white shadow-[0_25px_60px_rgba(0,0,0,0.5)] backdrop-blur-2xl"
      >
        {/* Close button */}
        <button
          type="button"
          onClick={handleClose}
          className="absolute right-5 top-5 flex h-9 w-9 items-center justify-center rounded-full border border-white/15 bg-white/5 text-white/70 hover:bg-white/10 hover:text-white transition"
          aria-label="Đóng"
        >
          <X className="h-4 w-4" />
        </button>

        {/* Modal Header */}
        <div className="flex items-center gap-3.5 mb-6">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl border border-white/20 bg-white/10 backdrop-blur-md text-[#63ecc8]">
            <KeyRound className="h-5 w-5 stroke-[1.8]" />
          </div>
          <div>
            <h2 className="font-[Sora,sans-serif] text-[19px] font-bold tracking-tight text-white">
              Khôi phục mật khẩu
            </h2>
            <p className="text-[12.5px] text-white/60">
              Hệ thống Quản trị Nguồn lực EMS
            </p>
          </div>
        </div>

        {/* Tabs Switcher */}
        <div className="mb-6 flex rounded-[16px] border border-white/15 bg-white/[0.05] p-1">
          <button
            type="button"
            onClick={() => {
              setTab("request");
              setError(null);
            }}
            className={cn(
              "flex-1 rounded-[12px] py-2 text-[13px] font-medium transition",
              tab === "request"
                ? "bg-white/15 text-white shadow-sm"
                : "text-white/60 hover:text-white"
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
              "flex-1 rounded-[12px] py-2 text-[13px] font-medium transition",
              tab === "reset"
                ? "bg-white/15 text-white shadow-sm"
                : "text-white/60 hover:text-white"
            )}
          >
            2. Đặt lại mật khẩu
          </button>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="mb-5 flex items-start gap-2.5 rounded-[14px] border border-red-500/30 bg-red-500/15 p-3 text-[13px] text-red-100 backdrop-blur-md">
            <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-red-400" />
            <span>{error}</span>
          </div>
        )}

        {/* Success Alert */}
        {successMessage && (
          <div className="mb-5 flex items-start gap-2.5 rounded-[14px] border border-emerald-500/30 bg-emerald-500/15 p-3 text-[13px] text-emerald-100 backdrop-blur-md">
            <CheckCircle2 className="h-4 w-4 shrink-0 mt-0.5 text-emerald-400" />
            <span>{successMessage}</span>
          </div>
        )}

        {/* Tab 1: Request Form */}
        {tab === "request" ? (
          <form onSubmit={handleRequestSubmit} className="space-y-4">
            <p className="text-[13px] text-white/75 leading-relaxed">
              Nhập email hoặc tên đăng nhập của bạn. Nếu thông tin khớp với hồ sơ nhân sự, hệ thống sẽ gửi liên kết và mã Token để khôi phục mật khẩu (có hiệu lực trong 15 phút).
            </p>

            <div>
              <label
                htmlFor="forgot-identity"
                className="mb-1.5 block text-[12.5px] font-medium text-white/80"
              >
                Email hoặc Tên đăng nhập
              </label>
              <div className="relative">
                <Mail className="pointer-events-none absolute left-3.5 top-1/2 h-[17px] w-[17px] -translate-y-1/2 text-white/50" />
                <input
                  id="forgot-identity"
                  type="text"
                  value={identity}
                  onChange={(e) => setIdentity(e.target.value)}
                  placeholder="vd. user@company.com hoặc employee1"
                  required
                  className="w-full rounded-[16px] border border-white/20 bg-white/[0.05] py-3 pl-10 pr-3.5 text-[14px] text-white outline-none placeholder:text-white/35 backdrop-blur-xl focus:border-white/50 focus:bg-white/[0.1] focus:ring-2 focus:ring-white/10"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isSubmitting || countdown > 0}
              className={cn(
                "flex w-full items-center justify-center gap-2 rounded-[16px] border border-white/25 bg-white/10 py-3 text-[14px] font-semibold text-white shadow-sm backdrop-blur-xl transition",
                "hover:bg-white/20 hover:border-white/40",
                "disabled:cursor-not-allowed disabled:opacity-60"
              )}
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Đang gửi yêu cầu...
                </>
              ) : countdown > 0 ? (
                `Vui lòng đợi ${countdown}s để gửi lại`
              ) : (
                <>
                  Gửi mã xác nhận qua Email
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </button>

            {requestSent && (
              <div className="mt-4 pt-4 border-t border-white/10 flex justify-center">
                <button
                  type="button"
                  onClick={() => {
                    setTab("reset");
                    setError(null);
                  }}
                  className="text-[13px] text-[#63ecc8] hover:underline font-medium flex items-center gap-1.5"
                >
                  Đã nhận được mã? Chuyển sang Đặt lại mật khẩu &rarr;
                </button>
              </div>
            )}
          </form>
        ) : (
          /* Tab 2: Reset Form */
          <form onSubmit={handleResetSubmit} className="space-y-4">
            <div>
              <label
                htmlFor="reset-token"
                className="mb-1.5 block text-[12.5px] font-medium text-white/80"
              >
                Mã Token khôi phục
              </label>
              <div className="relative">
                <KeyRound className="pointer-events-none absolute left-3.5 top-1/2 h-[17px] w-[17px] -translate-y-1/2 text-white/50" />
                <input
                  id="reset-token"
                  type="text"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  placeholder="Dán mã Token nhận từ email vào đây"
                  required
                  className="w-full rounded-[16px] border border-white/20 bg-white/[0.05] py-3 pl-10 pr-3.5 text-[14px] text-white font-mono outline-none placeholder:text-white/35 backdrop-blur-xl focus:border-white/50 focus:bg-white/[0.1] focus:ring-2 focus:ring-white/10"
                />
              </div>
            </div>

            <div>
              <label
                htmlFor="reset-new-password"
                className="mb-1.5 block text-[12.5px] font-medium text-white/80"
              >
                Mật khẩu mới (Tối thiểu 8 ký tự)
              </label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-[17px] w-[17px] -translate-y-1/2 text-white/50" />
                <input
                  id="reset-new-password"
                  type={showNewPassword ? "text" : "password"}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  className="w-full rounded-[16px] border border-white/20 bg-white/[0.05] py-3 pl-10 pr-10 text-[14px] text-white outline-none placeholder:text-white/35 backdrop-blur-xl focus:border-white/50 focus:bg-white/[0.1] focus:ring-2 focus:ring-white/10"
                />
                <button
                  type="button"
                  onClick={() => setShowNewPassword((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-white/60 hover:text-white"
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
                className="mb-1.5 block text-[12.5px] font-medium text-white/80"
              >
                Xác nhận mật khẩu mới
              </label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-[17px] w-[17px] -translate-y-1/2 text-white/50" />
                <input
                  id="reset-confirm-password"
                  type={showConfirmPassword ? "text" : "password"}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  className="w-full rounded-[16px] border border-white/20 bg-white/[0.05] py-3 pl-10 pr-10 text-[14px] text-white outline-none placeholder:text-white/35 backdrop-blur-xl focus:border-white/50 focus:bg-white/[0.1] focus:ring-2 focus:ring-white/10"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-white/60 hover:text-white"
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
                "flex w-full items-center justify-center gap-2 rounded-[16px] border border-white/25 bg-white/10 py-3 text-[14px] font-semibold text-white shadow-sm backdrop-blur-xl transition",
                "hover:bg-white/20 hover:border-white/40",
                "disabled:cursor-not-allowed disabled:opacity-60"
              )}
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Đang cập nhật...
                </>
              ) : (
                <>
                  Xác nhận Đặt lại mật khẩu
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
