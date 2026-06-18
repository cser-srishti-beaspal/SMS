package com.stationery.request.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stationery_requests")
public class StationeryRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", unique = true, nullable = false, updatable = false)
    private String requestId;

    @NotBlank(message = "Student username is required")
    @Column(name = "student_username", nullable = false)
    private String studentUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "admin_username")
    private String adminUsername;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RequestItem> items = new ArrayList<>();

    public StationeryRequest() {}

    public StationeryRequest(Long id, String requestId, String studentUsername, RequestStatus status, String rejectionReason, String adminUsername, LocalDateTime createdAt, LocalDateTime updatedAt, List<RequestItem> items) {
        this.id = id;
        this.requestId = requestId;
        this.studentUsername = studentUsername;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.adminUsername = adminUsername;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = items != null ? items : new ArrayList<>();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getStudentUsername() { return studentUsername; }
    public void setStudentUsername(String studentUsername) { this.studentUsername = studentUsername; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<RequestItem> getItems() { return items; }
    public void setItems(List<RequestItem> items) { this.items = items; }

    @PrePersist
    protected void onCreate() {
        if (this.requestId == null || this.requestId.isEmpty()) {
            this.requestId = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = RequestStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addItem(RequestItem item) {
        items.add(item);
        item.setRequest(this);
    }

    public void removeItem(RequestItem item) {
        items.remove(item);
        item.setRequest(null);
    }

    public static StationeryRequestBuilder builder() { return new StationeryRequestBuilder(); }

    public static class StationeryRequestBuilder {
        private Long id;
        private String requestId;
        private String studentUsername;
        private RequestStatus status;
        private String rejectionReason;
        private String adminUsername;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<RequestItem> items = new ArrayList<>();

        public StationeryRequestBuilder id(Long id) { this.id = id; return this; }
        public StationeryRequestBuilder requestId(String requestId) { this.requestId = requestId; return this; }
        public StationeryRequestBuilder studentUsername(String studentUsername) { this.studentUsername = studentUsername; return this; }
        public StationeryRequestBuilder status(RequestStatus status) { this.status = status; return this; }
        public StationeryRequestBuilder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }
        public StationeryRequestBuilder adminUsername(String adminUsername) { this.adminUsername = adminUsername; return this; }
        public StationeryRequestBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public StationeryRequestBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public StationeryRequestBuilder items(List<RequestItem> items) { this.items = items; return this; }

        public StationeryRequest build() {
            return new StationeryRequest(id, requestId, studentUsername, status, rejectionReason, adminUsername, createdAt, updatedAt, items);
        }
    }
}
