package com.stationery.inventory.controller;

import com.stationery.inventory.dto.StationeryItemRequest;
import com.stationery.inventory.dto.StationeryItemResponse;
import com.stationery.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController inventoryController;

    private StationeryItemRequest sampleRequest;
    private StationeryItemResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleRequest = StationeryItemRequest.builder()
                .name("Pen")
                .category("PENS")
                .availableQuantity(50)
                .minimumQuantity(10)
                .unit("box")
                .description("Blue pens")
                .build();

        sampleResponse = StationeryItemResponse.builder()
                .id(1L)
                .name("Pen")
                .category("PENS")
                .availableQuantity(50)
                .minimumQuantity(10)
                .unit("box")
                .description("Blue pens")
                .lowStock(false)
                .build();
    }

    @Test
    void createItem_Admin_Success() {
        // Arrange
        when(inventoryService.createItem(any(StationeryItemRequest.class), anyString(), anyString())).thenReturn(sampleResponse);

        // Act
        ResponseEntity<StationeryItemResponse> response = inventoryController.createItem(
                sampleRequest, "ROLE_ADMIN", "adminUser");

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(inventoryService, times(1)).createItem(sampleRequest, "adminUser", "ROLE_ADMIN");
    }

    @Test
    void createItem_Student_Forbidden() {
        // Act
        ResponseEntity<StationeryItemResponse> response = inventoryController.createItem(
                sampleRequest, "ROLE_STUDENT", "studentUser");

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNull(response.getBody());
        verify(inventoryService, never()).createItem(any());
    }

    @Test
    void getAllItems_Success() {
        // Arrange
        Page<StationeryItemResponse> page = new PageImpl<>(Collections.singletonList(sampleResponse));
        when(inventoryService.getAllItems(0, 20, "name")).thenReturn(page);

        // Act
        ResponseEntity<Page<StationeryItemResponse>> response = inventoryController.getAllItems(0, 20, "name");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(inventoryService, times(1)).getAllItems(0, 20, "name");
    }

    @Test
    void getItemById_Success() {
        // Arrange
        when(inventoryService.getItemById(1L)).thenReturn(sampleResponse);

        // Act
        ResponseEntity<StationeryItemResponse> response = inventoryController.getItemById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(inventoryService, times(1)).getItemById(1L);
    }

    @Test
    void getItemsByCategory_Success() {
        // Arrange
        Page<StationeryItemResponse> page = new PageImpl<>(Collections.singletonList(sampleResponse));
        when(inventoryService.getItemsByCategory("PENS", 0, 20)).thenReturn(page);

        // Act
        ResponseEntity<Page<StationeryItemResponse>> response = inventoryController.getItemsByCategory("PENS", 0, 20);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(inventoryService, times(1)).getItemsByCategory("PENS", 0, 20);
    }

    @Test
    void updateItem_Admin_Success() {
        // Arrange
        when(inventoryService.updateItem(eq(1L), any(StationeryItemRequest.class), anyString(), anyString())).thenReturn(sampleResponse);

        // Act
        ResponseEntity<StationeryItemResponse> response = inventoryController.updateItem(
                1L, sampleRequest, "ADMIN", "adminUser");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(inventoryService, times(1)).updateItem(1L, sampleRequest, "adminUser", "ADMIN");
    }

    @Test
    void updateItem_Student_Forbidden() {
        // Act
        ResponseEntity<StationeryItemResponse> response = inventoryController.updateItem(
                1L, sampleRequest, "STUDENT", "studentUser");

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(inventoryService, never()).updateItem(anyLong(), any(), anyString(), anyString());
    }

    @Test
    void deleteItem_Admin_Success() {
        // Act
        ResponseEntity<Void> response = inventoryController.deleteItem(1L, "ROLE_ADMIN", "adminUser");

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(inventoryService, times(1)).deleteItem(1L, "adminUser", "ROLE_ADMIN");
    }

    @Test
    void deleteItem_Student_Forbidden() {
        // Act
        ResponseEntity<Void> response = inventoryController.deleteItem(1L, "ROLE_STUDENT", "studentUser");

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(inventoryService, never()).deleteItem(anyLong());
    }

    @Test
    void getLowStockItems_Admin_Success() {
        // Arrange
        when(inventoryService.getLowStockItems()).thenReturn(Collections.singletonList(sampleResponse));

        // Act
        ResponseEntity<List<StationeryItemResponse>> response = inventoryController.getLowStockItems("ADMIN");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(inventoryService, times(1)).getLowStockItems();
    }

    @Test
    void getLowStockItems_Student_Forbidden() {
        // Act
        ResponseEntity<List<StationeryItemResponse>> response = inventoryController.getLowStockItems("STUDENT");

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(inventoryService, never()).getLowStockItems();
    }

    @Test
    void deductQuantity_Success() {
        // Arrange
        when(inventoryService.deductQuantity(1L, 5)).thenReturn(true);

        // Act
        ResponseEntity<Boolean> response = inventoryController.deductQuantity(1L, 5);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());
        verify(inventoryService, times(1)).deductQuantity(1L, 5);
    }

    @Test
    void searchItems_Success() {
        // Arrange
        when(inventoryService.searchItems("Pen")).thenReturn(Collections.singletonList(sampleResponse));

        // Act
        ResponseEntity<List<StationeryItemResponse>> response = inventoryController.searchItems("Pen");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(inventoryService, times(1)).searchItems("Pen");
    }
}
