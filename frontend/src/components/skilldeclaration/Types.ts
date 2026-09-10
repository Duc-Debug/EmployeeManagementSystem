export type SkillStatus = 'pending' | 'approved' | 'rejected';
export type Role = 'VT-01' | 'VT-02' | 'VT-03' | 'VT-04' | 'VT-05' | 'VT-06' | string;
export type FormMode = 'create' | 'update';

/** Một mục trong danh mục kỹ năng chuẩn */
export interface CatalogSkill {
    id: number;
    name: string;
    category: string;
    groupId?: number;
    description?: string;
    version?: number;
}

/** Một kỹ năng đã được nhân viên khai báo, hiển thị trong bảng */
export interface DeclaredSkill {
    skillId: number;
    name: string;
    code: string;
    cat: string;
    level: number; // 1..5
    years: number;
    status: SkillStatus;
}

/** Payload khi khai báo / cập nhật kỹ năng */
export interface SkillPayload {
    skillId: number;
    proficiencyLevel: number;
    yearsOfExperience: number;
}

/** Một toast thông báo hiển thị góc phải màn hình */
export interface ToastItem {
    id: number;
    title: string;
    message: string;
}

export const SKILL_CATALOG: CatalogSkill[] = [
    { id: 1, name: 'Java', category: 'Backend' },
    { id: 2, name: 'React.js', category: 'Frontend' },
    { id: 3, name: 'Node.js', category: 'Backend' },
    { id: 4, name: 'PostgreSQL', category: 'Database' },
    { id: 5, name: 'Docker', category: 'DevOps' },
    { id: 6, name: 'AWS', category: 'DevOps' },
];

export const INITIAL_SKILLS: DeclaredSkill[] = [
    { skillId: 1, name: 'Java', code: 'SK-014', cat: 'Backend', level: 3, years: 2, status: 'approved' },
    { skillId: 2, name: 'React.js', code: 'SK-027', cat: 'Frontend', level: 4, years: 1.5, status: 'pending' },
    { skillId: 5, name: 'Docker', code: 'SK-055', cat: 'DevOps', level: 2, years: 1, status: 'rejected' },
];

export const PROFICIENCY_LEVELS = [
    { level: 1, label: 'Cơ bản' },
    { level: 2, label: 'Khá' },
    { level: 3, label: 'Thành thạo' },
    { level: 4, label: 'Giỏi' },
    { level: 5, label: 'Chuyên gia' },
];

/* ── Dùng cho tab "Phê duyệt" ────────────────────────────── */

/** Một yêu cầu kỹ năng đang chờ quản lý duyệt */
export interface PendingApprovalSkill {
    id: number;
    employeeName: string;
    skillName: string;
    category: string;
    level: number; // 1..5
    years: number;
    status: 'pending' | 'approved' | 'rejected';
}

export const INITIAL_APPROVAL_REQUESTS: PendingApprovalSkill[] = [
    {
        id: 1,
        employeeName: 'Trần Thị Bình',
        skillName: 'Vue.js',
        category: 'Frontend',
        level: 4,
        years: 2,
        status: 'pending',
    },
    {
        id: 2,
        employeeName: 'Phạm Hoàng Dũng',
        skillName: 'Kubernetes',
        category: 'DevOps',
        level: 3,
        years: 1,
        status: 'pending',
    },
    {
        id: 3,
        employeeName: 'Nguyễn Minh Anh',
        skillName: 'PostgreSQL',
        category: 'Database',
        level: 5,
        years: 3,
        status: 'approved',
    },
    {
        id: 4,
        employeeName: 'Lê Thu Hà',
        skillName: 'React.js',
        category: 'Frontend',
        level: 4,
        years: 2.5,
        status: 'rejected',
    },
    {
        id: 5,
        employeeName: 'Đỗ Văn Khoa',
        skillName: 'Java Spring Boot',
        category: 'Backend',
        level: 3,
        years: 1.5,
        status: 'pending',
    },
];

/* ── Dùng cho tab "Ma trận kỹ năng bộ phận" ─────────────── */

export interface MatrixSkillColumn {
    name: string;      // e.g. 'Java'
    category: string;  // e.g. 'Backend'
}

export interface MatrixEmployee {
    id: string;
    code: string;          // e.g. 'NV01'
    name: string;
    role: string;          // e.g. 'Senior Backend Dev'
    currentProject: string;
    /** key = skillName, value = level 1-5 hoặc null (không có kỹ năng) */
    skills: Record<string, number | null>;
}

export const MATRIX_SKILL_COLUMNS: MatrixSkillColumn[] = [
    { name: 'Java',     category: 'Backend'  },
    { name: 'Node.js',  category: 'Backend'  },
    { name: 'React.js', category: 'Frontend' },
    { name: 'Vue.js',   category: 'Frontend' },
    { name: 'Docker',   category: 'DevOps'   },
];

export const MATRIX_EMPLOYEES: MatrixEmployee[] = [
    {
        id: 'nv01', code: 'NV01', name: 'Nguyễn Văn An', role: 'Senior Backend Dev',
        currentProject: 'Core Banking V2',
        skills: { Java: 4, 'Node.js': 4, 'React.js': 2, 'Vue.js': 1, Docker: 3 },
    },
    {
        id: 'nv02', code: 'NV02', name: 'Trần Thị Bình', role: 'Frontend Lead',
        currentProject: 'HRM Portal',
        skills: { Java: 1, 'Node.js': 3, 'React.js': 5, 'Vue.js': 4, Docker: 2 },
    },
    {
        id: 'nv03', code: 'NV03', name: 'Lê Văn Cường', role: 'DevOps Engineer',
        currentProject: 'Cloud Migration',
        skills: { Java: 2, 'Node.js': 2, 'React.js': 1, 'Vue.js': null, Docker: 5 },
    },
    {
        id: 'nv04', code: 'NV04', name: 'Phạm Hoàng Dũng', role: 'Fullstack Dev',
        currentProject: 'Core Banking V2',
        skills: { Java: 3, 'Node.js': 4, 'React.js': 4, 'Vue.js': 2, Docker: 3 },
    },
    {
        id: 'nv05', code: 'NV05', name: 'Đinh Thị Thu', role: 'QA / Automation',
        currentProject: 'HRM Portal',
        skills: { Java: 2, 'Node.js': 2, 'React.js': 2, 'Vue.js': 1, Docker: 1 },
    },
    {
        id: 'nv06', code: 'NV06', name: 'Vũ Quốc Bảo', role: 'Junior Dev',
        currentProject: 'Dự án Nội bộ',
        skills: { Java: 1, 'Node.js': 1, 'React.js': 3, 'Vue.js': null, Docker: 1 },
    },
];

/* ── Dùng cho tab "Hạng mục & Công việc" ────────────────── */

export type TaskStatus = 'in_progress' | 'done' | 'unassigned';

export interface Task {
    id: string;
    title: string;
    description: string;
    requiredSkill: string;
    requiredLevel: number;
    assigneeName: string | null; // null = tự động gợi ý
    status: TaskStatus;
}

export interface TaskCategory {
    id: string;
    title: string;
    tasks: Task[];
}

export const INITIAL_TASK_CATEGORIES: TaskCategory[] = [
    {
        id: 'cat-1',
        title: 'Hạng mục 1: Phát triển Microservices Backend API',
        tasks: [
            {
                id: 'task-1-1',
                title: 'Thiết kế Microservices Architecture & High Availability',
                description: 'Thiết kế kiến trúc hệ thống và xây dựng chuỗi REST/gRPC API cho module thanh toán.',
                requiredSkill: 'Java',
                requiredLevel: 4,
                assigneeName: 'Nguyễn Văn An',
                status: 'in_progress',
            },
            {
                id: 'task-1-2',
                title: 'Tối ưu Query Database PostgreSQL cho Transaction',
                description: 'Phân tích và tối ưu các câu query phức tạp, giảm thời gian phản hồi xuống dưới 100ms.',
                requiredSkill: 'PostgreSQL',
                requiredLevel: 4,
                assigneeName: 'Phạm Hoàng Dũng',
                status: 'done',
            },
            {
                id: 'task-1-3',
                title: 'Tích hợp Redis Caching & Cache Invalidation',
                description: 'Triển khai cơ chế cache phân tán và xử lý cache invalidation khi dữ liệu thay đổi.',
                requiredSkill: 'Node.js',
                requiredLevel: 3,
                assigneeName: null,
                status: 'unassigned',
            },
        ],
    },
    {
        id: 'cat-2',
        title: 'Hạng mục 2: Frontend & UX Module HRM',
        tasks: [
            {
                id: 'task-2-1',
                title: 'Xây dựng Dashboard thống kê nhân sự real-time',
                description: 'Thiết kế và implement các biểu đồ, chart thống kê với dữ liệu cập nhật tự động.',
                requiredSkill: 'React.js',
                requiredLevel: 4,
                assigneeName: 'Trần Thị Bình',
                status: 'in_progress',
            },
            {
                id: 'task-2-2',
                title: 'Responsive Design cho Mobile & Tablet',
                description: 'Đảm bảo toàn bộ giao diện hoạt động mượt mà trên mọi kích thước màn hình.',
                requiredSkill: 'Vue.js',
                requiredLevel: 3,
                assigneeName: null,
                status: 'unassigned',
            },
        ],
    },
];