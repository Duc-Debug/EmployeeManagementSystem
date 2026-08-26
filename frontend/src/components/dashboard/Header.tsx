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
            <header className="flex h-[56px] w-full flex-none items-center justify-between bg-[#4338ca] px-4 text-white shadow-sm">
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => {
                            setIsMenuOpen(false);
                            setIsSidebarOpen((prev) => !prev);
                        }}
                        className="rounded-lg p-1 hover:bg-white/10 transition"
                    >
                        <Menu className="h-5 w-5" />
                    </button>

                    <div className="flex items-center gap-2">
                        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-white text-xs font-black text-[#4338ca]">
                            EM
                        </div>
                        <span className="text-lg font-bold tracking-tight">Employee Management</span>
                    </div>
                </div>

                <div className="flex items-center gap-3">
                    <button className="rounded-full p-1.5 hover:bg-white/10">
                        <Bell className="h-5 w-5 text-white/80" />
                    </button>

                    <div className="relative ml-2" ref={dropdownRef}>
                        <div className="flex items-center gap-2.5 rounded-full border border-white/20 bg-white/10 py-1 pl-3 pr-1 backdrop-blur-sm">
                            <Clock className="h-4 w-4 text-white/80" />
                            <span className="text-sm font-bold tracking-tight text-white min-w-[65px] text-center">
                                {currentTime || "--:-- --"}
                            </span>

                            <button
                                onClick={() => setIsMenuOpen((prev) => !prev)}
                                className="flex h-7 w-7 items-center justify-center rounded-full bg-white/20 text-white hover:bg-white/30 transition focus:outline-none"
                            >
                                <User className="h-4 w-4" />
                            </button>
                        </div>

                        {isMenuOpen && (
                            <div className="absolute right-0 top-full mt-2 w-52 rounded-xl border border-slate-100 bg-white p-1.5 text-slate-700 shadow-xl z-50">
                                <button
                                    onClick={() => {
                                        setIsMenuOpen(false);
                                        setIsAccountSettingsOpen(true);
                                    }}
                                    className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-xs font-semibold hover:bg-slate-100 transition"
                                >
                                    <Settings className="h-4 w-4 text-slate-500" />
                                    Cài đặt tài khoản
                                </button>
                                <button
                                    onClick={handleLogout}
                                    className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-xs font-semibold text-rose-600 hover:bg-rose-50 transition"
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