import React, { useState, useEffect, useMemo, useCallback } from "react";
import {
  CalendarDays,
  CalendarCheck2,
  Plus,
  Edit2,
  Trash2,
  AlertCircle,
  CheckCircle2,
  Check,
  Clock,
  Loader2,
  X,
  RotateCcw,
  Building2,
  Building,
  ArrowRightLeft,
  Info,
  Sliders,
  Sparkles,
} from "lucide-react";
import { useAuthUser } from "@/lib/auth-session";
import {
  getHolidays,
  createHoliday,
  updateHoliday,
  deleteHoliday,
  type DayOfWeek,
  type Holiday,
} from "@/lib/api/working-calendar";
import {
  getStandardWorkWeekConfig,
  updateStandardWorkWeekConfig,
  convertCapacity,
  type CapacityUnit,
  type WeekStartDay,
  type StandardWorkWeekDay,
  type StandardWorkWeekConfig,
} from "@/lib/api/standard-work-week";
import { getOrgTree } from "@/lib/api/org-units";
import type { OrgUnitTreeNode } from "@/types/hrm";
import { ApiError } from "@/lib/api-client";

const DAY_LABELS: Record<DayOfWeek, { label: string; short: string }> = {
  MONDAY: { label: "Thứ Hai", short: "T2" },
  TUESDAY: { label: "Thứ Ba", short: "T3" },
  WEDNESDAY: { label: "Thứ Tư", short: "T4" },
  THURSDAY: { label: "Thứ Năm", short: "T5" },
  FRIDAY: { label: "Thứ Sáu", short: "T6" },
  SATURDAY: { label: "Thứ Bảy", short: "T7" },
  SUNDAY: { label: "Chủ Nhật", short: "CN" },
};

const ORDER_MONDAY_START: DayOfWeek[] = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

const ORDER_SUNDAY_START: DayOfWeek[] = [
  "SUNDAY",
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
];

const DEFAULT_STANDARD_DAYS: StandardWorkWeekDay[] = [
  { dayOfWeek: "MONDAY", isWorkingDay: true, workingHours: 8 },
  { dayOfWeek: "TUESDAY", isWorkingDay: true, workingHours: 8 },
  { dayOfWeek: "WEDNESDAY", isWorkingDay: true, workingHours: 8 },
  { dayOfWeek: "THURSDAY", isWorkingDay: true, workingHours: 8 },
  { dayOfWeek: "FRIDAY", isWorkingDay: true, workingHours: 8 },
  { dayOfWeek: "SATURDAY", isWorkingDay: false, workingHours: 0 },
  { dayOfWeek: "SUNDAY", isWorkingDay: false, workingHours: 0 },
];

interface FlatOrgUnit {
  id: number;
  unitName: string;
  unitCode: string;
}

function flattenOrgTree(nodes: readonly OrgUnitTreeNode[]): FlatOrgUnit[] {
  const result: FlatOrgUnit[] = [];
  function traverse(list: readonly OrgUnitTreeNode[]) {
    for (const node of list) {
      result.push({ id: node.id, unitName: node.unitName, unitCode: node.unitCode });
      if (node.children && node.children.length > 0) {
        traverse(node.children);
      }
    }
  }
  traverse(nodes);
  return result;
}

export default function WorkingCalendarConfigView() {
  const currentUser = useAuthUser();
  const roleCode = currentUser?.roleCode?.toUpperCase().replace(/_/g, "-") || "";
  const isHR = roleCode === "VT-05";
  const isAdmin = roleCode === "VT-06";
  const canManage = isHR || isAdmin;

  // Active top-level tab
  const [activeTab, setActiveTab] = useState<"work-week" | "holidays">("work-week");

  // Notification Banner
  const [notification, setNotification] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  const showNotification = (type: "success" | "error", message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 5000);
  };

  // =========================================================
  // 1. STANDARD WORK WEEK & UNIT CONFIG STATE
  // =========================================================
  const [scopeType, setScopeType] = useState<"COMPANY" | "ORG_UNIT">("COMPANY");
  const [orgUnits, setOrgUnits] = useState<FlatOrgUnit[]>([]);
  const [selectedOrgUnitId, setSelectedOrgUnitId] = useState<number | null>(null);

  const [config, setConfig] = useState<StandardWorkWeekConfig | null>(null);
  const [initialConfig, setInitialConfig] = useState<StandardWorkWeekConfig | null>(null);
  const [workDays, setWorkDays] = useState<StandardWorkWeekDay[]>(DEFAULT_STANDARD_DAYS);
  const [capacityUnit, setCapacityUnit] = useState<CapacityUnit>("HOURS");
  const [weekStartDay, setWeekStartDay] = useState<WeekStartDay>("MONDAY");
  const [standardHoursPerDay, setStandardHoursPerDay] = useState<number>(8);

  const [isLoadingConfig, setIsLoadingConfig] = useState<boolean>(true);
  const [isSavingConfig, setIsSavingConfig] = useState<boolean>(false);
  const [configError, setConfigError] = useState<string | null>(null);
  const [versionConflict, setVersionConflict] = useState<string | null>(null);

  // Quick Converter Modal / State
  const [converterValue, setConverterValue] = useState<number>(40);
  const [converterFrom, setConverterFrom] = useState<CapacityUnit>("HOURS");
  const [converterTo, setConverterTo] = useState<CapacityUnit>("FTE");
  const [conversionResult, setConversionResult] = useState<string | null>(null);
  const [isConverting, setIsConverting] = useState<boolean>(false);

  // Load Org Units for dropdown
  useEffect(() => {
    async function loadOrgs() {
      try {
        const tree = await getOrgTree();
        const flat = flattenOrgTree(tree);
        setOrgUnits(flat);
        if (flat.length > 0 && selectedOrgUnitId === null) {
          setSelectedOrgUnitId(flat[0].id);
        }
      } catch {
        // Fallback silently if org tree fails
      }
    }
    loadOrgs();
  }, [selectedOrgUnitId]);

  // Fetch Work Week Config
  const fetchWorkWeekConfig = useCallback(async () => {
    setIsLoadingConfig(true);
    setConfigError(null);
    setVersionConflict(null);
    try {
      const res = await getStandardWorkWeekConfig(
        scopeType,
        scopeType === "ORG_UNIT" ? selectedOrgUnitId : null
      );
      setConfig(res);
      setInitialConfig(res);
      setCapacityUnit(res.capacityUnit);
      setWeekStartDay(res.weekStartDay);
      setStandardHoursPerDay(res.standardHoursPerDay);

      // Sort days according to weekStartDay
      const dayOrder = res.weekStartDay === "SUNDAY" ? ORDER_SUNDAY_START : ORDER_MONDAY_START;
      const sortedDays = [...(res.days || [])].sort(
        (a, b) => dayOrder.indexOf(a.dayOfWeek) - dayOrder.indexOf(b.dayOfWeek)
      );
      setWorkDays(sortedDays.length > 0 ? sortedDays : DEFAULT_STANDARD_DAYS);
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : "Không thể tải cấu hình tuần làm việc từ máy chủ";
      setConfigError(msg);
      showNotification("error", msg);
      setWorkDays((prev) => (prev.length > 0 ? prev : DEFAULT_STANDARD_DAYS));
    } finally {
      setIsLoadingConfig(false);
    }
  }, [scopeType, selectedOrgUnitId]);

  const keepLocalChangesOnLatestVersion = useCallback(async () => {
    setIsLoadingConfig(true);
    setConfigError(null);
    try {
      const latest = await getStandardWorkWeekConfig(
        scopeType,
        scopeType === "ORG_UNIT" ? selectedOrgUnitId : null
      );
      // Refresh only the concurrency baseline; form fields intentionally remain untouched.
      setConfig(latest);
      setInitialConfig(latest);
      setVersionConflict(null);
      showNotification("success", "Đã tải phiên bản mới nhất và giữ lại các thay đổi của bạn.");
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : "Không thể tải phiên bản cấu hình mới nhất";
      setConfigError(msg);
      showNotification("error", msg);
    } finally {
      setIsLoadingConfig(false);
    }
  }, [scopeType, selectedOrgUnitId]);

  useEffect(() => {
    if (activeTab === "work-week") {
      fetchWorkWeekConfig();
    }
  }, [activeTab, fetchWorkWeekConfig]);

  // Re-sort days if weekStartDay changes
  const sortedWorkDays = useMemo(() => {
    const dayOrder = weekStartDay === "SUNDAY" ? ORDER_SUNDAY_START : ORDER_MONDAY_START;
    return [...workDays].sort(
      (a, b) => dayOrder.indexOf(a.dayOfWeek) - dayOrder.indexOf(b.dayOfWeek)
    );
  }, [workDays, weekStartDay]);

  // Working days count
  const workingDaysCount = useMemo(
    () => workDays.filter((d) => d.isWorkingDay).length,
    [workDays]
  );

  // Total standard hours per week computed dynamically
  const computedHoursPerWeek = useMemo(() => {
    return workDays
      .filter((d) => d.isWorkingDay)
      .reduce((sum, d) => sum + (Number(d.workingHours) || 0), 0);
  }, [workDays]);

  // Check if work week has changes
  const hasWorkWeekChanges = useMemo(() => {
    if (!initialConfig) return true;
    if (capacityUnit !== initialConfig.capacityUnit) return true;
    if (weekStartDay !== initialConfig.weekStartDay) return true;
    if (Number(standardHoursPerDay) !== Number(initialConfig.standardHoursPerDay)) return true;

    return workDays.some((d) => {
      const initDay = initialConfig.days?.find((id) => id.dayOfWeek === d.dayOfWeek);
      if (!initDay) return true;
      return (
        initDay.isWorkingDay !== d.isWorkingDay ||
        Number(initDay.workingHours) !== Number(d.workingHours)
      );
    });
  }, [initialConfig, capacityUnit, weekStartDay, standardHoursPerDay, workDays]);

  // Day toggle
  const handleToggleDay = (dayOfWeek: DayOfWeek) => {
    if (!canManage) return;
    setWorkDays((prev) =>
      prev.map((d) => {
        if (d.dayOfWeek === dayOfWeek) {
          const nextWorking = !d.isWorkingDay;
          return {
            ...d,
            isWorkingDay: nextWorking,
            workingHours: nextWorking ? standardHoursPerDay || 8 : 0,
          };
        }
        return d;
      })
    );
  };

  // Day hours change
  const handleDayHoursChange = (dayOfWeek: DayOfWeek, hours: number) => {
    if (!canManage) return;
    setWorkDays((prev) =>
      prev.map((d) => {
        if (d.dayOfWeek === dayOfWeek) {
          return { ...d, workingHours: hours };
        }
        return d;
      })
    );
  };

  // Quick apply standard hours to all active working days
  const handleApplyHoursToWorkingDays = () => {
    if (!canManage) return;
    const hours = Number(standardHoursPerDay) || 8;
    setWorkDays((prev) =>
      prev.map((d) => (d.isWorkingDay ? { ...d, workingHours: hours } : d))
    );
    showNotification("success", `Đã gán ${hours} giờ cho tất cả các ngày làm việc!`);
  };

  // Reset to default 40h standard
  const handleResetToStandard40h = () => {
    if (!canManage) return;
    setCapacityUnit("HOURS");
    setWeekStartDay("MONDAY");
    setStandardHoursPerDay(8);
    setWorkDays((prev) =>
      prev.map((d) => {
        const isWeekday =
          d.dayOfWeek !== "SATURDAY" && d.dayOfWeek !== "SUNDAY";
        return {
          ...d,
          isWorkingDay: isWeekday,
          workingHours: isWeekday ? 8 : 0,
        };
      })
    );
    showNotification("success", "Đã khôi phục cài đặt tuần chuẩn 40h (T2-T6: 8h, T7-CN: Nghỉ)!");
  };

  // Reset to initial loaded config
  const handleUndoChanges = () => {
    if (!initialConfig) return;
    setCapacityUnit(initialConfig.capacityUnit);
    setWeekStartDay(initialConfig.weekStartDay);
    setStandardHoursPerDay(initialConfig.standardHoursPerDay);
    setWorkDays(initialConfig.days);
  };

  // Save Standard Work Week Config
  const handleSaveWorkWeekConfig = async () => {
    if (!canManage) return;
    if (workingDaysCount === 0) {
      showNotification("error", "Tuần làm việc phải có ít nhất 1 ngày làm việc!");
      return;
    }
    if (computedHoursPerWeek < 4 || computedHoursPerWeek > 84) {
      showNotification("error", "Tổng số giờ làm việc chuẩn mỗi tuần phải từ 4.0 đến 84.0 giờ!");
      return;
    }

    setIsSavingConfig(true);
    try {
      const payload = {
        scopeType,
        orgUnitId: scopeType === "ORG_UNIT" ? selectedOrgUnitId : null,
        capacityUnit,
        weekStartDay,
        standardHoursPerDay: Number(standardHoursPerDay),
        // Send the loaded snapshot version so the API can reject stale admin edits.
        version: initialConfig?.isInherited ? undefined : initialConfig?.version,
        days: workDays.map((d) => ({
          dayOfWeek: d.dayOfWeek,
          isWorkingDay: d.isWorkingDay,
          workingHours: d.isWorkingDay ? Number(d.workingHours) : 0,
        })),
      };

      const res = await updateStandardWorkWeekConfig(payload);
      setConfig(res);
      setInitialConfig(res);
      setVersionConflict(null);
      showNotification(
        "success",
        `Lưu cấu hình tuần chuẩn thành công! Tổng giờ: ${res.standardHoursPerWeek}h/tuần.`
      );
    } catch (err: unknown) {
      const errorCode = err instanceof ApiError && err.data && typeof err.data === "object"
        ? (err.data as { code?: unknown }).code
        : undefined;
      if (
        err instanceof ApiError &&
        err.status === 409 &&
        errorCode === "STANDARD_WORK_WEEK_VERSION_CONFLICT"
      ) {
        // Keep the local form untouched until the user explicitly chooses which version to keep.
        setVersionConflict(
          "Cấu hình này vừa được người khác cập nhật. Các thay đổi chưa lưu của bạn vẫn được giữ lại."
        );
        return;
      }
      const msg = err instanceof ApiError ? err.message : "Không thể lưu cấu hình tuần làm việc chuẩn";
      showNotification("error", msg);
    } finally {
      setIsSavingConfig(false);
    }
  };

  // Handle Quick Capacity Conversion
  const handleQuickConvert = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!converterValue || converterValue <= 0) return;
    setIsConverting(true);
    try {
      const res = await convertCapacity({
        value: Number(converterValue),
        fromUnit: converterFrom,
        toUnit: converterTo,
        scopeType,
        orgUnitId: scopeType === "ORG_UNIT" ? selectedOrgUnitId : null,
      });
      setConversionResult(
        `${res.originalValue} ${converterFrom} = ${res.convertedValue} ${converterTo} (${res.formulaDescription})`
      );
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : "Lỗi khi quy đổi";
      showNotification("error", msg);
    } finally {
      setIsConverting(false);
    }
  };

  // =========================================================
  // 2. HOLIDAYS STATE & ACTIONS
  // =========================================================
  const currentYear = new Date().getFullYear();
  const [selectedYear, setSelectedYear] = useState<number>(currentYear);
  const [holidays, setHolidays] = useState<Holiday[]>([]);
  const [isLoadingHolidays, setIsLoadingHolidays] = useState<boolean>(true);

  // Holiday Modal State
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [editingHoliday, setEditingHoliday] = useState<Holiday | null>(null);
  const [holidayDate, setHolidayDate] = useState<string>("");
  const [holidayName, setHolidayName] = useState<string>("");
  const [workingHoursDeducted, setWorkingHoursDeducted] = useState<number>(8);
  const [modalError, setModalError] = useState<string | null>(null);
  const [isSubmittingHoliday, setIsSubmittingHoliday] = useState<boolean>(false);

  // Delete Confirmation Modal State
  const [deletingHoliday, setDeletingHoliday] = useState<Holiday | null>(null);
  const [isDeleting, setIsDeleting] = useState<boolean>(false);

  const fetchHolidaysData = useCallback(async (year: number) => {
    setIsLoadingHolidays(true);
    try {
      const res = await getHolidays(year);
      setHolidays(res || []);
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : "Không thể tải danh sách ngày lễ";
      showNotification("error", msg);
    } finally {
      setIsLoadingHolidays(false);
    }
  }, []);

  useEffect(() => {
    if (activeTab === "holidays") {
      fetchHolidaysData(selectedYear);
    }
  }, [activeTab, selectedYear, fetchHolidaysData]);

  const handleOpenAddHoliday = () => {
    setEditingHoliday(null);
    const today = new Date();
    const defaultMonth = String(today.getMonth() + 1).padStart(2, "0");
    const defaultDay = String(today.getDate()).padStart(2, "0");
    setHolidayDate(`${selectedYear}-${defaultMonth}-${defaultDay}`);
    setHolidayName("");
    setWorkingHoursDeducted(8);
    setModalError(null);
    setIsModalOpen(true);
  };

  const handleOpenEditHoliday = (holiday: Holiday) => {
    setEditingHoliday(holiday);
    setHolidayDate(holiday.holidayDate);
    setHolidayName(holiday.name);
    setWorkingHoursDeducted(holiday.workingHoursDeducted);
    setModalError(null);
    setIsModalOpen(true);
  };

  const handleSubmitHoliday = async (e: React.FormEvent) => {
    e.preventDefault();
    setModalError(null);

    if (!holidayDate) {
      setModalError("Vui lòng chọn ngày nghỉ lễ");
      return;
    }
    if (!holidayName.trim()) {
      setModalError("Vui lòng nhập tên ngày nghỉ lễ");
      return;
    }
    if (workingHoursDeducted < 1 || workingHoursDeducted > 24) {
      setModalError("Số giờ khấu trừ phải từ 1 đến 24 giờ");
      return;
    }

    setIsSubmittingHoliday(true);
    try {
      if (editingHoliday) {
        await updateHoliday(editingHoliday.id, {
          holidayDate,
          name: holidayName.trim(),
          workingHoursDeducted,
        });
        showNotification("success", `Cập nhật ngày lễ "${holidayName.trim()}" thành công!`);
      } else {
        await createHoliday({
          holidayDate,
          name: holidayName.trim(),
          workingHoursDeducted,
        });
        showNotification("success", `Thêm ngày lễ "${holidayName.trim()}" thành công!`);
      }
      setIsModalOpen(false);
      fetchHolidaysData(selectedYear);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setModalError(`Ngày nghỉ lễ ${holidayDate} đã tồn tại trong hệ thống! Vui lòng chọn ngày khác.`);
        } else {
          setModalError(err.message || "Đã xảy ra lỗi khi lưu ngày nghỉ lễ.");
        }
      } else {
        setModalError("Đã xảy ra lỗi không xác định khi lưu ngày nghỉ lễ.");
      }
    } finally {
      setIsSubmittingHoliday(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!deletingHoliday) return;
    setIsDeleting(true);
    try {
      await deleteHoliday(deletingHoliday.id);
      showNotification("success", `Đã xóa ngày lễ "${deletingHoliday.name}" thành công!`);
      setDeletingHoliday(null);
      fetchHolidaysData(selectedYear);
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : "Không thể xóa ngày nghỉ lễ";
      showNotification("error", msg);
    } finally {
      setIsDeleting(false);
    }
  };

  const getDayNameFromDateStr = (dateStr: string) => {
    try {
      const [y, m, d] = dateStr.split("-").map(Number);
      const date = new Date(y, m - 1, d);
      const dayIndex = date.getDay();
      const map: Record<number, string> = {
        0: "Chủ Nhật",
        1: "Thứ Hai",
        2: "Thứ Ba",
        3: "Thứ Tư",
        4: "Thứ Năm",
        5: "Thứ Sáu",
        6: "Thứ Bảy",
      };
      return map[dayIndex] || "";
    } catch {
      return "";
    }
  };

  const formatDateDisplay = (dateStr: string) => {
    try {
      const [y, m, d] = dateStr.split("-");
      return `${d}/${m}/${y}`;
    } catch {
      return dateStr;
    }
  };

  const totalHolidaysCount = holidays.length;
  const totalDeductedHours = useMemo(
    () => holidays.reduce((sum, h) => sum + (h.workingHoursDeducted || 0), 0),
    [holidays]
  );

  return (
    <div className="space-y-6 pb-12">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-slate-900">
            Cấu hình Đơn vị & Tuần làm việc chuẩn
          </h1>
          <p className="mt-1 text-xs font-semibold text-slate-500 sm:text-sm">
            Thiết lập đơn vị đo lường năng lực, tuần làm việc chuẩn và quản lý danh mục ngày nghỉ lễ toàn đơn vị.
          </p>
        </div>

        {/* Tab switchers */}
        <div className="flex items-center gap-1.5 p-1 bg-slate-100 rounded-xl border border-slate-200">
          <button
            type="button"
            onClick={() => setActiveTab("work-week")}
            className={`flex items-center gap-2 px-3.5 py-1.5 text-xs font-bold rounded-lg transition-all ${
              activeTab === "work-week"
                ? "bg-white text-indigo-600 shadow-xs"
                : "text-slate-600 hover:text-slate-900"
            }`}
          >
            <CalendarDays className="h-4 w-4" />
            Tuần làm việc & Đơn vị
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("holidays")}
            className={`flex items-center gap-2 px-3.5 py-1.5 text-xs font-bold rounded-lg transition-all ${
              activeTab === "holidays"
                ? "bg-white text-indigo-600 shadow-xs"
                : "text-slate-600 hover:text-slate-900"
            }`}
          >
            <CalendarCheck2 className="h-4 w-4" />
            Ngày nghỉ lễ
          </button>
        </div>
      </div>

      {/* Notification Banner */}
      {notification && (
        <div
          className={`flex items-center justify-between p-4 rounded-xl border text-sm font-medium animate-in fade-in duration-200 ${
            notification.type === "success"
              ? "bg-emerald-50 border-emerald-200 text-emerald-800"
              : "bg-rose-50 border-rose-200 text-rose-800"
          }`}
        >
          <div className="flex items-center gap-2.5">
            {notification.type === "success" ? (
              <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0" />
            ) : (
              <AlertCircle className="h-5 w-5 text-rose-600 shrink-0" />
            )}
            <span>{notification.message}</span>
          </div>
          <button
            onClick={() => setNotification(null)}
            className="text-slate-400 hover:text-slate-600 p-1"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {/* ========================================================= */}
      {/* TAB 1: TUẦN LÀM VIỆC & ĐƠN VỊ CHUẨN                       */}
      {/* ========================================================= */}
      {activeTab === "work-week" && (
        <div className="space-y-6">
          {versionConflict && (
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 rounded-2xl border border-amber-300 bg-amber-50 p-4 text-amber-950 shadow-xs">
              <div className="flex items-start gap-2.5">
                <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" />
                <div>
                  <p className="text-sm font-bold">Phát hiện thay đổi từ người dùng khác</p>
                  <p className="mt-1 text-xs text-amber-800">{versionConflict}</p>
                </div>
              </div>
              <div className="flex shrink-0 flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => void keepLocalChangesOnLatestVersion()}
                  className="rounded-xl border border-amber-300 bg-white px-3.5 py-2 text-xs font-bold text-amber-900 transition hover:bg-amber-100"
                >
                  Giữ thay đổi của tôi
                </button>
                <button
                  type="button"
                  onClick={() => void fetchWorkWeekConfig()}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-amber-700 px-3.5 py-2 text-xs font-bold text-white transition hover:bg-amber-800"
                >
                  <RotateCcw className="h-3.5 w-3.5" />
                  Tải bản mới &amp; bỏ thay đổi
                </button>
              </div>
            </div>
          )}

          {/* Scope Selector Bar */}
          <div className="bg-white rounded-2xl border border-slate-200 p-5 sm:p-6 shadow-xs">
            <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
                  <Building2 className="h-5 w-5" />
                </div>
                <div>
                  <h2 className="text-sm sm:text-base font-bold text-slate-900">
                    Phạm vi áp dụng cấu hình
                  </h2>
                  <p className="text-xs text-slate-500">
                    Chọn cấu hình cho toàn công ty hoặc thiết lập đặc thù riêng cho từng đơn vị phòng ban.
                  </p>
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-3">
                {/* Scope Toggle */}
                <div className="inline-flex p-1 bg-slate-100 rounded-xl border border-slate-200">
                  <button
                    type="button"
                    onClick={() => setScopeType("COMPANY")}
                    className={`flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold rounded-lg transition ${
                      scopeType === "COMPANY"
                        ? "bg-white text-indigo-600 shadow-xs"
                        : "text-slate-600 hover:text-slate-900"
                    }`}
                  >
                    <Building className="h-3.5 w-3.5" />
                    Toàn công ty
                  </button>
                  <button
                    type="button"
                    onClick={() => setScopeType("ORG_UNIT")}
                    className={`flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold rounded-lg transition ${
                      scopeType === "ORG_UNIT"
                        ? "bg-white text-indigo-600 shadow-xs"
                        : "text-slate-600 hover:text-slate-900"
                    }`}
                  >
                    <Building2 className="h-3.5 w-3.5" />
                    Theo đơn vị
                  </button>
                </div>

                {/* Org Unit Selector dropdown if scope is ORG_UNIT */}
                {scopeType === "ORG_UNIT" && (
                  <div className="flex items-center gap-2 bg-slate-50 px-3 py-1.5 rounded-xl border border-slate-200">
                    <span className="text-xs font-medium text-slate-500">Đơn vị:</span>
                    <select
                      value={selectedOrgUnitId || ""}
                      onChange={(e) => setSelectedOrgUnitId(Number(e.target.value))}
                      className="bg-transparent text-xs font-bold text-slate-800 border-none outline-hidden cursor-pointer max-w-56"
                    >
                      {orgUnits.map((u) => (
                        <option key={u.id} value={u.id}>
                          {u.unitName} ({u.unitCode})
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {/* Status Inheritance Badge */}
                {config && (
                  <div className="flex items-center gap-1.5">
                    {config.isInherited ? (
                      <span className="inline-flex items-center gap-1 text-[11px] font-semibold px-2.5 py-1 rounded-lg bg-amber-50 text-amber-700 border border-amber-200">
                        <Info className="h-3.5 w-3.5 text-amber-500" />
                        Đang kế thừa từ Toàn công ty
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-[11px] font-semibold px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200">
                        <Check className="h-3.5 w-3.5 text-emerald-500" />
                        Cấu hình riêng của đơn vị
                      </span>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>

          {isLoadingConfig ? (
            <div className="flex items-center justify-center py-16 text-slate-400 text-sm bg-white rounded-2xl border border-slate-200">
              <Loader2 className="h-5 w-5 animate-spin mr-2 text-indigo-600" />
              Đang tải cấu hình tuần làm việc chuẩn...
            </div>
          ) : (
            <>
              {/* Server Connection Warning Banner if API call failed */}
              {configError && (
                <div className="flex flex-col sm:flex-row sm:items-center justify-between p-4 bg-amber-50/90 border border-amber-200 rounded-2xl gap-3 text-amber-900 text-xs shadow-xs">
                  <div className="flex items-start gap-2.5">
                    <AlertCircle className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
                    <div>
                      <p className="font-bold text-amber-900">
                        Chưa đồng bộ được với Backend ({configError})
                      </p>
                      <p className="text-amber-700 mt-0.5">
                        Hệ thống đang hiển thị định mức mặc định (40h/tuần). Vui lòng khởi động lại (Restart) Server.
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => fetchWorkWeekConfig()}
                    className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-white border border-amber-300 font-bold text-amber-800 hover:bg-amber-100/60 self-start sm:self-auto shrink-0 transition shadow-xs cursor-pointer"
                  >
                    <RotateCcw className="h-3.5 w-3.5" />
                    Thử tải lại
                  </button>
                </div>
              )}

              {/* Unit & Work Week Core Settings Card */}
              <div className="bg-white rounded-2xl border border-slate-200 p-5 sm:p-6 shadow-xs space-y-6">
                <div className="pb-4 border-b border-slate-100 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-indigo-50 text-indigo-600 rounded-xl">
                      <Sliders className="h-5 w-5" />
                    </div>
                    <div>
                      <h3 className="text-sm sm:text-base font-bold text-slate-900">
                        Định mức giờ chuẩn & Đơn vị đo lường
                      </h3>
                      <p className="text-xs text-slate-500">
                        Định nghĩa đơn vị tính toán phân bổ và quy đổi năng lực nguồn lực cho đơn vị.
                      </p>
                    </div>
                  </div>

                  {canManage && (
                    <button
                      type="button"
                      onClick={handleResetToStandard40h}
                      className="hidden sm:inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-slate-600 bg-slate-50 border border-slate-200 rounded-xl hover:bg-slate-100 transition shadow-xs"
                    >
                      <Sparkles className="h-3.5 w-3.5 text-indigo-600" />
                      Gợi ý chuẩn 40h/tuần
                    </button>
                  )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
                  {/* 1. Capacity Unit Selection */}
                  <div className="space-y-2">
                    <label className="text-xs font-bold text-slate-700 block">
                      Đơn vị đo lường năng lực chuẩn
                    </label>
                    <div className="grid grid-cols-3 gap-2">
                      {(["HOURS", "DAYS", "FTE"] as CapacityUnit[]).map((unit) => {
                        const isSelected = capacityUnit === unit;
                        const labelMap = {
                          HOURS: "Giờ (Hours)",
                          DAYS: "Ngày (Days)",
                          FTE: "FTE (%)",
                        };
                        return (
                          <button
                            type="button"
                            key={unit}
                            onClick={() => canManage && setCapacityUnit(unit)}
                            disabled={!canManage}
                            className={`p-2.5 rounded-xl border text-center transition select-none ${
                              isSelected
                                ? "bg-indigo-50 border-indigo-300 text-indigo-900 ring-2 ring-indigo-500/20 shadow-xs"
                                : "bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100/60"
                            } ${canManage ? "cursor-pointer" : "cursor-default"}`}
                          >
                            <span className="block text-xs font-bold">{unit}</span>
                            <span className="block text-[10px] text-slate-500 mt-0.5">
                              {labelMap[unit]}
                            </span>
                          </button>
                        );
                      })}
                    </div>
                    <span className="text-[11px] text-slate-500 block">
                      * Đơn vị mặc định hiển thị trên ma trận năng lực và phân bổ dự án.
                    </span>
                  </div>

                  {/* 2. Week Start Day */}
                  <div className="space-y-2">
                    <label className="text-xs font-bold text-slate-700 block">
                      Ngày bắt đầu tuần làm việc
                    </label>
                    <div className="grid grid-cols-2 gap-2">
                      {(["MONDAY", "SUNDAY"] as WeekStartDay[]).map((startDay) => {
                        const isSelected = weekStartDay === startDay;
                        return (
                          <button
                            type="button"
                            key={startDay}
                            onClick={() => canManage && setWeekStartDay(startDay)}
                            disabled={!canManage}
                            className={`p-2.5 rounded-xl border text-center transition select-none ${
                              isSelected
                                ? "bg-indigo-50 border-indigo-300 text-indigo-900 ring-2 ring-indigo-500/20 shadow-xs"
                                : "bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100/60"
                            } ${canManage ? "cursor-pointer" : "cursor-default"}`}
                          >
                            <span className="block text-xs font-bold">
                              {startDay === "MONDAY" ? "Thứ Hai" : "Chủ Nhật"}
                            </span>
                            <span className="block text-[10px] text-slate-500 mt-0.5">
                              {startDay === "MONDAY" ? "Chuẩn ISO-8601" : "Chuẩn quốc tế"}
                            </span>
                          </button>
                        );
                      })}
                    </div>
                    <span className="text-[11px] text-slate-500 block">
                      * Thứ tự ngày trên bảng năng lực tuần sẽ sắp xếp theo ngày này.
                    </span>
                  </div>

                  {/* 3. Standard Hours Per Day */}
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <label className="text-xs font-bold text-slate-700">
                        Giờ chuẩn mỗi ngày
                      </label>
                      {canManage && (
                        <button
                          type="button"
                          onClick={handleApplyHoursToWorkingDays}
                          className="text-[11px] font-semibold text-indigo-600 hover:text-indigo-800 transition underline"
                        >
                          Gán cho các ngày làm
                        </button>
                      )}
                    </div>
                    <div className="flex items-center gap-2">
                      <input
                        type="number"
                        min={0.5}
                        max={12}
                        step={0.5}
                        value={standardHoursPerDay}
                        onChange={(e) => canManage && setStandardHoursPerDay(Number(e.target.value))}
                        disabled={!canManage}
                        className="w-full px-3 py-2 text-xs font-bold text-slate-800 bg-slate-50 border border-slate-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition"
                      />
                      <span className="text-xs font-bold text-slate-600 shrink-0">giờ / ngày</span>
                    </div>
                    <span className="text-[11px] text-slate-500 block">
                      * Dùng làm quy đổi chuẩn: 1 Ngày công = {standardHoursPerDay} giờ.
                    </span>
                  </div>
                </div>
              </div>

              {/* 7 Days of Standard Work Week Grid */}
              <div className="bg-white rounded-2xl border border-slate-200 p-5 sm:p-6 shadow-xs space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-3 border-b border-slate-100">
                  <div>
                    <h3 className="text-sm sm:text-base font-bold text-slate-900">
                      Lịch chi tiết các ngày trong tuần
                    </h3>
                    <p className="text-xs text-slate-500">
                      Bật/tắt ngày làm việc và định cấu hình số giờ làm việc chuẩn cho từng thứ trong tuần.
                    </p>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold px-2.5 py-1 rounded-lg bg-indigo-50 text-indigo-700 border border-indigo-100">
                      {workingDaysCount} / 7 ngày làm việc
                    </span>
                    <span className="text-xs font-bold px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-100">
                      {computedHoursPerWeek} giờ / tuần
                    </span>
                  </div>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-7 gap-3">
                  {sortedWorkDays.map((day) => {
                    const info = DAY_LABELS[day.dayOfWeek] || { label: day.dayOfWeek, short: day.dayOfWeek };
                    const isWork = day.isWorkingDay;

                    return (
                      <div
                        key={day.dayOfWeek}
                        className={`flex flex-col p-3 rounded-2xl border transition-all text-center select-none ${
                          isWork
                            ? "bg-indigo-50/60 border-indigo-200 text-slate-900 shadow-xs ring-1 ring-indigo-500/10"
                            : "bg-slate-50/70 border-slate-200 text-slate-400"
                        }`}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <span
                            className={`text-[10px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded-md ${
                              isWork ? "bg-indigo-200/70 text-indigo-800" : "bg-slate-200/60 text-slate-500"
                            }`}
                          >
                            {info.short}
                          </span>
                          <button
                            type="button"
                            onClick={() => canManage && handleToggleDay(day.dayOfWeek)}
                            disabled={!canManage}
                            className={`text-[11px] font-bold px-2 py-0.5 rounded-lg transition ${
                              isWork
                                ? "bg-indigo-600 text-white hover:bg-indigo-700"
                                : "bg-slate-200 text-slate-600 hover:bg-slate-300"
                            } ${canManage ? "cursor-pointer" : "cursor-default"}`}
                          >
                            {isWork ? "Làm" : "Nghỉ"}
                          </button>
                        </div>

                        <span className="text-xs font-bold text-slate-800 mb-3">{info.label}</span>

                        <div className="mt-auto space-y-1">
                          <div className="flex items-center justify-center gap-1">
                            <input
                              type="number"
                              min={0.5}
                              max={12}
                              step={0.5}
                              disabled={!canManage || !isWork}
                              value={isWork ? day.workingHours : 0}
                              onChange={(e) => handleDayHoursChange(day.dayOfWeek, Number(e.target.value))}
                              className={`w-16 px-1.5 py-1 text-xs font-bold text-center rounded-lg border focus:outline-hidden transition ${
                                isWork
                                  ? "bg-white border-indigo-200 text-indigo-900 focus:ring-1 focus:ring-indigo-500"
                                  : "bg-slate-100 border-slate-200 text-slate-400 cursor-not-allowed"
                              }`}
                            />
                            <span className="text-[11px] text-slate-500 font-semibold">h</span>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>

                {/* Actions for Work Week */}
                {canManage && (
                  <div className="pt-4 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                    <span className="text-xs text-slate-500 italic">
                      * Nhấp vào nút "Làm / Nghỉ" để chuyển đổi trạng thái ngày làm việc hoặc nhập số giờ làm cụ thể từng ngày.
                    </span>
                    <div className="flex items-center gap-2.5 self-end sm:self-auto">
                      {hasWorkWeekChanges && (
                        <button
                          type="button"
                          onClick={handleUndoChanges}
                          disabled={isSavingConfig}
                          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-slate-600 bg-white border border-slate-200 rounded-xl hover:bg-slate-50 transition shadow-xs cursor-pointer"
                        >
                          <RotateCcw className="h-3.5 w-3.5" />
                          Hoàn tác
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={handleSaveWorkWeekConfig}
                        disabled={isSavingConfig || !hasWorkWeekChanges}
                        className={`flex items-center gap-1.5 px-4 py-2 text-xs font-semibold rounded-xl shadow-xs transition ${
                          hasWorkWeekChanges
                            ? "bg-indigo-600 text-white hover:bg-indigo-700 active:scale-98 cursor-pointer"
                            : "bg-slate-100 text-slate-400 cursor-not-allowed"
                        }`}
                      >
                        {isSavingConfig && <Loader2 className="h-3.5 w-3.5 animate-spin mr-1" />}
                        Lưu cấu hình tuần chuẩn
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* KPI Summary & Capacity Conversion Bar */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="bg-white p-4 rounded-2xl border border-slate-200 flex items-center gap-3.5 shadow-xs">
                  <div className="p-2.5 bg-indigo-50 text-indigo-600 rounded-xl">
                    <CalendarDays className="h-5 w-5" />
                  </div>
                  <div>
                    <span className="text-xs font-medium text-slate-500 block">Số ngày làm việc / tuần</span>
                    <span className="text-base font-extrabold text-slate-900">{workingDaysCount} ngày</span>
                  </div>
                </div>

                <div className="bg-white p-4 rounded-2xl border border-slate-200 flex items-center gap-3.5 shadow-xs">
                  <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-xl">
                    <Clock className="h-5 w-5" />
                  </div>
                  <div>
                    <span className="text-xs font-medium text-slate-500 block">Tổng định mức giờ chuẩn tuần</span>
                    <span className="text-base font-extrabold text-emerald-600">{computedHoursPerWeek} giờ / tuần</span>
                  </div>
                </div>

                <div className="bg-white p-4 rounded-2xl border border-slate-200 flex items-center gap-3.5 shadow-xs">
                  <div className="p-2.5 bg-purple-50 text-purple-600 rounded-xl">
                    <ArrowRightLeft className="h-5 w-5" />
                  </div>
                  <div>
                    <span className="text-xs font-medium text-slate-500 block">Tỷ lệ quy đổi chuẩn</span>
                    <span className="text-xs font-bold text-slate-800">
                      1 Ngày = {standardHoursPerDay}h | 1 FTE = {computedHoursPerWeek}h
                    </span>
                  </div>
                </div>
              </div>

              {/* Interactive Quick Converter */}
              <div className="bg-slate-50 rounded-2xl border border-slate-200 p-5">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-3 border-b border-slate-200">
                  <div className="flex items-center gap-2">
                    <ArrowRightLeft className="h-4 w-4 text-indigo-600" />
                    <h4 className="text-xs sm:text-sm font-bold text-slate-900">
                      Công cụ quy đổi nhanh đơn vị năng lực
                    </h4>
                  </div>
                  <span className="text-[11px] text-slate-500">
                    Áp dụng theo định mức chuẩn: {standardHoursPerDay}h/ngày, {computedHoursPerWeek}h/tuần
                  </span>
                </div>

                <form onSubmit={handleQuickConvert} className="pt-3 flex flex-wrap items-center gap-3">
                  <div className="flex items-center gap-2">
                    <input
                      type="number"
                      min={0.1}
                      step={0.1}
                      value={converterValue}
                      onChange={(e) => setConverterValue(Number(e.target.value))}
                      className="w-24 px-3 py-1.5 text-xs font-bold bg-white border border-slate-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20"
                    />
                    <select
                      value={converterFrom}
                      onChange={(e) => setConverterFrom(e.target.value as CapacityUnit)}
                      className="px-2.5 py-1.5 text-xs font-bold bg-white border border-slate-200 rounded-xl focus:outline-hidden cursor-pointer"
                    >
                      <option value="HOURS">Giờ (Hours)</option>
                      <option value="DAYS">Ngày (Days)</option>
                      <option value="FTE">FTE</option>
                    </select>
                  </div>

                  <span className="text-xs font-bold text-slate-400">chuyển sang</span>

                  <select
                    value={converterTo}
                    onChange={(e) => setConverterTo(e.target.value as CapacityUnit)}
                    className="px-2.5 py-1.5 text-xs font-bold bg-white border border-slate-200 rounded-xl focus:outline-hidden cursor-pointer"
                  >
                    <option value="HOURS">Giờ (Hours)</option>
                    <option value="DAYS">Ngày (Days)</option>
                    <option value="FTE">FTE</option>
                  </select>

                  <button
                    type="submit"
                    disabled={isConverting}
                    className="flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl transition shadow-xs cursor-pointer"
                  >
                    {isConverting && <Loader2 className="h-3 w-3 animate-spin mr-1" />}
                    Quy đổi ngay
                  </button>

                  {conversionResult && (
                    <div className="w-full sm:w-auto mt-2 sm:mt-0 px-3 py-1.5 rounded-xl bg-indigo-100/70 border border-indigo-200 text-xs font-bold text-indigo-900">
                      {conversionResult}
                    </div>
                  )}
                </form>
              </div>
            </>
          )}
        </div>
      )}

      {/* ========================================================= */}
      {/* TAB 2: DANH MỤC NGÀY NGHỈ LỄ THƯỜNG NIÊN                  */}
      {/* ========================================================= */}
      {activeTab === "holidays" && (
        <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-5 border-b border-slate-100">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-100">
                <CalendarCheck2 className="h-5 w-5" />
              </div>
              <div>
                <h2 className="text-base font-bold text-slate-900">
                  Danh sách ngày nghỉ lễ theo năm
                </h2>
                <p className="text-xs text-slate-500">
                  Khai báo danh mục ngày nghỉ lễ hàng năm. Mỗi ngày lễ khi rơi vào ngày làm việc sẽ tự động trừ giờ khả dụng tuần.
                </p>
              </div>
            </div>

            {/* Controls: Year selector + Add Button */}
            <div className="flex items-center gap-3 flex-wrap">
              <div className="flex items-center gap-2 bg-slate-50 px-3 py-1.5 rounded-xl border border-slate-200">
                <span className="text-xs font-medium text-slate-500">Năm:</span>
                <select
                  value={selectedYear}
                  onChange={(e) => setSelectedYear(Number(e.target.value))}
                  className="bg-transparent text-xs font-bold text-slate-800 border-none outline-hidden cursor-pointer"
                >
                  {[currentYear - 1, currentYear, currentYear + 1, currentYear + 2].map((y) => (
                    <option key={y} value={y}>
                      Năm {y}
                    </option>
                  ))}
                </select>
              </div>

              {canManage && (
                <button
                  type="button"
                  onClick={handleOpenAddHoliday}
                  className="flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-indigo-600 rounded-xl hover:bg-indigo-700 transition shadow-xs cursor-pointer"
                >
                  <Plus className="h-4 w-4" />
                  Thêm ngày lễ mới
                </button>
              )}
            </div>
          </div>

          {/* Stats summary bar */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 my-4">
            <div className="flex items-center gap-3 p-3.5 rounded-xl bg-slate-50 border border-slate-100">
              <div className="p-2 rounded-lg bg-white border border-slate-200 text-slate-600">
                <CalendarCheck2 className="h-4 w-4" />
              </div>
              <div>
                <span className="text-[11px] font-medium text-slate-500 block">Tổng số ngày lễ trong năm</span>
                <span className="text-sm font-bold text-slate-900">{totalHolidaysCount} ngày lễ</span>
              </div>
            </div>

            <div className="flex items-center gap-3 p-3.5 rounded-xl bg-slate-50 border border-slate-100">
              <div className="p-2 rounded-lg bg-white border border-slate-200 text-indigo-600">
                <Clock className="h-4 w-4" />
              </div>
              <div>
                <span className="text-[11px] font-medium text-slate-500 block">Tổng giờ khấu trừ</span>
                <span className="text-sm font-bold text-indigo-600">{totalDeductedHours} giờ</span>
              </div>
            </div>
          </div>

          {/* Holidays Table */}
          {isLoadingHolidays ? (
            <div className="flex items-center justify-center py-12 text-slate-400 text-sm">
              <Loader2 className="h-5 w-5 animate-spin mr-2 text-indigo-600" />
              Đang tải danh sách ngày lễ năm {selectedYear}...
            </div>
          ) : holidays.length === 0 ? (
            <div className="text-center py-12 bg-slate-50/50 rounded-xl border border-dashed border-slate-200">
              <CalendarDays className="h-10 w-10 text-slate-300 mx-auto mb-2" />
              <p className="text-sm font-semibold text-slate-700">Chưa có ngày nghỉ lễ nào trong năm {selectedYear}</p>
              <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                {canManage
                  ? 'Nhấn nút "Thêm ngày lễ mới" phía trên để bắt đầu khai báo lịch nghỉ lễ cho công ty.'
                  : "Danh mục ngày lễ cho năm này hiện chưa được thiết lập bởi bộ phận Nhân sự."}
              </p>
              {canManage && (
                <button
                  type="button"
                  onClick={handleOpenAddHoliday}
                  className="mt-4 inline-flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-semibold text-indigo-600 bg-indigo-50 border border-indigo-200 rounded-xl hover:bg-indigo-100 transition cursor-pointer"
                >
                  <Plus className="h-3.5 w-3.5" />
                  Thêm ngày lễ
                </button>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-slate-200">
              <table className="w-full text-left text-xs text-slate-700">
                <thead className="bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-500 border-b border-slate-200">
                  <tr>
                    <th className="py-3 px-4 w-12 text-center">STT</th>
                    <th className="py-3 px-4 w-36">Ngày nghỉ lễ</th>
                    <th className="py-3 px-4 w-28">Thứ</th>
                    <th className="py-3 px-4">Tên ngày lễ</th>
                    <th className="py-3 px-4 w-36 text-center">Giờ khấu trừ</th>
                    {canManage && <th className="py-3 px-4 w-28 text-right">Thao tác</th>}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {holidays.map((item, idx) => (
                    <tr key={item.id} className="hover:bg-slate-50/80 transition">
                      <td className="py-3 px-4 text-center text-slate-400 font-medium">
                        {idx + 1}
                      </td>
                      <td className="py-3 px-4 font-semibold text-slate-900">
                        <span className="font-mono text-xs bg-slate-100 px-2 py-0.5 rounded-md text-slate-800">
                          {formatDateDisplay(item.holidayDate)}
                        </span>
                      </td>
                      <td className="py-3 px-4 font-medium text-slate-600">
                        {getDayNameFromDateStr(item.holidayDate)}
                      </td>
                      <td className="py-3 px-4 font-medium text-slate-900">
                        {item.name}
                      </td>
                      <td className="py-3 px-4 text-center">
                        <span className="inline-flex items-center gap-1 font-semibold text-indigo-700 bg-indigo-50 border border-indigo-100 px-2.5 py-0.5 rounded-md text-xs">
                          <Clock className="h-3 w-3" />
                          {item.workingHoursDeducted} giờ
                        </span>
                      </td>
                      {canManage && (
                        <td className="py-3 px-4 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            <button
                              type="button"
                              onClick={() => handleOpenEditHoliday(item)}
                              title="Chỉnh sửa ngày lễ"
                              className="p-1.5 text-slate-400 hover:text-indigo-600 rounded-lg hover:bg-slate-100 transition"
                            >
                              <Edit2 className="h-3.5 w-3.5" />
                            </button>
                            <button
                              type="button"
                              onClick={() => setDeletingHoliday(item)}
                              title="Xóa ngày lễ"
                              className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-slate-100 transition"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </button>
                          </div>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Modal Add/Edit Holiday */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xl max-w-md w-full p-6 animate-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100">
              <h3 className="text-base font-bold text-slate-900">
                {editingHoliday ? "Chỉnh sửa ngày nghỉ lễ" : "Thêm ngày nghỉ lễ mới"}
              </h3>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg hover:bg-slate-100 transition"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <form onSubmit={handleSubmitHoliday} className="mt-4 space-y-4">
              {modalError && (
                <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
                  <AlertCircle className="h-4 w-4 shrink-0" />
                  <span>{modalError}</span>
                </div>
              )}

              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  Ngày nghỉ lễ <span className="text-rose-500">*</span>
                </label>
                <input
                  type="date"
                  value={holidayDate}
                  onChange={(e) => setHolidayDate(e.target.value)}
                  required
                  className="w-full px-3 py-2 text-xs font-semibold text-slate-800 bg-slate-50 border border-slate-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  Tên ngày nghỉ lễ <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  placeholder="Ví dụ: Tết Nguyên Đán, Giỗ Tổ Hùng Vương..."
                  value={holidayName}
                  onChange={(e) => setHolidayName(e.target.value)}
                  required
                  maxLength={255}
                  className="w-full px-3 py-2 text-xs font-semibold text-slate-800 bg-slate-50 border border-slate-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  Số giờ làm việc khấu trừ (mặc định 8h) <span className="text-rose-500">*</span>
                </label>
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    min={1}
                    max={24}
                    value={workingHoursDeducted}
                    onChange={(e) => setWorkingHoursDeducted(Number(e.target.value))}
                    required
                    className="w-24 px-3 py-2 text-xs font-bold text-slate-800 bg-slate-50 border border-slate-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                  />
                  <span className="text-xs text-slate-500">giờ</span>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 bg-slate-100 rounded-xl hover:bg-slate-200 transition cursor-pointer"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingHoliday}
                  className="flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-indigo-600 rounded-xl hover:bg-indigo-700 transition shadow-xs cursor-pointer"
                >
                  {isSubmittingHoliday && <Loader2 className="h-3.5 w-3.5 animate-spin mr-1" />}
                  {editingHoliday ? "Lưu thay đổi" : "Thêm mới"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deletingHoliday && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xl max-w-sm w-full p-6 text-center animate-in zoom-in-95 duration-150">
            <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center mx-auto mb-3">
              <Trash2 className="h-6 w-6" />
            </div>
            <h3 className="text-base font-bold text-slate-900">Xóa ngày nghỉ lễ</h3>
            <p className="text-xs text-slate-500 mt-1">
              Bạn có chắc chắn muốn xóa ngày lễ <strong className="text-slate-800">"{deletingHoliday.name}"</strong> (ngày {formatDateDisplay(deletingHoliday.holidayDate)}) không?
            </p>
            <div className="mt-5 flex items-center justify-center gap-2.5">
              <button
                type="button"
                onClick={() => setDeletingHoliday(null)}
                className="px-4 py-2 text-xs font-semibold text-slate-600 bg-slate-100 rounded-xl hover:bg-slate-200 transition cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-rose-600 rounded-xl hover:bg-rose-700 transition shadow-xs cursor-pointer"
              >
                {isDeleting && <Loader2 className="h-3.5 w-3.5 animate-spin mr-1" />}
                Xác nhận xóa
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
