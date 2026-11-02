package com.erp.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList(".mp4", ".webm", ".mov", ".mkv", ".avi");
    private static final long MAX_IMAGE_SIZE = 15 * 1024 * 1024; // 15MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB

    public String uploadImage(MultipartFile file) throws IOException {
        return uploadMedia(file).get("url");
    }

    public Map<String, String> uploadMedia(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống!");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Tên tệp không hợp lệ!");
        }

        String lower = originalFilename.toLowerCase();
        boolean isImage = ALLOWED_IMAGE_EXTENSIONS.stream().anyMatch(lower::endsWith);
        boolean isVideo = ALLOWED_VIDEO_EXTENSIONS.stream().anyMatch(lower::endsWith);

        if (!isImage && !isVideo) {
            throw new IllegalArgumentException("Định dạng tệp không được hỗ trợ! Chỉ chấp nhận Ảnh (JPG, PNG, WEBP, GIF) hoặc Video (MP4, WEBM, MOV, MKV, AVI)");
        }

        if (isImage && file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Dung lượng ảnh vượt quá giới hạn 15MB!");
        }

        if (isVideo && file.getSize() > MAX_VIDEO_SIZE) {
            throw new IllegalArgumentException("Dung lượng video vượt quá giới hạn 100MB!");
        }

        String resourceType = isVideo ? "video" : "image";
        String publicId = (isVideo ? "video_" : "media_") + UUID.randomUUID().toString();

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", isVideo ? "erp_videos" : "erp_images",
                "public_id", publicId,
                "resource_type", resourceType
        ));

        String secureUrl = (String) uploadResult.get("secure_url");
        return Map.of(
                "url", secureUrl,
                "resourceType", isVideo ? "VIDEO" : "IMAGE"
        );
    }
}

