# 🌐 Employee Management System - Frontend App

Ứng dụng giao diện người dùng (Single Page Application - SPA) được xây dựng bằng **React 19**, **TypeScript**, **Tailwind CSS**, và **Vite**.

---

## 📋 1. Yêu cầu Hệ thống (Prerequisites)
- **Node.js:** `v20.x` hoặc `v22.x` (khuyến nghị Node 22 LTS).
- **npm:** Đi kèm theo Node.js (phiên bản `10.x` trở lên).

---

## ⚙️ 2. Biến Môi trường (Environment Variables)

Tạo file `.env` (hoặc `.env.local`) tại thư mục `frontend/`:

```env
# Địa chỉ API của Backend (Mặc định gọi tới localhost:8080 nếu để trống)
VITE_API_BASE_URL=http://localhost:8080/api/v1

# Đối với môi trường Production Cloud (Vercel):
# VITE_API_BASE_URL=https://employeemanagementsystem-tdpn.onrender.com/api/v1
```

---

## 🚀 3. Hướng dẫn Khởi chạy (Commands)

### Cài đặt Dependencies:
```bash
npm install
```

### Chạy Development Server (HMR):
```bash
npm run dev
```
- Ứng dụng sẽ chạy tại: `http://localhost:5173` (hoặc port hiển thị trên terminal).

### Chạy Unit Test Frontend:
```bash
npm run test
```

### Build Production & Kiểm tra Type:
```bash
npm run build
```
- Thư mục đầu ra: `dist/`.
- File cấu hình điều hướng SPA trên Vercel: [vercel.json](vercel.json).

### Preview bản Build Production:
```bash
npm run preview
```

---

## 📁 4. Cấu trúc Thư mục (Directory Structure)

```
frontend/
├── src/
│   ├── app/           # App layout, routing, header, sidebar
│   ├── components/    # Reusable UI components (buttons, modals, tables, forms)
│   ├── features/      # Feature modules (backup, my-schedule, timesheet, wbs, ...)
│   ├── hooks/         # Custom React hooks
│   ├── lib/           # API client, auth session, helper utilities
│   ├── pages/         # Top-level view pages
│   ├── tests/         # Frontend automated test suite
│   ├── types/         # TypeScript shared type definitions
│   ├── App.tsx        # Router setup and providers
│   └── main.tsx       # Application root entry point
├── vercel.json        # Rewrite rules for SPA routing on Vercel
├── package.json       # Project dependencies & scripts
└── vite.config.ts     # Vite bundler configuration
```
