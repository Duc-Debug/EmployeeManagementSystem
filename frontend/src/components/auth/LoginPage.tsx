"use client";

import { useState, type FormEvent } from "react";
import {
    Mail,
    Lock,
    Eye,
    EyeOff,
    ArrowRight,
    ShieldCheck,
    Loader2,
} from "lucide-react";
import { cn } from "@/lib/utils";

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
        <div className="relative min-h-screen overflow-x-hidden bg-[#0D091D] text-[#f6f4ff]">
            {/* ---------- ambient backdrop ---------- */}
            <div className="pointer-events-none fixed inset-0 z-0" aria-hidden="true">
                <div
                    className="absolute inset-0"
                    style={{
                        background:
                            "radial-gradient(1000px 800px at 10% 5%, #0042d2 0%, transparent 48%)," +
                            "radial-gradient(850px 650px at 90% 85%, #FB008B 0%, transparent 35%)," +
                            "linear-gradient(135deg, #0042d2 0%, #5a34b8 42%, #0D091D 100%)",
                    }}
                />
                <svg className="absolute inset-0 h-full w-full opacity-50" aria-hidden="true">
                    <defs>
                        <pattern id="ems-grid" width="46" height="46" patternUnits="userSpaceOnUse">
                            <path
                                d="M46 0H0V46"
                                fill="none"
                                stroke="rgba(246,244,255,.05)"
                                strokeWidth="1"
                            />
                        </pattern>
                    </defs>
                    <rect width="100%" height="100%" fill="url(#ems-grid)" />
                </svg>

            </div>

            {/* ---------- top bar ---------- */}
            <header className="relative z-10 flex items-center justify-between px-5 py-5 sm:px-10 lg:px-14">
                <div className="flex items-center gap-3">
                    <div className="flex h-[38px] w-[38px] items-center justify-center rounded-[10px] bg-gradient-to-br from-[#33d6ad] to-[#7c5cff] font-[Sora,sans-serif] text-[15px] font-extrabold text-[#140b30] shadow-[0_8px_24px_rgba(51,214,173,0.28)]">
                        EM
                    </div>
                    <div className="font-[Sora,sans-serif] text-[16.5px] font-bold tracking-tight">
                        Employee <span className="font-extrabold text-[#63ecc8]">Management</span> System
                    </div>
                </div>
            </header>

            {/* ---------- main stage ---------- */}
            <main className="relative z-10 flex min-h-[calc(100vh-82px)] items-center px-5 pb-14 pt-6 sm:px-10 lg:px-[90px]">
                <div className="mx-auto flex w-full max-w-[1180px] flex-col items-center gap-10">
                    {/* pitch */}
                    <section className="text-center">
                        <span className="mb-5 inline-flex items-center gap-2 text-[12.5px] font-bold uppercase tracking-[0.14em] text-[#63ecc8]">
                            Hệ thống nội bộ
                        </span>
                        <h1 className="mb-5 font-[Sora,sans-serif] text-[34px] font-extrabold leading-[1.08] tracking-tight sm:text-[42px] lg:text-[50px]">
                            Employee Management
                            <br />
                            <span className="bg-gradient-to-r from-[#63ecc8] to-[#7c5cff] bg-clip-text text-transparent">
                            System
                            </span>
                        </h1>
                        <p className="mx-auto mb-8 max-w-[440px] text-[16px] leading-[1.7] text-[#f6f4ff]/70">
                            Hệ thống EMS.
                        </p>
                    </section>

                    {/* login card */}
                    <section
                        aria-label="Đăng nhập"
                        className="relative mx-auto w-full max-w-[440px] rounded-[22px] border border-white/50 bg-gradient-to-b from-white/[0.98] to-[#f6f4ff]/[0.96] p-9 pb-8 text-[#140b30] shadow-[0_30px_70px_rgba(10,4,32,0.45)]"
                    >
                        <div className="flex items-center gap-3">
                            <div className="flex h-10 w-10 items-center justify-center rounded-xl border border-[#e3e2f7] bg-gradient-to-br from-[#ecfbf6] to-[#eef0ff]">
                                <ShieldCheck className="h-5 w-5 stroke-[1.8] text-[#6136c9]" />
                            </div>
                            <div>
                                <h2 className="font-[Sora,sans-serif] text-[19px] font-bold">
                                    Đăng nhập tài khoản
                                </h2>
                                <p className="mt-0.5 text-[12.5px] text-[#786fa0]">
                                    Bảng điều khiển EMS
                                </p>
                            </div>
                        </div>

                        <form className="mt-6" onSubmit={handleSubmit} noValidate>
                            <div className="mb-4">
                                <label
                                    htmlFor="username"
                                    className="mb-1.5 block text-[12.5px] font-bold tracking-wide text-[#4b3f7d]"
                                >
                                    Tên đăng nhập / Email
                                </label>
                                <div className="relative">
                                    <Mail className="pointer-events-none absolute left-3.5 top-1/2 h-[17px] w-[17px] -translate-y-1/2 stroke-[1.8] text-[#9b93c4]" />
                                    <input
                                        id="username"
                                        type="text"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                        placeholder="taikhoan@gmail.com"
                                        autoComplete="username"
                                        required
                                        className="w-full rounded-[11px] border-[1.5px] border-[#e4e1f5] bg-[#fbfaff] py-3 pl-10 pr-3.5 text-[14.5px] text-[#140b30] outline-none transition placeholder:text-[#a9a2c8] focus:border-[#7c5cff] focus:bg-white focus:ring-4 focus:ring-[#7c5cff]/15"
                                    />
                                </div>
                            </div>

                            <div className="mb-4">
                                <label
                                    htmlFor="password"
                                    className="mb-1.5 block text-[12.5px] font-bold tracking-wide text-[#4b3f7d]"
                                >
                                    Mật khẩu
                                </label>
                                <div className="relative">
                                    <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-[17px] w-[17px] -translate-y-1/2 stroke-[1.8] text-[#9b93c4]" />
                                    <input
                                        id="password"
                                        type={showPassword ? "text" : "password"}
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        placeholder="••••••••"
                                        autoComplete="current-password"
                                        required
                                        className="w-full rounded-[11px] border-[1.5px] border-[#e4e1f5] bg-[#fbfaff] py-3 pl-10 pr-10 text-[14.5px] text-[#140b30] outline-none transition placeholder:text-[#a9a2c8] focus:border-[#7c5cff] focus:bg-white focus:ring-4 focus:ring-[#7c5cff]/15"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowPassword((v) => !v)}
                                        aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                                        className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9b93c4] hover:text-[#6136c9]"
                                    >
                                        {showPassword ? (
                                            <EyeOff className="h-[17px] w-[17px] stroke-[1.8]" />
                                        ) : (
                                            <Eye className="h-[17px] w-[17px] stroke-[1.8]" />
                                        )}
                                    </button>
                                </div>
                            </div>

                            <div className="mb-5 flex items-center justify-between">
                                <label className="flex items-center gap-2 text-[13px] text-[#5c5286]">
                                    <input
                                        type="checkbox"
                                        checked={remember}
                                        onChange={(e) => setRemember(e.target.checked)}
                                        className="h-[15px] w-[15px] accent-[#7c5cff]"
                                    />
                                    Ghi nhớ đăng nhập
                                </label>
                                <a href="#" className="text-[13px] font-semibold text-[#6136c9] hover:underline">
                                    Quên mật khẩu?
                                </a>
                            </div>

                            {error && (
                                <div
                                    role="alert"
                                    className="mb-4 rounded-[10px] border border-red-200 bg-red-50 px-3.5 py-2.5 text-[13px] text-red-600"
                                >
                                    {error}
                                </div>
                            )}

                            <button
                                type="submit"
                                disabled={isSubmitting}
                                className={cn(
                                    "flex w-full items-center justify-center gap-2 rounded-[11px] bg-gradient-to-r from-[#33d6ad] to-[#63ecc8] py-3.5 text-[14.5px] font-bold text-[#0c1420] shadow-[0_12px_26px_rgba(51,214,173,0.32)] transition",
                                    "hover:-translate-y-px hover:shadow-[0_16px_32px_rgba(51,214,173,0.4)] active:translate-y-0",
                                    "disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0"
                                )}
                            >
                                {isSubmitting ? (
                                    <>
                                        <Loader2 className="h-4 w-4 animate-spin" />
                                        Đang đăng nhập...
                                    </>
                                ) : (
                                    <>
                                        Đăng nhập
                                        <ArrowRight className="h-4 w-4" />
                                    </>
                                )}
                            </button>
                        </form>

                        <p className="mt-5 text-center text-[12.5px] text-[#9b93c4]">
                            Gặp sự cố truy cập?{" "}
                            <a href="#" className="font-semibold text-[#6136c9] hover:underline">
                                Liên hệ bộ phận IT
                            </a>
                        </p>
                    </section>
                </div>
            </main>
        </div>
    );
}