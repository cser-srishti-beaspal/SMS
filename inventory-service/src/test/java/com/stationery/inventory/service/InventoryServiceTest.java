package com.stationery.inventory.service;

import com.stationery.inventory.dto.StationeryItemRequest;
import com.stationery.inventory.dto.StationeryItemResponse;
import com.stationery.inventory.exception.InsufficientStockException;
import com.stationery.inventory.exception.ResourceNotFoundException;
import com.stationery.inventory.model.StationeryItem;
import com.stationery.inventory.repository.StationeryItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the InventoryService class.
 * Uses Mockito to mock the repository layer and verify service behavior.
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private StationeryItemRepository stationeryItemRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private StationeryItem sampleItem;
    private StationeryItemRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleItem = StationeryItem.builder()
                .id(1L)
                .name("Blue Ballpoint Pen")
                .category("PEN")
                .unit("pieces")
                .availableQuantity(100)
                .minimumQuantity(20)
                .description("Standard blue ballpoint pen")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleRequest = StationeryItemRequest.builder()
                .name("Blue Ballpoint Pen")
                .category("PEN")
                .unit("pieces")
                .availableQuantity(100)
                .minimumQuantity(20)
                .description("Standard blue ballpoint pen")
                .build();
    }

    @Test
    @DisplayName("Should create a stationery item successfully")
    void createItem_Success() {
        // Arrange
        when(stationeryItemRepository.save(any(StationeryItem.class))).thenReturn(sampleItem);

        // Act
        StationeryItemResponse response = inventoryService.createItem(sampleRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Blue Ballpoint Pen");
        assertThat(response.getCategory()).isEqualTo("PEN");
        assertThat(response.getUnit()).isEqualTo("pieces");
        assertThat(response.getAvailableQuantity()).isEqualTo(100);
        assertThat(response.getMinimumQuantity()).isEqualTo(20);
        assertThat(response.isLowStock()).isFalse();

        verify(stationeryItemRepository, times(1)).save(any(StationeryItem.class));
    }

    @Test
    @DisplayName("Should get a stationery item by ID successfully")
    void getItemById_Success() {
        // Arrange
        when(stationeryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));

        // Act
        StationeryItemResponse response = inventoryService.getItemById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Blue Ballpoint Pen");

        verify(stationeryItemRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when item ID not found")
    void getItemById_NotFound() {
        // Arrange
        when(stationeryItemRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.getItemById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Stationery item not found with ID: 99");

        verify(stationeryItemRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Should update a stationery item successfully")
    void updateItem_Success() {
        // Arrange
        StationeryItemRequest updateRequest = StationeryItemRequest.builder()
                .name("Red Ballpoint Pen")
                .category("PEN")
                .unit("pieces")
                .availableQuantity(150)
                .minimumQuantity(25)
                .description("Standard red ballpoint pen")
                .build();

        StationeryItem updatedItem = StationeryItem.builder()
                .id(1L)
                .name("Red Ballpoint Pen")
                .category("PEN")
                .unit("pieces")
                .availableQuantity(150)
                .minimumQuantity(25)
                .description("Standard red ballpoint pen")
                .createdAt(sampleItem.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(stationeryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
        when(stationeryItemRepository.save(any(StationeryItem.class))).thenReturn(updatedItem);

        // Act
        StationeryItemResponse response = inventoryService.updateItem(1L, updateRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Red Ballpoint Pen");
        assertThat(response.getAvailableQuantity()).isEqualTo(150);
        assertThat(response.getMinimumQuantity()).isEqualTo(25);

        verify(stationeryItemRepository, times(1)).findById(1L);
        verify(stationeryItemRepository, times(1)).save(any(StationeryItem.class));
    }

    @Test
    @DisplayName("Should delete a stationery item successfully")
    void deleteItem_Success() {
        // Arrange
        when(stationeryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
        doNothing().when(stationeryItemRepository).delete(sampleItem);

        // Act
        inventoryService.deleteItem(1L);

        // Assert
        verify(stationeryItemRepository, times(1)).findById(1L);
        verify(stationeryItemRepository, times(1)).delete(sampleItem);
    }

    @Test
    @DisplayName("Should deduct quantity successfully when sufficient stock is available")
    void deductQuantity_Success() {
        // Arrange
        when(stationeryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
        when(stationeryItemRepository.save(any(StationeryItem.class))).thenReturn(sampleItem);

        // Act
        boolean result = inventoryService.deductQuantity(1L, 30);

        // Assert
        assertThat(result).isTrue();
        assertThat(sampleItem.getAvailableQuantity()).isEqualTo(70);

        verify(stationeryItemRepository, times(1)).findById(1L);
        verify(stationeryItemRepository, times(1)).save(any(StationeryItem.class));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when deducting more than available")
    void deductQuantity_InsufficientStock() {
        // Arrange
        when(stationeryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.deductQuantity(1L, 200))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        verify(stationeryItemRepository, times(1)).findById(1L);
        verify(stationeryItemRepository, never()).save(any(StationeryItem.class));
    }

    @Test
    @DisplayName("Should return low stock items correctly")
    void getLowStockItems() {
        // Arrange
        StationeryItem lowStockItem = StationeryItem.builder()
                .id(2L)
                .name("A4 Paper")
                .category("PAPER")
                .unit("packs")
                .availableQuantity(5)
                .minimumQuantity(10)
                .description("A4 size paper pack")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        StationeryItem normalStockItem = StationeryItem.builder()
                .id(3L)
                .name("Stapler")
                .category("STAPLER")
                .unit("pieces")
                .availableQuantity(50)
                .minimumQuantity(5)
                .description("Standard office stapler")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(stationeryItemRepository.findAll())
                .thenReturn(Arrays.asList(sampleItem, lowStockItem, normalStockItem));

        // Act
        List<StationeryItemResponse> lowStockItems = inventoryService.getLowStockItems();

        // Assert
        assertThat(lowStockItems).hasSize(1);
        assertThat(lowStockItems.get(0).getId()).isEqualTo(2L);
        assertThat(lowStockItems.get(0).getName()).isEqualTo("A4 Paper");
        assertThat(lowStockItems.get(0).isLowStock()).isTrue();

        verify(stationeryItemRepository, times(1)).findAll();
    }
}
