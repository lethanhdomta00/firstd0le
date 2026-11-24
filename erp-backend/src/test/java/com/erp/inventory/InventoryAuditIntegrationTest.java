package com.erp.inventory;

import com.erp.branch.model.Branch;
import com.erp.branch.model.Warehouse;
import com.erp.branch.model.WarehouseStock;
import com.erp.branch.model.WarehouseType;
import com.erp.branch.repository.BranchRepository;
import com.erp.branch.repository.WarehouseRepository;
import com.erp.branch.repository.WarehouseStockRepository;
import com.erp.inventory.model.*;
import com.erp.inventory.repository.ProductBatchRepository;
import com.erp.inventory.repository.ProductSerialRepository;
import com.erp.inventory.repository.StocktakeAuditRepository;
import com.erp.inventory.repository.StocktakeItemRepository;
import com.erp.model.Product;
import com.erp.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class InventoryAuditIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private WarehouseStockRepository warehouseStockRepository;

    @Autowired
    private ProductBatchRepository productBatchRepository;

    @Autowired
    private ProductSerialRepository productSerialRepository;

    @Autowired
    private StocktakeAuditRepository stocktakeAuditRepository;

    @Autowired
    private StocktakeItemRepository stocktakeItemRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private Product testProduct;
    private Warehouse testWarehouse;

    @BeforeEach
    void setUp() {
        Branch branch = new Branch();
        branch.setCode("BR-TEST-INV-" + System.currentTimeMillis());
        branch.setName("Chi nhánh Test Inventory");
        branch.setAddress("Hà Nội");
        branch.setPhone("0988776655");
        branch.setIsHeadquarter(true);
        branch = branchRepository.save(branch);

        Warehouse warehouse = new Warehouse();
        warehouse.setCode("WH-TEST-INV-" + System.currentTimeMillis());
        warehouse.setName("Kho Tổng Test Inventory");
        warehouse.setWarehouseType(WarehouseType.CENTRAL_HUB);
        warehouse.setBranchId(branch.getId());
        warehouse.setBranchName(branch.getName());
        warehouse.setActive(true);
        testWarehouse = warehouseRepository.save(warehouse);

        Product product = new Product();
        product.setName("Sản phẩm Test Lô & Serial " + System.currentTimeMillis());
        product.setSku("SKU-INV-" + System.currentTimeMillis());
        product.setPrice(new BigDecimal("1500000"));
        product.setStock(100);
        product.setPhysicalStock(100);
        testProduct = productRepository.save(product);

        WarehouseStock ws = new WarehouseStock();
        ws.setWarehouseId(testWarehouse.getId());
        ws.setWarehouseName(testWarehouse.getName());
        ws.setBranchId(branch.getId());
        ws.setProductId(testProduct.getId());
        ws.setProductSku(testProduct.getSku());
        ws.setProductName(testProduct.getName());
        ws.setPhysicalStock(100);
        ws.setAvailableStock(100);
        ws.setReservedStock(0);
        warehouseStockRepository.save(ws);
    }

    @Test
    @DisplayName("Test tạo Lô hàng, Radar cảnh báo hạn dùng và thuật toán phân bổ FEFO")
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "PRODUCT_VIEW", "PRODUCT_STOCK", "ORDER_CREATE", "ANALYTICS_VIEW"})
    void testProductBatchAndFefoAllocation() throws Exception {
        LocalDate now = LocalDate.now();

        // 1. Tạo Lô hàng A (Hạn dùng sau 15 ngày -> Vùng Đỏ cận date <= 30 ngày)
        String batchAPayload = String.format("""
                {
                  "batchNumber": "BATCH-TEST-A-%d",
                  "productId": %d,
                  "warehouseId": %d,
                  "initialQuantity": 40,
                  "manufacturingDate": "%s",
                  "expiryDate": "%s",
                  "supplierName": "Dược phẩm Hà Tây",
                  "notes": "Lô hàng A cận date test"
                }
                """, System.currentTimeMillis(), testProduct.getId(), testWarehouse.getId(),
                now.minusDays(60), now.plusDays(15));

        mockMvc().perform(post("/api/inventory/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchAPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingQuantity").value(40));

        // 2. Tạo Lô hàng B (Hạn dùng sau 180 ngày -> Vùng Xanh an toàn > 90 ngày)
        String batchBPayload = String.format("""
                {
                  "batchNumber": "BATCH-TEST-B-%d",
                  "productId": %d,
                  "warehouseId": %d,
                  "initialQuantity": 60,
                  "manufacturingDate": "%s",
                  "expiryDate": "%s",
                  "supplierName": "Dược phẩm Trung Ương 1",
                  "notes": "Lô hàng B date xa test"
                }
                """, System.currentTimeMillis() + 1, testProduct.getId(), testWarehouse.getId(),
                now.minusDays(10), now.plusDays(180));

        mockMvc().perform(post("/api/inventory/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchBPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingQuantity").value(60));

        // 3. Tra cứu danh sách lô theo sản phẩm
        mockMvc().perform(get("/api/inventory/batches")
                        .param("productId", testProduct.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));

        // 4. Test Thuật toán gợi ý xuất kho FEFO (Yêu cầu xuất 50 chiếc -> Lô A 40 chiếc, Lô B 10 chiếc)
        mockMvc().perform(get("/api/inventory/batches/allocate")
                        .param("productId", testProduct.getId().toString())
                        .param("quantity", "50")
                        .param("strategy", "FEFO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocatedQuantity").value(50))
                .andExpect(jsonPath("$.allocations", hasSize(2)))
                .andExpect(jsonPath("$.allocations[0].quantityToPick").value(40))
                .andExpect(jsonPath("$.allocations[1].quantityToPick").value(10));

        // 5. Test Radar Cảnh báo Hạn dùng (Summary & Radar groups)
        mockMvc().perform(get("/api/inventory/batches/radar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalActiveBatches", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.redZone", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Test định danh Serial/IMEI, vòng đời và bảo hành điện tử")
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "PRODUCT_VIEW", "PRODUCT_STOCK", "ORDER_STATUS", "ORDER_VIEW"})
    void testProductSerialTrackingAndWarranty() throws Exception {
        String serialNumber = "IMEI-TEST-" + System.currentTimeMillis();

        // 1. Đăng ký Serial mới vào kho
        String createSerialPayload = String.format("""
                {
                  "serialNumber": "%s",
                  "productId": %d,
                  "warehouseId": %d,
                  "warrantyMonths": 24,
                  "notes": "Thiết bị mẫu kiểm thử serial"
                }
                """, serialNumber, testProduct.getId(), testWarehouse.getId());

        MvcResult createRes = mockMvc().perform(post("/api/inventory/serials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSerialPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serialNumber").value(serialNumber))
                .andExpect(jsonPath("$.status").value("IN_STOCK"))
                .andExpect(jsonPath("$.warrantyMonths").value(24))
                .andReturn();

        JsonNode serialJson = objectMapper.readTree(createRes.getResponse().getContentAsString());
        long serialId = serialJson.get("id").asLong();

        // 2. Tra cứu Serial theo mã (lookup by-number)
        mockMvc().perform(get("/api/inventory/serials/by-number/" + serialNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serialId))
                .andExpect(jsonPath("$.productName").value(testProduct.getName()));

        // 3. Cập nhật trạng thái: Xuất bán và Kích hoạt bảo hành điện tử
        LocalDate soldDate = LocalDate.now();
        String updateStatusPayload = String.format("""
                {
                  "newStatus": "DELIVERED",
                  "orderCode": "ORD-TEST-9999",
                  "customerName": "Nguyễn Văn Khách Hàng",
                  "customerPhone": "0912345678",
                  "warrantyStartDate": "%s",
                  "warrantyMonths": 24,
                  "notes": "Đã xuất bán kích hoạt bảo hành 2 năm"
                }
                """, soldDate);

        mockMvc().perform(patch("/api/inventory/serials/" + serialId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateStatusPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE_WARRANTY"))
                .andExpect(jsonPath("$.orderCode").value("ORD-TEST-9999"))
                .andExpect(jsonPath("$.customerName").value("Nguyễn Văn Khách Hàng"))
                .andExpect(jsonPath("$.warrantyEndDate").value(soldDate.plusMonths(24).toString()));
    }

    @Test
    @DisplayName("Test quy trình Kiểm kê kho: Tạo phiếu, ghi nhận số đếm và tự động cân đối lệch tồn")
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "PRODUCT_VIEW", "PRODUCT_STOCK", "BRANCH_MANAGE"})
    void testStocktakeAuditAndReconciliation() throws Exception {
        // 1. Tạo Phiếu kiểm kê kho
        String createStocktakePayload = String.format("""
                {
                  "warehouseId": %d,
                  "auditType": "PERIODIC",
                  "title": "Đợt kiểm kê định kỳ quý 4",
                  "notes": "Kiểm kê toàn bộ hàng hóa kho tổng",
                  "productIds": [%d]
                }
                """, testWarehouse.getId(), testProduct.getId());

        MvcResult stocktakeRes = mockMvc().perform(post("/api/inventory/stocktakes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createStocktakePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andReturn();

        JsonNode stocktakeJson = objectMapper.readTree(stocktakeRes.getResponse().getContentAsString());
        long stocktakeId = stocktakeJson.get("id").asLong();
        long itemId = stocktakeJson.get("items").get(0).get("id").asLong();
        int systemStock = stocktakeJson.get("items").get(0).get("systemStock").asInt();
        assertEquals(100, systemStock);

        // 2. Ghi nhận số lượng kiểm đếm thực tế (Thực tế đếm được 95 cái -> Thiếu 5 cái)
        String recordCountPayload = String.format("""
                {
                  "counts": [
                    {
                      "itemId": %d,
                      "productId": %d,
                      "countedStock": 95,
                      "reason": "Hao hụt vận chuyển và vỡ hỏng 5 cái"
                    }
                  ],
                  "notes": "Ghi nhận kiểm đếm thực tế"
                }
                """, itemId, testProduct.getId());

        mockMvc().perform(post("/api/inventory/stocktakes/" + stocktakeId + "/counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordCountPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].countedStock").value(95))
                .andExpect(jsonPath("$.items[0].differenceQty").value(-5));

        // 3. Cân đối và Hoàn tất đợt kiểm kê (Reconciliation)
        mockMvc().perform(post("/api/inventory/stocktakes/" + stocktakeId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BALANCED"));

        // 4. Xác nhận tồn kho thực tế của Kho và Sản phẩm đã được tự động điều chỉnh về 95
        WarehouseStock updatedWs = warehouseStockRepository
                .findByWarehouseIdAndProductId(testWarehouse.getId(), testProduct.getId())
                .orElseThrow();
        assertEquals(95, updatedWs.getPhysicalStock(), "Tồn kho trong kho phải được cân đối về 95");

        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(95, updatedProduct.getStock(), "Tồn kho tổng của sản phẩm phải được cập nhật về 95");
    }
}
