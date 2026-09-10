"use client";

import { useState, type FormEvent, useEffect } from "react";
import { useSearchParams, useNavigate, Link } from "react-router-dom";
import {
  Lock,
  Eye,
  EyeOff,
  KeyRound,
  ShieldCheck,
  Loader2,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { resetPassword } from "@/lib/api/auth";

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

  useEffect(() => {
    document.title = "Đặt lại mật khẩu | Employee Management System";
    if (queryToken) {
      setToken(queryToken);
    }
  }, [queryToken]);

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
    <div className="relative min-h-screen overflow-x-hidden bg-[#241369] text-[#f6f4ff]">
      {/* Background Mesh Gradient */}
      <div className="pointer-events-none fixed inset-0 z-0" aria-hidden="true">
        <div
          className="absolute inset-0"
          style={{
            background:
              "linear-gradient(165deg, #a855f7 0%, #7c3aed 22%, #5b21b6 38%, #4338ca 55%, #3b82f6 78%, #60a5fa 100%)",
          }}
        />
      </div>

      {/* Top Header */}
      <header className="relative z-10 flex items-center justify-between px-5 py-5 sm:px-10 lg:px-14">
        <div className="flex items-center gap-3">
          <div className="flex h-[38px] w-[38px] items-center justify-center rounded-[12px] border border-white/20 bg-white/10 backdrop-blur-xl font-[Sora,sans-serif] text-[15px] font-extrabold text-white shadow-sm">
            EM
          </div>
          <div className="font-[Sora,sans-serif] text-[16.5px] font-bold tracking-tight text-white drop-shadow-sm">
            Employee <span className="font-extrabold text-[#63ecc8]">Management</span> System
          </div>
        </div>
        <Link
          to="/login"
          className="text-[13.5px] font-medium text-white/80 hover:text-white transition hover:underline"
        >
          &larr; Quay lại Đăng nhập
        </Link>
      </header>

      {/* Main Content */}
      <main className="relative z-10 flex min-h-[calc(100vh-82px)] items-center px-5 pb-14 pt-6 sm:px-10">
        <div className="mx-auto w-full max-w-[460px]">
          <section
            aria-label="Đặt lại mật khẩu"
            className="relative rounded-[36px] border border-white/25 bg-white/[0.08] p-8 sm:p-9 text-white shadow-[0_20px_50px_rgba(0,0,0,0.25)] backdrop-blur-3xl transition duration-300"
          >
            <div className="relative z-10 flex items-center gap-3.5 mb-6">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl border border-white/20 bg-white/10 backdrop-blur-md text-[#63ecc8]">
                <ShieldCheck className="h-5.5 w-5.5 stroke-[1.8]" />
              </div>
              <div>
                <h1 className="font-[Sora,sans-serif] text-[20px] font-bold tracking-tight text-white">
                  Đặt lại mật khẩu
                </h1>
                <p className="text-[12.5px] text-white/60">
                  Nhập mật khẩu mới cho tài khoản của bạn
                </p>
              </div>
            </div>

            {success ? (
              <div className="space-y-4 py-3 text-center">
                <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full border border-emerald-400/30 bg-emerald-500/20 text-emerald-400">
                  <CheckCircle2 className="h-7 w-7" />
                </div>
                <h2 className="text-[18px] font-semibold text-white">
                  Đổi mật khẩu thành công!
                </h2>
                <p className="text-[13.5px] text-white/75 leading-relaxed">
                  Mật khẩu tài khoản của bạn đã được cập nhật. Đang tự động chuyển hướng về trang Đăng nhập sau giây lát...
                </p>
                <div className="pt-3">
                  <Link
                    to="/login"
                    className="inline-flex items-center justify-center gap-2 rounded-[16px] border border-white/25 bg-white/15 px-6 py-3 text-[14px] font-semibold text-white shadow-sm backdrop-blur-xl transition hover:bg-white/25"
                  >
                    Đăng nhập ngay
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </div>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4">
                {error && (
                  <div
                    role="alert"
                    className="flex items-start gap-2.5 rounded-[14px] border border-red-400/30 bg-red-500/15 p-3 text-[13px] text-red-100 backdrop-blur-md"
                  >
                    <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-red-400" />
                    <span>{error}</span>
                  </div>
                )}

                <div>
                  <label
                    htmlFor="reset-page-token"
                    className="mb-1.5 block text-[12.5px] font-medium text-white/80"
                  >
                    Mã Token khôi phục
                  </label>
                  <div className="relative">
                    <KeyRound className="pointer-events-none absolute left-3.5 top-1/2 h-[17px] w-[17px] -translate-y-1/2 text-white/50" />
                    <input
                      id="reset-page-token"
                      type="text"
                      value={token}
                      onChange={(e) => setToken(e.target.value)}
                      placeholder="Nhập hoặc dán mã Token từ email"
                      required
                      className="w-full rounded-[16px] border border-white/20 bg-white/[0.05] py-3 pl-10 pr-3.5 text-[14px] text-white font-mono outline-none placeholder:text-white/35 backdrop-blur-xl focus:border-white/50 focus:bg-white/[0.1] focus:ring-2 focus:ring-white/10"
                    />
                  </div>
                </div>

                <div>
                  <label
                    htmlFor="reset-page-new-pw"
                    className="mb-1.5 block text-[12.5px] font-medium text-white/80"
                  >
                    Mật khẩu mới (Tối thiểu 8 ký tự)
                  </label>
                  <div className="relative">
                    <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-[17px] w-[17px] -translate-y-1/2 text-white/50" />
                    <input
                      id="reset-page-new-pw"
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
                    htmlFor="reset-page-confirm-pw"
                    className="mb-1.5 block text-[12.5px] font-medium text-white/80"
                  >
                    Xác nhận mật khẩu mới
                  </label>
                  <div className="relative">
                    <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-[17px] w-[17px] -translate-y-1/2 text-white/50" />
                    <input
                      id="reset-page-confirm-pw"
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
                    "flex w-full items-center justify-center gap-2 rounded-[16px] border border-white/25 bg-white/10 py-3.5 text-[14.5px] font-semibold text-white shadow-sm backdrop-blur-xl transition",
                    "hover:bg-white/20 hover:border-white/40",
                    "disabled:cursor-not-allowed disabled:opacity-60"
                  )}
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Đang xử lý...
                    </>
                  ) : (
                    <>
                      Cập nhật Mật khẩu
                      <ArrowRight className="h-4 w-4" />
                    </>
                  )}
                </button>

                <div className="pt-2 text-center">
                  <Link
                    to="/login"
                    className="text-[13px] text-white/70 hover:text-white transition hover:underline"
                  >
                    Hủy và quay lại Đăng nhập
                  </Link>
                </div>
              </form>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
