package com.erp.branch;

import com.erp.branch.model.Branch;
import com.erp.branch.model.Warehouse;
import com.erp.branch.model.WarehouseStock;
import com.erp.branch.model.WarehouseType;
import com.erp.branch.repository.BranchRepository;
import com.erp.branch.repository.WarehouseRepository;
import com.erp.branch.repository.WarehouseStockRepository;
import com.erp.model.Product;
import com.erp.repository.ProductRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class BranchWarehouseIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private WarehouseStockRepository warehouseStockRepository;

    @Autowired
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "BRANCH_MANAGE", "BRANCH_VIEW", "TRANSFER_MANAGE", "TRANSFER_VIEW"})
    void testBranchAndWarehouseCrud() throws Exception {
        // 1. Tạo Chi nhánh mới
        String branchPayload = """
                {
                  "code": "BR-TEST-01",
                  "name": "Chi nhánh Thử nghiệm Cầu Giấy",
                  "address": "Số 99 Cầu Giấy, Hà Nội",
                  "phone": "024-99998888",
                  "isHeadquarter": false
                }
                """;

        MvcResult branchRes = mockMvc().perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BR-TEST-01"))
                .andExpect(jsonPath("$.name").value("Chi nhánh Thử nghiệm Cầu Giấy"))
                .andReturn();

        JsonNode branchJson = objectMapper.readTree(branchRes.getResponse().getContentAsString());
        long branchId = branchJson.get("id").asLong();

        // 2. Cập nhật chi nhánh
        String updateBranchPayload = """
                {
                  "name": "Chi nhánh Cầu Giấy - Đã đổi tên",
                  "address": "Số 100 Cầu Giấy, Hà Nội",
                  "phone": "024-99998888",
                  "isHeadquarter": false
                }
                """;

        mockMvc().perform(put("/api/branches/" + branchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBranchPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chi nhánh Cầu Giấy - Đã đổi tên"));

        // 3. Toggle trạng thái chi nhánh
        mockMvc().perform(patch("/api/branches/" + branchId + "/toggle-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        // 4. Tạo Kho hàng mới thuộc chi nhánh
        String warehousePayload = """
                {
                  "branchId": %d,
                  "code": "WH-TEST-01",
                  "name": "Kho Bán Lẻ Test Cầu Giấy",
                  "warehouseType": "RETAIL_STORE",
                  "address": "Tầng hầm B1, 100 Cầu Giấy",
                  "active": true
                }
                """.formatted(branchId);

        MvcResult whRes = mockMvc().perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(warehousePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("WH-TEST-01"))
                .andExpect(jsonPath("$.name").value("Kho Bán Lẻ Test Cầu Giấy"))
                .andReturn();

        JsonNode whJson = objectMapper.readTree(whRes.getResponse().getContentAsString());
        long whId = whJson.get("id").asLong();

        // 5. Lấy danh sách kho
        mockMvc().perform(get("/api/warehouses?branchId=" + branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "BRANCH_MANAGE", "BRANCH_VIEW", "TRANSFER_MANAGE", "TRANSFER_VIEW", "PRODUCT_VIEW", "PRODUCT_STOCK"})
    void testMultiWarehouseStockLookupAndAdjustment() throws Exception {
        // Tạo sản phẩm mẫu
        Product p = new Product();
        p.setName("Áo Sơ Mi Oxford Cao Cấp Test");
        p.setSku("OXFORD-TEST-" + System.nanoTime());
        p.setPrice(new BigDecimal("350000"));
        p.setStock(0);
        p.setPhysicalStock(0);
        p = productRepository.saveAndFlush(p);

        // Tạo 1 chi nhánh và 2 kho
        Branch b = new Branch();
        b.setCode("BR-TEST-" + System.nanoTime());
        b.setName("Chi nhánh Test Tồn");
        b = branchRepository.saveAndFlush(b);

        Warehouse wh1 = new Warehouse();
        wh1.setBranchId(b.getId());
        wh1.setBranchName(b.getName());
        wh1.setCode("WH-A-" + System.nanoTime());
        wh1.setName("Kho Tổng A");
        wh1.setWarehouseType(WarehouseType.CENTRAL_HUB);
        wh1 = warehouseRepository.saveAndFlush(wh1);

        Warehouse wh2 = new Warehouse();
        wh2.setBranchId(b.getId());
        wh2.setBranchName(b.getName());
        wh2.setCode("WH-B-" + System.nanoTime());
        wh2.setName("Kho Bán Lẻ B");
        wh2.setWarehouseType(WarehouseType.RETAIL_STORE);
        wh2 = warehouseRepository.saveAndFlush(wh2);

        // Điều chỉnh tồn kho tại Kho A: 100 cái
        String adjustPayloadA = """
                {
                  "warehouseId": %d,
                  "productId": %d,
                  "newPhysicalStock": 100,
                  "minStockAlert": 10,
                  "reason": "Nhập hàng ban đầu"
                }
                """.formatted(wh1.getId(), p.getId());

        mockMvc().perform(post("/api/warehouses/stocks/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjustPayloadA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.physicalStock").value(100))
                .andExpect(jsonPath("$.availableStock").value(100));

        // Điều chỉnh tồn kho tại Kho B: 30 cái
        String adjustPayloadB = """
                {
                  "warehouseId": %d,
                  "productId": %d,
                  "newPhysicalStock": 30,
                  "minStockAlert": 5,
                  "reason": "Điều chuyển đợt 1"
                }
                """.formatted(wh2.getId(), p.getId());

        mockMvc().perform(post("/api/warehouses/stocks/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjustPayloadB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.physicalStock").value(30))
                .andExpect(jsonPath("$.availableStock").value(30));

        // Tra cứu ma trận tồn kho đa điểm
        mockMvc().perform(get("/api/warehouses/multi-stock/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(p.getId()))
                .andExpect(jsonPath("$.totalPhysicalStock").value(greaterThanOrEqualTo(130)))
                .andExpect(jsonPath("$.totalAvailableStock").value(greaterThanOrEqualTo(130)))
                .andExpect(jsonPath("$.breakdown", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "BRANCH_MANAGE", "BRANCH_VIEW", "TRANSFER_MANAGE", "TRANSFER_VIEW"})
    void testInternalTransferLifecycle_Draft_Dispatch_Receive_Damaged() throws Exception {
        // Chuẩn bị sản phẩm
        Product p = new Product();
        p.setName("Giày Thể Thao Sneaker Test ITN");
        p.setSku("SNEAKER-ITN-" + System.nanoTime());
        p.setPrice(new BigDecimal("800000"));
        p.setStock(50);
        p.setPhysicalStock(50);
        p = productRepository.saveAndFlush(p);

        // Chuẩn bị Chi nhánh & 2 Kho
        Branch b = new Branch();
        b.setCode("BR-TRANS-" + System.nanoTime());
        b.setName("Chi nhánh Vận Chuyển");
        b = branchRepository.saveAndFlush(b);

        Warehouse fromWh = new Warehouse();
        fromWh.setBranchId(b.getId());
        fromWh.setBranchName(b.getName());
        fromWh.setCode("WH-SRC-" + System.nanoTime());
        fromWh.setName("Kho Xuất Hà Nội");
        fromWh.setWarehouseType(WarehouseType.CENTRAL_HUB);
        fromWh = warehouseRepository.saveAndFlush(fromWh);

        Warehouse toWh = new Warehouse();
        toWh.setBranchId(b.getId());
        toWh.setBranchName(b.getName());
        toWh.setCode("WH-DST-" + System.nanoTime());
        toWh.setName("Kho Đích Sài Gòn");
        toWh.setWarehouseType(WarehouseType.RETAIL_STORE);
        toWh = warehouseRepository.saveAndFlush(toWh);

        // Khởi tạo tồn ban đầu tại Kho Xuất: 50 cái
        WarehouseStock fromStock = new WarehouseStock();
        fromStock.setWarehouseId(fromWh.getId());
        fromStock.setWarehouseName(fromWh.getName());
        fromStock.setBranchId(b.getId());
        fromStock.setProductId(p.getId());
        fromStock.setProductSku(p.getSku());
        fromStock.setProductName(p.getName());
        fromStock.setPhysicalStock(50);
        fromStock.setAvailableStock(50);
        fromStock.setReservedStock(0);
        fromStock.setInTransitStock(0);
        warehouseStockRepository.saveAndFlush(fromStock);

        // 1. Tạo phiếu điều chuyển ITN (DRAFT) - Yêu cầu chuyển 20 cái
        String createTransferPayload = """
                {
                  "fromWarehouseId": %d,
                  "toWarehouseId": %d,
                  "note": "Điều chuyển 20 đôi sneaker phục vụ chiến dịch khai trương",
                  "items": [
                    {
                      "productId": %d,
                      "quantity": 20,
                      "note": "Hàng nguyên hộp"
                    }
                  ]
                }
                """.formatted(fromWh.getId(), toWh.getId(), p.getId());

        MvcResult createRes = mockMvc().perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTransferPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalQuantity").value(20))
                .andReturn();

        JsonNode transferJson = objectMapper.readTree(createRes.getResponse().getContentAsString());
        long transferId = transferJson.get("id").asLong();
        long itemId = transferJson.get("items").get(0).get("id").asLong();

        // 2. Xác nhận xuất kho (DISPATCH)
        mockMvc().perform(post("/api/transfers/" + transferId + "/dispatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.transferredBy").value("admin"));

        // Kiểm tra tồn kho sau khi xuất:
        // Kho xuất còn 30 cái
        WarehouseStock updatedFromStock = warehouseStockRepository.findByWarehouseIdAndProductId(fromWh.getId(), p.getId()).orElseThrow();
        assertEquals(30, updatedFromStock.getPhysicalStock());
        assertEquals(30, updatedFromStock.getAvailableStock());

        // Kho nhận có 20 cái in_transit
        WarehouseStock updatedToStock = warehouseStockRepository.findByWarehouseIdAndProductId(toWh.getId(), p.getId()).orElseThrow();
        assertEquals(20, updatedToStock.getInTransitStock());
        assertEquals(0, updatedToStock.getPhysicalStock());

        // 3. Xác nhận nhập kho kiểm đếm thực tế (RECEIVE): Thực nhận 18 cái, 2 cái bị rách hộp/hỏng
        String receivePayload = """
                {
                  "note": "Kiểm nhận thực tế: Nhận 18 đôi đạt chuẩn, 2 đôi bị ướt hộp lúc vận chuyển",
                  "items": [
                    {
                      "itemId": %d,
                      "productId": %d,
                      "receivedQuantity": 18,
                      "damagedQuantity": 2,
                      "note": "2 đôi bị ướt hộp"
                    }
                  ]
                }
                """.formatted(itemId, p.getId());

        mockMvc().perform(post("/api/transfers/" + transferId + "/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(receivePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.receivedBy").value("admin"));

        // Kiểm tra tồn kho sau khi nhận:
        // Kho nhận: in_transit = 0, physicalStock = 18, availableStock = 18
        WarehouseStock finalToStock = warehouseStockRepository.findByWarehouseIdAndProductId(toWh.getId(), p.getId()).orElseThrow();
        assertEquals(0, finalToStock.getInTransitStock());
        assertEquals(18, finalToStock.getPhysicalStock());
        assertEquals(18, finalToStock.getAvailableStock());
    }
}
