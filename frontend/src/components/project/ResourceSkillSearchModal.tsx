import { useState, useEffect } from 'react';
import { X, Search, UserPlus, Award, AlertCircle, CheckCircle } from 'lucide-react';
import { getSkills, type SkillResponse } from '@/lib/api/skills';
import { searchResourceCandidates, type ResourceSearchResult } from '@/lib/api/allocations';
import type { ProjectMember } from './projectData';

interface ResourceSkillSearchModalProps {
    isOpen: boolean;
    onClose: () => void;
    fromYear: number;
    fromWeek: number;
    existingMemberIds: string[];
    onAddMembers: (newMembers: ProjectMember[]) => void;
}

export function ResourceSkillSearchModal({
    isOpen,
    onClose,
    fromYear,
    fromWeek,
    existingMemberIds,
    onAddMembers,
}: ResourceSkillSearchModalProps) {
    const [skillsList, setSkillsList] = useState<SkillResponse[]>([]);
    const [selectedSkillId, setSelectedSkillId] = useState<number | null>(null);
    const [minProficiencyLevel, setMinProficiencyLevel] = useState<number>(1);
    const [isLoadingSkills, setIsLoadingSkills] = useState<boolean>(false);
    const [isSearching, setIsSearching] = useState<boolean>(false);
    const [searchError, setSearchError] = useState<string | null>(null);
    const [searchResults, setSearchResults] = useState<ResourceSearchResult[]>([]);
    const [selectedCandidateIds, setSelectedCandidateIds] = useState<number[]>([]);

    useEffect(() => {
        if (isOpen) {
            setIsLoadingSkills(true);
            getSkills({ status: 'ACTIVE' })
                .then((skills) => {
                    setSkillsList(skills || []);
                    if (skills && skills.length > 0) {
                        setSelectedSkillId(skills[0].id);
                    }
                })
                .catch((err) => {
                    console.error('Lỗi khi tải danh sách kỹ năng:', err);
                    setSearchError('Không thể tải danh mục kỹ năng');
                })
                .finally(() => setIsLoadingSkills(false));
        }
    }, [isOpen]);

    const handleSearch = async () => {
        if (!selectedSkillId) return;
        setIsSearching(true);
        setSearchError(null);
        setSelectedCandidateIds([]);
        try {
            const targetEndWeek = fromWeek + 3;
            const toYear = targetEndWeek > 52 ? fromYear + 1 : fromYear;
            const toWeek = targetEndWeek > 52 ? targetEndWeek - 52 : targetEndWeek;

            const results = await searchResourceCandidates({
                skillId: selectedSkillId,
                minProficiencyLevel,
                fromYear,
                fromWeek,
                toYear,
                toWeek,
                durationWeeks: 4,
            });
            setSearchResults(results || []);
        } catch (err: any) {
            console.error('Search resources error:', err);
            setSearchError(err?.message || 'Có lỗi khi tìm kiếm nhân sự theo kỹ năng');
            setSearchResults([]);
        } finally {
            setIsSearching(false);
        }
    };

    const toggleCandidateSelection = (employeeId: number) => {
        setSelectedCandidateIds((prev) =>
            prev.includes(employeeId) ? prev.filter((id) => id !== employeeId) : [...prev, employeeId]
        );
    };

    const handleConfirmAdd = () => {
        const selectedCandidates = searchResults.filter((cand) =>
            selectedCandidateIds.includes(cand.employeeId)
        );

        const newMembers: ProjectMember[] = selectedCandidates.map((cand) => ({
            id: `u-${cand.employeeId}`,
            employeeId: cand.employeeId,
            name: cand.fullName,
            role: `${cand.jobTitle || 'Nhân viên'} (${cand.skillName} - Level ${cand.proficiencyLevel})`,
            avatar: '',
            capacity: 40,
            weeklyHours: {},
        }));

        onAddMembers(newMembers);
        onClose();
    };

    if (!isOpen) return null;

    const existingEmpIdSet = new Set(existingMemberIds.map((id) => id.replace('u-', '')));

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 overflow-y-auto">
            <div className="relative w-full max-w-2xl rounded-xl border border-slate-200 bg-white shadow-2xl transition-all my-8">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
                    <div className="flex items-center gap-2.5">
                        <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
                            <UserPlus className="h-5 w-5" />
                        </span>
                        <div>
                            <h3 className="text-base font-bold text-slate-800">
                                Lọc & Chọn Nhân Sự Phù Hợp Kỹ Năng
                            </h3>
                            <p className="text-xs text-slate-500">
                                Tìm kiếm nhân sự có kỹ năng phù hợp và kiểm tra năng lực khả dụng theo tuần
                            </p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                {/* Form Controls */}
                <div className="p-6 space-y-4">
                    <div className="grid grid-cols-1 sm:grid-cols-12 gap-4">
                        {/* Select Skill */}
                        <div className="sm:col-span-7">
                            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                                Kỹ Năng Yêu Cầu <span className="text-rose-500">*</span>
                            </label>
                            {isLoadingSkills ? (
                                <div className="h-9 w-full bg-slate-100 animate-pulse rounded-lg" />
                            ) : (
                                <select
                                    value={selectedSkillId || ''}
                                    onChange={(e) => setSelectedSkillId(Number(e.target.value))}
                                    className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 outline-none"
                                >
                                    <option value="" disabled>-- Chọn kỹ năng --</option>
                                    {skillsList.map((skill) => (
                                        <option key={skill.id} value={skill.id}>
                                            {skill.name} ({skill.groupName || 'Khác'})
                                        </option>
                                    ))}
                                </select>
                            )}
                        </div>

                        {/* Minimum Proficiency Level */}
                        <div className="sm:col-span-5">
                            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                                Mức Độ Thành Thạo Tối Thiểu
                            </label>
                            <select
                                value={minProficiencyLevel}
                                onChange={(e) => setMinProficiencyLevel(Number(e.target.value))}
                                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 outline-none"
                            >
                                <option value={1}>Cơ bản (Level 1)</option>
                                <option value={2}>Khá (Level 2)</option>
                                <option value={3}>Thành thạo (Level 3)</option>
                                <option value={4}>Chuyên gia (Level 4)</option>
                                <option value={5}>Bậc thầy (Level 5)</option>
                            </select>
                        </div>
                    </div>

                    <div className="flex justify-end">
                        <button
                            type="button"
                            onClick={handleSearch}
                            disabled={!selectedSkillId || isSearching}
                            className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-xs hover:bg-indigo-700 disabled:opacity-50 transition cursor-pointer"
                        >
                            <Search className="h-4 w-4" />
                            {isSearching ? 'Đang tìm kiếm...' : 'Tìm Nhân Sự Phù Hợp'}
                        </button>
                    </div>

                    {searchError && (
                        <div className="flex items-center gap-2 rounded-lg bg-rose-50 border border-rose-200 p-3 text-xs text-rose-700">
                            <AlertCircle className="h-4 w-4 shrink-0" />
                            <span>{searchError}</span>
                        </div>
                    )}

                    {/* Results List */}
                    <div className="mt-4 border-t border-slate-100 pt-4">
                        <div className="flex items-center justify-between mb-2">
                            <h4 className="text-xs font-bold text-slate-700">
                                Kết Quả Phù Hợp ({searchResults.length})
                            </h4>
                            {selectedCandidateIds.length > 0 && (
                                <span className="text-[11px] font-semibold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md">
                                    Đã chọn: {selectedCandidateIds.length} nhân sự
                                </span>
                            )}
                        </div>

                        <div className="max-h-60 overflow-y-auto space-y-2 pr-1">
                            {searchResults.length === 0 ? (
                                <div className="py-8 text-center text-xs text-slate-400">
                                    {isSearching
                                        ? 'Đang truy vấn cơ sở dữ liệu...'
                                        : 'Nhấn "Tìm Nhân Sự Phù Hợp" để hiển thị danh sách ứng viên có kỹ năng và năng lực khả dụng.'}
                                </div>
                            ) : (
                                searchResults.map((candidate) => {
                                    const isAlreadyAdded = existingEmpIdSet.has(String(candidate.employeeId));
                                    const isSelected = selectedCandidateIds.includes(candidate.employeeId);

                                    return (
                                        <div
                                            key={candidate.employeeId}
                                            onClick={() => !isAlreadyAdded && toggleCandidateSelection(candidate.employeeId)}
                                            className={`flex items-center justify-between p-3 rounded-lg border transition ${
                                                isAlreadyAdded
                                                    ? 'bg-slate-50 border-slate-200 opacity-60 cursor-not-allowed'
                                                    : isSelected
                                                    ? 'bg-indigo-50/60 border-indigo-300 cursor-pointer shadow-2xs'
                                                    : 'bg-white border-slate-200 hover:border-indigo-200 cursor-pointer'
                                            }`}
                                        >
                                            <div className="flex items-center gap-3">
                                                <input
                                                    type="checkbox"
                                                    disabled={isAlreadyAdded}
                                                    checked={isSelected || isAlreadyAdded}
                                                    onChange={() => {}}
                                                    className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
                                                />
                                                <div>
                                                    <div className="flex items-center gap-2">
                                                        <span className="text-xs font-bold text-slate-800">
                                                            {candidate.fullName}
                                                        </span>
                                                        <span className="text-[10px] font-semibold text-slate-500">
                                                            ({candidate.employeeCode})
                                                        </span>
                                                        <span className="inline-flex items-center gap-1 rounded bg-amber-50 px-1.5 py-0.5 text-[10px] font-bold text-amber-700 border border-amber-200">
                                                            <Award className="h-3 w-3" /> Level {candidate.proficiencyLevel}
                                                        </span>
                                                    </div>
                                                    <div className="text-[11px] text-slate-500 mt-0.5">
                                                        {candidate.jobTitle || 'Chuyên viên'} • {candidate.orgUnitName || 'Phòng ban'}
                                                    </div>
                                                </div>
                                            </div>

                                            <div className="text-right">
                                                <div className="text-xs font-bold text-emerald-700">
                                                    {candidate.totalRemainingHours}h rảnh
                                                </div>
                                                <div className="text-[10px] text-slate-400">
                                                    {isAlreadyAdded ? 'Đã có trong dự án' : 'Trong 4 tuần tới'}
                                                </div>
                                            </div>
                                        </div>
                                    );
                                })
                            )}
                        </div>
                    </div>
                </div>

                {/* Footer */}
                <div className="flex items-center justify-end gap-2 border-t border-slate-100 bg-slate-50 px-6 py-3 rounded-b-xl">
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 transition cursor-pointer"
                    >
                        Hủy
                    </button>
                    <button
                        type="button"
                        onClick={handleConfirmAdd}
                        disabled={selectedCandidateIds.length === 0}
                        className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-xs hover:bg-indigo-700 disabled:opacity-50 transition cursor-pointer"
                    >
                        <CheckCircle className="h-4 w-4" />
                        Thêm ({selectedCandidateIds.length}) Nhân Sự Vào Dự Án
                    </button>
                </div>
            </div>
        </div>
    );
}
