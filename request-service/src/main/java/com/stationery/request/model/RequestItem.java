package com.stationery.request.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "request_items")
public class RequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Min(1)
    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private StationeryRequest request;

    public RequestItem() {}

    public RequestItem(Long id, Long itemId, String itemName, Integer quantity, StationeryRequest request) {
        this.id = id;
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.request = request;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public StationeryRequest getRequest() { return request; }
    public void setRequest(StationeryRequest request) { this.request = request; }

    public static RequestItemBuilder builder() { return new RequestItemBuilder(); }

    public static class RequestItemBuilder {
        private Long id;
        private Long itemId;
        private String itemName;
        private Integer quantity;
        private StationeryRequest request;

        public RequestItemBuilder id(Long id) { this.id = id; return this; }
        public RequestItemBuilder itemId(Long itemId) { this.itemId = itemId; return this; }
        public RequestItemBuilder itemName(String itemName) { this.itemName = itemName; return this; }
        public RequestItemBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public RequestItemBuilder request(StationeryRequest request) { this.request = request; return this; }

        public RequestItem build() {
            return new RequestItem(id, itemId, itemName, quantity, request);
        }
    }
}
