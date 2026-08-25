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
} from "@/lib/organization";
import { DEMO_ORG_UNIT_TREE } from "@/src/mocks/hrm";
import type { OrgUnitTreeNode, OrgUnitType } from "@/src/types/hrm";

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
      .map((unit) => ({ depth: unit.level, id: unit.id, unitCode: unit.unitCode, unitName: unit.unitName }));
  }, [allUnits, editingUnit]);

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

  function openCreateDialog() {
    setDraft(createInitialDraft(selectedUnit?.id ?? null));
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
  const editorTitle = editorMode === "edit" ? "Sửa đơn vị tổ chức" : "Tạo đơn vị tổ chức";

  return (
    <div className="workspace-stack">
      <PageHeader
        actions={
          <button className="button button--primary" onClick={openCreateDialog} type="button">
            <Icon name="plus" />
            Tạo đơn vị
          </button>
        }
        description="Theo dõi và quản lý cơ cấu tổ chức."
        title="Cây tổ chức"
      />

      {announcement ? <p aria-live="polite" className="sr-only">{announcement}</p> : null}

      <div className="organization-layout">
        <section aria-labelledby="org-tree-title" className="data-panel organization-tree-panel">
          <div className="data-panel__header">
            <div>
              <h2 id="org-tree-title">Cơ cấu tổ chức</h2>
              <p>{allUnits.length} đơn vị</p>
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
                placeholder="Tìm theo tên hoặc mã đơn vị"
                type="search"
                value={query}
              />
            </div>
            <p className="organization-tree__hint">
              {normalizedQuery ? "Xóa tìm kiếm để di chuyển đơn vị." : "Kéo biểu tượng để thay đổi đơn vị cha. Dùng Chỉnh sửa để di chuyển bằng bàn phím hoặc cảm ứng."}
            </p>
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
            {!tree.some((unit) => matchesUnitOrChild(unit, normalizedQuery)) ? (
              <EmptyState icon="search" message="Thử tìm bằng mã hoặc tên đơn vị." title="Không có đơn vị phù hợp" />
            ) : null}
          </div>
        </section>

        {selectedUnit ? <OrganizationDetail onEdit={openEditDialog} selectedUnit={selectedUnit} tree={tree} /> : null}
      </div>

      <Dialog
        description={editorMode === "edit" ? "Cập nhật thông tin và đơn vị cha của đơn vị tổ chức." : "Nhập thông tin đơn vị tổ chức."}
        footer={
          <>
            <button className="button button--quiet" onClick={closeEditor} type="button">Hủy</button>
            <button className="button button--primary" form="org-unit-form" ref={submitRef} type="submit">
              {editorMode === "edit" ? "Lưu thay đổi" : "Tạo đơn vị"}
            </button>
          </>
        }
        initialFocusRef={editorFocusRef}
        onClose={closeEditor}
        open={Boolean(editor)}
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
    </div>
  );
}

interface OrganizationDetailProps {
  onEdit: (unit: OrgUnitTreeNode) => void;
  selectedUnit: OrgUnitTreeNode;
  tree: readonly OrgUnitTreeNode[];
}

function OrganizationDetail({ onEdit, selectedUnit, tree }: OrganizationDetailProps) {
  const parent = getParentOrgUnit(tree, selectedUnit.parentId);

  return (
    <section aria-labelledby="org-detail-title" className="data-panel organization-detail-panel">
      <div className="data-panel__header">
        <div>
          <h2 id="org-detail-title">{selectedUnit.unitName}</h2>
          <p>{selectedUnit.unitCode} · Cấp {selectedUnit.level}</p>
        </div>
        <div className="organization-detail__actions">
          <UnitStatusBadge status={selectedUnit.status} />
          <button className="table-action" onClick={() => onEdit(selectedUnit)} type="button">
            <Icon name="settings" />
            Chỉnh sửa
          </button>
        </div>
      </div>
      <div className="data-panel__body workspace-stack">
        <dl className="detail-list">
          <div><dt>Loại đơn vị</dt><dd>{formatUnitType(selectedUnit.unitType)}</dd></div>
          <div><dt>Đơn vị cha</dt><dd>{parent ? parent.unitName : "Đơn vị gốc"}</dd></div>
          <div><dt>Trạng thái</dt><dd>{selectedUnit.status === "ACTIVE" ? "Đang hoạt động" : "Ngừng hoạt động"}</dd></div>
        </dl>

        {selectedUnit.description ? <p className="detail-description">{selectedUnit.description}</p> : <p className="detail-description detail-description--empty">Chưa có mô tả.</p>}
      </div>
    </section>
  );
}

function UnitStatusBadge({ status }: Readonly<{ status: OrgUnitTreeNode["status"] }>) {
  const isActive = status === "ACTIVE";
  return (
    <span className={isActive ? "status-badge status-badge--active" : "status-badge status-badge--locked"}>
      <span aria-hidden="true" className="status-badge__dot" />
      {isActive ? "Hoạt động" : "Ngừng hoạt động"}
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

function formatUnitType(unitType: OrgUnitType) {
  const labels: Record<OrgUnitType, string> = {
    CENTER: "Khối / Trung tâm",
    COMPANY: "Công ty",
    DEPARTMENT: "Phòng ban",
    TEAM: "Nhóm chuyên môn",
  };
  return labels[unitType];
}
