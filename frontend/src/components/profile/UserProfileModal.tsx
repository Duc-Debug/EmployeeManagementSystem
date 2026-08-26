import { useState } from "react";
import { Settings, Lock, Edit3, X } from "lucide-react";
import EditProfileModal from "./EditProfileModal";
import ChangePasswordModal from "./ChangePasswordModal";

interface UserProfileModalProps {
    isOpen: boolean;
    onClose: () => void;
}

export default function UserProfileModal({ isOpen, onClose }: UserProfileModalProps) {
    const [isEditOpen, setIsEditOpen] = useState(false);
    const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false);

    // State lưu thông tin tài khoản
    const [userInfo, setUserInfo] = useState({
        name: "Chu Văn Hưng",
        email: "hungwgg01@gmail.com",
        role: "Trưởng phòng",
        department: "Phòng Công Nghệ Thông Tin",
    });

    const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

    if (!isOpen) return null;

    return (
        <>
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm animate-fadeIn">
                <div className="w-full max-w-xl rounded-2xl bg-white p-6 shadow-2xl border border-slate-100 text-slate-800">
                    {/* Header */}
                    <div className="flex items-center justify-between border-b pb-3 mb-4 border-slate-100">
                        <div className="flex items-center gap-2">
                            <Settings className="h-5 w-5 text-[#4338ca]" />
                            <h2 className="text-lg font-bold text-slate-800">Cài đặt tài khoản</h2>
                        </div>
                        <button
                            onClick={onClose}
                            className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
                        >
                            <X className="h-5 w-5" />
                        </button>
                    </div>

                    {/* Thông báo */}
                    {message && (
                        <div
                            className={`mb-4 rounded-lg p-3 text-xs font-medium ${
                                message.type === "success"
                                    ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                                    : "bg-rose-50 text-rose-700 border border-rose-200"
                            }`}
                        >
                            {message.text}
                        </div>
                    )}

                    {/* Nội dung thông tin tài khoản */}
                    <div className="rounded-xl bg-slate-50 p-5 border border-slate-100 mb-6">
                        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-4">
                            Thông tin tài khoản
                        </h3>
                        <div className="grid grid-cols-2 gap-4 text-xs">
                            <div>
                                <span className="text-slate-400 block mb-0.5">Họ và tên:</span>
                                <p className="font-bold text-slate-800 text-sm">{userInfo.name}</p>
                            </div>
                            <div>
                                <span className="text-slate-400 block mb-0.5">Email:</span>
                                <p className="font-semibold text-slate-800 text-sm">{userInfo.email}</p>
                            </div>
                            <div>
                                <span className="text-slate-400 block mb-0.5">Chức vụ:</span>
                                <p className="font-semibold text-slate-800 text-sm">{userInfo.role}</p>
                            </div>
                            <div>
                                <span className="text-slate-400 block mb-0.5">Phòng ban:</span>
                                <p className="font-semibold text-slate-800 text-sm">{userInfo.department}</p>
                            </div>
                        </div>
                    </div>

                    {/* Các nút bấm hành động */}
                    <div className="flex items-center justify-end gap-3 border-t border-slate-100 pt-4">
                        <button
                            onClick={() => {
                                setMessage(null);
                                setIsChangePasswordOpen(true);
                            }}
                            className="flex items-center gap-1.5 rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
                        >
                            <Lock className="h-3.5 w-3.5" /> Đổi mật khẩu
                        </button>
                        <button
                            onClick={() => {
                                setMessage(null);
                                setIsEditOpen(true);
                            }}
                            className="flex items-center gap-1.5 rounded-xl bg-[#4338ca] px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-[#3730a3] transition"
                        >
                            <Edit3 className="h-3.5 w-3.5" /> Sửa thông tin
                        </button>
                    </div>
                </div>
            </div>

            {/* Modal chỉnh sửa thông tin */}
            <EditProfileModal
                isOpen={isEditOpen}
                userInfo={userInfo}
                onClose={() => setIsEditOpen(false)}
                onSave={(updatedData) => {
                    setUserInfo(updatedData);
                    setIsEditOpen(false);
                    setMessage({ type: "success", text: "Cập nhật thông tin tài khoản thành công!" });
                }}
            />

            {/* Modal đổi mật khẩu */}
            <ChangePasswordModal
                isOpen={isChangePasswordOpen}
                onClose={() => setIsChangePasswordOpen(false)}
                onSuccess={() => {
                    setIsChangePasswordOpen(false);
                    setMessage({ type: "success", text: "Đổi mật khẩu thành công!" });
                }}
            />
        </>
    );
}