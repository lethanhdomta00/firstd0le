# Tổng Kết Dự Án ERP (Java 17 Spring Boot & Vue 3)

Tài liệu này tóm tắt toàn bộ những gì đã được xây dựng và giải thích chi tiết về luồng hoạt động của Backend trong dự án ERP.

## 1. Tóm tắt những thành phần đã xây dựng

*   **Khởi tạo dự án:** Xây dựng một cấu trúc Monorepo (2 thư mục chung một không gian) cho `erp-backend` và `erp-frontend`.
*   **Phát triển Frontend (Vue 3 + Vite):** 
    *   Tích hợp UI Framework **Element Plus** để tạo giao diện Admin chuyên nghiệp, bao gồm Layout có Sidebar và Header.
    *   Thiết lập **Vue Router** để chuyển trang (Login, Dashboard, Products) với tính năng "Bảo vệ Router" (Navigation Guards) chặn những ai chưa đăng nhập.
    *   Thiết lập **Pinia** và **Axios** để quản lý trạng thái đăng nhập (lưu JWT Token vào `localStorage`) và tự động gắn token vào header của mọi request gửi đi.
*   **Phát triển Backend (Java 17 + Spring Boot 3):**
    *   Thiết lập một RESTful API hoàn chỉnh với kết nối tới Database H2 (chạy trong bộ nhớ, dễ dàng đổi sang MySQL sau này).
    *   Cấu hình **Spring Security + JJWT** để bảo mật toàn bộ API bằng phương thức không lưu trạng thái (Stateless JWT Authentication).
    *   Tạo các Entity/Bảng cơ sở dữ liệu: `User` (cho việc phân quyền) và `Product` (Quản lý hàng hóa cốt lõi).
    *   Mở kết nối mạng (`server.address: 0.0.0.0`) và cấu hình CORS (`setAllowedOriginPatterns("*")`) để mọi Frontend nội bộ qua địa chỉ IP LAN (ví dụ: `192.168.1.7`) đều có thể gọi được tới Backend.

---

## 2. Luồng hoạt động phía Backend (Architecture Flow)

Backend được tổ chức chặt chẽ theo kiến trúc **Mô hình 3 lớp (3-Tier Architecture)** của Spring Boot. Khi một Request từ Frontend gửi tới, nó sẽ đi qua các lớp sau theo thứ tự:

**Request ➡️ Filter (Security) ➡️ Controller ➡️ Service ➡️ Repository ➡️ Database**

### A. Luồng Xác thực (Authentication / Security Flow)
Đây là cách hệ thống nhận diện và bảo vệ các API:

1. **Đăng nhập (`/api/auth/login`):** Frontend gửi `username` và `password`.
2. **Xác nhận:** Backend sử dụng `AuthenticationManager` để so sánh mật khẩu được mã hóa (BCrypt) lưu trong Database.
3. **Cấp Token (`JwtTokenProvider`):** Nếu thông tin chính xác, hệ thống sinh ra một chuỗi **JWT Token** (mang thông tin username, chữ ký bí mật, hạn dùng 24h) và trả về cho Frontend.
4. **Các Request sau này (`JwtAuthenticationFilter`):** Frontend tự động kẹp JWT Token này vào Header (`Authorization: Bearer <token>`). Khi một request (ví dụ lấy danh sách sản phẩm) vừa tới Backend, một Filter bảo mật sẽ chặn lại đầu tiên. Filter này kiểm tra tính hợp lệ của chữ ký Token. Nếu hợp lệ, nó cho Request đi tiếp vào lớp Controller; nếu không, ngay lập tức trả về lỗi `401 Unauthorized`.

### B. Luồng Xử lý Nghiệp Vụ Thực Tế (Ví dụ: Lấy danh sách Product)

Sau khi đi qua cổng bảo mật thành công, request đi sâu vào hệ thống:

1. **Controller (`ProductController`):**
   * Đóng vai trò là "Cửa ngõ tiếp tân". Nhận request `GET /api/products` từ Frontend. 
   * Nhiệm vụ của lớp này chỉ là bóc tách dữ liệu đầu vào, gọi xử lý ở lớp bên dưới và trả về đúng chuẩn HTTP Response (200 OK, 404 Not Found...).
2. **Service (Business Logic):**
   * Chứa các quy tắc nghiệp vụ cốt lõi. Ví dụ nếu nghiệp vụ ERP phức tạp (lấy sản phẩm phải tính cả thuế hiện hành, kiểm tra tồn kho tại khu vực cụ thể), ta sẽ đặt logic xử lý tại lớp `ProductService` trước khi giao tiếp với database.
3. **Repository (`ProductRepository`):**
   * Kế thừa từ interface `JpaRepository` của Spring Data JPA. 
   * Đóng vai trò như một cỗ máy tự động biên dịch các câu lệnh Java sang câu lệnh SQL (như `SELECT * FROM products`) mà không cần nhà phát triển phải tự viết SQL thuần túy.
4. **Database (H2 / MySQL / PostgreSQL):**
   * Nhận lệnh SQL từ Repository, thực thi truy xuất vào vùng nhớ/đĩa cứng và trả kết quả ngược lên trên lại tới tận Controller dưới dạng JSON chuẩn.

Nhờ việc tuân thủ chặt chẽ tiêu chuẩn thiết kế **RESTful** (sử dụng đúng các HTTP Method: GET để lấy, POST để tạo, PUT để cập nhật, DELETE để xóa), cả Frontend và Backend tương tác một cách vô cùng độc lập và mượt mà.

