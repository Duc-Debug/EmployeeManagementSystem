import type { OrgUnitTreeNode } from "@/src/types/hrm";

export function flattenOrgTree(nodes: readonly OrgUnitTreeNode[]): OrgUnitTreeNode[] {
  if (!nodes || !Array.isArray(nodes)) return [];
  return nodes.flatMap((node) => [node, ...flattenOrgTree(node.children || [])]);
}

export function findOrgUnit(
  nodes: readonly OrgUnitTreeNode[],
  unitId: number,
): OrgUnitTreeNode | undefined {
  if (!nodes || !Array.isArray(nodes)) return undefined;
  for (const node of nodes) {
    if (node.id === unitId) {
      return node;
    }

    const match = findOrgUnit(node.children || [], unitId);
    if (match) {
      return match;
    }
  }

  return undefined;
}

export function getParentOrgUnit(
  nodes: readonly OrgUnitTreeNode[],
  parentId: number | null,
): OrgUnitTreeNode | undefined {
  if (parentId === null) {
    return undefined;
  }

  return findOrgUnit(nodes, parentId);
}

export function getDescendantIds(unit: OrgUnitTreeNode): ReadonlySet<number> {
  const ids = new Set<number>([unit.id]);

  function collect(children: readonly OrgUnitTreeNode[] | undefined) {
    (children || []).forEach((child) => {
      ids.add(child.id);
      collect(child.children);
    });
  }

  collect(unit.children);
  return ids;
}

export function appendOrgUnit(
  nodes: readonly OrgUnitTreeNode[],
  parentId: number | null,
  unit: OrgUnitTreeNode,
): readonly OrgUnitTreeNode[] {
  if (parentId === null) {
    return [...nodes, unit];
  }

  return nodes.map((node) => {
    if (node.id === parentId) {
      return { ...node, children: [...node.children, unit] };
    }

    return { ...node, children: appendOrgUnit(node.children, parentId, unit) };
  });
}

export function updateOrgUnitInfo(
  nodes: readonly OrgUnitTreeNode[],
  unitId: number,
  update: Pick<OrgUnitTreeNode, "description" | "unitName" | "unitType">,
): readonly OrgUnitTreeNode[] {
  return nodes.map((node) => {
    if (node.id === unitId) {
      return { ...node, ...update };
    }

    return { ...node, children: updateOrgUnitInfo(node.children, unitId, update) };
  });
}

export function updateOrgUnitStatus(
  nodes: readonly OrgUnitTreeNode[],
  unitId: number,
  status: "ACTIVE" | "INACTIVE",
): readonly OrgUnitTreeNode[] {
  return nodes.map((node) => {
    if (node.id === unitId) {
      return { ...node, status };
    }

    return { ...node, children: updateOrgUnitStatus(node.children, unitId, status) };
  });
}

export function reparentOrgUnitTree(
  nodes: readonly OrgUnitTreeNode[],
  unitId: number,
  newParentId: number,
): readonly OrgUnitTreeNode[] {
  const unit = findOrgUnit(nodes, unitId);
  const newParent = findOrgUnit(nodes, newParentId);
  if (!unit || !newParent || unit.parentId === null || unit.parentId === newParentId || getDescendantIds(unit).has(newParentId)) {
    return nodes;
  }

  const withoutUnit = detachOrgUnit(nodes, unitId);
  if (!withoutUnit.detachedUnit) {
    return nodes;
  }

  const parentAfterDetach = findOrgUnit(withoutUnit.nodes, newParentId);
  if (!parentAfterDetach || parentAfterDetach.status !== "ACTIVE") {
    return nodes;
  }

  const movedUnit = rebuildHierarchy(withoutUnit.detachedUnit, parentAfterDetach.id, parentAfterDetach.level, parentAfterDetach.treePath);
  return appendOrgUnit(withoutUnit.nodes, parentAfterDetach.id, movedUnit);
}

function detachOrgUnit(
  nodes: readonly OrgUnitTreeNode[],
  unitId: number,
): { detachedUnit?: OrgUnitTreeNode; nodes: readonly OrgUnitTreeNode[] } {
  let detachedUnit: OrgUnitTreeNode | undefined;
  const nextNodes: OrgUnitTreeNode[] = [];

  nodes.forEach((node) => {
    if (node.id === unitId) {
      detachedUnit = node;
      return;
    }

    const result = detachOrgUnit(node.children, unitId);
    if (result.detachedUnit) {
      detachedUnit = result.detachedUnit;
    }
    nextNodes.push({ ...node, children: result.nodes });
  });

  return { detachedUnit, nodes: nextNodes };
}

function rebuildHierarchy(
  unit: OrgUnitTreeNode,
  parentId: number,
  parentLevel: number,
  parentTreePath: string,
): OrgUnitTreeNode {
  const level = parentLevel + 1;
  const treePath = `${parentTreePath}${unit.id}/`;
  return {
    ...unit,
    level,
    parentId,
    treePath,
    children: unit.children.map((child) => rebuildHierarchy(child, unit.id, level, treePath)),
  };
}
