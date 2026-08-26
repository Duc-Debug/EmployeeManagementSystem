# Hướng Dẫn Kiến Trúc Phân Quyền & Vai Trò Hệ Thống (RBAC Specification)

> **Tài liệu chuẩn hóa dành cho toàn bộ đội ngũ phát triển (Đặc biệt là Frontend & Backend Engineers).**  
> Vui lòng đọc kỹ tài liệu này trước khi xây dựng màn hình, điều hướng Menu, bảo vệ Route hoặc cấu hình API.

---

## 1. Bản chất cốt lõi: 6 Vai trò Nghiệp vụ Chính thức

Hệ thống quản trị nhân sự và nguồn lực vận hành theo mô hình **Role-Based Access Control (RBAC)** kết hợp **Data Scope Filtering**.  
Hệ thống có **chính xác 6 Vai trò (Role)** được định nghĩa và gán trực tiếp cho tài khoản:

| Mã VT | Tên Vai Trò | Trách nhiệm chính | Phạm vi dữ liệu mặc định (Data Scope) |
| :--- | :--- | :--- | :--- |
| **`VT-01`** | **Ban giám đốc** *(Executive / Director)* | Điều hành toàn diện, theo dõi năng lực và mức độ tải của toàn bộ nhân sự. Xem kịch bản mô phỏng trước khi cam kết dự án với khách hàng. | `COMPANY` *(Toàn công ty - Báo cáo tổng thể)* |
| **`VT-02`** | **Quản lý dự án** *(Project Manager - PM)* | Chịu trách nhiệm các dự án được giao: Tạo WBS, giao việc, đặt ngân sách giờ và duyệt bảng chấm công dự án. | `PROJECT_SCOPE` *(Chỉ các dự án được phân công)* |
| **`VT-03`** | **Quản lý nguồn lực** *(Resource Manager - RM)* | Trưởng bộ phận chuyên môn: Điều phối nhân sự theo tuần, giữ chỗ nguồn lực, duyệt nghỉ phép bộ phận. | `ORGANIZATION_BRANCH` *(Khối / Phòng ban mình quản lý)* |
| **`VT-04`** | **Nhân viên chuyên môn** *(Specialist / Employee)* | Thực hiện công việc dự án, xem lịch phân bổ cá nhân, nộp chấm công, gửi đơn nghỉ phép, cập nhật kỹ năng. | `SELF` *(Chỉ dữ liệu cá nhân của chính mình)* |
| **`VT-05`** | **Nhân sự** *(HR Specialist / Manager)* | Quản lý hồ sơ nhân sự, hợp đồng lao động, lịch làm việc chuẩn và danh sách ngày nghỉ lễ. | `COMPANY` *(Dữ liệu nhân sự toàn đơn vị)* |
| **`VT-06`** | **Quản trị viên** *(System Administrator)* | Quản trị tài khoản (`/users`), cơ cấu tổ chức (`/organization`), phân quyền truy cập (`/access`), nhật ký kiểm toán hệ thống. | `COMPANY` *(Toàn quyền quản trị kỹ thuật)* |

---

## ⚠️ Lưu ý đặc biệt quan trọng về "VT-07 / Nhân viên công ty"

> [!WARNING]  
> **"Nhân viên công ty" KHÔNG PHẢI là một Role độc lập để gán cho User!**
> 
> * **Định nghĩa đúng**: Đây chỉ là **khái niệm dùng chung** để gọi bất kỳ ai đã đăng nhập vào hệ thống khi mô tả các tiện ích dùng chung (như: *Đổi mật khẩu*, *Xem trang cá nhân*, *Đăng xuất*, *Xem thông báo chung*).
> * **Lỗi thường gặp của Frontend**: Tạo dropdown có option chọn `VT-07 - Nhân viên công ty` $\rightarrow$ **Tuyệt đối không làm điều này!**
> * Mọi tài khoản nhân viên đều phải thuộc về một trong các Role cụ thể (ví dụ: Nhân viên lập trình/thiết kế sẽ mang role `VT-04 · Nhân viên chuyên môn`).

---

## 2. Ma trận Phân Quyền Màn Hình & Chức Năng (Screen Matrix)

Bảng dưới đây quy định quyền truy cập vào các màn hình và hành động cụ thể trên giao diện:

| Phân hệ / Màn hình | VT-01 (Giám đốc) | VT-02 (PM) | VT-03 (Quản lý nguồn lực) | VT-04 (Nhân viên) | VT-05 (Nhân sự) | VT-06 (Admin) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **Quản lý tài khoản (`/users`)** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ **Toàn quyền** |
| **Cây cơ cấu tổ chức (`/organization`)** | 👁️ Xem | 👁️ Xem | 👁️ Xem | 👁️ Xem | 👁️ Xem | ✅ **Toàn quyền** |
| **Phân quyền truy cập (`/access`)** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ **Toàn quyền** |
| **Hồ sơ nhân sự (`/employees`)** | 👁️ Xem | 👁️ Xem (team) | 👁️ Xem (bộ phận) | 👁️ Xem (chính mình) | ✅ **Toàn quyền** | 👁️ Xem |
| **Quản lý dự án & WBS (`/projects`)** | 👁️ Xem | ✅ **Dự án của mình** | 👁️ Xem | 👁️ Dự án tham gia | ❌ | ❌ |
| **Điều phối & Phân bổ nguồn lực** | 👁️ Xem | 📝 Đề xuất | ✅ **Bộ phận mình** | ❌ | ❌ | ❌ |
| **Bảng chấm công (`/timesheets`)** | 👁️ Báo cáo | ✅ **Duyệt giờ dự án** | 👁️ Theo dõi tải | ✅ **Ghi & Nộp giờ** | 👁️ Tổng hợp công | ❌ |
| **Đơn nghỉ phép (`/leave-requests`)** | 👁️ Xem | 👁️ Lịch nghỉ team | ✅ **Duyệt nghỉ phép** | ✅ **Nộp đơn cá nhân** | ✅ **Quản lý quỹ phép** | ❌ |
| **Báo cáo & Mô phỏng năng lực** | ✅ **Toàn công ty** | 👁️ Dự án phụ trách | 👁️ Bộ phận phụ trách | ❌ | ❌ | ❌ |

*Chú thích:*
* ✅ **Toàn quyền / Quản lý**: Được tạo, sửa, xóa, duyệt trong phạm vi cho phép.
* 👁️ **Xem (Read-only)**: Chỉ xem dữ liệu, không có nút chỉnh sửa hay phê duyệt.
* ❌ **Không có quyền (Hidden / Blocked)**: Menu không hiển thị và Router chặn truy cập (403 Forbidden).

---

## 3. Ba Cấp Độ Phạm Vi Dữ Liệu (Data Scope)

Khi phân quyền một tài khoản, bên cạnh `RoleCode`, hệ thống áp dụng `DataScope` để lọc dữ liệu:

1. **`COMPANY` (Toàn công ty)**:
   * Được nhìn thấy dữ liệu trên toàn bộ công ty (Dành cho `VT-01`, `VT-05`, `VT-06`).
2. **`ORGANIZATION_BRANCH` (Cây đơn vị trực thuộc)**:
   * Chỉ nhìn thấy dữ liệu của Đơn vị được chỉ định (`scopeOrgUnitId`) và toàn bộ các phòng ban/nhóm con bên dưới (Dành cho `VT-03`).
3. **`SELF` (Cá nhân)**:
   * Chỉ nhìn thấy dữ liệu mà tài khoản đó là chủ sở hữu hoặc người thực hiện (Dành cho `VT-04`).

---

## 4. Hướng dẫn Lập trình Frontend (Next.js & TypeScript)

### A. Định nghĩa Type chuẩn (`frontend/src/types/hrm.ts`):
```typescript
export const ROLE_CODES = [
  "VT-01", // Ban giám đốc
  "VT-02", // Quản lý dự án
  "VT-03", // Quản lý nguồn lực
  "VT-04", // Nhân viên chuyên môn
  "VT-05", // Nhân sự
  "VT-06", // Quản trị viên
] as const;

export type RoleCode = (typeof ROLE_CODES)[number];
```

### B. Kiểm tra quyền ẩn/hiện Component (Permission Guard Helper):
```typescript
// Helper kiểm tra quyền Admin
export function isAdmin(user?: User | null): boolean {
  return user?.roleCode === "VT-06";
}

// Helper kiểm tra quyền quản lý nhân sự
export function isHRManager(user?: User | null): boolean {
  return user?.roleCode === "VT-05" || user?.roleCode === "VT-06";
}

// Helper kiểm tra quyền điều phối nguồn lực
export function isResourceManager(user?: User | null): boolean {
  return user?.roleCode === "VT-03" || user?.roleCode === "VT-01";
}
```

### C. Ẩn hiện các nút chức năng trên giao diện:
```tsx
{/* Nút Tạo tài khoản / Sửa đơn vị chỉ hiển thị với Quản trị viên */}
{isAdmin(currentUser) && (
  <button className="button button--primary" onClick={openCreateModal}>
    <Icon name="plus" />
    <span>Tạo đơn vị mới</span>
  </button>
)}
```

---

## 5. Tổng kết Checklist trước khi Review PR
- [x] Không còn xuất hiện `VT-07` trong bất kỳ dropdown chọn Role nào.
- [x] Đã hiển thị đúng 6 Role chính thức (`VT-01` $\rightarrow$ `VT-06`) với tên tiếng Việt chuẩn hóa.
- [x] Trang Phân quyền (`/access`) hiển thị chính xác bảng Ma trận quyền hạn cho từng Role.
- [x] Backend và Frontend đồng bộ 100% về mã RoleCode enum.
