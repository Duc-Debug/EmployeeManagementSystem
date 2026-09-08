export interface TaskItem {
    id: string;
    code: string;
    name: string;
    assigneeId: string;
    priority: 'Cao' | 'Trung bình' | 'Thấp';
    hours: number;
    budgetHours?: number; // Ngân sách giờ công do PM đặt
    actualHours?: number; // Giờ công thực tế đã duyệt
    status: 'Hoàn thành' | 'Đang làm' | 'Chờ duyệt' | 'Chưa làm';
    startWeek: string;
    endWeek: string;
}

export interface TaskCategoryGroup {
    id: string;
    code: string;
    name: string;
    lead: string;
    progress: number;
    color: string;
    tasks: TaskItem[];
}

export interface ProjectMember {
    id: string;
    name: string;
    role: string;
    avatar: string;
    capacity: number; // e.g. 40h/week
    weeklyHours: Record<string, number>; // e.g. { W1: 40, W2: 35, W3: 20... }
}

export interface MonthWeek {
    key: string;
    label: string;
    dates: string;
    isCurrent: boolean;
}

export interface ProjectMonth {
    id: string;
    name: string;
    weeks: MonthWeek[];
}

export const INITIAL_MONTHS_LIST: ProjectMonth[] = [
    {
        id: '2026-09',
        name: 'Tháng 09/2026',
        weeks: [
            { key: 'W1', label: 'Tuần 1', dates: '01/09 - 07/09', isCurrent: false },
            { key: 'W2', label: 'Tuần 2', dates: '08/09 - 14/09', isCurrent: true },
            { key: 'W3', label: 'Tuần 3', dates: '15/09 - 21/09', isCurrent: false },
            { key: 'W4', label: 'Tuần 4', dates: '22/09 - 28/09', isCurrent: false },
            { key: 'W5', label: 'Tuần 5', dates: '29/09 - 30/09', isCurrent: false }
        ]
    },
    {
        id: '2026-10',
        name: 'Tháng 10/2026',
        weeks: [
            { key: 'W1', label: 'Tuần 1', dates: '01/10 - 07/10', isCurrent: false },
            { key: 'W2', label: 'Tuần 2', dates: '08/10 - 14/10', isCurrent: false },
            { key: 'W3', label: 'Tuần 3', dates: '15/10 - 21/10', isCurrent: false },
            { key: 'W4', label: 'Tuần 4', dates: '22/10 - 28/10', isCurrent: false },
            { key: 'W5', label: 'Tuần 5', dates: '29/10 - 31/10', isCurrent: false }
        ]
    }
];

export const INITIAL_PROJECT_MEMBERS: ProjectMember[] = [
    {
        id: 'm1',
        name: 'Trần Lan Anh',
        role: 'Product Owner / BA',
        avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&auto=format&fit=crop&q=80',
        capacity: 40,
        weeklyHours: { W1: 40, W2: 35, W3: 20, W4: 15, W5: 10 }
    },
    {
        id: 'm2',
        name: 'Hoàng Nam',
        role: 'UI/UX Designer',
        avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&auto=format&fit=crop&q=80',
        capacity: 40,
        weeklyHours: { W1: 38, W2: 40, W3: 38, W4: 25, W5: 15 }
    },
    {
        id: 'm3',
        name: 'Lê Quốc Bảo',
        role: 'Backend Dev',
        avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100&auto=format&fit=crop&q=80',
        capacity: 40,
        weeklyHours: { W1: 35, W2: 42, W3: 48, W4: 40, W5: 30 }
    },
    {
        id: 'm4',
        name: 'Vũ Tuấn Kiệt',
        role: 'Backend Dev',
        avatar: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=100&auto=format&fit=crop&q=80',
        capacity: 40,
        weeklyHours: { W1: 30, W2: 35, W3: 32, W4: 38, W5: 35 }
    },
    {
        id: 'm5',
        name: 'Nguyễn Đức Anh',
        role: 'Frontend Dev',
        avatar: 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=100&auto=format&fit=crop&q=80',
        capacity: 40,
        weeklyHours: { W1: 36, W2: 40, W3: 40, W4: 38, W5: 36 }
    },
    {
        id: 'm6',
        name: 'Đặng Thùy Trang',
        role: 'QA / QC Tester',
        avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80',
        capacity: 40,
        weeklyHours: { W1: 20, W2: 28, W3: 30, W4: 40, W5: 40 }
    }
];

export const INITIAL_CATEGORIES: TaskCategoryGroup[] = [
    {
        id: 'cat-1',
        code: 'HM-01',
        name: 'Nghiên cứu & Thiết kế UI/UX',
        lead: 'Hoàng Nam',
        progress: 85,
        color: 'indigo',
        tasks: [
            { id: 't1', code: 'T-101', name: 'Khảo sát người dùng & Lập User Journey', assigneeId: 'm1', priority: 'Cao', hours: 40, budgetHours: 40, actualHours: 38, status: 'Hoàn thành', startWeek: 'Tuần 1', endWeek: 'Tuần 1' },
            { id: 't2', code: 'T-102', name: 'Xây dựng Design System & Component Library', assigneeId: 'm2', priority: 'Cao', hours: 60, budgetHours: 60, actualHours: 45, status: 'Đang làm', startWeek: 'Tuần 1', endWeek: 'Tuần 3' },
            { id: 't3', code: 'T-103', name: 'Wireframe & Prototype phân hệ Phân bổ nhân lực', assigneeId: 'm2', priority: 'Trung bình', hours: 45, budgetHours: 40, actualHours: 44, status: 'Đang làm', startWeek: 'Tuần 2', endWeek: 'Tuần 3' }
        ]
    },
    {
        id: 'cat-2',
        code: 'HM-02',
        name: 'Phát triển Hệ thống Backend & API',
        lead: 'Quang Huy',
        progress: 55,
        color: 'emerald',
        tasks: [
            { id: 't4', code: 'T-201', name: 'Thiết kế Database Schema & Quan hệ phân quyền RBAC', assigneeId: 'm3', priority: 'Cao', hours: 50, budgetHours: 50, actualHours: 48, status: 'Hoàn thành', startWeek: 'Tuần 1', endWeek: 'Tuần 2' },
            { id: 't5', code: 'T-202', name: 'Viết API Quản lý Hạng mục & Phân rã WBS', assigneeId: 'm4', priority: 'Cao', hours: 65, budgetHours: 60, actualHours: 66, status: 'Đang làm', startWeek: 'Tuần 2', endWeek: 'Tuần 4' },
            { id: 't6', code: 'T-203', name: 'Tích hợp API Phân bổ tuần & Thuật toán cảnh báo quá tải', assigneeId: 'm3', priority: 'Cao', hours: 70, budgetHours: 70, actualHours: 20, status: 'Chờ duyệt', startWeek: 'Tuần 3', endWeek: 'Tuần 5' },
            { id: 't7', code: 'T-204', name: 'Tối ưu hiệu năng truy vấn ma trận nhân sự thời gian thực', assigneeId: 'm4', priority: 'Trung bình', hours: 40, budgetHours: 40, actualHours: 0, status: 'Chưa làm', startWeek: 'Tuần 4', endWeek: 'Tuần 5' }
        ]
    },
    {
        id: 'cat-3',
        code: 'HM-03',
        name: 'Lập trình Giao diện Web Application (Frontend)',
        lead: 'Đức Anh',
        progress: 40,
        color: 'sky',
        tasks: [
            { id: 't8', code: 'T-301', name: 'Dựng khung Layout & State Management (Redux/Zustand)', assigneeId: 'm5', priority: 'Cao', hours: 45, budgetHours: 45, actualHours: 44, status: 'Hoàn thành', startWeek: 'Tuần 1', endWeek: 'Tuần 2' },
            { id: 't9', code: 'T-302', name: 'Code bảng ma trận kéo thả phân bổ nhân lực tuần', assigneeId: 'm5', priority: 'Cao', hours: 80, budgetHours: 80, actualHours: 55, status: 'Đang làm', startWeek: 'Tuần 2', endWeek: 'Tuần 4' },
            { id: 't10', code: 'T-303', name: 'Xây dựng cây phân cấp Hạng mục công việc đa tầng', assigneeId: 'm5', priority: 'Trung bình', hours: 50, budgetHours: 50, actualHours: 0, status: 'Chưa làm', startWeek: 'Tuần 3', endWeek: 'Tuần 5' }
        ]
    },
    {
        id: 'cat-4',
        code: 'HM-04',
        name: 'Đảm bảo Chất lượng (QA/QC) & UAT',
        lead: 'Thùy Trang',
        progress: 20,
        color: 'purple',
        tasks: [
            { id: 't11', code: 'T-401', name: 'Lập kế hoạch Kiểm thử & Test Case cho chức năng WBS', assigneeId: 'm6', priority: 'Trung bình', hours: 35, budgetHours: 35, actualHours: 35, status: 'Hoàn thành', startWeek: 'Tuần 1', endWeek: 'Tuần 2' },
            { id: 't12', code: 'T-402', name: 'Kiểm thử tải (Load test) khi phân bổ 500+ nhân sự', assigneeId: 'm6', priority: 'Cao', hours: 50, budgetHours: 50, actualHours: 0, status: 'Chưa làm', startWeek: 'Tuần 4', endWeek: 'Tuần 5' }
        ]
    }
];

