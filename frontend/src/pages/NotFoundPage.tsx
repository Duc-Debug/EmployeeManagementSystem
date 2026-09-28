import { useNavigate } from "react-router-dom";
import { Compass, ArrowLeft, Home } from "lucide-react";

export default function NotFoundPage() {
    const navigate = useNavigate();

    return (
        <div className="min-h-screen w-full flex flex-col items-center justify-center bg-[#f8fafc] text-slate-800 p-6 antialiased">
            <div className="pointer-events-none fixed inset-0 z-0 bg-slate-50/60" aria-hidden="true" />

            <div className="relative z-10 max-w-md w-full text-center bg-white rounded-3xl p-8 border border-slate-200/80 shadow-sm animate-in fade-in zoom-in-95 duration-200">
                <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 border border-indigo-100 shadow-xs">
                    <Compass className="h-10 w-10 animate-pulse" />
                </div>

                <div className="inline-block px-3 py-1 rounded-full bg-slate-100 text-slate-600 text-xs font-bold tracking-wider uppercase mb-3">
                    404 Error
                </div>

                <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 mb-2">
                    Page Not Found
                </h1>

                <p className="text-sm text-slate-500 mb-8 leading-relaxed">
                    The page you're looking for doesn't exist or has been moved to another URL.
                </p>

                <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
                    <button
                        type="button"
                        onClick={() => navigate(-1)}
                        className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:border-slate-300 transition shadow-2xs"
                    >
                        <ArrowLeft className="h-4 w-4" />
                        Go Back
                    </button>
                    <button
                        type="button"
                        onClick={() => navigate("/dashboard/overview")}
                        className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-xl bg-indigo-600 px-5 py-2.5 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs"
                    >
                        <Home className="h-4 w-4" />
                        Back to Dashboard
                    </button>
                </div>
            </div>
        </div>
    );
}
