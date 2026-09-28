import { useState, useEffect, type FormEvent } from "react";
import {
    X,
    UserPlus,
    Clock,
    Briefcase,
    Building,
    Award,
    AlertTriangle,
    Loader2,
    Search,
    Check,
} from "lucide-react";
import { OrgUnitCombobox, type OrgUnitOption } from "@/components/ui/OrgUnitCombobox";
import { DEFAULT_ORG_UNIT_OPTIONS } from "../employee/form/employeeForm.constants";
import DatePickerInput from "../calendar/DatePickerInput";
import { declareOutsourcedEmployee } from "@/lib/api/employees";
import { getSkills, type SkillResponse } from "@/lib/api/skills";
import { getProjectRoles } from "@/lib/api/project-roles";

interface OutsourcedEmployeeModalProps {
    open: boolean;
    onClose: () => void;
    onSuccess: () => void;
    orgUnitOptions?: readonly OrgUnitOption[];
}

const BASE_INPUT = "w-full rounded-xl border border-slate-200 bg-slate-50/70 px-3.5 py-2 text-xs font-semibold text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100";

export default function OutsourcedEmployeeModal({
    open,
    onClose,
    onSuccess,
    orgUnitOptions = DEFAULT_ORG_UNIT_OPTIONS,
}: OutsourcedEmployeeModalProps) {
    const [fullName, setFullName] = useState("");
    const [providerName, setProviderName] = useState("");
    const [employeeCode, setEmployeeCode] = useState("");
    const [orgUnitId, setOrgUnitId] = useState("");
    const [professionalRole, setProfessionalRole] = useState("");
    const [startDate, setStartDate] = useState("");
    const [contractEndDate, setContractEndDate] = useState("");
    const [standardHoursPerWeek, setStandardHoursPerWeek] = useState(40);
    const [selectedSkillIds, setSelectedSkillIds] = useState<number[]>([]);

    const [availableSkills, setAvailableSkills] = useState<SkillResponse[]>([]);
    const [skillSearchTerm, setSkillSearchTerm] = useState("");
    const [roleOptions, setRoleOptions] = useState<{ id: string; label: string }[]>([]);

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");

    // Reset form when modal opens
    useEffect(() => {
        if (open) {
            setFullName("");
            setProviderName("");
            setEmployeeCode("");
            setOrgUnitId(orgUnitOptions.length > 0 ? String(orgUnitOptions[0].id) : "");
            setProfessionalRole("");
            setStartDate("");
            setContractEndDate("");
            setStandardHoursPerWeek(40);
            setSelectedSkillIds([]);
            setSkillSearchTerm("");
            setErrorMessage("");

            // Fetch skills
            getSkills({ status: "ACTIVE" })
                .then((skills) => {
                    if (Array.isArray(skills)) {
                        setAvailableSkills(skills);
                    }
                })
                .catch((err) => {
                    console.warn("Không thể tải danh sách kỹ năng:", err);
                });

            // Fetch roles
            getProjectRoles(true)
                .then((roles) => {
                    if (Array.isArray(roles) && roles.length > 0) {
                        const active = roles.filter((r) => r.status === "ACTIVE");
                        setRoleOptions(
                            active.map((r) => ({
                                id: r.name,
                                label: r.code ? `${r.name} (${r.code})` : r.name,
                            }))
                        );
                    }
                })
                .catch((err) => {
                    console.warn("Không thể tải danh sách vai trò:", err);
                });
        }
    }, [open, orgUnitOptions]);

    if (!open) return null;

    const toggleSkill = (skillId: number) => {
        setSelectedSkillIds((prev) =>
            prev.includes(skillId) ? prev.filter((id) => id !== skillId) : [...prev, skillId]
        );
    };

    const filteredSkills = availableSkills.filter((s) =>
        s.name.toLowerCase().includes(skillSearchTerm.trim().toLowerCase())
    );

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setErrorMessage("");

        if (!fullName.trim()) {
            setErrorMessage("Vui lòng nhập họ và tên nhân sự.");
            return;
        }
        if (!providerName.trim()) {
            setErrorMessage("Vui lòng nhập tên đơn vị cung cấp.");
            return;
        }
        if (!orgUnitId) {
            setErrorMessage("Vui lòng chọn đơn vị phòng ban tiếp nhận.");
            return;
        }
        if (!startDate) {
            setErrorMessage("Vui lòng chọn ngày bắt đầu hợp đồng thuê.");
            return;
        }
        if (!contractEndDate) {
            setErrorMessage("Vui lòng chọn ngày kết thúc hợp đồng thuê.");
            return;
        }
        if (contractEndDate < startDate) {
            setErrorMessage("Ngày kết thúc hợp đồng thuê không được trước ngày bắt đầu.");
            return;
        }
        if (!standardHoursPerWeek || standardHoursPerWeek < 1 || standardHoursPerWeek > 168) {
            setErrorMessage("Số giờ chuẩn làm việc mỗi tuần phải từ 1 đến 168 giờ.");
            return;
        }

        setIsSubmitting(true);
        try {
            await declareOutsourcedEmployee({
                fullName: fullName.trim(),
                providerName: providerName.trim(),
                employeeCode: employeeCode.trim() || undefined,
                orgUnitId: Number(orgUnitId),
                professionalRole: professionalRole.trim() || undefined,
                startDate,
                contractEndDate,
                standardHoursPerWeek: Number(standardHoursPerWeek) || 40,
                skillIds: selectedSkillIds.length > 0 ? selectedSkillIds : undefined,
            });

            onSuccess();
            onClose();
        } catch (err: any) {
            setErrorMessage(err?.message || "Khai báo hồ sơ nhân sự thuê ngoài thất bại. Vui lòng kiểm tra lại.");
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="relative flex max-h-[90vh] w-full max-w-2xl flex-col rounded-3xl border border-slate-200/90 bg-white shadow-2xl overflow-hidden">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-slate-50/50">
                    <div className="flex items-center gap-3">
                        <div className="flex size-10 items-center justify-center rounded-2xl border border-amber-100 bg-amber-50 text-amber-600 shadow-2xs">
                            <UserPlus className="size-5" />
                        </div>
                        <div>
                            <h2 className="text-base font-bold text-slate-900">Khai báo hồ sơ nhân sự thuê ngoài</h2>
                            <p className="text-xs text-slate-500">
                                Đăng ký chuyên gia thuê ngoài cho dự án và ma trận năng lực (không cấp tài khoản)
                            </p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={isSubmitting}
                        className="rounded-xl p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
                    >
                        <X className="size-5" />
                    </button>
                </div>

                {/* Body Form */}
                <form onSubmit={handleSubmit} className="flex flex-col flex-1 overflow-hidden">
                    <div className="flex-1 overflow-y-auto px-6 py-5 space-y-4">
                        {/* Error alert */}
                        {errorMessage && (
                            <div className="flex items-center gap-2.5 rounded-2xl border border-rose-200 bg-rose-50/90 p-3.5 text-xs font-semibold text-rose-800">
                                <AlertTriangle className="size-4 shrink-0 text-rose-600" />
                                <span>{errorMessage}</span>
                            </div>
                        )}

                        {/* Row 1: Full Name & Provider Name */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-bold text-slate-700 mb-1">
                                    Họ và tên chuyên gia <span className="text-rose-500">*</span>
                                </label>
                                <input
                                    type="text"
                                    value={fullName}
                                    onChange={(e) => setFullName(e.target.value)}
                                    placeholder="Ví dụ: Nguyễn Văn Chuyên Gia"
                                    className={BASE_INPUT}
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-slate-700 mb-1">
                                    Đơn vị cung cấp (Vendor / Công ty đối tác) <span className="text-rose-500">*</span>
                                </label>
                                <div className="relative">
                                    <Building className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
                                    <input
                                        type="text"
                                        value={providerName}
                                        onChange={(e) => setProviderName(e.target.value)}
                                        placeholder="Ví dụ: FPT Software, TMA, CMC..."
                                        className={`${BASE_INPUT} pl-9`}
                                    />
                                </div>
                            </div>
                        </div>

                        {/* Row 2: Org Unit & Employee Code */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-bold text-slate-700 mb-1">
                                    Phòng ban / Đơn vị tiếp nhận <span className="text-rose-500">*</span>
                                </label>
                                <OrgUnitCombobox
                                    id="outsourced-org-unit"
                                    options={orgUnitOptions}
                                    value={orgUnitId}
                                    onChange={setOrgUnitId}
                                    placeholder="Chọn đơn vị tiếp nhận..."
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-slate-700 mb-1">
                                    Mã nhân sự (Tùy chọn)
                                </label>
                                <input
                                    type="text"
                                    value={employeeCode}
                                    onChange={(e) => setEmployeeCode(e.target.value.toUpperCase())}
                                    placeholder="Ví dụ: EXT-001 (để trống hệ thống tự sinh)"
                                    className={BASE_INPUT}
                                />
                            </div>
                        </div>

                        {/* Row 3: Professional Role & Standard Hours */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-bold text-slate-700 mb-1">
                                    Vai trò chuyên môn
                                </label>
                                <div className="relative">
                                    <Briefcase className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
                                    <input
                                        type="text"
                                        list="outsourced-roles-list"
                                        value={professionalRole}
                                        onChange={(e) => setProfessionalRole(e.target.value)}
                                        placeholder="Ví dụ: Senior Java Developer"
                                        className={`${BASE_INPUT} pl-9`}
                                    />
                                    <datalist id="outsourced-roles-list">
                                        {roleOptions.map((r) => (
                                            <option key={r.id} value={r.label} />
                                        ))}
                                    </datalist>
                                </div>
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-slate-700 mb-1">
                                    Định mức giờ làm việc / tuần <span className="text-rose-500">*</span>
                                </label>
                                <div className="relative">
                                    <Clock className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
                                    <input
                                        type="number"
                                        min={1}
                                        max={168}
                                        value={standardHoursPerWeek}
                                        onChange={(e) => setStandardHoursPerWeek(Number(e.target.value))}
                                        className={`${BASE_INPUT} pl-9`}
                                    />
                                </div>
                            </div>
                        </div>

                        {/* Row 4: Contract Duration */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 rounded-2xl border border-slate-200/80 bg-slate-50/50 p-3.5">
                            <div>
                                <label className="block text-xs font-bold text-slate-700 mb-1">
                                    Ngày bắt đầu hợp đồng thuê <span className="text-rose-500">*</span>
                                </label>
                                <DatePickerInput
                                    value={startDate}
                                    onChange={setStartDate}
                                    placeholder="Chọn ngày bắt đầu"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-slate-700 mb-1">
                                    Ngày kết thúc hợp đồng thuê <span className="text-rose-500">*</span>
                                </label>
                                <DatePickerInput
                                    value={contractEndDate}
                                    onChange={setContractEndDate}
                                    placeholder="Chọn ngày kết thúc"
                                />
                            </div>
                        </div>

                        {/* Row 5: Skills Selection */}
                        <div className="space-y-2">
                            <div className="flex items-center justify-between">
                                <label className="text-xs font-bold text-slate-700 flex items-center gap-1.5">
                                    <Award className="size-4 text-amber-500" />
                                    <span>Kỹ năng chuyên môn</span>
                                    {selectedSkillIds.length > 0 && (
                                        <span className="rounded-full bg-indigo-50 px-2 py-0.5 text-[11px] font-bold text-indigo-600 border border-indigo-200">
                                            {selectedSkillIds.length} đã chọn
                                        </span>
                                    )}
                                </label>
                            </div>

                            {/* Skills search */}
                            <div className="relative">
                                <Search className="pointer-events-none absolute left-3 top-1/2 size-3.5 -translate-y-1/2 text-slate-400" />
                                <input
                                    type="text"
                                    value={skillSearchTerm}
                                    onChange={(e) => setSkillSearchTerm(e.target.value)}
                                    placeholder="Tìm kiếm kỹ năng chuyên môn..."
                                    className={`${BASE_INPUT} pl-8 py-1.5 text-xs`}
                                />
                            </div>

                            {/* Skills tag container */}
                            <div className="max-h-32 overflow-y-auto rounded-xl border border-slate-200 bg-slate-50/40 p-2 flex flex-wrap gap-1.5">
                                {filteredSkills.length === 0 ? (
                                    <span className="text-[11px] text-slate-400 italic p-1">
                                        {availableSkills.length === 0
                                            ? "Không có danh mục kỹ năng hoặc đang tải..."
                                            : "Không tìm thấy kỹ năng phù hợp"}
                                    </span>
                                ) : (
                                    filteredSkills.map((skill) => {
                                        const isSelected = selectedSkillIds.includes(skill.id);
                                        return (
                                            <button
                                                key={skill.id}
                                                type="button"
                                                onClick={() => toggleSkill(skill.id)}
                                                className={`flex items-center gap-1 rounded-lg px-2.5 py-1 text-xs font-medium transition cursor-pointer ${
                                                    isSelected
                                                        ? "bg-indigo-600 text-white shadow-2xs font-semibold"
                                                        : "bg-white border border-slate-200 text-slate-700 hover:border-indigo-300 hover:bg-slate-50"
                                                }`}
                                            >
                                                {isSelected && <Check className="size-3" />}
                                                <span>{skill.name}</span>
                                            </button>
                                        );
                                    })
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Footer Actions */}
                    <div className="flex items-center justify-end gap-3 border-t border-slate-100 bg-slate-50/70 px-6 py-4">
                        <button
                            type="button"
                            onClick={onClose}
                            disabled={isSubmitting}
                            className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 shadow-2xs hover:bg-slate-50 transition"
                        >
                            Hủy bỏ
                        </button>
                        <button
                            type="submit"
                            disabled={isSubmitting}
                            className="flex items-center gap-2 rounded-xl border border-indigo-600 bg-indigo-600 px-5 py-2 text-xs font-bold text-white shadow-xs hover:bg-indigo-700 active:scale-95 transition disabled:opacity-50"
                        >
                            {isSubmitting ? (
                                <>
                                    <Loader2 className="size-4 animate-spin" />
                                    <span>Đang lưu...</span>
                                </>
                            ) : (
                                <>
                                    <UserPlus className="size-4" />
                                    <span>Khai báo nhân sự</span>
                                </>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
