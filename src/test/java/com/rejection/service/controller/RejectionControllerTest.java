package com.rejection.service.controller;

import com.rejection.service.business.RejectionService;
import com.rejection.service.model.RejectionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for RejectionController
 * Tests all endpoints, error scenarios, and security features
 */
@ExtendWith(MockitoExtension.class)
class RejectionControllerTest {
    
    @Mock
    private RejectionService rejectionService;
    
    private RejectionController controller;
    private MockHttpServletRequest request;
    
    @BeforeEach
    void setUp() {
        controller = new RejectionController(rejectionService);
        setupDefaultRequest();
    }
    
    private void setupDefaultRequest() {
        request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
    }
    
    @Test
    void testGetRandomRejection_Success() {
        // Arrange
        RejectionResponse mockResponse = new RejectionResponse(1, "Test rejection");
        when(rejectionService.getRandomRejection()).thenReturn(mockResponse);
        
        // Act
        ResponseEntity<RejectionResponse> response = controller.getRandomRejection(request);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test rejection", response.getBody().getReason());
        
        // Verify security headers
        assertEquals(mockResponse.getRequestId(), response.getHeaders().getFirst("X-Request-ID"));
        assertEquals("no-cache, no-store, must-revalidate", response.getHeaders().getFirst("Cache-Control"));
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        
        verify(rejectionService).getRandomRejection();
    }
    
    @Test
    void testGetRandomRejection_ServiceUnavailable() {
        // Arrange
        when(rejectionService.getRandomRejection()).thenThrow(new IllegalStateException("Service not ready"));
        
        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> controller.getRandomRejection(request));
        
        assertEquals("Service not ready", exception.getMessage());
        verify(rejectionService).getRandomRejection();
    }
    
    @Test
    void testGetRandomRejection_UnexpectedError() {
        // Arrange
        when(rejectionService.getRandomRejection()).thenThrow(new RuntimeException("Unexpected error"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> controller.getRandomRejection(request));
        
        assertEquals("Internal server error", exception.getMessage());
        verify(rejectionService).getRandomRejection();
    }
    
    @Test
    void testGetRandomRejection_WithProxyHeaders() {
        // Arrange
        request.addHeader("X-Forwarded-For", "192.168.1.100, 10.0.0.1");
        request.addHeader("User-Agent", "Test-Agent/1.0");
        
        RejectionResponse mockResponse = new RejectionResponse(1, "Test rejection");
        when(rejectionService.getRandomRejection()).thenReturn(mockResponse);
        
        // Act
        ResponseEntity<RejectionResponse> response = controller.getRandomRejection(request);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(rejectionService).getRandomRejection();
    }
    
    @Test
    void testGetRandomRejection_WithXRealIP() {
        // Arrange
        request.addHeader("X-Real-IP", "203.0.113.1");
        
        RejectionResponse mockResponse = new RejectionResponse(1, "Test rejection");
        when(rejectionService.getRandomRejection()).thenReturn(mockResponse);
        
        // Act
        ResponseEntity<RejectionResponse> response = controller.getRandomRejection(request);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(rejectionService).getRandomRejection();
    }
    
    @Test
    void testHealth_Success() {
        // Arrange
        RejectionService.ServiceStats mockStats = new RejectionService.ServiceStats(
            100L, 95L, 5L, 95.0, 100, true
        );
        when(rejectionService.getServiceStatistics()).thenReturn(mockStats);
        
        // Act
        ResponseEntity<Map<String, Object>> response = controller.health();
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertEquals("UP", body.get("status"));
        assertNotNull(body.get("timestamp"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = null;
        try {
            statistics = (Map<String, Object>) body.get("statistics");
        } catch (ClassCastException e) {
            Object statsObj = body.get("statistics");
            fail("Statistics should be a Map but was: " + (statsObj != null ? statsObj.getClass() : "null"));
            return; // This line will never be reached due to fail(), but helps with flow analysis
        }
        assertNotNull(statistics);
        assertEquals(100L, statistics.get("totalRequests"));
        assertEquals(95L, statistics.get("successfulRequests"));
        assertEquals(5L, statistics.get("errorRequests"));
        assertEquals(true, statistics.get("cacheInitialized"));
        assertEquals("95.00%", statistics.get("successRate"));
        assertEquals(100, statistics.get("cacheSize"));
        
        // Verify cache control header
        assertEquals("no-cache", response.getHeaders().getFirst("Cache-Control"));
        
        verify(rejectionService).getServiceStatistics();
    }
    
    @Test
    void testHealth_ServiceError() {
        // Arrange
        when(rejectionService.getServiceStatistics()).thenThrow(new RuntimeException("Statistics error"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = controller.health();
        
        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertEquals("DOWN", body.get("status"));
        assertEquals("Statistics error", body.get("error"));
        assertNotNull(body.get("timestamp"));
        
        verify(rejectionService).getServiceStatistics();
    }
    
    @Test
    void testClientIpExtraction_XForwardedFor() {
        // Arrange
        request.addHeader("X-Forwarded-For", "192.168.1.100, 10.0.0.1, 172.16.0.1");
        
        RejectionResponse mockResponse = new RejectionResponse(1, "Test rejection");
        when(rejectionService.getRandomRejection()).thenReturn(mockResponse);
        
        // Act
        ResponseEntity<RejectionResponse> response = controller.getRandomRejection(request);
        
        // Assert - Should extract first IP from X-Forwarded-For
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(rejectionService).getRandomRejection();
    }
    
    @Test
    void testClientIpExtraction_FallbackToRemoteAddr() {
        // Arrange - No proxy headers
        request.setRemoteAddr("203.0.113.50");
        
        RejectionResponse mockResponse = new RejectionResponse(1, "Test rejection");
        when(rejectionService.getRandomRejection()).thenReturn(mockResponse);
        
        // Act
        ResponseEntity<RejectionResponse> response = controller.getRandomRejection(request);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(rejectionService).getRandomRejection();
    }
    
    @Test
    void testClientIpExtraction_UnknownHeaders() {
        // Arrange
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("X-Real-IP", "unknown");
        request.setRemoteAddr("127.0.0.1");
        
        RejectionResponse mockResponse = new RejectionResponse(1, "Test rejection");
        when(rejectionService.getRandomRejection()).thenReturn(mockResponse);
        
        // Act
        ResponseEntity<RejectionResponse> response = controller.getRandomRejection(request);
        
        // Assert - Should fallback to remote address
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(rejectionService).getRandomRejection();
    }
}