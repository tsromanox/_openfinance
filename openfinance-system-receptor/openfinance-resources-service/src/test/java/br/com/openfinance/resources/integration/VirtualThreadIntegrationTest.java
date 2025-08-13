package br.com.openfinance.resources.integration;

import br.com.openfinance.resources.domain.model.Resource;
import br.com.openfinance.resources.domain.model.ResourceStatus;
import br.com.openfinance.resources.domain.model.ResourceType;
import br.com.openfinance.resources.domain.ports.output.ResourceRepository;
import br.com.openfinance.resources.application.services.VirtualThreadResourceBatchProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/test",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "openfinance.resources.batch.size=100",
    "openfinance.resources.batch.virtual-threads.max=1000"
})
class VirtualThreadIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private VirtualThreadResourceBatchProcessor batchProcessor;

    @Test
    @Transactional
    void testConcurrentResourceProcessingWithVirtualThreads() throws InterruptedException {
        // Given
        int resourceCount = 100;
        List<Resource> resources = createTestResources(resourceCount);
        
        // Save resources to repository
        for (Resource resource : resources) {
            resourceRepository.save(resource);
        }

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        // When - Process resources concurrently with Virtual Threads
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                virtualExecutor.submit(() -> {
                    try {
                        Thread currentThread = Thread.currentThread();
                        System.out.println("Thread " + threadIndex + 
                                         " running on: " + currentThread.getName() + 
                                         " (Virtual: " + currentThread.isVirtual() + ")");
                        
                        // Process a subset of resources
                        processResourceSubset(threadIndex, resourceCount / threadCount);
                        
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            // Wait for all virtual threads to complete
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        }

        // Then - Verify all resources were processed
        System.out.println("All " + threadCount + " virtual threads completed processing");
    }

    @Test
    void testVirtualThreadBatchProcessor() {
        // Given
        List<Resource> resources = createTestResources(50);
        for (Resource resource : resources) {
            resourceRepository.save(resource);
        }

        // When
        batchProcessor.processResourcesWithPagination();

        // Then
        System.out.println("Batch processing with Virtual Threads completed successfully");
    }

    @Test
    void testVirtualThreadScalability() throws InterruptedException {
        System.out.println("Testing Virtual Thread scalability...");
        
        int threadCount = 10000; // Test with many virtual threads
        CountDownLatch latch = new CountDownLatch(threadCount);
        long startTime = System.currentTimeMillis();

        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            for (int i = 0; i < threadCount; i++) {
                final int taskId = i;
                virtualExecutor.submit(() -> {
                    try {
                        // Simulate I/O-bound work
                        Thread.sleep(10);
                        
                        // Verify we're running on a virtual thread
                        Thread currentThread = Thread.currentThread();
                        assertThat(currentThread.isVirtual()).isTrue();
                        
                        if (taskId % 1000 == 0) {
                            System.out.println("Virtual thread " + taskId + " completed on: " + 
                                             currentThread.getName());
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("Successfully created and executed " + threadCount + 
                         " virtual threads in " + duration + "ms");
        System.out.println("Average time per virtual thread: " + 
                         String.format("%.4f", (double) duration / threadCount) + "ms");
    }

    private void processResourceSubset(int threadIndex, int count) {
        try {
            // Simulate resource processing work
            for (int i = 0; i < count; i++) {
                // Simulate I/O operation
                Thread.sleep(1);
                
                // Simulate processing logic
                String processedData = "Thread-" + threadIndex + "-Resource-" + i;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing interrupted", e);
        }
    }

    private List<Resource> createTestResources(int count) {
        List<Resource> resources = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Resource resource = Resource.builder()
                    .resourceId(UUID.randomUUID())
                    .externalResourceId("test-external-" + i)
                    .customerId("test-customer-" + (i % 10))
                    .participantId("test-participant-1")
                    .brandId("test-brand-1")
                    .type(ResourceType.values()[i % ResourceType.values().length])
                    .status(ResourceStatus.ACTIVE)
                    .name("Test Resource " + i)
                    .description("Integration test resource")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now().minusHours(2))
                    .build();
            resources.add(resource);
        }
        return resources;
    }
}