package br.com.openfinance.service.accounts.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableCaching
@EnableAsync
public class AccountsConfiguration {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "accounts",
                "account",
                "balances",
                "transactions"
        );
    }

    @Bean(name = "accountsAsyncExecutor")
    public Executor asyncExecutor() {
        // Virtual Threads Executor for async operations
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
