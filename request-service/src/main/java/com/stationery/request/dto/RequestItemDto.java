package com.stationery.request.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RequestItemDto {

    @NotNull(message = "Item ID is required")
    private Long itemId;

    private String itemName;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    public RequestItemDto() {}

    public RequestItemDto(Long itemId, String itemName, Integer quantity) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
    }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public static RequestItemDtoBuilder builder() { return new RequestItemDtoBuilder(); }

    public static class RequestItemDtoBuilder {
        private Long itemId;
        private String itemName;
        private Integer quantity;

        public RequestItemDtoBuilder itemId(Long itemId) { this.itemId = itemId; return this; }
        public RequestItemDtoBuilder itemName(String itemName) { this.itemName = itemName; return this; }
        public RequestItemDtoBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }

        public RequestItemDto build() {
            return new RequestItemDto(itemId, itemName, quantity);
        }
    }
}
