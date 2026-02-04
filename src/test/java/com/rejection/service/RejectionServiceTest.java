package com.rejection.service;

import com.rejection.service.business.RejectionService;
import com.rejection.service.cache.RejectionCache;
import com.rejection.service.model.RejectionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RejectionServiceTest {

    @Mock
    private RejectionCache rejectionCache;

    @InjectMocks
    private RejectionService rejectionService;

    @Test
    void getRandomRejection_ShouldReturnValidResponse() {
        when(rejectionCache.isInitialized()).thenReturn(true);
        when(rejectionCache.getRandomReason()).thenReturn("Test rejection");

        RejectionResponse response = rejectionService.getRandomRejection();

        assertNotNull(response);
        assertEquals("Test rejection", response.getReason());
        assertNotNull(response.getTimestamp());
        assertTrue(response.getId() > 0);
    }

    @Test
    void getRandomRejection_ShouldThrowWhenCacheNotInitialized() {
        when(rejectionCache.isInitialized()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> rejectionService.getRandomRejection());
    }
}