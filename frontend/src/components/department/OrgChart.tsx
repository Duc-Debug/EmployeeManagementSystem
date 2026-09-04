import React, { useState, useRef, useMemo, useEffect } from 'react';
import {
    Plus,
    Pencil,
    Trash2,
    ZoomIn,
    ZoomOut,
    RotateCcw,
    ChevronDown,
    ChevronRight,
    GripVertical,
    Search,
    Crown,
    BarChart2,
    Archive,
    Code,
    User,
    Users,
    Shield,
    UserCheck,
} from 'lucide-react';
import OrgNodeModal from './OrgNodeModal';
import OrgNodeDetailModal from './OrgNodeDetailModal';
import type { CardData } from './orgNode.constants';
import {
    getOrgTree,
    createOrgUnit,
    updateOrgUnit,
    moveOrgUnit,
    deactivateOrgUnit,
} from '@/lib/api/org-units';
import { getUsers } from '@/lib/api/users';
import { cn } from '@/lib/utils';
import type { OrgUnitTreeNode } from '@/types/hrm';

export interface OrgTreeNode extends CardData {
    id: string;
    children: OrgTreeNode[];
}

const INITIAL_ORG_TREE: OrgTreeNode = {
    id: 'board-root',
    badge: 'CẤP CAO NHẤT',
    badgeBg: 'bg-amber-100',
    badgeColor: 'text-amber-800',
    title: 'Ban Giám Đốc',
    desc: 'Quyết định chiến lược & Tầm nhìn',
    manager: 'Nguyễn Văn A (Tổng Giám Đốc)',
    subLeft: 'Hội đồng Quản trị',
    levelText: 'Tầng 1',
    cardBg: 'bg-white',
    borderColor: 'border-amber-300',
    icon: Crown,
    iconColor: 'text-amber-600',
    isDark: false,
    children: [
        {
            id: 'node-pm',
            badge: 'Quản Lý Vận Hành',
            badgeBg: 'bg-blue-50',
            badgeColor: 'text-blue-700',
            title: 'Quản Lý Dự Án',
            desc: 'Điều phối tiến độ & mục tiêu sản phẩm',
            manager: 'Trần Quốc Bảo (Giám Đốc Dự Án)',
            subLeft: 'Báo cáo trực tiếp BGĐ',
            levelText: 'Tầng 2',
            cardBg: 'bg-white',
            borderColor: 'border-blue-200',
            icon: BarChart2,
            iconColor: 'text-blue-600',
            children: [
                {
                    id: 'node-dev',
                    badge: 'Thực Thi Chuyên Môn',
                    badgeBg: 'bg-emerald-50',
                    badgeColor: 'text-emerald-700',
                    title: 'Nhân Viên Chuyên Môn',
                    desc: 'Kỹ sư, Lập trình viên, Designer & Chuyên gia',
                    manager: 'Lê Văn C (Trưởng Nhóm Kỹ Thuật)',
                    subLeft: 'Thuộc Khối Dự Án',
                    levelText: 'Tầng 3',
                    cardBg: 'bg-white',
                    borderColor: 'border-emerald-200',
                    icon: Code,
                    iconColor: 'text-emerald-600',
                    children: [],
                },
            ],
        },
        {
            id: 'node-rm',
            badge: 'Quản Lý Nguồn Lực',
            badgeBg: 'bg-purple-50',
            badgeColor: 'text-purple-700',
            title: 'Quản Lý Nguồn Lực',
            desc: 'Tối ưu hóa nhân lực & cơ sở hạ tầng',
            manager: 'Phạm Thị D (Giám Đốc Nguồn Lực)',
            subLeft: 'Báo cáo trực tiếp BGĐ',
            levelText: 'Tầng 2',
            cardBg: 'bg-white',
            borderColor: 'border-purple-200',
            icon: Archive,
            iconColor: 'text-purple-600',
            children: [
                {
                    id: 'node-hr',
                    badge: 'Phòng Khối Nhân Sự',
                    badgeBg: 'bg-pink-50',
                    badgeColor: 'text-pink-700',
                    title: 'Nhân Sự (HR)',
                    desc: 'Tuyển dụng, đào tạo & chế độ phúc lợi',
                    manager: 'Lê Thị Mai (Trưởng Phòng HR)',
                    subLeft: 'Trực thuộc Nguồn Lực',
                    levelText: 'Tầng 3',
                    cardBg: 'bg-white',
                    borderColor: 'border-pink-200',
                    icon: Users,
                    iconColor: 'text-pink-600',
                    children: [
                        {
                            id: 'node-admin',
                            badge: 'Vận Hành & Admin',
                            badgeBg: 'bg-amber-50',
                            badgeColor: 'text-amber-700',
                            title: 'Quản Trị Viên',
                            desc: 'Quản lý hệ thống, nội quy & tài sản',
                            manager: 'Đặng Hữu Tài (Quản Trị Hệ Thống)',
                            subLeft: 'Giám sát vận hành',
                            levelText: 'Tầng 4',
                            cardBg: 'bg-white',
                            borderColor: 'border-amber-200',
                            icon: Shield,
                            iconColor: 'text-amber-600',
                            children: [],
                        },
                    ],
                },
            ],
        },
    ],
};

function findNode(root: OrgTreeNode, id: string): OrgTreeNode | null {
    if (root.id === id) return root;
    for (const child of root.children) {
        const res = findNode(child, id);
        if (res) return res;
    }
    return null;
}

function findParentNode(root: OrgTreeNode, id: string): OrgTreeNode | null {
    for (const child of root.children) {
        if (child.id === id) return root;
        const res = findParentNode(child, id);
        if (res) return res;
    }
    return null;
}

function isDescendant(root: OrgTreeNode, parentId: string, targetId: string): boolean {
    const parent = findNode(root, parentId);
    if (!parent) return false;
    return findNode(parent, targetId) !== null;
}

function cloneTree(node: OrgTreeNode): OrgTreeNode {
    return {
        ...node,
        children: node.children.map(cloneTree),
    };
}

let nodeIdCounter = 100;
function nextNodeId() {
    nodeIdCounter += 1;
    return `node-${Date.now()}-${nodeIdCounter}`;
}

type EditTarget =
    | { kind: 'edit'; nodeId: string }
    | { kind: 'addChild'; parentId: string }
    | null;

function getMetaForUnit(unitType?: string, level?: number) {
    if (unitType === 'COMPANY' || level === 1) {
        return {
            badge: 'CẤP CAO NHẤT',
            badgeBg: 'bg-amber-100',
            badgeColor: 'text-amber-800',
            borderColor: 'border-amber-300',
            iconColor: 'text-amber-600',
            icon: Crown,
        };
    }
    if (unitType === 'CENTER' || level === 2) {
        return {
            badge: 'KHỐI TRUNG TÂM',
            badgeBg: 'bg-blue-50',
            badgeColor: 'text-blue-700',
            borderColor: 'border-blue-200',
            iconColor: 'text-blue-600',
            icon: BarChart2,
        };
    }
    if (unitType === 'DEPARTMENT' || level === 3) {
        return {
            badge: 'PHÒNG BAN',
            badgeBg: 'bg-indigo-50',
            badgeColor: 'text-indigo-700',
            borderColor: 'border-indigo-200',
            iconColor: 'text-indigo-600',
            icon: Users,
        };
    }
    return {
        badge: 'TỔ NHÓM',
        badgeBg: 'bg-purple-50',
        badgeColor: 'text-purple-700',
        borderColor: 'border-purple-200',
        iconColor: 'text-purple-600',
        icon: User,
    };
}

function convertBackendNodeToOrgChartNode(
    node: OrgUnitTreeNode,
    userMap: Map<number, string>,
    parentName: string | null
): OrgTreeNode {
    const meta = getMetaForUnit(node.unitType, node.level);
    const managerName = node.managerId
        ? userMap.get(node.managerId) || `Quản lý #${node.managerId}`
        : 'Chưa bổ nhiệm';

    return {
        id: String(node.id),
        badge: meta.badge,
        badgeBg: meta.badgeBg,
        badgeColor: meta.badgeColor,
        borderColor: meta.borderColor,
        iconColor: meta.iconColor,
        icon: meta.icon,
        title: node.unitName,
        desc: node.description || 'Chức năng, vai trò và mục tiêu phòng ban',
        manager: managerName,
        subLeft: parentName ? `Trực thuộc: ${parentName}` : 'Hội đồng Quản trị',
        levelText: `Tầng ${node.level || 1}`,
        cardBg: 'bg-white',
        children: node.children
            ? node.children.map((c: OrgUnitTreeNode) => convertBackendNodeToOrgChartNode(c, userMap, node.unitName))
            : [],
    };
}

export default function OrgChart() {
    const [tree, setTree] = useState<OrgTreeNode>(INITIAL_ORG_TREE);
    const [collapsedNodes, setCollapsedNodes] = useState<Set<string>>(new Set());
    const [draggedNodeId, setDraggedNodeId] = useState<string | null>(null);
    const [dropTargetId, setDropTargetId] = useState<string | null>(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [editTarget, setEditTarget] = useState<EditTarget>(null);
    const [detailNode, setDetailNode] = useState<OrgTreeNode | null>(null);
    const [notification, setNotification] = useState<{ message: string; type: "success" | "error" } | null>(null);

    function showNotify(msg: string, type: "success" | "error" = "success") {
        setNotification({ message: msg, type });
        setTimeout(() => setNotification(null), 4000);
    }

    // Tải dữ liệu thật từ Backend API (GET /api/v1/org-units/tree)
    useEffect(() => {
        let isMounted = true;
        async function loadBackendData() {
            try {
                const [treeRes, usersRes] = await Promise.all([
                    getOrgTree(),
                    getUsers(0, 100).catch(() => null),
                ]);
                if (!isMounted) return;
                const userMap = new Map<number, string>();
                if (usersRes?.content) {
                    usersRes.content.forEach((u) => userMap.set(u.id, u.fullName || u.username));
                }
                if (treeRes && treeRes.length > 0) {
                    const converted = convertBackendNodeToOrgChartNode(treeRes[0], userMap, null);
                    setTree(converted);
                }
            } catch (err) {
                console.warn('Backend API chưa sẵn sàng hoặc lỗi, dùng dữ liệu dự phòng:', err);
            }
        }
        loadBackendData();
        return () => {
            isMounted = false;
        };
    }, []);

    // Pan and Zoom
    const [zoom, setZoom] = useState(1);
    const [pan, setPan] = useState({ x: 0, y: 0 });
    const [isPanning, setIsPanning] = useState(false);
    const panStartRef = useRef({ startX: 0, startY: 0, initialPanX: 0, initialPanY: 0 });
    const canvasRef = useRef<HTMLDivElement>(null);

    function zoomIn() {
        setZoom((z) => Math.min(1.8, Math.round((z + 0.1) * 10) / 10));
    }

    function zoomOut() {
        setZoom((z) => Math.max(0.35, Math.round((z - 0.1) * 10) / 10));
    }

    function resetZoom() {
        setZoom(1);
        setPan({ x: 0, y: 0 });
    }

    function toggleCollapse(nodeId: string, e: React.MouseEvent) {
        e.stopPropagation();
        setCollapsedNodes((prev) => {
            const next = new Set(prev);
            if (next.has(nodeId)) {
                next.delete(nodeId);
            } else {
                next.add(nodeId);
            }
            return next;
        });
    }

    // Canvas Mouse Panning Handlers
    function handleMouseDown(e: React.MouseEvent<HTMLDivElement>) {
        if (e.button !== 0) return;
        const target = e.target as HTMLElement;
        if (target.closest('.org-tree-card') || target.closest('button') || target.closest('input')) {
            return;
        }
        setIsPanning(true);
        panStartRef.current = {
            startX: e.clientX,
            startY: e.clientY,
            initialPanX: pan.x,
            initialPanY: pan.y,
        };
    }

    function handleMouseMove(e: React.MouseEvent<HTMLDivElement>) {
        if (!isPanning) return;
        const dx = e.clientX - panStartRef.current.startX;
        const dy = e.clientY - panStartRef.current.startY;
        setPan({
            x: panStartRef.current.initialPanX + dx,
            y: panStartRef.current.initialPanY + dy,
        });
    }

    function handleMouseUp() {
        setIsPanning(false);
    }

    function handleWheel(e: React.WheelEvent<HTMLDivElement>) {
        e.preventDefault();
        const delta = e.deltaY < 0 ? 0.08 : -0.08;
        setZoom((z) => Math.min(1.8, Math.max(0.35, +(z + delta).toFixed(2))));
    }

    // Drag and drop validation
    function canDrop(sourceId: string | null, targetId: string): boolean {
        if (!sourceId || sourceId === targetId) return false;
        if (sourceId === tree.id) return false;
        if (isDescendant(tree, sourceId, targetId)) return false;
        return true;
    }

    async function handleDropNode(sourceId: string, targetId: string) {
        if (!canDrop(sourceId, targetId)) return;

        // Gửi API chuyển nhánh trực thuộc lên backend (PATCH /api/v1/org-units/{id}/move)
        const sNum = parseInt(sourceId, 10);
        const tNum = parseInt(targetId, 10);
        if (!isNaN(sNum) && !isNaN(tNum)) {
            try {
                await moveOrgUnit(sNum, tNum);
                setTree((current) => {
                    const root = cloneTree(current);
                    const parent = findParentNode(root, sourceId);
                    const target = findNode(root, targetId);
                    const dragged = findNode(root, sourceId);

                    if (!parent || !target || !dragged) return current;

                    parent.children = parent.children.filter((c) => c.id !== sourceId);
                    dragged.subLeft = `Trực thuộc: ${target.title}`;
                    target.children.push(dragged);

                    return root;
                });
                showNotify('Đã chuyển nhánh trực thuộc thành công.');
            } catch (err: any) {
                console.error('Lỗi khi gọi API moveOrgUnit:', err);
                const msg = err?.message || 'Không thể di chuyển đơn vị do máy chủ phản hồi lỗi (400/403/500). Sơ đồ giữ nguyên.';
                showNotify(msg, 'error');
            }
        } else {
            setTree((current) => {
                const root = cloneTree(current);
                const parent = findParentNode(root, sourceId);
                const target = findNode(root, targetId);
                const dragged = findNode(root, sourceId);

                if (!parent || !target || !dragged) return current;

                parent.children = parent.children.filter((c) => c.id !== sourceId);
                dragged.subLeft = `Trực thuộc: ${target.title}`;
                target.children.push(dragged);

                return root;
            });
        }
    }

    // Modal Add / Edit / Delete Handlers
    async function handleSaveNode(card: CardData) {
        if (!editTarget) return;

        if (editTarget.kind === 'edit') {
            // Gửi API cập nhật phòng ban lên backend (PUT /api/v1/org-units/{id})
            const numId = parseInt(editTarget.nodeId, 10);
            if (!isNaN(numId)) {
                try {
                    await updateOrgUnit(numId, {
                        unitName: card.title,
                        unitType: 'DEPARTMENT',
                        description: card.desc,
                    });
                    setTree((current) => {
                        const root = cloneTree(current);
                        const node = findNode(root, editTarget.nodeId);
                        if (node) {
                            node.badge = card.badge;
                            node.badgeBg = card.badgeBg;
                            node.badgeColor = card.badgeColor;
                            node.title = card.title;
                            node.desc = card.desc;
                            node.manager = card.manager;
                            node.subLeft = card.subLeft;
                            node.icon = card.icon;
                            node.iconColor = card.iconColor;
                            node.cardBg = card.cardBg;
                            node.borderColor = card.borderColor;
                        }
                        return root;
                    });
                    showNotify(`Cập nhật đơn vị "${card.title}" thành công.`);
                    setEditTarget(null);
                } catch (err: any) {
                    console.error('Lỗi khi gọi API updateOrgUnit:', err);
                    const msg = err?.message || 'Cập nhật đơn vị thất bại do máy chủ phản hồi lỗi.';
                    showNotify(msg, 'error');
                }
            } else {
                setTree((current) => {
                    const root = cloneTree(current);
                    const node = findNode(root, editTarget.nodeId);
                    if (node) {
                        node.badge = card.badge;
                        node.badgeBg = card.badgeBg;
                        node.badgeColor = card.badgeColor;
                        node.title = card.title;
                        node.desc = card.desc;
                        node.manager = card.manager;
                        node.subLeft = card.subLeft;
                        node.icon = card.icon;
                        node.iconColor = card.iconColor;
                        node.cardBg = card.cardBg;
                        node.borderColor = card.borderColor;
                    }
                    return root;
                });
                setEditTarget(null);
            }
        } else if (editTarget.kind === 'addChild') {
            // Gửi API tạo mới phòng ban con lên backend (POST /api/v1/org-units)
            const parentNum = parseInt(editTarget.parentId, 10);
            let createdId = nextNodeId();

            try {
                const code = `PB-${Date.now().toString().slice(-4)}`;
                const res = await createOrgUnit({
                    unitCode: code,
                    unitName: card.title,
                    unitType: 'DEPARTMENT',
                    parentId: !isNaN(parentNum) ? parentNum : null,
                    description: card.desc,
                });
                if (res?.id) {
                    createdId = String(res.id);
                }
                setTree((current) => {
                    const root = cloneTree(current);
                    const parent = findNode(root, editTarget.parentId);
                    if (parent) {
                        const parentLvl = parseInt(parent.levelText.replace(/\D/g, '') || '1', 10);
                        const newChild: OrgTreeNode = {
                            ...card,
                            id: createdId,
                            levelText: `Tầng ${parentLvl + 1}`,
                            children: [],
                        };
                        parent.children.push(newChild);
                        setCollapsedNodes((prev) => {
                            const next = new Set(prev);
                            next.delete(parent.id);
                            return next;
                        });
                    }
                    return root;
                });
                showNotify(`Thêm đơn vị con "${card.title}" thành công.`);
                setEditTarget(null);
            } catch (err: any) {
                console.error('Lỗi khi gọi API createOrgUnit:', err);
                const msg = err?.message || 'Tạo mới đơn vị thất bại do máy chủ phản hồi lỗi.';
                showNotify(msg, 'error');
            }
        }
    }

    async function handleDeleteNode() {
        if (!editTarget || editTarget.kind !== 'edit' || editTarget.nodeId === tree.id) return;

        // Gửi API vô hiệu hóa phòng ban (PATCH /api/v1/org-units/{id}/deactivate)
        const numId = parseInt(editTarget.nodeId, 10);
        if (!isNaN(numId)) {
            try {
                await deactivateOrgUnit(numId);
                setTree((current) => {
                    const root = cloneTree(current);
                    const parent = findParentNode(root, editTarget.nodeId);
                    if (parent) {
                        parent.children = parent.children.filter((c) => c.id !== editTarget.nodeId);
                    }
                    return root;
                });
                showNotify('Đã xóa/vô hiệu hóa đơn vị thành công.');
                setEditTarget(null);
            } catch (err: any) {
                console.error('Lỗi khi gọi API deactivateOrgUnit:', err);
                const msg = err?.message || 'Xóa đơn vị thất bại do máy chủ phản hồi lỗi.';
                showNotify(msg, 'error');
            }
        } else {
            setTree((current) => {
                const root = cloneTree(current);
                const parent = findParentNode(root, editTarget.nodeId);
                if (parent) {
                    parent.children = parent.children.filter((c) => c.id !== editTarget.nodeId);
                }
                return root;
            });
            setEditTarget(null);
        }
    }

    async function handleDirectDelete(nodeId: string) {
        if (nodeId === tree.id) return;

        // Gửi API vô hiệu hóa phòng ban (PATCH /api/v1/org-units/{id}/deactivate)
        const numId = parseInt(nodeId, 10);
        if (!isNaN(numId)) {
            try {
                await deactivateOrgUnit(numId);
                setTree((current) => {
                    const root = cloneTree(current);
                    const parent = findParentNode(root, nodeId);
                    if (parent) {
                        parent.children = parent.children.filter((c) => c.id !== nodeId);
                    }
                    return root;
                });
                showNotify('Đã xóa/vô hiệu hóa đơn vị thành công.');
            } catch (err: any) {
                console.error('Lỗi khi gọi API deactivateOrgUnit:', err);
                const msg = err?.message || 'Xóa đơn vị thất bại do máy chủ phản hồi lỗi.';
                showNotify(msg, 'error');
            }
        } else {
            setTree((current) => {
                const root = cloneTree(current);
                const parent = findParentNode(root, nodeId);
                if (parent) {
                    parent.children = parent.children.filter((c) => c.id !== nodeId);
                }
                return root;
            });
        }
    }

    const currentModalData = useMemo(() => {
        if (!editTarget) return null;
        if (editTarget.kind === 'edit') {
            return findNode(tree, editTarget.nodeId);
        }
        return null;
    }, [editTarget, tree]);

    const modalLevelText = useMemo(() => {
        if (!editTarget) return '';
        if (editTarget.kind === 'edit') {
            return findNode(tree, editTarget.nodeId)?.levelText ?? '';
        }
        const parent = findNode(tree, editTarget.parentId);
        const parentLvl = parseInt(parent?.levelText.replace(/\D/g, '') || '1', 10);
        return `Tầng ${parentLvl + 1}`;
    }, [editTarget, tree]);

    const normalizedQuery = searchQuery.trim().toLowerCase();

    return (
        <div
            ref={canvasRef}
            className={`relative w-full h-full overflow-hidden flex flex-col font-sans select-none ${
                isPanning ? 'cursor-grabbing' : 'cursor-grab'
            }`}
            onMouseDown={handleMouseDown}
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
            onMouseLeave={handleMouseUp}
            onWheel={handleWheel}
        >
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

            {/* Top Floating Controls */}
            <div className="absolute top-4 left-4 right-4 z-30 flex flex-wrap items-center justify-between gap-3 px-2 pointer-events-none">
                {/* Search input */}
                <div className="pointer-events-auto flex items-center gap-2 rounded-xl border border-slate-200 bg-white/95 px-3 py-1.5 shadow-md">
                    <Search className="h-4 w-4 text-slate-400" />
                    <input
                        type="text"
                        placeholder="Tìm kiếm phòng ban / vị trí..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="bg-transparent text-xs text-slate-800 placeholder-slate-400 focus:outline-none w-48 sm:w-64"
                    />
                    {searchQuery && (
                        <button
                            onClick={() => setSearchQuery('')}
                            className="text-xs text-slate-400 hover:text-slate-700"
                        >
                            ✕
                        </button>
                    )}
                </div>

                {/* Toolbar */}
                <div className="pointer-events-auto flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white/95 px-3 py-1.5 shadow-md">
                    <button
                        onClick={zoomOut}
                        className="rounded-lg p-1.5 text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition"
                        title="Thu nhỏ"
                        type="button"
                    >
                        <ZoomOut className="h-4 w-4" />
                    </button>
                    <span className="min-w-[44px] text-center text-xs font-bold text-slate-800">
                        {Math.round(zoom * 100)}%
                    </span>
                    <button
                        onClick={zoomIn}
                        className="rounded-lg p-1.5 text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition"
                        title="Phóng to"
                        type="button"
                    >
                        <ZoomIn className="h-4 w-4" />
                    </button>

                    <div className="h-4 w-[1px] bg-slate-200 mx-0.5" />

                    <button
                        onClick={resetZoom}
                        className="flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-100 hover:text-slate-900 transition"
                        title="Đặt lại kích thước chuẩn"
                        type="button"
                    >
                        <RotateCcw className="h-3.5 w-3.5" />
                        <span>100%</span>
                    </button>
                </div>
            </div>

            {/* Tree Canvas Viewport */}
            <div className="w-full h-full flex-1 flex justify-center items-start pt-20 pb-28 px-12 overflow-hidden">
                <div
                    className="flex justify-center items-start transition-transform duration-100 ease-out origin-top min-w-max"
                    style={{
                        transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`,
                    }}
                >
                    <RecursiveNode
                        node={tree}
                        isRoot={true}
                        collapsedNodes={collapsedNodes}
                        onToggleCollapse={toggleCollapse}
                        draggedNodeId={draggedNodeId}
                        dropTargetId={dropTargetId}
                        onDragStart={(id) => setDraggedNodeId(id)}
                        onDragEnd={() => {
                            setDraggedNodeId(null);
                            setDropTargetId(null);
                        }}
                        onDragOver={(id) => {
                            if (canDrop(draggedNodeId, id)) {
                                setDropTargetId(id);
                            }
                        }}
                        onDrop={(targetId) => {
                            if (draggedNodeId && canDrop(draggedNodeId, targetId)) {
                                handleDropNode(draggedNodeId, targetId);
                            }
                            setDraggedNodeId(null);
                            setDropTargetId(null);
                        }}
                        onEdit={(id) => setEditTarget({ kind: 'edit', nodeId: id })}
                        onAddChild={(parentId) => setEditTarget({ kind: 'addChild', parentId })}
                        onDelete={handleDirectDelete}
                        onViewDetail={(n) => setDetailNode(n)}
                        searchQuery={normalizedQuery}
                    />
                </div>
            </div>

            {/* Modal Chi tiết phòng ban (Bấm vào icon 6 chấm) */}
            <OrgNodeDetailModal
                open={Boolean(detailNode)}
                node={detailNode}
                onClose={() => setDetailNode(null)}
                onEdit={(id) => {
                    setDetailNode(null);
                    setEditTarget({ kind: 'edit', nodeId: id });
                }}
            />

            {/* Modal Edit / Add Node */}
            <OrgNodeModal
                open={editTarget !== null}
                initialData={currentModalData}
                levelText={modalLevelText}
                onClose={() => setEditTarget(null)}
                onSave={handleSaveNode}
                onDelete={
                    editTarget?.kind === 'edit' && editTarget.nodeId !== tree.id
                        ? handleDeleteNode
                        : undefined
                }
            />
        </div>
    );
}

// -------------------------------------------------------------
// Recursive Node Component with Dynamic Infinite Subtree Width
// -------------------------------------------------------------
interface RecursiveNodeProps {
    node: OrgTreeNode;
    isRoot?: boolean;
    collapsedNodes: Set<string>;
    onToggleCollapse: (nodeId: string, e: React.MouseEvent) => void;
    draggedNodeId: string | null;
    dropTargetId: string | null;
    onDragStart: (id: string) => void;
    onDragEnd: () => void;
    onDragOver: (id: string) => void;
    onDrop: (targetId: string) => void;
    onEdit: (id: string) => void;
    onAddChild: (parentId: string) => void;
    onDelete: (id: string) => void;
    onViewDetail: (node: OrgTreeNode) => void;
    searchQuery: string;
}

function RecursiveNode({
    node,
    isRoot = false,
    collapsedNodes,
    onToggleCollapse,
    draggedNodeId,
    dropTargetId,
    onDragStart,
    onDragEnd,
    onDragOver,
    onDrop,
    onEdit,
    onAddChild,
    onDelete,
    onViewDetail,
    searchQuery,
}: RecursiveNodeProps) {
    const hasChildren = node.children && node.children.length > 0;
    const isCollapsed = collapsedNodes.has(node.id) && !searchQuery;
    const isDraggable = !isRoot;
    const isDropTarget = dropTargetId === node.id;
    const isBeingDragged = draggedNodeId === node.id;

    const isMatch =
        searchQuery.length > 0 &&
        (node.title.toLowerCase().includes(searchQuery) ||
            node.desc.toLowerCase().includes(searchQuery) ||
            node.badge.toLowerCase().includes(searchQuery) ||
            (Boolean(node.manager) && node.manager!.toLowerCase().includes(searchQuery)));

    return (
        <div className="flex flex-col items-center min-w-max px-4">
            {/* Card Element */}
            <div
                draggable={isDraggable}
                onDragStart={(e) => {
                    if (!isDraggable) return;
                    e.dataTransfer.setData('text/plain', node.id);
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
                className={`org-tree-card relative w-[350px] rounded-3xl p-5 border transition-all duration-200 cursor-default bg-white border-slate-200/90 shadow-sm ${
                    isDropTarget
                        ? 'ring-4 ring-indigo-500 ring-offset-2 ring-offset-transparent bg-indigo-50/50 scale-[1.03] shadow-md'
                        : 'hover:shadow-md hover:border-indigo-300'
                } ${isBeingDragged ? 'opacity-35 scale-95 border-dashed' : ''} ${
                    isMatch ? 'ring-4 ring-amber-400/80 shadow-md' : ''
                }`}
            >
                {/* Header Row: Badge (Left), Actions (Right) */}
                <div className="flex items-center justify-between gap-2 mb-2.5">
                    <span
                        className={`rounded-full px-2.5 py-0.5 text-[10px] font-bold tracking-wider uppercase shrink-0 ${node.badgeBg} ${node.badgeColor}`}
                    >
                        {node.badge}
                    </span>

                    {/* Actions: 6-dot (Detail for ALL), Edit, Add, Delete */}
                    <div className="flex items-center gap-0.5 shrink-0">
                        <button
                            onClick={() => onViewDetail(node)}
                            className="rounded-lg p-1.5 text-slate-400 hover:bg-indigo-50 hover:text-indigo-600 transition"
                            title="Xem chi tiết phòng ban"
                            type="button"
                        >
                            <GripVertical className="h-4 w-4" />
                        </button>
                        <button
                            onClick={() => onEdit(node.id)}
                            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
                            title="Chỉnh sửa thông tin"
                            type="button"
                        >
                            <Pencil className="h-3.5 w-3.5" />
                        </button>
                        <button
                            onClick={() => onAddChild(node.id)}
                            className="rounded-lg p-1.5 text-slate-400 hover:bg-indigo-50 hover:text-indigo-600 transition"
                            title="Thêm nhánh con trực thuộc"
                            type="button"
                        >
                            <Plus className="h-3.5 w-3.5" />
                        </button>
                        {isDraggable && (
                            <button
                                onClick={() => onDelete(node.id)}
                                className="rounded-lg p-1.5 text-slate-400 hover:bg-rose-50 hover:text-rose-600 transition"
                                title="Xóa nhánh phòng ban này"
                                type="button"
                            >
                                <Trash2 className="h-3.5 w-3.5" />
                            </button>
                        )}
                    </div>
                </div>

                {/* Title Row: Icon + Full Department Name (Placed below badge row) */}
                <div className="flex items-center gap-2 mb-2">
                    {node.icon && <node.icon className={`h-5 w-5 ${node.iconColor} shrink-0`} />}
                    <h3 className="text-base font-bold text-slate-900 tracking-tight leading-snug">
                        {node.title}
                    </h3>
                </div>

                {/* Description */}
                {node.desc && (
                    <p className="text-xs text-slate-500 leading-relaxed font-medium mb-3 min-h-[32px]">
                        {node.desc}
                    </p>
                )}

                {/* Organizer / Manager Row */}
                <div className="flex items-center gap-2.5 rounded-xl bg-slate-50/90 border border-slate-100 px-3 py-2 mb-3 text-xs">
                    <div className="flex size-6 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600 shrink-0">
                        <UserCheck className="size-3.5" />
                    </div>
                    <div className="min-w-0 flex-1">
                        <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 block leading-none mb-0.5">
                            Người tổ chức
                        </span>
                        <span className="font-semibold text-slate-800 text-xs truncate block">
                            {node.manager || "Chưa bổ nhiệm"}
                        </span>
                    </div>
                </div>

                {/* Footer */}
                <div className="border-t border-slate-100 pt-3 flex items-center justify-between text-xs font-semibold">
                    <span className="text-[11px] font-medium text-slate-400">{node.subLeft}</span>
                    <div className="flex items-center gap-2">
                        {hasChildren && (
                            <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-600">
                                {node.children.length} nhánh
                            </span>
                        )}
                        <span className="font-bold text-slate-700">{node.levelText}</span>
                    </div>
                </div>

                {/* Collapse / Expand Toggle */}
                {hasChildren && (
                    <button
                        onClick={(e) => onToggleCollapse(node.id, e)}
                        className="absolute -bottom-3 left-1/2 -translate-x-1/2 flex items-center justify-center h-6 w-6 rounded-full border border-slate-200 bg-white text-slate-600 hover:bg-indigo-50 hover:text-indigo-600 hover:scale-110 shadow-xs transition-all z-10"
                        title={isCollapsed ? 'Mở rộng nhánh' : 'Thu gọn nhánh'}
                        type="button"
                    >
                        {isCollapsed ? (
                            <ChevronRight className="h-3.5 w-3.5" />
                        ) : (
                            <ChevronDown className="h-3.5 w-3.5" />
                        )}
                    </button>
                )}
            </div>

            {/* Tree Branch Connectors & Children Nodes */}
            {hasChildren && !isCollapsed && (
                <div className="flex flex-col items-center w-full min-w-max">
                    {/* Vertical drop line directly below parent */}
                    <div className="h-8 w-[2px] bg-slate-300" />

                    {/* Children Container (Horizontal Flex Row with seamless continuous bus line) */}
                    <div className="flex flex-row items-start justify-center min-w-max">
                        {node.children.map((child, idx) => {
                            const isFirst = idx === 0;
                            const isLast = idx === node.children.length - 1;
                            const isOnly = node.children.length === 1;

                            return (
                                <div
                                    key={child.id}
                                    className="flex flex-col items-center min-w-max relative"
                                >
                                    {/* Seamless continuous horizontal bus line & vertical drop line */}
                                    {!isOnly && (
                                        <div className="w-full h-8 relative mb-1">
                                            <div
                                                className={`absolute top-0 h-[2px] bg-slate-300 ${
                                                    isFirst
                                                        ? 'left-1/2 right-0'
                                                        : isLast
                                                        ? 'left-0 right-1/2'
                                                        : 'left-0 right-0'
                                                }`}
                                            />
                                            <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[2px] h-full bg-slate-300" />
                                        </div>
                                    )}
                                    {isOnly && <div className="h-8 w-[2px] bg-slate-300 mb-1" />}

                                    {/* Recursive Child Node */}
                                    <RecursiveNode
                                        node={child}
                                        collapsedNodes={collapsedNodes}
                                        onToggleCollapse={onToggleCollapse}
                                        draggedNodeId={draggedNodeId}
                                        dropTargetId={dropTargetId}
                                        onDragStart={onDragStart}
                                        onDragEnd={onDragEnd}
                                        onDragOver={onDragOver}
                                        onDrop={onDrop}
                                        onEdit={onEdit}
                                        onAddChild={onAddChild}
                                        onDelete={onDelete}
                                        onViewDetail={onViewDetail}
                                        searchQuery={searchQuery}
                                    />
                                </div>
                            );
                        })}
                    </div>
                </div>
            )}
        </div>
    );
}
