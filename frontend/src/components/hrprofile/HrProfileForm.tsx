import { useState, type FormEvent } from "react";
import {
    X,
    User,
    Mail,
    Clock,
    CalendarDays,
    BadgeAlert,
    Briefcase,
    Building2,
} from "lucide-react";
import { cn } from "../../lib/utils";
import type { HrProfileData } from "./hrprofile.types";

interface HrProfileFormProps {
    open: boolean;
    initialData?: HrProfileData;
    nextEmployeeCode: string;
    onClose: () => void;
    onSave: (data: HrProfileData) => void;
}

const DEPARTMENT_LIST = [
    "Phòng Công nghệ",
    "Phòng Marketing",
    "Phòng Nhân sự",
    "Phòng Kinh doanh",
    "Phòng Tài chính",
    "Ban Giám đốc",
];

const ROLE_LIST = [
    "Product Owner / BA",
    "Frontend Developer",
    "Backend Developer",
    "UI/UX Designer",
    "DevOps Engineer",
    "QA / QC Tester",
    "Project Manager",
    "HR Manager",
    "Accountant",
];

import TaskSelect from "../task/TaskSelect";
import DatePickerInput from "../calendar/DatePickerInput";

const ROLE_OPTIONS = ROLE_LIST.map((r) => ({ id: r, label: r }));
const DEPARTMENT_OPTIONS = DEPARTMENT_LIST.map((d) => ({ id: d, label: d }));

const BASE_INPUT = "w-full rounded-xl border border-slate-200 bg-slate-50/70 px-3.5 py-2 text-xs font-semibold text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100";
const DISABLED_INPUT = "w-full rounded-xl border border-slate-200 bg-slate-100 px-3.5 py-2 text-xs font-semibold text-slate-500 outline-none cursor-not-allowed";

export default function HrProfileForm({
    open,
    initialData,
    nextEmployeeCode,
    onClose,
    onSave,
}: HrProfileFormProps) {
    const isEdit = Boolean(initialData);

    const emptyForm = (): Partial<HrProfileData> => ({
        employeeCode: nextEmployeeCode,
        fullName: "",
        email: "",
        username: "",
        password: "",
        department: "",
        professionalRole: "",
        startDate: "",
        contractEndDate: "",
        standardHoursPerWeek: 40,
    });

    const [formData, setFormData] = useState<Partial<HrProfileData>>(() =>
        initialData ? { ...initialData } : emptyForm()
    );

    const [prevOpen, setPrevOpen] = useState(open);
    const [prevInitial, setPrevInitial] = useState(initialData);
    const [errorMessage, setErrorMessage] = useState("");

    if (open !== prevOpen || initialData !== prevInitial) {
        setPrevOpen(open);
        setPrevInitial(initialData);
        setErrorMessage("");
        setFormData(initialData ? { ...initialData } : emptyForm());
    }

    if (!open) return null;

    const set = <K extends keyof HrProfileData>(field: K, value: HrProfileData[K]) =>
        setFormData((prev) => ({ ...prev, [field]: value }));

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();
        setErrorMessage("");

        if (!formData.fullName?.trim()) {
            setErrorMessage("Vui lòng nhập họ và tên nhân viên.");
            return;
        }
        if (!formData.email?.trim()) {
            setErrorMessage("Vui lòng nhập địa chỉ email.");
            return;
        }
        const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
        if (!EMAIL_REGEX.test(formData.email.trim())) {
            setErrorMessage("Email không đúng định dạng. Email phải có ký tự '@' và tên miền hợp lệ chứa dấu '.' (ví dụ: user@company.com).");
            return;
        }
        if (!formData.employeeCode?.trim()) {
            setErrorMessage("Vui lòng nhập mã nhân viên.");
            return;
        }
        if (!formData.department?.trim()) {
            setErrorMessage("Vui lòng chọn đơn vị tổ chức trực thuộc.");
            return;
        }
        if (!formData.standardHoursPerWeek || Number(formData.standardHoursPerWeek) < 1) {
            setErrorMessage("Giờ làm việc chuẩn phải lớn hơn 0.");
            return;
        }
        if (formData.startDate && formData.contractEndDate && formData.contractEndDate < formData.startDate) {
            setErrorMessage("Ngày kết thúc hợp đồng không được trước ngày vào làm.");
            return;
        }

        onSave({
            ...(formData as HrProfileData),
            fullName: formData.fullName!.trim(),
            email: formData.email?.trim() || "",
            employeeCode: formData.employeeCode!.trim().toUpperCase(),
            standardHoursPerWeek: Number(formData.standardHoursPerWeek) || 40,
        });
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="relative flex max-h-[90vh] w-full max-w-2xl flex-col rounded-3xl border border-slate-200/90 bg-white shadow-2xl overflow-hidden">

                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
                    <div className="flex items-center gap-3">
                        <div className="flex size-10 items-center justify-center rounded-2xl border border-indigo-100 bg-indigo-50 text-indigo-600 shadow-2xs">
                            <User className="size-5" />
                        </div>
                        <div>
                            <h2 className="text-base font-bold text-slate-900">
                                {isEdit ? "Chỉnh sửa hồ sơ nhân sự" : "Tạo hồ sơ nhân sự mới"}
                            </h2>
                            <p className="text-xs text-slate-500">
                                {isEdit
                                    ? `Cập nhật thông tin hành chính cho ${formData.fullName}`
                                    : "Khai báo thông tin cá nhân, đơn vị và hợp đồng lao động"}
                            </p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-xl p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                        title="Đóng"
                    >
                        <X className="size-4" />
                    </button>
                </div>

                {/* Form Content - Scrollable */}
                <form
                    id="hr-profile-form"
                    onSubmit={handleSubmit}
                    className="flex-1 overflow-y-auto px-6 py-4 space-y-6 [scrollbar-width:thin] [scrollbar-color:#cbd5e1_transparent] [&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-thumb]:bg-slate-200 [&::-webkit-scrollbar-thumb]:rounded-full"
                >
                    {errorMessage && (
                        <div className="flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3.5 py-2.5 text-xs font-semibold text-rose-700">
                            <BadgeAlert className="size-4 shrink-0 text-rose-600" />
                            <span>{errorMessage}</span>
                        </div>
                    )}

                    {/* KHỐI 1: THÔNG TIN CÁ NHÂN & TÀI KHOẢN */}
                    <div className="space-y-3.5">
                        <div className="flex items-center gap-2 border-b border-slate-100 pb-2 text-xs font-bold uppercase tracking-wider text-slate-500">
                            <User className="size-4 text-indigo-600" />
                            <span>1. THÔNG TIN CÁ NHÂN &amp; TÀI KHOẢN</span>
                        </div>

                        {/* Hàng 1: Họ và tên & Email */}
                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                            {/* Họ và tên */}
                            <div className="space-y-1.5">
                                <label className="text-xs font-semibold text-slate-700">Họ và tên *</label>
                                <input
                                    type="text"
                                    required
                                    placeholder="VD: Nguyễn Văn A"
                                    value={formData.fullName || ""}
                                    onChange={(e) => {
                                        const name = e.target.value;
                                        setFormData((prev) => ({
                                            ...prev,
                                            fullName: name,
                                            username: !isEdit && !prev.username
                                                ? name.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9]/g, "").slice(0, 15)
                                                : prev.username,
                                        }));
                                    }}
                                    className={BASE_INPUT}
                                />
                            </div>

                            {/* Email */}
                            <div className="space-y-1.5">
                                <div className="flex items-center justify-between">
                                    <label className="text-xs font-semibold text-slate-700">Email *</label>
                                    {isEdit && <span className="text-[10px] text-slate-400 font-medium">(Cố định)</span>}
                                </div>
                                <div className="relative">
                                    <input
                                        type="email"
                                        required
                                        disabled={isEdit}
                                        placeholder="hung@company.com"
                                        value={formData.email || ""}
                                        onChange={(e) => set("email", e.target.value)}
                                        className={cn(BASE_INPUT, isEdit && DISABLED_INPUT)}
                                    />
                                    <Mail className="pointer-events-none absolute right-3.5 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
                                </div>
                            </div>
                        </div>

                        {/* Hàng 2: Mã nhân viên & Vai trò chuyên môn */}
                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                            {/* Mã nhân viên */}
                            <div className="space-y-1.5">
                                <div className="flex items-center justify-between">
                                    <label className="text-xs font-semibold text-slate-700">Mã nhân viên *</label>
                                    <span className="text-[10px] text-slate-400 font-medium">
                                        {isEdit ? "(Cố định)" : "(Tự sinh hoặc tự nhập)"}
                                    </span>
                                </div>
                                <input
                                    type="text"
                                    required
                                    disabled={isEdit}
                                    placeholder="VD: EMP-001"
                                    value={formData.employeeCode || ""}
                                    onChange={(e) => { if (!isEdit) set("employeeCode", e.target.value); }}
                                    className={cn("font-bold uppercase", isEdit ? DISABLED_INPUT : BASE_INPUT)}
                                />
                            </div>

                            {/* Vai trò chuyên môn */}
                            <div className="space-y-1.5">
                                <label className="text-xs font-semibold text-slate-700">Vai trò chuyên môn</label>
                                <TaskSelect
                                    value={formData.professionalRole || ""}
                                    options={ROLE_OPTIONS}
                                    onChange={(id) => set("professionalRole", id)}
                                    placeholder="-- Chọn vai trò --"
                                    hideSearch={true}
                                    icon={<Briefcase className="size-4 shrink-0 text-slate-400" />}
                                    buttonClassName="bg-slate-50/70 border-slate-200 py-2 rounded-xl"
                                />
                            </div>
                        </div>

                        {/* Đơn vị tổ chức trực thuộc */}
                        <div className="space-y-1.5">
                            <label className="text-xs font-semibold text-slate-700">Đơn vị tổ chức trực thuộc *</label>
                            <TaskSelect
                                value={formData.department || ""}
                                options={DEPARTMENT_OPTIONS}
                                onChange={(id) => set("department", id)}
                                placeholder="Chọn phòng ban / đơn vị (dạng cây)..."
                                hideSearch={true}
                                icon={<Building2 className="size-4 shrink-0 text-slate-400" />}
                                buttonClassName="bg-slate-50/70 border-slate-200 py-2 rounded-xl"
                            />
                        </div>
                    </div>

                    {/* KHỐI 2: HỢP ĐỒNG & THỜI GIAN LÀM VIỆC */}
                    <div className="space-y-3.5 pt-2">
                        <div className="flex items-center gap-2 border-b border-slate-100 pb-2 text-xs font-bold uppercase tracking-wider text-slate-500">
                            <CalendarDays className="size-4 text-indigo-600" />
                            <span>2. Hợp đồng &amp; Thời gian làm việc</span>
                        </div>

                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                            {/* Ngày vào làm */}
                            <div className="space-y-1.5">
                                <div className="flex items-center justify-between">
                                    <label className="text-xs font-semibold text-slate-700">Ngày vào làm</label>
                                    <span className="text-[10px] text-slate-400 font-medium">(Bắt đầu HĐLĐ)</span>
                                </div>
                                <DatePickerInput
                                    value={formData.startDate || ""}
                                    onChange={(val) => set("startDate", val)}
                                    placeholder="mm/dd/yyyy"
                                />
                            </div>

                            {/* Ngày kết thúc HĐLĐ */}
                            <div className="space-y-1.5">
                                <div className="flex items-center justify-between">
                                    <label className="text-xs font-semibold text-slate-700">Ngày kết thúc HĐLĐ</label>
                                    <span className="text-[10px] text-slate-400 font-medium">(Để trống nếu vô thời hạn)</span>
                                </div>
                                <DatePickerInput
                                    value={formData.contractEndDate || ""}
                                    min={formData.startDate || undefined}
                                    onChange={(val) => set("contractEndDate", val)}
                                    placeholder="mm/dd/yyyy"
                                />
                            </div>
                        </div>

                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                            {/* Giờ làm việc chuẩn / tuần */}
                            <div className="space-y-1.5">
                                <div className="flex items-center justify-between">
                                    <label className="text-xs font-semibold text-slate-700">Giờ làm việc chuẩn / tuần *</label>
                                    <span className="text-[10px] text-slate-400 font-medium">(Mặc định: 40h)</span>
                                </div>
                                <div className="relative">
                                    <input
                                        type="number"
                                        min={1}
                                        max={168}
                                        step={1}
                                        required
                                        placeholder="40"
                                        value={formData.standardHoursPerWeek ?? 40}
                                        onChange={(e) => set("standardHoursPerWeek", Number(e.target.value) || 0)}
                                        className={BASE_INPUT}
                                    />
                                    <Clock className="pointer-events-none absolute right-3.5 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
                                </div>
                            </div>
                        </div>
                    </div>
                </form>

                {/* Footer Actions */}
                <div className="flex items-center justify-end gap-3 border-t border-slate-100 bg-slate-50/50 px-6 py-3.5">
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-600 shadow-xs transition hover:bg-slate-50 hover:text-slate-800"
                    >
                        Hủy
                    </button>
                    <button
                        type="submit"
                        form="hr-profile-form"
                        className="rounded-xl border border-indigo-600 bg-indigo-600 px-5 py-2 text-xs font-semibold text-white shadow-xs transition hover:bg-indigo-700 active:scale-95"
                    >
                        {isEdit ? "Lưu thay đổi" : "Tạo hồ sơ"}
                    </button>
                </div>
            </div>
        </div>
    );
}