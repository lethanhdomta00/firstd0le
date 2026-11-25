package com.erp.config;

import com.erp.branch.model.*;
import com.erp.branch.repository.*;
import com.erp.inventory.model.*;
import com.erp.inventory.repository.*;
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
import com.erp.workflow.model.*;
import com.erp.workflow.repository.*;

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
    private final ProductBatchRepository productBatchRepository;
    private final ProductSerialRepository productSerialRepository;
    private final StocktakeAuditRepository stocktakeAuditRepository;
    private final ApprovalMatrixRuleRepository approvalMatrixRuleRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalStepHistoryRepository approvalStepHistoryRepository;
    private final ImmutableAuditLogRepository immutableAuditLogRepository;

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

        // 8. Khởi tạo Động cơ Phê duyệt Đa cấp & Ma trận Quy tắc (Giai đoạn 4)
        initWorkflowAndApprovalData();
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

        // Khởi tạo Dữ liệu Giai đoạn 3: Lô Hàng, Radar Cận Date, Serial / IMEI & Kiểm Kê Kho
        var allProds = productRepository.findAll();
        var allWhs = warehouseRepository.findAll();
        if (!allProds.isEmpty() && !allWhs.isEmpty()) {
            var firstP = allProds.get(0);
            var firstWh = allWhs.get(0);

            // 1. Khởi tạo Lô hàng mẫu (Batch/Lot & Expiry Radar)
            if (productBatchRepository.count() == 0) {
                java.time.LocalDate today = java.time.LocalDate.now();

                ProductBatch bSafe = new ProductBatch();
                bSafe.setBatchCode("LOT-202611-SAFE01");
                bSafe.setProductId(firstP.getId());
                bSafe.setProductSku(firstP.getSku());
                bSafe.setProductName(firstP.getName());
                bSafe.setWarehouseId(firstWh.getId());
                bSafe.setWarehouseName(firstWh.getName());
                bSafe.setManufacturingDate(today.minusDays(30));
                bSafe.setExpiryDate(today.plusDays(150)); // > 90 ngày: Vùng Xanh
                bSafe.setInitialQuantity(50);
                bSafe.setRemainingQuantity(50);
                bSafe.setCostPrice(firstP.getMovingAvgCost());
                bSafe.setSupplierName("Công ty Dệt May Sài Gòn");
                bSafe.setNotes("Lô hàng nhập mới chất lượng cao");
                bSafe.refreshStatus();

                ProductBatch bWarn = new ProductBatch();
                bWarn.setBatchCode("LOT-202611-WARN02");
                bWarn.setProductId(firstP.getId());
                bWarn.setProductSku(firstP.getSku());
                bWarn.setProductName(firstP.getName());
                bWarn.setWarehouseId(firstWh.getId());
                bWarn.setWarehouseName(firstWh.getName());
                bWarn.setManufacturingDate(today.minusDays(120));
                bWarn.setExpiryDate(today.plusDays(45)); // 30-90 ngày: Vùng Vàng (Cần lưu ý)
                bWarn.setInitialQuantity(30);
                bWarn.setRemainingQuantity(25);
                bWarn.setCostPrice(firstP.getMovingAvgCost());
                bWarn.setSupplierName("Xưởng May Tân Bình");
                bWarn.setNotes("Lô hàng cần đẩy mạnh bán khuyến mại");
                bWarn.refreshStatus();

                ProductBatch bDanger = new ProductBatch();
                bDanger.setBatchCode("LOT-202611-DANGER03");
                bDanger.setProductId(firstP.getId());
                bDanger.setProductSku(firstP.getSku());
                bDanger.setProductName(firstP.getName());
                bDanger.setWarehouseId(firstWh.getId());
                bDanger.setWarehouseName(firstWh.getName());
                bDanger.setManufacturingDate(today.minusDays(170));
                bDanger.setExpiryDate(today.plusDays(12)); // < 30 ngày: Vùng Đỏ (Nguy cấp)
                bDanger.setInitialQuantity(20);
                bDanger.setRemainingQuantity(15);
                bDanger.setCostPrice(firstP.getMovingAvgCost());
                bDanger.setSupplierName("Xưởng May Tân Bình");
                bDanger.setNotes("Lô hàng cận date dưới 15 ngày, xả hàng gấp");
                bDanger.refreshStatus();

                ProductBatch bExpired = new ProductBatch();
                bExpired.setBatchCode("LOT-202610-EXP04");
                bExpired.setProductId(firstP.getId());
                bExpired.setProductSku(firstP.getSku());
                bExpired.setProductName(firstP.getName());
                bExpired.setWarehouseId(firstWh.getId());
                bExpired.setWarehouseName(firstWh.getName());
                bExpired.setManufacturingDate(today.minusDays(200));
                bExpired.setExpiryDate(today.minusDays(5)); // Đã quá hạn
                bExpired.setInitialQuantity(10);
                bExpired.setRemainingQuantity(5);
                bExpired.setCostPrice(firstP.getMovingAvgCost());
                bExpired.setSupplierName("Đối tác Nhập khẩu");
                bExpired.setNotes("Hàng hết hạn, cách ly chuyển vào kho hủy");
                bExpired.refreshStatus();

                productBatchRepository.saveAll(java.util.List.of(bSafe, bWarn, bDanger, bExpired));
                log.info("Initialized 4 sample Product Batches across 3 Risk Zones for Expiry Radar");
            }

            // 2. Khởi tạo Định danh Serial / IMEI mẫu & Bảo hành điện tử
            if (productSerialRepository.count() == 0) {
                java.time.LocalDate today = java.time.LocalDate.now();

                ProductSerial s1 = new ProductSerial();
                s1.setSerialNumber("SN-IP15P-VN001");
                s1.setProductId(firstP.getId());
                s1.setProductSku(firstP.getSku());
                s1.setProductName(firstP.getName());
                s1.setWarehouseId(firstWh.getId());
                s1.setWarehouseName(firstWh.getName());
                s1.setStatus(SerialStatus.IN_STOCK);
                s1.setWarrantyMonths(12);
                s1.setNotes("Mã Serial mới nhập kho");

                ProductSerial s2 = new ProductSerial();
                s2.setSerialNumber("SN-IP15P-VN002");
                s2.setProductId(firstP.getId());
                s2.setProductSku(firstP.getSku());
                s2.setProductName(firstP.getName());
                s2.setWarehouseId(firstWh.getId());
                s2.setWarehouseName(firstWh.getName());
                s2.setStatus(SerialStatus.IN_STOCK);
                s2.setWarrantyMonths(12);

                ProductSerial s3 = new ProductSerial();
                s3.setSerialNumber("SN-IP15P-VN003");
                s3.setProductId(firstP.getId());
                s3.setProductSku(firstP.getSku());
                s3.setProductName(firstP.getName());
                s3.setWarehouseId(firstWh.getId());
                s3.setWarehouseName(firstWh.getName());
                s3.setStatus(SerialStatus.ACTIVE_WARRANTY);
                s3.setCustomerId(1L);
                s3.setCustomerName("Nguyễn Văn An");
                s3.setCustomerPhone("0909888777");
                s3.setOrderId(1L);
                s3.setOrderCode("ORD-20261120-001");
                s3.setWarrantyStartDate(today.minusMonths(2));
                s3.setWarrantyMonths(12);
                s3.setWarrantyEndDate(today.minusMonths(2).plusMonths(12));
                s3.setNotes("Kích hoạt bảo hành điện tử chính hãng");

                ProductSerial s4 = new ProductSerial();
                s4.setSerialNumber("SN-IP15P-VN004");
                s4.setProductId(firstP.getId());
                s4.setProductSku(firstP.getSku());
                s4.setProductName(firstP.getName());
                s4.setWarehouseId(firstWh.getId());
                s4.setWarehouseName(firstWh.getName());
                s4.setStatus(SerialStatus.UNDER_REPAIR);
                s4.setCustomerId(2L);
                s4.setCustomerName("Trần Thị Bích");
                s4.setCustomerPhone("0912345678");
                s4.setWarrantyStartDate(today.minusMonths(5));
                s4.setWarrantyMonths(12);
                s4.setWarrantyEndDate(today.minusMonths(5).plusMonths(12));
                s4.setNotes("Tiếp nhận sửa chữa bảo hành lỗi nguồn");

                productSerialRepository.saveAll(java.util.List.of(s1, s2, s3, s4));
                log.info("Initialized 4 sample Product Serials with warranty tracking");
            }

            // 3. Khởi tạo Phiếu Kiểm Kê Kho mẫu (Stocktake Audit)
            if (stocktakeAuditRepository.count() == 0) {
                StocktakeAudit audit = new StocktakeAudit();
                audit.setAuditCode("AUDIT-20261125-001");
                audit.setTitle("Kiểm kê định kỳ Kho Tổng Miền Bắc Tháng 11");
                audit.setWarehouseId(firstWh.getId());
                audit.setWarehouseName(firstWh.getName());
                audit.setAuditType("PERIODIC");
                audit.setStatus(StocktakeStatus.IN_PROGRESS);
                audit.setCreatedBy("admin");
                audit.setStartedAt(java.time.LocalDateTime.now().minusHours(3));
                audit.setNotes("Kiểm kê đợt 1 các mặt hàng bán chạy chuẩn bị quyết toán quý");

                StocktakeItem item1 = new StocktakeItem();
                item1.setProductId(firstP.getId());
                item1.setProductSku(firstP.getSku());
                item1.setProductName(firstP.getName());
                item1.setSystemStock(120);
                item1.setCountedStock(118); // Thiếu 2 cái
                item1.calculateDifference();
                item1.setReason("Hao hụt rách 2 bao bì trong quá trình bốc xếp");
                audit.addItem(item1);

                if (allProds.size() > 1) {
                    var secondP = allProds.get(1);
                    StocktakeItem item2 = new StocktakeItem();
                    item2.setProductId(secondP.getId());
                    item2.setProductSku(secondP.getSku());
                    item2.setProductName(secondP.getName());
                    item2.setSystemStock(80);
                    item2.setCountedStock(81); // Thừa 1 cái
                    item2.calculateDifference();
                    item2.setReason("Nhà cung cấp giao dư 1 cái khuyến mại");
                    audit.addItem(item2);
                }

                audit.setTotalItems(audit.getItems().size());
                audit.setTotalDiscrepancyQty(3);

                stocktakeAuditRepository.save(audit);
                log.info("Initialized sample Stocktake Audit in progress: AUDIT-20261125-001");
            }
        }
    }

    private void initWorkflowAndApprovalData() {
        if (approvalMatrixRuleRepository.count() == 0) {
            // 1. Quy tắc PO dưới 20 triệu: Tự động phê duyệt
            ApprovalMatrixRule rule1 = ApprovalMatrixRule.builder()
                    .ruleName("PO Dưới 20 Triệu - Tự động duyệt")
                    .documentType(ApprovalDocumentType.PURCHASE_ORDER)
                    .minAmount(java.math.BigDecimal.ZERO)
                    .maxAmount(new java.math.BigDecimal("20000000.00"))
                    .level(1)
                    .approverRole("ROLE_MANAGER")
                    .autoApprove(true)
                    .priority(1)
                    .active(true)
                    .build();

            // 2. Quy tắc PO từ 20 đến 100 triệu: Cấp 1 Trưởng phòng, Cấp 2 Kế toán trưởng
            ApprovalMatrixRule rule2a = ApprovalMatrixRule.builder()
                    .ruleName("PO 20Tr - 100Tr [Cấp 1: Trưởng phòng Mua hàng]")
                    .documentType(ApprovalDocumentType.PURCHASE_ORDER)
                    .minAmount(new java.math.BigDecimal("20000000.01"))
                    .maxAmount(new java.math.BigDecimal("100000000.00"))
                    .level(1)
                    .approverRole("ROLE_MANAGER")
                    .autoApprove(false)
                    .priority(2)
                    .active(true)
                    .build();

            ApprovalMatrixRule rule2b = ApprovalMatrixRule.builder()
                    .ruleName("PO 20Tr - 100Tr [Cấp 2: Kế toán trưởng]")
                    .documentType(ApprovalDocumentType.PURCHASE_ORDER)
                    .minAmount(new java.math.BigDecimal("20000000.01"))
                    .maxAmount(new java.math.BigDecimal("100000000.00"))
                    .level(2)
                    .approverRole("ROLE_ACCOUNTANT")
                    .autoApprove(false)
                    .priority(2)
                    .active(true)
                    .build();

            // 3. Quy tắc PO trên 100 triệu: Cấp 1 Trưởng phòng, Cấp 2 Kế toán trưởng, Cấp 3 Tổng Giám đốc (CEO)
            ApprovalMatrixRule rule3a = ApprovalMatrixRule.builder()
                    .ruleName("PO Trên 100Tr [Cấp 1: Trưởng phòng]")
                    .documentType(ApprovalDocumentType.PURCHASE_ORDER)
                    .minAmount(new java.math.BigDecimal("100000000.01"))
                    .maxAmount(null)
                    .level(1)
                    .approverRole("ROLE_MANAGER")
                    .autoApprove(false)
                    .priority(3)
                    .active(true)
                    .build();

            ApprovalMatrixRule rule3b = ApprovalMatrixRule.builder()
                    .ruleName("PO Trên 100Tr [Cấp 2: Kế toán trưởng]")
                    .documentType(ApprovalDocumentType.PURCHASE_ORDER)
                    .minAmount(new java.math.BigDecimal("100000000.01"))
                    .maxAmount(null)
                    .level(2)
                    .approverRole("ROLE_ACCOUNTANT")
                    .autoApprove(false)
                    .priority(3)
                    .active(true)
                    .build();

            ApprovalMatrixRule rule3c = ApprovalMatrixRule.builder()
                    .ruleName("PO Trên 100Tr [Cấp 3: Tổng Giám đốc / CEO]")
                    .documentType(ApprovalDocumentType.PURCHASE_ORDER)
                    .minAmount(new java.math.BigDecimal("100000000.01"))
                    .maxAmount(null)
                    .level(3)
                    .approverRole("ROLE_ADMIN")
                    .autoApprove(false)
                    .priority(3)
                    .active(true)
                    .build();

            // 4. Quy tắc Phiếu chi Quỹ trên 50 triệu: Cấp 1 Kế toán trưởng
            ApprovalMatrixRule rule4 = ApprovalMatrixRule.builder()
                    .ruleName("Phiếu chi Quỹ > 50Tr [Kế toán trưởng duyệt]")
                    .documentType(ApprovalDocumentType.PAYMENT_VOUCHER)
                    .minAmount(new java.math.BigDecimal("50000000.00"))
                    .maxAmount(null)
                    .level(1)
                    .approverRole("ROLE_ACCOUNTANT")
                    .autoApprove(false)
                    .priority(1)
                    .active(true)
                    .build();

            // 5. Quy tắc Chiết khấu POS đặc biệt > 500k: Giám đốc duyệt
            ApprovalMatrixRule rule5 = ApprovalMatrixRule.builder()
                    .ruleName("Chiết khấu POS > 500k [Giám đốc bán hàng duyệt]")
                    .documentType(ApprovalDocumentType.SPECIAL_DISCOUNT)
                    .minAmount(new java.math.BigDecimal("500000.00"))
                    .maxAmount(null)
                    .level(1)
                    .approverRole("ROLE_MANAGER")
                    .autoApprove(false)
                    .priority(1)
                    .active(true)
                    .build();

            approvalMatrixRuleRepository.saveAll(java.util.List.of(rule1, rule2a, rule2b, rule3a, rule3b, rule3c, rule4, rule5));
            log.info("Initialized default 8 Approval Matrix Rules for PO, Payment Voucher and Special Discount");
        }

        if (approvalRequestRepository.count() == 0) {
            // Mẫu 1: Đang chờ duyệt cấp 1 (PO mua hàng 45.000.000đ)
            ApprovalRequest req1 = ApprovalRequest.builder()
                    .requestCode("APR-20261125-00001")
                    .documentType(ApprovalDocumentType.PURCHASE_ORDER)
                    .documentId(1L)
                    .documentCode("PO-20261122-001")
                    .title("Phê duyệt Đơn mua hàng PO-20261122-001 phụ kiện thiết bị công nghệ")
                    .amount(new java.math.BigDecimal("45000000.00"))
                    .currency("VND")
                    .currentLevel(1)
                    .totalLevels(2)
                    .status(ApprovalStatus.PENDING)
                    .currentApproverRole("ROLE_MANAGER")
                    .requesterUsername("nhanvien")
                    .requesterFullName("Nguyễn Văn A (Kinh doanh)")
                    .oneTimeToken(UUID.randomUUID().toString().replace("-", ""))
                    .tokenExpiresAt(java.time.LocalDateTime.now().plusHours(24))
                    .payloadSnapshot("{\"supplier\":\"Công ty Công Nghệ Á Châu\",\"items\":2,\"amount\":45000000}")
                    .build();

            ApprovalStepHistory step0 = ApprovalStepHistory.builder()
                    .stepLevel(0)
                    .approverUsername("nhanvien")
                    .approverFullName("Nguyễn Văn A (Kinh doanh)")
                    .approverRole("ROLE_SALES")
                    .action("SUBMIT")
                    .comment("Trình duyệt đơn mua hàng định kỳ tháng 11 theo kế hoạch kinh doanh")
                    .build();
            req1.addStepHistory(step0);

            // Mẫu 2: Đã phê duyệt hoàn tất (Phiếu chi tiền thuê mặt bằng 15.000.000đ)
            ApprovalRequest req2 = ApprovalRequest.builder()
                    .requestCode("APR-20261124-00002")
                    .documentType(ApprovalDocumentType.PAYMENT_VOUCHER)
                    .documentId(1L)
                    .documentCode("PV-20261118-001")
                    .title("Phê duyệt Phiếu chi tiền thuê mặt bằng văn phòng Tháng 11")
                    .amount(new java.math.BigDecimal("15000000.00"))
                    .currency("VND")
                    .currentLevel(1)
                    .totalLevels(1)
                    .status(ApprovalStatus.APPROVED)
                    .currentApproverRole("ROLE_ACCOUNTANT")
                    .requesterUsername("ketoan")
                    .requesterFullName("Lê Thị Thu (Kế toán)")
                    .decisionReason("Đã kiểm tra hợp đồng thuê mặt bằng Keangnam hợp lệ")
                    .build();

            ApprovalStepHistory step2_0 = ApprovalStepHistory.builder()
                    .stepLevel(0)
                    .approverUsername("ketoan")
                    .approverFullName("Lê Thị Thu (Kế toán)")
                    .approverRole("ROLE_ACCOUNTANT")
                    .action("SUBMIT")
                    .comment("Trình duyệt thanh toán tiền mặt bằng văn phòng theo hợp đồng")
                    .build();
            ApprovalStepHistory step2_1 = ApprovalStepHistory.builder()
                    .stepLevel(1)
                    .approverUsername("admin")
                    .approverFullName("Quản trị viên tối cao")
                    .approverRole("ROLE_ADMIN")
                    .action("APPROVE")
                    .comment("Đã duyệt chuyển khoản ngân hàng VCB")
                    .build();
            req2.addStepHistory(step2_0);
            req2.addStepHistory(step2_1);

            approvalRequestRepository.saveAll(java.util.List.of(req1, req2));
            log.info("Initialized default 2 sample Approval Requests (1 PENDING, 1 APPROVED)");

            // Mẫu Nhật ký kiểm toán bất biến
            ImmutableAuditLog log1 = ImmutableAuditLog.builder()
                    .traceId("TRC-INIT-001")
                    .username("admin")
                    .userRole("ROLE_ADMIN")
                    .action("APPROVE")
                    .entityType("PAYMENT_VOUCHER")
                    .entityId(1L)
                    .entityCode("PV-20261118-001")
                    .oldValueJson("{\"status\":\"PENDING\"}")
                    .newValueJson("{\"status\":\"APPROVED\"}")
                    .ipAddress("127.0.0.1")
                    .userAgent("ERP Cloud Enterprise Desktop")
                    .reason("Đã duyệt thanh toán tiền mặt bằng văn phòng")
                    .build();

            ImmutableAuditLog log2 = ImmutableAuditLog.builder()
                    .traceId("TRC-INIT-002")
                    .username("nhanvien")
                    .userRole("ROLE_SALES")
                    .action("SUBMIT")
                    .entityType("PURCHASE_ORDER")
                    .entityId(1L)
                    .entityCode("PO-20261122-001")
                    .oldValueJson(null)
                    .newValueJson("{\"status\":\"PENDING_APPROVAL\",\"amount\":45000000}")
                    .ipAddress("127.0.0.1")
                    .userAgent("ERP Cloud Enterprise Desktop")
                    .reason("Trình duyệt PO phụ kiện công nghệ")
                    .build();

            immutableAuditLogRepository.saveAll(java.util.List.of(log1, log2));
            log.info("Initialized default 2 Immutable Audit Logs");
        }
    }
}
