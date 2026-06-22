package com.stationery.request.repository;

import com.stationery.request.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit logs sorted by ID in descending order (newest first).
     */
    List<AuditLog> findAllByOrderByIdDesc();
}
