package com.erp.copilot.tool;

import com.erp.copilot.dto.ToolExecutionResult;
import com.erp.copilot.dto.UiWidgetDto;
import com.erp.order.dto.ShippingTrackingDto;
import com.erp.order.dto.TrackingEventDto;
import com.erp.order.model.Order;
import com.erp.order.repository.OrderRepository;
import com.erp.order.service.ShippingLogisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogisticsAgentTool implements AgentTool {

    private final OrderRepository orderRepository;
    private final ShippingLogisticsService shippingLogisticsService;
    private final Locale vnLocale = new Locale("vi", "VN");

    @Override
    public String getToolName() {
        return "trackShipmentOrder";
    }

    @Override
    public String getDescription() {
        return "Tra cứu hành trình vận đơn bưu cục (GHN, GHTK, Viettel Post), trạng thái shipper phát hàng, tiền thu hộ COD và timeline vận chuyển thời gian thực.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> keyword = new HashMap<>();
        keyword.put("type", "string");
        keyword.put("description", "Mã đơn hàng (vd: 'ORD-20261118-001'), mã vận đơn tracking (vd: 'GHN-SG12345678') hoặc tên khách hàng.");
        properties.put("keyword", keyword);

        schema.put("properties", properties);
        schema.put("required", Collections.singletonList("keyword"));
        return schema;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args, String currentUsername) {
        String keyword = args.get("keyword") != null ? String.valueOf(args.get("keyword")).trim() : "";
        if (keyword.isEmpty()) {
            return ToolExecutionResult.failure(getToolName(), "Vui lòng cung cấp mã đơn hàng hoặc mã vận đơn cần tra cứu!");
        }

        // Tìm kiếm đơn hàng theo orderCode hoặc trackingCode hoặc tên khách
        Optional<Order> orderOpt = orderRepository.findByOrderCode(keyword);
        if (orderOpt.isEmpty()) {
            List<Order> found = orderRepository.findAll().stream()
                    .filter(o -> (o.getTrackingCode() != null && o.getTrackingCode().equalsIgnoreCase(keyword))
                            || (o.getOrderCode() != null && o.getOrderCode().toLowerCase().contains(keyword.toLowerCase()))
                            || (o.getCustomerName() != null && o.getCustomerName().toLowerCase().contains(keyword.toLowerCase())))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(1)
                    .toList();
            if (!found.isEmpty()) {
                orderOpt = Optional.of(found.get(0));
            }
        }

        if (orderOpt.isEmpty()) {
            return ToolExecutionResult.failure(getToolName(), "Không tìm thấy vận đơn nào phù hợp với thông tin: '" + keyword + "'.");
        }

        Order order = orderOpt.get();
        ShippingTrackingDto trackingDto = shippingLogisticsService.getShippingTracking(order.getId());
        NumberFormat currFmt = NumberFormat.getCurrencyInstance(vnLocale);
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

        Map<String, Object> widgetData = new LinkedHashMap<>();
        widgetData.put("orderId", trackingDto.getOrderId());
        widgetData.put("orderCode", trackingDto.getOrderCode());
        widgetData.put("carrier", trackingDto.getCarrier());
        widgetData.put("carrierName", trackingDto.getCarrierName());
        widgetData.put("trackingCode", trackingDto.getTrackingCode());
        widgetData.put("shippingStatus", trackingDto.getShippingStatus());
        widgetData.put("shippingStatusLabel", trackingDto.getShippingStatusLabel());
        widgetData.put("shippingFeeFormatted", currFmt.format(trackingDto.getShippingFee() != null ? trackingDto.getShippingFee() : BigDecimal.ZERO));
        widgetData.put("codAmountFormatted", currFmt.format(trackingDto.getCodAmount() != null ? trackingDto.getCodAmount() : BigDecimal.ZERO));
        widgetData.put("customerName", order.getCustomerName());
        widgetData.put("customerPhone", order.getCustomerPhone());
        widgetData.put("shippingAddress", order.getShippingAddress());

        List<Map<String, Object>> eventsList = new ArrayList<>();
        if (trackingDto.getEvents() != null) {
            for (TrackingEventDto ev : trackingDto.getEvents()) {
                Map<String, Object> evMap = new LinkedHashMap<>();
                evMap.put("title", ev.getTitle());
                evMap.put("description", ev.getDescription());
                evMap.put("location", ev.getLocation());
                evMap.put("timestamp", ev.getTimestamp() != null ? ev.getTimestamp().format(timeFmt) : "");
                evMap.put("completed", ev.isCompleted());
                eventsList.add(evMap);
            }
        }
        widgetData.put("events", eventsList);

        UiWidgetDto widget = UiWidgetDto.builder()
                .type("SHIPMENT_TRACKING_CARD")
                .data(widgetData)
                .build();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🚚 **HÀNH TRÌNH VẬN ĐƠN: %s** (Đơn hàng `%s`):\n", trackingDto.getTrackingCode(), trackingDto.getOrderCode()));
        sb.append(String.format("- **Đơn vị vận chuyển**: **%s** (%s)\n", trackingDto.getCarrierName(), trackingDto.getCarrier()));
        sb.append(String.format("- **Trạng thái giao**: **%s**\n", trackingDto.getShippingStatusLabel()));
        sb.append(String.format("- **Người nhận**: %s — 📞 %s\n", order.getCustomerName(), order.getCustomerPhone()));
        sb.append(String.format("- **Địa chỉ**: %s\n", order.getShippingAddress() != null ? order.getShippingAddress() : "Chưa cập nhật"));
        sb.append(String.format("- **Tiền thu hộ COD**: **%s** | Cước ship: %s\n",
                currFmt.format(trackingDto.getCodAmount() != null ? trackingDto.getCodAmount() : BigDecimal.ZERO),
                currFmt.format(trackingDto.getShippingFee() != null ? trackingDto.getShippingFee() : BigDecimal.ZERO)));

        sb.append("\n📍 **TIẾN TRÌNH CẬP NHẬT GẦN NHẤT**:\n");
        if (trackingDto.getEvents() != null && !trackingDto.getEvents().isEmpty()) {
            for (TrackingEventDto ev : trackingDto.getEvents()) {
                String mark = ev.isCompleted() ? "✅" : "⏳";
                sb.append(String.format("%s `%s` — **%s** (%s): %s\n",
                        mark,
                        ev.getTimestamp() != null ? ev.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm dd/MM")) : "--:--",
                        ev.getTitle(),
                        ev.getLocation() != null ? ev.getLocation() : "Trạm trung chuyển",
                        ev.getDescription()));
            }
        }

        Map<String, Object> resData = new LinkedHashMap<>();
        resData.put("trackingCode", trackingDto.getTrackingCode());
        resData.put("shippingStatus", trackingDto.getShippingStatus());

        return ToolExecutionResult.success(getToolName(), sb.toString(), resData, widget);
    }
}
