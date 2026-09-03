"use client";

import { useMemo, useRef, useState } from "react";

import { Icon } from "@/components/ui/Icon";
import type { OrgUnitTreeNode, OrgUnitType, User } from "@/src/types/hrm";

interface OrgChartDiagramProps {
  allUsers: readonly User[];
  canDrop?: (draggedUnitId: number, targetUnitId: number) => boolean;
  collapsedNodes?: ReadonlySet<number>;
  dragDisabled?: boolean;
  draggedUnitId?: number | null;
  dropTargetId?: number | null;
  onDragEnd?: () => void;
  onDragOver?: (event: React.DragEvent<HTMLElement>, targetUnitId: number) => void;
  onDragStart?: (event: React.DragEvent<HTMLElement>, unitId: number) => void;
  onDrop?: (event: React.DragEvent<HTMLElement>, targetUnitId: number) => void;
  onSelectUnit: (unitId: number) => void;
  onToggleCollapse?: (unitId: number, event: React.MouseEvent) => void;
  onZoomChange?: (newZoom: number) => void;
  query: string;
  selectedUnitId: number;
  units: readonly OrgUnitTreeNode[];
  zoom?: number;
}

function getUnitMeta(unitType: OrgUnitType) {
  switch (unitType) {
    case "COMPANY":
      return { className: "unit-tag--company", icon: "building" as const, label: "Công ty" };
    case "CENTER":
      return { className: "unit-tag--center", icon: "branch" as const, label: "Khối" };
    case "DEPARTMENT":
      return { className: "unit-tag--dept", icon: "users" as const, label: "Phòng ban" };
    case "TEAM":
      return { className: "unit-tag--team", icon: "user" as const, label: "Nhóm" };
    default:
      return { className: "unit-tag--dept", icon: "organization" as const, label: "Đơn vị" };
  }
}

export function OrgChartDiagram({
  allUsers,
  canDrop,
  collapsedNodes,
  dragDisabled = false,
  draggedUnitId = null,
  dropTargetId = null,
  onDragEnd,
  onDragOver,
  onDragStart,
  onDrop,
  onSelectUnit,
  onToggleCollapse,
  onZoomChange,
  query,
  selectedUnitId,
  units,
  zoom = 1,
}: OrgChartDiagramProps) {
  const [internalCollapsedNodes, setInternalCollapsedNodes] = useState<Set<number>>(new Set());
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isPanning, setIsPanning] = useState(false);
  const dragStartRef = useRef({ initialPanX: 0, initialPanY: 0, startX: 0, startY: 0 });

  const activeCollapsedNodes = collapsedNodes ?? internalCollapsedNodes;

  function toggleCollapse(unitId: number, event: React.MouseEvent) {
    if (onToggleCollapse) {
      onToggleCollapse(unitId, event);
      return;
    }
    event.stopPropagation();
    setInternalCollapsedNodes((prev) => {
      const next = new Set(prev);
      if (next.has(unitId)) {
        next.delete(unitId);
      } else {
        next.add(unitId);
      }
      return next;
    });
  }

  function handleMouseDown(e: React.MouseEvent<HTMLDivElement>) {
    if (e.button !== 0) return;
    const target = e.target as HTMLElement;
    if (target.closest(".org-diagram-card") || target.closest("button") || target.closest("input")) {
      return;
    }
    setIsPanning(true);
    dragStartRef.current = {
      initialPanX: pan.x,
      initialPanY: pan.y,
      startX: e.clientX,
      startY: e.clientY,
    };
  }

  function handleMouseMove(e: React.MouseEvent<HTMLDivElement>) {
    if (!isPanning) return;
    const dx = e.clientX - dragStartRef.current.startX;
    const dy = e.clientY - dragStartRef.current.startY;
    setPan({
      x: dragStartRef.current.initialPanX + dx,
      y: dragStartRef.current.initialPanY + dy,
    });
  }

  function handleMouseUp() {
    setIsPanning(false);
  }

  function handleWheel(e: React.WheelEvent<HTMLDivElement>) {
    e.preventDefault();
    const zoomStep = e.deltaY < 0 ? 0.08 : -0.08;
    const nextZoom = Math.min(2, Math.max(0.35, +(zoom + zoomStep).toFixed(2)));
    if (onZoomChange) {
      onZoomChange(nextZoom);
    }
  }

  // Create user lookup map
  const userMap = useMemo(() => {
    const map = new Map<number | string, User>();
    for (const u of allUsers) {
      if (u.employeeId) map.set(u.employeeId, u);
      map.set(u.id, u);
    }
    return map;
  }, [allUsers]);

  const normalizedQuery = query.trim().toLowerCase();

  return (
    <div
      className={`org-chart-canvas-pan-area ${isPanning ? "is-panning" : ""}`}
      onMouseDown={handleMouseDown}
      onMouseLeave={handleMouseUp}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onWheel={handleWheel}
    >
      <div
        className="org-chart-viewport"
        style={{
          transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`,
          transformOrigin: "top center",
          transition: isPanning ? "none" : "transform 0.2s cubic-bezier(0.16, 1, 0.3, 1)",
        }}
      >
        <div className="org-chart-tree-root">
          {units.map((rootUnit) => (
            <DiagramNodeGroup
              canDrop={canDrop}
              collapsedNodes={activeCollapsedNodes}
              dragDisabled={dragDisabled}
              draggedUnitId={draggedUnitId}
              dropTargetId={dropTargetId}
              key={rootUnit.id}
              onDragEnd={onDragEnd}
              onDragOver={onDragOver}
              onDragStart={onDragStart}
              onDrop={onDrop}
              onSelectUnit={onSelectUnit}
              onToggleCollapse={toggleCollapse}
              query={normalizedQuery}
              selectedUnitId={selectedUnitId}
              unit={rootUnit}
              userMap={userMap}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

interface DiagramNodeGroupProps {
  canDrop?: (draggedUnitId: number, targetUnitId: number) => boolean;
  collapsedNodes: ReadonlySet<number>;
  dragDisabled?: boolean;
  draggedUnitId?: number | null;
  dropTargetId?: number | null;
  onDragEnd?: () => void;
  onDragOver?: (event: React.DragEvent<HTMLElement>, targetUnitId: number) => void;
  onDragStart?: (event: React.DragEvent<HTMLElement>, unitId: number) => void;
  onDrop?: (event: React.DragEvent<HTMLElement>, targetUnitId: number) => void;
  onSelectUnit: (unitId: number) => void;
  onToggleCollapse: (unitId: number, event: React.MouseEvent) => void;
  query: string;
  selectedUnitId: number;
  unit: OrgUnitTreeNode;
  userMap: Map<number | string, User>;
}

function matchesQuery(unit: OrgUnitTreeNode, query: string): boolean {
  if (!query) return true;
  const nameMatch = unit.unitName.toLowerCase().includes(query);
  const codeMatch = unit.unitCode.toLowerCase().includes(query);
  const childMatch = unit.children.some((child) => matchesQuery(child, query));
  return nameMatch || codeMatch || childMatch;
}

function DiagramNodeGroup({
  canDrop,
  collapsedNodes,
  dragDisabled,
  draggedUnitId,
  dropTargetId,
  onDragEnd,
  onDragOver,
  onDragStart,
  onDrop,
  onSelectUnit,
  onToggleCollapse,
  query,
  selectedUnitId,
  unit,
  userMap,
}: DiagramNodeGroupProps) {
  if (!matchesQuery(unit, query)) {
    return null;
  }

  const isCollapsed = collapsedNodes.has(unit.id) && !query;
  const hasChildren = unit.children.length > 0;
  const isSelected = selectedUnitId === unit.id;
  const meta = getUnitMeta(unit.unitType);
  const manager = unit.managerId ? userMap.get(unit.managerId) : null;
  const isHighlight = Boolean(query) && (unit.unitName.toLowerCase().includes(query) || unit.unitCode.toLowerCase().includes(query));
  const isDraggable = !dragDisabled && unit.parentId !== null && !query;
  const isDropTarget = dropTargetId === unit.id;
  const isBeingDragged = draggedUnitId === unit.id;

  return (
    <div className="org-diagram-node-group">
      {/* Node Box */}
      <div
        className={`org-diagram-card ${isSelected ? "is-selected" : ""} ${unit.status === "INACTIVE" ? "is-inactive" : ""} ${isHighlight ? "is-highlight" : ""} ${isDropTarget ? "is-drop-target" : ""} ${isBeingDragged ? "is-dragging" : ""}`}
        draggable={isDraggable}
        onClick={() => onSelectUnit(unit.id)}
        onDragEnd={onDragEnd}
        onDragOver={(e) => onDragOver?.(e, unit.id)}
        onDragStart={(e) => isDraggable && onDragStart?.(e, unit.id)}
        onDrop={(e) => onDrop?.(e, unit.id)}
        role="button"
        tabIndex={0}
        title={isDraggable ? "Kéo thả để chuyển vị trí trực thuộc" : undefined}
      >
        <div className="org-diagram-card__header">
          <span className={`org-unit-tag ${meta.className}`}>
            <Icon name={meta.icon} />
            <span>{meta.label}</span>
          </span>
          <div style={{ display: "flex", alignItems: "center", gap: "0.25rem" }}>
            {unit.status === "INACTIVE" && (
              <span className="org-diagram-card__lock" title="Đang tạm khóa">
                <Icon name="lock" />
              </span>
            )}
            <span className="org-diagram-card__code">{unit.unitCode}</span>
          </div>
        </div>

        <div className="org-diagram-card__title">{unit.unitName}</div>

        <div className="org-diagram-card__meta">
          <div className="org-diagram-card__manager">
            <Icon name="user" />
            <span>{manager ? manager.fullName || manager.username : "Chưa có QL"}</span>
          </div>
          {hasChildren && (
            <span className="org-diagram-card__badge" title={`${unit.children.length} đơn vị trực thuộc`}>
              {unit.children.length} nhánh
            </span>
          )}
        </div>

        {hasChildren && (
          <button
            aria-label={isCollapsed ? "Mở rộng nhánh" : "Thu gọn nhánh"}
            className="org-diagram-card__toggle"
            onClick={(e) => onToggleCollapse(unit.id, e)}
            type="button"
          >
            <Icon name={isCollapsed ? "chevronRight" : "chevronDown"} />
          </button>
        )}
      </div>

      {/* Children Branches & Connector Lines */}
      {hasChildren && !isCollapsed && (
        <div className="org-diagram-children-wrapper">
          <div className="org-diagram-line-vertical" />
          <div className="org-diagram-children-grid">
            {unit.children.map((child) => (
              <DiagramNodeGroup
                canDrop={canDrop}
                collapsedNodes={collapsedNodes}
                dragDisabled={dragDisabled}
                draggedUnitId={draggedUnitId}
                dropTargetId={dropTargetId}
                key={child.id}
                onDragEnd={onDragEnd}
                onDragOver={onDragOver}
                onDragStart={onDragStart}
                onDrop={onDrop}
                onSelectUnit={onSelectUnit}
                onToggleCollapse={onToggleCollapse}
                query={query}
                selectedUnitId={selectedUnitId}
                unit={child}
                userMap={userMap}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
