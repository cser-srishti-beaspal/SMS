package com.stationery.inventory.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;  // Stock kam hone par throw hota hai
import org.slf4j.LoggerFactory;    // Item na mile toh throw hota hai
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;           // Paginated result wrap karta hai
import org.springframework.transaction.annotation.Transactional;    // Page number + size + sort se Pageable banata hai

import com.stationery.inventory.dto.StationeryItemRequest;       // Pagination info hold karta hai
import com.stationery.inventory.dto.StationeryItemResponse;           // Sorting direction aur field specify karta hai
import com.stationery.inventory.exception.InsufficientStockException;
import com.stationery.inventory.exception.ResourceNotFoundException;
import com.stationery.inventory.model.StationeryItem;
import com.stationery.inventory.repository.StationeryItemRepository;

/**
 * InventoryService — Stationery items ka poora business logic yahan hai.
 *
 * Kya karta hai:
 *   - CRUD: Create, Read, Update, Delete
 *   - Stock Management: quantity deduct karna, low stock check
 *   - Search: keyword se items dhundhna
 *   - Audit Logging: har mutating operation ka record
 *
 * Controller sirf HTTP handle karta hai — asla kaam yahan hota hai.
 */
@Service // Spring ko batata hai: "is class ka bean banao aur inject karo jahan chahiye"
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    // Repository: Database se seedha baat karta hai (SQL queries yahan nahi likhte)
    private final StationeryItemRepository stationeryItemRepository;

    // AuditLogger: Kaun, kya, kab kiya — yeh sab record karta hai
    private final AuditLogger auditLogger;

    // Constructor Injection — field injection (@Autowired) se better: testable + immutable
    public InventoryService(StationeryItemRepository stationeryItemRepository, AuditLogger auditLogger) {
        this.stationeryItemRepository = stationeryItemRepository;
        this.auditLogger = auditLogger;
    }


    // ─────────────────────────────────────────────────────────────
    // CREATE ITEM
    // Do versions hain: ek default (SYSTEM/ADMIN), ek actual logic wala
    // Yeh pattern Method Overloading hai — same method naam, alag parameters
    // ─────────────────────────────────────────────────────────────

    // Jab koi user info nahi hai toh SYSTEM/ADMIN default use hota hai
    @Transactional
    public StationeryItemResponse createItem(StationeryItemRequest request) {
        return createItem(request, "SYSTEM", "ADMIN"); // Actual method ko delegate karo
    }

    // Actual create logic — performedBy aur userRole audit trail ke liye
    @Transactional // Database operation complete ho ya rollback ho — adha kaam nahi
    public StationeryItemResponse createItem(StationeryItemRequest request, String performedBy, String userRole) {
        log.info("AUDIT: Creating new stationery item: '{}' by user: '{}'", request.getName(), performedBy);

        // Builder pattern: step-by-step object banao — readable aur flexible
        StationeryItem item = StationeryItem.builder()
                .name(request.getName())
                .category(request.getCategory().toUpperCase()) // Category hamesha uppercase store hogi
                .unit(request.getUnit())
                .availableQuantity(request.getAvailableQuantity())
                .minimumQuantity(request.getMinimumQuantity())
                .description(request.getDescription())
                .build();

        // Database mein save karo — savedItem mein auto-generated ID hogi
        StationeryItem savedItem = stationeryItemRepository.save(item);
        log.info("AUDIT: Item created with ID: {}, name: '{}'", savedItem.getId(), savedItem.getName());

        // Audit trail: kaun, kya kiya, kab — compliance aur debugging ke liye
        auditLogger.log(
                "ITEM_CREATED",
                performedBy,
                userRole,
                String.format("Created: %s (ID: %d), Category: %s, Qty: %d",
                        savedItem.getName(), savedItem.getId(),
                        savedItem.getCategory(), savedItem.getAvailableQuantity())
        );

        return mapToResponse(savedItem); // Entity → DTO (client ko sirf zaroori data do)
    }


    // ─────────────────────────────────────────────────────────────
    // READ OPERATIONS
    // readOnly = true: Spring ko hint deta hai — sirf read hai, write nahi
    //                  Performance better hoti hai (dirty checking skip hoti hai)
    // ─────────────────────────────────────────────────────────────

    // ID se single item fetch karo
    @Transactional(readOnly = true)
    public StationeryItemResponse getItemById(Long id) {
        log.debug("Fetching item with ID: {}", id);

        // findById → Optional return karta hai
        // orElseThrow → Optional khaali ho toh exception throw karo
        StationeryItem item = stationeryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stationery item not found with ID: " + id));

        return mapToResponse(item);
    }

    // Saare items fetch karo — Paginated (ek saath sab nahi, thoda thoda)
    // page: kaun sa page (0 = pehla), size: ek page mein kitne items, sortBy: kis field se sort
    @Transactional(readOnly = true)
    public Page<StationeryItemResponse> getAllItems(int page, int size, String sortBy) {
        log.debug("Fetching all items - page: {}, size: {}, sortBy: {}", page, size, sortBy);

        // PageRequest = pagination + sorting ki settings ek saath
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        // findAll(pageable) → sirf usi page ke items aate hain, sab nahi
        Page<StationeryItem> itemsPage = stationeryItemRepository.findAll(pageable);

        // Page ke andar har entity ko DTO mein convert karo
        return itemsPage.map(this::mapToResponse);
    }

    // Category ke hisaab se filter karke fetch karo
    @Transactional(readOnly = true)
    public Page<StationeryItemResponse> getItemsByCategory(String category, int page, int size) {
        log.debug("Fetching items by category: '{}' - page: {}, size: {}", category, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("name")); // Name se alphabetically sort
        Page<StationeryItem> itemsPage = stationeryItemRepository
                .findByCategory(category.toUpperCase(), pageable); // Case-insensitive match ke liye uppercase

        return itemsPage.map(this::mapToResponse);
    }


    // ─────────────────────────────────────────────────────────────
    // UPDATE ITEM
    // Sirf changed fields ka log hota hai — full object nahi
    // changesFound flag: agar koi change na hua toh audit mein note karo
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public StationeryItemResponse updateItem(Long id, StationeryItemRequest request) {
        return updateItem(id, request, "SYSTEM", "ADMIN");
    }

    @Transactional
    public StationeryItemResponse updateItem(Long id, StationeryItemRequest request, String performedBy, String userRole) {
        log.info("AUDIT: Updating item ID: {} by user: '{}'", id, performedBy);

        // Pehle check karo ki item exist karta hai
        StationeryItem existingItem = stationeryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stationery item not found with ID: " + id));

        // StringBuilder: audit message dynamically banao — sirf jo badla woh likho
        StringBuilder detailsBuilder = new StringBuilder(
                "Updated item '" + existingItem.getName() + "' (ID: " + id + "):");
        boolean changesFound = false;

        // Field-level comparison: old value vs new value
        // Sirf changed fields audit trail mein jaate hain — clean logs
        if (!existingItem.getName().equals(request.getName())) {
            log.info("AUDIT: ID {} - name: '{}' -> '{}'", id, existingItem.getName(), request.getName());
            detailsBuilder.append(" [Name: '").append(existingItem.getName())
                    .append("' -> '").append(request.getName()).append("']");
            changesFound = true;
        }
        if (!existingItem.getCategory().equals(request.getCategory().toUpperCase())) {
            log.info("AUDIT: ID {} - category: '{}' -> '{}'",
                    id, existingItem.getCategory(), request.getCategory().toUpperCase());
            detailsBuilder.append(" [Category: '").append(existingItem.getCategory())
                    .append("' -> '").append(request.getCategory().toUpperCase()).append("']");
            changesFound = true;
        }
        if (!existingItem.getAvailableQuantity().equals(request.getAvailableQuantity())) {
            log.info("AUDIT: ID {} - availableQty: {} -> {}",
                    id, existingItem.getAvailableQuantity(), request.getAvailableQuantity());
            detailsBuilder.append(" [Qty: ").append(existingItem.getAvailableQuantity())
                    .append(" -> ").append(request.getAvailableQuantity()).append("]");
            changesFound = true;
        }
        if (!existingItem.getMinimumQuantity().equals(request.getMinimumQuantity())) {
            log.info("AUDIT: ID {} - minimumQty: {} -> {}",
                    id, existingItem.getMinimumQuantity(), request.getMinimumQuantity());
            detailsBuilder.append(" [Min Qty: ").append(existingItem.getMinimumQuantity())
                    .append(" -> ").append(request.getMinimumQuantity()).append("]");
            changesFound = true;
        }
        // Description ke liye null check zaroori hai — NullPointerException se bachao
        if (existingItem.getDescription() == null || !existingItem.getDescription().equals(request.getDescription())) {
            detailsBuilder.append(" [Description changed]");
            changesFound = true;
        }

        if (!changesFound) {
            detailsBuilder.append(" No changes detected."); // Agar kuch badla hi nahi
        }

        // Naye values set karo aur save karo
        existingItem.setName(request.getName());
        existingItem.setCategory(request.getCategory().toUpperCase());
        existingItem.setUnit(request.getUnit());
        existingItem.setAvailableQuantity(request.getAvailableQuantity());
        existingItem.setMinimumQuantity(request.getMinimumQuantity());
        existingItem.setDescription(request.getDescription());

        StationeryItem updatedItem = stationeryItemRepository.save(existingItem);
        log.info("AUDIT: Successfully updated item ID: {}", id);

        auditLogger.log("ITEM_UPDATED", performedBy, userRole, detailsBuilder.toString());

        return mapToResponse(updatedItem);
    }


    // ─────────────────────────────────────────────────────────────
    // DELETE ITEM
    // Item pehle fetch karo — do reasons:
    //   1. Exist nahi karta toh exception throw karo
    //   2. Audit log mein item ka naam chahiye (sirf ID se naam nahi milta)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void deleteItem(Long id) {
        deleteItem(id, "SYSTEM", "ADMIN");
    }

    @Transactional
    public void deleteItem(Long id, String performedBy, String userRole) {
        log.info("AUDIT: Deleting item ID: {} by user: '{}'", id, performedBy);

        StationeryItem item = stationeryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stationery item not found with ID: " + id));

        stationeryItemRepository.delete(item);
        log.info("AUDIT: Deleted item ID: {}, name: '{}'", id, item.getName());

        auditLogger.log(
                "ITEM_DELETED",
                performedBy,
                userRole,
                String.format("Deleted: %s (ID: %d), Category: %s",
                        item.getName(), item.getId(), item.getCategory())
        );
    }


    // ─────────────────────────────────────────────────────────────
    // LOW STOCK CHECK
    // findAll() se saare items aate hain, phir stream filter se
    // sirf woh items rakhte hain jinka stock minimum se kam ya barabar hai
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StationeryItemResponse> getLowStockItems() {
        log.debug("Fetching low stock items");

        return stationeryItemRepository.findAll()
                .stream()
                // availableQuantity <= minimumQuantity → reorder karna chahiye
                .filter(item -> item.getAvailableQuantity() <= item.getMinimumQuantity())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    // ─────────────────────────────────────────────────────────────
    // DEDUCT QUANTITY
    // Request-Service yeh method call karta hai jab koi item request approve ho
    // Do checks:
    //   1. Item exist karta hai? (ResourceNotFoundException)
    //   2. Enough stock hai? (InsufficientStockException)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public boolean deductQuantity(Long itemId, Integer quantity) {
        log.info("AUDIT: Deducting {} units from item ID: {}", quantity, itemId);

        StationeryItem item = stationeryItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stationery item not found with ID: " + itemId));

        // Stock check — deduct karne se pehle verify karo
        if (item.getAvailableQuantity() < quantity) {
            log.warn("AUDIT: Insufficient stock for item ID: {}. Available: {}, Requested: {}",
                    itemId, item.getAvailableQuantity(), quantity);
            throw new InsufficientStockException(
                    String.format("Insufficient stock for '%s'. Available: %d, Requested: %d",
                            item.getName(), item.getAvailableQuantity(), quantity));
        }

        // Current quantity se requested quantity ghata do
        item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
        stationeryItemRepository.save(item);

        log.info("AUDIT: Deducted {} from item ID: {}. New quantity: {}",
                quantity, itemId, item.getAvailableQuantity());

        return true;
    }


    // ─────────────────────────────────────────────────────────────
    // KEYWORD SEARCH
    // Repository mein custom query hai: findByNameContainingIgnoreCase
    // "Containing" = LIKE %keyword% SQL mein
    // "IgnoreCase" = uppercase/lowercase se fark nahi padta
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StationeryItemResponse> searchItems(String keyword) {
        log.debug("Searching items with keyword: '{}'", keyword);

        return stationeryItemRepository.findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    // ─────────────────────────────────────────────────────────────
    // HELPER: Entity → DTO Mapping
    //
    // Entity (StationeryItem) = Database ka object, sab fields hote hain
    // DTO (StationeryItemResponse) = Client ko sirf zaroori data do
    //
    // lowStock flag yahan calculate hota hai:
    //   availableQuantity <= minimumQuantity → true (reorder alert)
    // ─────────────────────────────────────────────────────────────

    public StationeryItemResponse mapToResponse(StationeryItem item) {
        return StationeryItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .category(item.getCategory())
                .unit(item.getUnit())
                .availableQuantity(item.getAvailableQuantity())
                .minimumQuantity(item.getMinimumQuantity())
                .description(item.getDescription())
                .lowStock(item.getAvailableQuantity() <= item.getMinimumQuantity()) // true → stock alert dikhao
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}