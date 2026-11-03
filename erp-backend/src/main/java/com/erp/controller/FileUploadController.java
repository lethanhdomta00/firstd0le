package com.erp.controller;

import com.erp.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = fileStorageService.uploadMedia(file);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "url", result.get("url"),
                    "resourceType", result.get("resourceType"),
                    "fileName", result.get("fileName") != null ? result.get("fileName") : "",
                    "fileSize", result.get("fileSize") != null ? result.get("fileSize") : 0
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "message", "Lỗi trong quá trình tải tệp lên Cloud: " + e.getMessage()
            ));
        }
    }
}

