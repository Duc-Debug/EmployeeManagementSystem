"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Dialog } from "@/components/ui/Dialog";
import { EmptyState } from "@/components/ui/EmptyState";
import { Icon } from "@/components/ui/Icon";
import { SkillStatusBadge, type SkillApprovalStatus } from "@/components/ui/SkillStatusBadge";
import { ApiError } from "@/lib/api-client";
import { declareSkill, getMySkills, getSkillCatalog, type EmployeeSkill, type Skill } from "@/lib/api/skills";

import {
  SkillDeclarationForm,
  type SkillDeclarationDraft,
  type SkillDeclarationErrors,
} from "@/features/skills/SkillDeclarationForm";

const EMPTY_DRAFT: SkillDeclarationDraft = {
  proficiencyLevel: 3,
  skillId: "",
  yearsOfExperience: "1.0",
};

export function SkillsWorkspace() {
  const [skills, setSkills] = useState<EmployeeSkill[]>([]);
  const [catalog, setCatalog] = useState<Skill[]>([]);
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [duplicateSkillName, setDuplicateSkillName] = useState<string | null>(null);
  const [draft, setDraft] = useState<SkillDeclarationDraft>(EMPTY_DRAFT);
  const [errors, setErrors] = useState<SkillDeclarationErrors>({});
  const [announcement, setAnnouncement] = useState("");
  const [fetchError, setFetchError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [reloadTick, setReloadTick] = useState(0);

  const editorFocusRef = useRef<HTMLElement>(null);
  const submitRef = useRef<HTMLButtonElement>(null);
  const duplicateCancelRef = useRef<HTMLButtonElement>(null);

  const setEditorFocus = useCallback((element: HTMLElement | null) => {
    editorFocusRef.current = element;
  }, []);

  // Fetch catalog & my skills
  useEffect(() => {
    let ignore = false;

    Promise.all([
      getMySkills(),
      getSkillCatalog(),
    ])
      .then(([mySkillsData, catalogData]) => {
        if (!ignore) {
          setSkills(mySkillsData || []);
          setCatalog(catalogData || []);
          setFetchError(null);
        }
      })
      .catch((err) => {
        if (!ignore) {
          console.error("Lỗi nạp dữ liệu kỹ năng:", err);
          setFetchError(err instanceof Error ? err.message : "Không thể tải dữ liệu kỹ năng.");
        }
      })
      .finally(() => {
        if (!ignore) setIsLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [reloadTick]);

  // Stats calculation
  const stats = useMemo(() => {
    const total = skills.length;
    const pending = skills.filter((s) => s.status === "PENDING").length;
    const approved = skills.filter((s) => s.status === "APPROVED").length;
    const expert = skills.filter((s) => s.proficiencyLevel >= 4).length;
    return { approved, expert, pending, total };
  }, [skills]);

  // Filtered skills
  const filteredSkills = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase("vi");
    return skills.filter((item) => {
      const nameMatch = !normalizedQuery || [item.skillName || "", item.skillCode || "", item.skillCategory || ""]
        .some((val) => val.toLocaleLowerCase("vi").includes(normalizedQuery));
      let statusMatch = statusFilter === "ALL" || item.status === statusFilter;
      if (statusFilter === "EXPERT") {
        statusMatch = item.proficiencyLevel >= 4;
      }
      return nameMatch && statusMatch;
    });
  }, [query, skills, statusFilter]);

  function openCreateDialog() {
    setDraft(EMPTY_DRAFT);
    setErrors({});
    setIsDialogOpen(true);
  }

  function closeDialog() {
    setIsDialogOpen(false);
    setErrors({});
    setDraft(EMPTY_DRAFT);
  }

  function updateDraft<Key extends keyof SkillDeclarationDraft>(key: Key, value: SkillDeclarationDraft[Key]) {
    setDraft((prev) => ({ ...prev, [key]: value }));
    setErrors((prev) => ({ ...prev, [key]: undefined }));
  }

  function validateDraft(): boolean {
    const nextErrors: SkillDeclarationErrors = {};

    if (!draft.skillId) {
      nextErrors.skillId = "Vui lòng chọn kỹ năng chuyên môn.";
    }
    if (!draft.proficiencyLevel || draft.proficiencyLevel < 1 || draft.proficiencyLevel > 5) {
      nextErrors.proficiencyLevel = "Vui lòng chọn mức độ thành thạo từ 1 đến 5.";
    }
    const expNum = parseFloat(draft.yearsOfExperience);
    if (isNaN(expNum) || expNum < 0) {
      nextErrors.yearsOfExperience = "Số năm kinh nghiệm không hợp lệ (phải ≥ 0).";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  async function handleSaveSkill(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!validateDraft()) {
      return;
    }

    const selectedSkillId = Number(draft.skillId);
    const expYears = parseFloat(draft.yearsOfExperience);

    try {
      const created = await declareSkill({
        proficiencyLevel: draft.proficiencyLevel,
        skillId: selectedSkillId,
        yearsOfExperience: expYears,
      });

      setSkills((prev) => [created, ...prev]);
      setAnnouncement(`Khai báo kỹ năng thành công. Hồ sơ đang ở trạng thái chờ duyệt.`);
      closeDialog();
    } catch (err) {
      if (err instanceof ApiError) {
        // TC-02: Handle Duplicate Skill Warning
        if (err.status === 400 || err.status === 409) {
          const matchedSkill = catalog.find((s) => s.id === selectedSkillId);
          setDuplicateSkillName(matchedSkill?.name || "Kỹ năng này");
        } else {
          setErrors({ skillId: err.message });
        }
      }
    }
  }

  return (
    <div className="workspace-stack">
      <PageHeader
        actions={
          <button className="button button--primary" onClick={openCreateDialog} type="button">
            <Icon name="plus" />
            <span>Khai báo kỹ năng mới</span>
          </button>
        }
        description="Khai báo năng lực chuyên môn, trình độ thành thạo và số năm kinh nghiệm để được phân bổ vào các dự án phù hợp."
        title="Khai báo hồ sơ kỹ năng nhân sự"
      />

      {fetchError && (
        <div className="notice notice--error" style={{ marginBottom: "1rem" }}>
          <Icon name="alert" />
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", width: "100%" }}>
            <span>{fetchError}</span>
            <button className="button button--secondary" onClick={() => setReloadTick((t) => t + 1)} style={{ padding: "0.25rem 0.75rem", fontSize: "0.875rem" }} type="button">
              Thử lại
            </button>
          </div>
        </div>
      )}

      {announcement ? <p aria-live="polite" className="sr-only">{announcement}</p> : null}

      {/* KPI Stats Cards */}
      <section aria-label="Thống kê kỹ năng cá nhân" className="kpi-grid">
        <button
          className={`kpi-card ${statusFilter === "ALL" ? "is-active" : ""}`}
          onClick={() => setStatusFilter("ALL")}
          type="button"
        >
          <div className="kpi-card__header">
            <span className="kpi-card__label">Tổng kỹ năng</span>
            <span className="kpi-card__icon kpi-card__icon--indigo">
              <Icon name="shield" />
            </span>
          </div>
          <div className="kpi-card__val">{stats.total}</div>
          <div className="kpi-card__desc">Đã thêm vào hồ sơ</div>
        </button>

        <button
          className={`kpi-card ${statusFilter === "PENDING" ? "is-active" : ""}`}
          onClick={() => setStatusFilter((prev) => (prev === "PENDING" ? "ALL" : "PENDING"))}
          type="button"
        >
          <div className="kpi-card__header">
            <span className="kpi-card__label">Chờ xác nhận</span>
            <span className="kpi-card__icon kpi-card__icon--amber">
              <Icon name="time" />
            </span>
          </div>
          <div className="kpi-card__val kpi-card__val--amber">{stats.pending}</div>
          <div className="kpi-card__desc">Đang chờ phê duyệt</div>
        </button>

        <button
          className={`kpi-card ${statusFilter === "APPROVED" ? "is-active" : ""}`}
          onClick={() => setStatusFilter((prev) => (prev === "APPROVED" ? "ALL" : "APPROVED"))}
          type="button"
        >
          <div className="kpi-card__header">
            <span className="kpi-card__label">Đã xác nhận</span>
            <span className="kpi-card__icon kpi-card__icon--emerald">
              <Icon name="check" />
            </span>
          </div>
          <div className="kpi-card__val kpi-card__val--emerald">{stats.approved}</div>
          <div className="kpi-card__desc">Hồ sơ đã phê duyệt</div>
        </button>

        <button
          className={`kpi-card ${statusFilter === "EXPERT" ? "is-active" : ""}`}
          onClick={() => setStatusFilter((prev) => (prev === "EXPERT" ? "ALL" : "EXPERT"))}
          type="button"
        >
          <div className="kpi-card__header">
            <span className="kpi-card__label">Kỹ năng Chuyên sâu</span>
            <span className="kpi-card__icon kpi-card__icon--purple">
              <Icon name="star" />
            </span>
          </div>
          <div className="kpi-card__val kpi-card__val--purple">{stats.expert}</div>
          <div className="kpi-card__desc">Mức thành thạo ≥ 4 sao</div>
        </button>
      </section>

      {/* Main Data Panel */}
      <section aria-labelledby="skills-table-title" className="data-panel">
        <div className="data-panel__header">
          <div>
            <h2 id="skills-table-title">Danh sách kỹ năng đã khai báo</h2>
            <p>Hiển thị {filteredSkills.length} trên tổng số {skills.length} kỹ năng trong hồ sơ</p>
          </div>
        </div>

        <div className="data-panel__body">
          <div className="filter-toolbar">
            <div className="select-field">
              <label className="sr-only" htmlFor="status-filter">Lọc theo trạng thái</label>
              <select className="select" id="status-filter" onChange={(e) => setStatusFilter(e.target.value)} value={statusFilter}>
                <option value="ALL">Tất cả trạng thái</option>
                <option value="PENDING">Chờ xác nhận</option>
                <option value="APPROVED">Đã xác nhận</option>
                <option value="REJECTED">Bị từ chối</option>
                <option value="EXPERT">Chuyên sâu (≥ 4 sao)</option>
              </select>
            </div>

            {(query || statusFilter !== "ALL") && (
              <button
                className="button button--secondary button--compact filter-reset-btn"
                onClick={() => {
                  setQuery("");
                  setStatusFilter("ALL");
                }}
                type="button"
              >
                <Icon name="close" />
                <span>Đặt lại lọc</span>
              </button>
            )}

            <div className="search-field">
              <Icon name="search" />
              <label className="sr-only" htmlFor="skill-search">Tìm kỹ năng</label>
              <input
                className="input"
                id="skill-search"
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Tìm theo tên kỹ năng, mã, phân loại..."
                type="search"
                value={query}
              />
              {query && (
                <button aria-label="Xóa tìm kiếm" className="search-clear-btn" onClick={() => setQuery("")} type="button">
                  <Icon name="close" />
                </button>
              )}
            </div>
          </div>

          {isLoading ? (
            <div style={{ padding: "3rem", textAlign: "center", color: "var(--color-text-secondary)" }}>
              <Icon name="spinner" />
              <p style={{ marginTop: "0.5rem" }}>Đang nạp danh sách kỹ năng...</p>
            </div>
          ) : filteredSkills.length === 0 ? (
            <EmptyState
              action={
                <button className="button button--primary" onClick={openCreateDialog} type="button">
                  ➕ Khai báo kỹ năng ngay
                </button>
              }
              icon="shield"
              message="Bạn chưa khai báo kỹ năng nào trong hồ sơ. Hãy bấm 'Khai báo kỹ năng mới' để bắt đầu."
              title="Chưa có kỹ năng nào"
            />
          ) : (
            <div className="data-table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th scope="col">Kỹ năng chuyên môn</th>
                    <th scope="col">Phân loại</th>
                    <th scope="col">Mức độ thành thạo</th>
                    <th scope="col">Kinh nghiệm</th>
                    <th scope="col">Trạng thái phê duyệt</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredSkills.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <div style={{ display: "flex", flexDirection: "column" }}>
                          <strong style={{ color: "var(--color-text-primary)" }}>
                            {item.skillName || `Skill #${item.skillId}`}
                          </strong>
                          {item.skillCode && (
                            <span style={{ fontSize: "0.75rem", color: "var(--color-text-secondary)" }}>
                              {item.skillCode}
                            </span>
                          )}
                        </div>
                      </td>
                      <td>
                        <span className="role-badge" style={{ backgroundColor: "var(--color-bg-subtle, #f3f4f6)" }}>
                          <span className="role-badge__name">{item.skillCategory || "Chuyên môn"}</span>
                        </span>
                      </td>
                      <td>
                        <div style={{ display: "flex", alignItems: "center", gap: "0.25rem" }}>
                          <span style={{ color: "#f59e0b", letterSpacing: "1px" }}>
                            {"★".repeat(item.proficiencyLevel)}
                            {"☆".repeat(5 - item.proficiencyLevel)}
                          </span>
                          <span style={{ fontSize: "0.8125rem", color: "var(--color-text-secondary)", fontWeight: 500, marginLeft: "0.25rem" }}>
                            ({item.proficiencyLevel}/5)
                          </span>
                        </div>
                      </td>
                      <td>
                        <span style={{ fontWeight: 500 }}>{item.yearsOfExperience} năm</span>
                      </td>
                      <td>
                        <SkillStatusBadge status={item.status as SkillApprovalStatus} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </section>

      {/* Modal Dialog: Form Khai báo Kỹ năng mới (TC-01) */}
      <Dialog
        className="dialog--user-form"
        description="Điền thông tin trình độ thành thạo và kinh nghiệm làm việc thực tế đối với kỹ năng này."
        footer={
          <>
            <button className="button button--secondary" onClick={closeDialog} type="button">
              Hủy
            </button>
            <button className="button button--primary" form="skill-declaration-form" ref={submitRef} type="submit">
              Lưu khai báo
            </button>
          </>
        }
        initialFocusRef={editorFocusRef}
        onClose={closeDialog}
        open={isDialogOpen}
        preventBackdropClose={true}
        title="Khai báo kỹ năng cá nhân"
      >
        <SkillDeclarationForm
          errors={errors}
          formId="skill-declaration-form"
          initialFocusRef={setEditorFocus}
          onChange={updateDraft}
          onSubmit={handleSaveSkill}
          skillCatalog={catalog}
          submitRef={submitRef}
          value={draft}
        />
      </Dialog>

      {/* Modal Alert Dialog: Duplicate Skill Warning (TC-02) */}
      <Dialog
        className="dialog--compact"
        description="Phát hiện trùng lặp kỹ năng đã có sẵn trong hồ sơ cá nhân."
        footer={
          <>
            <button
              className="button button--secondary"
              onClick={() => setDuplicateSkillName(null)}
              ref={duplicateCancelRef}
              type="button"
            >
              Đóng
            </button>
          </>
        }
        initialFocusRef={duplicateCancelRef}
        onClose={() => setDuplicateSkillName(null)}
        open={Boolean(duplicateSkillName)}
        preventBackdropClose={true}
        title="Kỹ năng đã tồn tại"
      >
        <div className="dialog-confirmation">
          <div className="notice notice--warning" style={{ marginBottom: "1rem" }}>
            <Icon name="alert" />
            <span>Kỹ năng <strong>{duplicateSkillName}</strong> đã có trong hồ sơ của bạn.</span>
          </div>
          <p className="dialog-confirmation__text">
            Hệ thống không cho phép khai báo trùng kỹ năng. Vui lòng chọn kỹ năng khác hoặc chuyển sang chức năng cập nhật trình độ thành thạo.
          </p>
        </div>
      </Dialog>
    </div>
  );
}
