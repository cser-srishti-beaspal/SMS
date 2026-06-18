package com.stationery.inventory.controller;

import com.stationery.inventory.dto.StationeryItemRequest;
import com.stationery.inventory.dto.StationeryItemResponse;
import com.stationery.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for inventory management operations.
 * Exposes endpoints for CRUD operations, stock management, search, and low-stock alerts.
 * Admin-only operations are protected via X-User-Role header checks (set by API Gateway).
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Creates a new stationery item. Admin-only.
     *
     * @param request   the item creation request
     * @param userRole  the role from X-User-Role header
     * @param userName  the username from X-User-Name header
     * @return the created item with HTTP 201
     */
    @PostMapping
    public ResponseEntity<StationeryItemResponse> createItem(
            @Valid @RequestBody StationeryItemRequest request,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole,
            @RequestHeader(value = "X-User-Name", defaultValue = "SYSTEM") String userName) {

        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            log.warn("AUDIT: Unauthorized create attempt by user '{}' with role '{}'", userName, userRole);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("AUDIT: User '{}' (role: {}) creating new stationery item: '{}'",
                userName, userRole, request.getName());

        StationeryItemResponse response = inventoryService.createItem(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Retrieves all stationery items with pagination.
     *
     * @param page   page number (default 0)
     * @param size   page size (default 20)
     * @param sortBy sort field (default "name")
     * @return paginated list of items
     */
    @GetMapping
    public ResponseEntity<Page<StationeryItemResponse>> getAllItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy) {

        Page<StationeryItemResponse> items = inventoryService.getAllItems(page, size, sortBy);
        return ResponseEntity.ok(items);
    }

    /**
     * Retrieves a specific stationery item by ID.
     *
     * @param id the item ID
     * @return the item details
     */
    @GetMapping("/{id}")
    public ResponseEntity<StationeryItemResponse> getItemById(@PathVariable Long id) {
        StationeryItemResponse response = inventoryService.getItemById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves stationery items filtered by category with pagination.
     *
     * @param category the category to filter by
     * @param page     page number (default 0)
     * @param size     page size (default 20)
     * @return paginated list of items in the category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<StationeryItemResponse>> getItemsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<StationeryItemResponse> items = inventoryService.getItemsByCategory(category, page, size);
        return ResponseEntity.ok(items);
    }

    /**
     * Updates an existing stationery item. Admin-only.
     *
     * @param id       the item ID to update
     * @param request  the update request
     * @param userRole the role from X-User-Role header
     * @param userName the username from X-User-Name header
     * @return the updated item
     */
    @PutMapping("/{id}")
    public ResponseEntity<StationeryItemResponse> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody StationeryItemRequest request,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole,
            @RequestHeader(value = "X-User-Name", defaultValue = "SYSTEM") String userName) {

        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            log.warn("AUDIT: Unauthorized update attempt on item ID {} by user '{}' with role '{}'",
                    id, userName, userRole);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("AUDIT: User '{}' (role: {}) updating stationery item ID: {}", userName, userRole, id);

        StationeryItemResponse response = inventoryService.updateItem(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a stationery item. Admin-only.
     *
     * @param id       the item ID to delete
     * @param userRole the role from X-User-Role header
     * @param userName the username from X-User-Name header
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole,
            @RequestHeader(value = "X-User-Name", defaultValue = "SYSTEM") String userName) {

        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            log.warn("AUDIT: Unauthorized delete attempt on item ID {} by user '{}' with role '{}'",
                    id, userName, userRole);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("AUDIT: User '{}' (role: {}) deleting stationery item ID: {}", userName, userRole, id);

        inventoryService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all items that are at or below their minimum stock level. Admin-only.
     *
     * @param userRole the role from X-User-Role header
     * @return list of low-stock items
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<StationeryItemResponse>> getLowStockItems(
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole) {

        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            log.warn("AUDIT: Unauthorized low-stock access attempt with role '{}'", userRole);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<StationeryItemResponse> lowStockItems = inventoryService.getLowStockItems();
        return ResponseEntity.ok(lowStockItems);
    }

    /**
     * Deducts a quantity from an item's stock. Called internally by request-service.
     *
     * @param id       the item ID
     * @param quantity the quantity to deduct
     * @return true if deduction was successful
     */
    @PutMapping("/{id}/deduct")
    public ResponseEntity<Boolean> deductQuantity(
            @PathVariable Long id,
            @RequestParam Integer quantity) {

        log.info("AUDIT: Internal service call - deducting {} units from item ID: {}", quantity, id);

        boolean result = inventoryService.deductQuantity(id, quantity);
        return ResponseEntity.ok(result);
    }

    /**
     * Searches for stationery items by keyword (case-insensitive name match).
     *
     * @param keyword the search keyword
     * @return list of matching items
     */
    @GetMapping("/search")
    public ResponseEntity<List<StationeryItemResponse>> searchItems(
            @RequestParam String keyword) {

        List<StationeryItemResponse> items = inventoryService.searchItems(keyword);
        return ResponseEntity.ok(items);
    }
}
