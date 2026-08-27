# 🚀 HƯỚNG DẪN KIỂM THỬ POSTMAN TOÀN DIỆN (API TEST GUIDE)

Tài liệu này cung cấp hướng dẫn từng bước để kiểm thử toàn bộ các Endpoint của phân hệ **Quản lý tài khoản người dùng** trên Postman.

---

## 📥 Cách 1: Import file Collection vào Postman (Khuyên Dùng)

1. Mở ứng dụng **Postman**.
2. Bấm nút **Import** (ở góc trên cùng bên trái).
3. Chọn file collection: [`docs/postman/EmployeeManagement_Postman_Collection.json`](file:///d:/Project7/EmployeeManagementSystem/docs/postman/EmployeeManagement_Postman_Collection.json).
4. Postman sẽ tự động nạp toàn bộ danh sách 14 Request có kèm sẵn script **tự động lưu JWT Token** và **tự động Assert Test Cases**.

---

## 📋 Cách 2: Thực hiện kiểm thử thủ công theo kịch bản

### ⚙️ Thông tin chung
- **Base URL**: `http://localhost:8080`
- **Tài khoản Admin mặc định**:
  - `Username`: `admin`
  - `Password`: `admin123`

---

### BƯỚC 1: Đăng nhập lấy Bearer Token (Authentication)

#### 🔹 1.1 Đăng nhập Admin
* **Method**: `POST`
* **URL**: `http://localhost:8080/api/v1/auth/login`
* **Headers**: `Content-Type: application/json`
* **Body (JSON)**:
  ```json
  {
    "username": "admin",
    "password": "admin123"
  }
  ```
* **Kỳ vọng (HTTP 200 OK)**:
  ```json
  {
    "success": true,
    "message": "Đăng nhập thành công",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "tokenType": "Bearer",
      "userId": 1,
      "username": "admin",
      "roleCode": "VT-06"
    }
  }
  ```
  *(Copy giá trị `token` để gắn vào Header `Authorization: Bearer <token>` cho các request sau).*

---

### BƯỚC 2: Kiểm thử Luồng Quản lý Tài khoản (User Management)

> **Lưu ý**: Tất cả các request dưới đây đều yêu cầu Header:  
> `Authorization: Bearer <jwt_token>`

#### 🔹 2.1 Tạo tài khoản mới thành công (`TC-01`)
* **Method**: `POST`
* **URL**: `http://localhost:8080/api/v1/users`
* **Body (JSON)**:
  ```json
  {
    "username": "nhanvien_it",
    "password": "it123456",
    "roleCode": "VT-04",
    "employeeCode": "EMP-IT-001",
    "fullName": "Nguyễn Văn IT",
    "departmentId": 2
  }
  ```
* **Kỳ vọng (HTTP 201 Created)**:
  - Header: `Location: http://localhost:8080/api/v1/users/2`
  - Body:
    ```json
    {
      "success": true,
      "message": "Tạo tài khoản thành công",
      "data": {
        "id": 2,
        "username": "nhanvien_it",
        "roleCode": "VT-04",
        "roleName": "Nhân viên chuyên môn",
        "status": "ACTIVE",
        "employeeId": 2,
        "fullName": "Nguyễn Văn IT",
        "departmentId": 2
      }
    }
    ```

---

#### 🔹 2.2 Tạo tài khoản trùng tên đăng nhập (`TC-02`)
* **Method**: `POST`
* **URL**: `http://localhost:8080/api/v1/users`
* **Body (JSON)**: Giữ nguyên `username: "nhanvien_it"`
* **Kỳ vọng (HTTP 409 Conflict)**:
  ```json
  {
    "success": false,
    "message": "Tên đăng nhập đã tồn tại: nhanvien_it",
    "data": null
  }
  ```

---

#### 🔹 2.3 Lấy danh sách tài khoản phân trang (Pagination)
* **Method**: `GET`
* **URL**: `http://localhost:8080/api/v1/users?page=0&size=10`
* **Kỳ vọng (HTTP 200 OK)**:
  ```json
  {
    "success": true,
    "message": "Lấy danh sách tài khoản thành công",
    "data": {
      "content": [
        {
          "id": 1,
          "username": "admin",
          "roleCode": "VT-06",
          "status": "ACTIVE"
        },
        {
          "id": 2,
          "username": "nhanvien_it",
          "roleCode": "VT-04",
          "status": "ACTIVE"
        }
      ],
      "page": 0,
      "size": 10,
      "totalElements": 2,
      "totalPages": 1
    }
  }
  ```

---

#### 🔹 2.4 Cập nhật vai trò và phòng ban
* **Method**: `PUT`
* **URL**: `http://localhost:8080/api/v1/users/2/role`
* **Body (JSON)**:
  ```json
  {
    "roleCode": "VT-02",
    "departmentId": 3
  }
  ```
* **Kỳ vọng (HTTP 200 OK)**:
  ```json
  {
    "success": true,
    "message": "Cập nhật vai trò và bộ phận thành công",
    "data": {
      "id": 2,
      "roleCode": "VT-02",
      "roleName": "Quản lý dự án",
      "departmentId": 3
    }
  }
  ```

---

#### 🔹 2.5 Khóa tài khoản thành công (`TC-05`)
* **Method**: `PATCH`
* **URL**: `http://localhost:8080/api/v1/users/2/status?lock=true`
* **Kỳ vọng (HTTP 200 OK)**:
  ```json
  {
    "success": true,
    "message": "Khóa tài khoản thành công",
    "data": {
      "id": 2,
      "status": "LOCKED"
    }
  }
  ```

---

#### 🔹 2.6 Khóa lại tài khoản đã bị khóa (`TC-03`)
* **Method**: `PATCH`
* **URL**: `http://localhost:8080/api/v1/users/2/status?lock=true`
* **Kỳ vọng (HTTP 400 Bad Request)**:
  ```json
  {
    "success": false,
    "message": "Tài khoản này hiện đã bị khóa",
    "data": null
  }
  ```

---

#### 🔹 2.7 Mở lại tài khoản thành công
* **Method**: `PATCH`
* **URL**: `http://localhost:8080/api/v1/users/2/status?lock=false`
* **Kỳ vọng (HTTP 200 OK)**:
  ```json
  {
    "success": true,
    "message": "Mở lại tài khoản thành công",
    "data": {
      "id": 2,
      "status": "ACTIVE"
    }
  }
  ```

---

#### 🔹 2.8 Admin cố gắng tự khóa chính mình
* **Method**: `PATCH`
* **URL**: `http://localhost:8080/api/v1/users/1/status?lock=true`
* **Kỳ vọng (HTTP 400 Bad Request)**:
  ```json
  {
    "success": false,
    "message": "Bạn không thể tự khóa tài khoản của chính mình",
    "data": null
  }
  ```

---

### BƯỚC 3: Kiểm thử Phân quyền & Giám sát An ninh (`TC-04`)

#### 🔹 3.1 Non-Admin đăng nhập và cố truy cập API Admin
1. Đăng nhập bằng tài khoản `nhanvien_it` (Vai trò `VT-04`):
   * `POST /api/v1/auth/login` với `username: "nhanvien_it"`, `password: "it123456"`.
   * Lấy token của `nhanvien_it`.
2. Gửi request `GET /api/v1/users` kèm token của `nhanvien_it`.
3. **Kỳ vọng (HTTP 403 Forbidden)**:
   ```json
   {
     "success": false,
     "message": "Bạn không có quyền truy cập chức năng này",
     "data": null
   }
   ```
   *(Hệ thống đã tự động ghi vết bản ghi `ACCESS_DENIED` vào bảng `audit_logs`).*

---

### BƯỚC 4: Kiểm thử Đăng xuất & Thu hồi Token (Logout & Token Invalidation)

#### 🔹 4.1 Đăng xuất tài khoản người dùng
* **Method**: `POST`
* **URL**: `http://localhost:8080/api/v1/auth/logout`
* **Headers**: `Authorization: Bearer <token_admin_hoac_user>`
* **Kỳ vọng (HTTP 200 OK)**:
  ```json
  {
    "success": true,
    "message": "Đăng xuất thành công",
    "data": null
  }
  ```
  *(Token hiện tại đã được đưa vào In-Memory Caffeine Token Blacklist và hệ thống ghi vết bản ghi `LOGOUT` vào bảng `audit_logs`).*

---

#### 🔹 4.2 Cố gắng sử dụng lại Token đã Đăng xuất để gọi API
1. Gửi request `GET /api/v1/users` kèm Header `Authorization: Bearer <token_vua_logout>`.
2. **Kỳ vọng (HTTP 401 Unauthorized)**:
   ```json
   {
     "success": false,
     "message": "Bạn cần đăng nhập để truy cập chức năng này",
     "data": null
   }
   ```
   *(Bộ lọc `JwtAuthenticationFilter` phát hiện token nằm trong Blacklist và từ chối xác thực ngay lập tức).*

