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
        <div className="relative min-h-screen overflow-x-hidden bg-[#241369] text-[#f6f4ff]">
            {/* ---------- ambient backdrop (Mesh Gradient mượt chuẩn mẫu Aero) ---------- */}
            <div className="pointer-events-none fixed inset-0 z-0" aria-hidden="true">
                <div
                    className="absolute inset-0"
                    style={{
                        background: "linear-gradient(165deg, #a855f7 0%, #7c3aed 22%, #5b21b6 38%, #4338ca 55%, #3b82f6 78%, #60a5fa 100%)",
                    }}
                />
            </div>

            {/* ---------- top bar ---------- */}
            <header className="relative z-10 flex items-center justify-between px-5 py-5 sm:px-10 lg:px-14">
                <div className="flex items-center gap-3">
                    <div className="flex h-[38px] w-[38px] items-center justify-center rounded-[12px] border border-white/20 bg-white/10 backdrop-blur-xl font-[Sora,sans-serif] text-[15px] font-extrabold text-white shadow-sm">
                        EM
                    </div>
                    <div className="font-[Sora,sans-serif] text-[16.5px] font-bold tracking-tight text-white drop-shadow-sm">
                        Employee <span className="font-extrabold text-[#63ecc8]">Management</span> System
                    </div>
                </div>
            </header>

            {/* ---------- main stage ---------- */}
            <main className="relative z-10 flex min-h-[calc(100vh-82px)] items-center px-5 pb-14 pt-6 sm:px-10 lg:px-[90px]">
                <div className="mx-auto flex w-full max-w-[1180px] flex-col items-center gap-10">
                    {/* pitch */}
                    <section className="text-center">
                        <span className="mb-4 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3.5 py-1 text-[12px] font-medium tracking-wider text-[#63ecc8] backdrop-blur-md">
                            HỆ THỐNG NỘI BỘ
                        </span>
                        <h1 className="mb-4 font-[Sora,sans-serif] text-[34px] font-extrabold leading-[1.08] tracking-tight text-white drop-shadow-sm sm:text-[42px] lg:text-[50px]">
                            Employee Management
                            <br />
                            <span className="bg-gradient-to-r from-[#63ecc8] via-[#00d2ff] to-[#a800ff] bg-clip-text text-transparent">
                                System
                            </span>
                        </h1>
                        <p className="mx-auto mb-8 max-w-[440px] text-[15.5px] font-normal leading-[1.7] text-white/80">
                            Hệ thống quản trị và vận hành EMS.
                        </p>
                    </section>

                    {/* login card (Sửa màu kính & hiệu ứng mờ theo mẫu Aero) */}
                    <section
                        aria-label="Đăng nhập"
                        className="relative mx-auto w-full max-w-[440px] rounded-[36px] border border-white/25 bg-white/[0.07] p-9 pb-8 text-white shadow-[0_20px_50px_rgba(0,0,0,0.18)] backdrop-blur-3xl transition-all duration-300 hover:border-white/35"
                    >
                        <div className="relative z-10 flex items-center gap-3.5">
                            <div className="flex h-11 w-11 items-center justify-center rounded-2xl border border-white/20 bg-white/10 backdrop-blur-md">
                                <ShieldCheck className="h-5.5 w-5.5 stroke-[1.5] text-white/90" />
                            </div>
                            <div>
                                <h2 className="font-[Sora,sans-serif] text-[19px] font-semibold tracking-tight text-white">
                                    Đăng nhập tài khoản
                                </h2>
                                <p className="mt-0.5 text-[12.5px] font-normal text-white/60">
                                    Bảng điều khiển EMS
                                </p>
                            </div>
                        </div>

                        <form className="relative z-10 mt-7" onSubmit={handleSubmit} noValidate>
                            <div className="mb-4">
                                <label
                                    htmlFor="username"
                                    className="mb-1.5 block text-[12.5px] font-medium tracking-wide text-white/80"
                                >
                                    Tên đăng nhập
                                </label>
                                <div className="relative">
                                    <Mail className="pointer-events-none absolute left-3.5 top-1/2 h-[17px] w-[17px] -translate-y-1/2 stroke-[1.6] text-white/60" />
                                    <input
                                        id="username"
                                        type="text"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                        placeholder="tentaikhoan"
                                        autoComplete="username"
                                        required
                                        className="w-full rounded-[16px] border border-white/20 bg-white/[0.05] py-3 pl-10 pr-3.5 text-[14.5px] text-white outline-none transition placeholder:text-white/35 backdrop-blur-xl hover:border-white/30 hover:bg-white/[0.08] focus:border-white/50 focus:bg-white/[0.1] focus:ring-2 focus:ring-white/10"
                                    />
                                </div>
                            </div>

                            <div className="mb-4">
                                <label
                                    htmlFor="password"
                                    className="mb-1.5 block text-[12.5px] font-medium tracking-wide text-white/80"
                                >
                                    Mật khẩu
                                </label>
                                <div className="relative">
                                    <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-[17px] w-[17px] -translate-y-1/2 stroke-[1.6] text-white/60" />
                                    <input
                                        id="password"
                                        type={showPassword ? "text" : "password"}
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        placeholder="••••••••"
                                        autoComplete="current-password"
                                        required
                                        className="w-full rounded-[16px] border border-white/20 bg-white/[0.05] py-3 pl-10 pr-10 text-[14.5px] text-white outline-none transition placeholder:text-white/35 backdrop-blur-xl hover:border-white/30 hover:bg-white/[0.08] focus:border-white/50 focus:bg-white/[0.1] focus:ring-2 focus:ring-white/10"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowPassword((v) => !v)}
                                        aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                                        className="absolute right-3 top-1/2 -translate-y-1/2 text-white/60 hover:text-white transition"
                                    >
                                        {showPassword ? (
                                            <EyeOff className="h-[17px] w-[17px] stroke-[1.6]" />
                                        ) : (
                                            <Eye className="h-[17px] w-[17px] stroke-[1.6]" />
                                        )}
                                    </button>
                                </div>
                            </div>

                            <div className="mb-6 flex items-center justify-between">
                                <label className="flex items-center gap-2 text-[13px] text-white/75 cursor-pointer">
                                    <input
                                        type="checkbox"
                                        checked={remember}
                                        onChange={(e) => setRemember(e.target.checked)}
                                        className="h-[15px] w-[15px] rounded border-white/20 bg-white/5 accent-[#63ecc8]"
                                    />
                                    Ghi nhớ đăng nhập
                                </label>
                                <a href="#" className="text-[13px] font-medium text-white/80 hover:text-white transition hover:underline">
                                    Quên mật khẩu?
                                </a>
                            </div>

                            {error && (
                                <div
                                    role="alert"
                                    className="mb-4 rounded-[14px] border border-red-400/30 bg-red-500/10 px-3.5 py-2.5 text-[13px] text-red-100 backdrop-blur-md"
                                >
                                    {error}
                                </div>
                            )}

                            <button
                                type="submit"
                                disabled={isSubmitting}
                                className={cn(
                                    "flex w-full items-center justify-center gap-2 rounded-[16px] border border-white/25 bg-white/10 py-3.5 text-[14.5px] font-semibold text-white shadow-sm backdrop-blur-xl transition-all duration-200",
                                    "hover:bg-white/20 hover:border-white/40 hover:-translate-y-0.5 active:translate-y-0",
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
                                        <ArrowRight className="h-4 w-4 stroke-[1.8]" />
                                    </>
                                )}
                            </button>
                        </form>

                        <p className="relative z-10 mt-6 text-center text-[12.5px] text-white/60">
                            Gặp sự cố truy cập?{" "}
                            <a href="#" className="font-medium text-white/90 hover:text-white transition hover:underline">
                                Liên hệ bộ phận IT
                            </a>
                        </p>
                    </section>
                </div>
            </main>
        </div>
    );
}