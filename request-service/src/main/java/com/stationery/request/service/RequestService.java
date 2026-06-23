package com.stationery.request.service;

import java.util.List;        // Feign Client — Inventory Service se baat karta hai
import java.util.stream.Collectors;          // Request banana: items ki list

import org.slf4j.Logger;            // Ek item: itemId + name + quantity
import org.slf4j.LoggerFactory;           // Client ko bheja jaane wala response
import org.springframework.stereotype.Service; // Stock kam ho toh
import org.springframework.transaction.annotation.Transactional;  // Request na mile toh

import com.stationery.request.client.InventoryClient;             // Request ke andar ek line item
import com.stationery.request.dto.CreateRequestDto;                // Kaun, kya, kab kiya — record
import com.stationery.request.dto.RequestItemDto;           // Enum: PENDING, APPROVED, REJECTED, FULFILLED
import com.stationery.request.dto.RequestResponse;       // Main request entity
import com.stationery.request.exception.InsufficientStockException; // AuditLog DB operations
import com.stationery.request.exception.ResourceNotFoundException;  // StationeryRequest DB operations
import com.stationery.request.model.AuditLog;                                 // HTTP error jo Feign client throw karta hai
import com.stationery.request.model.RequestItem;
import com.stationery.request.model.RequestStatus;
import com.stationery.request.model.StationeryRequest;
import com.stationery.request.repository.AuditLogRepository;
import com.stationery.request.repository.RequestRepository;

import feign.FeignException;

/**
 * RequestService — Stationery requests ka poora lifecycle yahan manage hota hai.
 *
 * Request ka safar:
 *   PENDING → APPROVED → FULFILLED
 *   PENDING → REJECTED
 *
 * Is service ki khaas baat:
 *   Approve karte waqt yeh Inventory Service ko Feign Client se call karta hai
 *   taaki stock automatically deduct ho jaye — do services ek kaam milke karte hain.
 */
@Service
public class RequestService {

    private static final Logger log = LoggerFactory.getLogger(RequestService.class);

    private final RequestRepository requestRepository;    // Request ka DB
    private final InventoryClient inventoryClient;        // Inventory microservice ka HTTP client
    private final AuditLogRepository auditLogRepository;  // Audit trail ka DB

    // Constructor Injection — teeno dependencies inject ho rahi hain
    public RequestService(RequestRepository requestRepository,
                          InventoryClient inventoryClient,
                          AuditLogRepository auditLogRepository) {
        this.requestRepository = requestRepository;
        this.inventoryClient = inventoryClient;
        this.auditLogRepository = auditLogRepository;
    }


    // ─────────────────────────────────────────────────────────────
    // CREATE REQUEST
    // Student ne items maange — request PENDING status mein banti hai
    // Abhi inventory touch nahi hoti — sirf request record hoti hai
    // ─────────────────────────────────────────────────────────────
    @Transactional
    public RequestResponse createRequest(String username, CreateRequestDto createRequestDto) {
        log.info("AUDIT: Creating request for student: {}", username);

        // Nayi request banao — shuru mein hamesha PENDING status
        StationeryRequest request = StationeryRequest.builder()
                .studentUsername(username)
                .status(RequestStatus.PENDING)
                .build();

        // DTO se har item uthao aur request mein add karo
        // addItem() internally request aur item ka bidirectional relationship set karta hai
        for (RequestItemDto itemDto : createRequestDto.getItems()) {
            RequestItem item = RequestItem.builder()
                    .itemId(itemDto.getItemId())
                    .itemName(itemDto.getItemName())
                    .quantity(itemDto.getQuantity())
                    .build();
            request.addItem(item); // Item request se link hoti hai — orphan nahi rehti
        }

        StationeryRequest savedRequest = requestRepository.save(request);
        log.info("AUDIT: Request created. ID: {}, Student: {}, Items: {}",
                savedRequest.getRequestId(), username, createRequestDto.getItems().size());

        // Audit trail: student ne kab, kya request kiya
        auditLogRepository.save(new AuditLog(
                "REQUEST_CREATED",
                username,
                "STUDENT",
                "Created request ID: " + savedRequest.getRequestId()
                        + " containing " + createRequestDto.getItems().size() + " items.",
                savedRequest.getCreatedAt(),
                savedRequest.getUpdatedAt()
        ));

        return mapToResponse(savedRequest);
    }


    // ─────────────────────────────────────────────────────────────
    // READ OPERATIONS
    // Multiple ways se request fetch kar sakte hain:
    //   DB ID se, UUID-based requestId se, student username se, status se
    // ─────────────────────────────────────────────────────────────

    // Internal DB ID se fetch (Long) — direct DB lookup
    @Transactional(readOnly = true)
    public RequestResponse getRequestById(Long id) {
        log.debug("Fetching request by ID: {}", id);
        StationeryRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "id", id));
        return mapToResponse(request);
    }

    // UUID-based requestId se fetch (String) — client-facing ID
    // DB ID expose nahi karte client ko — UUID zyada secure hai
    @Transactional(readOnly = true)
    public RequestResponse getRequestByRequestId(String requestId) {
        log.debug("Fetching request by requestId: {}", requestId);
        StationeryRequest request = requestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "requestId", requestId));
        return mapToResponse(request);
    }

    // Student ki saari requests (koi bhi status)
    @Transactional(readOnly = true)
    public List<RequestResponse> getRequestsByStudent(String username) {
        log.debug("Fetching all requests for student: {}", username);
        return requestRepository.findByStudentUsername(username)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Student ki requests — specific status se filter karke
    @Transactional(readOnly = true)
    public List<RequestResponse> getRequestsByStudentAndStatus(String username, String status) {
        log.debug("Fetching requests for student: {} with status: {}", username, status);
        RequestStatus requestStatus = parseStatus(status); // String → Enum convert karo
        return requestRepository.findByStudentUsernameAndStatus(username, requestStatus)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Admin: saari requests bina filter ke
    @Transactional(readOnly = true)
    public List<RequestResponse> getAllRequests() {
        log.debug("Fetching all requests (admin)");
        return requestRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Admin: status se filter karke saari requests
    @Transactional(readOnly = true)
    public List<RequestResponse> getAllRequestsByStatus(String status) {
        log.debug("Fetching all requests with status: {} (admin)", status);
        RequestStatus requestStatus = parseStatus(status);
        return requestRepository.findByStatus(requestStatus)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    // ─────────────────────────────────────────────────────────────
    // APPROVE REQUEST
    // Yeh is service ka sabse important aur complex method hai.
    //
    // Do kaam ek saath hone chahiye:
    //   1. Request status APPROVED ho
    //   2. Inventory Service mein stock deduct ho
    //
    // @Transactional: agar inventory deduct fail ho → request bhi save nahi hogi
    // ─────────────────────────────────────────────────────────────
    @Transactional
    public RequestResponse approveRequest(Long id, String adminUsername) {
        log.info("AUDIT: Admin '{}' approving request ID: {}", adminUsername, id);

        StationeryRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "id", id));

        // Sirf PENDING request approve ho sakti hai — already approved/rejected ko nahi
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Request can only be approved when PENDING. Current: " + request.getStatus());
        }

        // Har item ke liye Inventory Service ko HTTP call karo (Feign Client se)
        for (RequestItem item : request.getItems()) {
            try {
                log.info("AUDIT: Deducting {} units of '{}' (ID: {}) from inventory",
                        item.getQuantity(), item.getItemName(), item.getItemId());

                // Feign Client → Inventory Service ka REST endpoint call karta hai
                // Internally: POST /api/inventory/{itemId}/deduct?quantity=X
                inventoryClient.deductItemQuantity(item.getItemId(), item.getQuantity());

            } catch (FeignException.BadRequest e) {
                // 400 Bad Request = Inventory Service ne stock kam hone ki error di
                log.error("AUDIT: Insufficient stock for '{}' (ID: {}). Approval failed.",
                        item.getItemName(), item.getItemId());
                throw new InsufficientStockException(item.getItemName(), item.getQuantity());

            } catch (FeignException e) {
                // Koi aur HTTP error — Inventory Service down hai ya network issue
                log.error("AUDIT: Failed to deduct inventory for '{}': {}",
                        item.getItemName(), e.getMessage());
                throw new RuntimeException("Failed to deduct inventory for: " + item.getItemName(), e);
            }
        }

        // Saare items deduct ho gaye — ab status update karo
        request.setStatus(RequestStatus.APPROVED);
        request.setAdminUsername(adminUsername);
        StationeryRequest savedRequest = requestRepository.save(request);

        log.info("AUDIT: Request ID: {} approved by '{}'. All deductions successful.", id, adminUsername);

        auditLogRepository.save(new AuditLog(
                "REQUEST_APPROVED",
                adminUsername,
                "ADMIN",
                "Approved request ID: " + savedRequest.getRequestId()
                        + " for student: " + savedRequest.getStudentUsername(),
                savedRequest.getCreatedAt(),
                savedRequest.getUpdatedAt()
        ));

        return mapToResponse(savedRequest);
    }


    // ─────────────────────────────────────────────────────────────
    // REJECT REQUEST
    // PENDING → REJECTED
    // Inventory touch nahi hoti — sirf status aur reason save hota hai
    // ─────────────────────────────────────────────────────────────
    @Transactional
    public RequestResponse rejectRequest(Long id, String adminUsername, String reason) {
        log.info("AUDIT: Admin '{}' rejecting request ID: {} reason: '{}'", adminUsername, id, reason);

        StationeryRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "id", id));

        // Sirf PENDING reject ho sakti hai
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Request can only be rejected when PENDING. Current: " + request.getStatus());
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setRejectionReason(reason);      // Student ko batao kyun reject hua
        request.setAdminUsername(adminUsername);
        StationeryRequest savedRequest = requestRepository.save(request);

        log.info("AUDIT: Request ID: {} rejected by '{}'.", id, adminUsername);

        auditLogRepository.save(new AuditLog(
                "REQUEST_REJECTED",
                adminUsername,
                "ADMIN",
                "Rejected request ID: " + savedRequest.getRequestId()
                        + " for student: " + savedRequest.getStudentUsername()
                        + ". Reason: " + reason,
                savedRequest.getCreatedAt(),
                savedRequest.getUpdatedAt()
        ));

        return mapToResponse(savedRequest);
    }


    // ─────────────────────────────────────────────────────────────
    // FULFILL REQUEST
    // APPROVED → FULFILLED
    // Items physically student ko de diye gaye — last step
    // Inventory pehle hi deduct ho chuki thi approve ke time — yahan nahi hoti
    // ─────────────────────────────────────────────────────────────
    @Transactional
    public RequestResponse fulfillRequest(Long id) {
        log.info("AUDIT: Fulfilling request ID: {}", id);

        StationeryRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "id", id));

        // Sirf APPROVED request fulfill ho sakti hai
        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new IllegalStateException(
                    "Request can only be fulfilled when APPROVED. Current: " + request.getStatus());
        }

        request.setStatus(RequestStatus.FULFILLED);
        StationeryRequest savedRequest = requestRepository.save(request);

        log.info("AUDIT: Request ID: {} fulfilled successfully.", id);

        auditLogRepository.save(new AuditLog(
                "REQUEST_FULFILLED",
                "SYSTEM",
                "SYSTEM",
                "Fulfilled request ID: " + savedRequest.getRequestId()
                        + " for student: " + savedRequest.getStudentUsername(),
                savedRequest.getCreatedAt(),
                savedRequest.getUpdatedAt()
        ));

        return mapToResponse(savedRequest);
    }


    // ─────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────

    // String → RequestStatus Enum convert karo
    // "pending" → RequestStatus.PENDING
    // Invalid value aaye toh clear error message do
    private RequestStatus parseStatus(String status) {
        try {
            return RequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status
                    + ". Valid values: PENDING, APPROVED, REJECTED, FULFILLED");
        }
    }

    // Entity → DTO: Client ko sirf zaroori data do, internal fields nahi
    // items ki list bhi map hoti hai: RequestItem → RequestItemDto
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
                .requestId(request.getRequestId())           // UUID — client-facing ID
                .studentUsername(request.getStudentUsername())
                .items(itemDtos)
                .status(request.getStatus().name())          // Enum → String: PENDING, APPROVED etc.
                .rejectionReason(request.getRejectionReason()) // Sirf reject pe populated hoga
                .adminUsername(request.getAdminUsername())   // Sirf approve/reject pe set hoga
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    // External services se aaye audit logs save karo
    // null timestamps hain toh current time set karo
    @Transactional
    public AuditLog saveAuditLog(AuditLog auditLog) {
        log.info("Saving external audit log: {} by: {}", auditLog.getAction(), auditLog.getPerformedBy());
        if (auditLog.getCreatedTime() == null) {
            auditLog.setCreatedTime(java.time.LocalDateTime.now());
        }
        if (auditLog.getUpdatedTime() == null) {
            auditLog.setUpdatedTime(java.time.LocalDateTime.now());
        }
        return auditLogRepository.save(auditLog);
    }

    // Saare audit logs latest pehle — ID descending order
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAllByOrderByIdDesc();
    }
}