import { X, UserCheck, GitBranch, Layers, FileText, Pencil } from "lucide-react";
import type { OrgTreeNode } from "./OrgChart";

interface OrgNodeDetailModalProps {
    open: boolean;
    node: OrgTreeNode | null;
    onClose: () => void;
    onEdit?: (nodeId: string) => void;
}

export default function OrgNodeDetailModal({
    open,
    node,
    onClose,
    onEdit,
}: OrgNodeDetailModalProps) {
    if (!open || !node) return null;

    const Icon = node.icon;
    const hasChildren = node.children && node.children.length > 0;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            {/* Backdrop */}
            <div
                className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm transition-opacity animate-in fade-in duration-150"
                onClick={onClose}
            />

            {/* Modal Card */}
            <div className="relative w-full max-w-md rounded-3xl border border-slate-200/90 bg-white p-6 shadow-2xl text-slate-800 animate-in zoom-in-95 duration-150 max-h-[90vh] overflow-y-auto">
                {/* Close Button */}
                <button
                    onClick={onClose}
                    type="button"
                    className="absolute right-5 top-5 rounded-xl p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
                >
                    <X className="h-5 w-5" />
                </button>

                {/* Header: Icon + Title + Badge */}
                <div className="flex items-start gap-3.5 mb-5 pr-8">
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl border border-slate-200/80 bg-slate-50 shadow-2xs">
                        {Icon && <Icon className={`h-6 w-6 ${node.iconColor}`} />}
                    </div>
                    <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2 mb-1 flex-wrap">
                            <span
                                className={`rounded-full px-2.5 py-0.5 text-[10px] font-bold tracking-wider uppercase ${node.badgeBg} ${node.badgeColor}`}
                            >
                                {node.badge}
                            </span>
                            <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-bold text-slate-600">
                                {node.levelText}
                            </span>
                        </div>
                        <h3 className="text-lg font-bold text-slate-900 tracking-tight leading-snug">
                            {node.title}
                        </h3>
                    </div>
                </div>

                {/* Body Details */}
                <div className="space-y-3.5 text-xs">
                    {/* Người tổ chức */}
                    <div className="rounded-2xl border border-indigo-100 bg-indigo-50/50 p-3.5">
                        <div className="flex items-center gap-2.5">
                            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-indigo-100 text-indigo-600">
                                <UserCheck className="h-4 w-4" />
                            </div>
                            <div className="min-w-0 flex-1">
                                <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-500 block">
                                    Người tổ chức / Người đứng đầu
                                </span>
                                <span className="text-sm font-bold text-indigo-950 truncate block mt-0.5">
                                    {node.manager || "Chưa bổ nhiệm"}
                                </span>
                            </div>
                        </div>
                    </div>

                    {/* Mô tả chức năng & nhiệm vụ */}
                    <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-3.5">
                        <div className="flex items-center gap-2 text-slate-500 font-semibold mb-1">
                            <FileText className="h-3.5 w-3.5" />
                            <span>Chức năng & Nhiệm vụ</span>
                        </div>
                        <p className="text-slate-700 leading-relaxed font-medium">
                            {node.desc || "Chưa có mô tả chi tiết."}
                        </p>
                    </div>

                    {/* Cơ quan báo cáo & Phân cấp */}
                    <div className="grid grid-cols-2 gap-2.5">
                        <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-3">
                            <div className="flex items-center gap-1.5 text-slate-400 font-semibold text-[11px] mb-1">
                                <Layers className="h-3.5 w-3.5" />
                                <span>Trực thuộc</span>
                            </div>
                            <span className="font-bold text-slate-800 text-xs block truncate">
                                {node.subLeft || "Cấp cao nhất"}
                            </span>
                        </div>
                        <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-3">
                            <div className="flex items-center gap-1.5 text-slate-400 font-semibold text-[11px] mb-1">
                                <GitBranch className="h-3.5 w-3.5" />
                                <span>Quy mô nhánh</span>
                            </div>
                            <span className="font-bold text-slate-800 text-xs block">
                                {hasChildren ? `${node.children.length} đơn vị con` : "Đơn vị cơ sở"}
                            </span>
                        </div>
                    </div>

                    {/* Danh sách các phòng ban trực thuộc */}
                    {hasChildren && (
                        <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-3.5">
                            <div className="flex items-center justify-between text-slate-500 font-semibold mb-2">
                                <span className="flex items-center gap-1.5">
                                    <GitBranch className="h-3.5 w-3.5" />
                                    <span>Đơn vị trực thuộc ({node.children.length})</span>
                                </span>
                            </div>
                            <div className="space-y-1.5">
                                {node.children.map((child) => (
                                    <div
                                        key={child.id}
                                        className="flex items-center justify-between rounded-xl bg-white border border-slate-100 px-3 py-2 text-xs"
                                    >
                                        <div className="flex items-center gap-2 min-w-0">
                                            <span className="size-2 rounded-full bg-indigo-500 shrink-0" />
                                            <span className="font-bold text-slate-800 truncate">
                                                {child.title}
                                            </span>
                                        </div>
                                        <span className="text-[11px] text-slate-500 shrink-0 ml-2">
                                            {child.manager ? child.manager.split("(")[0].trim() : "Chưa có QL"}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </div>

                {/* Footer Buttons */}
                <div className="mt-6 flex items-center justify-end gap-2.5 border-t border-slate-100 pt-4">
                    <button
                        onClick={onClose}
                        type="button"
                        className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition"
                    >
                        Đóng
                    </button>
                    {onEdit && (
                        <button
                            onClick={() => {
                                onClose();
                                onEdit(node.id);
                            }}
                            type="button"
                            className="flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs"
                        >
                            <Pencil className="h-3.5 w-3.5" />
                            <span>Chỉnh sửa phòng này</span>
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}
