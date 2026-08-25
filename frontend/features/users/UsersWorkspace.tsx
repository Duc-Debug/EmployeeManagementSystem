"use client";

import { useMemo, useState } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { RoleBadge, ScopeBadge, StatusBadge } from "@/components/ui/Badge";
import { Dialog } from "@/components/ui/Dialog";
import { EmptyState } from "@/components/ui/EmptyState";
import { Icon } from "@/components/ui/Icon";
import { flattenOrgTree } from "@/lib/organization";
import { DEMO_ORG_UNIT_TREE, DEMO_ROLES, DEMO_USERS } from "@/src/mocks/hrm";
import type { User } from "@/src/types/hrm";

interface CreateUserDraft {
  employeeCode: string;
  fullName: string;
  orgUnitId: string;
  password: string;
  roleCode: string;
  username: string;
}

type DraftErrors = Partial<Record<keyof CreateUserDraft, string>>;

const EMPTY_DRAFT: CreateUserDraft = {
  employeeCode: "",
  fullName: "",
  orgUnitId: "",
  password: "",
  roleCode: "",
  username: "",
};

export function UsersWorkspace() {
  const [users, setUsers] = useState<User[]>(() => DEMO_USERS.map((user) => ({ ...user })));
  const [query, setQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [draft, setDraft] = useState<CreateUserDraft>(EMPTY_DRAFT);
  const [errors, setErrors] = useState<DraftErrors>({});
  const [announcement, setAnnouncement] = useState("");
  const orgUnits = useMemo(() => flattenOrgTree(DEMO_ORG_UNIT_TREE), []);

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

  function updateDraft<Key extends keyof CreateUserDraft>(key: Key, value: CreateUserDraft[Key]) {
    setDraft((currentDraft) => ({ ...currentDraft, [key]: value }));
    setErrors((currentErrors) => ({ ...currentErrors, [key]: undefined }));
  }

  function validateDraft() {
    const nextErrors: DraftErrors = {};
    if (!draft.fullName.trim()) nextErrors.fullName = "Họ tên là bắt buộc.";
    if (draft.username.trim().length < 3) nextErrors.username = "Username cần có ít nhất 3 ký tự.";
    if (draft.password.length < 6) nextErrors.password = "Mật khẩu cần có ít nhất 6 ký tự.";
    if (!draft.employeeCode.trim()) nextErrors.employeeCode = "Mã nhân viên là bắt buộc.";
    if (!draft.roleCode) nextErrors.roleCode = "Hãy chọn role.";
    if (!draft.orgUnitId) nextErrors.orgUnitId = "Hãy chọn đơn vị tổ chức.";
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  function createUser(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!validateDraft()) {
      return;
    }

    const selectedRole = DEMO_ROLES.find((role) => role.code === draft.roleCode);
    const selectedOrgUnit = orgUnits.find((orgUnit) => orgUnit.id === Number(draft.orgUnitId));
    if (!selectedRole || !selectedOrgUnit) {
      return;
    }

    const newUser: User = {
      dataScope: "SELF",
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
    setDraft(EMPTY_DRAFT);
    setIsCreateOpen(false);
    setAnnouncement(`Đã thêm ${newUser.fullName} vào danh sách minh họa.`);
  }

  function toggleUserStatus(userId: number) {
    const user = users.find((item) => item.id === userId);
    if (!user) {
      return;
    }

    const changedUser = { ...user, status: user.status === "ACTIVE" ? "LOCKED" : "ACTIVE" } as User;
    setUsers((currentUsers) => currentUsers.map((item) => (item.id === userId ? changedUser : item)));
    setAnnouncement(
      changedUser.status === "LOCKED"
        ? `Đã khóa ${changedUser.fullName} trong giao diện minh họa.`
        : `Đã mở khóa ${changedUser.fullName} trong giao diện minh họa.`,
    );
  }

  function closeDialog() {
    setIsCreateOpen(false);
    setErrors({});
  }

  return (
    <div className="workspace-stack">
      <PageHeader
        actions={
          <button className="button button--primary" onClick={() => setIsCreateOpen(true)} type="button">
            <Icon name="plus" />
            Tạo tài khoản
          </button>
        }
        description="Tìm, lọc và cập nhật trạng thái tài khoản. Dữ liệu hiện tại là fixture UI, chưa gọi API."
        title="Quản lý tài khoản"
      />

      {announcement ? <p aria-live="polite" className="sr-only">{announcement}</p> : null}

      <section aria-labelledby="users-table-title" className="data-panel">
        <div className="data-panel__header">
          <div>
            <h2 id="users-table-title">Danh sách tài khoản</h2>
            <p>{filteredUsers.length} kết quả trong dữ liệu minh họa</p>
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
              message="Thử xóa hoặc điều chỉnh điều kiện tìm kiếm để xem dữ liệu minh họa."
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
                    {filteredUsers.map((user) => <UserTableRow key={user.id} onToggleStatus={toggleUserStatus} user={user} />)}
                  </tbody>
                </table>
              </div>

              <div className="mobile-record-list">
                {filteredUsers.map((user) => <UserRecordCard key={user.id} onToggleStatus={toggleUserStatus} user={user} />)}
              </div>
            </>
          )}
        </div>
      </section>

      <Dialog
        description="Biểu mẫu này phản ánh trường tạo tài khoản hiện có của backend. Lưu chỉ cập nhật dữ liệu minh họa tại chỗ."
        onClose={closeDialog}
        open={isCreateOpen}
        title="Tạo tài khoản"
      >
        <form className="form" noValidate onSubmit={createUser}>
          <div className="form-grid form-grid--two">
            <FormField error={errors.fullName} id="full-name" label="Họ và tên">
              <input aria-describedby="full-name-message" aria-invalid={Boolean(errors.fullName)} className="input" id="full-name" onChange={(event) => updateDraft("fullName", event.target.value)} required value={draft.fullName} />
            </FormField>
            <FormField error={errors.employeeCode} id="employee-code" label="Mã nhân viên">
              <input aria-describedby="employee-code-message" aria-invalid={Boolean(errors.employeeCode)} className="input" id="employee-code" onChange={(event) => updateDraft("employeeCode", event.target.value)} placeholder="vd. EMP-001" required value={draft.employeeCode} />
            </FormField>
          </div>

          <div className="form-grid form-grid--two">
            <FormField error={errors.username} id="new-username" label="Tên đăng nhập">
              <input aria-describedby="new-username-message" aria-invalid={Boolean(errors.username)} autoComplete="username" className="input" id="new-username" onChange={(event) => updateDraft("username", event.target.value)} required value={draft.username} />
            </FormField>
            <FormField error={errors.password} id="new-password" label="Mật khẩu">
              <input aria-describedby="new-password-message" aria-invalid={Boolean(errors.password)} autoComplete="new-password" className="input" id="new-password" onChange={(event) => updateDraft("password", event.target.value)} required type="password" value={draft.password} />
            </FormField>
          </div>

          <div className="form-grid form-grid--two">
            <FormField error={errors.roleCode} id="new-role" label="Role">
              <select aria-describedby="new-role-message" aria-invalid={Boolean(errors.roleCode)} className="select" id="new-role" onChange={(event) => updateDraft("roleCode", event.target.value)} required value={draft.roleCode}>
                <option value="">Chọn role</option>
                {DEMO_ROLES.map((role) => <option key={role.code} value={role.code}>{role.code} · {role.name}</option>)}
              </select>
            </FormField>
            <FormField error={errors.orgUnitId} id="new-org-unit" label="Đơn vị tổ chức">
              <select aria-describedby="new-org-unit-message" aria-invalid={Boolean(errors.orgUnitId)} className="select" id="new-org-unit" onChange={(event) => updateDraft("orgUnitId", event.target.value)} required value={draft.orgUnitId}>
                <option value="">Chọn đơn vị</option>
                {orgUnits.map((orgUnit) => <option key={orgUnit.id} value={orgUnit.id}>{orgUnit.unitCode} · {orgUnit.unitName}</option>)}
              </select>
            </FormField>
          </div>

          <div className="form-actions">
            <button className="button button--quiet" onClick={closeDialog} type="button">Hủy</button>
            <button className="button button--primary" type="submit">Tạo tài khoản</button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}

function FormField({ children, error, id, label }: Readonly<{ children: React.ReactNode; error?: string; id: string; label: string }>) {
  const hintId = `${id}-message`;
  return (
    <div className="field-group">
      <label htmlFor={id}>{label}</label>
      {children}
      <p className={error ? "field-error" : "field-hint"} id={hintId}>{error ?? " "}</p>
    </div>
  );
}

function UserTableRow({ onToggleStatus, user }: Readonly<{ onToggleStatus: (id: number) => void; user: User }>) {
  const buttonLabel = user.status === "ACTIVE" ? "Khóa" : "Mở khóa";
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
      <td>
        <div className="table-actions">
          <button className={user.status === "ACTIVE" ? "table-action table-action--danger" : "table-action"} onClick={() => onToggleStatus(user.id)} type="button">
            <Icon name={user.status === "ACTIVE" ? "lock" : "unlock"} />
            {buttonLabel}
          </button>
        </div>
      </td>
    </tr>
  );
}

function UserRecordCard({ onToggleStatus, user }: Readonly<{ onToggleStatus: (id: number) => void; user: User }>) {
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
        <div><dt>Data scope</dt><dd><ScopeBadge scope={user.dataScope} /></dd></div>
        <div><dt>Đơn vị</dt><dd>{user.orgUnitName ?? "Chưa gán đơn vị"}</dd></div>
      </dl>
      <div className="record-card__footer">
        <span className="field-hint">Dữ liệu minh họa</span>
        <button className={user.status === "ACTIVE" ? "table-action table-action--danger" : "table-action"} onClick={() => onToggleStatus(user.id)} type="button">
          <Icon name={user.status === "ACTIVE" ? "lock" : "unlock"} />
          {user.status === "ACTIVE" ? "Khóa" : "Mở khóa"}
        </button>
      </div>
    </article>
  );
}
