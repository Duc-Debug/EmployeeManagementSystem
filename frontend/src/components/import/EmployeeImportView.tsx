import { useState, useRef } from "react";
import {
  UploadCloud,
  FileSpreadsheet,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Download,
  RotateCcw,
  ShieldAlert,
  Loader2,
  Check,
  Search,
  Filter,
  Users,
} from "lucide-react";
import {
  type ImportEmployeePreviewResult,
  type ImportExecutionResult,
  previewEmployeeImport,
  confirmEmployeeImport,
  downloadEmployeeTemplate,
  exportErrorRowsToCsv,
} from "@/lib/api/employee-import";
import { useAuthUser } from "@/lib/auth-session";
import { cn } from "@/lib/utils";

type FilterTab = "ALL" | "VALID" | "INVALID";

export interface EmployeeImportViewProps {
  onSuccess?: (result: ImportExecutionResult) => void;
  onClose?: () => void;
}

export default function EmployeeImportView({ onSuccess, onClose }: EmployeeImportViewProps = {}) {
  const user = useAuthUser();
  const [file, setFile] = useState<File | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [isConfirming, setIsConfirming] = useState(false);
  const [previewResult, setPreviewResult] = useState<ImportEmployeePreviewResult | null>(null);
  const [executionResult, setExecutionResult] = useState<ImportExecutionResult | null>(null);
  const [activeFilter, setActiveFilter] = useState<FilterTab>("ALL");
  const [searchQuery, setSearchQuery] = useState("");
  const [generalError, setGeneralError] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const normalizedRole = user?.roleCode ? user.roleCode.toUpperCase().replace(/_/g, "-") : "";
  const hasImportPermission =
    user?.permissions?.includes("DATA_IMPORT") === true ||
    ["VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalizedRole);

  if (!hasImportPermission) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center p-8 bg-white rounded-3xl border border-slate-200 shadow-xs">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-rose-50 text-rose-600 mb-4 border border-rose-100">
          <ShieldAlert className="h-8 w-8" />
        </div>
        <h3 className="text-base font-bold text-slate-900 mb-1">
          Không có quyền truy cập
        </h3>
        <p className="text-xs text-slate-500 max-w-md mb-6 leading-relaxed">
          Chức năng nhập dữ liệu từ tệp (NCL-12-CN-004) chỉ dành cho Quản trị viên hệ thống (VT-06) hoặc tài khoản có quyền DATA_IMPORT.
        </p>
      </div>
    );
  }

  const handleFileSelect = (selectedFile: File) => {
    const validExtensions = [".xlsx", ".xls"];
    const fileExt = selectedFile.name.substring(selectedFile.name.lastIndexOf(".")).toLowerCase();
    if (!validExtensions.includes(fileExt)) {
      setGeneralError("Định dạng tệp không được hỗ trợ. Vui lòng chọn tệp .xlsx hoặc .xls.");
      return;
    }

    setFile(selectedFile);
    setGeneralError(null);
    setExecutionResult(null);
    handleUploadAndPreview(selectedFile);
  };

  const handleUploadAndPreview = async (fileToPreview: File) => {
    try {
      setIsUploading(true);
      setGeneralError(null);
      const result = await previewEmployeeImport(fileToPreview);
      setPreviewResult(result);
      if (result.invalidRows > 0 && result.validRows === 0) {
        setActiveFilter("INVALID");
      } else {
        setActiveFilter("ALL");
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Đã xảy ra lỗi khi xử lý tệp dữ liệu";
      setGeneralError(msg);
      setPreviewResult(null);
    } finally {
      setIsUploading(false);
    }
  };

  const handleConfirmImport = async () => {
    if (!previewResult || previewResult.validRows === 0) return;

    try {
      setIsConfirming(true);
      setGeneralError(null);
      const result = await confirmEmployeeImport(previewResult.rows);
      setExecutionResult(result);
      if (onSuccess && result.importedCount > 0) {
        onSuccess(result);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Đã xảy ra lỗi khi lưu dữ liệu nhân sự";
      setGeneralError(msg);
    } finally {
      setIsConfirming(false);
    }
  };

  const handleReset = () => {
    setFile(null);
    setPreviewResult(null);
    setExecutionResult(null);
    setGeneralError(null);
    setSearchQuery("");
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const filteredRows = (previewResult?.rows || []).filter((row) => {
    if (activeFilter === "VALID" && !row.valid) return false;
    if (activeFilter === "INVALID" && row.valid) return false;

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      const codeMatch = row.employeeCode?.toLowerCase().includes(q);
      const nameMatch = row.fullName?.toLowerCase().includes(q);
      const userMatch = row.username?.toLowerCase().includes(q);
      const emailMatch = row.email?.toLowerCase().includes(q);
      const deptMatch = row.orgUnitIdentifier?.toLowerCase().includes(q);
      return codeMatch || nameMatch || userMatch || emailMatch || deptMatch;
    }
    return true;
  });

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white p-6 rounded-3xl border border-slate-200/80 shadow-xs">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-100">
              NCL-12-CN-004
            </span>
            <span className="text-xs text-slate-500 font-medium">Hệ thống & Cài đặt</span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Nhập dữ liệu nhân sự từ tệp
          </h1>
          <p className="text-xs text-slate-500 mt-1 max-w-2xl">
            Tải lên bảng tính Excel (.xlsx, .xls) chứa danh sách nhân viên mới. Hệ thống sẽ tự động xác thực tính hợp lệ, phát hiện lỗi và phân bổ tài khoản.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => downloadEmployeeTemplate("xlsx")}
            className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 hover:border-slate-400 transition shadow-2xs"
          >
            <FileSpreadsheet className="h-4 w-4 text-emerald-600" />
            Biểu mẫu Excel (.xlsx)
          </button>
          {onClose && (
            <button
              type="button"
              onClick={onClose}
              className="p-2 rounded-xl text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition"
              title="Đóng"
            >
              ✕
            </button>
          )}
        </div>
      </div>

      {/* Error banner */}
      {generalError && (
        <div className="flex items-start gap-3 p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs shadow-xs animate-in slide-in-from-top-1">
          <AlertTriangle className="h-5 w-5 text-rose-600 shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="font-semibold">Đã xảy ra lỗi</p>
            <p className="mt-0.5 text-rose-700">{generalError}</p>
          </div>
          <button
            type="button"
            onClick={() => setGeneralError(null)}
            className="text-rose-500 hover:text-rose-700 font-bold"
          >
            ✕
          </button>
        </div>
      )}

      {/* Execution Result Modal / Banner */}
      {executionResult && (
        <div className="bg-white p-6 rounded-3xl border border-emerald-200 shadow-sm animate-in zoom-in-95 duration-200">
          <div className="flex items-start gap-4">
            <div className="h-12 w-12 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 border border-emerald-100">
              <CheckCircle2 className="h-7 w-7" />
            </div>
            <div className="flex-1">
              <h3 className="text-base font-bold text-slate-900">
                Nhập dữ liệu thành công!
              </h3>
              <p className="text-xs text-slate-600 mt-1">
                {executionResult.message}
              </p>

              <div className="flex flex-wrap gap-4 mt-4">
                <div className="bg-emerald-50 border border-emerald-100 px-4 py-2 rounded-xl">
                  <span className="text-xs text-emerald-700 font-medium">Hồ sơ đã tạo thành công: </span>
                  <span className="text-sm font-bold text-emerald-800">{executionResult.importedCount}</span>
                </div>
                {executionResult.skippedCount > 0 && (
                  <div className="bg-amber-50 border border-amber-100 px-4 py-2 rounded-xl">
                    <span className="text-xs text-amber-700 font-medium">Dòng bị bỏ qua: </span>
                    <span className="text-sm font-bold text-amber-800">{executionResult.skippedCount}</span>
                  </div>
                )}
              </div>

              {executionResult.errors && executionResult.errors.length > 0 && (
                <div className="mt-4 p-3 bg-amber-50/50 rounded-xl border border-amber-200 text-xs text-amber-900">
                  <p className="font-semibold mb-1">Cảnh báo trong quá trình lưu:</p>
                  <ul className="list-disc pl-4 space-y-1">
                    {executionResult.errors.map((err, idx) => (
                      <li key={idx}>{err}</li>
                    ))}
                  </ul>
                </div>
              )}

              <div className="mt-6 flex flex-wrap items-center gap-3">
                <button
                  type="button"
                  onClick={handleReset}
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 transition shadow-xs"
                >
                  <RotateCcw className="h-4 w-4" />
                  Nhập tệp dữ liệu khác
                </button>
                {onClose && (
                  <button
                    type="button"
                    onClick={onClose}
                    className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 transition"
                  >
                    Đóng & Về danh sách tài khoản
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Step 1: Upload Dropzone */}
      {!previewResult && !executionResult && (
        <div className="bg-white p-8 rounded-3xl border border-slate-200 shadow-xs">
          <div
            onDragOver={(e) => {
              e.preventDefault();
              setIsDragging(true);
            }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={(e) => {
              e.preventDefault();
              setIsDragging(false);
              if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                handleFileSelect(e.dataTransfer.files[0]);
              }
            }}
            onClick={() => fileInputRef.current?.click()}
            className={cn(
              "border-2 border-dashed rounded-2xl p-12 text-center cursor-pointer transition-all flex flex-col items-center justify-center gap-4",
              isDragging
                ? "border-indigo-500 bg-indigo-50/50 scale-[0.99]"
                : "border-slate-300 hover:border-indigo-400 hover:bg-slate-50/50"
            )}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept=".xlsx,.xls"
              className="hidden"
              onChange={(e) => {
                if (e.target.files && e.target.files[0]) {
                  handleFileSelect(e.target.files[0]);
                }
              }}
            />

            <div className="h-16 w-16 rounded-3xl bg-indigo-50 text-indigo-600 flex items-center justify-center border border-indigo-100 shadow-xs">
              {isUploading ? (
                <Loader2 className="h-8 w-8 animate-spin text-indigo-600" />
              ) : (
                <UploadCloud className="h-8 w-8 text-indigo-600" />
              )}
            </div>

            <div>
              <p className="text-sm font-bold text-slate-800">
                {isUploading
                  ? "Đang tải lên và phân tích tệp dữ liệu..."
                  : "Kéo và thả tệp dữ liệu nhân sự vào đây, hoặc nhấn để chọn"}
              </p>
              <p className="text-xs text-slate-500 mt-1">
                Hỗ trợ các định dạng Microsoft Excel (.xlsx, .xls). Dung lượng tối đa 10MB.
              </p>
            </div>

            {!isUploading && (
              <button
                type="button"
                className="mt-2 px-4 py-2 rounded-xl text-xs font-semibold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 border border-indigo-200 transition"
              >
                Chọn tệp từ máy tính
              </button>
            )}
          </div>

          {/* Quick guide */}
          <div className="mt-8 grid grid-cols-1 md:grid-cols-3 gap-4 pt-6 border-t border-slate-100 text-xs text-slate-600">
            <div className="flex items-start gap-3">
              <span className="flex h-6 w-6 rounded-full bg-slate-100 text-slate-700 font-bold text-xs items-center justify-center shrink-0">
                1
              </span>
              <div>
                <p className="font-semibold text-slate-800">Tải biểu mẫu chuẩn</p>
                <p className="text-slate-500 mt-0.5">Sử dụng đúng các cột tiêu đề theo mẫu chuẩn của hệ thống.</p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <span className="flex h-6 w-6 rounded-full bg-slate-100 text-slate-700 font-bold text-xs items-center justify-center shrink-0">
                2
              </span>
              <div>
                <p className="font-semibold text-slate-800">Kiểm tra & Xem trước</p>
                <p className="text-slate-500 mt-0.5">Hệ thống quét lỗi định dạng email, trùng lặp mã NV, tên phòng ban.</p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <span className="flex h-6 w-6 rounded-full bg-slate-100 text-slate-700 font-bold text-xs items-center justify-center shrink-0">
                3
              </span>
              <div>
                <p className="font-semibold text-slate-800">Xác nhận nhập dữ liệu</p>
                <p className="text-slate-500 mt-0.5">Hỗ trợ nhập từng phần (Partial Import) và tải tệp chứa các dòng bị lỗi.</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Step 2: Preview Table and Actions */}
      {previewResult && !executionResult && (
        <div className="space-y-6">
          {/* KPI Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-xs flex items-center justify-between">
              <div>
                <p className="text-xs font-medium text-slate-500">Tổng số dòng</p>
                <p className="text-2xl font-bold text-slate-900 mt-1">{previewResult.totalRows}</p>
              </div>
              <div className="h-12 w-12 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center">
                <Users className="h-6 w-6" />
              </div>
            </div>

            <div className="bg-white p-5 rounded-3xl border border-emerald-100 shadow-xs flex items-center justify-between">
              <div>
                <p className="text-xs font-medium text-emerald-600 font-semibold">Hợp lệ (Sẵn sàng nhập)</p>
                <p className="text-2xl font-bold text-emerald-700 mt-1">{previewResult.validRows}</p>
              </div>
              <div className="h-12 w-12 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <CheckCircle2 className="h-6 w-6" />
              </div>
            </div>

            <div className="bg-white p-5 rounded-3xl border border-rose-100 shadow-xs flex items-center justify-between">
              <div>
                <p className="text-xs font-medium text-rose-600 font-semibold">Phát hiện có lỗi</p>
                <p className="text-2xl font-bold text-rose-700 mt-1">{previewResult.invalidRows}</p>
              </div>
              <div className="h-12 w-12 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center">
                <XCircle className="h-6 w-6" />
              </div>
            </div>
          </div>

          {/* Action Toolbar */}
          <div className="bg-white p-4 rounded-3xl border border-slate-200 shadow-xs flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            {/* Filter Tabs */}
            <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-2xl w-fit">
              <button
                type="button"
                onClick={() => setActiveFilter("ALL")}
                className={cn(
                  "px-3.5 py-1.5 rounded-xl text-xs font-semibold transition",
                  activeFilter === "ALL"
                    ? "bg-white text-slate-900 shadow-2xs"
                    : "text-slate-600 hover:text-slate-900"
                )}
              >
                Tất cả ({previewResult.totalRows})
              </button>
              <button
                type="button"
                onClick={() => setActiveFilter("VALID")}
                className={cn(
                  "px-3.5 py-1.5 rounded-xl text-xs font-semibold transition flex items-center gap-1.5",
                  activeFilter === "VALID"
                    ? "bg-emerald-600 text-white shadow-2xs"
                    : "text-emerald-700 hover:bg-emerald-50"
                )}
              >
                <Check className="h-3.5 w-3.5" />
                Hợp lệ ({previewResult.validRows})
              </button>
              <button
                type="button"
                onClick={() => setActiveFilter("INVALID")}
                className={cn(
                  "px-3.5 py-1.5 rounded-xl text-xs font-semibold transition flex items-center gap-1.5",
                  activeFilter === "INVALID"
                    ? "bg-rose-600 text-white shadow-2xs"
                    : "text-rose-700 hover:bg-rose-50"
                )}
              >
                <AlertTriangle className="h-3.5 w-3.5" />
                Có lỗi ({previewResult.invalidRows})
              </button>
            </div>

            {/* Search and Action Buttons */}
            <div className="flex flex-wrap items-center gap-3">
              <div className="relative">
                <Search className="h-3.5 w-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Tìm theo tên, mã NV, username..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-8 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 w-56"
                />
              </div>

              {previewResult.invalidRows > 0 && (
                <button
                  type="button"
                  onClick={() => exportErrorRowsToCsv(previewResult.rows)}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200 transition shadow-2xs"
                >
                  <Download className="h-3.5 w-3.5" />
                  Xuất tệp dòng lỗi (.csv)
                </button>
              )}

              <button
                type="button"
                onClick={handleReset}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 transition"
              >
                <RotateCcw className="h-3.5 w-3.5" />
                Chọn tệp khác
              </button>

              <button
                type="button"
                onClick={handleConfirmImport}
                disabled={previewResult.validRows === 0 || isConfirming}
                className={cn(
                  "inline-flex items-center gap-2 px-4 py-1.5 rounded-xl text-xs font-bold text-white transition shadow-xs",
                  previewResult.validRows > 0 && !isConfirming
                    ? "bg-indigo-600 hover:bg-indigo-700 cursor-pointer"
                    : "bg-slate-300 cursor-not-allowed text-slate-500"
                )}
              >
                {isConfirming ? (
                  <>
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    Đang lưu vào CSDL...
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    Xác nhận nhập ({previewResult.validRows} hồ sơ)
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Data Preview Table */}
          <div className="bg-white rounded-3xl border border-slate-200 shadow-xs overflow-hidden">
            <div className="overflow-x-auto max-h-[550px]">
              <table className="w-full text-left border-collapse text-xs">
                <thead className="bg-slate-50/80 sticky top-0 z-10 border-b border-slate-200">
                  <tr className="text-slate-600 font-semibold">
                    <th className="py-3 px-4 w-12 text-center">Dòng</th>
                    <th className="py-3 px-4 w-28 text-center">Trạng thái</th>
                    <th className="py-3 px-4">Mã NV</th>
                    <th className="py-3 px-4">Họ và tên</th>
                    <th className="py-3 px-4">Tên đăng nhập</th>
                    <th className="py-3 px-4">Email</th>
                    <th className="py-3 px-4">Phòng ban</th>
                    <th className="py-3 px-4">Mã vai trò</th>
                    <th className="py-3 px-4">Chức danh</th>
                    <th className="py-3 px-4 text-center">Giờ/tuần</th>
                    <th className="py-3 px-4">Ngày bắt đầu</th>
                    <th className="py-3 px-4">Thuê ngoài</th>
                    <th className="py-3 px-4 min-w-[200px]">Chi tiết lỗi</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredRows.length === 0 ? (
                    <tr>
                      <td colSpan={13} className="py-12 text-center text-slate-400">
                        <Filter className="h-8 w-8 mx-auto mb-2 text-slate-300" />
                        Không tìm thấy dòng dữ liệu nào phù hợp với bộ lọc hiện tại
                      </td>
                    </tr>
                  ) : (
                    filteredRows.map((row) => (
                      <tr
                        key={row.rowNumber}
                        className={cn(
                          "transition hover:bg-slate-50/80",
                          !row.valid ? "bg-rose-50/30" : ""
                        )}
                      >
                        <td className="py-3 px-4 text-center font-mono text-slate-500 font-medium">
                          {row.rowNumber}
                        </td>
                        <td className="py-3 px-4 text-center">
                          {row.valid ? (
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                              <Check className="h-3 w-3" /> Hợp lệ
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-rose-50 text-rose-700 border border-rose-200">
                              <AlertTriangle className="h-3 w-3" /> Có lỗi
                            </span>
                          )}
                        </td>
                        <td className="py-3 px-4 font-mono font-semibold text-slate-900">
                          {row.employeeCode || <span className="text-rose-400 italic">Trống</span>}
                        </td>
                        <td className="py-3 px-4 font-medium text-slate-800">
                          {row.fullName || <span className="text-rose-400 italic">Trống</span>}
                        </td>
                        <td className="py-3 px-4 text-slate-600 font-mono">
                          {row.username || <span className="text-slate-400 italic">--</span>}
                        </td>
                        <td className="py-3 px-4 text-slate-600">
                          {row.email || <span className="text-slate-400 italic">--</span>}
                        </td>
                        <td className="py-3 px-4 text-slate-700">
                          {row.resolvedOrgUnitName ? (
                            <span className="font-medium text-slate-800">{row.resolvedOrgUnitName}</span>
                          ) : (
                            <span className="text-rose-600 font-medium">{row.orgUnitIdentifier || "Trống"}</span>
                          )}
                        </td>
                        <td className="py-3 px-4 font-mono text-slate-600">
                          {row.roleCode || "VT-04"}
                        </td>
                        <td className="py-3 px-4 text-slate-600">
                          {row.professionalRole || "--"}
                        </td>
                        <td className="py-3 px-4 text-center font-mono text-slate-700">
                          {row.standardHoursPerWeek ?? 40}h
                        </td>
                        <td className="py-3 px-4 text-slate-600">
                          {row.startDate || "--"}
                        </td>
                        <td className="py-3 px-4 text-slate-600">
                          {row.isOutsourced ? (
                            <span className="px-2 py-0.5 rounded-md bg-amber-50 text-amber-700 text-[10px] font-bold border border-amber-200">
                              Thuê ngoài
                            </span>
                          ) : (
                            <span className="text-slate-400">Nội bộ</span>
                          )}
                        </td>
                        <td className="py-3 px-4">
                          {row.errors && row.errors.length > 0 ? (
                            <div className="space-y-1">
                              {row.errors.map((err, idx) => (
                                <p
                                  key={idx}
                                  className="text-rose-700 text-[11px] leading-tight flex items-start gap-1"
                                >
                                  <span className="text-rose-500 font-bold">•</span>
                                  {err}
                                </p>
                              ))}
                            </div>
                          ) : (
                            <span className="text-emerald-600 text-[11px]">Sẵn sàng nhập</span>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            <div className="bg-slate-50 p-3 border-t border-slate-200 flex items-center justify-between text-xs text-slate-500">
              <span>Đang hiển thị {filteredRows.length} trên tổng số {previewResult.totalRows} dòng</span>
              <span>{file?.name} ({(file ? (file.size / 1024).toFixed(1) : 0)} KB)</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
