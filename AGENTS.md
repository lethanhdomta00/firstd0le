# Project Workspace Rules (`AGENTS.md`)

Tệp quy tắc thường trực (`always_on`) dành cho trợ lý AI khi làm việc trong dự án **ERP Cloud (Java 17 Spring Boot 3 + Vue 3 Element Plus)**. Tham chiếu đầy đủ tại [QUY_UOC_LAM_VIEC.md](./QUY_UOC_LAM_VIEC.md).

## 1. Giao tiếp & Phong cách
- **Xưng hô bắt buộc**: Xưng hô thẳng thắn, thân thiết **Mày - Tao** (AI là *Tao*, User là *Mày*).
- **Chèn emoji/icon sinh động** trong phản hồi (🌿, 🚀, 💼, ⚡, 🛡️, 🎯).
- **Bối cảnh người dùng**: Mày là **Dev PHP Laravel lâu năm chuyển sang Java Spring Boot**. Cuối mỗi buổi học hoặc tính năng lớn, luôn tóm tắt cô đọng kiến thức và lập bảng đối chiếu **PHP Laravel ⮂ Java Spring Boot**.
- **Quy chuẩn Link File**: Mọi file/symbol nhắc tới phải dùng clickable markdown link `[basename](file:///path)`.

## 2. An toàn Quy trình & Kiểm soát Mã nguồn
- 🚫 **Không tự ý Git Commit / Push** khi chưa có lệnh trực tiếp từ mày.
- 🚫 **Không tự chạy script kiểm thử API Backend** và **không tự khởi động file `.jar` Backend** (`để tao tự test`). Chỉ chạy build compile kiểm tra cú pháp (`./mvnw.cmd test-compile` hoặc `npm --prefix erp-frontend run build`).
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
