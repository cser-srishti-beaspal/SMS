package com.stationery.request.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action; // WHAT (e.g. USER_REGISTRATION, REQUEST_CREATED, ITEM_CREATED)

    @Column(name = "performed_by", nullable = false)
    private String performedBy; // WHO (username)

    @Column(name = "user_role", nullable = false)
    private String userRole; // WHO'S ROLE (e.g. STUDENT, ADMIN)

    @Column(nullable = false, length = 1000)
    private String details; // WHY/DETAILS (reason/remarks)

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime; // WHEN CREATED

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime; // WHEN UPDATED

    public AuditLog() {}

    public AuditLog(String action, String performedBy, String userRole, String details, LocalDateTime createdTime, LocalDateTime updatedTime) {
        this.action = action;
        this.performedBy = performedBy;
        this.userRole = userRole;
        this.details = details;
        this.createdTime = createdTime != null ? createdTime : LocalDateTime.now();
        this.updatedTime = updatedTime != null ? updatedTime : LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
