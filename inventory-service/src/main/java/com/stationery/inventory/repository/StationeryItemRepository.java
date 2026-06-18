package com.stationery.inventory.repository;

import com.stationery.inventory.model.StationeryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for StationeryItem entities.
 * Provides CRUD operations plus custom query methods for
 * category filtering, name searching, and low stock detection.
 */
@Repository
public interface StationeryItemRepository extends JpaRepository<StationeryItem, Long> {

    /**
     * Find all items belonging to a specific category.
     *
     * @param category the category to filter by
     * @return list of matching stationery items
     */
    List<StationeryItem> findByCategory(String category);

    /**
     * Find all items belonging to a specific category with pagination.
     *
     * @param category the category to filter by
     * @param pageable pagination parameters
     * @return page of matching stationery items
     */
    Page<StationeryItem> findByCategory(String category, Pageable pageable);

    /**
     * Search for items whose name contains the given keyword (case-insensitive).
     *
     * @param name the keyword to search for
     * @return list of matching stationery items
     */
    List<StationeryItem> findByNameContainingIgnoreCase(String name);

    /**
     * Find items with available quantity at or below the given threshold.
     * Used to identify low-stock items that need reordering.
     *
     * @param quantity the threshold quantity
     * @return list of low-stock stationery items
     */
    List<StationeryItem> findByAvailableQuantityLessThanEqual(Integer quantity);
}
