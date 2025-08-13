package br.com.openfinance.resources.application.services;

import br.com.openfinance.resources.domain.model.Resource;
import br.com.openfinance.resources.domain.model.ResourceStatus;
import br.com.openfinance.resources.domain.model.ResourceType;
import br.com.openfinance.resources.domain.ports.output.ResourceRepository;
import br.com.openfinance.resources.domain.ports.output.ResourceEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VirtualThreadResourceBatchProcessorTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ResourceApplicationService resourceApplicationService;

    @Mock
    private ResourceEventPublisher eventPublisher;

    private VirtualThreadResourceBatchProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new VirtualThreadResourceBatchProcessor(
                resourceRepository,
                resourceApplicationService,
                eventPublisher
        );
        
        // Set test values
        ReflectionTestUtils.setField(processor, "maxVirtualThreads", 100);
        ReflectionTestUtils.setField(processor, "batchSize", 10);
        ReflectionTestUtils.setField(processor, "timeoutMinutes", 5);
    }

    @Test
    void testProcessResourceUpdatesWithVirtualThreads() {
        // Given
        List<Resource> mockResources = createMockResources(5);
        when(resourceRepository.findResourcesForBatchUpdate(anyInt()))
                .thenReturn(mockResources);

        // When
        processor.processResourceUpdatesWithVirtualThreads();

        // Then
        verify(resourceRepository).findResourcesForBatchUpdate(10);
        verify(eventPublisher).publishBatchSyncCompleted(anyInt());
    }

    @Test
    void testProcessResourcesWithPagination() {
        // Given
        List<Resource> firstBatch = createMockResources(5);
        List<Resource> emptyBatch = new ArrayList<>();
        
        when(resourceRepository.findResourcesNeedingSync(anyInt()))
                .thenReturn(firstBatch)
                .thenReturn(emptyBatch);

        // When
        processor.processResourcesWithPagination();

        // Then
        verify(resourceRepository, atLeast(2)).findResourcesNeedingSync(anyInt());
    }

    @Test
    void testProcessEmptyBatch() {
        // Given
        when(resourceRepository.findResourcesForBatchUpdate(anyInt()))
                .thenReturn(new ArrayList<>());

        // When
        processor.processResourceUpdatesWithVirtualThreads();

        // Then
        verify(resourceRepository).findResourcesForBatchUpdate(10);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testProcessResourcesByCustomerWithVirtualThreads() {
        // Given
        String customerId = "test-customer-123";

        // When
        processor.processResourcesByCustomerWithVirtualThreads(customerId);

        // Then
        verify(resourceApplicationService).syncResourcesByCustomer(customerId);
    }

    private List<Resource> createMockResources(int count) {
        List<Resource> resources = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Resource resource = Resource.builder()
                    .resourceId(UUID.randomUUID())
                    .externalResourceId("external-" + i)
                    .customerId("customer-" + i)
                    .participantId("participant-1")
                    .brandId("brand-1")
                    .type(ResourceType.ACCOUNT)
                    .status(ResourceStatus.ACTIVE)
                    .name("Test Resource " + i)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now().minusHours(1))
                    .build();
            resources.add(resource);
        }
        return resources;
    }
}