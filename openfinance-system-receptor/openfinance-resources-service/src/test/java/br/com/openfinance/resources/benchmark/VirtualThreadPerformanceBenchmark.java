package br.com.openfinance.resources.benchmark;

import br.com.openfinance.resources.domain.model.Resource;
import br.com.openfinance.resources.domain.model.ResourceStatus;
import br.com.openfinance.resources.domain.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class VirtualThreadPerformanceBenchmark {

    private static final int RESOURCE_COUNT = 1000;
    private List<Resource> resources;

    @Setup
    public void setup() {
        resources = createMockResources(RESOURCE_COUNT);
    }

    @Benchmark
    public void benchmarkVirtualThreadProcessing() throws Exception {
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Void>> futures = new ArrayList<>();
            
            for (Resource resource : resources) {
                Future<Void> future = virtualExecutor.submit(() -> {
                    // Simulate resource processing
                    processResource(resource);
                    return null;
                });
                futures.add(future);
            }
            
            // Wait for all tasks to complete
            for (Future<Void> future : futures) {
                future.get();
            }
        }
    }

    @Benchmark
    public void benchmarkPlatformThreadProcessing() throws Exception {
        try (ExecutorService platformExecutor = Executors.newFixedThreadPool(100)) {
            List<Future<Void>> futures = new ArrayList<>();
            
            for (Resource resource : resources) {
                Future<Void> future = platformExecutor.submit(() -> {
                    // Simulate resource processing
                    processResource(resource);
                    return null;
                });
                futures.add(future);
            }
            
            // Wait for all tasks to complete
            for (Future<Void> future : futures) {
                future.get();
            }
        }
    }

    @Test
    @Disabled("Performance test - run manually")
    public void runVirtualThreadPerformanceTest() throws Exception {
        System.out.println("Starting Virtual Thread Performance Benchmark");
        
        long startTime = System.currentTimeMillis();
        
        // Test with Virtual Threads
        benchmarkVirtualThreadProcessing();
        
        long virtualThreadTime = System.currentTimeMillis() - startTime;
        
        startTime = System.currentTimeMillis();
        
        // Test with Platform Threads
        benchmarkPlatformThreadProcessing();
        
        long platformThreadTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Virtual Thread Processing Time: " + virtualThreadTime + "ms");
        System.out.println("Platform Thread Processing Time: " + platformThreadTime + "ms");
        
        double improvement = ((double) (platformThreadTime - virtualThreadTime) / platformThreadTime) * 100;
        System.out.println("Performance improvement: " + String.format("%.2f", improvement) + "%");
    }

    @Test
    @Disabled("Full benchmark - run manually with JMH")
    public void runFullBenchmark() throws Exception {
        Options opt = new OptionsBuilder()
                .include(VirtualThreadPerformanceBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    private void processResource(Resource resource) {
        try {
            // Simulate I/O operation (e.g., HTTP call)
            Thread.sleep(10);
            
            // Simulate CPU-bound work
            String result = "Processed: " + resource.getResourceId() + 
                           " - Type: " + resource.getType() + 
                           " - Customer: " + resource.getCustomerId();
            
            // Simulate validation
            if (resource.getStatus() == ResourceStatus.ACTIVE) {
                // Mark as processed
                resource.setUpdatedAt(LocalDateTime.now());
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing interrupted", e);
        }
    }

    private List<Resource> createMockResources(int count) {
        List<Resource> resourceList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Resource resource = Resource.builder()
                    .resourceId(UUID.randomUUID())
                    .externalResourceId("external-" + i)
                    .customerId("customer-" + (i % 100)) // 100 different customers
                    .participantId("participant-" + (i % 10)) // 10 different participants
                    .brandId("brand-" + (i % 5)) // 5 different brands
                    .type(ResourceType.values()[i % ResourceType.values().length])
                    .status(ResourceStatus.ACTIVE)
                    .name("Test Resource " + i)
                    .description("Test resource for benchmark " + i)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now().minusHours(1))
                    .build();
            resourceList.add(resource);
        }
        return resourceList;
    }

    @Test
    public void testVirtualThreadCreation() {
        System.out.println("Testing Virtual Thread creation performance");
        
        long startTime = System.nanoTime();
        
        // Create 10,000 virtual threads
        for (int i = 0; i < 10000; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        
        System.out.println("Created 10,000 virtual threads in: " + String.format("%.2f", durationMs) + "ms");
        System.out.println("Average time per thread: " + String.format("%.4f", durationMs / 10000) + "ms");
    }
}