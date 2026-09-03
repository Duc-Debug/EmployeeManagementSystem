
export type SkillStatus = 'pending' | 'approved' | 'rejected';
export type Role = 'VT-04' | 'OTHER';
export type FormMode = 'create' | 'update';

/** Một mục trong danh mục kỹ năng chuẩn */
export interface CatalogSkill {
    id: number;
    name: string;
    category: string;
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