"use client";

import { useMemo, useState } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Dialog } from "@/components/ui/Dialog";
import { EmptyState } from "@/components/ui/EmptyState";
import { Icon } from "@/components/ui/Icon";
import { findOrgUnit, flattenOrgTree, getParentOrgUnit } from "@/lib/organization";
import { DEMO_ORG_UNIT_TREE } from "@/src/mocks/hrm";
import type { OrgUnitTreeNode, OrgUnitType } from "@/src/types/hrm";

interface UnitDraft {
  description: string;
  parentId: string;
  unitCode: string;
  unitName: string;
  unitType: OrgUnitType;
}

type UnitErrors = Partial<Record<keyof UnitDraft, string>>;

function createInitialDraft(parentId: number): UnitDraft {
  return {
    description: "",
    parentId: String(parentId),
    unitCode: "",
    unitName: "",
    unitType: "DEPARTMENT",
  };
}

export function OrganizationWorkspace() {
  const [tree, setTree] = useState<readonly OrgUnitTreeNode[]>(DEMO_ORG_UNIT_TREE);
  const [selectedUnitId, setSelectedUnitId] = useState<number>(DEMO_ORG_UNIT_TREE[0]?.id ?? 0);
  const [expandedUnits, setExpandedUnits] = useState<Set<number>>(() => new Set(flattenOrgTree(DEMO_ORG_UNIT_TREE).map((unit) => unit.id)));
  const [query, setQuery] = useState("");
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [draft, setDraft] = useState<UnitDraft>(() => createInitialDraft(DEMO_ORG_UNIT_TREE[0]?.id ?? 0));
  const [errors, setErrors] = useState<UnitErrors>({});
  const [announcement, setAnnouncement] = useState("");
  const allUnits = useMemo(() => flattenOrgTree(tree), [tree]);
  const selectedUnit = findOrgUnit(tree, selectedUnitId) ?? allUnits[0];
  const normalizedQuery = query.trim().toLocaleLowerCase("vi");

  function updateDraft<Key extends keyof UnitDraft>(key: Key, value: UnitDraft[Key]) {
    setDraft((currentDraft) => ({ ...currentDraft, [key]: value }));
    setErrors((currentErrors) => ({ ...currentErrors, [key]: undefined }));
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

  function openCreateDialog() {
    const parentId = selectedUnit?.id ?? allUnits[0]?.id ?? 0;
    setDraft(createInitialDraft(parentId));
    setErrors({});
    setIsCreateOpen(true);
  }

  function validateDraft() {
    const nextErrors: UnitErrors = {};
    if (!draft.unitCode.trim()) nextErrors.unitCode = "Mã đơn vị là bắt buộc.";
    if (!draft.unitName.trim()) nextErrors.unitName = "Tên đơn vị là bắt buộc.";
    if (!draft.parentId) nextErrors.parentId = "Hãy chọn đơn vị cha.";
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  function handleCreateUnit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!validateDraft()) {
      return;
    }

    const parentId = Number(draft.parentId);
    const parent = findOrgUnit(tree, parentId);
    if (!parent) {
      setErrors({ parentId: "Không tìm thấy đơn vị cha trong cây minh họa." });
      return;
    }

    const newUnitId = Date.now();
    const newUnit: OrgUnitTreeNode = {
      children: [],
      description: draft.description.trim() || null,
      id: newUnitId,
      level: parent.level + 1,
      managerId: null,
      parentId,
      status: "ACTIVE",
      treePath: `${parent.treePath}${newUnitId}/`,
      unitCode: draft.unitCode.trim().toUpperCase(),
      unitName: draft.unitName.trim(),
      unitType: draft.unitType,
    };

    setTree((currentTree) => appendUnit(currentTree, parentId, newUnit));
    setExpandedUnits((currentUnits) => new Set(currentUnits).add(parentId));
    setSelectedUnitId(newUnit.id);
    setIsCreateOpen(false);
    setAnnouncement(`Đã thêm ${newUnit.unitName} vào cây minh họa.`);
  }

  return (
    <div className="workspace-stack">
      <PageHeader
        actions={
          <button className="button button--primary" onClick={openCreateDialog} type="button">
            <Icon name="plus" />
            Tạo đơn vị
          </button>
        }
        description="Duyệt quan hệ cha/con và xem thông tin cấu trúc. Một số trường chờ API chi tiết để hiển thị dữ liệu thật."
        title="Cây tổ chức"
      />

      {announcement ? <p aria-live="polite" className="sr-only">{announcement}</p> : null}

      <div className="organization-layout">
        <section aria-labelledby="org-tree-title" className="data-panel organization-tree-panel">
          <div className="data-panel__header">
            <div>
              <h2 id="org-tree-title">Cơ cấu tổ chức</h2>
              <p>{allUnits.length} đơn vị trong dữ liệu minh họa</p>
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
            <ul className="organization-tree">
              {tree.map((unit) => (
                <OrgTreeItem
                  key={unit.id}
                  onSelect={setSelectedUnitId}
                  onToggle={toggleExpanded}
                  query={normalizedQuery}
                  selectedUnitId={selectedUnitId}
                  unit={unit}
                  visibleExpandedUnits={expandedUnits}
                />
              ))}
            </ul>
            {!tree.some((unit) => matchesUnitOrChild(unit, normalizedQuery)) ? (
              <EmptyState icon="search" message="Thử tìm bằng mã hoặc tên đơn vị trong dữ liệu minh họa." title="Không có đơn vị phù hợp" />
            ) : null}
          </div>
        </section>

        {selectedUnit ? <OrganizationDetail selectedUnit={selectedUnit} tree={tree} /> : null}
      </div>

      <Dialog
        description="Các trường này khớp với endpoint tạo đơn vị hiện có. Người quản lý và nhân sự cần API bổ sung trước khi có thể ghi nhận chính thức."
        onClose={() => setIsCreateOpen(false)}
        open={isCreateOpen}
        title="Tạo đơn vị tổ chức"
      >
        <form className="form" noValidate onSubmit={handleCreateUnit}>
          <div className="form-grid form-grid--two">
            <OrgField error={errors.unitCode} id="unit-code" label="Mã đơn vị">
              <input aria-describedby="unit-code-message" aria-invalid={Boolean(errors.unitCode)} className="input" id="unit-code" onChange={(event) => updateDraft("unitCode", event.target.value)} placeholder="vd. P-KYTHUAT" required value={draft.unitCode} />
            </OrgField>
            <OrgField error={errors.unitName} id="unit-name" label="Tên đơn vị">
              <input aria-describedby="unit-name-message" aria-invalid={Boolean(errors.unitName)} className="input" id="unit-name" onChange={(event) => updateDraft("unitName", event.target.value)} required value={draft.unitName} />
            </OrgField>
          </div>
          <div className="form-grid form-grid--two">
            <OrgField error={errors.parentId} id="unit-parent" label="Đơn vị cha">
              <select aria-describedby="unit-parent-message" aria-invalid={Boolean(errors.parentId)} className="select" id="unit-parent" onChange={(event) => updateDraft("parentId", event.target.value)} required value={draft.parentId}>
                {allUnits.map((unit) => <option key={unit.id} value={unit.id}>{unit.unitCode} · {unit.unitName}</option>)}
              </select>
            </OrgField>
            <OrgField id="unit-type" label="Loại đơn vị">
              <select aria-describedby="unit-type-message" className="select" id="unit-type" onChange={(event) => updateDraft("unitType", event.target.value as OrgUnitType)} value={draft.unitType}>
                <option value="COMPANY">Công ty</option>
                <option value="CENTER">Khối / Trung tâm</option>
                <option value="DEPARTMENT">Phòng ban</option>
                <option value="TEAM">Nhóm chuyên môn</option>
              </select>
            </OrgField>
          </div>
          <OrgField id="unit-description" label="Mô tả">
            <textarea aria-describedby="unit-description-message" className="textarea" id="unit-description" onChange={(event) => updateDraft("description", event.target.value)} placeholder="Mô tả ngắn về đơn vị" value={draft.description} />
          </OrgField>
          <div className="notice notice--warning">
            <Icon name="alert" />
            <span>API hiện chưa hỗ trợ trường người quản lý hoặc danh sách nhân sự của đơn vị. Giao diện không tự suy diễn hai dữ liệu này.</span>
          </div>
          <div className="form-actions">
            <button className="button button--quiet" onClick={() => setIsCreateOpen(false)} type="button">Hủy</button>
            <button className="button button--primary" type="submit">Tạo đơn vị</button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}

function OrgTreeItem({
  onSelect,
  onToggle,
  query,
  selectedUnitId,
  unit,
  visibleExpandedUnits,
}: Readonly<{
  onSelect: (unitId: number) => void;
  onToggle: (unitId: number) => void;
  query: string;
  selectedUnitId: number;
  unit: OrgUnitTreeNode;
  visibleExpandedUnits: ReadonlySet<number>;
}>) {
  if (!matchesUnitOrChild(unit, query)) {
    return null;
  }

  const hasChildren = unit.children.length > 0;
  const isExpanded = Boolean(query) || visibleExpandedUnits.has(unit.id);
  const isSelected = selectedUnitId === unit.id;

  return (
    <li className="tree-node">
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
        ) : <span aria-hidden="true" className="tree-node__toggle-placeholder" />}
        <button
          aria-current={isSelected ? "true" : undefined}
          className={isSelected ? "tree-node__button is-selected" : "tree-node__button"}
          onClick={() => onSelect(unit.id)}
          type="button"
        >
          <Icon name="organization" />
          <span className="tree-node__copy">
            <strong>{unit.unitName}</strong>
            <small>{unit.unitCode}</small>
          </span>
        </button>
      </div>
      {hasChildren && isExpanded ? (
        <ul className="organization-tree organization-tree--nested">
          {unit.children.map((child) => (
            <OrgTreeItem
              key={child.id}
              onSelect={onSelect}
              onToggle={onToggle}
              query={query}
              selectedUnitId={selectedUnitId}
              unit={child}
              visibleExpandedUnits={visibleExpandedUnits}
            />
          ))}
        </ul>
      ) : null}
    </li>
  );
}

function OrganizationDetail({ selectedUnit, tree }: Readonly<{ selectedUnit: OrgUnitTreeNode; tree: readonly OrgUnitTreeNode[] }>) {
  const parent = getParentOrgUnit(tree, selectedUnit.parentId);

  return (
    <section aria-labelledby="org-detail-title" className="data-panel organization-detail-panel">
      <div className="data-panel__header">
        <div>
          <h2 id="org-detail-title">{selectedUnit.unitName}</h2>
          <p>{selectedUnit.unitCode} · Cấp {selectedUnit.level}</p>
        </div>
        <UnitStatusBadge status={selectedUnit.status} />
      </div>
      <div className="data-panel__body workspace-stack">
        <dl className="detail-list">
          <div><dt>Loại đơn vị</dt><dd>{formatUnitType(selectedUnit.unitType)}</dd></div>
          <div><dt>Đơn vị cha</dt><dd>{parent ? parent.unitName : "Đơn vị gốc"}</dd></div>
          <div><dt>Người quản lý</dt><dd>{selectedUnit.managerId ? `ID ${selectedUnit.managerId}` : "Chưa có dữ liệu từ API"}</dd></div>
          <div><dt>Trạng thái</dt><dd>{selectedUnit.status === "ACTIVE" ? "Đang hoạt động" : "Ngừng hoạt động"}</dd></div>
        </dl>

        {selectedUnit.description ? <p className="detail-description">{selectedUnit.description}</p> : <p className="detail-description detail-description--empty">Đơn vị này chưa có mô tả.</p>}

        <div className="detail-section">
          <div className="detail-section__heading">
            <h3>Nhân sự thuộc đơn vị</h3>
            <span>Chờ dữ liệu</span>
          </div>
          <EmptyState icon="users" message="Endpoint cây tổ chức hiện không trả danh sách nhân sự. Kết nối API chi tiết để hiển thị danh sách thành viên." title="Chưa có dữ liệu nhân sự" />
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
      {isActive ? "Hoạt động" : "Ngừng hoạt động"}
    </span>
  );
}

function OrgField({ children, error, id, label }: Readonly<{ children: React.ReactNode; error?: string; id: string; label: string }>) {
  const hintId = `${id}-message`;
  return (
    <div className="field-group">
      <label htmlFor={id}>{label}</label>
      {children}
      <p className={error ? "field-error" : "field-hint"} id={hintId}>{error ?? " "}</p>
    </div>
  );
}

function matchesUnitOrChild(unit: OrgUnitTreeNode, query: string): boolean {
  if (!query) {
    return true;
  }

  const unitMatches = [unit.unitCode, unit.unitName].some((value) => value.toLocaleLowerCase("vi").includes(query));
  return unitMatches || unit.children.some((child) => matchesUnitOrChild(child, query));
}

function appendUnit(nodes: readonly OrgUnitTreeNode[], parentId: number, newUnit: OrgUnitTreeNode): readonly OrgUnitTreeNode[] {
  return nodes.map((node) => {
    if (node.id === parentId) {
      return { ...node, children: [...node.children, newUnit] };
    }

    return { ...node, children: appendUnit(node.children, parentId, newUnit) };
  });
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
