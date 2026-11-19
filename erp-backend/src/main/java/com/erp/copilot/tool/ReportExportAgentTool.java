package com.erp.copilot.tool;

import com.erp.copilot.dto.ToolExecutionResult;
import com.erp.copilot.dto.UiWidgetDto;
import com.erp.copilot.service.CsvReportGeneratorService;
import com.erp.copilot.service.ExcelReportGeneratorService;
import com.erp.copilot.service.TempReportCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportExportAgentTool implements AgentTool {

    private final ExcelReportGeneratorService excelReportGeneratorService;
    private final CsvReportGeneratorService csvReportGeneratorService;
    private final TempReportCacheManager tempReportCacheManager;

    @Override
    public String getToolName() {
        return "generateExportReport";
    }

    @Override
    public String getDescription() {
        return "Tự động trích xuất dữ liệu và tạo file báo cáo định dạng Excel (.xlsx), CSV (.csv) hoặc PDF theo yêu cầu (Doanh thu, Dự báo cạn kho, Công nợ đại lý).";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> reportType = new HashMap<>();
        reportType.put("type", "string");
        reportType.put("enum", Arrays.asList("SALES_REVENUE", "INVENTORY_RESTOCK", "DEBT_OVERDUE"));
        reportType.put("description", "Loại báo cáo cần xuất: SALES_REVENUE (Doanh thu), INVENTORY_RESTOCK (Dự báo cạn kho), DEBT_OVERDUE (Tuổi nợ đại lý)");
        properties.put("reportType", reportType);

        Map<String, Object> fileFormat = new HashMap<>();
        fileFormat.put("type", "string");
        fileFormat.put("enum", Arrays.asList("EXCEL", "CSV"));
        fileFormat.put("description", "Định dạng file: EXCEL (.xlsx) hoặc CSV (.csv)");
        properties.put("fileFormat", fileFormat);

        Map<String, Object> timeRange = new HashMap<>();
        timeRange.put("type", "string");
        timeRange.put("enum", Arrays.asList("TODAY", "THIS_WEEK", "THIS_MONTH", "ALL"));
        timeRange.put("description", "Khoảng thời gian: TODAY (Hôm nay), THIS_WEEK (Tuần này), THIS_MONTH (Tháng này), ALL (Tất cả)");
        properties.put("timeRange", timeRange);

        schema.put("properties", properties);
        schema.put("required", Collections.singletonList("reportType"));
        return schema;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args, String currentUsername) {
        String reportType = String.valueOf(args.getOrDefault("reportType", "SALES_REVENUE")).toUpperCase();
        String fileFormat = String.valueOf(args.getOrDefault("fileFormat", "EXCEL")).toUpperCase();
        String timeRange = String.valueOf(args.getOrDefault("timeRange", "THIS_MONTH")).toUpperCase();

        LocalDateTime fromDate = null;
        LocalDateTime toDate = LocalDateTime.now();

        LocalDate today = LocalDate.now();
        if ("TODAY".equals(timeRange)) {
            fromDate = today.atStartOfDay();
            toDate = today.atTime(LocalTime.MAX);
        } else if ("THIS_WEEK".equals(timeRange)) {
            fromDate = today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay();
            toDate = today.atTime(LocalTime.MAX);
        } else if ("THIS_MONTH".equals(timeRange)) {
            fromDate = today.withDayOfMonth(1).atStartOfDay();
            toDate = today.atTime(LocalTime.MAX);
        }

        try {
            byte[] fileBytes;
            String fileName;
            String contentType;
            String desc;
            List<Map<String, String>> statsList = new ArrayList<>();

            if ("INVENTORY_RESTOCK".equals(reportType)) {
                fileBytes = excelReportGeneratorService.generateInventoryForecastReport();
                fileName = "Bao-cao-du-bao-can-kho-AI-" + LocalDate.now() + ".xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                desc = "Báo cáo phân tích tốc độ bán 30 ngày & dự toán ngân sách nhập hàng an toàn.";
                statsList.add(createStat("Mục tiêu", "Dự báo cạn kho & EOQ"));
                statsList.add(createStat("Dữ liệu", "Kho thực tế"));
            } else if ("DEBT_OVERDUE".equals(reportType)) {
                if ("CSV".equals(fileFormat)) {
                    fileBytes = csvReportGeneratorService.generateDebtOverdueCsv(0);
                    fileName = "Bao-cao-tuoi-no-dai-ly-" + LocalDate.now() + ".csv";
                    contentType = "text/csv;charset=utf-8";
                } else {
                    fileBytes = excelReportGeneratorService.generateDebtAgingReport();
                    fileName = "Bao-cao-tuoi-no-dai-ly-" + LocalDate.now() + ".xlsx";
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                }
                desc = "Báo cáo đối soát hạn mức tín dụng và phân loại nợ theo tuổi 30/60/90 ngày.";
                statsList.add(createStat("Phân loại", "Tuổi nợ đại lý"));
                statsList.add(createStat("Mục đích", "Đối soát & Thu hồi nợ"));
            } else {
                // SALES_REVENUE
                if ("CSV".equals(fileFormat)) {
                    fileBytes = csvReportGeneratorService.generateOrdersCsv(fromDate, toDate);
                    fileName = "Bao-cao-doanh-thu-don-hang-" + LocalDate.now() + ".csv";
                    contentType = "text/csv;charset=utf-8";
                } else {
                    fileBytes = excelReportGeneratorService.generateSalesReport(fromDate, toDate);
                    fileName = "Bao-cao-doanh-thu-kinh-doanh-" + LocalDate.now() + ".xlsx";
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                }
                desc = "Báo cáo tổng hợp tình hình kinh doanh, doanh số, thực thu và chi tiết đơn hàng.";
                statsList.add(createStat("Kỳ báo cáo", timeRange));
                statsList.add(createStat("Trạng thái", "Đã chốt số liệu"));
            }

            String fileId = tempReportCacheManager.storeReport(fileName, contentType, fileBytes);
            String downloadUrl = "/api/copilot/reports/download/" + fileId;
            String fileSizeText = TempReportCacheManager.formatSize(fileBytes.length);

            Map<String, Object> widgetData = new LinkedHashMap<>();
            widgetData.put("fileId", fileId);
            widgetData.put("fileName", fileName);
            widgetData.put("fileFormat", fileFormat);
            widgetData.put("fileSizeText", fileSizeText);
            widgetData.put("downloadUrl", downloadUrl);
            widgetData.put("summaryDescription", desc);
            widgetData.put("keyStats", statsList);

            UiWidgetDto widget = UiWidgetDto.builder()
                    .type("FILE_DOWNLOAD_CARD")
                    .data(widgetData)
                    .build();

            Map<String, Object> resultData = new LinkedHashMap<>();
            resultData.put("fileId", fileId);
            resultData.put("fileName", fileName);
            resultData.put("fileSizeText", fileSizeText);
            resultData.put("downloadUrl", downloadUrl);

            return ToolExecutionResult.success(
                    getToolName(),
                    "Đã tạo thành công file báo cáo " + fileName + " (" + fileSizeText + "). Bấm vào nút bên dưới để tải về máy!",
                    resultData,
                    widget
            );
        } catch (IOException e) {
            log.error("Lỗi khi sinh file báo cáo qua AI Copilot:", e);
            return ToolExecutionResult.failure(getToolName(), "Không thể sinh file báo cáo: " + e.getMessage());
        }
    }

    private Map<String, String> createStat(String label, String value) {
        Map<String, String> map = new HashMap<>();
        map.put("label", label);
        map.put("value", value);
        return map;
    }
}
