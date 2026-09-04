import { useState } from "react";
import DepartmentsView from "./department/DepartmentsView";
import SkilldeclarationView from "./skilldeclaration/SkilldeclarationView";
import type { ResourceEmployee } from "./skilldeclaration/SkillresourceSearch";
import { INITIAL_DEPARTMENTS } from "./department/department.constants";
import type { Department } from "./department/DepartmentModal";

// Dữ liệu nhân sự mẫu
const INITIAL_EMPLOYEES: ResourceEmployee[] = [
    {
        id: 'emp-001',
        code: 'NV-014',
        name: 'Nguyễn Văn An',
        title: 'Senior Backend Developer',
        department: 'Phòng Công nghệ',
        availability: 'busy',
        availabilityPercent: 0,
        skills: [
            { skillId: 'java', name: 'Java', level: 5 },
            { skillId: 'docker', name: 'Docker', level: 4 },
            { skillId: 'kubernetes', name: 'Kubernetes', level: 3 },
        ],
    },
    {
        id: 'emp-002',
        code: 'NV-027',
        name: 'Trần Thị Bích',
        title: 'Frontend Developer',
        department: 'Phòng Công nghệ',
        availability: 'full',
        availabilityPercent: 100,
        skills: [
            { skillId: 'react', name: 'React.js', level: 5 },
            { skillId: 'typescript', name: 'TypeScript', level: 4 },
            { skillId: 'nodejs', name: 'Node.js', level: 3 },
        ],
    },
    {
        id: 'emp-003',
        code: 'NV-055',
        name: 'Lê Hoàng Nam',
        title: 'DevOps Engineer',
        department: 'Phòng Nhân sự',
        availability: 'partial',
        availabilityPercent: 40,
        skills: [
            { skillId: 'docker', name: 'Docker', level: 5 },
            { skillId: 'kubernetes', name: 'Kubernetes', level: 5 },
            { skillId: 'aws', name: 'AWS', level: 4 },
        ],
    },
    {
        id: 'emp-004',
        code: 'NV-061',
        name: 'Phạm Thu Hà',
        title: 'QA Engineer',
        department: 'Phòng Marketing',
        availability: 'full',
        availabilityPercent: 100,
        skills: [
            { skillId: 'selenium', name: 'Selenium', level: 4 },
            { skillId: 'sql', name: 'SQL', level: 3 },
            { skillId: 'java', name: 'Java', level: 2 },
        ],
    },
    {
        id: 'emp-005',
        code: 'NV-072',
        name: 'Vũ Đức Minh',
        title: 'Fullstack Developer',
        department: 'Phòng Kinh doanh',
        availability: 'partial',
        availabilityPercent: 20,
        skills: [
            { skillId: 'react', name: 'React.js', level: 4 },
            { skillId: 'nodejs', name: 'Node.js', level: 5 },
            { skillId: 'typescript', name: 'TypeScript', level: 3 },
        ],
    },
    {
        id: 'emp-006',
        code: 'NV-088',
        name: 'Đỗ Ngọc Lan',
        title: 'Data Engineer',
        department: 'Phòng Tài chính',
        availability: 'busy',
        availabilityPercent: 0,
        skills: [
            { skillId: 'python', name: 'Python', level: 5 },
            { skillId: 'sql', name: 'SQL', level: 5 },
            { skillId: 'aws', name: 'AWS', level: 3 },
        ],
    },
];

export default function MainLayout() {
    const [activeTab, setActiveTab] = useState<"skills" | "departments">("skills");

    // State danh sách phòng ban và nhân sự chung cho toàn ứng dụng
    const [departments, setDepartments] = useState<Department[]>(INITIAL_DEPARTMENTS);
    const [employees, setEmployees] = useState<ResourceEmployee[]>(INITIAL_EMPLOYEES);

    // Xử lý Thêm/Sửa phòng ban
    const handleSaveDepartment = (dept: Department) => {
        const oldDept = departments.find((d) => d.id === dept.id);

        setDepartments((prev) => {
            const exists = prev.some((d) => d.id === dept.id);
            return exists ? prev.map((d) => (d.id === dept.id ? dept : d)) : [...prev, dept];
        });

        // Nếu tên phòng ban thay đổi, cập nhật lại tên phòng ban cho các nhân sự
        // thuộc phòng đó để đồng bộ với mục "Tra cứu & Tìm kiếm nhân sự"
        if (oldDept && oldDept.name !== dept.name) {
            setEmployees((prev) =>
                prev.map((emp) =>
                    emp.department === oldDept.name ? { ...emp, department: dept.name } : emp
                )
            );
        }
    };

    // Xử lý Xóa phòng ban
    const handleDeleteDepartment = (id: string) => {
        const deletedDept = departments.find((d) => d.id === id);
        setDepartments((prev) => prev.filter((d) => d.id !== id));

        // Cập nhật lại nhân sự nếu phòng ban bị xóa
        if (deletedDept) {
            setEmployees((prev) =>
                prev.filter((emp) => emp.department !== deletedDept.name)
            );
        }
    };

    return (
        <div className="flex min-h-screen bg-slate-900">
            {/* Sidebar điều hướng */}
            <aside className="w-64 border-r border-white/10 p-4">
                <button
                    onClick={() => setActiveTab("skills")}
                    className={`w-full text-left p-2 rounded-lg ${
                        activeTab === "skills" ? "bg-white/20 text-white" : "text-white/60"
                    }`}
                >
                    Khai báo kỹ năng
                </button>
                <button
                    onClick={() => setActiveTab("departments")}
                    className={`w-full text-left p-2 rounded-lg ${
                        activeTab === "departments" ? "bg-white/20 text-white" : "text-white/60"
                    }`}
                >
                    Phòng ban
                </button>
            </aside>

            {/* Nội dung chính */}
            <main className="flex-1 p-6">
                {activeTab === "skills" && (
                    <SkilldeclarationView
                        departments={departments}
                        employees={employees}
                    />
                )}

                {activeTab === "departments" && (
                    <DepartmentsView
                        departments={departments}
                        onSaveDepartment={handleSaveDepartment}
                        onDeleteDepartment={handleDeleteDepartment}
                    />
                )}
            </main>
        </div>
    );
}