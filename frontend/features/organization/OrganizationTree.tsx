"use client";

import type { DragEvent } from "react";

import { Icon } from "@/components/ui/Icon";
import type { OrgUnitTreeNode, OrgUnitType } from "@/src/types/hrm";

interface OrganizationTreeProps {
  dragDisabled: boolean;
  draggedUnitId: number | null;
  dropTargetId: number | null;
  expandedUnits: ReadonlySet<number>;
  onCanDrop: (draggedUnitId: number, targetUnitId: number) => boolean;
  onDragEnd: () => void;
  onDragOver: (event: DragEvent<HTMLElement>, targetUnitId: number) => void;
  onDragStart: (event: DragEvent<HTMLElement>, unitId: number) => void;
  onDrop: (event: DragEvent<HTMLElement>, targetUnitId: number) => void;
  onSelect: (unitId: number) => void;
  onToggle: (unitId: number) => void;
  query: string;
  selectedUnitId: number;
  units: readonly OrgUnitTreeNode[];
}

function getUnitTypeMeta(unitType: OrgUnitType) {
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

export function OrganizationTree({
  dragDisabled,
  draggedUnitId,
  dropTargetId,
  expandedUnits,
  onCanDrop,
  onDragEnd,
  onDragOver,
  onDragStart,
  onDrop,
  onSelect,
  onToggle,
  query,
  selectedUnitId,
  units,
}: OrganizationTreeProps) {
  return (
    <ul className="organization-tree">
      {units.map((unit) => (
        <OrganizationTreeItem
          dragDisabled={dragDisabled}
          draggedUnitId={draggedUnitId}
          dropTargetId={dropTargetId}
          expandedUnits={expandedUnits}
          key={unit.id}
          onCanDrop={onCanDrop}
          onDragEnd={onDragEnd}
          onDragOver={onDragOver}
          onDragStart={onDragStart}
          onDrop={onDrop}
          onSelect={onSelect}
          onToggle={onToggle}
          query={query}
          selectedUnitId={selectedUnitId}
          unit={unit}
        />
      ))}
    </ul>
  );
}

interface OrganizationTreeItemProps extends Omit<OrganizationTreeProps, "units"> {
  unit: OrgUnitTreeNode;
}

function OrganizationTreeItem({
  dragDisabled,
  draggedUnitId,
  dropTargetId,
  expandedUnits,
  onCanDrop,
  onDragEnd,
  onDragOver,
  onDragStart,
  onDrop,
  onSelect,
  onToggle,
  query,
  selectedUnitId,
  unit,
}: OrganizationTreeItemProps) {
  if (!matchesUnitOrChild(unit, query)) {
    return null;
  }

  const hasChildren = unit.children.length > 0;
  const isExpanded = Boolean(query) || expandedUnits.has(unit.id);
  const isSelected = selectedUnitId === unit.id;
  const isDragging = draggedUnitId === unit.id;
  const canReceiveDrop = draggedUnitId !== null && onCanDrop(draggedUnitId, unit.id);
  const isDropTarget = dropTargetId === unit.id && canReceiveDrop;
  const canDrag = !dragDisabled && unit.parentId !== null;
  const meta = getUnitTypeMeta(unit.unitType);

  return (
    <li className={isDragging ? "tree-node is-dragging" : "tree-node"}>
      <div className="tree-node__row">
        {hasChildren ? (
          <button
            aria-expanded={isExpanded}
            aria-label={isExpanded ? `Thu gọn ${unit.unitName}` : `Mở rộng ${unit.unitName}`}
            className="tree-node__toggle"
            onClick={() => onToggle(unit.id)}
            type="button"
          >
            <Icon name={isExpanded ? "chevronDown" : "chevronRight"} />
          </button>
        ) : (
          <span aria-hidden="true" className="tree-node__toggle-placeholder" />
        )}
        <button
          aria-label={`Kéo ${unit.unitName} để thay đổi đơn vị cha`}
          className="tree-node__drag-handle"
          disabled={!canDrag}
          draggable={canDrag}
          onClick={(event) => event.stopPropagation()}
          onDragEnd={onDragEnd}
          onDragStart={(event) => {
            event.stopPropagation();
            onDragStart(event, unit.id);
          }}
          title={canDrag ? "Kéo để chuyển vị trí cơ cấu" : undefined}
          type="button"
        >
          <Icon name="grip" />
        </button>
        <button
          aria-current={isSelected ? "true" : undefined}
          className={`tree-node__button ${isDropTarget ? "is-drop-target" : ""} ${isSelected ? "is-selected" : ""} ${unit.status === "INACTIVE" ? "is-inactive" : ""}`}
          onClick={() => onSelect(unit.id)}
          onDragOver={(event) => onDragOver(event, unit.id)}
          onDrop={(event) => onDrop(event, unit.id)}
          type="button"
        >
          <span className="tree-node__icon">
            <Icon name={unit.status === "INACTIVE" ? "lock" : meta.icon} />
          </span>
          <div className="tree-node__copy">
            <div className="tree-node__title-line">
              <strong className="tree-node__name">{unit.unitName}</strong>
              <span className={`org-unit-tag ${meta.className}`}>{meta.label}</span>
              {unit.status === "INACTIVE" && <span className="org-unit-tag unit-tag--locked">Khóa</span>}
              {hasChildren && <span className="tree-node__child-count">{unit.children.length}</span>}
            </div>
            <small className="tree-node__code">{unit.unitCode}</small>
          </div>
        </button>
      </div>
      {hasChildren && isExpanded ? (
        <ul className="organization-tree organization-tree--nested">
          {unit.children.map((child) => (
            <OrganizationTreeItem
              dragDisabled={dragDisabled}
              draggedUnitId={draggedUnitId}
              dropTargetId={dropTargetId}
              expandedUnits={expandedUnits}
              key={child.id}
              onCanDrop={onCanDrop}
              onDragEnd={onDragEnd}
              onDragOver={onDragOver}
              onDragStart={onDragStart}
              onDrop={onDrop}
              onSelect={onSelect}
              onToggle={onToggle}
              query={query}
              selectedUnitId={selectedUnitId}
              unit={child}
            />
          ))}
        </ul>
      ) : null}
    </li>
  );
}

function matchesUnitOrChild(unit: OrgUnitTreeNode, query: string): boolean {
  if (!query) {
    return true;
  }

  const unitMatches = [unit.unitCode, unit.unitName].some((value) =>
    value.toLocaleLowerCase("vi").includes(query),
  );
  return unitMatches || unit.children.some((child) => matchesUnitOrChild(child, query));
}
