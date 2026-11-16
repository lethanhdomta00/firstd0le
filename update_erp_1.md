# TÀI LIỆU ĐẶC TẢ KỸ THUẬT, NGHIỆP VỤ BUÔN BÁN & TỐI ƯU HIỆU NĂNG: ERP CLOUD ENTERPRISE SUITE

**Định hướng:** Hệ thống SaaS Quản trị Bán hàng, Phân phối Sỉ/Lẻ & Vận hành Kho hiệu năng cao (High-Performance B2B2C Commerce ERP)  
**Mục tiêu SLA:** Uptime $\ge 99.98\%$, Thời gian phản hồi API (P95) $< 100\text{ms}$, Thao tác quét mã POS $< 15\text{ms}$.

---

## 1. ĐẶC TẢ CHI TIẾT NGHIỆP VỤ BUÔN BÁN (BUSINESS LOGIC SPECIFICATION)

### 1.1. Nghiệp vụ Quản trị Tồn kho Đa trạng thái & Chống bán lố (Overselling Prevention)
Trong môi trường buôn bán có nhiều nhân viên Sale chốt đơn cùng lúc, hệ thống tuyệt đối không chỉ dùng 1 cột `stock` duy nhất mà phải tách thành **4 trạng thái tồn kho** trên mỗi SKU tại từng Kho (`warehouse_id`):

1. **Tồn thực tế (`physical_stock`):** Số lượng hàng đang nằm vật lý trên kệ trong kho.
2. **Tồn giữ chỗ (`reserved_stock`):** Số lượng hàng đã được khách đặt trong các đơn hàng ở trạng thái *Chờ duyệt / Đang đóng gói* (VD: Đơn `#ORD-9871` chờ xuất 12 sản phẩm).
3. **Tồn đang về (`incoming_stock`):** Hàng đã đặt từ Nhà cung cấp (PO) đang trên đường về kho.
4. **Tồn khả dụng (`available_stock`):** Số lượng tối đa cho phép nhân viên Sale hoặc Đại lý chốt đơn mới.
   $$\text{Available Stock} = \text{Physical Stock} - \text{Reserved Stock}$$

**Luồng trạng thái kho khi xử lý đơn hàng (State Machine):**
* **Bước 1 (Tạo đơn hàng `#ORD-9871`):** Kiểm tra $\text{Available Stock} \ge \text{Số lượng mua}$. Nếu thỏa mãn $\rightarrow$ Tăng `reserved_stock` thêm $12$, giữ nguyên `physical_stock`.
* **Bước 2A (Thủ kho bấm "Xuất kho"):** Giảm `reserved_stock` đi $12$, đồng thời giảm `physical_stock` đi $12$. Ghi nhận vào thẻ kho (`inventory_transactions`).
* **Bước 2B (Hủy đơn hàng trước khi xuất):** Giảm `reserved_stock` đi $12$ để trả lại số lượng vào `available_stock` ngay lập tức.

---

### 1.2. Thuật toán Tính Giá vốn Tự động (Moving Average Cost - MAC) & Lãi Gộp
Chủ buôn cần biết ngay lập tức mỗi đơn hàng lãi bao nhiêu tiền. Hệ thống áp dụng phương pháp **Bình quân gia quyền liên hoàn (Moving Average Cost)** tự động tính lại mỗi khi có phiếu **Nhập kho Nhà cung cấp**:

$$\text{Giá vốn mới (MAC)} = \frac{(\text{Tồn thực tế hiện tại} \times \text{Giá vốn hiện tại}) + (\text{Số lượng nhập mới} \times \text{Giá nhập mới} + \text{Chi phí vận chuyển/bốc xếp})}{\text{Tồn thực tế hiện tại} + \text{Số lượng nhập mới}}$$

* **Đóng băng giá vốn trên đơn hàng (`snapshot_cost_price`):** Khi đơn hàng `#ORD-9871` xuất kho, hệ thống phải lưu cứng `MAC` tại thời điểm đó vào dòng chi tiết đơn hàng (`order_items`). Việc nhập hàng giá cao hơn vào ngày hôm sau không được làm sai lệch lịch sử lợi nhuận của các đơn ngày hôm trước.
* **Lợi nhuận gộp đơn hàng:**
  $$\text{Lãi gộp đơn hàng} = \sum (\text{Giá bán thực tế} - \text{Snapshot Cost Price}) \times \text{Số lượng} - \text{Chiết khấu tổng đơn} - \text{Phí sàn/Vận chuyển shop chịu}$$

---

### 1.3. Cơ chế Bảng giá Sỉ/Lẻ Đa tầng & Quy đổi Đơn vị tính (B2B Tiered Pricing & UoM)
* **Quy đổi Đơn vị tính (Unit of Measure - UoM):**
  * Quản lý tồn kho luôn quy về **Đơn vị cơ sở nhỏ nhất** (VD: `Cái` / `Lon`).
  * Cho phép bán/nhập theo đơn vị quy đổi: `1 Hộp = 10 Cái`, `1 Thùng = 120 Cái`. Khi xuất `2 Thùng`, hệ thống tự động trừ $2 \times 120 = 240\text{ Cái}$ trong `physical_stock`.
* **Thứ tự ưu tiên áp giá tự động (Pricing Resolution Engine):** Khi chọn Khách hàng A và thêm mã `SKU-2101` với số lượng $Q$ vào đơn:
  1. **Ưu tiên 1 (Giá hợp đồng riêng):** Khách hàng A có giá thỏa thuận riêng cho `SKU-2101` không?
  2. **Ưu tiên 2 (Giá theo bậc số lượng - Volume Tier):** Mua $Q \ge 50$ áp giá Đại lý VIP; $10 \le Q < 50$ áp giá Sỉ; $Q < 10$ áp giá Lẻ.
  3. **Ưu tiên 3 (Nhóm khách hàng):** Khách thuộc nhóm `Đại lý Cấp 1` được giảm cố định $15\%$ trên giá niêm yết.
  4. **Ưu tiên 4:** Giá bán lẻ niêm yết (`base_retail_price`).

---

### 1.4. Kiểm soát Công nợ Đại lý & Gạch nợ Tự động qua VietQR Webhook
* **Công thức Kiểm tra Hạn mức Nợ (Credit Gatekeeper):**
  $$\text{Dư nợ sau đơn mới} = \text{Nợ hiện tại (`current_debt`)} + \text{Giá trị đơn hàng đang chờ duyệt} + \text{Tổng tiền đơn mới} - \text{Số tiền thanh toán trước}$$
  * Nếu $\text{Dư nợ sau đơn mới} > \text{Hạn mức cho phép (`credit_limit`)}$: Hệ thống tự động khóa nút "Xuất kho", chuyển trạng thái đơn sang `PENDING_CREDIT_APPROVAL` và bắn thông báo thời gian thực cho Kế toán trưởng.
* **Luồng Gạch nợ Tự động 100% (Zero-touch Reconciliation):**
  1. Hệ thống tạo mã VietQR chuẩn EMVCo chứa nội dung: `ERP ORD9871` (checksum chống gõ sai).
  2. Khách quét mã chuyển khoản $\rightarrow$ Ngân hàng báo biến động số dư qua dịch vụ trung gian (SePay / Casso / PayOS) gọi vào endpoint `POST /api/v1/webhooks/payment`.
  3. Hệ thống kiểm tra chữ ký bảo mật (`HMAC-SHA256`), đối chiếu `transaction_id` để chống cộng tiền trùng lặp (Idempotency).
  4. Tự động tạo Phiếu thu (`payment_receipt`), trừ `current_debt` của khách, đổi trạng thái đơn `#ORD-9871` sang `PAID` và đẩy sự kiện qua WebSocket để màn hình POS/Kế toán phát âm thanh *"Đã nhận 250.000.000đ từ đơn ORD-9871"*.

---

### 1.5. Luồng "Trò chuyện nội bộ" Tích hợp Thẻ Nghiệp vụ & "AI Copilot" Thực chiến
* **Interactive Rich-Card trong Chat:**
  * Khi nhân viên gõ `#ORD-9871` hoặc `@SKU-2101`, trình soạn thảo tin nhắn nhận diện qua Regex và đính kèm metadata JSON vào payload tin nhắn.
  * Giao diện khung chat render thành một **Live Widget** hiển thị trạng thái thực tế của đơn hàng kèm 2 nút bấm hành động trực tiếp (`[Duyệt Công Nợ]`, `[Xác Nhận Xuất Kho]`) kiểm tra quyền RBAC ngay tại chỗ.
* **AI Copilot - Bóc tách Tin nhắn Đặt hàng bằng Structured Outputs:**
  * Đầu vào: Tin nhắn văn bản tự nhiên của khách sỉ (VD: *"Em ơi lấy cho anh 5 thùng mã 2101 với 20 cái áo trắng size L về địa chỉ kho Hà Đông,记 nợ nhé"*).
  * Xử lý: Kết hợp **Hybrid Search (Full-text + Vector Embedding)** để ánh xạ từ khóa `"áo trắng size L"` sang đúng `SKU` trong kho của shop, sau đó dùng LLM JSON Schema Mode để trả về cấu trúc đơn hàng nháp chính xác 100% chỉ chờ Sale bấm 1 nút xác nhận.

---

## 2. KIẾN TRÚC TỐI ƯU HIỆU NĂNG CAO (HIGH-PERFORMANCE ARCHITECTURE)

Để bảng điều khiển luôn đạt trạng thái **"Hệ thống vận hành ổn định 99.98% - Đang đồng bộ thực"** và không bị treo khi dữ liệu lên tới hàng triệu bản ghi, hệ thống cần áp dụng các kỹ thuật tối ưu sau ở cả Frontend, Backend và Database:

### 2.1. Tối ưu Hiệu năng Frontend (React / Next.js / Vite)
1. **Local-First & Offline POS với IndexedDB (Dexie.js):**
   * Toàn bộ danh mục Sản phẩm (SKU, Mã vạch, Giá bán, Tồn khả dụng) được đồng bộ ngầm về **IndexedDB** ngay khi mở trang theo cơ chế **Delta Sync** (chỉ tải các SKU có `updated_at > last_sync_time`).
   * Khi thu ngân quét mã vạch bằng súng bắn mã, ứng dụng truy vấn trực tiếp từ IndexedDB trong bộ nhớ trình duyệt với độ trễ **$< 5\text{ms}$** (không cần đợi gọi API mạng).
2. **Ảo hóa Danh sách Lớn (DOM Virtualization):**
   * Ở trang **Quản lý Sản phẩm** và **Quản lý Đơn hàng**, sử dụng `@tanstack/react-virtual` để chỉ render khoảng $15 - 20$ dòng đang hiển thị trên màn hình. Dù bảng có $50.000$ mã SKU, trình duyệt vẫn giữ mức tiêu thụ RAM ổn định và cuộn mượt ở $60\text{ FPS}$.
3. **Optimistic UI & Stale-While-Revalidate:**
   * Sử dụng `TanStack Query (React Query)`: Khi nhân viên đổi trạng thái đơn hàng hoặc gửi tin nhắn nội bộ, giao diện cập nhật ngay lập tức ($0\text{ms}$) trước khi Server phản hồi.
4. **Tối ưu Quay Video Đóng hàng (WebRTC + Web Workers):**
   * Dùng `MediaRecorder API` chuẩn nén `video/webm;codecs=vp9` ở độ phân giải $720\text{p}$ ($15\text{ fps}$) để dung lượng file video 1 phút chỉ nặng khoảng $3\text{MB} - 5\text{MB}$.
   * Quá trình cắt ghép và upload lên Cloud Storage (Cloudflare R2 / AWS S3) qua **Multipart Presigned URL** được đẩy sang **Web Worker** chạy ngầm, không làm đơ giao diện đóng hàng của nhân viên kho.

---

### 2.2. Tối ưu Hiệu năng Backend & Xử lý Đồng thời (Concurrency & Caching)
1. **Chống Race Condition khi Chốt đơn Cùng lúc (Atomic Inventory Reservation):**
   * Khi 2 nhân viên Sale cùng bấm chốt 1 sản phẩm chỉ còn tồn cuối cùng, tuyệt đối không đọc tồn kho lên code rồi mới trừ (`SELECT` rồi `UPDATE`).
   * **Giải pháp 1 (SQL Atomic Update):**
     ```sql
     UPDATE warehouse_inventories
     SET reserved_stock = reserved_stock + :qty,
         version = version + 1
     WHERE variant_id = :variant_id 
       AND warehouse_id = :warehouse_id
       AND (physical_stock - reserved_stock) >= :qty;
     ```
     Nếu `affected_rows == 0`, trả về lỗi *"Sản phẩm vừa hết hàng"* ngay lập tức.
   * **Giải pháp 2 (Redis Lua Script cho Flash Sale):** Đưa số lượng tồn khả dụng lên Redis và trừ nguyên tử trên RAM trong $< 1\text{ms}$.
2. **Kiến trúc Bất đồng bộ với Message Queue (BullMQ / Redis):**
   * Khi bấm **"Tạo đơn hàng"** hoặc **"Xuất kho"**, API chính chỉ thực hiện giao dịch DB cốt lõi và trả kết quả về cho người dùng trong $< 50\text{ms}$.
   * Các tác vụ nặng như: *Tạo file PDF hóa đơn, gửi thông báo đẩy, đồng bộ ngược tồn kho lên Shopee/TikTok, ghi Audit Log, tính lại báo cáo* được đẩy vào **Background Worker Queue** xử lý bất đồng bộ.
3. **Tối ưu Kết nối Thời gian thực (WebSocket Multiplexing):**
   * Gộp tất cả các luồng: *Trò chuyện nội bộ, Thông báo đơn hàng mới, Trạng thái đồng bộ hệ thống, Thông báo tiền về* vào **1 kết nối WebSocket duy nhất** (phân tách qua các kênh `channel:tenant_{id}`, `channel:warehouse_{id}`).

---

### 2.3. Tối ưu Cơ sở dữ liệu (PostgreSQL High-Performance Tuning)
1. **Chiến lược Đánh Index Chuyên sâu (Composite & Partial Indexes):**
   * Bảng đơn hàng và tồn kho bắt buộc đánh Index kết hợp theo `tenant_id` đứng đầu:
     * `CREATE INDEX idx_orders_tenant_status_date ON orders (tenant_id, status, created_at DESC);`
     * `CREATE UNIQUE INDEX idx_variants_tenant_sku ON product_variants (tenant_id, sku);`
     * `CREATE INDEX idx_variants_barcode ON product_variants (tenant_id, barcode) WHERE is_active = true;` (Partial Index giúp tìm kiếm mã vạch nhanh gấp 5 lần).
2. **Báo cáo Dashboard Tức thì với Materialized Views (Pre-aggregated Analytics):**
   * Biểu đồ **"Hiệu suất Doanh thu & Đơn hàng (Theo Tuần / Tháng / Quý)"** trên Dashboard tuyệt đối không dùng câu lệnh `SELECT SUM(total_amount) FROM orders` quét toàn bộ bảng mỗi lần F5.
   * Tạo bảng tổng hợp sẵn theo ngày `daily_metrics_summary` hoặc dùng **PostgreSQL Materialized View** làm mới tự động, giúp thời gian tải trang Dashboard luôn đạt **$< 20\text{ms}$** bất kể doanh nghiệp có 10 triệu đơn hàng.
3. **Phân vùng Bảng Lớn (Table Partitioning):**
   * Hai bảng phình to nhanh nhất là **Nhật ký hoạt động (`audit_logs`)** và **Lịch sử biến động kho (`inventory_transactions`)** cần được thiết lập **Range Partitioning theo tháng (`created_at`)**.

---

## 3. THIẾT KẾ DATABASE SCHEMA CỐT LÕI (ERD SPECIFICATION)

```sql
-- 1. Bảng Biến thể Sản phẩm (Quản lý tới cấp độ SKU/Barcode)
CREATE TABLE product_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    sku VARCHAR(64) NOT NULL,
    barcode VARCHAR(64),
    variant_name VARCHAR(255) NOT NULL, -- VD: Áo Polo Đen - Size XL
    base_unit VARCHAR(32) NOT NULL,     -- Đơn vị cơ sở: Cái, Hộp
    moving_avg_cost NUMERIC(15, 2) NOT NULL DEFAULT 0, -- Giá vốn bình quân hiện tại
    base_retail_price NUMERIC(15, 2) NOT NULL DEFAULT 0,
    min_stock_alert INT DEFAULT 10,
    is_active BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Bảng Tồn kho Đa điểm (Tách biệt Tồn thực và Tồn giữ chỗ)
CREATE TABLE warehouse_inventories (
    tenant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    physical_stock NUMERIC(15, 3) NOT NULL DEFAULT 0,
    reserved_stock NUMERIC(15, 3) NOT NULL DEFAULT 0,
    incoming_stock NUMERIC(15, 3) NOT NULL DEFAULT 0,
    bin_location VARCHAR(64), -- Vị trí kệ kho: Kệ A - Tầng 2 - Ô 05
    version BIGINT NOT NULL DEFAULT 1, -- Dùng cho Optimistic Locking
    PRIMARY KEY (tenant_id, warehouse_id, variant_id),
    CONSTRAINT chk_positive_available CHECK (physical_stock >= 0 AND reserved_stock >= 0)
);

-- 3. Bảng Sổ cái Công nợ Khách hàng / Đại lý
CREATE TABLE customer_credit_accounts (
    customer_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    price_tier VARCHAR(32) DEFAULT 'RETAIL', -- RETAIL, DROPSHIP, WHOLESALE_L1, VIP
    credit_limit NUMERIC(15, 2) NOT NULL DEFAULT 0, -- Hạn mức nợ tối đa
    current_debt NUMERIC(15, 2) NOT NULL DEFAULT 0, -- Dư nợ hiện tại
    overdue_days INT DEFAULT 0,
    is_credit_locked BOOLEAN DEFAULT FALSE
);

-- 4. Bảng Chi tiết Đơn hàng (Lưu cứng Giá vốn tại thời điểm xuất)
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    uom_name VARCHAR(32) NOT NULL,       -- Đơn vị lúc bán: Thùng / Cái
    uom_conversion_rate NUMERIC(10, 2) DEFAULT 1, -- Hệ số quy đổi ra đơn vị cơ sở
    quantity NUMERIC(15, 3) NOT NULL,
    unit_price NUMERIC(15, 2) NOT NULL,  -- Giá bán sau khi áp bảng giá
    discount_amount NUMERIC(15, 2) DEFAULT 0,
    snapshot_cost_price NUMERIC(15, 2) NOT NULL -- Giá vốn đóng băng lúc xuất kho để tính Lãi Gộp
);
```

---

## 4. LỘ TRÌNH TRIỂN KHAI KỸ THUẬT & CHỈ SỐ ĐO LƯỜNG (KPIs)

| Giai đoạn | Hạng mục Kỹ thuật & Nghiệp vụ | Chỉ số Hiệu năng Mục tiêu (Benchmark) |
| :--- | :--- | :--- |
| **Giai đoạn 1: Lõi Kho, Giá vốn & Công nợ** | - Thiết lập Schema Đa kho (`physical` vs `reserved`)<br>- Thuật toán Giá vốn `MAC` & Báo cáo lãi gộp<br>- Bảng giá sỉ/lẻ & Kiểm soát hạn mức nợ | - Không bao giờ âm kho hoặc bán lố khi 50 người cùng đặt 1 SKU.<br>- Sai số tính giá vốn và công nợ $= 0\text{ VNĐ}$. |
| **Giai đoạn 2: POS Siêu tốc & Tự động hóa Thanh toán** | - Màn hình POS tích hợp IndexedDB + Quét Barcode<br>- Tích hợp Webhook VietQR gạch nợ tự động<br>- Nâng cấp Chat nội bộ nhúng thẻ `#ORD` & Quay video đóng gói | - Quét mã vạch hiện sản phẩm $< 10\text{ms}$ (kể cả khi đứt cáp mạng).<br>- Từ lúc khách chuyển khoản đến lúc báo âm thanh thu ngân $< 3\text{ giây}$. |
| **Giai đoạn 3: AI Copilot, Đa kênh & Đóng gói SaaS** | - AI bóc tách tin nhắn lên đơn + Cảnh báo tồn kho<br>- Đồng bộ Shopee/TikTok Shop & Đẩy đơn GHN/GHTK<br>- Kiến trúc Multi-tenancy + Trang tự mua gói cước | - AI bóc tách đơn hàng nháp trong $< 2.5\text{ giây}$.<br>- Tải trang Dashboard với $1.000.000$ đơn hàng $< 50\text{ms}$ nhờ Materialized View. |