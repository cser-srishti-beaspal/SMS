package com.stationery.request.dto;

public class ApproveRejectDto {

    private String rejectionReason;

    public ApproveRejectDto() {}

    public ApproveRejectDto(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public static ApproveRejectDtoBuilder builder() { return new ApproveRejectDtoBuilder(); }

    public static class ApproveRejectDtoBuilder {
        private String rejectionReason;

        public ApproveRejectDtoBuilder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }

        public ApproveRejectDto build() {
            return new ApproveRejectDto(rejectionReason);
        }
    }
}
