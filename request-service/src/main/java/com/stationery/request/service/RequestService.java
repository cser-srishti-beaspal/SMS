package com.stationery.request.service;

import com.stationery.request.client.InventoryClient;
import com.stationery.request.dto.CreateRequestDto;
import com.stationery.request.dto.RequestItemDto;
import com.stationery.request.dto.RequestResponse;
import com.stationery.request.exception.InsufficientStockException;
import com.stationery.request.exception.ResourceNotFoundException;
import com.stationery.request.model.RequestItem;
import com.stationery.request.model.RequestStatus;
import com.stationery.request.model.StationeryRequest;
import com.stationery.request.repository.RequestRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RequestService {

    private static final Logger log = LoggerFactory.getLogger(RequestService.class);

    private final RequestRepository requestRepository;
    private final InventoryClient inventoryClient;

    public RequestService(RequestRepository requestRepository, InventoryClient inventoryClient) {
        this.requestRepository = requestRepository;
        this.inventoryClient = inventoryClient;
    }

    /**
     * Create a new stationery request with PENDING status.
     */
    @Transactional
    public RequestResponse createRequest(String username, CreateRequestDto createRequestDto) {
        log.info("AUDIT: Creating new stationery request for student: {}", username);

        StationeryRequest request = StationeryRequest.builder()
                .studentUsername(username)
                .status(RequestStatus.PENDING)
                .build();

        // Add each item to the request
        for (RequestItemDto itemDto : createRequestDto.getItems()) {
            RequestItem item = RequestItem.builder()
                    .itemId(itemDto.getItemId())
                    .itemName(itemDto.getItemName())
                    .quantity(itemDto.getQuantity())
                    .build();
            request.addItem(item);
        }

        StationeryRequest savedRequest = requestRepository.save(request);
        log.info("AUDIT: Stationery request created successfully. RequestId: {}, Student: {}, Items: {}",
                savedRequest.getRequestId(), username, createRequestDto.getItems().size());

        return mapToResponse(savedRequest);
    }

    /**
     * Get a request by its database ID.
     */
    @Transactional(readOnly = true)
    public RequestResponse getRequestById(Long id) {
        log.debug("Fetching request by ID: {}", id);
        StationeryRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "id", id));
        return mapToResponse(request);
    }

    /**
     * Get a request by its UUID-based request ID.
     */
    @Transactional(readOnly = true)
    public RequestResponse getRequestByRequestId(String requestId) {
        log.debug("Fetching request by requestId: {}", requestId);
        StationeryRequest request = requestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "requestId", requestId));
        return mapToResponse(request);
    }

    /**
     * Get all requests for a specific student.
     */
    @Transactional(readOnly = true)
    public List<RequestResponse> getRequestsByStudent(String username) {
        log.debug("Fetching requests for student: {}", username);
        List<StationeryRequest> requests = requestRepository.findByStudentUsername(username);
        return requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get requests for a specific student filtered by status.
     */
    @Transactional(readOnly = true)
    public List<RequestResponse> getRequestsByStudentAndStatus(String username, String status) {
        log.debug("Fetching requests for student: {} with status: {}", username, status);
        RequestStatus requestStatus = parseStatus(status);
        List<StationeryRequest> requests = requestRepository.findByStudentUsernameAndStatus(username, requestStatus);
        return requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all requests (Admin only).
     */
    @Transactional(readOnly = true)
    public List<RequestResponse> getAllRequests() {
        log.debug("Fetching all requests (admin)");
        List<StationeryRequest> requests = requestRepository.findAll();
        return requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all requests filtered by status (Admin only).
     */
    @Transactional(readOnly = true)
    public List<RequestResponse> getAllRequestsByStatus(String status) {
        log.debug("Fetching all requests with status: {} (admin)", status);
        RequestStatus requestStatus = parseStatus(status);
        List<StationeryRequest> requests = requestRepository.findByStatus(requestStatus);
        return requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Approve a request: change status to APPROVED and deduct inventory quantities.
     */
    @Transactional
    public RequestResponse approveRequest(Long id, String adminUsername) {
        log.info("AUDIT: Admin '{}' approving request ID: {}", adminUsername, id);

        StationeryRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "id", id));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Request can only be approved when in PENDING status. Current status: " + request.getStatus());
        }

        // Deduct inventory for each item
        for (RequestItem item : request.getItems()) {
            try {
                log.info("AUDIT: Deducting {} units of item '{}' (ID: {}) from inventory",
                        item.getQuantity(), item.getItemName(), item.getItemId());
                inventoryClient.deductItemQuantity(item.getItemId(), item.getQuantity());
            } catch (FeignException.BadRequest e) {
                log.error("AUDIT: Insufficient stock for item '{}' (ID: {}). Approval failed.",
                        item.getItemName(), item.getItemId());
                throw new InsufficientStockException(item.getItemName(), item.getQuantity());
            } catch (FeignException e) {
                log.error("AUDIT: Failed to deduct inventory for item '{}' (ID: {}): {}",
                        item.getItemName(), item.getItemId(), e.getMessage());
                throw new RuntimeException("Failed to deduct inventory for item: " + item.getItemName(), e);
            }
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setAdminUsername(adminUsername);
        StationeryRequest savedRequest = requestRepository.save(request);

        log.info("AUDIT: Request ID: {} approved by admin '{}'. All inventory deductions successful.",
                id, adminUsername);

        return mapToResponse(savedRequest);
    }

    /**
     * Reject a request: change status to REJECTED with a reason.
     */
    @Transactional
    public RequestResponse rejectRequest(Long id, String adminUsername, String reason) {
        log.info("AUDIT: Admin '{}' rejecting request ID: {} with reason: '{}'", adminUsername, id, reason);

        StationeryRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "id", id));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Request can only be rejected when in PENDING status. Current status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setRejectionReason(reason);
        request.setAdminUsername(adminUsername);
        StationeryRequest savedRequest = requestRepository.save(request);

        log.info("AUDIT: Request ID: {} rejected by admin '{}'.", id, adminUsername);

        return mapToResponse(savedRequest);
    }

    /**
     * Fulfill a request: change status from APPROVED to FULFILLED.
     */
    @Transactional
    public RequestResponse fulfillRequest(Long id) {
        log.info("AUDIT: Fulfilling request ID: {}", id);

        StationeryRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "id", id));

        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new IllegalStateException(
                    "Request can only be fulfilled when in APPROVED status. Current status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.FULFILLED);
        StationeryRequest savedRequest = requestRepository.save(request);

        log.info("AUDIT: Request ID: {} fulfilled successfully.", id);

        return mapToResponse(savedRequest);
    }

    // ========== Helper Methods ==========

    private RequestStatus parseStatus(String status) {
        try {
            return RequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request status: " + status
                    + ". Valid values are: PENDING, APPROVED, REJECTED, FULFILLED");
        }
    }

    private RequestResponse mapToResponse(StationeryRequest request) {
        List<RequestItemDto> itemDtos = request.getItems().stream()
                .map(item -> RequestItemDto.builder()
                        .itemId(item.getItemId())
                        .itemName(item.getItemName())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return RequestResponse.builder()
                .id(request.getId())
                .requestId(request.getRequestId())
                .studentUsername(request.getStudentUsername())
                .items(itemDtos)
                .status(request.getStatus().name())
                .rejectionReason(request.getRejectionReason())
                .adminUsername(request.getAdminUsername())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
