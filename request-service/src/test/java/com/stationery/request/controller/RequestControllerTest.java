package com.stationery.request.controller;

import com.stationery.request.dto.ApproveRejectDto;
import com.stationery.request.dto.CreateRequestDto;
import com.stationery.request.dto.RequestItemDto;
import com.stationery.request.dto.RequestResponse;
import com.stationery.request.service.RequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestControllerTest {

    @Mock
    private RequestService requestService;

    @InjectMocks
    private RequestController requestController;

    private CreateRequestDto createRequestDto;
    private RequestResponse sampleResponse;

    @BeforeEach
    void setUp() {
        RequestItemDto itemDto = RequestItemDto.builder()
                .itemId(1L)
                .itemName("Eraser")
                .quantity(3)
                .build();

        createRequestDto = CreateRequestDto.builder()
                .items(Collections.singletonList(itemDto))
                .build();

        sampleResponse = RequestResponse.builder()
                .id(1L)
                .requestId("req-uuid-999")
                .studentUsername("student1")
                .status("PENDING")
                .items(Collections.singletonList(itemDto))
                .build();
    }

    @Test
    void createRequest_Student_Success() {
        // Arrange
        when(requestService.createRequest("student1", createRequestDto)).thenReturn(sampleResponse);

        // Act
        ResponseEntity<RequestResponse> response = requestController.createRequest(
                "student1", "ROLE_STUDENT", createRequestDto);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(requestService, times(1)).createRequest("student1", createRequestDto);
    }

    @Test
    void createRequest_Admin_Forbidden() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestController.createRequest("student1", "ROLE_ADMIN", createRequestDto);
        });
        assertTrue(exception.getMessage().contains("Access denied"));
        verify(requestService, never()).createRequest(any(), any());
    }

    @Test
    void getMyRequests_WithStatus_Success() {
        // Arrange
        when(requestService.getRequestsByStudentAndStatus("student1", "PENDING"))
                .thenReturn(Collections.singletonList(sampleResponse));

        // Act
        ResponseEntity<List<RequestResponse>> response = requestController.getMyRequests("student1", "PENDING");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(requestService, times(1)).getRequestsByStudentAndStatus("student1", "PENDING");
    }

    @Test
    void getMyRequests_NoStatus_Success() {
        // Arrange
        when(requestService.getRequestsByStudent("student1"))
                .thenReturn(Collections.singletonList(sampleResponse));

        // Act
        ResponseEntity<List<RequestResponse>> response = requestController.getMyRequests("student1", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(requestService, times(1)).getRequestsByStudent("student1");
    }

    @Test
    void getRequestById_Success() {
        // Arrange
        when(requestService.getRequestById(1L)).thenReturn(sampleResponse);

        // Act
        ResponseEntity<RequestResponse> response = requestController.getRequestById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(requestService, times(1)).getRequestById(1L);
    }

    @Test
    void getRequestByRequestId_Success() {
        // Arrange
        when(requestService.getRequestByRequestId("req-uuid-999")).thenReturn(sampleResponse);

        // Act
        ResponseEntity<RequestResponse> response = requestController.getRequestByRequestId("req-uuid-999");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(requestService, times(1)).getRequestByRequestId("req-uuid-999");
    }

    @Test
    void getAllRequests_Admin_NoStatus_Success() {
        // Arrange
        when(requestService.getAllRequests()).thenReturn(Collections.singletonList(sampleResponse));

        // Act
        ResponseEntity<List<RequestResponse>> response = requestController.getAllRequests("ROLE_ADMIN", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(requestService, times(1)).getAllRequests();
    }

    @Test
    void getAllRequests_Admin_WithStatus_Success() {
        // Arrange
        when(requestService.getAllRequestsByStatus("APPROVED"))
                .thenReturn(Collections.singletonList(sampleResponse));

        // Act
        ResponseEntity<List<RequestResponse>> response = requestController.getAllRequests("ADMIN", "APPROVED");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(requestService, times(1)).getAllRequestsByStatus("APPROVED");
    }

    @Test
    void getAllRequests_Student_Forbidden() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestController.getAllRequests("ROLE_STUDENT", null);
        });
        assertTrue(exception.getMessage().contains("Access denied"));
        verify(requestService, never()).getAllRequests();
    }

    @Test
    void approveRequest_Admin_Success() {
        // Arrange
        when(requestService.approveRequest(1L, "admin1")).thenReturn(sampleResponse);

        // Act
        ResponseEntity<RequestResponse> response = requestController.approveRequest(1L, "admin1", "ROLE_ADMIN");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(requestService, times(1)).approveRequest(1L, "admin1");
    }

    @Test
    void rejectRequest_Admin_Success() {
        // Arrange
        ApproveRejectDto rejectDto = ApproveRejectDto.builder()
                .rejectionReason("No budget")
                .build();
        when(requestService.rejectRequest(1L, "admin1", "No budget")).thenReturn(sampleResponse);

        // Act
        ResponseEntity<RequestResponse> response = requestController.rejectRequest(1L, "admin1", "ROLE_ADMIN", rejectDto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(requestService, times(1)).rejectRequest(1L, "admin1", "No budget");
    }

    @Test
    void fulfillRequest_Admin_Success() {
        // Arrange
        when(requestService.fulfillRequest(1L)).thenReturn(sampleResponse);

        // Act
        ResponseEntity<RequestResponse> response = requestController.fulfillRequest(1L, "ROLE_ADMIN");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleResponse, response.getBody());
        verify(requestService, times(1)).fulfillRequest(1L);
    }

    @Test
    void validateRole_NullRole_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestController.fulfillRequest(1L, null);
        });
        assertEquals("Access denied. Role is null", exception.getMessage());
    }
}
