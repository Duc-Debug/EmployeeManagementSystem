#  Employee Management & Resource Capacity Planning System
### Hệ thống Quản trị Nhân sự & Hoạch định Năng lực Nguồn lực Toàn diện

[![Release](https://img.shields.io/badge/Release-v1.0.0-blue.svg)](https://github.com/Duc-Debug/EmployeeManagementSystem/releases)
[![Backend Status](https://img.shields.io/badge/Backend-Render%20Live-brightgreen)](https://employeemanagementsystem-tdpn.onrender.com)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Node.js](https://img.shields.io/badge/Node.js-22%20LTS-green.svg)](https://nodejs.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4%2B-green.svg)](https://spring.io/projects/spring-boot)
[![Frontend](https://img.shields.io/badge/React-19%20%7C%20TypeScript%20%7C%20Vite-blue.svg)](https://react.dev/)
[![Database](https://img.shields.io/badge/Database-MySQL%208.0%20%7C%20Flyway-blue)](https://aiven.io/)
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20%2F%20Clean%20Architecture-purple)](#kiến-trúc-hệ-thống)

---

##  1. Giới thiệu Tổng quan (Overview)

**Employee Management System** là nền tảng quản trị nguồn lực doanh nghiệp chuyên sâu, giải quyết bài toán cốt lõi của các tổ chức công nghệ và dịch vụ: **Tối ưu hóa năng lực nhân sự, phân bổ dự án theo kỹ năng, cảnh báo xung đột lịch trình, và mô phỏng kịch bản kinh doanh**.

Hệ thống được thiết kế theo chuẩn **14 Epics (`NCL-01` $\rightarrow$ `NCL-14`)**, bao phủ **84 User Stories** và vận hành bởi ma trận phân quyền 2 lớp (**6 Vai trò nghiệp vụ RBAC** kết hợp **Data Scope theo cấu trúc cây tổ chức**).

### 🌐 Môi trường Triển khai Sẵn sàng (Live Deployment)
- **Frontend App (Vercel):** [https://employee-management-system-izcr9mk17-duc-debug.vercel.app]
- **Backend API (Render Cloud):** [https://employeemanagementsystem-tdpn.onrender.com](https://employeemanagementsystem-tdpn.onrender.com) *(API Base: `/api/v1`)*
- **Database (Aiven Cloud / Local):** MySQL 8.0 ( Flyway Migrations tự động).

---

## 👥 2. Ma trận 6 Vai trò Nghiệp vụ (RBAC & Demo Accounts)

Hệ thống kiểm soát truy cập nghiêm ngặt dựa trên **Role-Based Access Control (RBAC)** và **Data Scope** (Toàn công ty, Phòng ban cấp dưới, Chỉ phòng ban, hoặc Cá nhân).

> 🔒 **LƯU Ý BẢO MẬT & MÔI TRƯỜNG:**
> - Tài khoản demo mẫu dưới đây **chỉ áp dụng cho môi trường phát triển nội bộ (Local Development)** khi chạy database thử nghiệm ban đầu. Mật khẩu khởi tạo được cấu hình qua file `.env` cá nhân.
> - Thông tin đăng nhập trên môi trường **Production Cloud** được quản lý tách biệt bằng biến môi trường/secret bí mật và **tuyệt đối không được lưu hoặc commit vào repository**.

| Mã vai trò | Tên vai trò | Trách nhiệm chính trong hệ thống | Tài khoản Demo (Local Dev) |
| :---: | :--- | :--- | :--- |
| **`VT-01`** | **Ban Giám Đốc (BOD)** | Xem dashboard năng lực toàn công ty, báo cáo tỷ lệ giờ tính phí, mô phỏng kịch bản tiếp nhận dự án | *Demo account configured locally* (`giapduc`) |
| **`VT-02`** | **Quản lý Dự án (PM)** | Khởi tạo dự án, lập kế hoạch công việc (WBS), đặt ngân sách giờ công, theo dõi tiến độ Kanban/Gantt | *Demo account configured locally* (`pm_user`) |
| **`VT-03`** | **Quản lý Nguồn lực (RM)** | Tìm kiếm nhân sự theo kỹ năng & độ rảnh, phân bổ dự án tuần, giải quyết cảnh báo xung đột lịch | *Demo account configured locally* (`rm_user`) |
| **`VT-04`** | **Nhân viên Chuyên môn (Dev/QA/BA)** | Xem lịch tuần cá nhân, khai báo giờ bận/trống, gửi đơn nghỉ phép, cập nhật tiến độ công việc | *Demo account configured locally* (`dev_user`) |
| **`VT-05`** | **Nhân sự (HR)** | Quản lý hồ sơ nhân sự, lịch làm việc chuẩn, ngày lễ, theo dõi hợp đồng nhân sự thuê ngoài | *Demo account configured locally* (`hr_user`) |
| **`VT-06`** | **Quản trị viên (Admin)** | Quản trị tài khoản, phân quyền RBAC & Data Scope, quản lý cây sơ đồ tổ chức, sao lưu & phục hồi dữ liệu | *Demo account configured locally* (`admin`) |


---

## ⏱️ 3. Hướng dẫn Khởi chạy Nhanh Local

Phần này dành cho người mới hoặc hội đồng chấm đồ án thiết lập và chạy toàn bộ ứng dụng trên máy cá nhân một cách dễ dàng nhất.

### Yêu cầu Tiên quyết (Prerequisites)
1. **Java Development Kit (JDK):** Phiên bản **21** (Eclipse Temurin hoặc OpenJDK 21).
2. **Node.js:** Phiên bản **22 LTS** (hoặc v20.x trở lên) & **npm**.
3. **Database:** **MySQL 8.0+** (đang chạy trên cổng `3306`).
4. **Git:** Đã cài đặt trên máy.

---

### Bước 1: Chuẩn bị Cơ sở Dữ liệu
Mở MySQL Workbench hoặc Terminal MySQL, tạo database rỗng:
```sql
CREATE DATABASE IF NOT EXISTS hrm_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

### Bước 2: Cấu hình và Chạy Backend (Spring Boot)
1. Mở terminal, điều hướng vào thư mục backend:
   ```bash
   cd backend
   ```
2. Tạo file `.env` (hoặc kiểm tra `src/main/resources/application-local.properties`):
   ```properties
   SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/hrm_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh
   SPRING_DATASOURCE_USERNAME=root
   SPRING_DATASOURCE_PASSWORD=root
   JWT_SECRET=your_super_secret_jwt_key_at_least_32_characters_long_12345
   APP_SECURITY_INTERNAL_TOKEN=local-dev-internal-service-token-only
   
   # Tùy chọn: Tự động khởi tạo tài khoản Admin ban đầu nếu database hoàn toàn mới
   # APP_SECURITY_INITIAL_ADMIN_ENABLED=true
   # INITIAL_ADMIN_USERNAME=admin
   # INITIAL_ADMIN_PASSWORD=your_secure_admin_password_here
   ```
3. Khởi chạy Backend với Maven Wrapper:
   - **Windows:**
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   - **Linux / macOS:**
     ```bash
     chmod +x mvnw
     ./mvnw spring-boot:run
     ```
   *Khi thấy thông báo `Started EmployeemanagementApplication in ... seconds`, Backend đã sẵn sàng tại:* `http://localhost:8080`.

---

### Bước 3: Cấu hình và Chạy Frontend (React 19 + Vite)
1. Mở một cửa sổ terminal mới, điều hướng vào thư mục frontend:
   ```bash
   cd frontend
   ```
2. Cài đặt các gói phụ thuộc:
   ```bash
   npm install
   ```
3. Khởi chạy Development Server:
   ```bash
   npm run dev
   ```
   *Terminal sẽ hiển thị đường dẫn truy cập (mặc định:* `http://localhost:5173`*). Mở trình duyệt và đăng nhập với một trong các tài khoản demo ở mục 2.*

---

## 🧪 4. Chạy Kiểm thử Tự động (Testing)

Dự án duy trì bộ kiểm thử tự động nghiêm ngặt bao phủ cả Backend và Frontend:

### Kiểm thử Backend (JUnit 5 + Spring Boot Test):
```bash
cd backend
# Windows
.\mvnw.cmd test
# Linux/macOS
./mvnw test
```

### Kiểm thử Frontend:
```bash
cd frontend
npm run test
```

---

## 🏗️ 5. Kiến trúc Hệ thống (System Architecture)

Dự án áp dụng mô hình **Kiến trúc Lục giác (Hexagonal Architecture / Ports & Adapters)** kết hợp **Domain-Driven Design (DDD)** nhằm phân tách độc lập nghiệp vụ khỏi framework và hạ tầng:

```
backend/
├── src/main/java/com/hrm/employeemanagement/
│   ├── domain/               # 💎 Pure Core Domain (Entities, Value Objects, Domain Policies, Exceptions)
│   ├── application/          # ⚙️ Application Use Cases (Inbound/Outbound Ports, DTOs, Services)
│   └── infrastructure/       # 🔌 Infrastructure Adapters
│       ├── adapter/inbound/  # Controllers REST API, Web Security Filter, Token Authentication
│       ├── adapter/outbound/ # Spring Data JPA Repositories, MySQL Mappers, Caffeine Cache, Email
│       └── config/           # Security, Flyway, CORS, Async & Thread Pool Configurations
```

### 🔐 Điểm nổi bật về Kỹ thuật & Bảo mật:
- **Dynamic CORS Whitelisting:** Quản lý danh sách domain an toàn linh hoạt qua biến `APP_CORS_ALLOWED_ORIGINS`, tự động duy trì `localhost` cho dev.
- **Fail-Safe Token Blacklist:** Caffeine Cache In-Memory siêu tốc, băm SHA-256 JWT với TTL động, tự hủy key khi hết hạn, cách ly tùy chọn Redis phân tán.
- **Machine-to-Machine Security:** Bảo vệ các endpoint nền (Background Cron / Notifications) qua `X-Internal-Token`.
- **Database Idempotency:** Toàn bộ 127 Flyway Migrations tự động kiểm tra `flyway.repair()` và cơ chế DDL idempotent chống lỗi ngắt kết nối mạng giữa chừng.

---

## ☁️ 6. Hướng dẫn Triển khai Cloud (Production Deployment)

### 1. Database (Aiven MySQL Managed)
- Dịch vụ MySQL Cloud được kích hoạt SSL kết nối:
  `jdbc:mysql://<host>:<port>/defaultdb?sslmode=require&serverTimezone=Asia/Ho_Chi_Minh`

### 2. Backend (Render Web Service)
- **Runtime:** `Docker` (Multi-stage build với OpenJDK 21 Alpine).
- **Environment Variables quan trọng:**
  - `SPRING_PROFILES_ACTIVE`: `prod`
  - `SPRING_DATASOURCE_URL`: `<JDBC URL từ Aiven>`
  - `SPRING_DATASOURCE_USERNAME`: `<Aiven username>`
  - `SPRING_DATASOURCE_PASSWORD`: `<Aiven password>`
  - `JWT_SECRET`: `<Secret key >= 32 ký tự>`
  - `APP_SECURITY_INTERNAL_TOKEN`: `<M2M Secret Token>`
  - `APP_CORS_ALLOWED_ORIGINS`: `https://<your-vercel-domain>.vercel.app`
  - `APP_RESET_PASSWORD_BASE_URL`: `https://<your-vercel-domain>.vercel.app/reset-password`
  - `PORT`: `8080`

### 3. Frontend (Vercel)
- **Framework:** `Vite`
- **Root Directory:** `frontend`
- **Build Command:** `npm run build`
- **Output Directory:** `dist`
- **Environment Variable:**
  - `VITE_API_BASE_URL`: `https://employeemanagementsystem-tdpn.onrender.com/api/v1`

---

## 📄 7. Bản quyền & Thông tin Dự án
- **Dự án:** Hệ thống Quản trị Nhân sự & Hoạch định Năng lực Nguồn lực (Employee Management System).
- **Phụ trách:** Duc-Debug & Đội ngũ Phát triển CodeGym Giai đoạn 3.
- **Phiên bản:** `v1.0.0` (Bản nộp chính thức trên nhánh `main`).
