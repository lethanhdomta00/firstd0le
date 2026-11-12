# Project Workspace Rules (`AGENTS.md`)

Tệp quy tắc thường trực (`always_on`) dành cho trợ lý AI khi làm việc trong dự án **ERP Cloud (Java 17 Spring Boot 3 + Vue 3 Element Plus)**. Tham chiếu đầy đủ tại [QUY_UOC_LAM_VIEC.md](./QUY_UOC_LAM_VIEC.md).

## 1. Giao tiếp & Phong cách
- **Xưng hô bắt buộc**: Xưng hô thẳng thắn, thân thiết **Mày - Tao** (AI là *Tao*, User là *Mày*).
- **Chèn emoji/icon sinh động** trong phản hồi (🌿, 🚀, 💼, ⚡, 🛡️, 🎯).
- **Bối cảnh người dùng**: Mày là **Dev PHP Laravel lâu năm chuyển sang Java Spring Boot**. Cuối mỗi buổi học hoặc khi chốt nghỉ buổi, **BẮT BUỘC phải viết trọn vẹn phần Bài học cuối buổi** (giải thích sâu các cơ chế Java 17 / Spring Boot / JPA vừa dùng kèm bài tập/tình huống thực tế) và lập bảng đối chiếu **PHP Laravel ⮂ Java Spring Boot**.
- **Quy chuẩn Link File**: Mọi file/symbol nhắc tới phải dùng clickable markdown link `[basename](file:///path)`.

## 2. An toàn Quy trình & Kiểm soát Mã nguồn
- 🚫 **Không tự ý Git Commit / Push** khi chưa có lệnh trực tiếp từ mày.
- 🧪 **Bắt buộc viết & chạy Unit Test / Integration Test chuẩn (JUnit 5 + MockMvc + `@Transactional` rollback)** qua `./erp-backend/mvnw.cmd -f erp-backend/pom.xml test` và build Vite (`npm --prefix erp-frontend run build`) đạt xanh 100% trước khi bàn giao; vẫn 🚫 **không tự chạy script curl/Python kiểm thử API ngoài** và **không tự khởi động file `.jar` Backend** (`để tao tự test`).
- 🎯 **Tập trung đúng phạm vi**: Không sửa lan man ngoài yêu cầu hiện tại.

## 3. Tiêu chuẩn Không Lỗi API & Không Lỗi JavaScript (Zero Runtime / Network Errors)
- **Zero API Errors (404 / 403 / 500 / CORS)**:
  - Trước khi viết bất kỳ request API nào trên Frontend (`api.get`, `api.post`, `api.patch`, ...), **BẮT BUỘC đọc trực tiếp `@RestController` và `SecurityConfig.java`** để xác nhận:
    1. Đường dẫn URL chính xác (gồm prefix `@RequestMapping`).
    2. HTTP Method được cấp phép trong CORS (đảm bảo có `"PATCH"` nếu dùng `@PatchMapping`).
    3. Phân quyền (`@PreAuthorize`) phù hợp với vai trò người dùng hiện tại.
    4. Cấu trúc dữ liệu trả về (`List` vs `Page<T>` vs `Map`).
  - **Không dùng cách đoán URL rồi `try/catch` fallback sang URL khác** vì vẫn sinh ra dòng request đỏ trên DevTools Network Tab.
  - Chuẩn hóa ngày giờ gửi qua `LocalDateTime` theo local ISO string (`YYYY-MM-DDTHH:mm:ss` giờ VN GMT+7), tránh dùng `.toISOString()` gây lệch -7 tiếng UTC.
- **Zero JS Runtime Errors**:
  - Luôn đặt các hàm helper (`toDateKey`, `formatDateKey`, `formatToIsoString`, ...) lên đầu `<script setup>` **trước mọi `computed` / `watch`** để tránh lỗi `ReferenceError` do Temporal Dead Zone (`const` không hoisting).
  - Kiểm tra kỹ giá trị `0` bằng toán tử Nullish Coalescing (`??`) thay vì `||`.
  - Rà soát chéo toàn bộ biến/hàm dùng trong `<template>` đảm bảo đã khai báo đầy đủ trong `<script setup>`.

## 4. Tiêu chuẩn Giao diện Responsive & Element Plus UI
- **Responsive Audit (Desktop, Laptop nhỏ, Tablet, Mobile & khi mở DevTools)**:
  - **Header & Toolbars**: Các nút bấm luôn có `white-space: nowrap`, `flex-wrap: wrap`, `height` đồng đều (`32px`); cụm tiêu đề có `flex: 1 1 ...` để không bị bóp bẻ chữ 2 dòng hay xếp nút lộn xộn.
  - **CSS Grid & Split Layout**: Mọi cột `1fr` chứa bảng/lịch bắt buộc có `min-width: 0` (`minmax(0, 1fr)`). Sử dụng `@container` kết hợp `@media` để tự chuyển sang 1 cột khi không gian nội dung hẹp (ví dụ khi mở DevTools hoặc trên màn vừa).
  - **Filter Pills & Floating Widgets**: Dùng `flex-wrap: wrap` cho thanh lọc (tránh `overflow-x: auto` làm thanh cuộn ngang che chữ). Luôn chừa `padding-bottom: 64px` ở vùng cuộn cuối trang để tránh bị nút AI Copilot nổi góc dưới phải che khuất.
- **Modal & Element Plus Popper (`z-index`)**:
  - Mọi custom modal overlay giữ `z-index: 1050` (trên Sidebar `100` / Header `9`), còn `.el-popper`, `.el-select__popper`, `.el-picker__popper`, `.el-color-dropdown` luôn có `z-index: 12000 !important` trong `style.css` để không bao giờ bị kẹt bên dưới modal.
- **UI/UX Simplicity & Visual Hierarchy (Đơn giản, Thoáng mắt, Chuẩn Thương mại)**:
  - **Chống "Hộp chồng hộp" (Zero Nested Box Overload)**: Trong Modal/Drawer, không đóng khung viền từng khối con lồng nhau; ưu tiên bố cục phẳng với khoảng trắng rộng rãi, đường kẻ phân tách mảnh (`1px solid #f1f5f9`) và phân cấp cỡ chữ.
  - **Không lặp dữ liệu thừa (DRY UI)**: Thông tin đã hiển thị ở Header hoặc Bảng (người tạo, trạng thái, tổng số lượng) không lặp lại ở khối Metadata bên dưới.
  - **Bảng dữ liệu dễ quét (Scannable Tables)**: Mã chứng từ/tiền tệ giữ `white-space: nowrap`, tên dài cắt gọn `text-overflow: ellipsis`, gộp thuộc tính phụ (SKU dưới tên chính), gom action ít dùng (`Sửa`/`Xóa`) vào menu `⋯`, và gộp các nút xuất file phụ thành dropdown.
  - **Bố cục Hóa đơn & Modal Footer chuẩn**: Khối tổng tiền (`Tạm tính`, `VAT`, `Tổng cộng`) gom gọn `260px–300px` căn phải sát số tiền; Footer tách nút hủy nhẹ bên trái khỏi nút `Đóng` + CTA chính bên phải.

## 5. Tiêu chuẩn Hiệu năng Hàng Triệu Dữ liệu (High-Scale Performance)
- ⚡ **Zero `findAll()` OOM trong Service / BI**: Tuyệt đối không dùng `repository.findAll()` nạp toàn bộ bảng vào RAM JVM để đếm, tính tổng hay vẽ biểu đồ. Mọi thống kê (`stats`, `analytics`, báo cáo BI) bắt buộc dùng **SQL Aggregate (`COUNT`, `SUM`, `AVG`, `GROUP BY`)** hoặc projection DTO ngay tại tầng Database.
- 🗂️ **Database Indexing bắt buộc**: Mọi cột thường xuyên lọc, sắp xếp, join hoặc quét khoảng thời gian (`created_at`, `status`, `payment_status`, `order_code`, `product_id`, `product_sku`, `movement_type`) phải khai báo `@Index` ngay trong `@Table(indexes = ...)`.
- 🚫 **Chống N+1 Query & Chặn Export không giới hạn**: Luôn dùng `@EntityGraph` / `JOIN FETCH` hoặc `WHERE ... IN (...)` batching khi đọc quan hệ `@OneToMany` trong danh sách; API phân trang (`Page<T>`) luôn giới hạn `size` tối đa (e.g. `<= 100`), và export file lớn phải lọc theo khoảng thời gian/giới hạn an toàn hoặc stream cursor (`Stream<T>` / `ScrollableResults`).
