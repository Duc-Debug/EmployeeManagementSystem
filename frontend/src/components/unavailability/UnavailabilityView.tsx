"use client";

import { useState, useEffect, useMemo, useCallback, useRef } from "react";
import {
  CalendarX,
  Plus,
  Clock,
  CheckCircle2,
  AlertCircle,
  Filter,
  Search,
  RotateCcw,
  Trash2,
  Calendar,
  Loader2,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuthUser } from "@/lib/auth-session";
import {
  getEmployeeProfileByUserId,
  getEmployees,
  type EmployeeProfile,
} from "@/lib/api/employees";
import {
  getMyUnavailabilityDeclarations,
  getPendingUnavailabilityDeclarations,
  cancelUnavailability,
  type UnavailabilityDeclarationResult,
  UNAVAILABILITY_REASON_LABELS,
  UNAVAILABILITY_STATUS_LABELS,
} from "@/lib/api/unavailability";
import DeclareUnavailabilityModal from "./DeclareUnavailabilityModal";
import ApproveWithConflictModal from "./ApproveWithConflictModal";
import RejectUnavailabilityModal from "./RejectUnavailabilityModal";

export function formatDateVN(dateStr?: string | null): string {
  if (!dateStr) return "—";
  const parts = dateStr.split("-");
  if (parts.length !== 3) return dateStr;
  return `${parts[2]}/${parts[1]}/${parts[0]}`;
}

export default function UnavailabilityView() {
  const user = useAuthUser();

  const canApprove = useMemo(() => {
    return user?.permissions?.includes("UNAVAILABILITY_APPROVE") === true;
  }, [user]);

  const canDeclare = useMemo(() => {
    return user?.permissions?.includes("UNAVAILABILITY_DECLARE") === true;
  }, [user]);

  const [activeSubTab, setActiveSubTab] = useState<"my" | "pending">("my");
  const [myDeclarations, setMyDeclarations] = useState<UnavailabilityDeclarationResult[]>([]);
  const [pendingDeclarations, setPendingDeclarations] = useState<UnavailabilityDeclarationResult[]>([]);
  const [currentEmployee, setCurrentEmployee] = useState<EmployeeProfile | null>(null);
  const [employeesMap, setEmployeesMap] = useState<Record<number, string>>({});

  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");

  // Client-side pagination
  const [currentPage, setCurrentPage] = useState<number>(1);
  const pageSize = 10;

  // Reset page when tab or filter changes
  useEffect(() => {
    setCurrentPage(1);
  }, [activeSubTab, statusFilter, searchTerm]);

  // Notification toast with ref cleanup
  const toastTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const showToast = (type: "success" | "error", message: string) => {
    if (toastTimeoutRef.current) {
      clearTimeout(toastTimeoutRef.current);
    }
    setNotification({ type, message });
    toastTimeoutRef.current = setTimeout(() => {
      setNotification(null);
      toastTimeoutRef.current = null;
    }, 4000);
  };

  useEffect(() => {
    return () => {
      if (toastTimeoutRef.current) {
        clearTimeout(toastTimeoutRef.current);
      }
    };
  }, []);

  // Modals state
  const [isDeclareModalOpen, setIsDeclareModalOpen] = useState<boolean>(false);
  const [approvingDeclaration, setApprovingDeclaration] = useState<UnavailabilityDeclarationResult | null>(null);
  const [rejectingDeclaration, setRejectingDeclaration] = useState<UnavailabilityDeclarationResult | null>(null);
  const [cancellingDeclaration, setCancellingDeclaration] = useState<UnavailabilityDeclarationResult | null>(null);
  const [isCancelling, setIsCancelling] = useState<boolean>(false);

  // Load current employee profile
  useEffect(() => {
    if (!user?.id) return;
    let isMounted = true;
    async function fetchProfile() {
      try {
        const emp = await getEmployeeProfileByUserId(user!.id);
        if (isMounted) setCurrentEmployee(emp);
      } catch {
        // Handled silently
      }
    }
    fetchProfile();
    return () => {
      isMounted = false;
    };
  }, [user]);

  // Load employee directory for name mapping in manager view
  useEffect(() => {
    let isMounted = true;
    async function loadDirectory() {
      try {
        const res = await getEmployees(1, 200);
        if (isMounted && res?.content) {
          const map: Record<number, string> = {};
          for (const emp of res.content) {
            map[emp.id] = emp.fullName;
          }
          setEmployeesMap(map);
        }
      } catch {
        // Silently ignore if not authorized
      }
    }
    loadDirectory();
    return () => {
      isMounted = false;
    };
  }, []);

  // Load declarations data
  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const [myRes, pendingRes] = await Promise.all([
        getMyUnavailabilityDeclarations().catch((err) => {
          console.error("Lỗi tải danh sách khai báo cá nhân:", err);
          return [];
        }),
        canApprove
          ? getPendingUnavailabilityDeclarations().catch((err) => {
              console.error("Lỗi tải danh sách khai báo chờ duyệt:", err);
              return [];
            })
          : Promise.resolve([]),
      ]);
      setMyDeclarations(myRes);
      setPendingDeclarations(pendingRes);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Không thể tải danh sách khai báo.";
      showToast("error", msg);
    } finally {
      setIsLoading(false);
    }
  }, [canApprove]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Handle Cancel Declaration
  const handleConfirmCancel = async () => {
    if (!cancellingDeclaration) return;
    try {
      setIsCancelling(true);
      await cancelUnavailability(cancellingDeclaration.id);
      showToast("success", `Đã hủy thành công khai báo #${cancellingDeclaration.id}`);
      setCancellingDeclaration(null);
      await loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Hủy khai báo thất bại.";
      showToast("error", msg);
    } finally {
      setIsCancelling(false);
    }
  };

  // Filtered list
  const currentList = useMemo(() => {
    const list = activeSubTab === "my" ? myDeclarations : pendingDeclarations;
    const lowerSearch = searchTerm.trim().toLowerCase();

    return list.filter((item) => {
      const matchesStatus = statusFilter === "ALL" || item.status === statusFilter;
      const empName = employeesMap[item.employeeId] || "";
      const matchesSearch =
        lowerSearch === "" ||
        String(item.id).includes(lowerSearch) ||
        empName.toLowerCase().includes(lowerSearch) ||
        (item.reasonDetail && item.reasonDetail.toLowerCase().includes(lowerSearch)) ||
        (UNAVAILABILITY_REASON_LABELS[item.reasonType] &&
          UNAVAILABILITY_REASON_LABELS[item.reasonType].toLowerCase().includes(lowerSearch));
      return matchesStatus && matchesSearch;
    });
  }, [activeSubTab, myDeclarations, pendingDeclarations, statusFilter, searchTerm, employeesMap]);

  const totalPages = useMemo(() => {
    return Math.max(1, Math.ceil(currentList.length / pageSize));
  }, [currentList.length, pageSize]);

  const paginatedList = useMemo(() => {
    const startIndex = (currentPage - 1) * pageSize;
    return currentList.slice(startIndex, startIndex + pageSize);
  }, [currentList, currentPage, pageSize]);

  // Stats KPI
  const stats = useMemo(() => {
    const pendingCount = myDeclarations.filter((d) => d.status === "PENDING").length;
    const approvedCount = myDeclarations.filter((d) => d.status === "APPROVED").length;
    const totalApprovedHours = myDeclarations
      .filter((d) => d.status === "APPROVED")
      .reduce((sum, d) => sum + (Number(d.totalHoursDeducted) || 0), 0);

    return {
      pendingCount,
      approvedCount,
      totalApprovedHours,
      allPendingApprovalCount: pendingDeclarations.length,
    };
  }, [myDeclarations, pendingDeclarations]);

  const todayStr = useMemo(() => new Date().toISOString().split("T")[0], []);

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Toast Notification */}
      {notification && (
        <div
          className={cn(
            "fixed top-5 right-5 z-50 flex items-center gap-3 px-4 py-3 rounded-2xl shadow-lg border text-xs font-medium animate-in fade-in slide-in-from-top-3 duration-200",
            notification.type === "success"
              ? "bg-emerald-50 border-emerald-200 text-emerald-800"
              : "bg-rose-50 border-rose-200 text-rose-800"
          )}
        >
          {notification.type === "success" ? (
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          ) : (
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
          )}
          <span>{notification.message}</span>
        </div>
      )}

      {/* Header & Action Button */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-indigo-50 text-indigo-600 rounded-xl border border-indigo-100">
              <CalendarX className="w-5 h-5" />
            </div>
            <h1 className="text-xl font-bold text-slate-900">Khai báo Thời gian không sẵn sàng</h1>
          </div>
          <p className="text-xs text-slate-500 mt-1 max-w-2xl leading-relaxed">
            Đăng ký thời gian bận do đào tạo, công tác hoặc việc riêng để trừ vào năng lực khả dụng theo tuần.
          </p>
        </div>

        {canDeclare && (
          <div className="flex items-center gap-3">
            <button
              onClick={() => setIsDeclareModalOpen(true)}
              className="flex items-center gap-2 px-4 py-2.5 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-xs transition"
            >
              <Plus className="w-4 h-4" />
              Khai báo không sẵn sàng
            </button>
          </div>
        )}
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex items-center gap-4">
          <div className="p-3 rounded-xl bg-amber-50 text-amber-600 border border-amber-100">
            <Clock className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500">Đơn cá nhân đang chờ duyệt</p>
            <h3 className="text-2xl font-bold text-slate-900 mt-0.5">{stats.pendingCount}</h3>
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex items-center gap-4">
          <div className="p-3 rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-100">
            <CheckCircle2 className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500">Đơn cá nhân đã duyệt</p>
            <h3 className="text-2xl font-bold text-slate-900 mt-0.5">{stats.approvedCount}</h3>
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex items-center gap-4">
          <div className="p-3 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
            <Calendar className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs font-medium text-slate-500">Tổng giờ khấu trừ khả dụng</p>
            <h3 className="text-2xl font-bold text-indigo-700 mt-0.5">{stats.totalApprovedHours}h</h3>
          </div>
        </div>
      </div>

      {/* Main Tabs & Table Container */}
      <div className="bg-white rounded-3xl border border-slate-200 shadow-xs overflow-hidden">
        {/* Navigation Tabs */}
        <div className="flex items-center justify-between px-6 pt-4 border-b border-slate-200">
          <div className="flex items-center gap-6">
            <button
              onClick={() => {
                setActiveSubTab("my");
                setStatusFilter("ALL");
              }}
              className={cn(
                "pb-3.5 text-xs font-bold border-b-2 transition relative",
                activeSubTab === "my"
                  ? "border-indigo-600 text-indigo-600"
                  : "border-transparent text-slate-500 hover:text-slate-800"
              )}
            >
              Khai báo của tôi ({myDeclarations.length})
            </button>

            {canApprove && (
              <button
                onClick={() => {
                  setActiveSubTab("pending");
                  setStatusFilter("ALL");
                }}
                className={cn(
                  "pb-3.5 text-xs font-bold border-b-2 transition relative flex items-center gap-1.5",
                  activeSubTab === "pending"
                    ? "border-indigo-600 text-indigo-600"
                    : "border-transparent text-slate-500 hover:text-slate-800"
                )}
              >
                <span>Chờ phê duyệt</span>
                {stats.allPendingApprovalCount > 0 && (
                  <span className="px-2 py-0.5 text-[10px] font-extrabold bg-amber-100 text-amber-800 rounded-full">
                    {stats.allPendingApprovalCount}
                  </span>
                )}
              </button>
            )}
          </div>

          {/* Refresh Button */}
          <button
            onClick={loadData}
            disabled={isLoading}
            className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100 transition disabled:opacity-50"
            title="Làm mới dữ liệu"
          >
            <RotateCcw className={cn("w-4 h-4", isLoading && "animate-spin")} />
          </button>
        </div>

        {/* Filters Toolbar */}
        <div className="p-4 sm:p-6 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-50/40">
          <div className="relative flex-1 max-w-sm">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Tìm kiếm theo mã đơn, lý do..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs bg-white border border-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition"
            />
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2">
              <Filter className="w-3.5 h-3.5 text-slate-400" />
              <span className="text-xs text-slate-500 font-medium">Trạng thái:</span>
            </div>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-3 py-1.5 text-xs bg-white border border-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="PENDING">Chờ duyệt</option>
              <option value="APPROVED">Đã duyệt</option>
              <option value="REJECTED">Đã từ chối</option>
              <option value="CANCELLED">Đã hủy</option>
            </select>
          </div>
        </div>

        {/* Table Content */}
        <div className="overflow-x-auto">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-16 text-slate-400 text-xs gap-2">
              <Loader2 className="w-6 h-6 animate-spin text-indigo-600" />
              <span>Đang tải dữ liệu khai báo...</span>
            </div>
          ) : currentList.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-center text-slate-400 p-6">
              <div className="p-4 rounded-2xl bg-slate-100/80 mb-3 text-slate-400">
                <CalendarX className="w-8 h-8" />
              </div>
              {searchTerm || statusFilter !== "ALL" ? (
                <>
                  <p className="text-xs font-semibold text-slate-700">Không tìm thấy kết quả phù hợp</p>
                  <p className="text-[11px] text-slate-400 mt-1 max-w-sm">
                    Không có đơn khai báo nào khớp với bộ lọc hoặc từ khóa tìm kiếm &quot;{searchTerm}&quot;.
                  </p>
                  <button
                    type="button"
                    onClick={() => {
                      setSearchTerm("");
                      setStatusFilter("ALL");
                    }}
                    className="mt-3 px-3 py-1.5 text-xs font-semibold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 rounded-lg transition"
                  >
                    Xóa bộ lọc
                  </button>
                </>
              ) : (
                <>
                  <p className="text-xs font-semibold text-slate-600">Không có bản ghi nào</p>
                  <p className="text-[11px] text-slate-400 mt-1 max-w-sm">
                    {activeSubTab === "my"
                      ? "Bạn chưa có đơn khai báo thời gian không sẵn sàng nào trong danh mục này."
                      : "Hiện không có đơn khai báo nào đang chờ bạn phê duyệt."}
                  </p>
                </>
              )}
            </div>
          ) : (
            <>
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50/70 text-slate-600 font-semibold">
                    <th className="py-3 px-4 w-20">Mã đơn</th>
                    {activeSubTab === "pending" && <th className="py-3 px-4">Nhân sự</th>}
                    <th className="py-3 px-4">Khoảng thời gian</th>
                    <th className="py-3 px-4">Giờ khấu trừ</th>
                    <th className="py-3 px-4">Loại lý do</th>
                    <th className="py-3 px-4">Chi tiết / Ghi chú</th>
                    <th className="py-3 px-4">Trạng thái</th>
                    <th className="py-3 px-4 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {paginatedList.map((item) => {
                    const isPast = item.startDate < todayStr;
                    const canCancel =
                      activeSubTab === "my" &&
                      (item.status === "PENDING" || (item.status === "APPROVED" && !isPast));

                    return (
                      <tr key={item.id} className="hover:bg-slate-50/50 transition">
                        <td className="py-3.5 px-4 font-bold text-slate-900">#{item.id}</td>

                        {activeSubTab === "pending" && (
                          <td className="py-3.5 px-4">
                            <div className="flex flex-col">
                              <span className="font-semibold text-slate-900">
                                {employeesMap[item.employeeId] || `Nhân sự #${item.employeeId}`}
                              </span>
                              <span className="text-[11px] text-slate-400">Mã NV: #{item.employeeId}</span>
                            </div>
                          </td>
                        )}

                        <td className="py-3.5 px-4">
                          <div className="flex items-center gap-1.5 font-medium text-slate-800">
                            <span>{formatDateVN(item.startDate)}</span>
                            <span className="text-slate-400">&rarr;</span>
                            <span>{formatDateVN(item.endDate)}</span>
                          </div>
                        </td>

                        <td className="py-3.5 px-4">
                          <span className="font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded-md border border-indigo-100/60">
                            {item.totalHoursDeducted}h
                          </span>
                        </td>

                        <td className="py-3.5 px-4">
                          <span className="font-medium text-slate-700">
                            {UNAVAILABILITY_REASON_LABELS[item.reasonType] || item.reasonType}
                          </span>
                        </td>

                        <td className="py-3.5 px-4 max-w-xs">
                          <p className="truncate text-slate-600" title={item.reasonDetail || ""}>
                            {item.reasonDetail || "—"}
                          </p>
                          {item.approverComment && (
                            <p className="text-[11px] text-slate-400 italic mt-0.5 truncate" title={item.approverComment}>
                              Phản hồi: {item.approverComment}
                            </p>
                          )}
                        </td>

                        <td className="py-3.5 px-4">
                          <span
                            className={cn(
                              "inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold border",
                              item.status === "PENDING" && "bg-amber-50 text-amber-700 border-amber-200",
                              item.status === "APPROVED" && "bg-emerald-50 text-emerald-700 border-emerald-200",
                              item.status === "REJECTED" && "bg-rose-50 text-rose-700 border-rose-200",
                              item.status === "CANCELLED" && "bg-slate-100 text-slate-600 border-slate-200"
                            )}
                          >
                            {UNAVAILABILITY_STATUS_LABELS[item.status]}
                          </span>
                        </td>

                        <td className="py-3.5 px-4 text-right">
                          <div className="flex items-center justify-end gap-2">
                            {/* Approval actions for managers */}
                            {activeSubTab === "pending" && item.status === "PENDING" && (
                              <>
                                <button
                                  onClick={() => setApprovingDeclaration(item)}
                                  className="px-2.5 py-1.5 text-xs font-semibold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-lg transition"
                                >
                                  Duyệt
                                </button>
                                <button
                                  onClick={() => setRejectingDeclaration(item)}
                                  className="px-2.5 py-1.5 text-xs font-semibold text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200 rounded-lg transition"
                                >
                                  Từ chối
                                </button>
                              </>
                            )}

                            {/* Cancel action for owner */}
                            {canCancel && (
                              <button
                                onClick={() => setCancellingDeclaration(item)}
                                className="px-2.5 py-1 text-xs font-semibold text-slate-600 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition border border-slate-200 hover:border-rose-200"
                                title="Hủy đơn khai báo này"
                              >
                                Hủy đơn
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

              {/* Pagination Bar */}
              {currentList.length > 0 && (
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 px-6 py-4 border-t border-slate-100 bg-slate-50/50 text-xs text-slate-500">
                  <div>
                    Hiển thị{" "}
                    <span className="font-semibold text-slate-700">
                      {(currentPage - 1) * pageSize + 1}
                    </span>{" "}
                    đến{" "}
                    <span className="font-semibold text-slate-700">
                      {Math.min(currentPage * pageSize, currentList.length)}
                    </span>{" "}
                    trên tổng số{" "}
                    <span className="font-semibold text-slate-700">{currentList.length}</span> đơn
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                      disabled={currentPage <= 1}
                      className="px-3 py-1.5 font-medium text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-100 transition disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      Trang trước
                    </button>
                    <span className="px-2 font-medium">
                      {currentPage} / {totalPages}
                    </span>
                    <button
                      type="button"
                      onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                      disabled={currentPage >= totalPages}
                      className="px-3 py-1.5 font-medium text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-100 transition disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      Trang sau
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Modals */}
      {isDeclareModalOpen && (
        <DeclareUnavailabilityModal
          isOpen={isDeclareModalOpen}
          onClose={() => setIsDeclareModalOpen(false)}
          employeeId={currentEmployee?.id || user?.id || 0}
          employeeName={currentEmployee?.fullName || user?.fullName || user?.username}
          onSuccess={(newDecl) => {
            showToast("success", `Khai báo #${newDecl.id} đã được gửi thành công.`);
            loadData();
          }}
        />
      )}

      {approvingDeclaration && (
        <ApproveWithConflictModal
          isOpen={Boolean(approvingDeclaration)}
          declaration={approvingDeclaration}
          onClose={() => setApprovingDeclaration(null)}
          onSuccess={(approved) => {
            showToast("success", `Đã phê duyệt đơn #${approved.id} thành công.`);
            setApprovingDeclaration(null);
            loadData();
          }}
        />
      )}

      {rejectingDeclaration && (
        <RejectUnavailabilityModal
          isOpen={Boolean(rejectingDeclaration)}
          declaration={rejectingDeclaration}
          onClose={() => setRejectingDeclaration(null)}
          onSuccess={(rejected) => {
            showToast("success", `Đã từ chối đơn #${rejected.id}.`);
            setRejectingDeclaration(null);
            loadData();
          }}
        />
      )}

      {/* Cancellation Confirmation Dialog */}
      {cancellingDeclaration && (
        <div
          role="dialog"
          aria-modal="true"
          onClick={(e) => {
            if (e.target === e.currentTarget && !isCancelling) setCancellingDeclaration(null);
          }}
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-150"
        >
          <div className="relative w-full max-w-sm bg-white rounded-2xl shadow-xl border border-slate-200 p-6 space-y-4">
            <div className="flex items-center gap-3 text-rose-600">
              <div className="p-2.5 bg-rose-50 rounded-xl border border-rose-100">
                <Trash2 className="w-5 h-5" />
              </div>
              <h3 className="text-base font-bold text-slate-900">Xác nhận hủy đơn</h3>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              Bạn có chắc chắn muốn hủy đơn khai báo thời gian không sẵn sàng{" "}
              <strong>#{cancellingDeclaration.id}</strong> ({cancellingDeclaration.startDate} &rarr;{" "}
              {cancellingDeclaration.endDate})?
              {cancellingDeclaration.status === "APPROVED" && (
                <span className="block mt-2 text-indigo-700 bg-indigo-50 p-2 rounded-lg font-medium">
                  Đơn đã duyệt này sẽ được hoàn trả lại giờ khả dụng cho tuần tương ứng.
                </span>
              )}
            </p>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setCancellingDeclaration(null)}
                disabled={isCancelling}
                className="px-4 py-2 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition"
              >
                Bỏ qua
              </button>
              <button
                type="button"
                onClick={handleConfirmCancel}
                disabled={isCancelling}
                className="flex items-center gap-2 px-4 py-2 text-xs font-semibold text-white bg-rose-600 hover:bg-rose-700 rounded-xl shadow-xs transition disabled:opacity-50"
              >
                {isCancelling && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                Xác nhận hủy
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}