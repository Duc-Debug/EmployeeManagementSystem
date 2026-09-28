export default function PageLoading() {
    return (
        <div className="flex h-64 w-full items-center justify-center animate-in fade-in duration-200">
            <div className="flex flex-col items-center gap-3 text-slate-500">
                <div className="h-9 w-9 animate-spin rounded-full border-4 border-indigo-600 border-t-transparent shadow-xs" />
                <span className="text-xs font-semibold tracking-wide text-slate-600">Đang tải trang...</span>
            </div>
        </div>
    );
}
