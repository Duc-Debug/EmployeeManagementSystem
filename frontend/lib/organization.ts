import type { OrgUnitTreeNode } from "@/src/types/hrm";

export function flattenOrgTree(nodes: readonly OrgUnitTreeNode[]): OrgUnitTreeNode[] {
  return nodes.flatMap((node) => [node, ...flattenOrgTree(node.children)]);
}

export function findOrgUnit(
  nodes: readonly OrgUnitTreeNode[],
  unitId: number,
): OrgUnitTreeNode | undefined {
  for (const node of nodes) {
    if (node.id === unitId) {
      return node;
    }

    const match = findOrgUnit(node.children, unitId);
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
