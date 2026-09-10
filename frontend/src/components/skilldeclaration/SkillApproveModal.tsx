"use client";

import { useState, useEffect } from "react";
import { X, ShieldCheck, Check, AlertCircle, Sparkles, User, Award } from "lucide-react";
import { cn } from "@/lib/utils";
import type { PendingApprovalSkill } from "./Types";
import { PROFICIENCY_LEVELS } from "./Types";

interface SkillApproveModalProps {
    open: boolean;
    skillItem: PendingApprovalSkill | null;
    onClose: () => void;
    onConfirm: (
        id: number,
        adjustedProficiencyLevel: number,
        reviewNotes: string
    ) => Promise<void>;
}

export default function SkillApproveModal({
    open,
    skillItem,
    onClose,
    onConfirm,
}: SkillApproveModalProps) {
    const [selectedLevel, setSelectedLevel] = useState<number>(3);
    const [reviewNotes, setReviewNotes] = useState<string>("");
    const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (skillItem) {
            setSelectedLevel(skillItem.level);
            setReviewNotes(skillItem.reviewNotes || "");
            setError(null);
        }
    }, [skillItem, open]);

    if (!open || !skillItem) return null;

    const originalLevel = skillItem.level;
    const isLevelChanged = selectedLevel !== originalLevel;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);

        if (reviewNotes.length > 500) {
            setError("Ghi chú đánh giá không được vượt quá 500 ký tự.");
            return;
        }

        try {
            setIsSubmitting(true);
            await onConfirm(skillItem.id, selectedLevel, reviewNotes.trim());
            onClose();
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : "Có lỗi xảy ra khi phê duyệt kỹ năng.";
            setError(message);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-xs animate-in fade-in duration-150">
            <div className="relative w-full max-w-lg rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl text-slate-800 animate-in zoom-in-95 duration-150">
                {/* Close button */}
                <button
                    type="button"
                    onClick={onClose}
                    disabled={isSubmitting}
                    className="absolute right-5 top-5 rounded-xl p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition disabled:opacity-50"
                >
                    <X className="h-5 w-5" />
                </button>

                {/* Header */}
                <div className="flex items-start gap-3.5 border-b border-slate-100 pb-4">
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl border border-emerald-100 bg-emerald-50 text-emerald-600 shadow-2xs">
                        <ShieldCheck className="h-6 w-6" />
                    </div>
                    <div className="min-w-0 pr-6">
                        <h3 className="text-base font-bold text-slate-900 leading-snug">
                            Xác nhận mức thành thạo kỹ năng
                        </h3>
                        <p className="text-xs text-slate-500 mt-0.5">
                            Phê duyệt hoặc điều chỉnh mức độ chuyên môn của nhân sự (NCL-02-CN-006)
                        </p>
                    </div>
                </div>

                {/* Body Form */}
                <form onSubmit={handleSubmit} className="mt-4 space-y-4">
                    {error && (
                        <div className="flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3.5 py-2.5 text-xs font-semibold text-rose-700">
                            <AlertCircle className="h-4 w-4 shrink-0 text-rose-600" />
                            <span>{error}</span>
                        </div>
                    )}

                    {/* Thẻ tóm tắt thông tin nhân sự & kỹ năng */}
                    <div className="rounded-2xl border border-slate-100 bg-slate-50/80 p-4 text-xs space-y-2.5">
                        <div className="flex items-center justify-between">
                            <span className="flex items-center gap-1.5 text-slate-500 font-medium">
                                <User className="h-3.5 w-3.5 text-slate-400" /> Nhân viên:
                            </span>
                            <span className="font-bold text-slate-900">
                                {skillItem.employeeName}{" "}
                                {skillItem.employeeCode && (
                                    <span className="text-slate-400 font-mono font-normal">({skillItem.employeeCode})</span>
                                )}
                            </span>
                        </div>

                        {skillItem.orgUnitName && (
                            <div className="flex items-center justify-between">
                                <span className="text-slate-500 font-medium">Đơn vị / Bộ phận:</span>
                                <span className="font-semibold text-slate-700">{skillItem.orgUnitName}</span>
                            </div>
                        )}

                        <div className="flex items-center justify-between">
                            <span className="flex items-center gap-1.5 text-slate-500 font-medium">
                                <Award className="h-3.5 w-3.5 text-slate-400" /> Kỹ năng khai báo:
                            </span>
                            <span className="font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded-lg border border-indigo-100">
                                {skillItem.skillName} ({skillItem.category})
                            </span>
                        </div>

                        <div className="flex items-center justify-between pt-1 border-t border-slate-200/60">
                            <span className="text-slate-500 font-medium">Mức nhân viên đề xuất:</span>
                            <span className="inline-flex items-center gap-1.5 font-bold text-slate-800">
                                <span className="rounded-md bg-slate-200 px-2 py-0.5 text-[11px] font-bold text-slate-700">
                                    Level {originalLevel}
                                </span>
                                <span>- {PROFICIENCY_LEVELS.find((l) => l.level === originalLevel)?.label || `Level ${originalLevel}`}</span>
                                <span className="text-slate-400 font-normal">({skillItem.years} năm kinh nghiệm)</span>
                            </span>
                        </div>
                    </div>

                    {/* Chọn mức thành thạo xác nhận */}
                    <div className="space-y-1.5">
                        <div className="flex items-center justify-between">
                            <label className="text-xs font-bold text-slate-700">
                                Mức thành thạo phê duyệt *
                            </label>
                            {isLevelChanged && (
                                <span className="inline-flex items-center gap-1 text-[11px] font-bold text-amber-600 bg-amber-50 px-2 py-0.5 rounded-md border border-amber-200">
                                    <Sparkles className="h-3 w-3" />
                                    {selectedLevel > originalLevel ? "Điều chỉnh tăng" : "Điều chỉnh giảm"} so với đề xuất
                                </span>
                            )}
                        </div>

                        <div className="grid grid-cols-5 gap-2">
                            {PROFICIENCY_LEVELS.map((pl) => {
                                const isSelected = selectedLevel === pl.level;
                                const isOriginal = originalLevel === pl.level;
                                return (
                                    <button
                                        key={pl.level}
                                        type="button"
                                        onClick={() => setSelectedLevel(pl.level)}
                                        className={cn(
                                            "flex flex-col items-center justify-center p-2.5 rounded-xl border text-center transition cursor-pointer",
                                            isSelected
                                                ? "border-emerald-500 bg-emerald-50/80 text-emerald-900 ring-2 ring-emerald-400/40 shadow-xs"
                                                : "border-slate-200 bg-white hover:bg-slate-50 text-slate-700",
                                            isOriginal && !isSelected && "border-indigo-200 bg-indigo-50/30"
                                        )}
                                    >
                                        <span className="text-xs font-black">Lvl {pl.level}</span>
                                        <span className="text-[10px] font-semibold mt-0.5 truncate w-full">{pl.label}</span>
                                        {isOriginal && (
                                            <span className="text-[9px] text-indigo-600 font-medium mt-0.5">Gốc</span>
                                        )}
                                    </button>
                                );
                            })}
                        </div>
                    </div>

                    {/* Ghi chú đánh giá */}
                    <div className="space-y-1.5">
                        <div className="flex items-center justify-between">
                            <label className="text-xs font-bold text-slate-700">
                                Ghi chú đánh giá / Nhận xét (Review Notes)
                            </label>
                            <span className={cn("text-[10px] font-mono", reviewNotes.length > 500 ? "text-rose-600 font-bold" : "text-slate-400")}>
                                {reviewNotes.length}/500
                            </span>
                        </div>
                        <textarea
                            rows={3}
                            maxLength={500}
                            value={reviewNotes}
                            onChange={(e) => setReviewNotes(e.target.value)}
                            placeholder="Nhập nhận xét chuyên môn hoặc lý do giữ nguyên / điều chỉnh mức thành thạo..."
                            className="w-full rounded-xl border border-slate-200 bg-slate-50/70 p-3 text-xs text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-emerald-500 focus:bg-white focus:ring-2 focus:ring-emerald-100"
                        />
                    </div>

                    {/* Footer Actions */}
                    <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-slate-100">
                        <button
                            type="button"
                            onClick={onClose}
                            disabled={isSubmitting}
                            className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition cursor-pointer disabled:opacity-50"
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            disabled={isSubmitting}
                            className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-emerald-200 hover:bg-emerald-700 transition active:scale-95 cursor-pointer disabled:opacity-50"
                        >
                            {isSubmitting ? (
                                <span>Đang phê duyệt...</span>
                            ) : (
                                <>
                                    <Check className="h-4 w-4 stroke-[2.5]" />
                                    <span>Xác nhận phê duyệt</span>
                                </>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
