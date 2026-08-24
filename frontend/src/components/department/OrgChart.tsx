import React from "react";
import {
    Crown,
    BarChart2,
    Archive,
    Code,
    Users,
    Shield,
    User,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
interface CardData {
    badge: string;
    badgeBg: string;
    badgeColor: string;
    title: string;
    desc: string;
    subLeft: string;
    levelText: string;
    cardBg: string;
    borderColor: string;
    icon: LucideIcon;
    iconColor: string;
    isDark?: boolean;
}
const BOARD_CARD: CardData = {
    badge: "CẤP CAO NHẤT",
    badgeBg: "bg-indigo-900/80",
    badgeColor: "text-indigo-200",
    title: "Ban Giám Đốc",
    desc: "Quyết định chiến lược & Tầm nhìn",
    subLeft: "Hội đồng Quản trị",
    levelText: "Tầng 1",
    cardBg: "bg-[#111827]",
    borderColor: "border-indigo-500/30",
    icon: Crown,
    iconColor: "text-amber-400",
    isDark: true,
};
const LEFT_BRANCH: CardData[] = [
    {
        badge: "Quản Lý Vận Hành",
        badgeBg: "bg-blue-100/80",
        badgeColor: "text-blue-700",
        title: "Quản Lý Dự Án",
        desc: "Điều phối tiến độ, ngân sách & mục tiêu sản phẩm",
        subLeft: "Báo cáo trực tiếp BGĐ",
        levelText: "Tầng 2",
        cardBg: "bg-[#f0f7ff]",
        borderColor: "border-blue-300",
        icon: BarChart2,
        iconColor: "text-blue-600",
    },
    {
        badge: "Thực Thi Chuyên Môn",
        badgeBg: "bg-emerald-100/80",
        badgeColor: "text-emerald-700",
        title: "Nhân Viên Chuyên Môn",
        desc: "Kỹ sư, Lập trình viên, Designer & Chuyên gia",
        subLeft: "Thuộc Khối Dự Án",
        levelText: "Tầng 3",
        cardBg: "bg-[#f0fdf4]",
        borderColor: "border-emerald-300",
        icon: Code,
        iconColor: "text-emerald-600",
    },
];
const RIGHT_BRANCH: CardData[] = [
    {
        badge: "Quản Lý Nguồn Lực",
        badgeBg: "bg-purple-100/80",
        badgeColor: "text-purple-700",
        title: "Quản Lý Nguồn Lực",
        desc: "Tối ưu hóa nhân lực, tài chính & cơ sở hạ tầng",
        subLeft: "Báo cáo trực tiếp BGĐ",
        levelText: "Tầng 2",
        cardBg: "bg-[#faf5ff]",
        borderColor: "border-purple-300",
        icon: Archive,
        iconColor: "text-purple-600",
    },
    {
        badge: "Phòng Khối Nhân Sự",
        badgeBg: "bg-pink-100/80",
        badgeColor: "text-pink-700",
        title: "Nhân Sự (HR)",
        desc: "Tuyển dụng, đào tạo & chế độ phúc lợi",
        subLeft: "Trực thuộc Nguồn Lực",
        levelText: "Tầng 3",
        cardBg: "bg-[#fff1f2]",
        borderColor: "border-pink-300",
        icon: Users,
        iconColor: "text-pink-600",
    },
    {
        badge: "Vận Hành & Admin",
        badgeBg: "bg-amber-100/80",
        badgeColor: "text-amber-700",
        title: "Quản Trị Viên",
        desc: "Quản lý hệ thống, nội quy & tài sản",
        subLeft: "Giám sát vận hành",
        levelText: "Tầng 4",
        cardBg: "bg-[#fffbe1]",
        borderColor: "border-amber-300",
        icon: Shield,
        iconColor: "text-amber-600",
    },
    {
        badge: "Nhân Viên Chung",
        badgeBg: "bg-slate-200/80",
        badgeColor: "text-slate-700",
        title: "Nhân Viên Công Ty",
        desc: "Thành viên toàn công ty",
        subLeft: "Cấp cơ sở",
        levelText: "Tầng 5",
        cardBg: "bg-[#f1f5f9]",
        borderColor: "border-slate-300",
        icon: User,
        iconColor: "text-slate-500",
    },
];
export default function OrgChart() {
    return (
        <div className="w-full h-full p-8 md:p-12 overflow-x-auto overflow-y-auto flex justify-center items-start font-sans antialiased">
            <div className="relative flex flex-col items-center min-w-[780px] pb-20">

                {/* ---------------- TẦNG 1: BAN GIÁM ĐỐC ---------------- */}
                <div className="relative z-10">
                    <TreeCard card={BOARD_CARD} />
                </div>
                <div className="relative w-full h-[60px]">
                    <svg className="pointer-events-none absolute inset-0 w-full h-full overflow-visible">
                        <line x1="50%" y1="0" x2="50%" y2="30" stroke="#cbd5e1" strokeWidth="2" />
                        <line x1="25%" y1="30" x2="75%" y2="30" stroke="#cbd5e1" strokeWidth="2" />
                        <line x1="25%" y1="30" x2="25%" y2="60" stroke="#cbd5e1" strokeWidth="2" />
                        <line x1="75%" y1="30" x2="75%" y2="60" stroke="#cbd5e1" strokeWidth="2" />
                    </svg>
                </div>
                {/* ---------------- TẦNG 2 TRỞ XUỐNG ---------------- */}
                <div className="grid grid-cols-2 gap-16 w-full max-w-[760px]">
                    <div className="flex flex-col items-center">
                        {LEFT_BRANCH.map((card, idx) => (
                            <React.Fragment key={idx}>
                                <TreeCard card={card} />
                                {idx < LEFT_BRANCH.length - 1 && (
                                    <div className="h-10 w-[2px] bg-slate-300 my-1" />
                                )}
                            </React.Fragment>
                        ))}
                    </div>
                    <div className="flex flex-col items-center">
                        {RIGHT_BRANCH.map((card, idx) => (
                            <React.Fragment key={idx}>
                                <TreeCard card={card} />
                                {idx < RIGHT_BRANCH.length - 1 && (
                                    <div className="h-10 w-[2px] bg-slate-300 my-1" />
                                )}
                            </React.Fragment>
                        ))}
                    </div>
                </div>

            </div>
        </div>
    );
}
function TreeCard({ card }: { card: CardData }) {
    const Icon = card.icon;
    if (card.isDark) {
        return (
            <div className={`w-[320px] rounded-3xl ${card.cardBg} border ${card.borderColor} p-5 text-white shadow-xl relative transition-all duration-300 hover:shadow-2xl`}>
                <div className="flex items-center justify-between mb-3">
                    <span className={`rounded-full px-3 py-1 text-[10px] font-bold tracking-wider uppercase ${card.badgeBg} ${card.badgeColor}`}>
                        {card.badge}
                    </span>
                    <Icon className={`h-5 w-5 ${card.iconColor}`} />
                </div>
                <h3 className="text-lg font-bold tracking-tight text-white mb-1">{card.title}</h3>
                <p className="text-xs text-slate-400 leading-relaxed font-medium mb-5">{card.desc}</p>
                <div className="border-t border-slate-800 pt-3 flex items-center justify-between text-xs font-semibold">
                    <span className="text-slate-400 text-[11px]">{card.subLeft}</span>
                    <span className="text-indigo-300 font-bold">{card.levelText}</span>
                </div>
            </div>
        );
    }
    return (
        <div className={`w-[320px] rounded-3xl ${card.cardBg} border-2 ${card.borderColor} p-5 text-slate-900 shadow-sm relative transition-all duration-300 hover:shadow-md hover:-translate-y-0.5`}>
            <div className="flex items-center justify-between mb-3">
                <span className={`rounded-full px-3.5 py-1 text-[11px] font-bold tracking-tight ${card.badgeBg} ${card.badgeColor}`}>
                    {card.badge}
                </span>
                <Icon className={`h-5 w-5 ${card.iconColor}`} />
            </div>
            <h3 className="text-lg font-extrabold tracking-tight text-slate-900 mb-1">{card.title}</h3>
            <p className="text-xs text-slate-500 leading-relaxed font-medium mb-5 min-h-[32px]">{card.desc}</p>
            <div className="border-t border-slate-200/60 pt-3 flex items-center justify-between text-xs font-semibold">
                <span className={`text-[11px] ${card.badgeColor}`}>{card.subLeft}</span>
                <span className="text-slate-500 font-bold">{card.levelText}</span>
            </div>
        </div>
    );
}