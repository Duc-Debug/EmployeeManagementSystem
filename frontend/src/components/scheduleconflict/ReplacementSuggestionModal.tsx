import { useState, useEffect, useCallback } from "react";
import {
    X,
    UserCheck,
    AlertTriangle,
    ShieldAlert,
    Clock,
    Award,
    CheckCircle2,
    RefreshCw,
    Briefcase,
    MessageSquareText,
    Filter,
} from "lucide-react";
import {
    getReplacementSuggestions,
    confirmReplacementProposal,
} from "@/lib/api/schedule-conflict";
import type {
    ScheduleConflict,
    ReplacementCandidate,
    ReplacementSuggestionResult,
} from "@/lib/api/schedule-conflict";
import { getSkills } from "@/lib/api/skills";
import type { SkillResponse } from "@/lib/api/skills";

interface ReplacementSuggestionModalProps {
    conflict: ScheduleConflict;
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

export default function ReplacementSuggestionModal({
    conflict,
    isOpen,
    onClose,
    onSuccess,
}: ReplacementSuggestionModalProps) {
    const [data, setData] = useState<ReplacementSuggestionResult | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [errorMsg, setErrorMsg] = useState<string | null>(null);
    const [isForbidden, setIsForbidden] = useState<boolean>(false);
    const [selectedEmpId, setSelectedEmpId] = useState<number | null>(null);
    const [notes, setNotes] = useState<string>("");
    const [submitting, setSubmitting] = useState<boolean>(false);
    const [successMsg, setSuccessMsg] = useState<string | null>(null);

    // Skill filter state
    const [skillsList, setSkillsList] = useState<SkillResponse[]>([]);
    const [filterSkillId, setFilterSkillId] = useState<number | undefined>(undefined);

    // Load available skill catalog for filtering
    useEffect(() => {
        if (isOpen) {
            getSkills().then((res) => {
                if (Array.isArray(res)) {
                    setSkillsList(res);
                }
            }).catch((err) => {
                console.warn("Không thể tải danh mục kỹ năng:", err);
            });
        }
    }, [isOpen]);

    const loadSuggestions = useCallback(async () => {
        if (!isOpen || !conflict) return;
        setLoading(true);
        setErrorMsg(null);
        setIsForbidden(false);
        setSuccessMsg(null);
        try {
            const res = await getReplacementSuggestions(conflict.id, filterSkillId);
            setData(res);
            if (res.candidates && res.candidates.length > 0) {
                setSelectedEmpId(res.candidates[0].employeeId);
            } else {
                setSelectedEmpId(null);
            }
        } catch (err: any) {
            console.error("Lỗi khi tải gợi ý nhân sự thay thế:", err);
            const msg = err.message || "Không thể tải danh sách gợi ý nhân sự thay thế.";
            if (msg.toLowerCase().includes("quyền") || msg.toLowerCase().includes("forbidden") || err.status === 403) {
                setIsForbidden(true);
            }
            setErrorMsg(msg);
        } finally {
            setLoading(false);
        }
    }, [isOpen, conflict, filterSkillId]);

    useEffect(() => {
        loadSuggestions();
    }, [loadSuggestions]);

    if (!isOpen) return null;

    const handleConfirm = async () => {
        if (!selectedEmpId || !data) return;
        setSubmitting(true);
        setErrorMsg(null);
        try {
            const selectedCand = data.candidates.find((c) => c.employeeId === selectedEmpId);
            await confirmReplacementProposal(conflict.id, {
                conflictId: conflict.id,
                replacementEmployeeId: selectedEmpId,
                skillId: data.skillId,
                proficiencyLevel: selectedCand?.proficiencyLevel,
                notes: notes.trim() || `Xác nhận đề xuất thay thế cho NV ${conflict.employeeName}`,
            });

            setSuccessMsg(`Đã xác nhận đề xuất nhân sự thay thế ${selectedCand?.fullName} cho xung đột lịch thành công!`);
            setTimeout(() => {
                onSuccess();
                onClose();
            }, 1500);
        } catch (err: any) {
            console.error("Lỗi khi xác nhận đề xuất nhân sự thay thế:", err);
            setErrorMsg(err.message || "Không thể gửi xác nhận đề xuất nhân sự thay thế.");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 animate-in fade-in duration-200">
            <div className="relative w-full max-w-3xl rounded-3xl bg-white p-6 shadow-2xl border border-slate-100 flex flex-col max-h-[90vh] overflow-hidden">
                {/* Modal Header */}
                <div className="flex items-start justify-between border-b border-slate-100 pb-4">
                    <div>
                        <h2 className="text-xl font-extrabold text-slate-900">Gợi ý Người Thay thế cho Xung đột</h2>
                        <p className="text-xs text-slate-500 mt-0.5">
                            Tự động tìm kiếm nhân sự có cùng chuyên môn ở trình độ tương đương và còn đủ giờ rảnh trong tuần.
                        </p>
                    </div>
                    <button
                        onClick={onClose}
                        className="rounded-xl p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                {/* Conflict Details Summary Banner */}
                <div className="my-4 rounded-2xl bg-indigo-50/70 p-4 border border-indigo-100 flex flex-wrap items-center justify-between gap-3 text-xs">
                    <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-indigo-600 text-white font-bold">
                            <Briefcase className="h-5 w-5" />
                        </div>
                        <div>
                            <p className="font-bold text-slate-900">
                                {conflict.employeeName} <span className="text-slate-400 font-normal">({conflict.employeeCode})</span>
                            </p>
                            <p className="text-slate-500">{conflict.departmentName} • {conflict.weekLabel}</p>
                        </div>
                    </div>
                    <div className="flex items-center gap-4">
                        {/* Skill Selector Filter */}
                        {skillsList.length > 0 && (
                            <div className="flex items-center gap-1.5 bg-white px-3 py-1.5 rounded-xl border border-indigo-200 shadow-2xs">
                                <Filter className="h-3.5 w-3.5 text-indigo-600 shrink-0" />
                                <span className="text-[11px] font-semibold text-slate-600 shrink-0">Kỹ năng:</span>
                                <select
                                    value={filterSkillId || ""}
                                    onChange={(e) => setFilterSkillId(e.target.value ? Number(e.target.value) : undefined)}
                                    className="bg-transparent text-xs font-bold text-indigo-700 focus:outline-none cursor-pointer"
                                >
                                    <option value="">Tự động chọn kỹ năng</option>
                                    {skillsList.map((s) => (
                                        <option key={s.id} value={s.id}>
                                            {s.name}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        )}

                        <div className="text-right">
                            <p className="text-[11px] text-slate-500 font-medium">Giờ quá tải/vượt:</p>
                            <span className="font-extrabold text-rose-600 text-sm">+{conflict.excessHours}h</span>
                        </div>
                    </div>
                </div>

                {/* Body Content */}
                <div className="flex-1 min-h-0 overflow-y-auto pr-1 space-y-4">
                    {/* TC-03: No Permission Display */}
                    {isForbidden && (
                        <div className="flex flex-col items-center justify-center p-8 bg-rose-50 rounded-2xl border border-rose-200 text-center">
                            <ShieldAlert className="h-10 w-10 text-rose-600 mb-3" />
                            <h4 className="text-sm font-bold text-rose-900">Không có quyền thực thi</h4>
                            <p className="text-xs text-rose-700 mt-1 max-w-md">
                                {errorMsg || "Hệ thống từ chối truy cập. Tài khoản của bạn không thuộc vai trò Quản lý nguồn lực (VT-03). Thao tác này đã được ghi nhận trong nhật ký bảo mật."}
                            </p>
                        </div>
                    )}

                    {/* Generic Error Banner */}
                    {errorMsg && !isForbidden && (
                        <div className="flex items-center gap-2 rounded-xl bg-rose-50 p-3 text-xs text-rose-700 border border-rose-200">
                            <ShieldAlert className="h-4 w-4 shrink-0 text-rose-600" />
                            <span>{errorMsg}</span>
                        </div>
                    )}

                    {/* Success Notification Alert */}
                    {successMsg && (
                        <div className="flex items-center gap-2 rounded-xl bg-emerald-50 p-3 text-xs font-semibold text-emerald-800 border border-emerald-200">
                            <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
                            <span>{successMsg}</span>
                        </div>
                    )}

                    {/* Loading State */}
                    {loading && (
                        <div className="flex flex-col items-center justify-center py-12 text-slate-500">
                            <RefreshCw className="h-8 w-8 text-indigo-600 animate-spin mb-3" />
                            <p className="text-xs font-medium">Đang tự động tìm kiếm người thay thế phù hợp...</p>
                        </div>
                    )}

                    {/* TC-02: Empty Data State (No available replacements with same skill) */}
                    {!loading && !isForbidden && data && (!data.hasAvailableReplacements || data.candidates.length === 0) && (
                        <div className="flex flex-col items-center justify-center p-8 bg-amber-50/80 rounded-3xl border border-amber-200 text-center space-y-3">
                            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
                                <AlertTriangle className="h-7 w-7" />
                            </div>
                            <div>
                                <h3 className="text-sm font-bold text-amber-900">Không tìm thấy người thay thế phù hợp</h3>
                                <p className="text-xs text-amber-800 mt-1 max-w-lg leading-relaxed">
                                    {data.recommendationMessage || "Không ai cùng kỹ năng còn rảnh trong tuần đó. Kỹ năng hiếm hoặc tuần cao điểm."}
                                </p>
                            </div>
                            <div className="rounded-xl bg-white p-3 border border-amber-200 text-left text-xs text-slate-600 w-full max-w-md">
                                <p className="font-semibold text-amber-900 mb-1">Gợi ý dời lịch từ hệ thống:</p>
                                <ul className="list-disc list-inside space-y-1 text-[11px] text-slate-500">
                                    <li>Dời lịch phân bổ công việc sang tuần tiếp theo khi nhân sự hết quá tải.</li>
                                    <li>Thương lượng giảm bớt khối lượng phân bổ vào dự án trùng lịch.</li>
                                    <li>Tổ chức đào tạo hoặc khai báo thêm nhân sự có kỹ năng dự phòng.</li>
                                </ul>
                            </div>
                        </div>
                    )}

                    {/* TC-01: Success Flow (Candidates Found) */}
                    {!loading && !isForbidden && data && data.hasAvailableReplacements && data.candidates.length > 0 && (
                        <div className="space-y-4">
                            <div className="flex items-center justify-between text-xs">
                                <p className="font-bold text-slate-800">
                                    Danh sách {data.candidates.length} nhân sự thay thế phù hợp:
                                </p>
                                <span className="text-slate-500 font-medium">
                                    Kỹ năng: <span className="text-indigo-600 font-bold">{data.skillName}</span> (Level ≥ {data.requiredProficiencyLevel})
                                </span>
                            </div>

                            <div className="space-y-2 max-h-[260px] overflow-y-auto pr-1">
                                {data.candidates.map((cand: ReplacementCandidate) => {
                                    const isSelected = selectedEmpId === cand.employeeId;
                                    return (
                                        <div
                                            key={cand.employeeId}
                                            onClick={() => setSelectedEmpId(cand.employeeId)}
                                            className={`flex flex-col sm:flex-row sm:items-center justify-between p-4 rounded-2xl border transition cursor-pointer ${
                                                isSelected
                                                    ? "bg-indigo-50/80 border-indigo-500 shadow-xs"
                                                    : "bg-white border-slate-200 hover:border-indigo-200 hover:bg-slate-50/60"
                                            }`}
                                        >
                                            <div className="flex items-center gap-3">
                                                <input
                                                    type="radio"
                                                    name="replacement_employee"
                                                    checked={isSelected}
                                                    onChange={() => setSelectedEmpId(cand.employeeId)}
                                                    className="h-4 w-4 text-indigo-600 focus:ring-indigo-500"
                                                />
                                                <div>
                                                    <div className="flex items-center gap-2">
                                                        <span className="font-bold text-slate-900 text-xs">{cand.fullName}</span>
                                                        <span className="text-[11px] text-slate-400 font-medium">({cand.employeeCode})</span>
                                                    </div>
                                                    <p className="text-[11px] text-slate-500 mt-0.5">{cand.departmentName}</p>
                                                </div>
                                            </div>

                                            <div className="flex items-center gap-4 mt-2 sm:mt-0 pl-7 sm:pl-0">
                                                {/* Proficiency Badge */}
                                                <span className="inline-flex items-center gap-1 rounded-lg bg-sky-50 px-2.5 py-1 text-[11px] font-bold text-sky-700 border border-sky-200">
                                                    <Award className="h-3 w-3" />
                                                    <span>{cand.proficiencyLevelName}</span>
                                                </span>

                                                {/* Free Hours */}
                                                <div className="text-right min-w-[100px]">
                                                    <span className="inline-flex items-center gap-1 rounded-xl bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-700 border border-emerald-200">
                                                        <Clock className="h-3 w-3" />
                                                        <span>Rảnh {cand.freeHours}h</span>
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>

                            {/* Optional Notes Field */}
                            <div className="space-y-1.5 pt-2">
                                <label className="flex items-center gap-1.5 text-xs font-semibold text-slate-700">
                                    <MessageSquareText className="h-3.5 w-3.5 text-slate-400" />
                                    <span>Ghi chú đề xuất / phương án xử lý:</span>
                                </label>
                                <textarea
                                    value={notes}
                                    onChange={(e) => setNotes(e.target.value)}
                                    placeholder="Nhập ghi chú thương lượng hoặc điều phối nhân sự..."
                                    rows={2}
                                    className="w-full rounded-xl border border-slate-200 bg-slate-50 p-2.5 text-xs text-slate-800 placeholder-slate-400 focus:border-indigo-500 focus:bg-white focus:outline-none transition"
                                />
                            </div>
                        </div>
                    )}
                </div>

                {/* Modal Footer */}
                <div className="flex items-center justify-end gap-3 border-t border-slate-100 pt-4 mt-2">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={submitting}
                        className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition"
                    >
                        Đóng
                    </button>

                    {!loading && !isForbidden && data && data.hasAvailableReplacements && data.candidates.length > 0 && (
                        <button
                            type="button"
                            onClick={handleConfirm}
                            disabled={submitting || !selectedEmpId}
                            className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-5 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs disabled:opacity-50"
                        >
                            {submitting ? (
                                <>
                                    <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                                    <span>Đang ghi nhận...</span>
                                </>
                            ) : (
                                <>
                                    <UserCheck className="h-3.5 w-3.5" />
                                    <span>Xác nhận chọn người thay thế</span>
                                </>
                            )}
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}
