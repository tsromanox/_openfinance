package br.com.openfinance.participants.performance;

import br.com.openfinance.participants.service.ParticipantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PerformanceTest {

    @Autowired
    private ParticipantService participantService;

    @Test
    void shouldHandleConcurrentRequests() throws Exception {
        // Given
        int numberOfThreads = 100;
        int requestsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // When
        CompletableFuture<?>[] futures = IntStream.range(0, numberOfThreads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < requestsPerThread; j++) {
                        participantService.getAllParticipants();
                        participantService.getAvailableApiFamilies();
                        participantService.getCacheStatus();
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();
        stopWatch.stop();

        // Then
        double totalTime = stopWatch.getTotalTimeSeconds();
        int totalRequests = numberOfThreads * requestsPerThread * 3; // 3 operations per iteration
        double requestsPerSecond = totalRequests / totalTime;

        System.out.printf("Performance Test Results:%n");
        System.out.printf("Total Requests: %d%n", totalRequests);
        System.out.printf("Total Time: %.2f seconds%n", totalTime);
        System.out.printf("Requests per Second: %.2f%n", requestsPerSecond);

        assertThat(requestsPerSecond).isGreaterThan(100); // Should handle at least 100 req/s

        executor.shutdown();
    }
}
