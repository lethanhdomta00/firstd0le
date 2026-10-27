package com.erp.repository;

import com.erp.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    Optional<Announcement> findFirstByActiveTrueOrderByUpdatedAtDesc();
    List<Announcement> findAllByOrderByCreatedAtDesc();
}

