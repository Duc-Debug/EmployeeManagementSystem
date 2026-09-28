"use client";

import { useState } from "react";
import { AlertTriangle, Sliders } from "lucide-react";
import { ProlongedIdlenessWarningModal } from "./ProlongedIdlenessWarningModal";

export default function ProlongedIdlenessView() {
  const [isOpen, setIsOpen] = useState(true);

  return (
    <div className="flex flex-col h-full w-full space-y-4">
      <div className="flex items-center justify-between rounded-3xl border border-amber-200 bg-amber-50/50 p-6 shadow-xs">
        <div className="flex items-center gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-500 text-white shadow-xs">
            <AlertTriangle className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-slate-900">
              Cảnh Báo Nhân Sự Nhàn Rỗi Kéo Dài
            </h1>
            <p className="text-xs text-slate-500 mt-0.5">
              Phát hiện nhân sự có mức sử dụng dưới ngưỡng ban hành trong nhiều tuần liên tiếp .
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={() => setIsOpen(true)}
          className="rounded-2xl bg-amber-600 px-4 py-2.5 text-xs font-semibold text-white hover:bg-amber-700 transition shadow-xs flex items-center gap-1.5"
        >
          <Sliders className="h-4 w-4" />
          <span>Mở Bảng Rà Soát & Xử Lý</span>
        </button>
      </div>

      <ProlongedIdlenessWarningModal open={isOpen} onClose={() => setIsOpen(false)} />
    </div>
  );
}
