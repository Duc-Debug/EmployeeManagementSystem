"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type DragEvent, type FormEvent } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { StatusBadge } from "@/components/ui/Badge";
import { Dialog } from "@/components/ui/Dialog";
import { Icon } from "@/components/ui/Icon";
import type { ManagerOption } from "@/components/ui/ManagerCombobox";
import {
  findOrgUnit,
  flattenOrgTree,
  getDescendantIds,
} from "@/lib/organization";
import type { OrgUnitTreeNode, User } from "@/src/types/hrm";
import { activateOrgUnit, createOrgUnit, deactivateOrgUnit, getOrgTree, moveOrgUnit, updateOrgUnit } from "@/lib/api/org-units";
import { getUsers } from "@/lib/api/users";
import { ApiError } from "@/lib/api-client";

import { OrgUnitForm, type OrgUnitDraft, type OrgUnitDraftErrors } from "@/features/organization/OrgUnitForm";
import { OrganizationTree } from "@/features/organization/OrganizationTree";
import { OrgChartDiagram } from "@/features/organization/OrgChartDiagram";

type OrgEditorState = { mode: "create" } | { mode: "edit"; unitId: number } | null;

function createInitialDraft(parentId: number | null): OrgUnitDraft {
  return {
    description: "",
    managerId: "",
    parentId: parentId ? String(parentId) : "",
    unitCode: "",
    unitName: "",
    unitType: "DEPARTMENT",
  };
}

function toEditDraft(unit: OrgUnitTreeNode): OrgUnitDraft {
  return {
    description: unit.description ?? "",
    managerId: unit.managerId ? String(unit.managerId) : "",
    parentId: unit.parentId ? String(unit.parentId) : "",
    unitCode: unit.unitCode,
    unitName: unit.unitName,
    unitType: unit.unitType,
  };
}

export function OrganizationWorkspace() {
  const [tree, setTree] = useState<readonly OrgUnitTreeNode[]>([]);
  const [allUsers, setAllUsers] = useState<User[]>([]);
  const [selectedUnitId, setSelectedUnitId] = useState<number>(0);
  const [viewMode, setViewMode] = useState<"diagram" | "tree">("diagram");
  const [zoom, setZoom] = useState(1);
  const [expandedUnits, setExpandedUnits] = useState<Set<number>>(new Set());
  const [collapsedDiagramUnits, setCollapsedDiagramUnits] = useState<Set<number>>(new Set());
  const [showDetails, setShowDetails] = useState(true);
  const [query, setQuery] = useState("");
  const [editor, setEditor] = useState<OrgEditorState>(null);
  const [lockTarget, setLockTarget] = useState<OrgUnitTreeNode | null>(null);
  const [draft, setDraft] = useState<OrgUnitDraft>(() => createInitialDraft(null));
  const [errors, setErrors] = useState<OrgUnitDraftErrors>({});
  const [announcement, setAnnouncement] = useState("");
  const [draggedUnitId, setDraggedUnitId] = useState<number | null>(null);
  const [dropTargetId, setDropTargetId] = useState<number | null>(null);
  const editorFocusRef = useRef<HTMLElement>(null);
  const submitRef = useRef<HTMLButtonElement>(null);
  const setEditorFocus = useCallback((element: HTMLElement | null) => {
    editorFocusRef.current = element;
  }, []);

  const [fetchError, setFetchError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  useEffect(() => {
    let ignore = false;
    Promise.all([
      getOrgTree(),
      getUsers(0, 100),
    ])
      .then(([treeData, usersPage]) => {
        if (!ignore) {
          setTree(treeData || []);
          setAllUsers(usersPage?.content || []);
          if (treeData && treeData.length > 0) {
            setSelectedUnitId(treeData[0].id);
            setExpandedUnits(new Set(flattenOrgTree(treeData).map((unit) => unit.id)));
          }
          setFetchError(null);
        }
      })
      .catch((err) => {
        if (!ignore) {
          console.error("Lỗi nạp dữ liệu cơ cấu tổ chức:", err);
          setFetchError(err instanceof Error ? err.message : "Không thể tải dữ liệu cây tổ chức.");
        }
      });

    return () => {
      ignore = true;
    };
  }, [reloadTick]);

  const allUnits = useMemo(() => flattenOrgTree(tree), [tree]);
  const selectedUnit = findOrgUnit(tree, selectedUnitId) ?? allUnits[0];
  const editingUnit = editor?.mode === "edit" ? findOrgUnit(tree, editor.unitId) : undefined;
  const parentUnit = selectedUnit && selectedUnit.parentId ? findOrgUnit(tree, selectedUnit.parentId) : null;

  const parentOptions = useMemo(() => {
    const excludedIds = editingUnit ? getDescendantIds(editingUnit) : new Set<number>();
    return allUnits
      .filter((unit) => unit.status === "ACTIVE" && !excludedIds.has(unit.id))
      .map((unit) => ({
        depth: unit.level,
        id: unit.id,
        unitCode: unit.unitCode,
        unitName: unit.unitName,
        unitType: unit.unitType,
      }));
  }, [allUnits, editingUnit]);

  // Members belonging to the selected unit
  const unitMembers = useMemo(() => {
    if (!selectedUnit) return [];
    return allUsers.filter((u) => u.orgUnitId === selectedUnit.id);
  }, [allUsers, selectedUnit]);

  // Candidates for Manager filtered by suitable roles (VT-01, VT-02, VT-03, VT-05, VT-06)
  const managerOptions = useMemo<readonly ManagerOption[]>(() => {
    const suitableRoles = new Set(["VT-01", "VT-02", "VT-03", "VT-05", "VT-06"]);
    return allUsers
      .filter(
        (user): user is User & { employeeId: number } =>
          user.employeeId !== null && user.status === "ACTIVE" && suitableRoles.has(user.roleCode),
      )
      .map((user) => ({
        employeeId: user.employeeId,
        fullName: user.fullName || user.username,
        roleCode: user.roleCode,
        roleName: user.roleName,
        username: user.username,
      }));
  }, [allUsers]);

  // Manager of selected unit
  const managerUser = useMemo(() => {
    if (!selectedUnit || !selectedUnit.managerId) return null;
    return allUsers.find((u) => (u.employeeId && u.employeeId === selectedUnit.managerId) || u.id === selectedUnit.managerId) || null;
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
    setExpandedUnits((currentUnits) => {
      const nextUnits = new Set(currentUnits);
      if (nextUnits.has(unitId)) {
        nextUnits.delete(unitId);
      } else {
        nextUnits.add(unitId);
      }
      return nextUnits;
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
    setSelectedUnitId(unitId);
    setShowDetails(true);
  }

  function openCreateDialog(parentUnitId?: number) {
    setDraft(createInitialDraft(parentUnitId ?? selectedUnit?.id ?? null));
    setErrors({});
    setEditor({ mode: "create" });
  }

  function openEditDialog(unit: OrgUnitTreeNode) {
    setDraft(toEditDraft(unit));
    setErrors({});
    setEditor({ mode: "edit", unitId: unit.id });
  }

  function closeEditor() {
    setEditor(null);
    setErrors({});
  }

  async function handleToggleStatus(unit: OrgUnitTreeNode) {
    if (unit.status === "ACTIVE") {
      setLockTarget(unit);
    } else {
      try {
        await activateOrgUnit(unit.id);
        const refreshed = await getOrgTree();
        setTree(refreshed);
        setAnnouncement(`Đã mở khóa đơn vị ${unit.unitName}.`);
      } catch (err) {
        if (err instanceof ApiError) {
          setAnnouncement(err.message);
        }
      }
    }
  }

  async function confirmLockUnit() {
    if (!lockTarget) return;
    try {
      await deactivateOrgUnit(lockTarget.id);
      const refreshed = await getOrgTree();
      setTree(refreshed);
      setAnnouncement(`Đã tạm khóa đơn vị ${lockTarget.unitName}.`);
    } catch {
      // ignore
    }
    setLockTarget(null);
  }

  function updateDraft<Key extends keyof OrgUnitDraft>(key: Key, value: OrgUnitDraft[Key]) {
    setDraft((currentDraft) => ({ ...currentDraft, [key]: value }));
    setErrors((currentErrors) => ({ ...currentErrors, [key]: undefined }));
  }

  function validateDraft(): boolean {
    const nextErrors: OrgUnitDraftErrors = {};
    if (!editor) {
      return false;
    }

    if (editor.mode === "create" && !draft.unitCode.trim()) {
      nextErrors.unitCode = "Mã đơn vị là bắt buộc.";
    }
    if (!draft.unitName.trim()) {
      nextErrors.unitName = "Tên đơn vị là bắt buộc.";
    }

    if (draft.parentId) {
      const parent = findOrgUnit(tree, Number(draft.parentId));
      if (!parent || parent.status !== "ACTIVE") {
        nextErrors.parentId = "Không tìm thấy đơn vị cha. Vui lòng chọn lại.";
      }
    } else if (editingUnit?.parentId !== null) {
      nextErrors.parentId = "Hãy chọn đơn vị cha hợp lệ.";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  async function saveOrgUnit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editor || !validateDraft()) {
      return;
    }

    const parentId = draft.parentId ? Number(draft.parentId) : null;
    const managerId = draft.managerId ? Number(draft.managerId) : null;
    if (editor.mode === "create") {
      try {
        const created = await createOrgUnit({
          description: draft.description.trim() || null,
          managerId,
          parentId,
          unitCode: draft.unitCode.trim().toUpperCase(),
          unitName: draft.unitName.trim(),
          unitType: draft.unitType,
        });
        const refreshed = await getOrgTree();
        setTree(refreshed);
        if (parentId) {
          setExpandedUnits((currentUnits) => new Set(currentUnits).add(parentId));
        }
        setSelectedUnitId(created.id);
        setAnnouncement(`Đã tạo đơn vị ${created.unitName}.`);
        closeEditor();
      } catch (err) {
        if (err instanceof ApiError) {
          setErrors({ unitCode: err.message });
        }
      }
      return;
    }

    if (!editingUnit) {
      return;
    }

    try {
      await updateOrgUnit(editingUnit.id, {
        description: draft.description.trim() || null,
        managerId,
        unitName: draft.unitName.trim(),
        unitType: draft.unitType,
      });
      if (parentId !== null && parentId !== editingUnit.parentId) {
        await moveOrgUnit(editingUnit.id, parentId);
      }
      const refreshed = await getOrgTree();
      setTree(refreshed);
      if (parentId && parentId !== editingUnit.parentId) {
        setExpandedUnits((currentUnits) => new Set(currentUnits).add(parentId));
      }
      setAnnouncement(`Đã cập nhật đơn vị ${draft.unitName.trim()}.`);
      closeEditor();
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors({ unitName: err.message });
      }
    }
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

  function handleDragStart(event: DragEvent<HTMLElement>, unitId: number) {
    if (query.trim()) {
      return;
    }

    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", String(unitId));
    setDraggedUnitId(unitId);
    setDropTargetId(null);
  }

  function handleDragOver(event: DragEvent<HTMLElement>, targetUnitId: number) {
    if (draggedUnitId === null || !canMoveUnit(draggedUnitId, targetUnitId)) {
      return;
    }

    event.preventDefault();
    event.dataTransfer.dropEffect = "move";
    setDropTargetId(targetUnitId);
  }

  async function handleMoveUnit(sourceId: number, targetId: number) {
    if (!canMoveUnit(sourceId, targetId)) return;
    const source = findOrgUnit(tree, sourceId);
    const target = findOrgUnit(tree, targetId);

    try {
      await moveOrgUnit(sourceId, targetId);
      const refreshed = await getOrgTree();
      setTree(refreshed);
      setExpandedUnits((currentUnits) => new Set(currentUnits).add(targetId));
      setSelectedUnitId(sourceId);
      if (source && target) {
        setAnnouncement(`Đã chuyển ${source.unitName} vào ${target.unitName}.`);
      }
    } catch {
      // ignore
    }
  }

  async function handleDrop(event: DragEvent<HTMLElement>, targetUnitId: number) {
    event.preventDefault();
    const sourceId = draggedUnitId ?? Number(event.dataTransfer.getData("text/plain"));
    if (!sourceId || !canMoveUnit(sourceId, targetUnitId)) {
      resetDragState();
      return;
    }

    await handleMoveUnit(sourceId, targetUnitId);
    resetDragState();
  }

  const editorMode = editor?.mode;
  const editorTitle = editorMode === "edit" ? "Chỉnh sửa đơn vị tổ chức" : "Tạo đơn vị tổ chức mới";

  return (
    <div className="workspace-stack org-workspace-fullscreen">
      <PageHeader
        actions={
          <div style={{ display: "flex", gap: "0.5rem" }}>
            <button className="button button--primary" onClick={() => openCreateDialog()} type="button">
              <Icon name="plus" />
              <span>Tạo đơn vị mới</span>
            </button>
          </div>
        }
        description="Quản lý và trực quan hóa cây cơ cấu phân cấp phòng ban, khối và nhóm chuyên môn."
        title="Cơ cấu tổ chức"
      />

      {fetchError && (
        <div className="notice notice--error" style={{ marginBottom: "0.5rem" }}>
          <Icon name="alert" />
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", width: "100%" }}>
            <span>{fetchError}</span>
            <button className="button button--secondary" onClick={() => setReloadTick((t) => t + 1)} type="button" style={{ padding: "0.25rem 0.75rem", fontSize: "0.875rem" }}>
              Thử lại
            </button>
          </div>
        </div>
      )}

      {announcement ? <p aria-live="polite" className="sr-only">{announcement}</p> : null}

      {/* Main Interactive Org Chart & Tree Workspace */}
      <div className="org-modal-container">
        {/* Top Control Bar */}
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

        {/* Workspace split view (Main Canvas on the left, Detail sidebar on the right) */}
        <div className={`org-modal-body ${showDetails ? "has-sidebar" : "no-sidebar"}`}>
          {/* Main Chart/Tree Viewer Canvas (Left) */}
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
                onZoomChange={setZoom}
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

          {/* Quick Inspector Side Panel (Right) */}
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
                    <Icon name={selectedUnit.unitType === "COMPANY" ? "organization" : selectedUnit.unitType === "CENTER" ? "branch" : selectedUnit.unitType === "DEPARTMENT" ? "users" : "user"} />
                    <strong>{selectedUnit.unitName}</strong>
                  </div>
                  <div style={{ display: "flex", gap: "0.35rem", alignItems: "center", marginTop: "0.35rem", flexWrap: "wrap" }}>
                    <span className="role-chip">Cấp {selectedUnit.level} · {selectedUnit.unitType}</span>
                    <StatusBadge status={selectedUnit.status === "ACTIVE" ? "ACTIVE" : "LOCKED"} />
                  </div>
                </div>

                <div className="org-modal-info-rows">
                  <div className="org-modal-info-row">
                    <span className="label">Đơn vị cha:</span>
                    <span className="val">
                      {parentUnit ? (
                        <button
                          className="org-parent-link"
                          onClick={() => setSelectedUnitId(parentUnit.id)}
                          style={{
                            background: "none",
                            border: "none",
                            color: "inherit",
                            cursor: "pointer",
                            display: "inline-flex",
                            alignItems: "center",
                            gap: "0.25rem",
                            textDecoration: "underline",
                            padding: 0
                          }}
                          type="button"
                        >
                          <Icon name="branch" />
                          <span>{parentUnit.unitName}</span>
                          <small>({parentUnit.unitCode})</small>
                        </button>
                      ) : (
                        "Gốc (Công ty)"
                      )}
                    </span>
                  </div>

                  <div className="org-modal-info-row">
                    <span className="label">Người quản lý:</span>
                    <span className="val">
                      {managerUser ? (
                        <span style={{ display: "inline-flex", alignItems: "center", gap: "0.375rem", fontWeight: 600 }}>
                          <span>{managerUser.fullName || managerUser.username}</span>
                          {managerUser.roleName ? (
                            <small style={{ color: "#6366f1", fontWeight: "normal" }}>({managerUser.roleName})</small>
                          ) : null}
                        </span>
                      ) : (
                        <span style={{ color: "#94a3b8", fontStyle: "italic" }}>Chưa chỉ định</span>
                      )}
                    </span>
                  </div>

                  <div className="org-modal-info-row">
                    <span className="label">Nhánh con:</span>
                    <span className="val">{selectedUnit.children.length} đơn vị</span>
                  </div>

                  <div className="org-modal-info-row">
                    <span className="label">Nhân sự trực thuộc:</span>
                    <span className="val">{unitMembers.length} thành viên</span>
                  </div>
                </div>

                {selectedUnit.description ? (
                  <div className="org-modal-note">
                    <strong>Mô tả / Chức năng:</strong>
                    <p>{selectedUnit.description}</p>
                  </div>
                ) : null}

                {/* Quick actions for this unit */}
                <div className="org-modal-sidebar__actions">
                  <button
                    className="button button--secondary button--sm"
                    onClick={() => openCreateDialog(selectedUnit.id)}
                    type="button"
                  >
                    <Icon name="plus" />
                    <span>Thêm đơn vị con</span>
                  </button>
                  <button
                    className="button button--secondary button--sm"
                    onClick={() => openEditDialog(selectedUnit)}
                    type="button"
                  >
                    <Icon name="settings" />
                    <span>Chỉnh sửa</span>
                  </button>
                  {selectedUnit.parentId !== null ? (
                    <button
                      className={`button ${selectedUnit.status === "ACTIVE" ? "button--danger" : "button--secondary"} button--sm`}
                      onClick={() => handleToggleStatus(selectedUnit)}
                      title={selectedUnit.status === "ACTIVE" ? "Tạm khóa đơn vị này" : "Mở khóa đơn vị này"}
                      type="button"
                    >
                      <Icon name={selectedUnit.status === "ACTIVE" ? "lock" : "unlock"} />
                      <span>{selectedUnit.status === "ACTIVE" ? "Khóa" : "Mở khóa"}</span>
                    </button>
                  ) : (
                    <button
                      className="button button--secondary button--sm"
                      disabled
                      style={{ opacity: 0.5, cursor: "not-allowed" }}
                      title="Không thể khóa đơn vị gốc của công ty"
                      type="button"
                    >
                      <Icon name="lock" />
                      <span>Khóa</span>
                    </button>
                  )}
                </div>
              </div>
            </aside>
          )}
        </div>
      </div>

      {/* Create / Edit OrgUnit Dialog */}
      <Dialog
        className="dialog--org-form"
        description={editorMode === "edit" ? "Cập nhật tên, loại đơn vị và điều chỉnh nhánh trực thuộc." : "Điền thông tin và chọn đơn vị cấp trên trong cơ cấu."}
        footer={
          <>
            <button className="button button--secondary" onClick={closeEditor} type="button">
              Hủy
            </button>
            <button className="button button--primary" form="org-unit-form" ref={submitRef} type="submit">
              {editorMode === "edit" ? "Lưu thay đổi" : "Tạo đơn vị"}
            </button>
          </>
        }
        initialFocusRef={editorFocusRef}
        onClose={closeEditor}
        open={Boolean(editor)}
        preventBackdropClose={true}
        title={editorTitle}
      >
        {editorMode ? (
          <OrgUnitForm
            errors={errors}
            formId="org-unit-form"
            initialFocusRef={setEditorFocus}
            managerOptions={managerOptions}
            mode={editorMode}
            onChange={updateDraft}
            onSubmit={saveOrgUnit}
            parentOptions={parentOptions}
            value={draft}
          />
        ) : null}
      </Dialog>

      {/* Lock OrgUnit Confirmation Dialog */}
      <Dialog
        className="dialog--compact"
        description="Xác nhận trước khi tạm khóa đơn vị tổ chức này."
        footer={
          <>
            <button className="button button--secondary" onClick={() => setLockTarget(null)} type="button">
              Hủy
            </button>
            <button className="button button--danger" onClick={confirmLockUnit} type="button">
              Xác nhận khóa
            </button>
          </>
        }
        onClose={() => setLockTarget(null)}
        open={Boolean(lockTarget)}
        preventBackdropClose={true}
        title="Khóa đơn vị tổ chức"
      >
        {lockTarget ? (
          <div className="dialog-confirmation">
            <div className="lock-warning-card">
              <div className="lock-warning-card__icon">
                <Icon name="alert" />
              </div>
              <div className="lock-warning-card__info">
                <strong>{lockTarget.unitName}</strong>
                <span>Mã: {lockTarget.unitCode} · Cấp {lockTarget.level}</span>
              </div>
            </div>
            <p className="dialog-confirmation__text">
              Đơn vị này và các đơn vị con trực thuộc sẽ chuyển sang trạng thái tạm khóa.
            </p>
          </div>
        ) : null}
      </Dialog>
    </div>
  );
}
