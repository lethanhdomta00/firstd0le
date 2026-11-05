package com.erp.scheduler.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Slf4j
@RequiredArgsConstructor
public class CleanupTempFilesTaskHandler implements ScheduledTaskHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getJobType() {
        return "CLEANUP_TEMP_FILES";
    }

    @Override
    public String getDisplayName() {
        return "Dọn dẹp File đệm & Tối ưu hóa dữ liệu rác";
    }

    @Override
    public String execute(String paramsJson, String triggeredBy) throws Exception {
        int maxAgeDays = 7;
        if (paramsJson != null && !paramsJson.trim().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(paramsJson);
                if (root.has("maxAgeDays")) {
                    maxAgeDays = root.get("maxAgeDays").asInt(7);
                }
            } catch (Exception e) {
                log.warn("Lỗi parse paramsJson cho CLEANUP_TEMP_FILES: {}", e.getMessage());
            }
        }

        // Quét thư mục tạm nếu có
        int cleanedFiles = 0;
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "erp_uploads");
            if (Files.exists(tempDir)) {
                File[] files = tempDir.toFile().listFiles();
                if (files != null) {
                    long cutoff = System.currentTimeMillis() - (maxAgeDays * 24L * 60 * 60 * 1000);
                    for (File f : files) {
                        if (f.isFile() && f.lastModified() < cutoff) {
                            if (f.delete()) {
                                cleanedFiles++;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể xóa thư mục tạm: {}", e.getMessage());
        }

        String summary = String.format("Hoàn tất bảo trì hệ thống. Đã giải phóng %d tệp tin tạm (tuổi thọ > %d ngày).",
                cleanedFiles, maxAgeDays);
        log.info("CLEANUP_TEMP_FILES hoàn thành: {}", summary);
        return summary;
    }
}

