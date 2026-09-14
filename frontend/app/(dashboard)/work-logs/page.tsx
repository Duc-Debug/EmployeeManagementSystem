"use client";

import { useCallback, useEffect, useState } from "react";
import { PageHeader } from "@/components/layout/PageHeader";
import { Badge } from "@/components/ui/Badge";
import { Dialog } from "@/components/ui/Dialog";
import { EmptyState } from "@/components/ui/EmptyState";
import { Icon } from "@/components/ui/Icon";
import { WorkLogModal } from "@/components/timesheet/WorkLogModal";
import {
  type AssignedTaskOption,
  type WeeklyTimesheet,
  type WorkLog,
  deleteWorkLog,
  getMyAssignedTasks,
  getWeeklyTimesheet,
} from "@/lib/api/timesheetApi";
import { ApiError } from "@/lib/api-client";

export default function WorkLogsPage() {
  const [selectedDate, setSelectedDate] = useState<string>(
    new Date().toISOString().split("T")[0]
  );
  const [timesheet, setTimesheet] = useState<WeeklyTimesheet | null>(null);
  const [assignedTasks, setAssignedTasks] = useState<AssignedTaskOption[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingLog, setEditingLog] = useState<WorkLog | null>(null);
  const [defaultModalDate, setDefaultModalDate] = useState<string>("");

  // Delete confirmation
  const [deletingLogId, setDeletingLogId] = useState<number | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchTimesheetData = useCallback(async (date: string) => {
    setIsLoading(true);
    setErrorMessage("");
    try {
      const [tsData, tasksData] = await Promise.all([
        getWeeklyTimesheet(date),
        getMyAssignedTasks(),
      ]);
      setTimesheet(tsData);
      setAssignedTasks(tasksData);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage("Không thể tải thông tin bảng chấm công. Vui lòng thử lại.");
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchTimesheetData(selectedDate);
  }, [fetchTimesheetData, selectedDate]);

  // Week navigation helpers
  const handlePreviousWeek = () => {
    const current = new Date(selectedDate);
    current.setDate(current.getDate() - 7);
    setSelectedDate(current.toISOString().split("T")[0]);
  };

  const handleNextWeek = () => {
    const current = new Date(selectedDate);
    current.setDate(current.getDate() + 7);
    setSelectedDate(current.toISOString().split("T")[0]);
  };

  const handleCurrentWeek = () => {
    setSelectedDate(new Date().toISOString().split("T")[0]);
  };

  const handleOpenAddModal = (dateForDay?: string) => {
    setEditingLog(null);
    setDefaultModalDate(dateForDay || selectedDate);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (log: WorkLog) => {
    setEditingLog(log);
    setDefaultModalDate(log.workDate);
    setIsModalOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!deletingLogId) return;
    setIsDeleting(true);
    try {
      await deleteWorkLog(deletingLogId);
      setDeletingLogId(null);
      await fetchTimesheetData(selectedDate);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        alert(err.message);
      } else {
        alert("Lỗi khi xóa dòng ghi giờ.");
      }
    } finally {
      setIsDeleting(false);
    }
  };

  const getStatusBadgeVariant = (status: string) => {
    switch (status) {
      case "APPROVED":
        return "success";
      case "SUBMITTED":
        return "primary";
      case "REJECTED":
        return "danger";
      case "DRAFT":
      default:
        return "neutral";
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case "APPROVED":
        return "Đã duyệt";
      case "SUBMITTED":
        return "Chờ duyệt";
      case "REJECTED":
        return "Bị từ chối";
      case "DRAFT":
      default:
        return "Nháp";
    }
  };

  return (
    <div className="page-container">
      <PageHeader
        title="Ghi giờ công theo công việc"
        subtitle="Quản lý và ghi nhận giờ làm việc thực tế theo công việc trong các dự án đang chạy (NCL-09-CN-001)."
        actions={
          <button
            type="button"
            className="button button--primary"
            onClick={() => handleOpenAddModal()}
            disabled={timesheet && !timesheet.isEditable}
          >
            <Icon name="plus" /> Ghi giờ công
          </button>
        }
      />

      {errorMessage && (
        <div
          style={{
            padding: "1rem",
            marginBottom: "1.5rem",
            borderRadius: "0.5rem",
            backgroundColor: "#fef2f2",
            border: "1px solid #f87171",
            color: "#991b1b",
          }}
        >
          <strong>Lỗi: </strong> {errorMessage}
        </div>
      )}

      {/* Week Navigator & Summary Card */}
      <section
        style={{
          backgroundColor: "#ffffff",
          borderRadius: "0.75rem",
          border: "1px solid #e5e7eb",
          padding: "1.25rem 1.5rem",
          marginBottom: "1.5rem",
          boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
        }}
      >
        <div
          style={{
            display: "flex",
            flexWrap: "wrap",
            alignItems: "center",
            justifyContent: "space-between",
            gap: "1rem",
          }}
        >
          {/* Week controls */}
          <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "0.25rem" }}>
              <button
                type="button"
                className="button button--secondary button--sm"
                onClick={handlePreviousWeek}
                title="Tuần trước"
              >
                ◀ Tuần trước
              </button>
              <button
                type="button"
                className="button button--secondary button--sm"
                onClick={handleCurrentWeek}
              >
                Tuần hiện tại
              </button>
              <button
                type="button"
                className="button button--secondary button--sm"
                onClick={handleNextWeek}
                title="Tuần sau"
              >
                Tuần sau ▶
              </button>
            </div>

            <input
              type="date"
              className="input input--sm"
              style={{ width: "auto" }}
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
            />
          </div>

          {/* Week info & Status */}
          {timesheet && (
            <div style={{ display: "flex", alignItems: "center", gap: "1.5rem" }}>
              <div style={{ fontSize: "0.875rem", color: "#4b5563" }}>
                Khoảng thời gian:{" "}
                <strong style={{ color: "#111827" }}>
                  {timesheet.weekStartDate} → {timesheet.weekEndDate}
                </strong>
              </div>

              <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                <span style={{ fontSize: "0.875rem", color: "#6b7280" }}>Trạng thái bảng:</span>
                <Badge variant={getStatusBadgeVariant(timesheet.status)}>
                  {getStatusLabel(timesheet.status)}
                </Badge>
              </div>
            </div>
          )}
        </div>

        {/* Weekly Progress Bar */}
        {timesheet && (
          <div style={{ marginTop: "1.25rem", borderTop: "1px solid #f3f4f6", paddingTop: "1rem" }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "0.5rem", fontSize: "0.875rem" }}>
              <span>
                Tổng giờ công trong tuần:{" "}
                <strong style={{ fontSize: "1.125rem", color: "#1d4ed8" }}>
                  {timesheet.totalHours} giờ
                </strong>{" "}
                / 40.0 giờ chuẩn
              </span>
              <span style={{ color: "#6b7280" }}>
                {Math.round((timesheet.totalHours / 40.0) * 100)}% chỉ tiêu tuần
              </span>
            </div>
            <div
              style={{
                width: "100%",
                height: "0.5rem",
                backgroundColor: "#e5e7eb",
                borderRadius: "9999px",
                overflow: "hidden",
              }}
            >
              <div
                style={{
                  width: `${Math.min((timesheet.totalHours / 40.0) * 100, 100)}%`,
                  height: "100%",
                  backgroundColor: timesheet.totalHours > 48 ? "#f59e0b" : "#2563eb",
                  borderRadius: "9999px",
                  transition: "width 0.3s ease",
                }}
              />
            </div>
          </div>
        )}
      </section>

      {/* Daily Breakdown & Work Logs List */}
      {isLoading ? (
        <div style={{ padding: "3rem", textAlign: "center", color: "#6b7280" }}>
          Đang tải dữ liệu bảng chấm công...
        </div>
      ) : timesheet && timesheet.allEntries.length === 0 ? (
        <EmptyState
          title="Chưa có giờ công nào trong tuần này"
          description="Bạn chưa ghi nhận giờ làm việc nào cho các công việc trong tuần đã chọn."
          action={
            <button
              type="button"
              className="button button--primary"
              onClick={() => handleOpenAddModal()}
              disabled={!timesheet.isEditable}
            >
              <Icon name="plus" /> Ghi giờ công đầu tiên
            </button>
          }
        />
      ) : timesheet ? (
        <div style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
          {timesheet.dailyGroups.map((group) => (
            <div
              key={group.date}
              style={{
                backgroundColor: "#ffffff",
                borderRadius: "0.75rem",
                border: group.isExceededLimit ? "1.5px solid #ef4444" : "1px solid #e5e7eb",
                overflow: "hidden",
                boxShadow: "0 1px 2px rgba(0,0,0,0.04)",
              }}
            >
              {/* Daily Header */}
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  padding: "0.875rem 1.25rem",
                  backgroundColor: group.entries.length > 0 ? "#f9fafb" : "#ffffff",
                  borderBottom: group.entries.length > 0 ? "1px solid #e5e7eb" : "none",
                }}
              >
                <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
                  <span style={{ fontWeight: 600, color: "#111827" }}>{group.dayOfWeek}</span>
                  <span style={{ fontSize: "0.875rem", color: "#6b7280" }}>({group.date})</span>
                  {group.isExceededLimit && (
                    <Badge variant="danger">Vượt quá 12h/ngày (QTN-09)</Badge>
                  )}
                </div>

                <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
                  <span style={{ fontSize: "0.875rem", color: "#374151" }}>
                    Tổng:{" "}
                    <strong style={{ color: group.isExceededLimit ? "#dc2626" : "#111827" }}>
                      {group.totalHours}h
                    </strong>
                  </span>
                  {timesheet.isEditable && (
                    <button
                      type="button"
                      className="button button--secondary button--xs"
                      onClick={() => handleOpenAddModal(group.date)}
                      title="Ghi thêm giờ cho ngày này"
                    >
                      <Icon name="plus" /> Thêm
                    </button>
                  )}
                </div>
              </div>

              {/* Daily Table of Entries */}
              {group.entries.length > 0 ? (
                <div style={{ overflowX: "auto" }}>
                  <table className="table">
                    <thead>
                      <tr>
                        <th style={{ width: "18%" }}>Dự án</th>
                        <th style={{ width: "22%" }}>Công việc</th>
                        <th style={{ width: "32%" }}>Mô tả nội dung</th>
                        <th style={{ width: "8%", textAlign: "center" }}>Số giờ</th>
                        <th style={{ width: "10%", textAlign: "center" }}>Tính phí</th>
                        <th style={{ width: "10%", textAlign: "right" }}>Thao tác</th>
                      </tr>
                    </thead>
                    <tbody>
                      {group.entries.map((entry) => (
                        <tr key={entry.id}>
                          <td>
                            <div style={{ fontWeight: 500, color: "#111827" }}>{entry.projectName}</div>
                            <div style={{ fontSize: "0.75rem", color: "#6b7280" }}>{entry.projectCode}</div>
                          </td>
                          <td>
                            <div style={{ fontWeight: 500, color: "#111827" }}>{entry.taskName}</div>
                            <div style={{ fontSize: "0.75rem", color: "#6b7280" }}>{entry.taskCode}</div>
                          </td>
                          <td style={{ whiteSpace: "pre-wrap", color: "#374151", fontSize: "0.875rem" }}>
                            {entry.description}
                          </td>
                          <td style={{ textAlign: "center", fontWeight: 600, color: "#1d4ed8" }}>
                            {entry.hours}h
                          </td>
                          <td style={{ textAlign: "center" }}>
                            {entry.isBillable ? (
                              <Badge variant="success">Billable</Badge>
                            ) : (
                              <Badge variant="neutral">Non-billable</Badge>
                            )}
                          </td>
                          <td style={{ textAlign: "right" }}>
                            {timesheet.isEditable && entry.status === "DRAFT" ? (
                              <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.25rem" }}>
                                <button
                                  type="button"
                                  className="icon-button icon-button--sm"
                                  onClick={() => handleOpenEditModal(entry)}
                                  title="Chỉnh sửa"
                                >
                                  <Icon name="pencil" />
                                </button>
                                <button
                                  type="button"
                                  className="icon-button icon-button--sm"
                                  onClick={() => setDeletingLogId(entry.id)}
                                  title="Xóa"
                                  style={{ color: "#dc2626" }}
                                >
                                  <Icon name="trash" />
                                </button>
                              </div>
                            ) : (
                              <span style={{ fontSize: "0.75rem", color: "#9ca3af" }}>Khóa</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div style={{ padding: "0.75rem 1.25rem", color: "#9ca3af", fontSize: "0.8125rem" }}>
                  Chưa ghi nhận giờ công trong ngày này.
                </div>
              )}
            </div>
          ))}
        </div>
      ) : null}

      {/* Work Log Modal */}
      <WorkLogModal
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={() => fetchTimesheetData(selectedDate)}
        initialData={editingLog}
        assignedTasks={assignedTasks}
        defaultDate={defaultModalDate}
      />

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deletingLogId !== null}
        onClose={() => setDeletingLogId(null)}
        title="Xác nhận xóa dòng giờ công"
        description="Bạn có chắc chắn muốn xóa dòng giờ công này không? Dữ liệu bảng chấm công tuần sẽ được tự động tính toán lại."
        footer={
          <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.75rem", width: "100%" }}>
            <button
              type="button"
              className="button button--secondary"
              onClick={() => setDeletingLogId(null)}
              disabled={isDeleting}
            >
              Hủy bỏ
            </button>
            <button
              type="button"
              className="button button--danger"
              onClick={handleDeleteConfirm}
              disabled={isDeleting}
            >
              {isDeleting ? "Đang xóa..." : "Xóa dòng giờ công"}
            </button>
          </div>
        }
      >
        <p style={{ fontSize: "0.875rem", color: "#4b5563" }}>
          Thao tác này sẽ xóa bản ghi giờ công ở trạng thái nháp và ghi nhận vào nhật ký kiểm toán hệ thống.
        </p>
      </Dialog>
    </div>
  );
}
