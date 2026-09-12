"use client";

import { useState, useEffect, useRef, type FormEvent, type KeyboardEvent } from "react";
import {
    User,
    Lock,
    Eye,
    EyeOff,
    ArrowRight,
    Loader2,
    CheckCircle2,
    AlertCircle,
} from "lucide-react";
import { cn } from "@/lib/utils";
import ForgotPasswordModal from "./ForgotPasswordModal";
import InteractiveParticleBackground from "./InteractiveParticleBackground";

export interface LoginCredentials {
    username: string;
    password: string;
    remember: boolean;
}

interface AdminLoginPageProps {
    /** Called on submit. Throw or reject to surface an error message. */
    onLogin?: (credentials: LoginCredentials) => Promise<void> | void;
    /** Optional server-side error message (e.g. from a redirected request). */
    initialError?: string;
}

export default function LoginPage({ onLogin, initialError }: AdminLoginPageProps) {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [remember, setRemember] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(initialError ?? null);
    const [isForgotPasswordOpen, setIsForgotPasswordOpen] = useState(false);
    const [resetSuccessAlert, setResetSuccessAlert] = useState(false);

    const passwordInputRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        if (typeof window !== "undefined") {
            const params = new URLSearchParams(window.location.search);
            if (params.get("resetSuccess") === "true") {
                setResetSuccessAlert(true);
            }
        }
    }, []);

    // Nhấn Enter ở ô Username -> Nhảy xuống ô Password
    const handleUsernameKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter") {
            e.preventDefault();
            if (passwordInputRef.current) {
                passwordInputRef.current.focus();
            }
        }
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError(null);

        if (!username.trim() || !password) {
            setError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }

        setIsSubmitting(true);
        try {
            await onLogin?.({ username: username.trim(), password, remember });
        } catch (err) {
            setError(
                err instanceof Error ? err.message : "Đăng nhập thất bại. Vui lòng thử lại."
            );
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="relative min-h-screen flex flex-col justify-center items-center bg-slate-200/75 px-4 py-8 antialiased text-slate-800 overflow-hidden">
            {/* Interactive Particle Attraction Canvas (Tụ lại theo con trỏ chuột) */}
            <InteractiveParticleBackground />

            {/* Subtle Moving Ambient Lights (Êm dịu trên nền xám) */}
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

            {/* Login Card (Màu trắng nổi bật trên nền xám đậm nhẹ) */}
            <div className="relative z-10 w-full max-w-[380px] rounded-2xl border border-slate-300/80 bg-white p-6 sm:p-7 shadow-[0_10px_35px_rgba(0,0,0,0.06)] transition-all duration-200 hover:shadow-[0_15px_40px_rgba(0,0,0,0.09)]">
                {/* Header / Brand */}
                <div className="text-center mb-6">
                    <div className="relative inline-flex mb-3">
                        <div className="h-11 w-11 rounded-xl bg-indigo-600 text-white font-bold text-sm shadow-md shadow-indigo-600/20 flex items-center justify-center transition-transform duration-300 hover:scale-105">
                            EM
                        </div>
                        <span className="absolute -top-1 -right-1 flex h-3 w-3">
                            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                            <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500 border-2 border-white"></span>
                        </span>
                    </div>

                    <h1 className="text-lg font-bold text-slate-900 tracking-tight">
                        Employee Management System
                    </h1>
                    <p className="mt-1 text-xs text-slate-500">
                        Hệ thống quản trị & vận hành nhân sự
                    </p>
                </div>

                {/* Alerts */}
                {resetSuccessAlert && (
                    <div
                        role="status"
                        className="mb-4 flex items-start gap-2 rounded-xl border border-emerald-200 bg-emerald-50 p-2.5 text-xs text-emerald-800"
                    >
                        <CheckCircle2 className="h-4 w-4 shrink-0 mt-0.5 text-emerald-600" />
                        <span>Đặt lại mật khẩu thành công! Hãy đăng nhập bằng mật khẩu mới.</span>
                    </div>
                )}

                {error && (
                    <div
                        role="alert"
                        className="mb-4 flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-2.5 text-xs text-red-800"
                    >
                        <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-red-600" />
                        <span>{error}</span>
                    </div>
                )}

                {/* Form */}
                <form onSubmit={handleSubmit} noValidate className="space-y-4">
                    <div>
                        <label
                            htmlFor="username"
                            className="mb-1.5 block text-xs font-semibold text-slate-700"
                        >
                            Tên đăng nhập
                        </label>
                        <div className="relative group">
                            <User className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600" />
                            <input
                                id="username"
                                type="text"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                onKeyDown={handleUsernameKeyDown}
                                placeholder="Nhập tên tài khoản (Enter để qua mật khẩu)"
                                autoComplete="username"
                                autoFocus
                                required
                                className="w-full h-10 rounded-xl border border-slate-300/80 bg-slate-50/70 pl-9 pr-3.5 text-xs text-slate-900 placeholder:text-slate-400 outline-none transition duration-150 focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-600/10"
                            />
                        </div>
                    </div>

                    <div>
                        <div className="flex items-center justify-between mb-1.5">
                            <label
                                htmlFor="password"
                                className="block text-xs font-semibold text-slate-700"
                            >
                                Mật khẩu
                            </label>
                            <button
                                type="button"
                                onClick={() => setIsForgotPasswordOpen(true)}
                                className="text-xs font-medium text-indigo-600 hover:text-indigo-700 hover:underline transition"
                            >
                                Quên mật khẩu?
                            </button>
                        </div>
                        <div className="relative group">
                            <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-indigo-600" />
                            <input
                                ref={passwordInputRef}
                                id="password"
                                type={showPassword ? "text" : "password"}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="•••••••• (Enter để đăng nhập)"
                                autoComplete="current-password"
                                required
                                className="w-full h-10 rounded-xl border border-slate-300/80 bg-slate-50/70 pl-9 pr-9 text-xs text-slate-900 placeholder:text-slate-400 outline-none transition duration-150 focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-600/10"
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword((v) => !v)}
                                aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition"
                            >
                                {showPassword ? (
                                    <EyeOff className="h-4 w-4" />
                                ) : (
                                    <Eye className="h-4 w-4" />
                                )}
                            </button>
                        </div>
                    </div>

                    <div className="flex items-center">
                        <input
                            id="remember-me"
                            type="checkbox"
                            checked={remember}
                            onChange={(e) => setRemember(e.target.checked)}
                            className="h-3.5 w-3.5 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 accent-indigo-600 cursor-pointer"
                        />
                        <label
                            htmlFor="remember-me"
                            className="ml-2 text-xs text-slate-600 cursor-pointer select-none hover:text-slate-800"
                        >
                            Ghi nhớ đăng nhập
                        </label>
                    </div>

                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className={cn(
                            "w-full h-10 rounded-xl bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white text-xs font-semibold shadow-sm shadow-indigo-600/20 flex items-center justify-center gap-1.5 transition duration-150 hover:-translate-y-0.5 active:translate-y-0",
                            "disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
                        )}
                    >
                        {isSubmitting ? (
                            <>
                                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                                <span>Đang xác thực...</span>
                            </>
                        ) : (
                            <>
                                <span>Đăng nhập</span>
                                <ArrowRight className="h-3.5 w-3.5" />
                            </>
                        )}
                    </button>
                </form>

                <div className="mt-6 pt-5 border-t border-slate-100 text-center">
                    <p className="text-[11px] text-slate-400">
                        Hệ thống nội bộ • Cần hỗ trợ?{" "}
                        <a
                            href="#"
                            className="font-medium text-slate-600 hover:text-indigo-600 transition hover:underline"
                        >
                            Liên hệ IT
                        </a>
                    </p>
                </div>
            </div>

            {/* Bottom copyright */}
            <div className="relative z-10 mt-6 text-center text-xs text-slate-500">
                © {new Date().getFullYear()} EMS Enterprise Platform
            </div>

            {/* Forgot Password Modal */}
            <ForgotPasswordModal
                isOpen={isForgotPasswordOpen}
                onClose={() => setIsForgotPasswordOpen(false)}
                onResetSuccess={() => {
                    setIsForgotPasswordOpen(false);
                    setResetSuccessAlert(true);
                }}
            />
        </div>
    );
}

