# 📱 BỘ NGUYÊN TẮC THIẾT KẾ RESPONSIVE (ERP SYSTEM)

Tài liệu này quy chuẩn toàn bộ nguyên tắc thiết kế giao diện đa thiết bị (Desktop, Laptop, Tablet, Mobile) cho hệ thống ERP. Tất cả các màn hình hiện tại và các tính năng phát triển về sau **BẮT BUỘC** tuân thủ các nguyên tắc sau:

---

## 1. Hệ thống Breakpoints Chuẩn
* **Mobile (Điện thoại):** `< 768px` (XS)
* **Tablet (Máy tính bảng):** `768px - 991px` (SM)
* **Desktop (Màn hình máy tính):** `992px - 1199px` (MD)
* **Large Desktop (Màn hình lớn):** `>= 1200px` (LG / XL)

Dùng Composable có sẵn: `import { useResponsive } from '@/composables/useResponsive'` để lấy biến reactive `isMobile`, `isTablet`.

---

## 2. Khung Bố Cục (Layout & Navigation - `AdminLayout.vue`)
* **Màn hình lớn (>= 992px):** 
  - Sidebar hiển thị cố định bên trái (230px), có thể thu gọn (collapse) về 64px icon-only.
* **Màn hình nhỏ (< 992px):**
  - Sidebar chuyển hoàn toàn thành **Drawer trượt (Off-canvas Drawer)**.
  - Nút **Hamburger (Fold/Expand)** trên Header dùng để bật/tắt Drawer.
  - Tự động đóng Drawer khi người dùng bấm chọn một trang bất kỳ hoặc chạm vào vùng mờ (backdrop).
* **Header:**
  - Trên mobile: ẩn bớt các chữ phụ (tên user text, chữ đăng xuất), giữ lại Icon và Avatar để không bị tràn thanh Header.

---

## 3. Bảng Dữ Liệu (Data Tables - `el-table`)
* **Không bao giờ ép nhỏ các cột** làm vỡ chữ hay tràn chữ (như ngày tháng, số tiền, mã SKU).
* Bắt buộc bọc `el-table` trong container cuộn ngang:
  ```html
  <div class="table-responsive-wrapper">
    <el-table :data="items" style="width: 100%; min-width: 700px;">
      <!-- Các cột -->
      <el-table-column label="Thao tác" width="140" align="center" fixed="right" />
    </el-table>
  </div>
  ```
* Cột nút thao tác ("Sửa", "Xóa") nên đặt thuộc tính `fixed="right"` để người dùng luôn nhìn thấy hành động dù đang cuộn bảng sang trái/phải.

---

## 4. Hộp Thoại & Popup (`el-dialog`)
* **Không bao giờ để fixed width dạng cứng** (ví dụ: `width="540px"` trên màn hình 375px sẽ bị tràn ra ngoài màn hình).
* Bắt buộc dùng kích thước co giãn theo thiết bị:
  ```html
  <el-dialog :width="isMobile ? '92%' : '560px'" v-model="visible">
  ```
* Các ô input bên trong modal xếp hàng ngang trên desktop nhưng **phải tự động xếp chồng (stack)** trên mobile:
  ```html
  <el-row :gutter="16">
    <el-col :xs="24" :sm="14"> ... </el-col>
    <el-col :xs="24" :sm="10"> ... </el-col>
  </el-row>
  ```
* Các nút bấm ở Footer Dialog ("Hủy", "Lưu"):
  - Trên mobile: Nút kéo dài 100% chiều ngang, xếp dọc để ngón tay dễ bấm.

---

## 5. Thẻ Thống Kê & Lưới Bố Cục (Dashboard Grid)
* Không dùng `:span="8"` cứng ngắc làm thẻ bị ép thành 3 cột mỏng dính trên điện thoại.
* Luôn dùng bộ thuộc tính co giãn của Element Plus:
  ```html
  <el-col :xs="24" :sm="12" :md="8">
  ```
  - Mobile: Mỗi thẻ chiếm 1 hàng (24/24 cột) - to rõ ràng.
  - Tablet: Mỗi hàng 2 thẻ (12/24 cột).
  - Desktop: Mỗi hàng 3 thẻ (8/24 cột).

---

## 6. Trải Nghiệm Cảm Ứng (Touch Target & Typography)
* Chiều cao tối thiểu của nút bấm chính trên mobile là **40px - 44px** (chuẩn Apple/Google cho ngón tay người dùng).
* Cỡ chữ trên mobile tự động co về 14px - 18px để tiêu đề không bị ngắt dòng luộm thuộm.

