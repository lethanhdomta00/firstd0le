package com.erp.order.service;

import com.erp.model.Customer;
import com.erp.model.Product;
import com.erp.order.dto.ParseOrderMessageRequest;
import com.erp.order.dto.ParsedOrderItemDto;
import com.erp.order.dto.ParsedOrderResultDto;
import com.erp.order.model.PaymentMethod;
import com.erp.repository.CustomerRepository;
import com.erp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiOrderParserService {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    // Regex bóc tách số điện thoại VN (10 chữ số, đầu 03, 05, 07, 08, 09 hoặc +84)
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+84|0)(?:3[2-9]|5[2689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}\\b");

    // Regex bóc tách danh xưng và tên khách
    private static final Pattern NAME_PREFIX_PATTERN = Pattern.compile("(?i)(?:anh|chị|khách|bác|chú|cô|bạn|khách hàng|em|tên)\\s*[:\\-]?\\s*([\\p{L}\\s]{2,30})(?:\\s+(?:lấy|đặt|mua|ở|địa chỉ|sđt|phone|0[0-9]|\\+84|–|-|,|\\n)|$)");

    // Regex bóc tách địa chỉ giao hàng
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("(?i)(?:địa chỉ|đ/c|giao về|giao đến|giao tại|ship về|ship đến|nhận tại)\\s*[:\\-]?\\s*([^\\n,\\.]{5,120})");

    /**
     * Bóc tách toàn diện tin nhắn ngôn ngữ tự nhiên thành cấu trúc Đơn hàng nháp
     */
    @Transactional(readOnly = true)
    public ParsedOrderResultDto parseOrderMessage(ParseOrderMessageRequest request) {
        if (request == null || request.getText() == null || request.getText().trim().isEmpty()) {
            return ParsedOrderResultDto.builder()
                    .confidenceScore(0.0)
                    .summary("Nội dung tin nhắn trống")
                    .warnings(List.of("Vui lòng cung cấp nội dung tin nhắn đặt hàng của khách!"))
                    .items(Collections.emptyList())
                    .estimatedTotal(BigDecimal.ZERO)
                    .build();
        }

        String rawText = request.getText().trim();
        List<String> warnings = new ArrayList<>();
        double scorePoints = 0.0;

        // 1. Bóc tách số điện thoại
        String phone = null;
        Matcher phoneMatcher = PHONE_PATTERN.matcher(rawText);
        if (phoneMatcher.find()) {
            phone = phoneMatcher.group(0);
            scorePoints += 2.0;
        }

        // 2. Tra cứu khách hàng theo SĐT (nếu có)
        String customerName = null;
        String address = null;
        String email = null;
        if (phone != null) {
            Optional<Customer> customerOpt = customerRepository.findByPhone(phone);
            if (customerOpt.isPresent()) {
                Customer c = customerOpt.get();
                customerName = c.getName();
                address = c.getAddress();
                email = c.getEmail();
                scorePoints += 2.0;
                log.info("AI Parser: Nhận diện khách hàng thân thiết [{}] qua SĐT {}", customerName, phone);
            }
        }

        // 3. Nếu chưa có tên, tìm qua danh xưng
        if (customerName == null) {
            Matcher nameMatcher = NAME_PREFIX_PATTERN.matcher(rawText);
            if (nameMatcher.find()) {
                customerName = cleanName(nameMatcher.group(1));
                if (customerName != null && !customerName.isEmpty()) {
                    scorePoints += 1.5;
                }
            }
        }
        if (customerName == null) {
            customerName = "Khách đặt qua tin nhắn";
            warnings.add("Chưa nhận diện rõ tên khách hàng, đã gán tên mặc định.");
        }

        // 4. Nếu chưa có địa chỉ, bóc tách qua từ khóa địa chỉ
        if (address == null) {
            Matcher addrMatcher = ADDRESS_PATTERN.matcher(rawText);
            if (addrMatcher.find()) {
                address = addrMatcher.group(1).trim();
                scorePoints += 1.5;
            }
        }
        if (address == null) {
            address = "Giao hàng theo thỏa thuận";
            warnings.add("Chưa nhận diện được địa chỉ chi tiết, cần xác nhận lại với khách.");
        }

        // 5. Xác định hình thức thanh toán
        PaymentMethod paymentMethod = PaymentMethod.CASH;
        String normalizedLower = removeDiacritics(rawText).toLowerCase();
        if (normalizedLower.contains("cong no") || normalizedLower.contains("ghi no")
                || normalizedLower.contains("cho no") || normalizedLower.contains("so no")
                || normalizedLower.contains("chot no") || normalizedLower.contains("no nhe")) {
            paymentMethod = PaymentMethod.CREDIT;
            scorePoints += 1.0;
        } else if (normalizedLower.contains("chuyen khoan") || normalizedLower.contains("vietqr")
                || normalizedLower.contains("ck") || normalizedLower.contains("ban tien")
                || normalizedLower.contains("napas") || normalizedLower.contains("qr")) {
            paymentMethod = PaymentMethod.VIETQR;
            scorePoints += 1.0;
        }

        // 6. Bóc tách danh sách sản phẩm, số lượng, đơn vị tính (UoM)
        List<Product> allProducts = productRepository.findAll();
        List<ParsedOrderItemDto> parsedItems = matchProductsInText(rawText, normalizedLower, allProducts, warnings);

        BigDecimal estimatedTotal = BigDecimal.ZERO;
        for (ParsedOrderItemDto item : parsedItems) {
            if (item.getLineTotal() != null) {
                estimatedTotal = estimatedTotal.add(item.getLineTotal());
            }
        }

        if (!parsedItems.isEmpty()) {
            scorePoints += 3.5;
        } else {
            warnings.add("Không tìm thấy mã sản phẩm nào trong kho khớp với tin nhắn!");
        }

        double confidenceScore = Math.min(1.0, Math.max(0.2, scorePoints / 10.0));

        String summary = String.format("Nhận diện %d mặt hàng cho khách [%s] (SĐT: %s) • Tổng dự kiến: %,.0f đ",
                parsedItems.size(),
                customerName,
                phone != null ? phone : "Chưa có",
                estimatedTotal.doubleValue());

        return ParsedOrderResultDto.builder()
                .customerName(customerName)
                .customerPhone(phone)
                .customerEmail(email)
                .shippingAddress(address)
                .paymentMethod(paymentMethod)
                .paymentMethodLabel(paymentMethod.getLabel())
                .note("Đơn tự động bóc tách từ tin nhắn: \"" + (rawText.length() > 80 ? rawText.substring(0, 77) + "..." : rawText) + "\"")
                .items(parsedItems)
                .estimatedTotal(estimatedTotal)
                .confidenceScore(confidenceScore)
                .summary(summary)
                .warnings(warnings)
                .build();
    }

    private List<ParsedOrderItemDto> matchProductsInText(String rawText, String normalizedText, List<Product> products, List<String> warnings) {
        List<ParsedOrderItemDto> matched = new ArrayList<>();
        Set<Long> matchedProductIds = new HashSet<>();

        // Sắp xếp sản phẩm theo độ dài tên giảm dần để ưu tiên khớp cụm từ dài trước
        List<Product> sorted = new ArrayList<>(products);
        sorted.sort((a, b) -> Integer.compare(b.getName().length(), a.getName().length()));

        for (Product product : sorted) {
            String normProdName = removeDiacritics(product.getName()).toLowerCase();
            String skuLower = product.getSku().toLowerCase();

            // 1. Kiểm tra khớp full Tên sản phẩm
            int idx = normalizedText.indexOf(normProdName);
            int matchedLength = normProdName.length();
            boolean matchedBySku = false;

            // 2. Kiểm tra khớp mã SKU
            if (idx < 0) {
                idx = normalizedText.indexOf(skuLower);
                if (idx >= 0) {
                    matchedBySku = true;
                    matchedLength = skuLower.length();
                }
            }

            // 3. Khớp mờ thông minh: Nếu khách gõ tắt 2-3 từ đầu của tên sản phẩm (VD: "Quần Tây Âu" thay vì "Quần Tây Âu Co Giãn")
            if (idx < 0) {
                String[] words = normProdName.split("\\s+");
                if (words.length >= 3) {
                    String prefix3 = words[0] + " " + words[1] + " " + words[2];
                    idx = normalizedText.indexOf(prefix3);
                    if (idx >= 0) {
                        matchedLength = prefix3.length();
                    }
                }
                if (idx < 0 && words.length >= 2) {
                    String prefix2 = words[0] + " " + words[1];
                    // Chỉ match prefix2 nếu độ dài >= 5 ký tự để tránh match từ quá ngắn gây nhiễu
                    if (prefix2.length() >= 5) {
                        idx = normalizedText.indexOf(prefix2);
                        if (idx >= 0) {
                            matchedLength = prefix2.length();
                        }
                    }
                }
            }

            if (idx >= 0 && !matchedProductIds.contains(product.getId())) {
                matchedProductIds.add(product.getId());

                // Quét ngữ cảnh xung quanh từ khóa sản phẩm để tìm số lượng và đơn vị tính
                int startScan = Math.max(0, idx - 40);
                String beforeSnippet = normalizedText.substring(startScan, idx);

                int endScan = Math.min(normalizedText.length(), idx + matchedLength + 40);
                String afterSnippet = normalizedText.substring(idx + matchedLength, endScan);

                // Trích xuất số lượng và đơn vị tính (ưu tiên số lượng đứng ngay trước hoặc ngay sau tên sản phẩm)
                QuantityAndUom qu = extractQuantityAndUom(beforeSnippet, afterSnippet, product.getBaseUnit());

                int qty = qu.quantity;
                String uom = qu.uomName;
                int rate = qu.conversionRate;

                BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf((long) qty * rate));
                int availableStock = product.getAvailableStock();
                boolean inStock = availableStock >= (qty * rate);

                if (!inStock) {
                    warnings.add(String.format("Sản phẩm [%s] chỉ còn tồn khả dụng %d %s (yêu cầu: %d %s)",
                            product.getName(), availableStock, product.getBaseUnit(), qty * rate, product.getBaseUnit()));
                }

                matched.add(ParsedOrderItemDto.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .productSku(product.getSku())
                        .productImageUrl(product.getImageUrl())
                        .quantity(qty)
                        .uomName(uom)
                        .uomConversionRate(rate)
                        .unitPrice(unitPrice)
                        .lineTotal(lineTotal)
                        .availableStock(availableStock)
                        .inStock(inStock)
                        .build());
            }
        }

        return matched;
    }

    private QuantityAndUom extractQuantityAndUom(String beforeSnippet, String afterSnippet, String defaultBaseUnit) {
        int quantity = 1;
        String uomName = defaultBaseUnit != null && !defaultBaseUnit.trim().isEmpty() ? defaultBaseUnit : "Cái";
        int rate = 1;

        // Ưu tiên 1: Tìm số lượng và đơn vị tính đứng liền TRƯỚC tên sản phẩm
        // Ví dụ: "... lấy 2 thùng [Sản phẩm]" hoặc "... 10 cái [Sản phẩm]"
        Pattern pBefore = Pattern.compile("(\\d+)\\s*(thung|hop|kien|loc|cai|chiec|bo|goi|lon|chai)?\\s*$");
        Matcher mBefore = pBefore.matcher(beforeSnippet.trim());
        if (mBefore.find()) {
            try {
                quantity = Math.max(1, Integer.parseInt(mBefore.group(1)));
                String rawUom = mBefore.group(2);
                UomInfo uomInfo = resolveUom(rawUom, defaultBaseUnit);
                uomName = uomInfo.uomName;
                rate = uomInfo.rate;
                return new QuantityAndUom(quantity, uomName, rate);
            } catch (Exception ignored) {
            }
        }

        // Ưu tiên 2: Tìm số lượng và đơn vị tính đứng liền SAU tên sản phẩm
        // Ví dụ: "[Sản phẩm] x 5 thùng" hoặc "[Sản phẩm] : 10 cái" hoặc "[Sản phẩm] 3 hộp"
        Pattern pAfter = Pattern.compile("^\\s*(?:x|:|so luong|sl|lay)?\\s*(\\d+)\\s*(thung|hop|kien|loc|cai|chiec|bo|goi|lon|chai)?");
        Matcher mAfter = pAfter.matcher(afterSnippet.trim());
        if (mAfter.find()) {
            try {
                quantity = Math.max(1, Integer.parseInt(mAfter.group(1)));
                String rawUom = mAfter.group(2);
                UomInfo uomInfo = resolveUom(rawUom, defaultBaseUnit);
                uomName = uomInfo.uomName;
                rate = uomInfo.rate;
                return new QuantityAndUom(quantity, uomName, rate);
            } catch (Exception ignored) {
            }
        }

        return new QuantityAndUom(quantity, uomName, rate);
    }

    private static class UomInfo {
        final String uomName;
        final int rate;
        UomInfo(String uomName, int rate) {
            this.uomName = uomName;
            this.rate = rate;
        }
    }

    private UomInfo resolveUom(String rawUom, String defaultBaseUnit) {
        String uomName = defaultBaseUnit != null && !defaultBaseUnit.trim().isEmpty() ? defaultBaseUnit : "Cái";
        int rate = 1;
        if (rawUom != null) {
            rawUom = rawUom.toLowerCase();
            if (rawUom.contains("thung") || rawUom.contains("kien")) {
                uomName = "Thùng";
                rate = 10;
            } else if (rawUom.contains("hop") || rawUom.contains("loc")) {
                uomName = "Hộp";
                rate = 5;
            } else if (rawUom.contains("bo")) {
                uomName = "Bộ";
                rate = 1;
            } else {
                uomName = "Cái";
                rate = 1;
            }
        }
        return new UomInfo(uomName, rate);
    }

    private static class QuantityAndUom {
        final int quantity;
        final String uomName;
        final int conversionRate;

        QuantityAndUom(int quantity, String uomName, int conversionRate) {
            this.quantity = quantity;
            this.uomName = uomName;
            this.conversionRate = conversionRate;
        }
    }

    private String cleanName(String raw) {
        if (raw == null) return null;
        String trimmed = raw.replaceAll("[\\-\\.,:]", " ").trim();
        if (trimmed.length() < 2 || trimmed.length() > 40) return null;
        return trimmed;
    }

    public static String removeDiacritics(String str) {
        if (str == null) return "";
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }
}
