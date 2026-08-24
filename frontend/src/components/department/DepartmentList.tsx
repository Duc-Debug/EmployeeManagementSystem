import { Plus, FolderKanban, MoreHorizontal } from "lucide-react";

interface Department {
    id: string;
    name: string;
    manager: string;
    count: number;
    budget: string;
}

interface DepartmentListProps {
    departments: Department[];
}

export default function DepartmentList({ departments }: DepartmentListProps) {
    return (
        <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-base font-bold text-slate-900">Các phòng ban hiện tại</h2>
                <button className="flex items-center gap-2 rounded-xl bg-[#4338ca] px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-indigo-700">
                    <Plus className="h-4 w-4" /> Thêm phòng ban
                </button>
            </div>
            <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                    <thead>
                    <tr className="border-b border-slate-100 text-slate-400">
                        <th className="pb-3 font-semibold">Tên phòng ban</th>
                        <th className="pb-3 font-semibold">Trưởng phòng</th>
                        <th className="pb-3 font-semibold">Số nhân sự</th>
                        <th className="pb-3 font-semibold text-right">Thao tác</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-50">
                    {departments.map((dept) => (
                        <tr key={dept.id} className="hover:bg-slate-50/60">
                            <td className="py-3.5 font-bold text-slate-900">
                                <div className="flex items-center gap-2.5">
                                    <FolderKanban className="h-4 w-4 text-indigo-500" />
                                    {dept.name}
                                </div>
                            </td>
                            <td className="py-3.5 font-medium text-slate-700">{dept.manager}</td>
                            <td className="py-3.5 text-slate-600">{dept.count} thành viên</td>
                            <td className="py-3.5 text-right">
                                <button className="rounded-md p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600">
                                    <MoreHorizontal className="h-4 w-4" />
                                </button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}