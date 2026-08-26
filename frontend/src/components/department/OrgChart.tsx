import React, { useState } from "react";
import { Plus, Pencil, Trash2, GitBranch } from "lucide-react";
import OrgNodeModal from "./OrgNodeModal";
import type { CardData } from "./orgNode.constants";
import {
    Crown,
    BarChart2,
    Archive,
    Code,
    Users,
    Shield,
} from "lucide-react";

const BOARD_CARD: CardData = {
    badge: "CẤP CAO NHẤT",
    badgeBg: "bg-amber-100",
    badgeColor: "text-amber-800",
    title: "Ban Giám Đốc",
    desc: "Quyết định chiến lược & Tầm nhìn",
    subLeft: "Hội đồng Quản trị",
    levelText: "Tầng 1",
    cardBg: "bg-black/[100]",
    borderColor: "border-amber-200",
    icon: Crown,
    iconColor: "text-amber-600",
    isDark: true,
};

const INITIAL_BOARD_BRANCHES: CardData[][] = [
    [
        {
            badge: "Quản Lý Vận Hành",
            badgeBg: "bg-blue-400/20",
            badgeColor: "text-blue-200",
            title: "Quản Lý Dự Án",
            desc: "Điều phối tiến độ & mục tiêu sản phẩm",
            subLeft: "Báo cáo trực tiếp BGĐ",
            levelText: "Tầng 2",
            cardBg: "bg-white/[0.07]",
            borderColor: "border-blue-300/30",
            icon: BarChart2,
            iconColor: "text-blue-300",
        },
        {
            badge: "Thực Thi Chuyên Môn",
            badgeBg: "bg-emerald-400/20",
            badgeColor: "text-emerald-200",
            title: "Nhân Viên Chuyên Môn",
            desc: "Kỹ sư, Lập trình viên, Designer & Chuyên gia",
            subLeft: "Thuộc Khối Dự Án",
            levelText: "Tầng 3",
            cardBg: "bg-white/[0.07]",
            borderColor: "border-emerald-300/30",
            icon: Code,
            iconColor: "text-emerald-300",
        },
    ],
    [
        {
            badge: "Quản Lý Nguồn Lực",
            badgeBg: "bg-purple-400/20",
            badgeColor: "text-purple-200",
            title: "Quản Lý Nguồn Lực",
            desc: "Tối ưu hóa nhân lực & cơ sở hạ tầng",
            subLeft: "Báo cáo trực tiếp BGĐ",
            levelText: "Tầng 2",
            cardBg: "bg-white/[0.07]",
            borderColor: "border-purple-300/30",
            icon: Archive,
            iconColor: "text-purple-300",
        },
        {
            badge: "Phòng Khối Nhân Sự",
            badgeBg: "bg-pink-400/20",
            badgeColor: "text-pink-200",
            title: "Nhân Sự (HR)",
            desc: "Tuyển dụng, đào tạo & chế độ phúc lợi",
            subLeft: "Trực thuộc Nguồn Lực",
            levelText: "Tầng 3",
            cardBg: "bg-white/[0.07]",
            borderColor: "border-pink-300/30",
            icon: Users,
            iconColor: "text-pink-300",
        },
        {
            badge: "Vận Hành & Admin",
            badgeBg: "bg-amber-400/20",
            badgeColor: "text-amber-200",
            title: "Quản Trị Viên",
            desc: "Quản lý hệ thống, nội quy & tài sản",
            subLeft: "Giám sát vận hành",
            levelText: "Tầng 4",
            cardBg: "bg-white/[0.07]",
            borderColor: "border-amber-300/30",
            icon: Shield,
            iconColor: "text-amber-300",
        },
    ],
];

const EXTRA_BRANCH_PALETTE = [
    { cardBg: "bg-indigo-500/[0.10]", border: "border-indigo-300/30", accent: "border-indigo-300/40 bg-indigo-400/10 text-indigo-200" },
    { cardBg: "bg-teal-500/[0.10]", border: "border-teal-300/30", accent: "border-teal-300/40 bg-teal-400/10 text-teal-200" },
    { cardBg: "bg-rose-500/[0.10]", border: "border-rose-300/30", accent: "border-rose-300/40 bg-rose-400/10 text-rose-200" },
    { cardBg: "bg-orange-500/[0.10]", border: "border-orange-300/30", accent: "border-orange-300/40 bg-orange-400/10 text-orange-200" },
    { cardBg: "bg-cyan-500/[0.10]", border: "border-cyan-300/30", accent: "border-cyan-300/40 bg-cyan-400/10 text-cyan-200" },
];

interface ExtraBranch {
    id: string;
    paletteIndex: number;
    nodes: CardData[];
}

let extraBranchIdCounter = 0;
function nextExtraBranchId() {
    extraBranchIdCounter += 1;
    return `branch-${extraBranchIdCounter}`;
}

type EditTarget =
    | { kind: "board" }
    | { kind: "boardBranchNode"; branchIndex: number; index: number }
    | { kind: "addBoardBranchNode"; branchIndex: number }
    | { kind: "addFirstBoardBranchNode" }
    | { kind: "extraNode"; branchId: string; index: number }
    | { kind: "extraAdd"; branchId: string }
    | { kind: "extraFirst" };

export default function OrgChart() {
    const [boardCard, setBoardCard] = useState<CardData>(BOARD_CARD);
    const [boardBranches, setBoardBranches] = useState<CardData[][]>(INITIAL_BOARD_BRANCHES);
    const [extraBranches, setExtraBranches] = useState<ExtraBranch[]>([]);
    const [editTarget, setEditTarget] = useState<EditTarget | null>(null);

    function levelTextFor(index: number) {
        return `Tầng ${index + 2}`;
    }

    function extraBranchOf(branchId: string) {
        return extraBranches.find((b) => b.id === branchId);
    }

    function withPalette(card: CardData, paletteIndex: number): CardData {
        const p = EXTRA_BRANCH_PALETTE[paletteIndex % EXTRA_BRANCH_PALETTE.length];
        return { ...card, cardBg: p.cardBg, borderColor: p.border };
    }

    function currentCardFor(target: EditTarget): CardData | null {
        if (target.kind === "board") return boardCard;
        if (
            target.kind === "addBoardBranchNode" ||
            target.kind === "addFirstBoardBranchNode" ||
            target.kind === "extraAdd" ||
            target.kind === "extraFirst"
        )
            return null;
        if (target.kind === "boardBranchNode") return boardBranches[target.branchIndex]?.[target.index] ?? null;
        return extraBranchOf(target.branchId)?.nodes[target.index] ?? null;
    }

    function handleSave(card: CardData) {
        if (!editTarget) return;

        if (editTarget.kind === "board") {
            setBoardCard(card);
        } else if (editTarget.kind === "addFirstBoardBranchNode") {
            setBoardBranches((prev) => [...prev, [card]]);
        } else if (editTarget.kind === "addBoardBranchNode") {
            setBoardBranches((prev) =>
                prev.map((b, i) => (i === editTarget.branchIndex ? [...b, card] : b))
            );
        } else if (editTarget.kind === "boardBranchNode") {
            setBoardBranches((prev) =>
                prev.map((b, i) =>
                    i === editTarget.branchIndex
                        ? b.map((n, idx) => (idx === editTarget.index ? card : n))
                        : b
                )
            );
        } else if (editTarget.kind === "extraFirst") {
            const paletteIndex = extraBranches.length;
            const newBranch: ExtraBranch = {
                id: nextExtraBranchId(),
                paletteIndex,
                nodes: [withPalette({ ...card, levelText: "Cấp 1" }, paletteIndex)],
            };
            setExtraBranches([...extraBranches, newBranch]);
        } else if (editTarget.kind === "extraAdd") {
            setExtraBranches((prev) =>
                prev.map((b) =>
                    b.id === editTarget.branchId
                        ? { ...b, nodes: [...b.nodes, withPalette({ ...card, levelText: `Cấp ${b.nodes.length + 1}` }, b.paletteIndex)] }
                        : b
                )
            );
        } else if (editTarget.kind === "extraNode") {
            setExtraBranches((prev) =>
                prev.map((b) =>
                    b.id === editTarget.branchId
                        ? {
                            ...b,
                            nodes: b.nodes.map((n, i) => (i === editTarget.index ? withPalette({ ...card, levelText: n.levelText }, b.paletteIndex) : n)),
                        }
                        : b
                )
            );
        }
        setEditTarget(null);
    }

    function handleDelete() {
        if (!editTarget) return;

        if (editTarget.kind === "boardBranchNode") {
            setBoardBranches((prev) =>
                prev
                    .map((b, i) =>
                        i === editTarget.branchIndex
                            ? b.filter((_, idx) => idx !== editTarget.index).map((c, idx) => ({ ...c, levelText: levelTextFor(idx) }))
                            : b
                    )
                    .filter((b) => b.length > 0) // Xóa nhánh nếu không còn nút nào
            );
        } else if (editTarget.kind === "extraNode") {
            setExtraBranches((prev) =>
                prev
                    .map((b) =>
                        b.id === editTarget.branchId
                            ? { ...b, nodes: b.nodes.filter((_, i) => i !== editTarget.index).map((n, i) => ({ ...n, levelText: `Cấp ${i + 1}` })) }
                            : b
                    )
                    .filter((b) => b.nodes.length > 0)
            );
        }
        setEditTarget(null);
    }

    function deleteBoardBranch(branchIndex: number) {
        setBoardBranches((prev) => prev.filter((_, idx) => idx !== branchIndex));
    }

    function deleteExtraBranch(branchId: string) {
        setExtraBranches((prev) => prev.filter((b) => b.id !== branchId));
    }

    const modalLevelText =
        editTarget?.kind === "board"
            ? "Tầng 1"
            : editTarget?.kind === "addFirstBoardBranchNode"
                ? "Tầng 2"
                : editTarget?.kind === "addBoardBranchNode"
                    ? levelTextFor(boardBranches[editTarget.branchIndex]?.length ?? 0)
                    : editTarget?.kind === "boardBranchNode"
                        ? levelTextFor(editTarget.index)
                        : editTarget?.kind === "extraFirst"
                            ? "Cấp 1"
                            : editTarget?.kind === "extraAdd"
                                ? `Cấp ${(extraBranchOf(editTarget.branchId)?.nodes.length ?? 0) + 1}`
                                : editTarget?.kind === "extraNode"
                                    ? extraBranchOf(editTarget.branchId)?.nodes[editTarget.index]?.levelText ?? ""
                                    : "";

    return (
        <div className="w-full h-full p-8 md:p-12 overflow-x-auto overflow-y-auto flex flex-col items-center font-sans antialiased">
            <div className="relative flex flex-col items-center min-w-[780px]">
                <div className="relative z-10 flex flex-col items-center">
                    <TreeCard card={boardCard} onEdit={() => setEditTarget({ kind: "board" })} />
                    <button
                        onClick={() => setEditTarget({ kind: "addFirstBoardBranchNode" })}
                        className="mt-3 flex items-center gap-1.5 rounded-full border border-dashed border-amber-300/40 bg-amber-400/10 px-3 py-1 text-[11px] font-semibold text-amber-200 transition hover:bg-amber-400/20"
                    >
                        <Plus className="h-3.5 w-3.5" /> Thêm nhánh thuộc Ban Giám Đốc
                    </button>
                </div>
                {boardBranches.length > 0 && (
                    <div className="relative w-full h-[50px]">
                        <svg className="pointer-events-none absolute inset-0 w-full h-full overflow-visible">
                            <line x1="50%" y1="0" x2="50%" y2="25" stroke="rgba(255,255,255,0.25)" strokeWidth="2" />
                            {boardBranches.length > 1 && (
                                <line
                                    x1={`${(1 / (boardBranches.length * 2)) * 100}%`}
                                    y1="25"
                                    x2={`${(1 - 1 / (boardBranches.length * 2)) * 100}%`}
                                    y2="25"
                                    stroke="rgba(255,255,255,0.25)"
                                    strokeWidth="2"
                                />
                            )}
                            {boardBranches.map((_, idx) => {
                                const xPos = `${((2 * idx + 1) / (boardBranches.length * 2)) * 100}%`;
                                return (
                                    <line
                                        key={idx}
                                        x1={xPos}
                                        y1="25"
                                        x2={xPos}
                                        y2="50"
                                        stroke="rgba(255,255,255,0.25)"
                                        strokeWidth="2"
                                    />
                                );
                            })}
                        </svg>
                    </div>
                )}

                <div
                    className="grid gap-12 w-full pb-4"
                    style={{ gridTemplateColumns: `repeat(${Math.max(boardBranches.length, 1)}, minmax(0, 1fr))` }}
                >
                    {boardBranches.map((branch, bIdx) => (
                        <div key={bIdx} className="flex flex-col items-center relative group">
                            <div className="w-[320px] flex justify-end mb-1">
                                <button
                                    onClick={() => deleteBoardBranch(bIdx)}
                                    className="rounded p-1 text-white/30 transition hover:bg-rose-500/20 hover:text-rose-300"
                                    title="Xóa nhánh này"
                                >
                                    <Trash2 className="h-3.5 w-3.5" />
                                </button>
                            </div>

                            {branch.map((card, idx) => (
                                <React.Fragment key={idx}>
                                    <TreeCard
                                        card={card}
                                        onEdit={() => setEditTarget({ kind: "boardBranchNode", branchIndex: bIdx, index: idx })}
                                    />
                                    {idx < branch.length - 1 && <div className="h-8 w-[2px] bg-white/20 my-1" />}
                                </React.Fragment>
                            ))}
                            <button
                                onClick={() => setEditTarget({ kind: "addBoardBranchNode", branchIndex: bIdx })}
                                className="mt-4 flex w-[320px] items-center justify-center gap-2 rounded-2xl border border-dashed border-white/25 py-3 text-xs font-semibold text-white/50 transition hover:border-[#63ecc8]/50 hover:text-[#63ecc8]"
                            >
                                <Plus className="h-4 w-4" /> Thêm nút vào nhánh này
                            </button>
                        </div>
                    ))}
                </div>
            </div>
            {extraBranches.length > 0 && (
                <div className="mt-6 w-full min-w-[780px] border-t border-dashed border-white/15 pt-8">
                    <p className="mb-5 text-center text-[11px] font-semibold uppercase tracking-wider text-white/35">
                        Nhánh độc lập — không trực thuộc Ban Giám Đốc
                    </p>
                    <div className="flex flex-wrap justify-center gap-8">
                        {extraBranches.map((branch) => {
                            const palette = EXTRA_BRANCH_PALETTE[branch.paletteIndex % EXTRA_BRANCH_PALETTE.length];
                            return (
                                <div
                                    key={branch.id}
                                    className={`flex flex-col items-center rounded-3xl border border-dashed ${palette.border} bg-white/[0.02] p-5`}
                                >
                                    <div className="mb-4 flex w-[320px] items-center justify-between">
                                        <span className={`flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[10px] font-bold ${palette.accent}`}>
                                            <GitBranch className="h-3 w-3" /> Nhánh riêng
                                        </span>
                                        <button
                                            onClick={() => deleteExtraBranch(branch.id)}
                                            className="rounded-lg p-1.5 text-white/40 transition hover:bg-rose-500/15 hover:text-rose-300"
                                            title="Xóa nhánh"
                                        >
                                            <Trash2 className="h-3.5 w-3.5" />
                                        </button>
                                    </div>
                                    {branch.nodes.map((card, idx) => (
                                        <React.Fragment key={idx}>
                                            <TreeCard card={card} onEdit={() => setEditTarget({ kind: "extraNode", branchId: branch.id, index: idx })} />
                                            {idx < branch.nodes.length - 1 && <div className="h-10 w-[2px] bg-white/20 my-1" />}
                                        </React.Fragment>
                                    ))}
                                    <button
                                        onClick={() => setEditTarget({ kind: "extraAdd", branchId: branch.id })}
                                        className="mt-4 flex w-[320px] items-center justify-center gap-2 rounded-2xl border border-dashed border-white/25 py-3 text-xs font-semibold text-white/50 transition hover:border-[#63ecc8]/50 hover:text-[#63ecc8]"
                                    >
                                        <Plus className="h-4 w-4" /> Thêm nút vào nhánh này
                                    </button>
                                </div>
                            );
                        })}
                    </div>
                </div>
            )}

            <button
                onClick={() => setEditTarget({ kind: "extraFirst" })}
                className="mb-4 mt-8 flex items-center gap-2 rounded-xl border border-dashed border-white/25 px-5 py-2.5 text-xs font-semibold text-white/60 transition hover:border-[#63ecc8]/50 hover:text-[#63ecc8]"
            >
                <GitBranch className="h-4 w-4" /> Thêm nhánh mới (độc lập với Ban Giám Đốc)
            </button>

            <OrgNodeModal
                open={editTarget !== null}
                initialData={editTarget ? currentCardFor(editTarget) : null}
                levelText={modalLevelText}
                onClose={() => setEditTarget(null)}
                onSave={handleSave}
                onDelete={
                    editTarget && (editTarget.kind === "boardBranchNode" || editTarget.kind === "extraNode")
                        ? handleDelete
                        : undefined
                }
            />
        </div>
    );
}
const HUE_TEXT_SHADES: Record<string, { title: string; desc: string; subLeft: string; level: string }> = {
    blue: { title: "text-blue-200", desc: "text-blue-300/70", subLeft: "text-blue-300", level: "text-blue-200" },
    emerald: { title: "text-emerald-200", desc: "text-emerald-300/70", subLeft: "text-emerald-300", level: "text-emerald-200" },
    purple: { title: "text-purple-200", desc: "text-purple-300/70", subLeft: "text-purple-300", level: "text-purple-200" },
    pink: { title: "text-pink-200", desc: "text-pink-300/70", subLeft: "text-pink-300", level: "text-pink-200" },
    amber: { title: "text-amber-200", desc: "text-amber-300/70", subLeft: "text-amber-300", level: "text-amber-200" },
    indigo: { title: "text-indigo-200", desc: "text-indigo-300/70", subLeft: "text-indigo-300", level: "text-indigo-200" },
    teal: { title: "text-teal-200", desc: "text-teal-300/70", subLeft: "text-teal-300", level: "text-teal-200" },
    rose: { title: "text-rose-200", desc: "text-rose-300/70", subLeft: "text-rose-300", level: "text-rose-200" },
    orange: { title: "text-orange-200", desc: "text-orange-300/70", subLeft: "text-orange-300", level: "text-orange-200" },
    cyan: { title: "text-cyan-200", desc: "text-cyan-300/70", subLeft: "text-cyan-300", level: "text-cyan-200" },
};
const DEFAULT_TEXT_SHADES = { title: "text-slate-200", desc: "text-slate-300/70", subLeft: "text-slate-300", level: "text-slate-200" };

function hueFromClass(cls?: string): string | null {
    if (!cls) return null;
    const m = cls.match(/text-([a-z]+)-\d+/);
    return m ? m[1] : null;
}

function textShadesFor(card: CardData) {
    const hue = hueFromClass(card.iconColor) ?? hueFromClass(card.badgeColor);
    return (hue && HUE_TEXT_SHADES[hue]) || DEFAULT_TEXT_SHADES;
}

function TreeCard({ card, onEdit }: { card: CardData; onEdit: () => void }) {
    const Icon = card.icon;
    const shades = textShadesFor(card);
    const base = card.isDark
        ? `w-[320px] rounded-3xl ${card.cardBg} border ${card.borderColor} p-5 shadow-xl backdrop-blur-xl relative transition-all duration-300 hover:shadow-2xl hover:bg-white/[0.12] group`
        : `w-[320px] rounded-3xl ${card.cardBg} border ${card.borderColor} p-5 shadow-sm backdrop-blur-xl relative transition-all duration-300 hover:shadow-md hover:-translate-y-0.5 group`;

    return (
        <div className={base}>
            <button
                onClick={onEdit}
                className="absolute right-3 top-3 z-10 rounded-lg border border-white/20 bg-white/10 p-1.5 text-white/70 opacity-0 backdrop-blur-md transition hover:bg-white/25 hover:text-white group-hover:opacity-100"
                title="Sửa nút"
            >
                <Pencil className="h-3.5 w-3.5" />
            </button>
            <div className="flex items-center justify-between mb-3 pr-8">
                <span
                    className={`rounded-full px-3 py-1 text-[10px] font-bold tracking-wider uppercase ${card.badgeBg} ${card.badgeColor}`}
                >
                    {card.badge}
                </span>
                <Icon className={`h-5 w-5 ${card.iconColor} shrink-0`} />
            </div>
            <h3 className={`${card.isDark ? "text-lg font-bold" : "text-lg font-extrabold"} tracking-tight ${shades.title} mb-1`}>
                {card.title}
            </h3>
            <p className={`text-xs ${shades.desc} leading-relaxed font-medium mb-5 min-h-[32px]`}>{card.desc}</p>
            <div className="border-t border-white/15 pt-3 flex items-center justify-between text-xs font-semibold">
                <span className={`text-[11px] font-semibold ${shades.subLeft}`}>
                    {card.subLeft}
                </span>
                <span className={`font-bold ${shades.level}`}>
                    {card.levelText}
                </span>
            </div>
        </div>
    );
}