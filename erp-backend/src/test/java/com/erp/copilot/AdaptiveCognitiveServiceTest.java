package com.erp.copilot;

import com.erp.copilot.dto.CopilotResponseDto;
import com.erp.copilot.service.AdaptiveCognitiveService;
import com.erp.copilot.tool.AgenticToolRegistry;
import com.erp.model.Customer;
import com.erp.model.Product;
import com.erp.order.model.Order;
import com.erp.order.model.OrderStatus;
import com.erp.order.model.PaymentStatus;
import com.erp.order.repository.OrderRepository;
import com.erp.order.service.InventoryForecastService;
import com.erp.repository.CustomerRepository;
import com.erp.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdaptiveCognitiveServiceTest {

    @Autowired
    private AdaptiveCognitiveService cognitiveService;

    @Autowired
    private AgenticToolRegistry toolRegistry;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        // Tạo sản phẩm mẫu Polo đen
        if (productRepository.findBySku("POLO-NAM-BLACK").isEmpty()) {
            Product p = new Product();
            p.setName("Áo Polo Nam Classic Đen");
            p.setSku("POLO-NAM-BLACK");
            p.setPrice(new BigDecimal("299000.00"));
            p.setStock(45);
            p.setPhysicalStock(50);
            p.setReservedStock(5);
            p.setMinStockAlert(10);
            productRepository.save(p);
        }

        // Tạo khách hàng nợ quá hạn
        if (customerRepository.findByPhone("0988777666").isEmpty()) {
            Customer c = new Customer();
            c.setName("Đại lý Minh Tuấn");
            c.setPhone("0988777666");
            c.setCreditLimit(new BigDecimal("100000000.00"));
            c.setCurrentDebt(new BigDecimal("35000000.00"));
            c.setOverdueDays(45);
            customerRepository.save(c);
        }
    }

    @Test
    @DisplayName("Kiểm tra Tool Registry đã nạp đủ các Tools nghiệp vụ cốt lõi")
    void testToolRegistryInitialized() {
        assertNotNull(toolRegistry);
        assertTrue(toolRegistry.getAllTools().size() >= 8, "Phải có tối thiểu 8 tools được đăng ký");
        assertTrue(toolRegistry.getTool("checkProductStock").isPresent());
        assertTrue(toolRegistry.getTool("getSalesPerformance").isPresent());
        assertTrue(toolRegistry.getTool("getCustomerDebtSummary").isPresent());
        assertTrue(toolRegistry.getTool("generateExportReport").isPresent());
        assertTrue(toolRegistry.getTool("trackShipmentOrder").isPresent());
        assertTrue(toolRegistry.getTool("draftExpenseVoucher").isPresent());
        assertTrue(toolRegistry.getTool("queryProcurementAndSuppliers").isPresent());
        assertTrue(toolRegistry.getTool("queryPromotionsAndLoyalty").isPresent());
    }

    @Test
    @DisplayName("Test câu hỏi tiếng lóng: 'Kho còn cái áo cổ bẻ màu đen ko mày?' -> Nhận diện Polo đen và trả về STOCK_ALERT_CARD")
    void testSlangStockCheck() {
        CopilotResponseDto response = cognitiveService.processPrompt("Kho còn cái áo cổ bẻ màu đen ko mày?", "admin");
        assertNotNull(response);
        assertEquals("TIER_1_ACTION", response.getTier());
        assertTrue(response.getContent().contains("POLO-NAM-BLACK") || response.getContent().contains("Polo"));
        assertNotNull(response.getUiWidget());
        assertEquals("STOCK_ALERT_CARD", response.getUiWidget().getType());
        assertFalse(response.getThinkingTrace().isEmpty());
    }

    @Test
    @DisplayName("Test câu hỏi tiếng lóng: 'Hôm nay ấm no ko mày?' -> Nhận diện Doanh số và trả về MINI_SPARKLINE_CHART")
    void testSlangSalesSummary() {
        CopilotResponseDto response = cognitiveService.processPrompt("Hôm nay ấm no ko mày?", "admin");
        assertNotNull(response);
        assertEquals("TIER_1_ACTION", response.getTier());
        assertTrue(response.getContent().contains("BÁO CÁO DOANH SỐ"));
        assertNotNull(response.getUiWidget());
        assertEquals("MINI_SPARKLINE_CHART", response.getUiWidget().getType());
    }

    @Test
    @DisplayName("Test câu hỏi tiếng lóng: 'Thằng nào nợ dai nhất?' -> Nhận diện Nợ xấu và trả về hồ sơ nợ")
    void testSlangDebtAnalysis() {
        CopilotResponseDto response = cognitiveService.processPrompt("Thằng nào nợ dai nhất?", "admin");
        assertNotNull(response);
        assertEquals("TIER_1_ACTION", response.getTier());
        assertTrue(response.getContent().contains("CÔNG NỢ"));
    }

    @Test
    @DisplayName("Test yêu cầu xuất file: 'Xuất cho tao file Excel doanh thu tuần này' -> Nhận diện EXPORT_REPORT và trả về FILE_DOWNLOAD_CARD")
    void testReportExportPrompt() {
        CopilotResponseDto response = cognitiveService.processPrompt("Xuất cho tao file Excel doanh thu tuần này", "admin");
        assertNotNull(response);
        assertEquals("TIER_1_ACTION", response.getTier());
        assertNotNull(response.getUiWidget());
        assertEquals("FILE_DOWNLOAD_CARD", response.getUiWidget().getType());
        assertTrue(response.getUiWidget().getData().containsKey("downloadUrl"));
        assertTrue(response.getUiWidget().getData().containsKey("fileName"));
    }

    @Test
    @DisplayName("Test Tầng 2: Tư vấn chiến lược nhập hàng định lượng")
    void testAdvisoryTier() {
        CopilotResponseDto response = cognitiveService.processPrompt("Tháng sau tao tính nhập thêm 100 cái áo Polo, mày thấy nên nhập không?", "admin");
        assertNotNull(response);
        assertEquals("TIER_2_ADVISORY", response.getTier());
        assertTrue(response.getContent().contains("GÓC TƯ VẤN CHIẾN LƯỢC"));
    }

    @Test
    @DisplayName("Test Tầng 3: Hướng dẫn quy trình xuất hóa đơn đỏ PDF")
    void testErpHelpTier() {
        CopilotResponseDto response = cognitiveService.processPrompt("Làm sao để xuất hóa đơn đỏ PDF?", "admin");
        assertNotNull(response);
        assertEquals("TIER_3_ERP_QA", response.getTier());
        assertTrue(response.getContent().contains("QUY TRÌNH XUẤT HÓA ĐƠN ĐỎ PDF"));
    }

    @Test
    @DisplayName("Test câu cảm ơn: 'okeee cảm ơn' -> Phản hồi thân mật tự nhiên, KHÔNG có thinking trace thừa")
    void testGratitudeIntent() {
        CopilotResponseDto response = cognitiveService.processPrompt("okeee cảm ơn", "admin");
        assertNotNull(response);
        assertEquals("TIER_4_CONVERSATIONAL", response.getTier());
        assertTrue(response.getThinkingTrace().isEmpty(), "Câu cảm ơn không được có thinking trace thừa thãi");
        assertTrue(response.getContent().contains("không có chi") 
                || response.getContent().contains("Không có gì") 
                || response.getContent().contains("hỗ trợ")
                || response.getContent().contains("anh em"));
        assertNull(response.getUiWidget());
    }

    @Test
    @DisplayName("Test câu khen ngợi: 'quá đỉnh mày ơi' -> Phản hồi hào hứng, KHÔNG trace")
    void testPraiseIntent() {
        CopilotResponseDto response = cognitiveService.processPrompt("quá đỉnh mày ơi", "admin");
        assertNotNull(response);
        assertEquals("TIER_4_CONVERSATIONAL", response.getTier());
        assertTrue(response.getThinkingTrace().isEmpty());
    }

    @Test
    @DisplayName("Test xác nhận: 'okeee' -> Đáp nhanh sẵn sàng, KHÔNG trace")
    void testAcknowledgmentIntent() {
        CopilotResponseDto response = cognitiveService.processPrompt("okeee", "admin");
        assertNotNull(response);
        assertEquals("TIER_4_CONVERSATIONAL", response.getTier());
        assertTrue(response.getThinkingTrace().isEmpty());
    }

    @Test
    @DisplayName("Test chốt ca: 'chốt ca nghỉ thôi' -> Chúc nghỉ ngơi, KHÔNG trace")
    void testFarewellIntent() {
        CopilotResponseDto response = cognitiveService.processPrompt("chốt ca nghỉ thôi", "admin");
        assertNotNull(response);
        assertEquals("TIER_4_CONVERSATIONAL", response.getTier());
        assertTrue(response.getThinkingTrace().isEmpty());
        assertTrue(response.getContent().contains("nghỉ ngơi"));
    }

    @Test
    @DisplayName("Test câu hỏi không rõ ràng / bâng quơ -> Fallback nhẹ nhàng mở lời, KHÔNG trace, KHÔNG tuôn văn mẫu Onboarding")
    void testFallbackGeneralNoTrace() {
        CopilotResponseDto response = cognitiveService.processPrompt("thời tiết hôm nay thế nào nhỉ", "admin");
        assertNotNull(response);
        assertEquals("TIER_4_CONVERSATIONAL", response.getTier());
        assertTrue(response.getThinkingTrace().isEmpty(), "Fallback không được có thinking trace thừa thãi");
        assertFalse(response.getContent().contains("Tôi là ERP AI Copilot Ultra VIP"), "Không được xả văn mẫu Onboarding");
        assertTrue(response.getContent().contains("chưa hiểu rõ") || response.getContent().contains("tao nghe đây"));
    }

    @Test
    @DisplayName("Test Tầng 4: Chào hỏi xã giao thân thiện")
    void testConversationalTier() {
        CopilotResponseDto response = cognitiveService.processPrompt("Chào buổi sáng Copilot, hôm nay có gì vui không?", "admin");
        assertNotNull(response);
        assertEquals("TIER_4_CONVERSATIONAL", response.getTier());
        assertTrue(response.getContent().contains("Copilot") || response.getContent().contains("Chào buổi sáng"));
    }

    @Test
    @DisplayName("Test Trụ cột 1: Trí nhớ Ngữ cảnh Đa lượt & Phân giải Đại từ (Anaphora Resolution)")
    void testMultiTurnAnaphoraResolution() {
        // Lượt 1: Tra cứu tồn kho Polo đen
        CopilotResponseDto r1 = cognitiveService.processPrompt("Kho còn cái áo Polo đen không mày?", "admin_multi");
        assertNotNull(r1);

        // Lượt 2: Hỏi tiếp bằng đại từ thay thế "nó"
        CopilotResponseDto r2 = cognitiveService.processPrompt("Nó còn bao nhiêu cái?", "admin_multi");
        assertNotNull(r2);
        assertEquals("TIER_1_ACTION", r2.getTier());
        // Thinking trace phải có bước ghi nhận Anaphora Resolve
        boolean hasAnaphoraTrace = r2.getThinkingTrace().stream()
                .anyMatch(st -> st.getStep() != null && st.getStep().equals("ANAPHORA_RESOLVE"));
        assertTrue(hasAnaphoraTrace, "Phải có trace ANAPHORA_RESOLVE nhận diện ngữ cảnh kế thừa");
    }

    @Test
    @DisplayName("Test Trụ cột 3: Conversational POS - Ra lệnh tạo đơn dự thảo trong chat sinh ORDER_DRAFT_CARD")
    void testConversationalPosDraftOrder() {
        CopilotResponseDto response = cognitiveService.processPrompt(
                "Lên đơn cho chị Lan 0988123456: 2 áo Polo đen, chuyển khoản, giao về 45 Lê Lợi Q1", "admin_pos");
        assertNotNull(response);
        assertEquals("TIER_1_ACTION", response.getTier());
        assertNotNull(response.getUiWidget());
        assertEquals("ORDER_DRAFT_CARD", response.getUiWidget().getType());

        var data = response.getUiWidget().getData();
        assertNotNull(data);
        assertEquals("Lan", data.get("customerName"));
        assertEquals("0988123456", data.get("customerPhone"));
        assertEquals("BANK_TRANSFER", data.get("paymentMethod"));
        assertTrue(data.containsKey("totalAmount"));
        assertTrue(data.containsKey("items"));
    }

    @Test
    @DisplayName("Test câu hỏi giải thích tính năng: 'giải thích tính năng tài chính & công nợ' -> Trả về TIER_3_ERP_QA, KHÔNG nhầm sang tra cứu data nợ")
    void testExplainFinanceAndDebtQuery() {
        CopilotResponseDto response = cognitiveService.processPrompt("giải thích tính năng tài chính & công nợ", "admin");
        assertNotNull(response);
        assertEquals("TIER_3_ERP_QA", response.getTier());
        assertTrue(response.getContent().contains("TÀI CHÍNH & CÔNG NỢ"));
        assertTrue(response.getContent().contains("Quản lý Quỹ Đa Tài Khoản"));
        assertTrue(response.getContent().contains("Smart Bank Reconciliation"));
        assertTrue(response.getContent().contains("Báo Cáo Lãi Lỗ P&L BI"));
        assertTrue(response.getContent().contains("Quản Trị Công Nợ Đại Lý"));
        assertTrue(response.getContent().contains("PHÂN QUYỀN (RBAC)"));
        assertFalse(response.getContent().contains("Không tìm thấy công nợ"), "Không được nhầm sang Action Tool tra cứu data nợ");
    }

    @Test
    @DisplayName("Test tra cứu Mua hàng: 'Có đơn mua PO nào đang chờ duyệt không?' -> Kích hoạt queryProcurementAndSuppliers")
    void testProcurementIntentPendingPo() {
        CopilotResponseDto response = cognitiveService.processPrompt("Có đơn đặt mua PO nào đang chờ duyệt không?", "admin");
        assertNotNull(response);
        assertEquals("TIER_1_ACTION", response.getTier());
        assertNotNull(response.getContent());
        assertTrue(response.getContent().contains("ĐƠN ĐẶT MUA") || response.getContent().contains("không có đơn"));
    }

    @Test
    @DisplayName("Test tra cứu Khuyến mại: 'Hệ thống có voucher nào đang kích hoạt không?' -> Kích hoạt queryPromotionsAndLoyalty")
    void testPromotionIntentVouchers() {
        CopilotResponseDto response = cognitiveService.processPrompt("Hệ thống có voucher nào đang kích hoạt không?", "admin");
        assertNotNull(response);
        assertEquals("TIER_1_ACTION", response.getTier());
        assertNotNull(response.getContent());
        assertTrue(response.getContent().contains("MÃ VOUCHER") || response.getContent().contains("Voucher"));
    }

    @Test
    @DisplayName("Test câu hỏi giải thích quy trình Mua hàng & MAC: 'giải thích quy trình mua hàng và tính giá vốn bình quân gia quyền'")
    void testExplainProcurementAndMacQuery() {
        CopilotResponseDto response = cognitiveService.processPrompt("giải thích quy trình mua hàng và tính giá vốn bình quân gia quyền", "admin");
        assertNotNull(response);
        assertEquals("TIER_3_ERP_QA", response.getTier());
        assertTrue(response.getContent().contains("MUA HÀNG, SRM & GIÁ VỐN BÌNH QUÂN GIA QUYỀN"));
        assertTrue(response.getContent().contains("Purchase Order - PO"));
        assertTrue(response.getContent().contains("Goods Receipt Note - GRN"));
        assertTrue(response.getContent().contains("Giá vốn mới ="));
    }

    @Test
    @DisplayName("Test câu hỏi giải thích Khuyến mại & Loyalty: 'giới thiệu tính năng khuyến mại và tích điểm hội viên'")
    void testExplainPromotionsAndLoyaltyQuery() {
        CopilotResponseDto response = cognitiveService.processPrompt("giới thiệu tính năng khuyến mại và tích điểm hội viên", "admin");
        assertNotNull(response);
        assertEquals("TIER_3_ERP_QA", response.getTier());
        assertTrue(response.getContent().contains("ĐỘNG CƠ KHUYẾN MẠI, VOUCHER & TÍCH ĐIỂM HỘI VIÊN"));
        assertTrue(response.getContent().contains("Mã Giảm Giá & Voucher Đa Năng"));
        assertTrue(response.getContent().contains("Bảng Giá Sỉ"));
        assertTrue(response.getContent().contains("Kim Cương (Diamond)"));
    }
}
