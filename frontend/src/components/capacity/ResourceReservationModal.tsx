"use client";

import React, { useState, useEffect, useMemo, useRef } from "react";
import {
  X,
  BookmarkCheck,
  CalendarDays,
  Clock,
  User,
  FolderGit2,
  AlertCircle,
  CheckCircle2,
  Loader2,
  Trash2,
  ListFilter,
  PlusCircle,
  Info,
} from "lucide-react";
import {
  createResourceReservation,
  cancelResourceReservation,
  getResourceReservations,
  type ResourceReservationResult,
  type ReservationStatus,
} from "@/lib/api/allocations";
import { getProjects, type ProjectResult } from "@/lib/api/projects";
import { getEmployees, type EmployeeProfile } from "@/lib/api/employees";

interface ResourceReservationModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
  initialEmployeeId?: number;
  initialEmployeeName?: string;
  initialYear?: number;
  initialWeekNumber?: number;
}

export function ResourceReservationModal({
  open,
  onClose,
  onSuccess,
  initialEmployeeId,
  initialEmployeeName,
  initialYear,
  initialWeekNumber,
}: ResourceReservationModalProps) {
  const [activeTab, setActiveTab] = useState<"CREATE" | "LIST">("CREATE");

  // Form states
  const [projectId, setProjectId] = useState<number | "">("");
  const [employeeId, setEmployeeId] = useState<number | "">(initialEmployeeId ?? "");
  const [year, setYear] = useState<number>(initialYear ?? new Date().getFullYear());
  const [weekNumber, setWeekNumber] = useState<number>(initialWeekNumber ?? 1);
  const [reservedHours, setReservedHours] = useState<string>("8.0");
  const [note, setNote] = useState<string>("");

  // Employee autocomplete states
  const [employees, setEmployees] = useState<EmployeeProfile[]>([]);
  const [isLoadingEmployees, setIsLoadingEmployees] = useState(false);
  const [employeeSearchTerm, setEmployeeSearchTerm] = useState<string>(initialEmployeeName ?? "");
  const [isEmployeeDropdownOpen, setIsEmployeeDropdownOpen] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState<EmployeeProfile | null>(null);
  const employeeDropdownRef = useRef<HTMLDivElement>(null);

  // Projects list for dropdown
  const [projects, setProjects] = useState<ProjectResult[]>([]);
  const [isLoadingProjects, setIsLoadingProjects] = useState(false);

  // Reservations list states
  const [reservations, setReservations] = useState<ResourceReservationResult[]>([]);
  const [isLoadingReservations, setIsLoadingReservations] = useState(false);
  const [statusFilter, setStatusFilter] = useState<ReservationStatus | "ALL">("ACTIVE");

  // Submitting / Action states
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Cancel reservation prompt state
  const [cancellingId, setCancellingId] = useState<number | null>(null);
  const [cancelReason, setCancelReason] = useState<string>("");

  const loadProjectsList = async () => {
    setIsLoadingProjects(true);
    try {
      const pageResult = await getProjects(0, 100);
      setProjects(pageResult.content || []);
      if (!projectId && pageResult.content?.length > 0) {
        // Ưu tiên chọn dự án PLANNED đầu tiên nếu có
        const plannedPrj = pageResult.content.find((p) => p.status === "PLANNED");
        setProjectId(plannedPrj ? plannedPrj.id : pageResult.content[0].id);
      }
    } catch (err: any) {
      console.warn("Không thể tải danh sách dự án:", err);
    } finally {
      setIsLoadingProjects(false);
    }
  };

  const loadEmployeesList = async () => {
    setIsLoadingEmployees(true);
    try {
      const res = await getEmployees(1, 100);
      const list = res.content || [];
      setEmployees(list);
      if (initialEmployeeId) {
        const found = list.find((e) => e.id === initialEmployeeId);
        if (found) {
          setSelectedEmployee(found);
          setEmployeeSearchTerm(`${found.employeeCode} - ${found.fullName}`);
        } else if (initialEmployeeName) {
          setEmployeeSearchTerm(initialEmployeeName);
        }
      }
    } catch (err: any) {
      console.warn("Không thể tải danh sách nhân sự:", err);
    } finally {
      setIsLoadingEmployees(false);
    }
  };

  const filteredEmployees = useMemo(() => {
    if (!employeeSearchTerm.trim()) return employees;
    const term = employeeSearchTerm.trim().toLowerCase();
    return employees.filter(
      (emp) =>
        String(emp.id).includes(term) ||
        (emp.employeeCode && emp.employeeCode.toLowerCase().includes(term)) ||
        (emp.fullName && emp.fullName.toLowerCase().includes(term)) ||
        (emp.orgUnitName && emp.orgUnitName.toLowerCase().includes(term))
    );
  }, [employees, employeeSearchTerm]);

  // Click outside to close employee dropdown
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        employeeDropdownRef.current &&
        !employeeDropdownRef.current.contains(event.target as Node)
      ) {
        setIsEmployeeDropdownOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const loadReservationsList = async () => {
    setIsLoadingReservations(true);
    try {
      const data = await getResourceReservations({
        employeeId: initialEmployeeId ? Number(initialEmployeeId) : undefined,
        status: statusFilter === "ALL" ? undefined : statusFilter,
      });
      setReservations(data || []);
    } catch (err: any) {
      console.warn("Không thể tải danh sách giữ chỗ:", err);
    } finally {
      setIsLoadingReservations(false);
    }
  };

  // Reset form when modal opens or initial props change
  useEffect(() => {
    if (open) {
      if (initialEmployeeId) {
        setEmployeeId(initialEmployeeId);
      } else {
        setEmployeeId("");
        setSelectedEmployee(null);
        setEmployeeSearchTerm("");
      }
      if (initialEmployeeName) setEmployeeSearchTerm(initialEmployeeName);
      if (initialYear) setYear(initialYear);
      if (initialWeekNumber) setWeekNumber(initialWeekNumber);
      setErrorMsg(null);
      setSuccessMsg(null);
      loadProjectsList();
      loadEmployeesList();
      loadReservationsList();
    }
  }, [open, initialEmployeeId, initialEmployeeName, initialYear, initialWeekNumber]);

  useEffect(() => {
    if (open && activeTab === "LIST") {
      loadReservationsList();
    }
  }, [open, activeTab, statusFilter]);

  if (!open) return null;

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setSuccessMsg(null);

    if (!projectId) {
      setErrorMsg("Vui lòng chọn dự án cần giữ chỗ.");
      return;
    }
    if (!employeeId) {
      setErrorMsg("Vui lòng nhập ID nhân sự.");
      return;
    }
    const hoursNum = parseFloat(reservedHours);
    if (isNaN(hoursNum) || hoursNum <= 0) {
      setErrorMsg("Số giờ giữ chỗ phải lớn hơn 0.");
      return;
    }

    setIsSubmitting(true);
    try {
      await createResourceReservation({
        projectId: Number(projectId),
        employeeId: Number(employeeId),
        year: Number(year),
        weekNumber: Number(weekNumber),
        reservedHours: hoursNum,
        note: note.trim() || undefined,
      });
      setSuccessMsg("Giữ chỗ nguồn lực thành công (QTN-13: Chưa tính vào giờ phân bổ chính thức)!");
      setNote("");
      onSuccess();
      setTimeout(() => {
        setActiveTab("LIST");
        loadReservationsList();
      }, 800);
    } catch (err: any) {
      setErrorMsg(err.message || "Đã xảy ra lỗi khi tạo giữ chỗ nguồn lực.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancelReservation = async (id: number) => {
    if (!cancelReason.trim()) {
      setErrorMsg("Vui lòng nhập lý do hủy giữ chỗ.");
      return;
    }

    setIsSubmitting(true);
    setErrorMsg(null);
    try {
      await cancelResourceReservation(id, { reason: cancelReason.trim() });
      setCancellingId(null);
      setCancelReason("");
      setSuccessMsg("Đã hủy giữ chỗ nguồn lực thành công.");
      loadReservationsList();
      onSuccess();
    } catch (err: any) {
      setErrorMsg(err.message || "Không thể hủy giữ chỗ.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative w-full max-w-2xl rounded-2xl bg-white shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[90vh]">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-slate-50/50">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-50 text-amber-600 border border-amber-200">
              <BookmarkCheck className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-800">
                Giữ Chỗ Nguồn Lực (NCL-06-CN-005)
              </h2>
              <p className="text-xs text-slate-500">
                Kế hoạch giữ chỗ cho dự án dự kiến theo quy tắc QTN-13
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Tab switcher */}
        <div className="flex border-b border-slate-200 px-6 bg-white gap-4">
          <button
            type="button"
            onClick={() => {
              setActiveTab("CREATE");
              setErrorMsg(null);
              setSuccessMsg(null);
            }}
            className={`flex items-center gap-1.5 py-3 text-xs font-semibold border-b-2 transition ${
              activeTab === "CREATE"
                ? "border-amber-600 text-amber-700"
                : "border-transparent text-slate-500 hover:text-slate-700"
            }`}
          >
            <PlusCircle className="h-4 w-4" />
            Tạo giữ chỗ mới
          </button>
          <button
            type="button"
            onClick={() => {
              setActiveTab("LIST");
              setErrorMsg(null);
              setSuccessMsg(null);
            }}
            className={`flex items-center gap-1.5 py-3 text-xs font-semibold border-b-2 transition ${
              activeTab === "LIST"
                ? "border-amber-600 text-amber-700"
                : "border-transparent text-slate-500 hover:text-slate-700"
            }`}
          >
            <ListFilter className="h-4 w-4" />
            Danh sách giữ chỗ
          </button>
        </div>

        {/* Alert Notifications */}
        {errorMsg && (
          <div className="mx-6 mt-4 flex items-start gap-2.5 rounded-xl bg-rose-50 p-3 border border-rose-200 text-xs text-rose-700">
            <AlertCircle className="h-4 w-4 shrink-0 text-rose-500 mt-0.5" />
            <div className="flex-1 font-medium">{errorMsg}</div>
          </div>
        )}
        {successMsg && (
          <div className="mx-6 mt-4 flex items-start gap-2.5 rounded-xl bg-emerald-50 p-3 border border-emerald-200 text-xs text-emerald-700">
            <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-500 mt-0.5" />
            <div className="flex-1 font-medium">{successMsg}</div>
          </div>
        )}

        {/* Modal Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {activeTab === "CREATE" ? (
            <form onSubmit={handleCreateSubmit} className="space-y-4">
              {/* QTN-13 Notice */}
              <div className="flex items-start gap-2.5 rounded-xl bg-amber-50/60 p-3.5 border border-dashed border-amber-300 text-xs text-amber-800">
                <Info className="h-4 w-4 shrink-0 text-amber-600 mt-0.5" />
                <div className="leading-relaxed">
                  <span className="font-bold">Quy tắc QTN-13:</span> Giờ giữ chỗ cho dự án dự kiến không được cộng vào giờ phân bổ chính thức và không làm giảm giờ khả dụng còn lại của nhân sự.
                </div>
              </div>

              {/* Dự án */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5 flex items-center gap-1">
                  <FolderGit2 className="h-3.5 w-3.5 text-slate-500" />
                  Dự án (Dự kiến hoặc chính thức) <span className="text-rose-500">*</span>
                </label>
                {isLoadingProjects ? (
                  <div className="flex items-center gap-2 text-xs text-slate-400 py-2">
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    Đang tải danh sách dự án...
                  </div>
                ) : (
                  <select
                    value={projectId}
                    onChange={(e) => setProjectId(e.target.value ? Number(e.target.value) : "")}
                    className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-800 focus:border-amber-500 focus:outline-none"
                    required
                  >
                    <option value="">-- Chọn dự án --</option>
                    {projects.map((p) => (
                      <option key={p.id} value={p.id}>
                        [{p.projectCode}] {p.projectName} {p.status === "PLANNED" ? "★ (Dự kiến)" : `(${p.status})`}
                      </option>
                    ))}
                  </select>
                )}
              </div>

              {/* Nhân sự (Autocomplete với gợi ý danh sách) */}
              <div className="relative" ref={employeeDropdownRef}>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="block text-xs font-semibold text-slate-700 flex items-center gap-1">
                    <User className="h-3.5 w-3.5 text-slate-500" />
                    Nhân sự <span className="text-rose-500">*</span>
                  </label>
                  {selectedEmployee ? (
                    <span className="text-[11px] font-medium text-amber-700 bg-amber-50 px-2 py-0.5 rounded-md border border-amber-200/80 flex items-center gap-1">
                      <CheckCircle2 className="h-3 w-3 text-amber-600" />
                      [{selectedEmployee.employeeCode}] ID: {selectedEmployee.id}
                    </span>
                  ) : employeeId ? (
                    <span className="text-[11px] font-medium text-amber-700 bg-amber-50 px-2 py-0.5 rounded-md border border-amber-200/80 flex items-center gap-1">
                      <CheckCircle2 className="h-3 w-3 text-amber-600" />
                      ID: {employeeId}
                    </span>
                  ) : null}
                </div>

                <div className="relative">
                  <input
                    type="text"
                    value={employeeSearchTerm}
                    onChange={(e) => {
                      const val = e.target.value;
                      setEmployeeSearchTerm(val);
                      setIsEmployeeDropdownOpen(true);
                      if (!val.trim()) {
                        setEmployeeId("");
                        setSelectedEmployee(null);
                      } else {
                        const numVal = Number(val.trim());
                        if (!isNaN(numVal) && numVal > 0) {
                          setEmployeeId(numVal);
                          const match = employees.find((emp) => emp.id === numVal);
                          setSelectedEmployee(match || null);
                        } else {
                          const match = employees.find(
                            (emp) => emp.employeeCode?.toLowerCase() === val.trim().toLowerCase()
                          );
                          if (match) {
                            setEmployeeId(match.id);
                            setSelectedEmployee(match);
                          }
                        }
                      }
                    }}
                    onFocus={() => setIsEmployeeDropdownOpen(true)}
                    placeholder="Gõ mã ID (VD: 1), mã NV (VD: EMP01), hoặc họ tên..."
                    className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 pr-8 text-xs font-medium text-slate-800 focus:border-amber-500 focus:outline-none"
                    required
                  />
                  {employeeSearchTerm && (
                    <button
                      type="button"
                      onClick={() => {
                        setEmployeeId("");
                        setSelectedEmployee(null);
                        setEmployeeSearchTerm("");
                        setIsEmployeeDropdownOpen(true);
                      }}
                      className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-0.5"
                    >
                      <X className="h-3.5 w-3.5" />
                    </button>
                  )}
                </div>

                {/* Dropdown gợi ý */}
                {isEmployeeDropdownOpen && (
                  <div className="absolute z-30 mt-1 max-h-56 w-full overflow-y-auto rounded-xl border border-slate-200 bg-white shadow-xl py-1 text-xs divide-y divide-slate-50 animate-in fade-in zoom-in-95 duration-100">
                    {isLoadingEmployees ? (
                      <div className="flex items-center justify-center gap-2 p-4 text-slate-400">
                        <Loader2 className="h-3.5 w-3.5 animate-spin text-amber-600" />
                        Đang tải danh sách nhân sự...
                      </div>
                    ) : filteredEmployees.length === 0 ? (
                      <div className="p-4 text-center text-slate-400 italic">
                        Không tìm thấy nhân sự phù hợp với "{employeeSearchTerm}"
                      </div>
                    ) : (
                      filteredEmployees.map((emp) => {
                        const isSelected = employeeId === emp.id;
                        return (
                          <div
                            key={emp.id}
                            onMouseDown={(e) => {
                              e.preventDefault();
                              setEmployeeId(emp.id);
                              setSelectedEmployee(emp);
                              setEmployeeSearchTerm(`[${emp.employeeCode}] ${emp.fullName}`);
                              setIsEmployeeDropdownOpen(false);
                            }}
                            className={`flex items-center justify-between px-3 py-2 cursor-pointer transition hover:bg-amber-50/70 ${
                              isSelected ? "bg-amber-50/90 font-semibold text-amber-900" : "text-slate-700"
                            }`}
                          >
                            <div className="flex items-center gap-2.5">
                              <span className="font-mono text-[10px] font-bold bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded border border-slate-200">
                                {emp.employeeCode || `ID:${emp.id}`}
                              </span>
                              <div>
                                <div className="text-xs font-medium text-slate-800 flex items-center gap-1.5">
                                  {emp.fullName}
                                  {emp.professionalRole && (
                                    <span className="text-[10px] text-slate-500 font-normal">
                                      ({emp.professionalRole})
                                    </span>
                                  )}
                                </div>
                                <div className="text-[10px] text-slate-400">
                                  {emp.orgUnitName || "Chưa gắn đơn vị"}
                                </div>
                              </div>
                            </div>
                            <span className="text-[11px] font-mono text-slate-400 bg-slate-50 px-1.5 py-0.5 rounded border border-slate-100">
                              ID: {emp.id}
                            </span>
                          </div>
                        );
                      })
                    )}
                  </div>
                )}
              </div>

              {/* Năm và Tuần */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1.5 flex items-center gap-1">
                    <CalendarDays className="h-3.5 w-3.5 text-slate-500" />
                    Năm <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="number"
                    value={year}
                    onChange={(e) => setYear(Number(e.target.value))}
                    min={2020}
                    max={2035}
                    className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-800 focus:border-amber-500 focus:outline-none"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1.5 flex items-center gap-1">
                    <CalendarDays className="h-3.5 w-3.5 text-slate-500" />
                    Tuần (1 - 53) <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="number"
                    value={weekNumber}
                    onChange={(e) => setWeekNumber(Number(e.target.value))}
                    min={1}
                    max={53}
                    className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-800 focus:border-amber-500 focus:outline-none"
                    required
                  />
                </div>
              </div>

              {/* Số giờ giữ chỗ */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5 flex items-center gap-1">
                  <Clock className="h-3.5 w-3.5 text-slate-500" />
                  Số giờ giữ chỗ (Hours) <span className="text-rose-500">*</span>
                </label>
                <input
                  type="number"
                  step="any"
                  min="0.5"
                  max="168"
                  value={reservedHours}
                  onChange={(e) => setReservedHours(e.target.value)}
                  placeholder="Ví dụ: 8.0 hoặc 16"
                  className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-800 focus:border-amber-500 focus:outline-none"
                  required
                />
              </div>

              {/* Ghi chú */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Ghi chú lý do giữ chỗ
                </label>
                <textarea
                  rows={2}
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="Nhập ghi chú hoặc lý do đề xuất giữ chỗ..."
                  className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-800 focus:border-amber-500 focus:outline-none"
                />
              </div>

              {/* Submit Button */}
              <div className="pt-2 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={onClose}
                  className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition"
                >
                  Đóng
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-amber-600 px-4 py-2 text-xs font-semibold text-white hover:bg-amber-700 disabled:opacity-50 transition shadow-xs"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      Đang xử lý...
                    </>
                  ) : (
                    <>
                      <BookmarkCheck className="h-3.5 w-3.5" />
                      Xác nhận giữ chỗ
                    </>
                  )}
                </button>
              </div>
            </form>
          ) : (
            /* Tab: Danh sách giữ chỗ */
            <div className="space-y-4">
              {/* Filter */}
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-600">
                  {initialEmployeeName ? `Giữ chỗ của: ${initialEmployeeName}` : "Tất cả giữ chỗ"}
                </span>
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value as any)}
                  className="rounded-xl border border-slate-200 bg-white px-2.5 py-1 text-xs font-medium text-slate-700 focus:outline-none"
                >
                  <option value="ACTIVE">Chỉ đang hoạt động (ACTIVE)</option>
                  <option value="CONVERTED">Đã chuyển phân bổ (CONVERTED)</option>
                  <option value="CANCELLED">Đã hủy (CANCELLED)</option>
                  <option value="ALL">Tất cả trạng thái</option>
                </select>
              </div>

              {isLoadingReservations ? (
                <div className="flex flex-col items-center justify-center p-8 text-slate-400">
                  <Loader2 className="h-6 w-6 animate-spin text-amber-600 mb-2" />
                  <span className="text-xs">Đang tải danh sách giữ chỗ...</span>
                </div>
              ) : reservations.length === 0 ? (
                <div className="p-8 text-center text-xs text-slate-400 border border-dashed border-slate-200 rounded-xl">
                  Không tìm thấy dữ liệu giữ chỗ nào.
                </div>
              ) : (
                <div className="divide-y divide-slate-100 border border-slate-200 rounded-xl overflow-hidden">
                  {reservations.map((res) => (
                    <div key={res.id} className="p-3.5 bg-white hover:bg-slate-50/50 transition flex flex-col gap-2">
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-xs text-slate-800">
                              [{res.projectCode}] {res.projectName}
                            </span>
                            <span
                              className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                                res.status === "ACTIVE"
                                  ? "bg-amber-100 text-amber-800"
                                  : res.status === "CONVERTED"
                                  ? "bg-emerald-100 text-emerald-800"
                                  : "bg-slate-100 text-slate-600"
                              }`}
                            >
                              {res.status === "ACTIVE"
                                ? "Đang giữ"
                                : res.status === "CONVERTED"
                                ? "Đã chuyển đổi"
                                : "Đã hủy"}
                            </span>
                          </div>
                          <div className="flex items-center gap-3 text-[11px] text-slate-500 mt-1">
                            <span>Nhân sự: <strong className="text-slate-700">{res.employeeFullName} ({res.employeeCode})</strong></span>
                            <span>•</span>
                            <span>Tuần {res.weekNumber} / {res.year}</span>
                            <span>•</span>
                            <span className="font-semibold text-amber-700">Giữ chỗ: {res.reservedHours}h</span>
                          </div>
                          {res.note && (
                            <p className="text-[11px] text-slate-600 italic mt-1 bg-slate-50 p-1.5 rounded-lg border border-slate-100">
                              "{res.note}"
                            </p>
                          )}
                        </div>

                        {res.status === "ACTIVE" && (
                          <div>
                            {cancellingId === res.id ? (
                              <div className="flex flex-col items-end gap-1.5">
                                <input
                                  type="text"
                                  value={cancelReason}
                                  onChange={(e) => setCancelReason(e.target.value)}
                                  placeholder="Nhập lý do hủy..."
                                  className="rounded-lg border border-rose-200 px-2 py-1 text-xs text-slate-800 focus:outline-none focus:border-rose-400"
                                />
                                <div className="flex items-center gap-1">
                                  <button
                                    type="button"
                                    onClick={() => {
                                      setCancellingId(null);
                                      setCancelReason("");
                                    }}
                                    className="rounded px-2 py-0.5 text-[10px] text-slate-500 hover:bg-slate-100"
                                  >
                                    Hủy bỏ
                                  </button>
                                  <button
                                    type="button"
                                    disabled={isSubmitting}
                                    onClick={() => handleCancelReservation(res.id)}
                                    className="rounded bg-rose-600 px-2 py-0.5 text-[10px] font-semibold text-white hover:bg-rose-700 disabled:opacity-50"
                                  >
                                    Xác nhận hủy
                                  </button>
                                </div>
                              </div>
                            ) : (
                              <button
                                type="button"
                                onClick={() => {
                                  setCancellingId(res.id);
                                  setCancelReason("");
                                }}
                                className="inline-flex items-center gap-1 rounded-lg border border-rose-200 px-2 py-1 text-[11px] font-medium text-rose-600 hover:bg-rose-50 transition"
                                title="Hủy giữ chỗ này"
                              >
                                <Trash2 className="h-3 w-3" />
                                Hủy
                              </button>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
