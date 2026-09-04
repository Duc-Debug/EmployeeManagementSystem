import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Menu, Bell, Settings, Clock, User, LogOut } from "lucide-react";
import UserProfileModal from "../profile/UserProfileModal";

interface HeaderProps {
    setIsSidebarOpen: React.Dispatch<React.SetStateAction<boolean>>;
}

export default function Header({ setIsSidebarOpen }: HeaderProps) {
    const [currentTime, setCurrentTime] = useState<string>("");
    const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false);
    const [isAccountSettingsOpen, setIsAccountSettingsOpen] = useState<boolean>(false);

    const dropdownRef = useRef<HTMLDivElement>(null);
    const navigate = useNavigate();

    useEffect(() => {
        const updateClock = () => {
            const now = new Date();
            setCurrentTime(
                now.toLocaleTimeString("en-US", {
                    hour: "2-digit",
                    minute: "2-digit",
                    hour12: true,
                })
            );
        };
        updateClock();
        const timer = setInterval(updateClock, 1000);
        return () => clearInterval(timer);
    }, []);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsMenuOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const handleLogout = () => {
        setIsMenuOpen(false);
        localStorage.removeItem("accessToken");
        localStorage.removeItem("token");
        sessionStorage.clear();
        navigate("/login", { replace: true });
    };

    return (
        <>
            <header className="flex h-[56px] w-full flex-none items-center justify-between bg-white border-b border-slate-200 px-4 text-slate-800 shadow-xs">
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => {
                            setIsMenuOpen(false);
                            setIsSidebarOpen((prev) => !prev);
                        }}
                        className="rounded-lg p-1 text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition"
                    >
                        <Menu className="h-5 w-5" />
                    </button>

                    <div className="flex items-center gap-2">
                        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-600 text-xs font-black text-white shadow-xs">
                            EM
                        </div>
                        <span className="text-lg font-bold tracking-tight text-slate-900">Employee Management</span>
                    </div>
                </div>

                <div className="flex items-center gap-3">
                    <button className="rounded-full p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-700 transition">
                        <Bell className="h-5 w-5" />
                    </button>

                    {/* Đồng hồ hệ thống */}
                    <div className="flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 py-1.5 px-3 shadow-xs">
                        <Clock className="h-4 w-4 text-slate-400" />
                        <span className="text-xs font-bold tracking-tight text-slate-700 min-w-[65px] text-center">
                            {currentTime || "--:-- --"}
                        </span>
                    </div>

                    {/* Icon & Menu tài khoản cá nhân */}
                    <div className="relative" ref={dropdownRef}>
                        <button
                            onClick={() => setIsMenuOpen((prev) => !prev)}
                            className="flex items-center gap-2 rounded-full border border-slate-200 bg-white p-1 pr-2.5 shadow-xs transition hover:bg-slate-50 hover:border-indigo-300 focus:outline-none focus:ring-2 focus:ring-indigo-100 active:scale-95"
                            title="Tài khoản cá nhân"
                            type="button"
                        >
                            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-indigo-50 text-indigo-600 border border-indigo-100 font-bold shadow-2xs">
                                <User className="h-5 w-5" />
                            </div>
                            <span className="text-xs font-bold text-slate-800 max-w-[120px] truncate hidden sm:inline-block">
                                Chu Văn Hưng
                            </span>
                        </button>

                        {isMenuOpen && (
                            <div className="absolute right-0 top-full mt-2 w-56 rounded-2xl border border-slate-200 bg-white p-1.5 text-slate-700 shadow-xl z-50 animate-in fade-in zoom-in-95 duration-150">
                                <div className="px-3 py-2 border-b border-slate-100 mb-1">
                                    <p className="text-xs font-bold text-slate-900">Chu Văn Hưng</p>
                                    <p className="text-[11px] text-slate-500 truncate">hungwgg01@gmail.com</p>
                                </div>
                                <button
                                    onClick={() => {
                                        setIsMenuOpen(false);
                                        setIsAccountSettingsOpen(true);
                                    }}
                                    className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold hover:bg-slate-100 transition text-slate-700"
                                >
                                    <Settings className="h-4 w-4 text-slate-500" />
                                    Cài đặt tài khoản
                                </button>
                                <button
                                    onClick={handleLogout}
                                    className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold text-rose-600 hover:bg-rose-50 transition"
                                >
                                    <LogOut className="h-4 w-4" />
                                    Đăng xuất
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </header>

            {/* Component Profile Modal từ thư mục profile */}
            <UserProfileModal
                isOpen={isAccountSettingsOpen}
                onClose={() => setIsAccountSettingsOpen(false)}
            />
        </>
    );
}