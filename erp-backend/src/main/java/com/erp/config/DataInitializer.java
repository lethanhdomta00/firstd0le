package com.erp.config;

import com.erp.branch.model.*;
import com.erp.branch.repository.*;
import com.erp.finance.model.*;
import com.erp.finance.repository.BankStatementItemRepository;
import com.erp.finance.repository.FinanceVoucherRepository;
import com.erp.finance.repository.TreasuryAccountRepository;
import com.erp.model.Announcement;
import com.erp.model.Role;
import com.erp.model.User;
import com.erp.repository.AnnouncementRepository;
import com.erp.repository.UserRepository;
import com.erp.scheduler.model.JobStatus;
import com.erp.scheduler.model.ScheduledJob;
import com.erp.scheduler.repository.ScheduledJobRepository;
import com.erp.procurement.model.*;
import com.erp.procurement.repository.PurchaseOrderRepository;
import com.erp.procurement.repository.SupplierRepository;
import com.erp.promotion.model.*;
import com.erp.promotion.repository.PromotionVoucherRepository;
import com.erp.promotion.repository.TieredPriceRuleRepository;
import com.erp.repository.CustomerRepository;
import com.erp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AnnouncementRepository announcementRepository;
    private final ScheduledJobRepository scheduledJobRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final TreasuryAccountRepository treasuryAccountRepository;
    private final FinanceVoucherRepository financeVoucherRepository;
    private final BankStatementItemRepository bankStatementItemRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final PromotionVoucherRepository promotionVoucherRepository;
    private final TieredPriceRuleRepository tieredPriceRuleRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final InternalTransferRepository internalTransferRepository;

    @Override
    public void run(String... args) {
        try {
            // Tự động nâng cấp cột role từ enum cũ lên VARCHAR(30) để hỗ trợ đầy đủ các Roles RBAC mở rộng
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(30) NOT NULL DEFAULT 'USER'");
            log.info("Migrated users.role column to VARCHAR(30) for dynamic RBAC roles");
        } catch (Exception e) {
            log.warn("Notice on users.role schema alter: {}", e.getMessage());
        }

        // 1. Đảm bảo tài khoản ADMIN
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        if (adminOpt.isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("password"));
            admin.setRole(Role.ADMIN);
            admin.setStatus("ACTIVE");
            admin.setEmail("dolemta090899@gmail.com");
            userRepository.save(admin);
            log.info("Initialized default ADMIN account: admin / password (dolemta090899@gmail.com)");
        } else {
            User admin = adminOpt.get();
            boolean changed = false;
            if (admin.getRole() != Role.ADMIN) {
                admin.setRole(Role.ADMIN);
                changed = true;
            }
            if (!"ACTIVE".equalsIgnoreCase(admin.getStatus())) {
                admin.setStatus("ACTIVE");
                changed = true;
            }
            if (admin.getEmail() == null || admin.getEmail().isEmpty() || "admin@erp.com".equals(admin.getEmail())) {
                admin.setEmail("dolemta090899@gmail.com");
                changed = true;
            }
            if (changed) {
                userRepository.save(admin);
                log.info("Updated admin account with active status and real email (dolemta090899@gmail.com)");
            }
        }

        // 2. Khởi tạo tài khoản Nhân viên (STAFF) mẫu để test Chat Real-Time
        Optional<User> staffOpt = userRepository.findByUsername("nhanvien");
        if (staffOpt.isEmpty()) {
            User staff = new User();
            staff.setUsername("nhanvien");
            staff.setFullName("Nguyễn Văn A (Kinh doanh)");
            staff.setPassword(passwordEncoder.encode("password"));
            staff.setRole(Role.USER);
            staff.setStatus("ACTIVE");
            staff.setEmail("nhanvien@erp.com");
            staff.setPhone("0987654321");
            staff.setAvatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80");
            userRepository.save(staff);
            log.info("Initialized default STAFF account for testing: nhanvien / password");
        }

        // 3. Đảm bảo tài khoản HIHI (nếu có) có mật khẩu chuẩn là 'password'
        Optional<User> hihiOpt = userRepository.findByUsername("HIHI");
        if (hihiOpt.isPresent()) {
            User hihi = hihiOpt.get();
            hihi.setPassword(passwordEncoder.encode("password"));
            hihi.setStatus("ACTIVE");
            if (hihi.getFullName() == null || hihi.getFullName().isEmpty()) {
                hihi.setFullName("Trần Thị B (Kế toán)");
            }
            userRepository.save(hihi);
            log.info("Updated HIHI password to: password");
        }

        // 4. Khởi tạo tài khoản Copilot AI Bot (Trợ lý điều hành ảo)
        Optional<User> copilotOpt = userRepository.findByUsername("copilot");
        if (copilotOpt.isEmpty()) {
            User copilot = new User();
            copilot.setUsername("copilot");
            copilot.setFullName("ERP Copilot");
            copilot.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            copilot.setRole(Role.ADMIN);
            copilot.setStatus("ACTIVE");
            copilot.setEmail("copilot@erp-internal.ai");
            copilot.setPhone("1900-ERP-AI");
            copilot.setBio("Trợ lý điều hành thông minh đa chiều của hệ thống ERP Cloud");
            copilot.setAvatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=copilot");
            userRepository.save(copilot);
            log.info("Initialized default COPILOT AI Bot account: copilot");
        }

        // 5. Khởi tạo Thông báo / Sự kiện mẫu nếu bảng trống
        if (announcementRepository.count() == 0) {
            Announcement defaultAnnouncement = new Announcement();
            defaultAnnouncement.setTitle("Chào mừng hệ thống");
            defaultAnnouncement.setContent("🎉 Chào mừng bạn đến với ERP Cloud System! Chúc bạn một ngày làm việc hiệu quả, năng lượng và hoàn thành xuất sắc mọi mục tiêu.");
            defaultAnnouncement.setType("EVENT");
            defaultAnnouncement.setActive(true);
            announcementRepository.save(defaultAnnouncement);
            log.info("Initialized default active system announcement");
        }

        // 5. Khởi tạo 3 Tác vụ Lập lịch Động (Dynamic Scheduler Jobs) mẫu chuẩn Doanh nghiệp
        initDefaultSchedulerJobs();

        // 6. Khởi tạo Quỹ Tài khoản & Dữ liệu Tài chính Doanh nghiệp (Giai đoạn 2)
        initDefaultTreasuryAndFinance();

        // 7. Khởi tạo Động cơ Khuyến mại, Voucher & Loyalty (Giai đoạn 4)
        initPromotionsAndLoyaltyData();
    }

    private void initDefaultSchedulerJobs() {
        if (!scheduledJobRepository.existsByJobCode("JOB_INVENTORY_ALERT")) {
            ScheduledJob inventoryJob = ScheduledJob.builder()
                    .jobCode("JOB_INVENTORY_ALERT")
                    .jobName("Quét & Cảnh báo Tồn kho an toàn")
                    .jobGroup("INVENTORY")
                    .description("Tự động rà soát hàng tồn kho dưới ngưỡng an toàn và gửi cảnh báo tức thì về Telegram Quản lý")
                    .cronExpression("0 0 9,14 * * ?") // 9h sáng và 14h chiều mỗi ngày
                    .jobType("INVENTORY_AUDIT")
                    .status(JobStatus.ACTIVE)
                    .paramsJson("{\"threshold\": 10, \"notifyTelegram\": true}")
                    .build();
            scheduledJobRepository.save(inventoryJob);
            log.info("Initialized default enterprise job: JOB_INVENTORY_ALERT");
        }

        if (!scheduledJobRepository.existsByJobCode("JOB_DAILY_SALES")) {
            ScheduledJob salesJob = ScheduledJob.builder()
                    .jobCode("JOB_DAILY_SALES")
                    .jobName("Tổng hợp Báo cáo Kho vận & Giá trị hàng hóa")
                    .jobGroup("FINANCE")
                    .description("Tổng hợp tổng giá trị tài sản hàng hóa trong kho và các mặt hàng hết hàng gửi báo cáo cuối ngày")
                    .cronExpression("0 0 18 * * ?") // 18:00 chiều mỗi ngày
                    .jobType("DAILY_SALES_REPORT")
                    .status(JobStatus.ACTIVE)
                    .paramsJson("{\"notifyTelegram\": true}")
                    .build();
            scheduledJobRepository.save(salesJob);
            log.info("Initialized default enterprise job: JOB_DAILY_SALES");
        }

        if (!scheduledJobRepository.existsByJobCode("JOB_CLEANUP_TEMP")) {
            ScheduledJob cleanupJob = ScheduledJob.builder()
                    .jobCode("JOB_CLEANUP_TEMP")
                    .jobName("Dọn dẹp File đệm & Tối ưu hóa dữ liệu rác")
                    .jobGroup("SYSTEM")
                    .description("Tự động giải phóng các tệp tin tạm trên ổ đĩa có tuổi thọ trên 7 ngày để tối ưu dung lượng hệ thống")
                    .cronExpression("0 0 2 * * ?") // 2:00 sáng mỗi ngày
                    .jobType("CLEANUP_TEMP_FILES")
                    .status(JobStatus.ACTIVE)
                    .paramsJson("{\"maxAgeDays\": 7}")
                    .build();
            scheduledJobRepository.save(cleanupJob);
            log.info("Initialized default enterprise job: JOB_CLEANUP_TEMP");
        }
    }

    private void initDefaultTreasuryAndFinance() {
        if (treasuryAccountRepository.count() == 0) {
            TreasuryAccount cashOffice = TreasuryAccount.builder()
                    .accountCode("CASH_OFFICE")
                    .accountName("Quỹ tiền mặt Văn phòng")
                    .accountType(TreasuryAccountType.CASH)
                    .currentBalance(new java.math.BigDecimal("45000000.00"))
                    .active(true)
                    .note("Tiền mặt chi tiêu tác nghiệp hàng ngày tại văn phòng")
                    .build();

            TreasuryAccount bankVcb = TreasuryAccount.builder()
                    .accountCode("BANK_VCB")
                    .accountName("Vietcombank - Tài khoản Chính")
                    .accountType(TreasuryAccountType.BANK)
                    .bankName("Vietcombank")
                    .accountNumber("0071001234567")
                    .accountHolder("CTY CP ERP CLOUD ENTERPRISE")
                    .currentBalance(new java.math.BigDecimal("185000000.00"))
                    .active(true)
                    .note("Tài khoản nhận thanh toán doanh thu bán hàng & chi lương")
                    .build();

            TreasuryAccount bankMb = TreasuryAccount.builder()
                    .accountCode("BANK_MB")
                    .accountName("MB Bank - Tài khoản Thu hộ POS")
                    .accountType(TreasuryAccountType.BANK)
                    .bankName("MBBank")
                    .accountNumber("0988777888")
                    .accountHolder("CTY CP ERP CLOUD ENTERPRISE")
                    .currentBalance(new java.math.BigDecimal("92500000.00"))
                    .active(true)
                    .note("Tài khoản đối soát QR code và máy POS bán lẻ")
                    .build();

            TreasuryAccount bankTcb = TreasuryAccount.builder()
                    .accountCode("BANK_TCB")
                    .accountName("Techcombank - Quỹ Dự phòng Đầu tư")
                    .accountType(TreasuryAccountType.BANK)
                    .bankName("Techcombank")
                    .accountNumber("19036789999011")
                    .accountHolder("CTY CP ERP CLOUD ENTERPRISE")
                    .currentBalance(new java.math.BigDecimal("150000000.00"))
                    .active(true)
                    .note("Quỹ thặng dư dòng tiền dự phòng khẩn cấp")
                    .build();

            treasuryAccountRepository.saveAll(java.util.List.of(cashOffice, bankVcb, bankMb, bankTcb));
            log.info("Initialized default 4 Treasury Accounts (Cash & Banks)");

            // Khởi tạo một số phiếu chi mẫu
            FinanceVoucher pv1 = FinanceVoucher.builder()
                    .voucherCode("PV-20261118-001")
                    .voucherType(VoucherType.PAYMENT)
                    .costCenter(CostCenter.RENTAL)
                    .amount(new java.math.BigDecimal("15000000.00"))
                    .sourceAccount(bankVcb)
                    .payeePayorName("Tòa nhà Landmark Central")
                    .note("Thanh toán tiền thuê mặt bằng văn phòng tháng 11/2026")
                    .status(VoucherStatus.APPROVED)
                    .createdByUsername("admin")
                    .approvedByUsername("admin")
                    .approvedAt(java.time.LocalDateTime.now().minusDays(2))
                    .build();

            FinanceVoucher pv2 = FinanceVoucher.builder()
                    .voucherCode("PV-20261119-002")
                    .voucherType(VoucherType.PAYMENT)
                    .costCenter(CostCenter.MARKETING)
                    .amount(new java.math.BigDecimal("8500000.00"))
                    .sourceAccount(bankVcb)
                    .payeePayorName("Công ty TNHH Quảng cáo Google/Facebook")
                    .note("Chi phí chạy quảng cáo chiến dịch Áo Polo Nam")
                    .status(VoucherStatus.APPROVED)
                    .createdByUsername("admin")
                    .approvedByUsername("admin")
                    .approvedAt(java.time.LocalDateTime.now().minusDays(1))
                    .build();

            FinanceVoucher pv3 = FinanceVoucher.builder()
                    .voucherCode("PV-20261120-003")
                    .voucherType(VoucherType.PAYMENT)
                    .costCenter(CostCenter.UTILITIES)
                    .amount(new java.math.BigDecimal("2800000.00"))
                    .sourceAccount(cashOffice)
                    .payeePayorName("Điện lực TP.HCM")
                    .note("Tiền điện chiếu sáng và máy lạnh kho tháng 10")
                    .status(VoucherStatus.APPROVED)
                    .createdByUsername("admin")
                    .approvedByUsername("admin")
                    .approvedAt(java.time.LocalDateTime.now())
                    .build();

            financeVoucherRepository.saveAll(java.util.List.of(pv1, pv2, pv3));
            log.info("Initialized default 3 Finance Expense Vouchers");
        }

        if (bankStatementItemRepository.count() == 0) {
            BankStatementItem s1 = BankStatementItem.builder()
                    .transactionCode("FT26324891001")
                    .transactionDate(java.time.LocalDateTime.now().minusHours(3))
                    .amount(new java.math.BigDecimal("598000.00"))
                    .transactionType("CR")
                    .description("NGUYEN THI LAN CHUYEN KHOAN DON HANG ORD-20261114-1001")
                    .bankName("Vietcombank")
                    .accountNumber("0071001234567")
                    .matchedOrderCode("ORD-20261114-1001")
                    .matchedCustomerName("Nguyễn Thị Lan")
                    .status(ReconciliationStatus.MATCHED)
                    .reconciliationNote("Khớp 100% mã đơn hàng ORD-20261114-1001")
                    .build();

            BankStatementItem s2 = BankStatementItem.builder()
                    .transactionCode("FT26324891002")
                    .transactionDate(java.time.LocalDateTime.now().minusHours(2))
                    .amount(new java.math.BigDecimal("897000.00"))
                    .transactionType("CR")
                    .description("CONG TY TNHH MINH TUAN THANH TOAN TIEN HANG DON ORD-20261114-1002")
                    .bankName("Vietcombank")
                    .accountNumber("0071001234567")
                    .matchedOrderCode("ORD-20261114-1002")
                    .matchedCustomerName("Minh Tuấn")
                    .status(ReconciliationStatus.MATCHED)
                    .reconciliationNote("Khớp 100% mã đơn hàng ORD-20261114-1002")
                    .build();

            BankStatementItem s3 = BankStatementItem.builder()
                    .transactionCode("FT26324891003")
                    .transactionDate(java.time.LocalDateTime.now().minusHours(1))
                    .amount(new java.math.BigDecimal("1200000.00"))
                    .transactionType("CR")
                    .description("KHACH LE CHUYEN KHOAN TIEN HANG KHONG GHI MA DON")
                    .bankName("Vietcombank")
                    .accountNumber("0071001234567")
                    .status(ReconciliationStatus.UNMATCHED)
                    .reconciliationNote("Chưa phát hiện mã đơn hàng trong nội dung giao dịch")
                    .build();

            bankStatementItemRepository.saveAll(java.util.List.of(s1, s2, s3));
            log.info("Initialized default 3 Bank Statement items for smart reconciliation");
        }

        // 7. Khởi tạo Phân hệ Mua hàng & SRM (Giai đoạn 3)
        if (supplierRepository.count() == 0) {
            Supplier sup1 = Supplier.builder()
                    .code("NCC-TANBINH")
                    .name("Xưởng May Dệt Kim Tân Bình")
                    .contactPerson("Anh Hoàng Minh (Chủ xưởng)")
                    .phone("0912345678")
                    .email("detkimtanbinh@gmail.com")
                    .address("Lô C4, KCN Tân Bình, P. Tây Thạnh, Q. Tân Phú, TP.HCM")
                    .taxCode("0312456789")
                    .creditLimit(new java.math.BigDecimal("500000000.00"))
                    .currentDebt(new java.math.BigDecimal("65000000.00"))
                    .paymentTermsDays(30)
                    .status("ACTIVE")
                    .notes("Đối tác cung ứng vải dệt kim cotton & may gia công áo Polo xuất khẩu")
                    .build();

            Supplier sup2 = Supplier.builder()
                    .code("NCC-SAIGON-FABRIC")
                    .name("Tổng Kho Vải & Phụ Liệu Sài Gòn")
                    .contactPerson("Chị Thu Trang")
                    .phone("0988776655")
                    .email("saigonfabric@vnn.vn")
                    .address("45 Đường số 7, Cư xá Bình Thới, P.8, Q.11, TP.HCM")
                    .taxCode("0309876543")
                    .creditLimit(new java.math.BigDecimal("300000000.00"))
                    .currentDebt(new java.math.BigDecimal("32500000.00"))
                    .paymentTermsDays(45)
                    .status("ACTIVE")
                    .notes("Nhà cung cấp vải kaki, đũi và phụ liệu may mặc cao cấp")
                    .build();

            Supplier sup3 = Supplier.builder()
                    .code("NCC-HOANGGIA")
                    .name("Xưởng Phụ Kiện & Cúc Nút Hoàng Gia")
                    .contactPerson("Anh Quốc Bảo")
                    .phone("0903332211")
                    .email("phukienhoanggia@gmail.com")
                    .address("128 Hùng Vương, P.2, Q.5, TP.HCM")
                    .taxCode("0315554433")
                    .creditLimit(new java.math.BigDecimal("100000000.00"))
                    .currentDebt(new java.math.BigDecimal("8500000.00"))
                    .paymentTermsDays(15)
                    .status("ACTIVE")
                    .notes("Chuyên cúc áo dập nổi logo, khóa kéo YKK & nhãn mác dệt")
                    .build();

            var savedSups = supplierRepository.saveAll(java.util.List.of(sup1, sup2, sup3));
            log.info("Initialized default 3 Suppliers for Procurement & SRM");

            // Tạo 1 đơn PO mẫu đã APPROVED để test luồng Nhập kho GRN & Giá vốn bình quân
            var products = productRepository.findAll();
            if (!products.isEmpty() && !savedSups.isEmpty()) {
                var p1 = products.get(0);
                var p2 = products.size() > 1 ? products.get(1) : products.get(0);
                Supplier primarySup = savedSups.get(0);

                PurchaseOrder po1 = PurchaseOrder.builder()
                        .poCode("PO-20261122-001")
                        .supplier(primarySup)
                        .supplierName(primarySup.getName())
                        .status(PurchaseOrderStatus.APPROVED)
                        .orderDate(java.time.LocalDate.now().minusDays(2))
                        .expectedDeliveryDate(java.time.LocalDate.now().plusDays(3))
                        .discountAmount(new java.math.BigDecimal("500000.00"))
                        .taxAmount(new java.math.BigDecimal("1200000.00"))
                        .notes("Đơn hàng đặt lô áo chuẩn bị mở bán đợt lễ cuối năm")
                        .creatorUsername("admin")
                        .approverUsername("admin")
                        .approvedAt(java.time.LocalDateTime.now().minusDays(1))
                        .items(new java.util.ArrayList<>())
                        .build();

                PurchaseOrderItem poi1 = PurchaseOrderItem.builder()
                        .purchaseOrder(po1)
                        .productId(p1.getId())
                        .productSku(p1.getSku())
                        .productName(p1.getName())
                        .unitName(p1.getBaseUnit() != null ? p1.getBaseUnit() : "Cái")
                        .orderedQuantity(100)
                        .receivedQuantity(0)
                        .unitPrice(new java.math.BigDecimal("95000.00"))
                        .build();
                poi1.calculateSubtotal();
                po1.getItems().add(poi1);

                if (!p1.getId().equals(p2.getId())) {
                    PurchaseOrderItem poi2 = PurchaseOrderItem.builder()
                            .purchaseOrder(po1)
                            .productId(p2.getId())
                            .productSku(p2.getSku())
                            .productName(p2.getName())
                            .unitName(p2.getBaseUnit() != null ? p2.getBaseUnit() : "Cái")
                            .orderedQuantity(50)
                            .receivedQuantity(0)
                            .unitPrice(new java.math.BigDecimal("140000.00"))
                            .build();
                    poi2.calculateSubtotal();
                    po1.getItems().add(poi2);
                }

                po1.recalculateTotals();
                purchaseOrderRepository.save(po1);
                log.info("Initialized default Purchase Order PO-20261122-001 (APPROVED)");
            }
        }
    }

    private void initPromotionsAndLoyaltyData() {
        if (promotionVoucherRepository.count() == 0) {
            PromotionVoucher v1 = new PromotionVoucher();
            v1.setCode("VIP10");
            v1.setName("Tri Ân Khách VIP - Giảm 10%");
            v1.setDescription("Giảm 10% tối đa 100.000 VNĐ cho đơn hàng từ 200.000 VNĐ");
            v1.setDiscountType(DiscountType.PERCENTAGE);
            v1.setDiscountValue(new java.math.BigDecimal("10.00"));
            v1.setMaxDiscountAmount(new java.math.BigDecimal("100000.00"));
            v1.setMinOrderValue(new java.math.BigDecimal("200000.00"));
            v1.setStartDate(java.time.LocalDateTime.now().minusDays(5));
            v1.setEndDate(java.time.LocalDateTime.now().plusMonths(3));
            v1.setMaxUsage(200);
            v1.setUsedCount(0);
            v1.setMaxUsagePerCustomer(2);
            v1.setActive(true);

            PromotionVoucher v2 = new PromotionVoucher();
            v2.setCode("GIAM50K");
            v2.setName("Mã Ưu Đãi Mua Sắm - Giảm 50.000đ");
            v2.setDescription("Giảm 50.000 VNĐ cố định cho đơn hàng từ 350.000 VNĐ");
            v2.setDiscountType(DiscountType.FIXED_AMOUNT);
            v2.setDiscountValue(new java.math.BigDecimal("50000.00"));
            v2.setMinOrderValue(new java.math.BigDecimal("350000.00"));
            v2.setStartDate(java.time.LocalDateTime.now().minusDays(1));
            v2.setEndDate(java.time.LocalDateTime.now().plusMonths(1));
            v2.setMaxUsage(100);
            v2.setUsedCount(0);
            v2.setMaxUsagePerCustomer(1);
            v2.setActive(true);

            PromotionVoucher v3 = new PromotionVoucher();
            v3.setCode("FREESHIP");
            v3.setName("Hỗ Trợ Phí Vận Chuyển - Giảm 30.000đ");
            v3.setDescription("Giảm 30.000 VNĐ cho mọi đơn hàng từ 150.000 VNĐ");
            v3.setDiscountType(DiscountType.FIXED_AMOUNT);
            v3.setDiscountValue(new java.math.BigDecimal("30000.00"));
            v3.setMinOrderValue(new java.math.BigDecimal("150000.00"));
            v3.setStartDate(java.time.LocalDateTime.now().minusDays(10));
            v3.setEndDate(java.time.LocalDateTime.now().plusMonths(6));
            v3.setMaxUsage(500);
            v3.setUsedCount(0);
            v3.setMaxUsagePerCustomer(5);
            v3.setActive(true);

            promotionVoucherRepository.saveAll(java.util.List.of(v1, v2, v3));
            log.info("Initialized default 3 Promotion Vouchers: VIP10, GIAM50K, FREESHIP");
        }

        if (tieredPriceRuleRepository.count() == 0) {
            TieredPriceRule rule1 = new TieredPriceRule();
            rule1.setTierName("Sỉ Bậc 1 (Mua từ 10 - 49 cái)");
            rule1.setMinQuantity(10);
            rule1.setMaxQuantity(49);
            rule1.setPricingType(PricingType.PERCENT_DISCOUNT);
            rule1.setDiscountPercent(new java.math.BigDecimal("10.00"));
            rule1.setActive(true);

            TieredPriceRule rule2 = new TieredPriceRule();
            rule2.setTierName("Sỉ Xuất Xưởng (Mua từ 50 cái trở lên)");
            rule2.setMinQuantity(50);
            rule2.setMaxQuantity(null);
            rule2.setPricingType(PricingType.PERCENT_DISCOUNT);
            rule2.setDiscountPercent(new java.math.BigDecimal("20.00"));
            rule2.setActive(true);

            tieredPriceRuleRepository.saveAll(java.util.List.of(rule1, rule2));
            log.info("Initialized default 2 Tiered Price Rules for Wholesale Pricing");
        }

        // Cập nhật điểm Loyalty mẫu cho khách hàng có sẵn
        var customers = customerRepository.findAll();
        if (!customers.isEmpty()) {
            boolean updated = false;
            for (int i = 0; i < customers.size(); i++) {
                var c = customers.get(i);
                if (c.getLoyaltyPoints() == null || c.getLoyaltyPoints() == 0) {
                    if (i == 0) {
                        c.setLoyaltyPoints(120);
                        c.setTotalSpent(new java.math.BigDecimal("25000000.00"));
                        c.setMemberTier(MemberTier.GOLD);
                        updated = true;
                    } else if (i == 1) {
                        c.setLoyaltyPoints(60);
                        c.setTotalSpent(new java.math.BigDecimal("8500000.00"));
                        c.setMemberTier(MemberTier.SILVER);
                        updated = true;
                    }
                }
            }
            if (updated) {
                customerRepository.saveAll(customers);
                log.info("Initialized sample loyalty points and member tiers for existing customers");
            }
        }

        // Khởi tạo Chi nhánh & Kho hàng mặc định (Giai đoạn 2 - Multi-Branch & Multi-Warehouse)
        if (branchRepository.count() == 0) {
            Branch b1 = new Branch();
            b1.setCode("BR-HN-HQ");
            b1.setName("Trụ sở chính & Chi nhánh Hà Nội");
            b1.setAddress("Tòa nhà Keangnam Landmark 72, Mễ Trì, Nam Từ Liêm, Hà Nội");
            b1.setPhone("024-37899999");
            b1.setStatus("ACTIVE");
            b1.setIsHeadquarter(true);

            Branch b2 = new Branch();
            b2.setCode("BR-HCM-01");
            b2.setName("Chi nhánh Quận 1 - TP. Hồ Chí Minh");
            b2.setAddress("Số 45 Lê Lợi, Bến Nghé, Quận 1, TP. Hồ Chí Minh");
            b2.setPhone("028-38221234");
            b2.setStatus("ACTIVE");
            b2.setIsHeadquarter(false);

            Branch b3 = new Branch();
            b3.setCode("BR-DN-01");
            b3.setName("Chi nhánh Hải Châu - Đà Nẵng");
            b3.setAddress("Số 120 Bạch Đằng, Hải Châu, Đà Nẵng");
            b3.setPhone("0236-3888999");
            b3.setStatus("ACTIVE");
            b3.setIsHeadquarter(false);

            branchRepository.saveAll(java.util.List.of(b1, b2, b3));
            log.info("Initialized default 3 Branches: HN-HQ, HCM-01, DN-01");

            Warehouse wh1 = new Warehouse();
            wh1.setBranchId(b1.getId());
            wh1.setBranchName(b1.getName());
            wh1.setCode("WH-TONG-MB");
            wh1.setName("Kho Tổng Miền Bắc (Hà Nội)");
            wh1.setWarehouseType(WarehouseType.CENTRAL_HUB);
            wh1.setAddress("KCN Quang Minh, Mê Linh, Hà Nội");
            wh1.setActive(true);

            Warehouse wh2 = new Warehouse();
            wh2.setBranchId(b1.getId());
            wh2.setBranchName(b1.getName());
            wh2.setCode("WH-HN-RETAIL");
            wh2.setName("Kho Bán lẻ Cầu Giấy (Hà Nội)");
            wh2.setWarehouseType(WarehouseType.RETAIL_STORE);
            wh2.setAddress("123 Cầu Giấy, Quan Hoa, Cầu Giấy, Hà Nội");
            wh2.setActive(true);

            Warehouse wh3 = new Warehouse();
            wh3.setBranchId(b2.getId());
            wh3.setBranchName(b2.getName());
            wh3.setCode("WH-HCM-RETAIL");
            wh3.setName("Kho Bán lẻ Quận 1 (TP.HCM)");
            wh3.setWarehouseType(WarehouseType.RETAIL_STORE);
            wh3.setAddress("Số 45 Lê Lợi, Bến Nghé, Quận 1, TP.HCM");
            wh3.setActive(true);

            Warehouse wh4 = new Warehouse();
            wh4.setBranchId(b1.getId());
            wh4.setBranchName(b1.getName());
            wh4.setCode("WH-DEFECT-01");
            wh4.setName("Kho Hàng Lỗi & Chờ Xử Lý");
            wh4.setWarehouseType(WarehouseType.DAMAGED_GOODS);
            wh4.setAddress("KCN Quang Minh, Mê Linh, Hà Nội");
            wh4.setActive(true);

            warehouseRepository.saveAll(java.util.List.of(wh1, wh2, wh3, wh4));
            log.info("Initialized default 4 Warehouses: WH-TONG-MB, WH-HN-RETAIL, WH-HCM-RETAIL, WH-DEFECT-01");

            // Phân bổ tồn kho đa điểm cho các sản phẩm hiện có
            var products = productRepository.findAll();
            if (!products.isEmpty()) {
                java.util.List<WarehouseStock> stockList = new java.util.ArrayList<>();
                for (var prod : products) {
                    WarehouseStock s1 = new WarehouseStock();
                    s1.setWarehouseId(wh1.getId());
                    s1.setWarehouseName(wh1.getName());
                    s1.setBranchId(b1.getId());
                    s1.setProductId(prod.getId());
                    s1.setProductSku(prod.getSku());
                    s1.setProductName(prod.getName());
                    s1.setPhysicalStock(120);
                    s1.setAvailableStock(120);
                    s1.setReservedStock(0);
                    s1.setInTransitStock(10);
                    s1.setMinStockAlert(15);
                    stockList.add(s1);

                    WarehouseStock s2 = new WarehouseStock();
                    s2.setWarehouseId(wh2.getId());
                    s2.setWarehouseName(wh2.getName());
                    s2.setBranchId(b1.getId());
                    s2.setProductId(prod.getId());
                    s2.setProductSku(prod.getSku());
                    s2.setProductName(prod.getName());
                    s2.setPhysicalStock(25);
                    s2.setAvailableStock(25);
                    s2.setReservedStock(0);
                    s2.setInTransitStock(0);
                    s2.setMinStockAlert(10);
                    stockList.add(s2);

                    WarehouseStock s3 = new WarehouseStock();
                    s3.setWarehouseId(wh3.getId());
                    s3.setWarehouseName(wh3.getName());
                    s3.setBranchId(b2.getId());
                    s3.setProductId(prod.getId());
                    s3.setProductSku(prod.getSku());
                    s3.setProductName(prod.getName());
                    s3.setPhysicalStock(30);
                    s3.setAvailableStock(30);
                    s3.setReservedStock(0);
                    s3.setInTransitStock(10);
                    s3.setMinStockAlert(10);
                    stockList.add(s3);

                    WarehouseStock s4 = new WarehouseStock();
                    s4.setWarehouseId(wh4.getId());
                    s4.setWarehouseName(wh4.getName());
                    s4.setBranchId(b1.getId());
                    s4.setProductId(prod.getId());
                    s4.setProductSku(prod.getSku());
                    s4.setProductName(prod.getName());
                    s4.setPhysicalStock(2);
                    s4.setAvailableStock(0);
                    s4.setReservedStock(0);
                    s4.setInTransitStock(0);
                    s4.setMinStockAlert(0);
                    stockList.add(s4);

                    // Cập nhật tổng tồn trên Product
                    prod.setPhysicalStock(177);
                    prod.setStock(175);
                }
                warehouseStockRepository.saveAll(stockList);
                productRepository.saveAll(products);
                log.info("Distributed multi-warehouse stocks for {} products across 4 warehouses", products.size());

                // Khởi tạo 1 Phiếu điều chuyển kho mẫu đang đi đường (IN_TRANSIT)
                var pFirst = products.get(0);
                InternalTransfer sampleTransfer = new InternalTransfer();
                sampleTransfer.setTransferCode("ITN-20261125-001");
                sampleTransfer.setFromWarehouseId(wh1.getId());
                sampleTransfer.setFromWarehouseName(wh1.getName());
                sampleTransfer.setToWarehouseId(wh3.getId());
                sampleTransfer.setToWarehouseName(wh3.getName());
                sampleTransfer.setStatus(TransferStatus.IN_TRANSIT);
                sampleTransfer.setTotalQuantity(10);
                sampleTransfer.setCreatedBy("admin");
                sampleTransfer.setTransferredBy("admin");
                sampleTransfer.setDepartureDate(java.time.LocalDateTime.now().minusHours(4));
                sampleTransfer.setNote("Điều chuyển bổ sung hàng hot từ Kho Tổng Miền Bắc vào Chi nhánh Quận 1 TP.HCM");

                InternalTransferItem sampleItem = new InternalTransferItem();
                sampleItem.setProductId(pFirst.getId());
                sampleItem.setProductSku(pFirst.getSku());
                sampleItem.setProductName(pFirst.getName());
                sampleItem.setRequestedQuantity(10);
                sampleItem.setTransferredQuantity(10);
                sampleItem.setReceivedQuantity(0);
                sampleItem.setDamagedQuantity(0);
                sampleItem.setItemNote("Kiện nguyên đai nguyên kiện");
                sampleTransfer.addItem(sampleItem);

                internalTransferRepository.save(sampleTransfer);
                log.info("Initialized sample in-transit Internal Transfer: ITN-20261125-001");
            }
        }
    }
}
