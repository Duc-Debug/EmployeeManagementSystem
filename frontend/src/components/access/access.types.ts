export type ActionKey = 'view' | 'create' | 'edit' | 'delete' | 'approve' | 'export';

export type DataScope =
    | 'all'
    | 'department_managed'
    | 'department_own'
    | 'personal'
    | 'custom_tree';

export type RoleTheme = 'purple' | 'blue' | 'indigo' | 'emerald' | 'amber' | 'rose' | 'slate';

export interface DataScopeConfig {
    type: DataScope;
    selectedNodeIds?: string[];
}

/** All six actions are always present — modules that don't support one
 *  simply leave it `false`, rather than the matrix having to render a
 *  separate "not applicable" state. */
export type PermissionAction = Record<ActionKey, boolean>;

/** Static definition of a module and its factory-default permissions. */
export interface ModuleDef {
    id: string;
    name: string;
    category?: string;
    defaultScope: DataScope;
    defaultActions: PermissionAction;
}

/** A module's *current* permission configuration for a given role. */
export interface ModulePermission {
    moduleId: string;
    moduleName: string;
    scope: DataScopeConfig;
    actions: PermissionAction;
}

export interface Role {
    id: string;
    name: string;
    description: string;
    isSystemRole?: boolean;
    userCount: number;
    theme: RoleTheme;
    permissions: Record<string, ModulePermission>;
}

export interface RoleBasicInfo {
    id?: string;
    name: string;
    description: string;
    theme: RoleTheme;
}