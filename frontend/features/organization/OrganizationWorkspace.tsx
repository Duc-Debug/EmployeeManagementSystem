"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type DragEvent, type FormEvent } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { StatusBadge } from "@/components/ui/Badge";
import { Dialog } from "@/components/ui/Dialog";
import { EmptyState } from "@/components/ui/EmptyState";
import { Icon } from "@/components/ui/Icon";
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
  const [expandedUnits, setExpandedUnits] = useState<Set<number>>(new Set());
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
  const normalizedQuery = query.trim().toLocaleLowerCase("vi");
  
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

  // KPI Metrics
  const stats = useMemo(() => {
    const total = allUnits.length;
    const centers = allUnits.filter((u) => u.unitType === "CENTER").length;
    const departments = allUnits.filter((u) => u.unitType === "DEPARTMENT").length;
    const teams = allUnits.filter((u) => u.unitType === "TEAM").length;
    const active = allUnits.filter((u) => u.status === "ACTIVE").length;
    const locked = total - active;
    return { active, centers, departments, locked, teams, total };
  }, [allUnits]);

  // Members belonging to the selected unit
  const unitMembers = useMemo(() => {
    if (!selectedUnit) return [];
    return allUsers.filter((user) => user.orgUnitId === selectedUnit.id);
  }, [allUsers, selectedUnit]);

  // Candidates for Manager filtered by suitable roles (VT-01, VT-02, VT-03, VT-05, VT-06)
  const managerOptions = useMemo(() => {
    const suitableRoles = new Set(["VT-01", "VT-02", "VT-03", "VT-05", "VT-06"]);
    return allUsers
      .filter((user) => user.status === "ACTIVE" && suitableRoles.has(user.roleCode))
      .map((user) => ({
        employeeId: user.employeeId || user.id,
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

  function expandAll() {
    setExpandedUnits(new Set(allUnits.map((u) => u.id)));
  }

  function collapseAll() {
    setExpandedUnits(new Set());
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
    if (normalizedQuery) {
      event.preventDefault();
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

  async function handleDrop(event: DragEvent<HTMLElement>, targetUnitId: number) {
    event.preventDefault();
    const sourceId = draggedUnitId ?? Number(event.dataTransfer.getData("text/plain"));
    if (!sourceId || !canMoveUnit(sourceId, targetUnitId)) {
      resetDragState();
      return;
    }

    const source = findOrgUnit(tree, sourceId);
    const target = findOrgUnit(tree, targetUnitId);

    try {
      await moveOrgUnit(sourceId, targetUnitId);
      const refreshed = await getOrgTree();
      setTree(refreshed);
      setExpandedUnits((currentUnits) => new Set(currentUnits).add(targetUnitId));
      setSelectedUnitId(sourceId);
      if (source && target) {
        setAnnouncement(`Đã chuyển ${source.unitName} vào ${target.unitName}.`);
      }
    } catch {
      // ignore
    }
    resetDragState();
  }

  const editorMode = editor?.mode;
  const editorTitle = editorMode === "edit" ? "Chỉnh sửa đơn vị tổ chức" : "Tạo đơn vị tổ chức mới";

  return (
    <div className="workspace-stack">
      <PageHeader
        actions={
          <button className="button button--primary" onClick={() => openCreateDialog()} type="button">
            <Icon name="plus" />
            <span>Tạo đơn vị mới</span>
          </button>
        }
        description="Quản lý và trực quan hóa cây cơ cấu phân cấp phòng ban, khối và nhóm chuyên môn."
        title="Cơ cấu tổ chức"
      />

      {fetchError && (
        <div className="notice notice--error" style={{ marginBottom: "1rem" }}>
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

      {/* KPI Stats Grid */}
      <section aria-label="Thống kê cơ cấu tổ chức" className="kpi-grid">
        <div className="kpi-card is-active">
          <div className="kpi-card__header">
            <span className="kpi-card__label">Tổng đơn vị</span>
            <span className="kpi-card__icon kpi-card__icon--indigo">
              <Icon name="organization" />
            </span>
          </div>
          <div className="kpi-card__val">{stats.total}</div>
          <div className="kpi-card__desc">{stats.active} hoạt động</div>
        </div>

        <div className="kpi-card">
          <div className="kpi-card__header">
            <span className="kpi-card__label">Khối / Trung tâm</span>
            <span className="kpi-card__icon kpi-card__icon--purple">
              <Icon name="branch" />
            </span>
          </div>
          <div className="kpi-card__val kpi-card__val--purple">{stats.centers}</div>
          <div className="kpi-card__desc">Cấp 1 trực thuộc công ty</div>
        </div>

        <div className="kpi-card">
          <div className="kpi-card__header">
            <span className="kpi-card__label">Phòng ban</span>
            <span className="kpi-card__icon kpi-card__icon--emerald">
              <Icon name="users" />
            </span>
          </div>
          <div className="kpi-card__val kpi-card__val--emerald">{stats.departments}</div>
          <div className="kpi-card__desc">Cấp 2 nghiệp vụ</div>
        </div>

        <div className="kpi-card">
          <div className="kpi-card__header">
            <span className="kpi-card__label">Nhóm chuyên môn</span>
            <span className="kpi-card__icon kpi-card__icon--rose">
              <Icon name="user" />
            </span>
          </div>
          <div className="kpi-card__val kpi-card__val--rose">{stats.teams}</div>
          <div className="kpi-card__desc">Cấp 3 dự án / kỹ thuật</div>
        </div>
      </section>

      {/* Main Split Grid */}
      <div className="organization-layout">
        {/* Left Panel: Visual Tree Hierarchy */}
        <section aria-labelledby="org-tree-title" className="data-panel">
          <div className="data-panel__header">
            <div>
              <h2 id="org-tree-title">Cây phân cấp đơn vị</h2>
              <p>Hiển thị {allUnits.length} đơn vị trong cơ cấu</p>
            </div>
            <div className="tree-toolbar-actions">
              <button className="button button--secondary" onClick={expandAll} type="button">
                <span>Mở rộng</span>
              </button>
              <button className="button button--secondary" onClick={collapseAll} type="button">
                <span>Thu gọn</span>
              </button>
            </div>
          </div>

          <div className="organization-tree-panel__body">
            {/* Tree Search Box */}
            <div className="search-field org-tree-search">
              <Icon name="search" />
              <label className="sr-only" htmlFor="tree-search">Tìm kiếm đơn vị</label>
              <input
                className="input"
                id="tree-search"
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Tìm theo tên hoặc mã đơn vị..."
                type="search"
                value={query}
              />
              {query && (
                <button
                  aria-label="Xóa từ khóa tìm kiếm"
                  className="search-clear-btn"
                  onClick={() => setQuery("")}
                  type="button"
                >
                  <Icon name="close" />
                </button>
              )}
            </div>

            <p className="organization-tree__hint">Kéo thả biểu tượng để thay đổi đơn vị cha trực thuộc.</p>

            {tree.length === 0 ? (
              <EmptyState
                icon="organization"
                message="Chưa có dữ liệu cơ cấu tổ chức."
                title="Chưa có đơn vị"
              />
            ) : (
              <div className="tree-scroll-container">
                <OrganizationTree
                  dragDisabled={Boolean(normalizedQuery)}
                  draggedUnitId={draggedUnitId}
                  dropTargetId={dropTargetId}
                  expandedUnits={expandedUnits}
                  onCanDrop={canMoveUnit}
                  onDragEnd={resetDragState}
                  onDragOver={handleDragOver}
                  onDragStart={handleDragStart}
                  onDrop={handleDrop}
                  onSelect={setSelectedUnitId}
                  onToggle={toggleExpanded}
                  query={query}
                  selectedUnitId={selectedUnitId}
                  units={tree}
                />
              </div>
            )}
          </div>
        </section>

        {/* Right Panel: Selected Unit Detail View */}
        <section aria-labelledby="unit-detail-title" className="organization-detail-panel">
          {selectedUnit ? (
            <>
              <div className="org-detail-hero">
                <div className="org-detail-hero__top">
                  <div className="org-detail-hero__title-group">
                    <div className={`org-detail-hero__icon ${selectedUnit.status === "INACTIVE" ? "is-inactive" : ""}`}>
                      <Icon name={selectedUnit.unitType === "COMPANY" ? "organization" : selectedUnit.unitType === "CENTER" ? "branch" : selectedUnit.unitType === "DEPARTMENT" ? "users" : "user"} />
                    </div>
                    <h2 className="org-detail-hero__title" id="unit-detail-title">{selectedUnit.unitName}</h2>
                  </div>

                  <div className="org-detail-hero__actions">
                    <button
                      className="button button--secondary"
                      onClick={() => openCreateDialog(selectedUnit.id)}
                      type="button"
                    >
                      <Icon name="plus" />
                      <span>Thêm đơn vị con</span>
                    </button>
                    <button
                      className="button button--secondary"
                      onClick={() => openEditDialog(selectedUnit)}
                      type="button"
                    >
                      <Icon name="edit" />
                      <span>Sửa</span>
                    </button>
                    {selectedUnit.parentId !== null && (
                      <button
                        className={`button ${selectedUnit.status === "ACTIVE" ? "button--danger" : "button--secondary"}`}
                        onClick={() => handleToggleStatus(selectedUnit)}
                        type="button"
                      >
                        <Icon name={selectedUnit.status === "ACTIVE" ? "lock" : "unlock"} />
                        <span>{selectedUnit.status === "ACTIVE" ? "Khóa" : "Mở"}</span>
                      </button>
                    )}
                  </div>
                </div>

                <div className="org-detail-hero__badges">
                  <span className="org-detail-code-badge">{selectedUnit.unitCode}</span>
                  <span className="role-chip">{selectedUnit.unitType === "COMPANY" ? "CÔNG TY" : selectedUnit.unitType === "CENTER" ? "KHỐI" : selectedUnit.unitType === "DEPARTMENT" ? "PHÒNG BAN" : "NHÓM"}</span>
                  <span className="org-level-pill">Cấp {selectedUnit.level}</span>
                  <StatusBadge status={selectedUnit.status === "ACTIVE" ? "ACTIVE" : "LOCKED"} />
                </div>
              </div>

              <div className="org-detail-body">
                <div className="org-prop-table">
                  <div className="org-prop-row">
                    <span className="org-prop-label">
                      <Icon name="branch" />
                      Đơn vị cha trực thuộc
                    </span>
                    <span className="org-prop-value">
                      {parentUnit ? (
                        <button className="org-parent-link" onClick={() => setSelectedUnitId(parentUnit.id)} type="button">
                          <Icon name="branch" />
                          <span>{parentUnit.unitName}</span>
                          <small>{parentUnit.unitCode}</small>
                        </button>
                      ) : (
                        <span className="org-prop-root">Đơn vị gốc</span>
                      )}
                    </span>
                  </div>

                  <div className="org-prop-row">
                    <span className="org-prop-label">
                      <Icon name="user" />
                      Người quản lý
                    </span>
                    <span className="org-prop-value">
                      {managerUser ? (
                        <span style={{ display: "inline-flex", alignItems: "center", gap: "0.375rem", fontWeight: 600, color: "#4338ca" }}>
                          <span>{managerUser.fullName || managerUser.username}</span>
                          <small style={{ color: "#6366f1" }}>({managerUser.roleCode}) {managerUser.roleName}</small>
                        </span>
                      ) : (
                        <span style={{ color: "#94a3b8", fontStyle: "italic" }}>Chưa chỉ định</span>
                      )}
                    </span>
                  </div>

                  <div className="org-prop-row">
                    <span className="org-prop-label">
                      <Icon name="organization" />
                      Cấu trúc trực thuộc
                    </span>
                    <span className="org-prop-value">
                      {selectedUnit.children.length} đơn vị cấp con
                    </span>
                  </div>

                  <div className="org-prop-row">
                    <span className="org-prop-label">
                      <Icon name="users" />
                      Nhân sự phụ trách
                    </span>
                    <span className="org-prop-value">
                      {unitMembers.length} thành viên
                    </span>
                  </div>
                </div>

                <div className="org-note-card">
                  <div className="org-note-card__header">
                    <Icon name="document" />
                    <span>Mô tả & chức năng nhiệm vụ</span>
                  </div>
                  {selectedUnit.description ? (
                    <p className="org-note-card__text">{selectedUnit.description}</p>
                  ) : (
                    <p className="org-note-card__empty">Chưa có mô tả chức năng nhiệm vụ cho đơn vị này.</p>
                  )}
                </div>

                <div className="org-members-block">
                  <div className="org-members-block__header">
                    <span className="org-members-block__title">
                      <Icon name="users" />
                      Nhân sự trực thuộc: {unitMembers.length}
                    </span>
                  </div>

                  {unitMembers.length === 0 ? (
                    <div className="org-members-empty-card">
                      Chưa có nhân sự nào được gán trực thuộc đơn vị này.
                    </div>
                  ) : (
                    <div className="org-members-grid">
                      {unitMembers.map((member) => {
                        const memberName = member.fullName || member.username || "Thành viên";
                        return (
                          <div className="org-member-item" key={member.id}>
                            <span className="avatar avatar--small avatar--gradient">
                              {memberName.slice(0, 1).toUpperCase()}
                            </span>
                            <div className="org-member-item__info">
                              <strong>{memberName}</strong>
                              <span>@{member.username} · {member.roleName}</span>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              </div>
            </>
          ) : (
            <div style={{ padding: "2rem" }}>
              <EmptyState
                icon="organization"
                message="Chọn một đơn vị từ cây phân cấp bên trái để xem thông tin chi tiết."
                title="Chưa chọn đơn vị"
              />
            </div>
          )}
        </section>
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
