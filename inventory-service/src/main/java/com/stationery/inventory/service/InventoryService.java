package com.stationery.inventory.service;

import com.stationery.inventory.dto.StationeryItemRequest;
import com.stationery.inventory.dto.StationeryItemResponse;
import com.stationery.inventory.exception.InsufficientStockException;
import com.stationery.inventory.exception.ResourceNotFoundException;
import com.stationery.inventory.model.StationeryItem;
import com.stationery.inventory.repository.StationeryItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for inventory management operations.
 * Handles CRUD operations, stock management, and search functionality
 * for stationery items. Includes audit logging for all mutating operations.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final StationeryItemRepository stationeryItemRepository;
    private final AuditLogger auditLogger;

    public InventoryService(StationeryItemRepository stationeryItemRepository, AuditLogger auditLogger) {
        this.stationeryItemRepository = stationeryItemRepository;
        this.auditLogger = auditLogger;
    }

    /**
     * Creates a new stationery item in the inventory.
     * Admin-only operation with audit logging.
     *
     * @param request the item creation request
     * @return the created item response
     */
    @Transactional
    public StationeryItemResponse createItem(StationeryItemRequest request) {
        return createItem(request, "SYSTEM", "ADMIN");
    }

    /**
     * Creates a new stationery item in the inventory with audit logging.
     *
     * @param request     the item creation request
     * @param performedBy the user who performed the action
     * @param userRole    the role of the user
     * @return the created item response
     */
    @Transactional
    public StationeryItemResponse createItem(StationeryItemRequest request, String performedBy, String userRole) {
        log.info("AUDIT: Creating new stationery item with name: '{}' by user: '{}'", request.getName(), performedBy);

        StationeryItem item = StationeryItem.builder()
                .name(request.getName())
                .category(request.getCategory().toUpperCase())
                .unit(request.getUnit())
                .availableQuantity(request.getAvailableQuantity())
                .minimumQuantity(request.getMinimumQuantity())
                .description(request.getDescription())
                .build();

        StationeryItem savedItem = stationeryItemRepository.save(item);
        log.info("AUDIT: Successfully created stationery item with ID: {}, name: '{}'",
                savedItem.getId(), savedItem.getName());

        auditLogger.log(
                "ITEM_CREATED",
                performedBy,
                userRole,
                String.format("Created stationery item: %s (ID: %d) in category: %s with initial quantity: %d",
                        savedItem.getName(), savedItem.getId(), savedItem.getCategory(), savedItem.getAvailableQuantity())
        );

        return mapToResponse(savedItem);
    }

    /**
     * Retrieves a stationery item by its ID.
     *
     * @param id the item ID
     * @return the item response
     * @throws ResourceNotFoundException if the item is not found
     */
    @Transactional(readOnly = true)
    public StationeryItemResponse getItemById(Long id) {
        log.debug("Fetching stationery item with ID: {}", id);

        StationeryItem item = stationeryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stationery item not found with ID: " + id));

        return mapToResponse(item);
    }

    /**
     * Retrieves all stationery items with pagination and sorting.
     *
     * @param page   the page number (0-indexed)
     * @param size   the page size (default 20)
     * @param sortBy the field to sort by
     * @return a page of item responses
     */
    @Transactional(readOnly = true)
    public Page<StationeryItemResponse> getAllItems(int page, int size, String sortBy) {
        log.debug("Fetching all stationery items - page: {}, size: {}, sortBy: {}", page, size, sortBy);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<StationeryItem> itemsPage = stationeryItemRepository.findAll(pageable);

        return itemsPage.map(this::mapToResponse);
    }

    /**
     * Retrieves stationery items filtered by category with pagination.
     *
     * @param category the category to filter by
     * @param page     the page number (0-indexed)
     * @param size     the page size
     * @return a page of item responses in the specified category
     */
    @Transactional(readOnly = true)
    public Page<StationeryItemResponse> getItemsByCategory(String category, int page, int size) {
        log.debug("Fetching stationery items by category: '{}' - page: {}, size: {}", category, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        Page<StationeryItem> itemsPage = stationeryItemRepository.findByCategory(category.toUpperCase(), pageable);

        return itemsPage.map(this::mapToResponse);
    }

    /**
     * Updates an existing stationery item.
     * Admin-only operation with audit logging of changes.
     *
     * @param id      the item ID to update
     * @param request the update request
     * @return the updated item response
     * @throws ResourceNotFoundException if the item is not found
     */
    @Transactional
    public StationeryItemResponse updateItem(Long id, StationeryItemRequest request) {
        return updateItem(id, request, "SYSTEM", "ADMIN");
    }

    /**
     * Updates an existing stationery item with audit logging.
     *
     * @param id          the item ID to update
     * @param request     the update request
     * @param performedBy the user who performed the action
     * @param userRole    the role of the user
     * @return the updated item response
     * @throws ResourceNotFoundException if the item is not found
     */
    @Transactional
    public StationeryItemResponse updateItem(Long id, StationeryItemRequest request, String performedBy, String userRole) {
        log.info("AUDIT: Updating stationery item with ID: {} by user: '{}'", id, performedBy);

        StationeryItem existingItem = stationeryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stationery item not found with ID: " + id));

        // Build details explaining exactly what was updated
        StringBuilder detailsBuilder = new StringBuilder("Updated stationery item '" + existingItem.getName() + "' (ID: " + id + "):");
        boolean changesFound = false;

        // Log field-level changes for audit trail
        if (!existingItem.getName().equals(request.getName())) {
            log.info("AUDIT: Item ID {} - name changed from '{}' to '{}'",
                    id, existingItem.getName(), request.getName());
            detailsBuilder.append(" [Name: '").append(existingItem.getName()).append("' -> '").append(request.getName()).append("']");
            changesFound = true;
        }
        if (!existingItem.getCategory().equals(request.getCategory().toUpperCase())) {
            log.info("AUDIT: Item ID {} - category changed from '{}' to '{}'",
                    id, existingItem.getCategory(), request.getCategory().toUpperCase());
            detailsBuilder.append(" [Category: '").append(existingItem.getCategory()).append("' -> '").append(request.getCategory().toUpperCase()).append("']");
            changesFound = true;
        }
        if (!existingItem.getAvailableQuantity().equals(request.getAvailableQuantity())) {
            log.info("AUDIT: Item ID {} - availableQuantity changed from {} to {}",
                    id, existingItem.getAvailableQuantity(), request.getAvailableQuantity());
            detailsBuilder.append(" [Quantity: ").append(existingItem.getAvailableQuantity()).append(" -> ").append(request.getAvailableQuantity()).append("]");
            changesFound = true;
        }
        if (!existingItem.getMinimumQuantity().equals(request.getMinimumQuantity())) {
            log.info("AUDIT: Item ID {} - minimumQuantity changed from {} to {}",
                    id, existingItem.getMinimumQuantity(), request.getMinimumQuantity());
            detailsBuilder.append(" [Min Quantity: ").append(existingItem.getMinimumQuantity()).append(" -> ").append(request.getMinimumQuantity()).append("]");
            changesFound = true;
        }
        if (existingItem.getDescription() == null || !existingItem.getDescription().equals(request.getDescription())) {
            detailsBuilder.append(" [Description changed]");
            changesFound = true;
        }

        if (!changesFound) {
            detailsBuilder.append(" No visible changes made.");
        }

        existingItem.setName(request.getName());
        existingItem.setCategory(request.getCategory().toUpperCase());
        existingItem.setUnit(request.getUnit());
        existingItem.setAvailableQuantity(request.getAvailableQuantity());
        existingItem.setMinimumQuantity(request.getMinimumQuantity());
        existingItem.setDescription(request.getDescription());

        StationeryItem updatedItem = stationeryItemRepository.save(existingItem);
        log.info("AUDIT: Successfully updated stationery item with ID: {}", id);

        auditLogger.log(
                "ITEM_UPDATED",
                performedBy,
                userRole,
                detailsBuilder.toString()
        );

        return mapToResponse(updatedItem);
    }

    /**
     * Deletes a stationery item from the inventory.
     * Admin-only operation with audit logging.
     *
     * @param id the item ID to delete
     * @throws ResourceNotFoundException if the item is not found
     */
    @Transactional
    public void deleteItem(Long id) {
        deleteItem(id, "SYSTEM", "ADMIN");
    }

    /**
     * Deletes a stationery item from the inventory with audit logging.
     *
     * @param id          the item ID to delete
     * @param performedBy the user who performed the action
     * @param userRole    the role of the user
     * @throws ResourceNotFoundException if the item is not found
     */
    @Transactional
    public void deleteItem(Long id, String performedBy, String userRole) {
        log.info("AUDIT: Attempting to delete stationery item with ID: {} by user: '{}'", id, performedBy);

        StationeryItem item = stationeryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stationery item not found with ID: " + id));

        stationeryItemRepository.delete(item);
        log.info("AUDIT: Successfully deleted stationery item with ID: {}, name: '{}'",
                id, item.getName());

        auditLogger.log(
                "ITEM_DELETED",
                performedBy,
                userRole,
                String.format("Deleted stationery item: %s (ID: %d) from category: %s",
                        item.getName(), item.getId(), item.getCategory())
        );
    }

    /**
     * Retrieves all items that are at or below their minimum stock level.
     *
     * @return list of low-stock item responses
     */
    @Transactional(readOnly = true)
    public List<StationeryItemResponse> getLowStockItems() {
        log.debug("Fetching low stock items");

        List<StationeryItem> allItems = stationeryItemRepository.findAll();

        return allItems.stream()
                .filter(item -> item.getAvailableQuantity() <= item.getMinimumQuantity())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Deducts a specified quantity from an item's available stock.
     * Called internally by the request-service via REST when fulfilling requests.
     *
     * @param itemId   the item ID to deduct from
     * @param quantity the quantity to deduct
     * @return true if the deduction was successful
     * @throws ResourceNotFoundException   if the item is not found
     * @throws InsufficientStockException if available quantity is insufficient
     */
    @Transactional
    public boolean deductQuantity(Long itemId, Integer quantity) {
        log.info("AUDIT: Deducting quantity {} from item ID: {}", quantity, itemId);

        StationeryItem item = stationeryItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stationery item not found with ID: " + itemId));

        if (item.getAvailableQuantity() < quantity) {
            log.warn("AUDIT: Insufficient stock for item ID: {}. Available: {}, Requested: {}",
                    itemId, item.getAvailableQuantity(), quantity);
            throw new InsufficientStockException(
                    String.format("Insufficient stock for item '%s'. Available: %d, Requested: %d",
                            item.getName(), item.getAvailableQuantity(), quantity));
        }

        item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
        stationeryItemRepository.save(item);

        log.info("AUDIT: Successfully deducted {} from item ID: {}. New available quantity: {}",
                quantity, itemId, item.getAvailableQuantity());

        return true;
    }

    /**
     * Searches for stationery items by keyword (case-insensitive name match).
     *
     * @param keyword the search keyword
     * @return list of matching item responses
     */
    @Transactional(readOnly = true)
    public List<StationeryItemResponse> searchItems(String keyword) {
        log.debug("Searching stationery items with keyword: '{}'", keyword);

        List<StationeryItem> items = stationeryItemRepository.findByNameContainingIgnoreCase(keyword);

        return items.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Maps a StationeryItem entity to a StationeryItemResponse DTO.
     * Sets the lowStock flag to true if availableQuantity <= minimumQuantity.
     *
     * @param item the entity to map
     * @return the mapped response DTO
     */
    public StationeryItemResponse mapToResponse(StationeryItem item) {
        return StationeryItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .category(item.getCategory())
                .unit(item.getUnit())
                .availableQuantity(item.getAvailableQuantity())
                .minimumQuantity(item.getMinimumQuantity())
                .description(item.getDescription())
                .lowStock(item.getAvailableQuantity() <= item.getMinimumQuantity())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
