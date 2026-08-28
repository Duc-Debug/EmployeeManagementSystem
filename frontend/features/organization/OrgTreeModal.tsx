"use client";

import { useMemo, useState } from "react";

import { Dialog } from "@/components/ui/Dialog";
import { Icon } from "@/components/ui/Icon";
import { findOrgUnit, flattenOrgTree, getDescendantIds } from "@/lib/organization";
import type { OrgUnitTreeNode, User } from "@/src/types/hrm";

import { OrganizationTree } from "@/features/organization/OrganizationTree";
import { OrgChartDiagram } from "@/features/organization/OrgChartDiagram";

interface OrgTreeModalProps {
  allUsers: readonly User[];
  onClose: () => void;
  onCreateChild?: (parentId: number) => void;
  onEditUnit?: (unit: OrgUnitTreeNode) => void;
  onMoveUnit?: (sourceId: number, targetId: number) => Promise<void>;
  onSelectUnit: (unitId: number) => void;
  open: boolean;
  selectedUnitId: number;
  tree: readonly OrgUnitTreeNode[];
}

export function OrgTreeModal({
  allUsers,
  onClose,
  onCreateChild,
  onEditUnit,
  onMoveUnit,
  onSelectUnit,
  open,
  selectedUnitId,
  tree,
}: OrgTreeModalProps) {
  const [viewMode, setViewMode] = useState<"diagram" | "tree">("diagram");
  const [query, setQuery] = useState("");
  const [zoom, setZoom] = useState(1);
  const [expandedUnits, setExpandedUnits] = useState<Set<number>>(() => new Set());
  const [collapsedDiagramUnits, setCollapsedDiagramUnits] = useState<Set<number>>(() => new Set());
  const [showDetails, setShowDetails] = useState(false);
  const [draggedUnitId, setDraggedUnitId] = useState<number | null>(null);
  const [dropTargetId, setDropTargetId] = useState<number | null>(null);

  const allUnits = useMemo(() => flattenOrgTree(tree), [tree]);
  const selectedUnit = useMemo(
    () => findOrgUnit(tree, selectedUnitId) ?? allUnits[0],
    [tree, selectedUnitId, allUnits],
  );

  const parentUnit = selectedUnit && selectedUnit.parentId ? findOrgUnit(tree, selectedUnit.parentId) : null;
  const managerUser = useMemo(() => {
    if (!selectedUnit || !selectedUnit.managerId) return null;
    return allUsers.find((u) => (u.employeeId && u.employeeId === selectedUnit.managerId) || u.id === selectedUnit.managerId) || null;
  }, [allUsers, selectedUnit]);

  const unitMembers = useMemo(() => {
    if (!selectedUnit) return [];
    return allUsers.filter((u) => u.orgUnitId === selectedUnit.id);
  }, [allUsers, selectedUnit]);

  function zoomIn() {
    setZoom((z) => Math.min(1.8, z + 0.15));
  }

  function zoomOut() {
    setZoom((z) => Math.max(0.5, z - 0.15));
  }

  function resetZoom() {
    setZoom(1);
  }

  function expandAll() {
    setExpandedUnits(new Set(allUnits.map((u) => u.id)));
    setCollapsedDiagramUnits(new Set());
  }

  function collapseAll() {
    setExpandedUnits(new Set());
    setCollapsedDiagramUnits(new Set(allUnits.filter((u) => u.children.length > 0).map((u) => u.id)));
  }

  function toggleExpanded(unitId: number) {
    setExpandedUnits((prev) => {
      const next = new Set(prev);
      if (next.has(unitId)) {
        next.delete(unitId);
      } else {
        next.add(unitId);
      }
      return next;
    });
  }

  function toggleDiagramCollapse(unitId: number, event: React.MouseEvent) {
    event.stopPropagation();
    setCollapsedDiagramUnits((prev) => {
      const next = new Set(prev);
      if (next.has(unitId)) {
        next.delete(unitId);
      } else {
        next.add(unitId);
      }
      return next;
    });
  }

  function handleSelectUnit(unitId: number) {
    onSelectUnit(unitId);
    setShowDetails(true);
  }

  function canMoveUnit(sourceId: number, targetId: number): boolean {
    const source = findOrgUnit(tree, sourceId);
    const target = findOrgUnit(tree, targetId);
    return Boolean(
      source
      && target
      && source.parentId !== null
      && target.status === "ACTIVE"
      && source.parentId !== target.id
      && !getDescendantIds(source).has(target.id),
    );
  }

  function resetDragState() {
    setDraggedUnitId(null);
    setDropTargetId(null);
  }

  function handleDragStart(event: React.DragEvent<HTMLElement>, unitId: number) {
    if (query.trim()) {
      event.preventDefault();
      return;
    }
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", String(unitId));
    setDraggedUnitId(unitId);
    setDropTargetId(null);
  }

  function handleDragOver(event: React.DragEvent<HTMLElement>, targetUnitId: number) {
    if (draggedUnitId === null || !canMoveUnit(draggedUnitId, targetUnitId)) {
      return;
    }
    event.preventDefault();
    event.dataTransfer.dropEffect = "move";
    setDropTargetId(targetUnitId);
  }

  async function handleDrop(event: React.DragEvent<HTMLElement>, targetUnitId: number) {
    event.preventDefault();
    const sourceId = draggedUnitId ?? Number(event.dataTransfer.getData("text/plain"));
    if (!sourceId || !canMoveUnit(sourceId, targetUnitId)) {
      resetDragState();
      return;
    }
    if (onMoveUnit) {
      await onMoveUnit(sourceId, targetUnitId);
    }
    resetDragState();
  }

  return (
    <Dialog
      className="dialog--org-chart"
      description="Xem trực quan sơ đồ khối phân cấp công ty, khối, phòng ban và nhóm chuyên môn."
      onClose={onClose}
      open={open}
      title="Sơ đồ cây cơ cấu tổ chức"
    >
      <div className="org-modal-container">
        {/* Top Control Bar inside Modal */}
        <div className="org-modal-toolbar">
          <div className="org-modal-toolbar__group">
            {/* View Mode Toggle Button Group */}
            <div className="segmented-control">
              <button
                className={`segmented-control__btn ${viewMode === "diagram" ? "is-active" : ""}`}
                onClick={() => setViewMode("diagram")}
                type="button"
              >
                <Icon name="organization" />
                <span>Sơ đồ khối (Chart)</span>
              </button>
              <button
                className={`segmented-control__btn ${viewMode === "tree" ? "is-active" : ""}`}
                onClick={() => setViewMode("tree")}
                type="button"
              >
                <Icon name="branch" />
                <span>Cây phân cấp (Tree)</span>
              </button>
            </div>

            {/* Search Input */}
            <div className="search-field org-modal-search">
              <Icon name="search" />
              <input
                className="input"
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Tìm đơn vị trong sơ đồ..."
                type="search"
                value={query}
              />
              {query && (
                <button className="search-clear-btn" onClick={() => setQuery("")} type="button">
                  <Icon name="close" />
                </button>
              )}
            </div>

            {/* Details Panel Toggle Button */}
            <button
              className={`button ${showDetails ? "button--primary" : "button--secondary"} button--sm`}
              onClick={() => setShowDetails((prev) => !prev)}
              style={{ display: "inline-flex", alignItems: "center", gap: "0.35rem", minHeight: "2rem" }}
              title={showDetails ? "Ẩn khung chi tiết" : "Hiện khung chi tiết"}
              type="button"
            >
              <Icon name="document" />
              <span>Chi tiết</span>
            </button>
          </div>

          <div className="org-modal-toolbar__group">
            {/* Expansion Controls for both views */}
            <div className="tree-toolbar-actions" style={{ display: "flex", gap: "0.35rem" }}>
              <button className="button button--secondary button--xs" onClick={expandAll} type="button">
                <span>Mở rộng tất cả</span>
              </button>
              <button className="button button--secondary button--xs" onClick={collapseAll} type="button">
                <span>Thu gọn tất cả</span>
              </button>
            </div>

            {/* Zoom Controls (Active in Diagram View) */}
            {viewMode === "diagram" && (
              <div className="zoom-controls">
                <button className="button button--icon" onClick={zoomOut} title="Thu nhỏ" type="button">
                  -
                </button>
                <span className="zoom-percentage">{Math.round(zoom * 100)}%</span>
                <button className="button button--icon" onClick={zoomIn} title="Phóng to" type="button">
                  +
                </button>
                <button className="button button--secondary button--xs" onClick={resetZoom} type="button">
                  Reset
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Modal Main Workspace split view (flexibly expands when sidebar is closed) */}
        <div className={`org-modal-body ${showDetails ? "has-sidebar" : "no-sidebar"}`}>
          {/* Main Chart Viewer Canvas */}
          <div className="org-modal-canvas">
            {viewMode === "diagram" ? (
              <OrgChartDiagram
                allUsers={allUsers}
                canDrop={canMoveUnit}
                collapsedNodes={collapsedDiagramUnits}
                dragDisabled={false}
                draggedUnitId={draggedUnitId}
                dropTargetId={dropTargetId}
                onDragEnd={resetDragState}
                onDragOver={handleDragOver}
                onDragStart={handleDragStart}
                onDrop={handleDrop}
                onSelectUnit={handleSelectUnit}
                onToggleCollapse={toggleDiagramCollapse}
                query={query}
                selectedUnitId={selectedUnitId}
                units={tree}
                zoom={zoom}
              />
            ) : (
              <div className="org-modal-tree-container">
                <OrganizationTree
                  dragDisabled={false}
                  draggedUnitId={draggedUnitId}
                  dropTargetId={dropTargetId}
                  expandedUnits={expandedUnits}
                  onCanDrop={canMoveUnit}
                  onDragEnd={resetDragState}
                  onDragOver={handleDragOver}
                  onDragStart={handleDragStart}
                  onDrop={handleDrop}
                  onSelect={handleSelectUnit}
                  onToggle={toggleExpanded}
                  query={query}
                  selectedUnitId={selectedUnitId}
                  units={tree}
                />
              </div>
            )}
          </div>

          {/* Quick Inspector Side Panel inside Modal */}
          {showDetails && selectedUnit && (
            <aside className="org-modal-sidebar">
              <div className="org-modal-sidebar__header">
                <div className="org-modal-sidebar__header-title">
                  <h3>Chi tiết đơn vị được chọn</h3>
                  <span className="org-detail-code-badge">{selectedUnit.unitCode}</span>
                </div>
                <button
                  aria-label="Đóng bảng chi tiết"
                  className="org-modal-sidebar__close-btn"
                  onClick={() => setShowDetails(false)}
                  title="Đóng bảng chi tiết"
                  type="button"
                >
                  <Icon name="close" />
                </button>
              </div>

              <div className="org-modal-sidebar__content">
                <div className="org-modal-unit-card">
                  <div className="org-modal-unit-card__title">
                    <Icon name={selectedUnit.unitType === "COMPANY" ? "building" : selectedUnit.unitType === "CENTER" ? "branch" : selectedUnit.unitType === "DEPARTMENT" ? "users" : "user"} />
                    <strong>{selectedUnit.unitName}</strong>
                  </div>
                  <span className="role-chip">Cấp {selectedUnit.level} · {selectedUnit.unitType}</span>
                </div>

                <div className="org-modal-info-rows">
                  <div className="org-modal-info-row">
                    <span className="label">Đơn vị cha:</span>
                    <span className="val">{parentUnit ? parentUnit.unitName : "Gốc (Công ty)"}</span>
                  </div>

                  <div className="org-modal-info-row">
                    <span className="label">Người quản lý:</span>
                    <span className="val">
                      {managerUser ? (managerUser.fullName || managerUser.username) : "Chưa chỉ định"}
                    </span>
                  </div>

                  <div className="org-modal-info-row">
                    <span className="label">Nhánh con:</span>
                    <span className="val">{selectedUnit.children.length} đơn vị</span>
                  </div>

                  <div className="org-modal-info-row">
                    <span className="label">Nhân sự:</span>
                    <span className="val">{unitMembers.length} thành viên</span>
                  </div>
                </div>

                {selectedUnit.description && (
                  <div className="org-modal-description">
                    <small>Mô tả / Chức năng:</small>
                    <p>{selectedUnit.description}</p>
                  </div>
                )}

                <div className="org-modal-sidebar__actions">
                  {onCreateChild && (
                    <button
                      className="button button--secondary button--sm"
                      onClick={() => {
                        onCreateChild(selectedUnit.id);
                        onClose();
                      }}
                      type="button"
                    >
                      <Icon name="plus" />
                      <span>Thêm đơn vị con</span>
                    </button>
                  )}
                  {onEditUnit && (
                    <button
                      className="button button--secondary button--sm"
                      onClick={() => {
                        onEditUnit(selectedUnit);
                        onClose();
                      }}
                      type="button"
                    >
                      <Icon name="settings" />
                      <span>Chỉnh sửa</span>
                    </button>
                  )}
                </div>
              </div>
            </aside>
          )}
        </div>
      </div>
    </Dialog>
  );
}
