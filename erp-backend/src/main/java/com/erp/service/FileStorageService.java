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
    private static final List<String> ALLOWED_AUDIO_EXTENSIONS = Arrays.asList(".webm", ".ogg", ".mp3", ".wav", ".m4a", ".aac");
    private static final List<String> ALLOWED_DOC_EXTENSIONS = Arrays.asList(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".zip", ".rar", ".7z", ".tar", ".gz", ".txt", ".csv", ".json", ".xml"
    );
    private static final long MAX_IMAGE_SIZE = 15 * 1024 * 1024; // 15MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB
    private static final long MAX_AUDIO_SIZE = 25 * 1024 * 1024; // 25MB
    private static final long MAX_DOC_SIZE = 50 * 1024 * 1024; // 50MB

    public String uploadImage(MultipartFile file) throws IOException {
        return (String) uploadMedia(file).get("url");
    }

    public Map<String, Object> uploadMedia(MultipartFile file) throws IOException {
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
        boolean isAudio = ALLOWED_AUDIO_EXTENSIONS.stream().anyMatch(lower::endsWith);
        boolean isDoc = ALLOWED_DOC_EXTENSIONS.stream().anyMatch(lower::endsWith);

        if (!isImage && !isVideo && !isAudio && !isDoc) {
            throw new IllegalArgumentException("Định dạng tệp không được hỗ trợ! Chấp nhận Ảnh, Video, Âm thanh và Tài liệu (.pdf, .docx, .xlsx, .pptx, .zip, .rar, .txt, .csv...)");
        }

        if (isImage && file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Dung lượng ảnh vượt quá giới hạn 15MB!");
        }

        if (isVideo && file.getSize() > MAX_VIDEO_SIZE) {
            throw new IllegalArgumentException("Dung lượng video vượt quá giới hạn 100MB!");
        }

        if (isAudio && file.getSize() > MAX_AUDIO_SIZE) {
            throw new IllegalArgumentException("Dung lượng tệp âm thanh/ghi âm vượt quá giới hạn 25MB!");
        }

        if (isDoc && file.getSize() > MAX_DOC_SIZE) {
            throw new IllegalArgumentException("Dung lượng tệp tài liệu vượt quá giới hạn 50MB!");
        }

        String resourceType = (isVideo || isAudio) ? "video" : (isImage ? "image" : "raw");
        String folder = isVideo ? "erp_videos" : (isAudio ? "erp_voices" : (isImage ? "erp_images" : "erp_documents"));
        String sanitizedName = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String prefix = isVideo ? "video_" : (isAudio ? "voice_" : (isImage ? "media_" : "doc_"));
        String publicId = prefix + UUID.randomUUID().toString() + "_" + sanitizedName;

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", folder,
                "public_id", publicId,
                "resource_type", resourceType
        ));

        String secureUrl = (String) uploadResult.get("secure_url");
        String typeStr = isVideo ? "VIDEO" : (isAudio ? "VOICE" : (isImage ? "IMAGE" : "FILE"));

        return Map.of(
                "url", secureUrl,
                "resourceType", typeStr,
                "fileName", originalFilename,
                "fileSize", file.getSize()
        );
    }
}

