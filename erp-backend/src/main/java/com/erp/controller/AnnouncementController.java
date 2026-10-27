package com.erp.controller;

import com.erp.model.Announcement;
import com.erp.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;

    // GET /api/announcements/active -> Lấy thông báo đang hoạt động để chạy trên Header banner
    @GetMapping("/active")
    public ResponseEntity<?> getActiveAnnouncement() {
        return ResponseEntity.ok(announcementRepository.findFirstByActiveTrueOrderByUpdatedAtDesc().orElse(null));
    }

    // GET /api/announcements -> Danh sách tất cả thông báo (Chỉ ADMIN)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    // POST /api/announcements -> Tạo thông báo mới (Chỉ ADMIN)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createAnnouncement(@RequestBody Announcement announcement) {
        if (announcement.getContent() == null || announcement.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nội dung thông báo không được để trống!"));
        }
        if (announcement.getTitle() == null || announcement.getTitle().trim().isEmpty()) {
            announcement.setTitle("Thông báo hệ thống");
        }
        if (announcement.isActive()) {
            // Tắt active các thông báo khác nếu thông báo này bật
            List<Announcement> all = announcementRepository.findAll();
            for (Announcement a : all) {
                if (a.isActive()) {
                    a.setActive(false);
                    announcementRepository.save(a);
                }
            }
        }
        Announcement saved = announcementRepository.save(announcement);
        return ResponseEntity.ok(saved);
    }

    // PUT /api/announcements/{id} -> Cập nhật thông báo (Chỉ ADMIN)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement req) {
        return announcementRepository.findById(id)
                .map(announcement -> {
                    announcement.setTitle(req.getTitle());
                    announcement.setContent(req.getContent());
                    announcement.setType(req.getType());
                    if (req.isActive() && !announcement.isActive()) {
                        List<Announcement> all = announcementRepository.findAll();
                        for (Announcement a : all) {
                            if (a.isActive() && !a.getId().equals(id)) {
                                a.setActive(false);
                                announcementRepository.save(a);
                            }
                        }
                    }
                    announcement.setActive(req.isActive());
                    return ResponseEntity.ok(announcementRepository.save(announcement));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/announcements/{id}/toggle-active -> Bật/Tắt hiển thị lên Header banner (Chỉ ADMIN)
    @PutMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleActive(@PathVariable Long id) {
        return announcementRepository.findById(id)
                .map(announcement -> {
                    boolean targetState = !announcement.isActive();
                    if (targetState) {
                        List<Announcement> all = announcementRepository.findAll();
                        for (Announcement a : all) {
                            if (a.isActive() && !a.getId().equals(id)) {
                                a.setActive(false);
                                announcementRepository.save(a);
                            }
                        }
                    }
                    announcement.setActive(targetState);
                    announcementRepository.save(announcement);
                    return ResponseEntity.ok(Map.of(
                            "message", targetState ? "Đã kích hoạt hiển thị thông báo lên Banner Header!" : "Đã tắt hiển thị thông báo!",
                            "active", targetState
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/announcements/{id} -> Xóa thông báo (Chỉ ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id) {
        return announcementRepository.findById(id)
                .map(announcement -> {
                    announcementRepository.delete(announcement);
                    return ResponseEntity.ok(Map.of("message", "Đã xóa thông báo thành công!"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

