export interface TaskItem {
    id: string;
    code: string;
    name: string;
    assigneeId: string;
    priority: 'Cao' | 'Trung bình' | 'Thấp';
    hours: number;
    budgetHours?: number; // Ngân sách giờ công do PM đặt
    actualHours?: number; // Giờ công thực tế đã duyệt
    burnedPercentage?: number; // Tỷ lệ đã dùng (%) từ Backend
    burnStatus?: 'NOT_SET' | 'SAFE' | 'WARNING' | 'OVER_BUDGET'; // Trạng thái ngân sách từ Backend
    isOverBudget?: boolean; // Cờ vượt ngân sách từ Backend
    status: 'Hoàn thành' | 'Đang làm' | 'Chờ duyệt' | 'Chưa làm';
    startWeek: string;
    endWeek: string;
    startDate?: string;
    dueDate?: string;
    actualEndDate?: string;
    slackDays?: number;
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
    employeeId?: number;
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
    year?: number;
    weekNumber?: number;
}


export interface ProjectMonth {
    id: string;
    name: string;
    weeks: MonthWeek[];
}

export function getIsoWeekNumber(date: Date): { year: number; weekNumber: number } {
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const dayNum = d.getUTCDay() || 7;
    d.setUTCDate(d.getUTCDate() + 4 - dayNum);
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    const weekNumber = Math.ceil(((d.getTime() - yearStart.getTime()) / 86400000 + 1) / 7);
    return { year: d.getUTCFullYear(), weekNumber };
}

export function generateProjectMonth(year: number, monthZeroIndexed: number): ProjectMonth {
    const pad = (n: number) => String(n).padStart(2, '0');
    const monthId = `${year}-${pad(monthZeroIndexed + 1)}`;
    const monthName = `Tháng ${pad(monthZeroIndexed + 1)}/${year}`;

    const today = new Date();
    const currentYear = today.getFullYear();
    const currentMonth = today.getMonth();
    const currentDate = today.getDate();

    const lastDayOfMonth = new Date(year, monthZeroIndexed + 1, 0).getDate();
    const weeks: MonthWeek[] = [];

    let day = 1;
    let weekIndex = 1;

    while (day <= lastDayOfMonth) {
        const startDay = day;
        const endDay = Math.min(day + 6, lastDayOfMonth);
        const middleDate = new Date(year, monthZeroIndexed, Math.min(startDay + 3, endDay));
        const { year: isoYear, weekNumber } = getIsoWeekNumber(middleDate);

        const isCurrent =
            year === currentYear &&
            monthZeroIndexed === currentMonth &&
            currentDate >= startDay &&
            currentDate <= endDay;

        weeks.push({
            key: `W${weekIndex}`,
            label: `Tuần ${weekIndex}`,
            dates: `${pad(startDay)}/${pad(monthZeroIndexed + 1)} - ${pad(endDay)}/${pad(monthZeroIndexed + 1)}`,
            isCurrent,
            year: isoYear,
            weekNumber,
        });

        day = endDay + 1;
        weekIndex++;
    }

    return {
        id: monthId,
        name: monthName,
        weeks,
    };
}

export function generateProjectMonthsAroundCurrent(spanMonths = 6): ProjectMonth[] {
    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth();
    const list: ProjectMonth[] = [];

    for (let offset = 0; offset < spanMonths; offset++) {
        const d = new Date(currentYear, currentMonth + offset, 1);
        list.push(generateProjectMonth(d.getFullYear(), d.getMonth()));
    }
    return list;
}

export const INITIAL_MONTHS_LIST: ProjectMonth[] = generateProjectMonthsAroundCurrent();




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

