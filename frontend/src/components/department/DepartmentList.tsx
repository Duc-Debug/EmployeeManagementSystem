import { Plus, FolderKanban, Pencil, Trash2 } from "lucide-react";
import type { Department } from "./DepartmentModal";

interface DepartmentListProps {
    departments: Department[];
    onAdd: () => void;
    onEdit: (dept: Department) => void;
    onDelete: (id: string) => void;
}

export default function DepartmentList({ departments, onAdd, onEdit, onDelete }: DepartmentListProps) {
    return (
        <div className="rounded-2xl border border-white/15 bg-white/[0.07] p-5 backdrop-blur-xl shadow-[0_8px_24px_rgba(15,10,45,0.15)]">
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-base font-bold text-white">Các phòng ban hiện tại</h2>
                <button
                    onClick={onAdd}
                    className="flex items-center gap-2 rounded-xl border border-white/25 bg-white/15 px-4 py-2 text-xs font-semibold text-white backdrop-blur-xl transition hover:bg-white/25"
                >
                    <Plus className="h-4 w-4" /> Thêm phòng ban
                </button>
            </div>

            <div className="overflow-x-auto [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden">
                <table className="w-full text-left text-xs border-separate border-spacing-y-1">
                    <thead>
                    <tr className="text-white/50">
                        <th className="pb-3 pl-4 font-semibold">Tên phòng ban</th>
                        <th className="pb-3 font-semibold">Trưởng phòng</th>
                        <th className="pb-3 font-semibold">Số nhân sự</th>
                        <th className="pb-3 pr-4 font-semibold text-right">Thao tác</th>
                    </tr>
                    </thead>
                    <tbody>
                    {departments.map((dept) => (
                        <tr key={dept.id} className="group transition-colors">
                            {/* Ô đầu tiên - Bo góc trái */}
                            <td className="py-3.5 pl-4 font-bold text-white transition bg-transparent group-hover:bg-white/[0.08] first:rounded-l-xl">
                                <div className="flex items-center gap-3">
                                    <FolderKanban className="h-4 w-4 shrink-0 text-[#63ecc8]" />
                                    <span>{dept.name}</span>
                                </div>
                            </td>

                            {/* Ô giữa */}
                            <td className="py-3.5 font-medium text-white/80 transition bg-transparent group-hover:bg-white/[0.08]">
                                {dept.manager}
                            </td>

                            {/* Ô giữa */}
                            <td className="py-3.5 text-white/60 transition bg-transparent group-hover:bg-white/[0.08]">
                                {dept.count} thành viên
                            </td>

                            {/* Ô cuối cùng - Bo góc phải */}
                            <td className="py-3.5 pr-4 text-right transition bg-transparent group-hover:bg-white/[0.08] last:rounded-r-xl">
                                <div className="flex items-center justify-end gap-2">
                                    {/* Nút Sửa */}
                                    <div className="relative flex flex-col items-center group/btn">
                                        <button
                                            type="button"
                                            onClick={() => onEdit(dept)}
                                            className="flex h-8 w-8 items-center justify-center rounded-lg border border-white/15 bg-white/10 text-white/80 backdrop-blur-md transition hover:border-[#63ecc8]/50 hover:bg-[#63ecc8]/20 hover:text-[#63ecc8] active:scale-95"
                                        >
                                            <Pencil className="h-3.5 w-3.5" />
                                        </button>
                                        <span className="pointer-events-none absolute -top-8 z-30 hidden whitespace-nowrap rounded-md border border-white/20 bg-[#181233]/90 px-2 py-1 text-[11px] font-medium text-white shadow-lg backdrop-blur-md group-hover/btn:block transition-all">
                                            Sửa
                                        </span>
                                    </div>

                                    {/* Nút Xóa */}
                                    <div className="relative flex flex-col items-center group/btn">
                                        <button
                                            type="button"
                                            onClick={() => onDelete(dept.id)}
                                            className="flex h-8 w-8 items-center justify-center rounded-lg border border-white/15 bg-white/10 text-white/80 backdrop-blur-md transition hover:border-rose-400/50 hover:bg-rose-500/20 hover:text-rose-300 active:scale-95"
                                        >
                                            <Trash2 className="h-3.5 w-3.5" />
                                        </button>
                                        <span className="pointer-events-none absolute -top-8 z-30 hidden whitespace-nowrap rounded-md border border-white/20 bg-[#181233]/90 px-2 py-1 text-[11px] font-medium text-rose-300 shadow-lg backdrop-blur-md group-hover/btn:block transition-all">
                                            Xóa
                                        </span>
                                    </div>
                                </div>
                            </td>
                        </tr>
                    ))}
                    {departments.length === 0 && (
                        <tr>
                            <td colSpan={4} className="py-8 text-center text-white/40">
                                Chưa có phòng ban nào. Nhấn "Thêm phòng ban" để bắt đầu.
                            </td>
                        </tr>
                    )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}