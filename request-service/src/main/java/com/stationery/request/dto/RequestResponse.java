package com.stationery.request.dto;

import java.time.LocalDateTime;
import java.util.List;

public class RequestResponse {

    private Long id;
    private String requestId;
    private String studentUsername;
    private List<RequestItemDto> items;
    private String status;
    private String rejectionReason;
    private String adminUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RequestResponse() {}

    public RequestResponse(Long id, String requestId, String studentUsername, List<RequestItemDto> items, String status, String rejectionReason, String adminUsername, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.requestId = requestId;
        this.studentUsername = studentUsername;
        this.items = items;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.adminUsername = adminUsername;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getStudentUsername() { return studentUsername; }
    public void setStudentUsername(String studentUsername) { this.studentUsername = studentUsername; }

    public List<RequestItemDto> getItems() { return items; }
    public void setItems(List<RequestItemDto> items) { this.items = items; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static RequestResponseBuilder builder() { return new RequestResponseBuilder(); }

    public static class RequestResponseBuilder {
        private Long id;
        private String requestId;
        private String studentUsername;
        private List<RequestItemDto> items;
        private String status;
        private String rejectionReason;
        private String adminUsername;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public RequestResponseBuilder id(Long id) { this.id = id; return this; }
        public RequestResponseBuilder requestId(String requestId) { this.requestId = requestId; return this; }
        public RequestResponseBuilder studentUsername(String studentUsername) { this.studentUsername = studentUsername; return this; }
        public RequestResponseBuilder items(List<RequestItemDto> items) { this.items = items; return this; }
        public RequestResponseBuilder status(String status) { this.status = status; return this; }
        public RequestResponseBuilder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }
        public RequestResponseBuilder adminUsername(String adminUsername) { this.adminUsername = adminUsername; return this; }
        public RequestResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public RequestResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public RequestResponse build() {
            return new RequestResponse(id, requestId, studentUsername, items, status, rejectionReason, adminUsername, createdAt, updatedAt);
        }
    }
}
