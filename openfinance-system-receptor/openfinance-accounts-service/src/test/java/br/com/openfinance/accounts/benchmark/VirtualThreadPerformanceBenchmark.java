package br.com.openfinance.accounts.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"--enable-preview", "-Xmx2g"})
@Warmup(iterations = 3)
@Measurement(iterations = 5)
public class VirtualThreadPerformanceBenchmark {

    private static final int TASK_COUNT = 10000;
    private static final int TASK_DURATION_MS = 10;

    @Benchmark
    public void testPlatformThreadPool() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(100);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < TASK_COUNT; i++) {
            futures.add(executor.submit(this::simulateWork));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Benchmark
    public void testVirtualThreads() throws InterruptedException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<? extends Future<?>> futures = IntStream.range(0, TASK_COUNT)
                    .mapToObj(i -> executor.submit(this::simulateWork))
                    .toList();

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Benchmark
    public void testStructuredConcurrency() throws InterruptedException {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            IntStream.range(0, TASK_COUNT)
                    .forEach(i -> scope.fork(this::simulateWorkCallable));

            scope.join();
        }
    }

    private void simulateWork() {
        try {
            Thread.sleep(TASK_DURATION_MS);
            // Simulate some CPU work
            int sum = 0;
            for (int i = 0; i < 1000; i++) {
                sum += i;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Integer simulateWorkCallable() {
        simulateWork();
        return 1;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(VirtualThreadPerformanceBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
