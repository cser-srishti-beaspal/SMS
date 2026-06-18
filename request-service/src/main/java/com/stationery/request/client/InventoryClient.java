package com.stationery.request.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/api/inventory/{id}")
    ResponseEntity<Map<String, Object>> getInventoryItem(@PathVariable("id") Long id);

    @PutMapping("/api/inventory/{id}/deduct")
    ResponseEntity<Map<String, Object>> deductItemQuantity(
            @PathVariable("id") Long id,
            @RequestParam("quantity") Integer quantity
    );
}
