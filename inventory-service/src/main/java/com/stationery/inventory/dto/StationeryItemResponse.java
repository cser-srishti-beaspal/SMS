package com.stationery.inventory.dto;

import java.time.LocalDateTime;

public class StationeryItemResponse {

    private Long id;
    private String name;
    private String category;
    private String unit;
    private Integer availableQuantity;
    private Integer minimumQuantity;
    private String description;
    private boolean lowStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StationeryItemResponse() {}

    public StationeryItemResponse(Long id, String name, String category, String unit, Integer availableQuantity, Integer minimumQuantity, String description, boolean lowStock, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.availableQuantity = availableQuantity;
        this.minimumQuantity = minimumQuantity;
        this.description = description;
        this.lowStock = lowStock;
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

    public boolean isLowStock() { return lowStock; }
    public void setLowStock(boolean lowStock) { this.lowStock = lowStock; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static StationeryItemResponseBuilder builder() { return new StationeryItemResponseBuilder(); }

    public static class StationeryItemResponseBuilder {
        private Long id;
        private String name;
        private String category;
        private String unit;
        private Integer availableQuantity;
        private Integer minimumQuantity;
        private String description;
        private boolean lowStock;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public StationeryItemResponseBuilder id(Long id) { this.id = id; return this; }
        public StationeryItemResponseBuilder name(String name) { this.name = name; return this; }
        public StationeryItemResponseBuilder category(String category) { this.category = category; return this; }
        public StationeryItemResponseBuilder unit(String unit) { this.unit = unit; return this; }
        public StationeryItemResponseBuilder availableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; return this; }
        public StationeryItemResponseBuilder minimumQuantity(Integer minimumQuantity) { this.minimumQuantity = minimumQuantity; return this; }
        public StationeryItemResponseBuilder description(String description) { this.description = description; return this; }
        public StationeryItemResponseBuilder lowStock(boolean lowStock) { this.lowStock = lowStock; return this; }
        public StationeryItemResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public StationeryItemResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public StationeryItemResponse build() {
            return new StationeryItemResponse(id, name, category, unit, availableQuantity, minimumQuantity, description, lowStock, createdAt, updatedAt);
        }
    }
}
