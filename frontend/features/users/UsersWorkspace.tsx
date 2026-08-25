"use client";

import { useCallback, useMemo, useRef, useState, type FormEvent } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { RoleBadge, ScopeBadge, StatusBadge } from "@/components/ui/Badge";
import { Dialog } from "@/components/ui/Dialog";
import { EmptyState } from "@/components/ui/EmptyState";
import { Icon } from "@/components/ui/Icon";
import { flattenOrgTree } from "@/lib/organization";
import { DEMO_ORG_UNIT_TREE, DEMO_ROLES, DEMO_USERS } from "@/src/mocks/hrm";
import type { User } from "@/src/types/hrm";

import { type UserAccountDraft, type UserAccountErrors, UserAccountForm } from "@/features/users/UserAccountForm";

type UserEditorState = { mode: "create" } | { mode: "edit"; userId: number } | null;

const EMPTY_DRAFT: UserAccountDraft = {
  dataScope: "SELF",
  employeeCode: "",
  fullName: "",
  orgUnitId: "",
  password: "",
  roleCode: "",
  scopeOrgUnitId: "",
  username: "",
};

function toEditDraft(user: User): UserAccountDraft {
  return {
    ...EMPTY_DRAFT,
    dataScope: user.dataScope,
    fullName: user.fullName,
    orgUnitId: user.orgUnitId ? String(user.orgUnitId) : "",
    roleCode: user.roleCode,
    scopeOrgUnitId: user.scopeOrgUnitId ? String(user.scopeOrgUnitId) : "",
    username: user.username,
  };
}

export function UsersWorkspace() {
  const [users, setUsers] = useState<User[]>(() => DEMO_USERS.map((user) => ({ ...user })));
  const [query, setQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [editor, setEditor] = useState<UserEditorState>(null);
  const [lockTarget, setLockTarget] = useState<User | null>(null);
  const [draft, setDraft] = useState<UserAccountDraft>(EMPTY_DRAFT);
  const [errors, setErrors] = useState<UserAccountErrors>({});
  const [announcement, setAnnouncement] = useState("");
  const editorFocusRef = useRef<HTMLElement>(null);
  const submitRef = useRef<HTMLButtonElement>(null);
  const lockCancelRef = useRef<HTMLButtonElement>(null);
  const setEditorFocus = useCallback((element: HTMLElement | null) => {
    editorFocusRef.current = element;
  }, []);
  const orgUnits = useMemo(() => flattenOrgTree(DEMO_ORG_UNIT_TREE), []);
  const orgUnitOptions = useMemo(() => orgUnits.map((orgUnit) => ({
    depth: orgUnit.level,
    id: orgUnit.id,
    unitCode: orgUnit.unitCode,
    unitName: orgUnit.unitName,
  })), [orgUnits]);

  const editingUser = editor?.mode === "edit" ? users.find((user) => user.id === editor.userId) : undefined;
  const filteredUsers = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase("vi");
    return users.filter((user) => {
      const searchMatches = !normalizedQuery || [user.fullName, user.username, user.orgUnitName ?? ""]
        .some((value) => value.toLocaleLowerCase("vi").includes(normalizedQuery));
      const roleMatches = roleFilter === "ALL" || user.roleCode === roleFilter;
      const statusMatches = statusFilter === "ALL" || user.status === statusFilter;
      return searchMatches && roleMatches && statusMatches;
    });
  }, [query, roleFilter, statusFilter, users]);

  function openCreateDialog() {
    setDraft(EMPTY_DRAFT);
    setErrors({});
    setEditor({ mode: "create" });
  }

  function openEditDialog(user: User) {
    setDraft(toEditDraft(user));
    setErrors({});
    setEditor({ mode: "edit", userId: user.id });
  }

  function closeEditor() {
    setEditor(null);
    setErrors({});
    setDraft(EMPTY_DRAFT);
  }

  function updateDraft(key: keyof UserAccountDraft, value: string) {
    setDraft((currentDraft) => ({ ...currentDraft, [key]: value }));
    setErrors((currentErrors) => ({ ...currentErrors, [key]: undefined }));
  }

  function validateDraft(): boolean {
    const nextErrors: UserAccountErrors = {};
    if (!editor) {
      return false;
    }

    if (editor.mode === "create") {
      if (!draft.fullName.trim()) nextErrors.fullName = "Họ tên là bắt buộc.";
      if (!draft.employeeCode.trim()) nextErrors.employeeCode = "Mã nhân viên là bắt buộc.";
      if (draft.username.trim().length < 3) nextErrors.username = "Tên đăng nhập cần có ít nhất 3 ký tự.";
      if (draft.password.length < 6) nextErrors.password = "Mật khẩu cần có ít nhất 6 ký tự.";
      if (!draft.orgUnitId) nextErrors.orgUnitId = "Hãy chọn đơn vị tổ chức.";
    }

    if (!draft.roleCode) nextErrors.roleCode = "Hãy chọn role.";
    if (draft.dataScope === "ORGANIZATION_BRANCH" && !draft.scopeOrgUnitId) {
      nextErrors.scopeOrgUnitId = "Hãy chọn đơn vị tổ chức áp dụng.";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  function saveUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editor || !validateDraft()) {
      return;
    }

    const selectedRole = DEMO_ROLES.find((role) => role.code === draft.roleCode);
    if (!selectedRole) {
      setErrors({ roleCode: "Hãy chọn role hợp lệ." });
      return;
    }

    if (editor.mode === "create") {
      const selectedOrgUnit = orgUnits.find((orgUnit) => orgUnit.id === Number(draft.orgUnitId));
      if (!selectedOrgUnit) {
        setErrors({ orgUnitId: "Hãy chọn đơn vị tổ chức hợp lệ." });
        return;
      }

      const newUser: User = {
        dataScope: selectedRole.code === "VT-06" ? "COMPANY" : "SELF",
        employeeId: null,
        fullName: draft.fullName.trim(),
        id: Date.now(),
        orgUnitId: selectedOrgUnit.id,
        orgUnitName: selectedOrgUnit.unitName,
        roleCode: selectedRole.code,
        roleName: selectedRole.name,
        scopeOrgUnitId: null,
        status: "ACTIVE",
        username: draft.username.trim(),
      };
      setUsers((currentUsers) => [newUser, ...currentUsers]);
      setAnnouncement(`Đã tạo tài khoản ${newUser.fullName}.`);
      closeEditor();
      return;
    }

    if (!editingUser) {
      return;
    }

    const scopeOrgUnitId = draft.dataScope === "ORGANIZATION_BRANCH" ? Number(draft.scopeOrgUnitId) : null;
    setUsers((currentUsers) => currentUsers.map((user) => (
      user.id === editingUser.id
        ? {
          ...user,
          dataScope: draft.dataScope,
          roleCode: selectedRole.code,
          roleName: selectedRole.name,
          scopeOrgUnitId,
        }
        : user
    )));
    setAnnouncement(`Đã cập nhật tài khoản ${editingUser.fullName}.`);
    closeEditor();
  }

  function requestLock(user: User) {
    setLockTarget(user);
  }

  function confirmLock() {
    if (!lockTarget) {
      return;
    }

    setUsers((currentUsers) => currentUsers.map((user) => (
      user.id === lockTarget.id ? { ...user, status: "LOCKED" } : user
    )));
    setAnnouncement(`Đã khóa tài khoản ${lockTarget.fullName}.`);
    setLockTarget(null);
  }

  function unlockUser(user: User) {
    setUsers((currentUsers) => currentUsers.map((item) => (
      item.id === user.id ? { ...item, status: "ACTIVE" } : item
    )));
    setAnnouncement(`Đã mở khóa tài khoản ${user.fullName}.`);
  }

  const editorMode = editor?.mode;
  const editorTitle = editorMode === "edit" ? "Sửa tài khoản" : "Tạo tài khoản";

  return (
    <div className="workspace-stack">
      <PageHeader
        actions={
          <button className="button button--primary" onClick={openCreateDialog} type="button">
            <Icon name="plus" />
            Tạo tài khoản
          </button>
        }
        description="Tìm kiếm, lọc và quản lý trạng thái tài khoản."
        title="Quản lý tài khoản"
      />

      {announcement ? <p aria-live="polite" className="sr-only">{announcement}</p> : null}

      <section aria-labelledby="users-table-title" className="data-panel">
        <div className="data-panel__header">
          <div>
            <h2 id="users-table-title">Danh sách tài khoản</h2>
            <p>{filteredUsers.length} tài khoản</p>
          </div>
        </div>

        <div className="data-panel__body">
          <div className="filter-toolbar">
            <div className="search-field">
              <Icon name="search" />
              <label className="sr-only" htmlFor="user-search">Tìm tài khoản</label>
              <input
                className="input"
                id="user-search"
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Tìm theo tên, username hoặc đơn vị"
                type="search"
                value={query}
              />
            </div>

            <div className="select-field">
              <label className="sr-only" htmlFor="role-filter">Lọc theo role</label>
              <select className="select" id="role-filter" onChange={(event) => setRoleFilter(event.target.value)} value={roleFilter}>
                <option value="ALL">Tất cả role</option>
                {DEMO_ROLES.map((role) => <option key={role.code} value={role.code}>{role.code} · {role.name}</option>)}
              </select>
            </div>

            <div className="select-field">
              <label className="sr-only" htmlFor="status-filter">Lọc theo trạng thái</label>
              <select className="select" id="status-filter" onChange={(event) => setStatusFilter(event.target.value)} value={statusFilter}>
                <option value="ALL">Tất cả trạng thái</option>
                <option value="ACTIVE">Hoạt động</option>
                <option value="LOCKED">Đã khóa</option>
              </select>
            </div>
          </div>

          {filteredUsers.length === 0 ? (
            <EmptyState
              action={<button className="button button--secondary" onClick={() => { setQuery(""); setRoleFilter("ALL"); setStatusFilter("ALL"); }} type="button">Xóa bộ lọc</button>}
              icon="search"
              message="Thử điều chỉnh điều kiện tìm kiếm."
              title="Không tìm thấy tài khoản"
            />
          ) : (
            <>
              <div className="data-table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th scope="col">Tài khoản</th>
                      <th scope="col">Role</th>
                      <th scope="col">Đơn vị</th>
                      <th scope="col">Data Scope</th>
                      <th scope="col">Trạng thái</th>
                      <th scope="col"><span className="sr-only">Thao tác</span></th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredUsers.map((user) => (
                      <UserTableRow key={user.id} onEdit={openEditDialog} onRequestLock={requestLock} onUnlock={unlockUser} user={user} />
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="mobile-record-list">
                {filteredUsers.map((user) => (
                  <UserRecordCard key={user.id} onEdit={openEditDialog} onRequestLock={requestLock} onUnlock={unlockUser} user={user} />
                ))}
              </div>
            </>
          )}
        </div>
      </section>

      <Dialog
        description={editorMode === "edit" ? "Cập nhật vai trò và phạm vi truy cập của tài khoản." : "Nhập thông tin để tạo tài khoản."}
        footer={
          <>
            <button className="button button--quiet" onClick={closeEditor} type="button">Hủy</button>
            <button className="button button--primary" form="user-account-form" ref={submitRef} type="submit">
              {editorMode === "edit" ? "Lưu thay đổi" : "Tạo tài khoản"}
            </button>
          </>
        }
        initialFocusRef={editorFocusRef}
        onClose={closeEditor}
        open={Boolean(editor)}
        title={editorTitle}
      >
        {editorMode ? (
          <UserAccountForm
            errors={errors}
            formId="user-account-form"
            identity={editingUser}
            initialFocusRef={setEditorFocus}
            mode={editorMode}
            onChange={updateDraft}
            onSubmit={saveUser}
            orgUnitOptions={orgUnitOptions}
            submitRef={submitRef}
            value={draft}
          />
        ) : null}
      </Dialog>

      <Dialog
        description="Xác nhận trước khi khóa quyền truy cập của tài khoản này."
        footer={
          <>
            <button className="button button--quiet" onClick={() => setLockTarget(null)} ref={lockCancelRef} type="button">Hủy</button>
            <button className="button button--danger" onClick={confirmLock} type="button">Khóa tài khoản</button>
          </>
        }
        initialFocusRef={lockCancelRef}
        onClose={() => setLockTarget(null)}
        open={Boolean(lockTarget)}
        title="Khóa tài khoản"
      >
        {lockTarget ? (
          <div className="dialog-confirmation">
            <div className="account-summary">
              <strong>{lockTarget.fullName}</strong>
              <span>{lockTarget.username}</span>
            </div>
            <p>Tài khoản sẽ không thể đăng nhập cho đến khi được mở lại.</p>
          </div>
        ) : null}
      </Dialog>
    </div>
  );
}

interface UserActionProps {
  onEdit: (user: User) => void;
  onRequestLock: (user: User) => void;
  onUnlock: (user: User) => void;
  user: User;
}

function UserActions({ onEdit, onRequestLock, onUnlock, user }: UserActionProps) {
  const isActive = user.status === "ACTIVE";
  return (
    <div className="table-actions">
      <button className="table-action" onClick={() => onEdit(user)} type="button">
        <Icon name="settings" />
        Chỉnh sửa
      </button>
      <button className={isActive ? "table-action table-action--danger" : "table-action"} onClick={() => (isActive ? onRequestLock(user) : onUnlock(user))} type="button">
        <Icon name={isActive ? "lock" : "unlock"} />
        {isActive ? "Khóa" : "Mở khóa"}
      </button>
    </div>
  );
}

function UserTableRow({ onEdit, onRequestLock, onUnlock, user }: UserActionProps) {
  return (
    <tr>
      <td>
        <div className="table-person">
          <span aria-hidden="true" className="avatar avatar--small">{user.fullName.slice(0, 1)}</span>
          <div className="table-person__copy">
            <strong>{user.fullName}</strong>
            <span>{user.username}</span>
          </div>
        </div>
      </td>
      <td><RoleBadge code={user.roleCode} name={user.roleName} /></td>
      <td>{user.orgUnitName ?? "Chưa gán đơn vị"}</td>
      <td><ScopeBadge scope={user.dataScope} /></td>
      <td><StatusBadge status={user.status} /></td>
      <td><UserActions onEdit={onEdit} onRequestLock={onRequestLock} onUnlock={onUnlock} user={user} /></td>
    </tr>
  );
}

function UserRecordCard({ onEdit, onRequestLock, onUnlock, user }: UserActionProps) {
  return (
    <article className="record-card">
      <div className="record-card__header">
        <div className="table-person">
          <span aria-hidden="true" className="avatar avatar--small">{user.fullName.slice(0, 1)}</span>
          <div className="table-person__copy">
            <strong>{user.fullName}</strong>
            <span>{user.username}</span>
          </div>
        </div>
        <StatusBadge status={user.status} />
      </div>
      <dl className="record-card__facts">
        <div><dt>Role</dt><dd><RoleBadge code={user.roleCode} name={user.roleName} /></dd></div>
        <div><dt>Data Scope</dt><dd><ScopeBadge scope={user.dataScope} /></dd></div>
        <div><dt>Đơn vị</dt><dd>{user.orgUnitName ?? "Chưa gán đơn vị"}</dd></div>
      </dl>
      <div className="record-card__footer">
        <UserActions onEdit={onEdit} onRequestLock={onRequestLock} onUnlock={onUnlock} user={user} />
      </div>
    </article>
  );
}
