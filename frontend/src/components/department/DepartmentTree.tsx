import React, { useState, useMemo, useEffect } from "react";
import {
    Building2,
    GitBranch,
    Users,
    User,
    ChevronDown,
    ChevronRight,
    GripVertical,
    Search,
    Plus,
    Pencil,
    Trash2,
    Lock,
    Unlock,
    Maximize2,
    Minimize2,
    UserCheck,
    X,
    AlertTriangle,
} from "lucide-react";
import { cn } from "@/lib/utils";
import {
    getOrgTree,
    createOrgUnit,
    updateOrgUnit,
    moveOrgUnit,
    deactivateOrgUnit,
    activateOrgUnit,
} from "@/lib/api/org-units";
import type { OrgUnitTreeNode } from "@/types/hrm";

export type UnitType = "COMPANY" | "CENTER" | "DEPARTMENT" | "TEAM";

export interface DepartmentNode {
    id: string;
    unitCode: string;
    name: string;
    manager: string;
    count: number;
    unitType: UnitType;
    status: "ACTIVE" | "INACTIVE";
    description?: string;
    children: DepartmentNode[];
}

export const INITIAL_DEPARTMENT_TREE: DepartmentNode = {
    id: "unit-corp",
    unitCode: "CORP",
    name: "Tập đoàn Doanh nghiệp",
    manager: "Hội đồng Quản trị",
    count: 145,
    unitType: "COMPANY",
    status: "ACTIVE",
    description: "Cơ quan đầu não chiến lược và điều hành toàn bộ tập đoàn",
    children: [
        {
            id: "unit-tech",
            unitCode: "TECH",
            name: "Khối Kỹ thuật & Công nghệ",
            manager: "Trần Quốc Bảo",
            count: 55,
            unitType: "CENTER",
            status: "ACTIVE",
            description: "Nghiên cứu, phát triển sản phẩm phần mềm và hạ tầng số",
            children: [
                {
                    id: "unit-tech-fe",
                    unitCode: "TECH-FE",
                    name: "Phòng Lập trình Frontend",
                    manager: "Nguyễn Văn A",
                    count: 22,
                    unitType: "DEPARTMENT",
                    status: "ACTIVE",
                    description: "Phát triển giao diện web, mobile app và trải nghiệm người dùng",
                    children: [
                        {
                            id: "unit-tech-ui",
                            unitCode: "FE-UI",
                            name: "Nhóm UI/UX & Design System",
                            manager: "Lê Thị Mai",
                            count: 6,
                            unitType: "TEAM",
                            status: "ACTIVE",
                            description: "Thiết kế đồ họa và hệ thống design token",
                            children: [],
                        },
                    ],
                },
                {
                    id: "unit-tech-be",
                    unitCode: "TECH-BE",
                    name: "Phòng Lập trình Backend",
                    manager: "Lê Văn C",
                    count: 25,
                    unitType: "DEPARTMENT",
                    status: "ACTIVE",
                    description: "Xây dựng hệ thống microservices, cơ sở dữ liệu và API",
                    children: [],
                },
                {
                    id: "unit-tech-devops",
                    unitCode: "TECH-OPS",
                    name: "Nhóm Cloud & DevOps",
                    manager: "Vũ Hoàng D",
                    count: 8,
                    unitType: "TEAM",
                    status: "ACTIVE",
                    description: "Quản trị hạ tầng đám mây, CI/CD và bảo mật",
                    children: [],
                },
            ],
        },
        {
            id: "unit-ops",
            unitCode: "OPS",
            name: "Khối Vận hành & Nhân sự",
            manager: "Nguyễn Minh Anh",
            count: 35,
            unitType: "CENTER",
            status: "ACTIVE",
            description: "Quản trị nguồn nhân lực, tuyển dụng và hỗ trợ vận hành",
            children: [
                {
                    id: "unit-hr",
                    unitCode: "HR",
                    name: "Phòng Nhân sự & Tuyển dụng",
                    manager: "Phạm Mai E",
                    count: 18,
                    unitType: "DEPARTMENT",
                    status: "ACTIVE",
                    description: "Tuyển chọn nhân tài, đánh giá KPI và chính sách phúc lợi",
                    children: [],
                },
                {
                    id: "unit-admin",
                    unitCode: "ADM",
                    name: "Phòng Hành chính & Quản trị",
                    manager: "Đỗ Thị G",
                    count: 17,
                    unitType: "DEPARTMENT",
                    status: "ACTIVE",
                    description: "Quản lý cơ sở vật chất, tài sản và dịch vụ nội bộ",
                    children: [],
                },
            ],
        },
        {
            id: "unit-biz",
            unitCode: "BIZ",
            name: "Khối Kinh doanh & Marketing",
            manager: "Lê Thu Hà",
            count: 55,
            unitType: "CENTER",
            status: "ACTIVE",
            description: "Mở rộng thị trường, bán hàng và quảng bá thương hiệu",
            children: [
                {
                    id: "unit-sales",
                    unitCode: "SALES",
                    name: "Phòng Phát triển Kinh doanh",
                    manager: "Phạm Hoàng Nam",
                    count: 32,
                    unitType: "DEPARTMENT",
                    status: "ACTIVE",
                    description: "Tìm kiếm khách hàng doanh nghiệp và chăm sóc đối tác",
                    children: [],
                },
                {
                    id: "unit-mkt",
                    unitCode: "MKT",
                    name: "Phòng Truyền thông & Marketing",
                    manager: "Võ Ngọc Linh",
                    count: 23,
                    unitType: "DEPARTMENT",
                    status: "ACTIVE",
                    description: "Quảng cáo trực tuyến, quan hệ công chúng và sự kiện",
                    children: [],
                },
            ],
        },
    ],
};

function getUnitMeta(type: UnitType) {
    switch (type) {
        case "COMPANY":
            return {
                icon: Building2,
                label: "Công ty",
                color: "text-amber-700 bg-amber-50 border-amber-200",
            };
        case "CENTER":
            return {
                icon: GitBranch,
                label: "Khối",
                color: "text-blue-700 bg-blue-50 border-blue-200",
            };
        case "DEPARTMENT":
            return {
                icon: Users,
                label: "Phòng ban",
                color: "text-emerald-700 bg-emerald-50 border-emerald-200",
            };
        case "TEAM":
            return {
                icon: User,
                label: "Nhóm",
                color: "text-purple-700 bg-purple-50 border-purple-200",
            };
    }
}

function findNode(root: DepartmentNode, id: string): DepartmentNode | null {
    if (root.id === id) return root;
    for (const child of root.children) {
        const found = findNode(child, id);
        if (found) return found;
    }
    return null;
}

function findParent(root: DepartmentNode, id: string): DepartmentNode | null {
    for (const child of root.children) {
        if (child.id === id) return root;
        const found = findParent(child, id);
        if (found) return found;
    }
    return null;
}

function isDescendant(root: DepartmentNode, parentId: string, targetId: string): boolean {
    const parent = findNode(root, parentId);
    if (!parent) return false;
    return findNode(parent, targetId) !== null;
}

function cloneTree(node: DepartmentNode): DepartmentNode {
    return {
        ...node,
        children: node.children.map(cloneTree),
    };
}

let newIdCounter = 100;
function nextId() {
    newIdCounter += 1;
    return `unit-custom-${Date.now()}-${newIdCounter}`;
}

type ModalState =
    | { mode: "create"; parentId: string }
    | { mode: "edit"; node: DepartmentNode }
    | null;

function mapOrgUnitNodeToDepartmentNode(node: OrgUnitTreeNode): DepartmentNode {
    return {
        id: String(node.id),
        name: node.unitName,
        unitCode: node.unitCode,
        manager: node.managerId ? `Quản lý #${node.managerId}` : "Chưa chỉ định",
        count: (node.children ? node.children.length : 0) * 4 + 6,
        unitType: node.unitType,
        status: node.status === "ACTIVE" ? "ACTIVE" : "INACTIVE",
        description: node.description || undefined,
        children: node.children ? node.children.map(mapOrgUnitNodeToDepartmentNode) : [],
    };
}

export default function DepartmentTree() {
    const [tree, setTree] = useState<DepartmentNode>(INITIAL_DEPARTMENT_TREE);
    const [collapsed, setCollapsed] = useState<Set<string>>(new Set());
    const [searchQuery, setSearchQuery] = useState("");
    const [draggedId, setDraggedId] = useState<string | null>(null);
    const [dropTargetId, setDropTargetId] = useState<string | null>(null);
    const [modal, setModal] = useState<ModalState>(null);
    const [deleteTargetNode, setDeleteTargetNode] = useState<DepartmentNode | null>(null);
    const [notification, setNotification] = useState<{ message: string; type: "success" | "error" } | null>(null);

    // Tải cấu trúc cây đơn vị từ Backend API khi mở trang
    useEffect(() => {
        let isMounted = true;
        async function fetchOrgTree() {
            try {
                const res = await getOrgTree();
                if (isMounted && res && res.length > 0) {
                    setTree(mapOrgUnitNodeToDepartmentNode(res[0]));
                }
            } catch {
                // Backend offline: Tự động giữ cây ban đầu để không gián đoạn thao tác
            }
        }
        fetchOrgTree();
        return () => {
            isMounted = false;
        };
    }, []);

    const q = searchQuery.trim().toLowerCase();

    function showNotify(msg: string, type: "success" | "error" = "success") {
        setNotification({ message: msg, type });
        setTimeout(() => setNotification(null), 4000);
    }

    // Toggle collapse
    function toggleCollapse(id: string) {
        setCollapsed((prev) => {
            const next = new Set(prev);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    }

    function expandAll() {
        setCollapsed(new Set());
    }

    function collectExpandable(node: DepartmentNode, acc: string[]) {
        if (node.children.length > 0 && node.id !== tree.id) {
            acc.push(node.id);
            node.children.forEach((c) => collectExpandable(c, acc));
        }
    }

    function collapseAll() {
        const ids: string[] = [];
        collectExpandable(tree, ids);
        setCollapsed(new Set(ids));
    }

    // Drag and drop reparenting
    function canDrop(sourceId: string | null, targetId: string): boolean {
        if (!sourceId || sourceId === targetId) return false;
        if (sourceId === tree.id) return false;
        if (isDescendant(tree, sourceId, targetId)) return false;
        return true;
    }

    async function handleDropNode(sourceId: string, targetId: string) {
        if (!canDrop(sourceId, targetId)) return;

        const sNum = parseInt(sourceId, 10);
        const tNum = parseInt(targetId, 10);
        if (!isNaN(sNum) && !isNaN(tNum)) {
            try {
                await moveOrgUnit(sNum, tNum);
                setTree((current) => {
                    const root = cloneTree(current);
                    const parent = findParent(root, sourceId);
                    const target = findNode(root, targetId);
                    const dragged = findNode(root, sourceId);

                    if (!parent || !target || !dragged) return current;

                    // Remove from old parent
                    parent.children = parent.children.filter((c) => c.id !== sourceId);
                    // Append to new target parent
                    target.children.push(dragged);

                    // Auto expand target
                    setCollapsed((prev) => {
                        const next = new Set(prev);
                        next.delete(target.id);
                        return next;
                    });

                    showNotify(`Đã chuyển "${dragged.name}" vào trực thuộc "${target.name}"`);
                    return root;
                });
            } catch (err: any) {
                console.error("Lỗi khi di chuyển phòng ban:", err);
                const msg = err?.message || "Di chuyển đơn vị thất bại do lỗi từ máy chủ (400/403/500). Cây cơ cấu được giữ nguyên.";
                showNotify(msg, "error");
            }
        } else {
            setTree((current) => {
                const root = cloneTree(current);
                const parent = findParent(root, sourceId);
                const target = findNode(root, targetId);
                const dragged = findNode(root, sourceId);

                if (!parent || !target || !dragged) return current;

                // Remove from old parent
                parent.children = parent.children.filter((c) => c.id !== sourceId);
                // Append to new target parent
                target.children.push(dragged);

                // Auto expand target
                setCollapsed((prev) => {
                    const next = new Set(prev);
                    next.delete(target.id);
                    return next;
                });

                showNotify(`Đã chuyển "${dragged.name}" vào trực thuộc "${target.name}"`);
                return root;
            });
        }
    }

    // Toggle status Lock/Unlock
    async function handleToggleStatus(id: string) {
        if (id === tree.id) return;

        const numId = parseInt(id, 10);
        const nodeTarget = findNode(tree, id);
        if (!isNaN(numId) && nodeTarget) {
            try {
                if (nodeTarget.status === "ACTIVE") {
                    await deactivateOrgUnit(numId);
                } else {
                    await activateOrgUnit(numId);
                }
                setTree((current) => {
                    const root = cloneTree(current);
                    const node = findNode(root, id);
                    if (node) {
                        node.status = node.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
                        showNotify(`Đã ${node.status === "ACTIVE" ? "mở khóa" : "tạm khóa"} "${node.name}"`);
                    }
                    return root;
                });
            } catch (err: any) {
                console.error("Lỗi khi đổi trạng thái đơn vị:", err);
                const msg = err?.message || "Không thể thay đổi trạng thái đơn vị do lỗi từ máy chủ.";
                showNotify(msg, "error");
            }
        } else {
            setTree((current) => {
                const root = cloneTree(current);
                const node = findNode(root, id);
                if (node) {
                    node.status = node.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
                    showNotify(`Đã ${node.status === "ACTIVE" ? "mở khóa" : "tạm khóa"} "${node.name}"`);
                }
                return root;
            });
        }
    }

    // Yêu cầu xóa đơn vị -> Mở Dialog xác nhận
    function handleRequestDelete(node: DepartmentNode) {
        if (node.id === tree.id) return;
        setDeleteTargetNode(node);
    }

    // Xác nhận xóa đơn vị sau khi người dùng bấm xác nhận trên Dialog
    async function handleConfirmDelete() {
        if (!deleteTargetNode) return;
        const targetId = deleteTargetNode.id;
        const targetName = deleteTargetNode.name;

        const numId = parseInt(targetId, 10);
        if (!isNaN(numId)) {
            try {
                await deactivateOrgUnit(numId);
                setTree((current) => {
                    const root = cloneTree(current);
                    const parent = findParent(root, targetId);
                    if (parent) {
                        parent.children = parent.children.filter((c) => c.id !== targetId);
                        showNotify(`Đã xóa đơn vị "${targetName}"`);
                    }
                    return root;
                });
                setDeleteTargetNode(null);
            } catch (err: any) {
                console.error("Lỗi khi xóa đơn vị:", err);
                const msg = err?.message || "Không thể xóa đơn vị do lỗi từ máy chủ.";
                showNotify(msg, "error");
            }
        } else {
            setTree((current) => {
                const root = cloneTree(current);
                const parent = findParent(root, targetId);
                if (parent) {
                    parent.children = parent.children.filter((c) => c.id !== targetId);
                    showNotify(`Đã xóa đơn vị "${targetName}"`);
                }
                return root;
            });
            setDeleteTargetNode(null);
        }
    }

    // Save modal
    async function handleSaveModal(data: {
        name: string;
        unitCode: string;
        manager: string;
        count: number;
        unitType: UnitType;
        description?: string;
    }) {
        if (!modal) return;

        if (modal.mode === "edit") {
            const numId = parseInt(modal.node.id, 10);
            if (!isNaN(numId)) {
                try {
                    await updateOrgUnit(numId, {
                        unitName: data.name,
                        unitType: data.unitType,
                        description: data.description,
                    });
                    setTree((current) => {
                        const root = cloneTree(current);
                        const node = findNode(root, modal.node.id);
                        if (node) {
                            node.name = data.name;
                            node.unitCode = data.unitCode;
                            node.manager = data.manager;
                            node.count = data.count;
                            node.unitType = data.unitType;
                            node.description = data.description;
                            showNotify(`Đã cập nhật thông tin "${data.name}"`);
                        }
                        return root;
                    });
                    setModal(null);
                } catch (err: any) {
                    console.error("Lỗi khi cập nhật đơn vị:", err);
                    const msg = err?.message || "Cập nhật đơn vị thất bại do lỗi từ máy chủ.";
                    showNotify(msg, "error");
                }
            } else {
                setTree((current) => {
                    const root = cloneTree(current);
                    const node = findNode(root, modal.node.id);
                    if (node) {
                        node.name = data.name;
                        node.unitCode = data.unitCode;
                        node.manager = data.manager;
                        node.count = data.count;
                        node.unitType = data.unitType;
                        node.description = data.description;
                        showNotify(`Đã cập nhật thông tin "${data.name}"`);
                    }
                    return root;
                });
                setModal(null);
            }
        } else if (modal.mode === "create") {
            const parentNum = parseInt(modal.parentId, 10);
            let createdId = nextId();
            try {
                const res = await createOrgUnit({
                    unitCode: data.unitCode,
                    unitName: data.name,
                    unitType: data.unitType,
                    parentId: !isNaN(parentNum) ? parentNum : null,
                    description: data.description,
                });
                if (res?.id) {
                    createdId = String(res.id);
                }
                setTree((current) => {
                    const root = cloneTree(current);
                    const parent = findNode(root, modal.parentId);
                    if (parent) {
                        const newNode: DepartmentNode = {
                            id: createdId,
                            name: data.name,
                            unitCode: data.unitCode.toUpperCase(),
                            manager: data.manager,
                            count: data.count,
                            unitType: data.unitType,
                            status: "ACTIVE",
                            description: data.description,
                            children: [],
                        };
                        parent.children.push(newNode);
                        setCollapsed((prev) => {
                            const next = new Set(prev);
                            next.delete(parent.id);
                            return next;
                        });
                        showNotify(`Đã tạo mới đơn vị "${data.name}"`);
                    }
                    return root;
                });
                setModal(null);
            } catch (err: any) {
                console.error("Lỗi khi tạo mới đơn vị:", err);
                const msg = err?.message || "Không thể tạo mới đơn vị do lỗi từ máy chủ.";
                showNotify(msg, "error");
            }
        }
    }

    return (
        <div className="flex-1 min-h-0 flex flex-col space-y-3">
            {/* Notification Toast */}
            {notification && (
                <div
                    className={cn(
                        "fixed top-5 right-5 z-50 rounded-xl border px-4 py-2.5 text-xs font-semibold shadow-xl animate-in fade-in slide-in-from-top-3 duration-200",
                        notification.type === "success"
                            ? "border-emerald-300 bg-white text-emerald-700"
                            : "border-rose-300 bg-white text-rose-700"
                    )}
                >
                    {notification.type === "success" ? `✓ ${notification.message}` : `⚠ ${notification.message}`}
                </div>
            )}

            {/* Top Toolbar (White Card) */}
            <div className="shrink-0 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200/80 bg-white p-3.5 shadow-sm text-slate-800">
                {/* Search */}
                <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 w-full sm:w-80 transition focus-within:border-indigo-500 focus-within:bg-white focus-within:ring-2 focus-within:ring-indigo-100">
                    <Search className="h-4 w-4 text-slate-400 shrink-0" />
                    <input
                        type="text"
                        placeholder="Tìm theo tên, mã phòng, trưởng phòng..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="bg-transparent text-xs text-slate-800 placeholder-slate-400 focus:outline-none w-full"
                    />
                    {searchQuery && (
                        <button
                            onClick={() => setSearchQuery("")}
                            className="text-xs text-slate-400 hover:text-slate-600"
                        >
                            ✕
                        </button>
                    )}
                </div>

                {/* Toolbar Buttons */}
                <div className="flex items-center gap-2">
                    <button
                        onClick={expandAll}
                        className="flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 hover:text-slate-900 active:scale-95"
                        title="Mở rộng tất cả các nhánh"
                        type="button"
                    >
                        <Maximize2 className="h-3.5 w-3.5 text-slate-500" />
                        <span className="hidden sm:inline">Mở rộng tất cả</span>
                    </button>
                    <button
                        onClick={collapseAll}
                        className="flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 hover:text-slate-900 active:scale-95"
                        title="Thu gọn các nhánh"
                        type="button"
                    >
                        <Minimize2 className="h-3.5 w-3.5 text-slate-500" />
                        <span className="hidden sm:inline">Thu gọn</span>
                    </button>

                    <button
                        onClick={() => setModal({ mode: "create", parentId: tree.id })}
                        className="flex items-center gap-1.5 rounded-xl border border-indigo-600 bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-sm transition hover:bg-indigo-700 hover:shadow-md active:scale-95"
                        type="button"
                    >
                        <Plus className="h-4 w-4" />
                        <span>Thêm đơn vị mới</span>
                    </button>
                </div>
            </div>

            {/* Tree Main Card (Crisp White Card with Slim Local Scrollbar) */}
            <div className="flex-1 min-h-0 flex flex-col rounded-2xl border border-slate-200/80 bg-white p-5 shadow-lg text-slate-800 overflow-hidden">
                <div className="shrink-0 mb-3 flex items-center justify-between border-b border-slate-100 pb-3 px-2 text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                    <span>Cơ cấu phân cấp & Tên đơn vị</span>
                    <div className="hidden sm:flex items-center gap-8 pr-28">
                        <span>Trưởng đơn vị</span>
                        <span>Nhân sự</span>
                        <span>Trạng thái</span>
                    </div>
                </div>

                <div className="flex-1 min-h-0 overflow-y-auto pr-2 space-y-1.5 [scrollbar-width:thin] [scrollbar-color:#cbd5e1_transparent] [&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:bg-slate-200 [&::-webkit-scrollbar-thumb]:rounded-full hover:[&::-webkit-scrollbar-thumb]:bg-slate-300">
                    <TreeNodeItem
                        node={tree}
                        level={0}
                        isRoot={true}
                        collapsed={collapsed}
                        onToggle={toggleCollapse}
                        draggedId={draggedId}
                        dropTargetId={dropTargetId}
                        onDragStart={(id) => setDraggedId(id)}
                        onDragEnd={() => {
                            setDraggedId(null);
                            setDropTargetId(null);
                        }}
                        onDragOver={(targetId) => {
                            if (canDrop(draggedId, targetId)) {
                                setDropTargetId(targetId);
                            }
                        }}
                        onDrop={(targetId) => {
                            if (draggedId && canDrop(draggedId, targetId)) {
                                handleDropNode(draggedId, targetId);
                            }
                            setDraggedId(null);
                            setDropTargetId(null);
                        }}
                        onAddChild={(parentId) => setModal({ mode: "create", parentId })}
                        onEdit={(node) => setModal({ mode: "edit", node })}
                        onToggleStatus={handleToggleStatus}
                        onDelete={handleRequestDelete}
                        searchQuery={q}
                    />
                </div>
            </div>

            {/* Modal Create/Edit */}
            {modal && (
                <DepartmentTreeModal
                    modal={modal}
                    onClose={() => setModal(null)}
                    onSave={handleSaveModal}
                />
            )}

            {/* Dialog xác nhận xóa đơn vị */}
            {deleteTargetNode && (
                <DepartmentDeleteDialog
                    target={deleteTargetNode}
                    onClose={() => setDeleteTargetNode(null)}
                    onConfirm={handleConfirmDelete}
                />
            )}
        </div>
    );
}

// ----------------------------------------------------------------------
// Recursive Tree Item Component with Indent & Drag-Drop Reparenting
// ----------------------------------------------------------------------
interface TreeNodeItemProps {
    node: DepartmentNode;
    level: number;
    isRoot?: boolean;
    collapsed: Set<string>;
    onToggle: (id: string) => void;
    draggedId: string | null;
    dropTargetId: string | null;
    onDragStart: (id: string) => void;
    onDragEnd: () => void;
    onDragOver: (targetId: string) => void;
    onDrop: (targetId: string) => void;
    onAddChild: (parentId: string) => void;
    onEdit: (node: DepartmentNode) => void;
    onToggleStatus: (id: string) => void;
    onDelete: (node: DepartmentNode) => void;
    searchQuery: string;
}

function TreeNodeItem({
    node,
    level,
    isRoot = false,
    collapsed,
    onToggle,
    draggedId,
    dropTargetId,
    onDragStart,
    onDragEnd,
    onDragOver,
    onDrop,
    onAddChild,
    onEdit,
    onToggleStatus,
    onDelete,
    searchQuery,
}: TreeNodeItemProps) {
    const hasChildren = node.children && node.children.length > 0;
    const isCollapsed = collapsed.has(node.id) && !searchQuery;
    const isDraggable = !isRoot;
    const isBeingDragged = draggedId === node.id;
    const isTarget = dropTargetId === node.id;
    const isInactive = node.status === "INACTIVE";

    const meta = getUnitMeta(node.unitType);
    const IconComponent = meta.icon;

    // Check if matches search
    const isMatch =
        searchQuery.length > 0 &&
        (node.name.toLowerCase().includes(searchQuery) ||
            node.unitCode.toLowerCase().includes(searchQuery) ||
            node.manager.toLowerCase().includes(searchQuery));

    // Check if any child matches search
    const hasMatchingDescendant = useMemo(() => {
        if (!searchQuery) return false;
        function checkChild(n: DepartmentNode): boolean {
            if (
                n.name.toLowerCase().includes(searchQuery) ||
                n.unitCode.toLowerCase().includes(searchQuery) ||
                n.manager.toLowerCase().includes(searchQuery)
            ) {
                return true;
            }
            return n.children.some(checkChild);
        }
        return node.children.some(checkChild);
    }, [node.children, searchQuery]);

    if (searchQuery && !isMatch && !hasMatchingDescendant) {
        return null;
    }

    return (
        <div className="flex flex-col">
            {/* Node Row (Crisp White Card row) */}
            <div
                draggable={isDraggable}
                onDragStart={(e) => {
                    if (!isDraggable) return;
                    e.dataTransfer.setData("text/plain", node.id);
                    onDragStart(node.id);
                }}
                onDragEnd={onDragEnd}
                onDragOver={(e) => {
                    e.preventDefault();
                    onDragOver(node.id);
                }}
                onDrop={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    onDrop(node.id);
                }}
                className={cn(
                    "group flex items-center justify-between rounded-xl border border-slate-100/90 bg-white px-3.5 py-2.5 transition-all duration-150 select-none shadow-[0_1px_2px_rgba(0,0,0,0.03)]",
                    isTarget
                        ? "bg-emerald-50 border-emerald-500 ring-2 ring-emerald-300 shadow-md scale-[1.01]"
                        : "hover:border-slate-300 hover:bg-slate-50/90 hover:shadow-sm",
                    isBeingDragged ? "opacity-30 border-dashed border-slate-400 scale-95" : "",
                    isMatch ? "bg-amber-50 border-amber-300 ring-1 ring-amber-300" : "",
                    isInactive ? "opacity-60 bg-slate-50" : ""
                )}
                style={{ paddingLeft: `${level * 24 + 12}px` }}
            >
                {/* Left Part: Toggle, Grip, Icon, Name, Code */}
                <div className="flex items-center gap-2.5 min-w-0 flex-1 mr-3">
                    {/* Expand/Collapse Toggle */}
                    {hasChildren ? (
                        <button
                            type="button"
                            onClick={(e) => {
                                e.stopPropagation();
                                onToggle(node.id);
                            }}
                            className="flex h-6 w-6 shrink-0 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
                        >
                            {isCollapsed ? (
                                <ChevronRight className="h-4 w-4" />
                            ) : (
                                <ChevronDown className="h-4 w-4" />
                            )}
                        </button>
                    ) : (
                        <div className="w-6 shrink-0" />
                    )}

                    {/* Grip handle for Drag and Drop */}
                    {isDraggable ? (
                        <span
                            className="cursor-grab active:cursor-grabbing p-1 text-slate-300 hover:text-slate-600 transition shrink-0"
                            title="Kéo thả để chuyển đơn vị trực thuộc"
                        >
                            <GripVertical className="h-4 w-4" />
                        </span>
                    ) : (
                        <div className="w-4 shrink-0" />
                    )}

                    {/* Unit Type Icon */}
                    <div
                        className={cn(
                            "flex h-8 w-8 shrink-0 items-center justify-center rounded-xl border shadow-xs",
                            meta.color
                        )}
                    >
                        <IconComponent className="h-4 w-4" />
                    </div>

                    {/* Unit Name & Code */}
                    <div className="min-w-0 flex items-center gap-2 flex-wrap">
                        <span className="font-bold text-sm text-slate-800 tracking-tight truncate">
                            {node.name}
                        </span>
                        <span className="rounded-md border border-slate-200 bg-slate-100 px-1.5 py-0.5 text-[10px] font-mono font-semibold text-slate-600">
                            {node.unitCode}
                        </span>
                        <span
                            className={cn(
                                "rounded-full border px-2 py-0.2 text-[10px] font-bold uppercase",
                                meta.color
                            )}
                        >
                            {meta.label}
                        </span>
                        {hasChildren && (
                            <span className="rounded-full bg-slate-100 text-slate-600 px-2 py-0.5 text-[10px] font-semibold">
                                {node.children.length} nhánh con
                            </span>
                        )}
                    </div>
                </div>

                {/* Right Part: Manager, Count, Status, Actions */}
                <div className="flex items-center gap-4 shrink-0">
                    {/* Manager Name */}
                    <div className="hidden sm:flex items-center gap-1.5 text-xs text-slate-600 w-36 truncate">
                        <UserCheck className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                        <span className="truncate font-medium">{node.manager}</span>
                    </div>

                    {/* Members Count */}
                    <div className="hidden sm:block text-xs font-semibold text-slate-500 w-24">
                        {node.count} nhân sự
                    </div>

                    {/* Status Badge */}
                    <div className="hidden sm:block w-20">
                        <span
                            className={cn(
                                "inline-block rounded-full px-2.5 py-0.5 text-[10px] font-bold uppercase",
                                isInactive
                                    ? "bg-rose-50 text-rose-700 border border-rose-200"
                                    : "bg-emerald-50 text-emerald-700 border border-emerald-200"
                            )}
                        >
                            {isInactive ? "Tạm khóa" : "Hoạt động"}
                        </span>
                    </div>

                    {/* Action Buttons */}
                    <div className="flex items-center gap-1">
                        {/* Add Child */}
                        <button
                            type="button"
                            onClick={() => onAddChild(node.id)}
                            className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-emerald-50 hover:text-emerald-600 transition"
                            title="Thêm đơn vị con trực thuộc"
                        >
                            <Plus className="h-3.5 w-3.5" />
                        </button>

                        {/* Edit */}
                        <button
                            type="button"
                            onClick={() => onEdit(node)}
                            className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
                            title="Chỉnh sửa thông tin"
                        >
                            <Pencil className="h-3.5 w-3.5" />
                        </button>

                        {/* Lock / Unlock */}
                        {!isRoot && (
                            <button
                                type="button"
                                onClick={() => onToggleStatus(node.id)}
                                className={cn(
                                    "flex h-7 w-7 items-center justify-center rounded-lg transition",
                                    isInactive
                                        ? "text-emerald-600 hover:bg-emerald-50"
                                        : "text-amber-600 hover:bg-amber-50"
                                )}
                                title={isInactive ? "Mở khóa đơn vị này" : "Tạm khóa đơn vị này"}
                            >
                                {isInactive ? (
                                    <Unlock className="h-3.5 w-3.5" />
                                ) : (
                                    <Lock className="h-3.5 w-3.5" />
                                )}
                            </button>
                        )}

                        {/* Delete */}
                        {!isRoot && (
                            <button
                                type="button"
                                className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-rose-50 hover:text-rose-600 transition"
                                title="Xóa đơn vị này"
                            >
                                <Trash2 className="h-3.5 w-3.5" />
                            </button>
                        )}
                    </div>
                </div>
            </div>

            {/* Nested Children */}
            {hasChildren && !isCollapsed && (
                <div className="relative border-l-2 border-slate-200 ml-6 pl-1.5 space-y-1.5 mt-1.5">
                    {node.children.map((child) => (
                        <TreeNodeItem
                            key={child.id}
                            node={child}
                            level={level + 1}
                            isRoot={false}
                            collapsed={collapsed}
                            onToggle={onToggle}
                            draggedId={draggedId}
                            dropTargetId={dropTargetId}
                            onDragStart={onDragStart}
                            onDragEnd={onDragEnd}
                            onDragOver={onDragOver}
                            onDrop={onDrop}
                            onAddChild={onAddChild}
                            onEdit={onEdit}
                            onToggleStatus={onToggleStatus}
                            onDelete={onDelete}
                            searchQuery={searchQuery}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}

// ----------------------------------------------------------------------
// Modal Form Component for Adding / Editing Department Node
// ----------------------------------------------------------------------
interface DepartmentTreeModalProps {
    modal: NonNullable<ModalState>;
    onClose: () => void;
    onSave: (data: {
        name: string;
        unitCode: string;
        manager: string;
        count: number;
        unitType: UnitType;
        description?: string;
    }) => void;
}

function DepartmentTreeModal({ modal, onClose, onSave }: DepartmentTreeModalProps) {
    const isEdit = modal.mode === "edit";

    const [name, setName] = useState(isEdit ? modal.node.name : "");
    const [unitCode, setUnitCode] = useState(isEdit ? modal.node.unitCode : "");
    const [manager, setManager] = useState(isEdit ? modal.node.manager : "");
    const [count, setCount] = useState<number>(isEdit ? modal.node.count : 0);
    const [unitType, setUnitType] = useState<UnitType>(isEdit ? modal.node.unitType : "DEPARTMENT");
    const [description, setDescription] = useState(isEdit ? modal.node.description ?? "" : "");
    const [error, setError] = useState("");

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        if (!name.trim()) {
            setError("Vui lòng nhập tên đơn vị / phòng ban.");
            return;
        }
        if (!unitCode.trim()) {
            setError("Vui lòng nhập mã đơn vị.");
            return;
        }

        onSave({
            name: name.trim(),
            unitCode: unitCode.trim().toUpperCase(),
            manager: manager.trim() || "Chưa chỉ định",
            count: count || 0,
            unitType,
            description: description.trim() || undefined,
        });
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-150">
            <div className="relative w-full max-w-md rounded-3xl border border-slate-100 bg-white p-6 shadow-2xl text-slate-800">
                <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-4">
                    <h3 className="text-base font-bold text-slate-900">
                        {isEdit ? "Chỉnh sửa đơn vị / phòng ban" : "Thêm đơn vị mới"}
                    </h3>
                    <button
                        onClick={onClose}
                        className="rounded-xl p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                {error && (
                    <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 p-2.5 text-xs font-medium text-rose-700">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="space-y-1.5">
                        <label className="text-xs font-semibold text-slate-700">
                            Tên đơn vị / phòng ban *
                        </label>
                        <input
                            type="text"
                            required
                            placeholder="VD: Phòng Lập trình Frontend"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            className="w-full rounded-2xl border border-slate-200 bg-slate-50/60 px-4 py-2.5 text-xs font-semibold text-slate-800 placeholder-slate-400 outline-none focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100 transition"
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1.5">
                            <label className="text-xs font-semibold text-slate-700">
                                Mã đơn vị *
                            </label>
                            <input
                                type="text"
                                required
                                placeholder="VD: TECH-FE"
                                value={unitCode}
                                onChange={(e) => setUnitCode(e.target.value)}
                                className="w-full rounded-2xl border border-slate-200 bg-slate-50/60 px-4 py-2.5 text-xs font-semibold text-slate-800 placeholder-slate-400 uppercase outline-none focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100 transition"
                            />
                        </div>

                        <div className="space-y-1.5">
                            <label className="text-xs font-semibold text-slate-700">
                                Loại phân cấp *
                            </label>
                            <select
                                value={unitType}
                                onChange={(e) => setUnitType(e.target.value as UnitType)}
                                className="w-full rounded-2xl border border-slate-200 bg-slate-50/60 px-3.5 py-2.5 text-xs font-semibold text-slate-800 outline-none focus:border-indigo-500 focus:bg-white transition"
                            >
                                <option value="CENTER">Khối (Center)</option>
                                <option value="DEPARTMENT">Phòng ban (Dept)</option>
                                <option value="TEAM">Tổ / Nhóm (Team)</option>
                                <option value="COMPANY">Công ty (Company)</option>
                            </select>
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1.5">
                            <label className="text-xs font-semibold text-slate-700">
                                Trưởng đơn vị
                            </label>
                            <input
                                type="text"
                                placeholder="VD: Nguyễn Văn A"
                                value={manager}
                                onChange={(e) => setManager(e.target.value)}
                                className="w-full rounded-2xl border border-slate-200 bg-slate-50/60 px-4 py-2.5 text-xs font-semibold text-slate-800 placeholder-slate-400 outline-none focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100 transition"
                            />
                        </div>

                        <div className="space-y-1.5">
                            <label className="text-xs font-semibold text-slate-700">
                                Số lượng nhân sự
                            </label>
                            <input
                                type="number"
                                min={0}
                                placeholder="12"
                                value={count}
                                onChange={(e) => setCount(Number(e.target.value) || 0)}
                                className="w-full rounded-2xl border border-slate-200 bg-slate-50/60 px-4 py-2.5 text-xs font-semibold text-slate-800 placeholder-slate-400 outline-none focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100 transition"
                            />
                        </div>
                    </div>

                    <div className="space-y-1.5">
                        <label className="text-xs font-semibold text-slate-700">
                            Mô tả chức năng
                        </label>
                        <textarea
                            rows={2}
                            placeholder="Mô tả tóm tắt nhiệm vụ và mục tiêu..."
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            className="w-full rounded-2xl border border-slate-200 bg-slate-50/60 px-4 py-2.5 text-xs font-medium text-slate-800 placeholder-slate-400 outline-none focus:border-indigo-500 focus:bg-white transition resize-none"
                        />
                    </div>

                    <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
                        <button
                            type="button"
                            onClick={onClose}
                            className="rounded-2xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:text-slate-900 transition"
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            className="rounded-2xl border border-indigo-600 bg-indigo-600 px-5 py-2 text-xs font-bold text-white hover:bg-indigo-700 transition shadow-sm active:scale-95"
                        >
                            {isEdit ? "Lưu thay đổi" : "Tạo đơn vị"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

/* ========================================================================
   DEPARTMENT DELETE CONFIRMATION DIALOG (Thay thế window.confirm)
   ======================================================================== */
interface DepartmentDeleteDialogProps {
    target: DepartmentNode | null;
    onClose: () => void;
    onConfirm: () => void;
}

function DepartmentDeleteDialog({ target, onClose, onConfirm }: DepartmentDeleteDialogProps) {
    if (!target) return null;

    const meta = getUnitMeta(target.unitType);
    const childCount = target.children ? target.children.length : 0;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-sm animate-in fade-in duration-150">
            <div className="relative w-full max-w-md rounded-3xl border border-slate-200/90 bg-white p-6 shadow-2xl text-slate-800">
                {/* Header with Warning Icon */}
                <div className="flex items-start gap-4">
                    <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl border border-rose-200 bg-rose-50 text-rose-600 shadow-2xs">
                        <AlertTriangle className="size-6" />
                    </div>
                    <div className="min-w-0 flex-1">
                        <h3 className="text-base font-bold text-slate-900">
                            Xác nhận xóa đơn vị / phòng ban
                        </h3>
                        <p className="mt-1 text-xs text-slate-500 leading-relaxed">
                            Bạn có chắc chắn muốn xóa đơn vị này khỏi cơ cấu phân cấp của doanh nghiệp?
                        </p>
                    </div>
                    <button
                        onClick={onClose}
                        className="rounded-xl p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
                    >
                        <X className="size-4" />
                    </button>
                </div>

                {/* Target Unit Summary Card */}
                <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50/80 p-3.5 space-y-2.5">
                    <div className="flex items-center gap-3">
                        <div className="flex size-9 items-center justify-center rounded-xl bg-white border border-slate-200 font-bold text-indigo-600 text-xs shadow-2xs">
                            <Building2 className="size-4.5" />
                        </div>
                        <div className="min-w-0 flex-1">
                            <div className="flex items-center gap-2">
                                <p className="truncate text-xs font-bold text-slate-900">
                                    {target.name}
                                </p>
                                <span className={cn("rounded-full border px-2 py-0.2 text-[9px] font-bold uppercase", meta.color)}>
                                    {meta.label}
                                </span>
                            </div>
                            <p className="text-[11px] text-slate-500 font-mono">
                                Mã: {target.unitCode}
                            </p>
                        </div>
                    </div>

                    <div className="pt-2 border-t border-slate-200/70 text-[11px] text-slate-600 grid grid-cols-2 gap-2">
                        <div>
                            <span className="text-slate-400">Trưởng đơn vị: </span>
                            <span className="font-semibold text-slate-800">{target.manager}</span>
                        </div>
                        <div>
                            <span className="text-slate-400">Nhân sự: </span>
                            <span className="font-semibold text-slate-800">{target.count} người</span>
                        </div>
                    </div>

                    {childCount > 0 && (
                        <div className="rounded-xl border border-amber-200 bg-amber-50 p-2.5 text-xs text-amber-800 leading-relaxed">
                            ⚠️ <strong>Cảnh báo:</strong> Đơn vị này hiện có <strong>{childCount} đơn vị/phòng ban con trực thuộc</strong>. Khi xóa, tất cả các nhánh con bên dưới cũng sẽ bị xóa khỏi hệ thống!
                        </div>
                    )}
                </div>

                {/* Action Buttons */}
                <div className="mt-6 flex items-center justify-end gap-3">
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 shadow-xs transition hover:bg-slate-50"
                    >
                        Hủy
                    </button>
                    <button
                        type="button"
                        onClick={onConfirm}
                        className="flex items-center gap-1.5 rounded-xl border border-rose-600 bg-rose-600 px-4 py-2 text-xs font-semibold text-white shadow-xs transition hover:bg-rose-700 active:scale-95"
                    >
                        <Trash2 className="size-3.5" />
                        <span>Xác nhận xóa</span>
                    </button>
                </div>
            </div>
        </div>
    );
}
