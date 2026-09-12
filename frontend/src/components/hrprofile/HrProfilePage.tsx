import { useState, useMemo, useEffect } from "react";
import { Plus, Search, X, Check, AlertTriangle, Users, Loader2 } from "lucide-react";
import type { HrProfileData } from "./hrprofile.types";
import HrProfileCard from "./HrProfileCard";
import HrProfileForm from "./HrProfileForm";
import { useAuthUser } from "@/lib/auth-session";
import { getEmployees, updateEmployeeProfile } from "@/lib/api/employees";
import { getUsers } from "@/lib/api/users";
import { getOrgTree } from "@/lib/api/org-units";
import { flattenActiveOrgTree } from "@/lib/organization";
import { getStoredDates } from "@/lib/employee-storage";

export default function HrProfilePage() {
    const currentUser = useAuthUser();
    const roleCode = currentUser?.roleCode?.toUpperCase().replace(/_/g, "-") || "";
    const canManage = roleCode === "VT-05";
    const isSelfOnly = roleCode === "VT-04" || currentUser?.dataScope === "SELF";

    const [profiles, setProfiles] = useState<HrProfileData[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState("");
    const [isFormOpen, setIsFormOpen] = useState(false);
    const [editingProfile, setEditingProfile] = useState<HrProfileData | undefined>(undefined);
    const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

    const loadProfiles = async () => {
        setIsLoading(true);
        try {
            const [empRes, usersRes, treeRes] = await Promise.allSettled([
                getEmployees(1, 100),
                getUsers(0, 100),
                getOrgTree(),
            ]);

            const orgUnitMap = new Map<number, string>();
            if (treeRes.status === "fulfilled" && treeRes.value) {
                const flat = flattenActiveOrgTree(treeRes.value);
                flat.forEach((unit) => {
                    orgUnitMap.set(unit.id, unit.unitName);
                });
            }

            const userMap = new Map<number, any>();
            if (usersRes.status === "fulfilled" && usersRes.value?.content) {
                usersRes.value.content.forEach((u: any) => {
                    userMap.set(u.id, u);
                    if (u.employeeId) userMap.set(u.employeeId, u);
                });
            }

            if (empRes.status === "fulfilled" && empRes.value && empRes.value.content) {
                const mapped: HrProfileData[] = empRes.value.content.map((p) => {
                    const u = (p.userId && userMap.get(p.userId)) || userMap.get(p.id);
                    const empCode = p.employeeCode || (p.id ? `EMP-${String(p.id).padStart(3, "0")}` : "");
                    const dates = getStoredDates(p.id) || (p.userId ? getStoredDates(p.userId) : undefined) || getStoredDates(empCode);
                    const deptName = (p.orgUnitId && orgUnitMap.get(p.orgUnitId)) || u?.orgUnitName || "Chưa phân bổ";

                    return {
                        id: String(p.id),
                        employeeId: p.id,
                        employeeCode: empCode,
                        fullName: p.fullName || u?.fullName || "",
                        email: u?.email || "",
                        username: u?.username || "",
                        orgUnitId: p.orgUnitId ? String(p.orgUnitId) : undefined,
                        department: deptName,
                        professionalRole: p.professionalRole || u?.roleName || "Nhân viên chuyên môn",
                        startDate: p.startDate || dates?.joinDate || "",
                        contractEndDate: p.contractEndDate || dates?.contractEndDate || "",
                        standardHoursPerWeek: p.standardHoursPerWeek || 40,
                    };
                });
                setProfiles(mapped);
            } else {
                setProfiles([]);
            }
        } catch (err: any) {
            console.warn("Không thể tải danh sách hồ sơ từ API:", err);
            setProfiles([]);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadProfiles();
    }, []);

    const resolvedProfiles = useMemo(() => {
        if (!isSelfOnly) return profiles;
        const selfList = profiles.filter((p) => {
            if (currentUser?.employeeCode && p.employeeCode === currentUser.employeeCode) return true;
            if (currentUser?.id && p.employeeId === currentUser.id) return true;
            if (currentUser?.username && p.username === currentUser.username) return true;
            if (currentUser?.fullName && p.fullName === currentUser.fullName) return true;
            return false;
        });
        if (selfList.length > 0) return selfList;
        if (currentUser) {
            return [{
                id: `self-${currentUser.id}`,
                employeeCode: currentUser.employeeCode || `EMP-${currentUser.id}`,
                fullName: currentUser.fullName || currentUser.username,
                email: currentUser.email || "",
                username: currentUser.username,
                department: currentUser.orgUnitName || "Chưa phân bổ",
                professionalRole: currentUser.roleName || "Nhân viên chuyên môn",
                startDate: "2024-01-01",
                standardHoursPerWeek: 40,
                employeeId: currentUser.id,
            }];
        }
        return [];
    }, [profiles, isSelfOnly, currentUser]);

    const filtered = resolvedProfiles.filter((p) =>
        p.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        p.employeeCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (p.email && p.email.toLowerCase().includes(searchTerm.toLowerCase())) ||
        p.department.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const showNotification = (type: "success" | "error", message: string) => {
        setNotification({ type, message });
        setTimeout(() => setNotification(null), 3500);
    };

    const handleOpenAdd = () => {
        setEditingProfile(undefined);
        setIsFormOpen(true);
    };

    const handleOpenEdit = (profile: HrProfileData) => {
        setEditingProfile(profile);
        setIsFormOpen(true);
    };

    const handleDelete = (id: string) => {
        setProfiles((prev) => prev.filter((p) => p.id !== id));
        showNotification("success", "Đã xóa hồ sơ nhân sự thành công.");
    };

    const handleSave = async (data: HrProfileData) => {
        try {
            if (editingProfile && editingProfile.employeeId) {
                await updateEmployeeProfile(editingProfile.employeeId, {
                    version: 0,
                    fullName: data.fullName,
                    orgUnitId: data.orgUnitId ? Number(data.orgUnitId) : 1,
                    professionalRole: data.professionalRole,
                    startDate: data.startDate,
                    contractEndDate: data.contractEndDate,
                    standardHoursPerWeek: data.standardHoursPerWeek,
                });
                showNotification("success", `Đã cập nhật hồ sơ ${data.fullName} thành công.`);
            } else {
                showNotification("error", "Việc tạo tài khoản và hồ sơ nhân sự mới được thực hiện tại mục Quản lý tài khoản (dành cho Quản trị viên VT-06).");
                return;
            }
            await loadProfiles();
        } catch (err: any) {
            showNotification("error", err?.message || "Thao tác thất bại.");
        }
        setIsFormOpen(false);
    };

    const nextCode = `EMP-${String(profiles.length + 1).padStart(3, "0")}`;

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                <div>
                    <h1 className="text-2xl font-extrabold tracking-tight text-slate-900">Hồ sơ nhân sự</h1>
                    <p className="mt-1 text-xs font-semibold text-slate-500 sm:text-sm">
                        Quản lý thông tin hành chính, hợp đồng và định mức giờ làm việc của nhân viên.
                    </p>
                </div>
            </div>

            {/* Notification */}
            {notification && (
                <div className={`flex items-center justify-between rounded-2xl border p-4 text-xs font-semibold shadow-xs transition animate-fadeIn ${
                    notification.type === "success"
                        ? "border-emerald-200 bg-emerald-50/90 text-emerald-800"
                        : "border-rose-200 bg-rose-50/90 text-rose-800"
                }`}>
                    <div className="flex items-center gap-2.5">
                        {notification.type === "success"
                            ? <Check className="size-4 shrink-0 text-emerald-600" />
                            : <AlertTriangle className="size-4 shrink-0 text-rose-600" />}
                        <span>{notification.message}</span>
                    </div>
                    <button type="button" onClick={() => setNotification(null)} className="rounded-lg p-1 hover:bg-black/5 text-slate-500 transition">
                        <X className="size-3.5" />
                    </button>
                </div>
            )}

            {/* Main card */}
            <div className="rounded-2xl border border-slate-200/90 bg-white p-5 shadow-xs space-y-4">
                {/* Toolbar */}
                <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                    <div className="relative flex-1">
                        <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
                        <input
                            type="text"
                            placeholder="Tìm kiếm theo tên, mã NV, SĐT hoặc phòng ban..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-10 pr-4 text-xs font-medium text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                        />
                    </div>
                </div>

                {/* List */}
                <div className="space-y-3 pt-1">
                    {isLoading && (
                        <div className="flex flex-col items-center justify-center py-12 text-slate-500">
                            <Loader2 className="size-8 animate-spin text-indigo-600 mb-2" />
                            <p className="text-xs font-semibold">Đang tải danh sách hồ sơ...</p>
                        </div>
                    )}
                    {!isLoading && filtered.length === 0 && searchTerm && (
                        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50/40 p-10 text-center">
                            <Search className="size-8 text-slate-300" />
                            <h3 className="mt-3 text-sm font-bold text-slate-800">Không tìm thấy kết quả</h3>
                            <p className="mt-1 text-xs text-slate-500">Không có hồ sơ nào khớp với từ khoá tìm kiếm.</p>
                            <button type="button" onClick={() => setSearchTerm("")} className="mt-3 rounded-xl border border-slate-200 bg-white px-3.5 py-1.5 text-xs font-semibold text-indigo-600 shadow-2xs hover:bg-slate-50">
                                Xóa tìm kiếm
                            </button>
                        </div>
                    )}
                    {!isLoading && filtered.length === 0 && !searchTerm && (
                        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50/40 p-12 text-center">
                            <div className="flex size-14 items-center justify-center rounded-2xl border border-indigo-100 bg-indigo-50 text-indigo-600 shadow-2xs">
                                <Users className="size-7" />
                            </div>
                            <h3 className="mt-4 text-base font-bold text-slate-900">Chưa có hồ sơ nhân sự nào</h3>
                            <p className="mt-1 max-w-sm text-xs text-slate-500 leading-relaxed">
                                {canManage ? "Nhấn nút bên dưới để tạo hồ sơ đầu tiên." : "Hệ thống chưa có hồ sơ nhân sự nào để hiển thị."}
                            </p>
                            {canManage && (
                                <button type="button" onClick={handleOpenAdd} className="mt-5 flex items-center gap-1.5 rounded-xl border border-indigo-600 bg-indigo-600 px-4 py-2.5 text-xs font-bold text-white shadow-xs transition hover:bg-indigo-700 active:scale-95">
                                    <Plus className="size-4 stroke-[2.5]" />
                                    <span>Tạo hồ sơ mới</span>
                                </button>
                            )}
                        </div>
                    )}
                    {filtered.map((p) => (
                        <HrProfileCard key={p.id} profile={p} canManage={canManage} onEdit={handleOpenEdit} onDelete={handleDelete} />
                    ))}
                </div>
            </div>

            {/* Form modal */}
            <HrProfileForm
                open={isFormOpen}
                initialData={editingProfile}
                nextEmployeeCode={nextCode}
                onClose={() => setIsFormOpen(false)}
                onSave={handleSave}
            />
        </div>
    );
}
