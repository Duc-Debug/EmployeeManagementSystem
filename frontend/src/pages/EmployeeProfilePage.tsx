import { useState, useRef, useEffect } from "react";
import { Plus, Search, Filter, ChevronDown, Check } from "lucide-react";
import { cn } from "@/lib/utils";
import EmployeeCard from "../components/employee/EmployeeCard";
import EmployeeProfileForm from "../components/employee/form/EmployeeProfileForm";
import type { EmployeeFormData } from "../components/employee/form/employeeForm.types";

const INITIAL_EMPLOYEES: EmployeeFormData[] = [
    {
        id: "EMP-002",
        fullName: "Nguyễn Thị Mai",
        email: "mai.nguyen@company.com",
        department: "Nhân sự",
        position: "HR Specialist",
        joinDate: "2023-06-01",
        standardHoursPerDay: 8,
    },
];

const DEPARTMENT_OPTIONS = ["All", "Kỹ thuật", "Nhân sự", "Kinh doanh", "Marketing", "Tài chính"];

function DepartmentFilterDropdown({
                                      value,
                                      onChange,
                                  }: {
    value: string;
    onChange: (v: string) => void;
}) {
    const [open, setOpen] = useState(false);
    const ref = useRef<HTMLDivElement>(null);

    useEffect(() => {
        function handleClickOutside(e: MouseEvent) {
            if (ref.current && !ref.current.contains(e.target as Node)) {
                setOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const label = value === "All" ? "Tất cả phòng ban" : value;

    return (
        <div className="relative" ref={ref}>
            <button
                type="button"
                onClick={() => setOpen((o) => !o)}
                className="flex items-center gap-2 rounded-full border border-white/30 bg-white/15 px-5 py-2.5 text-xs font-semibold text-white backdrop-blur-sm transition hover:bg-white/25 active:scale-95"
            >
                <span>{label}</span>
                <ChevronDown
                    className={cn(
                        "h-3.5 w-3.5 text-white/70 transition-transform",
                        open && "rotate-180"
                    )}
                />
            </button>

            {open && (
                <div className="absolute right-0 z-20 mt-2 w-48 overflow-hidden rounded-xl border border-white/15 bg-white/10 p-1 shadow-xl backdrop-blur-xl">
                    {DEPARTMENT_OPTIONS.map((opt) => {
                        const isActive = value === opt;
                        return (
                            <button
                                key={opt}
                                type="button"
                                onClick={() => {
                                    onChange(opt);
                                    setOpen(false);
                                }}
                                className={cn(
                                    "flex w-full items-center justify-between rounded-lg px-3 py-2 text-left text-xs font-medium transition",
                                    isActive
                                        ? "bg-white/20 text-white"
                                        : "text-white/60 hover:bg-white/10 hover:text-white"
                                )}
                            >
                                {opt === "All" ? "Tất cả phòng ban" : opt}
                                {isActive && <Check className="h-3.5 w-3.5" />}
                            </button>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

export default function EmployeeProfilePage() {
    const [employees, setEmployees] = useState<EmployeeFormData[]>(INITIAL_EMPLOYEES);
    const [searchTerm, setSearchTerm] = useState("");
    const [selectedDept, setSelectedDept] = useState("All");
    const [isFormOpen, setIsFormOpen] = useState(false);
    const [editingEmployee, setEditingEmployee] = useState<EmployeeFormData | null>(null);

    const filteredEmployees = employees.filter((emp) => {
        const matchesSearch =
            emp.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
            emp.email.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesDept = selectedDept === "All" || emp.department === selectedDept;
        return matchesSearch && matchesDept;
    });

    const handleOpenAdd = () => {
        setEditingEmployee(null);
        setIsFormOpen(true);
    };

    const handleOpenEdit = (emp: EmployeeFormData) => {
        setEditingEmployee(emp);
        setIsFormOpen(true);
    };

    const handleDelete = (id?: string) => {
        if (!id) return;
        if (confirm("Bạn có chắc chắn muốn xóa hồ sơ nhân sự này?")) {
            setEmployees(employees.filter((e) => e.id !== id));
        }
    };

    const handleSave = (data: EmployeeFormData) => {
        if (editingEmployee?.id) {
            setEmployees(
                employees.map((e) => (e.id === editingEmployee.id ? { ...data, id: editingEmployee.id } : e))
            );
        } else {
            const newEmp: EmployeeFormData = {
                ...data,
                id: `EMP-${Date.now().toString().slice(-3)}`,
            };
            setEmployees([newEmp, ...employees]);
        }
        setIsFormOpen(false);
    };

    return (
        <div className="flex-1 space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-xl font-bold text-white">Quản lý hồ sơ nhân sự</h1>
                    <p className="text-xs text-white/60">Khai báo, cập nhật và quản lý danh sách hồ sơ nhân sự HR.</p>
                </div>
            </div>

            {/* Ô tìm kiếm chiếm trọn chiều rộng */}
            <div className="relative w-full">
                <Search className="absolute left-3.5 top-2.5 h-4 w-4 text-white/40" />
                <input
                    type="text"
                    placeholder="Tìm kiếm theo tên hoặc email..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full rounded-full border border-white/15 bg-white/5 pl-10 pr-4 py-2.5 text-xs text-white placeholder:text-white/40 outline-none focus:border-[#63ecc8]"
                />
            </div>

            {/* Bộ lọc + nút thêm nằm hàng riêng bên dưới */}
            <div className="flex flex-wrap items-center justify-end gap-3">
                <div className="flex items-center gap-2">
                    <Filter className="h-4 w-4 text-purple-300/70" />
                    <DepartmentFilterDropdown value={selectedDept} onChange={setSelectedDept} />
                </div>
                <button
                    type="button"
                    onClick={handleOpenAdd}
                    className="flex items-center gap-2 rounded-full border border-white/30 bg-white/15 px-5 py-2.5 text-xs font-semibold text-white backdrop-blur-sm transition hover:bg-white/25 active:scale-95"
                >
                    <Plus className="h-4 w-4 stroke-[2.5]" />
                    <span>Thêm nhân sự mới</span>
                </button>
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                {filteredEmployees.map((emp) => (
                    <EmployeeCard
                        key={emp.id}
                        employee={emp}
                        onEdit={handleOpenEdit}
                        onDelete={handleDelete}
                    />
                ))}
            </div>
            <EmployeeProfileForm
                open={isFormOpen}
                initialData={editingEmployee}
                onClose={() => setIsFormOpen(false)}
                onSave={handleSave}
            />
        </div>
    );
}