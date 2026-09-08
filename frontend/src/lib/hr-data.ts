export type EmployeeStatus = "active" | "inactive"

export interface Employee {
    id: string
    name: string
    email: string
    department: string
    role: string
    status: EmployeeStatus
    phone: string
}

export type AttendanceStatus = "Đúng giờ" | "Đi muộn" | "Vắng mặt" | "Đã điều chỉnh"

export interface AttendanceRecord {
    id: string
    name: string
    dept: string
    inTime: string
    outTime: string
    hours: string
    ot: string
    status: AttendanceStatus
}

export const DEPARTMENTS = [
    "Nhân sự",
    "Công nghệ thông tin",
    "Kế toán",
    "Marketing",
    "Kinh doanh",
] as const

export const INITIAL_EMPLOYEES: Employee[] = [
    {
        id: "EMP-002",
        name: "Nguyễn Thị Mai",
        email: "mai.nguyen@company.com",
        department: "Nhân sự",
        role: "HR Specialist",
        status: "active",
        phone: "0988 123 456",
    },
    {
        id: "EMP-001",
        name: "Trần Văn Hùng",
        email: "hung.tran@company.com",
        department: "Công nghệ thông tin",
        role: "Senior Software Engineer",
        status: "active",
        phone: "0912 888 999",
    },
    {
        id: "EMP-003",
        name: "Lê Hoàng Nam",
        email: "nam.le@company.com",
        department: "Kinh doanh",
        role: "Sales Director",
        status: "active",
        phone: "0933 555 777",
    },
    {
        id: "EMP-004",
        name: "Phạm Minh Anh",
        email: "anh.pham@company.com",
        department: "Marketing",
        role: "Content Marketing Lead",
        status: "active",
        phone: "0977 111 222",
    },
    {
        id: "EMP-005",
        name: "Đỗ Thanh Tùng",
        email: "tung.do@company.com",
        department: "Kế toán",
        role: "Kế toán trưởng",
        status: "inactive",
        phone: "0904 333 444",
    },
    {
        id: "EMP-006",
        name: "Vũ Phương Thảo",
        email: "thao.vu@company.com",
        department: "Nhân sự",
        role: "HR Manager",
        status: "active",
        phone: "0982 666 888",
    },
]

export const INITIAL_ATTENDANCE: AttendanceRecord[] = [
    { id: "EMP-002", name: "Nguyễn Thị Mai", dept: "Nhân sự", inTime: "07:55 AM", outTime: "05:35 PM", hours: "8.0 hrs", ot: "0.5 hrs", status: "Đúng giờ" },
    { id: "EMP-001", name: "Trần Văn Hùng", dept: "Công nghệ thông tin", inTime: "08:10 AM", outTime: "07:00 PM", hours: "8.0 hrs", ot: "1.5 hrs", status: "Đúng giờ" },
    { id: "EMP-003", name: "Lê Hoàng Nam", dept: "Kinh doanh", inTime: "08:25 AM", outTime: "05:30 PM", hours: "7.5 hrs", ot: "0.0 hrs", status: "Đi muộn" },
    { id: "EMP-004", name: "Phạm Minh Anh", dept: "Marketing", inTime: "07:50 AM", outTime: "05:30 PM", hours: "8.0 hrs", ot: "0.0 hrs", status: "Đúng giờ" },
    { id: "EMP-005", name: "Đỗ Thanh Tùng", dept: "Kế toán", inTime: "-- : --", outTime: "-- : --", hours: "0.0 hrs", ot: "0.0 hrs", status: "Vắng mặt" },
    { id: "EMP-006", name: "Vũ Phương Thảo", dept: "Nhân sự", inTime: "07:48 AM", outTime: "06:15 PM", hours: "8.0 hrs", ot: "0.75 hrs", status: "Đúng giờ" },
]

export interface WeeklyAvailability {
    id: string
    employeeId: string
    employeeName: string
    dayOfWeek: string
    startTime: string
    endTime: string
    totalHours: string
    notes: string
}

export type TabKey =
    | "dashboard"
    | "employees"
    | "attendance"
    | "leave"
    | "departments"
    | "reports"
    | "permissions"
    | "settings"
    | "weeklyHours"

export const TAB_TITLES: Record<TabKey, string> = {
    dashboard: "Tổng quan",
    employees: "Quản lý Nhân sự",
    attendance: "Chấm công",
    leave: "Quản lý Nghỉ phép",
    departments: "Cơ cấu Phòng ban",
    reports: "Báo cáo Thống kê",
    permissions: "Phân quyền Truy cập",
    settings: "Thiết lập Hệ thống",
    weeklyHours: "Khai báo giờ khả dụng theo tuần",
}

export const INITIAL_WEEKLY_AVAILABILITY: WeeklyAvailability[] = [
    { id: "W001", employeeId: "EMP-002", employeeName: "Nguyễn Thị Mai", dayOfWeek: "Thứ Hai", startTime: "08:00 AM", endTime: "05:00 PM", totalHours: "9.0", notes: "Có sẵn cả ngày" },
    { id: "W002", employeeId: "EMP-002", employeeName: "Nguyễn Thị Mai", dayOfWeek: "Thứ Ba", startTime: "08:00 AM", endTime: "05:00 PM", totalHours: "9.0", notes: "" },
    { id: "W003", employeeId: "EMP-002", employeeName: "Nguyễn Thị Mai", dayOfWeek: "Thứ Tư", startTime: "08:00 AM", endTime: "05:00 PM", totalHours: "9.0", notes: "" },
    { id: "W004", employeeId: "EMP-002", employeeName: "Nguyễn Thị Mai", dayOfWeek: "Thứ Năm", startTime: "08:00 AM", endTime: "05:00 PM", totalHours: "9.0", notes: "" },
    { id: "W005", employeeId: "EMP-002", employeeName: "Nguyễn Thị Mai", dayOfWeek: "Thứ Sáu", startTime: "08:00 AM", endTime: "03:00 PM", totalHours: "7.0", notes: "Sớm xin phép" },
    { id: "W006", employeeId: "EMP-001", employeeName: "Trần Văn Hùng", dayOfWeek: "Thứ Hai", startTime: "07:00 AM", endTime: "06:00 PM", totalHours: "11.0", notes: "" },
    { id: "W007", employeeId: "EMP-001", employeeName: "Trần Văn Hùng", dayOfWeek: "Thứ Ba", startTime: "07:00 AM", endTime: "06:00 PM", totalHours: "11.0", notes: "" },
]
