export interface TaskItem {
    id: string;
    code: string;
    name: string;
    assigneeId: string;
    priority: 'Cao' | 'Trung bình' | 'Thấp';
    hours: number;
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
            { key: 'W1', label: 'Tuần 1', dates: '31/08 - 06/09', isCurrent: false },
            { key: 'W2', label: 'Tuần 2', dates: '07/09 - 13/09', isCurrent: true },
            { key: 'W3', label: 'Tuần 3', dates: '14/09 - 20/09', isCurrent: false },
            { key: 'W4', label: 'Tuần 4', dates: '21/09 - 27/09', isCurrent: false }
        ]
    },
    {
        id: '2026-10',
        name: 'Tháng 10/2026',
        weeks: [
            { key: 'W1', label: 'Tuần 1', dates: '28/09 - 04/10', isCurrent: false },
            { key: 'W2', label: 'Tuần 2', dates: '05/10 - 11/10', isCurrent: false },
            { key: 'W3', label: 'Tuần 3', dates: '12/10 - 18/10', isCurrent: false },
            { key: 'W4', label: 'Tuần 4', dates: '19/10 - 25/10', isCurrent: false }
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
            { id: 't1', code: 'T-101', name: 'Khảo sát người dùng & Lập User Journey', assigneeId: 'm1', priority: 'Cao', hours: 40, status: 'Hoàn thành', startWeek: 'Tuần 1', endWeek: 'Tuần 1' },
            { id: 't2', code: 'T-102', name: 'Xây dựng Design System & Component Library', assigneeId: 'm2', priority: 'Cao', hours: 60, status: 'Đang làm', startWeek: 'Tuần 1', endWeek: 'Tuần 3' }
        ]
    },
    {
        id: 'cat-2',
        code: 'HM-02',
        name: 'Phát triển Hệ thống Backend & API',
        lead: 'Lê Quốc Bảo',
        progress: 55,
        color: 'emerald',
        tasks: [
            { id: 't3', code: 'T-201', name: 'Thiết kế Database Schema & Quan hệ phân quyền RBAC', assigneeId: 'm3', priority: 'Cao', hours: 50, status: 'Hoàn thành', startWeek: 'Tuần 1', endWeek: 'Tuần 2' },
            { id: 't4', code: 'T-202', name: 'Viết API Quản lý Hạng mục & Phân rã WBS', assigneeId: 'm3', priority: 'Cao', hours: 65, status: 'Đang làm', startWeek: 'Tuần 2', endWeek: 'Tuần 4' }
        ]
    }
];

