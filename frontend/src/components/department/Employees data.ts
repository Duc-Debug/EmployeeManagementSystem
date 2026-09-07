export interface Employee {
    id: string;
    name: string;
    position?: string;
}

/**
 * @deprecated Danh sách nhân sự giả lập. Hiện tại hệ thống đã lấy dữ liệu thật từ API GET /api/v1/users.
 */
export const MOCK_EMPLOYEES: Employee[] = [];
