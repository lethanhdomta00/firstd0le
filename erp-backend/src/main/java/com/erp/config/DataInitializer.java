package com.erp.config;

import com.erp.model.Announcement;
import com.erp.model.Role;
import com.erp.model.User;
import com.erp.repository.AnnouncementRepository;
import com.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AnnouncementRepository announcementRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. Đảm bảo tài khoản ADMIN
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        if (adminOpt.isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("password"));
            admin.setRole(Role.ADMIN);
            admin.setStatus("ACTIVE");
            admin.setEmail("admin@erp.com");
            userRepository.save(admin);
            log.info("Initialized default ADMIN account: admin / password");
        } else {
            User admin = adminOpt.get();
            boolean changed = false;
            if (admin.getRole() != Role.ADMIN) {
                admin.setRole(Role.ADMIN);
                changed = true;
            }
            if (!"ACTIVE".equalsIgnoreCase(admin.getStatus())) {
                admin.setStatus("ACTIVE");
                changed = true;
            }
            if (changed) {
                userRepository.save(admin);
                log.info("Updated admin account to ROLE_ADMIN and ACTIVE");
            }
        }

        // 2. Khởi tạo Thông báo / Sự kiện mẫu nếu bảng trống
        if (announcementRepository.count() == 0) {
            Announcement defaultAnnouncement = new Announcement();
            defaultAnnouncement.setTitle("Chào mừng hệ thống");
            defaultAnnouncement.setContent("🎉 Chào mừng bạn đến với ERP Cloud System! Chúc bạn một ngày làm việc hiệu quả, năng lượng và hoàn thành xuất sắc mọi mục tiêu.");
            defaultAnnouncement.setType("EVENT");
            defaultAnnouncement.setActive(true);
            announcementRepository.save(defaultAnnouncement);
            log.info("Initialized default active system announcement");
        }
    }
}
