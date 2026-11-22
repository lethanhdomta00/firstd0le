package com.erp.procurement;

import com.erp.finance.model.TreasuryAccount;
import com.erp.finance.model.TreasuryAccountType;
import com.erp.finance.repository.TreasuryAccountRepository;
import com.erp.order.model.InventoryLog;
import com.erp.order.model.InventoryMovementType;
import com.erp.order.repository.InventoryLogRepository;
import com.erp.model.Product;
import com.erp.procurement.dto.*;
import com.erp.procurement.model.PurchaseOrder;
import com.erp.procurement.model.PurchaseOrderStatus;
import com.erp.procurement.model.Supplier;
import com.erp.procurement.repository.GoodsReceiptNoteRepository;
import com.erp.procurement.repository.PurchaseOrderRepository;
import com.erp.procurement.repository.SupplierRepository;
import com.erp.procurement.service.GoodsReceiptService;
import com.erp.procurement.service.ProcurementPaymentService;
import com.erp.procurement.service.PurchaseOrderService;
import com.erp.procurement.service.SupplierService;
import com.erp.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class ProcurementServiceTest {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private GoodsReceiptService goodsReceiptService;

    @Autowired
    private ProcurementPaymentService procurementPaymentService;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryLogRepository inventoryLogRepository;

    @Autowired
    private TreasuryAccountRepository treasuryAccountRepository;

    @Test
    @DisplayName("1. Quản lý Nhà cung cấp (Tạo mới, Tìm kiếm & Cập nhật)")
    void testCreateAndSearchSupplier() {
        CreateSupplierRequest req = new CreateSupplierRequest();
        req.setCode("NCC-TEST-01");
        req.setName("Xưởng Gia Công May Mặc Hà Nội");
        req.setPhone("0977112233");
        req.setContactPerson("Anh Tuấn Anh");
        req.setAddress("KCN Thăng Long, Đông Anh, Hà Nội");
        req.setCreditLimit(new BigDecimal("150000000.00"));
        req.setPaymentTermsDays(45);

        SupplierDto created = supplierService.createSupplier(req);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getCode()).isEqualTo("NCC-TEST-01");
        assertThat(created.getName()).isEqualTo("Xưởng Gia Công May Mặc Hà Nội");
        assertThat(created.getCurrentDebt()).isEqualByComparingTo(BigDecimal.ZERO);

        Page<SupplierDto> page = supplierService.searchSuppliers("Hà Nội", "ACTIVE", PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().get(0).getName()).contains("Hà Nội");
    }

    @Test
    @DisplayName("2. Tạo Đơn đặt mua hàng (PO), tính tổng tiền & Phê duyệt đơn")
    void testCreateAndApprovePurchaseOrder() {
        // Tạo sản phẩm mẫu
        Product product = new Product();
        product.setName("Áo Sơ Mi Nam Công Sở Cao Cấp");
        product.setSku("SOMI-NAM-TEST-" + System.currentTimeMillis());
        product.setPrice(new BigDecimal("350000.00"));
        product.setMovingAvgCost(new BigDecimal("150000.00"));
        product.setPhysicalStock(20);
        product.setStock(20);
        Product savedProduct = productRepository.save(product);

        // Tạo NCC
        CreateSupplierRequest sReq = new CreateSupplierRequest();
        sReq.setName("Xưởng Dệt May Đại Việt");
        sReq.setPhone("0933445566");
        SupplierDto sup = supplierService.createSupplier(sReq);

        // Tạo PO
        CreatePurchaseOrderRequest poReq = new CreatePurchaseOrderRequest();
        poReq.setSupplierId(sup.getId());
        poReq.setOrderDate(LocalDate.now());
        poReq.setDiscountAmount(new BigDecimal("200000.00"));
        poReq.setTaxAmount(new BigDecimal("500000.00"));
        poReq.setNotes("Đơn hàng đặt thử nghiệm");

        CreatePurchaseOrderRequest.ItemRequest itemReq = new CreatePurchaseOrderRequest.ItemRequest();
        itemReq.setProductId(savedProduct.getId());
        itemReq.setOrderedQuantity(100);
        itemReq.setUnitPrice(new BigDecimal("160000.00"));
        poReq.setItems(List.of(itemReq));

        PurchaseOrderDto poDto = purchaseOrderService.createPurchaseOrder(poReq, "admin");
        assertThat(poDto.getId()).isNotNull();
        assertThat(poDto.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        // Total = 100 * 160.000 = 16.000.000
        assertThat(poDto.getTotalAmount()).isEqualByComparingTo(new BigDecimal("16000000.00"));
        // Final = 16.000.000 - 200.000 + 500.000 = 16.300.000
        assertThat(poDto.getFinalAmount()).isEqualByComparingTo(new BigDecimal("16300000.00"));

        // Gửi duyệt & Phê duyệt
        PurchaseOrderDto submitted = purchaseOrderService.submitForApproval(poDto.getId(), "admin");
        assertThat(submitted.getStatus()).isEqualTo(PurchaseOrderStatus.PENDING_APPROVAL);

        PurchaseOrderDto approved = purchaseOrderService.approvePurchaseOrder(poDto.getId(), "giamdoc");
        assertThat(approved.getStatus()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(approved.getApproverUsername()).isEqualTo("giamdoc");
    }

    @Test
    @DisplayName("3. Thuật toán Giá vốn Bình quân Gia quyền (Moving Weighted Average Cost) & Tăng công nợ NCC")
    void testMovingWeightedAverageCostAlgorithm() {
        // Tình huống: Tồn cũ = 50 cái, giá vốn cũ = 100.000đ.
        // Nhập mới = 50 cái, giá nhập mới = 120.000đ.
        // Kỳ vọng: Giá vốn mới = (50*100.000 + 50*120.000) / 100 = 110.000đ.
        // Tồn mới = 100 cái.
        Product product = new Product();
        product.setName("Áo Thun Cổ Tròn Basic");
        product.setSku("AT-BASIC-TEST-" + System.currentTimeMillis());
        product.setPrice(new BigDecimal("200000.00"));
        product.setMovingAvgCost(new BigDecimal("100000.00"));
        product.setPhysicalStock(50);
        product.setStock(50);
        Product savedProduct = productRepository.save(product);

        CreateSupplierRequest sReq = new CreateSupplierRequest();
        sReq.setName("Xưởng May Vải Cotton 100%");
        sReq.setPhone("0918889999");
        SupplierDto sup = supplierService.createSupplier(sReq);

        CreatePurchaseOrderRequest poReq = new CreatePurchaseOrderRequest();
        poReq.setSupplierId(sup.getId());
        poReq.setOrderDate(LocalDate.now());

        CreatePurchaseOrderRequest.ItemRequest itemReq = new CreatePurchaseOrderRequest.ItemRequest();
        itemReq.setProductId(savedProduct.getId());
        itemReq.setOrderedQuantity(50);
        itemReq.setUnitPrice(new BigDecimal("120000.00"));
        poReq.setItems(List.of(itemReq));

        PurchaseOrderDto poDto = purchaseOrderService.createPurchaseOrder(poReq, "admin");
        purchaseOrderService.approvePurchaseOrder(poDto.getId(), "admin");

        // Lấy chi tiết đơn PO đã duyệt
        PurchaseOrderDto poApproved = purchaseOrderService.getPurchaseOrderById(poDto.getId());
        Long poiId = poApproved.getItems().get(0).getId();

        // Tiến hành lập Phiếu Nhập Kho GRN
        CreateGoodsReceiptRequest grnReq = new CreateGoodsReceiptRequest();
        grnReq.setPurchaseOrderId(poDto.getId());
        grnReq.setWarehouseLocation("Kho Trung Tâm");
        grnReq.setNotes("Kiểm nhận 100% hàng đạt chuẩn");

        CreateGoodsReceiptRequest.ReceiptItemRequest rItem = new CreateGoodsReceiptRequest.ReceiptItemRequest();
        rItem.setPurchaseOrderItemId(poiId);
        rItem.setQuantityReceived(50);
        rItem.setImportUnitPrice(new BigDecimal("120000.00"));
        grnReq.setItems(List.of(rItem));

        GoodsReceiptNoteDto grn = goodsReceiptService.createGoodsReceipt(grnReq, "thukho");
        assertThat(grn.getId()).isNotNull();
        assertThat(grn.getTotalReceiptAmount()).isEqualByComparingTo(new BigDecimal("6000000.00"));

        // Kiểm tra sản phẩm sau khi tính giá vốn bình quân gia quyền
        Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getPhysicalStock()).isEqualTo(100);
        assertThat(updatedProduct.getMovingAvgCost()).isEqualByComparingTo(new BigDecimal("110000.00"));

        // Kiểm tra nhật ký kho InventoryLog
        List<InventoryLog> logs = inventoryLogRepository.findByProductIdOrderByCreatedAtDesc(savedProduct.getId());
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getMovementType()).isEqualTo(InventoryMovementType.IMPORT_PURCHASE);
        assertThat(logs.get(0).getQuantityDelta()).isEqualTo(50);
        assertThat(logs.get(0).getStockBefore()).isEqualTo(50);
        assertThat(logs.get(0).getStockAfter()).isEqualTo(100);

        // Kiểm tra công nợ NCC tăng thêm 6.000.000đ
        Supplier updatedSup = supplierRepository.findById(sup.getId()).orElseThrow();
        assertThat(updatedSup.getCurrentDebt()).isEqualByComparingTo(new BigDecimal("6000000.00"));

        // Kiểm tra trạng thái đơn PO đã sang COMPLETED
        PurchaseOrder updatedPo = purchaseOrderRepository.findById(poDto.getId()).orElseThrow();
        assertThat(updatedPo.getStatus()).isEqualTo(PurchaseOrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("4. Thanh toán công nợ Nhà cung cấp kết nối Sổ quỹ Tài chính")
    void testPaySupplierDebt() {
        // Tạo tài khoản quỹ
        TreasuryAccount bank = treasuryAccountRepository.findByAccountCode("BANK_VCB")
                .orElseGet(() -> {
                    TreasuryAccount acc = TreasuryAccount.builder()
                            .accountCode("BANK_VCB")
                            .accountName("Vietcombank Hội sở")
                            .accountType(TreasuryAccountType.BANK)
                            .currentBalance(new BigDecimal("100000000.00"))
                            .active(true)
                            .build();
                    return treasuryAccountRepository.save(acc);
                });

        BigDecimal initialBalance = bank.getCurrentBalance();

        // Tạo NCC có nợ 20 triệu
        Supplier sup = Supplier.builder()
                .code("NCC-DEBT-TEST")
                .name("Nhà Cung Cấp Vải Cotton")
                .phone("0988001122")
                .currentDebt(new BigDecimal("20000000.00"))
                .build();
        Supplier savedSup = supplierRepository.save(sup);

        // Thực hiện thanh toán 15 triệu
        PaySupplierDebtRequest payReq = new PaySupplierDebtRequest();
        payReq.setSupplierId(savedSup.getId());
        payReq.setAmount(new BigDecimal("15000000.00"));
        payReq.setSourceAccountCode("BANK_VCB");
        payReq.setNotes("Ủy nhiệm chi trả tiền vải đợt 1");

        Map<String, Object> res = procurementPaymentService.paySupplierDebt(payReq, "ketoan");
        assertThat(res.get("success")).isEqualTo(true);
        assertThat(res.get("voucherCode")).toString().startsWith("PV-PURCHASE-");

        // Kiểm tra công nợ NCC còn lại 5 triệu
        Supplier afterPaySup = supplierRepository.findById(savedSup.getId()).orElseThrow();
        assertThat(afterPaySup.getCurrentDebt()).isEqualByComparingTo(new BigDecimal("5000000.00"));

        // Kiểm tra số dư quỹ giảm đúng 15 triệu
        TreasuryAccount afterPayBank = treasuryAccountRepository.findByAccountCode("BANK_VCB").orElseThrow();
        assertThat(afterPayBank.getCurrentBalance()).isEqualByComparingTo(initialBalance.subtract(new BigDecimal("15000000.00")));
    }
}
