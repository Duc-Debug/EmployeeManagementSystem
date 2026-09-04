import { useState, useEffect } from "react";
import { List, Network } from "lucide-react";
import { cn } from "@/lib/utils";
import DepartmentList from "./DepartmentList";
import DepartmentModal from "./DepartmentModal";
import OrgChart from "./OrgChart";
import { type Department, INITIAL_DEPARTMENTS } from "./department.constants";

interface DepartmentsViewProps {
    departments?: Department[];
    onSaveDepartment?: (dept: Department) => void;
    onDeleteDepartment?: (id: string) => void;
}

export default function DepartmentsView({
                                            departments: propsDepartments,
                                            onSaveDepartment,
                                            onDeleteDepartment,
                                        }: DepartmentsViewProps) {
    const [departmentSubTab, setDepartmentSubTab] = useState<"list" | "tree">("list");

    // Khởi tạo state nội bộ với props truyền vào hoặc dữ liệu mặc định
    const [departments, setDepartments] = useState<Department[]>(
        propsDepartments && propsDepartments.length > 0 ? propsDepartments : INITIAL_DEPARTMENTS
    );
    const [modalOpen, setModalOpen] = useState(false);
    const [editingDept, setEditingDept] = useState<Department | null>(null);

    // Cập nhật lại state khi props từ MainLayout thay đổi
    useEffect(() => {
        if (propsDepartments && propsDepartments.length > 0) {
            setDepartments(propsDepartments);
        }
    }, [propsDepartments]);

    function openAddModal() {
        setEditingDept(null);
        setModalOpen(true);
    }

    function openEditModal(dept: Department) {
        setEditingDept(dept);
        setModalOpen(true);
    }

    function handleSave(dept: Department) {
        // 1. Cập nhật state nội bộ để UI hiển thị ngay lập tức
        setDepartments((prev) => {
            const exists = prev.some((d) => d.id === dept.id);
            if (exists) {
                return prev.map((d) => (d.id === dept.id ? dept : d));
            }
            return [...prev, dept];
        });

        // 2. Truyền dữ liệu lên MainLayout / Backend (nếu có)
        if (onSaveDepartment) {
            onSaveDepartment(dept);
        }

        setModalOpen(false);
    }

    function handleDelete(id: string) {
        // 1. Cập nhật state nội bộ để xóa khỏi danh sách
        setDepartments((prev) => prev.filter((d) => d.id !== id));

        // 2. Gọi callback xóa lên parent
        if (onDeleteDepartment) {
            onDeleteDepartment(id);
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4 border-b border-white/15 pb-4">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-white">
                        Quản lý phòng ban
                    </h1>
                    <p className="text-sm text-white/60">
                        Xem và điều chỉnh sơ đồ cơ cấu tổ chức doanh nghiệp
                    </p>
                </div>
                <div className="flex rounded-xl border border-white/15 bg-white/[0.07] p-1 backdrop-blur-xl">
                    <button
                        type="button"
                        onClick={() => setDepartmentSubTab("list")}
                        className={cn(
                            "flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-bold transition",
                            departmentSubTab === "list"
                                ? "bg-white/20 text-white"
                                : "text-white/60 hover:text-white"
                        )}
                    >
                        <List className="h-4 w-4" />
                        Danh sách phòng ban
                    </button>
                    <button
                        type="button"
                        onClick={() => setDepartmentSubTab("tree")}
                        className={cn(
                            "flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-bold transition",
                            departmentSubTab === "tree"
                                ? "bg-white/20 text-white"
                                : "text-white/60 hover:text-white"
                        )}
                    >
                        <Network className="h-4 w-4" />
                        Sơ đồ tổ chức
                    </button>
                </div>
            </div>

            {departmentSubTab === "list" ? (
                <DepartmentList
                    departments={departments}
                    onAdd={openAddModal}
                    onEdit={openEditModal}
                    onDelete={handleDelete}
                />
            ) : (
                <div className="h-[750px] overflow-hidden rounded-2xl border border-white/15 bg-white/[0.05] p-2 backdrop-blur-xl">
                    <OrgChart />
                </div>
            )}

            <DepartmentModal
                open={modalOpen}
                initialData={editingDept}
                onClose={() => setModalOpen(false)}
                onSave={handleSave}
            />
        </div>
    );
}