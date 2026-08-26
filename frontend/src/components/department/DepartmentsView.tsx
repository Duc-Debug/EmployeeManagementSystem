import { useState } from "react";
import { List, Network } from "lucide-react";
import { cn } from "@/lib/utils";
import DepartmentList from "./DepartmentList";
import DepartmentModal, { type Department } from "./DepartmentModal";
import OrgChart from "./OrgChart"; // Chuyển OrgChart.tsx vào cùng thư mục department/

const INITIAL_DEPARTMENTS: Department[] = [
    { id: "dept-1", name: "Phòng Nhân sự", manager: "Nguyễn Minh Anh", count: 12 },
    { id: "dept-2", name: "Phòng Công nghệ", manager: "Trần Quốc Bảo", count: 45 },
    { id: "dept-3", name: "Phòng Marketing", manager: "Lê Thu Hà", count: 18 },
    { id: "dept-4", name: "Phòng Kinh doanh", manager: "Phạm Hoàng Nam", count: 32 },
    { id: "dept-5", name: "Phòng Tài chính", manager: "Võ Ngọc Linh", count: 8 },
];

export default function DepartmentsView() {
    const [departmentSubTab, setDepartmentSubTab] = useState<"list" | "tree">("list");
    const [departments, setDepartments] = useState<Department[]>(INITIAL_DEPARTMENTS);
    const [modalOpen, setModalOpen] = useState(false);
    const [editingDept, setEditingDept] = useState<Department | null>(null);

    function openAddModal() {
        setEditingDept(null);
        setModalOpen(true);
    }

    function openEditModal(dept: Department) {
        setEditingDept(dept);
        setModalOpen(true);
    }

    function handleSave(dept: Department) {
        setDepartments((prev) => {
            const exists = prev.some((d) => d.id === dept.id);
            return exists ? prev.map((d) => (d.id === dept.id ? dept : d)) : [...prev, dept];
        });
        setModalOpen(false);
    }

    function handleDelete(id: string) {
        setDepartments((prev) => prev.filter((d) => d.id !== id));
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