package com.erp.config;

import com.erp.model.Announcement;
import com.erp.model.Role;
import com.erp.model.User;
import com.erp.repository.AnnouncementRepository;
import com.erp.repository.UserRepository;
import com.erp.scheduler.model.JobStatus;
import com.erp.scheduler.model.ScheduledJob;
import com.erp.scheduler.repository.ScheduledJobRepository;
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
    private final ScheduledJobRepository scheduledJobRepository;
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
            admin.setEmail("dolemta090899@gmail.com");
            userRepository.save(admin);
            log.info("Initialized default ADMIN account: admin / password (dolemta090899@gmail.com)");
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
            if (admin.getEmail() == null || admin.getEmail().isEmpty() || "admin@erp.com".equals(admin.getEmail())) {
                admin.setEmail("dolemta090899@gmail.com");
                changed = true;
            }
            if (changed) {
                userRepository.save(admin);
                log.info("Updated admin account with active status and real email (dolemta090899@gmail.com)");
            }
        }

        // 2. Khởi tạo tài khoản Nhân viên (STAFF) mẫu để test Chat Real-Time
        Optional<User> staffOpt = userRepository.findByUsername("nhanvien");
        if (staffOpt.isEmpty()) {
            User staff = new User();
            staff.setUsername("nhanvien");
            staff.setFullName("Nguyễn Văn A (Kinh doanh)");
            staff.setPassword(passwordEncoder.encode("password"));
            staff.setRole(Role.USER);
            staff.setStatus("ACTIVE");
            staff.setEmail("nhanvien@erp.com");
            staff.setPhone("0987654321");
            staff.setAvatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80");
            userRepository.save(staff);
            log.info("Initialized default STAFF account for testing: nhanvien / password");
        }

        // 3. Đảm bảo tài khoản HIHI (nếu có) có mật khẩu chuẩn là 'password'
        Optional<User> hihiOpt = userRepository.findByUsername("HIHI");
        if (hihiOpt.isPresent()) {
            User hihi = hihiOpt.get();
            hihi.setPassword(passwordEncoder.encode("password"));
            hihi.setStatus("ACTIVE");
            if (hihi.getFullName() == null || hihi.getFullName().isEmpty()) {
                hihi.setFullName("Trần Thị B (Kế toán)");
            }
            userRepository.save(hihi);
            log.info("Updated HIHI password to: password");
        }

        // 4. Khởi tạo Thông báo / Sự kiện mẫu nếu bảng trống
        if (announcementRepository.count() == 0) {
            Announcement defaultAnnouncement = new Announcement();
            defaultAnnouncement.setTitle("Chào mừng hệ thống");
            defaultAnnouncement.setContent("🎉 Chào mừng bạn đến với ERP Cloud System! Chúc bạn một ngày làm việc hiệu quả, năng lượng và hoàn thành xuất sắc mọi mục tiêu.");
            defaultAnnouncement.setType("EVENT");
            defaultAnnouncement.setActive(true);
            announcementRepository.save(defaultAnnouncement);
            log.info("Initialized default active system announcement");
        }

        // 5. Khởi tạo 3 Tác vụ Lập lịch Động (Dynamic Scheduler Jobs) mẫu chuẩn Doanh nghiệp
        initDefaultSchedulerJobs();
    }

    private void initDefaultSchedulerJobs() {
        if (!scheduledJobRepository.existsByJobCode("JOB_INVENTORY_ALERT")) {
            ScheduledJob inventoryJob = ScheduledJob.builder()
                    .jobCode("JOB_INVENTORY_ALERT")
                    .jobName("Quét & Cảnh báo Tồn kho an toàn")
                    .jobGroup("INVENTORY")
                    .description("Tự động rà soát hàng tồn kho dưới ngưỡng an toàn và gửi cảnh báo tức thì về Telegram Quản lý")
                    .cronExpression("0 0 9,14 * * ?") // 9h sáng và 14h chiều mỗi ngày
                    .jobType("INVENTORY_AUDIT")
                    .status(JobStatus.ACTIVE)
                    .paramsJson("{\"threshold\": 10, \"notifyTelegram\": true}")
                    .build();
            scheduledJobRepository.save(inventoryJob);
            log.info("Initialized default enterprise job: JOB_INVENTORY_ALERT");
        }

        if (!scheduledJobRepository.existsByJobCode("JOB_DAILY_SALES")) {
            ScheduledJob salesJob = ScheduledJob.builder()
                    .jobCode("JOB_DAILY_SALES")
                    .jobName("Tổng hợp Báo cáo Kho vận & Giá trị hàng hóa")
                    .jobGroup("FINANCE")
                    .description("Tổng hợp tổng giá trị tài sản hàng hóa trong kho và các mặt hàng hết hàng gửi báo cáo cuối ngày")
                    .cronExpression("0 0 18 * * ?") // 18:00 chiều mỗi ngày
                    .jobType("DAILY_SALES_REPORT")
                    .status(JobStatus.ACTIVE)
                    .paramsJson("{\"notifyTelegram\": true}")
                    .build();
            scheduledJobRepository.save(salesJob);
            log.info("Initialized default enterprise job: JOB_DAILY_SALES");
        }

        if (!scheduledJobRepository.existsByJobCode("JOB_CLEANUP_TEMP")) {
            ScheduledJob cleanupJob = ScheduledJob.builder()
                    .jobCode("JOB_CLEANUP_TEMP")
                    .jobName("Dọn dẹp File đệm & Tối ưu hóa dữ liệu rác")
                    .jobGroup("SYSTEM")
                    .description("Tự động giải phóng các tệp tin tạm trên ổ đĩa có tuổi thọ trên 7 ngày để tối ưu dung lượng hệ thống")
                    .cronExpression("0 0 2 * * ?") // 2:00 sáng mỗi ngày
                    .jobType("CLEANUP_TEMP_FILES")
                    .status(JobStatus.ACTIVE)
                    .paramsJson("{\"maxAgeDays\": 7}")
                    .build();
            scheduledJobRepository.save(cleanupJob);
            log.info("Initialized default enterprise job: JOB_CLEANUP_TEMP");
        }
    }
}
