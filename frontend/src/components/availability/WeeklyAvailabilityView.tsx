import { useState, useEffect, useMemo, useCallback } from "react";
import {
  Search,
  Plus,
  ChevronLeft,
  ChevronRight,
  RotateCcw,
  Loader2,
  Users,
  AlertCircle,
  Building2,
  CheckCircle2,
  Clock,
  Sparkles,
} from "lucide-react";
import { useAuthUser } from "@/lib/auth-session";
import { getEmployees, type EmployeeProfile } from "@/lib/api/employees";
import {
  getWeeklyCapacity,
  type WeeklyAvailabilityResult,
} from "@/lib/api/availability";
import {
  getCurrentIsoWeek,
  getIsoWeekDateRange,
} from "./availability.types";
import CapacitySummaryCard from "./CapacitySummaryCard";
import DeclareAvailabilityModal from "./DeclareAvailabilityModal";

export default function WeeklyAvailabilityView() {
  const currentUser = useAuthUser();
  const roleCode = currentUser?.roleCode?.toUpperCase().replace(/_/g, "-") || "";
  const isHR = roleCode === "VT-05";
  const isAdmin = roleCode === "VT-06";
  const canDeclare = isHR || isAdmin;
  const isSelfOnly = roleCode === "VT-04" || currentUser?.dataScope === "SELF";

  // Current ISO Week state
  const currentIso = useMemo(() => getCurrentIsoWeek(), []);
  const [selectedYear, setSelectedYear] = useState<number>(currentIso.year);
  const [selectedWeek, setSelectedWeek] = useState<number>(currentIso.weekNumber);

  // Employees & Capacities state
  const [employees, setEmployees] = useState<EmployeeProfile[]>([]);
  const [capacities, setCapacities] = useState<Record<number, WeeklyAvailabilityResult>>({});
  const [capacityErrors, setCapacityErrors] = useState<Record<number, string>>({});
  const [isLoadingEmployees, setIsLoadingEmployees] = useState<boolean>(true);
  const [isLoadingCapacities, setIsLoadingCapacities] = useState<boolean>(false);
  const [searchTerm, setSearchTerm] = useState<string>("");

  // Selected employee for detailed summary card view
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<number | null>(null);

  // Modal state
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [modalTargetEmployeeId, setModalTargetEmployeeId] = useState<number | undefined>(undefined);
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const showNotification = (type: "success" | "error", message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 4000);
  };

  // Week range label
  const weekDateRange = useMemo(() => {
    try {
      const { startDate, endDate } = getIsoWeekDateRange(selectedYear, selectedWeek);
      const startStr = startDate.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" });
      const endStr = endDate.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
      return `Thứ 2, ${startStr} — Chủ Nhật, ${endStr}`;
    } catch {
      return "";
    }
  }, [selectedYear, selectedWeek]);

  // Step week navigation
  const handleNavigateWeek = (delta: number) => {
    let nextWeek = selectedWeek + delta;
    let nextYear = selectedYear;
    if (nextWeek < 1) {
      nextYear -= 1;
      nextWeek = 52;
    } else if (nextWeek > 52) {
      nextYear += 1;
      nextWeek = 1;
    }
    setSelectedYear(nextYear);
    setSelectedWeek(nextWeek);
  };

  // Reset to current week
  const handleResetToCurrentWeek = () => {
    const iso = getCurrentIsoWeek();
    setSelectedYear(iso.year);
    setSelectedWeek(iso.weekNumber);
  };

  // 1. Fetch employee list
  useEffect(() => {
    let isMounted = true;
    async function loadEmployees() {
      setIsLoadingEmployees(true);
      try {
        const res = await getEmployees(1, 100);
        if (!isMounted) return;
        const list = res?.content || [];
        setEmployees(list);
        if (list.length > 0 && !selectedEmployeeId) {
          // If self-only, select current user employee profile
          const selfMatch = list.find((e) => e.userId === currentUser?.id || e.id === currentUser?.id);
          setSelectedEmployeeId(selfMatch ? selfMatch.id : list[0].id);
        }
      } catch (err) {
        console.warn("Lỗi tải danh sách nhân viên:", err);
      } finally {
        if (isMounted) setIsLoadingEmployees(false);
      }
    }
    loadEmployees();
    return () => {
      isMounted = false;
    };
  }, [currentUser?.id]);

  // Filtered employees according to role / search
  const visibleEmployees = useMemo(() => {
    let list = employees;
    if (isSelfOnly && currentUser) {
      list = list.filter((e) => e.userId === currentUser.id || e.id === currentUser.id);
    }
    if (!searchTerm.trim()) return list;
    const q = searchTerm.toLowerCase().trim();
    return list.filter(
      (e) =>
        e.fullName.toLowerCase().includes(q) ||
        e.employeeCode.toLowerCase().includes(q) ||
        (e.orgUnitName && e.orgUnitName.toLowerCase().includes(q))
    );
  }, [employees, isSelfOnly, currentUser, searchTerm]);

  // 2. Fetch weekly capacity for each visible employee
  const fetchCapacities = useCallback(async () => {
    if (visibleEmployees.length === 0) return;
    setIsLoadingCapacities(true);
    const resultsMap: Record<number, WeeklyAvailabilityResult> = {};
    const errorsMap: Record<number, string> = {};

    await Promise.all(
      visibleEmployees.map(async (emp) => {
        try {
          const res = await getWeeklyCapacity(emp.id, selectedYear, selectedWeek);
          if (res) {
            resultsMap[emp.id] = res;
          }
        } catch (err: any) {
          // Do not fabricate fake 40h capacity! Record error explicitly.
          errorsMap[emp.id] = err?.message || "Không thể tải dữ liệu";
        }
      })
    );

    setCapacities((prev) => {
      const next = { ...prev };
      Object.keys(errorsMap).forEach((idStr) => {
        delete next[Number(idStr)];
      });
      return { ...next, ...resultsMap };
    });
    setCapacityErrors((prev) => {
      const next = { ...prev };
      Object.keys(resultsMap).forEach((idStr) => {
        delete next[Number(idStr)];
      });
      return { ...next, ...errorsMap };
    });
    setIsLoadingCapacities(false);
  }, [visibleEmployees, selectedYear, selectedWeek]);

  useEffect(() => {
    fetchCapacities();
  }, [fetchCapacities]);

  const handleOpenDeclareModal = (employeeId?: number) => {
    setModalTargetEmployeeId(employeeId || selectedEmployeeId || (employees[0]?.id ?? 1));
    setIsModalOpen(true);
  };

  const handleDeclarationSuccess = (result: WeeklyAvailabilityResult) => {
    showNotification("success", `Khai báo giờ chuẩn tuần ${result.weekNumber}/${result.year} thành công!`);
    setCapacities((prev) => ({
      ...prev,
      [result.employeeId]: result,
    }));
    setCapacityErrors((prev) => {
      const next = { ...prev };
      delete next[result.employeeId];
      return next;
    });
  };

  const activeEmployee = employees.find((e) => e.id === selectedEmployeeId);
  const activeCapacity = selectedEmployeeId ? capacities[selectedEmployeeId] : null;

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-extrabold tracking-tight text-slate-900">
              Khai báo &amp; Năng lực khả dụng theo tuần
            </h1>
            <span className="inline-flex items-center gap-1 rounded-md bg-indigo-50 px-2 py-0.5 text-[10px] font-bold text-indigo-700 border border-indigo-200/60">
              <Sparkles className="size-3" /> NCL-02-CN-003 &amp; QTN-10
            </span>
          </div>
          <p className="mt-1 text-xs font-semibold text-slate-500 sm:text-sm">
            Quản lý giờ chuẩn tuần và tự động tính toán năng lực khả dụng: Giờ chuẩn − Giờ lễ − Nghỉ phép đã duyệt.
          </p>
        </div>

        {canDeclare && (
          <button
            type="button"
            onClick={() => handleOpenDeclareModal()}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-xs font-bold text-white shadow-sm transition hover:bg-indigo-700 active:scale-95"
          >
            <Plus className="size-4" />
            <span>Khai báo giờ tuần</span>
          </button>
        )}
      </div>

      {/* Notification Toast */}
      {notification && (
        <div
          className={`flex items-center justify-between rounded-2xl border p-4 text-xs font-semibold shadow-xs animate-in fade-in duration-150 ${
            notification.type === "success"
              ? "border-emerald-200 bg-emerald-50 text-emerald-800"
              : "border-rose-200 bg-rose-50 text-rose-800"
          }`}
        >
          <div className="flex items-center gap-2.5">
            <CheckCircle2 className="size-4 shrink-0 text-emerald-600" />
            <span>{notification.message}</span>
          </div>
          <button
            type="button"
            onClick={() => setNotification(null)}
            className="rounded-lg p-1 hover:bg-emerald-100"
          >
            ✕
          </button>
        </div>
      )}

      {/* Week Filter Bar */}
      <div className="flex flex-col gap-3 rounded-2xl border border-slate-200/90 bg-white p-4 shadow-xs sm:flex-row sm:items-center sm:justify-between">
        {/* Week Navigator */}
        <div className="flex items-center gap-2">
          <div className="flex items-center rounded-xl border border-slate-200 bg-slate-50/70 p-1">
            <button
              type="button"
              onClick={() => handleNavigateWeek(-1)}
              className="rounded-lg p-1.5 text-slate-500 transition hover:bg-white hover:text-slate-900 shadow-2xs"
              title="Tuần trước"
            >
              <ChevronLeft className="size-4" />
            </button>
            <span className="px-3 text-xs font-bold text-indigo-700">
              Tuần {selectedWeek} • Năm {selectedYear}
            </span>
            <button
              type="button"
              onClick={() => handleNavigateWeek(1)}
              className="rounded-lg p-1.5 text-slate-500 transition hover:bg-white hover:text-slate-900 shadow-2xs"
              title="Tuần sau"
            >
              <ChevronRight className="size-4" />
            </button>
          </div>

          <button
            type="button"
            onClick={handleResetToCurrentWeek}
            className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-600 transition hover:bg-slate-50 hover:text-slate-900 shadow-2xs"
            title="Quay về tuần hiện tại"
          >
            <RotateCcw className="size-3.5 text-slate-400" />
            <span>Hiện tại</span>
          </button>

          <span className="hidden text-xs font-medium text-slate-500 lg:inline">
            {weekDateRange}
          </span>
        </div>

        {/* Search Input */}
        <div className="relative min-w-[240px]">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Tìm theo tên, mã, phòng ban..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full rounded-xl border border-slate-200 bg-slate-50/70 pl-9 pr-3.5 py-2 text-xs font-semibold text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
          />
        </div>
      </div>

      {/* Selected Employee Capacity Highlight */}
      {activeEmployee && (
        <CapacitySummaryCard
          capacity={activeCapacity}
          errorMessage={selectedEmployeeId ? capacityErrors[selectedEmployeeId] : null}
          employeeName={`${activeEmployee.fullName} (${activeEmployee.employeeCode})`}
          weekLabel={`Tuần ${selectedWeek}/${selectedYear}`}
        />
      )}

      {/* Main Table: Employees & Weekly Capacity Breakdown */}
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xs">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-3.5 bg-slate-50/60">
          <div className="flex items-center gap-2">
            <Users className="size-4 text-indigo-600" />
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-700">
              Danh sách năng lực khả dụng ({visibleEmployees.length} nhân sự)
            </h3>
          </div>
          {isLoadingCapacities && (
            <div className="flex items-center gap-1.5 text-xs text-indigo-600 font-semibold">
              <Loader2 className="size-3.5 animate-spin" />
              <span>Đang tính toán QTN-10...</span>
            </div>
          )}
        </div>

        {isLoadingEmployees ? (
          <div className="flex flex-col items-center justify-center p-12 text-slate-400">
            <Loader2 className="size-7 animate-spin text-indigo-600 mb-2" />
            <p className="text-xs font-semibold">Đang tải danh sách nhân sự...</p>
          </div>
        ) : visibleEmployees.length === 0 ? (
          <div className="flex flex-col items-center justify-center p-12 text-slate-400">
            <AlertCircle className="size-8 text-slate-300 mb-2" />
            <p className="text-xs font-semibold">Không tìm thấy nhân sự phù hợp.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/70 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                  <th className="px-5 py-3">Nhân sự</th>
                  <th className="px-4 py-3">Phòng ban</th>
                  <th className="px-4 py-3 text-center">Giờ chuẩn tuần</th>
                  <th className="px-4 py-3 text-center">Lễ trừ</th>
                  <th className="px-4 py-3 text-center">Phép duyệt trừ</th>
                  <th className="px-4 py-3 text-center">Khả dụng ròng</th>
                  <th className="px-4 py-3 text-center">Tỷ lệ</th>
                  {canDeclare && <th className="px-5 py-3 text-right">Thao tác</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                {visibleEmployees.map((emp) => {
                  const cap = capacities[emp.id];
                  const err = capacityErrors[emp.id];
                  const hasData = Boolean(cap && !err);
                  const standard = cap?.standardHours ?? 0;
                  const holiday = cap?.holidayHours ?? 0;
                  const leave = Number(cap?.approvedLeaveHours ?? 0);
                  const net = Number(cap?.netAvailableHours ?? 0);
                  const netPct = standard > 0 ? Math.max(0, Math.min(100, Math.round((net / standard) * 100))) : 0;
                  const isSelected = emp.id === selectedEmployeeId;

                  return (
                    <tr
                      key={emp.id}
                      onClick={() => setSelectedEmployeeId(emp.id)}
                      className={`cursor-pointer transition hover:bg-indigo-50/40 ${
                        isSelected ? "bg-indigo-50/60 font-semibold" : ""
                      }`}
                    >
                      {/* Name & Code */}
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-2.5">
                          <div className="flex size-7 shrink-0 items-center justify-center rounded-lg bg-indigo-100 text-indigo-700 text-[11px] font-bold">
                            {emp.fullName.charAt(0)}
                          </div>
                          <div>
                            <p className="font-bold text-slate-900 hover:text-indigo-600 transition">
                              {emp.fullName}
                            </p>
                            <p className="font-mono text-[10px] text-slate-400">{emp.employeeCode}</p>
                          </div>
                        </div>
                      </td>

                      {/* Department */}
                      <td className="px-4 py-3.5 text-slate-600">
                        <span className="inline-flex items-center gap-1">
                          <Building2 className="size-3 text-slate-400" />
                          <span>{emp.orgUnitName || "Chưa gán"}</span>
                        </span>
                      </td>

                      {/* Standard Hours */}
                      <td className="px-4 py-3.5 text-center font-bold text-slate-800">
                        {hasData ? `${standard}h` : "--"}
                      </td>

                      {/* Holiday Deducted */}
                      <td className="px-4 py-3.5 text-center font-semibold text-amber-700">
                        {hasData ? (holiday > 0 ? `-${holiday}h` : "0h") : "--"}
                      </td>

                      {/* Approved Leave Deducted */}
                      <td className="px-4 py-3.5 text-center font-semibold text-rose-700">
                        {hasData ? (leave > 0 ? `-${leave}h` : "0h") : "--"}
                      </td>

                      {/* Net Available */}
                      <td className="px-4 py-3.5 text-center">
                        {err ? (
                          <span
                            className="inline-flex items-center gap-1 rounded-full bg-rose-50 px-2.5 py-0.5 text-[11px] font-bold text-rose-700 border border-rose-200"
                            title={err}
                          >
                            <AlertCircle className="size-3 text-rose-500" />
                            <span>Không tải được dữ liệu</span>
                          </span>
                        ) : hasData ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-extrabold text-emerald-700 border border-emerald-100">
                            {net}h
                          </span>
                        ) : (
                          <span className="text-slate-400 font-normal">--</span>
                        )}
                      </td>

                      {/* Percentage Bar */}
                      <td className="px-4 py-3.5">
                        {hasData ? (
                          <div className="w-24 mx-auto">
                            <div className="flex items-center justify-between text-[10px] font-bold mb-1">
                              <span className={netPct > 60 ? "text-emerald-700" : netPct > 30 ? "text-amber-700" : "text-rose-700"}>
                                {netPct}%
                              </span>
                            </div>
                            <div className="h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
                              <div
                                className={`h-full rounded-full ${
                                  netPct > 60
                                    ? "bg-emerald-500"
                                    : netPct > 30
                                    ? "bg-amber-500"
                                    : "bg-rose-500"
                                }`}
                                style={{ width: `${netPct}%` }}
                              />
                            </div>
                          </div>
                        ) : (
                          <div className="text-center text-slate-400">--</div>
                        )}
                      </td>

                      {/* Action */}
                      {canDeclare && (
                        <td className="px-5 py-3.5 text-right">
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              handleOpenDeclareModal(emp.id);
                            }}
                            className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-[11px] font-bold text-indigo-600 transition hover:border-indigo-300 hover:bg-indigo-50 shadow-2xs"
                          >
                            <Clock className="size-3" />
                            <span>Khai báo</span>
                          </button>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal Khai Báo */}
      <DeclareAvailabilityModal
        open={isModalOpen}
        employees={employees}
        initialEmployeeId={modalTargetEmployeeId}
        initialYear={selectedYear}
        initialWeekNumber={selectedWeek}
        initialStandardHours={
          modalTargetEmployeeId && capacities[modalTargetEmployeeId]
            ? capacities[modalTargetEmployeeId].standardHours
            : 40
        }
        onClose={() => setIsModalOpen(false)}
        onSuccess={handleDeclarationSuccess}
      />
    </div>
  );
}
