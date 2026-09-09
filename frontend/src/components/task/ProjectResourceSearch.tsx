import { useState } from 'react';
import SkillresourceSearch, {
    type ResourceEmployee,
    type DepartmentItem,
} from '../skilldeclaration/SkillresourceSearch';

const DEFAULT_RESOURCE_EMPLOYEES: ResourceEmployee[] = [
    {
        id: 'emp-1',
        code: 'NV001',
        name: 'Trần Lan Anh',
        title: 'Product Owner / BA',
        department: 'Phòng Công nghệ',
        availability: 'partial',
        availabilityPercent: 50,
        skills: [
            { skillId: 'react', name: 'React.js', level: 4 },
            { skillId: 'typescript', name: 'TypeScript', level: 4 },
        ],
    },
    {
        id: 'emp-2',
        code: 'NV002',
        name: 'Hoàng Nam',
        title: 'UI/UX Designer',
        department: 'Phòng Marketing',
        availability: 'partial',
        availabilityPercent: 37,
        skills: [
            { skillId: 'uiux', name: 'UI/UX Design', level: 5 },
            { skillId: 'react', name: 'React.js', level: 3 },
        ],
    },
    {
        id: 'emp-3',
        code: 'NV003',
        name: 'Lê Quốc Bảo',
        title: 'Backend Dev',
        department: 'Phòng Công nghệ',
        availability: 'full',
        availabilityPercent: 100,
        skills: [
            { skillId: 'java', name: 'Java', level: 5 },
            { skillId: 'nodejs', name: 'Node.js', level: 4 },
        ],
    },
];

const DEFAULT_DEPARTMENTS: DepartmentItem[] = [
    { id: 'dept-1', name: 'Phòng Công nghệ' },
    { id: 'dept-2', name: 'Phòng Marketing' },
];

interface ProjectResourceSearchProps {
    onAssignTask?: (employee: ResourceEmployee) => void;
}

export function ProjectResourceSearch({ onAssignTask }: ProjectResourceSearchProps) {
    const [assignedToast, setAssignedToast] = useState<string | null>(null);

    const handleAssign = (emp: ResourceEmployee) => {
        if (onAssignTask) {
            onAssignTask(emp);
        }
        setAssignedToast(`Đã gán thành công ${emp.name} (${emp.title}) vào dự án!`);
        setTimeout(() => setAssignedToast(null), 3000);
    };

    return (
        <div className="space-y-4 animate-in fade-in">
            {assignedToast && (
                <div className="flex items-center justify-between rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-xs font-semibold text-emerald-800 shadow-sm">
                    <span>{assignedToast}</span>
                    <button
                        type="button"
                        onClick={() => setAssignedToast(null)}
                        className="rounded-md p-1 hover:bg-emerald-100"
                    >
                        ✕
                    </button>
                </div>
            )}

            <SkillresourceSearch
                embedded={true}
                departments={DEFAULT_DEPARTMENTS}
                employees={DEFAULT_RESOURCE_EMPLOYEES}
                onAssignProject={handleAssign}
            />
        </div>
    );
}

