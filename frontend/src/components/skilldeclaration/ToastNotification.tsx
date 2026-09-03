import { useEffect } from 'react';
import { Check } from 'lucide-react';
import type { ToastItem } from './Types.ts';

interface SingleToastProps {
    toast: ToastItem;
    onDone: (id: number) => void;
}

function SingleToast({ toast, onDone }: SingleToastProps) {
    useEffect(() => {
        const timer = setTimeout(() => {
            onDone(toast.id);
        }, 3000);
        return () => clearTimeout(timer);
    }, [toast.id, onDone]);

    return (
        <div
            onClick={() => onDone(toast.id)}
            className="flex items-center gap-3 rounded-xl border border-white/20 bg-zinc-900/90 px-4 py-3 shadow-2xl backdrop-blur-xl text-white cursor-pointer hover:bg-zinc-800/90 transition"
        >
            <div className="flex h-7 w-7 items-center justify-center rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                <Check className="h-4 w-4" />
            </div>
            <div className="flex flex-col text-xs">
                <strong className="font-bold text-white">{toast.title}</strong>
                <span className="text-white/70">{toast.message}</span>
            </div>
        </div>
    );
}

interface ToastListProps {
    toasts: ToastItem[];
    onDone: (id: number) => void;
}

export function ToastList({ toasts, onDone }: ToastListProps) {
    return (
        <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 max-w-sm">
            {toasts.map((t) => (
                <SingleToast key={t.id} toast={t} onDone={onDone} />
            ))}
        </div>
    );
}