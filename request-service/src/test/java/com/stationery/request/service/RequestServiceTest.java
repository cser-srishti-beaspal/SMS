package com.stationery.request.service;

import com.stationery.request.client.InventoryClient;
import com.stationery.request.dto.CreateRequestDto;
import com.stationery.request.dto.RequestItemDto;
import com.stationery.request.dto.RequestResponse;
import com.stationery.request.exception.ResourceNotFoundException;
import com.stationery.request.model.RequestItem;
import com.stationery.request.model.RequestStatus;
import com.stationery.request.model.StationeryRequest;
import com.stationery.request.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private RequestService requestService;

    private StationeryRequest sampleRequest;
    private CreateRequestDto createRequestDto;
    private RequestItemDto requestItemDto;

    @BeforeEach
    void setUp() {
        requestItemDto = RequestItemDto.builder()
                .itemId(1L)
                .itemName("Blue Pen")
                .quantity(5)
                .build();

        createRequestDto = CreateRequestDto.builder()
                .items(List.of(requestItemDto))
                .build();

        RequestItem requestItem = RequestItem.builder()
                .id(1L)
                .itemId(1L)
                .itemName("Blue Pen")
                .quantity(5)
                .build();

        sampleRequest = StationeryRequest.builder()
                .id(1L)
                .requestId("test-uuid-1234")
                .studentUsername("student1")
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(new ArrayList<>(List.of(requestItem)))
                .build();

        requestItem.setRequest(sampleRequest);
    }

    @Test
    @DisplayName("Should create a request successfully")
    void createRequest_Success() {
        // Arrange
        when(requestRepository.save(any(StationeryRequest.class))).thenAnswer(invocation -> {
            StationeryRequest req = invocation.getArgument(0);
            req.setId(1L);
            req.setRequestId("generated-uuid");
            req.setCreatedAt(LocalDateTime.now());
            req.setUpdatedAt(LocalDateTime.now());
            return req;
        });

        // Act
        RequestResponse response = requestService.createRequest("student1", createRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals("student1", response.getStudentUsername());
        assertEquals("PENDING", response.getStatus());
        assertEquals(1, response.getItems().size());
        assertEquals("Blue Pen", response.getItems().get(0).getItemName());
        assertEquals(5, response.getItems().get(0).getQuantity());
        verify(requestRepository, times(1)).save(any(StationeryRequest.class));
    }

    @Test
    @DisplayName("Should get request by ID successfully")
    void getRequestById_Success() {
        // Arrange
        when(requestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));

        // Act
        RequestResponse response = requestService.getRequestById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test-uuid-1234", response.getRequestId());
        assertEquals("student1", response.getStudentUsername());
        assertEquals("PENDING", response.getStatus());
        verify(requestRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when request not found by ID")
    void getRequestById_NotFound() {
        // Arrange
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> requestService.getRequestById(99L)
        );
        assertEquals("Request not found with id: '99'", exception.getMessage());
        verify(requestRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Should get requests by student username successfully")
    void getRequestsByStudent_Success() {
        // Arrange
        when(requestRepository.findByStudentUsername("student1"))
                .thenReturn(List.of(sampleRequest));

        // Act
        List<RequestResponse> responses = requestService.getRequestsByStudent("student1");

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("student1", responses.get(0).getStudentUsername());
        verify(requestRepository, times(1)).findByStudentUsername("student1");
    }

    @Test
    @DisplayName("Should approve request successfully and deduct inventory")
    void approveRequest_Success() {
        // Arrange
        when(requestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(inventoryClient.deductItemQuantity(eq(1L), eq(5)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "success")));
        when(requestRepository.save(any(StationeryRequest.class))).thenAnswer(invocation -> {
            StationeryRequest req = invocation.getArgument(0);
            req.setUpdatedAt(LocalDateTime.now());
            return req;
        });

        // Act
        RequestResponse response = requestService.approveRequest(1L, "admin1");

        // Assert
        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertEquals("admin1", response.getAdminUsername());
        verify(inventoryClient, times(1)).deductItemQuantity(eq(1L), eq(5));
        verify(requestRepository, times(1)).save(any(StationeryRequest.class));
    }

    @Test
    @DisplayName("Should reject request successfully with reason")
    void rejectRequest_Success() {
        // Arrange
        when(requestRepository.findById(1L)).thenReturn(Optional.of(sampleRequest));
        when(requestRepository.save(any(StationeryRequest.class))).thenAnswer(invocation -> {
            StationeryRequest req = invocation.getArgument(0);
            req.setUpdatedAt(LocalDateTime.now());
            return req;
        });

        // Act
        RequestResponse response = requestService.rejectRequest(1L, "admin1", "Out of budget");

        // Assert
        assertNotNull(response);
        assertEquals("REJECTED", response.getStatus());
        assertEquals("admin1", response.getAdminUsername());
        assertEquals("Out of budget", response.getRejectionReason());
        verify(requestRepository, times(1)).save(any(StationeryRequest.class));
        verify(inventoryClient, never()).deductItemQuantity(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when approving non-existent request")
    void approveRequest_NotFound() {
        // Arrange
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> requestService.approveRequest(99L, "admin1")
        );
        assertEquals("Request not found with id: '99'", exception.getMessage());
        verify(requestRepository, times(1)).findById(99L);
        verify(inventoryClient, never()).deductItemQuantity(anyLong(), anyInt());
        verify(requestRepository, never()).save(any(StationeryRequest.class));
    }
}
