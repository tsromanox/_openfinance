package br.com.openfinance.accounts.application.services;

import br.com.openfinance.accounts.domain.model.Account;
import br.com.openfinance.accounts.domain.ports.output.*;
import br.com.openfinance.accounts.domain.services.AccountDomainService;
import br.com.openfinance.core.infrastructure.monitoring.MetricsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VirtualThreadAccountBatchProcessorTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransmitterAccountClient transmitterClient;

    @Mock
    private AccountEventPublisher eventPublisher;

    @Mock
    private AccountDomainService domainService;

    @Mock
    private MetricsCollector metricsCollector;

    private VirtualThreadAccountBatchProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new VirtualThreadAccountBatchProcessor(
                accountRepository,
                transmitterClient,
                eventPublisher,
                domainService,
                metricsCollector
        );

        ReflectionTestUtils.setField(processor, "batchSize", 100);
        ReflectionTestUtils.setField(processor, "maxVirtualThreads", 1000);
        ReflectionTestUtils.setField(processor, "timeoutMinutes", 5);
    }

    @Test
    void shouldProcessAccountsWithVirtualThreads() {
        // Given
        List<Account> accounts = createTestAccounts(100);
        Page<Account> page = new PageImpl<>(accounts);

        when(accountRepository.count()).thenReturn(100L);
        when(accountRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(domainService.shouldSyncAccount(any())).thenReturn(true);
        when(transmitterClient.fetchAccount(anyString(), anyString()))
                .thenAnswer(invocation -> createExternalAccount());
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        processor.processAccountUpdatesWithVirtualThreads();

        // Then
        verify(accountRepository, times(100)).save(any(Account.class));
        verify(metricsCollector, times(100)).recordAccountSynced();
        verify(eventPublisher).publishBatchSyncCompleted(100);
    }

    @Test
    void shouldHandleVirtualThreadInterruption() throws Exception {
        // Given
        List<Account> accounts = createTestAccounts(10);
        Page<Account> page = new PageImpl<>(accounts);

        when(accountRepository.count()).thenReturn(10L);
        when(accountRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(domainService.shouldSyncAccount(any())).thenReturn(true);

        // Simulate slow processing
        when(transmitterClient.fetchAccount(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    Thread.sleep(1000); // Simulate delay
                    return createExternalAccount();
                });

        // When - Set a very short timeout
        ReflectionTestUtils.setField(processor, "timeoutMinutes", 0);

        // Then - Should handle timeout gracefully
        assertThatCode(() -> processor.processAccountUpdatesWithVirtualThreads())
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRetryFailedAccountUpdates() {
        // Given
        Account account = createTestAccount();
        List<Account> accounts = List.of(account);
        Page<Account> page = new PageImpl<>(accounts);

        when(accountRepository.count()).thenReturn(1L);
        when(accountRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(domainService.shouldSyncAccount(any())).thenReturn(true);

        // First two calls fail, third succeeds
        when(transmitterClient.fetchAccount(anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection error"))
                .thenThrow(new RuntimeException("Timeout"))
                .thenReturn(createExternalAccount());

        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        processor.processAccountUpdatesWithVirtualThreads();

        // Then - Should eventually succeed after retries
        verify(transmitterClient, times(3)).fetchAccount(anyString(), anyString());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    private List<Account> createTestAccounts(int count) {
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            accounts.add(createTestAccount());
        }
        return accounts;
    }

    private Account createTestAccount() {
        return Account.builder()
                .accountId(UUID.randomUUID())
                .externalAccountId("EXT-" + UUID.randomUUID())
                .customerId("12345678901")
                .participantId("PART-001")
                .number("123456")
                .checkDigit("7")
                .build();
    }

    private Account createExternalAccount() {
        Account account = createTestAccount();
        account.setAvailableAmount(BigDecimal.valueOf(1000));
        account.setBlockedAmount(BigDecimal.ZERO);
        account.setAutomaticallyInvestedAmount(BigDecimal.ZERO);
        return account;
    }
}
