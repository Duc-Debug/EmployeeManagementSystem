import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Menu, HelpCircle, Bell, Settings, ChevronDown, Clock, User, LogOut } from "lucide-react";
import { cn } from "@/lib/utils";
const TOP_NAV = [
    "Bản tin",
    "Trình nhắn tin",
    "Lịch",
    "Tài liệu",
    "Tác vụ của tôi",
    "Drive",
    "Webmail",
    "Nhóm làm việc",
];
interface HeaderProps {
    setIsSidebarOpen: React.Dispatch<React.SetStateAction<boolean>>;
}
export default function Header({ setIsSidebarOpen }: HeaderProps) {
    const [currentTime, setCurrentTime] = useState<string>("");
    const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false);
    const dropdownRef = useRef<HTMLDivElement>(null);
    const navigate = useNavigate();
    useEffect(() => {
        const updateClock = () => {
            const now = new Date();
            const timeString = now.toLocaleTimeString("en-US", {
                hour: "2-digit",
                minute: "2-digit",
                hour12: true,
            });
            setCurrentTime(timeString);
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
    const handleToggleSidebar = () => {
        setIsMenuOpen(false);
        setIsSidebarOpen((prev) => !prev);
    };
    const handleLogout = () => {
        setIsMenuOpen(false);
        localStorage.removeItem("accessToken");
        localStorage.removeItem("token");
        sessionStorage.clear();
        navigate("/login", { replace: true });
    };
    return (
        <header className="flex h-[56px] w-full flex-none items-center justify-between bg-[#4338ca] px-4 text-white shadow-sm">
            <div className="flex items-center gap-6">
                <button
                    onClick={handleToggleSidebar}
                    className="rounded-lg p-1 hover:bg-white/10 transition"
                    title="Bật/Tắt Menu"
                >
                    <Menu className="h-5 w-5" />
                </button>

                <div className="flex items-center gap-2">
                    <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-white text-xs font-black text-[#4338ca]">
                        EM
                    </div>
                    <span className="text-lg font-bold tracking-tight">Employee Management</span>
                </div>
                <nav className="ml-4 hidden items-center gap-1 text-sm font-medium xl:flex">
                    {TOP_NAV.map((item, idx) => (
                        <button
                            key={idx}
                            className={cn(
                                "rounded-md px-3 py-1.5 transition hover:bg-white/10",
                                idx === 0 ? "text-white" : "text-white/80"
                            )}
                        >
                            {item}
                        </button>
                    ))}
                    <button className="flex items-center gap-1 rounded-md px-3 py-1.5 text-white/80 transition hover:bg-white/10">
                        Thêm <ChevronDown className="h-3.5 w-3.5" />
                    </button>
                </nav>
            </div>
            <div className="flex items-center gap-3">
                <button className="rounded-full p-1.5 hover:bg-white/10">
                    <HelpCircle className="h-5 w-5 text-white/80" />
                </button>
                <button className="rounded-full p-1.5 hover:bg-white/10">
                    <Bell className="h-5 w-5 text-white/80" />
                </button>
                {/* Widget Thời gian + Avatar */}
                <div className="relative ml-2" ref={dropdownRef}>
                    <div className="flex items-center gap-2.5 rounded-full border border-white/20 bg-white/10 py-1 pl-3 pr-1 backdrop-blur-sm">
                        <Clock className="h-4 w-4 text-white/80" />
                        <span className="text-sm font-bold tracking-tight text-white min-w-[65px] text-center">
                            {currentTime || "--:-- --"}
                        </span>

                        <button
                            onClick={() => setIsMenuOpen((prev) => !prev)}
                            className="flex h-7 w-7 items-center justify-center rounded-full bg-white/20 text-white hover:bg-white/30 transition focus:outline-none"
                            title="Tài khoản & Cài đặt"
                        >
                            <User className="h-4 w-4" />
                        </button>
                    </div>
                    {/* Dropdown Menu */}
                    {isMenuOpen && (
                        <div className="absolute right-0 top-full mt-2 w-48 rounded-xl border border-slate-100 bg-white p-1.5 text-slate-700 shadow-lg z-50">
                            <button
                                onClick={() => setIsMenuOpen(false)}
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
    );
}