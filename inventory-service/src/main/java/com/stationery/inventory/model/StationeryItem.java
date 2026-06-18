package com.stationery.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "stationery_items")
public class StationeryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Item name is required")
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String unit;

    @Min(value = 0, message = "Available quantity cannot be negative")
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Min(value = 0, message = "Minimum quantity cannot be negative")
    @Column(name = "minimum_quantity", nullable = false)
    private Integer minimumQuantity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StationeryItem() {}

    public StationeryItem(Long id, String name, String category, String unit, Integer availableQuantity, Integer minimumQuantity, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.availableQuantity = availableQuantity;
        this.minimumQuantity = minimumQuantity;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }

    public Integer getMinimumQuantity() { return minimumQuantity; }
    public void setMinimumQuantity(Integer minimumQuantity) { this.minimumQuantity = minimumQuantity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static StationeryItemBuilder builder() { return new StationeryItemBuilder(); }

    public static class StationeryItemBuilder {
        private Long id;
        private String name;
        private String category;
        private String unit;
        private Integer availableQuantity;
        private Integer minimumQuantity;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public StationeryItemBuilder id(Long id) { this.id = id; return this; }
        public StationeryItemBuilder name(String name) { this.name = name; return this; }
        public StationeryItemBuilder category(String category) { this.category = category; return this; }
        public StationeryItemBuilder unit(String unit) { this.unit = unit; return this; }
        public StationeryItemBuilder availableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; return this; }
        public StationeryItemBuilder minimumQuantity(Integer minimumQuantity) { this.minimumQuantity = minimumQuantity; return this; }
        public StationeryItemBuilder description(String description) { this.description = description; return this; }
        public StationeryItemBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public StationeryItemBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public StationeryItem build() {
            return new StationeryItem(id, name, category, unit, availableQuantity, minimumQuantity, description, createdAt, updatedAt);
        }
    }
}
