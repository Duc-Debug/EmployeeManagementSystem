import { useState, useEffect } from "react";
import AttendanceView from "@/components/attendance/AttendanceView";
import type { AttendanceRecord } from "@/lib/hr-data";
import { useAuthUser } from "@/lib/auth-session";
import { getUsers } from "@/lib/api/users";

const INITIAL_ATTENDANCE_RECORDS: AttendanceRecord[] = [];

export default function AttendancePage() {
    const user = useAuthUser();
    const [attendanceRecords, setAttendanceRecords] = useState<AttendanceRecord[]>(() => {
        if (!user) return INITIAL_ATTENDANCE_RECORDS;
        const role = user.roleCode ? user.roleCode.toUpperCase().replace(/_/g, "-") : "";
        const canFetchAll = ["VT-01", "VT-05", "VT-06", "ROLE-HR", "HR", "ROLE-ADMIN", "ADMIN"].includes(role);
        if (!canFetchAll) {
            return [
                {
                    id: user.employeeCode || String(user.id),
                    name: user.fullName || user.username || "Nhân viên",
                    dept: user.orgUnitName || "Phòng ban",
                    inTime: "--:--",
                    outTime: "--:--",
                    hours: "0",
                    ot: "0",
                    status: "Đúng giờ",
                },
            ];
        }
        return INITIAL_ATTENDANCE_RECORDS;
    });

    useEffect(() => {
        let isMounted = true;
        const normalizedRole = user?.roleCode ? user.roleCode.toUpperCase().replace(/_/g, "-") : "";
        const canFetchAllUsers = ["VT-01", "VT-05", "VT-06", "ROLE-HR", "HR", "ROLE-ADMIN", "ADMIN"].includes(normalizedRole);

        if (!canFetchAllUsers) return;

        async function fetchRealUsers() {
            try {
                const res = await getUsers(0, 50);
                if (!isMounted || !res?.content) return;
                const mapped: AttendanceRecord[] = res.content.map((u) => ({
                    id: String(u.id),
                    name: u.fullName || u.username,
                    dept: u.orgUnitName || "Chưa gán phòng",
                    inTime: "--:--",
                    outTime: "--:--",
                    hours: "0",
                    ot: "0",
                    status: u.status === "ACTIVE" ? "Đúng giờ" : "Vắng mặt",
                }));
                setAttendanceRecords(mapped);
            } catch {
                // User lacks permission or backend unavailable
            }
        }
        fetchRealUsers();
        return () => {
            isMounted = false;
        };
    }, [user]);

    const handleClockIn = () => {
        const time = new Date().toLocaleTimeString("en-US", {
            hour12: false,
            hour: "2-digit",
            minute: "2-digit",
        });
        const currentIdStr = user?.employeeCode || (user?.id != null ? String(user.id) : "—");
        const currentName = user?.fullName || user?.username || "Tôi ";
        const currentDept = user?.orgUnitName || "Phòng chuyên môn";

        setAttendanceRecords((prev) => {
            const index = prev.findIndex(
                (rec) =>
                    rec.id === currentIdStr ||
                    rec.id === user?.employeeCode ||
                    rec.name.toLowerCase() === currentName.toLowerCase()
            );
            if (index >= 0) {
                const next = [...prev];
                next[index] = { ...next[index], inTime: time, status: "Đúng giờ" };
                return next;
            }
            return [
                {
                    id: currentIdStr,
                    name: currentName,
                    dept: currentDept,
                    inTime: time,
                    outTime: "--:--",
                    hours: "8.0",
                    ot: "0",
                    status: "Đúng giờ",
                },
                ...prev,
            ];
        });
        return time;
    };

    const handleClockOut = () => {
        const time = new Date().toLocaleTimeString("en-US", {
            hour12: false,
            hour: "2-digit",
            minute: "2-digit",
        });
        const currentIdStr = user?.employeeCode || (user?.id != null ? String(user.id) : "—");
        const currentName = user?.fullName || user?.username || "Tôi ";

        setAttendanceRecords((prev) =>
            prev.map((rec) =>
                rec.id === currentIdStr ||
                rec.id === user?.employeeCode ||
                rec.name.toLowerCase() === currentName.toLowerCase()
                    ? { ...rec, outTime: time }
                    : rec
            )
        );
        return true;
    };

    const handleEditRecord = (id: string) => {
        console.log("Sửa bản ghi chấm công:", id);
    };

    return (
        <AttendanceView
            records={attendanceRecords}
            onClockIn={handleClockIn}
            onClockOut={handleClockOut}
            onEditRecord={handleEditRecord}
        />
    );
}
