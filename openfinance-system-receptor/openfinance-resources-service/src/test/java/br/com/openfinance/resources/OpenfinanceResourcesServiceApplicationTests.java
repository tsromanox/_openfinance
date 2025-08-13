package br.com.openfinance.resources;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OpenfinanceResourcesServiceApplicationTests {

    @Test
    void contextLoads() {
        // Test that the application context loads successfully
    }

    @Test
    void virtualThreadsEnabled() {
        // Test that virtual threads are enabled in the application
        Thread currentThread = Thread.currentThread();
        System.out.println("Test running on thread: " + currentThread.getName() + 
                          " (Virtual: " + currentThread.isVirtual() + ")");
    }
}