import React, { useState, useRef, useMemo } from 'react';
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
    Users,
    Shield,
} from 'lucide-react';
import OrgNodeModal from './OrgNodeModal';
import type { CardData } from './orgNode.constants';

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
    subLeft: 'Hội đồng Quản trị',
    levelText: 'Tầng 1',
    cardBg: 'bg-black/[100]',
    borderColor: 'border-amber-200',
    icon: Crown,
    iconColor: 'text-amber-600',
    isDark: true,
    children: [
        {
            id: 'node-pm',
            badge: 'Quản Lý Vận Hành',
            badgeBg: 'bg-blue-400/20',
            badgeColor: 'text-blue-200',
            title: 'Quản Lý Dự Án',
            desc: 'Điều phối tiến độ & mục tiêu sản phẩm',
            subLeft: 'Báo cáo trực tiếp BGĐ',
            levelText: 'Tầng 2',
            cardBg: 'bg-white/[0.07]',
            borderColor: 'border-blue-300/30',
            icon: BarChart2,
            iconColor: 'text-blue-300',
            children: [
                {
                    id: 'node-dev',
                    badge: 'Thực Thi Chuyên Môn',
                    badgeBg: 'bg-emerald-400/20',
                    badgeColor: 'text-emerald-200',
                    title: 'Nhân Viên Chuyên Môn',
                    desc: 'Kỹ sư, Lập trình viên, Designer & Chuyên gia',
                    subLeft: 'Thuộc Khối Dự Án',
                    levelText: 'Tầng 3',
                    cardBg: 'bg-white/[0.07]',
                    borderColor: 'border-emerald-300/30',
                    icon: Code,
                    iconColor: 'text-emerald-300',
                    children: [],
                },
            ],
        },
        {
            id: 'node-rm',
            badge: 'Quản Lý Nguồn Lực',
            badgeBg: 'bg-purple-400/20',
            badgeColor: 'text-purple-200',
            title: 'Quản Lý Nguồn Lực',
            desc: 'Tối ưu hóa nhân lực & cơ sở hạ tầng',
            subLeft: 'Báo cáo trực tiếp BGĐ',
            levelText: 'Tầng 2',
            cardBg: 'bg-white/[0.07]',
            borderColor: 'border-purple-300/30',
            icon: Archive,
            iconColor: 'text-purple-300',
            children: [
                {
                    id: 'node-hr',
                    badge: 'Phòng Khối Nhân Sự',
                    badgeBg: 'bg-pink-400/20',
                    badgeColor: 'text-pink-200',
                    title: 'Nhân Sự (HR)',
                    desc: 'Tuyển dụng, đào tạo & chế độ phúc lợi',
                    subLeft: 'Trực thuộc Nguồn Lực',
                    levelText: 'Tầng 3',
                    cardBg: 'bg-white/[0.07]',
                    borderColor: 'border-pink-300/30',
                    icon: Users,
                    iconColor: 'text-pink-300',
                    children: [
                        {
                            id: 'node-admin',
                            badge: 'Vận Hành & Admin',
                            badgeBg: 'bg-amber-400/20',
                            badgeColor: 'text-amber-200',
                            title: 'Quản Trị Viên',
                            desc: 'Quản lý hệ thống, nội quy & tài sản',
                            subLeft: 'Giám sát vận hành',
                            levelText: 'Tầng 4',
                            cardBg: 'bg-white/[0.07]',
                            borderColor: 'border-amber-300/30',
                            icon: Shield,
                            iconColor: 'text-amber-300',
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

export default function OrgChart() {
    const [tree, setTree] = useState<OrgTreeNode>(INITIAL_ORG_TREE);
    const [collapsedNodes, setCollapsedNodes] = useState<Set<string>>(new Set());
    const [draggedNodeId, setDraggedNodeId] = useState<string | null>(null);
    const [dropTargetId, setDropTargetId] = useState<string | null>(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [editTarget, setEditTarget] = useState<EditTarget>(null);

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

    function handleDropNode(sourceId: string, targetId: string) {
        if (!canDrop(sourceId, targetId)) return;

        setTree((current) => {
            const root = cloneTree(current);
            const parent = findParentNode(root, sourceId);
            const target = findNode(root, targetId);
            const dragged = findNode(root, sourceId);

            if (!parent || !target || !dragged) return current;

            parent.children = parent.children.filter((c) => c.id !== sourceId);
            target.children.push(dragged);

            return root;
        });
    }

    // Modal Add / Edit / Delete Handlers
    function handleSaveNode(card: CardData) {
        if (!editTarget) return;

        setTree((current) => {
            const root = cloneTree(current);

            if (editTarget.kind === 'edit') {
                const node = findNode(root, editTarget.nodeId);
                if (node) {
                    node.badge = card.badge;
                    node.badgeBg = card.badgeBg;
                    node.badgeColor = card.badgeColor;
                    node.title = card.title;
                    node.desc = card.desc;
                    node.subLeft = card.subLeft;
                    node.icon = card.icon;
                    node.iconColor = card.iconColor;
                    node.cardBg = card.cardBg;
                    node.borderColor = card.borderColor;
                }
            } else if (editTarget.kind === 'addChild') {
                const parent = findNode(root, editTarget.parentId);
                if (parent) {
                    const parentLvl = parseInt(parent.levelText.replace(/\D/g, '') || '1', 10);
                    const newChild: OrgTreeNode = {
                        ...card,
                        id: nextNodeId(),
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
            }
            return root;
        });
        setEditTarget(null);
    }

    function handleDeleteNode() {
        if (!editTarget || editTarget.kind !== 'edit' || editTarget.nodeId === tree.id) return;

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

    function handleDirectDelete(nodeId: string) {
        if (nodeId === tree.id) return;
        setTree((current) => {
            const root = cloneTree(current);
            const parent = findParentNode(root, nodeId);
            if (parent) {
                parent.children = parent.children.filter((c) => c.id !== nodeId);
            }
            return root;
        });
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
            {/* Top Floating Controls */}
            <div className="absolute top-4 left-4 right-4 z-30 flex flex-wrap items-center justify-between gap-3 px-2 pointer-events-none">
                {/* Search input */}
                <div className="pointer-events-auto flex items-center gap-2 rounded-xl border border-white/20 bg-slate-950/75 px-3 py-1.5 backdrop-blur-xl shadow-lg">
                    <Search className="h-4 w-4 text-white/50" />
                    <input
                        type="text"
                        placeholder="Tìm kiếm phòng ban / vị trí..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="bg-transparent text-xs text-white placeholder-white/40 focus:outline-none w-48 sm:w-64"
                    />
                    {searchQuery && (
                        <button
                            onClick={() => setSearchQuery('')}
                            className="text-xs text-white/50 hover:text-white"
                        >
                            ✕
                        </button>
                    )}
                </div>

                {/* Toolbar */}
                <div className="pointer-events-auto flex items-center gap-1.5 rounded-xl border border-white/20 bg-slate-950/75 px-3 py-1.5 backdrop-blur-xl shadow-lg">
                    <button
                        onClick={zoomOut}
                        className="rounded-lg p-1.5 text-white/70 hover:bg-white/15 hover:text-white transition"
                        title="Thu nhỏ"
                        type="button"
                    >
                        <ZoomOut className="h-4 w-4" />
                    </button>
                    <span className="min-w-[44px] text-center text-xs font-bold text-white/90">
                        {Math.round(zoom * 100)}%
                    </span>
                    <button
                        onClick={zoomIn}
                        className="rounded-lg p-1.5 text-white/70 hover:bg-white/15 hover:text-white transition"
                        title="Phóng to"
                        type="button"
                    >
                        <ZoomIn className="h-4 w-4" />
                    </button>

                    <div className="h-4 w-[1px] bg-white/20 mx-0.5" />

                    <button
                        onClick={resetZoom}
                        className="flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-semibold text-white/75 hover:bg-white/15 hover:text-white transition"
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
                        searchQuery={normalizedQuery}
                    />
                </div>
            </div>

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
            node.badge.toLowerCase().includes(searchQuery));

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
                className={`org-tree-card relative w-[320px] rounded-3xl p-5 backdrop-blur-xl border transition-all duration-200 cursor-default ${
                    node.cardBg
                } ${node.borderColor} ${
                    isDropTarget
                        ? 'ring-4 ring-[#63ecc8] ring-offset-2 ring-offset-transparent bg-[#63ecc8]/15 scale-[1.03] shadow-[0_0_25px_rgba(99,236,200,0.4)]'
                        : 'hover:shadow-2xl hover:border-white/40'
                } ${isBeingDragged ? 'opacity-35 scale-95 border-dashed' : ''} ${
                    isMatch ? 'ring-4 ring-amber-400/80 shadow-[0_0_25px_rgba(251,191,36,0.5)]' : ''
                }`}
            >
                {/* Actions */}
                <div className="absolute top-3 right-3 flex items-center gap-1">
                    {isDraggable && (
                        <span
                            className="cursor-grab active:cursor-grabbing p-1 text-white/40 hover:text-white transition"
                            title="Kéo thả để chuyển phòng ban trực thuộc"
                        >
                            <GripVertical className="h-4 w-4" />
                        </span>
                    )}
                    <button
                        onClick={() => onEdit(node.id)}
                        className="rounded-lg p-1.5 text-white/50 hover:bg-white/20 hover:text-white transition"
                        title="Chỉnh sửa thông tin"
                        type="button"
                    >
                        <Pencil className="h-3.5 w-3.5" />
                    </button>
                    <button
                        onClick={() => onAddChild(node.id)}
                        className="rounded-lg p-1.5 text-white/50 hover:bg-[#63ecc8]/20 hover:text-[#63ecc8] transition"
                        title="Thêm nhánh con trực thuộc"
                        type="button"
                    >
                        <Plus className="h-3.5 w-3.5" />
                    </button>
                    {isDraggable && (
                        <button
                            onClick={() => onDelete(node.id)}
                            className="rounded-lg p-1.5 text-white/40 hover:bg-rose-500/20 hover:text-rose-300 transition"
                            title="Xóa nhánh phòng ban này"
                            type="button"
                        >
                            <Trash2 className="h-3.5 w-3.5" />
                        </button>
                    )}
                </div>

                {/* Badge & Icon */}
                <div className="flex items-center justify-between mb-3 pr-20">
                    <span
                        className={`rounded-full px-3 py-1 text-[10px] font-bold tracking-wider uppercase ${node.badgeBg} ${node.badgeColor}`}
                    >
                        {node.badge}
                    </span>
                    {node.icon && <node.icon className={`h-5 w-5 ${node.iconColor} shrink-0`} />}
                </div>

                {/* Title & Desc */}
                <h3 className="text-lg font-bold text-white tracking-tight mb-1">{node.title}</h3>
                <p className="text-xs text-white/65 leading-relaxed font-medium mb-4 min-h-[32px]">
                    {node.desc}
                </p>

                {/* Footer */}
                <div className="border-t border-white/15 pt-3 flex items-center justify-between text-xs font-semibold">
                    <span className="text-[11px] font-medium text-white/60">{node.subLeft}</span>
                    <div className="flex items-center gap-2">
                        {hasChildren && (
                            <span className="rounded-full bg-white/10 px-2 py-0.5 text-[10px] text-white/70">
                                {node.children.length} nhánh
                            </span>
                        )}
                        <span className="font-bold text-white/90">{node.levelText}</span>
                    </div>
                </div>

                {/* Collapse / Expand Toggle */}
                {hasChildren && (
                    <button
                        onClick={(e) => onToggleCollapse(node.id, e)}
                        className="absolute -bottom-3 left-1/2 -translate-x-1/2 flex items-center justify-center h-6 w-6 rounded-full border border-white/20 bg-slate-900 text-white/80 hover:bg-white hover:text-slate-900 hover:scale-110 shadow-md transition-all z-10"
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
                    <div className="h-8 w-[2px] bg-white/30" />

                    {/* Children Container (Horizontal Flex Row) */}
                    <div className="flex flex-row items-start justify-center min-w-max gap-8">
                        {node.children.map((child, idx) => {
                            const isFirst = idx === 0;
                            const isLast = idx === node.children.length - 1;
                            const isOnly = node.children.length === 1;

                            return (
                                <div
                                    key={child.id}
                                    className="flex flex-col items-center min-w-max relative"
                                >
                                    {/* Horizontal bus line & vertical entry line into child */}
                                    {!isOnly && (
                                        <div className="w-full h-8 relative mb-1">
                                            <div
                                                className={`absolute top-0 h-[2px] bg-white/30 ${
                                                    isFirst
                                                        ? 'left-1/2 right-0'
                                                        : isLast
                                                        ? 'left-0 right-1/2'
                                                        : 'left-0 right-0'
                                                }`}
                                            />
                                            <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[2px] h-full bg-white/30" />
                                        </div>
                                    )}
                                    {isOnly && <div className="h-8 w-[2px] bg-white/30 mb-1" />}

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
