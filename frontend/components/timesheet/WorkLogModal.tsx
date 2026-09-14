"use client";

import { useEffect, useMemo, useState } from "react";
import { Dialog } from "@/components/ui/Dialog";
import { FormField } from "@/components/ui/FormField";
import {
  type AssignedTaskOption,
  type WorkLog,
  createWorkLog,
  updateWorkLog,
} from "@/lib/api/timesheetApi";
import { ApiError } from "@/lib/api-client";

interface WorkLogModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
  initialData?: WorkLog | null;
  assignedTasks: AssignedTaskOption[];
  defaultDate?: string;
}

export function WorkLogModal({
  open,
  onClose,
  onSuccess,
  initialData,
  assignedTasks,
  defaultDate,
}: WorkLogModalProps) {
  const isEditing = !!initialData;

  const [selectedProjectId, setSelectedProjectId] = useState<number | "">("");
  const [selectedTaskId, setSelectedTaskId] = useState<number | "">("");
  const [workDate, setWorkDate] = useState<string>(
    defaultDate || new Date().toISOString().split("T")[0]
  );
  const [hours, setHours] = useState<string>("4.0");
  const [isBillable, setIsBillable] = useState<boolean>(true);
  const [description, setDescription] = useState<string>("");

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string>("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Group unique projects from assigned tasks
  const projectOptions = useMemo(() => {
    const map = new Map<number, { id: number; code: string; name: string }>();
    assignedTasks.forEach((t) => {
      if (!map.has(t.projectId)) {
        map.set(t.projectId, {
          id: t.projectId,
          code: t.projectCode,
          name: t.projectName,
        });
      }
    });
    return Array.from(map.values());
  }, [assignedTasks]);

  // Tasks filtered by selected project
  const availableTasks = useMemo(() => {
    if (!selectedProjectId) return [];
    return assignedTasks.filter((t) => t.projectId === Number(selectedProjectId));
  }, [assignedTasks, selectedProjectId]);

  useEffect(() => {
    if (open) {
      setErrors({});
      setServerError("");
      if (initialData) {
        setSelectedProjectId(initialData.projectId);
        setSelectedTaskId(initialData.taskId);
        setWorkDate(initialData.workDate);
        setHours(String(initialData.hours));
        setIsBillable(initialData.isBillable);
        setDescription(initialData.description);
      } else {
        const todayStr = defaultDate || new Date().toISOString().split("T")[0];
        setWorkDate(todayStr);
        setHours("4.0");
        setIsBillable(true);
        setDescription("");
        if (projectOptions.length === 1) {
          setSelectedProjectId(projectOptions[0].id);
        } else {
          setSelectedProjectId("");
        }
        setSelectedTaskId("");
      }
    }
  }, [open, initialData, defaultDate, projectOptions]);

  const handleProjectChange = (projId: number | "") => {
    setSelectedProjectId(projId);
    setSelectedTaskId("");
  };

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!selectedProjectId) {
      newErrors.projectId = "Vui lòng chọn dự án.";
    }
    if (!selectedTaskId) {
      newErrors.taskId = "Vui lòng chọn công việc.";
    }
    if (!workDate) {
      newErrors.workDate = "Vui lòng chọn ngày làm việc.";
    }

    const numHours = parseFloat(hours);
    if (isNaN(numHours) || numHours <= 0) {
      newErrors.hours = "Số giờ làm việc phải lớn hơn 0.";
    } else if (numHours > 24) {
      newErrors.hours = "Số giờ làm việc cho một mục không được vượt quá 24 giờ.";
    }

    if (!description.trim()) {
      newErrors.description = "Vui lòng nhập mô tả nội dung công việc.";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    setServerError("");

    try {
      const payload = {
        projectId: Number(selectedProjectId),
        taskId: Number(selectedTaskId),
        workDate,
        hours: parseFloat(hours),
        isBillable,
        description: description.trim(),
      };

      if (isEditing && initialData) {
        await updateWorkLog(initialData.id, payload);
      } else {
        await createWorkLog(payload);
      }

      onSuccess();
      onClose();
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setServerError(err.message);
      } else {
        setServerError("Đã xảy ra lỗi khi lưu giờ làm việc. Vui lòng thử lại.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={isEditing ? "Chỉnh sửa giờ làm việc" : "Ghi giờ công theo công việc"}
      description="Ghi nhận số giờ làm việc thực tế cho công việc được phân công trong dự án."
      footer={
        <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.75rem", width: "100%" }}>
          <button
            type="button"
            className="button button--secondary"
            onClick={onClose}
            disabled={isSubmitting}
          >
            Hủy bỏ
          </button>
          <button
            type="button"
            className="button button--primary"
            onClick={handleSubmit}
            disabled={isSubmitting}
          >
            {isSubmitting ? "Đang lưu..." : isEditing ? "Lưu thay đổi" : "Lưu dòng giờ công"}
          </button>
        </div>
      }
    >
      <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
        {serverError && (
          <div
            style={{
              padding: "0.75rem 1rem",
              borderRadius: "0.5rem",
              backgroundColor: "#fef2f2",
              border: "1px solid #f87171",
              color: "#991b1b",
              fontSize: "0.875rem",
            }}
          >
            <strong>Lỗi: </strong> {serverError}
          </div>
        )}

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
          {/* Ngày làm việc */}
          <FormField id="work-date" label="Ngày làm việc *" error={errors.workDate}>
            <input
              id="work-date"
              type="date"
              className="input"
              value={workDate}
              onChange={(e) => setWorkDate(e.target.value)}
              disabled={isSubmitting}
            />
          </FormField>

          {/* Số giờ */}
          <FormField id="work-hours" label="Số giờ làm việc (h) *" error={errors.hours} hint="Tối đa 12h/ngày theo QTN-09">
            <input
              id="work-hours"
              type="number"
              step="0.5"
              min="0.5"
              max="24"
              className="input"
              value={hours}
              onChange={(e) => setHours(e.target.value)}
              disabled={isSubmitting}
            />
          </FormField>
        </div>

        {/* Dự án */}
        <FormField id="project-select" label="Dự án đang chạy *" error={errors.projectId}>
          <select
            id="project-select"
            className="select"
            value={selectedProjectId}
            onChange={(e) => handleProjectChange(e.target.value ? Number(e.target.value) : "")}
            disabled={isSubmitting}
          >
            <option value="">-- Chọn dự án --</option>
            {projectOptions.map((p) => (
              <option key={p.id} value={p.id}>
                [{p.code}] {p.name}
              </option>
            ))}
          </select>
        </FormField>

        {/* Công việc */}
        <FormField
          id="task-select"
          label="Công việc được giao *"
          error={errors.taskId}
          hint={
            !selectedProjectId
              ? "Vui lòng chọn dự án trước"
              : availableTasks.length === 0
              ? "Bạn không có công việc nào được phân công trong dự án này"
              : ""
          }
        >
          <select
            id="task-select"
            className="select"
            value={selectedTaskId}
            onChange={(e) => setSelectedTaskId(e.target.value ? Number(e.target.value) : "")}
            disabled={isSubmitting || !selectedProjectId || availableTasks.length === 0}
          >
            <option value="">-- Chọn công việc --</option>
            {availableTasks.map((t) => (
              <option key={t.taskId} value={t.taskId}>
                [{t.taskCode}] {t.taskName} ({t.taskStatus})
              </option>
            ))}
          </select>
        </FormField>

        {/* Tính phí (Billable) */}
        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
          <input
            id="is-billable"
            type="checkbox"
            checked={isBillable}
            onChange={(e) => setIsBillable(e.target.checked)}
            disabled={isSubmitting}
            style={{ width: "1.125rem", height: "1.125rem", cursor: "pointer" }}
          />
          <label htmlFor="is-billable" style={{ fontSize: "0.875rem", cursor: "pointer", userSelect: "none" }}>
            Giờ làm có tính phí cho khách hàng (Billable hours)
          </label>
        </div>

        {/* Mô tả nội dung */}
        <FormField id="work-desc" label="Mô tả nội dung công việc *" error={errors.description}>
          <textarea
            id="work-desc"
            rows={3}
            className="textarea"
            placeholder="Mô tả chi tiết những việc bạn đã thực hiện trong thời gian này..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            disabled={isSubmitting}
          />
        </FormField>
      </form>
    </Dialog>
  );
}
