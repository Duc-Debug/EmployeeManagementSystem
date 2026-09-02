import React from 'react';
import { CheckCircle2 } from 'lucide-react';

export interface ToastProps {
    message: string;
    visible: boolean;
}

/** Component hiển thị thông báo Toast góc dưới màn hình */
export const Toast: React.FC<ToastProps> = ({ message, visible }) => {
    if (!visible) return null;

    return (
        <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 rounded-2xl border border-purple-400/30 bg-[#1c1338]/95 px-4 py-3 text-sm font-medium text-purple-100 shadow-2xl backdrop-blur-xl animate-in fade-in slide-in-from-bottom-5 duration-200">
            <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-xl border border-emerald-400/30 bg-emerald-500/20 text-emerald-400">
                <CheckCircle2 className="h-4 w-4" />
            </div>
            <span>{message}</span>
        </div>
    );
};

export default Toast;