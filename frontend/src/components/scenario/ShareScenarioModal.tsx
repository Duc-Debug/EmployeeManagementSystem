"use client";

import React, { useState, useEffect, useCallback } from "react";
import {
  X,
  Share2,
  Users,
  Search,
  CheckCircle2,
  AlertCircle,
  ShieldCheck,
  UserCheck,
  UserMinus,
  Loader2,
  FolderKanban,
  Building2,
  Crown,
  Briefcase,
} from "lucide-react";
import {
  getShareCandidates,
  getScenarioShares,
  shareScenario,
  unshareScenario,
  type ShareCandidateResult,
  type ScenarioShareResult,
} from "@/lib/api/simulation-scenarios";
import { cn } from "@/lib/utils";

interface ShareScenarioModalProps {
  isOpen: boolean;
  scenarioId: number;
  scenarioCode: string;
  scenarioName: string;
  onClose: () => void;
  onShareUpdated?: () => void;
}

export const ShareScenarioModal: React.FC<ShareScenarioModalProps> = ({
  isOpen,
  scenarioId,
  scenarioCode,
  scenarioName,
  onClose,
  onShareUpdated,
}) => {
  const [activeTab, setActiveTab] = useState<"candidates" | "shares">("candidates");
  const [candidates, setCandidates] = useState<ShareCandidateResult[]>([]);
  const [activeShares, setActiveShares] = useState<ScenarioShareResult[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedUserIds, setSelectedUserIds] = useState<number[]>([]);

  const [loadingCandidates, setLoadingCandidates] = useState(false);
  const [loadingShares, setLoadingShares] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [revokingUserId, setRevokingUserId] = useState<number | null>(null);

  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Load Active Shares
  const loadActiveShares = useCallback(async () => {
    setLoadingShares(true);
    try {
      const data = await getScenarioShares(scenarioId);
      setActiveShares(data.filter((s) => s.isActive));
    } catch (err: unknown) {
      console.error("Failed to load scenario shares:", err);
    } finally {
      setLoadingShares(false);
    }
  }, [scenarioId]);

  // Load Share Candidates
  const loadCandidates = useCallback(
    async (query?: string) => {
      setLoadingCandidates(true);
      setError(null);
      try {
        const data = await getShareCandidates(scenarioId, query);
        setCandidates(data);
      } catch (err: unknown) {
        setError(err instanceof Error ? err.message : "Không thể tải danh sách ứng viên chia sẻ.");
      } finally {
        setLoadingCandidates(false);
      }
    },
    [scenarioId]
  );

  useEffect(() => {
    if (isOpen) {
      loadActiveShares();
      loadCandidates();
      setSelectedUserIds([]);
      setError(null);
      setSuccessMessage(null);
    }
  }, [isOpen, loadActiveShares, loadCandidates]);

  // Debounced search for candidates
  useEffect(() => {
    if (!isOpen) return;
    const timer = setTimeout(() => {
      loadCandidates(searchQuery);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchQuery, isOpen, loadCandidates]);

  if (!isOpen) return null;

  const handleToggleSelect = (userId: number) => {
    setSelectedUserIds((prev) =>
      prev.includes(userId) ? prev.filter((id) => id !== userId) : [...prev, userId]
    );
  };

  const handleSelectAllCandidates = () => {
    if (selectedUserIds.length === candidates.length) {
      setSelectedUserIds([]);
    } else {
      setSelectedUserIds(candidates.map((c) => c.userId));
    }
  };

  const handleShareSubmit = async () => {
    if (selectedUserIds.length === 0) return;
    setSubmitting(true);
    setError(null);
    setSuccessMessage(null);

    try {
      await shareScenario(scenarioId, { userIds: selectedUserIds, recipientUserIds: selectedUserIds });
      setSuccessMessage(`Đã chia sẻ thành công cho ${selectedUserIds.length} người.`);
      setSelectedUserIds([]);
      await Promise.all([loadActiveShares(), loadCandidates(searchQuery)]);
      onShareUpdated?.();
      setTimeout(() => {
        setActiveTab("shares");
        setSuccessMessage(null);
      }, 1200);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Chia sẻ kịch bản thất bại.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleRevokeShare = async (userId: number, fullName: string) => {
    if (!window.confirm(`Bạn có chắc chắn muốn thu hồi quyền xem của "${fullName}"?`)) return;
    setRevokingUserId(userId);
    setError(null);
    setSuccessMessage(null);

    try {
      await unshareScenario(scenarioId, userId);
      await Promise.all([loadActiveShares(), loadCandidates(searchQuery)]);
      onShareUpdated?.();
      setSuccessMessage(`Đã thu hồi quyền xem của "${fullName}".`);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Thu hồi quyền xem thất bại.");
    } finally {
      setRevokingUserId(null);
    }
  };

  const renderRoleBadge = (roleCode: string) => {
    const code = roleCode.toUpperCase().replace(/_/g, "-");
    if (code.includes("VT-01") || code.includes("DIRECTOR") || code.includes("ADMIN")) {
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-purple-50 text-purple-700 border border-purple-200">
          <Crown className="h-3 w-3 mr-1 text-purple-600" />
          VT-01 (Giám đốc)
        </span>
      );
    }
    if (code.includes("VT-02") || code.includes("PM")) {
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-blue-50 text-blue-700 border border-blue-200">
          <Briefcase className="h-3 w-3 mr-1 text-blue-600" />
          VT-02 (Quản lý dự án)
        </span>
      );
    }
    if (code.includes("VT-03") || code.includes("RM")) {
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
          <Building2 className="h-3 w-3 mr-1 text-emerald-600" />
          VT-03 (Quản lý nguồn lực)
        </span>
      );
    }
    return (
      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 text-slate-700">
        {roleCode}
      </span>
    );
  };

  const formatDateTime = (dateStr: string) => {
    if (!dateStr) return "--:--";
    try {
      const d = new Date(dateStr);
      return d.toLocaleString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="relative w-full max-w-2xl bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden flex flex-col max-h-[90vh]">
        {/* Modal Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center space-x-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
              <Share2 className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">Chia Sẻ Kịch Bản Mô Phỏng</h3>
              <p className="text-xs text-slate-500 flex items-center space-x-2">
                <span className="font-mono font-semibold text-indigo-600">{scenarioCode}</span>
                <span>•</span>
                <span className="truncate max-w-[280px]">{scenarioName}</span>
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="flex border-b border-slate-200 bg-white px-5 pt-2">
          <button
            onClick={() => {
              setActiveTab("candidates");
              setError(null);
              setSuccessMessage(null);
            }}
            className={cn(
              "flex items-center space-x-2 py-2.5 px-4 text-xs font-semibold border-b-2 transition -mb-px",
              activeTab === "candidates"
                ? "border-indigo-600 text-indigo-600"
                : "border-transparent text-slate-500 hover:text-slate-700"
            )}
          >
            <Users className="h-4 w-4" />
            <span>Người nhận hợp lệ</span>
            {candidates.length > 0 && (
              <span className="ml-1.5 rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-bold text-slate-600">
                {candidates.length}
              </span>
            )}
          </button>

          <button
            onClick={() => {
              setActiveTab("shares");
              setError(null);
              setSuccessMessage(null);
            }}
            className={cn(
              "flex items-center space-x-2 py-2.5 px-4 text-xs font-semibold border-b-2 transition -mb-px",
              activeTab === "shares"
                ? "border-indigo-600 text-indigo-600"
                : "border-transparent text-slate-500 hover:text-slate-700"
            )}
          >
            <UserCheck className="h-4 w-4" />
            <span>Đang chia sẻ</span>
            <span
              className={cn(
                "ml-1.5 rounded-full px-2 py-0.5 text-[10px] font-bold",
                activeShares.length > 0
                  ? "bg-indigo-100 text-indigo-700"
                  : "bg-slate-100 text-slate-600"
              )}
            >
              {activeShares.length}
            </span>
          </button>
        </div>

        {/* Notification Banners */}
        {error && (
          <div className="mx-5 mt-4 p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center space-x-2">
            <AlertCircle className="h-4 w-4 text-rose-600 shrink-0" />
            <span>{error}</span>
          </div>
        )}
        {successMessage && (
          <div className="mx-5 mt-4 p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center space-x-2">
            <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
            <span>{successMessage}</span>
          </div>
        )}

        {/* Tab 1: Candidates Search and Selection */}
        {activeTab === "candidates" && (
          <div className="flex-1 flex flex-col overflow-hidden p-5 space-y-4">
            {/* Rule Notice */}
            <div className="p-3 rounded-xl bg-indigo-50/60 border border-indigo-100 text-indigo-900 text-[11px] leading-relaxed flex items-start space-x-2.5">
              <ShieldCheck className="h-4 w-4 text-indigo-600 shrink-0 mt-0.5" />
              <div>
                <strong>Quy tắc người nhận (BR-03 &amp; BR-04):</strong> Người nhận được xem ở chế độ{" "}
                <span className="font-semibold text-indigo-700">VIEW_ONLY</span>. Chỉ hiển thị các
                ứng viên thỏa mãn phạm vi truy cập: Giám đốc (VT-01), Quản lý dự án (VT-02) quản lý
                ít nhất 1 dự án trong kịch bản, hoặc Quản lý nguồn lực (VT-03) cùng bộ phận.
              </div>
            </div>

            {/* Search input & Select all */}
            <div className="flex items-center space-x-3">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
                <input
                  type="text"
                  placeholder="Tìm theo tên hoặc tên đăng nhập..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full rounded-xl border border-slate-200 pl-9 pr-4 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
                />
              </div>

              {candidates.length > 0 && (
                <button
                  type="button"
                  onClick={handleSelectAllCandidates}
                  className="text-xs font-semibold text-indigo-600 hover:text-indigo-700 px-2 py-1 shrink-0"
                >
                  {selectedUserIds.length === candidates.length ? "Bỏ chọn tất cả" : "Chọn tất cả"}
                </button>
              )}
            </div>

            {/* Candidate List */}
            <div className="flex-1 overflow-y-auto space-y-2 border border-slate-100 rounded-xl p-2 min-h-[220px] max-h-[300px]">
              {loadingCandidates ? (
                <div className="flex flex-col items-center justify-center h-48 space-y-2 text-slate-400">
                  <Loader2 className="h-6 w-6 animate-spin text-indigo-600" />
                  <span className="text-xs">Đang tìm kiếm ứng viên hợp lệ...</span>
                </div>
              ) : candidates.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-48 space-y-2 text-slate-400">
                  <Users className="h-8 w-8 text-slate-300" />
                  <span className="text-xs text-slate-500">
                    {searchQuery
                      ? "Không tìm thấy người dùng phù hợp với từ khóa."
                      : "Không có người nhận hợp lệ nào khả dụng (tất cả đã được chia sẻ hoặc không thuộc phạm vi)."}
                  </span>
                </div>
              ) : (
                candidates.map((cand) => {
                  const isChecked = selectedUserIds.includes(cand.userId);
                  return (
                    <div
                      key={cand.userId}
                      onClick={() => handleToggleSelect(cand.userId)}
                      className={cn(
                        "p-3 rounded-xl border transition cursor-pointer flex items-start space-x-3",
                        isChecked
                          ? "bg-indigo-50/50 border-indigo-200 shadow-2xs"
                          : "bg-white border-slate-200 hover:border-indigo-100 hover:bg-slate-50/60"
                      )}
                    >
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => {}}
                        className="mt-1 h-4 w-4 rounded-sm border-slate-300 text-indigo-600 focus:ring-indigo-500"
                      />
                      <div className="flex-1 space-y-1">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-2">
                            <span className="font-bold text-xs text-slate-900">{cand.fullName}</span>
                            <span className="font-mono text-[11px] text-slate-400">
                              @{cand.username}
                            </span>
                          </div>
                          {renderRoleBadge(cand.roleCode)}
                        </div>

                        {cand.orgUnitName && (
                          <div className="flex items-center text-[11px] text-slate-500">
                            <Building2 className="h-3 w-3 mr-1 text-slate-400" />
                            <span>Bộ phận: {cand.orgUnitName}</span>
                          </div>
                        )}

                        {cand.managedProjectNames && cand.managedProjectNames.length > 0 && (
                          <div className="flex flex-wrap items-center gap-1 pt-0.5">
                            <span className="text-[10px] text-slate-400 flex items-center">
                              <FolderKanban className="h-3 w-3 mr-1 text-blue-500" />
                              Dự án quản lý:
                            </span>
                            {cand.managedProjectNames.map((pName, idx) => (
                              <span
                                key={idx}
                                className="px-1.5 py-0.2 rounded-md bg-blue-50 text-blue-700 text-[10px] font-medium border border-blue-100"
                              >
                                {pName}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Modal Actions */}
            <div className="pt-3 border-t border-slate-100 flex items-center justify-between">
              <div className="text-xs text-slate-500">
                Đã chọn:{" "}
                <strong className="text-indigo-600 font-semibold">{selectedUserIds.length}</strong>{" "}
                người nhận
              </div>
              <div className="flex items-center space-x-2">
                <button
                  type="button"
                  onClick={onClose}
                  className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition"
                >
                  Đóng
                </button>
                <button
                  type="button"
                  disabled={selectedUserIds.length === 0 || submitting}
                  onClick={handleShareSubmit}
                  className="rounded-xl bg-indigo-600 px-5 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-1.5 shadow-xs"
                >
                  {submitting && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                  <span>Chia sẻ ({selectedUserIds.length})</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: Active Shares List */}
        {activeTab === "shares" && (
          <div className="flex-1 flex flex-col overflow-hidden p-5 space-y-4">
            <div className="text-xs text-slate-500">
              Danh sách những người đang có quyền xem kịch bản này. Bạn có thể thu hồi quyền truy cập
              bất cứ lúc nào.
            </div>

            <div className="flex-1 overflow-y-auto space-y-2 border border-slate-100 rounded-xl p-2 min-h-[260px] max-h-[340px]">
              {loadingShares ? (
                <div className="flex flex-col items-center justify-center h-48 space-y-2 text-slate-400">
                  <Loader2 className="h-6 w-6 animate-spin text-indigo-600" />
                  <span className="text-xs">Đang tải danh sách chia sẻ...</span>
                </div>
              ) : activeShares.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-48 space-y-2 text-slate-400">
                  <UserMinus className="h-8 w-8 text-slate-300" />
                  <span className="text-xs text-slate-500">Kịch bản chưa được chia sẻ cho ai.</span>
                  <button
                    type="button"
                    onClick={() => setActiveTab("candidates")}
                    className="text-xs font-semibold text-indigo-600 hover:text-indigo-700 mt-1"
                  >
                    Chọn người nhận để chia sẻ ngay →
                  </button>
                </div>
              ) : (
                activeShares.map((share) => {
                  const isRevoking = revokingUserId === share.userId;
                  return (
                    <div
                      key={share.id}
                      className="p-3 bg-white rounded-xl border border-slate-200 shadow-2xs flex items-center justify-between"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center space-x-2">
                          <span className="font-bold text-xs text-slate-900">{share.fullName}</span>
                          <span className="font-mono text-[11px] text-slate-400">
                            @{share.username}
                          </span>
                          {renderRoleBadge(share.roleCode)}
                          <span className="px-2 py-0.2 rounded-full text-[10px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            Chỉ xem ({share.permission})
                          </span>
                        </div>
                        <div className="text-[11px] text-slate-500">
                          Chia sẻ lúc {formatDateTime(share.sharedAt)} bởi {share.sharedByName}
                        </div>
                      </div>

                      <button
                        type="button"
                        disabled={isRevoking}
                        onClick={() => handleRevokeShare(share.userId, share.fullName)}
                        className="rounded-lg px-2.5 py-1 text-xs font-semibold text-rose-600 hover:bg-rose-50 border border-rose-200 hover:border-rose-300 transition disabled:opacity-50 flex items-center space-x-1"
                      >
                        {isRevoking ? (
                          <Loader2 className="h-3 w-3 animate-spin text-rose-600" />
                        ) : (
                          <UserMinus className="h-3.5 w-3.5 text-rose-600" />
                        )}
                        <span>Thu hồi</span>
                      </button>
                    </div>
                  );
                })
              )}
            </div>

            <div className="pt-3 border-t border-slate-100 flex justify-end">
              <button
                type="button"
                onClick={onClose}
                className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition"
              >
                Đóng
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};