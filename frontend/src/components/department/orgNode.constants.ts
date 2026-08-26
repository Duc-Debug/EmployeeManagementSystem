import {
    Crown, BarChart2, Archive, Code, Users, Shield,
    User, Briefcase, Building2, Wallet, Megaphone,
    HeartHandshake, type LucideIcon
} from "lucide-react";

export interface CardData {
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

export const ICON_OPTIONS: { key: string; icon: LucideIcon; label: string }[] = [
    { key: "Crown", icon: Crown, label: "Vương miện" },
    { key: "BarChart2", icon: BarChart2, label: "Biểu đồ" },
    { key: "Code", icon: Code, label: "Kỹ thuật" },
    { key: "Archive", icon: Archive, label: "Lưu trữ" },
    { key: "Users", icon: Users, label: "Nhân sự" },
    { key: "Shield", icon: Shield, label: "Vận hành" },
    { key: "User", icon: User, label: "Cá nhân" },
    { key: "Briefcase", icon: Briefcase, label: "Kinh doanh" },
    { key: "Building2", icon: Building2, label: "Tổ chức" },
    { key: "Wallet", icon: Wallet, label: "Tài chính" },
    { key: "Megaphone", icon: Megaphone, label: "Marketing" },
    { key: "HeartHandshake", icon: HeartHandshake, label: "Đối ngoại" },
];

export const THEME_OPTIONS = [
    { key: "amber", label: "Hổ phách", badgeBg: "bg-amber-100", badgeColor: "text-amber-800", borderColor: "border-amber-200", iconColor: "text-amber-600" },
    { key: "blue", label: "Xanh dương", badgeBg: "bg-blue-100", badgeColor: "text-blue-800", borderColor: "border-blue-200", iconColor: "text-blue-600" },
    { key: "emerald", label: "Xanh ngọc", badgeBg: "bg-emerald-100", badgeColor: "text-emerald-800", borderColor: "border-emerald-200", iconColor: "text-emerald-600" },
    { key: "purple", label: "Tím", badgeBg: "bg-purple-100", badgeColor: "text-purple-800", borderColor: "border-purple-200", iconColor: "text-purple-600" },
    { key: "pink", label: "Hồng", badgeBg: "bg-pink-100", badgeColor: "text-pink-800", borderColor: "border-pink-200", iconColor: "text-pink-600" },
    { key: "neutral", label: "Trung tính", badgeBg: "bg-slate-100", badgeColor: "text-slate-800", borderColor: "border-slate-200", iconColor: "text-slate-600" },
];

export function iconKeyFor(icon: LucideIcon): string {
    return ICON_OPTIONS.find((o) => o.icon === icon)?.key ?? "User";
}