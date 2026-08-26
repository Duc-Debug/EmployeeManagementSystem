"use client";

import { useCallback, useMemo, useRef, useState, type DragEvent, type FormEvent } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Dialog } from "@/components/ui/Dialog";
import { EmptyState } from "@/components/ui/EmptyState";
import { Icon } from "@/components/ui/Icon";
import {
  appendOrgUnit,
  findOrgUnit,
  flattenOrgTree,
  getDescendantIds,
  getParentOrgUnit,
  reparentOrgUnitTree,
  updateOrgUnitInfo,
  updateOrgUnitStatus,
} from "@/lib/organization";
import { DEMO_ORG_UNIT_TREE, DEMO_USERS } from "@/src/mocks/hrm";
import type { OrgUnitTreeNode, OrgUnitType, User } from "@/src/types/hrm";

import { OrgUnitForm, type OrgUnitDraft, type OrgUnitDraftErrors } from "@/features/organization/OrgUnitForm";
import { OrganizationTree } from "@/features/organization/OrganizationTree";

type OrgEditorState = { mode: "create" } | { mode: "edit"; unitId: number } | null;

function createInitialDraft(parentId: number | null): OrgUnitDraft {
  return {
    description: "",
    parentId: parentId ? String(parentId) : "",
    unitCode: "",
    unitName: "",
    unitType: "DEPARTMENT",
  };
}

function toEditDraft(unit: OrgUnitTreeNode): OrgUnitDraft {
  return {
    description: unit.description ?? "",
    parentId: unit.parentId ? String(unit.parentId) : "",
    unitCode: unit.unitCode,
    unitName: unit.unitName,
    unitType: unit.unitType,
  };
}

export function OrganizationWorkspace() {
  const [tree, setTree] = useState<readonly OrgUnitTreeNode[]>(DEMO_ORG_UNIT_TREE);
  const [selectedUnitId, setSelectedUnitId] = useState<number>(DEMO_ORG_UNIT_TREE[0]?.id ?? 0);
  const [expandedUnits, setExpandedUnits] = useState<Set<number>>(() => new Set(flattenOrgTree(DEMO_ORG_UNIT_TREE).map((unit) => unit.id)));
  const [query, setQuery] = useState("");
  const [editor, setEditor] = useState<OrgEditorState>(null);
  const [lockTarget, setLockTarget] = useState<OrgUnitTreeNode | null>(null);
  const [draft, setDraft] = useState<OrgUnitDraft>(() => createInitialDraft(DEMO_ORG_UNIT_TREE[0]?.id ?? null));
  const [errors, setErrors] = useState<OrgUnitDraftErrors>({});
  const [announcement, setAnnouncement] = useState("");
  const [draggedUnitId, setDraggedUnitId] = useState<number | null>(null);
  const [dropTargetId, setDropTargetId] = useState<number | null>(null);
  const editorFocusRef = useRef<HTMLElement>(null);
  const submitRef = useRef<HTMLButtonElement>(null);
  const setEditorFocus = useCallback((element: HTMLElement | null) => {
    editorFocusRef.current = element;
  }, []);
  const allUnits = useMemo(() => flattenOrgTree(tree), [tree]);
  const selectedUnit = findOrgUnit(tree, selectedUnitId) ?? allUnits[0];
  const normalizedQuery = query.trim().toLocaleLowerCase("vi");
  const editingUnit = editor?.mode === "edit" ? findOrgUnit(tree, editor.unitId) : undefined;
  
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
    return DEMO_USERS.filter((user) => user.orgUnitId === selectedUnit.id);
  }, [selectedUnit]);

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

  function handleToggleStatus(unit: OrgUnitTreeNode) {
    if (unit.status === "ACTIVE") {
      setLockTarget(unit);
    } else {
      // Direct unlock
      setTree((currentTree) => updateOrgUnitStatus(currentTree, unit.id, "ACTIVE"));
      setAnnouncement(`Đã mở khóa và kích hoạt đơn vị ${unit.unitName}.`);
    }
  }

  function confirmLockUnit() {
    if (!lockTarget) return;
    setTree((currentTree) => updateOrgUnitStatus(currentTree, lockTarget.id, "INACTIVE"));
    setAnnouncement(`Đã tạm khóa đơn vị ${lockTarget.unitName}.`);
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

  function saveOrgUnit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editor || !validateDraft()) {
      return;
    }

    const parentId = draft.parentId ? Number(draft.parentId) : null;
    if (editor.mode === "create") {
      const parent = parentId ? findOrgUnit(tree, parentId) : undefined;
      const newUnitId = Date.now();
      const newUnit: OrgUnitTreeNode = {
        children: [],
        description: draft.description.trim() || null,
        id: newUnitId,
        level: parent ? parent.level + 1 : 0,
        managerId: null,
        parentId,
        status: "ACTIVE",
        treePath: parent ? `${parent.treePath}${newUnitId}/` : `/${newUnitId}/`,
        unitCode: draft.unitCode.trim().toUpperCase(),
        unitName: draft.unitName.trim(),
        unitType: draft.unitType,
      };
      setTree((currentTree) => appendOrgUnit(currentTree, parentId, newUnit));
      if (parentId) {
        setExpandedUnits((currentUnits) => new Set(currentUnits).add(parentId));
      }
      setSelectedUnitId(newUnitId);
      setAnnouncement(`Đã tạo đơn vị ${newUnit.unitName}.`);
      closeEditor();
      return;
    }

    if (!editingUnit) {
      return;
    }

    setTree((currentTree) => {
      const updatedTree = updateOrgUnitInfo(currentTree, editingUnit.id, {
        description: draft.description.trim() || null,
        unitName: draft.unitName.trim(),
        unitType: draft.unitType,
      });
      if (parentId !== null && parentId !== editingUnit.parentId) {
        return reparentOrgUnitTree(updatedTree, editingUnit.id, parentId);
      }

      return updatedTree;
    });
    if (parentId && parentId !== editingUnit.parentId) {
      setExpandedUnits((currentUnits) => new Set(currentUnits).add(parentId));
    }
    setAnnouncement(`Đã cập nhật đơn vị ${draft.unitName.trim()}.`);
    closeEditor();
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

  function handleDrop(event: DragEvent<HTMLElement>, targetUnitId: number) {
    event.preventDefault();
    const sourceId = draggedUnitId ?? Number(event.dataTransfer.getData("text/plain"));
    if (!sourceId || !canMoveUnit(sourceId, targetUnitId)) {
      resetDragState();
      return;
    }

    const source = findOrgUnit(tree, sourceId);
    const target = findOrgUnit(tree, targetUnitId);
    setTree((currentTree) => reparentOrgUnitTree(currentTree, sourceId, targetUnitId));
    setExpandedUnits((currentUnits) => new Set(currentUnits).add(targetUnitId));
    setSelectedUnitId(sourceId);
    if (source && target) {
      setAnnouncement(`Đã chuyển ${source.unitName} vào ${target.unitName}.`);
    }
    resetDragState();
  }

  const editorMode = editor?.mode;
  const editorTitle = editorMode === "edit" ? "Chỉnh sửa đơn vị tổ chức" : "Tạo đơn vị tổ chức mới";

  return (
    <div className="workspace-stack">
      <PageHeader
        actions={
          <button className="button button--primary create-user-cta" onClick={() => openCreateDialog()} type="button">
            <Icon name="plus" />
            <span>Tạo đơn vị mới</span>
          </button>
        }
        description="Quản lý và trực quan hóa cây cơ cấu phân cấp phòng ban, khối và nhóm chuyên môn."
        title="Cơ cấu tổ chức"
      />

      {announcement ? <p aria-live="polite" className="sr-only">{announcement}</p> : null}

      {/* KPI Metric Cards - Scaled Down 80% */}
      <section aria-label="Thống kê cơ cấu tổ chức" className="kpi-grid">
        <div className="kpi-card is-active">
          <div className="kpi-card__header">
            <span className="kpi-card__label">Tổng đơn vị</span>
            <span className="kpi-card__icon kpi-card__icon--indigo">
              <Icon name="organization" />
            </span>
          </div>
          <div className="kpi-card__val">{stats.total}</div>
          <div className="kpi-card__desc">{stats.active} hoạt động {stats.locked > 0 ? `· ${stats.locked} khóa` : ""}</div>
        </div>

        <div className="kpi-card">
          <div className="kpi-card__header">
            <span className="kpi-card__label">Khối / Trung tâm</span>
            <span className="kpi-card__icon kpi-card__icon--emerald">
              <Icon name="branch" />
            </span>
          </div>
          <div className="kpi-card__val kpi-card__val--emerald">{stats.centers}</div>
          <div className="kpi-card__desc">Cấp 1 trực thuộc công ty</div>
        </div>

        <div className="kpi-card">
          <div className="kpi-card__header">
            <span className="kpi-card__label">Phòng ban</span>
            <span className="kpi-card__icon kpi-card__icon--purple">
              <Icon name="users" />
            </span>
          </div>
          <div className="kpi-card__val kpi-card__val--purple">{stats.departments}</div>
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

      {/* Main Split Layout: Wider Tree on Left (58%), Narrower Detail on Right (42%) */}
      <div className="organization-layout">
        {/* Tree Panel */}
        <section aria-labelledby="org-tree-title" className="data-panel organization-tree-panel">
          <div className="data-panel__header">
            <div>
              <h2 id="org-tree-title">Cây phân cấp đơn vị</h2>
              <p>Hiển thị {allUnits.length} đơn vị trong cơ cấu</p>
            </div>
            <div className="tree-toolbar-actions">
              <button className="button button--secondary" onClick={expandAll} title="Mở rộng toàn bộ cây" type="button">
                Mở rộng
              </button>
              <button className="button button--secondary" onClick={collapseAll} title="Thu gọn toàn bộ cây" type="button">
                Thu gọn
              </button>
            </div>
          </div>

          <div className="data-panel__body organization-tree-panel__body">
            <div className="search-field">
              <Icon name="search" />
              <label className="sr-only" htmlFor="organization-search">Tìm đơn vị tổ chức</label>
              <input
                className="input"
                id="organization-search"
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Tìm theo tên hoặc mã đơn vị..."
                type="search"
                value={query}
              />
              {query && (
                <button aria-label="Xóa tìm kiếm" className="search-clear-btn" onClick={() => setQuery("")} type="button">
                  <Icon name="close" />
                </button>
              )}
            </div>

            <p className="organization-tree__hint">
              {normalizedQuery
                ? "Đang tìm kiếm. Xóa từ khóa để kéo thả sắp xếp cơ cấu."
                : "Kéo thả biểu tượng để thay đổi đơn vị cha trực thuộc."}
            </p>

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
                query={normalizedQuery}
                selectedUnitId={selectedUnitId}
                units={tree}
              />
            </div>

            {!tree.some((unit) => matchesUnitOrChild(unit, normalizedQuery)) ? (
              <EmptyState
                action={<button className="button button--secondary" onClick={() => setQuery("")} type="button">Xóa bộ lọc</button>}
                icon="search"
                message="Không tìm thấy đơn vị nào khớp với từ khóa."
                title="Không tìm thấy đơn vị"
              />
            ) : null}
          </div>
        </section>

        {/* Detail Panel */}
        {selectedUnit ? (
          <OrganizationDetail
            members={unitMembers}
            onCreateChild={() => openCreateDialog(selectedUnit.id)}
            onEdit={openEditDialog}
            onSelectParent={(parentId) => setSelectedUnitId(parentId)}
            onToggleStatus={handleToggleStatus}
            selectedUnit={selectedUnit}
            tree={tree}
          />
        ) : null}
      </div>

      {/* Create / Edit Dialog */}
      <Dialog
        className="dialog--user-form"
        description={editorMode === "edit" ? "Cập nhật thông tin định danh và đơn vị cha của đơn vị tổ chức." : "Nhập thông tin đơn vị tổ chức và chọn đơn vị cấp cha."}
        footer={
          <>
            <button className="button button--secondary" onClick={closeEditor} type="button">Hủy</button>
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
            mode={editorMode}
            onChange={updateDraft}
            onSubmit={saveOrgUnit}
            parentOptions={parentOptions}
            value={draft}
          />
        ) : null}
      </Dialog>

      {/* Lock Unit Confirmation Modal */}
      {lockTarget && (
        <Dialog
          className="dialog--compact"
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
          title="Tạm khóa đơn vị tổ chức"
        >
          <div className="lock-warning-card">
            <div className="lock-warning-card__icon">
              <Icon name="alert" />
            </div>
            <div className="lock-warning-card__info">
              <strong>{lockTarget.unitName} ({lockTarget.unitCode})</strong>
              <span>Khi tạm khóa, đơn vị này sẽ không thể chọn làm đơn vị cha mới cho đến khi mở khóa lại.</span>
            </div>
          </div>
          <p className="dialog-confirmation__text">
            Bạn có chắc chắn muốn ngừng hoạt động và tạm khóa đơn vị này không?
          </p>
        </Dialog>
      )}
    </div>
  );
}

interface OrganizationDetailProps {
  members: readonly User[];
  onCreateChild: () => void;
  onEdit: (unit: OrgUnitTreeNode) => void;
  onSelectParent: (parentId: number) => void;
  onToggleStatus: (unit: OrgUnitTreeNode) => void;
  selectedUnit: OrgUnitTreeNode;
  tree: readonly OrgUnitTreeNode[];
}

function OrganizationDetail({
  members,
  onCreateChild,
  onEdit,
  onSelectParent,
  onToggleStatus,
  selectedUnit,
  tree,
}: OrganizationDetailProps) {
  const parent = getParentOrgUnit(tree, selectedUnit.parentId);
  const isInactive = selectedUnit.status === "INACTIVE";

  const unitTypeLabels: Record<OrgUnitType, { label: string; tagClass: string; icon: "building" | "branch" | "users" | "user" }> = {
    CENTER: { icon: "branch", label: "Khối / Trung tâm", tagClass: "unit-tag--center" },
    COMPANY: { icon: "building", label: "Công ty", tagClass: "unit-tag--company" },
    DEPARTMENT: { icon: "users", label: "Phòng ban", tagClass: "unit-tag--dept" },
    TEAM: { icon: "user", label: "Nhóm chuyên môn", tagClass: "unit-tag--team" },
  };

  const currentMeta = unitTypeLabels[selectedUnit.unitType] ?? unitTypeLabels.DEPARTMENT;

  return (
    <section aria-labelledby="org-detail-title" className="data-panel organization-detail-panel">
      {/* Header Profile Banner */}
      <div className="org-detail-hero">
        <div className="org-detail-hero__top">
          <div className="org-detail-hero__title-group">
            <div className={`org-detail-hero__icon ${isInactive ? "is-inactive" : ""}`}>
              <Icon name={isInactive ? "lock" : currentMeta.icon} />
            </div>
            <h2 className="org-detail-hero__title" id="org-detail-title">{selectedUnit.unitName}</h2>
          </div>

          <div className="org-detail-hero__actions">
            <button className="button button--secondary button--compact" onClick={onCreateChild} title="Tạo đơn vị con trực thuộc" type="button">
              <Icon name="plus" />
              <span>Thêm đơn vị con</span>
            </button>
            <button className="table-action table-action--edit" onClick={() => onEdit(selectedUnit)} title="Chỉnh sửa đơn vị" type="button">
              <Icon name="settings" />
              <span>Sửa</span>
            </button>
            {selectedUnit.parentId !== null && (
              <button
                className={isInactive ? "table-action table-action--success" : "table-action table-action--danger"}
                onClick={() => onToggleStatus(selectedUnit)}
                title={isInactive ? "Mở khóa đơn vị" : "Tạm khóa đơn vị"}
                type="button"
              >
                <Icon name={isInactive ? "unlock" : "lock"} />
                <span>{isInactive ? "Mở" : "Khóa"}</span>
              </button>
            )}
          </div>
        </div>

        {/* Row 2: Metadata Badges */}
        <div className="org-detail-hero__badges">
          <span className="org-detail-code-badge">{selectedUnit.unitCode}</span>
          <span className={`org-unit-tag ${currentMeta.tagClass}`}>{currentMeta.label}</span>
          <span className="org-level-pill">Cấp {selectedUnit.level}</span>
          <UnitStatusBadge status={selectedUnit.status} />
        </div>
      </div>

      <div className="data-panel__body org-detail-body">
        {/* Key Attributes Structured Table */}
        <div className="org-prop-table">
          <div className="org-prop-row">
            <span className="org-prop-label">
              <Icon name="branch" />
              Đơn vị cha trực thuộc
            </span>
            <div className="org-prop-value">
              {parent ? (
                <button className="org-parent-link" onClick={() => onSelectParent(parent.id)} type="button">
                  <Icon name="building" />
                  <span>{parent.unitName}</span>
                  <small>({parent.unitCode})</small>
                </button>
              ) : (
                <span className="org-prop-root">Đơn vị gốc (Công ty)</span>
              )}
            </div>
          </div>

          <div className="org-prop-row">
            <span className="org-prop-label">
              <Icon name="organization" />
              Cấu trúc trực thuộc
            </span>
            <div className="org-prop-value">
              <strong>{selectedUnit.children.length}</strong> đơn vị cấp con
            </div>
          </div>

          <div className="org-prop-row">
            <span className="org-prop-label">
              <Icon name="users" />
              Nhân sự phụ trách
            </span>
            <div className="org-prop-value">
              <strong>{members.length}</strong> thành viên
            </div>
          </div>
        </div>

        {/* Description Section - Always Shown cleanly */}
        <div className="org-note-card">
          <div className="org-note-card__header">
            <Icon name="document" />
            <span>Mô tả & Chức năng nhiệm vụ</span>
          </div>
          {selectedUnit.description ? (
            <p className="org-note-card__text">{selectedUnit.description}</p>
          ) : (
            <p className="org-note-card__empty">Chưa có mô tả chi tiết cho đơn vị này.</p>
          )}
        </div>

        {/* Members List */}
        <div className="org-members-block">
          <div className="org-members-block__header">
            <div className="org-members-block__title">
              <Icon name="users" />
              <span>Nhân sự trực thuộc ({members.length})</span>
            </div>
          </div>

          {members.length > 0 ? (
            <div className="org-members-grid">
              {members.map((member) => (
                <div className="org-member-item" key={member.id}>
                  <span aria-hidden="true" className="avatar avatar--small avatar--gradient">
                    {member.fullName.slice(0, 1)}
                  </span>
                  <div className="org-member-item__info">
                    <strong>{member.fullName}</strong>
                    <span>@{member.username} · {member.roleName}</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="org-members-empty-card">
              <p>Chưa có tài khoản nhân sự nào được phân vào đơn vị này.</p>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}

function UnitStatusBadge({ status }: Readonly<{ status: OrgUnitTreeNode["status"] }>) {
  const isActive = status === "ACTIVE";
  return (
    <span className={isActive ? "status-badge status-badge--active" : "status-badge status-badge--locked"}>
      <span aria-hidden="true" className="status-badge__dot" />
      {isActive ? "Hoạt động" : "Tạm khóa"}
    </span>
  );
}

function matchesUnitOrChild(unit: OrgUnitTreeNode, query: string): boolean {
  if (!query) {
    return true;
  }

  const unitMatches = [unit.unitCode, unit.unitName].some((value) => value.toLocaleLowerCase("vi").includes(query));
  return unitMatches || unit.children.some((child) => matchesUnitOrChild(child, query));
}
