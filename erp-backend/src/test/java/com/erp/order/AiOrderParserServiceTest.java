package com.erp.order;

import com.erp.model.Customer;
import com.erp.model.Product;
import com.erp.order.dto.ParseOrderMessageRequest;
import com.erp.order.dto.ParsedOrderResultDto;
import com.erp.order.model.PaymentMethod;
import com.erp.order.service.AiOrderParserService;
import com.erp.repository.CustomerRepository;
import com.erp.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiOrderParserServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private AiOrderParserService aiOrderParserService;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        product1 = new Product();
        product1.setId(10L);
        product1.setName("Áo Polo Nam Classic");
        product1.setSku("POLO-01");
        product1.setBaseUnit("Cái");
        product1.setPrice(new BigDecimal("250000"));
        product1.setPhysicalStock(100);
        product1.setReservedStock(0);
        product1.setStock(100);

        product2 = new Product();
        product2.setId(20L);
        product2.setName("Quần Tây Âu Co Giãn");
        product2.setSku("PANTS-02");
        product2.setBaseUnit("Cái");
        product2.setPrice(new BigDecimal("400000"));
        product2.setPhysicalStock(5);
        product2.setReservedStock(0);
        product2.setStock(5);
    }

    @Test
    @DisplayName("AI bóc tách chính xác tin nhắn ngôn ngữ tự nhiên: SĐT, tên, địa chỉ, sản phẩm và công nợ")
    void testParseNaturalLanguageOrderMessage() {
        String message = "Anh Tuấn 0987654321 giao về 123 Cầu Giấy Hà Nội lấy 2 thùng Áo Polo Nam Classic và 10 cái Quần Tây Âu, ghi nợ cho anh nhé";

        when(productRepository.findAll()).thenReturn(List.of(product1, product2));
        when(customerRepository.findByPhone("0987654321")).thenReturn(Optional.empty());

        ParsedOrderResultDto result = aiOrderParserService.parseOrderMessage(new ParseOrderMessageRequest(message));

        assertNotNull(result);
        assertEquals("0987654321", result.getCustomerPhone());
        assertEquals("Tuấn", result.getCustomerName());
        assertTrue(result.getShippingAddress().contains("123 Cầu Giấy"));
        assertEquals(PaymentMethod.CREDIT, result.getPaymentMethod());

        // Kiểm tra sản phẩm
        assertEquals(2, result.getItems().size());

        // Item 1: Áo Polo - 2 thùng (mỗi thùng 10 cái -> 20 cái)
        var itemPolo = result.getItems().stream()
                .filter(i -> i.getProductId().equals(10L))
                .findFirst().orElseThrow();
        assertEquals(2, itemPolo.getQuantity());
        assertEquals("Thùng", itemPolo.getUomName());
        assertEquals(10, itemPolo.getUomConversionRate());
        assertEquals(new BigDecimal("5000000"), itemPolo.getLineTotal()); // 2 thùng * 10 * 250k = 5.000.000
        assertTrue(itemPolo.isInStock());

        // Item 2: Quần Tây - 10 cái (yêu cầu 10 cái nhưng kho chỉ còn 5 cái -> inStock = false)
        var itemPants = result.getItems().stream()
                .filter(i -> i.getProductId().equals(20L))
                .findFirst().orElseThrow();
        assertEquals(10, itemPants.getQuantity());
        assertEquals(new BigDecimal("4000000"), itemPants.getLineTotal());
        assertFalse(itemPants.isInStock());

        // Có cảnh báo vượt tồn kho
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getConfidenceScore() >= 0.7);
    }

    @Test
    @DisplayName("AI tự động tra cứu danh bạ khách hàng cũ qua số điện thoại")
    void testParseMessageWithExistingCustomer() {
        String message = "Ship cho anh về kho cũ 5 cái Áo Polo Nam Classic chuyển khoản nhé SĐT 0912345678";

        Customer existing = new Customer();
        existing.setName("Công ty TNHH Hoàng Mai");
        existing.setPhone("0912345678");
        existing.setAddress("Kho 4, Cảng Bạch Đằng, Hà Nội");

        when(customerRepository.findByPhone("0912345678")).thenReturn(Optional.of(existing));
        when(productRepository.findAll()).thenReturn(List.of(product1));

        ParsedOrderResultDto result = aiOrderParserService.parseOrderMessage(new ParseOrderMessageRequest(message));

        assertNotNull(result);
        assertEquals("Công ty TNHH Hoàng Mai", result.getCustomerName());
        assertEquals("0912345678", result.getCustomerPhone());
        assertEquals("Kho 4, Cảng Bạch Đằng, Hà Nội", result.getShippingAddress());
        assertEquals(PaymentMethod.VIETQR, result.getPaymentMethod());
        assertEquals(1, result.getItems().size());
        assertEquals(5, result.getItems().get(0).getQuantity());
    }
}
