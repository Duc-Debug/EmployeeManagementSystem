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
      if (parentId