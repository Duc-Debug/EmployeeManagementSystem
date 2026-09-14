import { useState, useEffect } from "react";
import {
  TrendingUp,
  AlertTriangle,
  CheckCircle2,
  Users,
  Search,
  Filter,
  RefreshCw,
  FileSpreadsheet,
  Building2,
  Calendar,
  Check
} from "lucide-react";
import { cn } from "@/lib/utils";
import {
  getRecruitmentDemandReport,
  downloadRecruitmentDemandReport,
  type RecruitmentDemandReportData
} from "@/lib/api/recruitment-demand";
import { getIsoWeeksInYear } from "@/lib/iso-week";
import { getOrgTree } from "@/lib/api/org-units";
import type { OrgUnitTreeNode } from "@/types/hrm";

export default function RecruitmentDemandReportView() {
  const currentYear = new Date().getFullYear();
  const [fromYear, setFromYear] = useState<number>(currentYear);
  const [fromWeek, setFromWeek] = useState<number>(1);
  const [toYear, setToYear] = useState<number>(currentYear);
  const [toWeek, setToWeek] = useState<number>(getIsoWeeksInYear(currentYear));
  const [selectedOrgUnitId, setSelectedOrgUnitId] = useState<string>("");

  const [reportData, setReportData] = useState<RecruitmentDemandReportData | null>(null);
  const [orgUnits, setOrgUnits] = useState<OrgUnitTreeNode[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [filterKeyword, setFilterKeyword] = useState<string>("");
  const [showConfirmModal, setShowConfirmModal] = useState<boolean>(false);
  const [auditNotice, setAuditNotice] = useState<string | null>(null);

  useEffect(() => {
    async function loadOrgUnitsData() {
      try {
        const tree = await getOrgTree();
        const flatten = (nodes: readonly OrgUnitTreeNode[]): OrgUnitTreeNode[] => {
          let list: OrgUnitTreeNode[] = [];
          for (const n of nodes) {
            list.push(n);
            if (n.children && n.children.length > 0) {
              list = list.concat(flatten(n.children));
            }
          }
          return list;
        };
        setOrgUnits(flatten([...(tree || [])]));
      } catch (err) {
        console.warn("Không thể tải danh sách phòng ban:", err);
      }
    }
    loadOrgUnitsData();
  }, []);

  const fetchReport = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getRecruitmentDemandReport({
        fromYear,
        fromWeek,
        toYear,
        toWeek,
        orgUnitId: selectedOrgUnitId ? Number(selectedOrgUnitId) : undefined
      });
      setReportData(data);
    } catch (err: any) {
      console.error("Lỗi khi tải báo cáo nhu cầu tuyển dụng:", err);
      setError(err?.message || "Không thể kết nối đến máy chủ hoặc bạn không có quyền truy cập báo cáo này.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchReport();
  }, [fromYear, fromWeek, toYear, toWeek, selectedOrgUnitId]);

  const filteredSkills = (reportData?.skills || []).filter((s) => {
    if (!filterKeyword.trim()) return true;
    const kw = filterKeyword.toLowerCase();
    return (
      s.skillCode.toLowerCase().includes(kw) ||
      s.skillName.toLowerCase().includes(kw) ||
      s.category.toLowerCase().includes(kw)
    );
  });

  const handleConfirmAction = () => {
    setShowConfirmModal(true);
  };

  const handleExecuteExport = async () => {
    try {
      await downloadRecruitmentDemandReport({
        fromYear, fromWeek, toYear, toWeek,
        orgUnitId: selectedOrgUnitId ? Number(selectedOrgUnitId) : undefined,
      });
    setShowConfirmModal(false);
    const nowStr = new Date().toLocaleString("vi-VN");
    setAuditNotice(`Đã ghi nhận nhật ký thao tác xuất báo cáo thành công lúc ${nowStr}.`);
    setTimeout(() => setAuditNotice(null), 5000);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể xuất báo cáo.");
    }
  };

  useEffect(() => {
    const maxWeek = getIsoWeeksInYear(fromYear);
    if (fromWeek > maxWeek) setFromWeek(maxWeek);
  }, [fromYear, fromWeek]);

  useEffect(() => {
    const maxWeek = getIsoWeeksInYear(toYear);
    if (toWeek > maxWeek) setToWeek(maxWeek);
  }, [toYear, toWeek]);

  return (
    <div className="space-y-6 pb-12">
      {/* Header section */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between rounded-3xl bg-white p-6 border border-slate-200 shadow-xs">
        <div>
          <div className="flex items-center gap-2.5 mb-1">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 border border-indigo-100">
              <TrendingUp className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-slate-900">
                Báo cáo nhu cầu tuyển dụng theo kỹ năng
              </h1>
              <p className="text-xs text-slate-500">
                Mã nghiệp vụ: <span className="font-semibold text-slate-700">NCL-10-CN-005</span> • Ban Giám Đốc & HR
              </p>
            </div>
          </div>
          <p className="mt-2 text-xs text-slate-600 max-w-3xl leading-relaxed">
            Hệ thống so sánh tổng số giờ nhu cầu nhân sự của các dự án theo vai trò/kỹ năng với năng lực hiện có của nhân sự, tổng hợp số giờ thiếu hụt thực tế giúp Ban Giám Đốc quyết định tuyển thêm hoặc thuê ngoài dựa trên số liệu khách quan.
          </p>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={fetchReport}
            disabled={isLoading}
            className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-xs disabled:opacity-50"
          >
            <RefreshCw className={cn("h-4 w-4 text-slate-500", isLoading && "animate-spin")} />
            Làm mới
          </button>

          <button
            onClick={handleConfirmAction}
            className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs"
          >
            <FileSpreadsheet className="h-4 w-4" />
            Xác nhận & Xuất báo cáo
          </button>
        </div>
      </div>

      {/* Thông báo Audit Log */}
      {auditNotice && (
        <div className="flex items-center gap-3 rounded-2xl bg-emerald-50 border border-emerald-200 p-4 text-xs font-medium text-emerald-800 animate-in fade-in duration-200">
          <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0" />
          <span>{auditNotice}</span>
        </div>
      )}

      {/* Bộ lọc khoảng thời gian & phòng ban */}
      <div className="rounded-3xl bg-white p-5 border border-slate-200 shadow-xs space-y-4">
        <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-500">
          <Filter className="h-4 w-4 text-indigo-600" />
          <span>BỘ LỌC THỜI GIAN & ĐƠN VỊ</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-3">
          <div>
            <label className="block text-[11px] font-semibold text-slate-600 mb-1 flex items-center gap-1">
              <Calendar className="h-3.5 w-3.5 text-slate-400" /> Năm bắt đầu
            </label>
            <select
              value={fromYear}
              onChange={(e) => setFromYear(Number(e.target.value))}
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-semibold text-slate-800 focus:bg-white focus:border-indigo-500 focus:outline-none transition"
            >
              {[currentYear - 1, currentYear, currentYear + 1].map((y) => (
                <option key={y} value={y}>Năm {y}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-semibold text-slate-600 mb-1">
              Tuần bắt đầu
            </label>
            <select
              value={fromWeek}
              onChange={(e) => setFromWeek(Number(e.target.value))}
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-semibold text-slate-800 focus:bg-white focus:border-indigo-500 focus:outline-none transition"
            >
              {Array.from({ length: getIsoWeeksInYear(fromYear) }, (_, i) => i + 1).map((w) => (
                <option key={w} value={w}>Tuần {w}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-semibold text-slate-600 mb-1 flex items-center gap-1">
              <Calendar className="h-3.5 w-3.5 text-slate-400" /> Năm kết thúc
            </label>
            <select
              value={toYear}
              onChange={(e) => setToYear(Number(e.target.value))}
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-semibold text-slate-800 focus:bg-white focus:border-indigo-500 focus:outline-none transition"
            >
              {[currentYear - 1, currentYear, currentYear + 1].map((y) => (
                <option key={y} value={y}>Năm {y}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-semibold text-slate-600 mb-1">
              Tuần kết thúc
            </label>
            <select
              value={toWeek}
              onChange={(e) => setToWeek(Number(e.target.value))}
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-semibold text-slate-800 focus:bg-white focus:border-indigo-500 focus:outline-none transition"
            >
              {Array.from({ length: getIsoWeeksInYear(toYear) }, (_, i) => i + 1).map((w) => (
                <option key={w} value={w}>Tuần {w}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-semibold text-slate-600 mb-1 flex items-center gap-1">
              <Building2 className="h-3.5 w-3.5 text-slate-400" /> Phòng ban / Đơn vị
            </label>
            <select
              value={selectedOrgUnitId}
              onChange={(e) => setSelectedOrgUnitId(e.target.value)}
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-semibold text-slate-800 focus:bg-white focus:border-indigo-500 focus:outline-none transition"
            >
              <option value="">Tất cả phòng ban</option>
              {orgUnits.map((u) => (
                <option key={u.id} value={u.id}>{u.unitName}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* KPI Cards section */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="rounded-3xl bg-white p-5 border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500">Tổng kỹ năng đánh giá</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-slate-100 text-slate-600">
              <Users className="h-4 w-4" />
            </div>
          </div>
          <p className="mt-3 text-2xl font-bold text-slate-900">
            {reportData ? reportData.totalSkillsEvaluated : 0}
          </p>
          <p className="mt-1 text-[11px] text-slate-500">Kỹ năng trong danh mục chuẩn</p>
        </div>

        <div className="rounded-3xl bg-white p-5 border border-rose-200 bg-rose-50/20 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-rose-700">Tổng giờ thiếu hụt</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-rose-100 text-rose-600">
              <AlertTriangle className="h-4 w-4" />
            </div>
          </div>
          <p className="mt-3 text-2xl font-bold text-rose-600">
            {reportData ? reportData.totalDeficitHours.toLocaleString("vi-VN") : 0} <span className="text-sm font-normal text-rose-500">giờ</span>
          </p>
          <p className="mt-1 text-[11px] text-rose-600/80 font-medium">Cần bổ sung nhân sự/thuê ngoài</p>
        </div>

        <div className="rounded-3xl bg-white p-5 border border-amber-200 bg-amber-50/20 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-amber-700">Kỹ năng thiếu nhân lực</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-amber-100 text-amber-600">
              <TrendingUp className="h-4 w-4" />
            </div>
          </div>
          <p className="mt-3 text-2xl font-bold text-amber-700">
            {reportData ? reportData.skillsWithDeficitCount : 0} <span className="text-sm font-normal text-amber-600">kỹ năng</span>
          </p>
          <p className="mt-1 text-[11px] text-amber-600/80 font-medium">Vượt quá năng lực hiện có</p>
        </div>

        <div className="rounded-3xl bg-white p-5 border border-emerald-200 bg-emerald-50/20 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-emerald-700">Kỹ năng đủ năng lực</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600">
              <CheckCircle2 className="h-4 w-4" />
            </div>
          </div>
          <p className="mt-3 text-2xl font-bold text-emerald-600">
            {reportData ? reportData.totalSkillsEvaluated - reportData.skillsWithDeficitCount : 0} <span className="text-sm font-normal text-emerald-600">kỹ năng</span>
          </p>
          <p className="mt-1 text-[11px] text-emerald-600/80 font-medium">Đáp ứng đủ nhu cầu dự án</p>
        </div>
      </div>

      {/* Hiển thị lỗi nếu có */}
      {error && (
        <div className="rounded-3xl bg-rose-50 border border-rose-200 p-6 text-center text-rose-700">
          <AlertTriangle className="h-8 w-8 text-rose-600 mx-auto mb-2" />
          <h3 className="text-sm font-bold">Không thể tải báo cáo</h3>
          <p className="text-xs text-rose-600 mt-1 max-w-md mx-auto">{error}</p>
        </div>
      )}

      {/* Kịch bản Dữ liệu rỗng - Năng lực đủ cho mọi nhu cầu (TC-02) */}
      {!isLoading && !error && reportData && reportData.skillsWithDeficitCount === 0 && (
        <div className="rounded-3xl bg-emerald-50/80 border border-emerald-200 p-8 text-center animate-in fade-in duration-200">
          <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-600 mb-3 border border-emerald-200">
            <CheckCircle2 className="h-7 w-7" />
          </div>
          <h3 className="text-base font-bold text-emerald-900">
            Năng lực hiện có đủ cho mọi nhu cầu
          </h3>
          <p className="text-xs text-emerald-700 max-w-lg mx-auto mt-1 leading-relaxed">
            Hệ thống báo không có kỹ năng nào đang thiếu trong khoảng thời gian đã chọn (<span className="font-semibold">{reportData.timeRangeText}</span>). Nguồn lực nội bộ hiện đáp ứng hoàn toàn nhu cầu ước tính của các dự án.
          </p>
        </div>
      )}

      {/* Bảng tổng hợp chi tiết nhu cầu tuyển dụng */}
      {!isLoading && !error && reportData && (
        <div className="rounded-3xl bg-white border border-slate-200 shadow-xs overflow-hidden">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-5 border-b border-slate-100 bg-slate-50/50">
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-bold text-slate-800">
                Chi tiết nhu cầu & thiếu hụt theo kỹ năng
              </h2>
              <span className="rounded-full bg-slate-200/70 px-2.5 py-0.5 text-[11px] font-semibold text-slate-700">
                {filteredSkills.length} bản ghi
              </span>
            </div>

            <div className="relative max-w-xs w-full">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
              <input
                type="text"
                placeholder="Tìm mã, tên kỹ năng..."
                value={filterKeyword}
                onChange={(e) => setFilterKeyword(e.target.value)}
                className="w-full rounded-xl border border-slate-200 bg-white pl-9 pr-3 py-1.5 text-xs text-slate-800 focus:border-indigo-500 focus:outline-none transition"
              />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                  <th className="py-3.5 px-4">Mã kỹ năng</th>
                  <th className="py-3.5 px-4">Tên kỹ năng</th>
                  <th className="py-3.5 px-4">Nhóm kỹ năng</th>
                  <th className="py-3.5 px-4 text-right">Nhu cầu dự án (giờ)</th>
                  <th className="py-3.5 px-4 text-right">Năng lực hiện có (giờ)</th>
                  <th className="py-3.5 px-4 text-right">Số giờ thiếu (giờ)</th>
                  <th className="py-3.5 px-4 text-center">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {filteredSkills.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="py-8 text-center text-slate-400 italic">
                      Không tìm thấy bản ghi kỹ năng thỏa mãn điều kiện lọc.
                    </td>
                  </tr>
                ) : (
                  filteredSkills.map((item) => (
                    <tr
                      key={item.skillId}
                      className={cn(
                        "hover:bg-slate-50/80 transition-colors",
                        item.status === "DEFICIT" && "bg-rose-50/30"
                      )}
                    >
                      <td className="py-3.5 px-4 font-mono font-bold text-indigo-600">
                        {item.skillCode}
                      </td>
                      <td className="py-3.5 px-4 font-semibold text-slate-900">
                        {item.skillName}
                      </td>
                      <td className="py-3.5 px-4 text-slate-500">
                        <span className="inline-flex items-center rounded-lg bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-600">
                          {item.category}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono font-semibold text-slate-800">
                        {item.requiredDemandHours.toLocaleString("vi-VN")} h
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono text-slate-600">
                        {item.availableCapacityHours.toLocaleString("vi-VN")} h
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono font-bold">
                        {item.shortfallHours > 0 ? (
                          <span className="text-rose-600 text-sm">
                            +{item.shortfallHours.toLocaleString("vi-VN")} h
                          </span>
                        ) : (
                          <span className="text-slate-400">0 h</span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        {item.status === "DEFICIT" ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2.5 py-1 text-[11px] font-bold text-rose-700 border border-rose-200">
                            <AlertTriangle className="h-3 w-3" /> Thiếu nhân lực
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-1 text-[11px] font-bold text-emerald-700 border border-emerald-200">
                            <Check className="h-3 w-3" /> Đủ năng lực
                          </span>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal xác nhận thao tác & ghi nhật ký (TC-04) */}
      {showConfirmModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4 animate-in fade-in duration-150">
          <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 border border-indigo-100">
              <FileSpreadsheet className="h-6 w-6" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Xác nhận thao tác xuất báo cáo
              </h3>
              <p className="text-xs text-slate-500 mt-1 leading-relaxed">
                Hệ thống sẽ ghi nhận lịch sử lượt thao tác này vào nhật ký báo cáo (Audit Log) bao gồm: Người thực hiện, Nội dung báo cáo và Thời điểm xác nhận.
              </p>
            </div>

            <div className="rounded-2xl bg-slate-50 p-3.5 text-xs space-y-1.5 border border-slate-200/60 font-mono">
              <p><span className="text-slate-400">Thời gian:</span> {reportData?.timeRangeText || "Toàn bộ"}</p>
              <p><span className="text-slate-400">Số giờ thiếu:</span> {reportData?.totalDeficitHours || 0} giờ</p>
              <p><span className="text-slate-400">Số kỹ năng thiếu:</span> {reportData?.skillsWithDeficitCount || 0} kỹ năng</p>
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowConfirmModal(false)}
                className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleExecuteExport}
                className="rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs"
              >
                Xác nhận
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
