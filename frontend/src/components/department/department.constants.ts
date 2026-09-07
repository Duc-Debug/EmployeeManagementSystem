import type { Department } from "./DepartmentModal";

export const INITIAL_DEPARTMENTS: Department[] = [
    { id: "dept-1", name: "Phòng Nhân sự", managerId: "emp-06", managerName: "Nguyễn Minh Anh", parentId: null, parentName: null },
    { id: "dept-2", name: "Phòng Công nghệ", managerId: "emp-01", managerName: "Trần Quốc Bảo", parentId: null, parentName: null },
    { id: "dept-3", name: "Phòng Marketing", managerId: "emp-09", managerName: "Lê Thu Hà", parentId: null, parentName: null },
    { id: "dept-4", name: "Phòng Kinh doanh", managerId: "emp-10", managerName: "Phạm Hoàng Nam", parentId: null, parentName: null },
    { id: "dept-5", name: "Phòng Tài chính", managerId: "emp-11", managerName: "Võ Ngọc Linh", parentId: null, parentName: null },
];