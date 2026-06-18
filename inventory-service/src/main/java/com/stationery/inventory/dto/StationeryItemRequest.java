package com.stationery.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StationeryItemRequest {

    @NotBlank(message = "Item name is required")
    private String name;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Unit is required")
    private String unit;

    @NotNull(message = "Available quantity is required")
    @Min(value = 0, message = "Available quantity cannot be negative")
    private Integer availableQuantity;

    @NotNull(message = "Minimum quantity is required")
    @Min(value = 0, message = "Minimum quantity cannot be negative")
    private Integer minimumQuantity;

    private String description;

    public StationeryItemRequest() {}

    public StationeryItemRequest(String name, String category, String unit, Integer availableQuantity, Integer minimumQuantity, String description) {
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.availableQuantity = availableQuantity;
        this.minimumQuantity = minimumQuantity;
        this.description = description;
    }

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

    public static StationeryItemRequestBuilder builder() { return new StationeryItemRequestBuilder(); }

    public static class StationeryItemRequestBuilder {
        private String name;
        private String category;
        private String unit;
        private Integer availableQuantity;
        private Integer minimumQuantity;
        private String description;

        public StationeryItemRequestBuilder name(String name) { this.name = name; return this; }
        public StationeryItemRequestBuilder category(String category) { this.category = category; return this; }
        public StationeryItemRequestBuilder unit(String unit) { this.unit = unit; return this; }
        public StationeryItemRequestBuilder availableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; return this; }
        public StationeryItemRequestBuilder minimumQuantity(Integer minimumQuantity) { this.minimumQuantity = minimumQuantity; return this; }
        public StationeryItemRequestBuilder description(String description) { this.description = description; return this; }

        public StationeryItemRequest build() {
            return new StationeryItemRequest(name, category, unit, availableQuantity, minimumQuantity, description);
        }
    }
}
