export interface Employee {
    id: string;
    name: string;
    position?: string;
}

/**
 * Danh sách nhân sự dùng để chọn "Người quản lý" cho phòng ban / đơn vị.
 *
 * TODO: Thay thế bằng dữ liệu thực tế lấy từ API nhân sự, ví dụ:
 *   GET /api/employees  ->  { id, name, position }[]
 * Có thể tạo hàm `getEmployees()` trong `@/lib/api/employees`
 * theo cùng khuôn mẫu với `getOrgTree()` ở `@/lib/api/org-units`.
 */
export const MOCK_EMPLOYEES: Employee[] = [
    { id: "emp-01", name: "Trần Quốc Bảo", position: "Trưởng khối Kỹ thuật & Công nghệ" },
    { id: "emp-02", name: "Nguyễn Văn A", position: "Trưởng phòng Lập trình Frontend" },
    { id: "emp-03", name: "Lê Thị Mai", position: "Trưởng nhóm UI/UX & Design System" },
    { id: "emp-04", name: "Lê Văn C", position: "Trưởng phòng Lập trình Backend" },
    { id: "emp-05", name: "Vũ Hoàng D", position: "Trưởng nhóm Cloud & DevOps" },
    { id: "emp-06", name: "Nguyễn Minh Anh", position: "Trưởng khối Vận hành & Nhân sự" },
    { id: "emp-07", name: "Phạm Mai E", position: "Trưởng phòng Nhân sự & Tuyển dụng" },
    { id: "emp-08", name: "Đỗ Thị G", position: "Trưởng phòng Hành chính & Quản trị" },
    { id: "emp-09", name: "Lê Thu Hà", position: "Trưởng khối Kinh doanh & Marketing" },
    { id: "emp-10", name: "Phạm Hoàng Nam", position: "Trưởng phòng Phát triển Kinh doanh" },
    { id: "emp-11", name: "Võ Ngọc Linh", position: "Trưởng phòng Truyền thông & Marketing" },
];